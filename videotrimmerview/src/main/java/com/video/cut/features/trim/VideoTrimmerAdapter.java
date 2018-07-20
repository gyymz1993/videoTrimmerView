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

import com.video.cut.R;
import com.video.cut.utils.TrimVideoUtil;

import java.util.ArrayList;
import java.util.List;

public class VideoTrimmerAdapter extends RecyclerView.Adapter {
  private List<Bitmap> mBitmaps = new ArrayList<>();
  private LayoutInflater mInflater;
  private Context context;

  public VideoTrimmerAdapter(Context context) {
    this.context = context;
    this.mInflater = LayoutInflater.from(context);
  }

  public List<Bitmap> getmBitmaps() {
    return mBitmaps;
  }

  public void setmBitmaps(List<Bitmap> mBitmaps) {
    this.mBitmaps = mBitmaps;
  }

  @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new TrimmerViewHolder(mInflater.inflate(R.layout.video_thumb_item_layout, parent, false));
  }

  @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    ((TrimmerViewHolder) holder).thumbImageView.setImageBitmap(mBitmaps.get(position));
  }

  @Override public int getItemCount() {
    return mBitmaps.size();
  }

  public void addBitmaps(Bitmap bitmap) {
    mBitmaps.add(bitmap);
    notifyDataSetChanged();
  }

  private final class TrimmerViewHolder extends RecyclerView.ViewHolder {
    ImageView thumbImageView;

    TrimmerViewHolder(View itemView) {
      super(itemView);
      thumbImageView = itemView.findViewById(R.id.thumb);
      LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) thumbImageView.getLayoutParams();
      layoutParams.width = TrimVideoUtil.VIDEO_FRAMES_WIDTH / TrimVideoUtil.MAX_COUNT_RANGE;
      thumbImageView.setLayoutParams(layoutParams);
    }
  }
}
