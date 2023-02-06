import { extendedFetch } from 'neo/lib/ajax/helpers/extendedFetch';
import { mockWindowFlags } from 'neo/tests/mocks/window';
import {
  jsonMock,
  mockFetch,
  mockFetchCalls,
  RESPONSE_NETWORK_ERROR,
  RESPONSE_OK,
  RESPONSE_OK_TEXT,
  RESPONSE_SERVER_ERROR,
} from 'neo/tests/mocks/fetch';
import * as delayModule from 'neo/lib/utils/delay';

jest.mock('neo/lib/utils/delay', () => ({
  delay: jest.fn().mockImplementation(() => Promise.resolve(setTimeout)),
}));

const delaySpy = jest.spyOn(delayModule, 'delay');

describe('extendedFetch', () => {
  beforeEach(() => {
    mockWindowFlags();
  });

  afterEach(() => {
    delaySpy.mockClear();
  });

  it('возвращает данные при успешном запросе', async() => {
    mockFetch(RESPONSE_OK);

    const response = await extendedFetch('__url__');

    expect(response.readedBody).toBe(jsonMock);
  });

  it('обрабатывает серверные ошибки', async() => {
    mockFetch(RESPONSE_SERVER_ERROR);

    await expect(extendedFetch('__url__')).rejects.toBe(RESPONSE_SERVER_ERROR);
  });

  it('ретраит серверные ошибки', async() => {
    mockFetchCalls([
      RESPONSE_SERVER_ERROR,
      RESPONSE_OK,
    ]);

    const response = await extendedFetch('__url__', {
      retry: 1,
    });

    expect(response.readedBody).toBe(jsonMock);
  });

  it('возвращает ошибку, если не удалось заретраить серверную ошибку', async() => {
    mockFetchCalls([
      RESPONSE_SERVER_ERROR,
      RESPONSE_SERVER_ERROR,
      RESPONSE_SERVER_ERROR,
      RESPONSE_OK,
    ]);

    await expect(extendedFetch('__url__', {
      retry: 2,
    })).rejects.toBe(RESPONSE_SERVER_ERROR);
  });

  it('бесконечно ретраит сетевые ошибки', async() => {
    mockFetchCalls([
      RESPONSE_NETWORK_ERROR,
      RESPONSE_NETWORK_ERROR,
      RESPONSE_NETWORK_ERROR,
      RESPONSE_OK,
    ]);

    const response = await extendedFetch('__url__', {
      retry: 1,
    });

    expect(response.readedBody).toBe(jsonMock);
  });

  it('обрабатывает таймаут', async() => {
    window.fetch = jest.fn().mockImplementation(() => (
      new Promise(() => null)
    ));

    await expect(extendedFetch('__url__')).rejects.toThrow('Request timeout');
  });

  it('делает одинаковые паузы между ретраями', async() => {
    mockFetchCalls([
      RESPONSE_SERVER_ERROR,
      RESPONSE_SERVER_ERROR,
      RESPONSE_OK,
    ]);

    await extendedFetch('__url__', {
      retry: 2,
      retryTimeout: 300,
      timeout: 400,
    });

    assertDelay(0, 400); // таймаут для запроса
    assertDelay(1, 300); // пауза перед ретраем 1
    assertDelay(2, 400); // таймаут для ретрая 1
    assertDelay(3, 300); // пауза перед ретраем 2
    assertDelay(4, 400); // таймаут для ретрая 2

    expect(delaySpy).toHaveBeenCalledTimes(5);
  });

  it('конвертирует параметры в строки', async() => {
    mockFetch(RESPONSE_OK);

    await extendedFetch('https://ya.ru', {
      params: {
        number: 1,
        numberList: [1, 2],
        stringList: ['foo', 'bar'],
      },
    });

    expect(window.fetch).toHaveBeenCalledWith(
      'https://ya.ru/?number=1&numberList=1%2C2&stringList=foo%2Cbar',
      expect.anything(),
    );
  });

  it('игнорирует пустые параметры', async() => {
    mockFetch(RESPONSE_OK);

    await extendedFetch('https://ya.ru', {
      params: {
        ignoreNull: null,
        ignoreUndefined: undefined,
        keepEmptyString: '',
      },
    });

    expect(window.fetch).toHaveBeenCalledWith(
      'https://ya.ru/?keepEmptyString=',
      expect.anything(),
    );
  });

  describe('Эксперимент с ретраями', () => {
    beforeEach(() => {
      mockWindowFlags({
        'yxneo_retry-fetch': '1',
      });
    });

    it('ретраит сетевые ошибки ограниченное количество раз', async() => {
      mockFetchCalls([
        RESPONSE_NETWORK_ERROR,
        RESPONSE_NETWORK_ERROR,
        RESPONSE_NETWORK_ERROR,
        RESPONSE_NETWORK_ERROR,
        RESPONSE_NETWORK_ERROR,
      ]);

      await expect(extendedFetch('__url__', {
        retry: 1,
      })).rejects.toBe(RESPONSE_NETWORK_ERROR);
    });

    it('увеличивает паузы между ретраями', async() => {
      mockFetchCalls([
        RESPONSE_SERVER_ERROR,
        RESPONSE_SERVER_ERROR,
        RESPONSE_SERVER_ERROR,
        RESPONSE_OK,
      ]);

      await extendedFetch('__url__', {
        retry: 3,
        retryTimeout: 300,
        timeout: 400,
      });

      assertDelay(0, 400); // таймаут для запроса
      assertDelay(1, 300 + (300 * 0 * 2)); // пауза перед ретраем 1

      assertDelay(2, 400); // таймаут для ретрая 1
      assertDelay(3, 300 + (300 * 1 * 2)); // пауза перед ретраем 2

      assertDelay(4, 400); // таймаут для ретрая 2
      assertDelay(5, 300 + (300 * 2 * 2));// пауза перед ретраем 3

      assertDelay(6, 400); // таймаут для ретрая 3

      expect(delaySpy).toHaveBeenCalledTimes(7);
    });
  });

  it('Обрабатывает ответ в виде текста', async() => {
    mockFetch(RESPONSE_OK_TEXT);

    const response = await extendedFetch('__url__', {
      responseContentType: 'text',
      method: 'post',
    });

    expect(response.readedBody).toBe('__responseBody__');
  });

  it('По умолчанию обрабатывает json', async() => {
    mockFetch(RESPONSE_OK);

    const response = await extendedFetch('__url__', { method: 'post' });

    expect(response.readedBody).toBe(jsonMock);
  });
});

function assertDelay(callNumber: number, timeMs: number) {
  expect(delaySpy.mock.calls[callNumber][0]).toBe(timeMs);
}
