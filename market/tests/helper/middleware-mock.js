'use strict';

const mockery = require('mockery');

const MOCKS = {
    uatraits: () => mockery.registerMock('express-uatraits', () => (req, res, next) => next()),
    yandexuid: () => mockery.registerMock('express-yandexuid', () => (req, res, next) => next()),
    secretkey: () => mockery.registerMock('express-secretkey', () => (req, res, next) => next()),
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
    }
};

module.exports = Object.assign(MOCKS, {
    integrationBefore: () => {
        MOCKS.uatraits();
        MOCKS.yandexuid();
        MOCKS.secretkey();
        MOCKS.render();
        MOCKS.realBunker();

        mockery.enable({
            warnOnReplace: false,
            warnOnUnregistered: false,
            useCleanCache: true
        });
    },
    integrationAfter: () => {
        mockery.disable();
    }
});
