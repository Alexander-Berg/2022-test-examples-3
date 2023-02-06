const defaultConfig = require('./defaults');
const staticTest = require('./presets/static-testing');

const presets = [
    ...defaultConfig,
    ...staticTest,
];

module.exports = presets;
