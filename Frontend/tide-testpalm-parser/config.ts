import _ from 'lodash';

import { TestpalmParserConfig } from './types';

const defaultOptions: TestpalmParserConfig = {
    enabled: false,
};

export const parseConfig = (options: Partial<TestpalmParserConfig>): TestpalmParserConfig => {
    return _.defaultsDeep(options, defaultOptions);
};
