const { parseMdKeepPosition } = require('./utils/parse-md');
const { run } = require('./utils/runkit');

run('**/*.test.md.js', parseMdKeepPosition);
