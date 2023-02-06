/* global blocks */
global.blocks = {};
require('./../../blocks-common/i-global/__set-http-header/i-global__set-http-header.priv');

const { assert } = require('chai');

describe('i-global__set-http-header', function() {
    it('should pass headers to report renderer', function() {
        const data = { expFlags: {} };
        const rrCtx = {
            headers: {},
            setResponseHeader: function(header, values) {
                this.headers[header] = values;
            }
        };

        blocks['i-global__pass-headers-to-report-renderer'](data, rrCtx);

        // для простоты первой итерации тестирования удаляем заголовок, зависящий от текущей даты
        delete rrCtx.headers.Expires;
        assert.deepEqual(
            rrCtx.headers,
            {
                'Cache-Control': 'private, max-age=300, no-transform',
                // Expires: 'Fri, 17 Jan 2020 13:22:20 GMT',
                'Accept-CH': 'Viewport-Width, DPR, Device-Memory, RTT, Downlink, ECT',
                'Accept-CH-Lifetime': '31536000'
            }
        );
    });
});
