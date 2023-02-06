import {v4 as uuid} from 'uuid';
import {AxiosError, AxiosResponse} from 'axios';
import {STATUS_CODES} from 'http';

import {TEST_URL_CONFIGS_COOKIE_NAME} from 'server/utilities/TestRequestManager/constants';
import {ETestHeaders} from 'server/constants/headers';

import {
    ERequestEventType,
    TRequestEvent,
} from 'server/utilities/TestRequestManager/types/events';
import {ERequestSourceType} from 'server/utilities/TestRequestManager/types/requestSource';
import {IUrlConfig} from 'server/utilities/TestRequestManager/types/urlConfig';
import {EApiEntry} from 'types/EApiEntry';
import {IApiRequestInfo} from 'server/utilities/TestRequestManager/types/requestInfo';
import {CoreConfig, ICookies} from '@yandex-data-ui/core/lib/types';

import {delay} from 'utilities/async/delay';
import {IRequestConfig} from 'server/utilities/HttpClient/IHttpClient';
import {transformAxiosRequest} from 'server/utilities/TestRequestManager/utilities/request';
import {transformAxiosResponse} from 'server/utilities/TestRequestManager/utilities/response';
import {matchUrlConfig} from 'server/utilities/TestRequestManager/utilities/urlConfig';
import {getRequestApiHostType} from 'server/utilities/TestRequestManager/utilities/apiHost';
import {handleRequestEvent} from 'server/utilities/TestRequestManager/utilities/handleRequestEvent';

import {ApiRequestChannel} from 'server/sockets/apiRequestsChannel';

export interface ITestRequestManagerOptions {
    cookies: ICookies;
    isSSR: boolean;
    apiRequestsChannel: ApiRequestChannel;
    requestUrl: string;
    requestPageUrl: string;
    sessionKey: string;
    appConfig: CoreConfig;
    pageToken: string | undefined;
}

class TestRequestManager {
    apiRequestsChannel: ApiRequestChannel;
    pageUrl: string;
    requestUrl: string;
    sessionKey: string;
    pageToken: string | undefined;
    isSSR: boolean;
    servicesAPI: Record<EApiEntry, string>;
    urlConfigs: IUrlConfig[];
    apiRequestInfoItems: IApiRequestInfo[] = [];

    constructor({
        cookies,
        isSSR,
        apiRequestsChannel,
        requestUrl,
        requestPageUrl,
        sessionKey,
        appConfig,
        pageToken,
    }: ITestRequestManagerOptions) {
        this.apiRequestsChannel = apiRequestsChannel;
        this.pageUrl = requestPageUrl;
        this.requestUrl = requestUrl;
        this.sessionKey = sessionKey;
        this.pageToken = pageToken;
        this.isSSR = isSSR;
        this.servicesAPI = appConfig.servicesAPI;
        this.urlConfigs = JSON.parse(
            cookies[TEST_URL_CONFIGS_COOKIE_NAME] ?? '[]',
        );
    }

    async beforeRequest(config: IRequestConfig): Promise<void> {
        const configCopy = {...config};
        const requestId = (config.id = uuid());

        this.handleRequestEvent({
            type: ERequestEventType.START_REQUEST,
            id: requestId,
            request: transformAxiosRequest(config),
            source: this.isSSR
                ? {type: ERequestSourceType.SSR, pageUrl: this.pageUrl}
                : {
                      type: ERequestSourceType.BROWSER,
                      pageUrl: this.pageUrl,
                      requestUrl: this.requestUrl,
                  },
            apiHostType:
                getRequestApiHostType(config.url, this.servicesAPI) ?? null,
            timestamp: Date.now(),
        });

        const matchingUrlConfig = matchUrlConfig({
            urlConfigs: this.urlConfigs,
            requestConfig: config,
            isSSR: this.isSSR,
        });

        if (!matchingUrlConfig) {
            return;
        }

        if (matchingUrlConfig.timeout) {
            const requestDelay = Math.min(
                matchingUrlConfig.timeout ?? 0,
                config.timeout ?? Infinity,
            );

            await delay(requestDelay);

            if (config.timeout) {
                config.timeout -= requestDelay;
                // 0 axios видимо воспринимает как отсутствие таймаута
                config.timeout = Math.max(config.timeout, 0.001);
            }
        }

        if (matchingUrlConfig.errorResponse) {
            const errorMessage = 'Blocked by user';
            const axiosError: AxiosError = new Error(
                errorMessage,
            ) as AxiosError;

            axiosError.config = configCopy;
            axiosError.response = {
                data: errorMessage,
                status: matchingUrlConfig.errorResponse.status,
                statusText:
                    STATUS_CODES[matchingUrlConfig.errorResponse.status] ?? '',
                headers: {
                    'content-length': String(errorMessage.length),
                    'content-type': 'text/plain',
                    [ETestHeaders.X_YA_BLOCKED_BY_USER]: 'true',
                },
                config: configCopy,
            };

            throw axiosError;
        }
    }

    async afterRequest(
        config: IRequestConfig,
        response?: AxiosResponse,
        isAborted?: boolean,
    ): Promise<void> {
        this.handleRequestEvent({
            type: ERequestEventType.END_REQUEST,
            id: config.id ?? '',
            isAborted: isAborted ?? false,
            response: response ? transformAxiosResponse(response) : null,
            timestamp: Date.now(),
        });
    }

    getApiRequestInfoItems(): IApiRequestInfo[] {
        return this.apiRequestInfoItems;
    }

    private handleRequestEvent(event: TRequestEvent): void {
        this.apiRequestInfoItems = handleRequestEvent(
            this.apiRequestInfoItems,
            event,
        );

        this.apiRequestsChannel.sendRequestEvent(
            this.sessionKey,
            this.pageToken ?? null,
            event,
        );
    }
}

export default TestRequestManager;
