import { IDataSourceCtx } from 'mg/types/contexts';
import { dataSourceCtxStub as dataSourceCtxStubNeo } from 'neo/tests/stubs/contexts';

export const dataSourceCtxStub: IDataSourceCtx = {
  neo: dataSourceCtxStubNeo.neo,
  mg: {
    formKey: '',
    apiBaseUrl: 'apiBaseUrl',
    newsApiBaseUrl: 'newsApiBaseUrl',
    subscribeApiBaseUrl: 'subscribeApiBaseUrl',
    user: {
      uid: {
        value: '1',
      },
      display_name: {
        name: 'Vasya Pupkin',
        avatar: {
          default: '30955/3bTLrehP1DqidKlDYBXVIl4c-1',
          empty: false,
        },
      },
    },
    isVertical: false,
  },
};
