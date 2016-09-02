package com.zyw.zhaochuan.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zyw.zhaochuan.R;
import com.zyw.zhaochuan.entity.FileListItem;

import java.util.List;

/**
 * Created by zyw on 2016/5/18.
 */
public class FileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements View.OnClickListener,View.OnLongClickListener{
    private List<FileListItem> list;
    Context context;
    public FileListAdapter(Context context,List<FileListItem> list)
    {
        this.list=list;
        this.context =context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //如果是项
            View view = LayoutInflater.from(
                    context).inflate(R.layout.filelist_item, parent,
                    false);
            ItemViewHolder holder = new ItemViewHolder(view);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            return holder;

    }


    public static interface OnRecyclerViewItemClickListener {
        void onItemClick(View view , int pos);
    }

    public static interface OnRecyclerViewItemLongClickListener {
        void onItemLongClick(View view,int pos);
    }
    private OnRecyclerViewItemClickListener mOnItemClickListener = null;
    private OnRecyclerViewItemLongClickListener mOnItemLongClickListener = null;

    public void setOnItemClickListener(OnRecyclerViewItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }
    public void setOnItemLongClickListener(OnRecyclerViewItemLongClickListener listener) {
        this.mOnItemLongClickListener = listener;
    }
    @Override
    public void onClick(View v) {
        if (mOnItemClickListener != null) {
            //注意这里使用getTag方法获取数据
            mOnItemClickListener.onItemClick(v,(int)v.getTag());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mOnItemLongClickListener != null) {
            //注意这里使用getTag方法获取数据
            mOnItemLongClickListener.onItemLongClick(v,(int)v.getTag());
        }
        return true;
    }

    /**
     * 此处给控件赋予内容
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        /**
         * 如果是项才设置项内容
         */
            ItemViewHolder itemViewHolder =(ItemViewHolder)holder;
            itemViewHolder.iconView.setImageBitmap(list.get(position).getIcon());
            itemViewHolder.fileNameView.setText(list.get(position).getFileName());
            itemViewHolder.modifyTimeView.setText(list.get(position).getModifyTime());
            itemViewHolder.fileSizeView.setText(list.get(position).getFileSize());
            //将数据保存在itemView的Tag中，以便点击时进行获取
            holder.itemView.setTag(position);

    }

    @Override
    public int getItemCount() {
        return list.size() == 0 ? 0 : list.size() ;
    }


    /**
     * 项的holder
     */
    public class ItemViewHolder extends RecyclerView.ViewHolder
    {
        public ImageView iconView;
        public TextView fileNameView;
        public TextView modifyTimeView;
        public  TextView fileSizeView;
        /**
         * 此处来 new 控件的
         * @param view
         */
        public ItemViewHolder(View view)
        {
            super(view);
            iconView=(ImageView) view.findViewById(R.id.filelist_icon);
            fileNameView =(TextView)view.findViewById(R.id.filelist_name);
            modifyTimeView =(TextView)view.findViewById(R.id.filelist_date);
            fileSizeView =(TextView)view.findViewById(R.id.filelist_size);
        }
    }
}