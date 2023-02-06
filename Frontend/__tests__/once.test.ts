import { once } from '../once';

describe('once', () => {
  test('should call function only once', () => {
    const mock = jest.fn();
    const toCall = once(mock);

    Array(10).fill(toCall).forEach((callback) => callback());

    expect(mock).toBeCalledTimes(1);
  });
});
