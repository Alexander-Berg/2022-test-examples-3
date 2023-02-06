import React from 'react';

import { shallow } from 'enzyme';
import { IconButton } from './IconButton';
import { IconEdit } from '../../../../static/icons/edit';

describe('UI/IconButton', () => {
    it('should render IconButton with size m', () => {
        const wrapper = shallow(<IconButton icon={IconEdit} size="m">button</IconButton>);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render IconButton with size s', () => {
        const wrapper = shallow(<IconButton icon={IconEdit} size="s">button</IconButton>);
        expect(wrapper).toMatchSnapshot();
    });
});
