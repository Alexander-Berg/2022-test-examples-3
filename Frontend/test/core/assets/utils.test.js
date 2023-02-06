const { baseConfig } = require('./data');

// делаем небольшой трюк: устанавливаем конфиг до запроса assets;
// это нужно потому, что в ряде модулей он статичен и не меняется во время запроса
const runtimeConfig = require('../../../core/assets/runtimeConfig');
runtimeConfig.setConfig(baseConfig);

// получаем assets после установки конфига
const assets = require('../../../core/assets/assets');

describe('AssetsApi', () => {
    describe('Utils', () => {
        it('Генерирует корректный уникальный id', () => {
            jest.spyOn(Math, 'random').mockReturnValueOnce(0.0001);
            jest.spyOn(Date, 'now').mockReturnValueOnce(100);

            expect(assets.generateId()).toEqual('uniq100-10000');
        });

        it('Возвращает функцию для инлайна с параметрами', () => {
            expect(assets.inlineWithParams('function(params) {console.log(params)}', { params: true }), 'Вызов функции с параметрами некоректный')
                .toEqual('(function(){;var params={"params":true};var fn=function(params) {console.log(params)};fn.call(null, params || {}, window, document);})();');

            expect(assets.inlineWithParams('function(params) {console.log(params)}'), 'Вызов функции без параметров некорректный')
                .toEqual('(function(){;var params=undefined;var fn=function(params) {console.log(params)};fn.call(null, params || {}, window, document);})();');

            expect(assets.inlineWithParams(null, { params: true }, 'window.helloWorld'), 'Вызов функции с глобальной переменной некорректный')
                .toEqual('(function(){null;var params={"params":true};var fn=window.helloWorld;fn.call(null, params || {}, window, document);})();');
        });

        it('Получение анкора по id', () => {
            assets.setHash('123');
            expect(assets.saltPageHash('my-awesome-id'), 'Сгенерированный id некорректен').toEqual('my-awesome-id-123');

            assets.setHash('1234');
            expect(assets.saltPageHash('my-awesome-id'), 'После обновлениия хэша id некорректен').toEqual('my-awesome-id-1234');
        });
    });
});
