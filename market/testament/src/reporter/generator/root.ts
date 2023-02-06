import Group, {GroupData} from './group';

export type RootData = {
    testFilePath: string;
    groups: GroupData[];
};

export default class Root {
    testFilePath: string;

    groups: Group[] = [];

    constructor(testFilePath: string) {
        this.testFilePath = testFilePath;
    }

    getData(): RootData {
        return {
            testFilePath: this.testFilePath,
            groups: this.groups.map(group => group.getData()),
        };
    }
}
