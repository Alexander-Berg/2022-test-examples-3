import * as net from 'net';
import * as http from 'http';
import * as express from 'express';

export async function startServer(app: express.Application): Promise<[http.Server, string]> {
    const server = http.createServer(app);
    await new Promise((resolve) => {
        server.listen(resolve);
    });

    const port = (server.address() as net.AddressInfo).port;
    const origin = `http://localhost:${port}`;

    return [server, origin];
}

export function stopServer(server: http.Server): Promise<void> {
    return new Promise((resolve, reject) => {
        server.close((err: any) => {
            if (err) {
                reject(err);
            } else {
                resolve();
            }
        });
    });
}
