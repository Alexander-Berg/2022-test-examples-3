import Hook, {HookData} from './hook';
import Test, {TestData} from './test';

export type GroupData = {
    name: string;
    beforeAll: HookData[];
    afterAll: HookData[];
    tests: TestData[];
    groups: GroupData[];
};

export default class Group {
    name: string;

    beforeAll: Hook[] = [];

    afterAll: Hook[] = [];

    tests: Test[] = [];

    groups: Group[] = [];

    constructor(name: string) {
        this.name = name;
    }

    getData(): GroupData {
        return {
            name: this.name,
            beforeAll: this.beforeAll.map(hook => hook.getData()),
            afterAll: this.afterAll.map(hook => hook.getData()),
            tests: this.tests.map(test => test.getData()),
            groups: this.groups.map(group => group.getData()),
        };
    }
}
