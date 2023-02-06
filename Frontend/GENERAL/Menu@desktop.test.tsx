import React from 'react';
import renderer from 'react-test-renderer';

import { MenuDesktop } from '.';

describe('MenuDesktop', () => {
    test('items', () => {
        const tree = renderer
            .create(
                <MenuDesktop
                    items={[
                        { children: 'Item 1' },
                        { children: 'Item 2', 'aria-selected': true },
                        { children: 'Item 3', disabled: true },
                    ]}
                />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });
});
