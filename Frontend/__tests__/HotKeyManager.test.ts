import HotKeyManager from '../HotKeyManager';
import {
    ListenersStack,
    extractListenersStacks,
    noop,
    emulateEvent,
    createDescriptor,
    expectEqualCallCount,
} from './utils';

describe('HotKeyManager', () => {
    const mocks: jest.SpyInstance[] = [];
    const stacks: ListenersStack[] = extractListenersStacks();

    afterEach(() => {
        mocks.forEach((mock) => mock.mockRestore());
    });

    const testEvent = (eventName: 'keyup' | 'keydown', capture: boolean) => {
        const cb = jest.fn();
        const unsub = HotKeyManager.subscribe(createDescriptor({ key: 'Enter' }), cb, { eventName, capture });
        emulateEvent(eventName, 'Enter');
        expect(cb).toBeCalledTimes(1);
        unsub();
    };

    it('слушает keydown', () => {
        testEvent('keydown', false);
    });

    it('слушает keyup', () => {
        testEvent('keyup', false);
    });

    it('слушает keydown в capture фазе', () => {
        testEvent('keydown', true);
    });

    it('слушает keyup в capture фазе', () => {
        testEvent('keyup', true);
    });

    it('возвращает правильную функцию unsubscribe из вызова .subscribe()', () => {
        stacks.forEach((stack) => {
            mocks.push(jest.spyOn(stack, 'subscribe'));
            mocks.push(jest.spyOn(stack, 'unsubscribe'));
        });

        const unsub = HotKeyManager.subscribe(createDescriptor(), noop);
        unsub();

        stacks.forEach((s) => {
            expectEqualCallCount(s.subscribe, s.unsubscribe);
            Object.keys(s.listeners).map((key) => s.listeners[key]).forEach((a) => {
                expect(a.length).toBe(0);
            });
        });
    });

    it('предотвращает подписку одной и той же функции на одно и то же событие (не в продакшене)', () => {
        if (process.env.NODE_ENV !== 'production') {
            let unsubscribe;
            const subscribeTwice = () => {
                unsubscribe = HotKeyManager.subscribe(createDescriptor(), noop);
                HotKeyManager.subscribe(createDescriptor(), noop);
            };

            expect(subscribeTwice).toThrow(ReferenceError);
            unsubscribe();
        }
    });

    it('вызывает всех слушателей, если они это не переопределяют', () => {
        const n = 3;

        const listeners = new Array(n).fill(null).map(() => jest.fn());

        const unsubs = listeners.map((l) => HotKeyManager.subscribe(createDescriptor({ key: 'Enter' }), l));

        emulateEvent('keyup', 'Enter');

        listeners.forEach((listener) => expect(listener).toBeCalledTimes(1));
        unsubs.forEach((unsub) => unsub());
    });

    it('прекращает обработку события, если слушатель вернул true', () => {
        const l1 = jest.fn();
        const l2 = jest.fn(() => true);
        const l3 = jest.fn();

        const unsubs = [
            HotKeyManager.subscribe(createDescriptor({ key: 'Enter' }), l1),
            HotKeyManager.subscribe(createDescriptor({ key: 'Enter' }), l2),
            HotKeyManager.subscribe(createDescriptor({ key: 'Enter' }), l3),
        ];

        emulateEvent('keyup', 'Enter');

        expect(l1).toBeCalledTimes(0);
        expect(l2).toBeCalledTimes(1);
        expect(l3).toBeCalledTimes(1);

        unsubs.forEach((unsub) => unsub());
    });

    it('прекращает обработку события, если вызван preventDefault', () => {
        const l1 = jest.fn();
        const l2 = jest.fn((ev: Event) => { ev.preventDefault() });
        const l3 = jest.fn();

        const unsubs = [
            HotKeyManager.subscribe(createDescriptor({ key: 'Enter' }), l1),
            HotKeyManager.subscribe(createDescriptor({ key: 'Enter' }), l2),
            HotKeyManager.subscribe(createDescriptor({ key: 'Enter' }), l3),
        ];

        emulateEvent('keyup', 'Enter');

        expect(l1).toBeCalledTimes(0);
        expect(l2).toBeCalledTimes(1);
        expect(l3).toBeCalledTimes(1);

        unsubs.forEach((unsub) => unsub());
    });
});
