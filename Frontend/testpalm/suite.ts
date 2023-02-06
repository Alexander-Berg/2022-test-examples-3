import { NpmScript } from '../../../command-helpers/npm-script';

export class TestpalmSuite extends NpmScript {
    static flags = { ...NpmScript.flags };

    protected get name() {
        return 'ci:testpalm:suite';
    }
}
