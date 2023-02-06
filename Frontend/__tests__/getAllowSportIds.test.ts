import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { getAllowSportIds } from '../getAllowSportIds';

describe('getAllowSportIds', () => {
  beforeEach(() => {
    jest.resetModules();
  });

  it('Должен возвращать пустой массив, если нет флага yxnerpa_sport_enable-match-rubric', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: { flags: { 'yxnerpa_sport_enable-match-rubric': '0' } },
      },
    });

    const allowSportIds = getAllowSportIds(serverCtx);
    expect(allowSportIds).toEqual([]);
  });

  it('Должен отфильтровывать по ESport айдишники, переданные во флаге yxnerpa_sport_enable-match-rubric', async() => {
    enum ESport {
      FOO = 111,
      BAR = 222,
    }

    jest.doMock('sport/types/sport', () => ({
      __esModule: true,
      ESport: ESport,
      SPORT_ORDER: [ESport.FOO, ESport.BAR],
    }));

    const { getAllowSportIds } = await import('../getAllowSportIds');

    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: { flags: { 'yxnerpa_sport_enable-match-rubric': `1,2,${ESport.FOO},${ESport.BAR}` } },
      },
    });

    const allowSportIds = getAllowSportIds(serverCtx);
    expect(allowSportIds).toEqual([ESport.FOO, ESport.BAR]);
  });

  it('Должен отдавать из SPORT_ORDER только те id, что есть во флаге', async() => {
    enum ESport {
      FOO = 111,
      BAR = 222,
      BAZ = 333
    }

    jest.doMock('sport/types/sport', () => ({
      __esModule: true,
      ESport: ESport,
      SPORT_ORDER: [ESport.FOO, ESport.BAR, ESport.BAZ],
    }));

    const { getAllowSportIds } = await import('../getAllowSportIds');

    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: { flags: { 'yxnerpa_sport_enable-match-rubric': `1,2,${ESport.FOO}` } },
      },
    });

    const allowSportIds = getAllowSportIds(serverCtx);
    expect(allowSportIds).toEqual([ESport.FOO]);
  });
});
