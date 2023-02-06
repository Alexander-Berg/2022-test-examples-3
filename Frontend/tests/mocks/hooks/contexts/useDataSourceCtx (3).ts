import { IDataSourceCtx } from 'sport/types/contexts/IDataSourceCtx';
import { dataSourceCtxStub } from 'sport/tests/stubs/contexts/DataSourceCtx';

jest.doMock('neo/hooks/contexts/useDataSourceCtx', () => ({
  useDataSourceCtx: (): IDataSourceCtx => dataSourceCtxStub,
}));
