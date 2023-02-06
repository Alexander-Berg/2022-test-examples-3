import { createEvent } from 'effector';
import { $complexity, COMPLEXITIES } from '../../store/complexity';
import { initMessageListener } from '../message';

const reset = createEvent();
const pause = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('complexity api', () => {
  beforeAll(() => {
    $complexity.reset(reset);
    initMessageListener();
  });

  afterAll(() => {
    $complexity.off(reset);
  });

  beforeEach(() => reset());

  test('should set complexity on valid payload', async () => {
    for (let complexity of COMPLEXITIES) {
      window.postMessage({ type: 'complexity', payload: complexity }, '*');

      // We need to wait because postMessage is async
      await pause();

      expect($complexity.getState()).toBe(complexity);
    }
  });

  test('should show error on invalid payload', async () => {
    const fn = jest.spyOn(global.console, 'error').mockImplementation();

    window.postMessage({ type: 'complexity', payload: 'invalid-complexity' }, '*');

    // We need to wait because postMessage is async
    await pause();

    expect(fn).toBeCalled();
  });
});
