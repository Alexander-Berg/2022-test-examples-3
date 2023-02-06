const assert = require('assert');
const request = require('supertest');
const config = require('yandex-cfg');
const _ = require('lodash');
const nock = require('nock');
const sinon = require('sinon');
const uuid = require('uuid');

const {
    nockTvmtool,
    nockBlackbox,
    nockSenderSendEmail,
    nockSpamCheckSuccess,
    nockSpamCheckFail,
    nockFormApiGetSettings,
} = require('tests/mocks');
const db = require('db');
const { DbType } = require('db/constants');
const app = require('app');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');

const mockId = '00000000-0000-0000-0000-000000000000';
const testDbType = DbType.internal;

describe('Registration controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('findOne', () => {
        it('should find registration', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = {
                id: 4,
                eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                accountId: {
                    id: 7,
                    email: 'solo@starwars-team.ru',
                    isEmailConfirmed: true,
                    firstName: 'solo',
                    lastName: 'han',
                },
                answers: {
                    email: {
                        label: 'Email',
                        value: 'art00@yandex-team.ru',
                    },
                },
                formAnswerId: 535297,
                source: 'forms',
                invitationStatus: 'not_decided',
                visitStatus: 'not_come',
                createdAt: '2018-11-15T00:00:00.000Z',
            };

            await factory.registration.create(data);

            await request(app.listen())
                .get('/v1/admin/registrations/4')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({
                    ..._.omit(data, 'answers'),
                    accountId: 7,
                    eventId: 5,
                    accountIsEmailConfirmed: true,
                    accountLogin: null,
                    accountEmail: 'solo@starwars-team.ru',
                    accountFirstName: 'solo',
                    accountLastName: 'han',
                    answerEmail: 'art00@yandex-team.ru',
                    accountJobPlace: 'Орден джедаев',
                    isEmailConfirmed: false,
                    isRegisteredBefore: false,
                });
        });

        it('should throw error when registrationId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/registrations/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Registration ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('create', () => {
        it('should create a registration and create user profile', async() => {
            /* eslint-disable max-len */

            nockBlackbox();
            nockTvmtool();
            nockFormApiGetSettings();
            nockSenderSendEmail({ to: 'saaaaaaaaasha@yandex-team.ru' });

            sinon.stub(uuid, 'v1').callsFake(() => mockId);

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 1, slug: 'fronttalks', registrationFormId: '10666' });

            await request(app.listen())
                .post('/v1/admin/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .set('x-form-id', '10666')
                .set('x-form-answer-id', '535297')
                .field('field_1', '{"question": {"label": {"ru": "Email"}, "id": 61461, "slug": "email"}, "value": "saaaaaaaaasha@yandex-team.ru"}')
                .field('field_2', '{"question": {"label": {"ru": "\\u0412\\u044b\\u043f\\u0430\\u0434\\u0430\\u044e\\u0449\\u0438\\u0439 \\u0441\\u043f\\u0438\\u0441\\u043e\\u043a"}, "id": 72343, "slug": "answerChoices72343"}, "value": "\\u041f\\u043e\\u0434\\u043a\\u043b\\u044e\\u0447\\u0443\\u0441\\u044c \\u043a \\u043e\\u043d\\u043b\\u0430\\u0439\\u043d-\\u0442\\u0440\\u0430\\u043d\\u0441\\u043b\\u044f\\u0446\\u0438\\u0438"}')
                .field('field_3', '{"question": {"label": {"ru": "\\u0418\\u043c\\u044f"}, "id": 61462, "slug": "firstName"}, "value": "Alexander"}')
                .field('field_4', '{"question": {"label": {"ru": "\\u0424\\u0430\\u043c\\u0438\\u043b\\u0438\\u044f"}, "id": 61463, "slug": "lastName"}, "value": "Ivanov"}')
                .expect(200)
                .expect({ success: true });

            const registration = await db.registration.findOne();
            const account = await db.account.findOne();
            const actual = _.omit(registration.toJSON(), ['id', 'createdAt', 'secretKey']);

            assert.strictEqual(account.email, 'saaaaaaaaasha@yandex-team.ru');
            assert.strictEqual(account.firstName, 'Alexander');
            assert.strictEqual(account.lastName, 'Ivanov');

            assert.deepStrictEqual(actual, {
                formAnswerId: 535297,
                eventId: 1,
                accountId: account.id,
                source: 'forms',
                invitationStatus: 'new',
                visitStatus: 'not_come',
                confirmationEmailCode: mockId,
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
                yandexuid: null,
                isEmailConfirmed: false,
                isRegisteredBefore: false,
                comment: null,
            });

            uuid.v1.restore();
        });

        it('should create a registration and confirm email by yandexuid', async() => {
            /* eslint-disable max-len */

            nockBlackbox();
            nockTvmtool();
            nockSenderSendEmail({ to: 'saaaaaaaaasha@yandex-team.ru' });

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 1, slug: 'fronttalks', registrationFormId: '10666' });
            await factory.account.create({ id: 1, yandexuid: '27962667', email: 'saaaaaaaaasha@yandex-team.ru' });

            await request(app.listen())
                .post('/v1/admin/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .set('x-form-id', '10666')
                .set('x-form-answer-id', '535297')
                .set('email', 'other-email@yandex.ru')
                .set('uid', '27962667')
                .field('field_1', '{"question": {"label": {"ru": "Email"}, "id": 61461, "slug": "email"}, "value": "saaaaaaaaasha@yandex-team.ru"}')
                .field('field_2', '{"question": {"label": {"ru": "\\u0412\\u044b\\u043f\\u0430\\u0434\\u0430\\u044e\\u0449\\u0438\\u0439 \\u0441\\u043f\\u0438\\u0441\\u043e\\u043a"}, "id": 72343, "slug": "answerChoices72343"}, "value": "\\u041f\\u043e\\u0434\\u043a\\u043b\\u044e\\u0447\\u0443\\u0441\\u044c \\u043a \\u043e\\u043d\\u043b\\u0430\\u0439\\u043d-\\u0442\\u0440\\u0430\\u043d\\u0441\\u043b\\u044f\\u0446\\u0438\\u0438"}')
                .field('field_3', '{"question": {"label": {"ru": "\\u0418\\u043c\\u044f"}, "id": 61462, "slug": "firstName"}, "value": "Alexander"}')
                .field('field_4', '{"question": {"label": {"ru": "\\u0424\\u0430\\u043c\\u0438\\u043b\\u0438\\u044f"}, "id": 61463, "slug": "lastName"}, "value": "Ivanov"}')
                .expect(200)
                .expect({ success: true });

            const registration = await db.registration.findOne();
            const account = await db.account.findOne();
            const actual = _.pick(registration.toJSON(), ['isEmailConfirmed', 'yandexuid', 'confirmationEmailCode']);

            assert.deepStrictEqual(actual, {
                confirmationEmailCode: null,
                yandexuid: '27962667',
                isEmailConfirmed: true,
            });

            assert.strictEqual(account.email, 'saaaaaaaaasha@yandex-team.ru');
            assert.strictEqual(account.firstName, 'Alexander');
            assert.strictEqual(account.lastName, 'Ivanov');
        });

        it('should create a registration and update user profile', async() => {
            /* eslint-disable max-len */

            nockBlackbox();
            nockTvmtool();
            nockSenderSendEmail({ to: 'saaaaaaaaasha@yandex-team.ru' });

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 1, slug: 'fronttalks', registrationFormId: '10666' });
            await factory.account.create({ email: 'saaaaaaaaasha@yandex-team.ru', firstName: 'Solo', lastName: 'Han' });

            await request(app.listen())
                .post('/v1/admin/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .set('x-form-id', '10666')
                .set('x-form-answer-id', '535297')
                .set('email', 'saaaaaaaaasha@yandex-team.ru')
                .field('field_1', '{"question": {"label": {"ru": "Email"}, "id": 61461, "slug": "email"}, "value": "saaaaaaaaasha@yandex-team.ru"}')
                .field('field_2', '{"question": {"label": {"ru": "\\u0412\\u044b\\u043f\\u0430\\u0434\\u0430\\u044e\\u0449\\u0438\\u0439 \\u0441\\u043f\\u0438\\u0441\\u043e\\u043a"}, "id": 72343, "slug": "answerChoices72343"}, "value": "\\u041f\\u043e\\u0434\\u043a\\u043b\\u044e\\u0447\\u0443\\u0441\\u044c \\u043a \\u043e\\u043d\\u043b\\u0430\\u0439\\u043d-\\u0442\\u0440\\u0430\\u043d\\u0441\\u043b\\u044f\\u0446\\u0438\\u0438"}')
                .field('field_3', '{"question": {"label": {"ru": "\\u0418\\u043c\\u044f"}, "id": 61462, "slug": "firstName"}, "value": "Alexander"}')
                .field('field_4', '{"question": {"label": {"ru": "\\u0424\\u0430\\u043c\\u0438\\u043b\\u0438\\u044f"}, "id": 61463, "slug": "lastName"}, "value": "Ivanov"}')
                .expect(200)
                .expect({ success: true });

            const registrationsCount = await db.registration.count();
            const account = await db.account.findOne();

            assert.strictEqual(account.email, 'saaaaaaaaasha@yandex-team.ru');
            assert.strictEqual(account.firstName, 'Alexander');
            assert.strictEqual(account.lastName, 'Ivanov');
            assert.strictEqual(registrationsCount, 1);
        });

        it('should throw error if form id is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderSendEmail({ to: config.sender.adminEmail });

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 1, slug: 'fronttalks', registrationFormId: '10666' });

            await request(app.listen())
                .post('/v1/admin/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .set('x-form-id', 'invalid')
                .set('x-form-answer-id', '535297')
                .field('field_1', '{"question": {"label": {"ru": "Email"}, "slug": "email"}, "value": "s@yandex.ru"}')
                .field('field_3', '{"question": {"label": {"ru": ""}, "slug": "firstName"}, "value": "Alexander"}')
                .field('field_4', '{"question": {"label": {"ru": ""}, "slug": "lastName"}, "value": "Ivanov"}')
                .expect(200)
                .expect({
                    internalCode: '400_III',
                    message: 'Form ID is invalid',
                    value: 'invalid',
                });
        });

        it('should throw error if event (which related with form) is not found', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderSendEmail({ to: config.sender.adminEmail });

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .set('x-form-id', '12345')
                .set('x-form-answer-id', '535297')
                .field('field_1', '{"question": {"label": {"ru": "Email"}, "slug": "email"}, "value": "s@yandex.ru"}')
                .field('field_3', '{"question": {"label": {"ru": ""}, "slug": "firstName"}, "value": "Alexander"}')
                .field('field_4', '{"question": {"label": {"ru": ""}, "slug": "lastName"}, "value": "Ivanov"}')
                .expect(200)
                .expect({
                    internalCode: '404_ENF',
                    message: 'Event not found',
                    scope: 'one',
                    dbType: testDbType,
                    where: { registrationFormId: 12345 },
                });
        });

        it('should throw error if account data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderSendEmail({ to: config.sender.adminEmail });

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 1, slug: 'fronttalks', registrationFormId: '10666' });

            await request(app.listen())
                .post('/v1/admin/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .set('x-form-id', '10666')
                .set('x-form-answer-id', '535297')
                .field('field_1', '{"question": {"label": {"ru": "Email"}, "slug": "email"}, "value": "invalid"}')
                .field('field_3', '{"question": {"label": {"ru": ""}, "slug": "firstName"}, "value": "Alexander"}')
                .field('field_4', '{"question": {"label": {"ru": ""}, "slug": "lastName"}, "value": "Ivanov"}')
                .expect(200)
                .expect({
                    internalCode: '400_CSF',
                    message: 'Check by schema failed',
                    errors: [{
                        data: 'invalid',
                        dataPath: '.email',
                        keyword: 'format',
                        message: 'should match format "email"',
                        params: { format: 'email' },
                        parentSchema: {
                            format: 'email',
                            maxLength: 512,
                            title: 'Email',
                            type: 'string',
                        },
                        schema: 'email',
                        schemaPath: '#/properties/email/format',
                    }],
                });
        });

        it('should throw error if user has already registered', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderSendEmail({ to: config.sender.adminEmail });

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await factory.registration.create({
                eventId: {
                    id: 5,
                    slug: 'front-talks-2018',
                    title: 'front-talks',
                    registrationFormId: '10666',
                },
                accountId: {
                    id: 1,
                    email: 'solo@starwars-team.ru',
                },
                answers: [{
                    key: 'email',
                    label: 'Email',
                    value: 'solo@yandex-team.ru',
                }],
            });

            await request(app.listen())
                .post('/v1/admin/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .set('x-form-id', '10666')
                .set('x-form-answer-id', '535297')
                .field('field_1', '{"question": {"label": {"ru": "Email"}, "slug": "email"}, "value": "solo@starwars-team.ru"}')
                .field('field_3', '{"question": {"label": {"ru": ""}, "slug": "firstName"}, "value": "Alexander"}')
                .field('field_4', '{"question": {"label": {"ru": ""}, "slug": "lastName"}, "value": "Ivanov"}')
                .expect(200)
                .expect({
                    internalCode: '409_UAR',
                    message: 'User has already registered',
                    eventId: 5,
                    accountId: 1,
                });
        });

        it('should throw error if request have spam in name', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderSendEmail({ to: config.sender.adminEmail });

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await factory.registration.create({
                eventId: {
                    id: 5,
                    slug: 'front-talks-2018',
                    title: 'front-talks',
                    registrationFormId: '10666',
                },
                accountId: {
                    id: 1,
                    email: 'solo@starwars-team.ru',
                },
                answers: [{
                    key: 'email',
                    label: 'Email',
                    value: 'solo@yandex-team.ru',
                }],
            });

            await request(app.listen())
                .post('/v1/admin/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .set('x-form-id', '10666')
                .set('x-form-answer-id', '535297')
                .field('field_1', '{"question": {"label": {"ru": "Email"}, "slug": "email"}, "value": "solo@starwars-team.ru"}')
                .field('field_3', '{"question": {"label": {"ru": ""}, "slug": "firstName"}, "value": "Alexander"}')
                .field('field_4', '{"question": {"label": {"ru": ""}, "slug": "lastName"}, "value": "Вы выиграли миллион, откройте ссылку www.prize.spam"}')
                .expect(200)
                .expect({ success: false });
        });

        it('should create a registration and set "isRegisteredBefore" to true', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderSendEmail({ to: 'saaaaaaaaasha@yandex-team.ru' });

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 1, slug: 'fronttalks', registrationFormId: '10666' });
            await factory.account.create({ email: 'saaaaaaaaasha@yandex-team.ru' });

            await request(app.listen())
                .post('/v1/admin/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .set('x-form-id', '10666')
                .set('x-form-answer-id', '535297')
                .field('field_1', '{"question": {"label": {"ru": "Email"}, "id": 61461, "slug": "email"}, "value": "saaaaaaaaasha@yandex-team.ru"}')
                .field('field_2', '{"question": {"label": {"ru": "\\u0412\\u044b\\u043f\\u0430\\u0434\\u0430\\u044e\\u0449\\u0438\\u0439 \\u0441\\u043f\\u0438\\u0441\\u043e\\u043a"}, "id": 72343, "slug": "answerChoices72343"}, "value": "\\u041f\\u043e\\u0434\\u043a\\u043b\\u044e\\u0447\\u0443\\u0441\\u044c \\u043a \\u043e\\u043d\\u043b\\u0430\\u0439\\u043d-\\u0442\\u0440\\u0430\\u043d\\u0441\\u043b\\u044f\\u0446\\u0438\\u0438"}')
                .field('field_3', '{"question": {"label": {"ru": "\\u0418\\u043c\\u044f"}, "id": 61462, "slug": "firstName"}, "value": "Alexander"}')
                .field('field_4', '{"question": {"label": {"ru": "\\u0424\\u0430\\u043c\\u0438\\u043b\\u0438\\u044f"}, "id": 61463, "slug": "lastName"}, "value": "Ivanov"}')
                .expect(200)
                .expect({ success: true });

            const registration = await db.registration.findOne();

            assert.strictEqual(registration.isRegisteredBefore, true);
        });

        it('should subscribe account to event\'s tags', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderSendEmail({ to: 'saaaaaaaaasha@yandex-team.ru' });

            sinon.stub(uuid, 'v1').callsFake(() => mockId);

            await factory.account.create({ id: 99, email: 'saaaaaaaaasha@yandex-team.ru' });
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            const event = await factory.event.create({ id: 1, slug: 'front-talks-2018', registrationFormId: '10666' });
            const tags = await factory.tag.create([
                { id: 5, slug: 'front-talks', name: 'Конференция front-talks' },
                { id: 6, slug: 'yac', name: 'Yet another conference' },
                { id: 7, slug: 'market', name: 'Еще одна конференфия Маркета' },
            ]);

            // Подписываем пользователя на тег, повторной записи появиться не должно
            await factory.subscription.create({ accountId: 99, tagId: 5 });

            await event.setTags(tags.slice(0, -1));

            await request(app.listen())
                .post('/v1/admin/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .set('x-form-id', '10666')
                .set('x-form-answer-id', '535297')
                .field('field_1', '{"question": {"label": {"ru": "Email"}, "id": 61461, "slug": "email"}, "value": "saaaaaaaaasha@yandex-team.ru"}')
                .field('field_2', '{"question": {"label": {"ru": "\\u0412\\u044b\\u043f\\u0430\\u0434\\u0430\\u044e\\u0449\\u0438\\u0439 \\u0441\\u043f\\u0438\\u0441\\u043e\\u043a"}, "id": 72343, "slug": "answerChoices72343"}, "value": "\\u041f\\u043e\\u0434\\u043a\\u043b\\u044e\\u0447\\u0443\\u0441\\u044c \\u043a \\u043e\\u043d\\u043b\\u0430\\u0439\\u043d-\\u0442\\u0440\\u0430\\u043d\\u0441\\u043b\\u044f\\u0446\\u0438\\u0438"}')
                .field('field_3', '{"question": {"label": {"ru": "\\u0418\\u043c\\u044f"}, "id": 61462, "slug": "firstName"}, "value": "Alexander"}')
                .field('field_4', '{"question": {"label": {"ru": "\\u0424\\u0430\\u043c\\u0438\\u043b\\u0438\\u044f"}, "id": 61463, "slug": "lastName"}, "value": "Ivanov"}')
                .expect(200)
                .expect({ success: true });

            const subscriptions = await db.subscription.findAll({
                raw: true,
                attributes: ['accountId', 'tagId', 'type'],
            });
            const actual = subscriptions;

            assert.strictEqual(subscriptions.length, 2);
            assert.deepStrictEqual(actual, [
                { accountId: 99, tagId: 5, type: 'news' },
                { accountId: 99, tagId: 6, type: 'news' },
            ]);

            uuid.v1.restore();
        });

        it('should not subscribe account to tags if it are not existed', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderSendEmail({ to: 'saaaaaaaaasha@yandex-team.ru' });

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 1, slug: 'front-talks-2018', registrationFormId: '10666' });
            await factory.tag.create([
                { id: 5, slug: 'front-talks', name: 'Конференция front-talks' },
                { id: 6, slug: 'yac', name: 'Yet another conference' },
                { id: 7, slug: 'market', name: 'Еще одна конференфия Маркета' },
            ]);

            await request(app.listen())
                .post('/v1/admin/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .set('x-form-id', '10666')
                .set('x-form-answer-id', '535297')
                .field('field_1', '{"question": {"label": {"ru": "Email"}, "id": 61461, "slug": "email"}, "value": "saaaaaaaaasha@yandex-team.ru"}')
                .field('field_2', '{"question": {"label": {"ru": "\\u0412\\u044b\\u043f\\u0430\\u0434\\u0430\\u044e\\u0449\\u0438\\u0439 \\u0441\\u043f\\u0438\\u0441\\u043e\\u043a"}, "id": 72343, "slug": "answerChoices72343"}, "value": "\\u041f\\u043e\\u0434\\u043a\\u043b\\u044e\\u0447\\u0443\\u0441\\u044c \\u043a \\u043e\\u043d\\u043b\\u0430\\u0439\\u043d-\\u0442\\u0440\\u0430\\u043d\\u0441\\u043b\\u044f\\u0446\\u0438\\u0438"}')
                .field('field_3', '{"question": {"label": {"ru": "\\u0418\\u043c\\u044f"}, "id": 61462, "slug": "firstName"}, "value": "Alexander"}')
                .field('field_4', '{"question": {"label": {"ru": "\\u0424\\u0430\\u043c\\u0438\\u043b\\u0438\\u044f"}, "id": 61463, "slug": "lastName"}, "value": "Ivanov"}')
                .expect(200)
                .expect({ success: true });

            const subscriptions = await db.subscription.findAll({ raw: true });

            assert.strictEqual(subscriptions.length, 0);
        });
    });

    describe('validate', () => {
        it('should return OK status if data is valid', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSpamCheckSuccess();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 1, slug: 'fronttalks', registrationFormId: '11111' });

            await request(app.listen())
                .post('/v1/admin/registrations/validate')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    questions:
                        [
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
                        ],
                    id: 11111,
                    name: 'Регистрация на testEvent',
                    slug: null,
                })
                .expect(200)
                .expect({ status: 'OK' });
        });

        // Как только появится возможность валидировать специальные вопросы
        // На стороне КФ, вернуть этот тест
        it.skip('should return ERROR status if data is invalid by schema', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/registrations/validate')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    questions:
                        [
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
                                value: 'invalid',
                                label: 'Email',
                            },
                            {
                                slug: 'about',
                                id: 118400,
                                value: 'test about',
                                label: 'О себе',
                            },
                        ],
                    id: 9944,
                    name: 'Регистрация на testEvent',
                    slug: null,
                })
                .expect(200)
                .expect({ status: 'ERROR', errors: { email: 'should match format email' } });
        });

        it('should return ERROR status if email has been registered to event', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSpamCheckSuccess();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                eventId: { id: 1, slug: 'fronttalks', registrationFormId: '11111' },
                accountId: { id: 1, email: 'solo@starwars.ru' },
            });

            await request(app.listen())
                .post('/v1/admin/registrations/validate')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    questions:
                        [
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
                                value: 'solo@starwars.ru',
                                label: 'Email',
                            },
                            {
                                slug: 'about',
                                id: 118400,
                                value: 'test about',
                                label: 'О себе',
                            },
                        ],
                    id: 11111,
                    name: 'Регистрация на testEvent',
                    slug: null,
                })
                .expect(200)
                .expect({
                    status: 'ERROR',
                    errors: { email: 'Пользователь с таким email уже был зарегистрирован на мероприятие' },
                });
        });

        it('should return ERROR status if name contains urls or just incorrect', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSpamCheckSuccess();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                eventId: { id: 1, slug: 'fronttalks', registrationFormId: '11111' },
                accountId: { id: 1, email: 'solo@starwars.ru' },
            });

            await request(app.listen())
                .post('/v1/admin/registrations/validate')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    questions:
                        [
                            {
                                slug: 'firstName',
                                id: 118397,
                                value: 'Вы выиграли миллион, откройте ссылку www.prize.spam',
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
                                value: 'solo@starwars.ru',
                                label: 'Email',
                            },
                            {
                                slug: 'about',
                                id: 118400,
                                value: 'test about',
                                label: 'О себе',
                            },
                        ],
                    id: 11111,
                    name: 'Регистрация на testEvent',
                    slug: null,
                })
                .expect(200)
                .expect({
                    status: 'ERROR',
                    errors: {
                        // eslint-disable-next-line camelcase
                        firstName: 'Имя задано неверно',
                    },
                });
        });

        it('should return ERROR status if fields contains spam', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSpamCheckFail();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                eventId: { id: 1, slug: 'fronttalks', registrationFormId: '11111' },
                accountId: { id: 1, email: 'solo@starwars.ru' },
            });

            await request(app.listen())
                .post('/v1/admin/registrations/validate')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    questions:
                        [
                            {
                                slug: 'firstName',
                                id: 118397,
                                value: 'Вы выиграли миллион, откройте ссылку www.prize.spam',
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
                                value: 'solo@starwars.ru',
                                label: 'Email',
                            },
                            {
                                slug: 'about',
                                id: 118400,
                                value: 'test about',
                                label: 'О себе',
                            },
                        ],
                    id: 11111,
                    name: 'Регистрация на testEvent',
                    slug: null,
                })
                .expect(200)
                .expect({
                    status: 'ERROR',
                    errors: {
                        // eslint-disable-next-line camelcase
                        param_name: 'Error, please try again later',
                        firstName: 'Error, please try again later',
                    },
                });
        });
    });

    describe('findLastRegistrations', () => {
        it('should find last registrations', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.account.create([
                { id: 7, email: 'solo@starwars-team.ru' },
                { id: 8, email: 'skywalker@starwars-team.ru' },
                { id: 9, email: 'yoda@starwars-team.ru' },
            ]);
            await factory.event.create([
                { id: 3, slug: 'yac-2013', title: 'yac 2013' },
                { id: 4, slug: 'front-talks-2017', title: 'front-talks 2k17' },
                { id: 5, slug: 'front-talks-2018', title: 'front-talks 2k18' },
                { id: 6, slug: 'front-talks-2019', title: 'front-talks 2k19' },
            ]);

            await factory.registration.create([
                { id: 3, accountId: 7, eventId: 3, invitationStatus: 'refuse', visitStatus: 'not_come' },
                { id: 4, accountId: 7, eventId: 4, invitationStatus: 'invite', visitStatus: 'come' },
                { id: 5, accountId: 7, eventId: 5, invitationStatus: 'invite', visitStatus: 'not_come' },
                { id: 6, accountId: 7, eventId: 6, invitationStatus: 'not_decided', visitStatus: 'not_come' },
                { id: 7, accountId: 8, eventId: 5, invitationStatus: 'refuse', visitStatus: 'not_come' },
                { id: 8, accountId: 9, eventId: 6, invitationStatus: 'not_decided', visitStatus: 'not_come' },
            ]);

            await request(app.listen())
                .post('/v1/admin/registrations/last')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    eventIds: [4, 5],
                    excludeEventId: [6],
                    registrationIds: [5, 7, 8],
                })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.length, 2);
                    assert.deepStrictEqual(body, [
                        {
                            id: 5,
                            eventsStatuses: [
                                {
                                    eventId: 4,
                                    eventTitle: 'front-talks 2k17',
                                    id: 4,
                                    invitationStatus: 'invite',
                                    visitStatus: 'come',
                                },
                                {
                                    eventId: 5,
                                    eventTitle: 'front-talks 2k18',
                                    id: 5,
                                    invitationStatus: 'invite',
                                    visitStatus: 'not_come',
                                },
                            ],
                        },
                        {
                            id: 7,
                            eventsStatuses: [{
                                eventId: 5,
                                eventTitle: 'front-talks 2k18',
                                id: 7,
                                invitationStatus: 'refuse',
                                visitStatus: 'not_come',
                            }],
                        },
                    ]);
                });
        });

        it('should throw error when registrationId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/registrations/last')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    eventIds: [4, 5],
                    excludeEventId: [6],
                    registrationIds: ['invalid', 7],
                })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Registration ID is invalid',
                    value: 'invalid',
                });
        });
    });
});
