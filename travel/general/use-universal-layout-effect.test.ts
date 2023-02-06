import { renderHook } from '@testing-library/react';

import { useUniversalLayoutEffect } from './use-universal-layout-effect';

describe.skip('hooks/use-universal-layout-effect', () => {
    test('TODO', () => {
        // TODO
        const { result } = renderHook(() => useUniversalLayoutEffect(() => {}));

        expect(result.current).toBe(undefined);
    });
});
