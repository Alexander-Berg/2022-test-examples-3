/* eslint-disable */
import { TestSuite, AutostopSchemaType } from '../../types';

export const defaultSuiteConfig: Partial<TestSuite> = {
    autostop: [
        {
            type: AutostopSchemaType.Time,
            dur: '10s',
            threshold: '1s',
        },
        {
            type: AutostopSchemaType.HTTP,
            code: '5xx',
            threshold: '100%',
            dur: '2s',
        },
    ],
};
