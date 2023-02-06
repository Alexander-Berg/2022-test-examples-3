import { EventTarget } from 'event-target-shim';
import { fromEventTargetShimEvent } from '../fromEventTargetShimEvent';

type StubEvents = {
  event: CustomEvent<number>;
};

describe('fromEventTargetShimEvent', () => {
  it('emits events', () => {
    const target = new EventTarget<StubEvents, {}>();
    const emitter$ = fromEventTargetShimEvent(target, 'event');

    const next = jest.fn();

    const subscription = emitter$.subscribe(next);
    target.dispatchEvent(new CustomEvent('event', { detail: 1 }));
    target.dispatchEvent(new CustomEvent('event', { detail: 2 }));
    subscription.unsubscribe();

    expect(next).toBeCalledTimes(2);
    expect(next.mock.calls[0][0].detail).toBe(1);
    expect(next.mock.calls[1][0].detail).toBe(2);
  });

  it('disposes handler', () => {
    const target = new EventTarget<StubEvents, {}>();
    const addEventListener = jest.spyOn(target, 'addEventListener');
    const removeEventListener = jest.spyOn(target, 'removeEventListener');

    const emitter$ = fromEventTargetShimEvent(target, 'event');
    const subscription = emitter$.subscribe(() => {});
    subscription.unsubscribe();

    expect(addEventListener).toBeCalledTimes(1);
    expect(removeEventListener).toBeCalledTimes(1);
    expect(addEventListener.mock.calls[0][0]).toBe('event');
    expect(removeEventListener).toBeCalledWith('event', addEventListener.mock.calls[0][1]);
  });
});
