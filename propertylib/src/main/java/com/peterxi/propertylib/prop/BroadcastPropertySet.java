package com.peterxi.propertylib.prop;

import android.annotation.SuppressLint;

import java.util.HashSet;
import java.util.Set;

import com.peterxi.propertylib.prop.base.PropKey;
import com.peterxi.propertylib.prop.base.PropMutableKey;
import com.peterxi.propertylib.prop.base.PropertySet;

@SuppressLint("ParcelCreator")
public class BroadcastPropertySet extends ProxyPropertySet {
    
    private static final String TAG = "BroadcastPropertySet";

    public static BroadcastPropertySet wrap(PropertySet set) {
        if (set == null)
            return null;
        return new BroadcastPropertySet(set);
    }

    public static PropertySet unwrap(PropertySet set) {
        if (set instanceof BroadcastPropertySet)
            return ((BroadcastPropertySet) set).getImpl();
        return set;
    }

    private Set<PropertySet> mSets = new HashSet<PropertySet>();

    private BroadcastPropertySet(PropertySet impl) {
        super(impl);
    }
    
    public void add(PropertySet set) {
        if (set instanceof RemotePropertySet)
            ((RemotePropertySet) set).attach(this);
        mSets.add(set);
    }

    public <E> void setProp(PropMutableKey<E> key, E value) {
        super.setProp(key, value);
        for (PropertySet set : mSets) {
            set.setProp(key, value);
        }
    }
    
    @Override
    public <E> E getProp(PropKey<E> key) {
        return super.getProp(key);
    }
    
    @Override
    public <E> void setProp(String key, E value) {
        super.setProp(key, value);
        for (PropertySet set : mSets) {
            set.setProp(key, value);
        }
    }
    
    @Override
    public void setProp(String key, String value) {
        super.setProp(key, value);
        for (PropertySet set : mSets) {
            set.setProp(key, value);
        }
    }
    
    @Override
    public void assign(PropertySet other) {
        super.assign(other);
        for (PropertySet set : mSets) {
            set.assign(other);
        }
    }
    
    @Override
    public String toString() {
        return TAG+" mSets: "+ mSets + " - " + super.toString();
    }

}

