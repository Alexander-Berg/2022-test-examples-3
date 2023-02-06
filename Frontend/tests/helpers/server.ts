import { Server } from 'http';
import next from 'next';

import config from '@yandex-int/yandex-cfg';

import server from 'src/server/express';

export default async function configureServer(port?: number): Promise<Server> {
    const app = next({ dev: false });
    const requestHandler = app.getRequestHandler();

    await app.prepare();

    server.all('*', (req, res) => requestHandler(req, res));

    return server.listen(port || config.server.port);
}
