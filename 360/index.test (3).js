'use strict';

jest.mock('@ps-int/mail-lib');
jest.mock('./functions.js');

const { Config } = require('@ps-int/mail-lib');
const config = require('./index.js');

describe('config', () => {
    it('loads config', () => {
        expect(config).toBeInstanceOf(Config);
    });
});
