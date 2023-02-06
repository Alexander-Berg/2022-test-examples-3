import {Socket} from 'net';
import http from 'http';

// @ts-ignore
// eslint-disable-next-line import/no-unresolved,import/extensions
import RequestController from '@yandex-market/kadavr/server/RequestController';

export type MockedTransportRequest = {
    method: string;
    path: string;
    headers: {[key: string]: string};
    body: string | Buffer | Uint8Array;
};

export type MockedTransportResponse = {
    statusCode: number;
    data: any;
    body: any;
    headers: Record<any, any>;
};

export default function (
    request: MockedTransportRequest,
): Promise<MockedTransportResponse> {
    return new Promise(resolve => {
        const socket = new Socket({
            allowHalfOpen: true,
            readable: true,
            writable: true,
        });
        const clientRequest = new http.IncomingMessage(socket);
        clientRequest.url = request.path;
        clientRequest.method = request.method;
        if (request.headers) {
            // eslint-disable-next-line guard-for-in
            for (const name in request.headers) {
                if (name !== 'accept-encoding') {
                    clientRequest.headers[name] = request.headers[name];
                }
            }
        }

        const serverResponse = new http.ServerResponse(clientRequest);
        let chunks = '';

        serverResponse.write = function (chunk: any) {
            this.emit('data', chunk);
            return true;
        };
        serverResponse.end = function (chunk: any) {
            if (chunk != null) {
                this.emit('data', chunk);
            }

            this.emit('end');

            return serverResponse;
        };

        serverResponse.assignSocket(
            new Socket({allowHalfOpen: true, readable: true, writable: true}),
        );

        serverResponse.on('data', (chunk: string) => {
            chunks += chunk;
        });
        serverResponse.on('end', () => {
            const body = chunks.length > 0 ? chunks : null;

            resolve({
                statusCode: serverResponse.statusCode,
                body,
                data: body,
                headers: serverResponse.getHeaders(),
            });
        });

        // eslint-disable-next-line no-new
        new RequestController(clientRequest, serverResponse);

        if (clientRequest.method === 'POST') {
            if (request.body) {
                if (
                    typeof request.body === 'string' ||
                    request.body instanceof Buffer ||
                    request.body instanceof Uint8Array
                ) {
                    clientRequest.push(request.body);
                } else {
                    clientRequest.push(JSON.stringify(request.body));
                }
            }
        }

        clientRequest.push(null);
    });
}
