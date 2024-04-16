# Blocking Dialogs

This project shows how blocking dialogs can be realized in Vaadin with Java 17+.
It is inspired by [mvysny/vaadin-loom](https://github.com/mvysny/vaadin-loom), which does the same thing but targets Java 21 and relies on virtual threads.

## Motivation

Blocking dialogs don't fit well with the architecture of Vaadin, or arguably with the web in general.
So the best option is probably to avoid them if you can.

However, when you have a Swing application that makes heavy use of blocking dialogs (which are fine in Swing), and you move it to Vaadin,
it can be a lot of work to make all these dialogs async, since you also need to make the callers async, and the callers of those, and so on
(see [What Color is Your Function?](https://journal.stuffwithstuff.com/2015/02/01/what-color-is-your-function/)).

So if you need blocking dialogs in Vaadin, this project shows how some potential solutions could look like.

## The Application

The application consists of four views, each implementing the same use case:
Showing a dialog that asks for your name, waiting for the result in a blocking manner, and displaying the result as a notification.

### [Deadlock](https://github.com/mperktold/blocking-dialogs/blob/main/src/main/java/com/example/application/views/deadlock/DeadlockView.java)

In this version, we just block in the event listener without thinking too much about it. That doesn't work, because it results in a deadlock.
It is not a real solution, but it serves to highlight the problem and as a starting point for other solutions.

So why doesn't this work? There are three problems:
1. We block inside the event handler, which prevents Vaadin from sending a response back to the browser.
2. When we block, we wait for the result of the dialog. But the dialog hasn't been sent to the browser yet at that point.
3. We block while holding the lock on the VaadinSession. So even if the client would send a result to the server, Vaadin couldn't call the corresponding listener because it can't obtain the lock.

### [No Lock](https://github.com/mperktold/blocking-dialogs/blob/main/src/main/java/com/example/application/views/nolock/NoLockView.java)

This is the first actual solution. It relies on @Push being enabled for the application, as do the other solutions below.

Here we use an ExecutorService to shift the work from the event handler to a background thread, which solves problem 1.
For simplicity, we manage the ExecutorService in our component, but in prosuction, that would be a waste of resources.
Instead, you should have a single application-wide ExecutorService to make the most out of it.
In the background thread, we do not hold the lock on the VaadinSession. We must aquire it explicitely for making changes to the UI:

https://github.com/mperktold/blocking-dialogs/blob/1a11d6c329516ed05224d07cd5c1642e9262cd14/src/main/java/com/example/application/views/nolock/NoLockView.java#L30-L32

https://github.com/mperktold/blocking-dialogs/blob/1a11d6c329516ed05224d07cd5c1642e9262cd14/src/main/java/com/example/application/views/nolock/NoLockView.java#L62-L63

Assuming we use `PushMode.AUTOMATIC` (which is the default), this solves both remaining problems:
- With `ui.access(dialog::open)`, we obtain the lock, open the dialog, and release the lock again.
  When releasing the lock, Vaadin will automatically push changes (i.e. the opened dialog) to the client.
- When blocking, we do not hold the lock anymore, so when the user closes the dialog, Vaadin is able to aquire the lock before calling the listener that wakes up the blocked code.

Note that for displaying the notification, we need to aquire the lock again, because we are still in the background thread.

Now, how does this approach scale for more complex use cases?
Suppose we need to react in a more complex way instead of just showing a notification.
For example, let's say we need to keep track of all the names that we have greated so far. If we've already seen a name, we ask the user if we really should show another greeting.
We might be tempted to try blocking again. After all, we are still in the background thread, so it should work, right?

Well, not if you do it like this:

```java
private final Set<String> namesSeen = new HashSet<>();

    // inside listener
    executor.execute(() -> {
        CompletableFuture<String> nameFuture = askNameAsync(ui);
        String name = nameFuture.join();
        ui.access(() -> handleNameInComplexWay(name));
    });

void handleNameInComplexWay(String name) {
    if (knownNamesSet.add(name) || askGreetSeenName(name)) {
        Notification.show("Hi, " + name);
    }
}

/** Asks user whether the given name should be greated even though it has been seen already. */
boolean askGreetSeenName(String name) { ... }
```

The problem is that `handleNameInComplexWay` is called while holding the lock. So we're trying to block while holding the lock again, which, as we know, results in a deadlock.
There are two ways to solve that (apart from not blocking at all):

One solution is to use the same technique as we did in the event handler: shifting the execution to another thread and block there.
This way, we are left

```java
void handleNameInComplexWay(UI ui, String name) {
    if (knownNamesSet.add(name)) {
        ui.access(() -> Notification.show("Hi, " + name));
    } else {
        executor.execute(() -> {
            if (askGreetSeenName(name))
                ui.access(() -> Notification.show("Hi, " + name));
        });
    }
}
```

The other (probably better) solution is to push down `ui.access` such that it only contains code that changes the UI.
So instead of wrapping the whole call of `handleNameInComplexWay` in `ui.access`, we would just call `handleNameInComplexWay` directly without acquiring the lock.
The lock is only needed when showing the dialog in `askGreetSeenName`, and for showing the notification:

```java
    // ...
    executor.execute(() -> {
        CompletableFuture<String> nameFuture = askNameAsync(ui);
        String name = nameFuture.join();
        handleNameInComplexWay(ui, name);
    });

void handleNameInComplexWay(UI ui, String name) {
    if (knownNamesSet.add(name) || askGreetSeenName(name)) {
        ui.access(() -> Notification.show("Hi, " + name));
    }
}

boolean askGreetSeenName(UI ui, String name) {
   // ...
   ui.access(dialog::open);
   return result;   
}
```

This is the main drawback of this approach: You can either block or make changes to the UI, but never both.
If you have the lock, you can change the UI, but you cannot block.
If you don't have the lock, you can block, but you cannot change the UI.

### [Release Lock](https://github.com/mperktold/blocking-dialogs/blob/main/src/main/java/com/example/application/views/releaselock/ReleaseLockView.java)

TODO

### [Await Lock](https://github.com/mperktold/blocking-dialogs/blob/main/src/main/java/com/example/application/views/awaitlock/AwaitLockView.java)

TODO
