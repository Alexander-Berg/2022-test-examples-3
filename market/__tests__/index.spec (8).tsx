import React from 'react';
import {shallow} from 'enzyme';

import Dropdown from '..';

describe('Dropdown', () => {
    it('рендер без ошибок', () => {
        const wrapped = () => shallow(<Dropdown options={[]} onSelectOption={() => {}} />);

        expect(wrapped).not.toThrowError();
    });
});
