package com.video.cut.interfaces;

public interface TrimVideoListener {
    void onStartTrim();

    void onFinishTrim(String url);

    void onCancel();

    void onFailed();
}
