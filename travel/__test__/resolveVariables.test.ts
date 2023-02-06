import resolveVariables from '../resolveVariables';

describe('resolveVariables', () => {
    it('Строка без переменных возвращается без изменений', () => {
        const variables = {
            ['text-primary-color']: '#333',
        };

        expect(resolveVariables('#333', variables)).toBe('#333');
    });

    it('Строка с одной переменной возвращается со значением переменной', () => {
        const variables = {
            ['link-primary-color']: '#04b',
        };

        expect(
            resolveVariables('rgba($link-primary-color, 0.6)', variables),
        ).toBe('rgba(#04b, 0.6)');
    });

    it('Строка с несколькими переменными возвращается со значениями переменных', () => {
        const variables = {
            ['link-primary-color']: '#04b',
            ['text-primary-color']: '#333',
        };

        expect(
            resolveVariables(
                'rgba($link-primary-color, $text-primary-color, $link-primary-color, 0.6)',
                variables,
            ),
        ).toBe('rgba(#04b, #333, #04b, 0.6)');
    });

    it('Вложенность переменных больше одной', () => {
        const variables = {
            ['link-primary-color']: '#04b',
            ['aeroflot-color']: '$link-primary-color',
        };

        expect(resolveVariables('rgba($aeroflot-color, 0.6)', variables)).toBe(
            'rgba(#04b, 0.6)',
        );
    });
});
