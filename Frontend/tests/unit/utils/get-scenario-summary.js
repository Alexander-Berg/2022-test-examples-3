const getScenarioSummary = require('../../../src/server/utils/get-scenario-summary');

describe('utils/getScenarioSummary', () => {
    let summaryStub, systemsStub, questionsStub, batchResult, sbsResult;

    beforeEach(() => {
        summaryStub = [
            { score: 0.225, systemId: 'sys-0', total: 40, wins: 9 },
            { score: 0.6, systemId: 'sys-1', total: 40, wins: 24 },
        ];

        summaryStub = [
            {
                results: [
                    {
                        key: 'system-question-key',
                        score: 0.225,
                        total: 40,
                        wins: 9,
                    },
                ],
                systemId: 'sys-0',
            },
            {
                results: [
                    {
                        key: 'system-question-key',
                        score: 0.6,
                        total: 40,
                        wins: 24,
                    },
                ],
                systemId: 'sys-1',
            },
        ];

        systemsStub = [
            {
                name: 'Резкое видео',
                id: 'sys-0',
                url: 'https://samadhi-layouts.s3.yandex.net/sbs-YXJGZS/Резкий.html',
            },
            {
                name: 'Нерезкое видео',
                id: 'sys-1',
                url: 'https://samadhi-layouts.s3.yandex.net/sbs-Pz0DnL/не резкий.html',
            },
        ];

        questionsStub = [
            {
                key: 'system-question-key',
                question: 'Какое видео более высокого качества?',
            },
        ];

        batchResult = [
            {
                question: 'Какое видео более высокого качества?',
                key: 'system-question-key',
                score: 0.225,
                wins: 9,
                total: 40,
                systemId: 'sys-0',
                systemName: 'Резкое видео',
                url: '',
            },
            {
                question: 'Какое видео более высокого качества?',
                key: 'system-question-key',
                score: 0.6,
                wins: 24,
                total: 40,
                systemId: 'sys-1',
                systemName: 'Нерезкое видео',
                url: '',
            },
        ];

        sbsResult = [
            {
                question: 'Какое видео более высокого качества?',
                key: 'system-question-key',
                score: 0.225,
                wins: 9,
                total: 40,
                systemId: 'sys-0',
                systemName: 'Резкое видео',
                url: 'https://samadhi-layouts.s3.yandex.net/sbs-YXJGZS/Резкий.html',
            },
            {
                question: 'Какое видео более высокого качества?',
                key: 'system-question-key',
                score: 0.6,
                wins: 24,
                total: 40,
                systemId: 'sys-1',
                systemName: 'Нерезкое видео',
                url: 'https://samadhi-layouts.s3.yandex.net/sbs-Pz0DnL/не резкий.html',
            },
        ];
    });

    it('должен возвращать корректные данные для batch режима', () => {
        const summary = getScenarioSummary(summaryStub, systemsStub.map((s) => {
            s.isBatch = true;
            return s;
        }), questionsStub);

        assert.deepEqual(summary, batchResult);
    });

    it('должен возвращать корректные данные для одноэкранного режима', () => {
        const summary = getScenarioSummary(summaryStub, systemsStub, questionsStub);
        assert.deepEqual(summary, sbsResult);
    });
});
