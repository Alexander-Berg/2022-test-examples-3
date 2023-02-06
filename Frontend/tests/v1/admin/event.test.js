const assert = require('assert');
const request = require('supertest');
const config = require('yandex-cfg');
const moment = require('moment');
const nock = require('nock');
const _ = require('lodash');
const uuid = require('uuid/v1');

const app = require('app');
const db = require('db');
const { DbType } = require('db/constants');
const Event = require('models/event');
const defaultSchema = require('lib/schema/registration');
const Registration = require('models/registration');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const Lpc = require('lib/turbo-lpc');
const {
    nockTvmtool,
    nockBlackbox,
    nockLpcTemplateCopy,
    nockLpcGetNodes,
    nockLpcSetPageData,
    nockLpcGetPageByPath,
    nockLpcNoPageByPath,
    nockLpcCreateFolderByPath,
    nockLpcPublishing,
    nockLpcUnpublishing,
    nockFormsApiCopyForm,
    nockFormsApiPatchForm,
    nockFormApiGetSettings,
} = require('tests/mocks');

const testDbType = DbType.internal;
const { registrationInvitationStatusEnum, registrationVisitStatusEnum } = config.schema;

describe('Admin event controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('findOne', () => {
        it('should find event', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const event = {
                id: 11,
                slug: 'devstart',
                startDate: '2018-02-16T12:00:00.000Z',
                endDate: '2018-02-17T18:00:00.000Z',
                dateIsConfirmed: false,
                redirectUrl: null,
                registrationDateIsConfirmed: false,
                registrationStartDate: '2018-02-16T04:00:00.000Z',
                registrationEndDate: '2018-02-16T04:30:00.000Z',
                registrationStatus: 'open',
                registrationRedirectUrl: 'https://somewhere.net/registerMe',
                registrationFormId: 10666,
                registrationFormIdHashed: null,
                feedbackFormId: 999,
                feedbackFormIdHashed: null,
                title: 'Starting this',
                city: 'Nsk',
                timezone: 'Asia/Novosibirsk',
                description: 'Sooo many flags',
                shortDescription: 'Such boolean',
                image: null,
                isPublished: false,
                askUserToSubscribe: false,
                areMaterialsPublished: false,
                autoControlRegistration: false,
                translationStatus: config.broadcastStatus.OFF,
                videoStatus: config.videoStatus.HIDE,
                broadcastWillBe: false,
                broadcastIsStarted: false,
                broadcastIsFinished: false,
                isVisible: true,
                isAcademy: false,
                isMigrated: false,
                isOnline: false,
                createdAt: '2018-10-26T12:00:00.000Z',
                locations: [{
                    place: 'Nsk, Nikolaeva 12',
                    city: 'Nsk',
                    timezone: 'Asia/Novosibirsk',
                    description: 'A map',
                    lat: 56.111,
                    lon: 11.566,
                    zoom: 18,
                    order: 1,
                }],
                broadcasts: [
                    {
                        title: 'Watch this',
                        iframeUrl: 'http://some.url.ru',
                        iframeWidth: 480,
                        iframeHeight: 320,
                        order: 1,
                        isVisibleForExternalUser: true,
                        createdAt: '2018-10-26T12:00:00.000Z',
                        withChat: false,
                        chatId: '',
                        chatTheme: config.lpc.chatColorTheme.LIGHT,
                    },
                    {
                        title: 'Don`t watch this',
                        iframeUrl: 'http://some.url.tr',
                        iframeWidth: 1024,
                        iframeHeight: 560,
                        order: 2,
                        isVisibleForExternalUser: true,
                        createdAt: '2018-10-26T12:00:00.000Z',
                        withChat: false,
                        chatId: '',
                        chatTheme: config.lpc.chatColorTheme.LIGHT,
                    },
                ],
                lpcPagePath: '.sandbox/events/test',
                lpcPageImage: null,
                tags: [],
                groups: [],
                badgeTemplateId: null,
                autoSendingMailTemplates: {
                    ...config.sender.mailIds[testDbType],
                    registrationCreateError: config.sender.mailIds.registrationCreateError,
                },
            };

            await db.event.create(event,
                {
                    include: [{
                        model: db.eventLocation,
                        as: 'locations',
                    }, {
                        model: db.eventBroadcast,
                        as: 'broadcasts',
                    }, {
                        model: db.tag,
                        as: 'tags',
                    }],
                });

            await request(app.listen())
                .get('/v1/admin/events/11')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    body.locations = body.locations.map(location => _.omit(location, ['id']));
                    body.broadcasts = body.broadcasts.map(broadcast => _.omit(broadcast, ['id']));

                    assert.deepStrictEqual(body, event);
                });
        });

        it('should throw error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should find event by group access', async() => {
            nockBlackbox();
            nockTvmtool();
            const group = { id: 1, slug: 'testGroup', name: 'testName' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.eventsGroup.create({ groupId: group.id, eventId: { id: 1 } });

            await request(app.listen())
                .get('/v1/admin/events/1')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.id, 1);
                    const actualGroup = body.groups.find(({ id }) => id === 1);

                    assert.deepStrictEqual(actualGroup, group);
                });
        });

        it('should throw error not found event by group access', async() => {
            nockBlackbox();
            nockTvmtool();
            const group = { id: 1, slug: 'testGroup', name: 'testName' };
            const group2 = { id: 2, slug: 'testGroup2', name: 'testName2' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.eventsGroup.create({ groupId: group2, eventId: { id: 1 } });

            await request(app.listen())
                .get('/v1/admin/events/1')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403);
        });
    });

    describe('findPage', () => {
        it('should find events with default pagination and sort', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const events = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.event.create(events);

            await request(app.listen())
                .get('/v1/admin/events')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 25);
                    assert.equal(body.meta.pageNumber, config.pagination.pageNumber);
                    assert.equal(body.meta.pageSize, config.pagination.pageSize);
                    assert.equal(body.rows.length, config.pagination.pageSize);
                    assert.equal(body.rows[0].slug, '#25');
                });
        });

        it('should find events with pagination', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const events = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.event.create(events);

            await request(app.listen())
                .get('/v1/admin/events')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 3, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 25);
                    assert.equal(body.rows.length, 3);
                    assert.equal(body.rows[0].slug, '#19');
                });
        });

        /* eslint-disable no-nested-ternary */
        it('should find events with filter', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const eventsData = _.range(1, 26)
                .map(num => ({
                    id: num,
                    slug: `#${num}`,
                    city: num === 10 ? null : 'Nsk',
                }));

            const events = await factory.event.create(eventsData);
            const tags = await factory.tag.create([
                { id: 11, name: 'Javascript', slug: 'js' },
                { id: 12, name: 'Java', slug: 'java' },
                { id: 13, name: 'C++', slug: 'cpp' },
                { id: 14, name: 'Assembler', slug: 'asm' },
            ]);

            await events[0].addTags(tags.slice(0, 2));
            await events[1].addTags(tags.slice(1, 3));
            await events[4].addTags(tags.slice(3));

            const filters = {
                or: [
                    { 'tags.id': { cont: [11, 12] } },
                    { 'tags.name': { cont: 'assem' } },
                    { city: { null: '' } },
                ],
            };

            await request(app.listen())
                .get('/v1/admin/events')
                .query({ filters: JSON.stringify(filters) })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 4);
                    assert.equal(body.rows.length, 4);
                    assert.equal(body.rows[0].slug, '#10');
                    assert.equal(body.rows[1].slug, '#5');
                    assert.equal(body.rows[2].slug, '#2');
                    assert.equal(body.rows[3].slug, '#1');
                });
        });

        it('should throw error if pageNumber is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 'inv@lid' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_PII',
                    message: 'Page number is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if sortBy is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ sortBy: 'wrongField' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_SNI',
                    message: 'Parameter sortBy is not allowed in SortableFields',
                    value: 'wrongField',
                });
        });
    });

    describe('Create', () => {
        it('should create an event', async() => {
            nockBlackbox('solo');
            nockTvmtool();

            const tags = [{
                id: 1,
                slug: 'javascript',
                name: 'JavaScript',
            }];

            const groups = [{
                id: 1,
                slug: 'javascript',
                name: 'JavaScript',
            }];

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.tag.create(tags);
            await factory.group.create(groups);

            const data = {
                id: 13,
                slug: 'devstart',
                startDate: '2018-02-16T12:00:00.000Z',
                endDate: '2018-02-17T18:00:00.000Z',
                dateIsConfirmed: false,
                registrationDateIsConfirmed: false,
                registrationStartDate: '2018-02-16T04:00:00.000Z',
                registrationEndDate: '2018-02-16T04:30:00.000Z',
                registrationStatus: 'opened',
                registrationRedirectUrl: 'https://somewhere.net/registerMe',
                registrationFormId: 10666,
                registrationFormIdHashed: null,
                feedbackFormId: 999,
                feedbackFormIdHashed: null,
                title: 'Starting this',
                city: 'Nsk',
                timezone: 'Asia/Novosibirsk',
                description: 'Sooo many flags',
                shortDescription: 'Such boolean',
                isPublished: false,
                askUserToSubscribe: false,
                areMaterialsPublished: true,
                autoControlRegistration: false,
                broadcastWillBe: false,
                broadcastIsStarted: false,
                broadcastIsFinished: false,
                isVisible: true,
                isAcademy: true,
                isMigrated: true,
                isOnline: true,
                createdAt: '2018-10-26T12:00:00.000Z',
                tags,
                groups,
                translationStatus: config.broadcastStatus.OFF,
                videoStatus: config.videoStatus.HIDE,
            };

            await request(app.listen())
                .post('/v1/admin/events')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({
                    ...data,
                    locations: [],
                    broadcasts: [],
                    redirectUrl: null,
                    image: null,
                    badgeTemplateId: null,
                    lpcPagePath: null,
                    lpcPageImage: null,
                    autoSendingMailTemplates: {
                        ...config.sender.mailIds[testDbType],
                        registrationCreateError: config.sender.mailIds.registrationCreateError,
                    },
                });

            const actual = await db.event.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].slug, data.slug);
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events')
                .send({ id: 13 })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should have required property \'slug\'');
                });
        });

        it('should throw error when event slug is banned', async() => {
            nockBlackbox();
            nockTvmtool();

            const tags = [{
                id: 1,
                slug: 'javascript',
                name: 'JavaScript',
            }];
            const data = {
                id: 13,
                slug: 'frontend',
                startDate: '2018-02-16T12:00:00.000Z',
                endDate: '2018-02-17T18:00:00.000Z',
                dateIsConfirmed: false,
                registrationDateIsConfirmed: false,
                registrationStartDate: '2018-02-16T04:00:00.000Z',
                registrationEndDate: '2018-02-16T04:30:00.000Z',
                registrationStatus: 'opened',
                registrationRedirectUrl: 'https://somewhere.net/registerMe',
                registrationFormId: 10666,
                feedbackFormId: 999,
                title: 'Starting this',
                city: 'Nsk',
                timezone: 'Asia/Novosibirsk',
                description: 'Sooo many flags',
                shortDescription: 'Such boolean',
                isPublished: false,
                askUserToSubscribe: false,
                areMaterialsPublished: true,
                autoControlRegistration: false,
                broadcastWillBe: false,
                broadcastIsStarted: false,
                broadcastIsFinished: false,
                isVisible: true,
                isAcademy: true,
                isMigrated: true,
                createdAt: '2018-10-26T12:00:00.000Z',
                tags,
            };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.tag.create(tags);

            await request(app.listen())
                .post('/v1/admin/events')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(409)
                .expect({
                    id: 13,
                    message: 'Такой слаг уже занят, выберите другой',
                    internalCode: '409_EAE',
                    slug: 'frontend',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/events')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    slug: 'wrong',
                    title: 'Totally wrong',
                    startDate: '2018-02-16',
                    endDate: '2018-02-16',
                })
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });

        it('should create an event by group access', async() => {
            nockBlackbox('solo');
            nockTvmtool();

            const tags = [{
                id: 1,
                slug: 'javascript',
                name: 'JavaScript',
            }];

            const groups = [{
                id: 1,
                slug: 'javascript',
                name: 'JavaScript',
            }];

            await factory.tag.create(tags);
            await factory.group.create(groups);
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: 1 });

            const data = {
                id: 13,
                slug: 'devstart',
                startDate: '2018-02-16T12:00:00.000Z',
                endDate: '2018-02-17T18:00:00.000Z',
                dateIsConfirmed: false,
                registrationDateIsConfirmed: false,
                registrationStartDate: '2018-02-16T04:00:00.000Z',
                registrationEndDate: '2018-02-16T04:30:00.000Z',
                registrationStatus: 'opened',
                registrationRedirectUrl: 'https://somewhere.net/registerMe',
                registrationFormId: 10666,
                registrationFormIdHashed: null,
                feedbackFormId: 999,
                feedbackFormIdHashed: null,
                title: 'Starting this',
                city: 'Nsk',
                timezone: 'Asia/Novosibirsk',
                description: 'Sooo many flags',
                shortDescription: 'Such boolean',
                isPublished: false,
                askUserToSubscribe: false,
                areMaterialsPublished: true,
                autoControlRegistration: false,
                broadcastWillBe: false,
                broadcastIsStarted: false,
                broadcastIsFinished: false,
                isVisible: true,
                isAcademy: true,
                isMigrated: true,
                isOnline: true,
                createdAt: '2018-10-26T12:00:00.000Z',
                tags,
                groups,
            };

            await request(app.listen())
                .post('/v1/admin/events')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200);

            const actual = await db.event.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].slug, data.slug);
        });

        it('should throw error create an event by no group access', async() => {
            nockBlackbox('solo');
            nockTvmtool();

            const tags = [{
                id: 1,
                slug: 'javascript',
                name: 'JavaScript',
            }];

            const groups = [{
                id: 1,
                slug: 'javascript',
                name: 'JavaScript',
            }];

            await factory.tag.create(tags);
            await factory.group.create(groups);
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: { id: 2 } });

            const data = {
                id: 13,
                slug: 'devstart',
                startDate: '2018-02-16T12:00:00.000Z',
                endDate: '2018-02-17T18:00:00.000Z',
                dateIsConfirmed: false,
                registrationDateIsConfirmed: false,
                registrationStartDate: '2018-02-16T04:00:00.000Z',
                registrationEndDate: '2018-02-16T04:30:00.000Z',
                registrationStatus: 'opened',
                registrationRedirectUrl: 'https://somewhere.net/registerMe',
                registrationFormId: 10666,
                registrationFormIdHashed: null,
                feedbackFormId: 999,
                feedbackFormIdHashed: null,
                title: 'Starting this',
                city: 'Nsk',
                timezone: 'Asia/Novosibirsk',
                description: 'Sooo many flags',
                shortDescription: 'Such boolean',
                isPublished: false,
                askUserToSubscribe: false,
                areMaterialsPublished: true,
                autoControlRegistration: false,
                broadcastWillBe: false,
                broadcastIsStarted: false,
                broadcastIsFinished: false,
                isVisible: true,
                isAcademy: true,
                isMigrated: true,
                isOnline: true,
                createdAt: '2018-10-26T12:00:00.000Z',
                tags,
                groups,
            };

            await request(app.listen())
                .post('/v1/admin/events')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403);

            const actual = await db.event.findAll();

            assert.equal(actual.length, 0);
        });
    });

    describe('delete', () => {
        it('should delete an event', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const event = {
                id: '11',
                slug: 'devstart',
                startDate: '2018-02-16',
                endDate: '2018-02-17',
                title: 'Starting this',
                locations: [{
                    place: 'Nsk, Nikolaeva 12',
                    description: 'A map',
                    lat: 56.111,
                    lon: 11.566,
                    zoom: 18,
                    order: 1,
                }],
                broadcasts: [
                    {
                        title: 'Watch this',
                        iframeUrl: 'http://some.url.ru',
                        iframeWidth: 480,
                        iframeHeight: 320,
                        streamId: 'one',
                        streamType: 'youtube',
                        order: 1,
                        withChat: false,
                        chatId: '',
                        chatTheme: config.lpc.chatColorTheme.LIGHT,
                    },
                    {
                        title: 'Don`t watch this',
                        iframeUrl: 'http://some.url.tr',
                        iframeWidth: 1024,
                        iframeHeight: 560,
                        streamId: 'two',
                        streamType: 'comdi',
                        order: 2,
                        withChat: false,
                        chatId: '',
                        chatTheme: config.lpc.chatColorTheme.LIGHT,
                    },
                ],
                tags: [],
            };

            await db.event.create(event,
                {
                    include: [{
                        model: db.eventLocation,
                        as: 'locations',
                    }, {
                        model: db.eventBroadcast,
                        as: 'broadcasts',
                    }, {
                        model: db.tag,
                        as: 'tags',
                    }],
                });

            await request(app.listen())
                .delete('/v1/admin/events/11')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204)
                .expect({});
        });

        it('should throw error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/events/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/events/11')
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
        it('should patch an event', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 13, slug: 'YaC' });

            const data = { id: 13, title: 'Yet Another Conf' };

            await request(app.listen())
                .patch('/v1/admin/events')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, 13);
                    assert.equal(body.slug, 'YaC');
                    assert.equal(body.title, 'Yet Another Conf');
                });

            const actual = await db.event.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].id, 13);
            assert.equal(actual[0].title, 'Yet Another Conf');
        });

        it('should update an event timezone with updating time of talks', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 13, slug: 'YaC', timezone: 'Asia/Novosibirsk' });

            await factory.programItem.create({
                eventId: { id: 13 },
                startDate: '2018-02-16T10:00:00.000Z',
                endDate: '2018-02-16T11:00:00.000Z',
            });

            const data = {
                id: 13,
                startDate: new Date('2018-02-17T12:00:00.000Z'),
                title: 'Yet Another Conf',
                locations: [_.omit({
                    ...factory.eventLocation.defaultData,
                    timezone: 'Asia/Yekaterinburg',
                }, ['id', 'eventId'])],
            };

            await request(app.listen())
                .patch('/v1/admin/events')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200);

            const [actual] = await db.programItem.findAll({ raw: true });

            const actualStartDate = moment(actual.startDate).utc();
            const actualEndDate = moment(actual.endDate).utc();

            assert.strictEqual(actualStartDate.format(), '2018-02-17T12:00:00Z');
            assert.strictEqual(actualEndDate.format(), '2018-02-17T13:00:00Z');
        });

        it('should throw error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/events')
                .send({ id: 'inv@lid' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 13, slug: 'YaC' });

            await request(app.listen())
                .patch('/v1/admin/events')
                .send({ id: 13, isPublished: 'not a boolean value' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should be boolean');
                });
        });

        it('should throw error when slug is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 13, slug: 'YaC' });

            await request(app.listen())
                .patch('/v1/admin/events')
                .send({ id: 13, slug: 'not valid value' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.strictEqual(body.errors[0].message, 'should match pattern "^[/a-z0-9_-]+[^/]$"');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .patch('/v1/admin/events')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    slug: 'wrong',
                    title: 'Totally wrong',
                    startDate: '2018-02-16',
                    endDate: '2018-02-16',
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

            const group = { id: 1, slug: 'testGroup', name: 'testName' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.eventsGroup.create({ groupId: group.id, eventId: { id: 1 } });

            const data = { id: 1, title: 'Yet Another Conf' };

            await request(app.listen())
                .patch('/v1/admin/events')
                .set('Cookie', ['Session_id=user-session-id'])
                .send(data)
                .expect('Content-Type', /json/)
                .expect(200);
        });

        it('should throw error user has access by group', async() => {
            nockBlackbox();
            nockTvmtool();

            const group = { id: 1, slug: 'testGroup', name: 'testName' };
            const group2 = { id: 2, slug: 'testGroup2', name: 'testName2' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.eventsGroup.create({ groupId: group2, eventId: { id: 1 } });

            const data = { id: 1, title: 'Yet Another Conf' };

            await request(app.listen())
                .patch('/v1/admin/events')
                .set('Cookie', ['Session_id=user-session-id'])
                .send(data)
                .expect('Content-Type', /json/)
                .expect(403);
        });
    });

    describe('addTags', () => {
        it('should add tags to the event', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 13, slug: 'YaC' });
            await factory.tag.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await request(app.listen())
                .post('/v1/admin/events/13/tag?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.eventTags.findAll({ order: [['tagId']] });

            assert.equal(actual.length, 2);
            assert.equal(actual[0].tagId, 11);
            assert.equal(actual[1].tagId, 12);
        });

        it('should throw error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events/inv@lid/tag')
                .send([11, 12])
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when tagId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events/13/tag?ids=11,inv@lid')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Tag ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/events/13/tag')
                .send([11])
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

    describe('removeTags', () => {
        it('should remove tags from the event', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            const event = await factory.event.create({ id: 13, slug: 'YaC' });
            const tags = await factory.tag.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await event.addTags(tags);

            await request(app.listen())
                .delete('/v1/admin/events/13/tag?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.eventTags.count();

            assert.equal(actual, 0);
        });

        it('should throw error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/events/inv@lid/tag?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when tagId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/events/13/tag?ids=11,inv@lid')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Tag ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/events/13/tag')
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

    describe('getProgram', () => {
        it('should get program for event', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const programItems = [
                {
                    startDate: new Date('2018-02-16T04:00:00.000Z'),
                    endDate: new Date('2018-02-16T11:30:00.000Z'),
                    isTalk: true,
                    eventId: '8',
                    title: 'Тест 1',
                },
                {
                    startDate: new Date('2018-02-16T11:30:00.000Z'),
                    endDate: new Date('2018-02-16T12:00:00.000Z'),
                    isTalk: true,
                    eventId: '8',
                    title: 'Тест 2',
                },
            ];

            await factory.event.create({ id: '8', slug: 'codefest', title: 'codefest' });
            await factory.programItem.create(programItems);

            await request(app.listen())
                .get('/v1/admin/events/8/program')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.length, 2);
                    assert.equal(body[0].isTalk, programItems[0].isTalk);
                    assert.equal(body[0].title, programItems[0].title);
                });
        });

        it('should throw error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/inv@lid/program')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('findHistoryPage', () => {
        it('should find event history with pagination', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    entityId: { id: 144 },
                    data: { mark: `#${num}` },
                }));

            await factory.eventHistory.create(entries);
            await request(app.listen())
                .get('/v1/admin/events/144/history')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 3, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 25);
                    assert.equal(body.rows.length, 3);
                    assert.equal(body.rows[0].data.mark, '#19');
                });
        });

        it('should find event history with default pagination', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    entityId: { id: 144 },
                    data: { mark: `#${num}` },
                }));

            await factory.eventHistory.create(entries);
            await request(app.listen())
                .get('/v1/admin/events/144/history')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 25);
                    assert.equal(body.meta.pageNumber, config.history.pagination.pageNumber);
                    assert.equal(body.meta.pageSize, config.history.pagination.pageSize);
                    assert.equal(body.rows.length, config.history.pagination.pageSize);
                    assert.equal(body.rows[0].data.mark, '#25');
                });
        });

        it('should throw error if pageNumber is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/11/history')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 'inv@lid' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_PII',
                    message: 'Page number is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/inv@lid/history')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 1, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('findRegistrationPage', () => {
        it('should find event registrations with pagination', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                    accountId: { id: num, email: `solo${num}@starwars-team.ru` },
                    answers: {
                        email: {
                            label: 'Email',
                            value: 'art00@yandex-team.ru',
                        },
                    },
                    createdAt: new Date(2018, 10, num),
                }));

            await factory.registration.create(entries);

            await request(app.listen())
                .get('/v1/admin/events/5/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 3, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 25);
                    assert.strictEqual(body.rows.length, 3);
                    assert.strictEqual(body.rows[0].id, 19);
                    assert.strictEqual(body.rows[0].answerEmail, 'art00@yandex-team.ru');
                });
        });

        it('should find event registrations with pagination with group access', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.eventsGroup.create({
                groupId: { id: 1, name: 'test', slug: 'test' },
                eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
            });

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: 1 });

            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    eventId: 5,
                    accountId: { id: num, email: `solo${num}@starwars-team.ru` },
                    answers: {
                        email: {
                            label: 'Email',
                            value: 'art00@yandex-team.ru',
                        },
                    },
                    createdAt: new Date(2018, 10, num),
                }));

            await factory.registration.create(entries);

            await request(app.listen())
                .get('/v1/admin/events/5/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 3, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 25);
                    assert.strictEqual(body.rows.length, 3);
                    assert.strictEqual(body.rows[0].id, 19);
                    assert.strictEqual(body.rows[0].answerEmail, 'art00@yandex-team.ru');
                });
        });

        it('should throw error find event registrations with pagination with group no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.eventsGroup.create({
                groupId: { id: 1, name: 'test', slug: 'test' },
                eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
            });

            await factory.userRole.create({
                role: 'groupManager',
                login: 'solo',
                eventGroupId: { id: 2, name: 'test2', slug: 'test2' },
            });

            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    eventId: 5,
                    accountId: { id: num, email: `solo${num}@starwars-team.ru` },
                    answers: {
                        email: {
                            label: 'Email',
                            value: 'art00@yandex-team.ru',
                        },
                    },
                    createdAt: new Date(2018, 10, num),
                }));

            await factory.registration.create(entries);

            await request(app.listen())
                .get('/v1/admin/events/5/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 3, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(403);
        });

        it('should find event registrations with filter and search', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const entries = _.range(1, 26)
                // eslint-disable-next-line complexity
                .map(num => ({
                    id: num,
                    eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                    accountId: {
                        id: num,
                        email: `solo${num}@starwars-team.ru`,
                        firstName: num > 10 ? 'Артём' : 'Соло',
                        jobPlace: num > 20 ? 'SW' : 'Yandex',
                    },
                    answers: {
                        email: {
                            label: 'Email',
                            value: num === 11 ? 'art00@yandex-team.ru' : 'ya@ya.ru',
                        },
                        choice_boolean_123: {
                            label: 'Да/Нет',
                            value: num === 12 ? 'Да' : 'Нет',
                        },
                        test: {
                            label: 'Test',
                            value: num === 14 ? 'aaaaa' : 'qwerty',
                        },
                    },
                }));

            await factory.registration.create(entries);

            const filters = {
                or: [
                    { accountJobPlace: 'SW' },
                    { accountEmail: { cont: 'solo13' } },
                    { answerEmail: 'art00@yandex-team.ru' },
                    { answerTest: { ncont: 'wert' } },
                    { answerChoice_boolean_123: 'да' },
                ],
            };

            // Поле search содержит букву "е", но находит "е" или "ё"
            await request(app.listen())
                .get('/v1/admin/events/5/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageSize: 20, search: 'артем', filters: JSON.stringify(filters) })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 9);
                    assert.strictEqual(body.rows.length, 9);
                    assert.strictEqual(body.rows[0].id, 25);
                    assert.strictEqual(body.rows[1].id, 24);
                    assert.strictEqual(body.rows[2].id, 23);
                    assert.strictEqual(body.rows[3].id, 22);
                    assert.strictEqual(body.rows[4].id, 21);
                    assert.strictEqual(body.rows[5].id, 14);
                    assert.strictEqual(body.rows[6].id, 13);
                    assert.strictEqual(body.rows[7].id, 12);
                    assert.strictEqual(body.rows[8].id, 11);
                });
        });

        it('should find event registrations with rus search', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                    accountId: {
                        id: num,
                        email: `test${num}@starwars-team.ru`,
                        firstName: num > 10 ? 'Соло' : 'Люк',
                    },
                    answers: {
                        email: {
                            label: 'Email',
                            value: 'art00@yandex-team.ru',
                        },
                    },
                }));

            await factory.registration.create(entries);

            await request(app.listen())
                .get('/v1/admin/events/5/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageSize: 20, search: 'соло' })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 15);
                    assert.strictEqual(body.rows.length, 15);
                    assert.strictEqual(body.rows[0].id, 25);
                    assert.strictEqual(body.rows[1].id, 24);
                    assert.strictEqual(body.rows[2].id, 23);
                    assert.strictEqual(body.rows[3].id, 22);
                    assert.strictEqual(body.rows[4].id, 21);
                });
        });

        it('should find event registrations with sorting by account', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const emails = ['luk', 'amidala', 'palpatin', 'solo'];

            const entries = _.range(1, 5)
                .map(num => ({
                    id: num,
                    eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                    accountId: {
                        id: num,
                        email: `${emails[num - 1]}@starwars-team.ru`,
                    },
                }));

            await factory.registration.create(entries);

            await request(app.listen())
                .get('/v1/admin/events/5/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageSize: 20, sortBy: 'accountEmail', sortOrder: 'DESC' })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 4);
                    assert.strictEqual(body.rows.length, 4);
                    assert.strictEqual(body.rows[0].id, 4);
                    assert.strictEqual(body.rows[1].id, 3);
                    assert.strictEqual(body.rows[2].id, 1);
                    assert.strictEqual(body.rows[3].id, 2);
                });
        });

        it('should find event registrations with sorting by answers', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const emails = ['luk', 'amidala', 'solo', 'palpatin'];

            const entries = _.range(1, 5)
                .map(num => ({
                    id: num,
                    eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                    accountId: {
                        id: num,
                        email: `solo${num}@starwars-team.ru`,
                    },
                    answers: {
                        email: {
                            label: 'Email',
                            value: `${emails[num - 1]}@starwars-team.ru`,
                        },
                    },
                }));

            await factory.registration.create(entries);

            await request(app.listen())
                .get('/v1/admin/events/5/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageSize: 20, sortBy: 'answerEmail', sortOrder: 'ASC' })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 4);
                    assert.strictEqual(body.rows.length, 4);
                    assert.strictEqual(body.rows[0].id, 2);
                    assert.strictEqual(body.rows[1].id, 1);
                    assert.strictEqual(body.rows[2].id, 4);
                    assert.strictEqual(body.rows[3].id, 3);
                });
        });

        it('should throw error if pageNumber is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/11/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 'inv@lid' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_PII',
                    message: 'Page number is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/inv@lid/registrations')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 1, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('findRegistrationSchema', () => {
        it('should return registration schema', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                id: 4,
                eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                accountId: { id: 7, email: 'solo@starwars-team.ru' },
                answers: {
                    email: {
                        label: 'Email',
                        value: 'art00@yandex-team.ru',
                    },
                },
            });

            await request(app.listen())
                .get('/v1/admin/events/5/registrations/schema')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(200)
                .expect(({ body }) => {
                    assert.deepStrictEqual(_.omit(body, 'sortableFields'), {
                        ...defaultSchema,
                        properties: {
                            ...defaultSchema.properties,
                            ...Registration.accountSchemaProps,
                            answerEmail: { type: 'string', title: 'Email' },
                        },
                    });
                });
        });

        it('should throw error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/inv@lid/registrations/schema')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('exportRegistrationList', () => {
        it('should export event registrations as csv', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                    accountId: { id: num, email: `solo${num}@starwars-team.ru` },
                    answers: [
                        {
                            key: 'email',
                            label: 'Email',
                            value: 'art00@yandex-team.ru',
                        },
                        {
                            key: 'jobPlace',
                            label: 'JobPlace',
                            value: 'Yandex',
                        },
                    ],
                    createdAt: new Date(2018, 10, num),
                }));

            await factory.registration.create(entries);
            const formattedDate = moment().format('DDMMYYYY');
            const fileName = `front-talks-${formattedDate}.csv`;

            await request(app.listen())
                .get('/v1/admin/events/5/registrations/export')
                .query({
                    format: 'csv',
                    'fields[]': ['Email', 'answerJobPlace'],
                })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', 'text/csv; charset=utf-8')
                .expect('Content-Disposition', `attachment; filename="${fileName}"`)
                .expect(200)
                .expect(({ text }) => {
                    assert.equal(text.substr(0, text.indexOf('\n')), 'Email,answerJobPlace');
                });
        });

        it('should throw error export event registrations as csv by group no  access', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.eventsGroup.create({
                groupId: { id: 1, name: 'test', slug: 'test' },
                eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
            });

            await factory.userRole.create({
                role: 'groupManager',
                login: 'solo',
                eventGroupId: { id: 2, name: 'test2', slug: 'test2' },
            });

            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    eventId: 5,
                    accountId: { id: num, email: `solo${num}@starwars-team.ru` },
                    answers: [
                        {
                            key: 'email',
                            label: 'Email',
                            value: 'art00@yandex-team.ru',
                        },
                        {
                            key: 'jobPlace',
                            label: 'JobPlace',
                            value: 'Yandex',
                        },
                    ],
                    createdAt: new Date(2018, 10, num),
                }));

            await factory.registration.create(entries);

            await request(app.listen())
                .get('/v1/admin/events/5/registrations/export')
                .query({
                    format: 'csv',
                    'fields[]': ['Email', 'answerJobPlace'],
                })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(403);
        });

        it('should export event registrations as xlsx', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    eventId: { id: 5, slug: 'front-talks-2018', title: 'front talks 2018' },
                    accountId: { id: num, email: `solo${num}@starwars-team.ru` },
                    answers: [
                        {
                            key: 'email',
                            label: 'Email',
                            value: 'art00@yandex-team.ru',
                        },
                        {
                            key: 'jobPlace',
                            label: 'JobPlace',
                            value: 'Yandex',
                        },
                    ],
                    createdAt: new Date(2018, 10, num),
                }));

            await factory.registration.create(entries);
            const formattedDate = moment().format('DDMMYYYY');
            const fileName = `front-talks-2018-${formattedDate}.xlsx`;

            await request(app.listen())
                .get('/v1/admin/events/5/registrations/export')
                .query({
                    format: 'xlsx',
                    'fields[]': ['accountEmail', 'answerEmail', 'answerJobPlace'],
                })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
                .expect('Content-Disposition', `attachment; filename="${fileName}"`)
                .expect(200)
                .expect(({ buffered }) => {
                    assert.equal(buffered, true);
                });
        });

        it('should throw error if eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/inv@lid/registrations/export')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if format is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/4/registrations/export?format=inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_VNA',
                    message: 'Value is not allowed',
                    value: 'inv@lid',
                    expected: ['csv', 'xlsx'],
                });
        });

        it('should throw error if fields is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/4/registrations/export?format=csv&fields=inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_MFN',
                    message: 'Must be an array of field names',
                });
        });
    });

    describe('importRegistrationList', () => {
        it('should import event registrations from csv', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await factory.registration.create({
                eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                accountId: { id: 1, email: 'solo25@starwars-team.ru', yandexuid: '12345678' },
            });

            await db.account.create({ email: 'solo24@starwars-team.ru', yandexuid: '12345678' });

            await request(app.listen())
                .post('/v1/admin/events/5/registrations/import')
                .set('Cookie', ['Session_id=user-session-id'])
                .attach('file', 'tests/data/registrations.csv')
                .expect({});

            const events = await db.event.findAll();
            const accounts = await db.account.findAll();
            const registrations = await db.registration.findAll();
            const accountJobPlaces = _.range(0, 25).map(num => accounts[num].dataValues.jobPlace);
            const accountEmails = _.range(0, 25).map(num => accounts[num].dataValues.email);
            const accountYUids = _.range(0, 25)
                .map(num => accounts[num].dataValues.yandexuid)
                .filter(Boolean);
            const accountIds = _.range(0, 25).map(num => accounts[num].dataValues.id);
            const regAccountIds = _.range(0, 25).map(
                num => registrations[num].dataValues.accountId,
            );

            assert.equal(events.length, 1);
            assert.equal(accounts.length, 25);
            assert.equal(registrations.length, 25);
            assert.equal(accountJobPlaces.filter(jobPlace => jobPlace === 'Yandex').length, 23);
            assert.equal(accountYUids.length, 2);
            assert(
                ['solo25@starwars-team.ru', 'solo24@starwars-team.ru']
                    .every(id => accountEmails.includes(id)),
                'default emails should be includes in accountEmails',
            );
            assert(
                accountIds.every(id => regAccountIds.includes(id)),
                'all accountIds should be includes in regAccountIds',
            );
        });

        it('should import event registrations from xlsx', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await db.account.create({ email: 'solo25@starwars-team.ru' });
            await db.account.create({ email: 'solo24@starwars-team.ru' });

            await factory.event.create({ id: 5, slug: 'front-talks-2018', title: 'front-talks' });

            await request(app.listen())
                .post('/v1/admin/events/5/registrations/import')
                .set('Cookie', ['Session_id=user-session-id'])
                .attach('file', 'tests/data/registrations.xlsx')
                .expect(204);

            const events = await db.event.findAll();
            const accounts = await db.account.findAll();
            const registrations = await db.registration.findAll();

            assert.equal(events.length, 1);
            assert.equal(accounts.length, 25);
            assert.equal(registrations.length, 25);
        });

        it('should throw error if eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events/inv@lid/registrations/import')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if file not attached', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events/5/registrations/import')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_NAF',
                    message: 'Need to attach imported file',
                });
        });
    });

    describe('action', () => {
        it('should change invitationStatus to few registrations', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                id: 4,
                invitationStatus: 'refuse',
            });
            await factory.registration.create({
                id: 5,
                invitationStatus: 'refuse',
            });

            await request(app.listen())
                .post('/v1/admin/events/5/registrations/action/online')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ ids: [4, 5] })
                .expect(204)
                .expect({});

            const registrations = await db.registration.findAll();

            assert.equal(registrations.length, 2);
            assert.equal(registrations[0].dataValues.invitationStatus, 'online');
            assert.equal(registrations[1].dataValues.invitationStatus, 'online');
        });

        it('should change visitStatus to few registrations', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                id: 4,
                visitStatus: 'come',
            });
            await factory.registration.create({
                id: 5,
                visitStatus: 'come',
            });

            await request(app.listen())
                .post('/v1/admin/events/5/registrations/action/not_come')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ ids: [4, 5] })
                .expect(204)
                .expect({});

            const registrations = await db.registration.findAll();

            assert.equal(registrations.length, 2);
            assert.equal(registrations[0].dataValues.visitStatus, 'not_come');
            assert.equal(registrations[1].dataValues.visitStatus, 'not_come');
        });

        it('should change visitStatus to all registrations', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                id: 4,
                visitStatus: 'not_come',
            });
            await factory.registration.create({
                id: 5,
                visitStatus: 'not_come',
            });
            await factory.registration.create({
                id: 6,
                visitStatus: 'not_come',
            });

            await request(app.listen())
                .post('/v1/admin/events/5/registrations/action/come')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204)
                .expect({});

            const registrations = await db.registration.findAll();

            assert.equal(registrations.length, 3);
            assert.equal(registrations[0].dataValues.visitStatus, 'come');
            assert.equal(registrations[1].dataValues.visitStatus, 'come');
            assert.equal(registrations[2].dataValues.visitStatus, 'come');
        });

        it('should change visitStatus to filtered and searched registrations', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                id: 4,
                invitationStatus: 'refuse',
                visitStatus: 'not_come',
                accountId: { id: 4, email: 'darth_vader@starwars-team.ru' },
            });
            await factory.registration.create({
                id: 5,
                invitationStatus: 'not_decided',
                visitStatus: 'not_come',
            });
            await factory.registration.create({
                id: 6,
                invitationStatus: 'refuse',
                visitStatus: 'not_come',
            });
            await factory.registration.create({
                id: 7,
                invitationStatus: 'not_decided',
                visitStatus: 'not_come',
            });

            const filters = { and: [{ invitationStatus: 'refuse' }] };

            await request(app.listen())
                .post('/v1/admin/events/5/registrations/action/come')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ filters, search: 'darth_vader' })
                .expect(204)
                .expect({});

            const registrations = await db.registration.findAll({ order: [['id', 'ASC']] });

            assert.equal(registrations.length, 4);
            assert.equal(registrations[0].dataValues.visitStatus, 'come');
            assert.equal(registrations[0].dataValues.id, 4);
            assert.equal(registrations[1].dataValues.visitStatus, 'not_come');
            assert.equal(registrations[1].dataValues.id, 5);
            assert.equal(registrations[2].dataValues.visitStatus, 'not_come');
            assert.equal(registrations[2].dataValues.id, 6);
            assert.equal(registrations[3].dataValues.visitStatus, 'not_come');
            assert.equal(registrations[3].dataValues.id, 7);
        });

        it('should throw error when actionName is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events/5/registrations/action/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_VNA',
                    message: 'Value is not allowed',
                    value: 'inv@lid',
                    expected: config.registrationActions,
                });
        });

        it('should throw error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events/undefined/registrations/action/remove')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ ids: [4, 5] })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'undefined',
                });
        });

        it('should throw error when ids list is invalid array', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events/5/registrations/action/invite')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ ids: [4, 'invalid'] })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    message: 'Registration id is invalid',
                    internalCode: '400_III',
                    value: 'invalid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/events/5/registrations/action/invite')
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

    describe('removeAction', () => {
        it('should remove few registrations', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                id: 4,
            });
            await factory.registration.create({
                id: 5,
            });

            await request(app.listen())
                .post('/v1/admin/events/5/registrations/action/remove')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ ids: [4, 5] })
                .expect(204)
                .expect({});

            const registrationsCount = await db.registration.count();

            assert.strictEqual(registrationsCount, 0);
        });

        it('should throw error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events/undefined/registrations/action/remove')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ ids: [4, 5] })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'undefined',
                });
        });
    });

    describe('registrationsCount', () => {
        it('should find count participants by invitationStatus', async() => {
            nockBlackbox();
            nockTvmtool();

            const registrations = [
                {
                    invitationStatus: registrationInvitationStatusEnum.online,
                    visitStatus: registrationVisitStatusEnum.come,
                },
                {
                    invitationStatus: registrationInvitationStatusEnum.refuse,
                    visitStatus: registrationVisitStatusEnum.notCome,
                },
                {
                    invitationStatus: registrationInvitationStatusEnum.online,
                    visitStatus: registrationVisitStatusEnum.come,
                },
                {
                    invitationStatus: registrationInvitationStatusEnum.notDecided,
                    visitStatus: registrationVisitStatusEnum.come,
                },
                {
                    invitationStatus: registrationInvitationStatusEnum.notDecided,
                    visitStatus: registrationVisitStatusEnum.come,
                },
                {
                    invitationStatus: registrationInvitationStatusEnum.online,
                    visitStatus: registrationVisitStatusEnum.notCome,
                },
            ];

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create(registrations);

            await request(app.listen())
                .get('/v1/admin/events/5/registrations/count')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(200)
                .expect({
                    all: 6,
                    [registrationInvitationStatusEnum.refuse]: 1,
                    [registrationInvitationStatusEnum.notDecided]: 2,
                    [registrationInvitationStatusEnum.online]: 3,
                    [registrationVisitStatusEnum.come]: 4,
                    [registrationVisitStatusEnum.notCome]: 2,
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .get('/v1/admin/events/5/registrations/count')
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

    describe('setBadgeTemplate', () => {
        it('should set badge template to event', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.event.create({
                id: 11,
                slug: 'event',
                badgeTemplateId: { id: 1, slug: 'tpl1' },
            });

            await factory.badgeTemplate.create({
                id: 2,
                slug: 'tpl2',
            });

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/events/11/badgeTemplate')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ slug: 'tpl2' })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => assert.equal(body.id, 2));

            const actual = await db.event.findAll({ order: [['id', 'ASC']] });

            assert.strictEqual(actual.length, 1);
            assert.equal(actual[0].id, 11);
            assert.equal(actual[0].badgeTemplateId, 2);
        });

        it('should throw error if event doesn\'t exist', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.badgeTemplate.create({
                id: 1,
                slug: 'tpl',
            });

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/events/11/badgeTemplate')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ slug: 'tpl' })
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    message: 'Event not found',
                    internalCode: '404_ENF',
                    id: '11',
                });
        });

        it('should throw error if badge template doesn\'t exist', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.event.create({
                id: 11,
                slug: 'event',
            });

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/events/11/badgeTemplate')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ slug: 'tpl' })
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    message: 'Badge template not found',
                    internalCode: '404_ENF',
                    where: {
                        slug: 'tpl',
                    },
                    dbType: testDbType,
                });
        });

        it('should throw error if eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.event.create({
                id: 11,
                slug: 'event',
            });

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/events/inv@lid/badgeTemplate')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ slug: 'tpl' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    message: 'Event ID is invalid',
                    internalCode: '400_III',
                    value: 'inv@lid',
                });
        });

        it('should throw error if slug is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            const slug = _.range(1, 111)
                .map(() => 'i')
                .join('');

            await factory.event.create({
                id: 11,
                slug: 'event',
            });

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/events/11/badgeTemplate')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ slug })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    message: 'Event slug max length > 100',
                    internalCode: '400_ESL',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .patch('/v1/admin/events/11/badgeTemplate')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ slug: 'tpl' })
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });
    });

    describe('getBadgeTemplate', () => {
        it('should get badge template by event', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.event.create({
                id: 11,
                slug: 'event',
                badgeTemplateId: { id: 1, slug: 'tpl' },
            });

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/11/badgeTemplate')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, 1);
                    assert.equal(body.slug, 'tpl');
                });
        });

        it('should throw error if event and badge template don\'t exist ', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/11/badgeTemplate')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    message: 'Event not found',
                    internalCode: '404_ENF',
                    id: '11',
                    scope: 'byIdWithBadgeTemplate',
                    dbType: testDbType,
                });
        });

        it('should throw error if badge template is not found', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.event.create({
                id: 11,
                slug: 'event',
            });

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/11/badgeTemplate')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(404)
                .expect({
                    eventId: '11',
                    internalCode: '404_ENF',
                    message: 'Badge template not found',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .get('/v1/admin/events/11/badgeTemplate')
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

    describe('badge', () => {
        const secretKey = uuid();

        it('should find badge information', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                id: 4,
                eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                accountId: {
                    id: 7,
                    email: 'solo@starwars-team.ru',
                    isEmailConfirmed: true,
                    firstName: 'solo',
                    lastName: 'han',
                },
                source: 'forms',
                invitationStatus: 'not_decided',
                visitStatus: 'not_come',
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
                createdAt: '2018-11-15T00:00:00.000Z',
                secretKey,
            });

            await request(app.listen())
                .get('/v1/admin/events/5/registrations/4/badge')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ secretKey })
                .expect(200)
                .expect({
                    event: {
                        id: 5,
                        slug: 'front-talks-2018',
                        title: 'front-talks',
                    },
                    registration: {
                        id: 4,
                        accountIsEmailConfirmed: true,
                        accountLogin: null,
                        accountEmail: 'solo@starwars-team.ru',
                        accountFirstName: 'solo',
                        accountLastName: 'han',
                        source: 'forms',
                        invitationStatus: 'not_decided',
                        visitStatus: 'come', // Статус заменяется на "Пришел"
                        answerEmail: 'saaaaaaaaasha@yandex-team.ru',
                        answerAnswerChoices72343: 'Подключусь к онлайн-трансляции',
                        answerFirstName: 'Alexander',
                        answerLastName: 'Ivanov',
                        createdAt: '2018-11-15T00:00:00.000Z',
                        accountId: 7,
                        eventId: 5,
                        formAnswerId: 535297,
                    },
                });
        });

        it('should throw 404 error when secret key don\'t match registration\'s one', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({ id: 4 });

            await request(app.listen())
                .get('/v1/admin/events/5/registrations/4/badge')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ secretKey })
                .expect(404)
                .expect({
                    registrationId: '4',
                    eventId: '5',
                    internalCode: '404_ENF',
                    message: 'Registration not found',
                    scope: 'badge',
                    secretKey,
                    dbType: testDbType,
                });
        });

        it('should throw 400 error when secret key has wrong format', async() => {
            nockBlackbox();
            nockTvmtool();

            const wrongSecretKey = 'an-invalid-secret';

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/5/registrations/4/badge')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ secretKey: wrongSecretKey })
                .expect(400)
                .expect({
                    internalCode: '400_UIF',
                    message: 'secretKey has invalid uuid format',
                    value: wrongSecretKey,
                });
        });
    });

    describe('findDistributions', () => {
        it('should find all distributions by event', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    eventId: num > 10 ? { id: 5 } : { id: 6, slug: 'test' },
                }));

            await factory.distribution.create(entries);

            await request(app.listen())
                .get('/v1/admin/events/5/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 3, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 15);
                    assert.strictEqual(body.rows.length, 3);
                    assert.strictEqual(body.rows[0].id, 19);
                    assert.strictEqual(body.rows[1].id, 18);
                    assert.strictEqual(body.rows[2].id, 17);
                });
        });

        it('should find distributions with count of sent emails', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await factory.distribution.create([
                { id: 1, eventId: { id: 5 } },
                { id: 2, eventId: { id: 5 } },
            ]);

            await factory.accountMail.create([
                { id: 1, distributionId: 1, wasSent: true },
                { id: 2, distributionId: 1, wasSent: true },
                { id: 3, distributionId: 1, wasSent: false },
                { id: 4, distributionId: 2, wasSent: false },
            ]);

            await request(app.listen())
                .get('/v1/admin/events/5/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 2);

                    assert.strictEqual(body.rows[0].sentCount, 0);
                    assert.strictEqual(body.rows[1].sentCount, 2);
                });
        });

        it('should throw error if eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/inv@lid/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 1, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if pageNumber is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/5/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 'inv@lid' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_PII',
                    message: 'Page number is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if event not found', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/5/distributions')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 3, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    message: 'Related entity (from table event) not found',
                    internalCode: '404_ENF',
                    id: '5',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .get('/v1/admin/events/5/distributions')
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

    describe('findSections', () => {
        it('should find all sections by event', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    eventId: num > 10 ? { id: 5 } : { id: 6, slug: 'test' },
                    order: num,
                }));

            await factory.section.create(entries);

            await request(app.listen())
                .get('/v1/admin/events/5/sections')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.length, 15);
                    assert.strictEqual(body[0].id, 11);
                    assert.strictEqual(body[1].id, 12);
                    assert.strictEqual(body[2].id, 13);
                });
        });

        it('should throw error if eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/inv@lid/sections')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if event not found', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/5/sections')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    message: 'Related entity (from table event) not found',
                    internalCode: '404_ENF',
                    id: '5',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .get('/v1/admin/events/5/sections')
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

    describe('suggest', () => {
        it('should response events contains text', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await factory.event.create([
                { id: 1, slug: 'a1', title: 'Феронт толкс 2018' },
                { id: 2, slug: 'a2', title: 'Еще одна конференция' },
                { id: 3, slug: 'a3', title: 'Жаба скрипт митап в Новосибирске' },
            ]);

            await request(app.listen())
                .get('/v1/admin/events/suggest')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ text: 'Фе' })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.length, 2);
                    assert.deepStrictEqual(body, [
                        { id: 1, slug: 'a1', title: 'Феронт толкс 2018' },
                        { id: 2, slug: 'a2', title: 'Еще одна конференция' },
                    ]);
                });
        });

        it('should response events contains text by limit', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await factory.event.create(_
                .range(15)
                .map(i => ({ id: i + 1, slug: `slug${i}`, title: `Фе ${i}` })),
            );

            await request(app.listen())
                .get('/v1/admin/events/suggest')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ text: 'Фе', limit: 5 })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.length, 5);
                });
        });

        it('should return 400 when limit is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/events/suggest')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ text: 'Фе', limit: 'invalid' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    message: 'Limit is invalid',
                    value: 'invalid',
                    internalCode: '400_PII',
                });
        });
    });

    describe('publish', () => {
        it('should update event isPublished to true', async() => {
            const eventId = 1;
            const lpcPagePath = 'test-path';

            nockBlackbox();
            nockTvmtool();
            nockLpcGetPageByPath(lpcPagePath);
            nockLpcPublishing(lpcPagePath);

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const tags = await factory.tag.create([{ id: 1 }]);
            const event = await factory.event.create({
                id: eventId,
                slug: 'a1',
                title: 'Феронт толкс 2018',
                isPublished: false,
                lpcPagePath,
            });

            await event.addTags(tags);

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/publish`)
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: true })
                .expect(204);

            const createdEvent = await Event.findOne({ id: eventId, dbType: testDbType });

            assert.strictEqual(createdEvent.isPublished, true);
        });

        it('should update event isPublished to true if group access', async() => {
            const eventId = 1;
            const lpcPagePath = 'test-path';

            nockBlackbox();
            nockTvmtool();
            nockLpcGetPageByPath(lpcPagePath);
            nockLpcPublishing(lpcPagePath);

            const groups = await factory.group.create([{ id: 1 }]);

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: 1 });

            const tags = await factory.tag.create([{ id: 1 }]);
            const event = await factory.event.create({
                id: eventId,
                slug: 'a1',
                title: 'Феронт толкс 2018',
                isPublished: false,
                lpcPagePath,
            });

            await event.addTags(tags);
            await event.addGroups(groups);

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/publish`)
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: true })
                .expect(204);

            const createdEvent = await Event.findOne({ id: eventId, dbType: testDbType });

            assert.strictEqual(createdEvent.isPublished, true);
        });

        it('should throw error if not group access', async() => {
            const eventId = 1;
            const lpcPagePath = 'test-path';

            nockBlackbox();
            nockTvmtool();
            nockLpcGetPageByPath(lpcPagePath);
            nockLpcPublishing(lpcPagePath);

            const tags = await factory.tag.create([{ id: 1 }]);
            const event = await factory.event.create({
                id: eventId,
                slug: 'a1',
                title: 'Феронт толкс 2018',
                isPublished: false,
                lpcPagePath,
            });

            await factory.eventsGroup.create({ eventId, groupId: { id: 1 } });

            const group = { id: 3, slug: 'testGroup', name: 'testName' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });

            await event.addTags(tags);

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/publish`)
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: true })
                .expect(403);
        });

        it('should not publish event if it doesn\'t have tags', async() => {
            const eventId = 1;
            const lpcPagePath = 'test-path';

            nockBlackbox();
            nockTvmtool();
            nockLpcGetPageByPath(lpcPagePath);
            nockLpcPublishing(lpcPagePath);

            await factory.event.create({
                id: eventId,
                slug: 'a1',
                title: 'Феронт толкс 2018',
                isPublished: false,
                lpcPagePath,
            });
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/publish`)
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: true })
                .expect(400)
                .expect({
                    message: 'Can\'t be published without tags',
                    internalCode: '400_NTG',
                });
        });

        it('should publish event if event has redirect url', async() => {
            const eventId1 = 1;

            nockBlackbox();
            nockTvmtool();

            const tags = await factory.tag.create([{ id: 1 }]);
            const event = await factory.event.create({
                id: eventId1,
                slug: 'a1',
                title: 'Феронт толкс 2018',
                isPublished: false,
                redirectUrl: 'https://ya.ru',
                lpcPagePath: undefined,
            });

            await event.addTags(tags);
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId1}/publish`)
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: true })
                .expect(204);

            const actualEvent = await Event.findOne({ id: eventId1, dbType: testDbType });

            assert.strictEqual(actualEvent.isPublished, true);
        });

        it('should not update event isPublished to true if event doesn\'t have LPC page path', async() => {
            const eventId1 = 1;

            nockBlackbox();
            nockTvmtool();

            const tags = await factory.tag.create([{ id: 1 }]);
            const event = await factory.event.create({
                id: eventId1,
                slug: 'a1',
                title: 'Феронт толкс 2018',
                isPublished: false,
                lpcPagePath: undefined,
            });

            await event.addTags(tags);
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId1}/publish`)
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: true })
                .expect(400)
                .expect({
                    internalCode: '400_NLP',
                    message: 'Can\'t be published without LPC page',
                });

            const event1 = await Event.findOne({ id: eventId1, dbType: testDbType });

            assert.strictEqual(event1.isPublished, false);
        });

        it('should not update event isPublished to true if event doesn\'t have LPC page', async() => {
            const eventId2 = 2;
            const lpcPagePath = 'test-path';

            nockBlackbox();
            nockTvmtool();
            nockLpcNoPageByPath(lpcPagePath);

            const tags = await factory.tag.create([{ id: 1 }]);
            const event = await factory.event.create({
                id: eventId2,
                slug: 'a2',
                title: 'Феронт толкс 2018',
                isPublished: false,
                lpcPagePath,
            });

            await event.addTags(tags);
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId2}/publish`)
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: true })
                .expect(400)
                .expect({
                    internalCode: '400_NLP',
                    message: 'Can\'t be published without LPC page',
                });

            const event2 = await Event.findOne({ id: eventId2, dbType: testDbType });

            assert.strictEqual(event2.isPublished, false);
        });

        it('should update isPublished to true and publish LPC page', async() => {
            const eventId = 1;
            const lpcPagePath = 'test-path';

            nockBlackbox();
            nockTvmtool();
            nockLpcGetPageByPath(lpcPagePath);
            nockLpcPublishing(lpcPagePath);

            const tags = await factory.tag.create([{ id: 1 }]);
            const event = await factory.event.create({
                id: eventId,
                slug: 'a1',
                title: 'Феронт толкс 2018',
                isPublished: false,
                lpcPagePath,
            });

            await event.addTags(tags);
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/publish`)
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: true })
                .expect(204);

            const createdEvent = await Event.findOne({ id: eventId, dbType: testDbType });

            assert.strictEqual(createdEvent.isPublished, true);
        });

        it('should update isPublished to true even if LPC page is published', async() => {
            const eventId = 1;
            const lpcPagePath = 'test-path';

            nockBlackbox();
            nockTvmtool();
            nockLpcGetPageByPath(lpcPagePath);
            nockLpcPublishing(lpcPagePath, true);

            const tags = await factory.tag.create([{ id: 1 }]);
            const event = await factory.event.create({
                id: eventId,
                slug: 'a1',
                title: 'Феронт толкс 2018',
                isPublished: false,
                lpcPagePath,
            });

            await event.addTags(tags);
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/publish`)
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: true })
                .expect(204);

            const createdEvent = await Event.findOne({ id: eventId, dbType: testDbType });

            assert.strictEqual(createdEvent.isPublished, true);
        });

        it('should update isPublished to false and unpublish LPC page', async() => {
            const eventId = 1;
            const lpcPagePath = 'test-path';

            nockBlackbox();
            nockTvmtool();
            nockLpcUnpublishing(lpcPagePath);

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({
                id: eventId,
                slug: 'a1',
                title: 'Феронт толкс 2018',
                isPublished: true,
                lpcPagePath,
            });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/publish`)
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: false })
                .expect(204);

            const event = await Event.findOne({ id: eventId, dbType: testDbType });

            assert.strictEqual(event.isPublished, false);
        });

        it('should throw 404 error if event doesn\'t exist', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events/1/publish')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: true })
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    message: 'Event not found',
                    internalCode: '404_ENF',
                    id: '1',
                    scope: 'lpc',
                    dbType: testDbType,
                });
        });

        it('should return 400 when isPublished is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 1, slug: 'a1', title: 'Феронт толкс 2018' });

            await request(app.listen())
                .post('/v1/admin/events/1/publish')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({ isPublished: 'invalid' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    message: 'isPublished is invalid',
                    internalCode: '400_PII',
                });
        });
    });

    describe('createLpcPage', () => {
        it('should create lpc page and set the id to event', async() => {
            const eventId = 1;
            const eventSlug = 'codefest';
            const eventsFolder = config.lpc.folder[testDbType];
            const lpcFolderPath = Lpc.getFolderPathByEventSlug(eventSlug, testDbType);
            const lpcPagePath = `${lpcFolderPath}/index`;

            nockBlackbox();
            nockTvmtool();
            nockLpcTemplateCopy(lpcPagePath, testDbType);
            nockLpcGetNodes(eventsFolder, true);
            nockLpcGetNodes(lpcFolderPath);
            nockLpcGetPageByPath(lpcPagePath);
            nockLpcGetPageByPath(config.lpc.turboTemplates[testDbType]);
            nockLpcCreateFolderByPath(lpcFolderPath);
            nockLpcSetPageData(lpcPagePath);

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await createEventWithProgram(eventSlug);
            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/lpc/page/create`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.lpcPagePath, lpcPagePath);
                });

            const [actual] = await db.event.findAll();

            assert.strictEqual(actual.lpcPagePath, lpcPagePath);
        });

        it('should throw error create lpc  page and set the id to event by group access', async() => {
            const eventId = 1;

            nockBlackbox();
            nockTvmtool();

            await factory.eventsGroup.create({ groupId: { id: 2 }, eventId: { id: eventId } });
            await factory.userRole.create({
                role: 'groupManager',
                login: 'solo',
                eventGroupId: { id: 1, name: 'test', slug: 'test' },
            });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/lpc/page/create`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403);
        });

        it('should throw 404 error if event doesn\'t exist', async() => {
            const eventId = 12345;

            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/lpc/page/create`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    message: 'Event not found',
                    internalCode: '404_ENF',
                    id: eventId.toString(),
                    scope: 'lpc',
                    dbType: testDbType,
                });
        });
    });

    describe('updateLpcPage', () => {
        it('should update lpc page', async() => {
            const eventId = 1;
            const eventSlug = 'codefest';
            const lpcFolderPath = Lpc.getFolderPathByEventSlug(eventSlug, testDbType);
            const lpcPagePath = `${lpcFolderPath}/index`;

            nockBlackbox();
            nockTvmtool();
            nockLpcTemplateCopy(lpcPagePath, testDbType);
            nockLpcGetPageByPath(lpcPagePath);
            nockLpcGetPageByPath(config.lpc.turboTemplates[testDbType]);
            nockLpcSetPageData(lpcPagePath);

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await createEventWithProgram(eventSlug);
            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/lpc/page/update`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);
        });

        it('should throw error update lpc page by group access', async() => {
            const eventId = 1;

            nockBlackbox();
            nockTvmtool();

            await factory.eventsGroup.create({ groupId: { id: 2 }, eventId: { id: eventId } });
            await factory.userRole.create({
                role: 'groupManager',
                login: 'solo',
                eventGroupId: { id: 1, name: 'test', slug: 'test' },
            });
            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/lpc/page/update`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(403);
        });

        it('should throw 404 error if event doesn\'t exist', async() => {
            const eventId = 12345;

            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/lpc/page/update`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    message: 'Event not found',
                    internalCode: '404_ENF',
                    id: eventId.toString(),
                    scope: 'lpc',
                    dbType: testDbType,
                });
        });
    });

    describe('createForm', () => {
        it('should create feedback form and set the id to event', async() => {
            const eventId = 1;
            const baseFormId = config.forms.feedbackFormId;
            const feedbackFormId = baseFormId + 1;

            nockBlackbox();
            nockTvmtool();
            const nockInstance = nockFormsApiCopyForm(baseFormId);
            const nockPatchForm = nockFormsApiPatchForm(baseFormId + 1);
            const nockSettingsInstance = nockFormApiGetSettings(baseFormId + 1);

            await factory.event.create({ id: eventId, slug: 'cf2k19', title: 'codefest' });
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/form/feedback`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.formId, feedbackFormId);
                });

            const [actual] = await db.event.findAll();

            assert.strictEqual(actual.feedbackFormId, feedbackFormId);
            assert.ok(nockInstance.isDone());
            assert.ok(nockPatchForm.isDone());
            assert.ok(nockSettingsInstance.isDone());
        });

        it('should throw error creat form by group access', async() => {
            const eventId = 1;

            nockBlackbox();
            nockTvmtool();

            await factory.eventsGroup.create({
                groupId: { id: 2, name: 'test', slug: 'test' },
                eventId: { id: eventId, slug: 'cf2k19', title: 'codefest' },
            });
            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: { id: 1 } });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/form/feedback`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403);
        });

        it('should create registration form and set the id to event', async() => {
            const eventId = 1;
            const baseFormId = config.forms.registrationFormId;
            const registrationFormId = baseFormId + 1;

            nockBlackbox();
            nockTvmtool();
            const nockInstance = nockFormsApiCopyForm(baseFormId);
            const nockPatchForm = nockFormsApiPatchForm(baseFormId + 1);
            const nockSettingsInstance = nockFormApiGetSettings(baseFormId + 1);

            await factory.event.create({ id: eventId, slug: 'cf2k19', title: 'codefest' });
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/form/registration`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.formId, registrationFormId);
                });

            const [actual] = await db.event.findAll();

            assert.strictEqual(actual.registrationFormId, registrationFormId);
            assert.ok(nockInstance.isDone());
            assert.ok(nockPatchForm.isDone());
            assert.ok(nockSettingsInstance.isDone());
        });

        it('should throw 400, if form type is not expected', async() => {
            const eventId = '12345';

            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/form/not-valid-form-type`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    expected: ['registration', 'feedback'],
                    internalCode: '400_VNA',
                    message: 'Value is not allowed',
                    value: 'not-valid-form-type',
                });
        });

        it('should throw 404 error if event doesn\'t exist', async() => {
            const eventId = '12345';

            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post(`/v1/admin/events/${eventId}/form/feedback`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    message: 'Event not found',
                    internalCode: '404_ENF',
                    id: eventId,
                    scope: 'shortById',
                    dbType: testDbType,
                });
        });
    });

    describe('addGroups', () => {
        it('should add groups to the event', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 13, slug: 'YaC' });
            await factory.group.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await request(app.listen())
                .post('/v1/admin/events/13/group?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.eventsGroup.findAll({ order: [['groupId']] });

            assert.equal(actual.length, 2);
            assert.equal(actual[0].groupId, 11);
            assert.equal(actual[1].groupId, 12);
        });

        it('should throw error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events/inv@lid/group')
                .send([11, 12])
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when groupId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/events/13/group?ids=11,inv@lid')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Group ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/events/13/group')
                .send([11])
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });

        it('should user has no access group access', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.event.create({ id: 13, slug: 'YaC' });
            await factory.group.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: 11 });
            await factory.eventsGroup.create({ groupId: 11, eventId: 13 });

            await request(app.listen())
                .post('/v1/admin/events/13/group?ids=12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.eventsGroup.findAll({ order: [['groupId']] });

            assert.equal(actual.length, 2);
            assert.equal(actual[0].groupId, 11);
            assert.equal(actual[1].groupId, 12);
        });
    });

    describe('removeGroups', () => {
        it('should remove groups from the event', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            const event = await factory.event.create({ id: 13, slug: 'YaC' });
            const groups = await factory.group.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
                { id: 13, name: 'Soundscript2', slug: 'soundscript2' },
            ]);

            await event.addGroups(groups);

            await request(app.listen())
                .delete('/v1/admin/events/13/group?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.eventsGroup.count();

            assert.equal(actual, 1);
        });

        it('should throw error when eventId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/events/inv@lid/group?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when groupId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/events/13/group?ids=11,inv@lid')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Group ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/events/13/group')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });

        it('should remove groups from the event by group access', async() => {
            nockBlackbox();
            nockTvmtool();

            const event = await factory.event.create({ id: 13, slug: 'YaC' });
            const groups = await factory.group.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await event.addGroups(groups);

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: 11 });

            await request(app.listen())
                .delete('/v1/admin/events/13/group?ids=11')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.eventsGroup.count();

            assert.equal(actual, 1);
        });

        it('should throw error remove groups from the event by group access', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.event.create({ id: 13, slug: 'YaC' });
            await factory.group.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: { id: 13 } });
            await factory.eventsGroup.create({ groupId: 11, eventId: 13 });

            await request(app.listen())
                .delete('/v1/admin/events/13/group?ids=11')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(403);
        });
    });
});

async function createEventWithProgram(slug = 'codefest') {
    const speaker = {
        id: 1,
        avatar: 'tet-avatar',
        firstName: 'Энакин',
        lastName: 'Скайуокер',
        middleName: 'Иванович',
        about: 'Я человек, и моё имя — Энакин!',
        jobPlace: 'Орден джедаев',
        jobPosition: 'Джуниор-падаван',
    };
    const section = {
        id: 1,
        eventId: 1,
        title: 'Фронтэнд',
        order: 1,
        slug: 'front',
        isPublished: true,
        type: config.schema.sectionTypeEnum.section,
    };
    const programItem = {
        id: 1,
        eventId: 1,
        sectionId: 1,
        startDate: '2018-02-16T04:00:00.000Z',
        endDate: '2018-02-16T11:30:00.000Z',
        isTalk: true,
        title: 'Эксперимент как инструмент для принятия решений',
        description: 'Виктор расскажет о подходе, который помогает определять.',
        shortDescription: 'Виктор расскажет.',
        isPublished: true,
    };
    const tag = {
        id: 1,
        slug: 'javascript',
        name: 'JavaScript',
    };
    const video = {
        id: 1,
        programItemId: 1,
        source: 'youtube',
        iframeUrl: 'https://www.youtube.com/embed/zB4I68XVPzQ',
        videoUrl: 'https://youtu.be/zB4I68XVPzQ',
        videoId: 'zB4I68XVPzQ',
        title: 'Star Wars: The Last Jedi Official Teaser',
        duration: 92,
        definition: 'hd',
        thumbnail: 'https://i.ytimg.com/vi/zB4I68XVPzQ/hqdefault.jpg',
        thumbnailHeight: 360,
        thumbnailWidth: 480,
    };
    const presentation = {
        id: 1,
        programItemId: 1,
        downloadUrl: 'https://yadi.sk/i/AEZ9I74I3UmjcW',
    };
    const lpcPagePath = `${Lpc.getFolderPathByEventSlug(slug, testDbType)}/index`;

    await factory.event.create({ id: 1, slug, title: 'codefest', lpcPagePath });
    await factory.section.create(section);
    await factory.programItem.create(programItem);
    await factory.speaker.create(speaker);
    await factory.tag.create(tag);
    await factory.video.create(video);
    await factory.presentation.create(presentation);
    await db.programItemTags.create({ tagId: 1, programItemId: 1 });
    await db.programItemSpeakers.create({ speakerId: 1, programItemId: 1 });

    return {
        ..._.omit(section, ['eventId', 'isPublished', 'type']),
        programItems: [{
            ..._.omit(programItem, ['sectionId', 'eventId', 'isPublished']),
            speakers: [speaker],
            tags: [tag],
            presentations: [_.omit(presentation, ['programItemId'])],
            videos: [_.omit(video, ['programItemId'])],
        }],
    };
}
