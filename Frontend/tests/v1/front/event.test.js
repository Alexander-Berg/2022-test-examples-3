const assert = require('assert');
const config = require('yandex-cfg');
const request = require('supertest');
const _ = require('lodash');
const moment = require('moment');

const app = require('app');
const db = require('db');
const { DbType } = require('db/constants');
const cleanDb = require('tests/db/clean');
const factory = require('tests/db/factory');

// Свойтва событий, которые должны быть установлены
const defaults = { isPublished: true, isVisible: true };

describe('Event controller', () => {
    beforeEach(cleanDb);

    describe('getBadgeQrCode', () => {
        it('should return qr code image', async() => {
            const secretKey = 'eee20546-1560-11e9-ab14-d663bd873d93';

            await request(app.listen())
                .get('/v1/front/events/1/registrations/1/qr')
                .query({ secretKey })
                .expect('Content-Type', /image\/png/)
                .expect(200);
        });

        it('should throw 400 error when secret key has wrong format', async() => {
            const wrongSecretKey = 'an-invalid-secret';

            await request(app.listen())
                .get('/v1/front/events/1/registrations/1/qr')
                .query({ secretKey: wrongSecretKey })
                .expect(400)
                .expect({
                    internalCode: '400_UIF',
                    message: 'secretKey has invalid uuid format',
                    value: wrongSecretKey,
                });
        });
    });

    describe('getSpeakers', () => {
        it('should return speakers list', async() => {
            const speakers = [
                {
                    id: 1,
                    avatar: null,
                    firstName: 'Энакин',
                    lastName: 'Скайуокер',
                    middleName: 'Иванович',
                    about: 'Я человек, и моё имя — Энакин!',
                    jobPlace: 'Орден джедаев',
                    jobPosition: 'Джуниор-падаван',
                },
                {
                    id: 2,
                    avatar: null,
                    firstName: 'Энакин',
                    lastName: 'Скайуокер',
                    middleName: 'Иванович',
                    about: 'Я человек, и моё имя — Энакин!',
                    jobPlace: 'Орден джедаев',
                    jobPosition: 'Джуниор-падаван',
                },
            ];

            await factory.event.create({ id: 1, slug: 'codefest', title: 'codefest' });
            await createProgramItemWithSpeakers({
                item: { id: 1, eventId: 1 },
                speakers,
            });
            await factory.programItem.create({ id: 2, eventId: 1 });
            await db.programItemSpeakers.create({ speakerId: 1, programItemId: 2 });

            await request(app.listen())
                .get('/v1/front/events/1/speakers')
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(speakers);
        });

        it('should throw 404 error when event doesn\'t exist', async() => {
            await request(app.listen())
                .get('/v1/front/events/1/speakers')
                .expect(404)
                .expect({
                    internalCode: '404_ENF',
                    message: 'Event not found',
                    scope: 'speakers',
                    dbType: DbType.internal,
                    id: '1',
                });
        });
    });

    describe('getProgram', () => {
        it('should return program', async() => {
            const speaker = {
                id: 1,
                avatar: null,
                firstName: 'Энакин',
                lastName: 'Скайуокер',
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

            await factory.event.create({ id: 1, slug: 'codefest', title: 'codefest' });
            await factory.section.create(section);
            await factory.programItem.create(programItem);
            await factory.speaker.create(speaker);
            await factory.tag.create(tag);
            await factory.video.create(video);
            await factory.presentation.create(presentation);
            await db.programItemTags.create({ tagId: 1, programItemId: 1 });
            await db.programItemSpeakers.create({ speakerId: 1, programItemId: 1 });

            const expectedSpeaker = _.omit({
                ...speaker,
                company: speaker.jobPlace,
                position: speaker.jobPosition,
            }, ['jobPlace', 'jobPosition']);

            await request(app.listen())
                .get('/v1/front/events/1/program')
                .expect('Content-Type', /json/)
                .expect(200)
                .expect([{
                    ..._.omit(section, ['eventId', 'isPublished', 'type']),
                    programItems: [{
                        ..._.omit(programItem, ['sectionId', 'eventId', 'isPublished']),
                        speakers: [expectedSpeaker],
                        presentations: [_.omit(presentation, ['programItemId'])],
                        videos: [_.omit(video, ['programItemId'])],
                    }],
                }]);
        });

        it('should throw 404 error when event doesn\'t exist', async() => {
            await request(app.listen())
                .get('/v1/front/events/1/program')
                .expect(404)
                .expect({
                    internalCode: '404_ENF',
                    message: 'Event not found',
                    scope: 'frontProgram',
                    dbType: DbType.internal,
                    id: '1',
                });
        });
    });

    describe('findPage', () => {
        it('should find event\'s page', async() => {
            await factory.event.create([
                { ...defaults, id: 1, slug: 'codefest', title: 'code fest' },
                { ...defaults, id: 2, slug: 'fronttalks', title: 'front talks' },
                { ...defaults, id: 3, slug: 'reactmeetup', title: 'react meetup', isPublished: false },
                { ...defaults, id: 4, slug: 'jsparty', title: 'yandex ysparty', isVisible: false },
            ]);

            await request(app.listen())
                .get('/v1/front/events')
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.rows.length, 2);
                    assert.strictEqual(body.rows[0].id, 2);
                    assert.strictEqual(body.rows[1].id, 1);
                });
        });

        it('should find events with sorting', async() => {
            await factory.event.create([
                { ...defaults, id: 1, slug: 'codefest', title: 'code fest' },
                { ...defaults, id: 2, slug: 'fronttalks', title: 'front talks' },
                { ...defaults, id: 3, slug: 'a reactmeetup', title: 'react' },
            ]);

            await request(app.listen())
                .get('/v1/front/events')
                .query({ sortBy: 'slug', sortOrder: 'ASC' })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 3);
                    assert.strictEqual(body.rows[0].id, 3);
                    assert.strictEqual(body.rows[1].id, 1);
                    assert.strictEqual(body.rows[2].id, 2);
                });
        });

        it('should find events by filter', async() => {
            await factory.event.create([
                { ...defaults, id: 1, slug: 'codefest', title: 'code fest', isAcademy: true },
                { ...defaults, id: 2, slug: 'fronttalks', title: 'front talks' },
                { ...defaults, id: 3, slug: 'reactmeetup', title: 'react', isPublished: false, isAcademy: true },
            ]);

            const filters = { or: [{ isAcademy: 'true' }] };

            await request(app.listen())
                .get('/v1/front/events')
                .query({ filters: JSON.stringify(filters) })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 1);
                    assert.strictEqual(body.rows.length, 1);
                    assert.strictEqual(body.rows[0].id, 1);
                });
        });

        it('should throw error if pageNumber is invalid', async() => {
            await request(app.listen())
                .get('/v1/front/events')
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
            await request(app.listen())
                .get('/v1/front/events')
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

    describe('findOne', () => {
        it('should find event', async() => {
            const event = await factory.event.create({ ...defaults, id: 1, slug: 'codefest' });
            const tags = await factory.tag.create([
                { id: 1, slug: 'javascript', name: 'JavaScript', isPublished: true },
                { id: 2, slug: 'php', name: 'PHP', isPublished: true, order: 5 },
                { id: 3, slug: 'arcadia', name: 'arcadia', isPublished: false },
            ]);

            await event.addTags(tags);

            await request(app.listen())
                .get('/v1/front/events/1')
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.id, 1);
                    assert.strictEqual(body.tags.length, 2);
                    assert.strictEqual(body.tags[0].id, 2);
                });
        });

        it('should throw error when event is not published', async() => {
            await factory.event.create({ id: 1, slug: 'codefest', isPublished: false });

            await request(app.listen())
                .get('/v1/front/events/1')
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    internalCode: '404_ENF',
                    message: 'Event not found',
                    id: '1',
                    dbType: DbType.internal,
                    scope: 'frontOne',
                });
        });

        it('should throw error when eventId is invalid', async() => {
            await request(app.listen())
                .get('/v1/front/events/inv@lid')
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Event ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('feed', () => {
        it('should find events to feed', async() => {
            const momentDate = moment().startOf('day');
            const startDate = momentDate.add(1, 'days').toDate();
            const wrongStartDate = momentDate.subtract(1, 'days').toDate();
            const commonData = {
                ...defaults,
                startDate,
                title: 'Test',
                registrationStatus: 'online',
                city: 'Test city',
            };
            const events = [
                { ...commonData, id: 1, slug: 'right-1', startDate, lpcPagePath: 'test' },
                {
                    ...commonData,
                    id: 2,
                    slug: 'right-2',
                    startDate: momentDate.add(2, 'days').toDate(),
                    redirectUrl: 'https://test.redirect.url',
                    lpcPagePath: 'test',
                },
                {
                    ...commonData,
                    id: 3,
                    slug: 'right-3',
                    startDate: momentDate.add(3, 'days').toDate(),
                    lpcPagePath: 'test',
                },
                {
                    ...commonData,
                    id: 4,
                    slug: 'right-4',
                    startDate: momentDate.add(4, 'days').toDate(),
                    lpcPagePath: 'test',
                },
                { ...commonData, id: 5, slug: 'wrong-1', startDate: wrongStartDate, lpcPagePath: 'test' },
                { ...commonData, id: 6, slug: 'wrong-2', startDate, lpcPagePath: null },
                { ...commonData, id: 7, slug: 'wrong-3', startDate, isPublished: false },
                { ...commonData, id: 8, slug: 'wrong-4', startDate, isVisible: false },
            ];

            await factory.event.create(events);

            await request(app.listen())
                .get('/v1/front/events/feed')
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.length, 3);

                    const [firstEvent, secondEvent] = body;

                    assert.strictEqual(firstEvent.slug, 'right-1');
                    assert.strictEqual(firstEvent.name, 'Test');
                    assert.strictEqual(firstEvent.city, 'Test city');
                    assert.strictEqual(firstEvent.date_start, moment(startDate).format('YYYY-MM-DD'));
                    assert.strictEqual(firstEvent.url,
                        `${config.frontend.endpoint.replace('{tld}', 'ru')}events/right-1`);

                    assert.strictEqual(secondEvent.url, 'https://test.redirect.url');
                });
        });
    });
});

// Используется для создания ассоциаций, через factory так не получится.
async function createProgramItemWithSpeakers({ item, speakers }) {
    const itemCreateOptions = { include: [{ model: db.speaker, as: 'speakers' }] };

    await db.programItem.create({
        ...factory.programItem.defaultData,
        ...item,
        speakers: speakers.map(speaker => ({ ...factory.speaker.defaultData, ...speaker })),
    }, itemCreateOptions);
}
