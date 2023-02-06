import { act, renderHook } from '@testing-library/react-hooks';

import { useBooleanState } from '../useBooleanState';

describe('useBooleanState', () => {
    it('should set and return `true` state', () => {
        const { result } = renderHook(() => useBooleanState(true));

        expect(result.current.state).toStrictEqual(true);
    });

    it('should set and return `false` state', () => {
        const { result } = renderHook(() => useBooleanState(false));

        expect(result.current.state).toStrictEqual(false);
    });

    it('should change state to opposite', () => {
        const { result } = renderHook(() => useBooleanState(true));

        expect(result.current.state).toStrictEqual(true);

        act(() => {
            const { toggle } = result.current;

            toggle();
        });

        expect(result.current.state).toStrictEqual(false);
    });

    it('should change state to `true`', () => {
        const { result } = renderHook(() => useBooleanState(false));

        expect(result.current.state).toStrictEqual(false);

        act(() => {
            const { setTrue } = result.current;

            setTrue();
        });

        expect(result.current.state).toStrictEqual(true);
    });

    it('should change state to `false`', () => {
        const { result } = renderHook(() => useBooleanState(true));

        expect(result.current.state).toStrictEqual(true);

        act(() => {
            const { setFalse } = result.current;

            setFalse();
        });

        expect(result.current.state).toStrictEqual(false);
    });

    it('should set state to `true` and then `false`', () => {
        const { result } = renderHook(() => useBooleanState(false));

        expect(result.current.state).toStrictEqual(false);

        act(() => {
            const { setState } = result.current;

            setState(true);
        });

        expect(result.current.state).toStrictEqual(true);

        act(() => {
            const { setState } = result.current;

            setState(false);
        });

        expect(result.current.state).toStrictEqual(false);
    });
});
