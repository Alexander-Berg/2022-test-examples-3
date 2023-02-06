const { baseConfig, objectExamples: [firstObj, secondObj] } = require('./data');

// делаем небольшой трюк: устанавливаем конфиг до запроса assets;
// это нужно потому, что в ряде модулей он статичен и не меняется во время запроса
const runtimeConfig = require('../../../core/assets/runtimeConfig');
runtimeConfig.setConfig(baseConfig);

// получаем assets после установки конфига
const assets = require('../../../core/assets/assets');

describe('AssetsApi', () => {
    describe('globalState', () => {
        it('Добавление корней и получение состояние', () => {
            assets.pushState(firstObj);
            assets.pushState(secondObj);

            expect(assets.getState()).toEqual([firstObj, secondObj]);
        });

        it('Очистка состояния', () => {
            assets.pushState(firstObj);
            assets.pushState(secondObj);

            assets.resetState();

            expect(assets.getState(), 'Состояние не пустое после очистки').toEqual([]);
        });

        it('Проверка наличия состояния', () => {
            assets.pushState(firstObj);
            assets.pushState(secondObj);

            expect(assets.hasState(), 'globalState не сообщает о наличии данных').toBe(true);

            assets.resetState();

            expect(assets.hasState(), 'globalState сообщает о наличии данных, когда их нет').toBe(false);
        });
    });
});
