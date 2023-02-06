import { testIOS, testSafari } from '../device';

describe('#testIOS', () => {
    it('должен вернуть true для iPhone', () => {
        expect(testIOS('Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_3 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) GSA/41.0.178428663 Mobile/15A432 Safari/604.1')).toBeTruthy();
    });

    it('должен вернуть false для IE', () => {
        expect(testIOS('Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 7.0; InfoPath.3; .NET CLR 3.1.40767; Trident/6.0; en-IN)')).toBeFalsy();
    });
});

describe('#testSafari', () => {
    it('должен вернуть true для Safari', () => {
        expect(testSafari('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/534.50.2 (KHTML, like Gecko) Version/5.0.6 Safari/533.22.3')).toBeTruthy();
    });

    it('должен вернуть false для Firefox', () => {
        expect(testSafari('Mozilla/5.0 (X11; U; OpenBSD i386; en-US; rv:1.9.2.8) Gecko/20101230 Firefox/3.6.8.')).toBeFalsy();
    });
});
