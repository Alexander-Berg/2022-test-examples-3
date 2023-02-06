/**
 * @jest-environment jsdom
 */

import React from 'react';
import {shallow, mount} from 'enzyme';

import TabBar from '../TabBar';
import {LoadType} from '../constants';
import styles from '../TabBar/styles.module.css';

jest.mock('react-router-dom', () => ({
    useHistory: () => ({}),
    useLocation: () => ({
        search: '?inJest=true',
    }),
}));

const observe = jest.fn();
const unobserve = jest.fn();
const disconnect = jest.fn();
const takeRecords = jest.fn();

window.IntersectionObserver = jest.fn(() => ({
    disconnect,
    root: null,
    rootMargin: '10px',
    takeRecords,
    thresholds: [],
    observe,
    unobserve,
}));

describe('TabBar', () => {
    it('do not crash without tabs', () => {
        const render = () => shallow(<TabBar tabs={[]} />);

        expect(render).not.toThrowError();
    });

    it('renders first tab', () => {
        const wrapper = shallow(
            <TabBar
                tabs={[
                    {id: 1, label: 'Tab 1', content: 'Content of tab 1'},
                    {id: 2, label: 'Tab 2', content: 'Content of tab 2'},
                ]}
            />
        );

        expect(wrapper.findWhere(w => w.text() === 'Content of tab 1' && w.type() === 'div')).toHaveLength(1);
        expect(wrapper.findWhere(w => w.text() === 'Content of tab 2')).toHaveLength(0);
    });

    it('renders default tab', () => {
        const wrapper = shallow(
            <TabBar
                tabs={[
                    {id: 1, label: 'Tab 1', content: 'Content of tab 1'},
                    {
                        id: 2,
                        label: 'Tab 2',
                        content: 'Content of tab 2',
                        defaultSelected: true,
                    },
                ]}
            />
        );

        expect(wrapper.findWhere(w => w.text() === 'Content of tab 1')).toHaveLength(0);
        expect(wrapper.findWhere(w => w.text() === 'Content of tab 2' && w.type() === 'div')).toHaveLength(1);
    });

    it('renders the default tab correctly when more one default tabs were provided', () => {
        const wrapper = shallow(
            <TabBar
                tabs={[
                    {id: 1, label: 'Tab 1', content: 'Content of tab 1'},
                    {
                        id: 2,
                        label: 'Tab 2',
                        content: 'Content of tab 2',
                        defaultSelected: true,
                    },
                    {
                        id: 3,
                        label: 'Tab 3',
                        content: 'Content of tab 3',
                        defaultSelected: true,
                    },
                ]}
            />
        );

        expect(wrapper.findWhere(w => w.text() === 'Content of tab 1')).toHaveLength(0);
        expect(wrapper.findWhere(w => w.text() === 'Content of tab 2' && w.type() === 'div')).toHaveLength(1);
        expect(wrapper.findWhere(w => w.text() === 'Content of tab 3')).toHaveLength(0);
    });

    it('switches tab', () => {
        const wrapper = mount(
            <TabBar
                tabs={[
                    {id: 1, label: 'Tab 1', content: 'Content of tab 1'},
                    {id: 2, label: 'Tab 2', content: 'Content of tab 2'},
                    {id: 3, label: 'Tab 3', content: 'Content of tab 3'},
                ]}
            />
        );

        expect(wrapper.findWhere(w => w.text() === 'Content of tab 1' && w.type() === 'div')).toHaveLength(1);
        expect(wrapper.findWhere(w => w.text() === 'Content of tab 2')).toHaveLength(0);
        expect(wrapper.findWhere(w => w.text() === 'Content of tab 3')).toHaveLength(0);

        wrapper.findWhere(w => w.text() === 'Tab 3' && w.type() === 'button').simulate('click');

        expect(wrapper.findWhere(w => w.text() === 'Content of tab 1')).toHaveLength(0);
        expect(wrapper.findWhere(w => w.text() === 'Content of tab 2')).toHaveLength(0);
        expect(wrapper.findWhere(w => w.text() === 'Content of tab 3' && w.type() === 'div')).toHaveLength(1);
    });

    it('works with React Element as a tab content', () => {
        const TabContentComponent = () => (
            <div>
                <p>Content of tab 1</p>
            </div>
        );

        const wrapper = shallow(<TabBar tabs={[{id: 1, label: 'Tab 1', content: <TabContentComponent />}]} />);

        expect(wrapper.find(TabContentComponent)).toHaveLength(1);
    });

    it('works with React Element as a tab label', () => {
        const TabLabelComponent = () => <span>Tab 1</span>;

        const wrapper = shallow(
            <TabBar
                tabs={[
                    {
                        id: 1,
                        label: <TabLabelComponent />,
                        content: 'Content of tab 1',
                    },
                ]}
            />
        );

        expect(wrapper.find(TabLabelComponent)).toHaveLength(1);
    });

    it('works with custom wrappers', () => {
        const ProperButtonsWrapper = ({children}) => <div>{children}</div>;
        const ProperContentWrapper = ({children}) => <div>{children}</div>;

        const wrapper = shallow(
            <TabBar
                tabs={[
                    {id: 1, label: 'Tab 1', content: 'Content of tab 1'},
                    {id: 2, label: 'Tab 2', content: 'Content of tab 2'},
                ]}
                renderButtons={buttons => <ProperButtonsWrapper>{buttons}</ProperButtonsWrapper>}
                renderContent={activeTab => (
                    <ProperContentWrapper>{activeTab ? activeTab.content : null}</ProperContentWrapper>
                )}
            />
        );

        expect(wrapper.find(ProperButtonsWrapper)).toHaveLength(1);
        expect(wrapper.find(ProperContentWrapper)).toHaveLength(1);
    });

    describe('lazy loading tabs', () => {
        const wrapper = mount(
            <TabBar
                tabs={[
                    {
                        id: 1,
                        label: 'Tab 1',
                        content: 'Content of tab 1',
                        loadType: LoadType.LAZY,
                    },
                    {
                        id: 2,
                        label: 'Tab 2',
                        content: 'Content of tab 2',
                        loadType: LoadType.LAZY,
                    },
                    {
                        id: 3,
                        label: 'Tab 3',
                        content: 'Content of tab 3',
                        loadType: LoadType.LAZY,
                    },
                ]}
            />
        );

        test('1 of 3 lazy tabs rendered', () => {
            expect(wrapper.findWhere(w => w.text() === 'Content of tab 1' && w.type() === 'div')).toHaveLength(1);

            expect(wrapper.findWhere(w => w.text() === 'Content of tab 2' && w.type() === 'div')).toHaveLength(0);

            expect(wrapper.findWhere(w => w.text() === 'Content of tab 3' && w.type() === 'div')).toHaveLength(0);
        });

        test('2 of 3 lazy tabs rendered', () => {
            wrapper.findWhere(w => w.text() === 'Tab 3' && w.type() === 'button').simulate('click');

            expect(
                wrapper.findWhere(
                    w => w.text() === 'Content of tab 1' && w.type() === 'div' && w.hasClass(styles.hiddenTab)
                )
            ).toHaveLength(1);

            expect(wrapper.findWhere(w => w.text() === 'Content of tab 2' && w.type() === 'div')).toHaveLength(0);

            expect(wrapper.findWhere(w => w.text() === 'Content of tab 3' && w.type() === 'div')).toHaveLength(1);
        });

        test('3 of 3 lazy tabs rendered', () => {
            wrapper.findWhere(w => w.text() === 'Tab 2' && w.type() === 'button').simulate('click');

            expect(
                wrapper.findWhere(
                    w => w.text() === 'Content of tab 1' && w.type() === 'div' && w.hasClass(styles.hiddenTab)
                )
            ).toHaveLength(1);

            expect(wrapper.findWhere(w => w.text() === 'Content of tab 2' && w.type() === 'div')).toHaveLength(1);

            expect(
                wrapper.findWhere(
                    w => w.text() === 'Content of tab 3' && w.type() === 'div' && w.hasClass(styles.hiddenTab)
                )
            ).toHaveLength(1);
        });
    });

    test('render eager tabs', () => {
        const wrapper = shallow(
            <TabBar
                tabs={[
                    {
                        id: 1,
                        label: 'Tab 1',
                        content: 'Content of tab 1',
                        loadType: LoadType.PRELOAD,
                    },
                    {
                        id: 2,
                        label: 'Tab 2',
                        content: 'Content of tab 2',
                        loadType: LoadType.PRELOAD,
                    },
                    {
                        id: 3,
                        label: 'Tab 3',
                        content: 'Content of tab 3',
                        loadType: LoadType.PRELOAD,
                    },
                ]}
            />
        );

        expect(wrapper.findWhere(w => w.text() === 'Content of tab 1' && w.type() === 'div')).toHaveLength(1);

        expect(
            wrapper.findWhere(
                w => w.text() === 'Content of tab 2' && w.type() === 'div' && w.hasClass(styles.hiddenTab)
            )
        ).toHaveLength(1);

        expect(
            wrapper.findWhere(
                w => w.text() === 'Content of tab 3' && w.type() === 'div' && w.hasClass(styles.hiddenTab)
            )
        ).toHaveLength(1);
    });

    describe('variable load type tabs', () => {
        const wrapper = mount(
            <TabBar
                tabs={[
                    {
                        id: 1,
                        label: 'Tab 1',
                        content: 'Content of tab 1',
                        loadType: LoadType.REFRESH,
                    },
                    {
                        id: 2,
                        label: 'Tab 2',
                        content: 'Content of tab 2',
                        loadType: LoadType.PRELOAD,
                    },
                    {
                        id: 3,
                        label: 'Tab 3',
                        content: 'Content of tab 3',
                        loadType: LoadType.LAZY,
                    },
                ]}
            />
        );

        test('render refresh tab and eager tab', () => {
            expect(wrapper.findWhere(w => w.text() === 'Content of tab 1' && w.type() === 'div')).toHaveLength(1);

            expect(
                wrapper.findWhere(
                    w => w.text() === 'Content of tab 2' && w.type() === 'div' && w.hasClass(styles.hiddenTab)
                )
            ).toHaveLength(1);

            expect(
                wrapper.findWhere(
                    w => w.text() === 'Content of tab 3' && w.type() === 'div' && w.hasClass(styles.hiddenTab)
                )
            ).toHaveLength(0);
        });

        test('render eager tab and lazy tab after change tab to tab with lazy content', () => {
            wrapper.findWhere(w => w.text() === 'Tab 3' && w.type() === 'button').simulate('click');

            expect(wrapper.findWhere(w => w.text() === 'Content of tab 1' && w.type() === 'div')).toHaveLength(0);

            expect(
                wrapper.findWhere(
                    w => w.text() === 'Content of tab 2' && w.type() === 'div' && w.hasClass(styles.hiddenTab)
                )
            ).toHaveLength(1);

            expect(wrapper.findWhere(w => w.text() === 'Content of tab 3' && w.type() === 'div')).toHaveLength(1);
        });

        test('render eager tab and lazy tab after change tab to tab with eager content', () => {
            wrapper.findWhere(w => w.text() === 'Tab 2' && w.type() === 'button').simulate('click');

            expect(wrapper.findWhere(w => w.text() === 'Content of tab 1' && w.type() === 'div')).toHaveLength(0);

            expect(wrapper.findWhere(w => w.text() === 'Content of tab 2' && w.type() === 'div')).toHaveLength(1);

            expect(
                wrapper.findWhere(
                    w => w.text() === 'Content of tab 3' && w.type() === 'div' && w.hasClass(styles.hiddenTab)
                )
            ).toHaveLength(1);
        });

        test('render eager tab, lazy tab, refresh tab', () => {
            wrapper.findWhere(w => w.text() === 'Tab 1' && w.type() === 'button').simulate('click');

            expect(wrapper.findWhere(w => w.text() === 'Content of tab 1' && w.type() === 'div')).toHaveLength(1);

            expect(
                wrapper.findWhere(
                    w => w.text() === 'Content of tab 2' && w.type() === 'div' && w.hasClass(styles.hiddenTab)
                )
            ).toHaveLength(1);

            expect(
                wrapper.findWhere(
                    w => w.text() === 'Content of tab 3' && w.type() === 'div' && w.hasClass(styles.hiddenTab)
                )
            ).toHaveLength(1);
        });
    });
});
