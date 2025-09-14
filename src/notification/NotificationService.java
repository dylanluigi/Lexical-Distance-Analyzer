package notification;

/**
 * Interface for the event notification service.
 */
public interface NotificationService {
    
    /**
     * Publishes an event to all subscribers.
     * 
     * @param event The event to publish
     */
    void publish(Event event);
    
    /**
     * Subscribes a listener to events of a specific type.
     * 
     * @param eventType The type of events to subscribe to
     * @param listener The listener to notify
     */
    void subscribe(EventType eventType, EventListener listener);
    
    /**
     * Unsubscribes a listener from events of a specific type.
     * 
     * @param eventType The type of events to unsubscribe from
     * @param listener The listener to unsubscribe
     */
    void unsubscribe(EventType eventType, EventListener listener);
    
    /**
     * Interface for event listeners.
     */
    interface EventListener {
        /**
         * Called when an event is published.
         * 
         * @param event The published event
         */
        void onEvent(Event event);
    }
    
    /**
     * Event types.
     */
    enum EventType {
        CORPUS_LOADED,
        DISTANCE_MATRIX_PROGRESS,
        DISTANCE_MATRIX_READY,
        QUERY_ANSWERED,
        GRAPH_DATA_READY,
        RENDER_REQUEST,
        COMPUTATION_ERROR
    }
    
    /**
     * Base interface for all events.
     */
    interface Event {
        /**
         * Gets the type of the event.
         * 
         * @return The event type
         */
        EventType getType();
    }
}