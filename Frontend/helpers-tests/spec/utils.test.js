'use strict';

const utils = require('../../utils');

describe('utils', () => {
    const message = 'Тест validateFields';
    let data;

    beforeEach(() => {
        data = {};
    });

    describe('validateFields', () => {
        it('Должна возникнуть ошибка, если в объекте data нет поля "test"', () => {
            const constraints = { required: ['test'] };

            assert.throws(
                () => utils.validateFields(data, constraints, message),
                'Тест validateFields: объект {} не содержит обязательных полей ["test"]'
            );
        });

        it('Не должна возникнуть ошибка, если в объекте data есть поле "test"', () => {
            const constraints = { required: ['test'] };

            data.test = 1;

            assert.doesNotThrow(() => utils.validateFields(data, constraints, message));
        });

        it('Должна возникнуть ошибка, если в объекте data есть лишние поля', () => {
            const constraints = { required: ['test'] };

            data.test = 1;
            data.field = 1;

            assert.throws(
                () => utils.validateFields(data, constraints, message),
                'Тест validateFields: объект {"test":1,"field":1} содержит лишние поля: "["field"]"'
            );
        });
    });
});
