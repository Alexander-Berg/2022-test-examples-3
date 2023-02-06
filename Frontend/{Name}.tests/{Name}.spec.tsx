import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { {Name} } from '../{Name}';

it('renders without crashing', () => {
    shallow(<{Name} />);
});
