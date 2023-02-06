const { resolve } = require('path');
const defineTest = require('jscodeshift/dist/testUtils').defineTest;

defineTest(resolve(__dirname, 'noop'), 'popup-direction');
