const path = require('path');

const testExecute = require('@yandex-levitan/codemods/utils/testExecute');

const b2bTransform = require('../b2b.transform');
const b2bNextTransform = require('../b2b.next.transform');

describe('Button', () => {
    testExecute(
        test,
        expect,
        path.join(__dirname, 'cases', 'b2b'),
        b2bTransform,
    );
});

describe('__next__/Button', () => {
    testExecute(
        test,
        expect,
        path.join(__dirname, 'cases', 'next'),
        b2bNextTransform,
    );
});
