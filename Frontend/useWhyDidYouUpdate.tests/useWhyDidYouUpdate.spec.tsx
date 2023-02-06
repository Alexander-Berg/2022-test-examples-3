import { renderHook } from '@testing-library/react-hooks';

import { useWhyDidYouUpdate } from '../useWhyDidYouUpdate';

describe('useWhyDidYouUpdate', () => {
    it('should show changed props and omit unchanged', () => {
        const testProps = {
            staticProp: 1,
            dynamicProp: 1,
        };

        const changedTestProps = {
            staticProp: 1,
            dynamicProp: 2,
        };

        const { result, rerender } = renderHook((props: Record<string, unknown>) => useWhyDidYouUpdate(props), {
            initialProps: testProps,
        });

        rerender(changedTestProps);

        expect(result.current).toHaveProperty('dynamicProp');
        expect(result.current).not.toHaveProperty('staticProp');
    });

    it('should show previous and current value of a changed props', () => {
        const testProps = {
            prop: 1,
        };

        const changedTestProps = {
            prop: 2,
        };

        const { result, rerender } = renderHook((props: Record<string, unknown>) => useWhyDidYouUpdate(props), {
            initialProps: testProps,
        });

        rerender(changedTestProps);

        expect(result.current).toEqual({ prop: { from: 1, to: 2 } });
    });
});
