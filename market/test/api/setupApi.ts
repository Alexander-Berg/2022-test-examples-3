import { createApiObjectMock, MockPromiseExtension } from '@yandex-market/mbo-test-utils';
import { Api } from 'src/java/Api';

export const setupApi = (extension?: MockPromiseExtension) => {
  const api = createApiObjectMock<Api>({}, extension);
  return api;
};
