import { mount } from 'enzyme';
import * as React from 'react';

import { ButtonWithIcon } from './component';

describe('Button with icon', () => {
    it('should match snapshot', () => {
        const component = mount(<ButtonWithIcon icon={'phone'} text={'Test'}/>);
        expect(component).toMatchSnapshot();
    });
});
