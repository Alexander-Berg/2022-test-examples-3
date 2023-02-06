import { PostMessage } from '../PostMessage';

describe('PostMessage', () => {
    describe('#sendMessage', () => {
        it('should not trigger an event if not in a iframe', () => {
            const transport = new PostMessage({
                parentOrigin: 'https://yandex.ru',
                allowedOrigin: 'https://yandex.ru',
                checkOrigin: true,
                widgetId: 'test',
                namespace: 'MESSENGER',
            });

            const spy = jest.fn();

            transport.onMessage.addListener(spy);

            transport.start();

            transport.sendMessage({ type: 'fullscreenOn' });

            expect(spy).toBeCalledTimes(0);
        });
    });

    describe('#handleMessage', () => {
        it('should not calls onMessage if incorrect origin', () => {
            const transport = new PostMessage({
                parentOrigin: 'https://yandex.ru',
                checkOrigin: true,
                enabled: true,
                widgetId: 'test',
                namespace: 'MESSENGER',
            });

            const spy = jest.fn();

            transport.onMessage.addListener(spy);

            transport.start();

            const event = new MessageEvent('message', {
                origin: 'https://intruder.ru',
                data: { type: 'sendImage' },
            });

            window.dispatchEvent(event);

            expect(spy).toBeCalledTimes(0);
        });

        it('should calls onMessage if origin is correct', () => {
            const transport = new PostMessage({
                parentOrigin: 'https://yandex.ru',
                allowedOrigin: 'https://yandex.ru',
                checkOrigin: true,
                enabled: true,
                widgetId: 'test',
                namespace: 'MESSENGER',
            });

            const spy = jest.fn();

            transport.onMessage.addListener(spy);

            transport.start();

            const event = new MessageEvent('message', {
                origin: 'https://yandex.ru',
                data: { type: 'sendImage' },
            });

            window.dispatchEvent(event);

            expect(spy).toBeCalledTimes(1);
        });
    });
});
