import * as React from 'react';
import { shallow, mount } from 'enzyme';

import { Select2 } from '../Select2';
import { withRelocate } from '../_relocate/Select2_relocate';

const options = [
    { value: 'grapefruit', text: 'Grapefruit' },
    { value: 'lime', text: 'Lime' },
    { value: 'coconut', text: 'Coconut' },
    { value: 'mango', text: 'Mango' },
];

describe('Select2', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<Select2 name="sbox" label="MyLabel" options={options} />);

        expect(wrapper.length).toBe(1);
        // Label рендерится первым задизейбленым вариантом,
        // чтобы он был по дефолту выбран, если value===undefined
        expect(wrapper.find('option').length).toBe(5);
        expect(wrapper.find('option').get(0).props).toEqual({
            children: 'MyLabel',
            disabled: true,
            value: '',
            className: 'turbo-select2__option',
        });
    });

    it('должен пробрасывать все пропсы', () => {
        const foo = jest.fn();

        const wrapper = mount(
            <Select2
                autoComplete="on"
                className="omg"
                disabled
                name="test"
                onChange={foo}
                options={options}
            />
        );

        expect(wrapper.find('label select').props()).toEqual({
            autoComplete: 'on',
            className: 'turbo-select2__control',
            disabled: true,
            name: 'test',
            value: 'grapefruit',
            onChange: foo,
            children: expect.any(Array),
        });

        const select2 = wrapper.find('.turbo-select2');

        expect(select2.length).toBe(1);
        expect(select2.hasClass('turbo-select2_disabled')).toBe(true);
        expect(select2.hasClass('omg')).toBe(true);
    });

    describe('Relocate', () => {
        beforeEach(() => {
            // @ts-ignore
            global.window.Ya = {
                relocateTurboInTurbo: jest.fn(),
                navigateTurboInTurbo: jest.fn(),
            };
        });

        it('Должен добавлять запись в историю', () => {
            const Select = withRelocate(Select2);

            const wrapper = mount(
                <Select
                    name="test"
                    relocate={{}}
                    options={[
                        { value: '/turbo?text=page/first', text: 'first' },
                        { value: '/turbo?text=page/second', text: 'second' },
                    ]}
                />
            );

            wrapper.find('select').simulate('change', { target: { value: '/turbo?text=page/first' } });
            // @ts-ignore
            expect(global.window.Ya.navigateTurboInTurbo).toHaveBeenCalledWith(
                'https://localhost/turbo?text=page/first',
                'page/first'
            );
        });

        it('Должен подменять запись в истории при relocate.replace', () => {
            const Select = withRelocate(Select2);

            const wrapper = mount(
                <Select
                    name="test"
                    relocate={{ replace: true }}
                    options={[
                        { value: '/turbo?text=page/first', text: 'first' },
                        { value: '/turbo?text=page/second', text: 'second' },
                    ]}
                />
            );

            wrapper.find('select').simulate('change', { target: { value: '/turbo?text=page/first' } });
            // @ts-ignore
            expect(global.window.Ya.relocateTurboInTurbo).toHaveBeenCalledWith(
                'https://localhost/turbo?text=page/first',
                'page/first'
            );
        });
    });
});
