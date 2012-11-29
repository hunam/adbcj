package org.adbcj.h2;

import org.adbcj.Field;
import org.adbcj.ResultHandler;
import org.adbcj.Value;

/**
 * In case of an exception this handler will
 * pass that exception to the underlying result handlers {@link ResultHandler#exception(Throwable, Object)}
 *
 * @author roman.stoffel@gamlor.info
 */
public class SafeResultHandlerDecorator<T> implements ResultHandler<T> {
    private final ResultHandler<T> original;


    public SafeResultHandlerDecorator(ResultHandler<T> original) {
        this.original = original;
    }

    public void startFields(T accumulator) {
        try {
            original.startFields(accumulator);
        } catch (Throwable e) {
            original.exception(e, accumulator);
        }
    }

    public void field(Field field, T accumulator) {
        try {
            original.field(field,accumulator);
        } catch (Throwable e) {
            original.exception(e, accumulator);
        }
    }

    public void endFields(T accumulator) {
        try {
            original.endFields(accumulator);
        } catch (Throwable e) {
            original.exception(e, accumulator);
        }
    }

    public void startResults(T accumulator) {
        try {
            original.startResults(accumulator);
        } catch (Throwable e) {
            original.exception(e, accumulator);
        }
    }

    public void startRow(T accumulator) {
        try {
            original.startRow(accumulator);
        } catch (Throwable e) {
            original.exception(e, accumulator);
        }
    }

    public void value(Value value, T accumulator) {
        try {
            original.value(value,accumulator);
        } catch (Throwable e) {
            original.exception(e, accumulator);
        }
    }

    public void endRow(T accumulator) {
        try {
            original.endRow(accumulator);
        } catch (Throwable e) {
            original.exception(e, accumulator);
        }
    }

    public void endResults(T accumulator) {
        try {
            original.endResults(accumulator);
        } catch (Throwable e) {
            original.exception(e, accumulator);
        }
    }

    public void exception(Throwable t, T accumulator) {
        original.exception(t, accumulator);
    }
}
