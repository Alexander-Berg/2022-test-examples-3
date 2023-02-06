import * as React from 'react';
import { shallow } from 'enzyme';
import { NewsCollection } from '../NewsCollection@phone';
import * as cardData from '../datastub';

describe('NewsCollection component', () => {
    it('should render without crashing', () => {
        const wrapper = shallow(
            <NewsCollection {...cardData.dataDefault} />
        );
        expect(wrapper.length).toEqual(1);
    });
});
