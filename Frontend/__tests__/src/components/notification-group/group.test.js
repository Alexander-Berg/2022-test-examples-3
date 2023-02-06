import React from 'react';
import { mount } from 'enzyme';

import { withRegistry } from '@bem-react/di';

import registry from '../../../../src/components/registry';

import GroupBase from '../../../../src/components/notification-group/index';
import ExpandButton from '../../../../src/components/expand-button';
import NotificationCenterContext from '../../../../src/components/notification-center/context';

const Group = withRegistry(registry)(GroupBase);

describe('Notification group', () => {
    const getState = () => ({ config: { logging: { exp_boxes: '123' } } });
    const dispatch = jest.fn(() => Promise.resolve());
    const groupKey = '2today12';
    let state = {}; // state from notification-list

    const _toggleCollapsed = groupKey =>  {
        return () => {
            const { collapsed, ...rest } = state[groupKey] || {};
            state = { ...state, [groupKey]: { collapsed: false, ...rest } };
        };
    };

    const context = {
        store: { getState, dispatch },
        metrika: { countClick: jest.fn(), countActions: jest.fn() },
    };

    const items = [
        <div key="item-1" className="item">an apple</div>,
        <div key="item-2" className="item">a banana</div>,
        <div key="item-3" className="item">a cigarette</div>,
    ];

    test('should render a button', () => {
        const component = mount(
            <Group
                items={ items }
                moreText="and also an egg"
                serviceData={{ icon_src: '//yandex.ru' }} />
        );

        expect(component.html()).toMatchSnapshot();
    });

    test('should pass text to a button', () => {
        const component = mount(
            <Group
                items={ items }
                moreText="and also an egg"
                serviceData={{ icon_src: '//yandex.ru' }} />
        );

        expect(component.find('.gnc-expand-button__text').text())
            .toBe('and also an egg');
    });

    test('should call handler on ExpandButton click', () => {
        jest.useFakeTimers();
        state = { '2today12': { collapsed: true } };
        const clckCounter = jest.fn();
        const clckContext = Object.assign({}, context, { clckCounter });

        const component = mount(
            <NotificationCenterContext.Provider value={ clckContext }>
                <Group
                    groupKey={groupKey}
                    toggleCollapsed={_toggleCollapsed(groupKey)}
                    isCollapsed={state[groupKey].collapsed}
                    items={ items }
                    moreText="and also an egg"
                    service="collections"
                    serviceData={{ icon_src: '//yandex.ru' }} />
            </NotificationCenterContext.Provider>
        );

        expect(state[groupKey].collapsed).toBe(true);

        component
            .find(ExpandButton)
            .find('.gnc-expand-button__content')
            .simulate('click');

        jest.runAllTimers();

        expect(clckCounter).toBeCalledTimes(1);
        expect(clckCounter).lastCalledWith({
            path: '3027.241.353',
            cid: 73327,
            vars: {
                block_id: groupKey,
                exp_boxes: '123',
                height: 0,
                position: 1,
                service: 'collections',
                type: 'group_3',
            },
        });

        expect(state[groupKey].collapsed).toBe(false);

        jest.clearAllTimers();
    });

    test('should call updateListLayoutAfterExpand on ExpandButton click', () => {
        jest.useFakeTimers();
        state = { '2today12': { collapsed: true } };
        const updateListLayoutAfterExpand = jest.fn();
        const clckCounter = jest.fn();
        const clckContext = Object.assign({}, context, { clckCounter });

        const component = mount(
            <NotificationCenterContext.Provider value={ clckContext }>
                <Group
                    updateListLayoutAfterExpand={updateListLayoutAfterExpand}
                    groupKey={groupKey}
                    toggleCollapsed={_toggleCollapsed(groupKey)}
                    isCollapsed={state[groupKey].collapsed}
                    items={ items }
                    moreText="and also an egg"
                    hidden={10}
                    service="collections"
                    serviceData={{ icon_src: '//yandex.ru' }} />
            </NotificationCenterContext.Provider>
        );

        component
            .find(ExpandButton)
            .find('.gnc-expand-button__content')
            .simulate('click');

        jest.runAllTimers();

        expect(updateListLayoutAfterExpand).toBeCalledTimes(1);
        expect(updateListLayoutAfterExpand).lastCalledWith(groupKey);

        jest.clearAllTimers()
    });
});
