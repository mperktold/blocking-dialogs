package com.example.application.views.releaselock;

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
import com.vaadin.flow.server.VaadinSession;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@PageTitle("Release Lock")
@Route(value = "release-lock", layout = MainLayout.class)
public class ReleaseLockView extends HorizontalLayout {

    // For simplicity only! In production, use an application-wide thread pool!
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ReleaseLockView() {
        var sayHello = new Button("Say hello", e -> {
            UI ui = UI.getCurrent();
            // Must use accessSynchronously here instead of access.
            // Otherwise the task could be executed by the event handler thread.
            executor.execute(() -> ui.accessSynchronously(() -> {
                CompletableFuture<String> nameFuture = askNameAsync();
                String name = blockingWait(ui, nameFuture);
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

    /**
     * Asks and waits for the user's name. Assumes to be running an EventListener thread,
     * and that the current VaadinSession is locked.
     */
    private static <T> T blockingWait(UI ui, CompletableFuture<T> future) {
        // Temporarily unlock Session while waiting for user input,
        // so we can immediately show dialog to the user and be ready to process
        // the answer.
        VaadinSession session = ui.getSession();
        var lock = (ReentrantLock)session.getLockInstance();
        int holdCount = lock.getHoldCount();
        for (int i = 0; i < holdCount; i++) {
            session.unlock();
        }
        try {
            return future.join();
        }
        finally {
            // Lock again to restore the previous state.
            for (int i = 0; i < holdCount; i++) {
                session.lock();
            }
        }
    }
}
