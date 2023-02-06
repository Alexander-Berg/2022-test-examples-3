import { createEvent } from 'effector';
import { $status, STATUSES } from '../../store/status';
import { initMessageListener } from '../message';

const reset = createEvent();
const pause = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('status api', () => {
  beforeAll(() => {
    $status.reset(reset);
    initMessageListener();
  });

  afterAll(() => {
    $status.off(reset);
  });

  beforeEach(() => reset());

  test('should set status on valid payload', async () => {
    for (let status of STATUSES) {
      window.postMessage({ type: 'status', payload: status }, '*');

      // We need to wait because postMessage is async
      await pause();

      expect($status.getState()).toBe(status);
    }
  });

  test('should show error on invalid payload', async () => {
    const fn = jest.spyOn(global.console, 'error').mockImplementation();

    window.postMessage({ type: 'status', payload: 'invalid-status' }, '*');

    // We need to wait because postMessage is async
    await pause();

    expect(fn).toBeCalled();
  });
});
