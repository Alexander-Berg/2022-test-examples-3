import {EsbuildWorker} from './esbuild/worker';
import Layer from '../../layer';
import Method from '../../method';

import {WidgetBuilderLayer, WidgetBuildResult} from './index';

export default abstract class AbstractWidgetBuilderLayer<
        TMethods extends Record<string, Method<any, any>>,
        TWorker,
    >
    // eslint-disable-next-line @typescript-eslint/ban-types
    extends Layer<{}, EsbuildWorker>
    implements WidgetBuilderLayer
{
    protected constructor(id: string, resolveScriptPath: string) {
        super(id, resolveScriptPath);
    }

    async init(): Promise<void> {
        await super.init();
    }

    // eslint-disable-next-line class-methods-use-this,@typescript-eslint/ban-types
    getMethods(): {} {
        return {};
    }

    build(
        file: string,
        widgetFullPath: string,
        testDirname: string,
    ): Promise<WidgetBuildResult> {
        return this.worker.build(file, widgetFullPath, testDirname);
    }
}
