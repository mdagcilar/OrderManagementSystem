package com.m3c.md.OrderManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FillTest {

    @Test
    public void getSize() {
        Fill testFill = new Fill(500, 52.477);
        Assert.assertEquals(500, testFill.getSize());
    }

    @Test
    public void getPrice() {
        Fill testFill = new Fill(500, 52.477);
        Assert.assertEquals(52.477, testFill.getPrice(), 0.005);
    }
}