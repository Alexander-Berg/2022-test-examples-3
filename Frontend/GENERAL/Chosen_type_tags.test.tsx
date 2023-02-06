import React from 'react'; // eslint-disable-line no-unused-vars, @typescript-eslint/no-unused-vars
import { mount } from 'enzyme';
import { ChosenTags } from './Chosen_type_tags';

describe('Chosen_type_tags', () => {
    it('Should render simple component', () => {
        const name = 'test name';
        const color = '#FFFFFF';

        const wrapper = mount(
            <>
                {ChosenTags({ id: '12', name, color })}
            </>,
        );

        // использую hostNodes, потому что ноды дублируются
        // https://github.com/enzymejs/enzyme/issues/1174
        expect(wrapper.find('.Chosen_type_Tags').hostNodes().prop('children')).toEqual(name);
        expect(wrapper.find('.Chosen_type_Tags').hostNodes().prop('style')).toEqual({ backgroundColor: color });
    });
});
