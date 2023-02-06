const assert = require('assert');
const sinon = require('sinon');
const catchErrorAsync = require('catch-error-async');
const _ = require('lodash');

const { DbType } = require('db/constants');
const { schema } = require('yandex-cfg');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const db = require('db');
const Event = require('models/event');

const testDbType = DbType.internal;

describe('Event model', () => {
    beforeEach(cleanDb);

    describe('findOne', () => {
        it('should find an event', async() => {
            await factory.eventLocation.create({
                eventId: {
                    id: 11,
                    slug: 'devstart',
                },
            });
            await factory.eventBroadcast.create({
                eventId: {
                    id: 11,
                    slug: 'devstart',
                },
                streamId: 'one',
            });

            const actual = await Event.findOne({ id: 11, dbType: testDbType });

            assert.equal(actual.slug, 'devstart');
            assert.equal(actual.broadcasts.length, 1);
            assert.equal(actual.locations.length, 1);
        });

        it('should find an event with tags', async() => {
            const event = await factory.event.create({
                id: 11,
                slug: 'devstart',
            });
            const tags = await factory.tag.create([
                { slug: 'javascript', name: 'javascript' },
                { slug: 'php', name: 'php' },
            ]);

            await event.addTags(tags);

            const actual = await Event.findOne({ id: 11, dbType: testDbType });

            assert.equal(actual.slug, 'devstart');
            assert.equal(actual.tags.length, 2);
            assert.equal(actual.tags[1].slug, 'php');
        });

        it('should order locations and broadcasts properly', async() => {
            const event = {
                id: 11,
                slug: 'devstart',
            };

            await factory.eventLocation.create([
                { eventId: event, place: 'Third', order: 4 },
                { eventId: event, place: 'First', order: 2 },
                { eventId: event, place: 'Second', order: 3 },
            ]);
            await factory.eventBroadcast.create([
                { eventId: event, title: 'three', order: 2 },
                { eventId: event, title: 'one', order: 0 },
                { eventId: event, title: 'two', order: 1 },
            ]);

            const actual = await Event.findOne({ id: 11, dbType: testDbType });

            assert.equal(actual.slug, 'devstart');
            assert.equal(actual.broadcasts[0].title, 'one');
            assert.equal(actual.broadcasts[1].title, 'two');
            assert.equal(actual.broadcasts[2].title, 'three');
            assert.equal(actual.locations[0].place, 'First');
            assert.equal(actual.locations[1].place, 'Second');
            assert.equal(actual.locations[2].place, 'Third');
        });

        it('should throw on nonexistent event', async() => {
            const error = await catchErrorAsync(
                Event.findOne.bind(Event), { id: 11, scope: 'one', dbType: testDbType },
            );

            assert.equal(error.message, 'Event not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
                scope: 'one',
                dbType: testDbType,
            });
        });
    });

    describe('count', () => {
        it('should count all events', async() => {
            const events = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.event.create(events);

            const actual = await Event.count({ scope: 'page', dbType: testDbType });

            assert.equal(actual, 25);
        });
    });

    describe('exists', () => {
        it('should return true: slug', async() => {
            const data = { slug: 'podracing' };
            const event = new Event(data, { dbType: testDbType });

            await factory.event.create(data);

            const actual = await event.exists();

            assert.ok(actual);
        });

        it('should return true: id', async() => {
            const data = { id: 331 };
            const event = new Event(data, { dbType: testDbType });

            await factory.event.create(data);

            const actual = await event.exists();

            assert.ok(actual);
        });

        it('should return false', async() => {
            const data = { id: 331 };
            const event = new Event(data, { dbType: testDbType });

            const actual = await event.exists();

            assert.ok(!actual);
        });
    });

    describe('findList', () => {
        it('should find all events', async() => {
            const events = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.event.create(events);

            const options = { pageSize: 20, pageNumber: 1, scope: 'page', dbType: testDbType };
            const actual = await Event.findPage(options);

            assert.equal(actual.meta.totalSize, 25);
            assert.ok(_.every(actual.rows,
                ({ slug }, i) => slug === events[24 - i].slug));
        });

        it('should find all not started events with registration', async() => {
            const date = new Date('2012-02-16T07:00:00.000Z');
            const clock = sinon.useFakeTimers(date.getTime());

            await factory.event.create([
                // События, которые уже прошли, у которых недоступна регистрация
                // Или свободна и которым не нужно менять статус автоматически
                { registrationStatus: 'free' },
                { registrationStatus: 'free' },
                {
                    registrationStatus: 'opened_later',
                    registrationStartDate: new Date('2045-02-16T04:00:00.000Z'),
                    registrationEndDate: new Date('2045-02-26T04:00:00.000Z'),
                },
                {
                    registrationStatus: 'closed',
                    registrationStartDate: new Date('2012-02-10T04:00:00.000Z'),
                    registrationEndDate: new Date('2012-02-13T04:00:00.000Z'),
                },
                {
                    registrationStatus: 'opened',
                    registrationStartDate: new Date('2012-02-16T04:00:00.000Z'),
                    registrationEndDate: new Date('2012-02-26T04:00:00.000Z'),
                    registrationDateIsConfirmed: false,
                },
                {
                    registrationStatus: 'opened',
                    registrationStartDate: new Date('2012-02-16T04:00:00.000Z'),
                    registrationEndDate: new Date('2012-02-26T04:00:00.000Z'),
                    autoControlRegistration: false,
                },
                // События, у которых сейчас идет регистрация
                {
                    slug: 'registration-is-started',
                    registrationStatus: 'opened_later',
                    registrationStartDate: new Date('2012-02-15T04:00:00.000Z'),
                    registrationEndDate: new Date('2012-02-26T04:00:00.000Z'),
                },
                {
                    slug: 'registration-is-closed',
                    registrationStatus: 'opened',
                    registrationStartDate: new Date('2012-02-10T04:00:00.000Z'),
                    registrationEndDate: new Date('2012-02-16T04:00:00.000Z'),
                },
                {
                    slug: 'registration-is-started2',
                    registrationStatus: 'opened',
                    registrationStartDate: new Date('2012-02-10T04:00:00.000Z'),
                    registrationEndDate: new Date('2012-02-26T04:00:00.000Z'),
                },
            ].map((item, index) => ({
                id: index + 1,
                slug: `#${index}`,
                registrationDateIsConfirmed: true,
                autoControlRegistration: true,
                ...item,
            })));

            const actual = await Event.findAll({ scope: 'sync', dbType: testDbType });

            assert.strictEqual(actual.length, 3);

            clock.restore();
        });
    });

    describe('findPage', () => {
        it('should find events with limit and offset', async() => {
            const now = Date.now();
            const events = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}`, startDate: new Date(now - num * 1000) }));

            await factory.event.create(events);

            const actual = await Event.findPage({ pageSize: 3, pageNumber: 3, scope: 'page', dbType: testDbType });

            assert.equal(actual.rows.length, 3);
            assert.equal(actual.rows[0].slug, '#7');
            assert.equal(actual.rows[1].slug, '#8');
            assert.equal(actual.rows[2].slug, '#9');
            assert.equal(actual.meta.totalSize, 25);
            assert.equal(actual.meta.pageNumber, 3);
            assert.equal(actual.meta.pageSize, 3);
        });

        it('should find events with filter', async() => {
            const filterParams = {
                or: [
                    {
                        type: 'string',
                        name: 'slug',
                        value: '3',
                        compare: 'cont',
                    },
                    {
                        type: 'number',
                        name: 'id',
                        value: '23',
                        compare: 'gt',
                    },
                    {
                        type: 'number',
                        name: 'id',
                        value: [21, 31],
                        compare: 'cont',
                    },
                    {
                        type: 'string',
                        name: 'city',
                        value: ['Msk'],
                        compare: 'ncont',
                    },
                ],
            };
            const now = Date.now();
            const events = _.range(1, 26)
                .map(num => ({
                    id: num, slug: `#${num}`, startDate: new Date(now - num * 1000), city: num === 1 ? 'Nsk' : 'Msk',
                }));

            await factory.event.create(events);

            const actual = await Event.findPage({
                pageSize: 10, pageNumber: 1, scope: 'page', filterParams, dbType: testDbType,
            });

            assert.equal(actual.rows.length, 7);
            assert.equal(actual.rows[0].slug, '#1');
            assert.equal(actual.rows[1].slug, '#3');
            assert.equal(actual.rows[2].slug, '#13');
            assert.equal(actual.rows[3].slug, '#21');
            assert.equal(actual.rows[4].slug, '#23');
            assert.equal(actual.rows[5].slug, '#24');
            assert.equal(actual.rows[6].slug, '#25');
            assert.equal(actual.meta.totalSize, 7);
            assert.equal(actual.meta.pageNumber, 1);
            assert.equal(actual.meta.pageSize, 10);
        });
    });

    describe('create', () => {
        it('should create a new event', async() => {
            const tags = [{
                id: 1,
                slug: 'javascript',
                name: 'JavaScript',
                description: 'Awesome programming language',
                isPublished: true,
                isVisibleInCatalog: false,
                order: 0,
                category: null,
                type: schema.eventTypeEnum.event,
            }];
            const event = new Event({
                slug: 'created',
                startDate: '2018-02-20',
                endDate: '2018-02-21',
                title: 'Starting this',
                tags,
            }, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });

            const eventId = await event.create({ dbType: testDbType });
            const actual = await db.event.findAll();

            assert.ok(/\d/.test(eventId));
            assert.equal(actual.length, 1);
            assert.equal(actual[0].slug, 'created');
        });

        it('should create history record when a event was created', async() => {
            const tags = [{
                id: 1,
                slug: 'javascript',
                name: 'JavaScript',
                description: 'Awesome programming language',
                isPublished: true,
                isVisibleInCatalog: false,
                order: 0,
                category: null,
                type: schema.eventTypeEnum.event,
            }];
            const event = new Event({
                slug: 'created',
                startDate: '2018-02-20',
                endDate: '2018-02-21',
                title: 'Starting this',
                tags,
            }, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });

            const eventId = await event.create({ dbType: testDbType });
            const [record] = await db.history.findAll();
            const actual = _.pick(record, ['entityId', 'authorLogin', 'operation', 'entityType']);
            const expected = {
                entityId: eventId,
                authorLogin: 'saaaaaaaaasha',
                operation: 'create',
                entityType: 'event',
            };

            assert.deepEqual(actual, expected);
        });

        it('should throw if event with such id already exists', async() => {
            await factory.event.create({ id: 13 });
            const event = new Event({ id: 13 }, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });
            const error = await catchErrorAsync(event.create.bind(event));

            assert.equal(error.message, 'Event already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                id: 13,
            });
        });

        it('should throw if event with such slug already exists', async() => {
            await factory.event.create({ slug: 'Outreach' });
            const event = new Event({ slug: 'Outreach' }, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });
            const error = await catchErrorAsync(event.create.bind(event, { dbType: testDbType }));

            assert.equal(error.message, 'Event already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                slug: 'Outreach',
            });
        });
    });

    describe('delete', () => {
        it('should delete event with locations and broadcasts', async() => {
            await factory.eventLocation.create({
                eventId: {
                    id: 11,
                    slug: 'devstart',
                },
            });
            await factory.eventBroadcast.create([
                { eventId: 11, streamId: 'one' },
            ]);

            const deleted = await Event.destroy(11, { user: {}, authorLogin: 'saaaaaaaaasha', dbType: testDbType });

            const eventsCount = await db.event.count();
            const locationsCount = await db.eventLocation.count();
            const broadcastsCount = await db.eventBroadcast.count();

            assert.ok(deleted);
            assert.equal(eventsCount, 0);
            assert.equal(locationsCount, 0);
            assert.equal(broadcastsCount, 0);
        });

        it('should create history record when a event was destroyed', async() => {
            await factory.event.create({ id: 11, slug: 'devstart' });

            await Event.destroy(11, { user: {}, authorLogin: 'saaaaaaaaasha', dbType: testDbType });

            const [record] = await db.history.findAll();
            const actual = _.pick(record, ['entityId', 'authorLogin', 'operation', 'entityType']);
            const expected = {
                entityId: 11,
                authorLogin: 'saaaaaaaaasha',
                operation: 'destroy',
                entityType: 'event',
            };

            assert.deepEqual(actual, expected);
        });

        it('should delete event with tag links', async() => {
            const event = await factory.event.create({
                id: 11,
                slug: 'devstart',
            });
            const tags = await factory.tag.create([
                { slug: 'javascript', name: 'javascript' },
                { slug: 'php', name: 'php' },
            ]);

            await event.addTags(tags);

            const deleted = await Event.destroy(11, { user: {}, authorLogin: 'saaaaaaaaasha', dbType: testDbType });
            const eventsCount = await db.event.count();
            const linksCount = await db.eventTags.count();
            const tagsCount = await db.tag.count();

            assert.ok(deleted);
            assert.equal(eventsCount, 0);
            assert.equal(linksCount, 0);
            assert.equal(tagsCount, 2);
        });

        it('should throw on nonexistent event', async() => {
            const error = await catchErrorAsync(
                Event.destroy.bind(Event), 11, { user: {}, dbType: testDbType },
            );

            assert.equal(error.message, 'Event not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(_.omit(error.options, 'scope'), {
                internalCode: '404_ENF',
                id: 11,
                dbType: testDbType,
            });
        });
    });

    describe('patch', () => {
        it('should patch an existing event', async() => {
            await factory.event.create({
                id: 13,
                slug: 'old-event',
                isPublished: false,
            });

            const data = {
                id: 13,
                isPublished: true,
            };

            const event = new Event(data, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });
            const eventId = await event.patch({ dbType: testDbType });
            const actual = await db.event.findAll();

            assert.ok(/\d/.test(eventId));
            assert.equal(actual.length, 1);
            assert.equal(actual[0].isPublished, true);
        });

        it('should create history record when a event was updated', async() => {
            await factory.event.create({ id: 11, slug: 'devstart' });

            const data = { id: 11, isPublished: true };
            const event = new Event(data, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });
            const eventId = await event.patch({ dbType: testDbType });

            const [record] = await db.history.findAll();
            const actual = _.pick(record, ['entityId', 'authorLogin', 'operation', 'entityType']);
            const expected = {
                entityId: eventId,
                authorLogin: 'saaaaaaaaasha',
                operation: 'update',
                entityType: 'event',
            };

            assert.deepEqual(actual, expected);
        });

        it('should not patch other events', async() => {
            await factory.event.create([{
                id: 13,
                slug: 'old-event',
                isPublished: false,
            }, {
                id: 15,
                slug: 'one-more-event',
                isPublished: false,
            }]);

            const data = {
                id: 13,
                isPublished: true,
            };

            const event = new Event(data, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });
            const eventId = await event.patch({ dbType: testDbType });
            const actual = await db.event.findAll({ order: [['id']] });

            assert.ok(/\d/.test(eventId));
            assert.equal(actual.length, 2);
            assert.equal(actual[0].isPublished, true);
            assert.equal(actual[1].isPublished, false);
        });

        it('should patch event locations', async() => {
            const eventData = { id: 13, slug: 'old-event' };

            await factory.eventLocation.create({ eventId: eventData });

            const data = {
                id: 13,
                locations: [{
                    place: 'Nsk, Nikolaeva 12',
                    description: 'Map#1',
                    lat: 56.111,
                    lon: 11.566,
                    zoom: 18,
                    order: 1,
                }, {
                    place: 'Nsk, Nikolaeva 12',
                    description: 'Map#2',
                    lat: 56.111,
                    lon: 11.566,
                    zoom: 17,
                    order: 4,
                }],
            };
            const event = new Event(data, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });
            const eventId = await event.patch({ dbType: testDbType });
            const actual = await db.eventLocation.findAll({ order: [['order']] });

            assert.equal(actual.length, 2);
            assert.ok(_.every(actual, { eventId }));
            assert.equal(actual[0].description, 'Map#1');
            assert.equal(actual[1].description, 'Map#2');
        });

        it('should not remove event locations if locations: undefined passed', async() => {
            const eventData = { id: 13, slug: 'old-event' };

            await factory.eventLocation.create({ eventId: eventData });

            const data = { id: 13 };
            const event = new Event(data, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });

            await event.patch({ dbType: testDbType });

            const locationsCount = await db.eventLocation.count();

            assert.equal(locationsCount, 1);
        });

        it('should remove event locations if locations: [] passed', async() => {
            const eventData = { id: 13, slug: 'old-event' };

            await factory.eventLocation.create({ eventId: eventData });

            const data = {
                id: 13,
                locations: [],
            };
            const event = new Event(data, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });

            await event.patch({ dbType: testDbType });

            const locationsCount = await db.eventLocation.count();

            assert.equal(locationsCount, 0);
        });

        it('should patch event broadcasts', async() => {
            const eventData = { id: 13, slug: 'old-event' };

            await factory.eventBroadcast.create({ eventId: eventData });

            const data = {
                id: 13,
                broadcasts: [{
                    title: 'Watch this',
                    iframeUrl: 'http://some.url.ru',
                    iframeWidth: 480,
                    iframeHeight: 320,
                    order: 1,
                }, {
                    title: 'Watch this two',
                    iframeUrl: 'http://some.url.ru',
                    iframeWidth: 480,
                    iframeHeight: 320,
                    order: 6,
                }],
            };
            const event = new Event(data, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });
            const eventId = await event.patch({ dbType: testDbType });
            const actual = await db.eventBroadcast.findAll({ order: [['order']] });

            assert.equal(actual.length, 2);
            assert.ok(_.every(actual, { eventId }));
            assert.equal(actual[0].title, 'Watch this');
            assert.equal(actual[1].title, 'Watch this two');
        });

        it('should not remove event broadcasts if broadcasts: undefined passed', async() => {
            const eventData = { id: 13, slug: 'old-event' };

            await factory.eventBroadcast.create({ eventId: eventData });

            const data = { id: 13 };
            const event = new Event(data, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });

            await event.patch({ dbType: testDbType });

            const broadcastsCount = await db.eventBroadcast.count();

            assert.equal(broadcastsCount, 1);
        });

        it('should remove event broadcasts if broadcasts: [] passed', async() => {
            const eventData = { id: 13, slug: 'old-event' };

            await factory.eventBroadcast.create({ eventId: eventData });

            const data = {
                id: 13,
                broadcasts: [],
            };
            const event = new Event(data, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });

            await event.patch({ dbType: testDbType });

            const broadcastsCount = await db.eventBroadcast.count();

            assert.equal(broadcastsCount, 0);
        });

        it('should throw on slug already exist', async() => {
            await factory.event.create([
                {
                    id: 13,
                    slug: 'fronttalks',
                },
                {
                    id: 14,
                    slug: 'codefest',
                },
            ]);

            const data = {
                id: 13,
                slug: 'codefest',
            };

            const event = new Event(data, { authorLogin: 'saaaaaaaaasha', dbType: testDbType });

            const error = await catchErrorAsync(
                event.patch.bind(event, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Event already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                slug: 'codefest',
            });
        });

        it('should throw on nonexistent event', async() => {
            const event = new Event({ id: 13 }, { dbType: testDbType });
            const error = await catchErrorAsync(
                event.patch.bind(event, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Event not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });

    describe('addTags', () => {
        it('should add event tags', async() => {
            await factory.event.create({ id: 13, slug: 'JS Party' });
            await factory.tag.create([
                { id: 3, slug: 'javascript', name: 'Javascript' },
                { id: 4, slug: 'node.js', name: 'Node.js' },
                { id: 5, slug: 'v8', name: 'V8' },
                { id: 6, slug: 'cpp', name: 'C++' },
            ]);

            await Event.addTags(13, [3, 4, 5], { authorLogin: 'art00', dbType: testDbType });
            const actual = await db.eventTags.findAll({
                raw: true,
                order: [['tagId']],
            });

            assert.equal(actual.length, 3);
            assert.ok(_.every(actual, { eventId: 13 }));
            assert.equal(actual[0].tagId, 3);
            assert.equal(actual[1].tagId, 4);
            assert.equal(actual[2].tagId, 5);
        });

        it('should create history record when a tags were added', async() => {
            await factory.event.create({ id: 13, slug: 'JS Party' });
            await factory.tag.create([
                { id: 3, slug: 'javascript', name: 'Javascript' },
                { id: 4, slug: 'node.js', name: 'Node.js' },
                { id: 5, slug: 'v8', name: 'V8' },
                { id: 6, slug: 'cpp', name: 'C++' },
            ]);

            await Event.addTags(13, [3, 4, 5], { authorLogin: 'art00', dbType: testDbType });

            const [record] = await db.history.findAll();
            const actual = _.pick(record, ['entityId', 'authorLogin', 'operation', 'entityType']);
            const expected = {
                entityId: 13,
                authorLogin: 'art00',
                operation: 'update',
                entityType: 'event',
            };

            assert.deepEqual(actual, expected);
        });

        it('should throw on nonexistent event', async() => {
            const error = await catchErrorAsync(
                Event.addTags.bind(Event), 13, [3, 4, 5], { authorLogin: 'art00', dbType: testDbType },
            );

            assert.equal(error.message, 'Event not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });

    describe('removeTags', () => {
        it('should remove event tags', async() => {
            const event = await factory.event.create({ id: 13, slug: 'JS Party' });
            const tags = await factory.tag.create([
                { id: 3, slug: 'javascript', name: 'Javascript' },
                { id: 4, slug: 'node.js', name: 'Node.js' },
                { id: 5, slug: 'v8', name: 'V8' },
            ]);

            await event.addTags(tags);
            const added = await db.eventTags.count();

            assert.equal(added, 3);

            await Event.removeTags(13, [4, 5], { authorLogin: 'art00', dbType: testDbType });

            const actual = await db.eventTags.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].eventId, 13);
            assert.equal(actual[0].tagId, 3);
        });

        it('should throw on nonexistent event', async() => {
            const error = await catchErrorAsync(
                Event.removeTags.bind(Event), 13, [3, 4, 5], { authorLogin: 'art00', dbType: testDbType },
            );

            assert.equal(error.message, 'Event not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });

    describe('addGroups', () => {
        it('should add event groups', async() => {
            await factory.event.create({ id: 13, slug: 'JS Party' });
            await factory.group.create([
                { id: 3, slug: 'javascript', name: 'Javascript' },
                { id: 4, slug: 'node.js', name: 'Node.js' },
                { id: 5, slug: 'v8', name: 'V8' },
                { id: 6, slug: 'cpp', name: 'C++' },
            ]);

            await Event.addGroups(13, [3, 4, 5], { authorLogin: 'art00', dbType: testDbType });
            const actual = await db.eventsGroup.findAll({
                raw: true,
                order: [['groupId']],
            });

            assert.equal(actual.length, 3);
            assert.ok(_.every(actual, { eventId: 13 }));
            assert.equal(actual[0].groupId, 3);
            assert.equal(actual[1].groupId, 4);
            assert.equal(actual[2].groupId, 5);
        });

        it('should create history record when a groups were added', async() => {
            await factory.event.create({ id: 13, slug: 'JS Party' });
            await factory.group.create([
                { id: 3, slug: 'javascript', name: 'Javascript' },
                { id: 4, slug: 'node.js', name: 'Node.js' },
                { id: 5, slug: 'v8', name: 'V8' },
                { id: 6, slug: 'cpp', name: 'C++' },
            ]);

            await Event.addGroups(13, [3, 4, 5], { authorLogin: 'art00', dbType: testDbType });

            const [record] = await db.history.findAll();
            const actual = _.pick(record, ['entityId', 'authorLogin', 'operation', 'entityType']);
            const expected = {
                entityId: 13,
                authorLogin: 'art00',
                operation: 'update',
                entityType: 'event',
            };

            assert.deepEqual(actual, expected);
        });

        it('should throw on nonexistent event', async() => {
            const error = await catchErrorAsync(
                Event.addGroups.bind(Event), 13, [3, 4, 5], { authorLogin: 'art00', dbType: testDbType },
            );

            assert.equal(error.message, 'Event not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });

    describe('removeGroups', () => {
        it('should remove event groups', async() => {
            const event = await factory.event.create({ id: 13, slug: 'JS Party' });
            const groups = await factory.group.create([
                { id: 3, slug: 'javascript', name: 'Javascript' },
                { id: 4, slug: 'node.js', name: 'Node.js' },
                { id: 5, slug: 'v8', name: 'V8' },
            ]);

            await event.addGroups(groups);
            const added = await db.eventsGroup.count();

            assert.equal(added, 3);

            await Event.removeGroups(13, ['4', '5'], { authorLogin: 'art00', dbType: testDbType });

            const actual = await db.eventsGroup.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].eventId, 13);
            assert.equal(actual[0].groupId, 3);
        });

        it('should throw on remove last group', async() => {
            const event = await factory.event.create({ id: 13, slug: 'JS Party' });

            const groups = await factory.group.create([
                { id: 3, slug: 'javascript', name: 'Javascript' },
                { id: 4, slug: 'node.js', name: 'Node.js' },
                { id: 5, slug: 'v8', name: 'V8' },
            ]);

            await event.addGroups(groups);

            const error = await catchErrorAsync(
                Event.removeGroups.bind(Event), 13, ['3', '4', '5'], { authorLogin: 'art00', dbType: testDbType },
            );

            assert.equal(error.message, 'Can\'t remove last groups');
            assert.equal(error.statusCode, 400);
        });

        it('should throw on nonexistent event', async() => {
            const error = await catchErrorAsync(
                Event.removeGroups.bind(Event), 13, ['3', '4', '5'], { authorLogin: 'art00', dbType: testDbType },
            );

            assert.equal(error.message, 'Event not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
                dbType: testDbType,
            });
        });
    });

    describe('findProgramByEventId', () => {
        it('should find program for event', async() => {
            const programItems = [
                {
                    startDate: new Date('2018-02-16T04:00:00.000Z'),
                    endDate: new Date('2018-02-16T11:30:00.000Z'),
                    isTalk: true,
                    eventId: '8',
                    title: 'Механизм миграций в sequelize',
                },
                {
                    startDate: new Date('2018-02-16T11:30:00.000Z'),
                    endDate: new Date('2018-02-16T12:00:00.000Z'),
                    isTalk: true,
                    eventId: '8',
                    title: 'Как писать свой велосипед',
                },
            ];

            await factory.event.create({ id: '8', slug: 'codefest', title: 'codefest' });
            await factory.programItem.create(programItems);

            const actual = await Event.findProgramByEventId('8', { dbType: testDbType });

            assert.equal(actual.length, 2);

            // Сортировка по дате начала доклада
            assert.equal(actual[0].title, programItems[0].title);
            assert.equal(actual[0].isTalk, programItems[0].isTalk);
        });

        it('should throw on nonexistent event', async() => {
            const error = await catchErrorAsync(
                Event.findProgramByEventId.bind(Event), 11, { dbType: testDbType },
            );

            assert.equal(error.message, 'Event not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
            });
        });
    });
});
