import React from 'react';
import renderer from 'react-test-renderer';

import { Icon, names, widths, heights } from './__modules__';
import { cnIcon } from './Icon';

describe('Icon', () => {
    test('cnIcon', () => {
        expect(cnIcon()).toBe('YpcIcon');
    });

    test('children', () => {
        const tree = renderer
            .create(
                <Icon>Text</Icon>
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });

    test('className', () => {
        const tree = renderer
            .create(
                <Icon className="Test" />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });

    describe('name', () => {
        for (const name of names) {
            test(name, () => {
                const tree = renderer
                    .create(
                        <Icon name={name} />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        }
    });

    describe('width', () => {
        for (const width of widths) {
            test(width, () => {
                const tree = renderer
                    .create(
                        <Icon width={width} />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        }
    });

    describe('height', () => {
        for (const height of heights) {
            test(height, () => {
                const tree = renderer
                    .create(
                        <Icon height={height} />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        }
    });
});
