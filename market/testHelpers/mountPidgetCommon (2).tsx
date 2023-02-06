import type {Reducer} from 'redux';
import React from 'react';
import Immutable from 'seamless-immutable';
import {Provider} from 'react-redux';
import {createBrowserHistory} from 'history';
import {render} from '@testing-library/react';

import type {State} from 'reducers';
import type {PidgetComponent, PidgetId} from 'pidgets/root';
import {mockStore} from 'spec/mocks';
import {screenshotMockStore} from 'spec/screenshot/screenshotHelpers';
import {PidgetCore, createPidgetId} from 'pidgets/root';
import createRootReducer from 'reducers';

const getState = (initialState = {}) =>
    Immutable.from({
        widgets: {
            currentView: {
                params: {},
            },
        },
    }).merge(initialState, {deep: true});

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

export type MountComponentArgs<S, P, I> = {
    Component: React.ComponentType<P>;
    props?: P;
    pidgetName?: string;
    pidgetState: S;
    globalState?: Partial<State>;
    useReducer?: boolean;
    innerPidgets?: Array<InnerPidget<I, P>>;
    isScreenshot?: boolean;
};

const pidgetBaseId = createPidgetId('componentTest:fakeId');

export function mountPidgetCommon<S, P = any, I = any>({
    Component,
    props = {} as P,
    pidgetName = Component.displayName,
    pidgetState,
    globalState = {},
    useReducer = false,
    innerPidgets = [],
    isScreenshot = false,
}: MountComponentArgs<S, P, I>) {
    if (!pidgetName) {
        throw new Error('mountComponent has required prop pidgetName');
    }

    const getPidgetId = (name?: string) => `/${name}_${pidgetBaseId}`;
    const history = createBrowserHistory();

    const reducer = createRootReducer(history) as Reducer<State>;

    let innerPidgetStates: InnerPidgetStates<I> = {};

    // Позволяет задавать id и state пиджетам, вложеным в пиджеты
    if (innerPidgets.length) {
        innerPidgetStates = innerPidgets.reduce((acc, innerPidget) => {
            acc[innerPidget.Component.displayName as string] = innerPidget.data.reduce(
                (innerAcc, {id, pidgetState: innerPidgetState}) => {
                    innerAcc[`${getPidgetId(pidgetName)}${`/${innerPidget.Component.displayName}_${id}`}`] =
                        innerPidgetState;

                    return innerAcc;
                },
                {} as InnerPidgetState<I>,
            );
            return acc;
        }, {} as InnerPidgetStates<I>);
    }

    const storeObj = {
        state: getState({
            pidgets: {
                [pidgetName]: {
                    [getPidgetId(pidgetName)]: pidgetState,
                },
                ...innerPidgetStates,
            },
            ...globalState,
        }),
        epics: isScreenshot ? [] : PidgetCore.getEpics(),
        reducer: useReducer ? reducer : undefined,
    };

    // @ts-expect-error(TS2345) найдено в рамках VNDFRONT-4532
    const store = isScreenshot ? screenshotMockStore(storeObj) : mockStore(storeObj);

    const Pidget = () => (
        <Provider store={store}>
            <Component {...props} $id={pidgetBaseId} />
        </Provider>
    );

    return {
        store,
        pidget: <Pidget />,
        pidgetForCat: render(<Pidget />),
    };
}
