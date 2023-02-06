/* global describe, it */
'use strict';

const fs = require('fs');

if (!fs.existsSync('./browsers.js')) {
    console.error('browsers.js is not found - please run:');
    console.error('');
    console.error('npm run build');
    console.error('');
    process.exit(1);
}

const assert = require('assert');
const useragent = require('./');

describe('useragent.detect', () => {
    it('Mozilla/5.0 (Linux; U; Android 2.2; ru-ru; Desire_A8181 Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1', () => {
        const answer = useragent.detect('Mozilla/5.0 (Linux; U; Android 2.2; ru-ru; Desire_A8181 Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.OSFamily, 'Android');
    });

    it('Mozilla/5.0 (iPhone; CPU iPhone OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3', () => {
        const answer = useragent.detect('Mozilla/5.0 (iPhone; CPU iPhone OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.isTouch, true);
        assert.equal(answer.OSFamily, 'iOS');
    });

    it('Mozilla/5.0 (SAMSUNG; SAMSUNG-GT-S5250/S5250XEKC1; U; Bada/1.0; ru-ru) AppleWebKit/533.1 (KHTML, like Gecko) Dolfin/2.0 Mobile WQVGA SMM-MMS/1.2.0 NexPlayer/3.0 profile/MIDP-2.1 configuration/CLDC-1.1 OPN-B', () => {
        const answer = useragent.detect('Mozilla/5.0 (SAMSUNG; SAMSUNG-GT-S5250/S5250XEKC1; U; Bada/1.0; ru-ru) AppleWebKit/533.1 (KHTML, like Gecko) Dolfin/2.0 Mobile WQVGA SMM-MMS/1.2.0 NexPlayer/3.0 profile/MIDP-2.1 configuration/CLDC-1.1 OPN-B');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserName, 'Dolfin');
        assert.equal(answer.OSFamily, 'Bada');
    });

    it('Mozilla/5.0 (iPad; U; CPU OS 4_3_3 like Mac OS X; ru-ru) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8J2 Safari/6533.18.5', () => {
        const answer = useragent.detect('Mozilla/5.0 (iPad; U; CPU OS 4_3_3 like Mac OS X; ru-ru) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8J2 Safari/6533.18.5');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.isTouch, true);
        assert.equal(answer.isTablet, true);
        assert.equal(answer.OSFamily, 'iOS');
    });

    it('Mozilla/5.0 (iPod; U; CPU like Mac OS X; ru-ru) AppleWebKit/420.1 (KHTML, like Gecko) Version/3.0 Mobile/4B1 Safari/419.3', () => {
        const answer = useragent.detect('Mozilla/5.0 (iPod; U; CPU like Mac OS X; ru-ru) AppleWebKit/420.1 (KHTML, like Gecko) Version/3.0 Mobile/4B1 Safari/419.3');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.OSFamily, 'iOS');
    });

    it('Mozilla/5.0 (Symbian/3; Series60/5.2 NokiaN8-00/025.007; Profile/MIDP-2.1 Configuration/CLDC-1.1 ) AppleWebKit/533.4 (KHTML, like Gecko) NokiaBrowser/7.3.1.37 Mobile Safari/533.4 3gpp-gba', () => {
        const answer = useragent.detect('Mozilla/5.0 (Symbian/3; Series60/5.2 NokiaN8-00/025.007; Profile/MIDP-2.1 Configuration/CLDC-1.1 ) AppleWebKit/533.4 (KHTML, like Gecko) NokiaBrowser/7.3.1.37 Mobile Safari/533.4 3gpp-gba');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.BrowserName, 'NokiaBrowser');
        assert.equal(answer.OSFamily, 'Symbian');
    });

    it('Mozilla/5.0 (SymbianOS/9.4; Series60/5.0 Nokia5230/50.0.001; Profile/MIDP-2.1 Configuration/CLDC-1.1 ) AppleWebKit/533.4 (KHTML, like Gecko) NokiaBrowser/7.3.1.25 Mobile Safari/533.4 3gpp-gba', () => {
        const answer = useragent.detect('Mozilla/5.0 (SymbianOS/9.4; Series60/5.0 Nokia5230/50.0.001; Profile/MIDP-2.1 Configuration/CLDC-1.1 ) AppleWebKit/533.4 (KHTML, like Gecko) NokiaBrowser/7.3.1.25 Mobile Safari/533.4 3gpp-gba');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.BrowserName, 'NokiaBrowser');
        assert.equal(answer.OSFamily, 'Symbian');
    });

    it('Opera/9.80 (S60; SymbOS; Opera Mobi/498; U; ru) Presto/2.4.18 Version/10.00', () => {
        const answer = useragent.detect('Opera/9.80 (S60; SymbOS; Opera Mobi/498; U; ru) Presto/2.4.18 Version/10.00');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Presto');
        assert.equal(answer.BrowserName, 'OperaMobile');
        assert.equal(answer.OSFamily, 'Symbian');
    });

    it('Opera/9.80 (Windows NT 6.1; Opera Tablet/11648; U; en) Presto/2.7.81 Version/11.00', () => {
        const answer = useragent.detect('Opera/9.80 (Windows NT 6.1; Opera Tablet/11648; U; en) Presto/2.7.81 Version/11.00');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Presto');
        assert.equal(answer.BrowserName, 'OperaMobile');
        assert.equal(answer.isTablet, true);
    });

    it('Opera/9.80 (J2ME/MIDP; Opera Mini/4.2.18149/22.453; U; ru) Presto/2.5.25 Version/10.54', () => {
        const answer = useragent.detect('Opera/9.80 (J2ME/MIDP; Opera Mini/4.2.18149/22.453; U; ru) Presto/2.5.25 Version/10.54');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Presto');
        assert.equal(answer.BrowserName, 'OperaMini');
    });

    it('SAMSUNG-GT-S3600i/S3600iXXIL2 NetFront/3.5 Profile/MIDP-2.0 Configuration/CLDC-1.1', () => {
        const answer = useragent.detect('SAMSUNG-GT-S3600i/S3600iXXIL2 NetFront/3.5 Profile/MIDP-2.0 Configuration/CLDC-1.1');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserName, 'NetFront');
    });

    it('Mozilla/4.0 (compatible; MSIE 6.0; Symbian OS; Series 60/4.0839.42.2.1; 9712) Opera 8.65 [ru]', () => {
        const answer = useragent.detect('Mozilla/4.0 (compatible; MSIE 6.0; Symbian OS; Series 60/4.0839.42.2.1; 9712) Opera 8.65 [ru]');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Presto');
        assert.equal(answer.BrowserName, 'OperaMobile');
        assert.equal(answer.OSFamily, 'Symbian');
    });

    it('Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; HTC; 7 Mozart T8698)', () => {
        const answer = useragent.detect('Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; HTC; 7 Mozart T8698)');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Trident');
        assert.equal(answer.BrowserName, 'IEMobile');
        assert.equal(answer.OSFamily, 'WindowsPhone');
        assert.equal(answer.isTouch, true);
    });

    it('Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; HTC_HD2_T8585; Windows Phone 6.5)', () => {
        const answer = useragent.detect('Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; HTC_HD2_T8585; Windows Phone 6.5)');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Trident');
        assert.equal(answer.BrowserName, 'IEMobile');
        assert.equal(answer.OSFamily, 'WindowsMobile');
    });

    it('Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; XBLWP7; ZuneWP7)', () => {
        const answer = useragent.detect('Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; XBLWP7; ZuneWP7)');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Trident');
        assert.equal(answer.BrowserName, 'IEMobile');
        assert.equal(answer.OSFamily, 'WindowsPhone');
        assert.equal(answer.isTouch, true);
    });

    it('Mozilla/4.0 (compatible; MSIE 4.01; Windows CE; PPC; 240x320)', () => {
        const answer = useragent.detect('Mozilla/4.0 (compatible; MSIE 4.01; Windows CE; PPC; 240x320)');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Trident');
        assert.equal(answer.BrowserName, 'IEMobile');
        assert.equal(answer.OSFamily, 'WindowsMobile');
    });

    it('Mozilla/4.0 (compatible; MSIE 6.0; ; Linux armv5tejl; U) Opera 8.02 [en_US] Maemo browser 0.4.31 N770/SU-18', () => {
        const answer = useragent.detect('Mozilla/4.0 (compatible; MSIE 6.0; ; Linux armv5tejl; U) Opera 8.02 [en_US] Maemo browser 0.4.31 N770/SU-18');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Presto');
        assert.equal(answer.BrowserName, 'OperaMobile');
        assert.equal(answer.OSFamily, 'MeeGo');
        assert.equal(answer.isTouch, true);
    });

    it('Mozilla/5.0 (X11; U; Linux armv7l; ru-RU; rv:1.9.2.3pre) Gecko/20100723 Firefox/3.5 Maemo Browser 1.7.4.8 RX-51 N900', () => {
        const answer = useragent.detect('Mozilla/5.0 (X11; U; Linux armv7l; ru-RU; rv:1.9.2.3pre) Gecko/20100723 Firefox/3.5 Maemo Browser 1.7.4.8 RX-51 N900');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Gecko');
        assert.equal(answer.isTouch, true);
        assert.equal(answer.BrowserName, 'MobileFirefox');
        assert.equal(answer.OSFamily, 'MeeGo');
    });

    it('Mozilla/5.0 (X11; U; Linux armv6l; ru-RU; rv:1.9a6pre) Gecko/20080828 Firefox/3.0a1 Tablet browser 0.3.7 RX-34+RX-44+RX-48_DIABLO_5.2008.43-7', () => {
        const answer = useragent.detect('Mozilla/5.0 (X11; U; Linux armv6l; ru-RU; rv:1.9a6pre) Gecko/20080828 Firefox/3.0a1 Tablet browser 0.3.7 RX-34+RX-44+RX-48_DIABLO_5.2008.43-7');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Gecko');
        assert.equal(answer.isTouch, true);
        assert.equal(answer.BrowserName, 'MobileFirefox');
        assert.equal(answer.isTablet, true);
    });

    it('Nokia5228/UC Browser7.9.0.102/50/400/UCWEB', () => {
        const answer = useragent.detect('Nokia5228/UC Browser7.9.0.102/50/400/UCWEB');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserName, 'UCBrowser');
    });

    it('Nokia5230/UC Browser7.7.1.88/50/400', () => {
        const answer = useragent.detect('Nokia5230/UC Browser7.7.1.88/50/400');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserName, 'UCBrowser');
    });

    it('SAMSUNG-SGH-B130/B130XEHF1 UP.Browser/6.2.3.3.c.1.101 (GUI) MMP/2.0', () => {
        const answer = useragent.detect('SAMSUNG-SGH-B130/B130XEHF1 UP.Browser/6.2.3.3.c.1.101 (GUI) MMP/2.0');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserName, 'OpenWave');
        assert.equal(answer.isWAP, true);
    });

    it('Mozilla/4.0 (Ubiquam U-300; BREW/3.1) NetFront/3.1', () => {
        const answer = useragent.detect('Mozilla/4.0 (Ubiquam U-300; BREW/3.1) NetFront/3.1');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserName, 'NetFront');
    });

    it('HTC_Smart_F3188 Mozilla/5.0 (like Gecko) Obigo/Q7', () => {
        const answer = useragent.detect('HTC_Smart_F3188 Mozilla/5.0 (like Gecko) Obigo/Q7');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserName, 'Obigo');
        assert.equal(answer.isWAP, true);
    });

    it('Mozilla/5.0 (hp-tablet; Linux; hpwOS/3.0.4; U; en-US) AppleWebKit/534.6 (KHTML, like Gecko) wOSBrowser/234.76 Safari/534.6 TouchPad/1.0', () => {
        const answer = useragent.detect('Mozilla/5.0 (hp-tablet; Linux; hpwOS/3.0.4; U; en-US) AppleWebKit/534.6 (KHTML, like Gecko) wOSBrowser/234.76 Safari/534.6 TouchPad/1.0');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.isTouch, true);
        assert.equal(answer.isTablet, true);
        assert.equal(answer.OSFamily, 'WebOS');
    });

    it('Mozilla/5.0 (webOS/2.1.2; U; en-US) AppleWebKit/532.2 (KHTML, like Gecko) Version/1.0 Safari/532.2 P160UNA/1.0', () => {
        const answer = useragent.detect('Mozilla/5.0 (webOS/2.1.2; U; en-US) AppleWebKit/532.2 (KHTML, like Gecko) Version/1.0 Safari/532.2 P160UNA/1.0');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.OSFamily, 'WebOS');
    });

    it('Mozilla/5.0 (BREW; U; BREW 3.1.5; en) AppleWebKit/525.26 (KHTML, like Gecko) Polaris/7.0 Safari/525.26 480X800 LGE VX110002', () => {
        const answer = useragent.detect('Mozilla/5.0 (BREW; U; BREW 3.1.5; en) AppleWebKit/525.26 (KHTML, like Gecko) Polaris/7.0 Safari/525.26 480X800 LGE VX110002');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.BrowserName, 'Polaris');
        assert.equal(answer.OSFamily, 'BREW');
    });

    it('Mozilla/5.0 (Linux; U; en-US) AppleWebKit/528.5+ (KHTML, like Gecko, Safari/528.5+) Version/4.0 Kindle/3.0 (screen 600x800; rotate)', () => {
        const answer = useragent.detect('Mozilla/5.0 (Linux; U; en-US) AppleWebKit/528.5+ (KHTML, like Gecko, Safari/528.5+) Version/4.0 Kindle/3.0 (screen 600x800; rotate)');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.OSFamily, 'Linux');
    });

    it('Mozilla/4.0 (PSP (PlayStation Portable); 2.00)', () => {
        const answer = useragent.detect('Mozilla/4.0 (PSP (PlayStation Portable); 2.00)');
        assert.equal(answer.isMobile, true);
    });

    it('DiigoBrowser Mozilla/5.0 (iPad; U; CPU OS 5_0 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8J2 Safari/6533.18.5', () => {
        const answer = useragent.detect('DiigoBrowser Mozilla/5.0 (iPad; U; CPU OS 5_0 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8J2 Safari/6533.18.5');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserShell, 'Diigo');
        assert.equal(answer.OSFamily, 'iOS');
    });

    it('Opera/9.80 (Android 3.2; Linux; Opera Tablet/ADR-1111101157; U; ru) Presto/2.9.201 Version/11.50', () => {
        const answer = useragent.detect('Opera/9.80 (Android 3.2; Linux; Opera Tablet/ADR-1111101157; U; ru) Presto/2.9.201 Version/11.50');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Presto');
        assert.equal(answer.BrowserName, 'OperaMobile');
        assert.equal(answer.isTablet, true);
        assert.equal(answer.OSFamily, 'Android');
    });

    it('Mozilla/5.0 (Android; Linux armv7l; rv:8.0) Gecko/20111104 Firefox/8.0 Fennec/8.0', () => {
        const answer = useragent.detect('Mozilla/5.0 (Android; Linux armv7l; rv:8.0) Gecko/20111104 Firefox/8.0 Fennec/8.0');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Gecko');
        assert.equal(answer.isTouch, true);
        assert.equal(answer.BrowserName, 'MobileFirefox');
        assert.equal(answer.OSFamily, 'Android');
    });

    it('Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; HTC; Radar C110e)', () => {
        const answer = useragent.detect('Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; HTC; Radar C110e)');
        assert.equal(answer.isMobile, true);
        assert.equal(answer.BrowserEngine, 'Trident');
        assert.equal(answer.BrowserName, 'IEMobile');
        assert.equal(answer.OSFamily, 'WindowsPhone');
        assert.equal(answer.isTouch, true);
    });

    it('Opera/9.80 (Windows NT 6.1; U; Edition Next; ru) Presto/2.9.220 Version/12.00', () => {
        const answer = useragent.detect('Opera/9.80 (Windows NT 6.1; U; Edition Next; ru) Presto/2.9.220 Version/12.00');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'Presto');
        assert.equal(answer.BrowserName, 'Opera');
        assert.equal(answer.OSFamily, 'Windows');
        assert.equal(answer.OSName, 'Windows 7');
    });

    it('Mozilla/5.0 (X11; U; Linux i686; en; rv:1.9.0.19) Gecko/20080528 Epiphany/2.22', () => {
        const answer = useragent.detect('Mozilla/5.0 (X11; U; Linux i686; en; rv:1.9.0.19) Gecko/20080528 Epiphany/2.22');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'Gecko');
        assert.equal(answer.BrowserName, 'Epiphany');
        assert.equal(answer.OSFamily, 'Linux');
    });

    it('Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; ru; rv:1.9.0.19) Gecko/2011091218 Camino/2.0.9 (MultiLang) (like Firefox/3.0.19)', () => {
        const answer = useragent.detect('Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; ru; rv:1.9.0.19) Gecko/2011091218 Camino/2.0.9 (MultiLang) (like Firefox/3.0.19)');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'Gecko');
        assert.equal(answer.BrowserName, 'Camino');
        assert.equal(answer.OSFamily, 'MacOS');
    });

    it('Mozilla/5.0 (X11; U; Linux i686; ru; rv:1.9.2.15pre) Gecko/20110126 Ubuntu/10.10 (maverick) Namoroka/3.6.15pre', () => {
        const answer = useragent.detect('Mozilla/5.0 (X11; U; Linux i686; ru; rv:1.9.2.15pre) Gecko/20110126 Ubuntu/10.10 (maverick) Namoroka/3.6.15pre');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'Gecko');
        assert.equal(answer.BrowserName, 'Firefox');
        assert.equal(answer.OSFamily, 'Linux');
        assert.equal(answer.OSName, 'Ubuntu');
    });

    it('Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/15.0.874.106 Safari/535.2', () => {
        const answer = useragent.detect('Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/15.0.874.106 Safari/535.2');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.BrowserName, 'Chrome');
        assert.equal(answer.OSFamily, 'Windows');
        assert.equal(answer.OSName, 'Windows XP');
    });

    it('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.75 Safari/535.7 YI', () => {
        const answer = useragent.detect('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.75 Safari/535.7 YI');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.BrowserName, 'YandexInternet');
        assert.equal(answer.OSFamily, 'MacOS');
    });

    it('Mozilla/5.0 (Windows; U; Windows NT 5.1; ) AppleWebKit/534.12 (KHTML, like Gecko) Maxthon/3.0 Safari/534.12', () => {
        const answer = useragent.detect('Mozilla/5.0 (Windows; U; Windows NT 5.1; ) AppleWebKit/534.12 (KHTML, like Gecko) Maxthon/3.0 Safari/534.12');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.BrowserName, 'Maxthon');
        assert.equal(answer.OSFamily, 'Windows');
        assert.equal(answer.OSName, 'Windows XP');
    });

    it('Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.34 (KHTML, like Gecko) rekonq Safari/534.34', () => {
        const answer = useragent.detect('Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.34 (KHTML, like Gecko) rekonq Safari/534.34');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'WebKit');
        assert.equal(answer.BrowserName, 'Rekonq');
        assert.equal(answer.OSFamily, 'Linux');
        assert.equal(answer.x64, true);
    });

    it('Mozilla/5.0 (compatible; Konqueror/3.5; Linux 2.6.21-rc1; x86_64; cs, en_US) KHTML/3.5.6 (like Gecko)', () => {
        const answer = useragent.detect('Mozilla/5.0 (compatible; Konqueror/3.5; Linux 2.6.21-rc1; x86_64; cs, en_US) KHTML/3.5.6 (like Gecko)');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'KHTML');
        assert.equal(answer.BrowserName, 'Konqueror');
        assert.equal(answer.OSFamily, 'Linux');
        assert.equal(answer.x64, true);
    });

    it('Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Maxthon 2.0)', () => {
        const answer = useragent.detect('Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Maxthon 2.0)');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'Trident');
        assert.equal(answer.BrowserName, 'MSIE');
        assert.equal(answer.BrowserShell, 'Maxthon');
        assert.equal(answer.OSFamily, 'Windows');
        assert.equal(answer.OSName, 'Windows XP');
    });

    it('Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; Avant Browser)', () => {
        const answer = useragent.detect('Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; Avant Browser)');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'Trident');
        assert.equal(answer.BrowserName, 'MSIE');
        assert.equal(answer.BrowserShell, 'AvantBrowser');
        assert.equal(answer.OSFamily, 'Windows');
        assert.equal(answer.OSName, 'Windows XP');
    });

    it('Mozilla/5.0 (Windows NT 5.1; rv:7.0.1) Gecko/20100101 Firefox/7.0.1 YB/5.4', () => {
        const answer = useragent.detect('Mozilla/5.0 (Windows NT 5.1; rv:7.0.1) Gecko/20100101 Firefox/7.0.1 YB/5.4');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'Gecko');
        assert.equal(answer.BrowserName, 'Firefox');
        assert.equal(answer.YandexBar, true);
        assert.equal(answer.OSFamily, 'Windows');
        assert.equal(answer.OSName, 'Windows XP');
    });

    it('Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/4.0; MRSPUTNIK 2, 4, 0, 226; GTB7.1; MRA 5.7 (build 03797); SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729)', () => {
        const answer = useragent.detect('Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/4.0; MRSPUTNIK 2, 4, 0, 226; GTB7.1; MRA 5.7 (build 03797); SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729)');
        assert.equal(answer.BrowserEngine, 'Trident');
        assert.equal(answer.BrowserName, 'MSIE');
        assert.equal(answer.MailRuSputnik, true);
        assert.equal(answer.OSFamily, 'Windows');
        assert.equal(answer.OSName, 'Windows 7');
        assert.equal(answer.x64, true);
    });

    it('Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; GTB6; SLCC1; .NET CLR 2.0.50727; .NET CLR 3.0.04506)', () => {
        const answer = useragent.detect('Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; GTB6; SLCC1; .NET CLR 2.0.50727; .NET CLR 3.0.04506)');
        assert.equal(answer.isMobile, false);
        assert.equal(answer.BrowserEngine, 'Trident');
        assert.equal(answer.BrowserName, 'MSIE');
        assert.equal(answer.GoogleToolBar, true);
        assert.equal(answer.OSFamily, 'Windows');
        assert.equal(answer.OSName, 'Windows Vista');
    });
});
