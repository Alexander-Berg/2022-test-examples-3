const fs = require('fs');
const path = require('path');

const defineSnapshotTest = require('jscodeshift/dist/testUtils').defineSnapshotTest;
const transform = require('./replacer');
const transformOptions = {
    import: 'lego-on-react',
    handler: 'lego-on-react',
    parser: 'tsx',
};
const input = fs.readFileSync(path.join(__dirname, './fixtures/file.tsx'), { encoding: 'utf-8' });

defineSnapshotTest(transform, transformOptions, input, 'All specifiers');
defineSnapshotTest(transform, { ...transformOptions, specifier: 'Button' }, input, 'Only Button');
defineSnapshotTest(transform,
    { ...transformOptions, specifier: 'Button', newImport: '@yandex-lego/components' },
    input,
    'Only Button with new import',
);
defineSnapshotTest(transform,
    { ...transformOptions, specifier: 'TextArea', import: '../components/TextArea' },
    input,
    'Textarea with wrapper',
);

defineSnapshotTest(transform,
    { ...transformOptions, import: 'to-replace-import', newImport: 'all-replaced' },
    input,
    'replace all imports',
);
