import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { Alert } from '../Alert';

it('renders without crashing', () => {
    shallow(<Alert visible>Alert!!</Alert>);
});
