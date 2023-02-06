import { subscribe } from '../subscribe';
import { getWidget, flush } from './shared';

import '../../typings/global.d';

describe('subscribe', () => {
  beforeEach(flush);

  test('should call subscribers on notify call', () => {
    const { widget } = getWidget();
    const mocks = [
      jest.fn(),
      jest.fn(),
      jest.fn(),
      jest.fn(),
    ];

    subscribe(widget.id, 'challenge-visible', mocks[0]);
    subscribe(widget.id, 'challenge-hidden', mocks[1]);
    subscribe(widget.id, 'network-error', mocks[2]);
    subscribe(widget.id, 'success', mocks[3]);

    widget.notify({ event: 'challenge-visible' });
    widget.notify({ event: 'challenge-hidden' });
    widget.notify({ event: 'network-error' });
    widget.notify({ event: 'success', data: 'test' });

    mocks.forEach((mock) => {
      expect(mock).toBeCalled();
    });
  });

  test('should call callback with data', () => {
    const { widget } = getWidget();
    const mock = jest.fn();

    subscribe(widget.id, 'success', mock);
    widget.notify({ event: 'success', data: 'test' });

    expect(mock).toBeCalledWith('test');
  });

  test('should remove subscription on unsubscribe call', () => {
    const { widget } = getWidget();
    const mock = jest.fn();

    const unsubscribe = subscribe(widget.id, 'challenge-visible', mock);

    unsubscribe();

    widget.notify({ event: 'challenge-visible' });

    expect(mock).not.toBeCalled();
  });
});
