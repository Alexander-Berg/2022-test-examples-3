import PubSub from '../pubsub';

type EventFn = (params?: object) => void;
type Callback = (fn: EventFn) => void;

let pubsub: PubSub;
let callOrder: number[];
let callback1: Callback;
let callback2: Callback;
let callback3: Callback;

beforeEach(() => {
    pubsub = new PubSub();

    callOrder = [];
    callback1 = jest.fn(() => callOrder.push(1));
    callback2 = jest.fn(() => callOrder.push(2));
    callback3 = jest.fn(() => callOrder.push(3));
});

test('Подписывает на события', () => {
    pubsub.on('event1', callback1);
    pubsub.on('event2', callback2);
    pubsub.trigger('event1', { id: 1 });
    pubsub.trigger('event2', { id: 2 });

    expect(callback1).toBeCalledOnceWith({ id: 1 });
    expect(callback2).toBeCalledOnceWith({ id: 2 });
});

test('Не подписывает 2 раза на одно событие', () => {
    pubsub.on('event', callback1);
    pubsub.on('event', callback2);
    pubsub.on('event', callback1);
    pubsub.trigger('event');

    expect(callOrder).toEqual([1, 2]);
});

test('Отписывает от событий', () => {
    pubsub.on('event', callback1);
    pubsub.on('event', callback2);
    pubsub.off('event', callback1);
    pubsub.trigger('event', { id: 1 });

    expect(callback1).not.toHaveBeenCalled();
    expect(callback2).toBeCalledOnceWith({ id: 1 });
});

test('Отписывает от несуществующих событий без ошибки', () => {
    pubsub.off('event', jest.fn());
});

test('События вызываются в порядке добавления', () => {
    pubsub.on('event', callback1);
    pubsub.on('event', callback2);
    pubsub.on('event', callback3);
    pubsub.off('event', callback1);
    pubsub.on('event', callback1);
    pubsub.trigger('event');

    expect(callOrder).toEqual([2, 3, 1]);
});

test('Подписка одной функции на несколько событий не нарушает порядок', () => {
    pubsub.on('event1', callback1);
    pubsub.on('event2', callback2);
    pubsub.on('event1', callback3);
    pubsub.on('event2', callback1);

    pubsub.trigger('event1');
    expect(callOrder).toEqual([1, 3]);

    callOrder = [];
    pubsub.trigger('event2');
    expect(callOrder).toEqual([2, 1]);
});
