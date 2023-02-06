'use strict';

const RuleTester = require('eslint').RuleTester;
const rule = require('../../src/rules/entrypoint-override');

function isWhitespaceOnly(str) {
    return /^\s*$/.test(str);
}

function trimWhitespaceLines(lines) {
    const length = lines.length;

    let i;
    for (i = 0; i < length && isWhitespaceOnly(lines[i]); ++i)
        ;

    let j;
    for (j = length; j > 0 && isWhitespaceOnly(lines[j - 1]); --j)
        ;

    return lines.slice(i, j);
}

function js(template) {
    let lines = trimWhitespaceLines(template[0].split('\n'));

    // Удаляем лишний отступ
    const baseIndentLength = lines[0].match(/^(\s*)/)[1].length;
    lines = lines.map(line => line.substr(baseIndentLength));

    return lines.join('\n');
}

const config = {
    parser: require.resolve('@typescript-eslint/parser'),
    parserOptions: {
        ecmaVersion: 6,
        sourceType: 'module',
    },
};

describe('Entrypoint override eslint plugin', () => {
    new RuleTester(config).run('parser: @typescript-eslint/parser', rule, {
        valid: [
            {
                code: js`
                    import { App } from './FeatureName.entries/desktop';

                    export function adapterFeatureName() {}
                    adapterFeatureName.__forceAssetPush = true;
                `,
            }, {
                code: js`
                    import { App } from './FeatureName.entries/touch-phone';

                    export function adapterFeatureName() {}
                    adapterFeatureName.__forceAssetPush = true;
                `,
            }, {
                code: js`
                    export function adapterFeatureName() {}
                    adapterFeatureName.__forceAssetPush = false;
                `,
            }, {
                code: js`
                    import { App } from './components.entries/touch-phone';

                    export function adapterFeatureName() {}
                `,
            }, {
                code: js`
                    import { App } from './FeatureName.entries/desktop';

                    export function adapterFeatureName() {}
                    adapterFeatureName.__forceAssetPush = false;
                `,
            }, {
                code: js`
                    import { App } from './FeatureName.entries/touch-phone';

                    export function adapterFeatureName() {}
                    adapterFeatureName.__forceAssetPush = false;
                `,
            },
        ],
        invalid: [
            {
                code: js`
                    import { App } from './FeatureName.entries/desktop';

                    export function adapterFeatureName() {}
                `,
                errors: [{ message: /^Entrypoint override usually requires setting the '__forceAssetPush' parameter\./ }],
                output: js`
                    import { App } from './FeatureName.entries/desktop';

                    export function adapterFeatureName() {}

                    adapterFeatureName.__forceAssetPush = true;
                `,
            }, {
                code: js`
                    import { App } from './FeatureName.entries/touch-phone';

                    export function adapterFeatureName() {}
                `,
                errors: [{ message: /^Entrypoint override usually requires setting the '__forceAssetPush' parameter\./ }],
                output: js`
                    import { App } from './FeatureName.entries/touch-phone';

                    export function adapterFeatureName() {}

                    adapterFeatureName.__forceAssetPush = true;
                `,
            },
        ],
    });
});
