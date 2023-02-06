import { parentWindow } from '../../../services/ParentWindow';
import { Transport } from '../Transport';
import { IncommingPacket, mapIncommingPacketToEvent } from '../PostMessage';

class TestTransport extends Transport {
    public sendRequest = jest.fn();
    public sendMessage = jest.fn();

    public start(): void {}
    public stop(): void {}

    public ready(): void {}

    public receiveMessage(message: IncommingPacket) {
        this.onMessage.dispatch(mapIncommingPacketToEvent(message));
    }
}

describe('parentWindow', () => {
    let transport: TestTransport;

    beforeEach(() => {
        transport = new TestTransport();
    });

    describe('handleMessage()', () => {
        it('should calls onMessage for a event', () => {
            const spy = jest.fn();

            parentWindow.init({
                transport,
                incomingMessageTypes: ['iframeOpen'],
                outgoingMessageTypes: [],
                onMessage: spy,
            });

            transport.receiveMessage({
                type: 'iframe-open' as any,
                namespace: undefined,
                payload: { chatId: '123' },
            });

            expect(spy).toBeCalledWith(expect.objectContaining({
                type: 'iframeOpen',
                payload: { chatId: '123' },
            }));
        });

        it('should calls onMessage for a event with action', () => {
            const spy = jest.fn();
            parentWindow.init({
                transport,
                incomingMessageTypes: ['iframeOpen'],
                outgoingMessageTypes: [],
                onMessage: spy,
            });

            transport.receiveMessage({
                type: 'iframeOpen',
                payload: { chatId: '123' },
            });

            expect(spy).toBeCalledWith(expect.objectContaining({
                type: 'iframeOpen',
                payload: { chatId: '123' },
            }));
        });

        it('should not calls onMessage if incorrect event namespace', () => {
            const spy = jest.fn();

            parentWindow.init({
                transport,
                incomingMessageTypes: ['iframe-open'],
                outgoingMessageTypes: [],
                onMessage: spy,
            });

            transport.receiveMessage({
                namespace: 'my',
                type: 'iframeOpen',
                payload: undefined,
            });

            expect(spy).toBeCalledTimes(0);
        });

        it('should not calls onMessage if not allowed event type', () => {
            const spy = jest.fn();

            parentWindow.init({
                transport,
                incomingMessageTypes: ['iframeOpen'],
                outgoingMessageTypes: [],
                onMessage: spy,
            });

            transport.receiveMessage({
                namespace: 'messenger',
                type: 'oauth',
                payload: {
                    token: 'test',
                },
            });

            expect(spy).toBeCalledTimes(0);
        });
    });

    describe('sendMessage()', () => {
        it('should not trigger an event if the event is not allowed', () => {
            parentWindow.init({
                transport,
                incomingMessageTypes: ['iframeOpen'],
                outgoingMessageTypes: ['close'],
            });

            parentWindow.sendMessage({ type: 'fullscreenOn' });

            expect(transport.sendMessage).toBeCalledTimes(0);
        });

        it('should trigger an event', () => {
            parentWindow.init({
                transport,
                incomingMessageTypes: ['iframe-open'],
                outgoingMessageTypes: ['counter'],
            });

            parentWindow.sendMessage({
                type: 'counter',
                payload: { value: 0 },
            });

            expect(transport.sendMessage).toBeCalledWith({
                type: 'counter',
                payload: { value: 0 },
            });
        });

        it('should trigger "ready" event only once', () => {
            parentWindow.init({
                transport,
                outgoingMessageTypes: ['ready'],
            });

            parentWindow.sendMessage({ type: 'ready' });
            parentWindow.sendMessage({ type: 'ready' });
            parentWindow.sendMessage({ type: 'ready' });

            expect(transport.sendMessage).toBeCalledTimes(1);
        });
    });

    describe('Params', () => {
        const chatId = '123';

        beforeEach(() => {
            parentWindow.clearParams();
            parentWindow.updateParams({
                parentUrl: 'https://yandex.ru/search?text=test',
                permalink: 'link',
                serviceId: 22,
                orgUrl: 'orgUrl',
            }, chatId);
        });

        describe('isChatDependentParam()', () => {
            it('should returns chat undependent param', () => {
                expect(parentWindow.isChatDependentParam('parentUrl')).toBeFalsy();
            });
        });

        describe('getParam()', () => {
            it('should returns a param', () => {
                expect(parentWindow.getParam('serviceId')).toEqual(22);
                expect(parentWindow.getParam('serviceId', chatId)).toEqual(22);
            });
        });

        describe('updateParam()', () => {
            it('should update a param', () => {
                parentWindow.updateParam('serviceId', 23, chatId);

                expect(parentWindow.getParam('serviceId', chatId)).toEqual(23);
            });
        });

        describe('clearParams', () => {
            it('should clear params', () => {
                parentWindow.clearParams();

                expect(parentWindow.getParam('serviceId', chatId)).toBeUndefined();
            });
        });
    });
});
