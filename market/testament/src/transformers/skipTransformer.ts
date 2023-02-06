import {Transformer, TransformOptions} from '@jest/transform';
import {Config} from '@jest/types';
import {TransformedSource} from '@jest/transform/build/types';

export default function skipTransformer(
    originalTransformer: string,
    source: string,
    path: Config.Path,
    config: Config.ProjectConfig,
    transformOptions?: TransformOptions,
): TransformedSource {
    for (const item of config.transform) {
        const [rxSource, transformerPath] = item;
        const rx = new RegExp(rxSource);

        if (transformerPath !== __filename && rx.test(__filename)) {
            // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
            const transformer = require(transformerPath) as Transformer;
            return transformer.process(source, path, config, transformOptions);
        }
    }

    return source;
}
