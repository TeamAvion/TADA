package org.teamavion.app;

import sun.misc.Unsafe;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Support class for the Kotlin support class: doing things that can't be done in KT
 * ;)
 */
public class Magic {
    public static final class Nothing<T>{ public final T instance; private Nothing(T instance){ this.instance = instance; }}
    public static <T> Nothing<T> getBlankInstance(Class<T> c){
        try {
            Constructor<T> c1 = c.getDeclaredConstructor();
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            ((Unsafe)f.get(null))
                    .putBoolean(
                            c1,
                            ((Unsafe)f.get(null))
                                    .objectFieldOffset(AccessibleObject.class.getDeclaredField("override")),
                            true
                    );
            return new Nothing<>(c1.newInstance());
        } catch (Exception e) {
            throw new RuntimeException("Can't get Unsafe!", e);
        }
    }
}
