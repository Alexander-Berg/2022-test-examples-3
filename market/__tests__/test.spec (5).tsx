import React from 'react';
import {shallow} from 'enzyme';
import NumberFormat from 'react-number-format';

import AdaptiveInput from '..';

describe('AdaptiveInput', () => {
    it('рендер только с числами', () => {
        const wrapped = shallow(<AdaptiveInput numeric />);

        expect(wrapped.find(NumberFormat)).toHaveLength(1);
    });
    it('рендер с обычным инпутом', () => {
        const wrapped = shallow(<AdaptiveInput numeric />);

        expect(wrapped.find(NumberFormat)).toHaveLength(1);
    });
});
