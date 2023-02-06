import { Mediator } from '../mediator';
import transport from '../__mocks__/transport';
import logger from '../__mocks__/logger';

describe('Mediator', () => {
    let mediator;

    beforeEach(() => {
        transport.send.mockClear();
        logger.log.mockClear();

        // @ts-ignore
        mediator = new Mediator({
            transport,
            logger,
            guid: 'test-guid',
            deviceId: 'test-device-id',
            userGuid: 'test-user-guid',
        });
    });

    describe.each([
        [
            'offer',
            ['test-offer'],
            {
                guid: 'test-guid',
                offer: 'test-offer',
            },
        ],
        [
            'answer',
            ['test-answer'],
            {
                guid: 'test-guid',
                answer: 'test-answer',
            },
        ],
        [
            'addCandidates',
            [[{ candidate: 'test-candidate', m_line_index: 2, m_id: 3 }]],
            {
                guid: 'test-guid',
                candidates: [{ candidate: 'test-candidate', m_line_index: 2, m_id: 3 }],
            },
        ],
        [
            'keepAlive',
            [{ standard: {} }],
            {
                guid: 'test-guid',
                stats: { standard: {} },
            },
        ],
        [
            'updateState',
            [{ video: true, audio: false }],
            {
                guid: 'test-guid',
                media_state: {
                    video: true,
                    audio: false,
                },
            },
        ],
        [
            'error',
            ['clientError', 'Error message', { guid: 'call-guid' }],
            {
                guid: 'test-guid',
                type: 'clientError',
                message: 'Error message',
                detail: '{"guid":"call-guid"}',
            },
        ],
    ])(
        '#%s',
        // @ts-ignore
        (method, args, params) => {
            test('Send correct message', () => {
                // @ts-ignore
                mediator[method].apply(mediator, args);

                expect(JSON.parse(transport.send.mock.calls[0][0])).toStrictEqual({
                    id: expect.any(String),
                    jsonrpc: '2.0',
                    method,
                    params,
                });
            });

            test('Log with correct info', () => {
                // @ts-ignore
                mediator[method].apply(mediator, args);

                expect(logger.log).toBeCalledWith({
                    action: 'sendMessage',
                    component: 'Mediator',
                    guid: 'test-guid',
                    deviceId: 'test-device-id',
                    userGuid: 'test-user-guid',
                    data: {
                        id: expect.any(String),
                        method,
                        params,
                    },
                });
            });
        },
    );

    describe('#connect', () => {
        test('Subscribe to transport message event', () => {
            mediator.connect();

            expect(transport.onMessage.addListener).toBeCalledWith(expect.any(Function));
        });

        test('Log with correct info', () => {
            mediator.connect();

            expect(logger.log).toBeCalledWith({
                action: 'connect',
                component: 'Mediator',
                guid: 'test-guid',
                deviceId: 'test-device-id',
                userGuid: 'test-user-guid',
            });
        });
    });

    describe('#disconnect', () => {
        test('Unsibscribe from transport message event', () => {
            mediator.disconnect();

            expect(transport.onMessage.removeListener).toBeCalledWith(expect.any(Function));
        });

        test('Log with correct info', () => {
            mediator.disconnect();

            expect(logger.log).toBeCalledWith({
                action: 'disconnect',
                component: 'Mediator',
                guid: 'test-guid',
                deviceId: 'test-device-id',
                userGuid: 'test-user-guid',
            });
        });
    });

    describe('#handleTransportMessage', () => {
        test('Emit error if called with wrong message', (done) => {
            mediator.onError.addListener((error) => {
                expect(error).toBeInstanceOf(Error);
                done();
            });

            mediator.handleTransportMessage('{error');
        });

        test('Emit method with correct params', (done) => {
            mediator.onOffer.addListener((params) => {
                expect(params).toStrictEqual({ offer: 'test-offer', guid: 'test-guid' });
                done();
            });

            mediator.handleTransportMessage(JSON.stringify({
                method: 'offer',
                params: { offer: 'test-offer', guid: 'test-guid' },
            }));
        });

        test('Emit result with correct data', (done) => {
            mediator.onResult.addListener((data) => {
                expect(data).toStrictEqual({
                    id: 'test-id',
                    params: { guid: 'test-guid' },
                });
                done();
            });

            mediator.handleTransportMessage(JSON.stringify({
                id: 'test-id',
                result: {
                    guid: 'test-guid',
                },
            }));
        });
    });
});
