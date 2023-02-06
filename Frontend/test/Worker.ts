import { bind } from '@yandex-int/messenger.decorators';
import EventTargetImpl from './EventTarget';
import { MessagePortImpl } from './MessagePort';

export default class WorkerImpl extends EventTargetImpl {
    public terminated = false;
    public onmessage = null;
    public onmessageerror = null;
    public onerror = null;

    constructor(private port: MessagePort) {
        super();
        (port as MessagePortImpl).setSource(this as any as ServiceWorker);
        port.start();
    }

    public addEventListener(type: string, listener: EventListener) {
        if (type === 'message') {
            this.port.addEventListener('message', listener);
        } else {
            super.addEventListener(type, listener);
        }
    }

    public removeEventListener(type: string, listener: EventListener) {
        if (type === 'message') {
            this.port.removeEventListener('message', listener);
        } else {
            super.removeEventListener(type, listener);
        }
    }

    public terminate() {
        this.terminated = true;
    }

    @bind
    public postMessage(message: any, transfer: any) {
        this.port.postMessage(message, transfer);
    }
}

export function workerFactory() {
    const channel = new MessageChannel();

    return [
        new WorkerImpl(channel.port1),
        new WorkerImpl(channel.port2),
    ];
}
