import * as React from 'react';
import { createStore, Action } from 'redux';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { mount } from 'enzyme';
import { act } from 'react-dom/test-utils';

import { ICountersPageComponentProps } from '../types';
import { CountersPage } from '../CountersPage';

interface ITestState {
    meta: {
        pageUrls: Record<string, string>;
    };
}

const pageUrl = '/turbo?text=ymturbo.t-dir.com%2Fyandexturbocatalog%2F';
const initialState: ITestState = {
    meta: {
        pageUrls: {
            [pageUrl]: pageUrl,
        },
    },
};
const historyEntries = [{
    pathname: '/turbo',
    search: '?text=ymturbo.t-dir.com%2Fyandexturbocatalog%2F',
    key: pageUrl,
}];

describe('HOC Counters', () => {
    describe('CountersPage', () => {
        it('Рендерит компонент, отправляет хит, вызывает onEntered', () => {
            let callOnEntered = () => {};

            const Component: React.FC<ICountersPageComponentProps> = ({ onEntered }) => {
                callOnEntered = () => onEntered && onEntered();
                return null;
            };

            const store = createStore(state => state, initialState);
            const metrikaHit = jest.fn();
            const metrikaReachGoal = jest.fn();
            const onEntered = jest.fn();

            const page = mount(
                <Provider store={store}>
                    <MemoryRouter initialEntries={historyEntries}>
                        <CountersPage<ICountersPageComponentProps>
                            metrikaHit={metrikaHit}
                            metrikaReachGoal={metrikaReachGoal}
                            declaration={{ metrika: { foo: 'bar' } }}
                            originalProps={{ onEntered }}
                            render={extraProps => <Component {...extraProps} />}
                        />
                    </MemoryRouter>
                </Provider>
            );

            expect(page.find(Component)).toHaveLength(1);
            expect(metrikaHit.mock.calls).toHaveLength(0);
            expect(metrikaReachGoal.mock.calls).toHaveLength(0);
            expect(onEntered.mock.calls).toHaveLength(0);

            act(() => {
                callOnEntered();
            });

            expect(metrikaHit.mock.calls).toHaveLength(1);
            expect(metrikaHit.mock.calls[0]).toEqual([{ foo: 'bar' }, {}, pageUrl]);
            expect(metrikaReachGoal.mock.calls).toHaveLength(0);
            expect(onEntered.mock.calls).toHaveLength(1);
        });

        it('Рендерит компонент, отправляет хит', () => {
            let callOnEntered = () => {};

            const Component: React.FC<ICountersPageComponentProps> = ({ onEntered }) => {
                callOnEntered = () => onEntered && onEntered();
                return null;
            };

            const store = createStore(state => state, initialState);
            const metrikaHit = jest.fn();
            const metrikaReachGoal = jest.fn();

            const page = mount(
                <Provider store={store}>
                    <MemoryRouter initialEntries={historyEntries}>
                        <CountersPage<ICountersPageComponentProps>
                            metrikaHit={metrikaHit}
                            metrikaReachGoal={metrikaReachGoal}
                            declaration={{ metrika: { foo: 'bar' } }}
                            originalProps={{}}
                            render={extraProps => <Component {...extraProps} />}
                        />
                    </MemoryRouter>
                </Provider>
            );

            expect(page.find(Component)).toHaveLength(1);
            expect(metrikaHit.mock.calls).toHaveLength(0);
            expect(metrikaReachGoal.mock.calls).toHaveLength(0);

            act(() => {
                callOnEntered();
            });

            expect(metrikaHit.mock.calls).toHaveLength(1);
            expect(metrikaHit.mock.calls[0]).toEqual([{ foo: 'bar' }, {}, pageUrl]);
            expect(metrikaReachGoal.mock.calls).toHaveLength(0);
        });

        it('Рендерит компонент, вызывает onEntered', () => {
            let callOnEntered = () => {};

            const Component: React.FC<ICountersPageComponentProps> = ({ onEntered }) => {
                callOnEntered = () => onEntered && onEntered();
                return null;
            };

            const store = createStore(state => state, initialState);
            const metrikaHit = jest.fn();
            const metrikaReachGoal = jest.fn();
            const onEntered = jest.fn();

            const page = mount(
                <Provider store={store}>
                    <MemoryRouter initialEntries={historyEntries}>
                        <CountersPage<ICountersPageComponentProps>
                            metrikaHit={metrikaHit}
                            metrikaReachGoal={metrikaReachGoal}
                            declaration={{}}
                            originalProps={{ onEntered }}
                            render={extraProps => <Component {...extraProps} />}
                        />
                    </MemoryRouter>
                </Provider>
            );

            expect(page.find(Component)).toHaveLength(1);
            expect(metrikaHit.mock.calls).toHaveLength(0);
            expect(metrikaReachGoal.mock.calls).toHaveLength(0);
            expect(onEntered.mock.calls).toHaveLength(0);

            act(() => {
                callOnEntered();
            });

            expect(metrikaHit.mock.calls).toHaveLength(1);
            expect(metrikaReachGoal.mock.calls).toHaveLength(0);
            expect(onEntered.mock.calls).toHaveLength(1);
        });

        it('Рендерит компонент', () => {
            let callOnEntered = () => {};

            const Component: React.FC<ICountersPageComponentProps> = ({ onEntered }) => {
                callOnEntered = () => onEntered && onEntered();
                return null;
            };

            const store = createStore(state => state, initialState);
            const metrikaHit = jest.fn();
            const metrikaReachGoal = jest.fn();

            const page = mount(
                <Provider store={store}>
                    <MemoryRouter initialEntries={historyEntries}>
                        <CountersPage<ICountersPageComponentProps>
                            metrikaHit={metrikaHit}
                            metrikaReachGoal={metrikaReachGoal}
                            declaration={{}}
                            originalProps={{}}
                            render={extraProps => <Component {...extraProps} />}
                        />
                    </MemoryRouter>
                </Provider>
            );

            expect(page.find(Component)).toHaveLength(1);
            expect(metrikaHit.mock.calls).toHaveLength(0);
            expect(metrikaReachGoal.mock.calls).toHaveLength(0);

            act(() => {
                callOnEntered();
            });

            expect(metrikaHit.mock.calls).toHaveLength(1);
            expect(metrikaReachGoal.mock.calls).toHaveLength(0);
        });

        it('Рендерит компонент, вызывает onEntered, отправляет хит после появления pageUrl', () => {
            let callOnEntered = () => {};

            const Component: React.FC<ICountersPageComponentProps> = ({ onEntered }) => {
                callOnEntered = () => onEntered && onEntered();
                return null;
            };

            const store = createStore(
                (state, action: Action & { payload: Record<string, string> }) => {
                    if (!state || action.type !== 'add-page-urls') {
                        return state;
                    }

                    return {
                        ...state,
                        meta: {
                            ...state.meta,
                            pageUrls: {
                                ...state.meta.pageUrls,
                                ...action.payload,
                            },
                        },
                    };
                },
                { meta: { pageUrls: {} } } as typeof initialState,
            );

            const metrikaHit = jest.fn();
            const metrikaReachGoal = jest.fn();
            const onEntered = jest.fn();

            const page = mount(
                <Provider store={store}>
                    <MemoryRouter initialEntries={historyEntries}>
                        <CountersPage<ICountersPageComponentProps>
                            metrikaHit={metrikaHit}
                            metrikaReachGoal={metrikaReachGoal}
                            declaration={{ metrika: { foo: 'bar' } }}
                            originalProps={{ onEntered }}
                            render={extraProps => <Component {...extraProps} />}
                        />
                    </MemoryRouter>
                </Provider>
            );

            expect(page.find(Component)).toHaveLength(1);
            expect(metrikaHit.mock.calls).toHaveLength(0);
            expect(metrikaReachGoal.mock.calls).toHaveLength(0);
            expect(onEntered.mock.calls).toHaveLength(0);

            act(() => {
                callOnEntered();
            });

            expect(metrikaHit.mock.calls).toHaveLength(0);
            expect(metrikaReachGoal.mock.calls).toHaveLength(0);
            expect(onEntered.mock.calls).toHaveLength(1);

            act(() => {
                store.dispatch({ type: 'add-page-urls', payload: { [pageUrl]: pageUrl } });
            });

            expect(metrikaHit.mock.calls).toHaveLength(1);
            expect(metrikaHit.mock.calls[0]).toEqual([{ foo: 'bar' }, {}, pageUrl]);
            expect(metrikaReachGoal.mock.calls).toHaveLength(0);
            expect(onEntered.mock.calls).toHaveLength(1);
        });
    });
});
