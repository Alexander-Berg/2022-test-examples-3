import { Socket as netSocket } from 'net';
import url from 'url';
import { IncomingMessage } from 'http';
import WebSocket from 'ws';

import { MessageType } from '../../src/types/message';
import { parseMessage } from '../lib/message';
import messageHandlers from '../message-handlers';

export type CheckoutServiceClient = Map<string, { merchant?: WebSocket; eventManager?: WebSocket }>;

const webSocket = new WebSocket.Server({ noServer: true });

const clients: CheckoutServiceClient = new Map();

function onWebSocketMessage(ws: WebSocket, message: WebSocket.Data) {
    if (typeof message !== 'string') return;

    try {
        messageHandlers(clients, ws, parseMessage(message));
    } catch (e) {
        // Невалидный форммат сообщения
    }
}

function onWebSocketClose(ws: WebSocket) {
    for (let [pageId, { merchant, eventManager }] of clients) {
        if (merchant === ws) {
            messageHandlers(clients, ws, {
                type: MessageType.MerchantClose,
                pageId,
            });
            return;
        } else if (eventManager === ws) {
            messageHandlers(clients, ws, {
                type: MessageType.EventManagerClose,
                pageId,
            });
            return;
        }
    }
}

webSocket.on('connection', ws => {
    ws.on('message', message => onWebSocketMessage(ws, message));

    ws.on('close', () => onWebSocketClose(ws));
});

webSocket.on('close', () => clients.clear());

export default (req: IncomingMessage, socket: netSocket, head: Buffer) => {
    const pathname = req.url ? url.parse(req.url).pathname : '';

    if (pathname === '/ws') {
        webSocket.handleUpgrade(req, socket, head, ws => {
            webSocket.emit('connection', ws, req);
        });
    } else {
        socket.destroy();
    }
};
