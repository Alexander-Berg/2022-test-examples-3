const {
    normalizeStatus,
    normalizeStage,
    getDetailedSteps,
    getCommonStatus,
    getCommonStatusRu,
    isFinishedWithNoAssessment,
    getExpSteps,
    finishedWithNoAssessmentStatus,
} = require('../../../../src/client/components/utils/statuses');
const config = require('config');

describe('utils/statuses', () => {
    describe('normalizeStatus', () => {
        const fixtures = [
            {
                name: 'при отсутствии lastStatus должна возвращаться пустая строка',
                input: {
                    expValidityStatus: 'approved',
                    lastStatus: null,
                },
                expected: '',
            },
            {
                name: 'при наличии результата должна возвращаться строка results',
                input: {
                    hasResults: true,
                },
                expected: 'results',
            },
            {
                name: 'на поле results не влияют другие поля',
                input: {
                    expValidityStatus: 'rejected',
                    hasResults: true,
                    lastStatus: {
                        stage: 'workflow-preparing',
                        status: 'failed',
                    },
                },
                expected: 'results',
            },
            {
                name: 'rejected',
                input: {
                    expValidityStatus: 'rejected',
                    lastStatus: {},
                },
                expected: 'failed',
            },
            {
                name: 'при статусе failed возвращает статус failed вне зависимости от stage',
                input: {
                    lastStatus: {
                        stage: 'merge-ext-systems',
                        status: 'failed',
                    },
                },
                expected: 'failed',
            },
            {
                input: {
                    lastStatus: {
                        stage: 'pool-ready',
                        status: 'succeeded',
                    },
                },
                expected: 'attention',
            },
            {
                input: {
                    expValidityStatus: 'approved',
                    lastStatus: {
                        stage: 'workflow-started',
                        status: 'in-progress',
                    },
                },
                expected: 'in-progress',
            },
        ];

        fixtures.forEach(({ name, input, expected }) => {
            const lastStatus = input.lastStatus;

            it(name ? name : `${lastStatus.stage}/${lastStatus.status}`, () => {
                assert.strictEqual(normalizeStatus({ ...input }), expected);
            });
        });
    });

    describe('normalizeStage', () => {
        const expStages = {
            'workflow-starting': { title: 'Запуск графа', step: 'preparing' },
            'merge-ext-systems': { title: 'Добавляем внешние системы', step: 'preparing' },
            'pool-auto-ready': { title: 'Готовим пул', step: 'approve' },
        };

        const fixtures = [
            {
                name: 'при наличии результата должна возвращаться строка results',
                input: {
                    hasResults: true,
                },
                expected: 'results',
            },
            {
                name: 'на поле results не влияют другие поля',
                input: {
                    expValidityStatus: 'rejected',
                    hasResults: true,
                    lastStatus: {
                        stage: 'workflow-preparing',
                        status: 'failed',
                    },
                },
                expected: 'results',
            },
            {
                name: 'rejected',
                input: {
                    expValidityStatus: 'rejected',
                    lastStatus: {},
                },
                expected: 'Отклонено',
            },
            {
                name: 'при отсутствии lastStatus должна возвращаться пустая строка',
                input: {
                    expValidityStatus: 'approved',
                    lastStatus: null,
                },
                expected: '',
            },
            {
                name: 'корректно обрабатывает статус failed с неизвестным stage',
                input: {
                    lastStatus: {
                        stage: 'merge-ext-systems',
                        status: 'failed',
                    },
                },
                expected: 'Завершился неудачно',
            },
            {
                input: {
                    lastStatus: {
                        stage: 'workflow-preparing',
                        status: 'failed',
                    },
                },
                expected: 'Не удалось подготовить',
            },
            {
                input: {
                    lastStatus: {
                        stage: 'workflow-starting',
                        status: 'failed',
                    },
                },
                expected: 'Не удалось запустить',
            },
            {
                input: {
                    lastStatus: {
                        stage: 'load-ext-systems',
                        status: 'failed',
                    },
                },
                expected: 'Завершился неудачно',
            },
            {
                input: {
                    lastStatus: {
                        stage: 'load-ext-systems',
                        status: 'canceled',
                    },
                },
                expected: 'Остановлен',
            },
            {
                input: {
                    lastStatus: {
                        stage: 'workflow-starting',
                        status: 'in-progress',
                    },
                },
                expected: 'Запуск графа',
            },
            {
                input: {
                    lastStatus: {
                        stage: 'pool-auto-ready',
                        status: 'in-progress',
                    },
                },
                expected: 'Готовим пул',
            },
            {
                name: 'при отключенной разметке и статусе pool-ready эксперимент считается завершенным',
                input: {
                    lastStatus: {
                        stage: 'pool-ready',
                        status: 'succeeded',
                    },
                    params: {
                        assessmentGroup: 'none',
                    },
                },
                expected: finishedWithNoAssessmentStatus,
            },
        ];

        fixtures.forEach(({ name, input, expected }) => {
            const lastStatus = input.lastStatus;

            it(name ? name : `${lastStatus.stage}/${lastStatus.status}`, () => {
                assert.strictEqual(normalizeStage({ ...input }, expStages), expected);
            });
        });
    });

    describe('getDetailedSteps', () => {
        const expStages = {
            'workflow-preparing': { title: 'Подготовка графа', step: 'preparing' },
            'pool-ready': { title: 'Проверьте пул', step: 'approve' },
            'pool-start': { title: 'Пул размечается', step: 'assessment' },
            'save-results': { title: 'Готовим результаты', step: 'results' },
        };

        const expSteps = {
            'preparing': {
                title: 'Подготовка',
                help: 'Готовим данные для отправки в Толоку',
            },
            'approve': {
                title: 'Аппрув',
                help: 'Аналитик (исполнитель тикета) проверит эксперимент и запустит в Толоке',
            },
            'assessment': {
                title: 'Разметка',
                help: 'Эксперимент размечается',
            },
            'results': {
                title: 'Результаты',
                help: 'Обрабатываем результаты',
            },
        };

        const fixtures = [
            {
                name: 'стадии макетного эксперимента',
                input: {
                    steps: ['preparing', 'approve', 'assessment', 'results'],
                },
                expected: [
                    {
                        title: 'Подготовка',
                        step: 'preparing',
                        help: 'Готовим данные для отправки в Толоку',
                        stages: ['workflow-preparing'],
                    },
                    {
                        title: 'Аппрув',
                        step: 'approve',
                        help: 'Аналитик (исполнитель тикета) проверит эксперимент и запустит в Толоке',
                        stages: ['pool-ready'],
                    },
                    {
                        title: 'Разметка',
                        step: 'assessment',
                        help: 'Эксперимент размечается',
                        stages: ['pool-start'],
                    },
                    {
                        title: 'Результаты',
                        step: 'results',
                        help: 'Обрабатываем результаты',
                        stages: ['save-results'],
                    },
                ],
            },
            {
                name: 'стадии чего угодно с автостартом',
                input: {
                    steps: ['preparing', 'assessment', 'results'],
                },
                expected: [
                    {
                        title: 'Подготовка',
                        step: 'preparing',
                        help: 'Готовим данные для отправки в Толоку',
                        stages: ['workflow-preparing'],
                    },
                    {
                        title: 'Разметка',
                        step: 'assessment',
                        help: 'Эксперимент размечается',
                        stages: ['pool-start'],
                    },
                    {
                        title: 'Результаты',
                        step: 'results',
                        help: 'Обрабатываем результаты',
                        stages: ['save-results'],
                    },
                ],
            },
        ];

        fixtures.forEach(({ name, input, expected, type }) => {
            it(name, () => {
                assert.deepEqual(getDetailedSteps(input.steps, expStages, expSteps, type), expected);
            });
        });
    });

    describe('getCommonStatus', () => {
        it('hasResults=true, lastStatus=undefined, hasWorkflows=true', () => {
            assert.strictEqual(getCommonStatus(true, undefined, true), 'finished');
        });

        it('hasResults=false, lastStatus.status=failed, hasWorkflows=true', () => {
            assert.strictEqual(getCommonStatus(false, { status: 'failed' }, true), 'failed');
        });

        it('hasResults=false, lastStatus.status=canceled, hasWorkflows=true', () => {
            assert.strictEqual(getCommonStatus(false, { status: 'canceled' }, true), 'failed');
        });

        it('hasResults=false, lastStatus.status=not failed, hasWorkflows=true', () => {
            assert.strictEqual(getCommonStatus(false, { status: 'not failed' }, true), 'running');
        });

        it('hasResults=false, lastStatus.status=not failed, hasWorkflows=false', () => {
            assert.strictEqual(getCommonStatus(false, { status: 'not failed' }, false), 'new');
        });

        it('assessmentGroup=none, lastStatus.stage=pool-ready, lastStatus.status=succeede', () => {
            assert.strictEqual(getCommonStatus(false, { stage: 'pool-ready', status: 'succeeded' }, true, 'none'), 'finished');
        });
    });

    describe('getCommonStatusRu', () => {
        it('hasResults=true, lastStatus=undefined, hasWorkflows= true', () => {
            assert.strictEqual(getCommonStatusRu(true, undefined, true), 'Есть результаты');
        });

        it('hasResults=false, lastStatus.status=failed, hasWorkflows=true', () => {
            assert.strictEqual(getCommonStatusRu(false, { status: 'failed' }, true), 'Ошибка');
        });

        it('hasResults=false, lastStatus.status=canceled, hasWorkflows=true', () => {
            assert.strictEqual(getCommonStatusRu(false, { status: 'canceled' }, true), 'Ошибка');
        });

        it('hasResults=false, lastStatus.status=not failed, hasWorkflows=true', () => {
            assert.strictEqual(getCommonStatusRu(false, { status: 'not failed' }, true), 'В процессе');
        });

        it('hasResults=false, lastStatus.status=not failed, hasWorkflows=false', () => {
            assert.strictEqual(getCommonStatusRu(false, { status: 'not failed' }, false), 'Подготовка не начата');
        });

        it('assessmentGroup=none, lastStatus.stage=pool-ready, lastStatus.status=succeeded', () => {
            assert.strictEqual(getCommonStatusRu(false, { stage: 'pool-ready', status: 'succeeded' }, true, 'none'), 'Есть результаты');
        });
    });

    describe('isFinishedWithNoAssessment', () => {
        it('эксперимент считается завершенным, если разметка отключена и stage="pool-ready", status="succeeded"', () => {
            assert.strictEqual(isFinishedWithNoAssessment({ stage: 'pool-ready', status: 'succeeded' }, 'none'), true);
        });

        it('вовзращает false, если статус не "succeeed"', () => {
            assert.strictEqual(isFinishedWithNoAssessment({ stage: 'pool-ready', status: 'failed' }, 'internal'), false);
        });

        it('вовзращает false, если разметка включена', () => {
            assert.strictEqual(isFinishedWithNoAssessment({ stage: 'pool-ready' }, 'internal'), false);
        });

        it('вовзращает false, если статус не pool-ready', () => {
            assert.strictEqual(isFinishedWithNoAssessment({ stage: 'configure' }, 'internal'), false);
        });
    });

    describe('getExpStages', () => {
        it('вовзращает корректные шаги для эксперимента без разметки', () => {
            assert.deepEqual(getExpSteps(true, true, config), config.skipAssessmentExpSteps);
        });

        it('вовзращает корректные шаги для эксперимента с автозапуском', () => {
            assert.deepEqual(getExpSteps(true, false, config), config.autostartExpSteps);
        });

        it('вовзращает корректные шаги для обычного эксперимента', () => {
            assert.deepEqual(getExpSteps(false, false, config), config.withoutAutostartExpSteps);
        });
    });
});

