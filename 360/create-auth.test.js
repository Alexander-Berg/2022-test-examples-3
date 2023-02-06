'use strict';

jest.mock('../config/index.js');

const config = require('../config/index.js');
const createAuth = require('./create-auth.js');

describe('createAuth', () => {
    it('requests `auth` and calls core.auth.set', () => {
        const req = {
            core: {
                auth: { set: jest.fn() },
                request: { safe: jest.fn().mockResolvedValue(1) }
            }
        };
        config.bind.mockReturnValue(2);

        return new Promise((resolve) => {
            createAuth(req, null, resolve);
        }).then(() => {
            expect(req.core.request.safe).toHaveBeenCalledWith('auth');
            expect(req.core.auth.set).toHaveBeenCalledWith(1);
            expect(config.bind).toHaveBeenCalledWith(req);
            expect(req.config).toBe(2);
        });
    });
});
