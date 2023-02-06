'use strict';

const mock = require('mock-require');

mock('posix', {
    openlog: () => {},
    syslog: () => {}
});
