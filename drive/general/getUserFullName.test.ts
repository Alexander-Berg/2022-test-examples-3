import { getUserFullName } from 'entities/User/helpers/getUserFullName/getUserFullName';

describe('getUserFullName', function () {
    it('works with empty params', function () {
        expect(getUserFullName()).toMatchInlineSnapshot(`"—"`);
    });

    it('works with params', function () {
        expect(getUserFullName('Ivan', 'Petrov')).toMatchInlineSnapshot(`"Ivan Petrov"`);
    });

    it('works with patronymic', function () {
        expect(getUserFullName('Ivan', 'Petrov', 'Ivanovich')).toMatchInlineSnapshot(`"Ivan Petrov Ivanovich"`);
    });
});
