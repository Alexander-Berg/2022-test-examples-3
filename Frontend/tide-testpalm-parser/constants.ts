import _ from 'lodash';

export const TITLE_KEYS = ['feature', 'type', 'experiment'];
export const META_TOP_KEYS = ['params', 'counter', 'aliases'];
export const SPECS_TYPE_KEYS = {
    integration: 'specs',
    e2e: 'specs-integration',
};
export const META_KEYS = [
    ...META_TOP_KEYS,
    'files',
    'v-team',
    'manager',
    'qa-engineer',
    'description',
    'priority',
    'tags',
    'browsers',
    'tlds',
    'langs',
];
export const HOOK_KEYS = ['before', 'beforeEach', 'after', 'afterEach'];
export const TOOL = 'testpalm';
export const FILE_EXTS = ['testpalm.yml'];
export const STEP_KEYS = ['do', 'assert'];
export const ORDER_KEYS = _.uniq([
    ...TITLE_KEYS,
    ...META_TOP_KEYS,
    ..._.values(SPECS_TYPE_KEYS),
    ...META_KEYS,
]);
export const IGNORE_KEYS = [...HOOK_KEYS, ...META_KEYS];
export const STEP_IGNORE_KEYS = ['tags', 'params', 'label'];

export const META_REQUIRED_KEYS = ['files', 'description', 'tags'];
export const META_EXTRA_KEYS = [
    'v-team',
    'manager',
    'qa-engineer',
    'priority',
    'browsers',
    'tlds',
    'langs',
];
