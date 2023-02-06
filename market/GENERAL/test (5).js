const path = require('path');

const testExecute = require('@yandex-levitan/codemods/utils/testExecute');

const b2bTransform = require('../b2b.transform');

describe('Clickable', () => {
    testExecute(test, expect, path.join(__dirname, 'cases'), b2bTransform);
});
