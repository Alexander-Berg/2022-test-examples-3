const assert = require('assert');
const request = require('supertest');
const nock = require('nock');
const { schema } = require('yandex-cfg');
const { getTestRegistrationQrCodeUrl } = require('lib/utils');

const { nockTvmtool, nockBlackbox, nockSenderRenderPreview } = require('tests/mocks');

const app = require('app');
const db = require('db');
const { DbType } = require('db/constants');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');

const { registrationInvitationStatusEnum } = schema;
const testDbType = DbType.internal;

describe('mailTemplate controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('findById', () => {
        it('should find mail template', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.mailTemplate.create({
                id: 4,
                title: 'Статус приглашения на {{eventName}}',
                text: 'Хей, {{firstName}}, сохрани {{withQr}} для входа!',
                externalId: 13285,
                externalSlug: 'WA8CHB23-SFU1211',
            });

            await request(app.listen())
                .get('/v1/admin/mailTemplates/4')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({
                    id: 4,
                    name: 'Тестовый',
                    externalSlug: 'WA8CHB23-SFU1211',
                    externalId: 13285,
                    externalLetterId: 13285,
                    systemAction: null,
                    groups: [],
                    text: 'Хей, {{firstName}}, сохрани {{withQr}} для входа!',
                    title: 'Статус приглашения на {{eventName}}',
                    createdAt: '2018-12-13T11:20:25.581Z',
                    withStatuses: [registrationInvitationStatusEnum.invite],
                    variablesSchema: {
                        type: 'object',
                        properties: {
                            eventName: {
                                title: 'eventName',
                                type: ['string', 'null'],
                            },
                            firstName: {
                                title: 'firstName',
                                type: ['string', 'null'],
                            },
                            withQr: {
                                title: 'Приложить QR-код регистрации на мероприятии',
                                type: 'boolean',
                            },
                        },
                    },
                });
        });

        it('should throw error when mail template not found', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/mailTemplates/5')
                .set('Cookie', 'Session_id=user-session-id')
                .expect(404)
                .expect({
                    message: 'Mail template not found',
                    id: '5',
                    internalCode: '404_ENF',
                    dbType: testDbType,
                });
        });

        it('should throw error when mailTemplateId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/mailTemplates/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'MailTemplate ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('findAll', () => {
        it('should find all mail templates', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const createdAt = '2018-12-13T11:20:25.581Z';
            const data = [
                {
                    id: 1,
                    name: 'Тестовый 1',
                    externalSlug: 'WA8CHB23-SFU1',
                    externalId: 11596,
                    externalLetterId: 13285,
                    systemAction: null,
                    createdAt,
                },
                {
                    id: 2,
                    name: 'Тестовый 2',
                    externalSlug: 'WB8CHB23-SFU1',
                    externalId: 11597,
                    externalLetterId: 13286,
                    systemAction: null,
                    createdAt,
                },
                {
                    id: 3,
                    name: 'Тестовый 3',
                    externalSlug: 'WC8CHB23-SFU1',
                    externalId: 11598,
                    externalLetterId: 13289,
                    systemAction: null,
                    createdAt,
                },
            ];

            await factory.mailTemplate.create(data);

            await request(app.listen())
                .get('/v1/admin/mailTemplates/all')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    const [third, second, first] = body;

                    assert.equal(first.id, 1);
                    assert.equal(first.name, 'Тестовый 1');
                    assert.equal(second.id, 2);
                    assert.equal(second.name, 'Тестовый 2');
                    assert.equal(third.id, 3);
                    assert.equal(third.name, 'Тестовый 3');
                });
        });

        it('should find all mail templates with vars default values from event', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await factory.event.create({
                id: 13,
                title: 'Fronttalks',
                startDate: new Date(2018, 10, 10),
            });

            await factory.mailTemplate.create({
                id: 1,
                isDefault: false,
                name: 'Тестовый 1',
                externalSlug: 'WA8CHB23-SFU1',
                externalId: 11596,
                externalLetterId: 13285,
                systemAction: null,
                text: 'Проверка переменных из ивента {{eventName}}, {{startDate}}',
            });

            await request(app.listen())
                .get('/v1/admin/mailTemplates/all?eventId=13')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    const [actual] = body;
                    const expectedSchema = {
                        type: 'object',
                        properties: {
                            eventName: {
                                type: ['string', 'null'],
                                title: 'eventName',
                                default: 'Fronttalks',
                            },
                            startDate: {
                                type: ['string', 'null'],
                                title: 'startDate',
                                default: '10.11.2018',
                            },
                        },
                    };

                    assert.deepStrictEqual(actual, {
                        id: 1,
                        isDefault: false,
                        name: 'Тестовый 1',
                        externalLetterId: 13285,
                        externalSlug: 'WA8CHB23-SFU1',
                        withStatuses: ['invite'],
                        variablesSchema: expectedSchema,
                    });
                });
        });

        it('should find all mail templates with default event variables preset', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await factory.event.create({
                id: 13,
                title: 'Fronttalks',
                startDate: new Date(2018, 10, 10),
            });

            await factory.mailTemplate.create({
                id: 1,
                name: 'Тестовый 1',
                externalSlug: 'WA8CHB23-SFU1',
                externalId: 11596,
                externalLetterId: 13285,
                systemAction: null,
                text: 'Ивента {{eventName}} стартует через {{waitingPeriod}}',
            });

            await factory.eventMailTemplatePreset.create({
                id: 10,
                eventId: { id: 13 },
                mailTemplateId: { id: 1 },
                variables: { eventName: 'Code Fest', waitingPeriod: 'через пару дней' },
            });

            await request(app.listen())
                .get('/v1/admin/mailTemplates/all?eventId=13')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    const [actual] = body;
                    const expectedSchema = {
                        type: 'object',
                        properties: {
                            eventName: {
                                type: ['string', 'null'],
                                title: 'eventName',
                                default: 'Code Fest',
                            },
                            waitingPeriod: {
                                type: ['string', 'null'],
                                title: 'waitingPeriod',
                                default: 'через пару дней',
                            },
                        },
                    };

                    assert.deepStrictEqual(actual.variablesSchema, expectedSchema);
                });
        });

        it('should find all mail templates with pagination by group access', async() => {
            nockBlackbox();
            nockTvmtool();
            const group = { id: 1, slug: 'testGroup', name: 'testName' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.mailTemplatesGroup.create({ groupId: group.id, mailTemplateId: { id: 2 } });

            const createdAt = '2018-12-13T11:20:25.581Z';
            const data = [
                {
                    id: 1,
                    name: 'Тестовый 1',
                    externalSlug: 'WA8CHB23-SFU1',
                    externalId: 11596,
                    externalLetterId: 13285,
                    systemAction: null,
                    createdAt,
                    isDefault: true,
                },
                {
                    id: 2,
                    name: 'Тестовый 2',
                    externalSlug: 'WB8CHB23-SFU1',
                    externalId: 11597,
                    externalLetterId: 13286,
                    systemAction: null,
                    createdAt,
                    isDefault: false,
                },
                {
                    id: 3,
                    name: 'Тестовый 3',
                    externalSlug: 'WC8CHB23-SFU1',
                    externalId: 11598,
                    externalLetterId: 13289,
                    systemAction: null,
                    createdAt,
                    isDefault: true,
                },
            ];

            await factory.mailTemplate.create(data);

            await request(app.listen())
                .get('/v1/admin/mailTemplates')
                .query({ pageNumber: 2, pageSize: 1 })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    const { rows, meta } = body;

                    assert.equal(rows[0].id, data[1].id);
                    assert.deepStrictEqual(meta, {
                        totalSize: 3,
                        pageSize: 1,
                        pageNumber: 2,
                    });
                });
        });

        it('should find mail templates without system actions', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const createdAt = '2018-12-13T11:20:25.581Z';
            const withoutSystemAction = {
                id: 3,
                name: 'Тестовый 3',
                externalSlug: 'WC8CHB23-SFU1',
                externalId: 11596,
                externalLetterId: 13285,
                systemAction: null,
                withStatuses: [registrationInvitationStatusEnum.invite],
                createdAt,
            };
            const data = [
                {
                    id: 1,
                    name: 'Тестовый 1',
                    externalSlug: 'WA8CHB23-SFU1',
                    externalId: 11597,
                    externalLetterId: 13286,
                    systemAction: 'when_registration',
                    createdAt,
                },
                {
                    id: 2,
                    name: 'Тестовый 2',
                    externalSlug: 'WB8CHB23-SFU1',
                    externalId: 11598,
                    externalLetterId: 13287,
                    systemAction: 'when_registration',
                    createdAt,
                },
                withoutSystemAction,
            ];

            await factory.mailTemplate.create(data);

            await request(app.listen())
                .get('/v1/admin/mailTemplates/all')
                .query({ excludeSystem: 'true' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    const [first] = body;

                    assert.equal(body.length, 1);
                    assert.equal(first.name, withoutSystemAction.name);
                });
        });

        describe('findAll', () => {
            it('should find all mail templates by group access', async() => {
                nockBlackbox();
                nockTvmtool();
                const group = { id: 1, slug: 'testGroup', name: 'testName' };

                await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
                await factory.mailTemplatesGroup.create({ groupId: group.id, mailTemplateId: { id: 2 } });

                const createdAt = '2018-12-13T11:20:25.581Z';
                const data = [
                    {
                        id: 1,
                        name: 'Тестовый 1',
                        externalSlug: 'WA8CHB23-SFU1',
                        externalId: 11596,
                        externalLetterId: 13285,
                        systemAction: null,
                        createdAt,
                        isDefault: true,
                    },
                    {
                        id: 2,
                        name: 'Тестовый 2',
                        externalSlug: 'WB8CHB23-SFU1',
                        externalId: 11597,
                        externalLetterId: 13286,
                        systemAction: null,
                        createdAt,
                        isDefault: false,
                    },
                    {
                        id: 3,
                        name: 'Тестовый 3',
                        externalSlug: 'WC8CHB23-SFU1',
                        externalId: 11598,
                        externalLetterId: 13289,
                        systemAction: null,
                        createdAt,
                        isDefault: false,
                    },
                ];

                await factory.mailTemplate.create(data);

                await request(app.listen())
                    .get('/v1/admin/mailTemplates/all')
                    .set('Cookie', ['Session_id=user-session-id'])
                    .expect('Content-Type', /json/)
                    .expect(200)
                    .expect(({ body }) => {
                        const [second, first] = body;

                        assert.strictEqual(body.length, 2);
                        assert.equal(first.id, 1);
                        assert.equal(second.id, 2);
                    });
            });
        });
    });

    describe('Create', () => {
        it('should create an mail template by group access', async() => {
            nockBlackbox();
            nockTvmtool();
            const nockInstance = nockSenderRenderPreview({ title: 'Приглашение {{eventName}}' });
            const group = { id: 1, slug: 'test', name: 'testName' };

            await factory.group.create(group);
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });

            await request(app.listen())
                .post('/v1/admin/mailTemplates')
                .send({
                    name: 'Тестовый',
                    externalSlug: 'WA8CHB23-SFU1',
                    externalId: 11596,
                    externalLetterId: 123,
                    groups: [group],
                })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.name, 'Тестовый');
                    assert.strictEqual(body.externalSlug, 'WA8CHB23-SFU1');
                    assert.strictEqual(body.title, 'Приглашение {{eventName}}');
                });

            await request(app.listen());

            const actual = await db.mailTemplate.findAll();

            assert.ok(nockInstance.isDone());
            assert.strictEqual(actual.length, 1);
            assert.strictEqual(actual[0].name, 'Тестовый');
        });

        it('should throw error create an mail template by no group access', async() => {
            nockBlackbox();
            nockTvmtool();
            const group = { id: 1, slug: 'test', name: 'testName' };

            await factory.group.create(group);
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: { id: 2 } });

            await request(app.listen())
                .post('/v1/admin/mailTemplates')
                .send({
                    name: 'Тестовый',
                    externalSlug: 'WA8CHB23-SFU1',
                    externalId: 11596,
                    externalLetterId: 123,
                    groups: [group],
                })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403);

            const actual = await db.mailTemplate.findAll();

            assert.equal(actual.length, 0);
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/mailTemplates')
                .send({ id: 13, externalId: 11596 })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should have required property \'name\'');
                    assert.equal(
                        body.errors[1].message,
                        'should have required property \'externalSlug\'',
                    );
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/mailTemplates')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    name: 'Тестовый',
                    externalSlug: 'WA8CHB23-SFU1',
                })
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });
    });

    describe('delete', () => {
        it('should delete an mail template', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.mailTemplate.create({ id: 11 });

            await request(app.listen())
                .delete('/v1/admin/mailTemplates/11')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204)
                .expect({});
        });

        it('should throw error when mailTemplateId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/mailTemplates/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'MailTemplate ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/mailTemplates/11')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });
    });

    describe('Patch', () => {
        it('should patch a mail template', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.mailTemplate.create({ id: 13 });

            await request(app.listen())
                .patch('/v1/admin/mailTemplates')
                .send({ id: 13, name: 'Обновленный', externalSlug: 'WB8CHB23-SFU1' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.name, 'Обновленный');
                    assert.equal(body.externalSlug, 'WB8CHB23-SFU1');
                });
        });

        it('should throw error when mailTemplateId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/mailTemplates')
                .send({ id: 'inv@lid' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'MailTemplate ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .patch('/v1/admin/mailTemplates')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    slug: 'wrong',
                })
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });

        it('should update user has access by group', async() => {
            nockBlackbox();
            nockTvmtool();

            const group = { id: 1, slug: 'test', name: 'testName' };
            const data = {
                id: 1,
                name: 'Тестовый',
                externalSlug: 'WA8CHB23-SFU1',
                externalId: 11596,
                externalLetterId: 123,
            };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.mailTemplatesGroup.create({ groupId: group.id, mailTemplateId: { id: 1 } });

            await factory.mailTemplate.create(data);

            await request(app.listen())
                .patch('/v1/admin/mailTemplates')
                .set('Cookie', ['Session_id=user-session-id'])
                .send(data)
                .expect('Content-Type', /json/)
                .expect(200);
        });
    });

    describe('renderPreview', () => {
        it('should render preview a mail template', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderRenderPreview({
                title: 'Приглашение на {{eventName}}',
                text: 'Добрый день, {{firstName}}. Вы приглашены на {{eventName}}',
            });

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.mailTemplate.create({ id: 13 });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/13/render')
                .send({ eventName: 'Fronttalks' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.deepStrictEqual(body, {
                        text: 'Добрый день, {{firstName}}. Вы приглашены на Fronttalks',
                        title: 'Приглашение на Fronttalks',
                    });
                });
        });

        it('should render preview a mail template with correct qr code', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderRenderPreview({
                title: 'Приглашение на {{eventName}}',
                text: 'Добрый день!{% if withQr %} Ваш пригласительный код: <img src="{{withQr}}">{% endif %}',
            });

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.mailTemplate.create({ id: 13 });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/13/render')
                .send({ eventName: 'Fronttalks', withQr: true })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    const qrImg = getTestRegistrationQrCodeUrl();

                    assert.deepStrictEqual(body, {
                        text: `Добрый день! Ваш пригласительный код: <img src="${qrImg}">`,
                        title: 'Приглашение на Fronttalks',
                    });
                });
        });

        it('should render preview a mail template with vars default values from event', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderRenderPreview({
                title: 'Приглашение на {{eventName}}',
                text: 'Добрый день!{% if withQr %} Ваш пригласительный код: <img src="{{withQr}}">{% endif %}',
            });

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.mailTemplate.create({ id: 13 });

            await factory.event.create({
                id: 13,
                title: 'Fronttalks',
                startDate: new Date(2018, 10, 10),
            });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/13/render?eventId=13')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.deepStrictEqual(body, {
                        text: 'Добрый день!',
                        title: 'Приглашение на Fronttalks',
                    });
                });
        });

        it('should throw error when letter id is not set', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.mailTemplate.create({ id: 13, externalLetterId: null });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/13/render')
                .send({ eventName: 'Fronttalks' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_IRR',
                    message: 'Letter id is required to render preview',
                });
        });

        it('should throw error when mail template not found', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/5/render')
                .set('Cookie', 'Session_id=user-session-id')
                .expect(404)
                .expect({
                    message: 'Mail template not found',
                    id: '5',
                    internalCode: '404_ENF',
                    dbType: testDbType,
                });
        });

        it('should throw error when mailTemplateId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/inv@lid/render')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'MailTemplate ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('setEventVariablePresets', () => {
        it('should set variable presets for event ', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 5 });
            await factory.mailTemplate.create({ id: 13 });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/13/preset/5')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ variables: { eventName: 'New event name' } })
                .expect('Content-Type', /json/)
                .expect(200);

            const [actual] = await db.eventMailTemplatePreset.findAll({
                attributes: ['variables', 'mailTemplateId', 'eventId'],
                raw: true,
            });
            const expected = {
                eventId: 5,
                mailTemplateId: 13,
                variables: { eventName: 'New event name' },
            };

            assert.deepStrictEqual(actual, expected);
        });

        it('should set variable presets for event by group access ', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.eventsGroup.create({ eventId: { id: 5 }, groupId: { id: 1 } });
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: 1 });
            await factory.mailTemplate.create({ id: 13 });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/13/preset/5')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ variables: { eventName: 'New event name' } })
                .expect('Content-Type', /json/)
                .expect(200);

            const [actual] = await db.eventMailTemplatePreset.findAll({
                attributes: ['variables', 'mailTemplateId', 'eventId'],
                raw: true,
            });
            const expected = {
                eventId: 5,
                mailTemplateId: 13,
                variables: { eventName: 'New event name' },
            };

            assert.deepStrictEqual(actual, expected);
        });

        it('should throw error set variable presets for event by group no access ', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.eventsGroup.create({ eventId: { id: 5 }, groupId: { id: 1 } });
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: { id: 2 } });
            await factory.mailTemplate.create({ id: 13 });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/13/preset/5')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ variables: { eventName: 'New event name' } })
                .expect('Content-Type', /json/)
                .expect(403);
        });

        it('should update variables preset for event', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.eventMailTemplatePreset.create({
                id: 10,
                eventId: { id: 6 },
                mailTemplateId: { id: 15 },
                variables: { eventName: 'Code Fest' },
            });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/15/preset/6')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ variables: { eventName: 'New event name' } })
                .expect('Content-Type', /json/)
                .expect(200);

            const [actual] = await db.eventMailTemplatePreset.findAll({
                attributes: ['variables', 'mailTemplateId', 'eventId'],
                raw: true,
            });
            const expected = {
                eventId: 6,
                mailTemplateId: 15,
                variables: { eventName: 'New event name' },
            };

            assert.deepStrictEqual(actual, expected);
        });

        it('should throw error if eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/13/preset/inv@lid/')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if mailTemplateId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/mailTemplates/inv@lid/preset/5')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'MailTemplate ID is invalid',
                    value: 'inv@lid',
                });
        });
    });
});
