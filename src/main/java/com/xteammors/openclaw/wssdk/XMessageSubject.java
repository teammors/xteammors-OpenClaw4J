package com.xteammors.openclaw.wssdk;


public interface XMessageSubject {

    //添加观察者
    void addObserver(XMessageObserver obj);

    //移除观察者
    void deleteObserver(XMessageObserver obj);

    //当主题方法改变时,这个方法被调用,通知所有的观察者
    void notifyObserver(String receiveMessage);

    void notifyErrorObserver(String errorMessage);

}

