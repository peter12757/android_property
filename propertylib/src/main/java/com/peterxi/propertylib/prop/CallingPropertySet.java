package com.peterxi.propertylib.prop;

import java.util.HashMap;
import java.util.Map;

import android.os.Binder;
import android.os.Parcel;
import android.os.Parcelable;

import com.peterxi.propertylib.prop.base.PropertySet;

public class CallingPropertySet extends RespectivePropertySet {
    
    public interface I {
    }

    private static Map<PropertySet, CallingPropertySet> sSets =
            new HashMap<PropertySet, CallingPropertySet>();
    
    public static CallingPropertySet wrap(PropertySet impl) {
        if (impl == null)
            return null;
        CallingPropertySet set = sSets.get(impl);
        if (set == null) {
            set = new CallingPropertySet(impl);
            sSets.put(impl, set);
        }
        return set;
    }
    
    public static PropertySet unwrap(PropertySet set) {
        if (set instanceof CallingPropertySet)
            return RespectivePropertySet.unwrap(set);
        return set;
    }

    public static void clearPid(int pid) {
        for (CallingPropertySet set : sSets.values()) {
            set.clearContext("p" + pid);
        }
    }
    
    private CallingPropertySet(PropertySet impl) {
        super(impl);
    }
    
    @Override
    protected Object[] getContexts() {
        return new Object[] { "p" + Binder.getCallingPid(), "u" + Binder.getCallingUid() };
    }
    
    public CallingPropertySet(Parcel source) {
        super(source);
    }

    public static final Parcelable.Creator<CallingPropertySet> CREATOR = 
            PropertySetCreator.get(CallingPropertySet.class);

}

