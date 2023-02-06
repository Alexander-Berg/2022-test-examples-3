const createReporter = require('istanbul-api').createReporter;
const istanbulCoverage = require('istanbul-lib-coverage');

const map = istanbulCoverage.createCoverageMap();
const reporter = createReporter();
const fs = require('fs');

const coverageFiles = ['../coverage/coverage-final.json', './__coverage__/coverage-final.json'];
coverageFiles.forEach((coverageFile) => {
    if (fs.existsSync(coverageFile)) {
        const coverage = require(coverageFile);
        Object.keys(coverage).forEach((filename) => {
            // Приводим к новому формату

            // Приводим ветви
            // https://github.com/istanbuljs/istanbuljs/blob/master/packages/istanbul-lib-source-maps/lib/transformer.js#L141
            const branchMap = coverage[filename].branchMap;
            Object.keys(branchMap).forEach((branch) => {
                if (!branchMap[branch].loc) {
                    branchMap[branch].loc = branchMap[branch].locations[0];
                }
            });

            // Приводим функции
            // https://github.com/istanbuljs/istanbuljs/blob/master/packages/istanbul-lib-source-maps/lib/transformer.js#L111
            // https://github.com/istanbuljs/istanbuljs/blob/master/packages/istanbul-lib-source-maps/lib/mapped.js#L56
            const fnMap = coverage[filename].fnMap;
            Object.keys(fnMap).forEach((fn) => {
                if (!fnMap[fn].decl) {
                    fnMap[fn].decl = fnMap[fn].loc;
                }
            });
            map.addFileCoverage(coverage[filename]);
        });
    }
});

reporter.addAll(['lcov', 'text', 'json-summary']);
reporter.write(map);
