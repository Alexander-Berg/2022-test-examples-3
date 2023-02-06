const defaultConfig = require('./defaults');
const mdstCspPreset = require('./presets/mdst');

const presets = [
    ...defaultConfig,
    mdstCspPreset,
];

module.exports = presets;
