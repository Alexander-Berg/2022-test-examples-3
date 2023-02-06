'use strict';

const mock = require('mock-require');

const middleware = (req, res, next) => {
    next();
};

mock('@yandex-int/express-langdetect', {
    default: () => middleware
});
