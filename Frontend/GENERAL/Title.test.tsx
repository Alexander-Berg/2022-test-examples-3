import * as React from 'react';
import { shallow } from 'enzyme';

import { Title } from './Title@desktop';

describe('Title', () => {
    it('Должен корректно отрендериться главный заголовок', () => {
        expect(shallow(<Title>Hello</Title>)).toMatchSnapshot();
    });
});
