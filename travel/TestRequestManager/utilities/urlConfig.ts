import isEqual from 'lodash/isEqual';

import {IUrlConfig} from 'server/utilities/TestRequestManager/types/urlConfig';
import {ERequestSourceType} from 'server/utilities/TestRequestManager/types/requestSource';

import {IRequestConfig} from 'server/utilities/HttpClient/IHttpClient';

interface IMatchUrlConfigOptions {
    urlConfigs: IUrlConfig[];
    requestConfig: IRequestConfig;
    isSSR: boolean;
}

export function matchUrlConfig(
    options: IMatchUrlConfigOptions,
): IUrlConfig | undefined {
    const {urlConfigs, requestConfig, isSSR} = options;

    return urlConfigs.find(urlConfig => {
        const urlMatches = requestConfig.url === urlConfig.url;
        const sourceMatches = urlConfig.sources.includes(
            isSSR ? ERequestSourceType.SSR : ERequestSourceType.BROWSER,
        );

        if (!urlMatches || !sourceMatches) {
            return false;
        }

        if (!urlConfig.params) {
            return true;
        }

        return isEqual(urlConfig.params, requestConfig.params);
    });
}
