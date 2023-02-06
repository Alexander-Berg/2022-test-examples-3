'use strict';

jest.mock('../lib/core.js');

const Core = require('../lib/core.js');
const createCore = require('./create-core.js');

describe('createCore', () => {
    it('creates req.core', () => {
        const req = {};
        const next = jest.fn();

        createCore(req, 1, next);

        expect(Core).toHaveBeenCalledWith(req, 1);
        expect(req.core).toBeInstanceOf(Core);
        expect(next).toHaveBeenCalled();
    });
});
