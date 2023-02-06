const assert = require('assert');
const _ = require('lodash');
const { schema } = require('yandex-cfg');
const catchErrorAsync = require('catch-error-async');

const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const MailTemplate = require('models/mailTemplate');

const { registrationInvitationStatusEnum } = schema;

const testDbType = DbType.internal;

describe('MailTemplate model', () => {
    beforeEach(cleanDb);

    describe('create', () => {
        it('should create a new mail template', async() => {
            const templateData = {
                name: 'Тестовый 1',
                externalId: 123,
                externalLetterId: 1123,
                externalSlug: 'WA8CHB23-SFU1',
                withStatuses: [registrationInvitationStatusEnum.invite],
            };
            const mailTemplate = new MailTemplate(templateData, { dbType: testDbType });
            const templateId = await mailTemplate.create({ dbType: testDbType });
            const actual = await db.mailTemplate.findAll({
                attributes: { exclude: ['createdAt', 'systemAction', 'id', 'text', 'title', 'isDefault'] },
                dbType: testDbType,
            });

            assert.ok(/\d/.test(templateId));
            assert.strictEqual(actual.length, 1);
            assert.deepStrictEqual(actual[0].toJSON(), templateData);
        });

        it('should throw if mail template with id already exists', async() => {
            await factory.mailTemplate.create({ id: 10 });

            const mailTemplate = new MailTemplate({
                id: 10,
                name: 'Тестовый 1',
                externalSlug: 'WA8CHB23-SFU2',
                withStatuses: [registrationInvitationStatusEnum.invite],
            }, { dbType: testDbType });
            const error = await catchErrorAsync(mailTemplate.create.bind(mailTemplate));

            assert.equal(error.message, 'Mail template already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                id: 10,
                externalSlug: 'WA8CHB23-SFU2',
            });
        });

        it('should throw if mail template with externalSlug already exists', async() => {
            await factory.mailTemplate.create({ id: 10, externalSlug: 'WA8CHB23-SFU2' });

            const mailTemplate = new MailTemplate({
                id: 11,
                name: 'Тестовый 1',
                externalSlug: 'WA8CHB23-SFU2',
                withStatuses: [registrationInvitationStatusEnum.invite],
            }, { dbType: testDbType });
            const error = await catchErrorAsync(mailTemplate.create.bind(mailTemplate));

            assert.equal(error.message, 'Mail template already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                id: 11,
                externalSlug: 'WA8CHB23-SFU2',
            });
        });

        it('should throw if mail template with systemAction already exists', async() => {
            await factory.mailTemplate.create({ id: 10, systemAction: 'when_registration' });

            const mailTemplate = new MailTemplate({
                id: 11,
                name: 'Тестовый 1',
                externalSlug: 'WA8CHB23-SFU2',
                systemAction: 'when_registration',
                withStatuses: [registrationInvitationStatusEnum.invite],
            }, { dbType: testDbType });
            const error = await catchErrorAsync(mailTemplate.create.bind(mailTemplate));

            assert.equal(error.message, 'Mail template already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                id: 11,
                systemAction: 'when_registration',
                externalSlug: 'WA8CHB23-SFU2',
            });
        });
    });

    describe('findOne', () => {
        it('should find a mail template', async() => {
            const templates = [
                {
                    id: 1,
                    name: 'Тестовый 1',
                    externalSlug: 'WA8CHB23-SFU1',
                    externalId: 12,
                    externalLetterId: 13286,
                    groups: [],
                    systemAction: null,
                    text: null,
                    title: null,
                    withStatuses: [registrationInvitationStatusEnum.invite],
                },
                {
                    id: 2,
                    name: 'Тестовый 2',
                    externalSlug: 'WA8CHB23-SFU2',
                    externalId: 13,
                    externalLetterId: 13285,
                    groups: [],
                    systemAction: null,
                    text: null,
                    title: null,
                    withStatuses: [registrationInvitationStatusEnum.refuse],
                },
            ];

            await factory.mailTemplate.create(templates);

            const actual = await MailTemplate.findOne({ id: 2, dbType: testDbType });

            assert.deepEqual(_.omit(actual.toJSON(), 'createdAt'), templates[1]);
        });

        it('should throw if mail template is not found', async() => {
            const error = await catchErrorAsync(
                MailTemplate.findOne.bind(MailTemplate), { id: 1, scope: 'one', dbType: testDbType },
            );

            assert.equal(error.message, 'Mail template not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 1,
                scope: 'one',
                dbType: testDbType,
            });
        });
    });

    describe('count', () => {
        it('should count all mail templates', async() => {
            const templates = _
                .range(1, 26)
                .map(num => ({
                    id: num,
                    name: `Тестовый ${num}`,
                    externalSlug: `WA8CHB23-SFU${num}`,
                    externalId: num,
                    externalLetterId: num,
                    systemAction: null,
                    withStatuses: [registrationInvitationStatusEnum.refuse],
                }));

            await factory.mailTemplate.create(templates);

            const actual = await MailTemplate.count({ scope: 'list', dbType: testDbType });

            assert.equal(actual, 25);
        });
    });

    describe('findList', () => {
        it('should find all mail templates', async() => {
            const mailTemplates = _
                .range(1, 26)
                .map(num => ({
                    id: num,
                    name: `Тестовый ${num}`,
                    externalSlug: `WA8CHB23-SFU${num}`,
                    externalId: num,
                    externalLetterId: num,
                    systemAction: null,
                    withStatuses: [registrationInvitationStatusEnum.refuse],
                }));

            await factory.mailTemplate.create(mailTemplates);

            const actual = await MailTemplate.findAll({ scope: 'list', order: [['id']], dbType: testDbType });

            assert.equal(actual.length, 25);
            assert.equal(actual[0].name, 'Тестовый 1');
            assert.equal(actual[1].name, 'Тестовый 2');
            assert.equal(actual[2].name, 'Тестовый 3');
        });
    });

    describe('delete', () => {
        it('should delete mail template', async() => {
            await factory.mailTemplate.create({ id: 15 });

            const deleted = await MailTemplate.destroy(15, { dbType: testDbType });
            const count = await db.mailTemplate.count();

            assert.ok(deleted);
            assert.equal(count, 0);
        });

        it('should throw if nonexistent mail template', async() => {
            const error = await catchErrorAsync(MailTemplate.destroy.bind(MailTemplate), 11, { dbType: testDbType });

            assert.equal(error.message, 'Mail template not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
            });
        });
    });

    describe('patch', () => {
        it('should patch an existing mail template', async() => {
            await factory.mailTemplate.create({
                id: 1,
                name: 'Тестовый 1',
                externalSlug: 'WA8CHB23-SFU1',
                externalLetterId: null,
                systemAction: null,
                withStatuses: [registrationInvitationStatusEnum.invite],
            });

            const mailTemplate = new MailTemplate({
                id: 1,
                name: 'Тестовый изменнный',
            }, { dbType: testDbType });
            const mailTemplateId = await mailTemplate.patch({ dbType: testDbType });
            const actual = await db.mailTemplate.findAll();

            assert.ok(/\d/.test(mailTemplateId));
            assert.equal(actual.length, 1);
            assert.equal(actual[0].name, 'Тестовый изменнный');
        });

        it('should not patch other mail templates', async() => {
            await factory.mailTemplate.create([
                {
                    id: 1,
                    name: 'Тестовый 1',
                    externalSlug: 'WA8CHB23-SFU1',
                    externalId: 12,
                    externalLetterId: 134,
                    systemAction: null,
                    withStatuses: [registrationInvitationStatusEnum.invite],
                },
                {
                    id: 2,
                    name: 'Тестовый 2',
                    externalSlug: 'WA8CHB23-SFU2',
                    externalId: 13,
                    externalLetterId: 166,
                    systemAction: null,
                    withStatuses: [registrationInvitationStatusEnum.invite],
                },
            ]);

            const mailTemplate = new MailTemplate({
                id: 1,
                name: 'Тестовый измененный',
            }, { dbType: testDbType });
            const mailTemplateId = await mailTemplate.patch({ dbType: testDbType });
            const actual = await db.mailTemplate.findAll({ order: [['id']] });

            assert.ok(/\d/.test(mailTemplateId));
            assert.equal(actual.length, 2);

            assert.equal(actual[0].name, 'Тестовый измененный');
            assert.equal(actual[1].name, 'Тестовый 2');
        });

        it('should throw on externals already exist', async() => {
            await factory.mailTemplate.create([
                {
                    id: 1,
                    name: 'Тестовый 1',
                    externalId: 111,
                    externalSlug: 'QWER',
                    externalLetterId: null,
                    systemAction: null,
                },
                {
                    id: 2,
                    name: 'Тестовый 2',
                    externalId: 222,
                    externalSlug: 'ASDF',
                    externalLetterId: null,
                    systemAction: null,
                },
            ]);

            const mailTemplate = new MailTemplate({
                id: 1,
                externalId: 222,
                externalSlug: 'ASDF',
            }, { dbType: testDbType });
            const error = await catchErrorAsync(mailTemplate.patch.bind(mailTemplate));

            assert.equal(error.message, 'Mail template already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                externalId: 222,
                externalSlug: 'ASDF',
            });
        });

        it('should throw on nonexistent mail template', async() => {
            const mailTemplate = new MailTemplate({ id: 1 }, { dbType: testDbType });
            const error = await catchErrorAsync(mailTemplate.patch.bind(mailTemplate));

            assert.equal(error.message, 'Mail template not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 1,
            });
        });
    });
});
