import { Adapter } from '../../..';
import { IExperimentMeta } from '../../../typings';

/**
 * Адаптер с непустым "render" и без "transform"
 */
export class AdapterTestRender extends Adapter {
    render() {
        return 'render';
    }
}

export function adapterTestRender(Base: typeof Adapter, expMeta: IExperimentMeta) {
    return class AdapterTestRender extends Base {
        render() {
            const base = super.render();

            return `${base}, exp-1(name: ${expMeta.name}, val: ${expMeta.val})`;
        }
    };
}
