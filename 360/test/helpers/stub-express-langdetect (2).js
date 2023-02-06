'use strict';

const mockRequire = require('mock-require');

const mock = () => (req, res, next) => {
    next();
};

mockRequire('@yandex-int/express-langdetect', {
    default: mock
});
