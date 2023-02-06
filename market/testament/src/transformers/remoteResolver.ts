import {Config} from '@jest/types';
import {TransformOptions} from '@jest/transform';
import {TransformedSource} from '@jest/transform/build/types';

import skipTransformer from './skipTransformer';

export function process(
    source: string,
    path: Config.Path,
    config: Config.ProjectConfig,
    transformOptions?: TransformOptions,
): TransformedSource {
    if (source.includes('createResolver')) {
        return `
Object.defineProperty(exports, "__esModule", { value: true });
module.exports = new Proxy({}, {
    get(target, key) {
        return (_ctx, params) => {
            const jestLayer = testamentMirror.getLayer('jest');
            return jestLayer.backend.runCode(((key, params) => {
                const ctx = getBackend('mandrel').getContext();
                const resolver = require(${JSON.stringify(path)})[key];
                return typeof resolver === 'function' ? resolver(ctx, params) : resolver;
            }).toString(), [key, params]);
        }
    }
});`;
    }

    return skipTransformer(__filename, source, path, config, transformOptions);
}
