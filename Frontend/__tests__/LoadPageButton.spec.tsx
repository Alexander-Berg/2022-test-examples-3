import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { LoadPageButton } from '../LoadPageButton';

it('renders without crashing', () => {
    shallow(<LoadPageButton url="" direction="prev" localization={{ showMore: 'Показать ещё' }} />);
});
