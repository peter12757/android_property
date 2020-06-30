package com.peterxi.propertylib.prop;

import android.os.Parcel;
import android.os.Parcelable;

import com.peterxi.propertylib.prop.base.PropKey;
import com.peterxi.propertylib.prop.base.PropMutableKey;

/*
 * Configurable prop can be modify by user interface, it is also mutable.   
 */
public class PropConfigurableKey<E> extends PropMutableKey<E> {
	
    public PropConfigurableKey() {
    }
    
    public PropConfigurableKey(String title) {
        super(title);
    }
    
    public PropConfigurableKey(String title, String desc){
        super(title, desc);
    }
    
    public PropConfigurableKey(String title, E[] values){
        super(title, values);
    }
    
    public PropConfigurableKey(String title, String desc, E[] values){
        super(title, desc, values);
    }
    
    public static final Parcelable.Creator<PropConfigurableKey<?>> CREATOR = new Parcelable.Creator<PropConfigurableKey<?>>() {
        public PropConfigurableKey<?> createFromParcel(Parcel source) {
            return (PropConfigurableKey<?>) PropKey.CREATOR.createFromParcel(source);
        }

        @Override
        public PropConfigurableKey<?>[] newArray(int size) {
            return new PropConfigurableKey<?>[size];
        }
    };

}
