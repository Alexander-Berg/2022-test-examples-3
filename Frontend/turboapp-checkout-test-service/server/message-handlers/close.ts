import WebSocket from 'ws';

import { MessageType, ResponseMessageType } from '../../src/types/message';
import { createMessage } from '../lib/message';
import { CheckoutServiceClient } from '../lib/server-upgrade';

export type CloseData = {
    type: MessageType.MerchantClose | MessageType.EventManagerClose;
    pageId: string;
};

export default (clients: CheckoutServiceClient, ws: WebSocket, message: CloseData | object) => {
    if (!('type' in message)) {
        return;
    }
    const { type, pageId } = message;

    if (type !== MessageType.EventManagerClose && type !== MessageType.MerchantClose) {
        return;
    }

    const client = clients.get(pageId);
    if (!client) {
        return;
    }

    if (type === MessageType.EventManagerClose) {
        delete client.eventManager;

        client.merchant?.send(
            createMessage({
                type: ResponseMessageType.Disconnected,
            })
        );
    } else {
        delete client.merchant;

        client.eventManager?.send(
            createMessage({
                type: ResponseMessageType.Disconnected,
            })
        );
    }

    if (!client.eventManager && !client.merchant) {
        clients.delete(pageId);
    }
};
