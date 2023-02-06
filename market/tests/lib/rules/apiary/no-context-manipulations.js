const {RuleTester} = require('eslint');
const rule = require('../../../../lib/rules/apiary/no-context-manipulations');

const ruleTester = new RuleTester({
    parserOptions: {
        ecmaVersion: 2017,
    },
});

const errors = [
    {message: 'Disallowed context usage'},
];

const addValidCase = (
    code,
    filename = 'file.js',
    {
        files = '*.js',
        contextArgumentName,
    } = {}) => ({
        code,
        filename,
        options: [{
            files,
            contextArgumentName,
        }],
        parser: 'babel-eslint',
    });
const addInvalidCase = (...args) => ({...addValidCase(...args), errors});

const variableExamples = ['null', 'undefined', '1', '"1"', '{}', '[]'];

const generateEvilCode = (patternFn, contextVarName = 'ctx') => [
    `${contextVarName}.smth;`,
    `const { a } = ${contextVarName};`,
    `const v = ${contextVarName};`,
    `${contextVarName}();`,
].map(evilCode => patternFn(evilCode));

ruleTester.run('apiary/no-context-manipulations', rule, {
    valid: [
        // no export - no cry
        addValidCase(`const x = 1;`),
        addValidCase(`function makeDirty(ctx) { ctx.resource(); }`),

        // export default inline
        addValidCase(`export default () => {};`),
        addValidCase(`export default function() {};`),
        addValidCase(`export default (ctx) => {};`),
        addValidCase(`export default function(ctx) {};`),
        addValidCase(`export default (ctx) => callSmth(ctx);`),
        addValidCase(`export default function(ctx) { callSmth(ctx); };`),

        // export default declared variable

        // not-functions works fine
        ...variableExamples.map(value =>
            addValidCase(`const x = ${value}; export default x;`)
        ),

        // functions works fine too

        // arrows
        addValidCase(`const x = () => {}; export default x;`),
        addValidCase(`const x = (ctx) => {}; export default x;`),
        addValidCase(`const x = (ctx) => callSmth(ctx); export default x;`),

        // function expression
        addValidCase(`const x = function() {}; export default x;`),
        addValidCase(`const x = function(ctx) {}; export default x;`),
        addValidCase(`const x = function(ctx) { callSmth(ctx) }; export default x;`),

        // function declaration
        addValidCase(`function x() {}; export default x;`),
        addValidCase(`function x(ctx) {}; export default x;`),
        addValidCase(`function x(ctx) { callSmth(ctx) }; export default x;`),

        // through temp variable
        addValidCase(`const x = () => {}; const y = x; export default y;`),

        // named export

        // not-functions works fine
        ...variableExamples.map(value =>
            addValidCase(`export const x = ${value};`)
        ),
        ...variableExamples.map(value =>
            addValidCase(`const x = ${value}; export {x, x as y};`)
        ),

        // functions works fine too
        addValidCase(`export const x = () => {};`),
        addValidCase(`export const x = (ctx) => {};`),
        addValidCase(`export const x = (ctx) => callSmth(ctx);`),

        addValidCase(`const x = () => {}; export {x, x as y};`),
        addValidCase(`const x = (ctx) => {}; export {x, x as y};`),
        addValidCase(`const x = (ctx) => callSmth(ctx); export {x, x as y};`),

        addValidCase(`export const x = function() {};`),
        addValidCase(`export const x = function(ctx) {};`),
        addValidCase(`export const x = function(ctx) { callSmth(ctx); }`),

        addValidCase(`const x = function() {}; export {x, x as y};`),
        addValidCase(`const x = function(ctx) {}; export {x, x as y};`),
        addValidCase(`const x = function(ctx) { callSmth(ctx); }; export {x, x as y};`),

        addValidCase(`export function x() {};`),
        addValidCase(`export function x(ctx) {};`),
        addValidCase(`export function x(ctx) { callSmth(ctx) };`),

        addValidCase(`function x() {}; export {x, x as y};`),
        addValidCase(`function x(ctx) {}; export {x, x as y};`),
        addValidCase(`function x(ctx) { callSmth(ctx) }; export {x, x as y};`),

        // allow access to no-context argument
        ...generateEvilCode(evilCode => `export default (ctx) => { ${evilCode} };`).map(result => addValidCase(result, undefined, {
            contextArgumentName: 'context'
        })),

        // allow any dirty code outside of specified files
        ...generateEvilCode(evilCode => `export const x = (ctx) => { ${evilCode} };`).map(result => addValidCase(result, 'not-js.ts')),
    ],

    invalid: [
        // export default inline
        ...generateEvilCode(evilCode => `export default (ctx) => { ${evilCode} };`).map(result => addInvalidCase(result)),
        ...generateEvilCode(evilCode => `export default function(ctx) { ${evilCode} };`).map(result => addInvalidCase(result)),

        // export default declared variable
        ...generateEvilCode(evilCode => `const x = (ctx) => { ${evilCode} }; export default x;`).map(result => addInvalidCase(result)),
        ...generateEvilCode(evilCode => `function x(ctx) { ${evilCode} }; export default x;`).map(result => addInvalidCase(result)),

        // named export
        ...generateEvilCode(evilCode => `export const x = (ctx) => { ${evilCode} };`).map(result => addInvalidCase(result)),
        ...generateEvilCode(evilCode => `export function x (ctx) { ${evilCode} };`).map(result => addInvalidCase(result)),
        ...generateEvilCode(evilCode => `const x = (ctx) => { ${evilCode} }; export {x}`).map(result => addInvalidCase(result)),
        ...generateEvilCode(evilCode => `const x = (ctx) => { ${evilCode} }; export {x as y}`).map(result => addInvalidCase(result)),

        // with custom context name

        ...generateEvilCode(evilCode => `export const x = (context) => { ${evilCode} };`, 'context').map(result => addInvalidCase(result, undefined, {
            contextArgumentName: 'context',
        })),
    ],
});
