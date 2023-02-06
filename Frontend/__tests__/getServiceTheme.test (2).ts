import { ETheme } from 'neo/types/ETheme';
import { serverCtxStub } from 'neo/tests/stubs/contexts';
import { EService } from 'neo/types/EService';
import { EPlatform } from 'neo/types/EPlatform';
import { EOS, EBrowser } from 'neo/types/browser';
import { getServiceTheme } from '../getServiceTheme';

describe('getServiceTheme', () => {
  it('флаги не заданы', () => {
    const expected = getServiceTheme(serverCtxStub);

    expect(expected).toBe(ETheme.LIGHT);
  });

  it('тема из флага', () => {
    [ETheme.LIGHT, ETheme.DARK, ETheme.SYSTEM].forEach((theme) => {
      const expectedServiceTheme = getServiceTheme({
        neo: {
          ...serverCtxStub.neo,
          service: EService.SPORT,
          flags: {
            'yxneo_sport_service-theme': theme,
          },
        },
      });

      expect(expectedServiceTheme).toBe(theme);
    });
  });

  it('тема из флага по страницам и платформам', () => {
    [ETheme.LIGHT, ETheme.DARK, ETheme.SYSTEM].forEach((theme) => {
      const expectedServiceTheme = getServiceTheme({
        neo: {
          ...serverCtxStub.neo,
          service: EService.SPORT,
          platform: EPlatform.PHONE,
          page: 'story',
          flags: {
            'yxneo_sport_service-theme': ETheme.LIGHT,
            'yxneo_sport_phone_story_service-theme': theme,
          },
        },
      });

      expect(expectedServiceTheme).toBe(theme);
    });
  });

  it('флаг темы: "auto", значение берётся из cookies', () => {
    const cookies: Record<string, ETheme> = {
      l: ETheme.LIGHT,
      d: ETheme.DARK,
      s: ETheme.SYSTEM,
    };

    Object.keys(cookies).forEach((value) => {
      const expectedServiceTheme = getServiceTheme({
        neo: {
          ...serverCtxStub.neo,
          service: EService.SPORT,
          flags: {
            'yxneo_sport_service-theme': 'auto',
          },
          ycookie: {
            yp: {
              skin: {
                value,
              },
            },
          },
        },
      });

      expect(expectedServiceTheme).toBe(cookies[value]);
    });
  });

  it('флаг темы: "auto", дефолтное значение: "system"', () => {
    const expectedServiceTheme = getServiceTheme({
      neo: {
        ...serverCtxStub.neo,
        service: EService.SPORT,
        flags: {
          'yxneo_sport_service-theme': 'auto',
        },
      },
    });

    expect(expectedServiceTheme).toBe(ETheme.SYSTEM);
  });

  it('флаг темы: "auto", дефолтное значение задаётся флагом', () => {
    [ETheme.LIGHT, ETheme.DARK, ETheme.SYSTEM].forEach((theme) => {
      const expectedServiceTheme = getServiceTheme({
        neo: {
          ...serverCtxStub.neo,
          service: EService.SPORT,
          flags: {
            'yxneo_sport_service-theme': 'auto',
            'yxneo_sport_service-theme-default': theme,
          },
        },
      });

      expect(expectedServiceTheme).toBe(theme);
    });
  });

  it('флаг темы: "auto" в ПП на android', () => {
    const expectedServiceTheme = getServiceTheme({
      neo: {
        ...serverCtxStub.neo,
        service: EService.SPORT,
        flags: {
          'yxneo_sport_service-theme': 'auto',
        },
        browserInfo: {
          browserBase: {},
          browser: {
            name: EBrowser.yandexSearch,
          },
          os: {
            family: EOS.android,
          },
        },
      },
    });

    expect(expectedServiceTheme).toBe(ETheme.LIGHT);
  });

  it('флаг темы: "auto" в ПП на ios', () => {
    const expectedServiceTheme = getServiceTheme({
      neo: {
        ...serverCtxStub.neo,
        service: EService.SPORT,
        flags: {
          'yxneo_sport_service-theme': 'auto',
        },
        browserInfo: {
          browserBase: {},
          browser: {
            name: EBrowser.yandexSearch,
          },
          os: {
            family: EOS.ios,
          },
        },
      },
    });

    expect(expectedServiceTheme).toBe(ETheme.SYSTEM);
  });
});
