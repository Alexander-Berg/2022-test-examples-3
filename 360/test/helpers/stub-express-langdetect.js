'use strict';

const mockRequire = require('mock-require');

mockRequire('@yandex-int/express-langdetect', () => (req, res, next) => {
    next();
});
