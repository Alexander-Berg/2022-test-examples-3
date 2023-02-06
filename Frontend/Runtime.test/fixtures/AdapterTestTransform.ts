import { Adapter } from '../../..';
import { IExperimentMeta } from '../../../typings';

/**
 * Адаптер с непсутым "transform" и без "render"
 */
export class AdapterTestTransform extends Adapter {
    transform() {
        return 'transform';
    }
}

export function adapterTestTransform(Base: typeof Adapter, expMeta: IExperimentMeta) {
    return class AdapterTestTransform extends Base {
        transform() {
            const base = super.transform();

            return `${base}, exp-1(name: ${expMeta.name}, val: ${expMeta.val})`;
        }
    };
}
