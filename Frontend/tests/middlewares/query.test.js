const assert = require('assert');
const sinon = require('sinon');
const _ = require('lodash');

const logger = require('lib/logger');
const catchError = require('middlewares/catchError');
const query = require('middlewares/query');
const Base = require('models/base');
const Event = require('models/event');
const schema = require('lib/schema');

const eventSchema = schema.event;

describe('Query middleware', () => {
    beforeEach(() => sinon.spy(logger, 'error'));
    afterEach(() => logger.error.restore());

    describe('getPagination', () => {
        it('should return pagination from query', async() => {
            const pagination = { pageNumber: 2, pageSize: 3 };
            const ctx = { query: pagination };
            const next = sinon.spy();

            await query(ctx, next);

            const actual = ctx.queryHelper.getPagination({});

            assert.deepEqual(actual, pagination);
            assert(next.calledOnce, 'Next callback should called once');
        });

        it('should return pagination defaults', async() => {
            const pagination = { pageNumber: 2, pageSize: 3 };
            const ctx = { query: {} };
            const next = sinon.spy();

            await query(ctx, next);

            const actual = ctx.queryHelper.getPagination(pagination);

            assert.deepEqual(actual, pagination);
            assert(next.calledOnce, 'Next callback should called once');
        });

        it('should merge query and defaults params', async() => {
            const ctx = { query: { pageNumber: 2 } };
            const next = sinon.spy();

            await query(ctx, next);

            const actual = ctx.queryHelper.getPagination({ pageNumber: 4, pageSize: 3 });

            assert.deepEqual(actual, { pageNumber: 2, pageSize: 3 });
            assert(next.calledOnce, 'Next callback should called once');
        });

        it('should throw error when pageNumber is not integer', async() => {
            const ctx = { logger, query: { pageNumber: 'invalid' } };
            const next = sinon.spy();

            await query(ctx, next);

            await catchError(ctx, ctx.queryHelper.getPagination);

            assert(next.calledOnce, 'Next callback should called once');
            assert.strictEqual(ctx.status, 400);
            assert.deepStrictEqual(ctx.body, {
                message: 'Page number is invalid',
                internalCode: '400_PII',
                value: 'invalid',
            });
        });

        it('should throw error when pageSize is not integer', async() => {
            const ctx = { logger, query: { pageNumber: 2, pageSize: 'invalid' } };
            const next = sinon.spy();

            await query(ctx, next);

            await catchError(ctx, ctx.queryHelper.getPagination);

            assert(next.calledOnce, 'Next callback should called once');
            assert.strictEqual(ctx.status, 400);
            assert.deepStrictEqual(ctx.body, {
                message: 'Page size is invalid',
                internalCode: '400_PII',
                value: 'invalid',
            });
        });
    });

    describe('getSortParams', () => {
        it('should return sort params from query', async() => {
            const sortParams = { sortBy: 'firstName', sortOrder: 'ASC' };
            const ctx = { query: sortParams };
            const next = sinon.spy();

            await query(ctx, next);

            const actual = ctx.queryHelper.getSortParams(['firstName', 'lastName']);

            assert.deepEqual(actual, [sortParams]);
            assert(next.calledOnce, 'Next callback should be called once');
        });

        it('should return sort params from query with related entity', async() => {
            const sortParams = { sortBy: 'accountFirstName', sortOrder: 'ASC' };
            const ctx = { query: sortParams };
            const next = sinon.spy();

            await query(ctx, next);

            const actual = ctx.queryHelper.getSortParams({ self: [], account: ['accountFirstName'] });

            assert.deepEqual(actual, [{
                ...sortParams,
                sortBy: 'firstName',
                parent: 'account',
            }]);
            assert(next.calledOnce, 'Next callback should be called once');
        });

        it('should return sort params from query with jsonb field', async() => {
            const sortParams = { sortBy: 'answerFirstName', sortOrder: 'ASC' };
            const ctx = { query: sortParams };
            const next = sinon.spy();

            await query(ctx, next);

            const actual = ctx.queryHelper.getSortParams({ self: [], answer: { type: 'JSONB', field: 'answers' } });

            assert.deepEqual(actual, [{
                ...sortParams,
                sortBy: 'firstName',
                jsonb: 'answers',
                jsonbParent: 'answer',
            }]);
            assert(next.calledOnce, 'Next callback should be called once');
        });

        it('should return default sort params', async() => {
            const sortParams = { sortBy: 'id', sortOrder: 'DESC' };
            const ctx = { query: {} };
            const next = sinon.spy();

            await query(ctx, next);

            const actual = ctx.queryHelper.getSortParams(sortParams, Base.defaultSort);

            assert.deepEqual(actual, [sortParams]);
            assert(next.calledOnce, 'Next callback should be called once');
        });

        it('should throw error when sortBy is not expected', async() => {
            const ctx = { logger, query: { sortOrder: 'DESC', sortBy: 'invalid' } };
            const next = sinon.spy();

            await query(ctx, next);
            await catchError(ctx, ctx.queryHelper.getSortParams.bind(ctx, ['id']));

            assert(next.calledOnce, 'Next callback should be called once');
            assert.strictEqual(ctx.status, 400);
            assert.deepStrictEqual(ctx.body, {
                message: 'Parameter sortBy is not allowed in SortableFields',
                internalCode: '400_SNI',
                value: 'invalid',
            });
        });

        it('should throw error when sortOrder is not expected', async() => {
            const ctx = { logger, query: { sortOrder: 'invalid', sortBy: 'id' } };
            const next = sinon.spy();

            await query(ctx, next);
            await catchError(ctx, ctx.queryHelper.getSortParams.bind(ctx, ['id']));

            assert(next.calledOnce, 'Next callback should be called once');
            assert.strictEqual(ctx.status, 400);
            assert.deepStrictEqual(ctx.body, {
                message: 'Value is not allowed',
                internalCode: '400_VNA',
                value: 'invalid',
                expected: ['ASC', 'DESC'],
            });
        });
    });

    describe('getFilterParams', () => {
        it('should return filter params from query', async() => {
            const date = '2018-10-29T10:00:00.000Z';
            const filterParams = {
                and: [
                    { id: '1' },
                    { slug: 'test' },
                    { isPublished: 'false' },
                    {
                        or: [
                            { startDate: date },
                            { id: { neq: '2' } },
                            { startDate: { null: '' } },
                            { startDate: { notNull: '' } },
                            { startDate: { lt: date } },
                            { startDate: { gt: date } },
                            { startDate: { cont: date } },
                            { startDate: { ncont: date } },
                            { 'tags.createdAt': { gt: date } },
                        ],
                    },
                ],
            };
            const ctx = { query: { filters: filterParams } };
            const next = sinon.spy();

            await query(ctx, next);

            const actual = ctx.queryHelper.getFilterParams(Event.filterableFields, eventSchema);

            const omitted = [
                'title', 'maxLength', 'format', 'enum', 'enumNames', 'compare', 'parent', 'jsonb', 'help',
            ];
            const omittedWithCompare = omitted.filter(el => el !== 'compare');

            assert.equal(actual.and.length, 4);
            assert.deepEqual(
                _.omit(actual.and[0], omitted), { type: 'number', name: 'id', value: 1 },
            );
            assert.deepEqual(_.omit(actual.and[1], omitted), {
                type: 'string',
                name: 'slug',
                value: 'test',
                pattern: '^[/a-z0-9_-]+[^/]$',
            });
            assert.deepEqual(
                _.omit(actual.and[2], omitted),
                { type: 'boolean', name: 'isPublished', value: 'false' },
            );

            assert.equal(actual.and[3].or.length, filterParams.and[3].or.length);
            assert.deepEqual(
                _.omit(actual.and[3].or[0], omitted),
                { type: 'string', name: 'startDate', value: date },
            );
            assert.deepEqual(
                _.omit(actual.and[3].or[1], omittedWithCompare),
                { type: 'number', name: 'id', value: '2', compare: 'neq' },
            );
            assert.deepEqual(
                _.omit(actual.and[3].or[2], omittedWithCompare),
                { type: 'string', name: 'startDate', value: '', compare: 'null' },
            );
            assert.deepEqual(
                _.omit(actual.and[3].or[3], omittedWithCompare),
                { type: 'string', name: 'startDate', value: '', compare: 'notNull' },
            );
            assert.deepEqual(
                _.omit(actual.and[3].or[4], omittedWithCompare),
                { type: 'string', name: 'startDate', value: date, compare: 'lt' },
            );
            assert.deepEqual(
                _.omit(actual.and[3].or[5], omittedWithCompare),
                { type: 'string', name: 'startDate', value: date, compare: 'gt' },
            );
            assert.deepEqual(
                _.omit(actual.and[3].or[6], omittedWithCompare),
                { type: 'string', name: 'startDate', value: date, compare: 'cont' },
            );
            assert.deepEqual(
                _.omit(actual.and[3].or[7], omittedWithCompare),
                { type: 'string', name: 'startDate', value: date, compare: 'ncont' },
            );
            assert.deepEqual(
                _.omit(actual.and[3].or[8], omittedWithCompare),
                { type: 'string', name: 'createdAt', value: date, compare: 'gt' },
            );

            assert(next.calledOnce, 'Next callback should be called once');
        });

        it('should return filter params from body.filters', async() => {
            const filterParams = {
                or: [
                    { id: '1' },
                    { slug: 'test' },
                    { isPublished: 'false' },
                ],
            };
            const ctx = { request: { body: { filters: filterParams } } };
            const next = sinon.spy();

            await query(ctx, next);

            const actual = ctx.queryHelper.getFilterParams(
                Event.filterableFields, eventSchema, 'request.body.filters',
            );

            const omitted = [
                'title', 'maxLength', 'format', 'enum', 'enumNames', 'compare', 'parent', 'jsonb', 'help',
            ];

            assert.equal(actual.or.length, 3);
            assert.deepEqual(
                _.omit(actual.or[0], omitted), { type: 'number', name: 'id', value: '1' },
            );
            assert.deepEqual(_.omit(actual.or[1], omitted), {
                type: 'string',
                name: 'slug',
                value: 'test',
                pattern: '^[/a-z0-9_-]+[^/]$',
            });
            assert.deepEqual(
                _.omit(actual.or[2], omitted),
                { type: 'boolean', name: 'isPublished', value: 'false' },
            );

            assert(next.calledOnce, 'Next callback should be called once');
        });

        it('should return filter params from object', async() => {
            const filterParams = {
                or: [
                    { id: '1' },
                    { slug: 'test' },
                ],
            };
            const ctx = { request: {} };
            const next = sinon.spy();

            await query(ctx, next);

            const actual = ctx.queryHelper.getFilterParams(
                Event.filterableFields, eventSchema, filterParams,
            );

            assert.equal(actual.or.length, 2);
            assert.equal(actual.or[0].type, 'number');
            assert.equal(actual.or[0].name, 'id');
            assert.equal(actual.or[0].value, '1');
            assert.equal(actual.or[1].type, 'string');
            assert.equal(actual.or[1].name, 'slug');
            assert.equal(actual.or[1].value, 'test');

            assert(next.calledOnce, 'Next callback should be called once');
        });

        it('should throw error when filter number has invalid value', async() => {
            const filterParams = {
                and: [{ id: 'invalid' }],
            };
            const ctx = { logger, query: { filters: filterParams } };
            const next = sinon.spy();

            await query(ctx, next);
            await catchError(
                ctx, ctx.queryHelper.getFilterParams.bind(ctx, Event.filterableFields, eventSchema),
            );

            assert.strictEqual(ctx.status, 400);
            assert.deepStrictEqual(ctx.body, {
                message: 'Float is invalid',
                internalCode: '400_FVI',
                value: 'invalid',
            });
        });

        it('should throw error when filter boolean has invalid value', async() => {
            const filterParams = {
                and: [{ isPublished: 'invalid' }],
            };
            const ctx = { logger, query: { filters: filterParams } };
            const next = sinon.spy();

            await query(ctx, next);
            await catchError(
                ctx, ctx.queryHelper.getFilterParams.bind(ctx, Event.filterableFields, eventSchema),
            );

            assert.strictEqual(ctx.status, 400);
            assert.deepStrictEqual(ctx.body, {
                internalCode: '400_CSF',
                message: 'Check by schema failed',
            });
        });

        it('should throw error when filter date has invalid value', async() => {
            const filterParams = {
                and: [{ startDate: 'invalid' }],
            };
            const ctx = { logger, query: { filters: filterParams } };
            const next = sinon.spy();

            await query(ctx, next);
            await catchError(
                ctx, ctx.queryHelper.getFilterParams.bind(ctx, Event.filterableFields, eventSchema),
            );

            assert.strictEqual(ctx.status, 400);
            assert.equal(ctx.body.internalCode, '400_CSF');
            assert.equal(ctx.body.message, 'Check by schema failed');
        });

        it('should throw error when json is invalid', async() => {
            const ctx = { logger, query: { filters: 'inv@lid' } };
            const next = sinon.spy();

            await query(ctx, next);
            await catchError(
                ctx,
                ctx.queryHelper.getFilterParams.bind(ctx, Event.filterableFields, eventSchema),
            );

            assert.strictEqual(ctx.status, 400);
            assert.equal(ctx.body.internalCode, '400_IJG');
            assert.equal(ctx.body.message, 'Invalid json in getFilterParams');
        });

        it('should throw error when operator not contain array', async() => {
            const ctx = { logger, query: { filters: { and: { id: 1 } } } };
            const next = sinon.spy();

            await query(ctx, next);
            await catchError(
                ctx,
                ctx.queryHelper.getFilterParams.bind(ctx, Event.filterableFields, eventSchema),
            );

            assert.strictEqual(ctx.status, 400);
            assert.equal(ctx.body.internalCode, '400_OCA');
            assert.equal(ctx.body.message, 'Operator must contain an array');
        });
    });
});
