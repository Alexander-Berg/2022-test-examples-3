const assert = require('assert');
const request = require('supertest');

const app = require('app');
const schema = require('lib/schema');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const { nockBlackbox, nockTvmtool } = require('tests/mocks');
const Tag = require('models/tag');

describe('Index routes', () => {
    beforeEach(cleanDb);

    describe('GET /admin/schemas/:type', () => {
        it('should return schema by type', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            const type = 'tag';

            await request(app.listen())
                .get(`/v1/admin/schemas/${type}`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.deepEqual(body, { ...schema[type], sortableFields: Tag.sortableFields });
                });
        });

        it('should throw error if schema is not found', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            const type = 'invalid';

            await request(app.listen())
                .get(`/v1/admin/schemas/${type}`)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    internalCode: '404_SNF',
                    message: 'Schema invalid is not found',
                });
        });
    });

    describe('GET /admin/userInfo', () => {
        it('should return current user info with roles', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });
            await factory.userRole.create({ login: 'yoda', role: 'groupManager', eventGroupId: { id: 1 } });

            await request(app.listen())
                .get('/v1/admin/userInfo')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.deepEqual(body, {
                        login: 'yoda',
                        roles: [{
                            eventGroupId: null,
                            role: 'admin',
                        },
                        {
                            eventGroupId: 1,
                            role: 'groupManager',
                        }],
                        uid: 5,
                    });
                });
        });
    });
});
