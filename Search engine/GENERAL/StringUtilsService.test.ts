import {StringUtilsService} from './StringUtilsService';

const {defaultSnakeCaseFormatter, parseHTML} = StringUtilsService;

describe('StringUtilsService', () => {
    describe('defaultSnakeCaseFormatter', () => {
        test('should return expected results', () => {
            expect(defaultSnakeCaseFormatter('some_name')).toBe('Some name');
            expect(defaultSnakeCaseFormatter('')).toBe('');
            expect(defaultSnakeCaseFormatter('Text')).toBe('Text');
            expect(defaultSnakeCaseFormatter('somE_words Here')).toBe(
                'Some words here',
            );
        });
    });

    describe('parseHTML', () => {
        test('new lines and spaces', () => {
            expect(parseHTML('line1\nline2  \ntext  text ')).toEqual(
                'line1\n<wbr />line2 <wbr /> <wbr />\n<wbr />text <wbr /> <wbr />text',
            );
        });

        test('break long words', () => {
            expect(parseHTML('a'.repeat(60))).toEqual(
                'a'.repeat(40) + '<wbr />' + 'a'.repeat(20),
            );
        });

        test('wiki url', () => {
            expect(parseHTML('some (text) ((//ya.ru ya)) ((ya)) (())')).toEqual(
                'some <wbr />(text) <a href="//ya.ru" target="_blank">ya</a> <a href="ya" target="_blank">ya</a> <wbr />(())',
            );
        });

        test('plain urls', () => {
            expect(parseHTML('text https://ya.ru?param=1#hash text')).toEqual(
                'text <a href="https://ya.ru?param=1#hash" target="_blank">https://ya.ru?param=1<wbr />#hash</a> <wbr />text',
            );
            expect(parseHTML('text http://ya.ru?')).toEqual(
                'text <a href="http://ya.ru" target="_blank">http://ya.ru</a>?',
            );
            expect(parseHTML('text http://ya.ru.')).toEqual(
                'text <a href="http://ya.ru" target="_blank">http://ya.ru</a>.',
            );
        });

        test('startrek urls', () => {
            expect(
                parseHTML('text https://st.yandex-team.ru/QUEUE-1234'),
            ).toEqual(
                'text <a href="https://st.yandex-team.ru/QUEUE-1234" target="_blank">QUEUE-1234</a>',
            );
            expect(
                parseHTML(
                    'text https://st.yandex-team.ru/QUEUE-1234#comment1234',
                ),
            ).toEqual(
                'text <a href="https://st.yandex-team.ru/QUEUE-1234#comment1234" target="_blank">QUEUE-1234#comment1234</a>',
            );
        });

        test('startrek plain tikets', () => {
            expect(parseHTML('textQUEUE-1234 TEXT- QUEUE-1234')).toEqual(
                'textQUEUE-1234 <wbr />TEXT- <a href="https://st.yandex-team.ru/QUEUE-1234" target="_blank">QUEUE-1234</a>',
            );
            expect(parseHTML('QUEUE-1234#comment1234')).toEqual(
                '<a href="https://st.yandex-team.ru/QUEUE-1234#comment1234" target="_blank">QUEUE-1234#comment1234</a>',
            );
        });

        test('sanitize', () => {
            expect(
                parseHTML(
                    '<style>oh hi</style> text <script>good day</script>',
                ),
            ).toEqual('<wbr />text');
            expect(
                parseHTML(
                    '<b style="color: red">bold</b> <a href="//ya.ru">ya</a> <haha>hahaha</haha>',
                ),
            ).toEqual(
                '<b>bold</b> <a href="//ya.ru" target="_blank">ya</a> <wbr />hahaha',
            );
        });
    });
});
