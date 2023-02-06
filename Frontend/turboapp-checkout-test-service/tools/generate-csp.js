const fs = require('fs');
const { getCSP } = require('csp-header');
const csp = require('@yandex-int/csp-presets-pack');
const getCspHashes = require('@yandex-int/html-csp-hash-generator');

const htmlFile = 'build/index.html';
const html = fs.readFileSync(htmlFile, { encoding: 'utf8' });
const hashes = getCspHashes(html);

const staticHost = 'tap-test.s3.mds.yandex.net';
const staticPreset = {
    [csp.SCRIPT]: [staticHost, ...hashes.scripts],
    [csp.STYLE]: [staticHost, ...hashes.styles],
    [csp.FONT]: [staticHost],
    [csp.IMG]: [staticHost],
    [csp.MEDIA]: [staticHost],
};

const wsPreset = {
    [csp.CONNECT]: ['wss://checkout-test-service.tap-tst.yandex.ru'],
};

const paymentTokenPreset = {
    [csp.CONNECT]: ['https://cors-anywhere.herokuapp.com'],
};

const framePreset = {
    [csp.FRAME]: ['http://localhost:*', '*.yandex.ru'],
};

const frameAncestorsPreset = {
    [csp.FRAME_ANCESTORS]: ['http://localhost:*', '*.yandex.ru'],
};

const cspHeader = getCSP({
    presets: [
        csp.self(),
        csp.eval(),
        csp.data(),
        csp.yaStatic(),
        staticPreset,
        wsPreset,
        framePreset,
        frameAncestorsPreset,
        paymentTokenPreset,
    ],
});

process.stdout.write(cspHeader);
