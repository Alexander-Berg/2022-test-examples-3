describe('pp/get-parsed-app-version', () => {
    afterEach(() => {
        jest.resetModules();
        // @ts-ignore
        delete global.storage;
    });

    it('Должна парсить версию приложения', () => {
        jest.doMock('pp/get-pp-params', () => {
            return {
                getPpParams: () => ({ app_version_name: '12.34' }),
            };
        });

        return import('pp/get-parsed-app-version').then(({ getParsedAppVersion }) => {
            expect(getParsedAppVersion()).toEqual([12, 34]);
        });
    });

    it('Должна мемоизировать версию приложения', () => {
        jest.doMock('pp/get-pp-params', () => {
            return {
                getPpParams: () => ({ app_version_name: '21.43' }),
            };
        });

        return import('pp/get-parsed-app-version').then(({ getParsedAppVersion }) => {
            expect(getParsedAppVersion()).toEqual([21, 43]);

            jest.doMock('pp/get-pp-params', () => {
                return {
                    getPpParams: () => ({ app_version_name: '43.21' }),
                };
            });

            expect(getParsedAppVersion()).toEqual([21, 43]);
        });
    });

    it('patch не влияет на major и minor', () => {
        jest.doMock('pp/get-pp-params', () => {
            return {
                getPpParams: () => ({ app_version_name: '56.34.12' }),
            };
        });

        return import('pp/get-parsed-app-version').then(({ getParsedAppVersion }) => {
            expect(getParsedAppVersion()).toEqual([56, 34]);
        });
    });

    it('Версия парсится без minor', () => {
        jest.doMock('pp/get-pp-params', () => {
            return {
                getPpParams: () => ({ app_version_name: '42' }),
            };
        });

        return import('pp/get-parsed-app-version').then(({ getParsedAppVersion }) => {
            expect(getParsedAppVersion()).toEqual([42, 0]);
        });
    });

    it('Должна парсить unknown версию приложения', () => {
        jest.doMock('pp/get-pp-params', () => {
            return {
                getPpParams: () => ({ app_version_name: 'unknown' }),
            };
        });

        return import('pp/get-parsed-app-version').then(({ getParsedAppVersion }) => {
            expect(getParsedAppVersion()).toEqual([0, 0]);
        });
    });
});
