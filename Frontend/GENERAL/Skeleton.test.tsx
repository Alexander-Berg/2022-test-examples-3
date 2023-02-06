import React from 'react';
import { render } from 'enzyme';

import { Skeleton } from '.';

describe('Sketeton', () => {
    test('should render', () => {
        expect(render(<Skeleton />)).toMatchSnapshot();
    });
});
