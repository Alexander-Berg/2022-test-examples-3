'use strict';

module.exports = {
    reporters: [
        'default',
        [
            'jest-html-reporters',
            {
                publicPath: 'html_reports',
                filename: 'index.html',
            },
        ],
    ],
};
