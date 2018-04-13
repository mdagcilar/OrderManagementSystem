package com.m3c.md.Ref;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RicTest {

    @Test
    public void testConstructor() {
        Ric testRic = new Ric("VOD.L");
        Assert.assertEquals(testRic.getRic(), "VOD.L");
    }

}