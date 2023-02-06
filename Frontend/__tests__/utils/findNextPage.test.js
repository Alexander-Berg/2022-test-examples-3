import { normalizeAnswers, findNextPage } from '../../src/utils/findNextPage';

describe('normalizeAnswers', () => {
    test('возвращает корректную структуры вида { ключ: значение/массив значений }', () => {
        const answers = {
            q1: 'answer 1',
            q2: [
                {
                    answer: 'answer 2-1',
                    isOtherOption: false,
                },
                {
                    answer: 'answer 2-2',
                    isOtherOption: false,
                },
            ],
            q3: 5,
        };

        const expectedOutput = {
            q1: 'answer 1',
            q2: ['answer 2-1', 'answer 2-2'],
            q3: 5,
        };

        expect(normalizeAnswers(answers)).toEqual(expectedOutput);
    });
});

describe('findNextPage', () => {
    const pages = [
        {
            key: 'page-1',
            constraints: {},
        },
        {
            key: 'page-2',
            constraints: {
                'bool-operator': 'and',
                conditions: [
                    {
                        key: 'system-radio-key-1',
                        'compare-operator': 'in',
                        value: [
                            'вариант А1', 'вариант А3',
                        ],
                    },
                ],
            },
        },
        {
            key: 'page-3',
            constraints: {
                'bool-operator': 'and',
                conditions: [
                    {
                        key: 'system-scale-key-4',
                        'compare-operator': '>',
                        value: 0,
                    },
                ],
            },
        },
    ];

    test('возвращает корректный индекс следующей страницы, для которой выполнены условия показа', () => {
        const currentPageIndex = 0;
        const answers = {
            'system-radio-key-1': 'вариант А2',
            'system-scale-key-4': 2,
        };
        const expectedIndex = 2;

        expect(findNextPage(currentPageIndex, pages, answers)).toBe(expectedIndex);
    });

    test('возвращает null, если в списке не осталось страниц, для которых выполняются условия показа', () => {
        const currentPageIndex = 0;
        const answers = {
            'system-radio-key-1': 'вариант А2',
            'system-scale-key-4': -1,
        };
        const expectedIndex = null;

        expect(findNextPage(currentPageIndex, pages, answers)).toBe(expectedIndex);
    });
});
