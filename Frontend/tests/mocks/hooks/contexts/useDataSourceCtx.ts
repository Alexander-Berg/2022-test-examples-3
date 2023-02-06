import { IDataSourceCtx } from 'neo/types/contexts';
import { dataSourceCtxStub } from 'neo/tests/stubs/contexts';

jest.doMock('neo/hooks/contexts/useDataSourceCtx', () => ({
  useDataSourceCtx: (): IDataSourceCtx => dataSourceCtxStub,
}));
