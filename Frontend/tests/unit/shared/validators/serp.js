const validator = require('../../../../src/shared/validators/serp');

describe('serp validator', function() {
    describe('title required', function() {
        it('Должен возвращать успешный результат в случае наличия непустого значения', function() {
            const result = validator.fields.title.required('Заголовок эксперимента');
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае пустого значения', function() {
            const result = validator.fields.title.required('');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('description required', function() {
        it('Должен возвращать успешный результат в случае наличия непустого значения', function() {
            const result = validator.fields.description.required('Описание эксперимента');
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае пустого значения', function() {
            const result = validator.fields.description.required('');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('poolTitle', function() {
        it('Должен возвращать успешный результат в случае передачи не-null значения', function() {
            const result = validator.fields.poolTitle.validate('touch_chrome_66_iphone');
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае передачи значения null', function() {
            const result = validator.fields.poolTitle.validate(null);
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('cross', function() {
        it('Должен возвращать успешный результат в случае передачи положительного значения', function() {
            const result = validator.fields.cross.validate(5);
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать успешный результат в случае передачи нулевого значения', function() {
            const result = validator.fields.cross.validate(0);
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае передачи отрицательного значения', function() {
            const result = validator.fields.cross.validate(-5);
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать успешный результат в случае, если число запросов больше чем запросов на пару систем', function() {
            const result = validator.fields.cross.validate(5, 10);
            assert.deepEqual(result, { isValid: true });
        });
        it('Должен возвращать ошибку валидации, если число запросов меньше чем запросов на пару систем', function() {
            const result = validator.fields.cross.validate(10, 5);
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('options', function() {
        it('Должен возвращать успешный результат в случае передачи двух систем', function() {
            const options = [
                {
                    title: 'Yandex',
                    engine: 'yandex-web',
                },
                {
                    title: 'Google',
                    engine: 'google-web',
                },
            ];
            const result = validator.fields.options.validate(options);
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать успешный результат в случае передачи более двух систем', function() {
            const options = [
                {
                    title: 'Yandex Prod',
                    engine: 'yandex-web',
                },
                {
                    title: 'Yandex Test',
                    engine: 'yandex-web',
                },
                {
                    title: 'Google',
                    engine: 'google-web',
                },
            ];
            const result = validator.fields.options.validate(options);
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае передачи одной системы', function() {
            const options = [
                {
                    title: 'Yandex',
                    engine: 'yandex-web',
                },
            ];
            const result = validator.fields.options.validate(options);
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('customCross', function() {
        let options;

        beforeEach(function() {
            options = [
                {
                    title: 'Yandex Prod',
                    engine: 'yandex-web',
                },
                {
                    title: 'Yandex Test',
                    engine: 'yandex-web',
                },
                {
                    title: 'Google',
                    engine: 'google-web',
                },
            ];
        });

        it('Должен возвращать успешный результат, если в объекте нет дубликатов', function() {
            const customCross = [
                {
                    left: 0,
                    right: 1,
                    count: '0',
                },
                {
                    left: 1,
                    right: 2,
                    count: '0',
                },
            ];

            const result = validator.fields.customCross.validate(customCross, options);
            assert.strictEqual(result.isValid, true);
        });

        it('Должен возвращать ошибку валидации, если в объекте есть дубликаты', function() {
            const customCross = [
                {
                    left: 0,
                    right: 1,
                    count: '0',
                },
                {
                    left: 1,
                    right: 2,
                    count: '0',
                },
                {
                    left: 1,
                    right: 0,
                    count: '5',
                },
            ];
            const expectedErrorMessage = 'Дублирующиеся пары в customCross, индексы: [0, 2]';

            const result = validator.fields.customCross.validate(customCross, options);
            assert.strictEqual(result.isValid, false);
            assert.strictEqual(result.errorsMessage, expectedErrorMessage);
        });

        it('Должен возвращать ошибку валидации, если в объекте есть дубликаты', function() {
            const customCross = [
                {
                    left: 0,
                    right: 1,
                    count: '0',
                },
                {
                    left: 1,
                    right: 2,
                    count: '0',
                },
                {
                    left: 1,
                    right: 0,
                    count: '5',
                },
            ];
            const expectedErrorMessage = 'Дублирующиеся пары в customCross, индексы: [0, 2]';

            const result = validator.fields.customCross.validate(customCross, options);
            assert.strictEqual(result.isValid, false);
            assert.strictEqual(result.errorsMessage, expectedErrorMessage);
        });

        it('Должен возвращать успешный результат в случае, если не все сравнения систем отменены', function() {
            const customCross = [
                {
                    left: 0,
                    right: 1,
                    count: '0',
                },
                {
                    left: 0,
                    right: 2,
                    count: '0',
                },
                // Сравнение системы с индексом 1 с системой с индексом 2 не отменено
            ];
            const result = validator.fields.customCross.validate(customCross, options);
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае, если отменены все сравнения систем', function() {
            const customCross = [
                {
                    left: 0,
                    right: 1,
                    count: '0',
                },
                {
                    left: 0,
                    right: 2,
                    count: '0',
                },
                {
                    left: 1,
                    right: 2,
                    count: '0',
                },
            ];
            const result = validator.fields.customCross.validate(customCross, options);
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('config override', function() {
        it('Должен возвращать успешный результат в случае передачи валидного JSON', function() {
            const result = validator.fields.configOverride.validate('{"property":"override"}');
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае передачи невалидного JSON', function() {
            const result = validator.fields.configOverride.validate('{"property":string_not_in_quotes}');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('filter isValidPythonCode', function() {
        it('Должен возвращать успешный результат в случае передачи строки, начинающейся c "class Filter"', function() {
            const result = validator.fields.filter.validate({ val: 'class Filter {}', source: 'text' });
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае передачи строки, не начинающейся c "class Filter"', function() {
            const result = validator.fields.filter.validate({ val: 'some not valid string', source: 'text' });
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('overlap', function() {
        it('Должен возвращать успешный результат в случае режима "default" и пустого значения', function() {
            const result = validator.fields.overlap.validate({ mode: 'default' });
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать успешный результат в случае режима "edit" и значения "1"', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '1' });
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать успешный результат в случае режима "edit" и значения "400"', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '400' });
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать успешный результат в случае режима "edit" и значения между "1" и "400"', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '200' });
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать успешный результат в случае режима "edit" и отсутствия значения', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '' });
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать успешный результат в случае режима "edit" и значения меньше 1', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '0' });
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать успешный результат в случае режима "edit" и значения больше 400', function() {
            const result = validator.fields.overlap.validate({ mode: 'edit', value: '401' });
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });
});
