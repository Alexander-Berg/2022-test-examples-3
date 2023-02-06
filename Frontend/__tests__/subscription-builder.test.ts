import { subscriptionBuilder } from '../subscription-builder';

describe('subscriptionBuilder', () => {
  test('should return notify and subscribe functions', () => {
    const { notify, subscribe } = subscriptionBuilder();

    expect(typeof notify).toBe('function');
    expect(typeof subscribe).toBe('function');
  });

  test('should call subscribtion function', () => {
    const { notify, subscribe } = subscriptionBuilder();
    const mock = jest.fn();

    subscribe({ event: 'event', fn: mock });
    notify({ event: 'event' });

    expect(mock).toBeCalled();
  });

  test('should call subscribtion function with data', () => {
    const { notify, subscribe } = subscriptionBuilder();
    const mock = jest.fn();
    const data = 'data';

    subscribe({ event: 'event', fn: mock });
    notify({ event: 'event', data });

    expect(mock).toBeCalledWith(data);
  });

  test('should call all subscribtion functions with data', () => {
    const { notify, subscribe } = subscriptionBuilder();
    const mocks = [jest.fn(), jest.fn(), jest.fn()];
    const data = 'data';

    mocks.forEach((mock) => subscribe({ event: 'event', fn: mock }));
    notify({ event: 'event', data });

    mocks.forEach((mock) => expect(mock).toBeCalledWith(data));
  });
});
