const {RuleTester} = require('eslint');

const rule = require('../../../../lib/rules/remote-widgets/neighbour-config');

const ruleTester = new RuleTester({ parserOptions: { ecmaVersion: 2015, sourceType: "module" } });

const expectedErrors = [
    {
        message: rule.message,
    },
];

const options = [
    {
        desktop: {
            development: 'desktop.bluemarket.fslb.beru.ru',
            testing: 'desktop.bluemarket.fslb.beru.ru',
            production: 'beru.ru',
        },
        touch: {
            development: 'touch.bluemarket.fslb.beru.ru',
            testing: 'touch.bluemarket.fslb.beru.ru',
            production: 'm.beru.ru',
        },
    },
];

ruleTester.run('neighbour-config', rule, {
    valid: [
        {
            code: 'export default {neighbour: {host: "beru.ru"}}',
            filename: 'platform.desktop/configs/production/node.js',
            options,
        },
        {
            code: 'export default {neighbour: {host: "desktop.bluemarket.fslb.beru.ru"}}',
            filename: 'platform.desktop/configs/development/node.js',
            options,
        },
        {
            code: 'export default {neighbour: {host: "desktop.bluemarket.fslb.beru.ru"}}',
            filename: 'platform.desktop/configs/testing/node.js',
            options,
        },
        {
            code: 'export default {neighbour: {host: "m.beru.ru"}}',
            filename: 'platform.touch/configs/production/node.js',
            options,
        },
        {
            code: 'export default {neighbour: {host: "touch.bluemarket.fslb.beru.ru"}}',
            filename: 'platform.touch/configs/development/node.js',
            options,
        },
        {
            code: 'export default {neighbour: {host: "touch.bluemarket.fslb.beru.ru"}}',
            filename: 'platform.touch/configs/testing/node.js',
            options,
        },
    ],

    invalid: [
        {
            code: 'export default {neighbour: {host: "eru.ru"}}',
            filename: 'platform.desktop/configs/production/node.js',
            errors: expectedErrors,
            output: `export default {neighbour: {host: '${options[0].desktop.production}'}}`,
            options,
        },
        {
            code: 'export default {neighbour: {host: "desktop.fslb.beru.ru"}}',
            filename: 'platform.desktop/configs/development/node.js',
            errors: expectedErrors,
            output: `export default {neighbour: {host: '${options[0].desktop.development}'}}`,
            options,
        },
        {
            code: 'export default {neighbour: {host: "desktop.fslb.beru.ru"}}',
            filename: 'platform.desktop/configs/testing/node.js',
            errors: expectedErrors,
            output: `export default {neighbour: {host: '${options[0].desktop.testing}'}}`,
            options,
        },
        {
            code: 'export default {neighbour: {host: "beru.ru"}}',
            filename: 'platform.touch/configs/production/node.js',
            errors: expectedErrors,
            output: `export default {neighbour: {host: '${options[0].touch.production}'}}`,
            options,
        },
        {
            code: 'export default {neighbour: {host: "touch.bluemarket.beru.ru"}}',
            filename: 'platform.touch/configs/development/node.js',
            errors: expectedErrors,
            output: `export default {neighbour: {host: '${options[0].touch.development}'}}`,
            options,
        },
        {
            code: 'export default {neighbour: {host: "touch.bluemarket.beru.ru"}}',
            filename: 'platform.touch/configs/testing/node.js',
            errors: expectedErrors,
            output: `export default {neighbour: {host: '${options[0].touch.testing}'}}`,
            options,
        },
    ],
});
