const promisesCustomMatchers = require('@self/platform/spec/unit/helpers/promisesCustomMatchers');
const stoutCustomMatchers = require('@self/platform/spec/unit/helpers/stoutCustomMatchers');

expect.extend(promisesCustomMatchers);
expect.extend(stoutCustomMatchers);

global.requestIdleCallback = global.requestIdleCallback || setImmediate;
global.cancelIdleCallback = global.cancelIdleCallback || clearImmediate;
