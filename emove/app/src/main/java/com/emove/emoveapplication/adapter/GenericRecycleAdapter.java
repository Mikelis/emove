package com.emove.emoveapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;



import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;


public abstract class GenericRecycleAdapter<T, K extends Holders.TextImageHolder> extends RecyclerView.Adapter  {
    private final Context context;
    protected List<T> mList = new ArrayList<T>() {
    };

    private int lastPosition;

    public GenericRecycleAdapter(List<T> list, Context context) {
        this.context = context;
        if (list != null) {
            this.mList = list;
        }
    }

    public void refresh(List<T> list) {
        if (list != null) {
            this.mList = list;
        }
        notifyDataSetChanged();
    }

    public T getItem(int position) {
        return mList.get(position);
    }

    public Context getContext() {
        return context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(getLayout(), parent, false);
        return getCustomHolder(v);
    }

    public Holders.TextImageHolder getCustomHolder(View v) {
        return new Holders.TextImageHolder(v) {
            @Override
            public void onClick(View v) {
                int index = this.getAdapterPosition();
                if (indexExists(mList, index)) {
                    onItem(mList.get(index));
                    onPosition(this.getAdapterPosition());
                    onItem(mList.get(index), v);
                }

            }

            @Override
            public void onChecked(boolean isChecked) {
                super.onChecked(isChecked);
                int index = this.getAdapterPosition();
                if (indexExists(mList, index)) {
                    onItemChecked(mList.get(index),isChecked);
                }
            }
        };
    }

    protected void onItemChecked(T t, boolean isChecked) {

    }

    protected void onPosition(int position) {

    }

    public boolean indexExists(final List list, final int index) {
        return index >= 0 && index < list.size();
    }

    protected abstract void onItem(T t);

    protected void onItem(T t, View v) {

    }

    public abstract int getLayout();

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        onSet(mList.get(position), (K) holder);
        onSetPosition((K) holder, position);
        setAnimation(((K) holder).getContainer(), position);
    }

    protected void onSetPosition(K holder, int position) {

    }

    public abstract void onSet(T item, K holder);

    @Override
    public int getItemCount() {
        return mList.size();
    }

    protected void setAnimation(View viewToAnimate, int position)
    {
        if (viewToAnimate == null) {
            return;
        }
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }

    }
}
