import * as babel from '@babel/core';

import plugin from './babel-plugin-classnames-bind';

it('babel-plugin-classnames-bind works', () => {
    const example = `
    import React from 'react';

    import {
        prepareQaAttributes,
        IWithQaAttributes
    } from 'utilities/qaAttributes/qaAttributes';

    import cx from './TestAnchors.scss';
`;

    const expected = `import _classNamesBind from "classnames/bind";
import React from 'react';
import { prepareQaAttributes, IWithQaAttributes } from 'utilities/qaAttributes/qaAttributes';
import _cx from './TestAnchors.scss';

const cx = _classNamesBind.bind(_cx);`;

    const result = babel.transform(example, {plugins: [plugin]});
    expect(result!.code).toBe(expected);
});
