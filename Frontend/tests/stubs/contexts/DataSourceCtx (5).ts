import { dataSourceCtxStub as dataSourceCtxStubMg } from 'mg/tests/stubs/contexts/DataSourceCtx';
import { IDataSourceCtx } from 'news/types/contexts/IDataSourceCtx';

export const dataSourceCtxStub: IDataSourceCtx = {
  ...dataSourceCtxStubMg,
  news: {
    isAppSearchHeader: false,
    favorites: {},
  },
} as unknown as IDataSourceCtx;
