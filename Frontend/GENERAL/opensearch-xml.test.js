describe('opensearch-xml', function() {
    const BEM = {};
    BEM.I18N = jest.fn((keyset, key) => key);
    BEM.I18N.lang = jest.fn();
    const main = require('./opensearch-xml.js')(BEM);
    let data;

    beforeEach(() => {
        data = {
            type: 'www',
            i18n: { language: 'ru' },
            reqdata: {
                ua: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36',
                device: 'desktop',
                device_detect: {
                    BrowserName: 'Chrome'
                },
                tld: 'ru',
                user_region: { id: 216364 },
                ruid: '1234'
            },
            cgidata: {
                hostname: 'yandex.ru'
            }
        };
    });

    it('should return xml (default)', function() {
        const expected = `<?xml version="1.0" encoding="UTF-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
                <ShortName>Яндекс</ShortName>
                <Description>Воспользуйтесь Яндексом для поиска в Интернете.</Description>
                <Image width="64" height="64" type="image/png">https://yastatic.net/s3/web4static/_/v2/ZcejnfbLE_TlMK13nS41mdC4A88.png</Image>
                <Url type="text/html" template="https://yandex.ru/search/?text={searchTerms}&amp;from=os&amp;clid=1836588"/>
                <Url type="application/x-suggestions+json" method="GET" template="https://suggest.yandex.ru/suggest-ff.cgi?part={searchTerms}&amp;uil=ru&amp;v=3&amp;sn=5&amp;lr=216364&amp;yu=1234"/>
                <InputEncoding>UTF-8</InputEncoding>
            </OpenSearchDescription>`;

        expect(main(data)).toEqualXML(expected);
    });

    it('should call BEM.I18N.lang()', function() {
        const langSpy = jest.spyOn(BEM.I18N, 'lang');

        langSpy.mockRestore();

        const lang = 'en';
        const testData = {
            ...data,
            i18n: { language: lang }
        };

        main(testData);
        expect(langSpy).toHaveBeenCalledTimes(1);
        expect(langSpy).toHaveBeenCalledWith(lang);

        langSpy.mockRestore();
    });

    it('should return specific xml for video', function() {
        data.type = 'video';
        const expected = `<?xml version="1.0" encoding="UTF-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
                <ShortName>Яндекс.Видео</ShortName>
                <Description>Яндекс.Видео: поиск видео в интернете.</Description>
                <Image width="32" height="32" type="image/x-icon">https://yastatic.net/iconostasis/_/kfr0xWUSbh-saWxe3AfE13Z0R1Y.ico</Image>
                <Url type="text/html" template="https://yandex.ru/video/search?text={searchTerms}&amp;from=os&amp;clid=1836588"/>
                <InputEncoding>UTF-8</InputEncoding>
            </OpenSearchDescription>`;

        expect(main(data)).toEqualXML(expected);
    });

    it('should return specific xml for images', function() {
        data.type = 'images';
        const expected = `<?xml version="1.0" encoding="UTF-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
                <ShortName>Яндекс.Картинки</ShortName>
                <Description>Поиск изображений.</Description>
                <Image width="32" height="32" type="image/x-icon">https://yastatic.net/images-thumbs/_/8cS_MbHl6VBweqVjWeI-eNy0tzE.ico</Image>
                <Url type="text/html" template="https://yandex.ru/images/search?text={searchTerms}&amp;from=os&amp;clid=1836588"/>
                <InputEncoding>UTF-8</InputEncoding>
            </OpenSearchDescription>`;

        expect(main(data)).toEqualXML(expected);
    });

    it('should return specific xml for news', function() {
        data.type = 'news';
        const expected = `<?xml version="1.0" encoding="UTF-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
                <ShortName>Яндекс.Новости</ShortName>
                <Description>Последние известия от электронных СМИ.</Description>
                <Image width="16" height="16" type="image/x-icon">https://yastatic.net/lego/_/l2V968dCa1zMr5TTWgVJQP6xPVU.ico</Image>
                <Url type="text/html" template="https://news.yandex.ru/search/?text={searchTerms}&amp;from=os&amp;rpt=nnews2&amp;grhow=clutop&amp;clid=1836588"/>
                <InputEncoding>UTF-8</InputEncoding>
            </OpenSearchDescription>`;

        expect(main(data)).toEqualXML(expected);
    });

    it('should return correct xmlns:ybrowser for YandexBrowser', function() {
        data.type = 'video';
        data.reqdata.device_detect.BrowserName = 'YandexBrowser';
        const expected = `<?xml version="1.0" encoding="UTF-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/" xmlns:ybrowser="http://browser.yandex.ru/opensearchextensions/">
                <ybrowser:ShortName>Видео</ybrowser:ShortName>
                <ShortName>Яндекс.Видео</ShortName>
                <Description>Яндекс.Видео: поиск видео в интернете.</Description>
                <Image width="32" height="32" type="image/x-icon">https://yastatic.net/iconostasis/_/kfr0xWUSbh-saWxe3AfE13Z0R1Y.ico</Image>
                <Url type="text/html" template="https://yandex.ru/video/search?text={searchTerms}&amp;from=os&amp;clid=1836588"/>
                <InputEncoding>UTF-8</InputEncoding>
            </OpenSearchDescription>`;

        expect(main(data)).toEqualXML(expected);
    });

    it('should path clid to url', function() {
        data.cgidata.args = { clid: [1234] };
        const expected = `<?xml version="1.0" encoding="UTF-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
                <ShortName>Яндекс</ShortName>
                <Description>Воспользуйтесь Яндексом для поиска в Интернете.</Description>
                <Image width="64" height="64" type="image/png">https://yastatic.net/s3/web4static/_/v2/ZcejnfbLE_TlMK13nS41mdC4A88.png</Image>
                <Url type="text/html" template="https://yandex.ru/search/?text={searchTerms}&amp;from=os&amp;clid=1234"/>
                <Url type="application/x-suggestions+json" method="GET" template="https://suggest.yandex.ru/suggest-ff.cgi?part={searchTerms}&amp;uil=ru&amp;v=3&amp;sn=5&amp;lr=216364&amp;yu=1234"/>
                <InputEncoding>UTF-8</InputEncoding>
            </OpenSearchDescription>`;

        expect(main(data)).toEqualXML(expected);
    });

    it('should set mob parameter for touch device', function() {
        data.reqdata.device = 'touch';
        const expected = `<?xml version="1.0" encoding="UTF-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
                <ShortName>Яндекс</ShortName>
                <Description>Воспользуйтесь Яндексом для поиска в Интернете.</Description>
                <Image width="64" height="64" type="image/png">https://yastatic.net/s3/web4static/_/v2/ZcejnfbLE_TlMK13nS41mdC4A88.png</Image>
                <Url type="text/html" template="https://yandex.ru/search/touch/?text={searchTerms}&amp;from=os&amp;clid=1836588"/>
                <Url type="application/x-suggestions+json" method="GET" template="https://suggest.yandex.ru/suggest-ff.cgi?part={searchTerms}&amp;uil=ru&amp;v=3&amp;sn=5&amp;lr=216364&amp;yu=1234&amp;mob=1"/>
                <InputEncoding>UTF-8</InputEncoding>
            </OpenSearchDescription>`;

        expect(main(data)).toEqualXML(expected);
    });
});
