import React from 'react';
import {render, unmountComponentAtNode} from 'react-dom';

import {act} from 'react-dom/test-utils';

import insertJSXIntoKey from '../insertJSXIntoKey';

function someKey(params: {
    left: number | string | boolean;
    center: number | string | boolean;
    right: number | string | boolean;
}): string {
    return params.left + ' foo ' + params.center + ' bar ' + params.right;
}

describe('insertJSXIntoKey', () => {
    let container: HTMLDivElement;

    beforeEach(() => {
        container = document.createElement('div');
        document.body.appendChild(container);
    });

    afterEach(() => {
        if (!container) {
            return;
        }

        unmountComponentAtNode(container);
        container.remove();
    });

    test('should return an array of a key parts and actual params', () => {
        const params = {
            left: 'LEFT',
            center: <div>div</div>,
            right: 'RIGHT',
        };

        const result = insertJSXIntoKey(someKey)(params);

        act(() => {
            render(result, container);
        });

        expect(container.innerHTML).toBe('LEFT foo <div>div</div> bar RIGHT');
    });
});
