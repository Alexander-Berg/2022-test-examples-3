import test from 'ava';
import {createElement} from 'react';

import render from '../../src/ssr.jsx';

import App from '../../src/bundles/index';

const appData = {
    tld: 'ru',
    lang: 'ru',
    nonce: 'foobar',
    staticHost: './static',
    bundle: 'index'
};

test('server-side rendering', async t => {
    const template = /<div\sid="mount"><div\sclass="app"\sdata-reactroot=""\sdata-reactid="1"\sdata-react-checksum="-?[0-9]+">.+?<\/div><\/div>/;
    return render('index', '/', appData)
        .then(({html}) => {
            t.true(template.test(html));
        });
});

test('client rendering', async t => {
    const app = createElement(App, {appData}, null);
    t.truthy(app);
});
