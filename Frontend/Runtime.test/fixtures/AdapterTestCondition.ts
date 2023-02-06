import { Adapter } from '../../..';
import { IExperimentMeta, IContext } from '../../../typings';

export function adapterTestCondition(Base: typeof Adapter, expMeta: IExperimentMeta) {
    return class AdapterTestRender extends Base {
        render() {
            const base = super.render();
            return `${base}, exp(name: ${expMeta.name}, val: ${expMeta.val}), __condition: ${adapterTestCondition.__condition(this.context)}`;
        }
    };
}

adapterTestCondition.__condition = function(context: IContext) {
    return context.expFlags.test_condition === 1;
};
