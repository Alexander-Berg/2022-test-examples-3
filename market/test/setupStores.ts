import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import Api from 'src/Api';

import { createReatomStore } from 'src/store/reatom/createReatomStore';

export const setupTestStore = (api: MockedApiObject<Api & {}>) => {
  const reatomStore = createReatomStore(api);

  return { reatomStore };
};
