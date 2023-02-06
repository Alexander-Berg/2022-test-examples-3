import _ from 'lodash';
import { TestCommand } from './types';

type InfoObject = {
    info: number | string;
};

const defaultReplacer = ({ code }): InfoObject => ({ info: code });
const executeReplacer = (
    commands: TestCommand | TestCommand[],
    replacer: Function | null | undefined,
    filePath: string,
): object | object[] => {
    commands = _.isArray(commands) ? commands : [commands];
    replacer = replacer || defaultReplacer;

    try {
        return replacer(...commands);
    } catch (error) {
        console.log(`ExecuteReplacerError for ${commands.map((c) => c.name)} in ${filePath}`);
        console.error(error);

        return [];
    }
};

export { executeReplacer };
