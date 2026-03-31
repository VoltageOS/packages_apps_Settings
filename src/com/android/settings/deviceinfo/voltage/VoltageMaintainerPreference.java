package com.android.settings.deviceinfo.voltage;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class VoltageMaintainerPreference extends Preference {

    private Runnable mLongClickListener;

    public VoltageMaintainerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnLongClickListener(Runnable listener) {
        mLongClickListener = listener;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        holder.itemView.setOnLongClickListener(v -> {
            if (mLongClickListener != null) {
                mLongClickListener.run();
                return true;
            }
            return false;
        });
    }
}
