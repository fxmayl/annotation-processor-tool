package com.my.processor;

/**
 * @author
 * @Description:
 * @date 2021/8/24 10:06
 */
public class IdAlreadyUsedException extends RuntimeException  {

    private FactoryAnnotatedClass factoryAnnotatedClass;

    public IdAlreadyUsedException(FactoryAnnotatedClass factoryAnnotatedClass) {
        super();
        this.factoryAnnotatedClass = factoryAnnotatedClass;
    }

    public FactoryAnnotatedClass getFactoryAnnotatedClass() {
        return factoryAnnotatedClass;
    }

    public void setFactoryAnnotatedClass(FactoryAnnotatedClass factoryAnnotatedClass) {
        this.factoryAnnotatedClass = factoryAnnotatedClass;
    }
}
