import { ImagesWithManualAssetsOrdering } from '../../client/applications/images';
import { AppLifeCycleManager } from '../../client/core';
import { registerImages } from './registerImages';

describe('registerImages', () => {
    test('Под флагом регистрирует корректный инстанс картинок для флага manualAssetsOrdering', () => {
        const manager = new AppLifeCycleManager({});
        const managerSpy = jest.spyOn(manager, 'registerApplication');
        registerImages(manager, { platform: 'touch-phone', flags: { manualAssetsOrdering: true } });

        expect(managerSpy).toBeCalled();
        const config = managerSpy.mock.calls[0][0];
        expect(config.getApp()).toBeInstanceOf(ImagesWithManualAssetsOrdering);
    });

    test('Конфиг картинок возвращает тот же инстанс под флагом manualAssetsOrdering', () => {
        const manager = new AppLifeCycleManager({});
        const managerSpy = jest.spyOn(manager, 'registerApplication');
        registerImages(manager, { platform: 'touch-phone', flags: { manualAssetsOrdering: true } });

        expect(managerSpy).toBeCalled();
        const config = managerSpy.mock.calls[0][0];
        expect(config.getApp()).toBe(config.getApp());
    });

    test('Конфиг картинок возвращает тот же инстанс', () => {
        const manager = new AppLifeCycleManager({});
        const managerSpy = jest.spyOn(manager, 'registerApplication');
        registerImages(manager, { platform: 'touch-phone', flags: {} });

        expect(managerSpy).toBeCalled();
        const config = managerSpy.mock.calls[0][0];
        expect(config.getApp()).toBe(config.getApp());
    });
});
