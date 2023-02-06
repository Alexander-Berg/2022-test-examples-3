/* eslint-disable @typescript-eslint/ban-types */

import path from 'path';

import {GenericState} from '@yandex-market/apiary/common/state';
import {GenericAction} from '@yandex-market/apiary/common/actions';
import {WidgetDescription} from '@yandex-market/apiary';
import Runtime from '@yandex-market/apiary/client/runtime';

import Layer from '../../layer';
import {mount, MountResult} from '../../../platform/apiary/widget/mount';
import JestLayer from '../jest';
import {
    ProcessWidgetPayload,
    ProcessWidgetResult,
    ProcessWidgetResultHTTP,
    WidgetPropsDescriptor,
} from '../../../platform/apiary/widget/process';
import PackedFunction from '../../packedFunction';

export type MountWidgetResult = {
    html: string;
    container: HTMLElement;
    http: ProcessWidgetResultHTTP;
    data: unknown;
    runtime: Runtime;
};

export default class ApiaryLayer extends Layer<{}, void> {
    static ID = 'apiary';

    #registry: Record<string, WidgetDescription<any, any, any, any, any, any>> =
        {};

    constructor() {
        super(ApiaryLayer.ID);
    }

    // eslint-disable-next-line class-methods-use-this
    getMethods(): {} {
        return {};
    }

    async init(): Promise<void> {
        const ClientWidget =
            // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
            require('@yandex-market/apiary/client/widget').default;
        const Metareducer =
            // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
            require('@yandex-market/apiary/client/metareducer').default;
        const {
            EMPTY_STATE,
            // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
        } = require('@yandex-market/apiary/common/state');
        const originReduce = Metareducer.prototype.reduce;

        jest.spyOn(Metareducer.prototype, 'reduce').mockImplementation(
            // @ts-ignore
            function (
                state: GenericState,
                action: GenericAction,
            ): GenericState {
                if (action.type === '@apiary/CLEAR_STATE') {
                    return EMPTY_STATE;
                }

                // @ts-ignore
                return originReduce.call(this, state, action);
            },
        );
        const originalDescribe = ClientWidget.describe.bind(ClientWidget);
        jest.spyOn(ClientWidget, 'describe').mockImplementation(
            // @ts-ignore
            (description: WidgetDescription<any, any, any, any, any, any>) => {
                this.#registry[description.name] = description;
                return originalDescribe(description);
            },
        );
        const originalInquire = ClientWidget.inquire.bind(ClientWidget);
        jest.spyOn(ClientWidget, 'inquire').mockImplementation(
            // @ts-ignore
            (name: string, cb: unknown) => {
                if (!ClientWidget._isInRegistry(name) && this.#registry[name]) {
                    ClientWidget.describe(this.#registry[name]);
                }
                return originalInquire(name, cb);
            },
        );
    }

    async mountWidget<TWidgetProps extends Record<any, any>>(
        pathToWidget: string,
        props?:
            | TWidgetProps
            | (() => Promise<TWidgetProps> | TWidgetProps)
            | PackedFunction<any[], TWidgetProps>,
    ): Promise<MountWidgetResult> {
        const widgetFullPath = this.getWidgetFullPath(pathToWidget);

        if (typeof props === 'function') {
            // @ts-ignore
            // eslint-disable-next-line no-param-reassign
            props = new PackedFunction(props, []);
        }

        const result = await this.processWidget(widgetFullPath, props ?? {});

        if (!result?.result) {
            throw new Error('Unknown error');
        }

        const {container, runtime} = ApiaryLayer.mount(
            widgetFullPath,
            result.data.html,
        );

        return {
            container,
            runtime,
            html: result.data.html,
            http: result.data.http,
            data: result.data.widget.data,
        };
    }

    private static mount(pathToWidget: string, html: string): MountResult {
        // todo resolve pathToWidget relative test filename
        return mount(pathToWidget, html);
    }

    // todo сделать методом
    async processWidget(
        pathToWidget: string,
        props?: unknown,
    ): Promise<ProcessWidgetResult | null> {
        let propsDescriptor: WidgetPropsDescriptor;

        if (props instanceof PackedFunction) {
            propsDescriptor = {type: 'function', payload: props.serialize()};
        } else {
            propsDescriptor = {type: 'data', payload: props};
        }

        const jestLayer = this.getMirror()?.getLayer<JestLayer>(JestLayer.ID);

        const result = await jestLayer?.backend.runCode(
            (
                internalPath: string,
                payload: ProcessWidgetPayload,
            ): ProcessWidgetResult => {
                // eslint-disable-next-line global-require,@typescript-eslint/no-var-requires
                const serviceInternal = require(internalPath);
                return serviceInternal.processWidget(payload);
            },
            [
                require.resolve('../../../platform/apiary/widget/process'),
                {pathToWidget, widgetProps: propsDescriptor},
            ],
        );

        return result ?? null;
    }

    // eslint-disable-next-line class-methods-use-this
    getWidgetFullPath(pathToWidget: string): string {
        const jestLayer = this.getMirror()?.getLayer<JestLayer>(JestLayer.ID);

        const testDirname = path.dirname(
            jestLayer?.getTestFilename() || __filename,
        );
        return require.resolve(pathToWidget, {
            paths: [testDirname],
        });
    }
}
