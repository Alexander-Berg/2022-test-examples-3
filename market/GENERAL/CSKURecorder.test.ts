import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { CSKURecorder } from './CSKURecorder';
import { setupApi } from 'src/test/api/setupApi';
import { Api } from 'src/java/Api';

describe('CSKURecorder', () => {
  let recorder: CSKURecorder;
  let api: MockedApiObject<Api>;

  beforeAll(() => {
    api = setupApi() as MockedApiObject<Api>;
    recorder = new CSKURecorder(1, api);
  });

  test('Отправляем запрос пачками', async () => {
    jest.useFakeTimers();
    // первая пачка
    [1, 2, 3].forEach(el => {
      recorder.record(el);
    });

    // форсирует таймаут
    jest.runOnlyPendingTimers();

    // вторая пачка
    [4, 5, 6].forEach(el => {
      recorder.record(el);
    });

    // игнорируется так как такие скю уже были загружены
    [1, 2, 3].forEach(el => {
      recorder.record(el);
    });

    jest.runOnlyPendingTimers();

    api.shopModelController.collectMetricsForCSKU.next().resolve();

    expect(api.shopModelController.collectMetricsForCSKU).toBeCalledTimes(2);
  });
});
