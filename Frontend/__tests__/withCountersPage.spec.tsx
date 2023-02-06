import * as React from 'react';
import { createStore } from 'redux';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { mount } from 'enzyme';

import { ICountersPageComponentProps } from '../types';
import { CountersContext } from '../CountersContext';
import { withCountersPage } from '../withCountersPage';

interface IComponentProps extends ICountersPageComponentProps {
    foo: string;
    bar: number;
}

const pageUrl = '/turbo?text=ymturbo.t-dir.com%2Fyandexturbocatalog%2F';
const store = createStore(state => state, { meta: { pageUrls: { [pageUrl]: pageUrl } } });
const historyEntries = [{
    pathname: '/turbo',
    search: '?text=ymturbo.t-dir.com%2Fyandexturbocatalog%2F',
    key: pageUrl,
}];

describe('HOC Counters', () => {
    describe('withCountersPage', () => {
        it('Создает HOC для компонента', () => {
            const Component: React.FC<IComponentProps> = () => null;
            const metrikaHit = () => {};
            const metrikaReachGoal = () => {};
            const onEntered = () => {};

            const countersHoc = withCountersPage<IComponentProps>({ metrika: { foo: 'bar' } });
            const WrappedComponent = countersHoc(Component);

            expect(mount(
                <Provider store={store}>
                    <MemoryRouter initialEntries={historyEntries}>
                        <CountersContext.Provider value={{ metrikaHit, metrikaReachGoal }}>
                            <WrappedComponent
                                onEntered={onEntered}
                                foo="test"
                                bar={4}
                            />
                        </CountersContext.Provider>
                    </MemoryRouter>
                </Provider>
            )).toMatchSnapshot();
        });

        it('Оборачивает компонент в CountersPage', () => {
            const Component: React.FC<IComponentProps> = () => null;
            const metrikaHit = () => {};
            const metrikaReachGoal = () => {};
            const onEntered = () => {};

            const WrappedComponent = withCountersPage({ metrika: { foo: 'bar' } }, Component);

            expect(mount(
                <Provider store={store}>
                    <MemoryRouter initialEntries={historyEntries}>
                        <CountersContext.Provider value={{ metrikaHit, metrikaReachGoal }}>
                            <WrappedComponent
                                onEntered={onEntered}
                                foo="test"
                                bar={4}
                            />
                        </CountersContext.Provider>
                    </MemoryRouter>
                </Provider>
            )).toMatchSnapshot();
        });
    });
});
