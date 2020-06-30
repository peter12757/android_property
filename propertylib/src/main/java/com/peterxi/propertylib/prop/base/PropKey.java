package com.peterxi.propertylib.prop.base;

import java.lang.reflect.Type;
import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class PropKey<E> implements Parcelable, Comparable<PropKey<E>> {
	
    protected static final String TAG = "PropKey";
    
    private Class<? extends PropertySet> mClass;
    private String mName;
    private String mTitle;
    private String mDesc;
    private Type mType;
    private E[] mValues;
    private String[] mValueTitles;

    public PropKey() {
    }
    
    public PropKey(String title) {
        mTitle = title;
    }
    
    public PropKey(String title, String desc){
        mTitle = title;
        mDesc = desc;
    }
    
    public PropKey(String title, E[] values){
        mTitle = title;
        mValues = values;
    }
    
    public PropKey(String title, E[] values, String[] titles){
        mTitle = title;
        mValues = values;
        mValueTitles = titles;
    }
    
    public PropKey(String title, String desc, E[] values){
        mTitle = title;
        mDesc = desc;
        mValues = values;
    }
    
    public PropKey(String title, String desc, E[] values, String[] titles){
        mTitle = title;
        mDesc = desc;
        mValues = values;
        mValueTitles = titles;
    }
    
    public Class<? extends PropertySet> getClazz() {
        return mClass;
    }
    
    public String getName(){
        return mName;
    }

    public String getTitle(){
        return mTitle;
    }

    public String getDesc(){
        return mDesc == null ? mTitle : mDesc;
    }

    public Type getType(){
        return mType;
    }

    public E valueFromString(String value) {
        return null;
    }

    public String valueToString(E value) {
        return null;
    }

    public E[] getValues() {
        return mValues;
    }
    
    public String getValueTitle(E value) {
        if (mValues == null || mValueTitles == null)
            return null;
        int index = Arrays.asList(mValues).indexOf(value);
        if (index < 0)
            return null;
        return mValueTitles[index];
    }
    
    @Override
    public int compareTo(PropKey<E> another) {
        return mName.compareTo(another.mName);
    }
    
    protected void setClass(Class<? extends PropertySet> clazz) {
        mClass = clazz;
    }

    protected void setName(String name) {
        mName = name;
        if (mTitle == null)
            mTitle = name;
    }

    protected void setType(Type type) {
        mType = type;
    }

    @Override
    public String toString() {
        return mTitle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mClass.getName());
        dest.writeString(mName);
    }
    
    public static final Parcelable.Creator<PropKey<?>> CREATOR = new Parcelable.Creator<PropKey<?>>() {
        @SuppressWarnings("unchecked")
        public PropKey<?> createFromParcel(Parcel source) {
            String clsName = source.readString();
            String keyName = source.readString();
            try {
                Class<? extends PropertySet> clazz = (Class<? extends PropertySet>)
                        Class.forName(clsName, true, getClass().getClassLoader());
                return PropertySetClass.get(clazz).findKey(keyName);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "createFromParcel", e);
                return null;
            }
        }

        @Override
        public PropKey<?>[] newArray(int size) {
            return new PropKey<?>[size];
        }
    };

}
