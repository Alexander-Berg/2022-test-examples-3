const assert = require('assert');
const request = require('supertest');

const app = require('app');
const cleanDb = require('tests/db/clean');
const factory = require('tests/db/factory');

describe('Homepage controller', () => {
    beforeEach(cleanDb);

    describe('getHomepage', () => {
        it('should return homepage', async() => {
            const event = await factory.event.create({ isPublished: true, isVisible: true, id: 1, slug: 'codefest' });

            await factory.tagCategory.create([
                { id: 1, name: 'Языки', order: 3 },
                { id: 2, name: 'Сообщества', order: 4 },
                { id: 3, name: 'Без тегов', order: 5 },
            ]);

            const tagsForEvent = await factory.tag.create([
                { id: 1, slug: 'js', name: 'Джаваскрирт', order: -5, categoryId: 1 },
                { id: 2, slug: 'java', name: 'Жаба', order: 10, categoryId: 1 },
                { id: 4, slug: 'yatalks', name: 'YaTalks', order: 9, categoryId: 2 },
            ]);

            await factory.tag.create([
                { id: 3, slug: 'go', name: 'Golang', order: 4, categoryId: 1 },
            ]);

            await event.addTags(tagsForEvent);

            await request(app.listen())
                .get('/v1/front/homepage')
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    const { filters } = body;

                    assert.deepStrictEqual(filters.tag, [
                        {
                            label: 'Языки',
                            children: [
                                { id: 1, label: 'Джаваскрирт' },
                                { id: 2, label: 'Жаба' },
                            ],
                        },
                        {
                            label: 'Сообщества',
                            children: [
                                { id: 4, label: 'YaTalks' },
                            ],
                        },
                    ]);
                });
        });
    });
});
