import * as React from 'react';
import { shallow } from 'enzyme';

import { Bug } from './Bug';

describe('Bug', () => {
    it('Должен корректно отрендериться без параметров', () => {
        expect(shallow(<Bug />)).toMatchSnapshot();
    });
});
