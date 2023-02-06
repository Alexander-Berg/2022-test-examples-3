import React from 'react';
import { render } from 'enzyme';

import Icon from 'b:icon m:glyph=download';

const glyphs = [
    'download',
];

describe('Should render icon with glyph', () => {
    glyphs.forEach(glyph => {
        it(glyph, () => {
            const wrapper = render(<Icon glyph={glyph} />);
            expect(wrapper).toMatchSnapshot();
        });
    });
});
