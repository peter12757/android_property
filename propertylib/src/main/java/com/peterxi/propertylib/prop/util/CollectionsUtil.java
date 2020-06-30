package com.peterxi.propertylib.prop.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

public class CollectionsUtil {
    
    public static <E> Collection<E> join(final Collection<E> c1, final Collection<E> c2) {
        if (c1 == null || c1.isEmpty())
            return c2;
        else if (c2 == null || c2.isEmpty())
            return c1;
        return new AbstractCollection<E>() {
            @Override
            public Iterator<E> iterator() {
                return new JointIterator<E>(c1.iterator(), c2.iterator());
            }
            @Override
            public int size() {
                return c1.size() + c2.size();
            }
        };
    }

    public interface Filter<E> {
        boolean invoke(E e);
    }
    
    public static <E> Collection<E> filter(final Collection<E> c, final Filter<E> f) {
        if (c == null || c.isEmpty())
            return c;
        return new AbstractCollection<E>() {
            private int mSize = -1;
            @Override
            public Iterator<E> iterator() {
                return new FilterIterator<E>(c.iterator(), f);
            }
            @Override
            public int size() {
                if (mSize == -1) {
                    mSize = 0;
                    for (E e : c) {
                        if (f.invoke(e))
                            ++mSize;
                    }
                }
                return mSize;
            }
        };
    }

    public interface BinaryOperation<E> {
        E operate(E e1, E e2);
    }

    /**
     *
     * @param elements
     * @param init nullable, be careful with null
     * @param op the int will be the first e1, nullable, will use
     * @param <E>
     * @return the accumulate result if succeed, init if given elements is null or empty.
     */
    public static <E> E accumulate(@Nullable final Collection<E> elements, @Nullable E init, @NonNull BinaryOperation<E> op) {
        if (null == elements || elements.isEmpty()) {
            return init;
        }
        for (E element : elements) {
            init = op.operate(init, element);
        }
        return init;
    }

    /**
     * Do adding up for all numbers.
     * @param numbers pass null or empty will return 0
     * @param clazz never null, represents the type of numbers
     * @param <E>
     * @return adding-up value if numbers are not empty, 0 otherwise
     */
    @SuppressWarnings("unchecked")
    public static <E extends Number> E addup(@Nullable Collection<E> numbers, @NonNull Class<E> clazz) {
        if (null == clazz) {
            throw new IllegalArgumentException("Can't do adding up operation without given clazz.");
        }
        if (clazz.equals(Byte.class)) {
            return (E) accumulate((Collection<Byte>) numbers, (byte) 0, new BinaryOperation<Byte>() {
                @Override
                public Byte operate(Byte e1, Byte e2) {
                    return (byte) (e1 + e2);
                }
            });
        } else if (clazz.equals(Short.class)) {
            return (E) accumulate((Collection<Short>) numbers, (short) 0, new BinaryOperation<Short>() {
                @Override
                public Short operate(Short e1, Short e2) {
                    return (short) (e1 + e2);
                }
            });
        } else if (clazz.equals(Integer.class)) {
            return (E) accumulate((Collection<Integer>) numbers, 0, new BinaryOperation<Integer>() {
                @Override
                public Integer operate(Integer e1, Integer e2) {
                    return e1 + e2;
                }
            });
        } else if (clazz.equals(Long.class)) {
            return (E) accumulate((Collection<Long>) numbers, 0L, new BinaryOperation<Long>() {
                @Override
                public Long operate(Long e1, Long e2) {
                    return e1 + e2;
                }
            });
        } else if (clazz.equals(Float.class)) {
            return (E) accumulate((Collection<Float>) numbers, 0.0F, new BinaryOperation<Float>() {
                @Override
                public Float operate(Float e1, Float e2) {
                    return e1 + e2;
                }
            });
        } else if (clazz.equals(Double.class)) {
            return (E) accumulate((Collection<Double>) numbers, 0.0D, new BinaryOperation<Double>() {
                @Override
                public Double operate(Double e1, Double e2) {
                    return e1 + e2;
                }
            });
        } else {
            throw new IllegalArgumentException("Can't do adding up operation for type: " + clazz.getCanonicalName());
        }
    }

    private static class JointIterator<E> implements Iterator<E> {
        private Iterator<E> mIter;
        private Iterator<E> mIter2;
        JointIterator(Iterator<E> iter1, Iterator<E> iter2) {
            mIter = iter1;
            mIter2 = iter2;
            if (!mIter.hasNext()) {
                mIter = mIter2;
                mIter2 = null;
            }
        }
        @Override
        public boolean hasNext() {
            return mIter.hasNext();
        }
        @Override
        public E next() {
            E e = mIter.next();
            if (!mIter.hasNext() && mIter2 != null) {
                mIter = mIter2;
                mIter2 = null;
            }
            return e;
        }
        @Override
        public void remove() {
        }
    };
    
    private static class FilterIterator<E> implements Iterator<E> {
        private Iterator<E> mIter;
        private Filter<E> mFilter;
        private E mItem;
        FilterIterator(Iterator<E> iter, Filter<E> flt) {
            mIter = iter;
            mFilter = flt;
            next();
        }
        @Override
        public boolean hasNext() {
            return mFilter != null;
        }
        @Override
        public E next() {
            E item = mItem;
            while (mIter.hasNext()) {
                mItem = mIter.next();
                if (mFilter.invoke(mItem)) {
                    return item;
                }
            }
            mItem = null;
            mFilter = null;
            return item;
        }
        @Override
        public void remove() {
        }
    };
    
}
