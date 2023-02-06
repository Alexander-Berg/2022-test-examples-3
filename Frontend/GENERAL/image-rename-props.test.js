const fs = require('fs');
const path = require('path');

const defineSnapshotTest = require('jscodeshift/dist/testUtils').defineSnapshotTest;
const transform = require('./image-rename-props');
const transformOptions = {
    parser: 'tsx',
};
const input = fs.readFileSync(path.join(__dirname, './fixtures/image.tsx'), { encoding: 'utf-8' });

defineSnapshotTest(transform, transformOptions, input, 'Image rename props');
