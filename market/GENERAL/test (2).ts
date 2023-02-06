import { createApiObjectMock } from '@yandex-market/mbo-test-utils';

import Api from 'src/Api';
import { register } from '.';

export const api = createApiObjectMock<Api>({});

register('test', api);
