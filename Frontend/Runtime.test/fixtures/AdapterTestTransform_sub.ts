import { Adapter } from '../../..';

export class AdapterTestTransform_sub extends Adapter {
    transform() {
        return 'sub-transform';
    }
}

export function adapterTestTransform_sub(Base: typeof Adapter) {
    return class AdapterTestTransform_sub extends Base {
        transform() {
            return 'exp-sub-transform';
        }
    };
}
