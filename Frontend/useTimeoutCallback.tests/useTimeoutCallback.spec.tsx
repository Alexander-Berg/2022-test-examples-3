import { renderHook, act } from '@testing-library/react-hooks';

import { useTimeoutCallback } from '../useTimeoutCallback';

describe('useTimeoutCallback', () => {
    it('should call the callback on timeout', () => {
        const onTimeout = jest.fn();
        const TIMEOUT = 100;
        const { result } = renderHook(() => useTimeoutCallback(onTimeout, TIMEOUT));

        act(() => {
            const [run] = result.current;

            run();
        });

        setTimeout(() => {
            expect(onTimeout).toBeCalledTimes(1);
        }, TIMEOUT);
    });

    it('should cancel the callback before timeout', () => {
        const onTimeout = jest.fn();
        const TIMEOUT = 100;
        const { result } = renderHook(() => useTimeoutCallback(onTimeout, TIMEOUT));

        act(() => {
            const [run, cancel] = result.current;

            run();

            cancel();
        });

        setTimeout(() => {
            expect(onTimeout).toBeCalledTimes(0);
        }, TIMEOUT);
    });
});
