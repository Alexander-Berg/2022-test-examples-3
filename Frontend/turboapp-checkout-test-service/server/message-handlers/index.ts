import WebSocket from 'ws';

import { CheckoutServiceClient } from '../lib/server-upgrade';

import initHandler from './init';
import closeHandler from './close';
import merchantMessageHandler from './merchant-message';
import eventManagerMessageHandler from './event-manager-message';

const handlers = [initHandler, closeHandler, merchantMessageHandler, eventManagerMessageHandler];

export default (clients: CheckoutServiceClient, ws: WebSocket, data: object) => {
    handlers.forEach(handler => handler(clients, ws, data));
};
