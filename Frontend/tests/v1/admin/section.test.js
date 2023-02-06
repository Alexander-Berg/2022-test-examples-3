const assert = require('assert');
const request = require('supertest');
const config = require('yandex-cfg');
const nock = require('nock');

const { nockTvmtool, nockBlackbox } = require('tests/mocks');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');

const db = require('db');
const app = require('app');

const types = config.schema.sectionTypeEnum;
const sectionData = {
    id: 5,
    title: 'Фронтэнд',
    slug: 'front',
    order: 1,
    type: types.section,
    eventId: 8,
};

describe('Section controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('Create', () => {
        it('should return created section', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 8, slug: 'codefest', title: 'codefest' });

            await request(app.listen())
                .post('/v1/admin/sections')
                .send(sectionData)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.title, sectionData.title);
                    assert.equal(body.slug, sectionData.slug);
                });

            const actual = await db.section.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].title, sectionData.title);
        });

        it('should return created section by group access', async() => {
            nockBlackbox();
            nockTvmtool();

            const group = { id: 1, slug: 'test', name: 'test' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.eventsGroup.create({
                groupId: group.id,
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            });

            await request(app.listen())
                .post('/v1/admin/sections')
                .send(sectionData)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.title, sectionData.title);
                    assert.equal(body.slug, sectionData.slug);
                });

            const actual = await db.section.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].title, sectionData.title);
        });

        it('should throw error create section by group no access', async() => {
            nockBlackbox();
            nockTvmtool();

            const group = { id: 1, slug: 'test', name: 'test' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.eventsGroup.create({
                groupId: { id: 2, slug: 'test2', name: 'test2' },
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            });

            await request(app.listen())
                .post('/v1/admin/sections')
                .send(sectionData)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403);
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.event.create({ id: 8, slug: 'codefest', title: 'codefest' });

            const data = {
                eventId: 8,
            };

            await request(app.listen())
                .post('/v1/admin/sections')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should have required property \'title\'');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox('saaaaaaaaasha');
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/sections')
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

    describe('Patch', () => {
        it('should patch a section', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = {
                ...sectionData,
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            };

            await factory.section.create(data);

            await request(app.listen())
                .patch('/v1/admin/sections')
                .send({ id: 5, order: 2, title: 'Бэкенд' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, 5);
                    assert.equal(body.title, 'Бэкенд');
                    assert.equal(body.order, 2);
                    assert.equal(body.slug, data.slug);
                });

            const actual = await db.section.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].id, data.id);
            assert.equal(actual[0].type, data.type);
        });

        it('should patch a section by group access', async() => {
            nockBlackbox();
            nockTvmtool();
            const group = { id: 1, slug: 'test', name: 'test' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.eventsGroup.create({
                groupId: group.id,
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            });

            const data = {
                ...sectionData,
                eventId: 8,
            };

            await factory.section.create(data);

            await request(app.listen())
                .patch('/v1/admin/sections')
                .send({ id: 5, order: 2, title: 'Бэкенд' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, 5);
                    assert.equal(body.title, 'Бэкенд');
                    assert.equal(body.order, 2);
                    assert.equal(body.slug, data.slug);
                });

            const actual = await db.section.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].id, data.id);
            assert.equal(actual[0].type, data.type);
        });

        it('should throw error patch section by group no access', async() => {
            nockBlackbox();
            nockTvmtool();

            const group = { id: 1, slug: 'test', name: 'test' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.eventsGroup.create({
                groupId: { id: 2, slug: 'test2', name: 'test2' },
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            });

            const data = {
                ...sectionData,
                eventId: 8,
            };

            await factory.section.create(data);

            await request(app.listen())
                .patch('/v1/admin/sections')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(403);
        });

        it('should throw error when sectionId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/sections')
                .send({ id: 'inv@lid' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Section ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = {
                ...sectionData,
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            };

            await factory.section.create(data);

            await request(app.listen())
                .patch('/v1/admin/sections')
                .send({ id: 5, order: 'not a number value' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should be integer');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox('saaaaaaaaasha');
            nockTvmtool();

            await request(app.listen())
                .patch('/v1/admin/sections')
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
        it('should destroy a section by id', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = {
                ...sectionData,
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            };

            await factory.section.create(data);

            await request(app.listen())
                .delete('/v1/admin/sections/5')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(204);
        });

        it('should destroy a section by id by group access', async() => {
            nockBlackbox();
            nockTvmtool();
            const group = { id: 1, slug: 'test', name: 'test' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.eventsGroup.create({
                groupId: group.id,
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            });

            const data = {
                ...sectionData,
                eventId: 8,
            };

            await factory.section.create(data);

            await request(app.listen())
                .delete('/v1/admin/sections/5')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(204);
        });

        it('should throw error destroy a section by id by group no access', async() => {
            nockBlackbox();
            nockTvmtool();
            const group = { id: 1, slug: 'test', name: 'test' };

            await factory.userRole.create({ role: 'groupManager', login: 'solo', eventGroupId: group });
            await factory.eventsGroup.create({
                groupId: { id: 2, slug: 'test2', name: 'test2' },
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            });

            const data = {
                ...sectionData,
                eventId: 8,
            };

            await factory.section.create(data);

            await request(app.listen())
                .delete('/v1/admin/sections/5')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(403);
        });

        it('should throw error when id is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/sections/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Section ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox('saaaaaaaaasha');
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/sections/1')
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
});
