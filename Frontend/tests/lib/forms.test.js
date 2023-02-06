/* eslint-disable max-len, camelcase */

const assert = require('assert');
const nock = require('nock');
const catchErrorAsync = require('catch-error-async');
const config = require('yandex-cfg');

const Forms = require('lib/forms');

const { nockFormsApiCopyForm, nockFormsApiPatchForm, nockFormsApiRemove } = require('tests/mocks');

describe('Forms', () => {
    describe('parseDataFromCreateHook', () => {
        it('should parse data from request', () => {
            const ctx = {
                header: {
                    'x-form-id': '10666',
                    'x-form-answer-id': '535297',
                    uid: '12345',
                },
                request: {
                    body: {
                        field_1: '{"question": {"label": {"ru": "Email"}, "id": 61461, "slug": "email"}, "value": "saaaaaaaaasha@yandex-team.ru"}',
                        field_2: '{"question": {"label": {"ru": "\\u0412\\u044b\\u043f\\u0430\\u0434\\u0430\\u044e\\u0449\\u0438\\u0439 \\u0441\\u043f\\u0438\\u0441\\u043e\\u043a"}, "id": 72343, "slug": "answerChoices72343"}, "value": "\\u041f\\u043e\\u0434\\u043a\\u043b\\u044e\\u0447\\u0443\\u0441\\u044c \\u043a \\u043e\\u043d\\u043b\\u0430\\u0439\\u043d-\\u0442\\u0440\\u0430\\u043d\\u0441\\u043b\\u044f\\u0446\\u0438\\u0438"}',
                        field_3: '{"question": {"label": {"ru": "\\u0418\\u043c\\u044f"}, "id": 61462, "slug": "firstName"}, "value": "Alexander"}',
                        field_4: '{"question": {"label": {"ru": "\\u0424\\u0430\\u043c\\u0438\\u043b\\u0438\\u044f"}, "id": 61463, "slug": "lastName"}, "value": "Ivanov"}',
                    },
                },
            };

            const actual = Forms.parseDataFromCreateHook(ctx);

            assert.deepStrictEqual(actual, {
                formId: 10666,
                formAnswerId: 535297,
                answers: {
                    email: {
                        label: 'Email',
                        value: 'saaaaaaaaasha@yandex-team.ru',
                    },
                    answerChoices72343: {
                        label: 'Выпадающий список',
                        value: 'Подключусь к онлайн-трансляции',
                    },
                    firstName: {
                        label: 'Имя',
                        value: 'Alexander',
                    },
                    lastName: {
                        label: 'Фамилия',
                        value: 'Ivanov',
                    },
                },
                accountData: {
                    email: 'saaaaaaaaasha@yandex-team.ru',
                    firstName: 'Alexander',
                    lastName: 'Ivanov',
                    isEmailConfirmed: false,
                    yandexuid: '12345',
                },
            });
        });

        it('should throw error if form id is invalid', async() => {
            const ctx = {
                header: {
                    'x-form-id': 'abc',
                    'x-form-answer-id': '535297',
                },
                request: { body: {} },
            };

            const error = await catchErrorAsync(
                Forms.parseDataFromCreateHook.bind(Forms), ctx,
            );

            assert.strictEqual(error.message, 'Form ID is invalid');
            assert.strictEqual(error.statusCode, 400);
            assert.deepStrictEqual(error.options, {
                internalCode: '400_III',
                value: 'abc',
            });
        });

        it('should throw error if form answer id is invalid', async() => {
            const ctx = {
                header: {
                    'x-form-id': '123',
                    'x-form-answer-id': 'abc',
                },
                request: { body: {} },
            };

            const error = await catchErrorAsync(
                Forms.parseDataFromCreateHook.bind(Forms), ctx,
            );

            assert.strictEqual(error.message, 'Form Answer ID is invalid');
            assert.strictEqual(error.statusCode, 400);
            assert.deepStrictEqual(error.options, {
                internalCode: '400_III',
                value: 'abc',
            });
        });
    });

    describe('parseDataFromValidateHook', () => {
        it('should parse data from request', () => {
            const questions = [
                {
                    slug: 'firstName',
                    id: 118397,
                    value: 'Art',
                    label: 'Имя',
                },
                {
                    slug: 'lastName',
                    id: 118398,
                    value: 'Zav',
                    label: 'Фамилия',
                },
                {
                    slug: 'email',
                    id: 118399,
                    value: 'art00@yandex-team.ru',
                    label: 'Email',
                },
                {
                    slug: 'about',
                    id: 118400,
                    value: 'test about',
                    label: 'О себе',
                },
            ];
            const ctx = {
                request: {
                    body: {
                        questions,
                        id: 9944,
                        name: 'Регистрация на testEvent',
                        slug: null,
                    },
                },
            };

            const actual = Forms.parseDataFromValidateHook(ctx);

            assert.deepStrictEqual(actual, {
                formId: 9944,
                accountData: {
                    about: 'test about',
                    firstName: 'Art',
                    lastName: 'Zav',
                    email: 'art00@yandex-team.ru',
                },
                questions,
            });
        });

        it('should throw error if form id is invalid', async() => {
            const ctx = {
                request: {
                    body: {
                        questions: [],
                        id: 'inv@lid',
                    },
                },
            };

            const error = await catchErrorAsync(
                Forms.parseDataFromValidateHook.bind(Forms), ctx,
            );

            assert.strictEqual(error.message, 'Form ID is invalid');
            assert.strictEqual(error.statusCode, 400);
            assert.deepStrictEqual(error.options, {
                internalCode: '400_III',
                value: 'inv@lid',
            });
        });
    });

    describe('_parseAccountData', () => {
        it('should parse account data from fields', () => {
            const fields = [
                {
                    slug: 'email',
                    label: 'Email',
                    value: 'saaaaaaaaasha@yandex-team.ru',
                },
                {
                    slug: 'answerChoices72343',
                    label: 'Выпадающий список',
                    value: 'Подключусь к онлайн-трансляции',
                },
                {
                    slug: 'firstName',
                    label: 'Имя',
                    value: 'Alexander',
                },
                {
                    slug: 'lastName',
                    label: 'Фамилия',
                    value: 'Ivanov',
                },
            ];

            const actual = Forms._parseAccountData(fields);

            assert.deepStrictEqual(actual, {
                email: 'saaaaaaaaasha@yandex-team.ru',
                firstName: 'Alexander',
                lastName: 'Ivanov',
            });
        });
    });

    describe('_formatFields', () => {
        it('should format raw fields to json', () => {
            const fields = {
                field_1: '{"question": {"label": {"ru": "Email"}, "id": 61461, "slug": "email"}, "value": "saaaaaaaaasha@yandex-team.ru"}',
                field_2: '{"question": {"label": {"ru": "\\u0412\\u044b\\u043f\\u0430\\u0434\\u0430\\u044e\\u0449\\u0438\\u0439 \\u0441\\u043f\\u0438\\u0441\\u043e\\u043a"}, "id": 72343, "slug": "answerChoices72343"}, "value": "\\u041f\\u043e\\u0434\\u043a\\u043b\\u044e\\u0447\\u0443\\u0441\\u044c \\u043a \\u043e\\u043d\\u043b\\u0430\\u0439\\u043d-\\u0442\\u0440\\u0430\\u043d\\u0441\\u043b\\u044f\\u0446\\u0438\\u0438"}',
                field_3: '{"question": {"label": {"ru": "\\u0418\\u043c\\u044f"}, "id": 61462, "slug": "firstName"}, "value": "Alexander"}',
                field_4: '{"question": {"label": {"ru": "\\u0424\\u0430\\u043c\\u0438\\u043b\\u0438\\u044f"}, "id": 61463, "slug": "lastName"}, "value": "Ivanov"}',
            };

            const actual = Forms._formatFields(fields);

            assert.deepStrictEqual(actual, [
                {
                    slug: 'email',
                    label: 'Email',
                    value: 'saaaaaaaaasha@yandex-team.ru',
                },
                {
                    slug: 'answerChoices72343',
                    label: 'Выпадающий список',
                    value: 'Подключусь к онлайн-трансляции',
                },
                {
                    slug: 'firstName',
                    label: 'Имя',
                    value: 'Alexander',
                },
                {
                    slug: 'lastName',
                    label: 'Фамилия',
                    value: 'Ivanov',
                },
            ]);
        });

        it('should save param_* fields', () => {
            const fields = {
                field_3: '{"question": {"label": {"ru": "\\u0418\\u043c\\u044f"}, "id": 61462, "slug": "param_name"}, "value": "Alexander"}',
                field_4: '{"question": {"label": {"ru": "\\u0424\\u0430\\u043c\\u0438\\u043b\\u0438\\u044f"}, "id": 61463, "slug": "param_surname"}, "value": "Ivanov"}',
            };

            const actual = Forms._formatFields(fields);

            assert.deepStrictEqual(actual, [
                {
                    slug: 'firstName',
                    label: 'Имя',
                    value: 'Alexander',
                },
                {
                    slug: 'lastName',
                    label: 'Фамилия',
                    value: 'Ivanov',
                },
            ]);
        });

        it('should save and transform value if a serializer is exist', () => {
            const fields = {
                field_3: '{"question": {"label": {"ru": "Пол"}, "id": 61462, "slug": "param_gender"}, "value": "мужской"}',
                field_4: '{"question": {"label": {"ru": "Дата рождения"}, "id": 61463, "slug": "param_birthdate"}, "value": "07 марта 1999 г."}',
            };

            const actual = Forms._formatFields(fields);

            assert.deepStrictEqual(actual, [
                {
                    slug: 'sex',
                    label: 'Пол',
                    value: 'man',
                },
                {
                    slug: 'birthDate',
                    label: 'Дата рождения',
                    value: '1999-03-07',
                },
            ]);
        });

        it('should ignore not valid fields', () => {
            const fields = {
                field_1: '{"question": {"label": {"ru": "Email"}, "id": 61461, "slug": "email"}, "value": "e@e.ru"}',
                field_5: 'not valid json',
            };

            const actual = Forms._formatFields(fields);

            assert.deepStrictEqual(actual, [
                {
                    slug: 'email',
                    label: 'Email',
                    value: 'e@e.ru',
                },
            ]);
        });

        it('should ignore labels fields', () => {
            const fields = {
                field_1: '{"question": {"label": {"ru": "Email"}, "id": 61461, "slug": "email"}, "value": "e@e.ru"}',
                field_5: '{"question": {"label": {"ru": "Напишите имя на киррилице"}, "id": 61462, "slug": "answer_statement_4234"}, "value": null}',
            };

            const actual = Forms._formatFields(fields);

            assert.deepStrictEqual(actual, [
                {
                    slug: 'email',
                    label: 'Email',
                    value: 'e@e.ru',
                },
            ]);
        });
    });

    describe('removeForm', () => {
        afterEach(nock.cleanAll);

        it('should remove form', async() => {
            nockFormsApiRemove();

            const actual = await Forms.removeForm({}, 111);

            assert.strictEqual(actual, true);
        });

        it('should return false when forms api is down', async() => {
            nock(config.forms.api).delete(/surveys\/\d+$/)
                .reply(404, {});

            const actual = await Forms.removeForm({}, 111);

            assert.strictEqual(actual, false);
        });
    });

    describe('copyForm', () => {
        afterEach(nock.cleanAll);

        it('should copy form by id', async() => {
            const formId = 10011680;
            const copiedFormId = formId + 1;
            const nockInstance = nockFormsApiCopyForm(formId);
            const nockPatchForm = nockFormsApiPatchForm(copiedFormId);

            const actual = await Forms.copyForm({}, { id: formId });

            assert.strictEqual(actual, copiedFormId);
            assert.ok(nockInstance.isDone());
            assert.ok(nockPatchForm.isDone());
        });

        it('should throw error when forms api is down', async() => {
            nock(config.forms.api)
                .post(/surveys\/.*$/)
                .reply(404, {});

            const error = await catchErrorAsync(Forms.copyForm.bind(Forms), {}, 10011680);

            assert.strictEqual(error.statusCode, 400);
        });
    });
});
