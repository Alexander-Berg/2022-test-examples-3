import _ from 'lodash';

import { STEP_IGNORE_KEYS } from '../constants';
import { TestSpec, TestSpecItem, TestSpecStep } from '../../../types';

export function normalizeTestSpec(testSpec: TestSpec): TestSpec {
    const ignoreKeys = new Set(STEP_IGNORE_KEYS);
    const traverse = (
        value: TestSpec | TestSpecItem | TestSpecStep[] | TestSpecStep,
    ): TestSpec | TestSpecItem | TestSpecStep[] | TestSpecStep => {
        if (_.isArray(value)) {
            return (value as TestSpecStep[]).reduce((acc, element) => {
                const elementValue = traverse(element as TestSpecStep) as TestSpecStep;

                if (!_.isEmpty(elementValue)) {
                    acc.push(elementValue);
                }

                return acc;
            }, []);
        }

        if (_.isObject(value)) {
            return _.keys(value).reduce((acc, key) => {
                if (!ignoreKeys.has(key)) {
                    acc[key] = traverse(value[key]);
                }

                return acc;
            }, {});
        }

        return value;
    };

    return traverse(testSpec) as TestSpec;
}
