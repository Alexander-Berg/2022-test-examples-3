import { createEvent } from 'effector';
import { $tokens } from '../../store/tokens';
import { initMessageListener } from '../message';

// Default value of $tokens store
jest.mock('@yandex-int/captcha/variables.json', () => {
  return {
    token1: {
      name: '--token-css-1',
      defaultValue: 'token1-default-value',
      value: 'token1-default-value',
      comment: 'token 1 comment',
      type: 'text',
    },
    token2: {
      name: '--token-css-2',
      defaultValue: 'token2-default-value',
      value: 'token2-default-value',
      comment: 'token 2 comment',
      type: 'text',
    },
    token3: {
      name: '--token-css-3',
      defaultValue: 'token3-default-value',
      value: 'token3-default-value',
      comment: 'token 3 comment',
      type: 'text',
    },
  };
});

const reset = createEvent();
const pause = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('message api', () => {
  beforeAll(() => {
    $tokens.reset(reset);
    initMessageListener();
  });

  afterAll(() => {
    $tokens.off(reset);
  });

  beforeEach(() => reset());

  test('should set single token on valid payload', async () => {
    window.postMessage({ type: 'single', payload: { name: 'token1', value: 'test' } }, '*');

    // We need to wait because postMessage is async
    await pause();

    expect($tokens.getState().token1.value).toBe('test');
  });

  test('should set multiple tokens on valid payload', async () => {
    window.postMessage({
      type: 'multiple', payload: [
        { name: 'token1', value: 'test1' },
        { name: 'token2', value: 'test2' },
      ],
    }, '*');

    // We need to wait because postMessage is async
    await pause();

    expect($tokens.getState().token1.value).toBe('test1');
    expect($tokens.getState().token2.value).toBe('test2');
  });

  test('should set tokens from json on valid payload', async () => {
    window.postMessage({
      type: 'json', payload: {
        token1: 'test1',
        token2: 'test2',
      },
    }, '*');

    // We need to wait because postMessage is async
    await pause();

    expect($tokens.getState().token1.value).toBe('test1');
    expect($tokens.getState().token2.value).toBe('test2');
  });

  test('should show error on invalid payload', async () => {
    const fn = jest.spyOn(global.console, 'error').mockImplementation();

    window.postMessage({ type: 'single' }, '*');
    window.postMessage({ type: 'single', payload: 'test1' }, '*');
    window.postMessage({ type: 'single', payload: { name: 'token1' } }, '*');
    window.postMessage({ type: 'single', payload: { value: 'test1' } }, '*');

    window.postMessage({ type: 'multiple' }, '*');
    window.postMessage({ type: 'multiple', payload: 'test2' }, '*');
    window.postMessage({ type: 'multiple', payload: { name: 'token1' } }, '*');
    window.postMessage({ type: 'multiple', payload: { value: 'test2' } }, '*');

    window.postMessage({ type: 'json' }, '*');
    window.postMessage({ type: 'json', payload: null }, '*');
    window.postMessage({ type: 'json', payload: 'test2' }, '*');
    window.postMessage({ type: 'json', payload: { name: { value: 'token1' } } }, '*');

    // We need to wait because postMessage is async
    await pause();

    expect(fn).toBeCalledTimes(12);
  });
});
