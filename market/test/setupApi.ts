import { createApiObjectMock, MockPromiseExtension } from '@yandex-market/mbo-test-utils';
import Api from 'src/Api';

export const setupApi = (extension?: MockPromiseExtension) => createApiObjectMock<Api>(extension);
