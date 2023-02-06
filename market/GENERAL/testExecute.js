const fs = require('fs');
const path = require('path');

const jCore = require('jscodeshift/src/core');
const getParser = require('jscodeshift/src/getParser');
const args = require('minimist')(process.argv.slice(2));

function readParseTestCase(casesDir, file) {
    const text = fs.readFileSync(path.join(casesDir, file)).toString();
    const parsed = text.split(/\/\* +(from|to) +\*\//gi);
    const caseText = parsed[0].replace(/(\/\*|\*\/)/gi, '').trim();
    const from = parsed[2].replace(/(^\n+|\n+$)/gi, '');
    const to = parsed[4].replace(/(^\n+|\n+$)/gi, '');
    return {
        caseText,
        from,
        to,
    };
}

function execute(
    source,
    transform,
    mockPath = 'filePath',
    options = {parser: 'flow'},
) {
    const parser = getParser(options.parser);
    const jscodeshift = jCore.withParser(parser);
    const out = transform(
        {
            path: mockPath,
            source,
        },
        {
            j: jscodeshift,
            jscodeshift,
            stats: null,
            report: () => {},
        },
        options,
    );
    return out;
}

/**
 *  Хелпер для запуска функций трансформации в тестах
 * @param {function} test - test из jest
 * @param {function} expect - expect из jest
 * @param {string} casesDir - Директория с тест кейсами
 * @param {function} transform - Функция трансформации
 * @param {string} mockPath - Строка, мокирует директорию, передаваемый в функцию трансформации в file.path
 */
function testExecute(test, expect, casesDir, transform, mockPath) {
    const cases = fs.readdirSync(casesDir);
    cases.forEach(file => {
        const stat = fs.lstatSync(path.join(casesDir, file));
        if (stat.isDirectory() || (args.case && file !== args.case)) {
            return;
        }
        const {caseText, from, to} = readParseTestCase(casesDir, file);
        test(`${file}: ${caseText}`, () => {
            expect(execute(from, transform, mockPath)).toBe(to);
        });
    });
}

module.exports = testExecute;
