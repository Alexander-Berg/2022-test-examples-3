import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruTumbler } from '../BeruTumbler';

describe('BeruTumbler', () => {
    it('рендерится по умолчанию корректно', () => {
        const wrapper = shallow(<BeruTumbler name="field1" value="value1" />);

        expect(wrapper.hasClass('beru-tumbler_checked')).toEqual(false);
        expect(wrapper.find('.beru-tumbler__checkbox').props()).toMatchObject({
            name: 'field1',
            value: 'value1',
            type: 'checkbox',
        });
    });

    it('рендерится в выбранном состоянии корректно', () => {
        const wrapper = shallow(<BeruTumbler name="field1" value="value1" checked />);

        expect(wrapper.hasClass('beru-tumbler_checked')).toEqual(true);
        expect(wrapper.find('.beru-tumbler__checkbox').props()).toMatchObject({
            name: 'field1',
            value: 'value1',
            type: 'checkbox',
            checked: true,
        });
    });

    it('onChange callback вызывается при клике по тумблеру', () => {
        const onChange = jest.fn();
        const wrapper = shallow(<BeruTumbler name="field1" value="value1" onChange={onChange} checked />);

        wrapper.find('.beru-tumbler__checkbox').simulate('change');

        expect(onChange).toHaveBeenCalled();
    });
});
