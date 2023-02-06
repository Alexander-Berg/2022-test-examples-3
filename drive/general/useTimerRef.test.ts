import { renderHook } from '@testing-library/react-hooks';

import { useTimerRef } from 'shared/hooks/useTimerRef/useTimerRef';

describe('useTimerRef', function () {
    it('works with empty params', function () {
        const { result } = renderHook(() => useTimerRef());

        expect(result.current).toEqual({ current: 0 });
    });

    it('works with filled params', function () {
        const { result } = renderHook(() => useTimerRef(100));

        expect(result.current).toEqual({ current: 100 });
    });
});
