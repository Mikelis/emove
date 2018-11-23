package com.emove.emoveapplication.adapter;

import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.emove.emoveapplication.R;

import androidx.recyclerview.widget.RecyclerView;


/**
 * Created by mikelis.kaneps on 01.06.2015.
 */
public class Holders {

    public static class TextImageHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final View container;
        public final ImageView image;
        public final TextView text;
        public final CheckBox box;
        private final CompoundButton.OnCheckedChangeListener changeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(buttonView.getTag()==null){
                    return;
                }
                onChecked(isChecked);
            }
        };


        public TextImageHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container);
            image = itemView.findViewById(R.id.image);
            text = itemView.findViewById(R.id.text);
            box = itemView.findViewById(R.id.checkbox);

            if(box!=null){
                box.setOnCheckedChangeListener(changeListener);
            }
            if (text != null) {
                getContainer().setOnClickListener(this);
            } else if (image != null) {
                getContainer().setOnClickListener(this);
            }

        }

        public View getContainer() {
            if (container != null) {
                return container;
            } else return null;
        }


        @Override
        public void onClick(View v) {

        }
        public void onChecked(boolean isChecked) {
        }

    }


}