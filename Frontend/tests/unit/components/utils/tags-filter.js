const { convertQPsToFilter, convertResultsFilterToQPs } = require('../../../../src/client/components/utils/tags-filter');

describe('components/utils/tags-filter', () => {
    beforeEach(() => {
        global.atob = (str) => Buffer.from(str, 'base64').toString('latin1');
        global.btoa = (str) => Buffer.from(str).toString('base64');
    });

    afterEach(() => {
        delete global.atob;
        delete global.btoa;
    });

    const defaultTagsFilter = {
        filters: [
            {
                id: 0,
                system: '0',
                tags: [
                    {
                        presence: 'yes',
                        tag: 'ad_premium',
                    },
                    {
                        presence: 'no',
                        tag: 'misspell',
                    },
                    {
                        presence: 'no',
                        tag: 'sport',
                    },
                ],
            },
            {
                id: 1,
                system: '2',
                tags: [
                    {
                        presence: 'yes',
                        tag: 'znatoki',
                    },
                    {
                        presence: 'no',
                        tag: 'video',
                    },
                ],
            },
        ],
        operator: 'and',
    };
    const defaultSystems = ['0', '2'];
    const invalidBase64QP = 'fadjskl3214DLKSvadООттлафвышмйуцnlkJjk';
    const validBase64QP = 'eyJleHQtb3AiOiJhbmQiLCJmaWx0ZXJzIjpbeyJzeXN0ZW0iOiIwIiwibm8tdGFncyI6WyJtaXNzcGVsbCIsInNwb3J0Il0sImhhcy10YWdzIjpbImFkX3ByZW1pdW0iXX0seyJzeXN0ZW0iOiIyIiwibm8tdGFncyI6WyJ2aWRlbyJdLCJoYXMtdGFncyI6WyJ6bmF0b2tpIl19XX0=';

    describe('convertQPsToFilter:', () => {
        it('должен вернуть пустой объект в случае невалидного base64', () => {
            const result = convertQPsToFilter(invalidBase64QP);
            assert.deepEqual(result, {});
        });

        it('должен вернуть ожидаемый объект в случае валидного base64', () => {
            const result = convertQPsToFilter(validBase64QP);
            assert.deepEqual(result, defaultTagsFilter);
        });
    });

    describe('convertResultsFilterToQPs:', () => {
        it('должен вернуть валидный base64 и массив систем', () => {
            const result = convertResultsFilterToQPs({ ...defaultTagsFilter, systems: defaultSystems });
            assert.deepEqual(result, {
                systems: ['0', '2'],
                tagsFiltering: validBase64QP,
            });
        });

        it('должен вернуть массив систем в случае пустого оператора', () => {
            const result = convertResultsFilterToQPs({ operator: null, systems: defaultSystems });
            assert.deepEqual(result, {
                systems: ['0', '2'],
                tagsFiltering: null,
            });
        });
    });
});
