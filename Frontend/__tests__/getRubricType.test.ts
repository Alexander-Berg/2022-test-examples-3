import { URL } from 'url';
import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { getRubricType } from '../getRubricType';

describe('getRubricType', () => {
  it('Выделяет рубрику из урла', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          url: {
            pathname: 'firstPart/secondPart/thirdPart',
          } as URL,
        },
      },
    });
    const rubricType = getRubricType(serverCtx);

    expect(rubricType).toBe('thirdPart');
  });
});
