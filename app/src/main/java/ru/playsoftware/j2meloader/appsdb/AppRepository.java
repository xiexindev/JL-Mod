/*
 * Copyright 2018 Nikita Shakarun
 * Copyright 2020-2024 Yury Kharchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.playsoftware.j2meloader.appsdb;

import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.schedulers.Schedulers;
import kotlin.io.FilesKt;
import kotlin.text.Charsets;
import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.AppUtils;
import ru.playsoftware.j2meloader.util.Constants;

public class AppRepository {
	private final MutableLiveData<List<AppItem>> appList = new MutableLiveData<>();
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	private final ErrorObserver errorObserver = new ErrorObserver();
	private final AppListSQLiteQuery query = new AppListSQLiteQuery();

	private AppDatabase db;

	private void initDb(String file) {
		db = AppDatabase.open(file);
		ConnectableFlowable<List<AppItem>> listConnectableFlowable = db.appItemDao().getAll(query)
				.publish();
		compositeDisposable.add(listConnectableFlowable
				.firstElement()
				.observeOn(Schedulers.io())
				.subscribe(this::syncWithFilesystem, errorObserver::onError));
		compositeDisposable.add(listConnectableFlowable
				.subscribe(this::onAppListLoaded, errorObserver::onError));
		compositeDisposable.add(listConnectableFlowable.connect());
	}

	/**
	 * Re-reads the "last launched midlet" file and pushes an updated list.
	 * Must be called when the screen becomes visible again (e.g. after closing a game).
	 */
	public void refreshLastMidlet() {
		if (db == null) {
			return;
		}
		compositeDisposable.add(db.appItemDao().getAllSingle(query)
				.subscribeOn(Schedulers.from(db.getQueryExecutor()))
				.subscribe(this::onAppListLoaded, errorObserver::onError));
	}

	private void onAppListLoaded(List<AppItem> appItems) {
		sortLastMidLet(appItems);
		appList.postValue(appItems);
	}

	private void execute(Completable completable) {
		completable.subscribeOn(Schedulers.from(db.getQueryExecutor())).subscribe(errorObserver);
	}

	public void insert(AppItem item) {
		execute(db.appItemDao().insert(item));
	}

	public void update(AppItem item) {
		execute(db.appItemDao().update(item));
	}

	public void delete(AppItem item) {
		execute(db.appItemDao().delete(item));
		Completable.fromAction(() -> AppUtils.deleteApp(item))
				.subscribeOn(Schedulers.io())
				.subscribe(errorObserver);
	}

	public AppItem get(String name, String vendor) {
		return db.appItemDao().get(name, vendor);
	}

	public AppItem get(int id) {
		return db.appItemDao().get(id);
	}

	public void close() {
		if (db != null) {
			db.close();
		}
		compositeDisposable.clear();
	}

	public void setFilter(String filter) {
		if (query.setFilter(filter)) {
			compositeDisposable.add(db.appItemDao().getAllSingle(query)
					.subscribeOn(Schedulers.from(db.getQueryExecutor()))
					.subscribe(this::onAppListLoaded, errorObserver::onError));
		}
	}

	public String getFilter() {
		return query.getFilter();
	}

	public void setSort(int sort) {
		if (query.setSort(sort)) {
			compositeDisposable.add(db.appItemDao().getAllSingle(query)
					.subscribeOn(Schedulers.from(db.getQueryExecutor()))
					.subscribe(this::onAppListLoaded, errorObserver::onError));
		}
	}

	public LiveData<List<AppItem>> getAppList() {
		return appList;
	}

	public void setDatabaseFile(String file) {
		if (db != null) {
			if (file.equals(db.getOpenHelper().getReadableDatabase().getPath())) {
				return;
			}
			close();
		}
		initDb(file);
	}

	private void sortLastMidLet(List<AppItem> appItems){
		if (appItems == null || appItems.size() <= 1) return;
		String path;
		try {
			path = FilesKt.readText(
					new File(Config.getConfigsDir(), Constants.FILE_LAST_MIDLET), Charsets.UTF_8);
		} catch (Exception e) {
			return;
		}
		if (TextUtils.isEmpty(path)) return;
		int index = -1;
		for (int i = 0; i < appItems.size(); i++) {
			// isLast must be updated for every item, not only until the first match
			if (appItems.get(i).setLast(path)) {
				index = i;
			}
		}
		if (index <= 0) return;

		appItems.add(0, appItems.remove(index));
	}

	private void syncWithFilesystem(List<AppItem> list) {
		List<AppItem> items = new ArrayList<>(list);
		List<String> paths = AppUtils.getAppDirectories();
		// incomplete installation must not be added to DB
		paths.remove(".tmp");
		if (paths.isEmpty()) {
			// If db isn't empty
			if (!items.isEmpty()) {
				execute(db.appItemDao().deleteAll());
				AppUtils.removeFromRecentShortcuts(items);
			}
			return;
		}
		for (Iterator<AppItem> it = items.iterator(); it.hasNext() && !paths.isEmpty(); ) {
			AppItem item = it.next();
			if (paths.remove(item.getPath())) {
				it.remove();
			}
		}
		if (items.size() > 0) {
			execute(db.appItemDao().delete(items));
			AppUtils.removeFromRecentShortcuts(items);
		}
		if (paths.size() > 0) {
			execute(db.appItemDao().insert(AppUtils.getApps(paths)));
		}
	}

	static class ErrorObserver implements CompletableObserver {

		@Override
		public void onSubscribe(@NotNull Disposable d) {
		}

		@Override
		public void onComplete() {
		}

		@Override
		public void onError(@NotNull Throwable e) {
			Log.e("AppRepository", e.toString(), e);
		}
	}
}
