import * as React from 'react';
import { shallow } from 'enzyme';

import { InputText } from '../InputText';

describe('InputText', () => {
    it('должен рендериться без ошибок', () => {
        const wrapper = shallow(<InputText name="sbox" value="test-value" />);

        expect(wrapper.length).toBe(1);
    });

    it('должен вызывать onChange', () => {
        const handleChange = jest.fn();
        const wrapper = shallow(
            <InputText name="sbox" value="test-value" onChange={handleChange} />
        );

        wrapper.find('input').simulate('change', {
            target: {
                name: 'sbox',
                value: 'test',
            },
        });

        expect(handleChange).toBeCalledWith({
            target: {
                name: 'sbox',
                value: 'test',
            },
        });
    });
});
