import '@yandex-int/messenger.utils/lib_cjs/test/MessageChannel';
import { applyJSDOMPostMessageFix, rollbackJSDOMPostMessageFix, scenarioFactory, pause } from '@yandex-int/messenger.utils';

import { ChannelOutgoing } from '../ChannelOutgoing';
import { ChannelIncoming } from '../ChannelIncoming';
import { WindowPostMessageTransport } from '../WindowPostMessageTransport';
import { IframePostMessageTransport } from '../IframePostMessageTransport';
import { UnhandledResponseError } from '../errors/UnhandledResponseError';
import { CLOSE_CODE } from '../consts';

type Observers = {
    object1: { text: string };
}

type Observables = {
    object2: { text: string };
}

type Requests = {
    message1(data: { text: string }): { status: string };
    message2(data: { text: string }): { status: string };
    message3(data: { text: string }): number;
};

type Events = {
    event1: { text: string };
    event2: { count: number };
}

type Events2 = {
    event3: { text: string };
    event4: { count: number };
}

type ScopeParams = {

};

describe('Channel', () => {
    let outChannel: ChannelOutgoing<Requests, Requests, Events2, Events, Observers, Observables, ScopeParams>;
    let inChannel: ChannelIncoming<Requests, Requests, Events, Events2, Observables, Observers, ScopeParams>;

    const url = 'https://yandex.ru/chat';
    const origin = new URL(url).origin;
    let innerTransport: IframePostMessageTransport<any>;
    let outerTransport: WindowPostMessageTransport<any>;
    let iframe: HTMLIFrameElement;
    let channelId: string;

    beforeAll(() => {
        applyJSDOMPostMessageFix();
    });

    afterAll(() => {
        rollbackJSDOMPostMessageFix();
    });

    beforeEach(() => {
        iframe = document.createElement('iframe');

        iframe.src = url;

        document.body.appendChild(iframe);

        innerTransport = new IframePostMessageTransport(
            iframe.contentWindow,
            origin,
        );

        outerTransport = new WindowPostMessageTransport(
            iframe.contentWindow,
            origin,
            [origin],
        );

        channelId = Math.random().toString(16);

        outChannel = new ChannelOutgoing<
            Requests,
            Requests,
            Events2,
            Events,
            Observers,
            Observables,
            ScopeParams
        >(
            channelId,
            (callback) => {
                callback(innerTransport);
            },
            {
                scope: {},
            },
        );

        inChannel = new ChannelIncoming<Requests, Requests, Events, Events2, Observables, Observers, ScopeParams>(
            channelId,
            outerTransport,
            () => Promise.resolve({}),
        );

        outerTransport.connect();
        innerTransport.connect();
    });

    afterEach(() => {
        inChannel.dispose();
        outChannel.dispose();
        innerTransport.dispose();
        outerTransport.dispose();
    });

    describe('#requests', () => {
        it('respond to request should work with promise and not promise', async() => {
            outChannel.connect();

            inChannel.onRequest.addListener((event) => {
                if (event.type === 'message1') {
                    expect(event.payload.text).toBe('text');

                    event.response(Promise.resolve({ status: 'ok' }));
                }

                if (event.type === 'message2') {
                    event.response({ status: 'ok' });
                }
            });

            expect(await outChannel.request('message1', { text: 'text' })).toMatchObject({
                status: 'ok',
            });

            expect(await outChannel.request('message2', { text: 'text' })).toMatchObject({
                status: 'ok',
            });
        });

        it('request from inchannel with autoestablish connection', async() => {
            outChannel.connect();

            outChannel.onRequest.addListener((event) => {
                if (event.type === 'message1') {
                    event.response({ status: 'ok' });
                }
            });

            expect(await inChannel.request('message1', { text: 'text' }))
                .toMatchObject({
                    status: 'ok',
                });
        });

        it('request should be rejected with Promise<Error>', async() => {
            try {
                inChannel.onRequest.addListener(({ type, reject }) => {
                    reject(new UnhandledResponseError(type));
                });

                await outChannel.request('message1', { text: 'unhandled' });
            } catch (e) {
                expect(e).toBeInstanceOf(UnhandledResponseError);
            }
        });

        it('request should be rejected with reject', async() => {
            try {
                inChannel.onRequest.addListener(({ reject, type }) => {
                    reject(new UnhandledResponseError(type));
                });

                await outChannel.request('message1', { text: 'unhandled' });
            } catch (e) {
                expect(e).toBeInstanceOf(UnhandledResponseError);
            }
        });
    });

    describe('#reconnect', () => {
        it('reconnection by window @@@@ping', async() => {
            outChannel.onRequest.addListener((event) => {
                if (event.type === 'message1') {
                    event.response({ status: 'ok' });
                }
            });

            outChannel.connect();

            expect(await inChannel.request('message1', { text: 'text' }))
                .toMatchObject({
                    status: 'ok',
                });

            inChannel.dispose();
            outerTransport.dispose();

            await new Promise<void>(resolve => {
                function handler() {
                    outChannel.onClose.removeListener(handler);
                    resolve();
                }

                outChannel.onClose.addListener(handler);
            });

            outerTransport = new WindowPostMessageTransport(
                iframe.contentWindow,
                origin,
                [origin],
            );

            outerTransport.connect();

            inChannel = new ChannelIncoming(
                channelId,
                outerTransport,
                () => Promise.resolve({}),
            );

            inChannel.onRequest.addListener((event) => {
                if (event.type === 'message1') {
                    event.response({ status: 'ok' });
                }
            });

            expect(await inChannel.request('message1', { text: 'text' }))
                .toMatchObject({
                    status: 'ok',
                });
        });
    });

    describe('#event', () => {
        it('onEvent should be fired in inChannel', async (done) => {
            inChannel.onEvent.addListener((event) => {
                if (event.type === 'event1') {
                    expect(event.data.text).toBe('test1');
                    done();
                }
            });

            outChannel.event('event1', { text: 'test1' });
        });

        it('onEvent should be fired in outChannel', async (done) => {
            outChannel.onEvent.addListener((event) => {
                if (event.type === 'event3') {
                    expect(event.data.text).toBe('test2');
                    done();
                }
            });

            inChannel.event('event3', { text: 'test2' });

            outChannel.connect();
        });
    });

    describe('#observers', () => {
        it('observer should be updated in outChannel', async() => {
            inChannel.onObserve.addListener((event) => {
                if (event.data.objectName === 'object1') {
                    event.notify({
                        text: 'object1',
                    });
                }
            });

            const callback = jest.fn();

            await outChannel.observe('object1', callback);
            await pause(100);

            expect(callback).nthCalledWith(1, expect.objectContaining({
                text: 'object1',
            }));

            inChannel.notifyObservers('object1', {
                text: 'updated!',
            });

            await pause(100);

            expect(callback).nthCalledWith(2, expect.objectContaining({
                text: 'updated!',
            }));
        });

        it('observer should be updated in inChannel', async() => {
            outChannel.onObserve.addListener((event) => {
                if (event.data.objectName === 'object2') {
                    event.notify({
                        text: 'object2',
                    });
                }
            });

            const callback = jest.fn();

            outChannel.connect();

            await inChannel.observe('object2', callback);
            await pause(100);

            expect(callback).toBeCalledWith(expect.objectContaining({
                text: 'object2',
            }));

            outChannel.notifyObservers('object2', {
                text: 'updated!',
            });

            await pause(100);

            expect(callback).nthCalledWith(2, expect.objectContaining({
                text: 'updated!',
            }));
        });
    });

    describe('#close', () => {
        it('outchannel should be closed when inChannel was closed', async () => {
            const scenario = scenarioFactory(
                (spy, i) => expect(spy).nthCalledWith(i, expect.objectContaining({
                    closeCode: CLOSE_CODE.CONNECTION_CLOSED,
                    closeReason: 'kill signal',
                })),
            );

            outChannel.onConnectionEstablished.addListener(() => {
                inChannel.close({});
            });

            outChannel.onClose.addListener(scenario.handle);

            outChannel.connect();

            await scenario.untilDone;
        });

        it('outchannel should be closed when inChannel was closed with custom reason', async () => {
            const scenario = scenarioFactory(
                (spy, i) => expect(spy).nthCalledWith(i),
                (spy, i) => expect(spy).nthCalledWith(i, expect.objectContaining({
                    closeCode: CLOSE_CODE.CONNECTION_CLOSED,
                    closeReason: 'kill signal',
                    closeError: expect.objectContaining({ message: 'test error' }),
                })),
            );

            outChannel.onConnectionEstablished.addListener(() => {
                scenario.handle();
                inChannel.close({ closeCode: CLOSE_CODE.CONNECTION_CLOSED, reason: 'test error' });
            });

            outChannel.onClose.addListener(scenario.handle);

            outChannel.connect();

            await scenario.untilDone;
        });
    });
});
