//

type RafID = number;
type RafCallback = (time: number) => void;
export class RequestAnimationFrameMock {
    private queue = new Map<RafID, RafCallback>();
    private rafId: RafID = 0;
    private runImmediate = false;

    private generateId() {
        return this.rafId++;
    }

    private top(): RafCallback {
        const iterator = this.queue.values();
        const entry = iterator.next();

        return entry.value;
    }

    private pop(): void {
        const iterator = this.queue.keys();
        const entry = iterator.next();

        this.queue.delete(entry.value);
    }

    private isEmpty(): boolean {
        return this.queue.size === 0;
    }

    private requestAnimationFrame = (callback: RafCallback): RafID => {
        const id = this.generateId();

        if (this.runImmediate) {
            setTimeout(() => {
                callback(window.performance.now());
            }, 0);
        } else {
            this.queue.set(id, callback);
        }

        return id;
    }

    private cancelAnimationFrame = (id: RafID): void => {
        this.queue.delete(id);
    }

    public triggerFrame() {
        if (this.isEmpty()) {
            return;
        }

        const callback = this.top();
        callback(performance.now());
        this.pop();
    }

    public triggerAllPendingFrames() {
        if (this.isEmpty()) {
            return;
        }

        for (const [id, callback] of this.queue.entries()) {
            callback(performance.now());
            this.queue.delete(id);
        }
    }

    public useFakeRaf(): void {
        window.requestAnimationFrame = this.requestAnimationFrame;
        window.cancelAnimationFrame = this.cancelAnimationFrame;
    }

    public reset(): void {
        this.queue.clear();
        this.rafId = 0;
        this.runImmediate = false;
    }

    public triggerAllFrames() {
        this.triggerAllPendingFrames();
        this.runImmediate = true;
    }
}
