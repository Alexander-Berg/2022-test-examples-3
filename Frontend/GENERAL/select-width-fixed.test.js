const fs = require('fs');
const path = require('path');

const defineSnapshotTest = require('jscodeshift/dist/testUtils').defineSnapshotTest;
const transform = require('./select-width-fixed');
const transformOptions = {
    parser: 'tsx',
};
const input = fs.readFileSync(path.join(__dirname, './fixtures/select-width-fixed.tsx'), { encoding: 'utf-8' });

defineSnapshotTest(transform, transformOptions, input, 'All specifiers');
