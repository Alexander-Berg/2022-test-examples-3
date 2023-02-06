var expect = require("chai").expect,
    detect = require("../"),
    width = require("../width");

describe('width', function () {
    it('detect Galaxy Tab 10.1', function() {
        let ua = 'MQQBrowser/2.7 Mozilla/5.0 (Linux; U; Android 3.1; ru-ru; GT-P7500 Build/HMJ37) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13';

        expect(width(detect(ua))).to.equal(1280)
    });
});

