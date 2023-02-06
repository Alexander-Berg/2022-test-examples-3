import React from 'react';
import renderer from 'react-test-renderer';

import { Example } from '.';

describe('Example', () => {
    test('baseline snapshot', () => {
        const tree = renderer
            .create(
                <Example
                    headerText="Header Text"
                    creationDate={new Date(2021, 1, 1)}
                    text="Text"
                    color="#333"
                />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });
});
