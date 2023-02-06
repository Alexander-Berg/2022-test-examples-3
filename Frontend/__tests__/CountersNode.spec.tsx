import * as React from 'react';
import { mount } from 'enzyme';

import { CountersNode } from '../CountersNode';

interface IComponentProps {
    action?: (arg1: string, arg2: string) => void;
    action2?: (arg1: string, arg2: string) => void;
}

describe('HOC Counters', () => {
    describe('CountersNode', () => {
        it('Рендерит компонент, отправляет цель, вызывает функцию', () => {
            let callAction = () => {};

            const Component: React.FC<IComponentProps> = ({ action }) => {
                callAction = () => action && action('test', 'wow');
                return null;
            };

            const metrikaHit = jest.fn();
            const metrikaReachGoal = jest.fn();
            const action = jest.fn();
            const action2 = jest.fn();

            const originalProps = { action, action2 };

            const page = mount(
                <CountersNode<IComponentProps>
                    metrikaHit={metrikaHit}
                    metrikaReachGoal={metrikaReachGoal}
                    declaration={{
                        metrika: {
                            action: {
                                goal: 'foo',
                                params: { bar: 1 },
                            },
                        },
                    }}
                    originalProps={originalProps}
                    render={extraProps => <Component {...originalProps} {...extraProps} />}
                />
            );

            expect(page.find(Component).length).toBe(1);
            expect(metrikaHit.mock.calls.length).toBe(0);
            expect(metrikaReachGoal.mock.calls.length).toBe(0);
            expect(action.mock.calls.length).toBe(0);
            expect(action2.mock.calls.length).toBe(0);

            callAction();

            expect(metrikaHit.mock.calls.length).toBe(0);
            expect(metrikaReachGoal.mock.calls.length).toBe(1);
            expect(metrikaReachGoal.mock.calls[0]).toEqual(['foo', { bar: 1 }]);
            expect(action.mock.calls.length).toBe(1);
            expect(action.mock.calls[0]).toEqual(['test', 'wow']);
            expect(action2.mock.calls.length).toBe(0);
        });

        it('Рендерит компонент, отправляет цель', () => {
            let callAction = () => {};

            const Component: React.FC<IComponentProps> = ({ action }) => {
                callAction = () => action && action('test', 'wow');
                return null;
            };

            const metrikaHit = jest.fn();
            const metrikaReachGoal = jest.fn();
            const action2 = jest.fn();

            const originalProps = { action2 };

            const page = mount(
                <CountersNode<IComponentProps>
                    metrikaHit={metrikaHit}
                    metrikaReachGoal={metrikaReachGoal}
                    declaration={{
                        metrika: {
                            action: {
                                goal: 'foo',
                                params: { bar: 1 },
                            },
                        },
                    }}
                    originalProps={originalProps}
                    render={extraProps => <Component {...originalProps} {...extraProps} />}
                />
            );

            expect(page.find(Component).length).toBe(1);
            expect(metrikaHit.mock.calls.length).toBe(0);
            expect(metrikaReachGoal.mock.calls.length).toBe(0);
            expect(action2.mock.calls.length).toBe(0);

            callAction();

            expect(metrikaHit.mock.calls.length).toBe(0);
            expect(metrikaReachGoal.mock.calls.length).toBe(1);
            expect(metrikaReachGoal.mock.calls[0]).toEqual(['foo', { bar: 1 }]);
            expect(action2.mock.calls.length).toBe(0);
        });

        it('Рендерит компонент, вызывает функцию', () => {
            let callAction2 = () => {};

            const Component: React.FC<IComponentProps> = ({ action2 }) => {
                callAction2 = () => action2 && action2('test', 'wow');
                return null;
            };

            const metrikaHit = jest.fn();
            const metrikaReachGoal = jest.fn();
            const action = jest.fn();
            const action2 = jest.fn();

            const originalProps = { action, action2 };

            const page = mount(
                <CountersNode<IComponentProps>
                    metrikaHit={metrikaHit}
                    metrikaReachGoal={metrikaReachGoal}
                    declaration={{
                        metrika: {
                            action: {
                                goal: 'foo',
                                params: { bar: 1 },
                            },
                        },
                    }}
                    originalProps={originalProps}
                    render={extraProps => <Component {...originalProps} {...extraProps} />}
                />
            );

            expect(page.find(Component).length).toBe(1);
            expect(metrikaHit.mock.calls.length).toBe(0);
            expect(metrikaReachGoal.mock.calls.length).toBe(0);
            expect(action.mock.calls.length).toBe(0);
            expect(action2.mock.calls.length).toBe(0);

            callAction2();

            expect(metrikaHit.mock.calls.length).toBe(0);
            expect(metrikaReachGoal.mock.calls.length).toBe(0);
            expect(action.mock.calls.length).toBe(0);
            expect(action2.mock.calls.length).toBe(1);
            expect(action2.mock.calls[0]).toEqual(['test', 'wow']);
        });

        it('Рендерит компонент', () => {
            let callAction2 = () => {};

            const Component: React.FC<IComponentProps> = ({ action2 }) => {
                callAction2 = () => action2 && action2('test', 'wow');
                return null;
            };

            const metrikaHit = jest.fn();
            const metrikaReachGoal = jest.fn();
            const action = jest.fn();

            const originalProps = { action };

            const page = mount(
                <CountersNode<IComponentProps>
                    metrikaHit={metrikaHit}
                    metrikaReachGoal={metrikaReachGoal}
                    declaration={{
                        metrika: {
                            action: {
                                goal: 'foo',
                                params: { bar: 1 },
                            },
                        },
                    }}
                    originalProps={originalProps}
                    render={extraProps => <Component {...originalProps} {...extraProps} />}
                />
            );

            expect(page.find(Component).length).toBe(1);
            expect(metrikaHit.mock.calls.length).toBe(0);
            expect(metrikaReachGoal.mock.calls.length).toBe(0);
            expect(action.mock.calls.length).toBe(0);

            callAction2();

            expect(metrikaHit.mock.calls.length).toBe(0);
            expect(metrikaReachGoal.mock.calls.length).toBe(0);
            expect(action.mock.calls.length).toBe(0);
        });
    });
});
