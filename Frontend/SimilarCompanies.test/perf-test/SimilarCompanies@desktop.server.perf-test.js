/* eslint-disable no-console, no-constant-condition, max-len */
/**
 * Профилирование адаптера SimilarCompanies@desktop или класса OrgSimilarCompanies@desktop
 *
 * 1. Собрать код в testing или production
 * - `npm run build:testing && find .build/ -name "*.js.map" -type f -delete`
 * 2. Профилирование в DevTools:
 * - Поставить `useProf = false`
 * - `node --inspect -r ignore-styles src/features/Companies/Companies.features/SimilarCompanies/SimilarCompanies.test/perf-test/SimilarCompanies@desktop.server.perf-test.js`
 * 3. Сбор и анализ профайла v8*.log:
 * - Поставить `useProf = true`
 * - `node --prof -r ignore-styles src/features/Companies/Companies.features/SimilarCompanies/SimilarCompanies.test/perf-test/SimilarCompanies@desktop.server.perf-test.js`
 * - Обработать лог с помощью ynode-tick-processor
 */
const { getSerpContextStub } = require('../../../../../../../.build/src/lib/Context/TestStubs');
// const { OrgSimilarCompanies } = require('../../../../../../../.build/src/features/Companies/Companies.utils/OrgSimilarCompanies/OrgSimilarCompanies@desktop');
const { AdapterSimilarCompanies } = require('../../../../../../../.build/src/features/Companies/Companies.features/SimilarCompanies/SimilarCompanies@desktop.server');
const data = require('./data.json');

const serpContext = getSerpContextStub({
    device: {
        device: 'desktop',
    },
});

const context = {
    ...serpContext,
    skipAutowrap: false,
    isInWizard: false,
    isInRightColumn: false,
    isWideRightColumn: false,
    isMainResult: true,
    documentListIndex: 0,
};

class PerfReporter {
    constructor() {
        this.startTime = Date.now();
        this.lastTime = this.startTime;
        this.lastCounter = 0;
        this.step = 10000;
    }

    report(counter) {
        // Быстрая проверка почти без оверхеда
        if (counter % this.step !== 0) return;

        const currentTime = Date.now();
        const elapsedMs = currentTime - this.lastTime;
        // Выводим не чаще раза в 5с
        if (elapsedMs < 5000) {
            // const estimatedStep = 5000 * this.step / elapsedMs;
            // console.log(estimatedStep, elapsedMs);
            return;
        }

        const averageSpeed = this.calcSpeed(this.startTime, currentTime, counter);
        const currentSpeed = this.calcSpeed(this.lastTime, currentTime, counter - this.lastCounter);
        console.log(`Speed: average ${averageSpeed} ops/s, current ${currentSpeed} ops/s`);

        this.lastTime = currentTime;
        this.lastCounter = counter;
    }

    calcSpeed(startTimeMs, endTimeMs, opCount) {
        return Math.round(opCount / (endTimeMs - startTimeMs) * 1000);
    }
}

const useProf = false;
// const useProf = true;
const PROF_ITERATIONS = 5000000;
// const PROF_ITERATIONS = 10;
let i = 0;
const perfReporter = new PerfReporter();
console.log('Now entering a loop...');

while (true) {
    const dataItem = data[i % data.length];
    // new OrgSimilarCompanies(context, dataItem.state).getProps();
    new AdapterSimilarCompanies({
        context: context,
        snippet: { state: dataItem.state, items: dataItem.items },
    }).render();
    i++;
    perfReporter.report(i);
    if (useProf && i >= PROF_ITERATIONS) break;
}
