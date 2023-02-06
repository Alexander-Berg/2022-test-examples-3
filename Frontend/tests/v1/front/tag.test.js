const assert = require('assert');
const request = require('supertest');

const { DbType } = require('db/constants');
const app = require('app');
const cleanDb = require('tests/db/clean');
const factory = require('tests/db/factory');

describe('Tag controller', () => {
    beforeEach(cleanDb);

    describe('findBySlug', () => {
        it('should return tag by slug with events', async() => {
            const events = await factory.event.create([
                { id: 1, slug: 'codefest', title: 'codefest', isPublished: true },
                { id: 2, slug: 'highload', title: 'highload', isPublished: true },
                { id: 3, slug: 'fronttalks', title: 'fronttalks', isPublished: false },
            ]);
            const tag = await factory.tag.create({
                id: 7,
                slug: 'php',
                name: 'php',
                createdAt: '2018-10-26T13:00:00.000Z',
                isPublished: true,
            });

            await tag.addEvents(events);

            await request(app.listen())
                .get('/v1/front/tags/php')
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.id, 7);
                    assert.strictEqual(body.finishedEvents.length, 2);
                });
        });

        it('should throw error when tagId is invalid', async() => {
            await request(app.listen())
                .get('/v1/front/tags/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Tag slug is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when tag is not found', async() => {
            await request(app.listen())
                .get('/v1/front/tags/php')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    internalCode: '404_ENF',
                    message: 'Tag not found',
                    scope: 'frontOne',
                    slug: 'php',
                    dbType: DbType.internal,
                });
        });
    });
});
