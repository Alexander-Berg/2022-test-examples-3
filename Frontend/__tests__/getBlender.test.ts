import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { getBlender } from '../getBlender';

describe('getBlender', () => {
  it('возвращает дополнительные данные', () => {
    const serverCtx = getServerCtxStub({
      additionalItemMap: { 'sport-blender': { data: 'test' } },
    });
    const result = getBlender(serverCtx);

    expect(result).toBe('test');
  });

  it('дополнительных данных нет', () => {
    const serverCtx = getServerCtxStub({
      additionalItemMap: { 'sport-blender': {} },
    });
    const result = getBlender(serverCtx);

    expect(result).toStrictEqual([]);
  });
});
