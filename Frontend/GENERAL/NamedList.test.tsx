import React from 'react';

import { shallow } from 'enzyme';
import { NamedList } from './NamedList';
import { NamedListElement } from './NamedListElement';

describe('UI/NamedList', () => {
    it('should render NamedList', () => {
        const wrapper = shallow(<NamedList><div>content</div></NamedList>);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render NamedListElement', () => {
        const wrapper = shallow(<NamedListElement title="Title">Content</NamedListElement>);
        expect(wrapper).toMatchSnapshot();
    });
});
