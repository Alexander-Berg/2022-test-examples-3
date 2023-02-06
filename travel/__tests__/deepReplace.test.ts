import {deepReplace} from 'utilities/deepReplace';

describe('deepReplace', () => {
    it('Вернёт объект с подстановками', () => {
        expect(
            deepReplace(
                {
                    undef: undefined,
                    num: 7,
                    emptyStr: '',
                    emptyObject: null,
                    bool: false,
                    str: 'Hello, [[#target#world]]',
                    obj: {
                        arr: ['', 'Yeah!', 'My name is [[#name#Neo]]'],
                        name: '[[#name#Neo]]',
                    },
                },
                {
                    target: 'universe',
                    name: 'Grut',
                },
            ),
        ).toEqual({
            undef: undefined,
            num: 7,
            emptyStr: '',
            emptyObject: null,
            bool: false,
            str: 'Hello, universe',
            obj: {
                arr: ['', 'Yeah!', 'My name is Grut'],
                name: 'Grut',
            },
        });
    });
});
