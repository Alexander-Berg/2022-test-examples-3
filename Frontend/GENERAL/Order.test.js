import React from 'react';
import { shallow } from 'enzyme';

import { Order } from './Order';

describe('Render', () => {
    it('Should render order with default data', () => {
        const wrapper = shallow(
            <Order
                lists={{}}
                onReorder={jest.fn()}
                order={[]}
                missingOrders={[]}
                createOnDeletePerson={jest.fn()}
                createOnReturnPerson={jest.fn()}
                viewValue={'on-duty'}
                onViewChange={jest.fn()}
                viewItems={[{ val: 'on-duty' }, { val: 'not-on-duty' }]}
                ordersLength={0}
                activeOrders={[]}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render order on on-duty mode', () => {
        const wrapper = shallow(
            <Order
                lists={{
                    ['0']: {
                        max: 2,
                        min: 2,
                        items: [
                            {
                                id: 2,
                                login: 'user2',
                                name: { ru: 'Имя2 Фамилия2', en: 'Name2 Surname2' },
                            },
                            {
                                id: 3,
                                login: 'user3',
                                name: { ru: 'Имя3 Фамилия3', en: 'Name3 Surname3' },
                            }],
                    },
                    ['1']: {
                        max: 2,
                        min: 2,
                        items: [
                            {
                                id: 4,
                                login: 'user4',
                                name: { ru: 'Имя4 Фамилия4', en: 'Name4 Surname4' },
                            },
                        ],
                    },
                }}
                onReorder={jest.fn()}
                order={['0', '1']}
                missingOrders={[]}
                createOnDeletePerson={jest.fn()}
                createOnReturnPerson={jest.fn()}
                viewValue={'on-duty'}
                onViewChange={jest.fn()}
                viewItems={[{ val: 'on-duty' }, { val: 'not-on-duty' }]}
                ordersLength={3}
                activeOrders={[
                    {
                        id: 0,
                        login: 'user0',
                        name: { ru: 'Имя0 Фамилия0', en: 'Name0 Surname0' },
                    },
                ]}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render order on on-not-duty mode', () => {
        const wrapper = shallow(
            <Order
                lists={{
                    ['0']: {
                        max: 2,
                        min: 2,
                        items: [
                            {
                                id: 2,
                                login: 'user2',
                                name: { ru: 'Имя2 Фамилия2', en: 'Name2 Surname2' },
                            },
                            {
                                id: 3,
                                login: 'user3',
                                name: { ru: 'Имя3 Фамилия3', en: 'Name3 Surname3' },
                            }],
                    },
                    ['1']: {
                        max: 2,
                        min: 2,
                        items: [
                            {
                                id: 4,
                                login: 'user4',
                                name: { ru: 'Имя4 Фамилия4', en: 'Name4 Surname4' },
                            },
                        ],
                    },
                }}
                onReorder={jest.fn()}
                order={['0', '1']}
                missingOrders={[
                    {
                        id: 5,
                        login: 'user5',
                        name: { ru: 'Имя5 Фамилия5', en: 'Name5 Surname5' },
                    },
                ]}
                createOnDeletePerson={jest.fn()}
                createOnReturnPerson={jest.fn()}
                viewValue={'on-not-duty'}
                onViewChange={jest.fn()}
                viewItems={[{ val: 'on-duty' }, { val: 'not-on-duty' }]}
                ordersLength={3}
                activeOrders={[]}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
