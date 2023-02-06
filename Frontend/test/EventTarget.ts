type EventListener = (event: Event) => void;

export default class EventTargetImpl implements EventTarget {
    private listeners = new Map<string, Set<EventListener>>();

    public addEventListener(type: string, listener: EventListener) {
        if (!this.listeners.has(type)) {
            this.listeners.set(type, new Set());
        }

        this.listeners.get(type).add(listener);
    }

    public dispatchEvent(event: Event) {
        if (!this.listeners.has(event.type)) {
            return false;
        }

        this.listeners.get(event.type).forEach((listener) => {
            listener(event);
        });

        return true;
    }

    public removeEventListener(type: string, listener: EventListener) {
        if (this.listeners.has(type)) {
            this.listeners.get(type).delete(listener);
        }
    }
}
