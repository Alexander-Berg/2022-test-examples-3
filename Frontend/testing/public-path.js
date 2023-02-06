const currentConfig = require('../current/config');
const getYENV = require('../../tools/get-yenv');

module.exports = ({}) => {
    let staticHost = process.env.STATIC_HOST;

    switch (getYENV()) {
        case 'testing':
            return (staticHost || currentConfig().staticPath) + 'pages/bundles/';

        case 'production':
        case 'pre-production':
            return currentConfig().staticPath + 'pages/bundles/';

        default:
            return '/static/turbo/pages/bundles';
    }
};
