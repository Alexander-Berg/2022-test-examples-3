import {testParamsForConditions} from '../resource/utils';

describe('Хелпер testParamsForConditions', () => {
    describe('если в conditions регулярка /^[0-9]+$/', () => {
        const conditions = {uid: /^[0-9]+$/};

        it('проходит проверка, если в параметрах передать 123456', () => {
            const params = {
                uid: '123456',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(true);
        });

        it('не проходит проверка, если в параметрах передать abcdef', () => {
            const params = {
                uid: 'abcdef',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(false);
        });

        it('не проходит проверка, если в параметрах передать "123456abc"', () => {
            const params = {
                uid: '123456abc',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(false);
        });

        it('не проходит проверка, если в параметрах передать "123456?test#id=124"', () => {
            const params = {
                uid: '123456?test#id=124',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(false);
        });
    });

    describe('если в conditions регулярка /^[0-9a-zA-Z]+$/', () => {
        const conditions = {uid: /^[0-9a-zA-Z]+$/};

        it('проходит проверка, если в параметрах передать 123456', () => {
            const params = {
                uid: '123456',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(true);
        });

        it('проходит проверка, если в параметрах передать abcdef', () => {
            const params = {
                uid: 'abcdef',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(true);
        });

        it('проходит проверка, если в параметрах передать "123456abc"', () => {
            const params = {
                uid: '123456abc',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(true);
        });

        it('не проходит проверка, если в параметрах передать "123456?test#id=124"', () => {
            const params = {
                uid: '123456?test#id=124',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(false);
        });
    });

    describe('если в conditions регулярка /^[a-zA-Z]+$/', () => {
        const conditions = {uid: /^[a-zA-Z]+$/};

        it('не проходит проверка, если в параметрах передать 123456', () => {
            const params = {
                uid: '123456',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(false);
        });

        it('проходит проверка, если в параметрах передать abcdef', () => {
            const params = {
                uid: 'abcdef',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(true);
        });

        it('не проходит проверка, если в параметрах передать "123456abc"', () => {
            const params = {
                uid: '123456abc',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(false);
        });

        it('не проходит проверка, если в параметрах передать "123456?test#id=124"', () => {
            const params = {
                uid: '123456?test#id=124',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(false);
        });
    });


    describe('если проверяем несколько параметров', () => {
        const conditions = {
            uid: /^[a-zA-Z]+$/,
            pageSize: /^[0-9]+$/,
        };

        it('проходит проверка, если все проверяемые параметры корректные', () => {
            const params = {
                uid: 'abcdef',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(true);
        });

        it('не проходит проверка, если один из параметров некорректный', () => {
            const params = {
                uid: '123456',
                status: ['UNPAID'],
                pageSize: '5',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(false);
        });

        it('не проходит проверка, если все параметры некорректные', () => {
            const params = {
                uid: '123456abc',
                status: ['UNPAID'],
                pageSize: 'ааа',
                rgb: 'BLUE',
                partials: ['ITEMS', 'BUYER'],
            };

            const areParamsValid = testParamsForConditions(conditions, params);

            expect(areParamsValid).toEqual(false);
        });
    });
});
