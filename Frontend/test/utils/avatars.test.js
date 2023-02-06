const applyAlias = require('../../core/utils/avatars').applyAlias;
const resizeByNamespace = require('../../core/utils/avatars/resize-by-namespace');
const { fitColumnsToAvailableAlias } = require('../../core/utils/avatars/suggest-alias');

const TURBO_SRC = '//avatars.mds.yandex.net/get-turbo/399060/6edbbae4-1af1-46ea-97fc-6938b4b1cbf7';
const TURBO_SRC_WITH_ALIAS = '//avatars.mds.yandex.net/get-turbo/399060/6edbbae4-1af1-46ea-97fc-6938b4b1cbf7/max_g480_c6_r1x1_pd20';
const NEWS_SRC = '//avatars.mds.yandex.net/get-ynews/399060/6edbbae4-1af1-46ea-97fc-6938b4b1cbf7';
const NEWS_LOGO_SRC = '//avatars.mds.yandex.net/get-ynews-logo/399060/6edbbae4-1af1-46ea-97fc-6938b4b1cbf7';
const SNIPPETS_SRC = '//avatars.mds.yandex.net/get-snippets_images/399060/6edbbae4-1af1-46ea-97fc-6938b4b1cbf7';

describe('avatars', () => {
    it('должен выбросить исключение если не передан img', () => {
        const run = () => applyAlias();

        expect(run, 'Required parameter img is undefined').toThrow();
    });

    it('должен дописывать слеш в конце урлов без слеша', () => {
        const url = TURBO_SRC;
        const urlWithAlias = TURBO_SRC_WITH_ALIAS;
        const res = applyAlias(/** @type {AvatarImage} */{ src: url });

        expect(res.src.replace(/^(https?:)?\/\/(.+?)\/*?$/g, '$2').split('/').length, 'Не дописал слеш в конец урла без слеша').toEqual(5);

        const res2 = applyAlias(/** @type {AvatarImage} */{ src: url + '/' });

        expect(res2.src.replace(/^(https?:)?\/\/(.+?)\/*?$/g, '$2').split('/').length, 'Дописал лишний слеш в урл со слешом').toEqual(5);

        const res3 = applyAlias(/** @type {AvatarImage} */{ src: urlWithAlias });

        expect(res3.src, 'Дописал лишний слеш в урл с алиасом').toEqual(urlWithAlias);
    });

    it('должен фиксить альясы содержащие g320 и/или pd15', () => {
        let img = { src: TURBO_SRC + '/max_g320_c12_r16x9_pd20' };
        let res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/max_g360_c12_r16x9_pd20'), 'Не пофиксил альяс с g320').toBe(true);

        img = { src: TURBO_SRC + '/crop_g320_c12_r16x9_pd20' };
        res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/crop_g360_c12_r16x9_pd20'), 'Не пофиксил альяс с g320').toBe(true);

        img = { src: TURBO_SRC + '/crop_g480_c12_r16x9_pd15' };
        res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/crop_g480_c12_r16x9_pd20'), 'Не пофиксил альяс с pd15').toBe(true);

        img = { src: TURBO_SRC + '/crop_g320_c12_r16x9_pd15' };
        res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/crop_g360_c12_r16x9_pd20'), 'Не пофиксил альяс с g320 и pd15').toBe(true);

        img = { src: TURBO_SRC + '/crop_g480_c12_r16x9_pd20' };
        res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/crop_g480_c12_r16x9_pd20'), 'Изменил нормальный альяс').toBe(true);
    });

    it('должен подгонять колонки к существующим альясам', () => {
        let img = { src: TURBO_SRC + '/max_g360_c1_r16x9_pd20' };
        let res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/max_g360_c2_r16x9_pd20'), 'Не пофиксил альяс с _c1_').toBe(true);

        img = { src: TURBO_SRC + '/crop_g480_c5_r16x9_pd20' };
        res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/crop_g480_c6_r16x9_pd20'), 'Не пофиксил альяс с _c5_').toBe(true);

        img = { src: TURBO_SRC + '/crop_g480_c8_r16x9_pd20' };
        res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/crop_g480_c6_r16x9_pd20'), 'Не пофиксил альяс с _c6_').toBe(true);

        img = { src: TURBO_SRC + '/crop_g480_c11_r16x9_pd10' };
        res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/crop_g480_c12_r16x9_pd10'), 'Не пофиксил альяс с _c11_').toBe(true);

        img = { src: TURBO_SRC + '/crop_g480_c4_r16x9_pd10' };
        res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/crop_g480_c6_r16x9_pd10'), 'Не пофиксил альяс с _c4_').toBe(true);

        img = { src: TURBO_SRC + '/crop_g480_c10_r16x9_pd10' };
        res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/crop_g480_c12_r16x9_pd10'), 'Не пофиксил альяс с _c10_').toBe(true);

        img = { src: TURBO_SRC + '/crop_g480_c2_r16x9_pd10' };
        res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/crop_g480_c2_r16x9_pd10'), 'Изменил нормальный альяс').toBe(true);

        img = { src: TURBO_SRC + '/crop_g480_c6_r16x9_pd10' };
        res = applyAlias(/** @type {AvatarImage} */img);
        expect(res.src.endsWith('/crop_g480_c6_r16x9_pd10'), 'Изменил нормальный альяс').toBe(true);
    });

    it('не должен изменять исходный объект картинки', () => {
        const img = { src: TURBO_SRC };
        const res = applyAlias(/** @type {AvatarImage} */img);

        expect(img.src, 'Изменил исходную картинку').not.toEqual(res.src);
    });

    it('не должен изменять картинку с алиасом', () => {
        const img = { src: TURBO_SRC + '/bla-bla-car/' };
        const res = applyAlias(/** @type {AvatarImage} */img);

        expect(img.src, 'Применил алиас к картинке которая уже с алиасом').toEqual(res.src);
    });

    it('не должен изменять картинку не из аватарницы', () => {
        let img = { src: '//ya.ru/image/bla.png?text=qwe' };
        let res = applyAlias(/** @type {AvatarImage} */img);

        expect(img.src, 'Применил алиас к картинке не из аватарницы').toEqual(res.src);

        img = { src: TURBO_SRC };
        res = applyAlias(/** @type {AvatarImage} */img);

        expect(img.src, 'Не применил алиас к картинке из аватарницы').not.toEqual(res.src);

        img = { src: '//avatars.mdst.yandex.net/get-turbo/399060/6edbbae4-1af1-46ea-97fc-6938b4b1cbf7' };
        res = applyAlias(/** @type {AvatarImage} */img);

        expect(img.src, 'Не применил алиас к картинке из тестовой аватарницы').not.toEqual(res.src);
    });

    it('должен оставлять orig вместо алиаса если есть поле "apply_alias: false"', () => {
        const url = `${TURBO_SRC}/orig`;
        const img = /** @type {AvatarImage} */{ src: url, apply_alias: false, width: 800, height: 400, ratio: 2 };
        const res = applyAlias(img, /** @type {AliasType} */ 'image');

        expect(res.src).toBe(url);
    });

    describe('suggest alias', () => {
        it('должен проставить корректный alias для aliasType=gallery', () => {
            // Turbo
            let img = { src: TURBO_SRC };
            let res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'gallery');

            expect(res.src.endsWith('/gallery_m_x10'), 'Применился не верный алиас для галереи без retina').toBe(true);

            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'gallery', { retinaScale: 2 });
            expect(res.src.endsWith('/gallery_m_x20'), 'Применился не верный алиас для галереи c retina').toBe(true);

            // Unsupported
            img = { src: NEWS_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'gallery');
            expect(res.src.endsWith('/orig'), 'Не поставился /orig для незарегестрированного namespace').toBe(true);
        });

        it('должен проставить корректный alias для aliasType=image', () => {
            // Turbo
            let img = { src: TURBO_SRC };
            let res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'image');
            expect(res.src.endsWith('/max_g480_c12_r16x9_pd10'), 'Применился не верный алиас для turbo без retina').toBe(true);

            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'image', { retinaScale: 2 });
            expect(res.src.endsWith('/max_g480_c12_r16x9_pd20'), 'Применился не верный алиас для turbo c retina').toBe(true);

            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'image', { fit: 'crop' });
            expect(res.src.endsWith('/crop_g480_c12_r16x9_pd10'), 'Применился не верный алиас для turbo c fit crop').toBe(true);

            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'image', { columns: 2 });
            expect(res.src.endsWith('/max_g480_c2_r16x9_pd10'), 'Применился не верный алиас для turbo под 2 колонки').toBe(true);

            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'image', { retinaScale: 2, viewPortWidth: 320 });
            expect(res.src.endsWith('/max_g360_c12_r16x9_pd20'), 'Применился не верный алиас для вьюпорта 320 и retina').toBe(true);

            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'image', { fit: 'crop', viewPortWidth: 320 });
            expect(res.src.endsWith('/crop_g360_c12_r16x9_pd10'), 'Применился не верный алиас для вьюпорта 320 с fit crop').toBe(true);

            // News
            img = { src: NEWS_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'image');
            expect(res.src.endsWith('/406x219'), 'Применился не верный алиас для news без retina: ' + res.src).toBe(true);

            img = { src: NEWS_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'image', { retinaScale: 2 });
            expect(res.src.endsWith('/406x219'), 'Применился не верный алиас для news c retina: ' + res.src).toBe(true);

            // Snippets
            img = { src: SNIPPETS_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'image');
            expect(res.src.endsWith('/414x310'), 'Применился не верный алиас для snippets без retina: ' + res.src).toBe(true);

            img = { src: SNIPPETS_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'image', { retinaScale: 2 });
            expect(res.src.endsWith('/828x620'), 'Применился не верный алиас для snippets c retina: ' + res.src).toBe(true);

            // Unsupported
            img = { src: NEWS_LOGO_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'image');
            expect(res.src.endsWith('/orig'), 'Не поставился /orig для незарегестрированного namespace').toBe(true);
        });

        it('должен проставить корректный alias для aliasType=logo', () => {
            // Turbo
            let img = { src: TURBO_SRC };
            // без опций
            let res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'logo');
            let condition = res.src.endsWith('/logo_square_s_x10');
            expect(condition, 'Применился не верный алиас для logo без опций: ' + res.src).toBe(true);

            // retina=2
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'logo', { retinaScale: 2 });
            condition = res.src.endsWith('/logo_square_s_x20');
            expect(condition, 'Применился не верный алиас для logo c retina: ' + res.src).toBe(true);

            // wide=s
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'logo', { wide: 's' });
            condition = res.src.endsWith('/logo_horizontal_s_x10');
            expect(condition, 'Применился не верный алиас для logo c wide=s: ' + res.src).toBe(true);

            // wide=m
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'logo', { wide: 'm' });
            condition = res.src.endsWith('/logo_horizontal_m_x10');
            expect(condition, 'Применился не верный алиас для logo c wide=m: ' + res.src).toBe(true);

            // wide=l
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'logo', { wide: 'l' });
            condition = res.src.endsWith('/logo_horizontal_l_x10');
            expect(condition, 'Применился не верный алиас для logo c wide=l: ' + res.src).toBe(true);

            // wide=unsupported
            let run = () => applyAlias(
                /** @type {AvatarImage} */img,
                /** @type {AliasType} */ 'logo',
                { wide: 'unsupported' }
            );

            expect(run, 'options.wide can only be - s, m, l', 'Для неверного размера options.wide не выбросилось искллючение').toThrow();

            // Unsupported
            img = { src: NEWS_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'logo');
            expect(res.src.endsWith('/orig'), 'Не поставился /orig для незарегестрированного namespace').toBe(true);
        });

        it('должен проставить корректный alias для aliasType=related', () => {
            // Turbo
            let img = { src: TURBO_SRC };
            let res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'related');
            expect(res.src.endsWith('/max_g480_c12_r16x9_pd10'), 'Применился не верный алиас для turbo без retina: ' + res.src).toBe(true);

            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'related', { retinaScale: 2 });
            expect(res.src.endsWith('/max_g480_c12_r16x9_pd20'), 'Применился не верный алиас для turbo c retina: ' + res.src).toBe(true);

            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'related', { viewPortWidth: 320 });
            expect(res.src.endsWith('/max_g360_c12_r16x9_pd10'), 'Применился не верный алиас для turbo с вьюпортом 320 без retina: ' + res.src).toBe(true);

            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'related', { viewPortWidth: 320, retinaScale: 2 });
            expect(res.src.endsWith('/max_g360_c12_r16x9_pd20'), 'Применился не верный алиас для turbo с вьюпортом 320 и retina: ' + res.src).toBe(true);

            // news_logo
            img = { src: NEWS_LOGO_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'related');
            expect(res.src.endsWith('/180x180'), 'Применился не верный алиас для news без retina: ' + res.src).toBe(true);

            img = { src: NEWS_LOGO_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'related', { retinaScale: 2 });
            expect(res.src.endsWith('/180x180'), 'Применился не верный алиас для news c retina: ' + res.src).toBe(true);

            // Snippets
            img = { src: NEWS_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'related');
            expect(res.src.endsWith('/100x100'), 'Применился не верный алиас для snippets без retina: ' + res.src).toBe(true);

            img = { src: NEWS_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'related', { retinaScale: 2 });
            expect(res.src.endsWith('/100x100'), 'Применился не верный алиас для snippets c retina: ' + res.src).toBe(true);

            // Unsupported
            img = { src: SNIPPETS_SRC };
            res = applyAlias(/** @type {AvatarImage} */img, /** @type {AliasType} */ 'related');
            expect(res.src.endsWith('/orig'), 'Не поставился /orig для незарегестрированного namespace: ' + res.src).toBe(true);
        });
    });

    describe('dimensions calculation', () => {
        it('должен возвращать оригинал для маленьких картинок и картинок без NS', () => {
            let img = /** @type {AvatarImage} */{ src: TURBO_SRC, width: 100, height: 100, ratio: 1 };
            let res = resizeByNamespace(img, 'unsupported', 'alias');
            expect(res, 'Не вернул оригинальную картинку для несуществующего namespace').toEqual({
                width: 100,
                height: 100,
                ratio: 1,
            });

            img = /** @type {AvatarImage} */{ src: TURBO_SRC, width: 100, height: 100, ratio: 1 };
            res = resizeByNamespace(img, 'turbo', 'unsupported');
            expect(res, 'Не вернул оригинальную картинку для несуществующего alias').toEqual({
                width: 100,
                height: 100,
                ratio: 1,
            });

            img = /** @type {AvatarImage} */{ src: TURBO_SRC, width: 100, height: 100, ratio: 1 };
            res = resizeByNamespace(img, 'turbo', 'max_g480_c12_r16x9_pd20');
            expect(res, 'Не вернул оригинальную картинку для маленькой картинки').toEqual({
                width: 100,
                height: 100,
                ratio: 1,
            });

            img = /** @type {AvatarImage} */{ src: TURBO_SRC, width: 100, height: 100, ratio: 1 };
            res = resizeByNamespace(img, 'turbo', 'gallery_m_x10');
            expect(res, 'Не вернул оригинальную картинку для маленькой картинки по высоте').toEqual({
                width: 100,
                height: 100,
                ratio: 1,
            });
        });

        it('должен скейлить только по одной стороне для alias с одной стороной', () => {
            let img = /** @type {AvatarImage} */{ src: TURBO_SRC, width: 800, height: 400, ratio: 2 };
            let res = resizeByNamespace(img, 'turbo', 'gallery_m_x10');
            expect(res, 'Не сделал scale по высоте для горизонтальной кратинки').toEqual({
                width: 320,
                height: 160,
                ratio: 2,
            });

            img = /** @type {AvatarImage} */{ src: TURBO_SRC, width: 400, height: 800, ratio: 0.5 };
            res = resizeByNamespace(img, 'turbo', 'gallery_m_x10');
            expect(res, 'Не сделал scale по высоте для вертикальной картинки кратинки').toEqual({
                width: 80,
                height: 160,
                ratio: 0.5,
            });

            img = /** @type {AvatarImage} */{ src: TURBO_SRC, width: 400, height: 400, ratio: 1 };
            res = resizeByNamespace(img, 'turbo', 'gallery_m_x10');
            expect(res, 'Не сделал scale по высоте для квадратной картинки кратинки').toEqual({
                width: 160,
                height: 160,
                ratio: 1,
            });
        });

        it('должен скейлить по ближайшей к namespace стороне', () => {
            let img = /** @type {AvatarImage} */{ src: SNIPPETS_SRC, width: 1024, height: 340, ratio: 2 };
            let res = resizeByNamespace(img, 'snippets', '828x620'); // { width: 828, height: 620, ratio: 1.34 }

            expect(res, 'Не сделал scale по высоте для горизонтальной кратинки в горизонтальном alias').toEqual({
                width: 828,
                height: 274.92,
                ratio: 3.01,
            });

            img = /** @type {AvatarImage} */{ src: TURBO_SRC, width: 400, height: 800, ratio: 0.5 };
            res = resizeByNamespace(img, 'turbo', 'max_g480_c12_r16x9_pd10'); // { width: 328, height: 185, ratio: 1.77 }

            expect(res, 'Не сделал scale по ширине для вертикальной картинки в горизонтальном namespace').toEqual({
                height: 250,
                ratio: 0.5,
                width: 125,
            });
        });

        it('должен ограничивать размеры с опциями maxWidth/maxHeight', () => {
            let img = /** @type {AvatarImage} */{ src: TURBO_SRC, width: 800, height: 400, ratio: 2 };
            let res = applyAlias(img, /** @type {AliasType} */ 'image', { maxHeight: 100 });

            expect(res, 'Не сделал ограничение по высоте').toEqual({
                width: 200,
                src: TURBO_SRC + '/max_g480_c12_r16x9_pd10',
                height: 100,
                ratio: 2,
            });

            img = /** @type {AvatarImage} */{ src: TURBO_SRC, width: 800, height: 400, ratio: 2 };
            res = applyAlias(img, /** @type {AliasType} */ 'image', { maxWidth: 200 });

            expect(res, 'Не сделал ограничение по ширине').toEqual({
                width: 200,
                src: TURBO_SRC + '/max_g480_c12_r16x9_pd10',
                height: 100,
                ratio: 2,
            });
        });
    });

    describe('fitColumnsToAvailableAlias', () => {
        const aliasOptions = {
            fit: 'max',
            gridWidth: 360,
            ratio: '1x1',
            pixelDensity: 20,
        };

        it('Должен возвращать подходящее значения колонок учитывая границы', () => {
            const availableAliases = {
                max_g360_c2_r1x1_pd20: { width: 96, height: 96, ratio: 1 },
                max_g360_c4_r1x1_pd20: { width: 240, height: 240, ratio: 1 },
                max_g360_c6_r1x1_pd20: { width: 320, height: 320, ratio: 1 },
                max_g360_c12_r1x1_pd20: { width: 656, height: 656, ratio: 1 },
            };
            const testColumnsValueToExpected = [
                [3, 2],
                [4, 4],
                [5, 6],
                [8, 6],
                [9, 12],
                [12, 12],
            ];

            testColumnsValueToExpected.forEach(([testColumns, expectedColumns]) => {
                const currentAliasOptions = {
                    ...aliasOptions,
                    columns: testColumns,
                };
                const suggestedColumns = fitColumnsToAvailableAlias(currentAliasOptions, availableAliases);

                expect(suggestedColumns).toEqual(expectedColumns);
            });
        });

        it('Должен возвращать подходящее значения колонок учитывая наличие алиаса', () => {
            const availableAliases = {
                max_g360_c6_r1x1_pd20: { width: 320, height: 320, ratio: 1 },
                max_g360_c12_r1x1_pd20: { width: 656, height: 656, ratio: 1 },
            };
            const testColumnsValueToExpected = [
                [4, 6],
                [8, 6],
                [12, 12],
            ];

            testColumnsValueToExpected.forEach(([testColumns, expectedColumns]) => {
                const currentAliasOptions = {
                    ...aliasOptions,
                    columns: testColumns,
                };
                const suggestedColumns = fitColumnsToAvailableAlias(currentAliasOptions, availableAliases);

                expect(suggestedColumns).toEqual(expectedColumns);
            });
        });

        it('Должен возвращать дефолтное значение 12 для ненайденных алиасов', () => {
            const testColumns = 4;
            const currentAliasOptions = {
                ...aliasOptions,
                columns: testColumns,
            };
            const suggestedColumns = fitColumnsToAvailableAlias(currentAliasOptions, {});

            expect(suggestedColumns).toEqual(12);
        });
    });
});
