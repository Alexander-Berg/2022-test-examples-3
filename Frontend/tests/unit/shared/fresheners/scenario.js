const _ = require('lodash');
const { fillSystemsAndPagesId } = require('../../../../src/shared/fresheners/scenario');

const EXP = {
    variants: {
        items: [
            { name: 'System 1' },
            { name: 'System 2' },
        ],
    },
};

const EXP_BATCH = {
    batchMode: true,
    variants: {
        'type': 'testId',
        'systems': [
            {
                'name': 'Система 0',
            },
            {
                'name': 'Система 1',
            },
        ],
        'pages': [
            {
                'scenario': 'Вопрос к первому экрану',
                'name': 'Screen 0',
            },
            {
                'scenario': 'Вопрос ко второму экрану',
                'name': 'Screen 1',
            },
        ],
        'items': [
            {
                'urls': [
                    {
                        'title': 'БЯК',
                        'url': 'https://yandex.ru/search/touch/?text=%D0%BF%D1%8F%D1%82%D0%BD%D0%B8%D1%86%D0%BA%D0%B0%D1%8F%2039&clid=2270455&win=349&lr=213',
                        'testId': '168252',
                    },
                    {
                        'title': 'БЯК',
                        'url': 'https://yandex.ru/search/touch/?text=%D0%BF%D1%8F%D1%82%D0%BD%D0%B8%D1%86%D0%BA%D0%B0%D1%8F%2039&clid=2270455&win=349&lr=213',
                        'testId': '168252',
                    },
                ],
            },
            {
                'urls': [
                    {
                        'title': 'БЯК',
                        'url': 'https://yandex.ru/search/touch/?text=%D0%BF%D1%8F%D1%82%D0%BD%D0%B8%D1%86%D0%BA%D0%B0%D1%8F%2039&clid=2270455&win=349&lr=213',
                        'testId': '168253',
                    },
                    {
                        'title': 'БЯК',
                        'url': 'https://yandex.ru/search/touch/?text=%D0%BF%D1%8F%D1%82%D0%BD%D0%B8%D1%86%D0%BA%D0%B0%D1%8F%2039&clid=2270455&win=349&lr=213',
                        'testId': '168253',
                    },
                ],
            },
        ],
    },
};

describe('scenario fillSystemsAndPagesIdss', function() {
    describe('fillSystemsAndPagesId', function() {
        let experiment;
        describe('батчевый режим', function() {
            beforeEach(function() {
                experiment = _.cloneDeep(EXP_BATCH);
            });

            it('должен проставлять systemId каждой системе', function() {
                const { exp } = fillSystemsAndPagesId(experiment);

                assert.isTrue(exp.variants.systems.every((system, i) => system.systemId === `sys-${i}`));
            });

            it('должен проставлять pageId каждой странице', function() {
                const { exp } = fillSystemsAndPagesId(experiment);

                assert.isTrue(exp.variants.pages.every((page, i) => page.pageId === `page-${i}`));
            });

            it('должен проставлять pageId каждому item', function() {
                const { exp } = fillSystemsAndPagesId(experiment);

                assert.isTrue(exp.variants.items.every((item, i) => item.pageId === `page-${i}`));
            });

            it('должен проставлять systemId каждому объекту в items.urls', function() {
                const { exp } = fillSystemsAndPagesId(experiment);

                assert.isTrue(exp.variants.items.every((item) => item.urls.every((url, i) => url.systemId === `sys-${i}`)));
            });
        });

        it('должен проставлять systemId каждой системе в variants.items в небатчевом режиме', function() {
            const experiment = _.cloneDeep(EXP);
            const { exp } = fillSystemsAndPagesId(experiment);

            assert.isTrue(exp.variants.items.every((item, i) => item.systemId === `sys-${i}`));
        });
    });
});
