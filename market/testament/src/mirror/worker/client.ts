import {Worker} from 'worker_threads';

// @ts-ignore
import {v4 as uuidv4} from 'uuid';

import type {ErrorResponseMessage, SuccessResponseMessage} from './process';

export type Defer<TResult> = {
    promise: Promise<TResult>;
    resolve(value: TResult): void;
    reject(error: Error): void;
};

function createDefer<TResult>(): Defer<TResult> {
    // @ts-ignore
    const defer: Defer<TResult> = {};
    defer.promise = new Promise<TResult>((resolve, reject) => {
        defer.resolve = resolve;
        defer.reject = reject;
    });

    return defer;
}

export type SendData = {
    method: string;
    args: any[];
};

export type Message = {
    type: string;
    id: string;
    data: SendData;
};

export default class WorkerClient<TBackend> {
    private worker: Worker;

    readonly backend: TBackend;

    private deferMap = new Map<string, Defer<any>>();

    constructor(processFile: string) {
        const argv =
            // @ts-ignore
            global.jestEnvironmentOptions?.argv?.slice(2) ??
            process.argv.slice(2);
        this.worker = new Worker(processFile, {
            argv,
        });
        // @ts-ignore
        this.backend = new Proxy({} as TBackend, {
            get:
                (
                    _: TBackend,
                    method: string,
                ): ((...args: any[]) => Promise<any>) =>
                (...args: any[]) =>
                    this.call(method, args),
        });

        this.worker.on(
            'message',
            (
                message: SuccessResponseMessage<unknown> | ErrorResponseMessage,
            ) => {
                const defer = this.deferMap.get(message.id);
                this.deferMap.delete(message.id);
                // @ts-ignore
                if (message.response.error) {
                    const e = new Error();
                    // @ts-ignore
                    e.name = message.response.error.name ?? undefined;
                    e.message =
                        // @ts-ignore
                        message.response.error.message ?? 'Unknown error';
                    // @ts-ignore
                    e.stack = message.response.error.stack ?? e.stack;
                    // @ts-ignore
                    defer.reject(e);
                } else {
                    // @ts-ignore
                    defer.resolve(message.response.body);
                }
            },
        );
    }

    async call<TResult>(method: string, args: any[]): Promise<TResult> {
        return this.send({method, args});
    }

    async send<TResult>(data: SendData): Promise<TResult> {
        const message: Message = {
            type: 'worker-call',
            id: uuidv4(),
            data,
        };
        const defer = createDefer<TResult>();
        this.deferMap.set(message.id, defer);
        this.worker.postMessage(message);

        try {
            return await defer.promise;
        } catch (e) {
            const tmpError = new Error();
            const [, ...currentStack] = tmpError.stack?.split('\n') ?? [''];
            // @ts-ignore
            e.stack = `${e.stack}\n${currentStack.join('\n')}`;
            throw e;
        }
    }

    end(): Promise<number> {
        return this.worker.terminate();
    }
}
