import { Adapter } from '../../..';
import { IExperimentMeta } from '../../../typings';

export function adapterTestTransform(Base: typeof Adapter, expMeta: IExperimentMeta) {
    return class AdapterTestTransform extends Base {
        transform() {
            const base = super.transform();

            return `${base}, exp-2(name: ${expMeta.name}, val: ${expMeta.val})`;
        }
    };
}
