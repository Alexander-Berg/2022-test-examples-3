import {PluginParams} from '@packages/ts-migrate/packages/ts-migrate-server';

import plugin from './i18nPlugin';

describe('i18nPlugin', () => {
  it('client.next/utils/makeI18n.tsx: в дефолтном экспорте заменят strings на TemplateStringsArray', () => {
    const result = plugin.run(
      configure({
        fileName: 'client.next/utils/makeI18n.tsx',
        text: `
export default ({dict}: I18n, keyPostfix: string | string[] = '') => (
  strings: string[],
  ...values: any[]
): $TsFixMe => {
    const key = buildKey(strings, values);

    return Object.assign(() => translate(dict, key, undefined, deps, keyPostfix).text, {
        with: (params: I18nMethodParams) => translate(dict, key, params, deps, keyPostfix).text,
        detailed: (params: I18nMethodParams) => translate(dict, key, params, deps, keyPostfix),
        isExist: (params: I18nMethodParams) =>
            !translate(dict, key, {...params, suppressWarning: true}, deps, keyPostfix).textNotFound,
    });
};`,
      }),
    );

    expect(result).toBe(`
export default ({dict}: I18n, keyPostfix: string | string[] = '') => (
  strings: TemplateStringsArray,
  ...values: any[]
): $TsFixMe => {
    const key = buildKey(strings, values);

    return Object.assign(() => translate(dict, key, undefined, deps, keyPostfix).text, {
        with: (params: I18nMethodParams) => translate(dict, key, params, deps, keyPostfix).text,
        detailed: (params: I18nMethodParams) => translate(dict, key, params, deps, keyPostfix),
        isExist: (params: I18nMethodParams) =>
            !translate(dict, key, {...params, suppressWarning: true}, deps, keyPostfix).textNotFound,
    });
};`);
  });

  it('client.next/utils/makeI18n.tsx: патчит buildKey', () => {
    const result = plugin.run(
      configure({
        fileName: 'client.next/utils/makeI18n.tsx',
        text: `const deps = {router, markdown};
const buildKey = compose(
  join(''),
  flatten,
  unapply(transpose),
);
let missHandler = null;`,
      }),
    );

    expect(result).toBe(`const deps = {router, markdown};
const buildKey: (b: TemplateStringsArray, a: any[]) => string = compose(
            join(''),
            flatten,
            unapply(transpose)
          );
let missHandler = null;`);
  });

  it('client.next/containers/withI18n.tsx: патчит тип I18nFunction', () => {
    const result = plugin.run(
      configure({
        fileName: 'client.next/containers/withI18n.tsx',
        text: `function foo(){}
type I18nFunction = {qwerty:string};
class I18n {}`,
      }),
    );

    expect(result).toBe(`function foo(){}
type I18nFunction = (literals: TemplateStringsArray, ...placeholders: readonly string[]) => $TsFixMe;
class I18n {}`);
  });

  it('client.next/containers/I18n/index.tsx: заменят compose на compose<Props, OwnProps>', () => {
    const result = plugin.run(
      configure({
        fileName: 'client.next/containers/I18n/index.tsx',
        text: `export const enhance = compose(
// $FlowFixMe<MARKETPARTNER-13972>
connect(mapStateToProps));`,
      }),
    );

    expect(result)
      .toBe(`export const enhance = compose<Props, OwnProps>(// $FlowFixMe<MARKETPARTNER-13972>
connect(mapStateToProps));`);
  });

  it('client.next/containers/I18n/index.tsx: не заменяет compose, если указаны параметры дженерика', () => {
    const text = `export const enhance = compose<T,S>(
      // $FlowFixMe<MARKETPARTNER-13972>
      connect(mapStateToProps));`;

    const result = plugin.run(
      configure({fileName: 'client.next/containers/I18n/index.tsx', text}),
    );

    expect(result).toBe(text);
  });
});

function configure({fileName, text}): PluginParams {
  return {
    fileName,
    // @ts-expect-error
    getDiagnostics() {
      return '';
    },
    options: {},
    rootDir: '',
    sourceFile: null,
    text,
  };
}
