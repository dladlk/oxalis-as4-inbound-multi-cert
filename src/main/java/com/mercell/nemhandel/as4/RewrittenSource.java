package com.mercell.nemhandel.as4;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks classes, which were copied and modified from the ERST AS4 project.
 * 
 * Sometime the name or package of the rewritten class is changed, so it is useful to see which class was modified.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RewrittenSource {

	/** 
	 * Class name - if not visible as Class
	 */
	String value();
}
