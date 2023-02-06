describe('pp/get-os-version', () => {
    afterEach(() => {
        jest.resetModules();
        // @ts-ignore
        delete global.storage;
    });

    it('Должно вернуть мажорную версию операционной системы', () => {
        const osStringVersion = '11';
        const osExpectedVersion = 11;

        jest.doMock('pp/get-pp-params', () => {
            return {
                getPpParams: () => ({ os_version: osStringVersion }),
            };
        });

        return import('pp/get-os-version').then(({ getOsVersion }) => {
            expect(getOsVersion()).toEqual(osExpectedVersion);
        });
    });

    it('Должно вернуть мажорную версию операционной системы имеющей минорную версию', () => {
        const osStringVersion = '4.4';
        const osExpectedVersion = 4;

        jest.doMock('pp/get-pp-params', () => {
            return {
                getPpParams: () => ({ os_version: osStringVersion }),
            };
        });

        return import('pp/get-os-version').then(({ getOsVersion }) => {
            expect(getOsVersion()).toEqual(osExpectedVersion);
        });
    });
});
