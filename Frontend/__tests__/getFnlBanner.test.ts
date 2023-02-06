import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { getFnlBanner } from '../getFnlBanner';

describe('getFnlBanner', () => {
  it('Возвращает undefined, если флаг не передан', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxneo_sport_fnl-banner': '0',
          },
        },
      },
    });
    const result = getFnlBanner(serverCtx);

    expect(result).toBe(undefined);
  });

  it('Возвращает IBannerProps, если флаг включен', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxneo_sport_fnl-banner': '1',
          },
        },
      },
    });

    const result = getFnlBanner(serverCtx);
    expect(result).toEqual(
      {
        type: 'fnl',
        href: 'https://yandex.ru/sport/competition/28641',
      },
    );
  });
});

describe('isFnlBannerRubric', () => {
  beforeEach(() => {
    jest.resetModules();
    jest.dontMock('sport/types/apphost/active_rubric');
  });

  it('Возвращает false, если getActiveRubricId возвращает null', async() => {
    jest.doMock('sport/lib/rubric/getActiveRubricId', () => ({
      __esModule: true,
      getActiveRubricId: () => (null),
    }));
    const { isFnlBannerRubric } = await import('../getFnlBanner');
    const serverCtx = getServerCtxStub();
    const result = isFnlBannerRubric(serverCtx);

    expect(result).toBeFalsy();
  });

  it('Возвращает false, если getActiveRubricId возвращает rubricId, отличной от футбола', async() => {
    jest.doMock('sport/lib/rubric/getActiveRubricId', () => ({
      __esModule: true,
      getActiveRubricId: () => (412),
    }));

    const { isFnlBannerRubric } = await import('../getFnlBanner');
    const serverCtx = getServerCtxStub();
    const result = isFnlBannerRubric(serverCtx);

    expect(result).toBeFalsy();
  });

  it('Возвращает true, если getActiveRubricId возвращает rubricId футбола', async() => {
    jest.doMock('sport/lib/rubric/getActiveRubricId', () => ({
      __esModule: true,
      getActiveRubricId: () => (411),
    }));
    const { isFnlBannerRubric } = await import('../getFnlBanner');
    const serverCtx = getServerCtxStub();
    const result = isFnlBannerRubric(serverCtx);

    expect(result).toBeTruthy();
  });
});
