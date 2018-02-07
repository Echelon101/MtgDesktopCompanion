package org.utils.patterns.observer;

import java.util.ArrayList;
import java.util.List;

public class Observable {
    private boolean changed = false;
    private List<Observer> obs;

    public Observable() {
        obs = new ArrayList<>();
    }

    public synchronized void addObserver(Observer o) {
        if (o == null)
            throw new NullPointerException();
        if (!obs.contains(o)) {
            obs.add(o);
        }
    }

    public void notifyObservers(Object arg) {
        Object[] arrLocal;
        synchronized (this) {
            if (!changed)
                return;
            arrLocal = obs.toArray();
            clearChanged();
        }

        for (int i = arrLocal.length-1; i>=0; i--)
            ((Observer)arrLocal[i]).update(this, arg);
    }

 
    protected synchronized void setChanged() {
        changed = true;
    }

    
    protected synchronized void clearChanged() {
        changed = false;
    }

    
    public synchronized boolean hasChanged() {
        return changed;
    }

}