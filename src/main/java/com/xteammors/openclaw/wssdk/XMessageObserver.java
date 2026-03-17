package com.xteammors.openclaw.wssdk;

public interface XMessageObserver {

    public void onIMMessage(String message);

    public void onIMError(String message);

}
