import { WS_SERVER_HOST } from '../config';
import { MessageType } from '../types/message';
import { CheckoutDetailsUpdate, CheckoutState } from '../types/checkout-api';

import { Emitter, eventEmitter } from './event-emitter';

type EventMap = {
    open: Event;
    close: CloseEvent;
    message: CheckoutMessage;
};

export enum CheckoutMessageType {
    Connected = 'connected',
    Disconnected = 'disconnected',
    Event = 'event',
}

type CheckoutConnectedMessage = {
    type: CheckoutMessageType.Connected;
};

type CheckoutDisconnectedMessage = {
    type: CheckoutMessageType.Disconnected;
};
type CheckoutEventMessage = {
    type: CheckoutMessageType.Event;
    data: {
        event?: string;
        checkoutState?: CheckoutState;
        checkoutDetails?: CheckoutDetailsUpdate;
    };
};
export type CheckoutMessage = CheckoutConnectedMessage | CheckoutDisconnectedMessage | CheckoutEventMessage;

export class CheckoutWebSocket {
    private socket: WebSocket | null = null;
    private eventEmitter = eventEmitter<EventMap>();
    private onReady: Promise<void>;
    private onReadyResolve: () => void = () => {};
    private closed = false;

    constructor(private pageId: string) {
        this.onReady = new Promise(resolve => {
            this.onReadyResolve = resolve;
        });
    }

    addEventHandler: Emitter<EventMap>['on'] = (event, handler) => {
        this.eventEmitter.on(event, handler);
    };

    removeEventHandler: Emitter<EventMap>['off'] = (event, handler) => {
        this.eventEmitter.off(event, handler);
    };

    fireEvent: Emitter<EventMap>['emit'] = (event, handler) => {
        this.eventEmitter.emit(event, handler);
    };

    connect() {
        this.closed = false;
        this.socket = new WebSocket(WS_SERVER_HOST);
        this.socket.onopen = this.onOpen.bind(this);
        this.socket.onclose = this.onClose.bind(this);
        this.socket.onmessage = this.onMessage.bind(this);
    }

    close() {
        this.closed = true;
        this.socket?.close();
    }

    send(type: MessageType, data: object) {
        this.onReady.then(() => {
            this.socket?.send(
                JSON.stringify({
                    type,
                    pageId: this.pageId,
                    data,
                })
            );
        });
    }

    private onOpen(e: Event) {
        this.onReadyResolve();

        this.fireEvent('open', e);
    }

    private onClose(e: CloseEvent) {
        this.onReady = new Promise(resolve => {
            this.onReadyResolve = resolve;
        });

        this.fireEvent('close', e);

        // Автоматически переподключаемся раз в секунду при потере соединения
        setTimeout(() => {
            if (!this.closed) {
                this.connect();
            }
        }, 1000);
    }

    private onMessage(e: MessageEvent) {
        this.fireEvent('message', JSON.parse(e.data));
    }
}
