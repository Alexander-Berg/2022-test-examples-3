import { createEvent } from 'effector';
import { $tokens, updateToken, updateTokens, updateTokensJSON } from '../tokens';

const reset = createEvent();

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

describe('tokens', () => {
  beforeAll(() => {
    $tokens.reset(reset);
  });

  afterAll(() => {
    $tokens.off(reset);
  });

  beforeEach(() => reset());

  test('each token should have name, defaultValue and value fields', () => {
    Object.values($tokens.getState()).forEach(({ name, defaultValue, value }) => {
      expect(typeof name).toBe('string');
      expect(typeof defaultValue).toBe('string');
      expect(typeof value).toBe('string');

      expect(value).toBe(defaultValue);
    });
  });

  test('should update token', () => {
    updateToken({ name: 'token1', value: 'test' });

    expect($tokens.getState().token1.value).toBe('test');
  });

  test('should update multiple tokens', () => {
    const changes = [
      { name: 'token1', value: 'test1' },
      { name: 'token2', value: 'test2' },
      { name: 'token3', value: 'test3' },
    ];

    updateTokens(changes);

    expect($tokens.getState().token1.value).toBe('test1');
    expect($tokens.getState().token2.value).toBe('test2');
    expect($tokens.getState().token3.value).toBe('test3');
  });

  test('should update tokens using json', () => {
    const json = {
      token1: 'test1',
      token2: 'test2',
      token3: 'test3',
    };

    updateTokensJSON(json);

    expect($tokens.getState().token1.value).toBe('test1');
    expect($tokens.getState().token2.value).toBe('test2');
    expect($tokens.getState().token3.value).toBe('test3');
  });

  test('should preserve previous changes on updateToken', () => {
    updateToken({ name: 'token1', value: 'test1' });
    updateToken({ name: 'token2', value: 'test2' });

    expect($tokens.getState().token1.value).toBe('test1');
    expect($tokens.getState().token2.value).toBe('test2');
  });

  test('should preserve previous changes on updateTokens', () => {
    updateTokens([{ name: 'token1', value: 'test1' }]);
    updateTokens([{ name: 'token2', value: 'test2' }]);

    expect($tokens.getState().token1.value).toBe('test1');
    expect($tokens.getState().token2.value).toBe('test2');
  });

  test('should use default values when they are not present in json in updateTokensJSON', () => {
    let json: Record<string, string> = {
      token1: 'test1',
    };

    updateTokensJSON(json);

    expect($tokens.getState().token1.value).toBe('test1');
    expect($tokens.getState().token2.value).toBe('token2-default-value');

    json = {
      token2: 'test2',
    };

    updateTokensJSON(json);

    expect($tokens.getState().token1.value).toBe('token1-default-value');
    expect($tokens.getState().token2.value).toBe('test2');
  });

  test('should console.error message when token is not present', () => {
    const fn = jest.spyOn(global.console, 'error').mockImplementation();

    updateToken({ name: 'non-existent-token', value: 'value' });
    updateTokens([{ name: 'non-existent-token', value: 'value' }]);
    updateTokensJSON({ 'non-existent-token': 'value' });

    expect(fn).toBeCalledTimes(3);
  });
});
