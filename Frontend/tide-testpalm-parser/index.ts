import { parseConfig } from './config';
import TestpalmParser from './parser';
import * as constants from './constants';
import Tide from '../../tide';
import { TestpalmParserConfig } from './types';

const { TOOL } = constants;

export = (tide: Tide, options: Partial<TestpalmParserConfig> = {}): void => {
    const pluginConfig = parseConfig(options);

    if (!pluginConfig.enabled) {
        return;
    }

    tide.addConstants({ [TOOL]: constants });

    tide.prependListener(tide.events.BEFORE_FILES_READ, () => {
        tide.addParser(new TestpalmParser({ options: pluginConfig }));
    });
};
