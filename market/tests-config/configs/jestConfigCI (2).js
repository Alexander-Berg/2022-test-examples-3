const {join} = require('path');

module.exports = {
    reporters: [
        'default',
        [
            `${__dirname}/report`,
            {
                filename: join('html_reports', 'index.html'),
                dumpResults: true,
            },
        ],
    ],
};
