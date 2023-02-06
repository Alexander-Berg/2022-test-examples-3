require('source-map-support').install({
    environment: 'node',
    hookRequire: true,
    handleUncaughtExceptions: true,
});

module.exports = require('./main.renderer.production');
