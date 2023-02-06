import { Adapter } from '../../..';
import { IExperimentMeta } from '../../../typings';

/**
 * Адаптер, содержащийся только в экспериментальном реестре
 */
export function adapterTestOnlyExperimental(Base: typeof Adapter, expMeta: IExperimentMeta) {
    return class AdapterTestTransform extends Base {
        transform() {
            const base = super.transform();

            return `${base}, exp-1(name: ${expMeta.name}, val: ${expMeta.val})`;
        }

        render() {
            return 'experimental-render';
        }
    };
}
