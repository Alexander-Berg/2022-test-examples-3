import { renderHook } from '@testing-library/react';

import { useId } from './use-id';

describe('hooks/use-id', () => {
    test('should get random id', () => {
        const { result } = renderHook(() => useId());

        expect(result.current).toBeInstanceOf(String);
    });

    test('should get current id', () => {
        const { result } = renderHook(() => useId('foo'));

        expect(result.current).toBe('foo');
    });
});
