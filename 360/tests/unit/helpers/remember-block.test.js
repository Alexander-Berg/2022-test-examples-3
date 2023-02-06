const { getBlockId } = require('../../../components/helpers/remember-block');

describe('rememberBlockHelper', () => {
    describe('getBlockId', () => {
        it('корректно извлекает id из полного урла', () => {
            expect(getBlockId('/client/remember/e6e37424-ba6c-48e6-bda8-e0347c47d6e6'))
                .toBe('e6e37424-ba6c-48e6-bda8-e0347c47d6e6');
        });

        it('корректно извлекает id из полного урла с параметрами', () => {
            expect(getBlockId('/client/remember/e6e37424-ba6c-48e6-bda8-e0347c47d6e6?dialog=slider'))
                .toBe('e6e37424-ba6c-48e6-bda8-e0347c47d6e6');
        });

        it('корректно извлекает id из idContext', () => {
            expect(getBlockId('/remember/e6e37424-ba6c-48e6-bda8-e0347c47d6e6'))
                .toBe('e6e37424-ba6c-48e6-bda8-e0347c47d6e6');
        });

        it('Возвращает null для урлов не относящихся к блоку', () => {
            expect(getBlockId('/client/disk')).toBe(null);
        });
    });
});
