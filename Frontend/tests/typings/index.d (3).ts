// eslint-disable-next-line import/no-extraneous-dependencies
import type { assert as chaiAssert } from 'chai';
import 'hermione';
import '@yandex-int/hermione-ya-commands';
import type { ISendNode } from '@yandex-int/react-baobab-logger/lib/common/Sender/Sender.typings/Sender';
import type { AuthFunction, LogoutFunction } from '@yandex-int/hermione-auth-commands/typings';
import type { yaAssertViewThemeStorybook } from '../hermione/commands/yaAssertViewThemeStorybook';
import type { yaWaitElementsChanging } from '../hermione/commands/yaWaitElementsChanging';
import type { yaWaitForBackgroundLoaded } from '../hermione/commands/yaWaitForBackgroundLoaded';
import type { yaWaitForSearchPage } from '../hermione/commands/yaWaitForSearchPage';
import type { yaWaitForSKUPage } from '../hermione/commands/yaWaitForSKUPage';
import type { yaWaitForOfferPage } from '../hermione/commands/yaWaitForOfferPage';
import type { yaOpenPageByUrlWithColorScheme } from '../hermione/commands/yaOpenPageByUrlWithColorScheme';
import type { yaGetBaobabTree } from '../hermione/commands/yaGetBaobabTree';
import type { yaGetReqId } from '../hermione/commands/yaGetReqId';
import type { yaGetCounters } from '../hermione/commands/yaGetCounters';
import type { yaCompareServerAndClientBaobabTree } from '../hermione/commands/yaCompareServerAndClientBaobabTree';
import type { yaCheckBaobabEvent } from '../hermione/commands/yaCheckBaobabEvent';
import type { yaCheckNewTabOpen } from '../hermione/commands/yaCheckNewTabOpen';
import type { yaGetBaobabNode } from '../hermione/commands/yaGetBaobabNode';
import type { yaGetBaobabSentEvents } from '../hermione/commands/yaGetBaobabSentEvents';
import type { yaLocalStorageClear } from '../hermione/commands/yaLocalStorageClear';
import type { yaMockCookies } from '../hermione/commands/yaMockCookies';
import type { yaFindXHR } from '../hermione/commands/yaFindXHR';
import type { yaGetCookie } from '../hermione/commands/yaGetCookie';
import type { yaScrollContainerToElem } from '../hermione/commands/yaScrollContainerToElem';

import type { yaGetMetrikaGoals } from '../hermione/commands/yaGetMetrikaGoals';
import type { yaCheckMetrikaGoal } from '../hermione/commands/yaCheckMetrikaGoal';
import type { yaCheckMetrics } from '../hermione/commands/yaCheckMetrics';
import type { yaGetMetrics, IMetrics } from '../hermione/commands/yaGetMetrics';
import type { yaMakeBlackOverlay } from '../hermione/commands/yaMakeBlackOverlay';
import type { yaMockSuggest } from '../hermione/commands/yaMockSuggest';

/** Добавлены типы для webdriverio v4 */
/// <reference path="@types/webdriverio" />

interface IYaMockXHROptions {
    /** Матчеры урлов, которые нужно отслеживать. */
    recordData?: string[];

    /** Матчеры отслеживаемых урлов и моки ответов от сервера. */
    urlDataMap?: Record<string, string | object> | string;
    /** Матчеры отслеживаемых урлов и новый адрес для запроса. */
    urlRedirectMap?: Record<string, string>;

    /** По умолчанию: 200. */
    status?: number;
    /** По умолчанию: 0. */
    timeout?: number;
}

interface IYaCheckBaobabEventOptions {
    path: string;
    attrs?: Record<string, string | number | boolean>;
}

export interface ICounterTree {
    tree: ISendNode;
    event?: string;
}

export interface IServerCounter {
    server: { tree: ICounterTree; }
}

export interface IClientCounter {
    client: {
        events: string;
        bu?: string;
    }[];
}

export type ICounter = IServerCounter | IClientCounter;
export type IAllCounters = IServerCounter & IClientCounter;

export type TMetrikaParams = Record<string | number, string | number>;

export interface IMetrikaGoal {
    counterId: string;
    name: string;
    params?: TMetrikaParams
}

declare global {
    /**
     * Устанавливается в @yandex-int/hermione-ya-commands
     * @see https://nda.ya.ru/t/gF1J1hwn4Kndmw
     */
    const assert: typeof chaiAssert;

    namespace WebdriverIO {
        interface Browser extends WebdriverIO.Client<{}> {
            yaOpenComponent(
                path: string,
                withPlatform?: boolean,
                knobs?: Array<{name: string, value: string}>,
            ): Promise<void>;

            yaScrollElement(
                selector: string,
                offsetX?: number,
                offsetY?: number,
            ): ReturnType<WebdriverIO.Browser['execute']>;

            yaAssertViewportView(
                state: string,
                opts?: Hermione.AssertViewOpts,
            ): Promise<void>;

            yaMockXHR(options: IYaMockXHROptions): Promise<void>;

            yaWaitElementsChanging: typeof yaWaitElementsChanging;
            yaWaitForBackgroundLoaded: typeof yaWaitForBackgroundLoaded;
            yaWaitForSearchPage: typeof yaWaitForSearchPage;
            yaWaitForSKUPage: typeof yaWaitForSKUPage;
            yaWaitForOfferPage: typeof yaWaitForOfferPage;
            yaOpenPageByUrlWithColorScheme: typeof yaOpenPageByUrlWithColorScheme;
            yaChangeThemeStorybook(theme: string): Promise<void>;
            yaAssertViewThemeStorybook: typeof yaAssertViewThemeStorybook;

            yaOpenSpasUrl(url: string): Promise;

            yaCheckBaobabEvent: typeof yaCheckBaobabEvent;
            yaCheckNewTabOpen: typeof yaCheckNewTabOpen;
            yaCompareServerAndClientBaobabTree: typeof yaCompareServerAndClientBaobabTree;
            yaGetReqId: typeof yaGetReqId;
            yaGetBaobabTree: typeof yaGetBaobabTree;
            yaGetBaobabNode: typeof yaGetBaobabNode;
            yaGetBaobabSentEvents: typeof yaGetBaobabSentEvents;
            yaGetCounters: typeof yaGetCounters;

            yaLocalStorageClear(): typeof yaLocalStorageClear;
            yaMockCookies: typeof yaMockCookies;
            yaFindXHR: typeof yaFindXHR;
            yaGetCookie: typeof yaGetCookie;

            getAllCounters(): Promise<IAllCounters>;
            getCounters(reqid: string): Promise<IAllCounters>;
            assertCounters(
                reqid: string,
                params: { retryDelay: number, timeout: number },
                validator: (counter: ICounter) => ICounter | void,
            ): Promise<ICounter | undefined>;

            yaCheckMetrikaGoal: typeof yaCheckMetrikaGoal;
            yaGetMetrikaGoals: typeof yaGetMetrikaGoals;

            yaGetMetrics: typeof yaGetMetrics;
            getMetrics(
                metrics: IMetrics,
            ): Promise<void>;
            yaCheckMetrics: typeof yaCheckMetrics;
            checkMetrics(
                expectedMetrics: Partial<IMetrics>,
                params: {
                    initialDelay: number;
                    retryDelay: number;
                    timeout: number;
                },
            ): Promise<void>;

            authOnRecord: AuthFunction;
            logoutOnRecord: LogoutFunction;
            yaScrollContainerToElem: typeof yaScrollContainerToElem;
            yaMakeBlackOverlay: typeof yaMakeBlackOverlay;
            yaMockSuggest: typeof yaMockSuggest;
        }
    }

    namespace Hermione {
        interface GlobalHelper {
            also: OnlyBuilder;
        }
    }

    namespace Ya {
        namespace Metrika {
            function getGoalIdsFor(
                id: string,
                index?: string | number,
            ): string[];

            function getGoalsFor(
                id: string,
                index?: string | number,
            ): [string, IMetrikaParams][];
        }
    }

    interface Window {
        mockCookies: Record<string | string>;
    }
}
