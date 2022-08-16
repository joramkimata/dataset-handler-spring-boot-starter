package io.github.joramkimata.datasethandler.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface DataSetField {

	public String fieldName() default "";

	public boolean isSearchable() default true;

	public boolean isSortable() default true;
	
	public boolean isEnum() default false;
	
	public boolean isDisplayedOnList() default true;
	
	public boolean isObject() default false;
	
	

}
