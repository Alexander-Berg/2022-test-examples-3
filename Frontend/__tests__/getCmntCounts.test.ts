import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { getCmntCounts } from '../getCmntCounts';

describe('CmntCounts', () => {
  it('возвращает дополнительные данные в feed', () => {
    const serverCtx = getServerCtxStub({
      additionalItemMap: {
        cmnt_response: {
          payload: {
            feed: {
              test: { count: 5 },
            },
          },
        },
      },
    });
    const result = getCmntCounts(serverCtx.neo.ctxRR);

    expect(result).toStrictEqual({ test: 5 });
  });

  it('возвращает пустой feed', () => {
    const serverCtx = getServerCtxStub({
      additionalItemMap: { cmnt_response: {} },
    });
    const result = getCmntCounts(serverCtx.neo.ctxRR);

    expect(result).toStrictEqual({});
  });
});
