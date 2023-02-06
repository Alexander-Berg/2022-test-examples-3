import { renderHook } from '@testing-library/react';

import { useFocusTrap } from './use-focus-trap';

describe.skip('hooks/use-focus-trap', () => {
    test('TODO', () => {
        // TODO
        const { result } = renderHook(() => useFocusTrap());

        expect(result.current).toBe(1);
    });
});
