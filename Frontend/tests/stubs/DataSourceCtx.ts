import { dataSourceCtxStub as dataSourceCtxStubMg } from 'mg/tests/stubs/contexts/DataSourceCtx';
import { IDataSourceCtx } from 'partners/types/contexts/IDataSourceCtx';
import partners from './data/partners.json';

export const dataSourceCtxStub = {
  ...dataSourceCtxStubMg,
  partners,
} as unknown as IDataSourceCtx;
