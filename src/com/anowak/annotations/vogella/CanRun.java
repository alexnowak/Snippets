/*
 * From: http://www.vogella.com/tutorials/JavaAnnotations/article.html
 */
package com.anowak.annotations.vogella;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface CanRun {

}
