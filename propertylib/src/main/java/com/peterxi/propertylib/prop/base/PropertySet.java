package com.peterxi.propertylib.prop.base;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import com.peterxi.propertylib.prop.PropConfigurableKey;
import com.pptv.base.debug.Dumpable;
import com.pptv.base.debug.Dumpper;
import com.pptv.base.debug.Log;
import com.pptv.base.util.data.Collections;
import com.pptv.base.util.reflect.ClassWrapper;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class PropertySet implements Parcelable, Externalizable, Dumpable {

    private static final long serialVersionUID = -370004061720351487L;
    
    private static final String TAG = "PropertySet";
        
    private PropertySetClass mClass = PropertySetClass.get(getClass());
    
    private Map<PropKey<?>, Object> mProps = new TreeMap<PropKey<?>, Object>();

    private PropertySet mSuperSet;

    public PropertySet() {
    }

    public PropertySet(PropertySet o) {
        assign(o);
    }

    protected PropertySet(PropertySetClass clazz) {
        mClass = clazz;
    }
    
    public void setSuperSet(PropertySet set) {
        synchronized (mProps) {
            if (set == this || 
                    (set != null && set.hasSuperSet(this)))
                throw new IllegalArgumentException("super set cycle"); 
            mSuperSet = set;
        }
    }

    public PropertySet getSuperSet() {
        synchronized (mProps) {
            return mSuperSet;
        }
    }

    public PropertySet clearSuperSet() {
        synchronized (mProps) {
            PropertySet set = mSuperSet;
            mSuperSet = null;
            return set;
        }
    }
    
    public boolean hasSuperSet(PropertySet set) {
        synchronized (mProps) {
            return mSuperSet == set
                    || (mSuperSet != null && mSuperSet.hasSuperSet(set));
        }
    }

    public void clearProps() {
        synchronized (mProps) {
            mProps.clear();
        }
    }
    
    public boolean isEmpty() {
        synchronized (mProps) {
            return mProps.isEmpty();
        }
    }

    public boolean hasProp(PropKey<?> key) {
        synchronized (mProps) {
            return hasPropSelf(key)
                    || (mSuperSet != null && mSuperSet.hasProp(key));
        }
    }
    
    public boolean hasPropSelf(PropKey<?> key) {
        synchronized (mProps) {
            return mProps.containsKey(key);
        }
    }

    public <E> E getProp(PropKey<E> key) {
        synchronized (mProps) {
            E val = (E) getPropSelf(key);
            if (val == null && mSuperSet != null)
                val = mSuperSet.getProp(key);
            return val;
        }
    }
    
    @SuppressWarnings("unchecked")
    public <E> E getPropSelf(PropKey<E> key) {
        synchronized (mProps) {
            return (E) mProps.get(key);
        }
    }

    public int getPropSizeSelf() {
        return mProps.size();
    }

    public <E> E getProp(PropKey<E> key, E def) {
        E val = getProp(key);
        return val == null ? def : val;
    }

    public <E> E getPropSelf(PropKey<E> key, E def) {
        E val = getPropSelf(key);
        return val == null ? def : val;
    }

    /*
     * Only child class can modify all props
     */
    protected <E> void setProp(PropKey<E> key, E value) {
        synchronized (mProps) {
            if (value == null)
                mProps.remove(key);
            else
                mProps.put(key, value);
        }
    }
    
    /*
     * Only mutable props can be modified by out world
     */
    public <E> void setProp(PropMutableKey<E> key, E value) {
        setProp((PropKey<E>) key, value);
    }

    /*
     * Configurable props may be handled differently, 
     * we leave child class a chance
     */
    public <E> void setProp(PropConfigurableKey<E> key, E value) {
        setProp((PropMutableKey<E>) key, value);
    }

    public <E> Property<E> getProperty(PropKey<E> key) {
        return new Property<E>(this, key);
    }

    public boolean hasProp(String key) {
        PropKey<?> pKey = findKey(key);
        if (pKey == null)
            return false;
        return hasProp(pKey);
    }
    
    @SuppressWarnings("unchecked")
    public <E> E getProp(String key, E def) {
        PropKey<E> pKey = findKey(key, false);
        if (pKey == null) {
            if (def == null || (def instanceof String))
                return (E) getProp(key, (String) def);
            return null;
        }
        return getProp(pKey, def);
    }

    public String getProp(String key, String def) {
        String value = getProp(key);
        return value == null ? def : value;
    }

    /*
     * Get prop value by string name
     */
    public String getProp(String key) {
        PropKey<Object> pKey = findKey(key);
        if (pKey == null)
            return null;
        Object value = getProp(pKey);
        if (value == null)
            return null;
        return Property.valueToString(this, pKey, value);
    }
    
    public <E> void setProp(String key, E value) {
        if (!hasKey(key)) {
            if (value == null || (value instanceof String))
                setProp(key, (String) value);
            return;
        }
        PropMutableKey<E> pKey = findMutableKey(key);
        if (pKey == null)
            return;
        if (value != null 
                && !PropValue.isInstance(pKey.getType(), value)) {
            String msg = "setProp: value type " + value.getClass() 
                    + " is not compatible to key type " + pKey.getType();
            if (value instanceof String)
                value = Property.valueFromString(this, pKey, (String) value);
            else
                throw new IllegalArgumentException(msg);
        }
        if (pKey instanceof PropConfigurableKey)
            setProp((PropConfigurableKey<E>) pKey, value);
        else
            setProp(pKey, value);
    }
    
    /*
     * Set prop by string name, only for configurable props
     */
    public void setProp(String key, String value) {
        PropConfigurableKey<Object> pKey = findConfigurableKey(key);
        if (pKey == null)
            return;
        setProp(pKey, value == null ? null
                : Property.valueFromString(this, pKey, value));
    }
    
    @SuppressWarnings("unchecked")
    public <E> Property<E> getProperty(String key) {
        return new Property<E>(this, (PropKey<E>) mClass.findKey(key));
    }

    public void applyProperties(Properties props) {
        for (Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            Object value = (String) entry.getValue();
            setProp(key, value);
        }
    }
    
    public void applyProperties(Map<String, String> props) {
        for (Entry<String, String> entry : props.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            setProp(key, value);
        }
    };

    @SuppressWarnings("unchecked")
    public void storeProperties(Map<String, String> props) {
        synchronized (mProps) {
            for (Entry<PropKey<?>, Object> entry : mProps.entrySet()) {
                PropKey<Object> pKey = (PropKey<Object>) entry.getKey();
                if (!(pKey instanceof PropConfigurableKey))
                    continue;
                String key = pKey.getName();
                String value = Property.valueToString(this, pKey, entry.getValue());
                props.put(key, value);
            }
        }
    };

    public <E> E valueFromString(PropKey<E> key, String value) {
        return null;
    }

    public <E> String valueToString(PropKey<E> key, E value) {
        return null;
    }
    
    public void assign(PropertySet other) {
        if (other == this) // avoid clear self
            return;
        synchronized (mProps) {
            mProps.clear();
            apply(other);
        }
    }

    public <E> void assign(PropertySet other, PropKey<E> key) {
        if (other == this) // avoid clear self
            return;
        synchronized (mProps) {
            mProps.remove(key);
            apply(other, key);
        }
    }

    /*
     * Merge with other, other one has higher priority
     */
    public void apply(PropertySet other) {
        apply(other, 0);
    }
    
    public void apply(PropertySet other, int depth) {
        applyMerge(other, true, depth);
    }
    
    public <E> void apply(PropertySet other, PropKey<E> key) {
        apply(other, key, 0);
    }
    
    public void applySimple(PropertySet other) {
        apply(other, -1);
    }

    public <E> void apply(PropertySet other, PropKey<E> key, int depth) {
        applyMerge(other, key, true, depth);
    }
    
    public <E> void apply(PropMutableKey<E> key, E value) {
        apply(key, value, 0);
    }
    
    public <E> void apply(PropMutableKey<E> key, E value, int depth) {
        applyMerge(key, value, true, depth);
    }
    
    /*
     * Merge with other, this one has higher priority
     */
    public void merge(PropertySet other) {
        merge(other, 0);
    }

    public void mergeSimple(PropertySet other) {
        merge(other, -1);
    }

    public void merge(PropertySet other, int depth) {
        applyMerge(other, false, depth);
    }

    public <E> void merge(PropertySet other, PropKey<E> key) {
        merge(other, key, 0);
    }
    
    public <E> void merge(PropertySet other, PropKey<E> key, int depth) {
        applyMerge(other, key, false, depth);
    }
    
    public <E> void merge(PropMutableKey<E> key, E value) {
        merge(key, value, 0);
    }
    
    public <E> void merge(PropMutableKey<E> key, E value, int depth) {
        applyMerge(key, value, false, depth);
    }
    
    /*
     * Merge with other
     * Merge vector, collection
     * Merge map by keys
     */
    @SuppressWarnings("unchecked")
    static <E> E applyMerge(E vt, E vo, boolean replace, int depth) {
        if (vt == null) {
            return vo;
        } else if (vo == null) {
            return vt;
        } else if (vt instanceof PropertySet) {
            PropertySet vm;
            try {
                vm = ((PropertySet) vt).getClass().newInstance();
                vm.applyMerge((PropertySet) vt, replace, depth);
                vm.applyMerge((PropertySet) vo, replace, depth);
                return (E) vm;
            } catch (Exception e) {
                return null;
            }
        } else if (vt instanceof Bundle) {
            Bundle bt = (Bundle) vt;
            Bundle bo = (Bundle) vo;
            for (String key : bo.keySet()) {
                Object value = applyMerge(bt.get(key), bo.get(key), replace, depth - 1);
                if (value instanceof Serializable) {
                    bt.putSerializable(key, (Serializable) value);
                }
            }
            return vt;
        } else if (depth > 0) {
            if (vt instanceof Object[]) {
                Object[] at = (Object[])vt;
                Object[] ao = (Object[])vo;
                Object[] am = Arrays.copyOf(at, at.length + ao.length);
                System.arraycopy(ao, 0, am, at.length, ao.length);
                return (E) am;
            } else if (vt instanceof Map<?, ?>) {
                Map<Object, Object> mt = (Map<Object, Object>) vt;
                Map<Object, Object> mo = (Map<Object, Object>) vo;
                for (Entry<Object, Object> entry : mo.entrySet()) {
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    mt.put(key, applyMerge(mt.get(key), value, replace, depth - 1));
                }
                return vt;
            } else if (vt instanceof Collection<?>) {
                Collection<Object> lt = (Collection<Object>) vt;
                Collection<Object> lo = (Collection<Object>) vo;
                lt.addAll(lo);
                return vt;
            } else if (replace) {
                return vo;
            } else {
                return vt;
            }
        } else if (replace) {
            return vo;
        } else {
            return vt;
        }
    }

    public <E> void applyMerge(PropertySet other, PropKey<E> key, boolean replace, int depth) {
        synchronized (mProps) {
            E value = applyMerge(getProp(key), other.getProp(key), replace, depth);
            setProp(key, value);
        }
    }
    
    public <E> void applyMerge(PropKey<E> key, E value, boolean replace, int depth) {
        synchronized (mProps) {
            value = applyMerge(getProp(key), value, replace, depth);
            setProp(key, value);
        }
    }
    
    public void applyMerge(PropertySet other, boolean replace, int depth) {
        if (this == other)
            return;
        synchronized (mProps) {
            synchronized (other.mProps) {
                for (Entry<PropKey<?>, Object> entry : other.mProps.entrySet()) {
                    PropKey<?> key = entry.getKey();
                    if (!hasKey(key)) {
                        Log.w(TAG, "applyMerge: ignore unknown prop " + key.getName());
                        continue;
                    }
                    Object value = entry.getValue();
                    if (depth >= 0) {
                        mProps.put(key, applyMerge(mProps.get(key), value, replace, depth));
                    }  if (value instanceof PropertySet){
                        value = applyMerge(mProps.get(key), value, replace, depth);
                        mProps.put(key, value);
                    }else if ((!mProps.containsKey(key) || replace) && value != null) { // simple
                        // only simple type
                        Class<?> clazz = value.getClass();
                        if (clazz.getPackage() == sJavaLangPackage || clazz.isEnum())
                            mProps.put(key, value);
                    }
                }
            }
        }
    }

    public <E> E[] getPropValues(PropKey<E> key) {
        return null;
    }
    
    public <E> String getValueTitle(PropKey<E> key, E value) {
        return null;
    }

    @Override
    public String toString() {
        synchronized (mProps) {
            return getClass().getSimpleName() + " (size = " + mProps.size() + ")";
        }
    }
    
    protected String toString(PropKey<String> title) {
        return toString(getProp(title));
    }
    
    protected String toString(String title) {
        synchronized (mProps) {
            return getClass().getSimpleName() + " (" + title 
                    +  " size = " + mProps.size() + ")";
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertySet)) {
            return false;
        }
        synchronized (mProps) {
            Map<PropKey<?>, Object> op = ((PropertySet) o).mProps;
            synchronized (op) {
                return mProps.equals(op);
            }
        }
    }

    @Override
    public PropertySet clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            PropertySet clone = (PropertySet) super.clone();
            clone.assign(this);
            // child will do more things
            return clone;
        }
        Class<? extends PropertySet> javaClass = 
                myClass().getJavaClass();
        ClassWrapper<? extends PropertySet> wrapClass = 
                ClassWrapper.wrap(javaClass);
        if (wrapClass.hasConstructor(javaClass)) {
            return wrapClass.newInstance(this);
        } else if (wrapClass.hasConstructor()) {
            PropertySet newInstance = wrapClass.newInstance();
            newInstance.assign(this);
            return newInstance;
        } else
            return (PropertySet) super.clone(); // throw
    }
    
    public String deepToString() {
        synchronized (mProps) {
            return mProps.toString();
        }
    }
    
    private static final Package sJavaLangPackage = Object.class.getPackage();
    
    public void toStringMap(String prefix, Map<String, String> map) {
        synchronized (mProps) {
            if (mSuperSet != null)
                mSuperSet.toStringMap(prefix, map);
            toStringMapSelf(prefix, map);
        }
    }

    @SuppressWarnings("unchecked")
    public void toStringMapSelf(String prefix, Map<String, String> map) {
        synchronized (mProps) {
            for (Map.Entry<PropKey<?>, Object> e : mProps.entrySet()) {
                Object value = e.getValue();
                if (value instanceof PropertySet) {
                    ((PropertySet) value).toStringMap(prefix, map);
                    continue;
                }
                // only simple type
                Class<?> clazz = value.getClass();
                if (clazz.getPackage() == sJavaLangPackage || clazz.isEnum())
                    map.put(prefix + e.getKey().getName(), 
                            Property.valueToString(this, (PropKey<Object>) e.getKey(), value));
            }
        }
    }
    
    private String _toString() {
        synchronized (mProps) {
            return "PropertySet (size = " + mProps.size() + ")";
        }
    }

    @Override
    public void dump(Dumpper dumpper) {
        _dump(dumpper);
    }

    private void _dump(Dumpper dumpper) {
        synchronized (mProps) {
            if (mSuperSet != null)
                dumpper.dump("mSuperSet", mSuperSet);
            dumpProps(dumpper);
        }
    }

    public void dumpProps(Dumpper dumpper) {
        for (Entry<PropKey<?>, Object> entry : mProps.entrySet()) {
            PropKey<?> key = entry.getKey();
            Object value = entry.getValue();
            String title = String.format("%-16s", key == null ? "<null>" : key.getName());
            dumpper.dump(title, value);
        }
    }

    public void dumpProperties(Dumpper dumpper) {
        dumpper.dump("Properties", new Dumpable() {
            @Override
            public String toString() {
                return PropertySet.this._toString();
            }
            @Override
            public void dump(Dumpper dumpper) {
                PropertySet.this._dump(dumpper);
            }
        });
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        synchronized (mProps) {
            out.writeInt(mProps.size());
            for (Entry<PropKey<?>, Object> entry : mProps.entrySet()) {
                PropKey<?> key = entry.getKey();
                Object value = entry.getValue();
                out.writeObject(key.getName());
                Class<?> type = value.getClass();
                Class<?> compType = type.getComponentType();
                if (compType != null) {
                    type = compType;
                }
                if (Serializable.class.isAssignableFrom(type))
                    out.writeObject(value);
                else
                    out.writeObject(null);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        for (int i = 0; i < size; ++i) {
            String key = (String) in.readObject();
            Object value = in.readObject();
            if (value != null)
                mProps.put(mClass._findKey(key), value);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        synchronized (mProps) {
            PropertySetClass creator = mClass.getCreatorClass();
            dest.writeInt(mProps.size());
            for (Entry<PropKey<?>, Object> entry : mProps.entrySet()) {
                PropKey<?> key = entry.getKey();
                Object value = entry.getValue();
                if (!creator.hasKey(key)) {
                    Log.w(TAG, "writeToParcel: ignore unknown prop " + key.getName());
                    // write null key to match count
                    dest.writeString(null);
                    continue;
                }
                dest.writeString(key.getName());
                PropValue.writeValueToParcel(dest, value, parcelableFlags);
            }
        }
    }
    
    protected void readFromParcel(Parcel source) {
        int size = source.readInt();
        for (int i = 0; i < size; ++i) {
            String key = source.readString();
            if (key == null) continue;
            Object value = PropValue.readValueFromParcel(source);
            mProps.put(mClass._findKey(key), value);
        }
    }
    
    public PropertySetClass myClass() {
        return mClass;
    }
    
    public Collection<PropKey<?>> allKeys() {
        return mClass.allKeys();
    }
    
    public Collection<PropKey<?>> allMutableKeys() {
        return Collections.filter(allKeys(), new Collections.Filter<PropKey<?>>() {
            @Override
            public boolean invoke(PropKey<?> e) {
                return e instanceof PropMutableKey;
            }
        });
    }
    
    public Collection<PropKey<?>> allConfigurableKeys() {
        return Collections.filter(allKeys(), new Collections.Filter<PropKey<?>>() {
            @Override
            public boolean invoke(PropKey<?> e) {
                return e instanceof PropConfigurableKey;
            }
        });
    }
    
    public boolean hasKey(PropKey<?> key) {
        return mClass.hasKey(key);
    }

    public boolean hasKey(String key) {
        return mClass.hasKey(key);
    }

    /**
     * 找到name所对应的值，否则应该返回null
     * 
     * @param name
     * @return
     */
    public <E> PropKey<E> findKey(String key, boolean force) {
        return mClass.findKey(key, force);
    }

    public <E> PropMutableKey<E> findMutableKey(String key, boolean force) {
        PropKey<E> pKey = findKey(key, force);
        if (pKey != null && !(pKey instanceof PropMutableKey<?>)) {
            if (force)
                Log.e(TAG, "findKey not mutable " + key, new Throwable());
            pKey = null;
        }
        return (PropMutableKey<E>) pKey;
    }

    public <E> PropConfigurableKey<E> findConfigurableKey(String key, boolean force) {
        PropKey<E> pKey = findKey(key, force);
        if (pKey != null && !(pKey instanceof PropMutableKey<?>)) {
            Log.e(TAG, "findKey not configable " + key, new Throwable());
            pKey = null;
        }
        return (PropConfigurableKey<E>) pKey;
    }

    public <E> PropKey<E> findKey(String key) {
        return findKey(key, true);
    }

    public <E> PropMutableKey<E> findMutableKey(String key) {
        return findMutableKey(key, true);
    }

    public <E> PropConfigurableKey<E> findConfigurableKey(String key) {
        return findConfigurableKey(key, true);
    }

    protected static int getKeyIndex(Class<? extends PropertySet> clazz, PropKey<?> key) {
        PropertySetClass pclazz = PropertySetClass.get(clazz);
        return pclazz.getKeyIndex(key);
    }

    protected static PropKey<?> getKeyAt(Class<? extends PropertySet> clazz, int index) {
        PropertySetClass pclazz = PropertySetClass.get(clazz);
        return pclazz.getkeyAt(index);
    }

    @Override
    public int describeContents() {
        return 0;
    }
    
    protected static class PropertySetCreator<E extends PropertySet> implements Parcelable.Creator<E> {
        
        private Class<E> mClass;
        
        PropertySetCreator(Class<E> clazz) {
            mClass = clazz;
        }
        
        public E createFromParcel(Parcel in) {
            ClassWrapper<E> clazz = ClassWrapper.wrap(mClass);
            if (clazz.hasConstructor(Parcel.class))
                return clazz.newInstance(in);
            E p  = clazz.newInstance();
            p.readFromParcel(in);
            return p;
        }

        @SuppressWarnings("unchecked")
        public E[] newArray(int size) {
            return (E[]) Array.newInstance(mClass, size);
        }
        
        public static <E extends PropertySet> PropertySetCreator<E> get(Class<E> clazz) {
            return new PropertySetCreator<E>(clazz);
        }
    }

}

class Test extends PropertySet {
    
    public static PropKey<String> PROP_NAME = new PropKey<String>("Name");
    public static PropKey<Integer> PROP_ID = new PropKey<Integer>("Id");

    private static List<PropKey<?>> sKeys = new ArrayList<PropKey<?>>();

    static {
        sKeys.add(PROP_NAME);
        sKeys.add(PROP_ID);
    }

    Test() {
        setProp(PROP_NAME, "");
        setProp(PROP_ID, 0);

        getProp(new PropKey<String>("Name"));
    }

    public static final Parcelable.Creator<Test> CREATOR = new Parcelable.Creator<Test>() {
        public Test createFromParcel(Parcel source) {
            Test p = new Test();
            p.readFromParcel(source);
            return p;
        }

        @Override
        public Test[] newArray(int size) {
            return null;
        }
    };

}
