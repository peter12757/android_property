package com.peterxi.propertylib.prop;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;

import com.peterxi.propertylib.prop.base.PropertySet;

public abstract class PropertyMapManager {

    protected PropertyManager mManager;
    protected Map<PropertySet, PropertySet> mSets =
            new HashMap<PropertySet, PropertySet>();
    
    protected PropertyMapManager(Context context) {
        mManager = PropertyManager.getInstance(context);
    }

    public void setup(Object context) {
        PropertyManager.PropContext pc = mManager.getContext(context);
        Map<String, PropertySet> props = pc.getProps();
        if (pc == null || props == null)
            return;
        for (Entry<String, PropertySet> prop : props.entrySet()) {
            register(context, prop.getKey(), prop.getValue());
        }
    }
    
    public void setup(Object context, String[] names) {
        PropertyManager.PropContext pc = mManager.getContext(context);
        Map<String, PropertySet> props = pc.getProps();
        if (pc == null || props == null)
            return;
        for (String n : names) {
            PropertySet s = props.get(n);
            if (s != null)
                register(context, n, s);
        }
    }
    
    public synchronized PropertySet register(Object context, String name, PropertySet set) {
        PropertySet mset = mSets.get(set);
        if (mset == null) {
            mset = map(set);
            if (mset != null) {
                mSets.put(set, mset);
            }
        }
        if (mset != null) {
            mManager.register(context, name, mset);
        } else {
            mset = set;
        }
        return mset;

    }

    public PropertySet get(PropertySet set) {
        PropertySet rset = mSets.get(set);
        if (rset == null)
            rset = set;
        return rset;
    }

    protected abstract PropertySet map(PropertySet set);

    @Override
    public String toString() {
        return "PropertyMapManager{" +
                ", mSets=" + mSets +
                '}';
    }
}
