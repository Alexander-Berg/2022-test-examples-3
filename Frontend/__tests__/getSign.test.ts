import { getSign } from 'mg/lib/getSign';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';

test('getSign', () => {
  process.env.YNEWS_API_SIGN_KEY = 'q1w2e3r4t5ey6u7u7i8o9p0q1w2e3r4t5';
  const serverCtx = getServerCtxStub({
    specialArgs: {
      neo: {
        timestamp: Number(new Date('2020-11-16T15:26:28')),
      },
    },
  });

  expect(getSign(serverCtx, '/api/v2/regions?alias=abroad')).toEqual('50f0ef7f83318f032675e67392a242e1');
});
