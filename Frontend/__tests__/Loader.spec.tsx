import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { Loader } from '../Loader';

it('renders without crashing', () => {
    shallow(<div style={{ height: '200px', width: '100%', position: 'relative' }}><Loader /></div>);
});
