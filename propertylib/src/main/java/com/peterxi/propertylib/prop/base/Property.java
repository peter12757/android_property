package com.peterxi.propertylib.prop.base;

import com.peterxi.propertylib.prop.PropConfigurableKey;

public class Property<E> {

    private PropertySet mOwner;
    private PropKey<E> mKey;
    
    public Property(PropertySet owner, PropKey<E> key) {
        mOwner = owner;
        mKey = key;
    }
    
    public E get() {
        return mOwner.getProp(mKey);
    }
    
    public void set(E v) {
        if (mKey instanceof PropConfigurableKey<?>)
            mOwner.setProp((PropConfigurableKey<E>) mKey, v);
        else if (mKey instanceof PropMutableKey<?>)
            mOwner.setProp((PropMutableKey<E>) mKey, v);
        else
            mOwner.setProp(mKey, v);
    }
    
    @Override
    public String toString() {
        return mKey.getTitle();
    }

    public PropKey<E> getKey() {
        return mKey;
    }

    public E[] getValues() {
        return getValues(mOwner, mKey);
    }

    public static <E> E[] getValues(PropertySet owner, PropKey<E> key) {
        E[] values = owner.getPropValues(key);
        if (values != null) {
            return values;
        }
        values = key.getValues();
        if (values != null) {
            return values;
        }
        return PropValue.getValues(key.getType());
    }
    
    public String getValueTitle(E value) {
        return getValueTitle(mOwner, mKey, value);
    }
    
    public static <E> String getValueTitle(PropertySet owner, PropKey<E> key, E value) {
        String desc = owner.getValueTitle(key, value);
        if (desc != null)
            return desc;
        desc = key.getValueTitle(value);
        if (desc != null)
            return desc;
        return PropValue.toString(value, true);
    }

    public E valueFromString(String value) {
        return valueFromString(mOwner, mKey, value);
    }

    public static <E> E valueFromString(PropertySet owner, PropKey<E> key, String value) {
        E v = owner.valueFromString(key, value);
        if (v == null)
            v = key.valueFromString(value);
        if (v == null)
            v = PropValue.fromString(key.getType(), value);
        return v;
    }

    public String valueToString(E value) {
        return valueToString(mOwner, mKey, value);
    }

    public static <E> String valueToString(PropertySet owner, PropKey<E> key, E value) {
        String s = owner.valueToString(key, value);
        if (s == null)
            s = key.valueToString(value);
        if (s == null)
            s = PropValue.toString(value);
        return s;
    }

}
