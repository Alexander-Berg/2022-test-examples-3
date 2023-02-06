/* eslint-disable global-require */
import dataLayerParser from '../../../src/parsers/data-layer-parser';

describe('dataLayer', () => {
    let _canParseMock: jest.SpyInstance;

    beforeEach(() => {
        _canParseMock = jest.spyOn(dataLayerParser, '_canParse').mockImplementation(() => true);
        Object.defineProperty(global, 'window', {
            value: {},
            writable: true,
        });
        Object.defineProperty(global, 'document', {
            value: {},
            writable: true,
        });
        Object.defineProperty(global, 'localStorage', {
            value: {},
            writable: true,
        });
    });

    afterEach(() => {
        _canParseMock?.mockRestore();
    });

    it('003', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/003.json'),
            },
            writable: true,
        });
        // window.dataLayer = require('./data-layers/003.json');
        const data = dataLayerParser.getData();

        expect(data).toEqual({
            method: 'dl',
            data: {
                name: 'Карта памяти micro SDHC Transcend TS16GUSDHC10 16GB',
                price: 590,
                vendor: 'Transcend',
                category: 'Компьютерная техника/Носители информации/Карты памяти/micro SD / TransFlash',
                currency: 'RUB'
            }
        });
    });

    it('apple.ru', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/apple-ru.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toBe(undefined);
    });

    it('citilink', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/citilink.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'Карта памяти microSDHC TRANSCEND 8 ГБ',
                category: 'Карты памяти',
                vendor: 'TRANSCEND',
                price: 290
            },
            method: 'dl'
        });
    });

    it('ecco-shoes', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/ecco-shoes.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'Полуботинки ECCO HOWELL',
                category: 'Мужская обувь',
                vendor: 'ECCO',
                price: 6199
            },
            method: 'dl'
        });
    });

    it('itovari', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/itovari.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'Apple Watch Sport 42mm with Sport Band (Green)',
                category: 'Apple Watch Sport',
                price: 30900
            },
            method: 'dl'
        });
    });

    it('mediamarkt', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/mediamarkt.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'Watch Sport 42mm Sport Band MJ3N2 White Смарт-часы',
                vendor: 'Apple',
                price: 31989,
                currency: 'RUB',
                category: 'Телефоны'
            },
            method: 'dl'
        });
    });

    it('mvideo', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/mvideo.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'Смартфон Apple iPhone 5S 16Gb Gold (ME434RU/A)',
                vendor: 'Apple',
                price: 29990,
                category: 'Смартфоны / Сотовые телефоны'
            },
            method: 'dl'
        });
    });

    it('ozon', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/ozon.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'Transcend microSDHC Class 10 8GB карта памяти + адаптер',
                price: 445,
                category: 'Карты памяти'
            },
            method: 'dl'
        });
    });

    it('pleer.ru', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/pleer.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'APPLE Watch Sport 42mm with Orange Sport Band MLC42RU/A',
                price: 31488,
                vendor: 'APPLE',
                category: 'Телефоны, умные часы(смарт) и браслеты, VoIP, аксессуары.../Умные часы / смарт-часы'
            },
            method: 'dl'
        });
    });

    it('re-store', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/re-store.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toBe(undefined);
    });

    it('smart-apple', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/smart-apple.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'Apple Watch Sport 42 mm (алюминий «серый космос», чёрный спортивный ремешок)',
                price: 30500,
                currency: 'RUB'
            },
            method: 'dl'
        });
    });

    it('svyaznoy', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/svyaznoy.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'Sony DR-BT50',
                price: 0,
                category: 'Bluetooth-гарнитуры',
                vendor: 'Sony'
            },
            method: 'dl'
        });
    });

    it('svyaznoy with products as object', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/svyaznoy-with-products-as-object.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'Lenovo P70 (темно-синий)',
                price: 19990,
                category: 'Мобильные телефоны',
                vendor: 'Lenovo'
            },
            method: 'dl'
        });
    });

    it('tehnosila', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/tehnosila.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'Карта памяти Transcend Micro SDHC 16Гб Class10 Ultimate + адаптер',
                price: 990,
                vendor: 'Transcend',
                category: 'Фото- и видеотехника/Аксессуары для фото и видеотехники/Карты памяти'
            },
            method: 'dl'
        });
    });

    it('vseinstrumenti', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: require('./data-layers/vseinstrumenti.json'),
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toEqual({
            data: {
                name: 'Мультиметр Ресанта DT 9205A',
                price: 659,
                vendor: 'Resanta_',
                category: 'Инструмент/Измерительный инструмент'
            },
            method: 'dl'
        });
    });

    it('dataLayer is undefined', () => {
        const data = dataLayerParser.getData();
        expect(data).toBe(undefined);
    });

    it('dataLayer is string', () => {
        Object.defineProperty(global, 'window', {
            value: {
                dataLayer: 'haha i am dataLayer',
            },
            writable: true,
        });

        const data = dataLayerParser.getData();
        expect(data).toBe(undefined);
    });
});
