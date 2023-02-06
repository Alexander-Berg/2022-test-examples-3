import type { ApphostContext } from '@src/apphost';
import { ETheme } from '@src/store/services/internal/types';
import { Platform } from '@src/typings/platform';
import { getUserTheme, getClassesTheme } from './userTheme';
jest.mock('@src/typings/expflags', () => ({ FLAGS_WHITELIST: new Set(['dark_theme_touch', 'dark_theme_touch_default_pref']) }));

describe('getUserTheme', () => {
    const defaultContext = {
        platform: Platform.Touch,
        isYandexApp: false,
        cookies: { yp: 'fooobar' },
        device: { os: { family: 'iOS' } },
    } as unknown as ApphostContext;

    const updateContext = (context: object): ApphostContext => ({
        ...defaultContext,
        ...context,
    } as unknown as ApphostContext);

    it('по-умолчанию должна вернуться системная тема', () => {
        expect(getUserTheme(defaultContext, {})).toBe('system');
    });

    it('с флагом светлой темы должна вернуться светлая тема', () => {
        expect(getUserTheme(defaultContext, { dark_theme_touch: 'light' })).toBe('light');
    });

    it('с флагом темной темы должна вернуться темная тема', () => {
        expect(getUserTheme(defaultContext, { dark_theme_touch: 'dark' })).toBe('dark');
    });

    it('со значением флага system вернется системная тема', () => {
        expect(getUserTheme(defaultContext, { dark_theme_touch: 'system' })).toBe('system');
    });

    it('для ios в ПП вернется системная тема', () => {
        expect(getUserTheme(updateContext({ isYandexApp: true }), {})).toBe('system');
    });

    it('для android в ПП версии < 22.13 вернется светлая тема', () => {
        expect(getUserTheme(updateContext({
            isYandexApp: true,
            device: {
                os: { family: 'Android' },
                browser: { version: '22.12' },
            },
        }), {})).toBe('light');
    });

    it('для android в ПП версии >= 22.13 вернется системная тема', () => {
        const stub = {
            flagsJson: {},
            isYandexApp: true,
            device: {
                os: { family: 'Android' },
                browser: { version: '22.13' },
            },
        };

        expect(getUserTheme(updateContext(Object.assign({}, stub, {
            device: { browser: { version: '22.13' } },
        })), {})).toBe('system');

        expect(getUserTheme(updateContext(Object.assign({}, stub, {
            device: { browser: { version: '23' } },
        })), {})).toBe('system');
    });

    it('не в ПП и без куки темы вернется системная тема', () => {
        expect(getUserTheme(defaultContext, {})).toBe('system');
    });

    it('не в ПП и c кукой темы l вернется светлая тема', () => {
        expect(getUserTheme(updateContext({ cookies: { yp: 'fooobar#123.skin.l' } }), {})).toBe('light');
    });

    it('не в ПП и c кукой темы d вернется светлая тема', () => {
        expect(getUserTheme(updateContext({ cookies: { yp: 'fooobar#123.skin.d' } }), {})).toBe('dark');
    });

    it('не в ПП, без куки темы и с флагом dark_theme_touch_default_pref вернется тема из флага', () => {
        expect(getUserTheme(defaultContext, { dark_theme_touch_default_pref: 'dark' })).toBe('dark');
    });
});

describe('getClassesTheme', () => {
    it('с темой dark возвращает классы темной темы', () => {
        expect(getClassesTheme(ETheme.DARK)).toBe(
            'i-ua_skin_dark Theme Theme_root_default Theme_color_yandex-inverse',
        );
    });

    it('с темой light возвращает классы светлой темы', () => {
        expect(getClassesTheme(ETheme.LIGHT)).toBe(
            'i-ua_skin_light Theme Theme_root_default Theme_color_yandex-default',
        );
    });

    it('С темой system вернутся классы системной темы', () => {
        expect(getClassesTheme(ETheme.SYSTEM)).toBe(
            'i-ua_skin_system Theme Theme_root_default Theme_color_yandex-default',
        );
    });
});
