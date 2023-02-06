import _ from 'lodash';
import { Command } from 'commander';

import { addOptionsFromInput, parseConfig } from './options';
import { TideTestdataManager } from './tide-testdata-manager';
import { isFile, removeExtension } from '../../utils/file-system';
import { Tide, File } from '../../types';
import { formatExample } from '../../utils/cli';

const inquirer = require('inquirer');

export = (tide: Tide, options = {}): void => {
    const pluginOptions = parseConfig(options);
    const c = tide.constants;

    if (!pluginOptions.enabled) {
        return;
    }

    tide.on(tide.events.CLI, (program) => {
        program
            .command('update-url')
            .usage('[searchPath] --old-url <query-string> --new-url <query-string>')
            .description('change url params without the need to re-generate dumps')
            .requiredOption('--old-url <url>', 'url to find as a query string or json object')
            .requiredOption('--new-url <url>', 'url to replace as a query string or json object')
            .option(
                '--use-slow-algorithm',
                'use slower, but more reliable algorithm. Try this if there are issues with the default version',
            )
            .addHelpText(
                'after',
                formatExample([
                    {
                        description:
                            'Обновить параметр url в тестах по пути src/features/Organic font_size с 10 до 22',
                        command:
                            'tide -- update-url --old-url exp_flags=font_size=10 --new-url exp_flags=font_size=22',
                    },
                    {
                        description:
                            'Обновить параметр url в тестах по пути src/features/Organic font_size с 10 до 22 (json-формат)',
                        command:
                            'tide -- update-url src/features/Organic --old-url \'{ "query": ["exp_flags", "font_size=10"] }\' --new-url \'{ "query": ["exp_flags", "font_size=22"] }\'',
                    },
                    {
                        description: 'Удалить флаги, подходящие под регулярное выражение',
                        command:
                            "tide -- update-url src/features/Organic --old-url 'exp_flags=weather_touch_now*' --new-url ''",
                    },
                    {
                        description:
                            'Добавить новый флаг exp_flags=universe=42 в тестах по пути src/features/Organic',
                        command:
                            "tide -- update-url src/features/Organic --old-url '' --new-url 'exp_flags=universe=42'",
                    },
                    {
                        description:
                            'Обновить путь с /api/method?exp_flags=font-size=10 на /api/v2/new_method?exp_flags=font-size=22 в тестах по пути src/features/Organic',
                        command:
                            "tide -- update-url src/features/Organic --old-url '/api/method?exp_flags=font-size=10' --new-url '/api/v2/new_method?exp_flags=font-size=22'",
                    },
                ]),
            )
            .action((commandOptions, cmdObj: Command): void => {
                addOptionsFromInput(pluginOptions, cmdObj, commandOptions);

                const { searchPath } = pluginOptions;
                const testdataManager = new TideTestdataManager(tide, pluginOptions);

                tide.on(tide.events.BEFORE_FILES_READ, () => {
                    tide.parsers.hermione?.setOptions({ mode: 'ast' });

                    if (isFile(searchPath)) {
                        const exts = [
                            ...(c.testpalm?.FILE_EXTS as string[]),
                            ...(c.hermione?.FILE_EXTS as string[]),
                        ];
                        const filePathWithoutExt = removeExtension(searchPath, exts);

                        const hermioneFilePaths = c.hermione?.FILE_EXTS.map(
                            (ext) => `${filePathWithoutExt}.${ext}`,
                        ) as string[];

                        tide.setFilePaths(
                            c.testpalm?.TOOL as string,
                            c.testpalm?.FILE_EXTS.map((ext) => `${filePathWithoutExt}.${ext}`),
                        );
                        tide.setFilePaths(c.hermione?.TOOL as string, hermioneFilePaths);
                        tide.setFilePaths(c.testdata?.TOOL as string, [
                            tide.config.parsers['tide-testdata-parser'].baseDirPath(
                                hermioneFilePaths[0],
                            ),
                        ]);
                    } else {
                        tide.setFilePaths(c.testpalm?.TOOL as string, [searchPath]);
                        tide.setFilePaths(c.hermione?.TOOL as string, [searchPath]);
                        tide.setFilePaths(c.testdata?.TOOL as string, [
                            tide.config.parsers['tide-testdata-parser'].baseDirPath(searchPath),
                        ]);
                    }
                });

                tide.on(tide.events.AFTER_FILES_READ, () => {
                    if (!pluginOptions.oldUrl || !pluginOptions.newUrl) {
                        throw new Error(
                            'oldUrl or newUrl option was not provided, but is required to proceed',
                        );
                    }

                    for (const file of tide.fileCollection.getFiles()) {
                        testdataManager.updateUrl(pluginOptions.oldUrl, pluginOptions.newUrl, file);
                    }
                });

                tide.on(tide.events.BEFORE_FILES_WRITE, async () => {
                    const tools = Array.from(tide.tools) as string[];
                    const modifiedFilesByTool: Record<string, File[]> = {};

                    for (const tool of tools) {
                        modifiedFilesByTool[tool] = tide.fileCollection
                            .getFilesByTool(tool)
                            .filter(
                                (file: File) =>
                                    file.isModified || tide.parsers[file.tool].isModified(file),
                            );
                        tide.setFilePaths(
                            tool,
                            modifiedFilesByTool[tool].map((f) => f.filePath as string),
                        );
                    }

                    const modifiedFiles: File[] = _.flatten(_.values(modifiedFilesByTool));
                    if (!modifiedFiles) {
                        throw new Error(
                            "No files were modified! Couldn't find and replace any of the specified url parameters.",
                        );
                    }

                    if (tide.config.silent) {
                        return;
                    }

                    console.log('The following files will be modified:');
                    for (const file of modifiedFiles) {
                        console.log(`- ${file.filePath}`);
                    }

                    for (const tool of tools) {
                        if (!modifiedFilesByTool[tool].length) {
                            console.warn(`Warning: no ${tool} files were modified.`);
                        }
                    }

                    const answers = await inquirer.prompt([
                        {
                            type: 'confirm',
                            name: 'continue',
                            message: 'Do you want to continue?',
                            default: true,
                        },
                    ]);

                    if (!answers.continue) {
                        tide.fileCollection.eachFile((file: File) => {
                            file.isModified = false;
                        });
                    }
                });
            });
    });
};
