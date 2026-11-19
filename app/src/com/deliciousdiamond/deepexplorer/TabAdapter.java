package com.deliciousdiamond.deepexplorer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/** Simple tab switcher list. */
public final class TabAdapter extends BaseAdapter {
    public interface Listener {
        void onSelect(BrowserTab tab);

        void onClose(BrowserTab tab);
    }

    private final Context context;
    private final List<BrowserTab> tabs;
    private final Listener listener;
    private int currentId;

    public TabAdapter(Context context, List<BrowserTab> tabs, int currentId, Listener listener) {
        this.context = context;
        this.tabs = tabs;
        this.currentId = currentId;
        this.listener = listener;
    }

    public void setCurrentId(int currentId) {
        this.currentId = currentId;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return tabs.size();
    }

    @Override
    public BrowserTab getItem(int position) {
        return tabs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return tabs.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = LayoutInflater.from(context).inflate(R.layout.item_tab, parent, false);
        }
        final BrowserTab tab = getItem(position);
        TextView title = row.findViewById(R.id.tab_item_title);
        TextView url = row.findViewById(R.id.tab_item_url);
        ImageView close = row.findViewById(R.id.tab_item_close);

        String label = tab.title == null || tab.title.isEmpty() ? "新标签页" : tab.title;
        title.setText(label);
        title.setTextColor(context.getResources().getColor(tab.id == currentId ? R.color.primary : R.color.on_surface));
        url.setText(tab.url == null || tab.url.isEmpty() ? "新标签页" : tab.url);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSelect(tab);
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClose(tab);
            }
        });
        return row;
    }
}
