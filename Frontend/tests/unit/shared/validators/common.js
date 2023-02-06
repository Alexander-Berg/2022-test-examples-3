const validator = require('../../../../src/shared/validators/common');

describe('common validator', function() {
    describe('configOverride', function() {
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

    describe('notificationMode', function() {
        it('Должен возвращать успешный результат в случае передачи валидного значения поля', function() {
            const input = {
                preset: 'all',
                workflowNotificationChannels: ['yandex-chats'],
            };
            const result = validator.fields.notificationMode.validate(input);
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку валидации в случае некорректного пресета', function() {
            const input = {
                preset: 'none',
                workflowNotificationChannels: [],
            };
            const result = validator.fields.notificationMode.validate(input);
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });

        it('Должен возвращать ошибку валидации в случае некорректного канала доставки', function() {
            const input = {
                preset: 'all',
                workflowNotificationChannels: ['email', 'yand', 'test'],
            };
            const result = validator.fields.notificationMode.validate(input);
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('approveMode', function() {
        it('Должен возвращать успешный результат в случае передачи валидного значения поля', function() {
            const result = validator.fields.approveMode.validate('manual');
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать успешный результат для пустого поля', function() {
            const result = validator.fields.approveMode.validate();
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку в случае передачи невалидного значения поля', function() {
            const result = validator.fields.approveMode.validate(false);
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.isAbove(result.errorsMessage.length, 0);
        });
    });

    describe('questionGroupsCount', function() {
        it('Должен возвращать успешный результат в случае передачи валидного значения поля', function() {
            const result = validator.validateQuestionGroupsCount([
                {
                    key: 'group-1',
                },
                {
                    key: 'group-2',
                },
                {
                    key: 'group-3',
                },
            ]);
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку в случае передачи невалидного значения поля', function() {
            const result = validator.validateQuestionGroupsCount(new Array(21).fill({}));
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.strictEqual(result.errorsMessage, validator.ERROR_MESSAGES.QUESTION_GROUPS_COUNT_ABOVE_LIMIT);
        });
    });

    describe('questionsCount', function() {
        it('Должен возвращать успешный результат в случае передачи валидного значения поля', function() {
            const result = validator.validateQuestionsCount([
                {
                    key: 'group-1',
                    poll: new Array(25),
                },
                {
                    key: 'group-2',
                    poll: new Array(20),
                },
                {
                    key: 'group-3',
                    poll: new Array(5),
                },
            ]);
            assert.deepEqual(result, { isValid: true });
        });

        it('Должен возвращать ошибку в случае передачи невалидного значения поля', function() {
            const result = validator.validateQuestionsCount([
                {
                    key: 'group-1',
                    poll: new Array(25),
                },
                {
                    key: 'group-2',
                    poll: new Array(25),
                },
                {
                    key: 'group-3',
                    poll: new Array(1),
                },
            ]);
            assert.strictEqual(result.isValid, false);
            assert.isString(result.errorsMessage);
            assert.strictEqual(result.errorsMessage, validator.ERROR_MESSAGES.QUESTIONS_COUNT_ABOVE_LIMIT);
        });
    });
});
