import {waitFor, fireEvent} from '@testing-library/react';
import {mountPidget} from '~/pidgets/testHelpers/mountPidget';

import {State as PidgetState, makeInitialState} from '../state';
import {TestPidget} from '../index';

cat.describe('Pidget. TestPidget. Проверка эпиков в пиджетах', () => {
    cat.test({name: 'В эпиках должны работать агрегаты из rxjs', id: '0'}, async () => {
        await cat.step('Монтируем компонент', async () => {
            mountPidget<PidgetState>({
                Component: TestPidget,
                pidgetState: makeInitialState(),
                useReducer: true,
            });
        });

        await cat.step('Проверяем, что отображается первое число Фибоначчи', async () => {
            expect(cat.queryBySelector('[data-e2e="fibonacciNumber"]')).toBeInTheDocument();
            expect(cat.getBySelector('[data-e2e="fibonacciNumber"]')).toHaveTextContent('0');
        });

        await cat.step('Проверяем следующие n чисел Фибоначчи', async () => {
            for (const fibonacciNumber of [1, 1, 2, 3, 5, 8]) {
                await cat.step(
                    `Кликаем на кнопку получения следующего числа. Получаем ${fibonacciNumber}`,
                    // eslint-disable-next-line no-loop-func
                    async () => {
                        await fireEvent.click(cat.getBySelector('[data-e2e="loadNextFibonacciNumber"]'));
                        // eslint-disable-next-line no-loop-func
                        await waitFor(() =>
                            expect(cat.getBySelector('[data-e2e="fibonacciNumber"]')).toHaveTextContent(
                                String(fibonacciNumber),
                            ),
                        );
                    },
                );
            }
        });
    });
});
