'use strict';

const isBudgetAvailable = require('./is-budget-available.js');

test('works without options', () => {
    expect(isBudgetAvailable()).toBeTruthy();
});

test('over budget => false', () => {
    const options = {
        retryPolicy: {
            budget: 0.2
        },
        retryStats: {
            getSum: () => ({
                calcRatio: () => 0.3
            })
        }
    };

    expect(isBudgetAvailable(options)).toBeFalsy();
});

test('below budget => true', () => {
    const options = {
        retryPolicy: {
            budget: 0.2
        },
        retryStats: {
            getSum: () => ({
                calcRatio: () => 0.1
            })
        }
    };

    expect(isBudgetAvailable(options)).toBeTruthy();
});
