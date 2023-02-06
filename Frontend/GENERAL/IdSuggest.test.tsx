import React from 'react';
import renderer from 'react-test-renderer';

import { IdSuggest } from '.';

describe('IdSuggest', () => {
    test('baseline snapshot', () => {
        const tree = renderer
            .create(
                <IdSuggest
                    url="https://example.com"
                />
            )
            .toJSON();

        expect(tree).toMatchSnapshot();
    });
});
