const querystring = require('querystring');
const _ = require('lodash');
const proxyquire = require('proxyquire');

const { HoneypotsMode } = require('../../../../../src/types/honeypots-mode');
const { Engine } = require('../../../../../src/types/search-engine');

const commonCases = require('../common-cases');

const {
    input,
    output: expect,
    pluginsHost,
    profilesFixtures,
    poolsListFixtures,
    feauturesFixtures,
    regular,
    ytWorkspace,
    defaultCalcWorkflowId,
    defaultUiVersion,
    currentUnixTimestampStub,
    targetingConfig,
    systemFeatures,
} = require('./fixtures');

const adapter = proxyquire.load('../../../../../src/server/data-adapters/nirvana/search', {
    '../utils': {
        getCurrentDate() {
            return new Date(currentUnixTimestampStub);
        },
    },
});

describe('nirvana/search', () => {
    let result, adapterIn;

    beforeEach(() => {
        adapterIn = {
            config: input,
            host: 'sbs.yandex-team.ru',
            ticket: 'SIDEBYSIDE-100500',
            pluginsHost,
            parseQuery: (str) => querystring.parse(str),
            profiles: profilesFixtures,
            poolsList: poolsListFixtures,
            features: feauturesFixtures,
            ytWorkspace,
            regular,
            author: 'eroshinev',
            owners: ['eroshinev'],
            calcWorkflowId: defaultCalcWorkflowId,
            uiVersion: defaultUiVersion,
            targetingConfig,
            systemFeatures,
        };

        result = adapter(adapterIn);
    });

    describe('автоматические hitman-ханипоты', () => {
        it('должны включаться при определенных условиях (desktop)', () => {
            const i = _.cloneDeep(adapterIn);

            i.config.honeypotsMode = HoneypotsMode.AUTO;
            i.config.options = [
                {
                    'title': 'System0',
                    'query': 'котики',
                    'host': 'https://hamster.yandex.ru',
                    'cgi': 'test-id=1\nwaitall=da',
                    'cgi-settings': [
                        {
                            'key': 'remove_yandex_ads',
                            'title': 'Скрыть рекламу',
                            'type': 'fixed',
                            'cgi': {
                                'exp_flags': 'direct_raw_parameters=aoff=1',
                            },
                            'isCustom': false,
                            'checked': false,
                        },
                    ],
                    'engine': 'yandex-web',
                    'w-exps-flags': '',
                    'region': 'ru',
                    'flags': '&test-id=1&waitall=da24562456\nafd\nafdgadfh',
                },
                {
                    'title': 'System1',
                    'query': 'котики',
                    'cgi': 'sbs_plugin=google_ru_utf8&sbs_plugin=yandex_style',
                    'cgi-settings': [
                        {
                            'key': 'hide_google_ads',
                            'title': 'Скрыть рекламу',
                            'type': 'fixed',
                            'cgi': {
                                'sbs_plugin': 'hide_google_ads',
                            },
                            'isCustom': false,
                            'checked': true,
                        },
                    ],
                    'engine': 'google-web',
                    'w-exps-flags': '',
                },
            ];
            i.config.goldenset = [];
            i.config.features = { 'no_brand': 'on' };
            i.config.device = 'desktop';

            assert.deepEqual(adapter(i).honeypots, [
                {
                    'is-honeypot': true,
                    'is-external': true,
                    'sys-id': 'hitman-honeypot',
                    'sys-name': 'Hitman-Honeypot',
                    'sys-type': 'unknown',
                },
            ]);
            assert.deepEqual(adapter(i)['dual-honeypots'], []);
            assert.deepEqual(adapter(i)['hitman-db'], {
                'use-hitman-honeypots': true,
                'honeypots-hit-type': 'sbs_honeypots_search_with_zeros_desktop',
                'good-sys-id': 'hitman-honeypot',
            });
        });

        it('должны включаться при определенных условиях (touch)', () => {
            const i = _.cloneDeep(adapterIn);

            i.config.honeypotsMode = HoneypotsMode.AUTO;
            i.config.options = [
                {
                    'title': 'System0',
                    'query': 'котики',
                    'host': 'https://hamster.yandex.ru',
                    'cgi': 'test-id=1\nwaitall=da',
                    'cgi-settings': [
                        {
                            'key': 'remove_yandex_ads',
                            'title': 'Скрыть рекламу',
                            'type': 'fixed',
                            'cgi': {
                                'exp_flags': 'direct_raw_parameters=aoff=1',
                            },
                            'isCustom': false,
                            'checked': false,
                        },
                    ],
                    'engine': 'yandex-web',
                    'w-exps-flags': '',
                    'region': 'ru',
                    'flags': '&test-id=1&waitall=da24562456\nafd\nafdgadfh',
                },
                {
                    'title': 'System1',
                    'query': 'котики',
                    'cgi': 'sbs_plugin=google_ru_utf8&sbs_plugin=yandex_style',
                    'cgi-settings': [
                        {
                            'key': 'hide_google_ads',
                            'title': 'Скрыть рекламу',
                            'type': 'fixed',
                            'cgi': {
                                'sbs_plugin': 'hide_google_ads',
                            },
                            'isCustom': false,
                            'checked': true,
                        },
                    ],
                    'engine': 'google-web',
                    'w-exps-flags': '',
                },
            ];
            i.config.goldenset = [];
            i.config.features = { 'no_brand': 'on' };
            i.config.device = 'touch';

            assert.deepEqual(adapter(i).honeypots, [
                {
                    'is-honeypot': true,
                    'is-external': true,
                    'sys-id': 'hitman-honeypot',
                    'sys-name': 'Hitman-Honeypot',
                    'sys-type': 'unknown',
                },
            ]);
            assert.deepEqual(adapter(i)['dual-honeypots'], []);
            assert.deepEqual(adapter(i)['hitman-db'], {
                'use-hitman-honeypots': true,
                'honeypots-hit-type': 'sbs_honeypots_search_with_zeros_touch',
                'good-sys-id': 'hitman-honeypot',
            });
        });

        it('по-умолчанию должны быть выключены', () => {
            const i = _.cloneDeep(adapterIn);
            assert.deepEqual(adapter(i)['hitman-db'], {});
        });
    });

    describe('параметры таргетирования', () => {
        it('корректны в случае, если все контролы и варианты присутствуют', () => {
            const i = _.cloneDeep(adapterIn);
            assert.deepEqual(adapter(i).toloka.targetings, expect.toloka.targetings);
        });

        it('корректны в случае, если контрола в конфиге нет', () => {
            const i = _.cloneDeep(adapterIn);
            const removedControlKey = 'testRange';
            const expectedResult = expect.toloka.targetings.filter((f) => f['filter-key'] !== removedControlKey);

            i.targetingConfig.fields = i.targetingConfig.fields.filter((f) => f.key !== removedControlKey);

            assert.deepEqual(adapter(i).toloka.targetings, expectedResult);
        });

        it('корректны в случае, если у контрола в конфиге нет варианта', () => {
            const i = _.cloneDeep(adapterIn);
            const targetingsResult = _.cloneDeep(expect.toloka.targetings);

            const controlKey = 'testMultiselect';
            const removedOptionVal = 'SV';

            const expectedResult = targetingsResult.map((t) => {
                if (t['filter-key'] === controlKey) {
                    t.constraint.values = t.constraint.values.filter((o) => o !== removedOptionVal);
                }
                return t;
            });

            i.targetingConfig.fields = i.targetingConfig.fields.map((f) => {
                if (f.key === controlKey) {
                    f.options.items = f.options.items.filter((o) => o.value !== removedOptionVal);
                }
                return f;
            });

            assert.deepEqual(adapter(i).toloka.targetings, expectedResult);
        });

        it('корректны в случае, если у контрола в конфиге вообще нет подходящих вариантов', () => {
            const i = _.cloneDeep(adapterIn);
            const targetingsResult = _.cloneDeep(expect.toloka.targetings);

            const controlKey = 'testSelect';
            const removedOptionVal = 'B';

            const expectedResult = targetingsResult.map((t) => {
                if (t['filter-key'] === controlKey) {
                    t.constraint.values = t.constraint.values.filter((o) => o !== removedOptionVal);
                }
                return t;
            }).filter((t) => !t.constraint.values || t.constraint.values.length > 0);

            i.targetingConfig.fields = i.targetingConfig.fields.map((f) => {
                if (f.key === controlKey) {
                    f.options.items = f.options.items.filter((o) => o.value !== removedOptionVal);
                }
                return f;
            });

            assert.deepEqual(adapter(i).toloka.targetings, expectedResult);
        });

        it('корректны в случае, если у контролов пустые значения', () => {
            const i = _.cloneDeep(adapterIn);

            const removedControlKeys = ['testMultiselect', 'testSelect'];
            const expectedResult = expect.toloka.targetings.filter((f) => removedControlKeys.indexOf(f['filter-key']) === -1);

            i.config.targetings['testMultiselect'] = [];
            i.config.targetings['testSelect'] = '';

            assert.deepEqual(adapter(i).toloka.targetings, expectedResult);
        });

        it('корректны в случае, если у контрола типа range выбраны крайние значения', () => {
            const i = _.cloneDeep(adapterIn);
            const targetingsResult = _.cloneDeep(expect.toloka.targetings);

            const controlKey = 'testRange';
            const expectedResult = targetingsResult.filter((t) => t['filter-key'] !== controlKey);
            i.config.targetings[controlKey] = { min: 0, max: 100 };

            assert.deepEqual(adapter(i).toloka.targetings, expectedResult);
        });

        it('корректны в случае, если у контрола типа range выбрано одно крайнее значение', () => {
            const i = _.cloneDeep(adapterIn);
            const targetingsResult = _.cloneDeep(expect.toloka.targetings);

            const controlKey = 'testRange';
            const expectedResult = targetingsResult.map((t) => {
                if (t['filter-key'] === controlKey) {
                    delete t.constraint['min-value'];
                }
                return t;
            });

            i.config.targetings[controlKey] = Object.assign(i.config.targetings[controlKey], { min: 0 });

            assert.deepEqual(adapter(i).toloka.targetings, expectedResult);
        });

        it('в сконвертрованном конфиге минимальное значение меньше максимального для контрола типа ageRange ', () => {
            const i = _.cloneDeep(adapterIn);

            const controlAltKey = 'age';

            const result = adapter(i).toloka.targetings.find((t) => t['filter-key'] === controlAltKey);
            assert.isAbove(result.constraint['max-value'], result.constraint['min-value']);
        });

        it('корректны если задан таргетинг по результату кастомного опроса', () => {
            const i = _.cloneDeep(adapterIn);
            assert.deepEqual(adapter(i).toloka['poll-targeting-config'], expect.toloka['poll-targeting-config']);
        });

        it('параметр таргетинга должен отсутствовать в конфиге, если в заявке никогда не задавался таргетинг по результату кастомного опроса', () => {
            const i = _.cloneDeep(adapterIn);
            i.config.targetings['history_poll'] = undefined;
            assert.isTrue(typeof adapter(i).toloka['poll-targeting-config'] === 'undefined');
        });

        it('параметр таргетинга должен отсутствовать в конфиге, если в заявке не был выбран ни один вопрос', () => {
            const i = _.cloneDeep(adapterIn);
            i.config.targetings['history_poll'] = {
                boolOperator: 'and',
                items: [],
            };
            assert.isTrue(typeof adapter(i).toloka['poll-targeting-config'] === 'undefined');
        });
    });

    describe('значение параметра overlap (количество опрашиваемых)', () => {
        it('указанное в заявке соответствует значению в конфиге', () => {
            const i = _.cloneDeep(adapterIn);
            assert.deepEqual(adapter(i).toloka.overlap, expect.toloka.overlap);
        });

        it('должно отсутствовать в конфиге, если в заявке не было указано его значение (overlap.mode: "default")', () => {
            const i = _.cloneDeep(adapterIn);
            i.config['overlap'] = {
                'mode': 'default',
                'value': '',
            };
            assert.isTrue(typeof adapter(i).toloka['overlap'] === 'undefined' );
        });

        it('должно отсутствовать в конфиге, если такой параметр не существует (undefined)', () => {
            const i = _.cloneDeep(adapterIn);
            delete i.config['overlap'];
            assert.isTrue(typeof adapter(i).toloka['overlap'] === 'undefined' );
        });
    });

    describe('опция мерджера выставляется корректно', () => {
        it('"auto-start": true', () => {
            const i = _.cloneDeep(adapterIn);
            i.config.useMerger = 'yes';
            assert.isTrue(adapter(i).main['merge-with-others']);
        });

        it('"auto-start": false', () => {
            const i = _.cloneDeep(adapterIn);
            i.config.useMerger = 'no';
            assert.isFalse(adapter(i).main['merge-with-others']);
        });
    });

    describe('опция фильтрации одинаковых скриншотов выставляется корректно', () => {
        it('если выбрано значение "yes", то в конфиг будет добавлено значение "filter-all"', () => {
            const i = _.cloneDeep(adapterIn);
            i.config.checkEqualScreenshots = 'yes';
            assert.isTrue(adapter(i).quality['screen-filter-mode'] === 'filter-all');
        });

        it('если выбрано значение "no", то в конфиг будет добавлено значение "disabled"', () => {
            const i = _.cloneDeep(adapterIn);
            i.config.checkEqualScreenshots = 'no';
            assert.isTrue(adapter(i).quality['screen-filter-mode'] === 'disabled');
        });

        it('если выбрано значение "default", то в конфиг будет добавлено значение "filter-with-threshold"', () => {
            const i = _.cloneDeep(adapterIn);
            i.config.checkEqualScreenshots = 'default';
            assert.isTrue(adapter(i).quality['screen-filter-mode'] === 'filter-with-threshold');
        });
    });

    describe('суффикс -iphone должен добавляться к значению sys-type только для тачей', () => {
        it('суффикс не добавляется для десктопов', () => {
            const i = _.cloneDeep(adapterIn);
            i.config.iphone = 'yes';
            i.config.device = 'desktop';
            assert.notInclude(adapter(i).systems[0]['sys-type'], '-iphone');
        });

        it('суффикс не добавляется для планшетов', () => {
            const i = _.cloneDeep(adapterIn);
            i.config.iphone = 'yes';
            i.config.device = 'pad';
            assert.notInclude(adapter(i).systems[0]['sys-type'], '-iphone');
        });

        it('суффикс добавляется для тачей', () => {
            const i = _.cloneDeep(adapterIn);
            i.config.iphone = 'yes';
            i.config.device = 'touch';
            assert.include(adapter(i).systems[0]['sys-type'], '-iphone');
        });
    });

    describe('Должны правильно формироваться профиль и параметры Толоки:', () => {
        const cases = commonCases.profileAndTolokaParams;

        cases.forEach(({ description, device, iphone, poolTitle, profile, tolokaParams, tolokaCloneParams }) => {
            it(description, () => {
                const i = _.cloneDeep(adapterIn);

                i.config.device = device;
                i.config.iphone = iphone;
                i.config.poolTitle = poolTitle;

                const nirvanaConfig = adapter(i);

                assert.equal(nirvanaConfig.where['screen-profile-name'], profile);
                assert.deepEqual(nirvanaConfig.where['custom-toloka-view-params'], tolokaParams);
                assert.deepEqual(nirvanaConfig['pool-clone-info'], tolokaCloneParams);
            });
        });
    });

    describe('фичи должны корректно работать', () => {
        it('включенная фича должна подставляться в дефолтные флаги', () => {
            assert.ok(Object.keys(result.exp['default-exp-flags']['cgi-params']).includes('enabled_feature'));
        });

        it('выключенная фича не должна подставляться в дефолтные флаги', () => {
            assert.ok(!Object.keys(result.exp['default-exp-flags']['cgi-params']).includes('disabled_feature'));
        });
    });

    describe('main section should contain correct', () => {
        [
            'fast-design-beta',
            'sbs-name',
            'st-ticket',
            'ui-host',
            'results-per-page',
            'is-scheduled-sbs',
            'yt-workspace',
            'calc-workflow-id',
            'creation-type',
        ].forEach((field) => {
            it(field, () => {
                assert.strictEqual(result.main[field], expect.main[field]);
            });
        });

        it('api creation-type', () => {
            const result = adapter({ ...adapterIn, uiVersion: undefined });
            assert.strictEqual(result.main['creation-type'], 'api');
            assert.strictEqual(result.main['ui-version'], undefined);
        });

        describe('do-skip-assessment', () => {
            it('при отсутствии значения', () => {
                const result = adapter({ ...adapterIn, config: input });
                assert.strictEqual(result.main['do-skip-assessment'], false);
            });


            it('при значении assessmentGroup === "none" устанавливается в true', () => {
                const result = adapter({ ...adapterIn, config: { ...input, assessmentGroup: 'none' } });
                assert.strictEqual(result.main['do-skip-assessment'], true);
            });

            it('при значении assessmentGroup !== "none" устанавливается в false', () => {
                const result = adapter({ ...adapterIn, config: { ...input, assessmentGroup: 'tolokers' } });
                assert.strictEqual(result.main['do-skip-assessment'], false);
            });
        });

        it('owners', () => {
            assert.deepEqual(result.main.owners, expect.main.owners);
        });

        describe('notifications', () => {
            it('должен формировать правильное значение параметра режима работы уведомлений', () => {
                const notifications = ['one', 'two'];
                const result = adapter({ ...adapterIn, notifications });
                assert.equal(result.main.notifications, notifications);
            });
        });

        describe('abc-service', () => {
            it('при отсутствии значения', () => {
                const result = adapter({ ...adapterIn, config: input });
                assert.strictEqual(result.main['abc-service'], null);
            });

            it('при наличии числового значения', () => {
                const result = adapter({ ...adapterIn, config: { ...input, abcService: 30713 } });
                assert.strictEqual(result.main['abc-service'], 30713);
            });

            it('при наличии строкового значения', () => {
                const result = adapter({ ...adapterIn, config: { ...input, abcService: 'https://abc.yandex-team.ru/services/mayak' } });
                assert.strictEqual(result.main['abc-service'], 'https://abc.yandex-team.ru/services/mayak');
            });
        });
    });

    describe('where section should contain correct', () => {
        ['device-type', 'domain'].forEach((field) => {
            it(field, () => {
                assert.equal(result.where[field], expect.where[field]);
            });
        });
    });

    describe('crosses section should contain correct', () => {
        it('default cross', () => {
            assert.strictEqual(result.crosses['default-cross-value'], expect.crosses['default-cross-value']);
        });
        it('custom crosses', () => {
            assert.deepEqual(result.crosses['custom-crosses'], expect.crosses['custom-crosses']);
        });
    });

    describe('exp section should contain correct', () => {
        [
            'default-exp-flags',
            'honeypot-tasks',
            'normal-tasks',
            'filter-reserve-factor',
        ].forEach((field) => {
            it(field, () => {
                assert.deepEqual(result.exp[field], expect.exp[field]);
            });
        });

        it('if filter reserve factor is undefined', () => {
            const i = { ...adapterIn };

            i.config.filterReserveFactor = void 0;

            assert.strictEqual(adapter(i).exp['filter-reserve-factor'], 1);
        });

        describe('do-shuffle-queries', () => {
            it('при отсутствии значения и незаполненном cross', () => {
                const result = adapter({ ...adapterIn, config: { ...input, cross: '', shuffleQueries: undefined } });
                assert.strictEqual(result.exp['do-shuffle-queries'], undefined);
            });


            it('при отсутствии значения и заполненном cross', () => {
                const result = adapter({ ...adapterIn, config: { ...input, shuffleQueries: undefined } });
                assert.strictEqual(result.exp['do-shuffle-queries'], true);
            });


            it('при значении yes устанавливается в true', () => {
                const result = adapter({ ...adapterIn, config: { ...input, shuffleQueries: 'yes' } });
                assert.strictEqual(result.exp['do-shuffle-queries'], true);
            });


            it('при значении no устанавливается в false', () => {
                const result = adapter({ ...adapterIn, config: { ...input, shuffleQueries: 'no' } });
                assert.strictEqual(result.exp['do-shuffle-queries'], false);
            });
        });
    });

    describe('systems section', () => {
        it('should convert correctly', () => {
            assert.deepEqual(result.systems, expect.systems);
        });
    });

    describe('honeypots section', () => {
        it('should convert correctly', () => {
            assert.deepEqual(result.honeypots, expect.honeypots);
        });
    });

    describe('dual-honeypots section', () => {
        it('should convert correctly', () => {
            assert.deepEqual(result['dual-honeypots'], expect['dual-honeypots']);
        });
    });

    describe('режим использования ханипотов', () => {
        it('свойства use-auto-honeypots, honeypots, honeypots-tasts должны иметь корректные значения, если ханипоты выключены, но пользовательские ханипоты заданы', () => {
            const i = _.cloneDeep(adapterIn);

            i.config.honeypotsMode = HoneypotsMode.DISABLED;

            assert.isFalse(adapter(i).main['use-auto-honeypots']);
            assert.deepEqual(adapter(i).honeypots, []);
            assert.equal(adapter(i).exp['honeypot-tasks'], 0);
        });

        it('автоханипоты должны быть принудительно выключены, если в эксперименте есть тип системы, для который автоханипоты не поддерживаются', () => {
            const i = _.cloneDeep(adapterIn);

            i.config.options[0].engine = Engine.YANDEX_O;
            i.config.honeypotsMode = HoneypotsMode.AUTO;

            assert.isFalse(adapter(i).main['use-auto-honeypots']);
            assert.deepEqual(adapter(i).honeypots, []);
            assert.equal(adapter(i).exp['honeypot-tasks'], 0);
        });
    });
});

describe('nirvana-input/serp default honeypots', () => {
    let adapterIn, result;

    beforeEach(() => {
        input.goldenset = [];
        input.device = 'touch';
        input.useMerger = 'yes';
        input.honeypotsMode = HoneypotsMode.AUTO;
        adapterIn = {
            config: input,
            host: 'sbs.yandex-team.ru',
            ticket: 'SIDEBYSIDE-100500',
            pluginsHost,
            parseQuery: (str) => querystring.parse(str),
            profiles: profilesFixtures,
            poolsList: poolsListFixtures,
            features: feauturesFixtures,
            ytWorkspace,
            regular,
            systemFeatures,
        };
        result = adapter(adapterIn);
    });

    describe('dual-honeypots section', () => {
        it('should convert correctly', () => {
            const expected = [
                {
                    'good-sys': {
                        engine: 'yandex',
                        'sys-name': 'honeypot_y_p1_g_5',
                        'sys-id': 'h19',
                        'is-honeypot': true,
                        'exp-flags': '&sbs_plugin=no_interaction&sbs_plugin=csp_disable&exp_flags=login_tooltip%3Dnull&exp_flags=GEO_location_fed%3D0&exp_flags=enable-t-classes&exp_flags=hide-popups%3D1&exp_flags=distr_atom_to_bk%3D0&srcskip=ATOM_PROXY&test-mode=1&rearr=scheme_Local%2FUgc%2FDryRun%3D1&waitall=da&timeout=2000000',
                        beta: 'yandex',
                        'sys-type': 'yandex-web-touch',
                    },
                    'bad-sys': {
                        engine: 'google',
                        'sys-name': 'honeypot_y_p1_g_5_bad',
                        'sys-id': 'h18',
                        'is-bad-side': true,
                        'is-honeypot': true,
                        'bad-flags': '',
                        'exp-flags': '&start=40&sbs_plugin=no_interaction',
                        beta: 'google',
                        'sys-type': 'google-web-touch',
                    },
                },
                {
                    'good-sys': {
                        engine: 'google',
                        'sys-name': 'honeypot_g_p1_y_5',
                        'sys-id': 'h21',
                        'is-honeypot': true,
                        'exp-flags': '&sbs_plugin=no_interaction',
                        beta: 'google',
                        'sys-type': 'google-web-touch',
                    },
                    'bad-sys': {
                        engine: 'yandex',
                        'sys-name': 'honeypot_g_p1_y_5_bad',
                        'sys-id': 'h20',
                        'is-bad-side': true,
                        'is-honeypot': true,
                        'bad-flags': '',
                        'exp-flags': '&sbs_plugin=no_interaction&sbs_plugin=csp_disable&exp_flags=login_tooltip%3Dnull&exp_flags=GEO_location_fed%3D0&exp_flags=enable-t-classes&exp_flags=hide-popups%3D1&exp_flags=distr_atom_to_bk%3D0&srcskip=ATOM_PROXY&test-mode=1&rearr=scheme_Local%2FUgc%2FDryRun%3D1&waitall=da&p=5&timeout=2000000',
                        beta: 'yandex',
                        'sys-type': 'yandex-web-touch',
                    },
                },
                {
                    'good-sys': {
                        engine: 'google',
                        'sys-name': 'honeypot_g_p1_5',
                        'sys-id': 'h13',
                        'is-honeypot': true,
                        'exp-flags': '&sbs_plugin=no_interaction',
                        beta: 'google',
                        'sys-type': 'google-web-touch',
                    },
                    'bad-sys': {
                        engine: 'google',
                        'sys-name': 'honeypot_g_p1_5_bad',
                        'sys-id': 'h14',
                        'is-bad-side': true,
                        'is-honeypot': true,
                        'bad-flags': '',
                        'exp-flags': '&start=40&sbs_plugin=no_interaction',
                        beta: 'google',
                        'sys-type': 'google-web-touch',
                    },
                },
                {
                    'good-sys': {
                        engine: 'yandex',
                        'sys-name': 'honeypot_ya_p1_5',
                        'sys-id': 'h15',
                        'is-honeypot': true,
                        'exp-flags': '&sbs_plugin=no_interaction&sbs_plugin=csp_disable&exp_flags=login_tooltip%3Dnull&exp_flags=GEO_location_fed%3D0&exp_flags=enable-t-classes&exp_flags=hide-popups%3D1&exp_flags=distr_atom_to_bk%3D0&srcskip=ATOM_PROXY&test-mode=1&rearr=scheme_Local%2FUgc%2FDryRun%3D1&waitall=da&timeout=2000000',
                        beta: 'hamster.yandex',
                        'sys-type': 'yandex-web-touch',
                    },
                    'bad-sys': {
                        engine: 'yandex',
                        'sys-name': 'honeypot_ya_p2_5_bad',
                        'sys-id': 'h16',
                        'is-bad-side': true,
                        'is-honeypot': true,
                        'bad-flags': '',
                        'exp-flags': '&sbs_plugin=no_interaction&sbs_plugin=csp_disable&exp_flags=login_tooltip%3Dnull&exp_flags=GEO_location_fed%3D0&exp_flags=enable-t-classes&exp_flags=hide-popups%3D1&exp_flags=distr_atom_to_bk%3D0&srcskip=ATOM_PROXY&test-mode=1&rearr=scheme_Local%2FUgc%2FDryRun%3D1&waitall=da&p=5&timeout=2000000',
                        beta: 'hamster.yandex',
                        'sys-type': 'yandex-web-touch',
                    },
                },
            ];
            assert.deepEqual(result['dual-honeypots'], expected);
        });
    });

    describe('search-params', () => {
        it('Должен содержать значение queries.val в query-group-id если queries.source === metrics', () => {
            const ID = '123456';
            const config = { ...input, queries: { source: 'metrics', val: ID } };
            const result = adapter({ ...adapterIn, config });

            const searchParams = result['search-params'];
            assert.equal(searchParams['query-group-id'], ID);
        });

        it('Не должен содержать query-group-url если queries.source !== url', () => {
            const config = { ...input, queries: { source: 'metrics', val: '123' } };
            const result = adapter({ ...adapterIn, config });

            const searchParams = result['search-params'];
            assert.notExists(searchParams['query-group-url']);
        });

        it('Должен содержать queries.val в query-group-url если queries.source === url', () => {
            const URL = 'https://google.com?q=text';
            const config = { ...input, queries: { source: 'url', val: URL } };
            const result = adapter({ ...adapterIn, config });

            const searchParams = result['search-params'];
            assert.equal(searchParams['query-group-url'], URL);
        });
    });
});
