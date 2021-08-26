package com.my.test;


import com.my.processor.Factory;

/**
 * @author
 * @Description:
 * @date 2021/8/23 19:43
 */
@Factory(type = Shape.class, id = "triangle")
public class Triangle implements Shape {
    @Override
    public void draw() {
        System.out.println("Draw a Triangle");
    }
}
