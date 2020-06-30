package com.peterxi.propertylib.prop;

import android.util.Log;

import java.lang.reflect.Array;

import com.peterxi.propertylib.prop.base.Property;
import com.peterxi.propertylib.prop.base.PropertySet;
import com.peterxi.propertylib.prop.util.ObjectWrapper;

/**
 * 
 * @author peter
 *
 */
public class ObjectProperties extends PropertySet {
    
    public static ObjectProperties wrap(Object obj) {
        return obj == null ? null : new ObjectProperties(obj);
    }
    
    private ObjectWrapper<?> mObj;
    
    public ObjectProperties(Object obj) {
        mObj = ObjectWrapper.wrap(obj);
    }

    @Override
    public String getProp(String key) {
        if (hasKey(key)) {
            return super.getProp(key);
        }
        int p = key.indexOf(".");
        if (p > 0) {
            Object obj = mObj.get(key.substring(0, p));
            return new ObjectProperties(obj).getProp(key.substring(p + 1));
        } else {
            String suffix = null;
            if (key.endsWith("]")) {
                int n = key.lastIndexOf('[');
                if (n == -1)
                    throw new IllegalArgumentException(key + ": missing \"[\" for array");
                suffix = key.substring(n + 1, key.length() - 1);
                key = key.substring(0, n);
            }
            Class<Object> type = mObj.getType(key);
            if (suffix != null && !type.isArray())
                throw new IllegalStateException(key + "： not array");
            Object obj = mObj.get(key);
            if (suffix != null) {
                int index = -1;
                try {
                    index = Integer.parseInt(suffix);
                } catch (Exception e) {
                }
                if (index == -1) {
                    throw new IllegalArgumentException(key + ": suffix not const int");
                }
                obj = Array.get(obj, index);
            }
            return Property.valueToString(this,
                    new StringPropKey<Object>(key, type), obj);
        }
    }
    
    @Override
    public String toString() {
        return mObj.toString();
    }
    
    //static {
    //    TestObjectProperties.test();
    //}
}

class TestObjectProperties {
    
    public int[] a = new int[] { 1, 2 };
    
    static void test() {
        String a = new ObjectProperties(new TestObjectProperties()).getProp("a[1]");
        Log.d("TestObjectProperties", a);
    }
}
