import Cookie from 'js-cookie';

import {TUaasExperiments} from '@yandex-data-ui/core/lib/types';

import parseExpConfigString from './parseExpConfigString';

export type TActiveUaasExperiments = Partial<
    Record<keyof TUaasExperiments, boolean>
>;

export default function getActiveExperimentsFromCookies(
    experiments: TUaasExperiments,
): TActiveUaasExperiments {
    return Object.entries(experiments).reduce(
        (acc, [expConfigName, expConfigString]) => {
            const parsedConfigString = parseExpConfigString(expConfigString);

            let enabled;

            if (parsedConfigString.disabled) {
                enabled = expConfigString;
            } else {
                const {expKey, expValue} = parsedConfigString;
                const cookieValue = Cookie.get(expKey);

                enabled = cookieValue === expValue;
            }

            return {
                ...acc,
                [expConfigName]: enabled,
            };
        },
        {},
    );
}
