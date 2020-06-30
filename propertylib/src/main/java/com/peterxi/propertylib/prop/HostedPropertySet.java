package com.peterxi.propertylib.prop;

import android.os.Parcel;
import android.os.Parcelable;

import com.peterxi.propertylib.prop.base.PropValue;
import com.peterxi.propertylib.prop.base.PropertySet;
import com.peterxi.propertylib.prop.base.PropertySetClass;

public class HostedPropertySet extends PropertySet {

    public HostedPropertySet(PropertySetClass clazz) {
        super(clazz);
    }
    
    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        PropValue.writeValueToParcel(dest, myClass().getJavaClass(),
                parcelableFlags | PropValue.FLAG_EXTERNAL_CLASS);
        super.writeToParcel(dest, parcelableFlags);
    }
    
    @SuppressWarnings("unchecked")
    public HostedPropertySet(Parcel source) {
        this(PropertySetClass.get((Class<? extends PropertySet>) 
                PropValue.readValueFromParcel(source)));
        super.readFromParcel(source);
    }

    @Override
    public String toString() {
        return "HostedPropertySet - " + myClass().getJavaClass().getSimpleName();
    }
    
    public static final Parcelable.Creator<HostedPropertySet> CREATOR = 
            PropertySetCreator.get(HostedPropertySet.class);

}
