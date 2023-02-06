import WebSocket from 'ws';

import { MessageType, ResponseMessageType } from '../../src/types/message';
import { createMessage } from '../lib/message';
import { CheckoutServiceClient } from '../lib/server-upgrade';

export type InitData = {
    type: MessageType.MerchantInit | MessageType.EventManagerInit;
    pageId: string;
};

export default (clients: CheckoutServiceClient, ws: WebSocket, message: InitData | object) => {
    if (!('type' in message)) {
        return;
    }
    const { type, pageId } = message;

    if (type !== MessageType.EventManagerInit && type !== MessageType.MerchantInit) {
        return;
    }

    const client = clients.get(pageId) ?? {};
    clients.set(pageId, client);

    if (type === MessageType.EventManagerInit) {
        client.eventManager = ws;
    } else {
        client.merchant = ws;
    }

    if (client.eventManager && client.merchant) {
        client.eventManager.send(
            createMessage({
                type: ResponseMessageType.Connected,
            })
        );
        client.merchant.send(
            createMessage({
                type: ResponseMessageType.Connected,
            })
        );
    }
};
