import { receiveOperators, updateOperator, deleteOperator, operatorsReducer, initialState, OperatorsState } from '../operators';
import { bootstrap } from '../app';

import * as faker from '../../jest/faker';

function getOperatorsResult(items: Api.Operator[], offset = 0, limit = 20): Api.GetOperatorsResponse['result'] {
    return {
        items,
        offset,
        limit,
        total: items.length,
    };
}

describe.skip('Operators action creators', () => {
    describe('receiveOperators', () => {
        it('returns action with correct data', () => {
            const data = getOperatorsResult([faker.operator(), faker.operator()]);

            expect(receiveOperators(data)).toEqual(expect.objectContaining(data));
        });
    });

    describe('updateOperator', () => {
        it('returns action with correct data', () => {
            const operator = faker.operator();

            expect(updateOperator(operator)).toEqual(expect.objectContaining({
                operator,
            }));
        });
    });

    describe('deleteOperators', () => {
        it('returns action with correct data', () => {
            const operator = faker.operator();

            expect(deleteOperator(operator)).toEqual(expect.objectContaining({
                uid: operator.uid,
            }));
        });
    });
});

describe.skip('Operators reducer', () => {
    const now = Date.now();

    const nowSpy = jest.spyOn(Date, 'now').mockReturnValue(now);

    afterAll(() => {
        nowSpy.mockRestore();
    });

    describe('receiveOperators', () => {
        it('returns correct state', () => {
            const data = getOperatorsResult([faker.operator(), faker.operator()]);

            const state = operatorsReducer(initialState, receiveOperators(data));

            expect(state).not.toBe(initialState);

            expect(state).toEqual({
                ...initialState,
                byId: {
                    [data.items[0].uid]: data.items[0],
                    [data.items[1].uid]: data.items[1],
                },
                lastRequestTime: now,
                total: 2,
                offset: 2,
            });
        });
    });

    describe('bootstrap', () => {
        it('returns correct state', () => {
            const operator = faker.operator();

            const state = operatorsReducer(initialState, bootstrap({
                operator,
                robotId: faker.guid(),
            }));

            expect(state).not.toBe(initialState);

            expect(state).toEqual({
                ...initialState,
                byId: {
                    [operator.uid]: operator,
                },
            });
        });
    });

    describe('updateOperator', () => {
        it('returns new state if operator does not exists', () => {
            const operator = faker.operator();

            const state = operatorsReducer(initialState, updateOperator(operator));

            expect(state).not.toBe(initialState);

            expect(state).toEqual({
                ...initialState,
                byId: {
                    [operator.uid]: operator,
                },
            });
        });

        it('returns original state if operators version lower or equal then existing', () => {
            const operator = faker.operator();

            const state: OperatorsState = {
                ...initialState,
                byId: {
                    [operator.uid]: operator,
                },
            };

            expect(operatorsReducer(state, updateOperator(operator))).toBe(state);

            const updatedOperator: Api.Operator = {
                ...operator,
                updateTime: operator.updateTime + 1,
            };

            expect(operatorsReducer(state, updateOperator(updatedOperator))).toEqual({
                ...state,
                byId: {
                    [operator.uid]: updatedOperator,
                },
            });
        });
    });

    describe('deleteOperator', () => {
        it('returns original state if operator does not exists', () => {
            const operator = faker.operator();

            expect(operatorsReducer(initialState, deleteOperator(operator))).toBe(initialState);
        });

        it('returns new state if operator exists', () => {
            const operator = faker.operator();

            const state = {
                ...initialState,
                byId: {
                    [operator.uid]: operator,
                },
            };

            expect(operatorsReducer(state, deleteOperator(operator))).not.toBe(state);
            expect(operatorsReducer(state, deleteOperator(operator))).toEqual(initialState);
        });
    });
});
