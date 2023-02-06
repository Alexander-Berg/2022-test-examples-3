import { IServerCtx } from 'neo/types/contexts';
import { serverCtxStub } from 'neo/tests/stubs/contexts';

jest.doMock('neo/hooks/contexts/useServerCtx', () => ({
  useServerCtx: (): IServerCtx => serverCtxStub,
}));
