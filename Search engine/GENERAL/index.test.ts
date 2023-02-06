import {getLastModifiedPhrase} from './index';

describe('getLastModifiedPhrase', () => {
    test('[modified] to equal snapshot', () => {
        const dateString = '2021-03-24T22:04:22.124+03:00';
        const login = 'robot';

        expect(getLastModifiedPhrase(dateString, login)).toMatchInlineSnapshot(`
            "last modified at
                24.03.2021 22:04
                by robot"
        `);
    });

    test('[created] to equal snapshot', () => {
        const dateString = '2021-03-24T22:04:22.124+03:00';
        const login = 'robot';
        const type = 'created';

        expect(getLastModifiedPhrase(dateString, login, type))
            .toMatchInlineSnapshot(`
            "created at
                24.03.2021 22:04
                by robot"
        `);
    });
});
