import * as React from 'react';
import { mount } from 'enzyme';

import { CountersContext } from '../CountersContext';
import { withCountersNode } from '../withCountersNode';

interface IComponentProps {
    action?: (arg1: string, arg2: string) => void;
    action2?: (arg1: string, arg2: string) => void;
}

describe('HOC Counters', () => {
    describe('withCountersNode', () => {
        it('Создает HOC для компонента', () => {
            const Component: React.FC<IComponentProps> = () => null;
            const metrikaHit = () => {};
            const metrikaReachGoal = () => {};
            const action = () => {};
            const action2 = () => {};

            const countersHoc = withCountersNode<IComponentProps>({ metrika: { action: { goal: 'foo', params: { bar: 1 } } } });
            const WrappedComponent = countersHoc(Component);

            expect(mount(
                <CountersContext.Provider value={{ metrikaHit, metrikaReachGoal }}>
                    <WrappedComponent
                        action={action}
                        action2={action2}
                    />
                </CountersContext.Provider>
            )).toMatchSnapshot();
        });

        it('Оборачивает компонент в Counters', () => {
            const Component: React.FC<IComponentProps> = () => null;
            const metrikaHit = () => {};
            const metrikaReachGoal = () => {};
            const action = () => {};
            const action2 = () => {};

            const WrappedComponent = withCountersNode({ metrika: { action: { goal: 'foo', params: { bar: 1 } } } }, Component);

            expect(mount(
                <CountersContext.Provider value={{ metrikaHit, metrikaReachGoal }}>
                    <WrappedComponent
                        action={action}
                        action2={action2}
                    />
                </CountersContext.Provider>
            )).toMatchSnapshot();
        });
    });
});
