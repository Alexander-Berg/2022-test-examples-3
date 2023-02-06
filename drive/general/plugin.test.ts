import { join } from 'node:path';
import { readFile } from 'node:fs/promises';
import { transform } from '@babel/core';
import { plugin, MovaBabelPluginOptions } from 'lib/plugin';

async function transformFile(filename: string, options: MovaBabelPluginOptions = {}): Promise<string> {
    const sourceCode = await readFile(join(__dirname, '__fixtures__', 'plugin', filename), 'utf-8');

    const { code } = transform(sourceCode, {
        filename,
        plugins: ['@babel/plugin-transform-typescript', [plugin, options]],
    });

    return code;
}

describe('plugin', function () {
    it('should work correct with source file', async function () {
        const code = await transformFile('counter.ts');

        expect(code).toMatchInlineSnapshot(`
            "import { i18n } from './counter.i18n';
            export function counter() {
              let count = 0;

              if (\\"en\\" === 'en') {
                count = 1;
              }

              return () => i18n(\\"$c22b\\") + ':' + i18n(\\"$961d\\", {
                count: count++
              });
            }"
        `);
    });

    it('should work correct with store file', async function () {
        const code = await transformFile('counter.i18n.ts');

        expect(code).toMatchInlineSnapshot(`
            "import { i18n as i18nBuilder, keyset, plurals } from 'mova-i18n';
            const translates = keyset({
              $c22b: {
                en: 'hi'
              },
              $12de: {
                en: 'User'
              },
              $961d: {
                en: {
                  one: '{count} item',
                  other: '{count} items'
                }
              }
            });
            export const i18n = i18nBuilder(\\"en\\", plurals)(translates);"
        `);
    });

    it('should work correct with store file with different lang', async function () {
        const code = await transformFile('counter.i18n.ts', { lang: 'cs' });

        expect(code).toMatchInlineSnapshot(`
            "import { i18n as i18nBuilder, keyset, plurals } from 'mova-i18n';
            const translates = keyset({
              $c22b: {
                cs: '???'
              },
              $12de: {
                cs: '???'
              },
              $961d: {
                cs: {
                  one: '{count} ???',
                  few: '{count} ???',
                  other: '{count} ???'
                }
              }
            });
            export const i18n = i18nBuilder(\\"cs\\", plurals)(translates);"
        `);
    });

    it('should do nothing with other source file', async function () {
        const code = await transformFile('other.ts');

        expect(code).toMatchInlineSnapshot(`
            "function i18n(text) {
              return 'i18n: ' + text;
            }

            export function counter() {
              let count = 0;
              return () => i18n('hi');
            }"
        `);
    });
});
