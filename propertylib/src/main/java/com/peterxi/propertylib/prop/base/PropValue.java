package com.peterxi.propertylib.prop.base;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;


import android.os.BadParcelableException;
import android.os.Build;
import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import android.util.Log;

import com.peterxi.propertylib.prop.util.ClassWrapper;
import com.peterxi.propertylib.prop.util.ObjectWrapper;

public class PropValue implements Parcelable {
	
    public static final int FLAG_EXTERNAL_CLASS = 1 << 8;
    
    private static final String TAG = "PropValue";

    public static <E> PropValue wrap(E value) {
        return wrap(value, 0);
    }
    
    public static <E> PropValue wrap(E value, int flags) {
        return value == null ? null : new PropValue(value, flags);
    }
    
    @SuppressWarnings("unchecked")
    public static <E> E unwrap(PropValue value) {
        return value == null ? null : (E) value.getPropValue();
    }
    
	private Object mPropValue;
	private int mFlags;
	
    public PropValue(Object o, int f) {
        mPropValue = o;
        mFlags = f;
    }
    
	private PropValue(Parcel in) {
		mPropValue = readValueFromParcel(in);
	}
	
	public void setFlags(int flags) {
	    mFlags = flags;
	}

	@Override
	public String toString() {
		return toString(mPropValue);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		writeValueToParcel(dest, mPropValue, flags | mFlags);
	}
	
	private Object getPropValue() {
		return mPropValue;
	}

	private static Map<Class<? extends Parcelable>, Class<? extends Parcelable>> sParcelableMap = 
	        new HashMap<Class<? extends Parcelable>, Class<? extends Parcelable>>();
	
	@SuppressWarnings("unchecked")
    private static Class<? extends Parcelable> getParcelableClass(Class<? extends Parcelable> clazz) {
	    if (PropertySet.class.isAssignableFrom(clazz)) {
            PropertySetClass creatorClass = PropertySetClass.get((Class<? extends PropertySet>) clazz)
	                .getCreatorClass();
            if (creatorClass == null)
                throw new ParcelFormatException(clazz.getName() + ": No CREATOR");
            return creatorClass.getJavaClass();
        }
	    Class<? extends Parcelable> c = sParcelableMap.get(clazz);
        if (c == null) {
            Parcelable.Creator<?> creator = null;
            try {
                Field fc = clazz.getDeclaredField("CREATOR");
                creator = (Parcelable.Creator<?>) fc.get(null);
            } catch (NoSuchFieldException e1) {
            } catch (Exception e1) {
                Log.w(TAG, "getParcelableClass " + clazz.getName(), e1);
            }
            if (creator != null) {
                c = clazz;
            } else {
                Class<?> superCls = clazz.getSuperclass();
                if (Parcelable.class.isAssignableFrom(superCls)) {
                    c = getParcelableClass((Class<? extends Parcelable>) superCls);
                } else {
                    throw new ParcelFormatException(clazz.getName() + ": No CREATOR");
                }
            }
            sParcelableMap.put(clazz, c);
        }
        return c;
	}
	
	// CAUTION: should be compatible to Parcel.writeValue/readValue
    private static final int VAL_CLASSLOADER = -2;
    private static final int VAL_PARCELABLE = -4;
    private static final int VAL_PARCELABLEARRAY = -16;
    private static final int VAL_OBJECTARRAY = -17;
    private static final int VAL_CLASS = -64;
    private static final int VAL_SERIALIZABLE = 21;
	
    @SuppressWarnings("unchecked")
    public static void writeValueToParcel(Parcel dest, Object value, int flags) {
        if ((flags & FLAG_EXTERNAL_CLASS) != 0 && value != null) {
            ClassLoader loader = (value instanceof Class) 
                    ? ((Class<?>) value).getClassLoader() : value.getClass().getClassLoader();
            if (sClassLoaders.containsValue(loader)) {
                String name = null;
                for (Entry<String, ClassLoader> e : sClassLoaders.entrySet()) {
                    if (e.getValue() == loader)
                        name = e.getKey();
                }
                // Log.v(TAG, "writeValueToParcel loader: " + name);
                dest.writeInt(VAL_CLASSLOADER);
                dest.writeString(name);
            }
        }
        if (value instanceof Parcelable) {
            Class<? extends Parcelable> c = getParcelableClass((Class<? extends Parcelable>) value.getClass());
            dest.writeInt(VAL_PARCELABLE);
            // CAUTION: not null, compatible to readParcelable
            dest.writeString(c.getName());
            ((Parcelable) value).writeToParcel(dest, flags);
        } else if (value instanceof Parcelable[]) {
            Class<? extends Parcelable> c = 
                    (Class<? extends Parcelable>) value.getClass().getComponentType();
            c = getParcelableClass(c);
            dest.writeInt(VAL_PARCELABLEARRAY);
            dest.writeString(c.getName());
            // CAUTION: not null, compatible to readParcelableArray
            Parcelable[] value2 = (Parcelable[]) value;
            dest.writeInt(value2.length);
            for (int i = 0; i < value2.length; ++i) {
                if (value2[i] == null) {
                    dest.writeString(null);
                } else {
                    dest.writeString(c.getName());
                    value2[i].writeToParcel(dest, flags);
                }
            }
        } else if (value instanceof Object[]) {
            // This is fixed on android 5.0, but not early platform
            dest.writeInt(VAL_OBJECTARRAY);
            dest.writeString(value.getClass().getComponentType().getName());
            // CAUTION: not null, compatible to readArray, 
            // CAUTION: not fix deep error
            Object[] value2 = (Object[]) value;
            dest.writeInt(value2.length);
            for (int i = 0; i < value2.length; ++i) {
                dest.writeValue(value2[i]);
            }
        } else if (value instanceof Class) {
            dest.writeInt(VAL_CLASS);
            dest.writeString(((Class<?>) value).getName());
        } else {
            dest.writeValue(value);
        }
    }
    
    private static ClassLoader sClassLoader = PropValue.class.getClassLoader();

    private static Map<String, ClassLoader> sClassLoaders = new TreeMap<String, ClassLoader>(); 

    public static void setClassLoaders(Map<String, ClassLoader> loaders) {
        sClassLoaders = loaders;
    }

    public static Object readValueFromParcel(Parcel source) {
        return readValueFromParcel(source, sClassLoader);
    }
    
    public static Object readValueFromParcel(Parcel source, ClassLoader loader) {
        int tag = source.readInt();
        if (tag >= -1) {
            if (tag == VAL_SERIALIZABLE 
                    && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                    && loader != sClassLoader) {
                return readSerializable(source, loader);
            }
            source.setDataPosition(source.dataPosition() - 4);
            Object value = source.readValue(loader);
            // Fix Parcelable array bug
            if (value instanceof Object[]) {
                value = copyArray(source.readString(), value);
            }
            return value;
        } else if (tag == VAL_CLASSLOADER) {
            String name = source.readString();
            loader = sClassLoaders.get(name);
            // Log.v(TAG, "readValueFromParcel loader: " + name);
            return readValueFromParcel(source, loader);
        } else if (tag == VAL_PARCELABLE) {
            return source.readParcelable(loader);
        } else if (tag == VAL_PARCELABLEARRAY) {
            String type = source.readString();
            return copyArray(type, source.readParcelableArray(loader));
        } else if (tag == VAL_OBJECTARRAY) {
            String type = source.readString();
            return copyArray(type, source.readArray(loader));
        } else if (tag == VAL_CLASS) {
            String type = source.readString();
            try {
                return Class.forName(type, true, loader);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "readValueFromParcel", e);
                throw new BadParcelableException("Invalid class name " + type);
            }
        } else {
            throw new ParcelFormatException("Invalid tag " + tag);
        }
    }

    private static Object[] copyArray(String type, Object value) {
        try {
            Class<?> componentType = Class.forName(type, 
                    true, sClassLoader);
            Object[] bad = (Object[]) value;
            Object[] good = (Object[]) Array.newInstance(componentType, bad.length);
            for (int j = 0; j < bad.length; ++j)
                good[j] = bad[j];
            return good;
        } catch (Exception e) {
            Log.w(TAG, "readValueFromParcel", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Parcelable> Parcelable[] copyParcelableArray(E[] array) {
        Class<E> clazz = (Class<E>) array.getClass().getComponentType();
        Parcelable[] copy = array;
        if (PropertySet.class.isAssignableFrom(clazz)) {
            Class<E> clazz2 = (Class<E>) getParcelableClass(clazz); 
            if (clazz2 != clazz) {
                clazz = clazz2;
            }
            copy = (Parcelable[]) Array.newInstance(clazz, array.length);
            for (int i = 0; i < array.length; ++i) {
                if (array[i].getClass() == clazz)
                    copy[i] = array[i];
                else
                    copy[i] = ClassWrapper.wrap(clazz).newInstance(
                            ObjectWrapper.wrap(clazz, array[i]));
            }
        }
        return copy;
    }

    public static String toString(Object value) {
        return toString(value, false);
    }

    public static String toString(Object value, boolean title) {
        if (value == null) {
            return "<null>";
        } else if (value instanceof Enum) {
            return title ? String.valueOf(value) 
                    : ((Enum<?>) value).name();
        } else if (value instanceof Object[]) {
            return Arrays.deepToString((Object[]) value);
        } else if (value.getClass().isArray()) {
            if (value instanceof boolean [])
                return Arrays.toString((boolean []) value);
            else if (value instanceof char [])
                return Arrays.toString((char []) value);
            else if (value instanceof byte [])
                return Arrays.toString((byte []) value);
            else if (value instanceof short [])
                return Arrays.toString((short []) value);
            else if (value instanceof int [])
                return Arrays.toString((int []) value);
            else if (value instanceof long [])
                return Arrays.toString((long []) value);
            else if (value instanceof float [])
                return Arrays.toString((float []) value);
            else if (value instanceof double [])
                return Arrays.toString((double []) value);
            else
                return null;
        } else {
            return String.valueOf(value);
        }
    }

    public static <E> E fromString(Class<E> type, String value) {
        return fromString((Type) type, value);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <E> E fromString(Type type, String value) {
        // Log.v(TAG, "fromString type=" + type + ", value=" + value);
        try {
            if (type instanceof Class) {
                Class<?> clazz = (Class<?>) type;
                if (clazz.isPrimitive()) {
                    // Log.v(TAG, "fromString isPrimitive");
                    return (E) fromStringPrimitive(clazz, value);
                } else if (clazz.isEnum()) {
                    // Log.v(TAG, "fromString isEnum");
                    return (E) Enum.valueOf((Class<Enum>)clazz, value);
                } else if (clazz == String.class) {
                    // Log.v(TAG, "fromString string");
                    return (E) value;
                } else if (clazz.isArray()) {
                    // Log.v(TAG, "fromString isArray");
                    return (E) fromStringArray(clazz.getComponentType(), value);
                } else {
                    // Log.v(TAG, "fromString other");
                    Method m = null;
                    try {
                        m = clazz.getMethod("valueOf", String.class);
                    } catch (Exception e) {
                        // Log.w(TAG, "fromString", e);
                    }
                    if (m != null && Modifier.isStatic(m.getModifiers())) {
                        // Log.v(TAG, "fromString valueOf");
                        return (E) m.invoke(null, value);
                    }
                    Constructor c = null;
                    try {
                        c = clazz.getConstructor(String.class);
                    } catch (Exception e) {
                        // Log.w(TAG, "fromString", e);
                    }
                    if (c != null) {
                        // Log.v(TAG, "fromString constructor");
                        return (E) c.newInstance(value);
                    }
                    Exception e = new InvalidParameterException(
                            "No fromString method for type " + type);;
                    Log.w(TAG, "fromString", e);
                    throw e;
                }
            } else if (type instanceof GenericArrayType) {
                // Log.v(TAG, "fromString GenericArrayType");
                return (E) fromStringArray(((GenericArrayType) type).getGenericComponentType(), value);
            } else if (type instanceof ParameterizedType) {
                // Log.v(TAG, "fromString ParameterizedType");
                return fromString(((ParameterizedType) type).getRawType(), value);
            }
        } catch (Exception e) {
            Log.w(TAG, "fromString", e);
        }
        return null;
    }

    public static Object fromStringPrimitive(Class<?> clazz, String value) {
        // Log.v(TAG, "fromStringPrimitive clazz=" + clazz + ", value=" + value);
        if (clazz == boolean.class)
            return Boolean.valueOf(value);
        else if (clazz == char.class)
            if (value.length() == 1)
                return value.charAt(0);
            else 
                throw new NumberFormatException("Invalid char: \"" + value + "\""); 
        else if (clazz == byte.class)
            return Byte.valueOf(value);
        else if (clazz == short.class)
            return Short.valueOf(value);
        else if (clazz == int.class)
            return Integer.valueOf(value);
        else if (clazz == long.class)
            return Long.valueOf(value);
        else if (clazz == float.class)
            return Float.valueOf(value);
        else if (clazz == double.class)
            return Double.valueOf(value);
        else
            return null;
    }

    public static Object fromStringArray(Type componentType, String value) {
        // Log.v(TAG, "fromStringArray componentType=" + componentType + ", value=" + value);
        if (!value.startsWith("[") || !value.endsWith("]"))
            return null;
        Class<?> clazz = null;
        if ((componentType instanceof Class))
            clazz = (Class<?>) componentType;
        else
            clazz = (Class<?>) ((ParameterizedType) componentType).getRawType();
        value = value.substring(1, value.length() - 1);
        String[] values = value.isEmpty() ? new String[0] : value.split(", ?", -1);
        if (componentType == String.class)
            return values;
        Object pValues = Array.newInstance(clazz, values.length);
        for (int i = 0; i < values.length; ++i) {
            Array.set(pValues, i, fromString(componentType, values[i]));
        }
        return pValues;
    }
    
    public static boolean isInstance(Type type, Object value) {
        if (value == null) {
            return false;
        } else if (type instanceof Class) {
            return ((Class<?>) type).isInstance(value);
        } else if (type instanceof GenericArrayType) {
            return isAssignableFrom(type, value.getClass());
        } else if (type instanceof ParameterizedType) {
            return isAssignableFrom(((ParameterizedType) type).getRawType(), value.getClass());
        } else {
            return false;
        }
    }
    
    public static boolean isAssignableFrom(Type type, Class<?> clazz) {
        if (type instanceof Class) {
            return ((Class<?>) type).isAssignableFrom(clazz);
        } else if (type instanceof GenericArrayType) {
            return clazz.isArray() && isAssignableFrom(
                    ((GenericArrayType) type).getGenericComponentType(), clazz.getComponentType());
        } else if (type instanceof ParameterizedType) {
            return isAssignableFrom(((ParameterizedType) type).getRawType(), clazz);
        } else {
            return false;
        }
    }

    @SuppressWarnings({ "unchecked" })
    public static <E> E[] getValues(Type type) {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isEnum()) {
                return (E[]) clazz.getEnumConstants();
            } else if (clazz == Boolean.class) {
                return (E[]) new Boolean[] { false, true };
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    
    // fix Sdk version <= Android 4.4
    private final static Serializable readSerializable(Parcel source, final ClassLoader loader) {
        // Log.v(TAG, "readSerializable");
        String name = source.readString();
        if (name == null) {
            // For some reason we were unable to read the name of the Serializable (either there
            // is nothing left in the Parcel to read, or the next value wasn't a String), so
            // return null, which indicates that the name wasn't found in the parcel.
            return null;
        }

        byte[] serializedData = ObjectWrapper.wrap(source).invoke("createByteArray");
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
        try {
            ObjectInputStream ois = new ObjectInputStream(bais) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass osClass)
                        throws IOException, ClassNotFoundException {
                    // try the custom classloader if provided
                    if (loader != null) {
                        Class<?> c = Class.forName(osClass.getName(), false, loader);
                        if (c != null) {
                            return c;
                        }
                    }
                    return super.resolveClass(osClass);
                }
            };
            return (Serializable) ois.readObject();
        } catch (IOException ioe) {
            throw new RuntimeException("Parcelable encountered " +
                "IOException reading a Serializable object (name = " + name +
                ")", ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("Parcelable encountered " +
                "ClassNotFoundException reading a Serializable object (name = "
                + name + ")", cnfe);
        }
    }

    public static final Parcelable.Creator<PropValue> CREATOR = new Parcelable.Creator<PropValue>(){

		@Override
		public PropValue createFromParcel(Parcel source) {
			return new PropValue(source);
		}

		@Override
		public PropValue[] newArray(int size) {
			return new PropValue[size];
		}

	};

}
