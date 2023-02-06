import {
    validateMaxLength,
    validatePhone,
    validateEmpty,
    applyPhoneMask,
    submitValidation } from '../validation';

describe('validation', () => {
    describe('Валидация формы - компоненты', () => {
        it('undefined', () => {
            expect(validateEmpty(undefined)).toEqual(true);
        });
        it('Пустая строка', () => {
            expect(validateEmpty('')).toEqual(true);
        });
        it('Строка пробелов', () => {
            expect(validateEmpty('   ')).toEqual(true);
        });
        it('Не проходит, если длина больше maxLength', () => {
            expect(validateMaxLength('123', 2)).toEqual(false);
        });
    });

    describe('Валидация формы', () => {
        const line = {
            type: 'input',
            meta: {
                required: true,
                maxLength: 20,
                validation: 'phone',
            },
        };

        it('Проверка на пустую строку', () => {
            expect(submitValidation(Object.create(line, {}), '')).toEqual(false);
        });

        it('Проверка на длину строки', () => {
            expect(submitValidation(Object.create(line, {
                meta: { value: { maxLength: 2 } },
            }), '123')).toEqual(false);
        });

        it('Проверка телефона', () => {
            expect(submitValidation(Object.create(line, {}), '+78008002020')).toEqual(true);
        });

        it('Проверка email', () => {
            expect(submitValidation(Object.create(line, {
                meta: { value: { validation: 'email' } } }), 'email@email.ru')).toEqual(true);
        });
    });

    describe('Валидация телефона', () => {
        it('Не проходит, если длина < 7 символов', () => {
            expect(validatePhone('+7234')).toEqual(false);
        });
        it('Корректный номер телефона', () => {
            expect(validatePhone('+78006002010')).toEqual(true);
        });
    });
});

describe('mask', () => {
    describe('Маска для телефона', () => {
        it('Происходит конвертация телефона по маске', () => {
            expect(applyPhoneMask('ru', '+78002002020')).toEqual('+7 800 200-20-20');
        });
        it('Возвращается изначальное значение', () => {
            expect(applyPhoneMask('', '+18002002020')).toEqual('+18002002020');
        });
    });
});
