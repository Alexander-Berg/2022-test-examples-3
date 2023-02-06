import React from 'react';
import {Provider} from 'react-redux';
import {combineReducers, Reducer, AnyAction} from 'redux';
import {Registry} from '@yandex-levitan/b2b';

import type {AnyAppState} from 'shared/types/reducers';
import type {AnyEpicAction, UnifyActions} from 'shared/types/redux';

import mockStore from 'shared/spec/mocks/store';

import getState from '~/spec/fixtures/state';
import {PidgetCore, createPidgetId} from '~/pidgets/root';
import {convergeReducers} from '~/utils/reducers';
import {pidgetsReducer} from '~/reducers/pidgets';
import rootReducer, {dummyReducer} from '~/reducers';
import {LevitanProvider} from '~/containers/LevitanProvider';
import {mockStore as screenshotMockStore} from '~/spec/screenshotHelpers';
import {PIDGET_MOUNT} from '~/actions/pidgets';

import type {MockStateOptions} from '~/spec/fixtures/state';
import type {PidgetComponent, PidgetId} from '~/pidgets/root';

type InnerPidgetState<S> = {[key: string]: S};

type InnerPidgetStates<S> = {
    [key: string]: InnerPidgetState<S>;
};

type InnerPidget<S, P> = {
    Component: PidgetComponent<P>;
    data: Array<{
        props?: P;
        id: PidgetId;
        pidgetState: S;
    }>;
};

export type MountComponentArgs<S, P, I, T> = {
    Component: PidgetComponent<P>;
    props?: P;
    pidgetName?: string;
    pidgetState?: S;
    globalState?: Partial<MockStateOptions>;
    useReducer?: boolean;
    ignorePidgetMount?: boolean;
    isScreenshot?: boolean;
    innerPidgets?: Array<InnerPidget<I, T>>;
    api?: (route: string, params: unknown) => unknown;
    levitanProviderOverrides?: any;
    pageRedusers?: Reducer<any, UnifyActions<AnyEpicAction>>;
};

const pidgetBaseId = createPidgetId('componentTest:fakeId');

export function mountPidgetCommon<S, P = any, I = any, T = any>({
    Component,
    props = {} as P,
    pidgetName = Component.displayName,
    pidgetState,
    globalState = {},
    useReducer = false,
    ignorePidgetMount = false,
    isScreenshot = false,
    innerPidgets = [],
    api,
    levitanProviderOverrides = {},
    pageRedusers,
}: MountComponentArgs<S, P, I, T>) {
    if (!pidgetName) {
        throw new Error('mountComponent has required prop pidgetName');
    }

    const getPidgetId = (name?: string) => `/${name}_${pidgetBaseId}`;

    // @ts-expect-error Ожидаем ошибку, для части экшенов payload обязательный, для других - нет
    const reducer = combineReducers({
        ...rootReducer,
        page: pageRedusers || dummyReducer,
        pidgets: convergeReducers(pidgetsReducer, PidgetCore.getReducer()),
    }) as Reducer<AnyAppState>;

    let innerPidgetStates: InnerPidgetStates<I> = {};

    // Позволяет задавать id и state пиджетам, вложеным в пиджеты
    if (innerPidgets.length) {
        innerPidgetStates = innerPidgets.reduce((acc, innerPidget) => {
            acc[innerPidget.Component.displayName as string] = innerPidget.data.reduce(
                (innerAcc, {id, pidgetState: innerPidgetState}) => {
                    innerAcc[
                        `${getPidgetId(pidgetName)}${`/${innerPidget.Component.displayName}_${id}`}`
                    ] = innerPidgetState;

                    return innerAcc;
                },
                {} as InnerPidgetState<I>,
            );
            return acc;
        }, {} as InnerPidgetStates<I>);
    }

    const storeObj = {
        state: getState({
            ...globalState,
            pidgets: {
                [pidgetName]: {
                    [getPidgetId(pidgetName)]: pidgetState,
                },
                ...innerPidgetStates,
                ...globalState.pidgets,
            },
        }),
        epics: isScreenshot ? [] : PidgetCore.getEpics(),
        reducer: useReducer ? reducer : undefined,
        api,
    };
    const store = isScreenshot ? screenshotMockStore(storeObj) : mockStore(storeObj);

    /**
     *  Если передан параметр "ignorePidgetMount" === true - игонорируем экшен PIDGET_MOUNT,
     *  чтобы при инициализации пиджета не переопределялся стейт, который мы передали в "pidgetState".
     *  Имеет смысл использовать "ignorePidgetMount", только вместе с "useReducer".
     */
    if (ignorePidgetMount) {
        const {dispatch} = store;
        store.dispatch = <A extends AnyAction>(action: A) => {
            return action.type === PIDGET_MOUNT ? ({} as A) : dispatch(action);
        };
    }

    const pidget = (
        <Provider store={store}>
            <LevitanProvider>
                <Registry.RegistryProvider {...levitanProviderOverrides}>
                    <Component {...props} $id={pidgetBaseId} />
                </Registry.RegistryProvider>
            </LevitanProvider>
        </Provider>
    );

    return {store, pidget};
}
