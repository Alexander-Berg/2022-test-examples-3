import { useCallback } from 'react';

type TCallback = () => void;

export const useEnter = (cbs: Array<TCallback | undefined>) => {
    const handleEnter = useCallback(() => {
        for (let i = 0; i < cbs.length; i++) {
            const cb = cbs[i];
            if (typeof cb !== 'undefined') {
                cb();
            }
        }
    }, cbs);

    return handleEnter;
};
