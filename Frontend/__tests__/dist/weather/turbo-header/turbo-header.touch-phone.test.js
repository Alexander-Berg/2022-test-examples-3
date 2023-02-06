const header = require('../../../../dist/weather/turbo-header.touch-phone');
const yaplus = require('../../../../dist/base/yaplus.touch-phone');
const login = require('../../../../dist/base/login-button.touch-phone');

const langsAndTlds = require('../../../tlds-and-langs');

const ctx = {
    hasSearch: true,
    serviceLogoUrl: '/test"<script>alert(1)</script>',
    logoTarget: '_blank',
    logoRel: 'noopener',
    loginHTML: login.getContent({ key: 'button', content: 'html' }),
};

const bundles = ['base', 'white'];

describe('turbo-header', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            bundles.forEach(key => {
                test(`key ${key}, tld ${tld}, lang ${lang}`, () => {
                    let content = header.getContent({
                        key,
                        lang,
                        tld,
                        content: 'html',
                        ctx: Object.assign({
                            yaplusHTML: yaplus.getContent({ key, content: 'html' }),
                        }, ctx),
                    });

                    expect(content).toMatchSnapshot();
                });
            });
        });
    });
});
