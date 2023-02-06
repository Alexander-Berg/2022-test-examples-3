const assert = require('assert');
const config = require('yandex-cfg');
const request = require('supertest');
const moment = require('moment');
const nock = require('nock');

const app = require('app');
const db = require('db');
const { DbType } = require('db/constants');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const {
    nockTvmtool,
    nockBlackbox,
    nockSenderCreatePromoDistribution,
    nockSenderSendEmail,
} = require('tests/mocks');

const { registrationInvitationStatusEnum } = config.schema;
const testDbType = DbType.internal;
const testAnswers1 = { email: { label: 'Email', value: 'qwer100@mail.ru' } };
const testAnswers2 = { email: { label: 'Email', value: '__qwer1__@yandex-team.ru' } };

const getRegistration = ({ id, eventId = 5, invitationStatus, isEmailConfirmed = true, answers }) => {
    return {
        id,
        eventId: { id: eventId },
        accountId: { id, email: `q${id}@ya.ru` },
        invitationStatus,
        isEmailConfirmed,
        ...(answers && { answers }),
    };
};

const testRegistrations = [
    getRegistration({ id: 1, invitationStatus: registrationInvitationStatusEnum.invite, answers: testAnswers1 }),
    getRegistration({ id: 2 }),
    getRegistration({ id: 3, invitationStatus: registrationInvitationStatusEnum.invite, answers: testAnswers2 }),
    getRegistration({
        id: 4,
        invitationStatus: registrationInvitationStatusEnum.invite,
        isEmailConfirmed: false,
        answers: testAnswers1,
    }),
    getRegistration({ id: 5, invitationStatus: registrationInvitationStatusEnum.invite }),
];
const testPromoRegistrations = [
    ...testRegistrations,
    getRegistration({ id: 6, invitationStatus: registrationInvitationStatusEnum.invite, answers: testAnswers2 }),
    getRegistration({ id: 7, invitationStatus: registrationInvitationStatusEnum.invite, answers: testAnswers2 }),
    getRegistration({ id: 8, invitationStatus: registrationInvitationStatusEnum.invite, answers: testAnswers2 }),
];

const momentCreatedAt = moment();

const testDistributionData = {
    id: 1,
    eventId: { id: 42 },
    templateId: { id: 11 },
    createdAt: momentCreatedAt.toDate(),
    sendAt: null,
    status: 'new',
    variables: {},
    recipientsCount: 16,
    filters: { invitationStatus: ['invite'] },
    authorLogin: 'lyubiroman',
    dbType: DbType.internal,
};

describe('Admin distribution controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('findOne', () => {
        it('should find distribution', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.distribution.create(testDistributionData);

            await request(app.listen())
                .get('/v1/admin/distributions/1')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.deepStrictEqual(body, {
                        ...testDistributionData,
                        createdAt: momentCreatedAt.format('YYYY-MM-DDTHH:mm:ss.SSS[Z]'),
                        previousSentTemplateId: null,
                        byPreviousDistributionType: 'all',
                        eventId: 42,
                        templateId: 11,
                        sentCount: '0',
                    });
                });
        });

        it('should find distribution by group access', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.distribution.create(testDistributionData);
            await factory.eventsGroup.create({ groupId: { id: 1 }, eventId: 42 });
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: 1 });

            await request(app.listen())
                .get('/v1/admin/distributions/1')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.deepStrictEqual(body, {
                        ...testDistributionData,
                        createdAt: momentCreatedAt.format('YYYY-MM-DDTHH:mm:ss.SSS[Z]'),
                        previousSentTemplateId: null,
                        byPreviousDistributionType: 'all',
                        eventId: 42,
                        templateId: 11,
                        sentCount: '0',
                    });
                });
        });

        it('should throw error find distribution by group no  access', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.distribution.create(testDistributionData);
            await factory.eventsGroup.create({ groupId: { id: 1 }, eventId: 42 });
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: { id: 2 } });

            await request(app.listen())
                .get('/v1/admin/distributions/1')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403);
        });

        it('should throw 404 error if no distribution', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/distributions/1')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    message: 'Distribution not found',
                    internalCode: '404_ENF',
                    id: '1',
                    dbType: testDbType,
                });
        });

        it('should throw error when distribution id is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/distributions/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Distribution ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('POST /admin/distributions', () => {
        it('should create email distribution by filters', async() => {
            nockBlackbox();
            nockTvmtool();

            const filters = {
                and: [
                    { invitationStatus: [registrationInvitationStatusEnum.invite] },
                    { answerEmail: { cont: 'qwer1' } },
                ],
            };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create(testRegistrations);
            await factory.mailTemplate.create({
                id: 1,
                withStatuses: [registrationInvitationStatusEnum.invite],
            });

            const sendAt = moment()
                .utc()
                .add(5, 'hours')
                .format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    filters,
                    sendAt,
                    templateId: 1,
                    eventId: 5,
                    variables: { withQr: false },
                    byTemplate: 'all',
                })
                .expect(({ body }) => {
                    assert.strictEqual(body.recipientsCount, 2);
                    assert.strictEqual(body.sendAt, sendAt);
                    assert.strictEqual(body.status, 'new');
                    assert.strictEqual(body.byPreviousDistributionType, 'all');
                    assert.strictEqual(body.authorLogin, 'solo');
                });

            const distributions = await db.distribution.findAll();

            assert.strictEqual(distributions.length, 1);
            assert.deepStrictEqual(distributions[0].filters, filters);
            assert.strictEqual(distributions[0].variables.withQr, false);
            assert.strictEqual(distributions[0].byPreviousDistributionType, 'all');
        });

        it('should create email distribution by filters by group access', async() => {
            nockBlackbox();
            nockTvmtool();

            const filters = {
                and: [
                    { invitationStatus: [registrationInvitationStatusEnum.invite] },
                    { answerEmail: { cont: 'qwer1' } },
                ],
            };

            await factory.registration.create(testRegistrations);
            await factory.mailTemplate.create({
                id: 1,
                withStatuses: [registrationInvitationStatusEnum.invite],
            });
            await factory.eventsGroup.create({ eventId: 5, groupId: { id: 1 } });
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: 1 });

            const sendAt = moment()
                .utc()
                .add(5, 'hours')
                .format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    filters,
                    sendAt,
                    templateId: 1,
                    eventId: 5,
                    variables: { withQr: false },
                    byTemplate: 'all',
                })
                .expect(({ body }) => {
                    assert.strictEqual(body.recipientsCount, 2);
                    assert.strictEqual(body.sendAt, sendAt);
                    assert.strictEqual(body.status, 'new');
                    assert.strictEqual(body.byPreviousDistributionType, 'all');
                    assert.strictEqual(body.authorLogin, 'solo');
                });

            const distributions = await db.distribution.findAll();

            assert.strictEqual(distributions.length, 1);
            assert.deepStrictEqual(distributions[0].filters, filters);
            assert.strictEqual(distributions[0].variables.withQr, false);
            assert.strictEqual(distributions[0].byPreviousDistributionType, 'all');
        });

        it('should throw error email distribution by filters by group no access', async() => {
            nockBlackbox();
            nockTvmtool();

            const filters = {
                and: [
                    { invitationStatus: [registrationInvitationStatusEnum.invite] },
                    { answerEmail: { cont: 'art00' } },
                ],
            };

            await factory.registration.create(testRegistrations);
            await factory.mailTemplate.create({
                id: 1,
                withStatuses: [registrationInvitationStatusEnum.invite],
            });
            await factory.eventsGroup.create({ eventId: 5, groupId: { id: 1 } });
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: { id: 2 } });

            const sendAt = moment()
                .utc()
                .add(5, 'hours')
                .format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    filters,
                    sendAt,
                    templateId: 1,
                    eventId: 5,
                    variables: { withQr: false },
                    byTemplate: 'all',
                })
                .expect(403);
        });

        it('should move distribution time if sentAt starts less than 15 minutes from current time', async() => {
            nockBlackbox();
            nockTvmtool();

            const filters = {
                and: [{
                    invitationStatus: [registrationInvitationStatusEnum.invite],
                }],
            };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                id: 1,
                eventId: { id: 5 },
                invitationStatus: registrationInvitationStatusEnum.invite,
                isEmailConfirmed: true,
            });
            await factory.registration.create({ id: 2, eventId: { id: 5 } });
            await factory.mailTemplate.create({
                id: 1,
                withStatuses: [registrationInvitationStatusEnum.invite],
            });

            const currentDate = moment().add(config.minutesBeforeDistributionSend - 2, 'minutes');

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    filters,
                    sendAt: '2019-02-16T12:00:00.000Z',
                    templateId: 1,
                    eventId: 5,
                    variables: { withQr: false },
                    byTemplate: 'all',
                })
                .expect(200)
                .expect(({ body }) => {
                    assert.ok(moment(body.sendAt) > currentDate);
                });

            const [distribution] = await db.distribution.findAll();

            assert.ok(moment(distribution.sendAt) > currentDate);
        });

        it('should throw 400 error if was sent wrong statuses were found by filters and template', async() => {
            nockBlackbox();
            nockTvmtool();

            const filters = {
                and: [{
                    invitationStatus: [
                        registrationInvitationStatusEnum.invite,
                        registrationInvitationStatusEnum.refuse,
                    ],
                }],
            };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                id: 1,
                eventId: { id: 5 },
                invitationStatus: registrationInvitationStatusEnum.invite,
                isEmailConfirmed: true,
            });
            await factory.registration.create({
                id: 2,
                eventId: { id: 5 },
                isEmailConfirmed: true });
            await factory.registration.create({
                id: 3,
                eventId: { id: 5 },
                invitationStatus: registrationInvitationStatusEnum.refuse,
                isEmailConfirmed: true,
            });
            await factory.mailTemplate.create({
                id: 1,
                withStatuses: [registrationInvitationStatusEnum.invite],
            });

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    filters,
                    sendAt: '2019-02-16T12:00:00.000Z',
                    templateId: 1,
                    eventId: 5,
                    byPreviousDistributionType: 'all',
                })
                .expect(400)
                .expect({
                    internalCode: '400_WFS',
                    message: 'The template can\'t be sent with selected statuses',
                });
        });

        it('should throw 400 if wrong statuses (for template) were sent by registration ids', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({ id: 1, isEmailConfirmed: true });
            await factory.registration.create({ id: 2, isEmailConfirmed: true });
            await factory.mailTemplate.create({ id: 1, withStatuses: ['invite'] });

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    ids: [1, 2],
                    sendAt: '2019-02-16T12:00:00.000Z',
                    templateId: 1,
                    eventId: 5,
                    variables: { withQr: false },
                    byTemplate: 'all',
                })
                .expect(400)
                .expect({
                    internalCode: '400_WFS',
                    message: 'The template can\'t be sent with selected statuses',
                });
        });

        it('should create email distribution by registration ids', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({ id: 1, isEmailConfirmed: true });
            await factory.registration.create({ id: 2, isEmailConfirmed: true });
            await factory.mailTemplate.create({ id: 1, withStatuses: [] });

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    ids: [1, 2],
                    sendAt: '2019-02-16T12:00:00.000Z',
                    templateId: 1,
                    eventId: 5,
                    variables: { withQr: false },
                    byTemplate: 'all',
                })
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.status, 'new');
                    assert.equal(body.byPreviousDistributionType, 'all');
                });

            const distributions = await db.distribution.findAll();

            assert.equal(distributions.length, 1);
            assert.deepEqual(distributions[0].filters, { ids: [1, 2] });
            assert.equal(distributions[0].variables.withQr, false);
            assert.equal(distributions[0].byPreviousDistributionType, 'all');
        });

        it('should throw 400 error when templateId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 5 });

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    templateId: 'inv@lid',
                    eventId: 5,
                    variables: { withQr: false },
                })
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Mail Template ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw 400 error when ids is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 5 });

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    ids: ['inv@lid'],
                    byTemplate: 'all',
                    templateId: 1,
                    eventId: 5,
                    variables: { withQr: false },
                })
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Registration ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/distributions')
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

    describe('POST /admin/distributions/count', () => {
        it('should respond count of people who get email by distribution', async() => {
            nockBlackbox();
            nockTvmtool();

            const filters = { and: [{ invitationStatus: 'invite' }] };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({ id: 1, isEmailConfirmed: true, eventId: { id: 5 }, ...filters.and[0] });
            await factory.registration.create({ id: 2, isEmailConfirmed: true, eventId: { id: 5 }, ...filters.and[0] });
            await factory.registration.create({ id: 3, isEmailConfirmed: true, eventId: { id: 5 } });
            await factory.mailTemplate.create({ id: 1 });

            await request(app.listen())
                .post('/v1/admin/distributions/count')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ filters, templateId: 1, eventId: 5, byTemplate: 'all' })
                .expect(200)
                .expect(({ body: { count } }) => {
                    assert.strictEqual(count, 2);
                });
        });

        it('should respond count of people who has confirmed email', async() => {
            nockBlackbox();
            nockTvmtool();

            const filters = { and: [{ invitationStatus: 'invite' }] };
            const accountId = { isEmailConfirmed: true, email: '1@1.ru', id: 10 };
            const eventId = { id: 5 };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create([
                { id: 1, isEmailConfirmed: true, accountId: { id: 13, email: '3@3.ru' }, eventId, ...filters.and[0] },
                { id: 2, isEmailConfirmed: false, accountId, eventId, ...filters.and[0] },
                { id: 3, isEmailConfirmed: true, accountId: { id: 11, email: '2@2.ru' }, eventId },
            ]);
            await factory.mailTemplate.create({ id: 1 });

            await request(app.listen())
                .post('/v1/admin/distributions/count')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ filters, templateId: 1, eventId: 5, byTemplate: 'all' })
                .expect(200)
                .expect(({ body: { count } }) => {
                    assert.strictEqual(count, 1);
                });
        });
    });

    describe('POST /admin/distributions/send-test-mail', () => {
        it('should send test email distribution', async() => {
            nockBlackbox();
            nockTvmtool();
            nockSenderSendEmail({ to: 'solo@yandex-team.ru' });
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.mailTemplate.create({ id: 1 });
            await factory.eventLocation.create({
                place: ' ул. Льва Толстого, 16',
                eventId: {
                    id: 10,
                    title: 'Front Talks 2019',
                    slug: 'front-talks',
                    startDate: '2019-02-16T12:00:00.000Z',
                },
            });

            await request(app.listen())
                .post('/v1/admin/distributions/send-test-mail')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ templateId: 1, eventId: 10 })
                .expect(200)
                .expect({});
        });

        it('should throw 400 error when templateId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/distributions/send-test-mail')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ templateId: 'inv@lid', eventId: 5 })
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Mail Template ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw 400 error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/distributions/send-test-mail')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ templateId: 5, eventId: 'inv@lid' })
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw 404 error when event is not found', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.mailTemplate.create({ id: 1 });

            await request(app.listen())
                .post('/v1/admin/distributions/send-test-mail')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ templateId: 1, eventId: 10 })
                .expect(404)
                .expect({
                    internalCode: '404_ENF',
                    message: 'Event not found',
                    id: 10,
                    dbType: testDbType,
                });
        });

        it('should throw 404 error when template is not found', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/distributions/send-test-mail')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ templateId: 1, eventId: 10 })
                .expect(404)
                .expect({
                    internalCode: '404_ENF',
                    message: 'Mail template not found',
                    id: 1,
                    dbType: testDbType,
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/distributions/send-test-mail')
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

    describe('cancel', () => {
        it('should cancel an empty distribution by id', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.distribution.create({
                id: 7,
            });

            await request(app.listen())
                .post('/v1/admin/distributions/7/cancel')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(200)
                .expect({ status: config.schema.distributionStatusEnum.canceled });
        });

        it('should cancel an empty distribution by id by group access', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.eventsGroup.create({ eventId: { id: 1 }, groupId: { id: 1 } });
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: 1 });
            await factory.distribution.create({
                id: 7,
                eventId: 1,
            });

            await request(app.listen())
                .post('/v1/admin/distributions/7/cancel')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(200)
                .expect({ status: config.schema.distributionStatusEnum.canceled });
        });

        it('should throw error an empty distribution by id by group no access', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.eventsGroup.create({ eventId: { id: 1 }, groupId: { id: 1 } });
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: { id: 2 } });
            await factory.distribution.create({
                id: 7,
                eventId: 1,
            });

            await request(app.listen())
                .post('/v1/admin/distributions/7/cancel')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(403);
        });

        it('should cancel a distribution with letters by id', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.distribution.create({
                id: 7,
                status: config.schema.distributionStatusEnum.inProgress,
            });
            await factory.accountMail.create({
                distributionId: 7,
            });

            await request(app.listen())
                .post('/v1/admin/distributions/7/cancel')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(200)
                .expect({ status: config.schema.distributionStatusEnum.canceled });

            const distribution = await db.distribution.findOne();
            const accountMail = await db.accountMail.findOne();

            assert.equal(distribution.status, config.schema.distributionStatusEnum.canceled);
            assert.equal(accountMail.skipSend, true);
        });

        it('should throw error when distribution has status in_progress and letters', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.distribution.create({
                id: 7,
                status: config.schema.distributionStatusEnum.finished,
            });
            await factory.accountMail.create({
                distributionId: 7,
            });

            await request(app.listen())
                .post('/v1/admin/distributions/7/cancel')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(400)
                .expect({
                    internalCode: '400_DSR',
                    message: 'Distribution with this status can\'t be canceled',
                    status: config.schema.distributionStatusEnum.finished,
                });
        });

        it('should throw error when distributionId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/distributions/inv@lid/cancel')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Distribution ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/distributions/1/cancel')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });
    });

    describe('getMails', () => {
        it('should return account mails', async() => {
            nockBlackbox();
            nockTvmtool();

            const email = '1202@mail.ru';

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.account.create({ id: 6, email });

            const templateId = {
                id: 5,
                title: 'Статус по мероприятию {{eventName}}',
                text: 'Сорян, {{userName}}! Вы не попали в число участников на {{eventName}}',
            };
            const data = [
                {
                    id: 1,
                    sentAt: null,
                    wasSent: false,
                    title: 'Статус по мероприятию Fronttalks',
                    variables: { eventName: 'Fronttalks' },
                    accountId: { id: 7 },
                    distributionId: { id: 1, templateId },
                },
                {
                    id: 2,
                    sentAt: null,
                    wasSent: false,
                    title: 'Статус по мероприятию Fronttalks',
                    variables: { eventName: 'Fronttalks' },
                    accountId: { id: 7 },
                    distributionId: { id: 1, templateId: 5 },
                },
                {
                    id: 3,
                    sentAt: null,
                    wasSent: true,
                    title: 'Статус по мероприятию Fronttalks',
                    variables: { eventName: 'Fronttalks' },
                    accountId: { id: 6 },
                    distributionId: { id: 1, templateId: 5 },
                },
                {
                    id: 4,
                    sentAt: null,
                    wasSent: false,
                    title: 'Статус по мероприятию Fronttalks',
                    variables: { eventName: 'Fronttalks' },
                    accountId: { id: 6 },
                    distributionId: { id: 1, templateId: 5 },
                },
            ];

            await factory.accountMail.create(data);

            await request(app.listen())
                .get('/v1/admin/distributions/1/mails')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ search: '20' })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body: { rows, meta } }) => {
                    assert.strictEqual(meta.totalSize, 1);
                    assert.strictEqual(rows[0].id, 3);
                    assert.strictEqual(rows[0].email, email);
                });
        });

        it('should throw 404 error when distribution is not found', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/distributions/10/mails')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(404)
                .expect({
                    internalCode: '404_ENF',
                    message: 'Distribution not found',
                    id: '10',
                    dbType: testDbType,
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .get('/v1/admin/distributions/10/mails')
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

    describe('promo destributions', () => {
        it('should create promo distribution by filters', async() => {
            nockBlackbox();
            nockTvmtool();

            const filters = {
                and: [
                    { invitationStatus: [registrationInvitationStatusEnum.invite] },
                    { answerEmail: { cont: 'qwer1' } },
                ],
            };

            const promoFilters = { ids: [8, 7, 6, 3, 1], accountIds: [8, 7, 6, 3, 1] };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 5, title: 'Test event title' });
            await factory.registration.create(testPromoRegistrations);
            await factory.mailTemplate.create({
                id: 1,
                withStatuses: [registrationInvitationStatusEnum.invite],
                title: 'Test template title',
                text: 'Test mail text',
            });

            const sendAt = moment()
                .utc()
                .add(5, 'hours')
                .format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');

            const nockSender = nockSenderCreatePromoDistribution({ accountSlug: 'ya.events' });

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    filters,
                    sendAt,
                    templateId: 1,
                    eventId: 5,
                    variables: { withQr: false },
                    byTemplate: 'all',
                    isPromo: true,
                })
                .expect(({ body }) => {
                    assert.strictEqual(body.recipientsCount, 5);
                    assert.strictEqual(body.sendAt, sendAt);
                    assert.strictEqual(body.status, 'sending');
                    assert.strictEqual(body.byPreviousDistributionType, 'all');
                    assert.deepStrictEqual(body.filters, promoFilters);
                });

            assert.ok(nockSender.isDone());

            const distributions = await db.distribution.findAll();

            assert.strictEqual(distributions.length, 1);
            assert.deepStrictEqual(distributions[0].filters, promoFilters);
            assert.strictEqual(distributions[0].variables.withQr, false);
            assert.strictEqual(distributions[0].byPreviousDistributionType, 'all');
            assert.strictEqual(distributions[0].campaignSlug, 'promp-distrib-slug');
            assert.strictEqual(distributions[0].senderId, '1ID2IN3SENDER');
        });

        it('should not create promo distribution if recipients count is too low', async() => {
            nockBlackbox();
            nockTvmtool();

            const filters = {
                and: [
                    { invitationStatus: [registrationInvitationStatusEnum.invite] },
                    { answerEmail: { cont: 'qwer1' } },
                ],
            };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 5, title: 'Test event title' });
            await factory.registration.create(testRegistrations);
            await factory.mailTemplate.create({
                id: 1,
                withStatuses: [registrationInvitationStatusEnum.invite],
                title: 'Test template title',
                text: 'Test mail text',
            });

            const sendAt = moment()
                .utc()
                .add(5, 'hours')
                .format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    filters,
                    sendAt,
                    templateId: 1,
                    eventId: 5,
                    variables: { withQr: false },
                    byTemplate: 'all',
                    isPromo: true,
                })
                .expect(400)
                .expect({
                    internalCode: '400_DSR',
                    message: 'Promo distribution can\'t be sent',
                });
        });

        it('should not cancel promo distribution', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.distribution.create({
                id: 9,
                status: config.schema.distributionStatusEnumPromo.inProgress,
                campaignSlug: 'promp-distrib-slug',
                senderId: '1ID2IN3SENDER',
            });

            await request(app.listen())
                .post('/v1/admin/distributions/9/cancel')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(400)
                .expect({
                    internalCode: '400_DSR',
                    message: 'Promo Distribution can\'t be canceled here',
                    status: config.schema.distributionStatusEnumPromo.inProgress,
                });
        });

        it('should throw error if template has empty fields', async() => {
            nockBlackbox();
            nockTvmtool();

            const filters = {
                and: [
                    { invitationStatus: [registrationInvitationStatusEnum.invite] },
                    { answerEmail: { cont: 'qwer1' } },
                ],
            };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 5, title: 'Test event title' });
            await factory.registration.create(testPromoRegistrations);

            const sendAt = moment()
                .utc()
                .add(5, 'hours')
                .format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.mailTemplate.create({ id: 1 });

            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    filters,
                    sendAt,
                    templateId: 1,
                    eventId: 5,
                    variables: { withQr: false },
                    byTemplate: 'all',
                    isPromo: true,
                })
                .expect(500)
                .expect({
                    message: 'Email template should contain title and text',
                });
        });

        it('should respond count of people who get email by distribution, accounting promo distributions', async() => {
            nockBlackbox();
            nockTvmtool();

            const answers1 = { email: { label: 'Email', value: 'qwer1@mail.ru' } };
            // eslint-disable-next-line camelcase
            const answers2 = { email: { label: 'Email', value: '__qwer1__@mail.ru' } };
            const filtersPromo = {
                and: [
                    { invitationStatus: [registrationInvitationStatusEnum.invite] },
                ],
            };

            const promoFilters = { ids: [8, 7, 6, 3, 1], accountIds: [8, 7, 6, 3, 1] };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 5, title: 'Test event title' });

            const accounts = [{
                id: 1,
                sex: 'man',
                email: 'q1acc@yandex.ru',
                yandexuid: '1518583433759763986',
                login: 'q1',
            }, {
                id: 2,
                sex: 'man',
                email: 'q2acc@yandex.ru',
                yandexuid: '2518583433759763986',
                login: 'q2',
            }, {
                id: 3,
                sex: 'man',
                email: 'q3acc@yandex.ru',
                yandexuid: '3518583433759763986',
                login: 'q3',
            }, {
                id: 6,
                sex: 'man',
                email: 'q6acc@yandex.ru',
                yandexuid: '3518583433759763944',
                login: 'q6',
            }, {
                id: 7,
                sex: 'man',
                email: 'q7acc@yandex.ru',
                yandexuid: '3518583433759763955',
                login: 'q7',
            }, {
                id: 8,
                sex: 'man',
                email: 'q8acc@yandex.ru',
                yandexuid: '3518583433759763665',
                login: 'q8',
            },
            ];

            await factory.account.create(accounts);

            await factory.registration.create([
                getRegistration({
                    id: 1,
                    invitationStatus: registrationInvitationStatusEnum.invite,
                    answers: answers1,
                }),
                getRegistration({
                    id: 2,
                    invitationStatus: registrationInvitationStatusEnum.online,
                    answers: answers1,
                }),
                getRegistration({
                    id: 3,
                    invitationStatus: registrationInvitationStatusEnum.invite,
                    answers: answers2,
                }),
                getRegistration({
                    id: 6,
                    invitationStatus: registrationInvitationStatusEnum.invite,
                    answers: answers1,
                }),
                getRegistration({
                    id: 7,
                    invitationStatus: registrationInvitationStatusEnum.invite,
                    answers: answers2,
                }),
                getRegistration({
                    id: 8,
                    invitationStatus: registrationInvitationStatusEnum.invite,
                    answers: answers2,
                }),
            ]);

            await factory.mailTemplate.create({
                id: 1,
                withStatuses: [registrationInvitationStatusEnum.invite],
                title: 'Test template title',
                text: 'Test mail text',
            });

            const sendAt = moment()
                .utc()
                .add(5, 'hours')
                .format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');

            nockSenderCreatePromoDistribution({ accountSlug: 'ya.events' });

            // Создаем промо рассылку, чтобы затем проверить, что она учитывается при отправке другой рассылки
            await request(app.listen())
                .post('/v1/admin/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    filters: filtersPromo,
                    sendAt,
                    templateId: 1,
                    eventId: 5,
                    variables: { withQr: false },
                    byTemplate: 'all',
                    isPromo: true,
                })
                .expect(({ body }) => {
                    assert.strictEqual(body.recipientsCount, 5);
                    assert.strictEqual(body.status, 'sending');
                    assert.deepStrictEqual(body.filters, promoFilters);
                });

            await factory.mailTemplate.create({
                id: 2,
                withStatuses: [],
            });

            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/distributions/count')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    templateId: 2,
                    eventId: 5,
                    byPreviousDistributionType: 'was_sent',
                    previousSentTemplateId: 1,
                })
                .expect(200)
                .expect(({ body: { count } }) => {
                    assert.strictEqual(count, 5);
                });
        });
    });
});
