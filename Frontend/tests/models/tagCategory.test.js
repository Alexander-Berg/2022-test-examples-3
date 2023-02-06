const assert = require('assert');
const _ = require('lodash');
const catchErrorAsync = require('catch-error-async');

const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const TagCategory = require('models/tagCategory');

const testDbType = DbType.internal;

describe('TagCategory model', () => {
    beforeEach(cleanDb);

    describe('findOne', () => {
        it('should find a tag category', async() => {
            const tagCategory = await factory.tagCategory.create({ id: 11, name: 'Языки программирования' });

            const actual = await TagCategory.findOne({ id: 11, dbType: testDbType });

            assert.strictEqual(actual.name, tagCategory.name);
        });

        it('should throw if tag category is not found', async() => {
            const error = await catchErrorAsync(
                TagCategory.findOne.bind(TagCategory), { id: 11, scope: 'one', dbType: testDbType },
            );

            assert.strictEqual(error.message, 'Tag category not found');
            assert.strictEqual(error.statusCode, 404);
            assert.deepStrictEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
                scope: 'one',
                dbType: testDbType,
            });
        });
    });

    describe('findPage', () => {
        it('should find tag categories with limit and offset', async() => {
            const tagCategories = _
                .range(1, 26)
                .map(num => ({ id: num, name: `#${num}`, order: num }));

            await factory.tagCategory.create(tagCategories);

            const actual = await TagCategory.findPage({
                scope: 'page',
                pageSize: 3,
                pageNumber: 3,
                dbType: testDbType,
            });

            assert.strictEqual(actual.rows.length, 3);
            assert.strictEqual(actual.rows[0].name, '#19');
            assert.strictEqual(actual.rows[1].name, '#18');
            assert.strictEqual(actual.rows[2].name, '#17');
            assert.strictEqual(actual.meta.totalSize, 25);
            assert.strictEqual(actual.meta.pageNumber, 3);
            assert.strictEqual(actual.meta.pageSize, 3);
        });

        it('should find tag categories by filters', async() => {
            const filterParams = {
                and: [
                    {
                        type: 'string',
                        name: 'name',
                        value: '3',
                        compare: 'cont',
                    },
                ],
            };
            const tagCategories = _
                .range(1, 26)
                .map(num => ({ id: num, name: `#${num}`, order: num }));

            await factory.tagCategory.create(tagCategories);

            const actual = await TagCategory.findPage({
                scope: 'page',
                pageSize: 10,
                pageNumber: 1,
                filterParams,
                dbType: testDbType,
            });

            assert.strictEqual(actual.rows.length, 3);
            assert.strictEqual(actual.rows[0].name, '#23');
            assert.strictEqual(actual.rows[1].name, '#13');
            assert.strictEqual(actual.rows[2].name, '#3');
            assert.strictEqual(actual.meta.totalSize, 3);
            assert.strictEqual(actual.meta.pageNumber, 1);
            assert.strictEqual(actual.meta.pageSize, 10);
        });
    });

    describe('create', () => {
        it('should create a new tag category', async() => {
            const tag = new TagCategory({
                name: 'php',
                order: 5,
            }, { dbType: testDbType });

            const tagCategoryId = await tag.create({ dbType: testDbType });

            const actual = await db.tagCategory.findAll();

            assert.ok(/\d/.test(tagCategoryId));
            assert.strictEqual(actual.length, 1);
            assert.strictEqual(actual[0].name, 'php');
            assert.strictEqual(actual[0].order, 5);
        });
    });

    describe('patch', () => {
        it('should patch an existing tag category', async() => {
            await factory.tagCategory.create({
                id: 13,
                name: 'old tag category',
            });

            const data = {
                id: 13,
                name: 'new tag category',
            };

            const tagCategory = new TagCategory(data, { dbType: testDbType });
            const tagCategoryId = await tagCategory.patch({ yandexuid: '1', dbType: testDbType });
            const actual = await db.tagCategory.findAll();

            assert.ok(/\d/.test(tagCategoryId));
            assert.strictEqual(actual.length, 1);
            assert.strictEqual(actual[0].name, 'new tag category');
        });

        it('should throw error if tag is not exists', async() => {
            const tagCategory = new TagCategory({ id: 13 }, { dbType: testDbType });
            const error = await catchErrorAsync(
                tagCategory.patch.bind(tagCategory, { dbType: testDbType }),
            );

            assert.strictEqual(error.message, 'Tag category not found');
            assert.strictEqual(error.statusCode, 404);
            assert.deepStrictEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });

    describe('destroy', () => {
        it('should destroy a tag category', async() => {
            await factory.tagCategory.create({ id: 11, name: 'php' });

            const destroyed = await TagCategory.destroy(11, { dbType: testDbType });
            const tags = await TagCategory.findPage({ scope: 'page', pageSize: 10, pageNumber: 1, dbType: testDbType });

            assert.strictEqual(destroyed, 1);
            assert.strictEqual(tags.meta.totalSize, 0);
        });

        it('should not destroy a related tags if tag category is deleted', async() => {
            const tagsData = [
                { slug: 'codefest', name: 'codefest' },
                { slug: 'highload', name: 'highload' },
                { slug: 'holyjs', name: 'holyjs' },
            ];

            const tags = await factory.tag.create(tagsData);
            const tagCategory = await factory.tagCategory.create({
                id: 11,
                name: 'javascript',
            });

            await tagCategory.addTags(tags);

            const destroyed = await TagCategory.destroy(11, { dbType: testDbType });
            const actualTags = await db.tag.findAll();

            assert.strictEqual(destroyed, 1);
            assert.strictEqual(actualTags.length, 3);
        });

        it('should throw if tag category is not found', async() => {
            const error = await catchErrorAsync(
                TagCategory.destroy.bind(TagCategory), 11, { dbType: testDbType },
            );

            assert.strictEqual(error.message, 'Tag category not found');
            assert.strictEqual(error.statusCode, 404);
            assert.deepStrictEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
            });
        });
    });

    describe('suggest', () => {
        beforeEach(async() => {
            await factory.tagCategory.create([
                { name: 'котлин' },
                { name: 'эндпоинт' },
                { name: 'фронтэнд' },
            ]);
        });

        it('should return category tags contains substring and order by position', async() => {
            const actual = await TagCategory.suggest('энд', 10, testDbType);

            assert.strictEqual(actual.length, 2);
            assert.strictEqual(actual[0].name, 'эндпоинт');
            assert.strictEqual(actual[1].name, 'фронтэнд');
        });

        it('should return category tags contains any word and order by position', async() => {
            const actual = await TagCategory.suggest('  пои   фронт   ', 10, testDbType);

            assert.strictEqual(actual.length, 2);
            assert.strictEqual(actual[0].name, 'фронтэнд');
            assert.strictEqual(actual[1].name, 'эндпоинт');
        });

        it('should limit category tags', async() => {
            const actual = await TagCategory.suggest('о', 2, testDbType);

            assert.strictEqual(actual.length, 2);
        });

        it('should return all category tags when limit is not passed', async() => {
            const actual = await TagCategory.suggest('о', null, testDbType);

            assert.strictEqual(actual.length, 3);
        });
    });
});
