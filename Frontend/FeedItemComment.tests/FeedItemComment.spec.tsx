import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { FeedItemComment } from '../FeedItemComment';

// eslint-disable-next-line mocha/no-global-tests
it('renders without crashing', () => {
    shallow(<FeedItemComment text="Комментарий" comments="20" />);
});
