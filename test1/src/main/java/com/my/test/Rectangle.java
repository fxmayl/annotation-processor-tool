package com.my.test;


import com.my.processor.Factory;

/**
 * @author
 * @Description:
 * @date 2021/8/23 19:42
 */
@Factory(type = Shape.class, id = "rectangle")
public class Rectangle implements Shape {
    @Override
    public void draw() {
        System.out.println("Draw a Rectangle");
    }
}
