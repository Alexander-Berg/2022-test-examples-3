import { NpmScript } from '../../../command-helpers/npm-script';

export class TestpalmValidate extends NpmScript {
    static flags = { ...NpmScript.flags };

    protected get name() {
        return 'ci:testpalm:validate';
    }
}
