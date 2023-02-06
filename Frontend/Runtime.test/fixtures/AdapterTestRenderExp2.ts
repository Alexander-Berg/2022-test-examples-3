import { Adapter } from '../../..';
import { IExperimentMeta } from '../../../typings';

export function adapterTestRender(Base: typeof Adapter, expMeta: IExperimentMeta) {
    return class AdapterTestRender extends Base {
        render() {
            const base = super.render();

            return `${base}, exp-2(name: ${expMeta.name}, val: ${expMeta.val})`;
        }
    };
}
