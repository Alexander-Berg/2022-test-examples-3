import Sandboxer from '@yandex-int/sandboxer';
import { IConfiguratorOptions } from '@yandex-int/sandboxer/build/configurator';
import Resource from '@yandex-int/sandboxer/build/resource';

export default class MockedSandboxer extends Sandboxer {
    constructor(options: IConfiguratorOptions, mock: Function) {
        super(options);

        this.resource = {
            finder: {
                find: mock,
            },
        } as unknown as Resource;
    }
}
