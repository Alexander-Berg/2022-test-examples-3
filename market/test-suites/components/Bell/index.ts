'use strict';

import counter from './counter';
import dropdown from './dropdown';

// @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
export default params => ({
    suiteName: 'Bell',
    childSuites: [counter(params), dropdown(params)],
});
