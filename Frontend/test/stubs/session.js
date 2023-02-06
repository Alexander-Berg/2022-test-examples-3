class SessionStub {
    constructor(options) {
        const opts = options || {};
        const append = opts.appendToValues || '';

        this.timestamp = Date.now() + append;
        this.userIp = '192.168.1.1' + append;
        this.httpMethod = 'GET' + append;
        this.url = 'https://yandex.ru/search/touch?text=Hello+Kitty' + append;
        this.reqid = '1234567890-TST1' + append;
        this.ruid = '12345678901234' + append;
        this.service = 'turbo.yandex' + append;
        this.ui = 'touch' + append;
        this.headers = {
            host: 'some-machine101h.yandex.net:443' + append,
            cookie: 'test=123;' + append,
            'x-forwarded-for': '192.168.1.2' + append,
            'x-real-ip': '192.168.1.1' + append,
            referer: 'https://crunchy-site.com/page' + append,
            'user-agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5)' + append,
            'x-yandex-icookie': '9302043061499246905',
        };
    }

    /**
     * Serialize session object to string
     * using format of blockstat logs 1st section
     *
     * @returns {String}
     */
    serialize() {
        /* eslint consistent-this: 0 */
        const s = this;
        const h = s.headers;

        return [
            s.timestamp,
            h.cookie,
            s.userIp,
            h['x-forwarded-for'],
            h['x-real-ip'],
            h.referer,
            h['user-agent'],
            s.httpMethod,
            s.url,
            h.host,
            s.reqid,
            s.ruid,
            s.service,
            s.ui,
            h['x-yandex-icookie'],
        ].map(v => String(v).replace(/\t/g, '\\t')).join('\t') + '\t';
    }
}

module.exports = SessionStub;
