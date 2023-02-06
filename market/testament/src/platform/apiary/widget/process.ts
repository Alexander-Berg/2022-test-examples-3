/* eslint-disable no-unused-vars, @typescript-eslint/no-unused-vars, no-console */
import Processor from '@yandex-market/apiary/server/processor';
import {
    cookiesJob,
    metaJob,
    redirectJob,
    statusJob,
    titleJob,
} from '@yandex-market/apiary-annex/jobs';
import {ServerError} from '@yandex-market/apiary/server';

import {JestWorker} from '../../../mirror/layers/jest/worker';
import {SerializedFunction} from '../../../mirror/packedFunction';
import JestLayer from '../../../mirror/layers/jest';

declare let getBackend: <TAPI>(id: string) => TAPI | null;

export type AbstractWidgetPropsDescriptor<TType extends string, TPayload> = {
    type: TType;
    payload: TPayload;
};

export type WidgetPropsDescriptor =
    | AbstractWidgetPropsDescriptor<'function', SerializedFunction<any>>
    | AbstractWidgetPropsDescriptor<'data', unknown>;

export type ProcessWidgetResultHTTP = {
    status: Array<{code: number; lock: boolean}>;
    headers: Array<{name: string; value: string}>;
};

export type ProcessWidgetResultSuccess = {
    result: true;
    data: {
        html: string;
        http: ProcessWidgetResultHTTP;
        widget: {
            data: unknown;
        };
    };
};

export type ProcessWidgetResult = ProcessWidgetResultSuccess;

export type ProcessWidgetPayload = {
    pathToWidget: string;
    widgetProps: WidgetPropsDescriptor;
};

export const processWidget = async (
    payload: ProcessWidgetPayload,
): Promise<ProcessWidgetResult> => {
    const {pathToWidget, widgetProps} = payload;
    try {
        let resolvedProps: any = {};

        if (widgetProps.type === 'function') {
            resolvedProps = await getBackend<JestWorker>(JestLayer.ID)?.runCode(
                widgetProps.payload.code,
                widgetProps.payload.args,
            );
        } else if (widgetProps.type === 'data') {
            resolvedProps = widgetProps.payload;
        }

        // eslint-disable-next-line
        // TODO MARKETFRONTECH-4364
        // eslint-disable-next-line
        const RootWidget = require(pathToWidget).default;
        if (!RootWidget) {
            throw new Error(
                `Widget ${pathToWidget} is empty or has errors. See log above.`,
            );
        }

        // eslint-disable-next-line
        const makeMockedContext = require('@yandex-market/mandrel/mockedContext');
        // eslint-disable-next-line
        const {getStoutRequest} = require('@yandex-market/mandrel/context');
        const {
            getMods,
            needToBlockAll,
            needOnlyMarkup,
            // eslint-disable-next-line
        } = require('@yandex-market/mandrel/progressive/utils');
        const context = makeMockedContext();

        const http: ProcessWidgetResultHTTP = {
            headers: [],
            status: [],
        };
        let widgetData: unknown = null;

        if (context._page.state === 'pending') {
            await new Promise(resolve => {
                context._page.on('endInit', resolve);
            });
        }

        const isHeadRequest = getStoutRequest(context).method === 'HEAD';
        const mods = getMods(context);
        const forceRobotMode = mods.robot || isHeadRequest;

        const html: string = await new Promise<string>(
            (resolve, reject): void => {
                const parts: string[] = [];

                Processor.process({
                    listeners: {
                        error(err: ServerError | Error | void) {
                            reject(err);
                        },
                        widgetIsFinished(info: any) {
                            widgetData = info.data || null;
                        },
                    },

                    strategy: {
                        // @ts-ignore
                        streamRender: false,
                        blockAll: needToBlockAll(context) || forceRobotMode,
                        onlyMarkup: needOnlyMarkup(context) || forceRobotMode,
                        onlyJobs: isHeadRequest,
                        statusLock: false,
                        write: (part: string) => {
                            parts.push(part);
                        },
                        // metaJob, redirectJob
                        buildUrl(name: string, params: unknown): string {
                            // TODO
                            // 1 - импортировать роутер
                            // 2 - вызвать у него buildUrl и вернуть
                            return 'buildUrl';
                        },

                        setStatus(code: number, lock = false) {
                            http.status.push({code, lock});
                        },

                        setHeader(name: string, value: string) {
                            http.headers.push({name, value});
                        },
                    },

                    context,

                    entry: [RootWidget, resolvedProps],

                    jobs: [
                        cookiesJob,
                        // @ts-ignore
                        redirectJob,
                        // @ts-ignore
                        statusJob,
                        titleJob,
                        // @ts-ignore
                        metaJob,
                    ],
                })
                    .then(() => {
                        resolve(parts.join(''));
                    })
                    .catch(err => {
                        reject(err);
                    });
            },
        );

        return {
            result: true,
            data: {
                html,
                http,
                widget: {
                    data: widgetData,
                },
            },
        };
    } catch (e) {
        let error;

        // @ts-ignore
        if (e.cause && e.cause.message) {
            // @ts-ignore
            error = e.cause;
        } else {
            error = e;
        }

        throw error;
    }
};
