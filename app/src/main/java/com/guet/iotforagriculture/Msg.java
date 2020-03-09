package com.guet.iotforagriculture;

import android.media.Image;
import android.os.Message;

public class Msg {

    private String content = null;

    public Msg(String content){
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
