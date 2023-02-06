'use strict';

const gulp = require('gulp');
const concat = require('gulp-concat');
const debug = require('debug')('gulp:priv-test');
const wrapFile = require('../../gulp-plugins/wrap-file');
const comments = require('../../helpers/comments');
const runMocha  = require('../../helpers/mocha');
const fastSrc = require('../../gulp-plugins/fastSrc');
const config = require('../../config');

const MOCHA_HTML_REPORTER = process.env.MOCHA_HTML_REPORTER;
const MOCHA_REPORTER = MOCHA_HTML_REPORTER ? 'mocha-simple-html-reporter' : process.env.MOCHA_REPORTER || 'min';

const expTestPrivOptions = {
    /**
     * Возвращает хедер, подключаемого теста на priv эксперимента
     *
     * @param {Vinyl} file
     *
     * @returns {String}
     */
    before: file => {
        const flag = /experiments\/([^/]+)/.exec(file.path)[1];

        /**
         * взводим флаг для тестов:
         *   - 1ый раз, чтобы правильно работал describeBlock
         *   - 2ой раз (before/after), чтобы все правильно работало на этапе выполнения it-ов
         */
        return `
${comments.beginSourceString(file)}
experimentarium.activate({'${flag}': '1'});
describe('expFlag "${flag}"', function() {
    before(() => experimentarium.activate({ '${flag}': '1' }));
    after(() => experimentarium.deactivate());

`;
    },

    after: file => `
});
experimentarium.deactivate();
${comments.endSourceString(file)}
`
};

const getTestTargets = platform => `${config.paths.BUILD}/pages-${platform}/search/all/all.test-priv.js`;

const getMochaOpts = platform => ({
    color: config.color,
    reporter: MOCHA_REPORTER,
    reporterOption: MOCHA_HTML_REPORTER && { output: `priv-test_${platform}.html` }
});

/**
 * Собирает тесты на priv-ы экспериментов для заданной платформы
 *
 * @param {SearchPage} page
 *
 * @returns {Promise}
 */
function buildExperimentsTests(page) {
    const sources = page.levels.map(level => `experiments/*/blocks-${level}/**/*.test-priv.js`);

    return new Promise(resolve => {
        fastSrc(sources)
            .pipe(
                wrapFile(expTestPrivOptions)
            )
            .pipe(concat('experiments.test-priv.js'))
            .pipe(gulp.dest(page.allPath))
            .on('finish', resolve);
    });
}

/**
 * Собирает тесты на priv-ы adapters/construct/blocks- для заданной платформы
 *
 * @param {SearchPage} page
 *
 * @returns {Promise}
 */
function buildTests(page) {
    const foldersMapping = {
        adapters: 'adapters/',
        construct: 'construct/',
        blocks: ''
    };

    return Promise.all(Object.keys(foldersMapping).map(entity => {
        const sources = page.levels.map(level => `${foldersMapping[entity]}blocks-${level}/**/*.test-priv.js`);

        return new Promise(resolve => {
            fastSrc(sources)
                .pipe(wrapFile({
                    before: comments.beginSourceString,
                    after: comments.endSourceString
                }))
                .pipe(concat(`${entity}.test-priv.js`))
                .pipe(gulp.dest(page.allPath))
                .on('finish', resolve);
        });
    }));
}

/**
 * Собирает тесты в один файл
 *
 * @param {SearchPage} page
 *
 * @returns {Promise}
 */
function buildAllTests(page) {
    const entities = ['blocks', 'adapters', 'construct', 'experiments'];
    const sources = entities.map(entity => `${page.allPath}/${entity}.test-priv.js`);

    // Инклудим все глобальные настройки в самом начале
    sources.unshift('test/mocha/init.test-priv.js');

    return new Promise(resolve => {
        gulp.src(sources)
            .pipe(concat('all.test-priv.js'))
            .pipe(gulp.dest(page.allPath))
            .on('finish', resolve);
    });
}

async function build(page) {
    await Promise.all([
        buildExperimentsTests(page),
        buildTests(page)
    ]);
    await buildAllTests(page);
}

async function runTest(page) {
    await runMocha(getTestTargets(page.platform), getMochaOpts(page.platform));
}

module.exports.run = async page => {
    debug('start');
    await build(page);
    debug('end build');
    await runTest(page);
    debug('end test');
};
