import { BOOLEAN_OPERATORS, checkCondition, checkConstraints } from '../../src/utils/constraints';

describe('checkCondition', () => {
    test('правильно обрабатывает операцию >', () => {
        const condition = {
            'compare-operator': '>',
            value: 3,
        };

        const valueToCheck = 5;

        expect(checkCondition(condition, valueToCheck)).toBe(true);
    });

    test('правильно обрабатывает операцию <', () => {
        const condition = {
            'compare-operator': '<',
            value: 3,
        };

        const valueToCheck = 5;

        expect(checkCondition(condition, valueToCheck)).toBe(false);
    });

    test('правильно обрабатывает операцию in', () => {
        const condition = {
            'compare-operator': 'in',
            value: ['ok', 'approved'],
        };

        const valueToCheck = ['ok'];

        expect(checkCondition(condition, valueToCheck)).toBe(true);
    });

    test('правильно обрабатывает операцию not-in', () => {
        const condition = {
            'compare-operator': 'not-in',
            value: ['not-ok', 'not-approved'],
        };

        const valueToCheck = ['ok'];

        expect(checkCondition(condition, valueToCheck)).toBe(true);
    });

    test('правильно обрабатывает операцию any-in, когда нужный ответ присутствует', () => {
        const condition = {
            'compare-operator': 'any-in',
            value: ['ok'],
        };

        const valueToCheck = ['ok', 'approved', 'not-ok'];

        expect(checkCondition(condition, valueToCheck)).toBe(true);
    });

    test('правильно обрабатывает операцию any-in, когда нужный ответ отсутствует', () => {
        const condition = {
            'compare-operator': 'any-in',
            value: ['ok'],
        };

        const valueToCheck = ['approved', 'not-ok'];

        expect(checkCondition(condition, valueToCheck)).toBe(false);
    });
});

describe('checkConstraints', () => {
    test('возвращает true, если список условий пуст', () => {
        expect(checkConstraints({ conditions: [] }, {})).toBe(true);
    });

    describe('bool-operator: AND', () => {
        test('вовзращает true, если все условия соблюдены', () => {
            const constraints = {
                'bool-operator': BOOLEAN_OPERATORS.AND,
                conditions: [
                    {
                        'compare-operator': 'in',
                        value: ['ok', 'approved'],
                        key: 'question-1',
                    },
                    {
                        'compare-operator': '>',
                        value: 5,
                        key: 'question-2',
                    },
                ],
            };

            const answers = {
                'question-1': ['approved'],
                'question-2': 6,
            };

            expect(checkConstraints(constraints, answers)).toBe(true);
        });

        test('вовзращает false, если хотя бы одно из условий нарушено', () => {
            const constraints = {
                'bool-operator': BOOLEAN_OPERATORS.AND,
                conditions: [
                    {
                        'compare-operator': 'in',
                        value: ['ok', 'approved'],
                        key: 'question-1',
                    },
                    {
                        'compare-operator': '>',
                        value: 5,
                        key: 'question-2',
                    },
                ],
            };

            const answers = {
                'question-1': ['not-approved'],
                'question-2': 6,
            };

            expect(checkConstraints(constraints, answers)).toBe(false);
        });

        test('возвращает falsy-значение, если bool-operator не находится в списке известных', () => {
            const constraints = {
                conditions: [
                    {
                        'compare-operator': 'in',
                        value: ['ok', 'approved'],
                        key: 'question-1',
                    },
                    {
                        'compare-operator': '>',
                        value: 5,
                        key: 'question-2',
                    },
                ],
            };

            const answers = {
                'question-1': ['not-approved'],
                'question-2': 6,
            };

            expect(checkConstraints(constraints, answers)).toBeFalsy();
        });
    });
});
