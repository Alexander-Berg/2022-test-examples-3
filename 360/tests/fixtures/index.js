const formatters = require('../../app/helpers/formatter');
const getHash = (hash) => hash.replace(/(\/|\:)/g, '');

module.exports = function getFixture({ type, params, formatted }) {
    const hash = params.hash ? getHash(params.hash) : '';
    const originalNow = Date.now;
    Date.now = () => 1537190300000;
    switch (type) {
        case 'public_info': {
            const data = require(`./${type}/${hash}.json`);
            return formatted ? formatters.formatPublicInfo(data, hash) : data;
        }
        case 'public_list': {
            const portion = (params.offset || 0) / 40;
            const data = require(`./${type}/${hash}/${portion}.json`);
            return formatted ? formatters.formatResourcesList(data, 40) : data;
        }
        case 'public_dir_size': {
            const data = require(`./${type}/${hash}.json`);
            return formatted ? { data } : data;
        }
    }
    Date.now = originalNow;
};
