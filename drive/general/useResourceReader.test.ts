import { renderHook } from '@testing-library/react-hooks';

import { UseFetchResource } from 'shared/hooks/useFetch/useFetch';
import { useResourceReader } from 'shared/hooks/useResourceReader/useResourceReader';

describe('useResourceReader', function () {
    it('works with empty resource', function () {
        const { result } = renderHook(() => useResourceReader(undefined));

        expect(result.current).toBeUndefined();
    });

    it('undefined on empty cache', function () {
        const resource: UseFetchResource<string> = {
            getKey: () => {
                return 'uniq-key1';
            },
            read: () => {
                throw new Error('500ka');
            },
            reload: () => {},
        };

        const firstCall = renderHook(() => useResourceReader(resource));

        expect(firstCall.result.current).toBeUndefined();
    });

    it('works with read', function () {
        const resource: UseFetchResource<string> = {
            getKey: () => {
                return 'uniq-key2';
            },
            read: () => {
                return 'value';
            },
            reload: () => {},
        };

        const { result } = renderHook(() => useResourceReader(resource));

        expect(result.current).toEqual('value');
    });

    it('works with cache', function () {
        let firstTime = true;
        const resource: UseFetchResource<string> = {
            getKey: () => {
                return 'uniq-key3';
            },
            read: () => {
                if (firstTime) {
                    firstTime = false;

                    return 'value';
                }

                throw new Error('500ka');
            },
            reload: () => {},
        };

        const firstCall = renderHook(() => useResourceReader(resource));

        expect(firstCall.result.current).toEqual('value');

        const secondCall = renderHook(() => useResourceReader(resource));

        expect(secondCall.result.current).toEqual('value');
    });

    it('works with Promise', function () {
        const resource: UseFetchResource<string> = {
            getKey: () => {
                return 'uniq-key4';
            },
            read: () => {
                throw new Promise((resolve) => setTimeout(resolve, 100));
            },
            reload: () => {},
        };

        try {
            useResourceReader(resource);
        } catch (e) {
            expect(e instanceof Promise).toBeTruthy();
        }
    });

    it('works with throws', function () {
        const resource: UseFetchResource<string> = {
            getKey: () => {
                return 'uniq-key4';
            },
            read: () => {
                throw new Error('404');
            },
            reload: () => {},
        };

        const { result } = renderHook(() => useResourceReader(resource, true));

        expect(result.error?.message).toEqual('404');
    });
});
