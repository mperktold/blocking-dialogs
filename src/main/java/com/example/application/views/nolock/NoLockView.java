package com.example.application.views.nolock;

import com.example.application.views.MainLayout;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@PageTitle("No Lock")
@Route(value = "no-lock", layout = MainLayout.class)
public class NoLockView extends HorizontalLayout {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public NoLockView() {
        var sayHello = new Button("Say hello", e -> {
            UI ui = UI.getCurrent();
            executor.execute(() -> {
                CompletableFuture<String> nameFuture = askNameAsync(ui);
                String name = nameFuture.join();
                ui.access(() -> Notification.show("Hi, " + name));
            });
        });
        setMargin(true);
        add(sayHello);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        executor.shutdown();
    }

    /**
     * Asks the user's name and returns a CompletableFuture which is completed with the user's
     * input when clicking OK.
     */
    private static CompletableFuture<String> askNameAsync(UI ui) {
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
        ui.access(dialog::open);
        return result;
    }
}
