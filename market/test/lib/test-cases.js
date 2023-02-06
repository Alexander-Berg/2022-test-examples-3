const Fs = require('fs');

const paths = require('./paths');

module.exports = function () {
    const result = [];

    Fs.readdirSync(paths.configs).forEach(config => {
        const shortName = config.split('.')[0];
        result.push(
            [shortName, 'development', `${paths.configs}/${config}`],
            [shortName, 'production', `${paths.configs}/${config}`],
        );
    });

    return result;
};
