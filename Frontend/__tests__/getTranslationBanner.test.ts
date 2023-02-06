import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { getTranslationBanner } from '../getTranslationBanner';

describe('getTranslationBanner', () => {
  it('Возвращает undefined, если флаг yxneo_sport_translation-banner выключен', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxneo_sport_translation-banner': '0',
          },
        },
      },
    });

    const result = getTranslationBanner(serverCtx);
    expect(result).toBe(undefined);
  });

  it('Возвращает IBannerProps, если флаг yxneo_sport_translation-banner включен', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxneo_sport_translation-banner': '1',
          },
        },
      },
    });

    const result = getTranslationBanner(serverCtx);
    expect(result).toEqual(
      {
        type: 'translation',
        href: 'https://yandex.ru/sport/live?utm_source=white_sport&utm_medium=selfpromo&utm_campaign=MSCAMP-287%7Csport',
      },
    );
  });
});
