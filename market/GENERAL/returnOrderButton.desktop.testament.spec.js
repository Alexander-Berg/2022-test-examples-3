// @flow
// flowlint untyped-import:off
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockLocation, mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import {createCase} from './returnOrderButton.common';

/** @type {Mirror} */
let mirror;

beforeAll(async () => {
    mockLocation();
    mockIntersectionObserver();

    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
});

afterAll(() => {
    mirror.destroy();
});

describe('OrderCard', () => {
    createCase(() => mirror);
});
