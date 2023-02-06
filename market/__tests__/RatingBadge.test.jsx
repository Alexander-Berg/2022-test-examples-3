import React from 'react';
import { create } from 'react-test-renderer';

import RatingBadge from '../index';

describe('RatingBadge', () => {
    test('snapshot: rating 4.5', () => {
        const c = create(<RatingBadge value={4.5} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: rating 5.0', () => {
        const c = create(<RatingBadge value={5.0} />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
