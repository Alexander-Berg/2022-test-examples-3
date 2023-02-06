const Benchmark = require('benchmark');

const units = ['s/op', 'ms/op', 'μs/op', 'ns/op'];

/**
 * @param {Number[]} values
 * @returns {{multiplier: Number, name: String, format: Function}}
 */
function findCommonUnit(values) {
    let currentUnitIndex = 0;
    let multiplier = 1;

    while (currentUnitIndex < units.length - 1) {
        if (values.every(value => value * multiplier >= 0.5)) break;

        currentUnitIndex++;
        multiplier *= 1000;
    }

    const unitName = units[currentUnitIndex];

    return {
        multiplier: multiplier,
        name: unitName,
        format: number => {
            number *= multiplier;
            return (number < 0.1 ? number : number.toFixed(3)) + ' ' + unitName;
        }
    };
}

/**
 * @param {String} [name]
 * @param {Object} [options]
 * @returns {Benchmark.Suite}
 */
function createSuite(name, options) {
    const suite = new Benchmark.Suite(name, options);

    const addOriginal = suite.add;
    let maxNameLength = 0;

    suite.add = function(name, fn, options) {
        maxNameLength = Math.max(maxNameLength, name.length);
        addOriginal.call(suite, name, fn, options);
        return suite;
    };

    return suite
        .on('start', function(event) {
            const suiteName = event.currentTarget.name;
            if (suiteName) {
                // eslint-disable-next-line no-console
                console.log(suiteName);
            }
        })
        .on('cycle', function(event) {
            const stats = event.target.stats;
            const sortedSample = stats.sample.sort((a, b) => b - a);
            const median = sortedSample[sortedSample.length >> 1];
            const fastest = sortedSample[sortedSample.length - 1];

            const padding = ' '.repeat(maxNameLength - event.target.name.length);
            const unit = findCommonUnit([fastest, stats.mean, median]);

            // eslint-disable-next-line no-console
            console.log(
                String(event.target).replace(' x ', padding + ' x '),
                '\tfastest:', unit.format(fastest),
                '\tmedian:', unit.format(median),
                '\tmean:', unit.format(stats.mean)
            );
        })
        .on('complete', function() {
            // eslint-disable-next-line no-console
            console.log('Fastest is ' + this.filter('fastest').map('name'));
        })
        .on('error', function(e) {
            // eslint-disable-next-line no-console
            console.log(e);
        });
}

module.exports = {
    createSuite: createSuite
};
