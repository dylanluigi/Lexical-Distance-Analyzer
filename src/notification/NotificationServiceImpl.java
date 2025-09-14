package notification;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the NotificationService interface.
 */
public class NotificationServiceImpl implements NotificationService {
    
    private final Map<EventType, List<EventListener>> listeners = new ConcurrentHashMap<>();
    
    @Override
    public void publish(Event event) {
        List<EventListener> eventListeners = listeners.get(event.getType());
        
        if (eventListeners != null) {
            // Make a copy to avoid concurrent modification issues
            List<EventListener> listenersCopy = new ArrayList<>(eventListeners);
            
            for (EventListener listener : listenersCopy) {
                // If this is a UI listener and we're not on the JavaFX thread, run it there
                if (listener instanceof UINotificationHandler) {
                    Platform.runLater(() -> listener.onEvent(event));
                } else {
                    listener.onEvent(event);
                }
            }
        }
    }
    
    @Override
    public void subscribe(EventType eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }
    
    @Override
    public void unsubscribe(EventType eventType, EventListener listener) {
        if (listeners.containsKey(eventType)) {
            listeners.get(eventType).remove(listener);
        }
    }
    
    /**
     * Marker interface for listeners that should be notified on the JavaFX thread.
     */
    public interface UINotificationHandler extends EventListener {
    }
}