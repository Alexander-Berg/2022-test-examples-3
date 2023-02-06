import * as React from 'react';
import { mount, shallow } from 'enzyme';
import { MarketSelect } from '../MarketSelect';

const props = {
    value: 'aprice',
    options: [
        {
            text: 'по популярности',
            value: 'default',
        },
        {
            text: 'по цене',
            value: 'aprice',
        },
        {
            text: 'по рейтингу и цене',
            value: 'rorp',
        },
        {
            text: 'по размеру скидки',
            value: 'discount_p',
        },
    ],
};

describe('MarketSelect', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketSelect {...props} />);

        expect(wrapper.length).toBe(1);
        expect(wrapper.find('option').length).toBe(props.options.length);
    });

    it('должен вызываться handleValueChange', () => {
        let handleChange = jest.fn();
        let select = mount(
            <MarketSelect
                {...props}
                onChange={handleChange}
            />
        );

        select.find('select').simulate('change', { target: { value: 'discount_p' } });

        expect(handleChange).toHaveBeenCalledTimes(1);
        expect(handleChange).toBeCalledWith('discount_p');
    });
});
