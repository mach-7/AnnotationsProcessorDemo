package com.manchau.dev.annotationsDemo;
/*
 * Created by maneesh.chauhan on 01/02/2019
 */

public class AppRunner {

	public static void main(String args[]) {
		User tester = new UserBuilder().address("Jaakonkatu 5, Helsinki")
				.email("tester@ekahau.com")
				.phone("+358-4456-65432")
				.name("Tester")
				.build();
		System.out.println(tester);

	}
}
