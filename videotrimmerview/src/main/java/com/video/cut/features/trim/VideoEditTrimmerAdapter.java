package com.video.cut.features.trim;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.video.cut.R;
import com.video.cut.utils.DeviceUtil;
import com.video.cut.utils.TrimVideoUtil;
import com.video.cut.utils.UnitConverter;

import java.util.ArrayList;
import java.util.List;

public class VideoEditTrimmerAdapter extends RecyclerView.Adapter<VideoEditTrimmerAdapter.TrimmerViewHolder> {

    //设置三种不同Item类型,分别是头部,item,尾部
    public static final int ITME_TYPE_HEADER = 1;
    public static final int ITME_TYPE_CONTENT = 2;
    public static final int ITME_TYPE_BOTTOM = 3;
    private List<Bitmap> mBitmaps = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context context;
    private View VIEW_FOOTER;
    private View VIEW_HEADER;

    public VideoEditTrimmerAdapter(Context context) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
    }

    public List<Bitmap> getmBitmaps() {
        return mBitmaps;
    }

    public void setmBitmaps(List<Bitmap> mBitmaps) {
        this.mBitmaps = mBitmaps;
    }

    @NonNull
    @Override
    public TrimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (ITME_TYPE_HEADER == viewType) {
            view = getHeadView();
        } else if (ITME_TYPE_BOTTOM == viewType) {
            view = getFootView();
        } else {
            view = mInflater.inflate(R.layout.video_thumb_item_layout, parent, false);

        }
        return new TrimmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrimmerViewHolder holder, int position) {
        if (!isHeaderView(position) && !isFooterView(position)) {
            if (haveHeaderView()) position--;
            holder.thumbImageView.setImageBitmap(mBitmaps.get(position));
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.thumbImageView.getLayoutParams();
            layoutParams.width = (int) Math.ceil(DeviceUtil.getDeviceWidth() / TrimVideoUtil.MAX_COUNT_RANGE);
            holder.thumbImageView.setLayoutParams(layoutParams);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderView(position)) {
            return ITME_TYPE_HEADER;
        } else if (isFooterView(position)) {
            return ITME_TYPE_BOTTOM;
        } else {
            return ITME_TYPE_CONTENT;
        }
    }

    @Override
    public int getItemCount() {
        int count = (mBitmaps == null ? 0 : mBitmaps.size());
        if (VIEW_FOOTER != null) {
            count++;
        }
        if (VIEW_HEADER != null) {
            count++;
        }
        return count;
    }

    public void addBitmaps(Bitmap bitmap) {
        mBitmaps.add(bitmap);
        notifyDataSetChanged();
    }

    private boolean haveHeaderView() {
        return VIEW_HEADER != null;
    }

    public boolean haveFooterView() {
        return VIEW_FOOTER != null;
    }

    private boolean isHeaderView(int position) {
        return haveHeaderView() && position == 0;
    }

    private boolean isFooterView(int position) {
        return haveFooterView() && position == getItemCount() - 1;
    }

    public void addHeaderView() {
        if (haveHeaderView()) {
            throw new IllegalStateException("hearview has already exists!");
        } else {
            getHeadView();
            notifyItemInserted(0);
        }

    }

    public void addFooterView() {
        if (haveFooterView()) {
            throw new IllegalStateException("footerView has already exists!");
        } else {
            getFootView();
            notifyItemInserted(getItemCount() - 1);
        }
    }

    protected View getFootView() {
        VIEW_FOOTER = LayoutInflater.from(context).inflate(R.layout.video_edit_item_place, null);
        TextView textView = VIEW_FOOTER.findViewById(R.id.id_ry_place);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) textView.getLayoutParams();
        layoutParams.width = UnitConverter.getDisplayMetrics().widthPixels / 2 -  UnitConverter.dpToPx(35);
        textView.setLayoutParams(layoutParams);
        return VIEW_FOOTER;
    }

    protected View getHeadView() {
        VIEW_HEADER = LayoutInflater.from(context).inflate(R.layout.video_edit_item_place, null);
        TextView textView = VIEW_HEADER.findViewById(R.id.id_ry_place);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) textView.getLayoutParams();
        layoutParams.width = UnitConverter.getDisplayMetrics().widthPixels / 2 - UnitConverter.dpToPx(35);
        textView.setLayoutParams(layoutParams);
        return VIEW_HEADER;
    }

    public class TrimmerViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbImageView;

        TrimmerViewHolder(View itemView) {
            super(itemView);
            thumbImageView = itemView.findViewById(R.id.thumb);
        }
    }

}
