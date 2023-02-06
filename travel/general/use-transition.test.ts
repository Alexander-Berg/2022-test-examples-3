import { renderHook } from '@testing-library/react';

import { useTransition } from './use-transition';

describe.skip('hooks/use-transition', () => {
    test('TODO', () => {
        // TODO
        const { result } = renderHook(() => useTransition({}));

        expect(result.current).toBe(1);
    });
});
