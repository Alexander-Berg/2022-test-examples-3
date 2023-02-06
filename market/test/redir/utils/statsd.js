var os = require('os');

var StatsD = require('node-statsd').StatsD,
    statsd = new StatsD({
        host: 'localhost',
        prefix:  os.hostname().replace(/([^\.])\..*/, '$1') + '.'
    });

exports.increment = statsd.increment.bind(statsd);
