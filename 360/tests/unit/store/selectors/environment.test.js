import {
    isIosSafariSelector,
    shouldShowYaPlus,
    getOsId
} from '../../../../components/redux/store/selectors/environment';

describe('environment selectors', () => {
    let state;
    beforeEach(() => {
        state = {
            page: {
                idContext: '/disk'
            },
            resources: { },
            environment: {
                agent: {
                    OSFamily: 'iOS',
                    BrowserBase: 'Safari'
                },
                session: { experiment: {}, locale: 'ru', tld: 'ru', region: { country_tld: 'ru' } }
            },
            user: { hasYaPlus: false, isWS: false, isPDD: false }
        };
    });

    describe('isIosSafariSelector', () => {
        it('Должен вернуть true, если зашли с iOS через Safari', () => {
            state.environment.agent.OSFamily = 'iOS';
            state.environment.agent.BrowserBase = 'Safari';
            expect(isIosSafariSelector(state)).toBe(true);
        });

        it('Должен вернуть false, если зашли не с iOS или не через Safari', () => {
            state.environment.agent.OSFamily = 'Windows';
            state.environment.agent.BrowserBase = 'Chrome';
            expect(isIosSafariSelector(state)).toBe(false);
        });
    });

    describe('shouldShowYaPlus', () => {
        it('Должен быть true на ru', () => {
            expect(shouldShowYaPlus(state)).toBe(true);
        });

        it('Должен быть true на kz', () => {
            state.environment.session.tld = 'kz';
            expect(shouldShowYaPlus(state)).toBe(true);
        });

        it('Должен быть false на kz с en локалью', () => {
            state.environment.session.tld = 'kz';
            state.environment.session.locale = 'en';
            expect(shouldShowYaPlus(state)).toBe(false);
        });

        it('Должен быть true на by', () => {
            state.environment.session.tld = 'by';
            expect(shouldShowYaPlus(state)).toBe(true);
        });

        it('Должен быть true на uz', () => {
            state.environment.session.tld = 'uz';
            expect(shouldShowYaPlus(state)).toBe(true);
        });

        it('Должен быть false на fr', () => {
            state.environment.session.tld = 'fr';
            expect(shouldShowYaPlus(state)).toBe(false);
        });

        it('Должен быть false на ru для ws пользователей', () => {
            state.user.isWS = true;
            expect(shouldShowYaPlus(state)).toBe(false);
        });

        it('Должен быть false на ru для ПДД пользователей', () => {
            state.user.isPDD = true;
            expect(shouldShowYaPlus(state)).toBe(false);
        });
    });

    describe('getOsId', () => {
        const ids = {
            android: {
                isMobile: true,
                id: 'android'
            },
            ios: {
                isMobile: true,
                id: 'ios'
            },
            windowsphone: {
                isMobile: true,
                id: 'winphone'
            },
            windows: {
                id: 'win'
            },
            macos: {
                id: 'mac'
            },
            linux: {
                id: 'linux'
            },
            blabla: {
                id: 'linux'
            }
        };

        for (const os in ids) {
            it(os, () => {
                expect(getOsId({ environment: { agent: { OSFamily: os, isMobile: ids[os].isMobile } } })).toEqual(ids[os].id);
            });
        }
    });
});
