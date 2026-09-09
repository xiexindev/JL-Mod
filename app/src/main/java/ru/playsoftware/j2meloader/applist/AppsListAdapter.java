/*
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017-2019 Nikita Shakarun
 * Copyright 2019-2026 Yury Kharchenko
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

package ru.playsoftware.j2meloader.applist;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.databinding.ListRowGridJarBinding;
import ru.playsoftware.j2meloader.databinding.ListRowJarBinding;

class AppsListAdapter extends ListAdapter<AppItem, AppsListAdapter.AppViewHolder> {
	static final int LAYOUT_TYPE_LIST = 0;
	static final int LAYOUT_TYPE_GRID = 1;

	private final OnItemClickListener itemClickListener;
	private int layout = LAYOUT_TYPE_LIST;

	AppsListAdapter(OnItemClickListener onItemClickListener) {
		super(new DiffUtil.ItemCallback<>() {
			@Override
			public boolean areItemsTheSame(@NonNull AppItem oldItem, @NonNull AppItem newItem) {
				return oldItem.getId() == newItem.getId();
			}

			@Override
			public boolean areContentsTheSame(@NonNull AppItem oldItem, @NonNull AppItem newItem) {
				return oldItem.getTitle().equals(newItem.getTitle()) &&
						oldItem.getVersion().equals(newItem.getVersion());
			}
		});
		this.itemClickListener = onItemClickListener;
	}

	@Override
	public int getItemViewType(int position) {
		return layout;
	}

	@NonNull
	@Override
	public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		if (viewType == LAYOUT_TYPE_GRID) {
			ListRowGridJarBinding binding = ListRowGridJarBinding.inflate(inflater, parent, false);
			return new AppViewHolder(binding, itemClickListener);
		} else {
			ListRowJarBinding binding = ListRowJarBinding.inflate(inflater, parent, false);
			return new AppListViewHolder(binding, itemClickListener);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
		holder.onBind(getItem(position));
	}

	void setLayout(int layout) {
		this.layout = layout;
	}

	static class AppViewHolder extends RecyclerView.ViewHolder {
		private final ImageView icon;
		private final TextView name;

		private final TextView last;

		AppViewHolder(ListRowGridJarBinding binding, OnItemClickListener itemClickListener) {
			this(binding.getRoot(), binding.listTitle, binding.listImage,binding.listLast, itemClickListener);
		}

		AppViewHolder(View itemView,
					  TextView listTitle,
					  ImageView listImage,
					  TextView lastTitle,
					  OnItemClickListener itemClickListener) {
			super(itemView);

			icon = listImage;
			name = listTitle;
			last = lastTitle;
			itemView.setOnClickListener(v -> {
				AppsListAdapter adapter = (AppsListAdapter) getBindingAdapter();
				if (adapter != null) {
					AppItem item = adapter.getItem(getLayoutPosition());
					itemClickListener.onClick(item);
				}
			});
			itemView.setOnCreateContextMenuListener(itemClickListener);
		}

		void onBind(AppItem item) {
			Drawable icon = Drawable.createFromPath(item.getImagePathExt());
			if (icon != null) {
				icon.setFilterBitmap(false);
				this.icon.setImageDrawable(icon);
			} else {
				this.icon.setImageResource(R.mipmap.ic_launcher);
			}
			name.setText(item.getTitle());
			itemView.setTag(item);
			last.setVisibility(View.GONE);
		}
	}

	static class AppListViewHolder extends AppViewHolder {
		private final TextView author;
		private final TextView version;

		AppListViewHolder(ListRowJarBinding binding, OnItemClickListener itemClickListener) {
			super(binding.getRoot(), binding.listTitle, binding.listImage, binding.listLast, itemClickListener);
			author = binding.listAuthor;
			version = binding.listVersion;
		}

		@Override
		void onBind(AppItem item) {
			super.onBind(item);
			author.setText(item.getAuthor());
			version.setText(item.getVersion());
		}
	}

	interface OnItemClickListener extends View.OnCreateContextMenuListener {
		void onClick(AppItem item);
	}
}
