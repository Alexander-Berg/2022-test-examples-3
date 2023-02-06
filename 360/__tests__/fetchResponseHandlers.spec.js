import cookieRenew from '../utils/cookie-renew';
import {
  jsonResponseHandler,
  textResponseHandler,
  getResponseHandler
} from '../utils/fetchResponseHandlers';

jest.mock('../utils/cookie-renew');

describe('fetchResponseHandlers', () => {
  const text = Symbol();
  const json = Symbol();
  const fetchResponse = {
    json: jest.fn(),
    text: jest.fn()
  };

  beforeEach(() => {
    fetchResponse.json.mockReset().mockResolvedValue(json);
    fetchResponse.text.mockReset().mockResolvedValue(text);
  });

  describe('jsonResponseHandler', () => {
    test('должен возвращать результат работы метода json у ответа fetch', async () => {
      const result = await jsonResponseHandler(fetchResponse);

      expect(fetchResponse.json).toHaveBeenCalledTimes(1);
      expect(result).toBe(json);
    });

    test('должен вызывать cookieRenew', async () => {
      fetchResponse.json.mockResolvedValue({cookieRenew: true});
      await jsonResponseHandler(fetchResponse);

      expect(cookieRenew).toHaveBeenCalledTimes(1);
    });
  });

  describe('textResponseHandler', () => {
    test('должен возвращать результат работы метода text у ответа fetch', async () => {
      const result = await textResponseHandler(fetchResponse);

      expect(fetchResponse.text).toHaveBeenCalledTimes(1);
      expect(result).toBe(text);
    });
  });

  describe('getResponseHandler', () => {
    const responseMock = {
      headers: {
        get: jest.fn()
      }
    };

    beforeEach(() => {
      responseMock.headers.get.mockReset();
    });

    test('должен получать заголовки из response.headers', () => {
      getResponseHandler(responseMock);

      expect(responseMock.headers.get).toHaveBeenCalledTimes(1);
      expect(responseMock.headers.get).toHaveBeenCalledWith('content-type');
    });

    test('должен возвращать jsonResponseHandler, если пустой content-type', () => {
      responseMock.headers.get.mockReturnValue('');

      expect(getResponseHandler(responseMock)).toBe(jsonResponseHandler);
    });

    test('должен возвращать jsonResponseHandler, если не смогли распарсить content-type', () => {
      responseMock.headers.get.mockReturnValue('pewpew');

      expect(getResponseHandler(responseMock)).toBe(jsonResponseHandler);
    });

    test('должен возвращать jsonResponseHandler, если не знаем такого content-type', () => {
      responseMock.headers.get.mockReturnValue('pewpew/pewpew');

      expect(getResponseHandler(responseMock)).toBe(jsonResponseHandler);
    });

    test('должен возвращать textResponseHandler, если content-type = text/plain', () => {
      responseMock.headers.get.mockReturnValue('text/plain');

      expect(getResponseHandler(responseMock)).toBe(textResponseHandler);
    });
  });
});
