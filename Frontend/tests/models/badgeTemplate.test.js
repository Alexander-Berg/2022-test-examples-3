const assert = require('assert');
const _ = require('lodash');
const catchErrorAsync = require('catch-error-async');

const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const BadgeTemplate = require('models/badgeTemplate');

const testDbType = DbType.internal;

describe('BadgeTemplate model', () => {
    beforeEach(cleanDb);

    describe('findOne', () => {
        it('should find a badge template', async() => {
            const tplsData = [
                { id: 11, slug: 'tpl1' },
                { id: 12, slug: 'tpl2' },
            ];

            await factory.badgeTemplate.create(tplsData);

            const actual = await BadgeTemplate.findOne({ id: 11, dbType: testDbType });

            assert.equal(actual.slug, 'tpl1');
        });

        it('should find a badge template by slug', async() => {
            await factory.badgeTemplate.create({ id: 11, slug: 'tpl1' }, { dbType: testDbType });

            const actual = await BadgeTemplate.findOne({ slug: 'tpl1', dbType: testDbType });

            assert.equal(actual.slug, 'tpl1');
            assert.equal(actual.id, 11);
        });

        it('should throw if badge template is not found', async() => {
            const error = await catchErrorAsync(
                BadgeTemplate.findOne.bind(BadgeTemplate), { id: 11, scope: 'one', dbType: testDbType },
            );

            assert.equal(error.message, 'Badge template not found');
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
        it('should count all badge templates', async() => {
            const tpls = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.badgeTemplate.create(tpls);

            const actual = await BadgeTemplate.count({ scope: 'list', dbType: testDbType });

            assert.equal(actual, 25);
        });
    });

    describe('findList', () => {
        it('should find all badge templates', async() => {
            const badgeTemplates = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.badgeTemplate.create(badgeTemplates);

            const actual = await BadgeTemplate.findAll({ scope: 'list', order: [['id']], dbType: testDbType });

            assert.equal(actual.length, 25);
            assert.equal(actual[0].slug, '#1');
            assert.equal(actual[1].slug, '#2');
            assert.equal(actual[2].slug, '#3');
        });
    });

    describe('create', () => {
        it('should create a new badge template', async() => {
            const badgeTemplate = new BadgeTemplate({
                slug: 'badge_template',
                data: [{ content: '{username}' }],
            }, { authorLogin: 'art00', dbType: testDbType });

            const templateId = await badgeTemplate.create({ dbType: testDbType });
            const actual = await db.badgeTemplate.findAll();

            assert.ok(/\d/.test(templateId));
            assert.equal(actual.length, 1);
            assert.equal(actual[0].slug, 'badge_template');
        });

        it('should throw if badge template with such id already exists', async() => {
            await factory.badgeTemplate.create({ id: 10 });
            const badgeTemplate = new BadgeTemplate(
                { id: 10, slug: 'test' }, { authorLogin: 'art00', dbType: testDbType },
            );
            const error = await catchErrorAsync(badgeTemplate.create.bind(badgeTemplate));

            assert.equal(error.message, 'Badge template already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                id: 10,
                slug: 'test',
            });
        });

        it('should throw if badge template with such slug already exists', async() => {
            await factory.badgeTemplate.create({ slug: 'test' });
            const badgeTemplate = new BadgeTemplate(
                { id: 11, slug: 'test' }, { authorLogin: 'art00', dbType: testDbType },
            );
            const error = await catchErrorAsync(badgeTemplate.create.bind(badgeTemplate));

            assert.equal(error.message, 'Badge template already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                id: 11,
                slug: 'test',
            });
        });
    });

    describe('delete', () => {
        it('should delete badge template', async() => {
            await factory.badgeTemplate.create({ id: 15, slug: 'test' });

            const deleted = await BadgeTemplate.destroy(15, { authorLogin: 'art00', dbType: testDbType });

            const count = await db.badgeTemplate.count();

            assert.ok(deleted);
            assert.equal(count, 0);
        });

        it('should throw if nonexistent badge template', async() => {
            const error = await catchErrorAsync(
                BadgeTemplate.destroy.bind(BadgeTemplate), 11, { dbType: testDbType },
            );

            assert.equal(error.message, 'Badge template not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
            });
        });
    });

    describe('patch', () => {
        it('should patch an existing badge template', async() => {
            await factory.badgeTemplate.create({
                id: 13,
                slug: 'test',
            });

            const newContent = [{ test: 1 }];
            const data = {
                id: 13,
                data: newContent,
            };

            const badgeTemplate = new BadgeTemplate(data, { authorLogin: 'art00', dbType: testDbType });
            const badgeTemplateId = await badgeTemplate.patch({ dbType: testDbType });
            const actual = await db.badgeTemplate.findAll();

            assert.ok(/\d/.test(badgeTemplateId));
            assert.equal(actual.length, 1);
            assert.deepEqual(actual[0].data, newContent);
            assert.deepEqual(actual[0].updatedByLogin, 'art00');
        });

        it('should not patch other badge templates', async() => {
            await factory.badgeTemplate.create([{
                id: 13,
                slug: 'old-tpl',
                data: [{ test: 1 }],
            }, {
                id: 15,
                slug: 'one-more-tpl',
                data: [{ test: 1 }],
            }]);

            const data = {
                id: 13,
                data: [{ test: 2 }],
            };

            const badgeTemplate = new BadgeTemplate(data, { authorLogin: 'art00', dbType: testDbType });
            const badgeTemplateId = await badgeTemplate.patch({ dbType: testDbType });
            const actual = await db.badgeTemplate.findAll({ order: [['id']] });

            assert.ok(/\d/.test(badgeTemplateId));
            assert.equal(actual.length, 2);
            assert.deepEqual(actual[0].data, [{ test: 2 }]);
            assert.deepEqual(actual[1].data, [{ test: 1 }]);
        });

        it('should throw on slug already exist', async() => {
            await factory.badgeTemplate.create([
                {
                    id: 13,
                    slug: 'badge1',
                },
                {
                    id: 14,
                    slug: 'badge2',
                },
            ]);

            const data = {
                id: 13,
                slug: 'badge2',
            };

            const badgeTemplate = new BadgeTemplate(data, { authorLogin: 'art00', dbType: testDbType });
            const error = await catchErrorAsync(
                badgeTemplate.patch.bind(badgeTemplate),
            );

            assert.equal(error.message, 'Badge template already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                slug: 'badge2',
            });
        });

        it('should throw on nonexistent badge template', async() => {
            const badgeTemplate = new BadgeTemplate({ id: 13 }, { dbType: testDbType });
            const error = await catchErrorAsync(
                badgeTemplate.patch.bind(badgeTemplate),
            );

            assert.equal(error.message, 'Badge template not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });
});
