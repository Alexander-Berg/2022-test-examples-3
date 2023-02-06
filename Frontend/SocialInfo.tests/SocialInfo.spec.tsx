import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { SocialInfo } from '../SocialInfo';

// eslint-disable-next-line mocha/no-global-tests
it('renders without crashing', () => {
    shallow(<SocialInfo time={1583491674} shouldHideTime={false} />);
});
