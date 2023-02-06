import { NpmScript } from '../../../command-helpers/npm-script';

export class TestpalmSynchronize extends NpmScript {
    static flags = { ...NpmScript.flags };

    protected get name() {
        return 'ci:testpalm:synchronize';
    }
}
