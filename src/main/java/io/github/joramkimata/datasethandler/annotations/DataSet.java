package io.github.joramkimata.datasethandler.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface DataSet {

    public boolean isSearchable() default true;

    public boolean isSortable() default true;

    public String table() default "";
    
    public String distinctField() default "";

}
