const path = require('path');

const testExecute = require('@yandex-levitan/codemods/utils/testExecute');

const transform = require('../b2b.transform');

describe('Icon', () => {
    testExecute(test, expect, path.join(__dirname, 'cases'), transform);
});
