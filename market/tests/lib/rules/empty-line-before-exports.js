/**
 * @fileoverview Need set space before export statement
 * @author Alexander Ivankov
 */

//------------------------------------------------------------------------------
// Requirements
//------------------------------------------------------------------------------

const { RuleTester } = require('eslint');
const rule = require('../../../lib/rules/empty-line-before-exports');

RuleTester.setDefaultConfig({
  parserOptions: {
    ecmaVersion: 6,
    sourceType: 'module',
  },
});

//------------------------------------------------------------------------------
// Tests
//------------------------------------------------------------------------------

const ruleTester = new RuleTester();
ruleTester.run('space-before-exports', rule, {
  valid: [
    `
      const a = 'a';

      export default a;
    `,
    `
      export const a = 'a';

      export const b = 'b';
    `,
    `
      const a = 'a';

      export const b = 'b';

      export const d = 'b';
      export const c = 'b';
      export default c;
    `,
    {
      code: `
      export {default as a} from './a';
      export {default as b} from './b';
        `,
      options: [{ emptyLineBetweenReexports: false }],
    },
    {
      code: `
      export {default as a} from './a';

      export {default as b} from './b';
        `,
      options: [{ emptyLineBetweenReexports: true }],
    },
    {
      code: `
      export const a = 'a';

      export const b = 'b';
        `,
      options: [{ emptyLineBetweenExports: true }],
    },
    {
      code: `
      export const d = 'd';
      export const b = 'b';
        `,
      options: [{ emptyLineBetweenExports: false }],
    },
    {
      code: `
      export {default as a} from './';

      export {default as c} from './';

      export const d = 'd';
      export const b = 'b';
        `,
      options: [{ emptyLineBetweenExports: false }],
    },
    {
      code: `
      export {default as a} from './';

      export {default as c} from './';
      export const d = 'd';
      export const b = 'b';
        `,
      options: [{ emptyLineBetweenExports: false }],
    },
  ],

  invalid: [
    {
      code: `
      const a = 'a';
      export default a;
      `,
      errors: [
        {
          message: 'An empty string is required before export declaration.',
          type: 'ExportDefaultDeclaration',
        },
      ],
      output: `
      const a = 'a';

      export default a;
      `,
    },
    {
      code: `
      const a = 'a';
      export const b = 'b';

      export const d = 'b';
      export const c = 'b';
      export default c;
      `,
      errors: [
        {
          message: 'An empty string is required before export declaration.',
          type: 'ExportNamedDeclaration',
        },
      ],
      output: `
      const a = 'a';

      export const b = 'b';

      export const d = 'b';
      export const c = 'b';
      export default c;
      `,
    },
    {
      code: `
      export const a = 'a';
      export const b = 'b';
      `,
      options: [{ emptyLineBetweenExports: true }],
      errors: [
        {
          message: 'An empty string is required before export declaration.',
          type: 'ExportNamedDeclaration',
        },
      ],
      output: `
      export const a = 'a';

      export const b = 'b';
      `,
    },
    {
      code: `
      export const a = 'a';

      export const b = 'b';
      `,
      options: [{ emptyLineBetweenExports: false }],
      errors: [
        {
          message: 'An empty string not allowed between exports declarations.',
          type: 'ExportNamedDeclaration',
        },
      ],
      output: `
      export const a = 'a';
      export const b = 'b';
      `,
    },
    {
      code: `
      export {default as a} from './a';

      export {default as b} from './b';
      export const c = 'a';
      export const d = 'b';

      export const f = 'b';
      export default f;
        `,
      options: [{ emptyLineBetweenReexports: false }],
      errors: [
        {
          message: 'An empty string not allowed between exports declarations.',
          type: 'ExportNamedDeclaration',
        },
      ],
      output: `
      export {default as a} from './a';
      export {default as b} from './b';
      export const c = 'a';
      export const d = 'b';

      export const f = 'b';
      export default f;
        `,
    },
    {
      code: `
      export {default as a} from './a';

      export {default as b} from './b';
      export {default as c} from './c';
      export const d = 'b';

      export const f = 'b';
      export default f;
        `,
      options: [{ emptyLineBetweenExports: false }],
      errors: [
        {
          message: 'An empty string not allowed between exports declarations.',
          type: 'ExportNamedDeclaration',
        },
      ],
      output: `
      export {default as a} from './a';

      export {default as b} from './b';
      export {default as c} from './c';
      export const d = 'b';
      export const f = 'b';
      export default f;
        `,
    },
    {
      code: `
      const test = 't';
      export {default as a} from './a';
      export const f = 'b';
      export default test;
        `,
      options: [
        {
          emptyLineBetweenExports: true,
          emptyLineBetweenReexports: true,
          emptyLineBetweenExportTypes: true,
        },
      ],
      errors: [
        {
          message: 'An empty string is required before export declaration.',
          type: 'ExportNamedDeclaration',
        },
        {
          message: 'An empty string is required before export declaration.',
          type: 'ExportNamedDeclaration',
        },
        {
          message: 'An empty string is required before export declaration.',
          type: 'ExportDefaultDeclaration',
        },
      ],
      output: `
      const test = 't';

      export {default as a} from './a';

      export const f = 'b';

      export default test;
        `,
    },
    {
      code: `
      const test = 't';
      export {default as a} from './a';

      export const f = 'b';

      export default test;
        `,
      options: [
        {
          emptyLineBetweenExports: false,
          emptyLineBetweenReexports: false,
          emptyLineBetweenExportTypes: false,
        },
      ],
      errors: [
        {
          message: 'An empty string is required before export declaration.',
          type: 'ExportNamedDeclaration',
        },
        {
          message: 'An empty string not allowed between exports declarations.',
          type: 'ExportNamedDeclaration',
        },
        {
          message: 'An empty string not allowed between exports declarations.',
          type: 'ExportDefaultDeclaration',
        },
      ],
      output: `
      const test = 't';

      export {default as a} from './a';
      export const f = 'b';
      export default test;
        `,
    },
    {
      code: `
      const test = 't';
      export {default as a} from './a';

      export {default as b} from './a';
      export * from 'b';
      export * from 'g';
      export const f = 'b';
      export const d = 'b';
      export default test;
        `,
      options: [
        {
          emptyLineBetweenExports: false,
          emptyLineBetweenReexports: false,
          emptyLineBetweenExportTypes: true,
        },
      ],
      errors: [
        {
          message: 'An empty string is required before export declaration.',
          type: 'ExportNamedDeclaration',
        },
        {
          message: 'An empty string not allowed between exports declarations.',
          type: 'ExportNamedDeclaration',
        },
        {
          message: 'An empty string is required before export declaration.',
          type: 'ExportAllDeclaration',
        },
        {
          message: 'An empty string is required before export declaration.',
          type: 'ExportNamedDeclaration',
        },
        {
          message: 'An empty string is required before export declaration.',
          type: 'ExportDefaultDeclaration',
        },
      ],
      output: `
      const test = 't';

      export {default as a} from './a';
      export {default as b} from './a';

      export * from 'b';
      export * from 'g';

      export const f = 'b';
      export const d = 'b';

      export default test;
        `,
    },
  ],
});
