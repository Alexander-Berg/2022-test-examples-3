/* global describe, it, expect */
const { sync: globSync } = require('fast-glob');

const { serialize } = require('./serialize-mdast');

function readTests(pattern) {
    const files = globSync(pattern, {
        absolute: true,
    });

    const allTests = [];

    for (const file of files) {
        const suites = require(file);

        for (const suite of suites) {
            const {
                group,
                tests,
            } = suite;

            let {
                weight: groupWeight,
            } = suite;

            if (typeof groupWeight !== 'number') {
                groupWeight = 0;
            }

            for (const test of tests) {
                const { expect, markup } = test;
                let { title, weight } = test;

                if (typeof title !== 'string') {
                    title = JSON.stringify(markup);
                }

                if (typeof weight !== 'number') {
                    weight = groupWeight;
                } else {
                    weight += groupWeight;
                }

                allTests.push({
                    group,
                    title,
                    weight,
                    expect,
                    markup,
                });
            }
        }
    }

    return allTests;
}

function splitTests(tests) {
    let onlyTests = [];
    let skipTests = [];
    const weight = tests.reduce((acc, { weight }) => Math.max(acc, weight), -Infinity);

    for (const test of tests) {
        if (test.weight === weight) {
            onlyTests.push(test);
        } else {
            skipTests.push(test);
        }
    }

    return {
        onlyTests,
        skipTests,
    };
}

function groupTests(tests) {
    const groups = Object.create(null);

    for (const test of tests) {
        const { group } = test;

        if (!Array.isArray(groups[group])) {
            groups[group] = [];
        }

        groups[group].push(test);
    }

    return groups;
}

function dumpSkipTests(testList) {
    for (const [group, tests] of Object.entries(groupTests(testList))) {
        describe(group, () => {
            for (const { title } of tests) {
                it.skip(title, () => {});
            }
        });
    }
}

function runTests(testList, parse) {
    for (const [group, tests] of Object.entries(groupTests(testList))) {
        describe(group, () => {
            for (const { markup, title, expect: expected } of tests) {
                it(title, () => {
                    expect(serialize(parse(markup)))
                        .toEqual(serialize(expected));
                });
            }
        });
    }
}

function run(pattern, parse) {
    const {
        onlyTests,
        skipTests,
    } = splitTests(readTests(pattern));

    runTests(onlyTests, parse);
    dumpSkipTests(skipTests);
}

exports.run = run;
