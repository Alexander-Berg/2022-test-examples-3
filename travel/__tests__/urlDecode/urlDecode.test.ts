const urlDecode = require('../../urlDecode/urlDecode');

describe('urlDecode', () => {
    it('если строка содержит win1251 последовательности, должна вернуть декодированную последовательность', () => {
        const input = '%D1%E0%ED%EA%F2-%CF%E5%F2%E5%F0%E1%F3%F0%E3';
        const decoded = urlDecode(input);

        expect(decoded).toEqual('Санкт-Петербург');
    });

    it('если строка содержит html-последовательности, должна вернуть строку с текстовыми символами', () => {
        const input = '/?a=123&amp;b=123&amp;c=123';
        const decoded = urlDecode(input);

        expect(decoded).toEqual('/?a=123&b=123&c=123');
    });

    it('если строка – URL, должен декодировать правильно', () => {
        const input =
            'https://a.ru/trains/order/?adults=2&amp;coachNumber=08&amp;coachType=sitting&amp;expandedServiceClassKey=2%D1-withSchema-withRequirements-%C4%CE%D1%D1&amp;forward=P1_763%C0_9602494_2006004_2021-08-29T11%3A10&amp;fromId=c2&amp;fromName=%D1%E0%ED%EA%F2-%CF%E5%F2%E5%F0%E1%F3%F0%E3&amp;number=763%C0&amp;provider=P1&amp;time=11.10&amp;toId=c213&amp;toName=%CC%EE%F1%EA%E2%E0&amp;when=2021-08-29';
        const expected =
            'https://a.ru/trains/order/?adults=2&coachNumber=08&coachType=sitting&expandedServiceClassKey=2С-withSchema-withRequirements-ДОСС&forward=P1_763А_9602494_2006004_2021-08-29T11%3A10&fromId=c2&fromName=Санкт-Петербург&number=763А&provider=P1&time=11.10&toId=c213&toName=Москва&when=2021-08-29';
        const decoded = urlDecode(input);

        expect(decoded).toEqual(expected);
    });

    it('если строка – раскодированный URL, должен возвращается исходный URL', () => {
        const input =
            'https://a.ru/trains/order/?adults=2&coachNumber=08&coachType=sitting&expandedServiceClassKey=2С-withSchema-withRequirements-ДОСС&forward=P1_763А_9602494_2006004_2021-08-29T11%3A10&fromId=c2&fromName=Санкт-Петербург&number=763А&provider=P1&time=11.10&toId=c213&toName=Москва&when=2021-08-29';
        const decoded = urlDecode(input);

        expect(decoded).toEqual(input);
    });

    it('если строка закодирована через UTF-8, должен вернуть исходную строку', () => {
        const source = 'https://a.ru/abc?from=Санкт-Петербург&to=Пр обел';
        const input = encodeURI(source);
        const decoded = urlDecode(input);

        expect(decoded).toEqual(input);
    });
});
