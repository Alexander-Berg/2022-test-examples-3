import ellipsis from '../ellipsis';

const SOURCE_STR = '0123456789012345678901234567890123456789';
const CUT_TO_LENGTH = 30;

describe('ellipsis', () => {
    it('выдача содержит исходную строку целиком', () => {
        const ret = ellipsis(SOURCE_STR, SOURCE_STR.length + 1);

        expect(ret).toEqual(SOURCE_STR);
    });

    it('выдача содержит обрезанную строку с ... на конце', () => {
        const ret = ellipsis(SOURCE_STR, CUT_TO_LENGTH);

        expect(ret).toEqual(`${SOURCE_STR.substr(0, CUT_TO_LENGTH)}...`);
    });
});

// =================================================================================================
