import plugin from './importTypeHocPlugin';

describe('importTypeHocPlugin', () => {
  test('заменяет путь к модулю', () => {
    expect(run(`import type {foo, HOC, bar} from 'recompose';`)).toBe(
      `import type {foo, HOC, bar} from '~/types/recompose';`,
    );
  });

  test('добавляет отдельный импорт для HOC', () => {
    expect(run(`import {foo, HOC, bar} from 'recompose';`)).toBe(
      `import {foo, bar} from 'recompose';
import type {HOC} from '~/types/recompose';`,
    );
  });
});

function run(text: string): string {
  return plugin.run({
    fileName: '',
    // @ts-expect-error
    getDiagnostics() {
      return '';
    },
    options: {},
    rootDir: '',
    sourceFile: null,
    text,
  }) as string;
}
