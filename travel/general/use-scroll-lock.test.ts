import { renderHook } from '@testing-library/react';

import { useScrollLock } from './use-scroll-lock';

describe.skip('hooks/use-scroll-lock', () => {
    test('TODO', () => {
        // TODO
        const { result } = renderHook(() => useScrollLock());

        expect(result.current).toBe(1);
    });
});
