import {
    MessageEmitter,
} from '.';

describe('MessageEmitter', () => {
    test('MessageEmitter.on(listener)', () => {
        const global = {
            addEventListener: jest.fn((_eventName, _onMessage) => {}),
            removeEventListener: jest.fn((_eventName, _onMessage) => {}),
        };

        const listener1 = jest.fn((_data, _origin) => {});
        const listener2 = jest.fn((_data, _origin) => {});
        const listener3 = jest.fn((_data, _origin) => {});

        MessageEmitter.on(listener1, global);

        expect(global.addEventListener.mock.calls[0][0]).toBe('message');

        const onMessage = global.addEventListener.mock.calls[0][1];

        onMessage({ data: '{"a":1,"b":2}', origin: 'https://yandex.ru' });

        expect(listener1.mock.calls[0]).toEqual([{ a: 1, b: 2 }, 'https://yandex.ru']);

        MessageEmitter
            .on(listener2, global)
            .on(listener3, global);

        onMessage({ data: { c: 3, d: 4 }, origin: 'https://ya.ru' });

        expect(listener1.mock.calls[1]).toEqual([{ c: 3, d: 4 }, 'https://ya.ru']);
        expect(listener2.mock.calls[0]).toEqual([{ c: 3, d: 4 }, 'https://ya.ru']);
        expect(listener3.mock.calls[0]).toEqual([{ c: 3, d: 4 }, 'https://ya.ru']);

        expect(global.addEventListener.mock.calls.length).toBe(1);
        expect(global.removeEventListener.mock.calls.length).toBe(0);

        expect(listener1.mock.calls.length).toBe(2);
        expect(listener2.mock.calls.length).toBe(1);
        expect(listener3.mock.calls.length).toBe(1);

        MessageEmitter
            .off(listener1, global)
            .off(listener2, global)
            .off(listener3, global);
    });

    test('MessageEmitter.off(listener)', () => {
        const global = {
            addEventListener: jest.fn((_eventName, _onMessage) => {}),
            removeEventListener: jest.fn((_eventName, _onMessage) => {}),
        };

        const listener1 = jest.fn((_data, _origin) => {});
        const listener2 = jest.fn((_data, _origin) => {});
        const listener3 = jest.fn((_data, _origin) => {});

        MessageEmitter.on(listener1, global);

        expect(global.addEventListener.mock.calls.length).toBe(1);
        expect(global.removeEventListener.mock.calls.length).toBe(0);

        MessageEmitter.off(listener1, global);

        expect(global.addEventListener.mock.calls.length).toBe(1);
        expect(global.removeEventListener.mock.calls.length).toBe(1);

        MessageEmitter
            .on(listener1, global)
            .on(listener2, global)
            .on(listener3, global);

        expect(global.addEventListener.mock.calls.length).toBe(2);
        expect(global.removeEventListener.mock.calls.length).toBe(1);

        MessageEmitter
            .off(listener1, global)
            .off(listener3, global);

        expect(global.addEventListener.mock.calls.length).toBe(2);
        expect(global.removeEventListener.mock.calls.length).toBe(1);

        MessageEmitter.off(listener2, global);

        expect(global.addEventListener.mock.calls.length).toBe(2);
        expect(global.removeEventListener.mock.calls.length).toBe(2);

        expect(global.removeEventListener.mock.calls[0][0]).toBe('message');
    });
});
