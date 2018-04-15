package com.m3c.md.Ref;

import org.junit.Assert;
import org.junit.Test;

public class RicTest {

    private Ric ric = new Ric("VOD.L");;

    @Test
    public void getRic() {
        Assert.assertEquals(ric.getRic(), "VOD.L");
    }
}