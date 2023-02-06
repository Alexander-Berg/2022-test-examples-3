import { AdapterTestRender as BaseTestRender } from './AdapterTestRender';

export function adapterTestRender(Base: typeof BaseTestRender) {
    return class AdapterTestRender extends Base {
        render() {
            return `forced-assets-render: ${adapterTestRender.__forceAssetPush}`;
        }
    };
}

adapterTestRender.__forceAssetPush = true;
