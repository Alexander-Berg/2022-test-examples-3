import { getPromoBannerType } from '../../../../components/redux/store/selectors/promo';
import { getPage } from '../../../../components/redux/store/selectors/common';
import {
    ONE_DAY,
    DESKTOP_APP_CLOSED_BANNER_TIMEOUT,
    DESKTOP_APP_DOWNLOAD_ACTIVITY_TIMEOUT,
    B2C_BANNER_TIMEOUT_DEFAULT
} from '../../../../components/consts';

jest.mock('../../../../components/redux/store/selectors/common', () => ({
    getPage: jest.fn()
}));

describe('promo selectors -->', () => {
    describe('getPromoBannerType', () => {
        const getState = ({
            showWelcomePopupInSession = false,
            timestampLastClosedMobilePromo = '0',
            timestampLastClosedDesktopPromo = '0',
            timestampLastDownloadedDesktopPromo = '0',
            timestampLastB2CBannerShow = '0',
            timestampLastClosedB2CBanner = '0',
            isNeedToResetB2cBannerShowDuration = '0',
            shouldShowMobileAppPromoBanner = false,
            shouldShowDesktopSoftPromoBanner = false,
            shouldShowB2cBanner = false,
            showDocsInHeader = false,
            paid = false,
            experiment = {},
            serverTime = 1621959803531
        } = {}) => ({
            config: { environment: 'production' },
            settings: {
                showWelcomePopupInSession,
                timestampLastClosedMobilePromo,
                timestampLastClosedDesktopPromo,
                timestampLastDownloadedDesktopPromo,
                timestampLastB2CBannerShow,
                timestampLastClosedB2CBanner,
                isNeedToResetB2cBannerShowDuration
            },
            environment: {
                agent: { isMobile: false },
                session: {
                    shouldShowB2cBanner,
                    shouldShowMobileAppPromoBanner,
                    shouldShowDesktopSoftPromoBanner,
                    showDocsInHeader,
                    experiment,
                    serverTime
                }
            },
            user: { paid }
        });

        const mobileExperiment = {
            diskWebClientMobileTest: {
                closedBannerTimeout: ONE_DAY * 30,
            }
        };

        getPage.mockReturnValue('listing');

        it('Вернет undefined с параметрами по-умолчанию', () => {
            expect(getPromoBannerType(getState())).toBeUndefined();
        });

        it('Возвращает тип баннера, если открыта страница с файлами', () => {
            ['listing', 'albums'].forEach((page) => {
                getPage.mockReturnValueOnce(page);
                expect(getPromoBannerType(getState({
                    shouldShowDesktopSoftPromoBanner: true
                }))).toBe('desktop');
            });

            ['photo', 'tuning', 'error', 'journal', 'feed', 'album', 'remember-block'].forEach((page) => {
                getPage.mockReturnValueOnce(page);
                expect(getPromoBannerType(getState({
                    shouldShowDesktopSoftPromoBanner: true
                }))).toBeUndefined();
            });
        });

        it('Возвращает тип баннера, когда указан признак показа одного из промо-баннеров', () => {
            expect(getPromoBannerType(getState({
                shouldShowMobileAppPromoBanner: true,
                experiment: {
                    diskWebClientMobileTest: {
                        closedBannerTimeout: 1000 * 60 * 60 * 24 * 30 //30 дней
                    }
                }
            }))).toBe('mobile');
            expect(getPromoBannerType(getState({
                shouldShowDesktopSoftPromoBanner: true
            }))).toBe('desktop');
            expect(getPromoBannerType(getState({
                shouldShowB2cBanner: true
            }))).toBe('b2c-1');
        });

        it('Возвращает undefined, если в сессии не был показан welcome-popup', () => {
            expect(getPromoBannerType(getState({
                showWelcomePopupInSession: true
            }))).toBeUndefined();
        });

        it('Возвращает тип баннера, если интервал от указанного времени до текущего момента больше таймаута скрытия баннера', () => {
            Date.now = jest.fn(() => 10);

            // true, если переданное время последнего закрытия баннера равно 0
            expect(getPromoBannerType(getState({
                timestampLastClosedMobilePromo: '0',
                shouldShowMobileAppPromoBanner: true,
                experiment: mobileExperiment
            }))).toBe('mobile');

            Date.now = jest.fn(() => DESKTOP_APP_CLOSED_BANNER_TIMEOUT + 10);

            // true, тк интервал от указанного временени до текущего момента больше таймаута закрытия
            expect(getPromoBannerType(getState({
                timestampLastClosedMobilePromo: '5',
                shouldShowMobileAppPromoBanner: true,
                experiment: mobileExperiment
            }))).toBe('mobile');
            // false, тк интервал от указанного временени до текущего момента меньше таймаута закрытия
            expect(getPromoBannerType(getState({
                timestampLastClosedMobilePromo: '15',
                timestampLastDownloadedDesktopPromo: '0',
                shouldShowMobileAppPromoBanner: true,
                experiment: mobileExperiment
            }))).toBeUndefined();
        });

        it('Возвращает desktop, если интервал от указанного времени до текущего момента больше таймаута последней загрузки ПО', () => {
            Date.now = jest.fn(() => DESKTOP_APP_DOWNLOAD_ACTIVITY_TIMEOUT + 10);

            // true, тк интервал от указанного временени до текущего момента больше таймаута последней загрузки ПО
            expect(getPromoBannerType(getState({
                timestampLastDownloadedDesktopPromo: '5',
                shouldShowDesktopSoftPromoBanner: true
            }))).toBe('desktop');
            // false, тк интервал от указанного временени до текущего момента меньше таймаута последней загрузки ПО
            expect(getPromoBannerType(getState({
                timestampLastClosedDesktopPromo: '0',
                timestampLastDownloadedDesktopPromo: '15',
                shouldShowDesktopSoftPromoBanner: true
            }))).toBeUndefined();
        });

        describe('b2c баннер', () => {
            it('Вернем b2c баннер, если выполняются условия показа и его показывали больше 3 дней назад', () => {
                Date.now = jest.fn(() => B2C_BANNER_TIMEOUT_DEFAULT + 1);
                expect(getPromoBannerType(getState({
                    shouldShowB2cBanner: true,
                    serverTime: Date.now(),
                    timestampLastClosedB2CBanner: '0'
                }))).toBe('b2c-1');
            });

            it('Вернем undefined, если выполняются условия показа, но его показывали менее чем 3 дня назад', () => {
                Date.now = jest.fn(() => B2C_BANNER_TIMEOUT_DEFAULT - 2);
                expect(getPromoBannerType(getState({
                    shouldShowB2cBanner: true,
                    serverTime: Date.now(),
                    timestampLastClosedB2CBanner: '100'
                }))).toBeUndefined();
            });

            it('Вернем undefined, если не выполняются базовые условия показа баннера', () => {
                Date.now = jest.fn(() => B2C_BANNER_TIMEOUT_DEFAULT + 1);
                expect(getPromoBannerType(getState({
                    shouldShowB2cBanner: false,
                    serverTime: Date.now(),
                    timestampLastClosedB2CBanner: '0'
                }))).toBeUndefined();
            });
        });
    });
});
