package com.example.application.views.usecase;

import java.time.LocalDate;
import java.util.Objects;

public final class Person {

	private String firstName;
	private String lastName;
	private LocalDate birthdate;

	public Person() {}

	public Person(String firstName, String lastName, LocalDate birthdate) {
		checkNameComponent("firstName", firstName);
		checkNameComponent("lastName", lastName);
		checkBirthdate(birthdate);
		this.firstName = firstName;
		this.lastName = lastName;
		this.birthdate = birthdate;
	}

	private static void checkNameComponent(String component, String name) {
		Objects.requireNonNull(name, component + " must not be null");
		if (name.isBlank())
			throw new IllegalStateException(component + " must not be empty or blank");
	}

	private static void checkBirthdate(LocalDate birthdate) {
		Objects.requireNonNull(birthdate, "birthdate must not be null");
		if (birthdate.isAfter(LocalDate.now()))
			throw new IllegalStateException("Birthdate must not be in the future");
	}

	public String firstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String lastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String name() {
		return firstName + " " + lastName;
	}

	public LocalDate birthdate() {
		return birthdate;
	}

	public void setBirthdate(LocalDate birthdate) {
		this.birthdate = birthdate;
	}
}
