import WebSocket from 'ws';

import { MessageType, ResponseMessageType } from '../../src/types/message';
import { createMessage } from '../lib/message';
import { CheckoutServiceClient } from '../lib/server-upgrade';

export type EventManagerMessageData = {
    type: MessageType.EventManagerMessage;
    pageId: string;
    data: object;
};

export default (clients: CheckoutServiceClient, ws: WebSocket, message: EventManagerMessageData | object) => {
    if (!('type' in message)) {
        return;
    }
    const { type, pageId, data } = message;

    if (type !== MessageType.EventManagerMessage) {
        return;
    }

    const client = clients.get(pageId);

    client?.merchant?.send(
        createMessage({
            type: ResponseMessageType.Event,
            data,
        })
    );
};
