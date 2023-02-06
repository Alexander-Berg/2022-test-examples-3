import { Event } from '../Event';

describe('Event', () => {
    describe('#addListener', () => {
        test('Listener should be dispatched', () => {
            const event = new Event<string>();
            const handler = jest.fn();

            expect(event.addListener(handler)).toBeTruthy();

            event.dispatch('first');
            expect(handler).lastCalledWith('first');

            event.dispatch('second');
            expect(handler).lastCalledWith('second');
        });

        test('The same listener should no be added', () => {
            const event = new Event<string>();
            const handler = jest.fn();

            expect(event.addListener(handler)).toBeTruthy();
            expect(event.addListener(handler)).toBeFalsy();

            event.dispatch('first');
            expect(handler).lastCalledWith('first');
            expect(handler).toBeCalledTimes(1);
        });

        test('All listeners should be dispatched', () => {
            const event = new Event<string>();
            const handler1 = jest.fn();
            const handler2 = jest.fn();

            event.addListener(handler1);

            event.dispatch('first');
            expect(handler1).lastCalledWith('first');

            event.addListener(handler2);

            event.dispatch('second');
            expect(handler1).lastCalledWith('second');
            expect(handler1).toBeCalledTimes(2);
            expect(handler2).lastCalledWith('second');
            expect(handler2).toBeCalledTimes(1);
        });
    });

    describe('#removeListener', () => {
        test('Removed listener should not be dispatched', () => {
            const event = new Event<string>();
            const handler = jest.fn();

            event.addListener(handler);

            event.dispatch('first');
            expect(handler).lastCalledWith('first');

            event.removeListener(handler);

            expect(handler).toBeCalledTimes(1);
        });
    });

    describe('#removeAllListener', () => {
        test('removed listener should not be dispatched', () => {
            const event = new Event<string>();
            const handler1 = jest.fn();
            const handler2 = jest.fn();

            event.addListener(handler1);
            event.addListener(handler2);

            event.dispatch('first');
            expect(handler1).lastCalledWith('first');
            expect(handler2).lastCalledWith('first');

            event.removeAllListener();

            event.dispatch('second');
            expect(handler1).toBeCalledTimes(1);
            expect(handler2).toBeCalledTimes(1);
        });
    });
});
