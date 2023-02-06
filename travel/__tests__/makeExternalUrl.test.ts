import makeExternalUrl from '../makeExternalUrl';

describe('makeExternalUrl', () => {
    it('Вместо пробела используется "+"', () => {
        expect(
            makeExternalUrl('https://somedomain.ru/test', {
                test: 'space space',
            }),
        ).toBe('https://somedomain.ru/test?test=space+space');
    });

    it('Не добавит "?", если нет get-параметров', () => {
        expect(makeExternalUrl('https://somedomain.ru/test')).toBe(
            'https://somedomain.ru/test',
        );
    });
});
