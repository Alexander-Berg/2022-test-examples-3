import * as path from 'path';
import * as fs from 'fs';
import { exec, FAILURE_BEHAVIOUR } from '@yandex-int/frontend.ci.utils';
import { flags } from '@oclif/command';

import { ExecWithLeavesCommand } from '../command-helpers/exec-with-leaves';

const CONFIG_PATH = '.config/expflags/testids.json';
const AUTHOR = 'robot-frontend';

type ConfigParams = {
    handler: string;
    project: string;
    context: string;
    config?: object;
};

export class ExpflagsConvertCommand extends ExecWithLeavesCommand {
    static flags = {
        ...ExecWithLeavesCommand.flags,
        projectSuffix: flags.string({
            description: "Суффикс, добавляемый через '-', к основному проекту, указанному в конфиге.",
        }),
    };

    protected get name() {
        return 'expflags-to-testids';
    }

    protected runInLeafContext(leaf: string, _reportsDir: string) {
        const args = this.parse(ExpflagsConvertCommand);

        const cwd = process.cwd();
        const configPath = path.join(cwd, CONFIG_PATH);

        if (!fs.existsSync(configPath)) return;

        this.log(`Найдена конфигурация ${this.name} для листа ${leaf}`);

        const { project, handler, context, config } = this.getParams(configPath);

        const projectNameSuffix = args.flags.projectSuffix;
        const fullProjectName = projectNameSuffix ? `${project}-${projectNameSuffix}` : project;

        let command = `npx testid create-from-testpalm ${fullProjectName} ${AUTHOR} --handler ${handler} --context ${context}`;

        if (config !== undefined) {
            command += ` --config '${JSON.stringify(config)}'`;
        }

        exec(command, FAILURE_BEHAVIOUR.THROW, cwd);
    }

    private getParams(configPath: string) {
        const configFile = this.readConfigFile(configPath);

        return this.parseConfig(configFile);
    }

    private readConfigFile(path: string) {
        try {
            return fs.readFileSync(path, 'utf8');
        } catch (error) {
            throw new Error(`failed to read file "${path}": ${error.toString()}`);
        }
    }

    private parseConfig(content: string): ConfigParams {
        try {
            return JSON.parse(content);
        } catch (error) {
            throw new SyntaxError(`failed to parse config: \n\n${content} \n\nreason: ${error.toString()}`);
        }
    }
}
