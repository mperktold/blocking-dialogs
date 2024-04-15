package com.example.application.views.deadlock;

import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

@PageTitle("Deadlock")
@Route(value = "deadlock", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class DeadlockView extends HorizontalLayout {

    public DeadlockView() {
        var sayHello = new Button("Say hello", e -> {
            CompletableFuture<String> nameFuture = askNameAsync();
            String name = nameFuture.join();
            Notification.show("Hi, " + name);
        });
        setMargin(true);
        add(sayHello);
    }

    /**
     * Asks the user's name and returns a CompletableFuture which is completed with the user's
     * input when clicking OK.
     */
    private static CompletableFuture<String> askNameAsync() {
        var result = new CompletableFuture<String>();
        var dialog = new Dialog();
        var nameField = new TextField("Name");
        var okButton = new Button("OK", e -> {
            result.complete(nameField.getValue());
            dialog.close();
        });
        dialog.add(new H1("What's your name?"), nameField, okButton);
        dialog.addDialogCloseActionListener(e -> {
            result.completeExceptionally(new CancellationException());
            dialog.close();
        });
        dialog.open();
        return result;
    }
}
