package com.linart.tvpreview;

public final class CrestNameIndexCheck {
    public static void main(String[] args) {
        CrestNameIndex<Object> index = new CrestNameIndex<>();
        Object first = new Object(), reserves = new Object();
        index.add("Śląsk Wrocław", first);
        index.add("Śląsk II Wrocław", reserves);
        check(index.find(" SLASK-WROCLAW ") == first);
        check(index.find("slask ii wroclaw") == reserves);
        check(index.find("Polonia") == null);
        index.add("Śląsk Wrocław", first);
        check(index.find("Śląsk Wrocław") == first);
        index.add("Śląsk Wrocław", new Object());
        check(index.find("Śląsk Wrocław") == null);
        index.add("Śląsk Wrocław", first);
        check(index.find("Śląsk Wrocław") == null);
        check(index.find(null) == null);
        System.out.println("Crest name checks: 7 passed");
    }
    private static void check(boolean valid) {
        if (!valid) throw new AssertionError("Unsafe club name association");
    }
}
