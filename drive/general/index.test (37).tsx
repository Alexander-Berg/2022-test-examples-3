import CarListSpinner from './index';
import React from 'react';
import { shallow } from 'enzyme';

describe('CarListSpinnerCarListSpinners test', () => {
    it('Should match snapshot without items in array', () => {
        let component = shallow(<CarListSpinner/>);
        expect(component).toMatchSnapshot();
    });
});