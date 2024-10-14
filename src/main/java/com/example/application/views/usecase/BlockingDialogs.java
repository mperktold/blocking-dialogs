package com.example.application.views.usecase;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.server.VaadinSession;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class BlockingDialogs {

	private static final ExecutorService executor = Executors.newCachedThreadPool();

	/** Wraps a ComponentEventListener to be executed in a background thread to enable blocking. */
	public static <E extends ComponentEvent<?>> ComponentEventListener<E> wrapListener(ComponentEventListener<E> listener) {
		UI ui = UI.getCurrent();
		return e -> executor.execute(() -> ui.accessSynchronously(() -> listener.onComponentEvent(e)));
	}

	/** Displays a form with all fields in the given Binder and returns a CompletableFuture of the validated bean. */
	public static <T> CompletableFuture<T> saveCancelAsync(String title, Binder<T> binder, Supplier<T> factory) {
		var result = new CompletableFuture<T>();
		var dialog = new Dialog(title);
		dialog.add(new VerticalLayout(binder.getFields()
			.map(Component.class::cast)
			.toArray(Component[]::new)
		));
		dialog.getFooter().add(new Button("Save", wrapListener(e -> {
			T obj = factory.get();
			try {
				binder.writeBean(obj);
				result.complete(obj);
				dialog.close();
			}
			catch (Exception ex) {
				alertBlocking("Cannot save", ex.getMessage());
			}
		})));
		dialog.getFooter().add(new Button("Cancel", wrapListener(e -> {
			if (yesNoBlocking("Discard changes", "Are you sure you want to discard all changes?")) {
				result.cancel(false);
				dialog.close();
			}
		})));
		dialog.addDetachListener(e -> result.cancel(false));
		dialog.open();
		return result;
	}

	/** Displays a form with all fields in the given Binder and returns blocks for the validated bean. */
	public static <T> T saveCancelBlocking(String title, Binder<T> binder, Supplier<T> factory) {
		return blockingWait(saveCancelAsync(title, binder, factory));
	}

	/** Displays a yes-no-message dialog to the user and returns the decision as a CompletableFuture. */
	public static CompletableFuture<Boolean> yesNoAsync(String title, String message) {
		var result = new CompletableFuture<Boolean>();
		var dialog = new ConfirmDialog(title, message,
			"Yes", e -> result.complete(true),
			"No", e -> result.complete(false)
		);
		dialog.open();
		return result;
	}

	/** Displays a yes-no-message dialog to the user and blocks for the decision. */
	public static boolean yesNoBlocking(String title, String message) {
		return blockingWait(yesNoAsync(title, message));
	}

	/** Displays an alert dialog to the user and a CompletableFuture which is completed when the dialog is closed. */
	public static CompletableFuture<Void> alertAsync(String title, String message) {
		var result = new CompletableFuture<Void>();
		var dialog = new ConfirmDialog(title, message,
			"OK", e -> result.complete(null)
		);
		dialog.open();
		return result;
	}

	/** Displays an alert dialog to the user and blocks until the dialog is closed. */
	public static void alertBlocking(String title, String message) {
		blockingWait(alertAsync(title, message));
	}

	/** Releases the Lock on the VaadinSession while blocking for the given CompletableFuture. */
	public static <T> T blockingWait(CompletableFuture<T> future) {
		// Temporarily unlock Session while waiting for user input,
		// so we can immediately show dialog to the user and be ready to process
		// the answer.
		var session = VaadinSession.getCurrent();
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
