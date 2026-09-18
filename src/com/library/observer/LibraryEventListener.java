package com.library.observer;

/**
 * Listener interface for subscribing to library domain events.
 */
public interface LibraryEventListener {
    void onEvent(LibraryEvent event);
}
