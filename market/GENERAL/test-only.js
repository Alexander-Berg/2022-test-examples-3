const print = require('@yandex-market/yammy-lib/lib/print');

const {testable} = require('../args/testable');
const {CI, NO_AUTO_BUILD} = require('../lib/constraints');

const testNames = {
    name: 'testNames',
    description: 'Названия тестов для пакета',
    async choices({pkg: name}) {
        const Pkg = require('../models/pkg');
        const pkg = Pkg.factory(name);
        const {scripts} = await pkg.config();

        return Object.keys(scripts)
            .filter(s => /^test:/.test(s))
            .map(s => s.replace('test:', ''));
    },
    variadic: true,
};

module.exports = {
    name: 'test-only',
    description: 'Запустить тесты вида test:name в одном пакете',
    args: [testable, testNames],
    async handler({pkg: pkgName, testNames}) {
        const spawn = require('@yandex-market/yammy-lib/run/spawn');
        let success = true;
        const statusTable = {};

        if (!CI && !NO_AUTO_BUILD) {
            await spawn('yammy', ['build', pkgName, 'deep'], {cwd: process.cwd()});
        }

        for (const test of testNames) {
            const scriptName = `test:${test}`;
            console.error(print.action(`Running ${print.command(scriptName)} on ${print.pkg(pkgName)}`));

            try {
                await spawn('yarn', ['workspace', pkgName, 'run', scriptName], {cwd: process.cwd()});
                statusTable[scriptName] = print.success('success');
            } catch (e) {
                success = false;
                statusTable[scriptName] = print.error('failed');
            }
        }

        const statuses = Object.entries(statusTable)
            .map(([name, status]) => `${print.command(name)}: ${status}`)
            .join('\n');

        console.error(`\nTests results:\n${statuses}\n`);

        if (!success) {
            throw new Error('Some tests failed');
        }
    },
};
