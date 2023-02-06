import { BackendError } from '../BackendError';
import { JqueryXMLHttpRequest } from '../types';

describe('BackendError', () => {
  describe('xhrToText', () => {
    it('should correct parse not internet connection', () => {
      expect(BackendError.xhrToText({ readyState: 0 } as JqueryXMLHttpRequest)).toBe(
        'Internet connection error',
      );
    });

    it('should correct parse responseStatus message', () => {
      expect(
        BackendError.xhrToText({
          statusText: 'CRM Error',
          response: '',
          responseJSON: { responseStatus: { message: 'responseStatus' }, message: 'responseJSON' },
        } as JqueryXMLHttpRequest),
      ).toBe('При загрузке данных произошла ошибка (CRM Error).\n\nresponseStatus');
    });

    it('should correct parse response', () => {
      expect(
        BackendError.xhrToText({
          statusText: 'CRM Error',
          response: 'response',
        } as JqueryXMLHttpRequest),
      ).toBe('response');
    });

    it('should correct parse root message', () => {
      expect(
        BackendError.xhrToText({
          response: { statusText: 'CRM Error' },
          responseJSON: { message: 'responseJSON' },
        } as JqueryXMLHttpRequest),
      ).toBe('responseJSON');
    });
  });

  it('should create by createFromXhr', () => {
    const props = {
      status: 500,
      response: { statusText: 'CRM Error' },
      responseJSON: { message: 'responseJSON' },
    } as JqueryXMLHttpRequest;

    const error = BackendError.createFromXhr(props);

    expect(error).toBeInstanceOf(BackendError);
    expect(error.status).toBe(500);
    expect(error.message).toBe(BackendError.xhrToText(props));
    expect(error.data).toBe(props.responseJSON);
  });

  it('should create with no error', () => {
    const error = new BackendError({
      status: 500,
      statusText: 'Forbidden',
      message: 'message',
      data: 'data',
      code: 'code',
    });

    expect(error).toBeInstanceOf(BackendError);
    expect(error.status).toBe(500);
    expect(error.statusText).toBe('Forbidden');
    expect(error.message).toBe('message');
    expect(error.data).toBe('data');
    expect(error.code).toBe('code');
  });
});
