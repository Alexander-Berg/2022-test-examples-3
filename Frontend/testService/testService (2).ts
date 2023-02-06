import { BaseServiceTest } from 'services/spec/baseServiceTest/baseServiceTest';

export function testService(title: string, callback: (baseServiceTest: BaseServiceTest) => void) {
    it(title, async function() {
        return callback(new BaseServiceTest());
    });
}
