import { JqueryXMLHttpRequest, BackendError } from 'api/BackendError';
import { RumError } from './RumError';
import { CONNECTION_ERROR_MESSAGE, getMessageFromError } from './utils/getMessageFromError';
jest.mock('../RumProvider', () => {
  return {
    rumInstance: {
      logError: jest.fn(),
      ERROR_LEVEL: { ERROR: 'ERROR' },
    },
    getGlobalAdditional: jest.fn(() => ({
      user: { login: 'Bond', id: '007007' },
    })),
  };
});

const mockXHRConnectionError = {
  readyState: 0,
  status: 0,
} as JqueryXMLHttpRequest;

const mockXHRInternalError = {
  readyState: 4,
  status: 500,
  statusText: 'Error',
} as JqueryXMLHttpRequest;

const mockXHRWithResponseJSON = {
  readyState: 4,
  status: 500,
  statusText: 'Error',
  responseJSON: {
    responseStatus: {
      message: 'some message',
    },
  },
} as JqueryXMLHttpRequest;

describe('RumError', () => {
  let rumErrorInstance: RumError;
  let logErrorArgs = {} as {
    message: string;
    additional: { wid: string; user: { login: string; id: string } };
    source: string;
  };
  beforeEach(() => {
    rumErrorInstance = new RumError();
    rumErrorInstance.rumInstance.logError = jest.fn((info) => {
      logErrorArgs = info;
    });
  });
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('.logBackendError', () => {
    it('sends correct source', () => {
      const error = BackendError.createFromXhr(mockXHRConnectionError, {
        url: 'test/url?_wid=2343',
      });
      rumErrorInstance.logBackendError(error);
      expect(logErrorArgs.source).toEqual('backend');
    });
    it('sends correct _wid', () => {
      const error = BackendError.createFromXhr(mockXHRConnectionError, {
        url: 'test/url?_wid=2343',
      });
      rumErrorInstance.logBackendError(error);
      expect(logErrorArgs.additional.wid).toEqual('2343');
    });
    it('sends correct user id and login', () => {
      const error = BackendError.createFromXhr(mockXHRConnectionError, {
        url: 'test/url?_wid=2343',
      });
      rumErrorInstance.logBackendError(error);
      expect(logErrorArgs.additional['user.id']).toEqual('007007');
      expect(logErrorArgs.additional['user.login']).toEqual('Bond');
    });
    describe('when connection error', () => {
      it('calls Rum.logError with connection error message', () => {
        const error = BackendError.createFromXhr(mockXHRConnectionError, {
          url: 'test/url?_wid=2343',
        });
        rumErrorInstance.logBackendError(error);
        expect(logErrorArgs.message).toEqual(CONNECTION_ERROR_MESSAGE);
      });
    });
    describe('when response status not 200', () => {
      it('calls Rum.logError with error message from response status', () => {
        const error = BackendError.createFromXhr(mockXHRInternalError, {
          url: 'test/url?_wid=2343',
        });
        rumErrorInstance.logBackendError(error);
        expect(logErrorArgs.message).toEqual(getMessageFromError(error));
      });
    });
    describe('when response has responseJSON', () => {
      it('calls Rum.logError with error message from response JSON', () => {
        const error = BackendError.createFromXhr(mockXHRWithResponseJSON, {
          url: 'test/url?_wid=2343',
        });
        rumErrorInstance.logBackendError(error);
        expect(logErrorArgs.message).toEqual(getMessageFromError(error));
      });
    });
  });
  describe('.logFrontendError', () => {
    it('sends correct source', () => {
      const mockError = new Error('test error');
      rumErrorInstance.logFrontendError(mockError);
      expect(logErrorArgs.source).toEqual('frontend');
    });
    it('sends message from error', () => {
      const mockError = new Error('test error');
      rumErrorInstance.logFrontendError(mockError);
      expect(logErrorArgs.message).toEqual('FrontendError: test error');
    });
  });
});
