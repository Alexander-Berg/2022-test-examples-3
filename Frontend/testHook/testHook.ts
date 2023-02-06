import { createElement } from 'react';

import { State } from 'schema/state/State';

import { testRender } from 'hooks/common/testRender/testRender';

export function testHook<T>(state: State, callback: () => T): T {
    let result: Optional<T> = undefined;

    function Component() {
        result = callback();

        return null;
    }

    testRender(state, createElement(Component));

    return result!;
}
