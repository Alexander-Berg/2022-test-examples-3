const assert = require('assert');
const db = require('db');

const sequalizeBuilder = require('lib/sequalizeBuilder');

const { Op, where, literal } = db.sequelize;

describe('sequalizeBuilder library', () => {
    describe('getOrder', () => {
        it('should get sequalize object', () => {
            const data = [
                {
                    sortBy: 'startDate',
                    sortOrder: 'DESC',
                },
                {
                    sortBy: 'id',
                    sortOrder: 'DESC',
                },
                {
                    parent: 'account',
                    sortBy: 'email',
                    sortOrder: 'DESC',
                },
                {
                    jsonb: 'answer',
                    sortBy: 'email',
                    sortOrder: 'DESC',
                },
            ];

            const actual = sequalizeBuilder.getOrder(data);
            const expected = [
                ['startDate', 'DESC'],
                ['id', 'DESC'],
                ['account', 'email', 'DESC'],
                ['answer.email.value', 'DESC'],
            ];

            assert.deepStrictEqual(actual, expected);
        });
    });

    describe('getFilter', () => {
        it('should get sequalize where object', () => {
            const dateDef = new Date('2018-11-28T10:00:00.000Z');
            const date1 = new Date('2018-11-28T00:00:00.000Z');
            const date2 = new Date('2018-11-29T00:00:00.000Z');
            const data = {
                and: [
                    {
                        type: 'string', name: 'slug', value: '3',
                    },
                    {
                        type: 'string', name: 'slug', value: '3', compare: 'cont',
                    },
                    {
                        type: 'string', name: 'slug', value: ['1', '2'], compare: 'cont',
                    },
                    {
                        type: 'string',
                        format: 'date-time',
                        name: 'startDate',
                        value: '2018-11-28T10:00:00.000Z',
                    },
                    {
                        type: 'string',
                        format: 'date-time',
                        name: 'startDate',
                        value: '2018-11-28T10:00:00.000Z',
                        compare: 'cont',
                    },
                    {
                        type: 'string',
                        format: 'date-time',
                        name: 'startDate',
                        value: '2018-11-28T10:00:00.000Z',
                        compare: 'ncont',
                    },
                    {
                        type: 'boolean', name: 'isPublished', value: 'true',
                    },
                    {
                        or: [
                            {
                                type: 'number', name: 'id', value: '1',
                            },
                            {
                                type: 'string',
                                format: 'date-time',
                                name: 'startDate',
                                value: '2018-11-28T00:00:00.000Z',
                                compare: 'lt',
                            },
                            {
                                type: 'string', name: 'slug', value: '', compare: 'null',
                            },
                            {
                                type: 'number', name: 'id', value: '44', compare: 'gt',
                            },
                        ],
                    },
                ],
            };

            const actual = sequalizeBuilder.getFilter(data);
            const expected = {
                [Op.and]: [
                    { slug: { [Op.eq]: '3' } },
                    { slug: { [Op.iLike]: '%3%' } },
                    { slug: { [Op.in]: literal('(\'1\',\'2\')') } },
                    { startDate: { [Op.eq]: dateDef } },
                    { startDate: { [Op.gt]: date1, [Op.lt]: date2 } },
                    { startDate: { [Op.or]: [{ [Op.lt]: date1 }, { [Op.gt]: date2 }] } },
                    { isPublished: true },
                    { [Op.or]: [
                        { id: 1 },
                        { startDate: { [Op.lt]: date1 } },
                        { slug: { [Op.eq]: null } },
                        { id: { [Op.gt]: 44 } },
                    ] },
                ],
            };

            assert.deepStrictEqual(actual, expected);
        });

        it('should get sequalize where object with extra fields', () => {
            const data = {
                or: [
                    {
                        type: 'string', name: 'slug', value: '3',
                    },
                    {
                        type: 'string', name: 'slug', value: '3', parent: 'entity',
                    },
                ],
            };

            const actual = sequalizeBuilder.getFilter(data);
            const expected = {
                [Op.or]: [
                    { slug: { [Op.eq]: '3' } },
                    where(literal('entity.slug '), Op.eq, '3'),
                ],
            };

            assert.deepStrictEqual(actual, expected);
        });
    });

    describe('mergeWhere', () => {
        it('should merge sequalize where objects', () => {
            const actual = sequalizeBuilder.mergeWhere(
                {
                    [Op.or]: [
                        {
                            slug: {
                                [Op.iLike]: '%3%',
                            },
                        },
                    ],
                },
                {
                    [Op.or]: [
                        {
                            id: 1,
                        },
                    ],
                },
            );
            const expected = {
                [Op.and]: [
                    {
                        [Op.or]: [
                            {
                                slug: {
                                    [Op.iLike]: '%3%',
                                },
                            },
                        ],
                    },
                    {
                        [Op.or]: [
                            {
                                id: 1,
                            },
                        ],
                    },
                ],
            };

            assert.deepStrictEqual(actual, expected);
        });
    });
});
