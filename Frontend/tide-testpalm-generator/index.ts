import { inspect } from 'util';
import _ from 'lodash';
import { Command } from 'commander';
import createDebug from 'debug';

import { parseConfig } from './config';
import TestpalmGenerator from './generator';
import { getTestpalmFilePath } from './utils';
import { Tide, TestFile, File } from '../../types';
import { formatExample } from '../../utils/cli';

const debug = createDebug('tide-testpalm-generator');

export = (tide: Tide, options = {}): void => {
    let pluginConfig = parseConfig(options);

    if (!pluginConfig.enabled) {
        return;
    }

    tide.on(tide.events.CLI, (program) => {
        program
            .command('generate')
            .usage('/path/to/file [options]')
            .description('generate tests from one tool to another')
            .option(
                '--ht, --hermione-to-testpalm',
                'generate tests from hermione to testpalm',
                true,
            )
            .option('--rewrite', 'rewrite exists tests in files')
            .option('--no-rewrite', 'no rewrite exists tests in files')
            .option(
                '-A, --all-links',
                'read all testpalm files for retrieving all links to hermione files',
            )
            .addHelpText(
                'after',
                formatExample([
                    {
                        description: 'Сгенерировать Testpalm YML на основе hermione-тестов',
                        command: 'tide -- generate src/features/kitten',
                    },
                    {
                        description:
                            'Сгенерировать Testpalm YML на основе hermione-теста, перезаписать существующие',
                        command: 'tide -- generate src/features/kitten --rewrite',
                    },
                ]),
            )
            .action((commandOptions, command: Command): void => {
                pluginConfig = _.defaults(commandOptions, pluginConfig);
                pluginConfig.filePaths = command.args;

                tide.on(tide.events.BEFORE_FILES_READ, () => {
                    const { constants } = tide;

                    _.keys(tide.parsers).forEach((parser) => tide.parsers[parser].off());
                    ['hermione-stub', 'testpalm'].forEach((parser) => tide.parsers[parser]?.on());

                    if (
                        pluginConfig.hermioneToTestpalm &&
                        constants.hermione &&
                        constants.testpalm
                    ) {
                        tide.setFilePaths(constants.hermione.TOOL, pluginConfig.filePaths);

                        if (pluginConfig.allLinks) {
                            tide.setFilePaths(
                                constants.testpalm.TOOL,
                                constants.testpalm.FILE_EXTS.map((fileExt) => '**/*.' + fileExt),
                            );
                        } else {
                            const stream = tide.filePathReader.stream;
                            stream.on('data', (filePath: string) => {
                                if (
                                    constants.hermione?.FILE_EXTS?.some((ext) =>
                                        filePath.endsWith(ext),
                                    )
                                ) {
                                    stream.write(getTestpalmFilePath(filePath, tide));
                                }
                            });
                        }
                    }
                });

                tide.on(tide.events.AFTER_FILES_READ, () => {
                    const { constants } = tide;

                    if (
                        pluginConfig.hermioneToTestpalm &&
                        constants.hermione &&
                        constants.testpalm
                    ) {
                        if (!tide.fileCollection.getFilesByTool(constants.hermione.TOOL).length) {
                            throw new Error(
                                'No .hermione.js files were found that match specified patterns.',
                            );
                        }

                        console.log(
                            `\nGenerating ${constants.testpalm.TOOL} files from ${constants.hermione.TOOL} files...`,
                        );

                        const generator = new TestpalmGenerator(tide, pluginConfig);

                        tide.fileCollection.eachFile(constants.hermione.TOOL, (file: File) => {
                            const hermioneFile = file as TestFile;

                            if (!hermioneFile.ast && !hermioneFile.data) {
                                return;
                            }

                            const testpalmFile = generator.getTestpalmFile(
                                hermioneFile,
                            ) as TestFile;
                            const newData = generator.generateData(hermioneFile, testpalmFile.data);
                            const replacedData = generator.mergeData(testpalmFile.data, newData);

                            testpalmFile.data = replacedData;

                            debug(hermioneFile.filePath);
                            debug(testpalmFile.filePath);
                            debug(
                                inspect(replacedData, {
                                    showHidden: false,
                                    depth: null,
                                    colors: true,
                                }),
                            );
                        });

                        const testpalmFilePaths = _.uniq(
                            (tide.getFilePaths(constants.hermione.TOOL) as string[]).reduce(
                                (acc: string[], hermioneFilePath: string) => {
                                    acc.push(...tide.fileCollection.getMapping(hermioneFilePath));

                                    return acc;
                                },
                                [],
                            ),
                        );

                        tide.setFilePaths(constants.testpalm.TOOL, testpalmFilePaths);
                    }
                });

                tide.on(tide.events.BEFORE_FILES_WRITE, () => {
                    if (pluginConfig.hermioneToTestpalm) {
                        tide.parsers.hermione.off();
                    }
                });
            });
    });
};
