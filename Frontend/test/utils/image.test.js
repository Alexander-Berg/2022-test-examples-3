const _ = require('../../core/lodash');

const Image = require('../../core/utils/image');

describe('images', () => {
    describe('normalizeImage', () => {
        it('Данные из первого элемента src_set переносятся в основной объект', () => {
            const id = Math.random();

            const image = {
                src_set: [
                    {
                        test: id,
                    },
                ],
            };

            const node = {};

            expect(Image.normalizeImage(image, node).test === id, 'Свойство из первого элемента src_set не записалось в основной объект')
                .toBe(true);
        });

        it('Данные из первого элемента src_set не перезатирают данные основного объекта', () => {
            const test1 = Math.random();
            const test2 = Math.random();

            const image = {
                test: test1,
                src_set: [
                    {
                        test: test2,
                    },
                ],
            };

            const node = {};

            expect(Image.normalizeImage(image, node).test === test1, 'Свойство из первого элемента src_set перезаписало значение в основном объекте')
                .toBe(true);
        });

        it('Данные из 2 и последующих элементов src_set не попадают в объект', () => {
            const test1 = Math.random();
            const test2 = Math.random();

            const image = {
                src_set: [
                    {
                        test1: test1,
                    },
                    {
                        test2: test2,
                    },
                ],
            };

            const node = {};

            expect(Image.normalizeImage(image, node).test1 === test1 && typeof Image.normalizeImage(image, node).test2 === 'undefined',
                'Свойство из второго элемента src_set записались в основной объект').toBe(true);
        });

        it('Пустой src_set не вызывает ошибки', () => {
            const node = {};

            Image.normalizeImage({
                src_set: [],
            }, node);

            Image.normalizeImage({
                src_set: {},
            }, node);

            Image.normalizeImage({
                src_set: false,
            }, node);
        });

        it('Отсутствие src_set не вызывает ошибки', () => {
            const node = {};

            Image.normalizeImage({}, node);
        });
    });

    describe('getViewportWidth', () => {
        it('При отсутствии параметра берется szm кука по умолчанию', () => {
            expect(Image.getViewportWidth({}) === 412, 'По умолчанию взялась не szm кука от Image')
                .toBe(true);
        });
    });

    describe('getImageBoxSize', () => {
        it('Для изображения с размерами boxSize берется через ratio', () => {
            const image = {
                width: 1000,
                height: 500,
            };

            const viewPortWidth = Image.getViewportWidth({});

            const _image = Image.getImageBoxSize(image, viewPortWidth);

            expect(_image.boxSize.height / _image.boxSize.width === 0.5, 'Размеры boxSize берутся не через ratio')
                .toBe(true);

            expect(_image.boxSize.width === _image.boxSize.viewPortWidth, 'Размеры boxSize берутся не через ratio')
                .toBe(true);

            expect(_image.boxSize.hasSize, 'Размеры boxSize берутся не через ratio')
                .toBeTruthy();
        });

        it('Для изображения без размеров boxSize считается от вьюпорта', () => {
            const image = {};

            const viewPortWidth = Image.getViewportWidth({});

            const _image = Image.getImageBoxSize(image, viewPortWidth);

            expect(_image.boxSize.height / _image.boxSize.width === 1, 'Размеры boxSize берутся считается от вьюпорта')
                .toBe(true);

            expect(_image.boxSize.width === _image.boxSize.viewPortWidth, 'Размеры boxSize берутся считается от вьюпорта')
                .toBe(true);

            expect(!_image.boxSize.hasSize, 'Размеры boxSize берутся считается от вьюпорта')
                .toBe(true);
        });
    });

    describe('prepareImage', () => {
        it('Сравниваем пачку входных и выходных эталонных данных изображений', () => {
            const imagesInput = require('./image.test.imageInput.json');

            const node = {
                _: _,
            };

            const imagesOutput = require('./image.test.imageOutput.json');

            imagesInput.forEach((image, ID) => {
                expect(JSON.stringify(imagesOutput[ID]) === JSON.stringify(Image.prepareImage(image, node)),
                    'Преобразование входных данных не соответствует эталонным').toBe(true);
            });
        });
    });
});
