package com.test.rest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sergey
 */
@XmlRootElement
public class Person {
    private String firstName;
    private String lastName;
    private int age;
    private UsAddress address;

    public Person() {
    }

    public Person(String firstName, String lastName, int age, UsAddress address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.address = address;
    }
    
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public UsAddress getAddress() {
        return address;
    }

    public void setAddress(UsAddress address) {
        this.address = address;
    }
}
