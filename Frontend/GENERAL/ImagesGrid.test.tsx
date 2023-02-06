import React from 'react';
import { render } from 'enzyme';
import { ImagesGrid } from './ImagesGrid';

describe('ImagesGrid', () => {
    test('(snapshot)', () => {
        expect(render(<ImagesGrid />)).toMatchSnapshot();
    });
});
