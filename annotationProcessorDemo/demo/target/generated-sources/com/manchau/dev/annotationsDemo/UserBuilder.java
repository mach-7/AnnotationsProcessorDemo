package com.manchau.dev.annotationsDemo;

import java.lang.String;

public final class UserBuilder {
  private String email;

  private String address;

  private String name;

  private String phone;

  public UserBuilder email(String email) {
    this.email = email;
    return this;
  }

  public UserBuilder address(String address) {
    this.address = address;
    return this;
  }

  public UserBuilder name(String name) {
    this.name = name;
    return this;
  }

  public UserBuilder phone(String phone) {
    this.phone = phone;
    return this;
  }

  public User build() {
    User user = new User();
    user.setEmail(this.email);
    user.setAddress(this.address);
    user.setName(this.name);
    user.setPhone(this.phone);
    return user;
  }
}
