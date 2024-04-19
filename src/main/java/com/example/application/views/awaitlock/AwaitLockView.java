package com.example.application.views.awaitlock;

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
import java.util.concurrent.locks.Condition;

@PageTitle("Await Lock")
@Route(value = "await-lock", layout = MainLayout.class)
public class AwaitLockView extends HorizontalLayout {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AwaitLockView() {
        var sayHello = new Button("Say hello", e -> {
            UI ui = UI.getCurrent();
            executor.execute(() -> ui.accessSynchronously(() -> {
                String name = askName(ui);
                Notification.show("Hi, " + name);
            }));
        });
        setMargin(true);
        add(sayHello);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        executor.shutdown();
    }

    private static String askName(UI ui) {
        Condition condition = ui.getSession().getLockInstance().newCondition();
        var result = new CompletableFuture<String>();
        var dialog = new Dialog();
        var nameField = new TextField("Name");
        var okButton = new Button("OK", e -> {
            result.complete(nameField.getValue());
            condition.signal();
            dialog.close();
        });
        dialog.add(new H1("What's your name?"), nameField, okButton);
        dialog.addDialogCloseActionListener(e -> {
            result.completeExceptionally(new CancellationException());
            condition.signal();
            dialog.close();
        });
        dialog.open();
        ui.push();
        condition.awaitUninterruptibly();
        return result.join();   // already completed here
    }
}
