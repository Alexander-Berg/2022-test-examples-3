'use strict';

const TEST_CALLEE_NAMES = ['specs', 'describe', 'it'];
const YML_SPECS_TITLE_KEYS = ['category', 'feature', 'type', 'experiment'];
const YML_SPECS_TYPE_KEYS = ['specs', 'specs-integration'];
const YML_HOOK_KEYS = ['before', 'beforeEach', 'after', 'afterEach'];
const YML_META_KEYS = [
    'tags', 'params', 'description', 'aliases', 'browsers', 'priority',
    'tlds', 'langs', 'manager', 'qa-engineer', 'counter'
];
const YML_STEP_KEYS = ['do', 'assert'];
const YML_EXPECTED_FILE_EXTNAMES = ['.js', '.ts'];

const YML_IGNORE_KEYS = [].concat(YML_HOOK_KEYS, YML_META_KEYS);

module.exports = {
    TEST_CALLEE_NAMES,
    YML_SPECS_TITLE_KEYS,
    YML_SPECS_TYPE_KEYS,
    YML_HOOK_KEYS,
    YML_META_KEYS,
    YML_STEP_KEYS,
    YML_EXPECTED_FILE_EXTNAMES,
    YML_IGNORE_KEYS
};
