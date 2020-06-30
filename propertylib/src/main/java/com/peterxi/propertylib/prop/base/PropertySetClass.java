package com.peterxi.propertylib.prop.base;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import android.os.Parcelable;
import android.util.Log;

import com.peterxi.propertylib.prop.util.CollectionsUtil;

public class PropertySetClass {

    protected static final String TAG = "PropertySetClass";

    private static Map<Class<? extends PropertySet>, PropertySetClass> sClassMap = 
            new HashMap<Class<? extends PropertySet>, PropertySetClass>();

    static {
        sClassMap.put(PropertySet.class, new RootPropertySetClass());
    }
    
    private Class<? extends PropertySet> mClass;
    private PropertySetClass mParent;
    private PropertySetClass mCreatorClass;
    private int mParentKeyCount;
    private List<PropKey<?>> mKeyList;
    private Map<String, PropKey<?>> mNameKeyMap;
    
    @SuppressWarnings({ "unchecked" })
    PropertySetClass(Class<? extends PropertySet> clazz) {
        mClass = clazz;
        Class<?> superCls = clazz.getSuperclass();
        mParent = get((Class<? extends PropertySet>) superCls);
        mCreatorClass = mParent.mCreatorClass;
        mParentKeyCount += mParent.getKeyCount();
        mKeyList = new ArrayList<PropKey<?>>();
        mNameKeyMap = new TreeMap<String, PropKey<?>>();
        // check if has CREATOR
        Parcelable.Creator<?> creator = null;
        try {
            Field fc = clazz.getDeclaredField("CREATOR");
            creator = (Parcelable.Creator<?>) fc.get(clazz);
            if (creator == null)
                throw new NullPointerException("CREATOR: null, should be inited before static instances");
        } catch (NoSuchFieldException e) {
        } catch (Exception e) {
            Log.w(TAG, "<init> " + clazz.getName(), e);
        }
        if (creator != null) {
            mCreatorClass = this;
        }
        // extract all PROP_* members
        Field[] fields = clazz.getDeclaredFields();
        for (Field f : fields) {
            if (!Modifier.isStatic(f.getModifiers()))
                continue;
            if (!f.getName().startsWith("PROP_"))
                continue;
            ParameterizedType type = (ParameterizedType) f.getGenericType();
            Type type2 = type.getActualTypeArguments()[0];
            String name = f.getName().substring(5);
            //Log.d(TAG, "<init> - " + name);
            PropKey<?> key = null;
            try {
                key = (PropKey<?>) f.get(null);
            } catch (Exception e) {
                Log.d(TAG, "<init> " + clazz.getName(), e);
            }
            if (key != null) {
                key.setClass(clazz);
                key.setName(name);
                key.setType(type2);
                mKeyList.add(key);
                mNameKeyMap.put(name, key);
            }
        }
    }
    
    PropertySetClass(Class<PropertySet> clazz, int unused) {
        mClass = clazz;
        mParent = null;
        mCreatorClass = null;
    }

    public <E> PropKey<E> findKey(String key) {
        key = key.toUpperCase();
        return _findKey(key, this);
    }

    public <E> PropKey<E> findKey(String key, boolean force) {
        key = key.toUpperCase();
        return _findKey(key, force ? this : null);
    }

    protected <E> PropKey<E> _findKey(String key) {
        return _findKey(key, this);
    }

    @SuppressWarnings("unchecked")
    protected <E> PropKey<E> _findKey(String key, PropertySetClass from) {
        PropKey<?> pKey = mNameKeyMap.get(key);
        if (pKey == null)
            pKey = mParent._findKey(key, from);
        return (PropKey<E>) pKey;
    }
    
    public int getKeyIndex(PropKey<?> key) {
        int index = mKeyList.indexOf(key);
        if (index == -1)
            index = mParent.getKeyIndex(key);
        else
            index += mParentKeyCount;
        return index;
    }

    public int getKeyCount() {
        return mParentKeyCount + mKeyList.size();
    }
    
    @SuppressWarnings("unchecked")
    public <E> PropKey<E> getkeyAt(int index) {
        if (index < mParentKeyCount)
            return mParent.getkeyAt(index);
        index -= mParentKeyCount;
        return index < mKeyList.size() ? (PropKey<E>) mKeyList.get(index) : null;
    }

    public boolean hasKey(PropKey<?> key) {
        return mKeyList.contains(key) || mParent.hasKey(key);
    }

    public boolean hasKey(String key) {
        key = key.toUpperCase();
        return _hasKey(key);
    }

    protected boolean _hasKey(String key) {
        return mNameKeyMap.containsKey(key) || mParent._hasKey(key);
    }
    
    public Collection<PropKey<?>> allKeys() {
        return CollectionsUtil.join(mParent.allKeys(), mKeyList);
    }

    public Class<? extends PropertySet> getJavaClass() {
        return mClass;
    }
    
    PropertySetClass getCreatorClass() {
        return mCreatorClass;
    }
    
    public static synchronized PropertySetClass get(Class<? extends PropertySet> clazz) {
        PropertySetClass pclazz = sClassMap.get(clazz);
        if (pclazz == null) {
            pclazz = new PropertySetClass(clazz);
            sClassMap.put(clazz, pclazz);
        }
        return pclazz;
    }

    @Override
    public String toString() {
        return "PropertySetClass{" +
                "mClass=" + mClass +
                ", mParent=" + mParent +
                ", mCreatorClass=" + mCreatorClass +
                ", mParentKeyCount=" + mParentKeyCount +
                ", mKeyList=" + mKeyList +
                ", mNameKeyMap=" + mNameKeyMap +
                '}';
    }
}

class RootPropertySetClass extends PropertySetClass {

    private List<PropKey<?>> mKeyList = new ArrayList<PropKey<?>>();
    
    RootPropertySetClass() {
        super(PropertySet.class, 0);
    }

    @Override
    protected <E> PropKey<E> _findKey(String key, PropertySetClass from) {
        if (from != null) {
            Log.e(TAG, "_findKey missing " + key + " from " + from.getJavaClass(),
                    new Throwable());
        }
        return null;
    }

    @Override
    public int getKeyIndex(PropKey<?> key) {
        return -1;
    }

    @Override
    public int getKeyCount() {
        return 0;
    }

    @Override
    public <E> PropKey<E> getkeyAt(int index) {
        return null;
    }

    @Override
    public boolean hasKey(PropKey<?> key) {
        return false;
    }

    @Override
    protected boolean _hasKey(String key) {
        return false;
    }

    @Override
    public Collection<PropKey<?>> allKeys() {
        return mKeyList;
    }
    
}

