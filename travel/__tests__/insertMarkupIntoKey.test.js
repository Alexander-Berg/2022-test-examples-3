import React from 'react';

import insertMarkupIntoKey from '../insertMarkupIntoKey';

const keyset = jest.fn((key, { param0, param1, param2 }) => `${param0} foo ${param1} bar ${param2}`);
const key = 'KEY';
const div = React.createElement('div');
const params = {
    param0: 'PARAM_0',
    param1: div,
    param2: 'PARAM_2'
};

describe('insertMarkupIntoKey', () => {

    it('should call a keysetRU function with a key, string params and placeholders instead of React element params', () => {
        insertMarkupIntoKey(keyset, key, params);

        expect(keyset).toBeCalledWith(key, {
            param0: 'PARAM_0',
            param1: '%%__param1%%',
            param2: 'PARAM_2'
        });
    });

    it('should return an array of a key parts and actual params', () => {
        const result = insertMarkupIntoKey(keyset, key, params);

        expect(result).toEqual([
            'PARAM_0 foo ',
            div,
            ' bar PARAM_2'
        ]);
    });
});
