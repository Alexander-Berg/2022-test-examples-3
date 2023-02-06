import Token from '../token';
import { widgets } from '../../api/widgets';

import '../../typings/global.d';

describe('Token', () => {
  const id = 0;
  let token: Token;

  beforeEach(() => {
    token = new Token(id);
    widgets[id] = {
      token: undefined,
      reset: () => { },
      destroy: () => { },
      showError: () => { },
      subscribe: () => { return () => { } },
      execute: () => { },
      invisible: false,
    };
  });

  test('should save value to window.smartCaptcha.widgets[id]', () => {
    token.set('value');

    expect(widgets[id]?.token).toBe('value');
  });

  test("should warn user when 'set' or 'reset' token when it does not present window.smartCaptcha.widgets", () => {
    // @ts-ignore
    widgets[id] = undefined;

    expect(() => token.set('value')).toThrow(Error);
    expect(() => token.reset()).toThrow(Error);
  });

  test('should change value in the dom', () => {
    token.set('value');
    expect(token.container.value).toBe('value');

    token.reset();
    expect(token.container.value).toBe('');
  });

  test('should call callback on set', () => {
    const mockCallback = jest.fn();
    const token = new Token(id, mockCallback);

    token.set('value');

    expect(mockCallback).toHaveBeenCalledWith('value');
  });

  test('should call callback from window', () => {
    // @ts-expect-error
    window.mockCallback = jest.fn();
    const token = new Token(id, 'mockCallback');

    token.set('value');

    // @ts-expect-error
    expect(window.mockCallback).toHaveBeenCalledWith('value');
  });

  test('should warn user when string callback is not present in window', () => {
    global.console.warn = jest.fn();

    // @ts-ignore
    const _ = new Token(id, 'missingCallback');

    expect(global.console.warn).toHaveBeenCalled();
  });

  test('should destroy container on destroy call', () => {
    document.body.innerHTML = '<div id="container"></div>';

    const token = new Token(id, 'mockCallback');
    const container = document.getElementById('container');

    container?.appendChild(token.container);

    expect(container?.children.length).toBe(1);

    token.destroy();

    expect(container?.children.length).toBe(0);
  });
});
