const moment = require('moment');
const _ = require('lodash');

module.exports.convert = function (data, options) {
    let momentData = moment(data);

    momentData = _.get(options, 'withTime') ? momentData : momentData.startOf('day');

    const formattedMomentData = momentData
        .utc()
        .format('YYYY-MM-DDTHH:mm:ss.SSS');

    return `${formattedMomentData}Z`;
};
