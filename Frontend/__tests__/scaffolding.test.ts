/* eslint no-loop-func: 0, max-statements: 0 */

import path from 'path';

import assert from 'yeoman-assert';
import helpers from 'yeoman-test';

import npmRun from './utils/npm-run';
import { modulesCombinations, IModule } from './utils/modules';

for (const combination of modulesCombinations) {
    /**
     * Описание выбранных модулей в формате:
     * ReduxSaga [x] | Sentry [ ] | Tanker [x] | Bunker [ ]
     */
    const describeText = combination
        .map(module => module.enabled ?
            `${module.name} [x]` :
            `${module.name} [ ]`
        )
        .join(' | ');

    describe(describeText, () => {
        /**
        * Для каждой комбинации опциональных модулей проверяем:
        * 1. Создаются ли все необходимые дополнительные файлы
        * 2. Проходит ли линтинг
        * 3. Проходят ли тесты
        * 4. Собирается ли проект
        */

        let enabledModules: IModule[] | null = null;

        beforeAll(async() => {
            enabledModules = combination.filter(module => module.enabled);

            const defaultPrompts = {
                githubOrg: 'project-on-project-stub',
                projectName: 'project-on-project-stub',
                s3Bucket: 'project-on-project-stub',
                shouldAddBunker: false,
                shouldAddReduxSaga: false,
                shouldAddSentry: false,
                shouldAddTanker: false,
                shouldComposeWithCI: false,
                shouldComposeWithMonitorado: false,
            };

            const additionalPrompts = enabledModules
                .reduce((prompts, module) => ({
                    ...prompts, ...module.additionalPrompts,
                }), {});

            return await helpers
                .run(path.join(__dirname, '../generators/app'))
                .withPrompts({ ...defaultPrompts, ...additionalPrompts });
        });

        test('should create additional files', () => {
            for (const module of enabledModules) {
                if (!module.additionalFiles) {
                    continue;
                }

                for (const file of module.additionalFiles) {
                    assert.file(file);
                }
            }
        });

        test('should pass linting', () => {
            expect(npmRun('lint:es')).toMatchSnapshot();
            expect(npmRun('lint:ts')).toMatchSnapshot();
            expect(npmRun('lint:css')).toMatchSnapshot();
        });

        test('should pass tests', () => {
            expect(npmRun('test:mocha')).toMatchSnapshot();
        });

        test('should build successfully', () => {
            expect(() => {
                npmRun('build', { throwError: true });
            }).not.toThrowError();
        });
    });
}
