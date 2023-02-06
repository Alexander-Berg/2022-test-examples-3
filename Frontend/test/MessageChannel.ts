import { MessagePortImpl } from './MessagePort';

export default class MessageChannelImpl implements MessageChannel {
    public port1: MessagePort;
    public port2: MessagePort;

    constructor() {
        this.port1 = new MessagePortImpl(() => this.port2) as MessagePort;
        this.port2 = new MessagePortImpl(() => this.port1) as MessagePort;
    }
}

(global as any).MessageChannel = MessageChannelImpl;
