package io.github.lazyimmortal.sesame.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import io.github.lazyimmortal.sesame.R;
import io.github.lazyimmortal.sesame.data.modelFieldExt.common.SelectModelFieldFunc;
import io.github.lazyimmortal.sesame.entity.IdAndName;
import io.github.lazyimmortal.sesame.util.Log;

import java.util.*;

public class ListAdapter extends BaseAdapter {
    private static ListAdapter adapter;

    private static ListDialog.ListType listType;

    public static ListAdapter get(Context c) {
        if (adapter == null) {
            adapter = new ListAdapter(c);
        }
        return adapter;
    }

    public static ListAdapter getClear(Context c) {
        ListAdapter adapter = get(c);
        adapter.findIndex = -1;
        adapter.findWord = null;
        return adapter;
    }

    public static ListAdapter getClear(Context c, ListDialog.ListType listType) {
        if (adapter == null) {
            adapter = new ListAdapter(c);
            viewHolderList = new ArrayList<>();
        }
        ListAdapter.listType = listType;
        adapter.findIndex = -1;
        adapter.findWord = null;
        return adapter;
    }

    Context context;
    List<? extends IdAndName> list;
    SelectModelFieldFunc selectModelFieldFunc;
    int findIndex = -1;
    String findWord = null;

    private ListAdapter(Context c) {
        context = c;
    }

    public void setBaseList(List<? extends IdAndName> l) {
        if (l != list)
            exitFind();
        list = l;
    }

    public void setSelectedList(SelectModelFieldFunc selectModelFieldFunc) {
        this.selectModelFieldFunc = selectModelFieldFunc;
        try {
            Collections.sort(list, (o1, o2) -> {
                if (this.selectModelFieldFunc.contains(o1.id) == this.selectModelFieldFunc.contains(o2.id)) {
                    return o1.compareTo(o2);
                }
                return this.selectModelFieldFunc.contains(o1.id) ? -1 : 1;
            });
        } catch (Throwable t) {
            Log.i("ListAdapter err");
            Log.printStackTrace(t);
        }
    }

    /**
     * 获取当前选中的项列表
     */
    public List<IdAndName> getSelectedItems() {
        List<IdAndName> selectedItems = new ArrayList<>();
        if (list != null && selectModelFieldFunc != null) {
            for (IdAndName item : list) {
                if (selectModelFieldFunc.contains(item.id)) { // 如果该项被选中
                    selectedItems.add(item); // 添加到选中项列表
                }
            }
        }
        return selectedItems;
    }

    public int findLast(String findThis) {
        if (list == null || list.isEmpty()) {
            return -1;
        }
        findThis = findThis.toLowerCase();
        if (!Objects.equals(findThis, findWord)) {
            findIndex = -1;
            findWord = findThis;
        }
        int start = findIndex;
        int last = list.size() - 1;
        if (start < 0) {
            start = 0;
        } else if (start > last) {
            start = last;
        }
        int current = start;
        for (; ; ) {
            current--;
            if (current < 0) {
                current = last;
            }
            if (list.get(current).name.toLowerCase().contains(findThis)) {
                findIndex = current;
                break;
            }
            if (current == start) {
                break;
            }
        }
        notifyDataSetChanged();
        return findIndex;
    }

    public int findNext(String findThis) {
        if (list == null || list.isEmpty()) {
            return -1;
        }
        findThis = findThis.toLowerCase();
        if (!Objects.equals(findThis, findWord)) {
            findIndex = -1;
            findWord = findThis;
        }
        int start = findIndex;
        int last = list.size() - 1;
        if (start < 0) {
            start = 0;
        } else if (start > last) {
            start = last;
        }
        int current = start;
        for (; ; ) {
            current++;
            if (current > last) {
                current = 0;
            }
            if (list.get(current).name.toLowerCase().contains(findThis)) {
                findIndex = current;
                break;
            }
            if (current == start) {
                break;
            }
        }
        notifyDataSetChanged();
        return findIndex;
    }

    public void exitFind() {
        findIndex = -1;
    }

    public void selectAll() {
        selectModelFieldFunc.clear();
        for (IdAndName ai : list) {
            selectModelFieldFunc.add(ai.id, 0);
        }
        notifyDataSetChanged();
    }

    public void SelectInvert() {
        for (IdAndName ai : list) {
            if (!selectModelFieldFunc.contains(ai.id)) {
                selectModelFieldFunc.add(ai.id, 0);
            } else {
                selectModelFieldFunc.remove(ai.id);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public Object getItem(int p1) {
        return list.get(p1);
    }

    @Override
    public long getItemId(int p1) {
        return p1;
    }

    @Override
    public View getView(int p1, View p2, ViewGroup p3) {
        ViewHolder vh;
        if (p2 == null) {
            vh = new ViewHolder();
            p2 = View.inflate(context, R.layout.list_item, null);
            vh.tv = p2.findViewById(R.id.tv_idn);
            vh.cb = p2.findViewById(R.id.cb_list);
            if (listType == ListDialog.ListType.SHOW) {
                vh.cb.setVisibility(View.GONE);
            }
            p2.setTag(vh);
            viewHolderList.add(vh);
        } else {
            vh = (ViewHolder) p2.getTag();
        }

        IdAndName ai = list.get(p1);
        vh.tv.setText(ai.name);
        vh.tv.setTextColor(findIndex == p1 ? Color.RED : Color.BLACK);
        vh.cb.setChecked(selectModelFieldFunc != null && selectModelFieldFunc.contains(ai.id));
        return p2;
    }

    public static List<ViewHolder> viewHolderList;

    public static class ViewHolder {
        TextView tv;
        CheckBox cb;
    }

}
