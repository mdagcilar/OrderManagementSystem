package com.m3c.md.OrderClient;

public class Mock {
    public static void show(String out) {
        System.out.println(Thread.currentThread().getName() + ":" + out);
    }
}