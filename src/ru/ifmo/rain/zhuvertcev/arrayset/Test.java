package ru.ifmo.rain.zhuvertcev.arrayset;

import org.junit.Assert;
import ru.ifmo.rain.zhuvertcev.walk.RecursiveWalk;
import ru.ifmo.rain.zhuvertcev.walk.WalkException;

import java.util.*;

public class Test {
    public static void main(String[] args)
    {
        Map<Integer, Integer> map1 = new HashMap<>(1, 2);
        Map<Integer, Integer> map2 = new HashMap<>(3, 4);
        NavigableSet<Map<Integer, Integer>> ns = new ArraySet<>(List.of(map1, map2));
        System.out.println(ns.subSet(map1, map2));
    }
}
