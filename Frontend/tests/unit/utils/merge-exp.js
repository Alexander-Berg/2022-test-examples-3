const { merge } = require('../../../src/server/utils/merge-exp');

describe('utils/mergeExperimentsUtils', () => {
    it('Должен корректно мержить настройки двух экспериментов', () => {
        const expected = {
            title: 't1',
            desc: 'd1',
            parentId: 123,
            params: {
                owners: ['splav', 'sbmaxx'],
                abcService: 123,
                assessmentGroup: 'none',
                systems: [
                    {
                        name: 'sys-0',
                        id: 'sys-0',
                        features: [
                            'remove_yandex_ads',
                            'ya_video_stab',
                            'csp_disable',
                        ],
                    },
                ],
            },
        };

        assert.deepEqual(merge({
            title: 't0',
            desc: 'd0',
            parentId: 123,
            params: {
                owners: ['splav', 'eroshinev', 'sbmaxx'],
                abcService: 123,
                assessmentGroup: 'none',
                systems: [
                    {
                        name: 'sys-0',
                        id: 'sys-0',
                        features: [
                            'remove_yandex_ads',
                            'ya_video_stab',
                            'csp_disable',
                            'ya_collections_no_ajax',
                        ],
                    },
                ],
            },
        }, {
            title: 't1',
            desc: 'd1',
            parentId: undefined,
            params: {
                owners: ['splav', 'sbmaxx'],
                assessmentGroup: undefined,
                systems: [
                    {
                        name: 'sys-0',
                        id: 'sys-0',
                        features: [
                            'remove_yandex_ads',
                            'ya_video_stab',
                            'csp_disable',
                        ],
                    },
                ],
            },
        }), expected);
    });
});
