import { IDataSourceCtx } from 'news/types/contexts/IDataSourceCtx';
import { dataSourceCtxStub } from 'news/tests/stubs/contexts/DataSourceCtx';

jest.doMock('neo/hooks/contexts/useDataSourceCtx', () => ({
  useDataSourceCtx: (): IDataSourceCtx => dataSourceCtxStub,
}));
