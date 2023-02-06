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
    if (source.includes('redux-observable') || source.includes('rxjs')) {
        return `
Object.defineProperty(exports, "__esModule", { value: true });
module.exports = new Proxy({default: []}, {
    get(obj, key) {
        if(obj.hasOwnProperty(key)) {
            return obj[key];
        }
        return () => ({})
    }
});
    `;
    }

    return skipTransformer(__filename, source, path, config, transformOptions);
}
