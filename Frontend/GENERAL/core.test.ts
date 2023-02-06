import { AppLifeCycleManager, IManagerContext } from './core';
import type { IConstructor, IConfig, AppName, IApplication } from './core';

function getInstance(params: Partial<IConstructor> = {}): AppLifeCycleManager {
    return new AppLifeCycleManager({ ...params });
}

function getSimpleConfig(
    name: AppName,
    application: IApplication<{}>,
    config: Partial<IConfig<IManagerContext>> = {}): IConfig<IManagerContext> {
    return {
        name,
        activeWhen: jest.fn(() => true),
        getApp: () => application,
        ...config,
    };
}

function getSimpleApp(): IApplication<{}> {
    return {
        bootstrap: jest.fn(() => Promise.resolve()),
        mount: jest.fn(() => Promise.resolve()),
        unmount: jest.fn(() => Promise.resolve()),
    };
}

afterEach(() => {
    jest.useRealTimers();
});

describe('core', () => {
    it('registerApplication не вызывает методы приложений', () => {
        const spas = getInstance();
        const app = getSimpleApp();
        const config = getSimpleConfig('simple-app', app);

        spas.registerApplication(config);

        expect(app.bootstrap).not.toBeCalled();
        expect(app.mount).not.toBeCalled();
        expect(app.unmount).not.toBeCalled();
    });

    describe('Жизненные циклы и инвалидация', () => {
        test('invalidateApps', async() => {
            const spas = getInstance();
            const alwaysActiveApp = getSimpleApp();
            const appToUnmount = getSimpleApp();
            const alwaysActiveConfig = getSimpleConfig('alwaysActiveApp', alwaysActiveApp);
            const configToUnmount = getSimpleConfig('appToUnmount', appToUnmount);

            spas.registerApplication(alwaysActiveConfig);
            spas.registerApplication(configToUnmount);

            await spas.invalidateApps({});
            expect(alwaysActiveApp.bootstrap).toBeCalledTimes(1);
            expect(alwaysActiveApp.mount).toBeCalledTimes(1);
            expect(alwaysActiveApp.unmount).not.toBeCalled();
            expect(appToUnmount.bootstrap).toBeCalledTimes(1);
            expect(appToUnmount.mount).toBeCalledTimes(1);
            expect(appToUnmount.unmount).not.toBeCalled();

            // @ts-ignore
            configToUnmount.activeWhen.mockImplementationOnce(() => false);

            await spas.invalidateApps({});
            expect(alwaysActiveApp.bootstrap).toBeCalledTimes(1);
            expect(alwaysActiveApp.mount).toBeCalledTimes(1);
            expect(alwaysActiveApp.unmount).not.toBeCalled();
            expect(appToUnmount.bootstrap).toBeCalledTimes(1);
            expect(appToUnmount.mount).toBeCalledTimes(1);
            expect(appToUnmount.unmount).toBeCalledTimes(1);
        });

        test('shouldInvalidate', async() => {
            const spas = getInstance();
            spas.registerApplication(getSimpleConfig('simple-app', getSimpleApp()));

            expect(spas.shouldInvalidate('https://yandex.ru/')).toBe(true);
            await spas.invalidateApps({});
            expect(spas.shouldInvalidate('https://yandex.ru/')).toBe(false);
        });

        test('Guard beforeEnter вызывается', async() => {
            const spas = getInstance();
            const simpleApp = getSimpleApp();
            const beforeEnter = jest.fn(() => Promise.resolve(true));
            const configWithHook = getSimpleConfig('simpleApp', simpleApp, { beforeEnter });

            spas.registerApplication(configWithHook);

            spas.invalidateApps({});
            expect(configWithHook.beforeEnter).toBeCalledWith(window.location.href);
        });

        test('Guard beforeEnter предотвращает mount', async() => {
            const spas = getInstance();
            const simpleApp = getSimpleApp();
            const beforeEnter = jest.fn(() => Promise.resolve(false));
            const configWithHook = getSimpleConfig('simpleApp', simpleApp, { beforeEnter });

            spas.registerApplication(configWithHook);

            spas.invalidateApps({});
            expect(configWithHook.beforeEnter).toBeCalledWith(window.location.href);
            expect(simpleApp.mount).not.toBeCalled();
        });
    });

    describe('Состояние приложений', () => {
        test('При регистрации приложения не активны', async() => {
            const spas = getInstance();
            const app = getSimpleApp();
            const config = getSimpleConfig('simple-app', app);

            spas.registerApplication(config);
            expect(spas.getActiveApps()).toEqual([]);
            expect(spas.isAppActive('simple-app')).toEqual(false);
        });

        test('При инвалидации приложения переходят в правильный статус', async() => {
            const spas = getInstance();
            const app = getSimpleApp();
            const config = getSimpleConfig('simple-app', app);

            spas.registerApplication(config);

            await spas.invalidateApps({});

            expect(spas.getActiveApps()).toEqual(['simple-app']);
            expect(spas.isAppActive('simple-app')).toEqual(true);

            // @ts-ignore
            config.activeWhen.mockImplementationOnce(() => false);

            await spas.invalidateApps({});
            expect(spas.getActiveApps()).toEqual([]);
            expect(spas.isAppActive('simple-app')).toEqual(false);
        });

        test('Активирует приложение, указанное в конструкторе', () => {
            const app = getSimpleApp();
            const config = getSimpleConfig('simple-app', app);
            const spas = getInstance({ initialApp: 'simple-app', apps: [config] });

            expect(spas.getActiveApps()).toEqual(['simple-app']);
            expect(spas.isAppActive('simple-app')).toEqual(true);
        });

        test('Активация нескольких приложений после инвалидации', async() => {
            const spas = getInstance();
            spas.registerApplication(getSimpleConfig('simple-1', getSimpleApp()));
            spas.registerApplication(getSimpleConfig('simple-2', getSimpleApp()));
            spas.registerApplication(getSimpleConfig('simple-3', getSimpleApp()));

            await spas.invalidateApps({});

            expect(spas.getActiveApps()).toEqual(['simple-1', 'simple-2', 'simple-3']);
            expect(spas.isAppActive('simple-1')).toEqual(true);
            expect(spas.isAppActive('simple-2')).toEqual(true);
            expect(spas.isAppActive('simple-3')).toEqual(true);
        });

        test('Ленивый bootstrap позволяет вызов без указания времени', async() => {
            jest.useFakeTimers();
            const spy = jest.spyOn(global, 'setTimeout');
            const spas = getInstance();
            const app = getSimpleApp();
            spas.registerApplication(getSimpleConfig('simple-1', app));
            const promise = spas.startLazyBootstrapping({});

            expect(setTimeout).toBeCalledWith(expect.any(Function), 1000);
            jest.runAllTimers();

            spy.mockRestore();
            return promise;
        });

        test('Приложения переводятся в статус bootstrapped лениво', async() => {
            const spas = getInstance();
            const app = getSimpleApp();
            spas.registerApplication(getSimpleConfig('simple-1', app));
            // Чтобы не морочиться с искусственными таймерами, делаем просто короткий интервал
            const promise = spas.startLazyBootstrapping(10);

            await promise;

            expect(app.bootstrap).toBeCalled();
            expect(app.mount).not.toBeCalled();
        });

        test('Приложения переводятся в статус bootstrapped лениво для нескольких приложений', async() => {
            const spas = getInstance();
            const app1 = getSimpleApp();
            const app2 = getSimpleApp();
            spas.registerApplication(getSimpleConfig('simple-1', app1));
            spas.registerApplication(getSimpleConfig('simple-2', app2));
            // Чтобы не морочиться с искусственными таймерами, делаем просто короткий интервал
            await spas.startLazyBootstrapping(10);

            expect(app1.bootstrap).toBeCalled();
            expect(app2.bootstrap).toBeCalled();
        });
    });
});
