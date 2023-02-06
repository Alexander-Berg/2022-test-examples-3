const domains = require('../constants/domains');

module.exports = env => {
    switch (env) {
        case 'common':
            return `https://${domains[env]}`;
        case 'internal':
            return `https://${domains[env]}`;
        case 'b2b':
            return `https://${domains[env]}`;
    }
};
