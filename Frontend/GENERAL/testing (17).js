const { SELF } = require('@yandex-int/express-yandex-csp');
const defaultConfig = require('./defaults');
const mdstCspPreset = require('./presets/mdst');

const presets = [
    ...defaultConfig,
    mdstCspPreset,
    {
        'frame-src': [SELF],
        'font-src': ['data:'],
    },
];

module.exports = presets;
