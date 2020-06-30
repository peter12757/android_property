package com.peterxi.propertylib.prop;

import android.annotation.SuppressLint;
import android.net.Uri;

import com.peterxi.propertylib.prop.base.PropKey;
import com.peterxi.propertylib.prop.base.PropertySet;

/**
 * 
 * @author peter
 *
 */
@SuppressLint("ParcelCreator")
public class UriProperties extends PropertySet {
    
    public final static PropKey<String> PROP__SCHEME_ = new PropKey<String>("协议");

    public final static PropKey<String> PROP__AUTHORITY_ = new PropKey<String>("组织");
    
    public final static PropKey<String> PROP__USER_INFO_ = new PropKey<String>("用户名");
    
    public final static PropKey<String> PROP__HOST_ = new PropKey<String>("主机");
    
    public final static PropKey<Integer> PROP__PORT_ = new PropKey<Integer>("端口");
    
    public final static PropKey<String> PROP__PATH_ = new PropKey<String>("路径");
    
    public final static PropKey<String> PROP__FILE_ = new PropKey<String>("文件");
   
    public static UriProperties wrap(String url) {
        return url == null ? null : new UriProperties(url);
    }
    
    public static UriProperties wrap(Uri uri) {
        return uri == null ? null : new UriProperties(uri);
    }
    
    private Uri mUri;
    
    public UriProperties(String url) {
        this(Uri.parse(url));
    }
    

    public UriProperties(Uri uri) {
        mUri = uri;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <E> E getProp(PropKey<E> key) {
        if (key == PROP__SCHEME_)
            return (E) mUri.getScheme();
        else if (key == PROP__AUTHORITY_)
            return (E) mUri.getAuthority();
        else if (key == PROP__USER_INFO_)
            return (E) mUri.getUserInfo();
        else if (key == PROP__HOST_)
            return (E) mUri.getHost();
        else if (key == PROP__PORT_)
            return (E) (Integer) mUri.getPort();
        else if (key == PROP__PATH_)
            return (E) mUri.getPath();
        else if (key == PROP__FILE_) {
            return (E) mUri.getLastPathSegment();
        } else
            return null;
    }

    @Override
    public String getProp(String key) {
        if (hasKey(key)) {
            return super.getProp(key);
        } else {
            return mUri.getQueryParameter(key);
        }
    }
    
    @Override
    public String toString() {
        return mUri.toString();
    }
    
}
