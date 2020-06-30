package com.peterxi.propertylib.prop;

import android.content.Context;

import com.peterxi.propertylib.prop.base.PropertySet;

public class LocalPropertyManager extends PropertyMapManager {

    public LocalPropertyManager(Context context) {
        super(context);
    }

    @Override
    protected PropertySet map(PropertySet set) {
        if (set instanceof CallingPropertySet.I) {
            return CallingPropertySet.wrap(set);
        }
        return null;
    }
    
}
