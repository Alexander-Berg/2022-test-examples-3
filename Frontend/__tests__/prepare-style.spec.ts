import { prepareStyle } from '../prepare-style';

describe('prepare-style', () => {
    it('Должен возвращать объект CSSProperties от строк', () => {
        expect(prepareStyle('background: red;margin-left: 2px; padding:0'))
            .toEqual({
                background: 'red',
                marginLeft: '2px',
                padding: '0',
            });

        expect(prepareStyle('transform:translate(7,42)'))
            .toEqual({
                transform: 'translate(7,42)',
            });

        expect(prepareStyle('background-image:url("http://my-example.com/test.png")'))
            .toEqual({
                backgroundImage: 'url("http://my-example.com/test.png")',
            });
    });

    it('Должен возвращать CSSProperties с правильными полями', () => {
        expect(prepareStyle({
            'background-url': 'url(//test.com/main.png)',
            marignLeft: '10px',
        }))
            .toEqual({
                backgroundUrl: 'url(//test.com/main.png)',
                marignLeft: '10px',
            });
    });
});
