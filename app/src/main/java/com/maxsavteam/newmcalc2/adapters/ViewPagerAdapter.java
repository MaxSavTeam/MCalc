package com.maxsavteam.newmcalc2.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewHolder> {

    public interface AdapterCallback {
        int getParentHeight();
    }

    private final ArrayList<ViewPagerFragmentFactory> mFactories;
    private final AdapterCallback adapterCallback;

    public ViewPagerAdapter(ArrayList<ViewPagerFragmentFactory> mFactories, AdapterCallback adapterCallback) {
        this.mFactories = mFactories;
        this.adapterCallback = adapterCallback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewPagerFragmentFactory factory = mFactories.get(0);
        for (var f : mFactories) {
            if (f.getType() == viewType) {
                factory = f;
            }
        }
        return new ViewHolder(factory.justCreateView(parent));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        mFactories.get(position).bindView(holder.itemView, adapterCallback.getParentHeight());
    }

    @Override
    public int getItemViewType(int position) {
        return mFactories.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return mFactories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public interface ViewPagerFragmentFactory {

        View justCreateView(ViewGroup parent);

        void bindView(View view, int parentHeight);

        int getType();

    }
}
