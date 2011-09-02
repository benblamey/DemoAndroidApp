package com.ml4d.core;

import junit.framework.TestCase;

public class String2Test extends TestCase {

	public void test1_1() {
		assertTrue(String2.areEqual(null, null));
	}
	
	public void test1_2() {
		assertFalse(String2.areEqual(null, ""));
	}
	
	public void test1_3() {
		assertFalse(String2.areEqual(null, "foo"));
	}
	
	public void test1_4() {
		assertFalse(String2.areEqual(null, "FOO"));
	}
	
	
	public void test2_1() {
		assertFalse(String2.areEqual("", null));
	}
	
	public void test2_2() {
		assertTrue(String2.areEqual("", ""));
	}
	
	public void test2_3() {
		assertFalse(String2.areEqual("", "foo"));
	}
	
	public void test2_4() {
		assertFalse(String2.areEqual("", "FOO"));
	}
	
	
	public void test3_1() {
		assertFalse(String2.areEqual("foo", null));
	}
	
	public void test3_2() {
		assertFalse(String2.areEqual("foo", ""));
	}
	
	public void test3_3() {
		assertTrue(String2.areEqual("foo", "foo"));
	}
	
	public void test3_4() {
		assertFalse(String2.areEqual("foo", "FOO"));
	}
	
	
	public void test4_1() {
		assertFalse(String2.areEqual("FOO", null));
	}
	
	public void test4_2() {
		assertFalse(String2.areEqual("FOO", ""));
	}
	
	public void test4_3() {
		assertFalse(String2.areEqual("FOO", "foo"));
	}
	
	public void test4_4() {
		assertTrue(String2.areEqual("FOO", "FOO"));
	}
}
