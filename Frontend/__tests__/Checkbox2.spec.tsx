import * as React from 'react';
import { shallow } from 'enzyme';

import { Checkbox2 } from '../Checkbox2';

describe('Checkbox2', () => {
    it('должен рендерится без ошибок', () => {
        let wrapper = shallow(<Checkbox2 name="cbox" checked value="1" />);

        expect(wrapper.length).toBe(1);

        wrapper = shallow(
            <Checkbox2 name="cbox" checked value="1">
                <span>children</span>
            </Checkbox2>
        );

        expect(wrapper.length).toBe(1);

        // TODO: переписать
        // const content = wrapper.find('label .turbo-checkbox2__content');

        // expect(content.html()).toContain('children');
    });

    it('должен пробрасывать все пропсы', () => {
        const foo = jest.fn();
        const wrapper = shallow(
            <Checkbox2
                name="test"
                value="1"
                checked
                className="omg"
                disabled
                onChange={foo}
            />
        );

        expect(wrapper.find('label input').props()).toEqual({
            autoComplete: 'off',
            checked: true,
            className: 'turbo-checkbox2__input',
            disabled: true,
            value: '1',
            name: 'test',
            onChange: foo,
            type: 'checkbox',
        });
        expect(wrapper.hasClass('turbo-checkbox2')).toBeTruthy();
        expect(wrapper.hasClass('turbo-checkbox2_disabled')).toBeTruthy();
        expect(wrapper.hasClass('omg')).toBeTruthy();
    });
});
