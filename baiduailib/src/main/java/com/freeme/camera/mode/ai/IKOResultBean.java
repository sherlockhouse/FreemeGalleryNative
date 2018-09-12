package com.freeme.camera.mode.ai;

import java.io.Serializable;

public class IKOResultBean implements Serializable {
    private byte[] mResultByte;
    private IKOBean mResultBean;

    public byte[] getmResultByte() {
        return mResultByte;
    }

    public void setmResultByte(byte[] mResultByte) {
        this.mResultByte = mResultByte;
    }

    public IKOBean getmResultBean() {
        return mResultBean;
    }

    public void setmResultBean(IKOBean mResultBean) {
        this.mResultBean = mResultBean;
    }
}
