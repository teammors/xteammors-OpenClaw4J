package com.xteammors.openclaw.wssdk;

import java.util.ArrayList;
import java.util.List;

public class XMessageManagerSubject implements XMessageSubject {

    //存放订阅者
    private List<XMessageObserver> observers = new ArrayList<XMessageObserver>();

    @Override
    public void addObserver(XMessageObserver obj) {
        observers.add(obj);
    }

    @Override
    public void deleteObserver(XMessageObserver obj) {
        int i = observers.indexOf(obj);
        if (i >= 0) {
            observers.remove(obj);
        }
    }

    @Override
    public void notifyObserver(String receiveMessage) {
        for (int i = 0; i < observers.size(); i++) {
            XMessageObserver o = observers.get(i);
            o.onIMMessage(receiveMessage);
        }
    }

    @Override
    public void notifyErrorObserver(String errorMessage) {
        for (int i = 0; i < observers.size(); i++) {
            XMessageObserver o = observers.get(i);
            o.onIMError(errorMessage);
        }
    }

    public void publish(String message) {
        notifyObserver(message);
    }

    public void publishError(String message) {
        notifyErrorObserver(message);
    }
}

