import 'jest';
import * as React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';
import createMockStore from 'redux-mock-store';

import { IAppState } from '~/types';
import { getInitialState } from '~/redux/initialState';
import { EcomListFilter } from '../EcomListFilter';
import { EcomListFilterItem, EcomListFilterItemPrice } from '../Item';
import { IProps } from '../EcomListFilter.types';

const getDefaultProps = (): IProps => ({
    hiddenContent: false,
    filterState: {},
    productCounts: 44,
    params: [
        {
            name: 'Сиденье',
            values: [
                {
                    value: 'Не стандартный',
                    id: 1
                },
                {
                    value: 'Стандартный',
                    id: 2
                }
            ],
            id: 1
        },
        {
            name: 'Цвет стула',
            values: [
                {
                    value: 'Белый',
                    id: 1
                },
                {
                    value: 'Вишня',
                    id: 2
                }
            ],
            id: 2
        },
        {
            name: 'Цвет каркаса',
            values: [
                {
                    value: 'Красный',
                    id: 2
                },
                {
                    value: 'Черный',
                    id: 1
                }
            ],
            id: 3,
        }
    ],
    vendors: [
        {
            id: 10,
            value: 'Lorem',
        },
        {
            id: 2,
            value: 'amet ex deserunt proident',
        },
        {
            id: 36,
            value: 'aute ea fugiat eu',
        }
    ]
});

describe('EcomListFilter', () => {
    describe('Эксперимент с главными фильтрами', () => {
        let props: IProps;
        const createStore = createMockStore<IAppState>();
        const state = getInitialState();
        state.meta.expFlags = state.meta.expFlags || {};
        state.meta.expFlags['turbo-app-main-filters-only'] = 1;
        const store = createStore(state);

        beforeEach(() => {
            props = getDefaultProps();
        });

        it('Рендерит только блок цен и брендов', () => {
            const wrapper = mount(
                <Provider store={store}>
                    <EcomListFilter {...props} />
                </Provider>
            );

            const filters = wrapper.find(EcomListFilterItem);
            expect(filters.length).toBe(1);

            const price = wrapper.find(EcomListFilterItemPrice);
            expect(price.length).toBe(1);
        });

        it('Не рендерит блок брендов при их отсутствии', () => {
            props.vendors = undefined;

            const wrapper = mount(
                <Provider store={store}>
                    <EcomListFilter {...props} />
                </Provider>
            );

            const filters = wrapper.find(EcomListFilterItem);
            expect(filters.length).toBe(0);

            const price = wrapper.find(EcomListFilterItemPrice);
            expect(price.length).toBe(1);
        });

        it('Не рендерит блок брендов, если они пустые', () => {
            props.vendors = [];

            const wrapper = mount(
                <Provider store={store}>
                    <EcomListFilter {...props} />
                </Provider>
            );

            const filters = wrapper.find(EcomListFilterItem);
            expect(filters.length).toBe(0);

            const price = wrapper.find(EcomListFilterItemPrice);
            expect(price.length).toBe(1);
        });
    });
});
