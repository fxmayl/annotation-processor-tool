package com.my.test;

/**
 * @author
 * @Description: 测试
 * @date 2021/8/25 15:17
 */
public class TestMain {
    public static void main(String[] args) {
        ShapeFactory shapeFactory = new ShapeFactory();
        Shape triangle = shapeFactory.create("triangle");
        triangle.draw();
    }
}
