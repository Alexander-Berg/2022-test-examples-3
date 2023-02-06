import { Adapter } from '../../..';

export class AdapterWith_Underscore_sub extends Adapter {
    transform() {
        return 'with-underscore-sub-transform';
    }
}

export function adapterWith_Underscore_sub(Base: typeof Adapter) {
    return class AdapterWith_Underscore_sub extends Base {
        transform() {
            return 'exp-with-underscore-sub-transform';
        }
    };
}
