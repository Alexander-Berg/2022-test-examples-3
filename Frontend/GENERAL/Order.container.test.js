import React from 'react';
import 'babel-polyfill';
import { mount } from 'enzyme';
import inherit from 'inherit';

import Modal from 'b:modal';
import { OrderContainer } from './Order.container';

inherit.self(Modal, {
    content: () => [],
});

describe('Init', () => {
    it('Should init with default state when no data is provided', () => {
        const wrapper = mount(
            <OrderContainer
                orders={[]}
                missingOrders={[]}
                activeOrders={[]}
                onOrdersChange={() => {}}
                onMissingOrdersChange={() => {}}
                scheduleIndex={0}
            />,
        );

        expect(wrapper.state()).toEqual({
            lists: {},
            view: 'on-duty',
        });

        wrapper.unmount();
    });

    it('Should init with a state with provided data', () => {
        const wrapper = mount(
            <OrderContainer
                orders={[
                    {
                        login: 'user2',
                        name: { ru: 'Имя2 Фамилия2', en: 'Name2 Surname2' },
                    },
                    {
                        login: 'user3',
                        name: { ru: 'Имя3 Фамилия3', en: 'Name3 Surname3' },
                    },
                ]}
                missingOrders={[]}
                activeOrders={[]}
                onOrdersChange={() => {}}
                onMissingOrdersChange={() => {}}
                scheduleIndex={0}
                groupNumber={2}
            />,
        );

        expect(wrapper.state()).toEqual({
            lists: {
                ['0']: {
                    max: 2,
                    min: 2,
                    items: [
                        {
                            login: 'user2',
                            name: { ru: 'Имя2 Фамилия2', en: 'Name2 Surname2' },
                        },
                        {
                            login: 'user3',
                            name: { ru: 'Имя3 Фамилия3', en: 'Name3 Surname3' },
                        },
                    ],
                },
            },
            view: 'on-duty',
        });

        wrapper.unmount();
    });
});

describe('Check radio-button', () => {
    it('Should check radio-button', () => {
        const wrapper = mount(
            <OrderContainer
                orders={[]}
                missingOrders={[]}
                activeOrders={[]}
                onOrdersChange={() => {}}
                onMissingOrdersChange={() => {}}
                scheduleIndex={0}
                groupNumber={2}
            />,
        );

        expect(wrapper.state('view')).toBe('on-duty');

        wrapper.instance()._onViewChange({ target: { value: 'not-on-duty' } });

        expect(wrapper.state('view')).toBe('not-on-duty');

        wrapper.unmount();
    });
});

describe('Should handle removing persons', () => {
    const onOrdersChange = jest.fn();
    const onMissingOrdersChange = jest.fn();

    let wrapper = null;

    beforeEach(() => {
        wrapper = mount(
            <OrderContainer
                orders={[
                    {
                        login: 'user1',
                        name: { ru: 'Имя1 Фамилия1', en: 'Name1 Surname1' },
                    },
                ]}
                missingOrders={[
                    {
                        login: 'user2',
                        name: { ru: 'Имя2 Фамилия2', en: 'Name2 Surname2' },
                    },
                ]}
                activeOrders={[]}
                onOrdersChange={onOrdersChange}
                onMissingOrdersChange={onMissingOrdersChange}
                scheduleIndex={0}
                groupNumber={2}
            />,
        );
    });

    afterEach(() => {
        wrapper.unmount();
    });

    it('Delete person', () => {
        wrapper.instance()._deletePerson('0', 0);

        expect(onOrdersChange).toHaveBeenCalled();
        expect(onMissingOrdersChange).toHaveBeenCalled();
    });

    it('Return person', () => {
        wrapper.instance()._returnPerson(0);

        expect(onOrdersChange).toHaveBeenCalled();
        expect(onMissingOrdersChange).toHaveBeenCalled();
    });
});
