import {render, cleanup} from '@testing-library/react';

import {createStateContainer} from '../StateContainer';

describe('StateContainer', () => {
    afterEach(cleanup);

    test('create StateContainer with default values', () => {
        const defaultValue = {};
        const values: object[] = [];
        const [StateContainer] = createStateContainer(defaultValue);

        render(<StateContainer>{value => values.push(value)}</StateContainer>);

        expect(values).toHaveLength(1);
        expect(values.pop()).toBe(defaultValue);
    });

    test('sets new value to container', () => {
        const value = {};
        const values: any[] = [];
        const [StateContainer, setState] = createStateContainer<{} | null>(
            null,
        );

        render(<StateContainer>{value => values.push(value)}</StateContainer>);

        setState(value);

        expect(values).toHaveLength(2);
        expect(values.pop()).toBe(value);
    });

    test('has no effect on unmounted component', () => {
        const value = {};
        const values: any[] = [];
        const [StateContainer, setState] = createStateContainer<{} | null>(
            null,
        );

        setState(value);

        const {unmount} = render(
            <StateContainer>{value => values.push(value)}</StateContainer>,
        );

        unmount();
        setState(value);

        expect(values).toHaveLength(1);
        expect(values.pop()).toBe(null);
    });
});
