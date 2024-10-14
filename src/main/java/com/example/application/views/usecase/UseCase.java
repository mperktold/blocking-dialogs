package com.example.application.views.usecase;

import static java.util.stream.Collectors.toCollection;

import com.example.application.views.MainLayout;
import com.github.javafaker.Faker;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSelectionModel;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.stream.Stream;

@Route(value = "use-case", layout = MainLayout.class)
public class UseCase extends Composite<VerticalLayout> {

	private final Faker faker = new Faker();
	private final Grid<Person> grid = new Grid<>();
	private GridListDataView<Person> dataView;
	private GridSelectionModel<Person> selectionModel;

	@Override
	protected VerticalLayout initContent() {
		ArrayList<Person> items = Stream.generate(this::fakePerson)
			.limit(20)
			.collect(toCollection(ArrayList::new));
		dataView = grid.setItems(items);
		selectionModel = grid.setSelectionMode(Grid.SelectionMode.SINGLE);
		grid.addColumn(Person::name).setHeader("Name");
		grid.addColumn(Person::birthdate).setHeader("Birthdate");
		var add = new Button("Add", BlockingDialogs.wrapListener(
			e -> addNew()
		));
		var generate = new Button("Generate", BlockingDialogs.wrapListener(
			e -> generateFakePerson()
		));
		var remove = new Button("Remove", BlockingDialogs.wrapListener(
			e -> removeSelected()
		));
		var buttons = new HorizontalLayout(generate, add, remove);
		var layout = new VerticalLayout();
		layout.add(buttons);
		layout.addAndExpand(grid);
		return layout;
	}

	private Person fakePerson() {
		return new Person(
			faker.name().firstName(),
			faker.name().lastName(),
			LocalDate.ofInstant(faker.date().birthday().toInstant(), ZoneId.systemDefault())
		);
	}

	private void addNew() {
		// This is a more complex example showing a "larger" blocking dialog, which in turn executes blocking logic
		// when trying to close it. See BlockingDialogs.saveCancelAsync for the "inner" blocking dialogs.
		var today = LocalDate.now();
		var binder = new Binder<Person>();
		binder.forField(new TextField("First name"))
			.asRequired()
			.bind(Person::firstName, Person::setFirstName);
		binder.forField(new TextField("Last name"))
			.asRequired()
			.bind(Person::lastName, Person::setLastName);
		binder.forField(new DatePicker("Birthdate"))
			.asRequired()
			.withValidator(today::isAfter, "Birthdate must not be in the future")
			.bind(Person::birthdate, Person::setBirthdate);
		Person person = BlockingDialogs.saveCancelBlocking("New person", binder, Person::new);
		dataView.addItem(person);
	}

	private void generateFakePerson() {
		// This is an example where no blocking actually happens. Here we can clearly see that, but in our real
		// application, there are several code paths where we need to assume that blocking could happen, even though it
		// doesn't happen in all cases.
		dataView.addItem(fakePerson());
	}

	private void removeSelected() {
		// This is an example where no blocking actually happens. Here we can clearly see that, but in our real
		// application, there are several code paths where we need to assume that blocking could happen, even though it
		// doesn't happen in all cases.
		selectionModel.getFirstSelectedItem().ifPresent(this::remove);
	}

	private void remove(Person person) {
		String title = "Remove " + person.name();
		String msg = "Do you really want to remove " + person.name() + " from this list?";
		if (BlockingDialogs.yesNoBlocking(title, msg)) {
			dataView.removeItem(person);
			selectionModel.deselect(person);
		}
	}
}
