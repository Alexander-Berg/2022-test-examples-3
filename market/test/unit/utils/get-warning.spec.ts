import getWarning from '../../../utils/get-warning';
import { ReportWarning } from '../../../src/types/report';

describe('get warning', () => {
    test("should return text of 'medicine_recipe' warning", () => {
        const warnings = [
            {
                type: 'appearance',
                value: {
                    full:
                        'Внешний вид товаров и/или упаковки может быть изменён изготовителем и отличаться от изображенных на Яндекс.Маркете.',
                    short:
                        'Внешний вид товаров и/или упаковки может быть изменён изготовителем и отличаться от изображенных на Яндекс.Маркете.',
                },
            },
            {
                type: 'medicine_recipe',
                value: {
                    full:
                        'Есть противопоказания, посоветуйтесь с врачом. Отпускается по рецепту врача. Приобретение лекарственных препаратов осуществляется только в торговом зале аптеки',
                    short:
                        'Есть противопоказания, посоветуйтесь с врачом. Отпускается по рецепту врача. Приобретение лекарственных препаратов осуществляется только в торговом зале аптеки',
                },
            },
        ];

        const warning = getWarning(warnings);
        const expected = {
            type: warnings[1].type,
            warning: warnings[1].value.full,
        };

        expect(warning).toEqual(expected);
    });

    test("should return text of 'childfood6' warning", () => {
        const warnings = [
            {
                type: 'appearance',
                value: {
                    full:
                        'Внешний вид товаров и/или упаковки может быть изменён изготовителем и отличаться от изображенных на Яндекс.Маркете.',
                    short:
                        'Внешний вид товаров и/или упаковки может быть изменён изготовителем и отличаться от изображенных на Яндекс.Маркете.',
                },
            },
            {
                type: 'childfood6',
                value: {
                    full: 'Необходима консультация специалистов. Для питания детей с 6 месяцев',
                    short: 'Необходима консультация специалистов. Для питания детей с 6 месяцев',
                },
            },
        ];

        const warning = getWarning(warnings);
        const expected = {
            type: warnings[1].type,
            warning: warnings[1].value.full,
        };

        expect(warning).toEqual(expected);
    });

    test("test with new unknown warning type should return text of 'perfum' warning ", () => {
        const warnings = [
            {
                type: 'test',
                value: {
                    full: 'Новый тестовый дисклеймер',
                    short: 'Новый тестовый дисклеймер',
                },
            },
            {
                type: 'perfum',
                value: {
                    full:
                        'Информацию об обязательном подтверждении соответствия товаров требованиям законодательства РФ запрашивайте в магазине',
                    short:
                        'Информацию об обязательном подтверждении соответствия товаров требованиям законодательства РФ запрашивайте в магазине',
                },
            },
        ];

        const warning = getWarning(warnings);
        const expected = {
            type: warnings[1].type,
            warning: warnings[1].value.full,
        };

        expect(warning).toEqual(expected);
    });

    test('test with empty warning array should return undefined', () => {
        const warnings: Array<ReportWarning> = [];

        const warning = getWarning(warnings);
        const expected = undefined;

        expect(warning).toEqual(expected);
    });

    test('test with undefined should return undefined', () => {
        const warnings = undefined;

        const warning = getWarning(warnings);
        const expected = undefined;

        expect(warning).toEqual(expected);
    });
});
