import {
    getOperatorDisplayName,
    getOperatorDisplayNameByUid,
    getCurrentOperator,
    getOperator,
    getOperators,
    isCurrentOperator,
} from '../operators';

import * as faker from '../../jest/faker';

import { createState, createOperatorsState } from '../../jest/state';

describe.skip('selectors/operators', () => {
    describe('getOperatorDisplayName', () => {
        it('returns operators display name', () => {
            const operator = faker.operator();

            const displayName = `${operator.first_name} ${operator.last_name}`;

            expect(getOperatorDisplayName(operator)).toEqual(displayName);
        });

        it('returns operators first name if the last name is empty', () => {
            const operator = faker.operator({ last_name: '' });

            expect(getOperatorDisplayName(operator)).toEqual(operator.first_name);
        });

        it('returns operators last name if the first name is empty', () => {
            const operator = faker.operator({ first_name: '' });

            expect(getOperatorDisplayName(operator)).toEqual(operator.last_name);
        });

        it('returns operators login if the first and last name is empty', () => {
            const login = 'johndoe';

            const operator = faker.operator({
                first_name: '',
                last_name: '',
                email: `${login}@yandex.ru`,
            });

            expect(getOperatorDisplayName(operator)).toEqual(login);
        });
    });

    describe('getOperatorDisplayNameByUid', () => {
        it('returns operators display name by uid', () => {
            const operator = faker.operator();

            const state = createState({
                operators: createOperatorsState(operator),
            });

            const displayName = `${operator.first_name} ${operator.last_name}`;

            expect(getOperatorDisplayNameByUid(state, operator.uid)).toEqual(displayName);
        });

        it('returns empty string for unknown uid', () => {
            const state = createState();

            expect(getOperatorDisplayNameByUid(state, faker.uid())).toEqual('');
        });
    });

    describe('getCurrentOperator', () => {
        it('returns current operator', () => {
            const operator = faker.operator();

            const state = createState({
                operators: createOperatorsState(operator),
                app: {
                    operatorId: operator.uid,
                },
            });

            expect(getCurrentOperator(state)).toEqual(operator);
        });

        it('returns undefined if there is no current operator', () => {
            const state = createState({
                app: {
                    operatorId: faker.uid(),
                },
            });

            expect(getCurrentOperator(state)).toBe(undefined);
        });
    });

    describe('getOperator', () => {
        it('returns a operator', () => {
            const operator = faker.operator();

            const state = createState({
                operators: createOperatorsState(operator),
            });

            expect(getOperator(state, operator.uid)).toEqual(operator);
        });

        it('returns undefined if unknown uid', () => {
            const operator = faker.operator();

            const state = createState({
                operators: createOperatorsState(operator),
            });

            expect(getOperator(state, faker.uid())).toBe(undefined);
        });
    });

    describe('getOperators', () => {
        it('returns array of operators', () => {
            const state = createState({
                operators: createOperatorsState(
                    faker.operator(),
                    faker.operator(),
                    faker.operator(),
                ),
            });

            expect(getOperators(state)).toHaveLength(3);
        });

        /**
         * @todo Написать тесты для сортировок
         * @ticket https://st.yandex-team.ru/MSSNGRFRONT-3755
         */
    });

    describe('isCurrentOperatorId', () => {
        it('returns true for current operator uid', () => {
            const operatorId = faker.uid();

            const state = createState({
                app: { operatorId },
            });

            expect(isCurrentOperator(state, operatorId)).toBe(true);
        });

        it('returns false for non-current operator uid', () => {
            const state = createState({
                app: { operatorId: faker.uid() },
            });

            expect(isCurrentOperator(state, faker.uid())).toBe(false);
        });
    });
});
