import { convertError, DEFAULT_ERROR_MESSAGE } from './commonErrorHandlers';

const apiJsonErrorText = 'json error message from back';
const apiJsonError = {
  headers: new Map([['content-type', 'application/json']]),
  json: () =>
    Promise.resolve({
      message: apiJsonErrorText,
    }),
};

const apiTextErrorText = 'test error message from back';
const apiJTextError = {
  headers: new Map(),
  text: () => Promise.resolve(apiTextErrorText),
};

const nativeErrorText = 'error is undefined';
const nativeError = new Error(nativeErrorText);

describe('commonErrorHandlers', () => {
  test('convertError api json error', async () => {
    const convertedError = await convertError(apiJsonError);
    expect(convertedError).toBe(apiJsonErrorText);
  });

  test('convertError api text error', async () => {
    const convertedError = await convertError(apiJTextError);
    expect(convertedError).toBe(apiTextErrorText);
  });

  test('convertError native error', async () => {
    const convertedError = await convertError(nativeError);
    expect(convertedError).toBe(nativeErrorText);
  });

  test('convertError undefined error', async () => {
    const convertedError = await convertError(undefined);
    expect(convertedError).toBe(DEFAULT_ERROR_MESSAGE);
  });
});
