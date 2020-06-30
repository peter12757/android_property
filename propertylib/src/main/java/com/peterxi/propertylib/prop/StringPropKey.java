package com.peterxi.propertylib.prop;

public class StringPropKey<E> extends PropConfigurableKey<E> {

    public static <E> StringPropKey<E> wrap(String key, Class<E> type) {
        return new StringPropKey<E>(key, type);
    }
    
    public StringPropKey(String key, Class<E> type) {
        setName(key);
        setType(type);
    }
}
