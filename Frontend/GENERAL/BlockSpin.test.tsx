import React from 'react';

import { shallow } from 'enzyme';
import { BlockSpin } from './BlockSpin';

describe('UI/BlockSpin', () => {
    it('should render default BlockSpin', () => {
        const wrapper = shallow(<BlockSpin spin={false} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render BlockSpin with children', () => {
        const wrapper = shallow(<BlockSpin spin={false}><div>Content</div></BlockSpin>);
        expect(wrapper).toMatchSnapshot();
    });
});
