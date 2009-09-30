package org.boudnik.better.sql;

/**
 * @author Alexander Boudnik (shr)
 * @since Apr 6, 2008 3:47:06 PM
 */
public interface Data<T> {
    public void set(final T value);

    public T get();

    public Class<? extends OBJ.FIELD> getType();

    public int getMaxLength();

    public boolean isRequired();
}
