import * as React from 'react';
import { create, act, ReactTestRenderer } from 'react-test-renderer';

import { App } from '../../App';

const wait = (timeout) => new Promise((resolve) => setTimeout(resolve, timeout));

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('App', () => {
  test('should send postMessage `themer-ready` after initialization', async () => {
    const calledArgs: MessageEvent[] = [];
    const fn = jest.fn((arg: MessageEvent) => calledArgs.push(arg));
    window.addEventListener('message', fn);

    let root: ReactTestRenderer;
    act(() => {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      root = create(<App />);
    });

    // We need to wait because postMessage is async
    await wait(100);

    const result = calledArgs.find(({ data }) => data.type === 'themer-ready');

    expect(fn).toBeCalled();
    expect(result.type).toBe('themer-ready');
    expect(result).toHaveProperty('defaultValues');
    expect(result).toBe('formSpec');

    window.removeEventListener('message', fn);
  });
});
