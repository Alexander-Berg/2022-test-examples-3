import IState from '../../interfaces/state/IState';
import EnvironmentType from '../../interfaces/EnvironmentType';

import createUrlChecker from '../createUrlChecker';

const stateBuilder = (fullUrl: string, environment: EnvironmentType): IState =>
    ({
        page: {
            fullUrl,
        },
        environment: {
            type: environment,
        },
    } as IState);

describe('createUrlChecker', () => {
    describe('serverside', () => {
        it('Вне зависимости от того было ли изменение страницы - не выкидываем ошибку', async () => {
            const state = stateBuilder('1', EnvironmentType.server);
            const getState = (): IState => state;
            const checker = createUrlChecker(getState);

            expect(getState().page.fullUrl).toBe('1');
            await expect(checker(1)).resolves.toBe(1);

            state.page.fullUrl = '2';

            expect(getState().page.fullUrl).toBe('2');
            await expect(checker(3)).resolves.toBe(3);
        });
    });

    describe('clientside', () => {
        it('Если произошло изменение страницы - выбросим ошибку', async () => {
            const state = stateBuilder('1', EnvironmentType.client);
            const getState = (): IState => state;
            const checker = createUrlChecker(getState);

            expect(getState().page.fullUrl).toBe('1');
            await expect(checker(4)).resolves.toBe(4);

            state.page.fullUrl = '2';

            expect(getState().page.fullUrl).toBe('2');
            await expect(checker(5)).rejects.toThrow();
        });
    });
});
