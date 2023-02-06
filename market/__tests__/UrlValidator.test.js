import {URL_VALID_FN} from "../src/jsx/util";
import {expect} from 'chai';

describe("Check valid URLs", () => {
    const validUrls = [
        "http://yandex.ru",
        "http://екатеринбург.рф",
        "",
        null,
        undefined,
        "http://a.b--c.de/",
        "http://foo.com/blah_blah",
        "http://foo.com/blah_blah/",
        "http://foo.com/blah_blah_(wikipedia)",
        "http://foo.com/blah_blah_(wikipedia)_(again)",
        "http://www.example.com/wpstyle/?p=364",
        "https://www.example.com/foo/?bar=baz&inga=42&quux",
        "http://✪df.ws/123",
        "http://userid:password@example.com:8080",
        "http://userid:password@example.com:8080/",
        "http://userid@example.com",
        "http://userid@example.com/",
        "http://userid@example.com:8080",
        "http://userid@example.com:8080/",
        "http://userid:password@example.com",
        "http://userid:password@example.com/",
        "http://142.42.1.1/",
        "http://142.42.1.1:8080/",
        "http://➡.ws/䨹",
        "http://⌘.ws",
        "http://⌘.ws/",
        "http://foo.com/blah_(wikipedia)#cite-1",
        "http://foo.com/blah_(wikipedia)_blah#cite-1",
        "http://foo.com/unicode_(✪)_in_parens",
        "http://foo.com/(something)?after=parens",
        "http://☺.damowmow.com/",
        "http://code.google.com/events/#&product=browser",
        "http://j.mp",
        "ftp://foo.bar/baz",
        "http://foo.bar/?q=Test%20URL-encoded%20stuff",
        "http://مثال.إختبار",
        "http://例子.测试",
        "http://उदाहरण.परीक्षा",
        "http://-.~_!$&'()*+,;=:%40:80%2f::::::@example.com",
        "http://1337.net",
        "http://a.b-c.de",
        "http://223.255.255.254",
        "https://yandex.ru/images/today?size=2560x1600",
        "https://yandex.ru/images/search?text=%D0%BA%D0%B0%D0%B1%D1%80%D0%B8%D0%BE%D0%BB%D0%B5%D1%82%D1%8B&img_url=https%3A%2F%2Fget.wallhere.com%2Fphoto%2Fcar-vehicle-2015-Bentley-coupe-Convertible-Bentley-Continental-GT-netcarshow-netcar-car-images-car-photo-Continental-GT-Speed-convertible-wheel-supercar-land-vehicle-automotive-design-automobile-make-luxury-vehicle-personal-luxury-car-executive-car-bentley-continental-supersports-bentley-continental-gtc-bentley-continental-flying-spur-378484.jpg&pos=0&rpt=simage&nl=1"
    ];

    validUrls.forEach(url => {
        it(`given ${url}, URL_VALID_FN() should return true`, () => {
            expect(URL_VALID_FN(url)).to.be.eql(true);
        });
    });
});

describe("Check invalid URLs", () => {
    const invalidUrls = [
        "   ",
        "\t",
        "http://",
        "http://.",
        "http://..",
        "http://../",
        "http://?",
        "http://??",
        "http://??/",
        "http://#",
        "http://##",
        "http://##/",
        "http://foo.bar?q=Spaces should be encoded",
        "//",
        "//a",
        "///a",
        "///",
        "http:///a",
        "foo.com",
        "rdar://1234",
        "h://test",
        "http:// shouldfail.com",
        ":// should fail",
        "http://foo.bar/foo(bar)baz quux",
        "ftps://foo.bar/",
        "http://-error-.invalid/",
        "http://-a.b.co",
        "http://a.b-.co",
        "http://0.0.0.0",
        "http://10.1.1.0",
        "http://10.1.1.255",
        "http://224.1.1.1",
        "http://1.1.1.1.1",
        "http://123.123.123",
        "http://3628126748",
        "http://.www.foo.bar/",
        "http://.www.foo.bar./",
        "http://10.1.1.1",
        "http://10.1.1.254"
    ];

    invalidUrls.forEach(url => {
        it(`given ${url}, URL_VALID_FN() should return false`, () => {
            expect(URL_VALID_FN(url)).to.be.eql(false);
        });
    });
});
