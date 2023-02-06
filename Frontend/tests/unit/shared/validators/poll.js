const validator = require('../../../../src/shared/validators/poll');

describe('poll validator', function() {
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

    describe('overlap', function() {
        describe('required', function() {
            it('Должен возвращать успешный результат в случае наличия значения', function() {
                const result = validator.fields.overlap.required('10');
                assert.deepEqual(result, { isValid: true });
            });

            it('Должен возвращать ошибку валидации в случае отсутствия значения', function() {
                const result = validator.fields.overlap.required('');
                assert.strictEqual(result.isValid, false);
                assert.isString(result.errorsMessage);
                assert.isAbove(result.errorsMessage.length, 0);
            });
        });

        describe('number', function() {
            it('Должен возвращать успешный результат в случае числового значения', function() {
                const result = validator.fields.overlap.number('10');
                assert.deepEqual(result, { isValid: true });
            });

            it('Должен возвращать ошибку валидации в случае нечислового значения', function() {
                const result = validator.fields.overlap.number('string');
                assert.strictEqual(result.isValid, false);
                assert.isString(result.errorsMessage);
                assert.isAbove(result.errorsMessage.length, 0);
            });
        });

        describe('aboveMinimum', function() {
            it('Должен возвращать успешный результат в случае значения 10', function() {
                const result = validator.fields.overlap.aboveMinimum('10');
                assert.deepEqual(result, { isValid: true });
            });

            it('Должен возвращать успешный результат в случае значения больше 10', function() {
                const result = validator.fields.overlap.aboveMinimum('15');
                assert.deepEqual(result, { isValid: true });
            });

            it('Должен возвращать ошибку валидации в случае значения меньше 1', function() {
                const result = validator.fields.overlap.aboveMinimum('0');
                assert.strictEqual(result.isValid, false);
                assert.isString(result.errorsMessage);
                assert.isAbove(result.errorsMessage.length, 0);
            });
        });

        describe('underMaximum', function() {
            it('Должен возвращать успешный результат в случае значения 3000', function() {
                const result = validator.fields.overlap.underMaximum('3000');
                assert.deepEqual(result, { isValid: true });
            });

            it('Должен возвращать успешный результат в случае значения меньше 3000', function() {
                const result = validator.fields.overlap.underMaximum('900');
                assert.deepEqual(result, { isValid: true });
            });

            it('Должен возвращать ошибку валидации в случае значения больше 3000', function() {
                const result = validator.fields.overlap.underMaximum('3001');
                assert.strictEqual(result.isValid, false);
                assert.isString(result.errorsMessage);
                assert.isAbove(result.errorsMessage.length, 0);
            });
        });
    });

    describe('pollHasQuestions', function() {
        it('Должен возвращать true в случае передачи опроса с одним вопросом', function() {
            const poll = [
                {
                    type: 'question',
                },
            ];
            const result = validator.pollHasQuestions(poll);
            assert.strictEqual(result, true);
        });

        it('Должен возвращать true в случае передачи опроса с одной галочкой', function() {
            const poll = [
                {
                    type: 'checkbox',
                },
            ];
            const result = validator.pollHasQuestions(poll);
            assert.strictEqual(result, true);
        });

        it('Должен возвращать true в случае передачи опроса с одной радио-группой', function() {
            const poll = [
                {
                    type: 'radio',
                },
            ];
            const result = validator.pollHasQuestions(poll);
            assert.strictEqual(result, true);
        });

        it('Должен возвращать true в случае передачи опроса с одной шкалой', function() {
            const poll = [
                {
                    type: 'scale',
                },
            ];
            const result = validator.pollHasQuestions(poll);
            assert.strictEqual(result, true);
        });

        it('Должен возвращать false в случае отсутствия в опросе хотя бы одного интерактивного элемента',
            function() {
                const poll = [
                    {
                        type: 'text',
                    },
                ];
                const result = validator.pollHasQuestions(poll);
                assert.strictEqual(result, false);
            },
        );
    });

    describe('poll notEmpty', function() {
        it('Должен возвращать успешный результат в случае передачи опроса с интерактивным элементом', function() {
            const poll = [
                {
                    type: 'question',
                },
                {
                    type: 'text',
                },
            ];

            const result = validator.fields.poll.notEmpty(poll);
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае отсутствия в опросе хотя бы одного интерактивного элемента',
            function() {
                const questionGroups = [
                    {
                        poll: [
                            {
                                type: 'image',
                            },
                        ],
                    },
                    {
                        poll: [
                            {
                                type: 'text',
                            },
                        ],
                    },
                ];
                const result = validator.fields.poll.notEmpty(questionGroups);
                assert.strictEqual(result.isValid, false);
                assert.isString(result.errorsMessage);
                assert.isAbove(result.errorsMessage.length, 0);
            },
        );
    });

    describe('multipartPoll notEmpty', function() {
        it('Должен возвращать успешный результат в случае передачи опроса с интерактивным элементом на одной из страниц', function() {
            const questionGroups = [
                {
                    poll: [
                        {
                            type: 'question',
                        },
                    ],
                },
                {
                    poll: [
                        {
                            type: 'text',
                        },
                    ],
                },
            ];
            const result = validator.fields.multipartPoll.notEmpty(questionGroups);
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае отсутствия в опросе хотя бы одного интерактивного элемента',
            function() {
                const questionGroups = [
                    {
                        poll: [
                            {
                                type: 'image',
                            },
                        ],
                    },
                    {
                        poll: [
                            {
                                type: 'text',
                            },
                        ],
                    },
                ];
                const result = validator.fields.multipartPoll.notEmpty(questionGroups);
                assert.strictEqual(result.isValid, false);
                assert.isString(result.errorsMessage);
                assert.isAbove(result.errorsMessage.length, 0);
            },
        );
    });

    describe('multipartPoll noItemErrors', function() {
        it('Должен возвращать успешный результат в случае передачи опроса без ошибок', function() {
            const questionGroups = [
                {
                    poll: [
                        {
                            type: 'text',
                            data: {
                                text: 'Text 1',
                            },
                            error: null,
                        },
                        {
                            type: 'radio',
                            data: {
                                question: 'Q1',
                                options: [
                                    {
                                        text: 'var 1',
                                    },
                                    {
                                        text: 'var 2',
                                    },
                                ],
                                hasOther: true,
                            },
                            error: null,
                        },
                    ],
                },
                {
                    poll: [
                        {
                            type: 'radio',
                            data: {
                                question: 'Q2',
                                options: [
                                    {
                                        text: 'var 1',
                                    },
                                    {
                                        text: 'var 2',
                                    },
                                ],
                                hasOther: true,
                            },
                            error: null,
                        },
                    ],
                },
            ];
            const result = validator.fields.multipartPoll.noItemErrors(questionGroups);
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае присутствия ошибок на одной из страниц',
            function() {
                const questionGroups = [
                    {
                        poll: [
                            {
                                type: 'text',
                                data: {
                                    text: 'Text 1',
                                },
                                error: null,
                            },
                            {
                                type: 'radio',
                                data: {
                                    question: 'Q1',
                                    options: [
                                        {
                                            text: 'var 1',
                                        },
                                        {
                                            text: 'var 2',
                                        },
                                    ],
                                    hasOther: true,
                                },
                                error: null,
                            },
                        ],
                    },
                    {
                        poll: [
                            {
                                type: 'image',
                                data: {
                                    url: null,
                                },
                                error: 'Нужно загрузить картинку',
                            },
                        ],
                    },
                ];
                const result = validator.fields.multipartPoll.noItemErrors(questionGroups);
                assert.strictEqual(result.isValid, false);
                assert.isString(result.errorsMessage);
                assert.isAbove(result.errorsMessage.length, 0);
            },
        );
    });
});
