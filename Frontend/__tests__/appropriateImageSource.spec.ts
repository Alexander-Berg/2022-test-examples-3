import { URLSearchParams } from 'url';

import { getAppropriateImageSrc, IAppropriateImageParams, EStorageTypes } from '../appropriateImageSource';

describe('Утилита подбора урла картинки в карточке рекомендаций', () => {
    test('Возвращает оригинальный источник картинки, если неизвестный тип хранилища', () => {
        const url = getAppropriateImageSrc({
            boxHeight: 0,
            boxWidth: 0,
            imageHeight: 0,
            imageWidth: 0,
            isRetina: false,
            sizes: {},
            src: 'https://expected.url',
            // @ts-ignore Проверяем дефолтный кейс функции, возможный в рантайме
            storage: 'undefined',
            viewportWidth: 0,
        });

        expect(url).toEqual('https://expected.url');
    });

    describe('Аватарница', () => {
        describe('get-turbo ns', () => {
            const defaultParams: IAppropriateImageParams =
            {
                boxHeight: 152,
                boxWidth: 352,
                imageHeight: 0,
                imageWidth: 0,
                isRetina: false,
                src: 'https://avatars.mds.yandex.net/get-turbo/2997919/rth6b0d1c05aa192330fb8859eeefe15ea6/orig',
                storage: EStorageTypes.avatars,
                viewportWidth: 360,
                projectId: 'get-turbo',
            };

            test('Выбирает из доступных ratio ближайший к размеру box', () => {
                const _1x1 = { ...defaultParams, boxWidth: 200, boxHeight: 200 };
                const _2x3 = { ...defaultParams, boxWidth: 200, boxHeight: 300 };
                const _3x4 = { ...defaultParams, boxWidth: 200, boxHeight: 270 };
                const _4x3 = { ...defaultParams, boxWidth: 300, boxHeight: 200 };
                const _16x9 = { ...defaultParams };

                expect(getAppropriateImageSrc(_1x1)).toMatch('r1x1');
                expect(getAppropriateImageSrc(_2x3)).toMatch('r2x3');
                expect(getAppropriateImageSrc(_3x4)).toMatch('r3x4');
                expect(getAppropriateImageSrc(_4x3)).toMatch('r4x3');
                expect(getAppropriateImageSrc(_16x9)).toMatch('r16x9');
            });

            test('Выбирает сетку по вьюпорту из доступных пресетов', () => {
                const _360_1 = { ...defaultParams, viewportWidth: 320 };
                const _360_2 = { ...defaultParams, viewportWidth: 479 };
                const _480_1 = { ...defaultParams, viewportWidth: 480 };
                const _480_2 = { ...defaultParams, viewportWidth: 500 };

                expect(getAppropriateImageSrc(_360_1)).toMatch('g360');
                expect(getAppropriateImageSrc(_360_2)).toMatch('g360');
                expect(getAppropriateImageSrc(_480_1)).toMatch('g480');
                expect(getAppropriateImageSrc(_480_2)).toMatch('g480');
            });

            test('Выбирает pd по флагу isRetina', () => {
                const isRetina = { ...defaultParams, isRetina: true };
                const isNotRetina = { ...defaultParams, isRetina: false };

                expect(getAppropriateImageSrc(isRetina)).toMatch('pd20');
                expect(getAppropriateImageSrc(isNotRetina)).toMatch('pd10');
            });

            test('Возвращает полный урл', () => {
                expect(getAppropriateImageSrc(defaultParams))
                    .toEqual('https://avatars.mds.yandex.net/get-turbo/2997919/rth6b0d1c05aa192330fb8859eeefe15ea6/crop_g360_c12_r16x9_pd10');
            });
        });

        describe('get-snippets_images ns', () => {
            const defaultProps: IAppropriateImageParams =
            {
                boxHeight: 152,
                boxWidth: 352,
                imageWidth: 625,
                imageHeight: 469,
                isRetina: false,
                src: 'https://avatars.mds.yandex.net/get-snippets_images/34219/4ab97855410558315c70c95a55fd99286f386aa1/orig',
                storage: EStorageTypes.avatars,
                viewportWidth: 360,
                projectId: 'get-snippets_images',
            };

            test('Выбирает ближайший к размеру box', () => {
                const _1 = { ...defaultProps };
                const _2 = { ...defaultProps, boxWidth: 450 };
                const _3 = { ...defaultProps, boxHeight: 250, boxWidth: 625 };
                const _4 = { ...defaultProps, boxHeight: 350, boxWidth: 830 };

                expect(getAppropriateImageSrc(_1)).toMatch('414x310');
                expect(getAppropriateImageSrc(_2)).toMatch('600x450');
                expect(getAppropriateImageSrc(_3)).toMatch('828x620');
                expect(getAppropriateImageSrc(_4)).toMatch('orig');
            });

            test('Выбирает retina', () => {
                const isRetina = { ...defaultProps, isRetina: true };
                const isNotRetina = { ...defaultProps, isRetina: false };

                expect(getAppropriateImageSrc(isRetina)).toMatch('828x620');
                expect(getAppropriateImageSrc(isNotRetina)).toMatch('414x310');
            });
        });

        describe('others ns', () => {
            const defaultProps: IAppropriateImageParams =
                {
                    boxHeight: 100,
                    boxWidth: 100,
                    imageWidth: 625,
                    imageHeight: 469,
                    imageSizes: {
                        '244x122': '/244x122',
                        '366x183': '/366x183',
                        '488x244': '/488x244',
                        '600x': '/600x',
                        '732x366': '/732x366',
                    },
                    isRetina: false,
                    src: 'https://avatars.mds.yandex.net/get-ynews/37648/a750c7a93018ee5d0bdced936526eba9/orig',
                    storage: EStorageTypes.avatars,
                    viewportWidth: 360,
                    projectId: 'get-ynews',
                };

            test('Выбирает ближайший к размеру box', () => {
                const _1 = { ...defaultProps };
                const _2 = { ...defaultProps, boxWidth: 200, boxHeight: 100 };
                const _3 = { ...defaultProps, boxWidth: 250, boxHeight: 125 };
                const _4 = { ...defaultProps, boxWidth: 350, boxHeight: 175 };
                const _5 = { ...defaultProps, boxWidth: 800, boxHeight: 400 };

                expect(getAppropriateImageSrc(_1)).toMatch('244x122');
                expect(getAppropriateImageSrc(_2)).toMatch('366x183');
                expect(getAppropriateImageSrc(_3)).toMatch('488x244');
                expect(getAppropriateImageSrc(_4)).toMatch('600x');
                expect(getAppropriateImageSrc(_5)).toMatch('orig');
            });

            test('Выбирает retina', () => {
                const isRetina = { ...defaultProps, isRetina: true };
                const isNotRetina = { ...defaultProps, isRetina: false };

                expect(getAppropriateImageSrc(isRetina)).toMatch('366x183');
                expect(getAppropriateImageSrc(isNotRetina)).toMatch('244x122');
            });
        });
    });

    describe('Тумбнейлер', () => {
        const defaultParams: IAppropriateImageParams =
        {
            boxHeight: 152,
            boxWidth: 352,
            imageWidth: 625,
            imageHeight: 506,
            isRetina: false,
            src: 'http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=4&w=625&h=506',
            storage: EStorageTypes.im2tub,
            viewportWidth: 360,
            projectId: 'get-turbo',
        };

        test('Масштабирует изображение по ширине контейнера с сохранением пропорций', () => {
            expect(getAppropriateImageSrc(defaultParams)).toMatch('&n=33&w=352&h=284');
        });

        test('Удваивает размер изображения, если передан флаг isRetina', () => {
            const isRetina = { ...defaultParams, isRetina: true };

            expect(getAppropriateImageSrc(isRetina)).toMatch('&n=33&w=704&h=569');
        });

        test('Возвращает полный урл', () => {
            expect(getAppropriateImageSrc(defaultParams))
                .toEqual('http://im2-tub-com.yandex.net/i?id=828719948d4c15bbfe32b27f9c377fdd&ref=itditp&n=33&w=352&h=284');
        });
    });

    describe('Ресайзер', () => {
        const defaultParams: IAppropriateImageParams =
        {
            boxHeight: 152,
            boxWidth: 352,
            imageHeight: 630,
            imageWidth: 1200,
            isRetina: false,
            src: 'https://lisa.ru/images/cache/2020/3/18/resize_1200_630_true_crop_1920_1079_0_51_q90_887582_25d9c03ae8af50ca4cfbc9a05.jpeg',
            storage: EStorageTypes.other,
            viewportWidth: 360,
            projectId: 'get-turbo',
        };

        test('Масштабирует изображение по ширине контейнера с сохранением пропорций', () => {
            expect(getAppropriateImageSrc(defaultParams)).toMatch('width=352&height=184');
        });

        test('Удваивает размер изображения, если передан флаг isRetina', () => {
            const isRetina = { ...defaultParams, isRetina: true };

            expect(getAppropriateImageSrc(isRetina)).toMatch('width=704&height=369');
        });

        test('Возвращает полный урл', () => {
            const ratio = defaultParams.imageWidth / defaultParams.imageHeight;
            const expectedParams = new URLSearchParams();
            expectedParams.set('url', defaultParams.src);
            expectedParams.set('width', `${defaultParams.boxWidth}`);
            expectedParams.set('height', `${Math.floor(defaultParams.boxWidth / ratio)}`);

            expect(getAppropriateImageSrc(defaultParams))
                .toEqual(`https://resize.rs.yandex.net/public?${expectedParams.toString()}`);
        });
    });
});
