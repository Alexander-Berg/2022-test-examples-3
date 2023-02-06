const validator = require('../../../../src/shared/validators/layout');

describe('layout validator', function() {
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

    describe('question required', function() {
        it('Должен возвращать успешный результат в случае наличия непустого вопроса', function() {
            const result = validator.fields.question.required('Описание эксперимента');
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае пустого вопроса', function() {
            const result = validator.fields.question.required('');
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('layouts', function() {
        describe('layoutsFilled', function() {
            it(
                'Должен возвращать успешный результат в случае наличия по крайней мере двух картинок в первом экране',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                    { origUrl: 'url2' },
                                ],
                            },
                        ],
                    };
                    const result = validator.fields.layouts.layoutsFilled(layouts);
                    assert.deepEqual(result, { isValid: true });
                },
            );

            it('Должен возвращать ошибку валидации в случае отсутствия двух картинок на первом экране', function() {
                const layouts = {
                    layouts: [
                        {
                            screens: [
                                { origUrl: 'url1' },
                            ],
                        },
                    ],
                };
                const result = validator.fields.layouts.layoutsFilled(layouts);
                assert.strictEqual(result.isValid, false);
                assert.isString(result.errorsMessage);
                assert.isAbove(result.errorsMessage.length, 0);
            });
        });

        describe('screensHasSameSize', function() {
            it(
                'Должен возвращать успешный результат в случае наличия у всех экранов одинакового количества картинок',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                    { origUrl: 'url2' },
                                ],
                            },
                            {
                                screens: [
                                    { origUrl: 'url3' },
                                    { origUrl: 'url4' },
                                ],
                            },
                            {
                                screens: [
                                    { origUrl: 'url5' },
                                    { origUrl: 'url6' },
                                ],
                            },
                        ],
                    };
                    const result = validator.fields.layouts.screensHasSameSize(layouts);
                    assert.deepEqual(result, { isValid: true });
                },
            );

            it(
                'Должен возвращать ошибку валидации в случае, если экраны имеют разное количество картинок',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                    { origUrl: 'url2' },
                                ],
                            },
                            {
                                screens: [
                                    { origUrl: 'url3' },
                                ],
                            },
                            {
                                screens: [
                                    { origUrl: 'url4' },
                                    { origUrl: 'url5' },
                                ],
                            },
                        ],
                    };
                    const result = validator.fields.layouts.screensHasSameSize(layouts);
                    assert.strictEqual(result.isValid, false);
                    assert.isString(result.errorsMessage);
                    assert.isAbove(result.errorsMessage.length, 0);
                },
            );
        });

        describe('screensNamesUnique', function() {
            it('Должен возвращать успешный результат в случае, если все экраны имеют уникальные названия', function() {
                const layouts = {
                    layouts: [
                        {
                            screens: [
                                { origUrl: 'url1' },
                            ],
                        },
                        {
                            screens: [
                                { origUrl: 'url2' },
                            ],
                        },
                    ],
                    screens: [
                        {
                            name: 'Экран 1',
                        },
                        {
                            name: 'Экран 2',
                        },
                    ],
                };
                const result = validator.fields.layouts.screensNamesUnique(layouts);
                assert.deepEqual(result, { isValid: true });
            });

            it(
                'Должен возвращать ошибку валидации в случае наличия хотя бы двух экранов с одинаковыми названиями',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                ],
                            },
                            {
                                screens: [
                                    { origUrl: 'url2' },
                                ],
                            },
                        ],
                        screens: [
                            {
                                name: 'Одинаковое название',
                            },
                            {
                                name: 'Одинаковое название',
                            },
                        ],
                    };
                    const result = validator.fields.layouts.screensNamesUnique(layouts);
                    assert.strictEqual(result.isValid, false);
                    assert.isString(result.errorsMessage);
                    assert.isAbove(result.errorsMessage.length, 0);
                },
            );
        });

        describe('screensNamesIndicated', function() {
            it(
                'Должен возвращать успешный результат в случае, если у всех экранов указано непустое название',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                ],
                            },
                            {
                                screens: [
                                    { origUrl: 'url2' },
                                ],
                            },
                        ],
                        screens: [
                            {
                                name: 'Экран 1',
                            },
                            {
                                name: 'Экран 2',
                            },
                            {
                                name: 'Экран 3',
                            },
                        ],
                    };
                    const result = validator.fields.layouts.screensNamesIndicated(layouts);
                    assert.deepEqual(result, { isValid: true });
                },
            );

            it(
                'Должен возвращать ошибку валидации в случае, если хотя бы у одного экрана не будет названия',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                ],
                            },
                            {
                                screens: [
                                    { origUrl: 'url2' },
                                ],
                            },
                        ],
                        screens: [
                            {
                                name: 'Экран 1',
                            },
                            {
                                name: '',
                            },
                            {
                                name: 'Экран 3',
                            },
                        ],
                    };
                    const result = validator.fields.layouts.screensNamesIndicated(layouts);
                    assert.strictEqual(result.isValid, false);
                    assert.isString(result.errorsMessage);
                    assert.isAbove(result.errorsMessage.length, 0);
                },
            );

            it(
                'Должен возвращать ошибку валидации в случае, если хотя бы у одного экрана значение null',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                ],
                            },
                            {
                                screens: [
                                    { origUrl: 'url2' },
                                ],
                            },
                        ],
                        screens: [
                            {
                                name: 'Экран 1',
                            },
                            null,
                            null,
                        ],
                    };
                    const result = validator.fields.layouts.screensNamesIndicated(layouts);
                    assert.strictEqual(result.isValid, false);
                    assert.isString(result.errorsMessage);
                    assert.isAbove(result.errorsMessage.length, 0);
                },
            );
        });

        describe('systemsNamesUnique', function() {
            it(
                'Должен возвращать успешный результат в случае, если все варианты имеют уникальные названия',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                    { origUrl: 'url2' },
                                ],
                            },
                        ],
                        systems: [
                            {
                                name: 'Вариант 1',
                            },
                            {
                                name: 'Вариант 2',
                            },
                        ],
                    };
                    const result = validator.fields.layouts.systemsNamesUnique(layouts);
                    assert.deepEqual(result, { isValid: true });
                },
            );

            it(
                'Должен возвращать ошибку валидации в случае наличия хотя бы двух вариантов с одинаковыми названиями',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                    { origUrl: 'url2' },
                                ],
                            },
                        ],
                        systems: [
                            {
                                name: 'Одинаковое название',
                            },
                            {
                                name: 'Одинаковое название',
                            },
                        ],
                    };
                    const result = validator.fields.layouts.systemsNamesUnique(layouts);
                    assert.strictEqual(result.isValid, false);
                    assert.isString(result.errorsMessage);
                    assert.isAbove(result.errorsMessage.length, 0);
                },
            );
        });

        describe('systemsNamesIndicated', function() {
            it(
                'Должен возвращать успешный результат в случае, если у всех вариантов указано непустое название',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                    { origUrl: 'url2' },
                                    { origUrl: 'url3' },
                                ],
                            },
                        ],
                        systems: [
                            {
                                name: 'Вариант 1',
                            },
                            {
                                name: 'Вариант 2',
                            },
                            {
                                name: 'Вариант 3',
                            },
                        ],
                    };
                    const result = validator.fields.layouts.systemsNamesIndicated(layouts);
                    assert.deepEqual(result, { isValid: true });
                },
            );

            it(
                'Должен возвращать ошибку валидации в случае, если хотя бы у одного варианта не будет названия',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                    { origUrl: 'url2' },
                                    { origUrl: 'url3' },
                                ],
                            },
                        ],
                        systems: [
                            {
                                name: 'Вариант 1',
                            },
                            {
                                name: '',
                            },
                            {
                                name: 'Вариант 3',
                            },
                        ],
                    };
                    const result = validator.fields.layouts.systemsNamesIndicated(layouts);
                    assert.strictEqual(result.isValid, false);
                    assert.isString(result.errorsMessage);
                    assert.isAbove(result.errorsMessage.length, 0);
                },
            );

            it(
                'Должен возвращать ошибку валидации в случае, если хотя бы один вариант равно null-у',
                function() {
                    const layouts = {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'url1' },
                                    { origUrl: 'url2' },
                                    { origUrl: 'url3' },
                                ],
                            },
                        ],
                        systems: [
                            {
                                name: 'Вариант 1',
                            },
                            null,
                            {
                                name: 'Вариант 3',
                            },
                        ],
                    };
                    const result = validator.fields.layouts.systemsNamesIndicated(layouts);
                    assert.strictEqual(result.isValid, false);
                    assert.isString(result.errorsMessage);
                    assert.isAbove(result.errorsMessage.length, 0);
                },
            );
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
