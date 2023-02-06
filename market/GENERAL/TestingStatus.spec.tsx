import React from 'react';
import {shallow} from 'enzyme';

import TestingStatus from '../TestingStatus';

describe('TestingStatus', () => {
    it('рендерится', () => {
        const component = shallow(<TestingStatus startDate="2020-01-20" endDate="2020-01-31" />);

        expect(component.isEmptyRender()).toEqual(false);
    });
});
