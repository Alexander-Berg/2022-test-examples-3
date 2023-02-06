import {parentPort} from 'worker_threads';

import WorkerBackend from './backend';
import {Message} from './client';

const workerBackend = new WorkerBackend();

export type SuccessResponseMessage<TData> = {
    id: string;
    response: {
        body: TData;
    };
};

export type ErrorResponseMessage = {
    id: string;
    response: {
        error: {
            message: string;
            stack: string;
            name: string;
        };
    };
};

parentPort?.on('message', async (message: Message) => {
    if (message.type === 'worker-call') {
        try {
            // @ts-ignore
            const result = await workerBackend[message.data.method](
                ...message.data.args,
            );

            const responseMessage: SuccessResponseMessage<unknown> = {
                id: message.id,
                response: {
                    body: result,
                },
            };

            parentPort?.postMessage(responseMessage);
        } catch (e: any) {
            if (e instanceof Error || (e.stack && e.message)) {
                const responseMessage: ErrorResponseMessage = {
                    id: message.id,
                    response: {
                        error: {
                            message: e.message,
                            stack: e.stack as string,
                            name: e.name,
                        },
                    },
                };
                parentPort?.postMessage(responseMessage);
            } else {
                throw e;
            }
        }
    }
});

process.on('unhandledRejection', reason => {
    console.error('Unhandled rejection', reason);
});

process.on('uncaughtException', reason => {
    console.error('Unhandled exception', reason);
});
