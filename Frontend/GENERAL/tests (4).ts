import { PersistedState } from 'redux-persist/es/types';

import deepFreeze from './deep-freeze';

export const prepareSlice = <T>(slice: T): Readonly<T & PersistedState> => {
    return deepFreeze({
        ...slice,
        _persist: {
            version: 1,
            rehydrated: true,
        },
    });
};
