package com.example.exception;

public class HelloException extends RuntimeException {

    public String name;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
