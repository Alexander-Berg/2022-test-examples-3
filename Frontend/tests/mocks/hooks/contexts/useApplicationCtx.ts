import { IApplicationCtx } from 'neo/types/contexts';
import { applicationCtxStub } from 'neo/tests/stubs/contexts';

jest.doMock('neo/hooks/contexts/useApplicationCtx', () => ({
  useApplicationCtx: (): IApplicationCtx => applicationCtxStub,
}));
