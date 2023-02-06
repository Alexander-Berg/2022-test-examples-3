import { join } from 'node:path';
import { readFile } from 'node:fs/promises';
import { types as t, parse, traverse } from '@babel/core';
import generator from '@babel/generator';
import { processTranslates } from 'lib/processTranslates';

async function transformFile(filename: string): Promise<t.ObjectExpression> {
    const sourceCode = await readFile(join(__dirname, '__fixtures__', 'processTranslates', filename), 'utf-8');

    const file = parse(sourceCode, {
        filename,
        plugins: ['@babel/plugin-transform-typescript'],
    });

    let node: t.ObjectExpression;

    traverse(file, {
        VariableDeclarator(path) {
            if (t.isObjectExpression(path.node.init)) {
                node = path.node.init;
            }
        },
    });

    return node;
}

describe('processTranslates', function () {
    it('should work correct', async function () {
        const objectExpression = await transformFile('t1.ts');

        processTranslates(objectExpression, { lang: 'be', langPriorityList: ['be', 'en'], hashLen: 4 });

        expect(generator(objectExpression).code).toMatchInlineSnapshot(`
            "{
              $4e5a: {
                be: 'Увайсьці'
              },
              $3748: {
                be: 'Прывітанне, {username}'
              },
              $21f9: {
                be: {
                  one: '{count} карыстальнік',
                  few: '{count} карыстальніка',
                  many: '{count} карыстальнікаў'
                }
              }
            }"
        `);
    });

    it('should work correct with fallback', async function () {
        const objectExpression = await transformFile('t2.ts');

        processTranslates(objectExpression, { lang: 'be', langPriorityList: ['be', 'en'], hashLen: 4 });

        expect(generator(objectExpression).code).toMatchInlineSnapshot(`
            "{
              $4e5a: {
                be: 'Login'
              },
              $3748: {
                be: 'Прывітанне, {username}'
              },
              $21f9: {
                be: {
                  one: '{count} user',
                  other: '{count} users'
                }
              }
            }"
        `);
    });

    it('should work correct with empty translate and fallback', async function () {
        const objectExpression = await transformFile('t4.ts');

        processTranslates(objectExpression, { lang: 'be', langPriorityList: ['be', 'en'], hashLen: 4 });

        expect(generator(objectExpression).code).toMatchInlineSnapshot(`
            "{
              $4e5a: {
                be: 'Login'
              },
              $3748: {
                be: 'Прывітанне, {username}'
              },
              $21f9: {
                be: {
                  one: '{count} user',
                  other: '{count} users'
                }
              }
            }"
        `);
    });

    it('should work correct with no available fallback', async function () {
        const objectExpression = await transformFile('t3.ts');

        processTranslates(objectExpression, { lang: 'be', langPriorityList: ['be', 'en'], hashLen: 4 });

        expect(generator(objectExpression).code).toMatchInlineSnapshot(`
            "{
              $4e5a: {},
              $3748: {},
              $a479: {
                be: 'Спампаваць'
              }
            }"
        `);
    });
});
