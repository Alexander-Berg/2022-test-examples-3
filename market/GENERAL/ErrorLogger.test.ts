import { ErrorLogger } from '.';

const error = { message: 'error' };
const additional = { reqId: 123 };

describe('ErrorLogger', () => {
  test('filter errors', () => {
    const selector = jest.fn();
    const sendError = jest.fn();
    const provider = { sendError };

    const errorLogger = ErrorLogger.getDefaultInstance(provider, true);
    errorLogger.addErrorSelectors(selector);
    errorLogger.logException(error);

    expect(selector.mock.calls).toHaveLength(1);
    expect(sendError.mock.calls).toHaveLength(0);
  });

  test('sendError', () => {
    const sendError = jest.fn((er, add) => {
      expect(er).toEqual(error);
      expect(add).toEqual(additional);
    });

    const provider = { sendError };

    const errorLogger = ErrorLogger.getDefaultInstance(provider, true);
    errorLogger.addErrorSelectors(() => true);
    errorLogger.logException(error, additional);

    expect(sendError.mock.calls).toHaveLength(1);
  });

  test('describeOnError', () => {
    const describer = jest.fn(() => additional);

    const sendError = jest.fn((er, add) => {
      expect(er).toEqual(error);
      expect(add).toEqual(additional);
    });

    const provider = { sendError };

    const errorLogger = ErrorLogger.getDefaultInstance(provider, true);
    errorLogger.addErrorSelectors(() => true);
    errorLogger.describeOnError(describer);
    errorLogger.logException(error);

    expect(describer.mock.calls).toHaveLength(1);
    expect(sendError.mock.calls).toHaveLength(1);
  });
});
