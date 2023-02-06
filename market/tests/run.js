import * as path from 'path';
import * as fs from 'fs';
import * as assert from 'assert';

import {check} from '../lib';

function run(testCase) {
    it(testCase, () => {
        const actual = require(`${process.cwd()}/${testCase}/result`);
        const result = check({
            actualVersion: `${testCase}/actual`,
            checkVersion: `${testCase}/check`,
            files: '*.js',
        });

        assert.deepStrictEqual(actual, result);
    });
}

function main() {
    process.chdir(path.join(__dirname, 'cases'));

    describe('cases', () => {
        for (const title of fs.readdirSync('.')) {
            run(title);
        }
    })
}

main();
