import 'jest';
import * as React from 'react';
import { mount, shallow } from 'enzyme';
import { EcomListFilterItem } from '../Item';
import { EcomListFilterItemPrice } from '../Price/ItemPrice';

describe('EcomListFilter', () => {
    describe('Item', () => {
        it('Item без выбранных чекбоксов', () => {
            const onChangeAction = jest.fn();
            const wrapper = mount(<EcomListFilterItem
                onChange={onChangeAction}
                values={[
                    { id: 1, value: '1' },
                    { id: 2, value: '2' },
                    { id: 3, value: '3' },
                    { id: 4, value: '4' },
                    { id: 5, value: '5' },
                    { id: 6, value: '6' },
                    { id: 7, value: '7' },
                    { id: 8, value: '8' },
                ]}
                id="test"
            />);
            expect(wrapper.state('itemList')).toHaveLength(4);
            wrapper.find({ 'data-value': 1 }).simulate('click');
            expect(onChangeAction).toHaveBeenCalledWith('test', ['1']);
        });

        it('Item c предвыбранным чекбоксом', () => {
            const onChangeAction = jest.fn();
            const wrapper = mount(<EcomListFilterItem
                onChange={onChangeAction}
                values={[
                    { id: 1, value: '1' },
                    { id: 2, value: '2' },
                ]}
                state={ ['1'] }
                id="test"
            />);

            wrapper.find({ 'data-value': 2 }).simulate('click');
            expect(onChangeAction).toHaveBeenCalledWith('test', ['1', '2']);
        });

        it('Порядок фильтров устанавливается только на первом рендере', () => {
            const wrapper = shallow(<EcomListFilterItem
                values={[
                    { id: 1, value: '1' },
                    { id: 2, value: '2' },
                    { id: 3, value: '3' },
                    { id: 4, value: '4' },
                    { id: 5, value: '5' },
                    { id: 6, value: '6' },
                    { id: 7, value: '7' },
                    { id: 8, value: '8' },
                ]}
                state={['7', '4', '6']}
                id="test"
            />);
            /** Snapshot первого рендера */
            expect(wrapper).toMatchSnapshot();
            /** Изменяем отмеченные чекбоксы фильтров */
            wrapper.setProps({ state: ['7', '4'] });
            /** Snapshot второго рендера */
            expect(wrapper).toMatchSnapshot();
        });

        it('PriceItem должен вернуть заполненным только одно поле', () => {
            const onChangeAction = jest.fn();
            const wrapper = shallow(<EcomListFilterItemPrice
                onChange={onChangeAction}
                state={ [] }
            />);
            wrapper.find('input[name="from"]')
                .simulate('change', { target: { value: '11', name: 'from' } });
            expect(onChangeAction).toHaveBeenCalledWith('price', [11, null]);
            wrapper.find('input[name="to"]')
                .simulate('change', { target: { value: '22', name: 'to' } });
            expect(onChangeAction).toHaveBeenCalledWith('price', [null, 22]);
        });

        it('PriceItem должен вернуть from и to', () => {
            const onChangeAction = jest.fn();
            const wrapper = shallow(<EcomListFilterItemPrice
                onChange={onChangeAction}
                state={ [10] }
            />);

            wrapper.find('input[name="to"]')
                .simulate('change', { target: { value: '22', name: 'to' } });
            expect(onChangeAction).toHaveBeenCalledWith('price', [10, 22]);
        });

        it('PriceItem должен вернуть пустой массив', () => {
            const onChangeAction = jest.fn();
            const wrapper = mount(<EcomListFilterItemPrice
                onChange={onChangeAction}
                state={ [10, null] }
            />);
            wrapper.find('input[name="from"]')
                .simulate('change', { target: { value: '', name: 'from' } });
            expect(onChangeAction).toHaveBeenCalledWith('price', []);
        });

        it('PriceItem должен обрабатывать цену с пробелами', () => {
            const onChangeAction = jest.fn();
            const wrapper = shallow(<EcomListFilterItemPrice
                onChange={onChangeAction}
            />);

            wrapper.find('input[name="from"]')
                .simulate('change', { target: { value: '100 000', name: 'from' } });
            expect(onChangeAction).toHaveBeenLastCalledWith('price', [100000, null]);

            wrapper.find('input[name="to"]')
                .simulate('change', { target: { value: '100 000 000', name: 'to' } });
            expect(onChangeAction).toHaveBeenLastCalledWith('price', [null, 100000000]);
        });
    });
});
