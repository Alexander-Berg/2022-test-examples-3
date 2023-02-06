const targetings = require('../../../../src/server/data-adapters/nirvana/targetings');
const targetingConfig = require('../../../../src/shared/targetings-constants/constants').default;


describe('nirvana/targetings', function() {
    it('должен корректно обрабатывать ограничения по profile и skill-id', function() {
        const currentDate = new Date();
        const params = {
            targetings: {
                education: ['HIGH'],
                '8837': [2],
                country: ['RU','KZ'],
                '9585': [2, 3],
                prism: 'visual',
            },
        };
        const expect = [
            {
                'filter-key': '8837',
                'filter-kind': 'skill',
                constraint: {
                    'constraint-type': 'in-list',
                    values: params.targetings['8837'],
                },
            },
            {
                'filter-key': '9585',
                'filter-kind': 'skill',
                constraint: {
                    'constraint-type': 'in-list',
                    values: params.targetings['9585'],
                },
            },
            {
                'filter-key': 'education',
                'filter-kind': 'toloka-filter-key',
                constraint: {
                    'constraint-type': 'in-list',
                    values: params.targetings.education,
                },
            },
            {
                'filter-key': 'country',
                'filter-kind': 'toloka-filter-key',
                constraint: {
                    'constraint-type': 'in-list',
                    values: params.targetings.country,
                },
            },
            {
                'filter-key': 'prism',
                'filter-kind': 'sbs',
                constraint: {
                    'constraint-type': 'equal',
                    value: params.targetings.prism,
                },
            },
        ];

        assert.deepEqual(targetings.getSection(params, targetingConfig, currentDate), expect);
    });
});
