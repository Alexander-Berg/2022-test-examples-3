import WebSocket from 'ws';

import { MessageType, ResponseMessageType } from '../../src/types/message';
import { createMessage } from '../lib/message';
import { CheckoutServiceClient } from '../lib/server-upgrade';

export type MerchantMessageData = {
    type: MessageType.MerchantMessage;
    pageId: string;
    data: object;
};

export default (clients: CheckoutServiceClient, ws: WebSocket, message: MerchantMessageData | object) => {
    if (!('type' in message)) {
        return;
    }
    const { type, pageId, data } = message;

    if (type !== MessageType.MerchantMessage) {
        return;
    }

    const client = clients.get(pageId);

    client?.eventManager?.send(
        createMessage({
            type: ResponseMessageType.Event,
            data,
        })
    );
};
