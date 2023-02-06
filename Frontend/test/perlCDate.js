exports.perlCDate = function(date) {
    var dateTime = date,
        SECOND = 1e3,
        MINUTE = 60 * SECOND;

    if (!(dateTime instanceof Date)) {
        dateTime = new Date(dateTime);
    }

    return {
        epoch: function() {
            var utc = dateTime.getTime() - (dateTime.getTimezoneOffset() * MINUTE);

            return parseInt(utc / 1000, 10);
        },
        tzoffset: function() {
            return 0;//dateTime.getTimezoneOffset() * 60;
        }
    };
};
