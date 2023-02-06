const { baseConfig, objectExamples: [firstObj, secondObj] } = require('./data');

// делаем небольшой трюк: устанавливаем конфиг до запроса assets;
// это нужно потому, что в ряде модулей он статичен и не меняется во время запроса
const runtimeConfig = require('../../../core/assets/runtimeConfig');
runtimeConfig.setConfig(baseConfig);

// получаем assets после установки конфига
const assets = require('../../../core/assets/assets');

describe('AssetsApi', () => {
    afterEach(() => assets.resetStore());

    describe('Store', () => {
        it('Добавление данных в стор и его получение', () => {
            assets.pushStore(firstObj);
            assets.pushStore(secondObj);

            expect(assets.getStore().getState()).toEqual({ id: '1234', tree: { '1': 2, '2': 3 } });
        });

        it('Очистка состояния', () => {
            assets.pushStore(firstObj);
            assets.pushStore(secondObj);

            assets.resetStore();

            expect(assets.getStore().getState(), 'Состояние не пустое после очистки').toEqual({});
        });

        it('Проверка наличия состояния', () => {
            assets.pushStore(firstObj);
            assets.pushStore(secondObj);

            expect(assets.hasStore(), 'store не сообщает о наличии данных').toBe(true);

            assets.resetStore();

            expect(assets.hasStore(), 'store сообщает о наличии данных, когда их нет').toBe(false);
        });
    });
});
