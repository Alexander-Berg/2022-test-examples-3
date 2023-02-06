import { renderHook } from '@testing-library/react-hooks';

import { useCurrent } from '../useCurrent';

describe('useCurrent', () => {
    it('should return same passed value', () => {
        const { result, rerender } = renderHook((value = 0) => useCurrent(value));

        expect(result.current.current).toEqual(0);

        rerender(1);

        expect(result.current.current).toEqual(1);
    });

    it('should trigger cb function on unmount with saved value', () => {
        const onCleanup = jest.fn();

        const { rerender } = renderHook(
            ({ value }) => {
                useCurrent(value, onCleanup);
            },
            {
                initialProps: {
                    value: 0,
                },
            },
        );

        rerender({ value: 1 });

        setTimeout(() => {
            expect(onCleanup).toHaveBeenCalled();
            expect(onCleanup).toHaveBeenCalledWith({ value: 0 });
        }, 10);
    });
});
