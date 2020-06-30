package com.peterxi.propertylib.prop;

import com.peterxi.propertylib.prop.base.PropValue;

public interface IRemotePropertyManager {

    void setPropValue(String context, String set, String key,
            PropValue propValue);

    PropValue getPropValue(String context, String set, String key);

    void syncProps(String context, String set, PropValue props /* PropertySet */);
    
}
