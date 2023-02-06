import { Adapter } from '../../..';
import { IExperimentMeta } from '../../../typings';

export class AdapterTestRender_sub extends Adapter {
    render() {
        return 'sub-render';
    }
}

export function adapterTestRender_sub(Base: typeof Adapter, _expMeta: IExperimentMeta) {
    return class AdapterTestRender_sub extends Base {
        render() {
            return 'exp-sub-render';
        }
    };
}
