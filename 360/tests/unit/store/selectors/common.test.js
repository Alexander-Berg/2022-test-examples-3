import '../../noscript';

import { shouldShowWelcomePopup } from '../../../../components/redux/store/selectors/common';

describe('common selectors -->', () => {
    describe('shouldShowWelcomePopup', () => {
        const getState = ({
            isPDD = false,
            justInitialized = false,
            devices = {},
            OSFamily = 'Windows',
            isMobile = false,
            shouldShowWelcomePopupDueInactiveApp = false,
            timestampLastDisplayedDialogWelcome = '0',
            countDisplaysDialogWelcome = '0'
        } = {}) => ({
            config: { isDocsHost: false },
            user: {
                isPDD,
                justInitialized,
                devices
            },
            environment: {
                agent: { OSFamily, isMobile },
                session: { experiment: {}, shouldShowWelcomePopupDueInactiveApp }
            },
            settings: { timestampLastDisplayedDialogWelcome, countDisplaysDialogWelcome }
        });

        const getParams = ({
            idContext = '',
            dialog = '',
            reduxDialog = '',
            hidePromo = false,
            page = ''
        } = {}) => ({
            idContext,
            dialog,
            'hide-promo': hidePromo,
            reduxDialog,
            page
        });

        it('Вернет true с параметрами по-умолчанию', () => {
            expect(shouldShowWelcomePopup(
                getState(),
                getParams()
            )).toBe(true);
        });

        it('Вернет false, если пользователь Диска для домена пришел с линукса', () => {
            expect(shouldShowWelcomePopup(
                getState({ isPDD: true, OSFamily: 'Linux' }),
                getParams()
            )).toBe(false);
        });

        it('Вернет false, если пользователь зашел с тач-устройства', () => {
            expect(shouldShowWelcomePopup(
                getState({ isMobile: true }),
                getParams()
            )).toBe(false);
        });

        it('Вернет false, если в url передан непустой параметр hide-promo', () => {
            expect(shouldShowWelcomePopup(
                getState(),
                getParams({ hidePromo: true })
            )).toBe(false);
        });

        it('Вернет false, если в url передан непустой параметр dialog', () => {
            expect(shouldShowWelcomePopup(
                getState(),
                getParams({ dialog: '123' })
            )).toBe(false);
        });

        it('Вернет false, если в url передан непустой параметр reduxDialog', () => {
            expect(shouldShowWelcomePopup(
                getState(),
                getParams({ reduxDialog: '123' })
            )).toBe(false);
        });

        it('Вернет false, если в url параметр idContext равен /error', () => {
            expect(shouldShowWelcomePopup(
                getState(),
                getParams({ idContext: '/error' })
            )).toBe(false);
        });

        it('Вернет false, если в url параметр page равен tuning', () => {
            expect(shouldShowWelcomePopup(
                getState(),
                getParams({ page: 'tuning' })
            )).toBe(false);
        });

        it('Вернет true, если пользователь впервые вошел в Диск', () => {
            expect(shouldShowWelcomePopup(
                getState({ justInitialized: true, devices: { desktop: true } }),
                getParams()
            )).toBe(true);
        });

        it('Вернет true, если у пользователя не было активности >= 180 дней в ПО', () => {
            expect(shouldShowWelcomePopup(
                getState({ shouldShowWelcomePopupDueInactiveApp: true, devices: { desktop: true } }),
                getParams()
            )).toBe(true);
        });

        it('Вернет true, если у старого пользователя нет ПО', () => {
            expect(shouldShowWelcomePopup(
                getState({
                    justInitialized: false,
                    devices: { desktop: false },
                    countDisplayedWelcome: '0'
                }),
                getParams()
            )).toBe(true);
        });

        it('Вернет false, если общее количество показов welcome-попапа больше 3', () => {
            expect(shouldShowWelcomePopup(
                getState({
                    justInitialized: false,
                    devices: { desktop: true },
                    countDisplayedWelcome: '10'
                }),
                getParams()
            )).toBe(false);
        });

        it('Вернет false, если попап был показан за последние сутки', () => {
            Date.now = jest.fn(() => 2);

            expect(shouldShowWelcomePopup(
                getState({
                    justInitialized: false,
                    devices: { desktop: true },
                    countDisplayedWelcome: '0',
                    timestampLastDisplayedDialogWelcome: '1'
                }),
                getParams()
            )).toBe(false);
        });
    });
});
