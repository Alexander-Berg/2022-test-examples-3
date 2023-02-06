import * as React from 'react';
import { create } from 'react-test-renderer';

import { Title as TitleDesktop } from '../Title@desktop';
import { Title as TitleTouch } from '../Title@touch';

describe('Title', () => {
    describe('Должен корректно отрендериться главный заголовок', () => {
        it('desktop', () => {
            const tree = create(<TitleDesktop>Hello</TitleDesktop>).toJSON();

            expect(tree).toMatchSnapshot();
        });

        it('touch', () => {
            const tree = create(<TitleTouch>Hello</TitleTouch>).toJSON();

            expect(tree).toMatchSnapshot();
        });
    });
});
