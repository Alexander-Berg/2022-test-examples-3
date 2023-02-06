import React from 'react';
import renderer from 'react-test-renderer';

import { Menu } from './Menu';
import { cnMenu } from '.';

describe('Menu', () => {
    test('cnMenu', () => {
        expect(cnMenu()).toBe('YpcMenu');
    });

    test('role', () => {
        const tree = renderer
            .create(
                <Menu
                    items={[]}
                    role="listbox"
                />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });

    test('className', () => {
        const tree = renderer
            .create(
                <Menu
                    items={[]}
                    className="Test"
                />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });

    test('tabIndex', () => {
        const tree = renderer
            .create(
                <Menu
                    items={[]}
                    tabIndex={-1}
                />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });

    test('items', () => {
        const tree = renderer
            .create(
                <Menu
                    items={[
                        { id: 'item-test' },
                        { role: 'option' },
                        { disabled: true },
                        { tabIndex: 1 },
                        { disabled: true, tabIndex: 1 },
                        { className: 'Test' },
                        { 'aria-selected': true },
                        { children: 'Test' },
                        { children: <div>Test</div> },
                    ]}
                />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });
});
