import {initTracer} from 'jaeger-client';

import apiCompatibility from 'opentracing/lib/test/api_compatibility';

const JAEGER_TEST_CONFIG = {
    serviceName: 'travel-front',
};

apiCompatibility(() => initTracer(JAEGER_TEST_CONFIG, {}));
