import { VPathMeta } from '@components/VPath/VPathContext';
import { buildVPath } from '@components/VPath/VPathUtil';

type IMeta = VPathMeta|string

class VPathSelectBuilder {
    private pathMeta:IMeta[]

    constructor(pathMeta:IMeta[]) {
        this.pathMeta = pathMeta;
    }

    force(pathMeta:IMeta[]) {
        return new VPathSelectBuilder(pathMeta);
    }

    select(path:IMeta) {
        return new VPathSelectBuilder([...this.pathMeta, path]);
    }

    get path() {
        return this.pathMeta.reduce<string>((sum, meta) => {
            if (typeof meta === 'string') {
                return sum + ' ' + meta;
            }
            return sum + ` [data-vpath="${buildVPath(meta.path, meta.arrayIndex)}"]`;
        }, '');
    }

    toString() {
        return this.pathMeta.reduce<string>((sum, meta) => {
            if (typeof meta === 'string') {
                return sum + `.select('${meta}')\n`;
            }
            if (meta.arrayIndex !== undefined) {
                return sum + `.select({path: '${meta.path}', arrayIndex: '${meta.arrayIndex}'})\n`;
            }
            return sum + `.select({path: '${meta.path}'})\n`;
        }, 'VPS\n') + '.path';
    }
}

export const VPS = new VPathSelectBuilder([]);
