package com.m3c.md.Ref;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class InstrumentTest {

    @Test
    public void setup() {
        Ric testRic = mock(Ric.class);
        Instrument testInstrument = new Instrument(testRic);
        when(testRic.getRic()).thenReturn("VOD.L");
        Assert.assertEquals(testInstrument.toString(), "VOD.L");
    }

}