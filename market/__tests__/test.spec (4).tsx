import React from 'react';
import {shallow} from 'enzyme';

import TokenInput from '..';

describe('TokenInput', () => {
    it('рендер без ошибок', () => {
        expect(() => shallow(<TokenInput onChangeTokens={() => {}} />)).not.toThrowError();
    });
});
