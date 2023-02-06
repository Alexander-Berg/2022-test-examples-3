import * as React from 'react';
import { shallow } from 'enzyme';

import Guard from './Guard';

describe('Guard', () => {
    [true, false].forEach(isAllowed => {
        it(`Должен корректно отрендериться isAllowed=${isAllowed}`, () => {
            expect(shallow(
                <Guard isAllowed={isAllowed} fallback={<>isFallback</>}>isAllowed</Guard>,
            )).toMatchSnapshot();
        });
    });
});
