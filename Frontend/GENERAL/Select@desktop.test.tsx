import React from 'react';
import renderer from 'react-test-renderer';

import {
    cnSelect,
    SelectDesktop,
} from '.';

describe('SelectDesktop', () => {
    test('cnSelect', () => {
        expect(cnSelect()).toBe('YpcSelect');
    });

    test('value & options', () => {
        const tree = renderer
            .create(
                <SelectDesktop
                    value="v2"
                    options={[
                        { value: 'v1', children: 'Item 1', disabled: true },
                        { value: 'v2', children: (<div>Item 2</div>) },
                        { value: 'v3', children: 'Item 3', id: 'test-id' },
                    ]}
                />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });

    test('onSelectChange', () => {
        const tree = renderer
            .create(
                <SelectDesktop
                    options={[]}
                    onSelectChange={() => {}}
                />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });

    test('buttonProps', () => {
        const tree = renderer
            .create(
                <SelectDesktop
                    options={[]}
                    buttonProps={{
                        children: (
                            <div>Test</div>
                        ),
                    }}
                />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });

    test('className', () => {
        const tree = renderer
            .create(
                <SelectDesktop
                    options={[]}
                    className="Test"
                />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });
});
