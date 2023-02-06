const assert = require('assert');
const request = require('supertest');
const nock = require('nock');
const moment = require('moment');
const _ = require('lodash');

const { nockTvmtool, nockBlackbox } = require('tests/mocks');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const { DbType } = require('db/constants');
const db = require('db');
const app = require('app');

describe('ProgramItem controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('findOne', () => {
        it('should return a program item', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            const data = {
                id: 11,
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-16T11:30:00.000Z',
                isTalk: true,
                eventId: 8,
                title: 'Эксперимент как инструмент для принятия решений',
                description: 'Виктор расскажет о подходе, который помогает определять.',
                sectionId: null,
                presentations: [],
                videos: [],
                createdAt: '2018-02-16T11:30:00.000Z',
            };

            await factory.event.create({ id: 8, slug: 'test', title: 'test' });
            await factory.programItem.create(data);

            await request(app.listen())
                .get('/v1/admin/programItems/11')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({ ...data, tags: [], speakers: [], dbType: DbType.internal });
        });

        it('should throw error when programItemId is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .get('/v1/admin/programItems/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'ProgramItem ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when program item is not found', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .get('/v1/admin/programItems/11')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    internalCode: '404_ENF',
                    message: 'Program item not found',
                    id: '11',
                    dbType: DbType.internal,
                });
        });
    });

    describe('Create', () => {
        it('should return created program item', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            const speakers = [{
                id: 1,
                avatar: null,
                email: 'darkside@yandex.ru',
                phone: '999',
                city: 'Татуин',
                socialAccount: null,
                firstName: 'Энакин',
                lastName: 'Скайуокер',
                middleName: 'Иванович',
                about: 'Я человек, и моё имя — Энакин!',
                jobPlace: 'Орден джедаев',
                jobPosition: 'Джуниор-падаван',
                createdAt: moment().toDate(),
            }];

            await factory.userRole.create({ role: 'admin', login: 'yoda' });
            await factory.event.create({ id: 8, slug: 'codefest', title: 'codefest' });
            await factory.speaker.create(speakers);

            const data = {
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-16T04:30:00.000Z',
                isTalk: true,
                eventId: 8,
                speakers,
            };

            await request(app.listen())
                .post('/v1/admin/programItems')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.title, data.title);
                    assert.strictEqual(body.startDate, data.startDate);
                    assert.deepStrictEqual(body.speakers[0], _.pick(speakers[0], ['id', 'firstName', 'lastName']));
                });

            const actual = await db.programItem.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].title, data.title);
        });

        it('should return created program item by group access', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            const speakers = [{
                id: 1,
                avatar: null,
                email: 'darkside@yandex.ru',
                phone: '999',
                city: 'Татуин',
                socialAccount: null,
                firstName: 'Энакин',
                lastName: 'Скайуокер',
                middleName: 'Иванович',
                about: 'Я человек, и моё имя — Энакин!',
                jobPlace: 'Орден джедаев',
                jobPosition: 'Джуниор-падаван',
                createdAt: moment().toDate(),
            }];

            await factory.eventsGroup.create({
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
                groupId: { id: 1, slug: 'test', name: 'test' },
            });
            await factory.userRole.create({ role: 'groupManager', login: 'yoda', eventGroupId: 1 });
            await factory.speaker.create(speakers);

            const data = {
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-16T04:30:00.000Z',
                isTalk: true,
                eventId: 8,
                speakers,
            };

            await request(app.listen())
                .post('/v1/admin/programItems')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.title, data.title);
                    assert.strictEqual(body.startDate, data.startDate);
                    assert.deepStrictEqual(body.speakers[0], _.pick(speakers[0], ['id', 'firstName', 'lastName']));
                });

            const actual = await db.programItem.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].title, data.title);
        });

        it('should throw error created program item by group no access', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            const speakers = [{
                id: 1,
                avatar: null,
                email: 'darkside@yandex.ru',
                phone: '999',
                city: 'Татуин',
                socialAccount: null,
                firstName: 'Энакин',
                lastName: 'Скайуокер',
                middleName: 'Иванович',
                about: 'Я человек, и моё имя — Энакин!',
                jobPlace: 'Орден джедаев',
                jobPosition: 'Джуниор-падаван',
                createdAt: moment().toDate(),
            }];

            await factory.userRole.create({
                role: 'groupManager',
                login: 'yoda',
                eventGroupId: { id: 2, slug: 'test2', name: 'test2' },
            });
            await factory.eventsGroup.create({
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
                groupId: { id: 1, slug: 'test', name: 'test' },
            });
            await factory.speaker.create(speakers);

            const data = {
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-16T04:30:00.000Z',
                isTalk: true,
                eventId: 8,
                speakers,
            };

            await request(app.listen())
                .post('/v1/admin/programItems')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403);
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });
            await factory.event.create({ id: 8, slug: 'codefest', title: 'codefest' });

            const data = {
                id: 5,
                startDate: 'invalid',
                endDate: 'invalid',
                eventId: 8,
            };

            await request(app.listen())
                .post('/v1/admin/programItems')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should match format "date-time"');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox('saaaaaaaaasha');
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/programItems')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({})
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'saaaaaaaaasha',
                });
        });
    });

    describe('destroy', () => {
        it('should destroy a program item by id', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            const data = {
                id: 7,
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-16T04:30:00.000Z',
                isTalk: true,
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            };

            await factory.programItem.create(data);

            await request(app.listen())
                .delete('/v1/admin/programItems/7')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(204);
        });

        it('should destroy a program item by id by group access', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({
                role: 'groupManager',
                login: 'yoda',
                eventGroupId: { id: 1, slug: 'test', name: 'test' },
            });

            await factory.eventsGroup.create({ groupId: 1, eventId: { id: 8, slug: 'codefest', title: 'codefest' } });

            const data = {
                id: 7,
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-16T04:30:00.000Z',
                isTalk: true,
                eventId: 8,
            };

            await factory.programItem.create(data);

            await request(app.listen())
                .delete('/v1/admin/programItems/7')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(204);
        });

        it('should throw error a program item by id by group no access', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({
                role: 'groupManager',
                login: 'yoda',
                eventGroupId: { id: 1, slug: 'test', name: 'test' },
            });

            await factory.eventsGroup.create({
                groupId: { id: 2, slug: 'test2', name: 'test2' },
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            });

            const data = {
                id: 7,
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-16T04:30:00.000Z',
                isTalk: true,
                eventId: 8,
            };

            await factory.programItem.create(data);

            await request(app.listen())
                .delete('/v1/admin/programItems/7')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(403);
        });

        it('should throw error when programItemId is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .delete('/v1/admin/programItems/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'ProgramItem ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox('saaaaaaaaasha');
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/programItems/1')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'saaaaaaaaasha',
                });
        });
    });

    describe('Patch', () => {
        it('should patch a program item', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            const data = {
                id: 7,
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-16T04:30:00.000Z',
                isTalk: true,
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            };

            await factory.programItem.create(data);

            await request(app.listen())
                .patch('/v1/admin/programItems')
                .send({ id: 7, title: 'Бэкенд' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, 7);
                    assert.equal(body.title, 'Бэкенд');
                    assert.equal(body.isTalk, data.isTalk);
                    assert.equal(body.eventId, 8);
                });

            const actual = await db.programItem.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].id, data.id);
            assert.equal(actual[0].isTalk, data.isTalk);
        });

        it('should patch a program item by groups access', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({
                role: 'groupManager',
                login: 'yoda',
                eventGroupId: { id: 1, slug: 'test', name: 'test' },
            });

            await factory.eventsGroup.create({
                groupId: 1,
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            });

            const data = {
                id: 7,
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-16T04:30:00.000Z',
                isTalk: true,
                eventId: 8,
            };

            await factory.programItem.create(data);

            await request(app.listen())
                .patch('/v1/admin/programItems')
                .send({ id: 7, title: 'Бэкенд' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, 7);
                    assert.equal(body.title, 'Бэкенд');
                    assert.equal(body.isTalk, data.isTalk);
                    assert.equal(body.eventId, 8);
                });

            const actual = await db.programItem.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].id, data.id);
            assert.equal(actual[0].isTalk, data.isTalk);
        });

        it('should throw error a program item by group no access', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({
                role: 'groupManager',
                login: 'yoda',
                eventGroupId: { id: 1, slug: 'test', name: 'test' },
            });

            await factory.eventsGroup.create({
                groupId: { id: 2, slug: 'test2', name: 'test2' },
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            });

            const data = {
                id: 7,
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-16T04:30:00.000Z',
                isTalk: true,
                eventId: 8,
            };

            await factory.programItem.create(data);

            await request(app.listen())
                .patch('/v1/admin/programItems')
                .send({ id: 7, title: 'Бэкенд' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403);
        });

        it('should throw error when programItemId is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .patch('/v1/admin/programItems')
                .send({ id: 'inv@lid' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'ProgramItem ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            const data = {
                id: 7,
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-16T04:30:00.000Z',
                isTalk: true,
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            };

            await factory.programItem.create(data);

            await request(app.listen())
                .patch('/v1/admin/programItems')
                .send({ id: 7, isTalk: 'not a boolean value' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should be boolean');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox('saaaaaaaaasha');
            nockTvmtool();

            await request(app.listen())
                .patch('/v1/admin/programItems')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({})
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'saaaaaaaaasha',
                });
        });
    });

    describe('addTags', () => {
        it('should add tags to the program item', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });
            await factory.programItem.create({ id: 13 });
            await factory.tag.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await request(app.listen())
                .post('/v1/admin/programItems/13/tag?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.programItemTags.findAll({ order: [['tagId']] });

            assert.equal(actual.length, 2);
            assert.equal(actual[0].tagId, 11);
            assert.equal(actual[1].tagId, 12);
        });

        it('should add tags to the program item by group', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'groupManager', login: 'yoda', eventGroupId: { id: 1 } });
            await factory.eventsGroup.create({ groupId: 1, eventId: { id: 1 } });
            await factory.programItem.create({ id: 13, eventId: 1 });
            await factory.tag.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await request(app.listen())
                .post('/v1/admin/programItems/13/tag?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.programItemTags.findAll({ order: [['tagId']] });

            assert.equal(actual.length, 2);
            assert.equal(actual[0].tagId, 11);
            assert.equal(actual[1].tagId, 12);
        });

        it('should throw error add tags to the program item by group', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'groupManager', login: 'yoda', eventGroupId: { id: 1 } });
            await factory.eventsGroup.create({ groupId: { id: 2, name: 'test2', slug: 'test2' }, eventId: { id: 1 } });
            await factory.programItem.create({ id: 13, eventId: 1 });
            await factory.tag.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await request(app.listen())
                .post('/v1/admin/programItems/13/tag?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(403);
        });

        it('should throw error when programItemId is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .post('/v1/admin/programItems/inv@lid/tag')
                .send([11, 12])
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'ProgramItem ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when tagId is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .post('/v1/admin/programItems/13/tag?ids=11,inv@lid')
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
            nockBlackbox('anyok');
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/programItems/13/tag')
                .send([11])
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'anyok',
                });
        });
    });

    describe('removeTags', () => {
        it('should remove tags from the program item', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });
            const programItem = await factory.programItem.create({ id: 13 });
            const tags = await factory.tag.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await programItem.addTags(tags);

            await request(app.listen())
                .delete('/v1/admin/programItems/13/tag?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.programItemTags.count();

            assert.equal(actual, 0);
        });

        it('should remove tags from the program item by group', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'groupManager', login: 'yoda', eventGroupId: { id: 1 } });
            await factory.eventsGroup.create({ groupId: 1, eventId: { id: 1 } });

            const programItem = await factory.programItem.create({ id: 13, eventId: 1 });
            const tags = await factory.tag.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await programItem.addTags(tags);

            await request(app.listen())
                .delete('/v1/admin/programItems/13/tag?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.programItemTags.count();

            assert.equal(actual, 0);
        });

        it('should throw error remove tags from the program item by group no access', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'groupManager', login: 'yoda', eventGroupId: { id: 1 } });
            await factory.eventsGroup.create({ groupId: { id: 2, slug: 'test2', name: 'test2' }, eventId: { id: 1 } });

            const programItem = await factory.programItem.create({ id: 13, eventId: 1 });
            const tags = await factory.tag.create([
                { id: 11, name: 'Actionscript', slug: 'actionscript' },
                { id: 12, name: 'Soundscript', slug: 'soundscript' },
            ]);

            await programItem.addTags(tags);

            await request(app.listen())
                .delete('/v1/admin/programItems/13/tag?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(403);
        });

        it('should throw error when programItemId is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .delete('/v1/admin/programItems/inv@lid/tag')
                .send([11, 12])
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'ProgramItem ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when tagId is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .delete('/v1/admin/programItems/13/tag?ids=11,inv@lid')
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
            nockBlackbox('anyok');
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/programItems/13/tag')
                .send([11])
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'anyok',
                });
        });
    });

    describe('addSpeakers', () => {
        it('should add speakers to the program item', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });
            await factory.programItem.create({ id: 13 });
            await factory.speaker.create([
                { id: 11, firstName: 'Ваня', email: 'q1@ya.ru' },
                { id: 12, firstName: 'Петя', email: 'q2@ya.ru' },
            ]);

            await request(app.listen())
                .post('/v1/admin/programItems/13/speaker?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.programItemSpeakers.findAll({ order: [['speakerId']] });

            assert.equal(actual.length, 2);
            assert.equal(actual[0].speakerId, 11);
            assert.equal(actual[1].speakerId, 12);
        });

        it('should throw error when programItemId is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .post('/v1/admin/programItems/inv@lid/speaker')
                .send([11, 12])
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'ProgramItem ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when speakerId is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .post('/v1/admin/programItems/13/speaker?ids=11,inv@lid')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Speaker ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox('anyok');
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/programItems/13/speaker')
                .send([11])
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'anyok',
                });
        });
    });

    describe('removeSpeakers', () => {
        it('should remove speakers from the program item', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });
            const programItem = await factory.programItem.create({ id: 13 });
            const speakers = await factory.speaker.create([
                { id: 11, firstName: 'Вова', email: 'q1@ya,ru' },
                { id: 12, firstName: 'Петя', email: 'q2@ya,ru' },
            ]);

            await programItem.addSpeakers(speakers);

            await request(app.listen())
                .delete('/v1/admin/programItems/13/speaker?ids=11,12')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.programItemSpeakers.count();

            assert.equal(actual, 0);
        });

        it('should throw error when programItemId is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .delete('/v1/admin/programItems/inv@lid/speaker')
                .send([11, 12])
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'ProgramItem ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when tagId is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'yoda' });

            await request(app.listen())
                .delete('/v1/admin/programItems/13/speaker?ids=11,inv@lid')
                .send()
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Speaker ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox('anyok');
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/programItems/13/speaker')
                .send([11])
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'anyok',
                });
        });
    });
});
