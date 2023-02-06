import React from 'react';
import {render} from '@testing-library/react';
import {Provider} from 'react-redux';
import {combineReducers, Reducer} from 'redux';

import mockStore from '~/spec/mocks/store';
import getState from '~/spec/fixtures/state';
import type {MockStateOptions} from '~/spec/fixtures/state';
import {makeDispatch, PidgetCore} from '~/pidgets/root';
import {convergeReducers} from '~/utils/reducers';
import {pidgetsReducer} from '~/reducers/pidgets';
import rootReducer, {dummyReducer} from '~/reducers';
import type {AnyAppState} from '~/reducers';

type MountComponentArgs<S, P> = {
    Component: React.ComponentType<P>;
    props?: P;
    pidgetName?: string;
    pidgetState: S;
    globalState?: Partial<MockStateOptions>;
    useReducer?: boolean;
};

export function mountPidget<S, P = any>({
    Component,
    props = {} as P,
    pidgetName = Component.displayName,
    pidgetState,
    globalState = {},
    useReducer = false,
}: MountComponentArgs<S, P>) {
    if (!pidgetName) {
        throw new Error('mountComponent has required prop pidgetName');
    }

    const pidgetBaseId = 'fakeId';
    const pidgetId = `/${pidgetName}_${pidgetBaseId}`;

    // @ts-expect-error Ожидаем ошибку, для части экшенов payload обязательный, для других - нет
    const reducer = combineReducers({
        ...rootReducer,
        page: dummyReducer,
        pidgets: convergeReducers(pidgetsReducer, PidgetCore.getReducer()),
    }) as Reducer<AnyAppState>;

    const store = mockStore({
        state: getState({
            pidgets: {
                [pidgetName]: {
                    [pidgetId]: pidgetState,
                },
            },
            ...globalState,
        }),
        epics: PidgetCore.getEpics(),
        reducer: useReducer ? reducer : undefined,
    });

    store.dispatch = makeDispatch(store.dispatch, {
        pidgetId,
        pidgetName,
    });

    return {
        pidget: render(
            <Provider store={store}>
                <Component {...props} $id={pidgetBaseId} />
            </Provider>,
        ),
        store,
    };
}
