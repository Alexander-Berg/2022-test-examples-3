'use strict';

const mockery = require('mockery');

const MOCKS = {
    uatraits: () => mockery.registerMock('express-uatraits', () => (req, res, next) => next()),
    yandexuid: () => mockery.registerMock('express-yandexuid', () => (req, res, next) => next()),
    secretkey: () => mockery.registerMock('express-secretkey', () => {
        const mock = (req, res, next) => next();

        mock.validate = (req, res, next) => next();

        return mock;
    }),
    render: () => mockery.registerMock('./middleware/express-bundle-response', () => (req, res, next) => {
        res.bundleRender = function (bundleName, data) {
            res.json(data);
        };
        next();
    }),
    realBunker: () => mockery.registerMock('express-bunker', () => (req, res, next) => {
        req.bunker = require('../mock/bunker.json');
        next();
    }),
    renderWithLocals: () => function (bundleName, data) {
        return data;
    },
    partnersAccess: () => mockery.registerMock('./middleware/partners-access', (req, res, next) => {
        req.partnersAccess = 'allowed';
        next();
    }),
    secretkeyCheck: () => mockery.registerMock('../middleware/secretkey-check', (req, res, next) => next())
};

module.exports = Object.assign(MOCKS, {
    integrationBefore: () => {
        MOCKS.uatraits();
        MOCKS.yandexuid();
        MOCKS.secretkey();
        MOCKS.render();
        MOCKS.realBunker();
        MOCKS.partnersAccess();
        MOCKS.secretkeyCheck();

        mockery.enable({
            warnOnReplace: false,
            warnOnUnregistered: false,
            useCleanCache: true
        });
    },
    integrationAfter: () => {
        mockery.deregisterAll();
        mockery.disable();
    }
});
