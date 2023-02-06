import * as React from 'react';
import { mount } from 'enzyme';

import { CountersContext } from '../CountersContext';
import { CountersRoot } from '../CountersRoot';
import { ICountersContext } from '../types';

describe('HOC Counters', () => {
    describe('CountersRoot', () => {
        it('Рендерит компонент и метрику, обновляет контекст', () => {
            const Component: React.FC<ICountersContext> = () => null;
            const Metrika: React.FC = () => null;

            const metrikaHit = jest.fn();
            const metrikaReachGoal = jest.fn();

            const root = mount(
                <CountersRoot
                    metrikaHit={metrikaHit}
                    metrikaReachGoal={metrikaReachGoal}
                    MetrikaComponent={Metrika}
                >
                    <CountersContext.Consumer>
                        {context => context && <Component {...context} />}
                    </CountersContext.Consumer>
                </CountersRoot>
            );

            expect(root.find(Component).length).toBe(1);
            expect(root.find(Component).props()).toEqual({ metrikaHit, metrikaReachGoal });
            expect(root.find(Metrika).length).toBe(1);
            expect(metrikaHit.mock.calls.length).toBe(0);
            expect(metrikaReachGoal.mock.calls.length).toBe(0);
        });
    });
});
