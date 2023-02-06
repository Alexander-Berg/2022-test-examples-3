import '@yandex-int/messenger.utils/lib_cjs/test/MessageChannel';
import { pause } from '@yandex-int/messenger.utils';

import { workerFactory } from '@yandex-int/messenger.utils/lib_cjs/test/Worker';

import { channelProxyFactory } from '../ChannelProxy';
import { channelMessagesHandlerFactory } from '../ChannelMessagesHandler';
import { ChannelOutgoing } from '../ChannelOutgoing';
import { WorkerPostMessageTransport } from '../WorkerPostMessageTransport';
import { ChannelBoss } from '../ChannelBoss';
import { ChannelTransport } from '../types';

type Observers = {
    object1: { text: string };
    object2: { text: string };
}

type Handler = {
    message1(data: { text: string }): { status: string };
};

type ScopeParams = {

};

describe('ChannelBoss', () => {
    let channel: ChannelOutgoing<Handler, Handler, {}, {}, Observers, {}, ScopeParams>;
    let boss: ChannelBoss<[Handler], Observers, ScopeParams>;
    let transport: WorkerPostMessageTransport<unknown>;

    beforeEach(() => {
        const [inner, outer] = workerFactory();
        transport = new WorkerPostMessageTransport(inner as Worker);
        channel = new ChannelOutgoing<Handler, Handler, {}, {}, Observers, {}, ScopeParams>(
            'test',
            (callback: (transport: ChannelTransport) => void) => {
                callback(transport);
            },
        );

        boss = new ChannelBoss();
        boss.init(outer as Worker);
    });

    afterEach(() => {
        jest.useRealTimers();
        boss.destroy();
        channel.dispose();
        transport.dispose();
    });

    describe('#connect', () => {
        it('requests should wait for connection', async () => {
            boss.onRequest.addListener((event) => {
                event.response({ status: 'ok' });
            });

            expect(await channel.request('message1', { text: 'test' }))
                .toMatchObject({
                    status: 'ok',
                });
        });

        it('onInit should be called when new channel connected', async (done) => {
            boss.onInit.addListener((scope) => {
                expect(scope).toMatchObject({
                    scope1: {
                        ok: true,
                    },
                });

                channel2.dispose();
                done();
            });

            const channel2 = new ChannelOutgoing<Handler, Handler, {}, {}, Observers, ScopeParams>(
                'test2',
                (callback: (transport: ChannelTransport) => void) => {
                    callback(transport);
                },
                {
                    scope: {
                        scope1: {
                            ok: true,
                        },
                    },
                });

            channel2.connect();
        });
    });

    describe('#request', () => {
        it('check response', async () => {
            boss.onRequest.addListener((event) => {
                event.response(Promise.resolve({ status: 'ok' }));
            });

            const response = await channel.request('message1', { text: 'test' });

            expect(response.status).toBe('ok');
        });

        it('error', async () => {
            boss.onRequest.addListener((event) => {
                event.response(Promise.reject(new Error('test')));
            });

            try {
                await channel.request('message1', { text: 'test' });
            } catch (e) {
                expect(true).toBe(true);
            }
        });
    });

    describe('#observe', () => {
        it('response should be right', async () => {
            boss.onObserve.addListener((event) => {
                switch (event.data.objectName) {
                    case 'object1':
                        event.notify({ text: 'test' });
                        break;
                    case 'object2':
                        event.notify({ text: 'test2' });
                        break;
                    default:
                        return;
                }
            });

            const spy1 = jest.fn();
            const spy2 = jest.fn();

            await channel.observe('object1', spy1);

            expect(spy1).toBeCalledTimes(1);
            expect(spy1).toBeCalledWith({ text: 'test' });

            boss.notifyObservers('object2', { text: 'test2' });

            await channel.observe('object2', spy2);

            expect(spy2).toBeCalledTimes(1);
            expect(spy2).toBeCalledWith({ text: 'test2' });

            boss.notifyObservers('object1', { text: 'test3' });

            await new Promise(resolve => setTimeout(resolve, 100));

            expect(spy2).toBeCalledTimes(1);
            expect(spy1).toBeCalledTimes(2);
            expect(spy1).toBeCalledWith({ text: 'test3' });
        });
    });

    describe('#unobserve', () => {
        it('should not be called', async () => {
            const spy1 = jest.fn();

            await channel.observe('object1', spy1);

            boss.notifyObservers('object1', { text: 'test' });

            await pause(100);

            expect(spy1).toBeCalledTimes(1);
            expect(spy1).toBeCalledWith({ text: 'test' });

            await channel.unobserve('object1', spy1);

            await pause(100);

            boss.notifyObservers('object1', { text: 'test2' });

            expect(spy1).toBeCalledTimes(1);
        });
    });

    describe('#channelProxy', () => {
        it('methods shoud be invoked over channel', async () => {
            boss.destroy();

            const scope = 'scope1';
            const api = {
                getMessage() {
                    return 'message';
                },
                concat([a, b]: [string, string]) {
                    return a + b;
                },
            };

            const [inner, outer] = workerFactory();

            const channel = new ChannelOutgoing<typeof api, {}, {}, {}>(
                'test2',
                (callback: (transport: ChannelTransport) => void) => {
                    callback(new WorkerPostMessageTransport(inner as Worker));
                },
                {
                    scope: { [scope]: {} },
                },
            );

            const channelProxy = channelProxyFactory<typeof api>(
                scope,
                { channel },
                ['getMessage', 'concat'],
            );

            const messageHandler = channelMessagesHandlerFactory(scope, () => api);

            const newBoss = new ChannelBoss<[typeof api], {}, { [scope]: {} }>();

            boss = newBoss as any;

            newBoss.init(outer as Worker);

            newBoss.onInit.addListener(messageHandler.onInit);
            newBoss.onRequest.addListener(messageHandler.onEvent);

            const response = await channelProxy.getMessage();

            expect(response).toBe('message');

            const response2 = await channelProxy.concat(['t', 'a']);

            expect(response2).toBe('ta');
        });
    });
});
