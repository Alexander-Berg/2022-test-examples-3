'use strict';

const rule = require('../../../../lib/rules/ginny/no-compound-selector');
const RuleTester = require('eslint').RuleTester;

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2016}});
const ruleError = [
    {
        message: rule.meta.docs.description,
    },
];

ruleTester.run('ginny/no-compound-selector', rule, {
    valid: [
        {
            code: `class A extends PageObject {
                static get foo() {
                    return '.a';
                }
            }`,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return '.a';
                }
                
                get foo() {
                    return '.a .b';
                }
            }`,
        },
        {
            code: `class A {
                static get foo() {
                    return '.a .b';
                }
            }`,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return '#id';
                }
            }`,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return '.block__element';
                }
            }`,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return '.block__element_mod';
                }
            }`,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return \`\${A.root}__element\`;
                }
            }`,
        },
    ],
    invalid: [
        {
            code: `class A extends PageObject {
                static get foo() {
                    return '.a'
                }
                
                static get button() {
                    return '.a .b';
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return '.a.b'
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return '.a>.b'
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return 'span+b'
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return 'span:nth-child(1)'
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return 'input[checked]'
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return '.block::before'
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return '.input:focus()'
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return '.span#id'
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    const selector = select\`\${Component}\`;
                    return \`div\${selector}\`;
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return \`\${A.root} .a\`;
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return \`\${A.root} \${B.root}\`;
                }
            }`,
            errors: ruleError,
        },
        {
            code: `class A extends PageObject {
                static get foo() {
                    return \`\${A.root} \${B.root}\`;
                }
            }`,
            errors: ruleError,
        },
    ],
});
