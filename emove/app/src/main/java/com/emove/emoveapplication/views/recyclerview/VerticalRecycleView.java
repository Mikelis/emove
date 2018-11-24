package com.emove.emoveapplication.views.recyclerview;

import android.content.Context;
import android.util.AttributeSet;

import com.emove.emoveapplication.R;
import com.emove.emoveapplication.views.decoration.RecycleViewDecoration;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


/**
 * Created by mikelis.kaneps on 01.06.2015.
 */
public class VerticalRecycleView extends RecyclerView {
    private final LinearLayoutManager layoutManager;

    public VerticalRecycleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        layoutManager
                = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);

        this.setLayoutManager(layoutManager);
        this.addItemDecoration(new RecycleViewDecoration(getResources().getDrawable(R.drawable.abc_list_divider_mtrl_alpha)));
    }


}
