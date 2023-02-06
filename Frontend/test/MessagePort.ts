import EventTargetImpl from './EventTarget';

export class MessagePortImpl extends EventTargetImpl implements MessagePort {
    public onmessage = null;
    public onmessageerror = null;
    public portToGetter: () => MessagePort;
    public source: MessageEventSource;

    private started = false;

    constructor(portToGetter: () => MessagePort) {
        super();
        this.portToGetter = portToGetter;
    }

    public start() {
        this.started = true;
    }

    public close() {
        this.started = false;
    }

    public postMessage(message: any, transfer?: any) {
        if (!this.started) {
            return;
        }

        const event = new MessageEvent('message', {
            data: message,
            source: this.source,
            ports: transfer?.filter((item) => item instanceof MessagePort) as any,
        });

        global.setTimeout(() => {
            this.portToGetter().dispatchEvent(event);
        }, 0);
    }

    public setSource(source: MessageEventSource) {
        this.source = source;
    }
}

(global as any).MessagePort = MessagePortImpl;
