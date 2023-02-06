import { exec as callbackExec } from 'child_process';
import { connect } from 'net';
import { promisify } from 'util';
import { unusedPort } from './utils';

interface App {
    listen: (port: number) => void;
    stop: () => Promise<unknown>;
}

const exec = promisify(callbackExec);

export const GRPC_CLIENT_PATH = 'grpc_client';
export const grpc = (
    endpoint: string = '/',
    data?: {
        context?: string | object;
        host?: string;
        port?: number;
    },
    // eslint-disable-next-line
): Promise<any> => {
    const {
        context: anyContext = [{}],
        host = 'localhost',
        port = 10014,
    } = data || {};

    const context =
        typeof anyContext === 'object'
            ? JSON.stringify(anyContext)
            : anyContext;

    const cmd = `${GRPC_CLIENT_PATH} ${host}:${port}${endpoint} --proto-to-json --pretty -c '${context}'`;

    const client = connect({
        host,
        port,
    });
    return new Promise((resolve, reject) => {
        client.on('connect', async () => {
            try {
                const { stdout } = await exec(cmd);
                resolve(JSON.parse(stdout));
                client.destroy();
                // eslint-disable-next-line
            } catch (error: any) {
                if (error.stderr && /command not found/.test(error.stderr)) {
                    return reject(
                        new Error(
                            'Install grpc_client. https://a.yandex-team.ru/arcadia/crm/apphost/nodejs/packages/apphost-test/README.md',
                        ),
                    );
                }
                reject(error);
            }
        });
    });
};

export const createGrpc = (
    appFactory: () => App,
    settings?: {
        endpoint?: string;
        host?: string;
        port?: number;
    },
): typeof grpc => {
    const app = appFactory();
    const {
        endpoint = '/',
        host = 'localhost',
        port = unusedPort(),
    } = settings || {};

    if (process.env.JEST_WORKER_ID != null) {
        beforeAll(() => {
            app.listen(port);
        });

        afterAll(done => {
            app.stop().then(done);
        });
    }

    return (...args) => {
        return grpc(args[0] || endpoint, {
            context: args[1]?.context,
            host: args[1]?.host || host,
            port: args[1]?.port || port,
        });
    };
};
