import { dataSourceCtxStub as dataSourceCtxStubMg } from 'mg/tests/stubs/contexts/DataSourceCtx';
import { IDataSourceCtx } from 'sport/types/contexts/IDataSourceCtx';

export const dataSourceCtxStub: IDataSourceCtx = {
  ...dataSourceCtxStubMg,
  sport: {
    currentTimestamp: 1590573235493,
    timezone: 5,
  },
} as unknown as IDataSourceCtx;
