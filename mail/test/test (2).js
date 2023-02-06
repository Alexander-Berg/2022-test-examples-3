const assert = require('assert');

const uatraits = process._linkedBinding('uatraits');

const { Detector } = uatraits;

const browser_path = process.env.BROWSER_PATH;
const profiles_path = process.env.PROFILES_PATH;
const extra_path = process.env.EXTRA_PATH;


const ua = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36';


(() => {
    const detector = new Detector(browser_path);
    assert.deepStrictEqual(detector.detect(ua), {
        'BrowserBase': 'Chromium',
        'BrowserBaseVersion': '75.0.3770.142',
        'BrowserEngine': 'WebKit',
        'BrowserEngineVersion': '537.36',
        'BrowserName': 'Chrome',
        'BrowserVersion': '75.0.3770.142',
        'OSFamily': 'MacOS',
        'OSName': 'Mac OS X Mojave',
        'OSVersion': '10.14.5',
        'isBrowser': 'true',
        'isMobile': 'false',
        'isTouch': 'false',
    });
    assert.deepStrictEqual(detector.detectByHeaders({ 'user-agent': ua }), {
        'BrowserBase': 'Chromium',
        'BrowserBaseVersion': '75.0.3770.142',
        'BrowserEngine': 'WebKit',
        'BrowserEngineVersion': '537.36',
        'BrowserName': 'Chrome',
        'BrowserVersion': '75.0.3770.142',
        'OSFamily': 'MacOS',
        'OSName': 'Mac OS X Mojave',
        'OSVersion': '10.14.5',
        'isBrowser': 'true',
        'isMobile': 'false',
        'isTouch': 'false',
    });
}) ();

(() => {
    const detector = new Detector(browser_path, profiles_path);
    assert.deepStrictEqual(detector.detect(ua), {
        'BrowserBase': 'Chromium',
        'BrowserBaseVersion': '75.0.3770.142',
        'BrowserEngine': 'WebKit',
        'BrowserEngineVersion': '537.36',
        'BrowserName': 'Chrome',
        'BrowserVersion': '75.0.3770.142',
        'OSFamily': 'MacOS',
        'OSName': 'Mac OS X Mojave',
        'OSVersion': '10.14.5',
        'isBrowser': 'true',
        'isMobile': 'false',
        'isTouch': 'false',
    });
    assert.deepStrictEqual(detector.detectByHeaders({ 'User-Agent': ua}), {
        'BrowserBase': 'Chromium',
        'BrowserBaseVersion': '75.0.3770.142',
        'BrowserEngine': 'WebKit',
        'BrowserEngineVersion': '537.36',
        'BrowserName': 'Chrome',
        'BrowserVersion': '75.0.3770.142',
        'OSFamily': 'MacOS',
        'OSName': 'Mac OS X Mojave',
        'OSVersion': '10.14.5',
        'isBrowser': 'true',
        'isMobile': 'false',
        'isTouch': 'false',
    });
}) ();

(() => {
    const detector = new Detector(browser_path, profiles_path, extra_path);
    assert.deepStrictEqual(detector.detect(ua), {
        'BrowserBase': 'Chromium',
        'BrowserBaseVersion': '75.0.3770.142',
        'BrowserEngine': 'WebKit',
        'BrowserEngineVersion': '537.36',
        'BrowserName': 'Chrome',
        'BrowserVersion': '75.0.3770.142',
        'CSP1Support': 'true',
        'CSP2Support': 'true',
        'OSFamily': 'MacOS',
        'OSName': 'Mac OS X Mojave',
        'OSVersion': '10.14.5',
        'SVGSupport': 'true',
        'WebPSupport': 'true',
        'historySupport': 'true',
        'isBrowser': 'true',
        'isMobile': 'false',
        'isTouch': 'false',
        'localStorageSupport': 'true',
        'postMessageSupport': 'true',
    });
    assert.deepStrictEqual(detector.detectByHeaders({ 'User-Agent': ua}), {
        'BrowserBase': 'Chromium',
        'BrowserBaseVersion': '75.0.3770.142',
        'BrowserEngine': 'WebKit',
        'BrowserEngineVersion': '537.36',
        'BrowserName': 'Chrome',
        'BrowserVersion': '75.0.3770.142',
        'CSP1Support': 'true',
        'CSP2Support': 'true',
        'OSFamily': 'MacOS',
        'OSName': 'Mac OS X Mojave',
        'OSVersion': '10.14.5',
        'SVGSupport': 'true',
        'WebPSupport': 'true',
        'historySupport': 'true',
        'isBrowser': 'true',
        'isMobile': 'false',
        'isTouch': 'false',
        'localStorageSupport': 'true',
        'postMessageSupport': 'true',
    });
}) ();


// test('browser', () => {
//     const detector = new Detector('./test/browser.xml');
//     expect(detector.detect(ua)).toMatchSnapshot();
//     expect(detector.detectByHeaders({ 'user-agent': ua })).toMatchSnapshot();
// });

// test('browser + profiles', () => {
//     const detector = new Detector('./test/browser.xml', './test/profiles.xml');
//     expect(detector.detect(ua)).toMatchSnapshot();
//     expect(detector.detectByHeaders({ 'User-Agent': ua})).toMatchSnapshot();
// });

// test('browser + profiles + extra', () => {
//     const detector = new Detector('./test/browser.xml', './test/profiles.xml', './test/extra.xml');
//     expect(detector.detect(ua)).toMatchSnapshot();
//     expect(detector.detectByHeaders({ 'User-Agent': ua})).toMatchSnapshot();
// });
