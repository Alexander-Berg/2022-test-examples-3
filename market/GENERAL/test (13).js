/**
 * @file Created by Mikhail Silaev on 02.03.18.
 * @author Mikhail Silaev
 */

const print = require('@yandex-market/yammy-lib/lib/print');

const {testables} = require('../args/testable');
const {CI, NO_AUTO_BUILD} = require('../lib/constraints');

module.exports = {
    name: 'test',
    description: 'Запустить тесты в каждом репозитории',
    args: [testables],
    async handler({pkgs}) {
        const spawn = require('@yandex-market/yammy-lib/run/spawn');
        const Pkg = require('../models/pkg');

        let success = true;
        const statusTable = {};

        for (const pkgName of pkgs) {
            const pkg = Pkg.factory(pkgName);
            const generator = await pkg.build.generator();
            if (generator) { continue; }

            try {
                if (!CI && !NO_AUTO_BUILD) {
                    await spawn('yammy', ['build', pkgName, 'deep'], {cwd: process.cwd()});
                }

                console.error(print.action(`Running ${print.command('test')} on ${print.pkg(pkgName)}`));

                await spawn('yarn', ['workspace', pkgName, 'run', 'test'], {cwd: process.cwd()});
                statusTable[pkgName] = print.success('success');
            } catch (e) {
                success = false;
                statusTable[pkgName] = print.error('failed');
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
