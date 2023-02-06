import * as esbuild from 'esbuild';

import {WidgetBuilderLayer, WidgetBuildResult} from '../index';

export class EsbuildWorker implements WidgetBuilderLayer {
    // eslint-disable-next-line class-methods-use-this
    async build(
        file: string,
        widgetFullPath: string,
        testDirname: string,
    ): Promise<WidgetBuildResult> {
        const script = await esbuild.build({
            stdin: {
                contents: file,
                resolveDir: testDirname,
            },
            bundle: true,
            write: false,
        });

        return {
            js: script?.outputFiles[0]?.text ?? '',
            css: script?.outputFiles[1]?.text ?? '',
        };
    }
}

export default new EsbuildWorker();
