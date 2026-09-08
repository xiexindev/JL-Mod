/*
 * Copyright 2017-2018 Nikita Shakarun
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

package ru.playsoftware.j2meloader;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;
import androidx.preference.PreferenceManager;


import ru.playsoftware.j2meloader.util.Constants;
import ru.playsoftware.j2meloader.util.FileUtils;

public class EmulatorApplication extends Application implements OnSharedPreferenceChangeListener {
	private static EmulatorApplication instance;

	public static EmulatorApplication getInstance() {
		return instance;
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		instance = this;
		if (BuildConfig.DEBUG) {
			MultiDex.install(this);
		}


		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		if (!sp.contains(Constants.PREF_TOOLBAR)) {
			boolean enable = !ViewConfiguration.get(this).hasPermanentMenuKey();
			sp.edit().putBoolean(Constants.PREF_TOOLBAR, enable).apply();
		}
		sp.registerOnSharedPreferenceChangeListener(this);
		setNightMode(sp.getString(Constants.PREF_THEME, null));
		AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
	}

	@NonNull
	public static String getProcessName() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			return Application.getProcessName();
		} else {
			return FileUtils.getText("/proc/self/cmdline").trim();
		}
	}

	void setNightMode(String theme) {
		if (theme == null) {
			theme = getString(R.string.pref_theme_default);
		}
		AppCompatDelegate.setDefaultNightMode(switch (theme) {
			case "light" -> AppCompatDelegate.MODE_NIGHT_NO;
			case "dark" -> AppCompatDelegate.MODE_NIGHT_YES;
			case "auto-battery" -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
			case "auto-time" -> //noinspection deprecation
					AppCompatDelegate.MODE_NIGHT_AUTO_TIME;
			default -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
		});
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (Constants.PREF_THEME.equals(key)) {
			setNightMode(sharedPreferences.getString(Constants.PREF_THEME, null));
		}
	}
}
