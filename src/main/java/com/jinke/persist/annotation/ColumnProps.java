package com.jinke.persist.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnProps {
    String type();
    boolean notNull() default true;
    String defaultValue() default "";
    String comment() default "";
    String other() default "";
}
