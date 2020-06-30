package com.peterxi.propertylib.prop.base;

import android.os.Parcel;
import android.os.Parcelable;

public class PropMutableKey<E> extends PropKey<E> {
	
    public PropMutableKey() {
    }
    
    public PropMutableKey(String title) {
        super(title);
    }
    
    public PropMutableKey(String title, String desc){
        super(title, desc);
    }
    
    public PropMutableKey(String title, E[] values){
        super(title, values);
    }
    
    public PropMutableKey(String title, String desc, E[] values){
        super(title, desc, values);
    }
    
    public static final Parcelable.Creator<PropMutableKey<?>> CREATOR = new Parcelable.Creator<PropMutableKey<?>>() {
        public PropMutableKey<?> createFromParcel(Parcel source) {
            return (PropMutableKey<?>) PropKey.CREATOR.createFromParcel(source);
        }

        @Override
        public PropMutableKey<?>[] newArray(int size) {
            return new PropMutableKey<?>[size];
        }
    };

}
