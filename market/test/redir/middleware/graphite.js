var statsd = require('./../utils/statsd');

function makeMetricName(clid, target) {
    return 'suggest_script.' +
        clid + '.' +
        'sitebar.pricebar.click.' +
        target;
}


function graphiteMiddleware(req, res, next) {
    if (req.query.clid && req.query.target) {
        statsd.increment(
            makeMetricName(req.query.clid, req.query.target),
            1
        );
    }

    next();
}

module.exports = graphiteMiddleware;