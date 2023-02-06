import * as React from 'react';
import { Helmet, HelmetData } from 'react-helmet';
import ReactDOM from 'react-dom/server';

import { getAssetsFromHelmet, getHeadTagsFromHelmet } from './helmet';

// https://github.com/nfl/react-helmet/issues/310
Helmet.canUseDOM = false;

let helmet!: HelmetData;

beforeEach(() => {
    let App = (
        <Helmet>
            {/* <html lang="en" amp /> */}
            {/* <body className="root" /> */}
            {/* <base target="_blank" href="http://mysite.com/" /> */}
            {/* eslint-disable-next-line quotes */}
            <title itemProp="name" lang="en">My Plain Title or {`dynamic`} title</title>
            <meta name="description" content="Helmet application" />
            <meta property="og:type" content="article" />

            <link rel="canonical" href="http://mysite.com/example" />
            <link rel="apple-touch-icon" href="http://mysite.com/img/apple-touch-icon-57x57.png" />
            <link rel="apple-touch-icon" sizes="72x72" href="http://mysite.com/img/apple-touch-icon-72x72.png" />

            <script src="http://include.com/pathtojs.js" type="text/javascript" />

            <script type="application/ld+json">{`
                {
                    "@context": "http://schema.org"
                }
            `}
            </script>

            <noscript>{`
                <link rel="stylesheet" type="text/css" href="foo.css" />
            `}
            </noscript>

            <style type="text/css">{`
                body {
                    background-color: blue;
                }

                p {
                    font-size: 12px;
                }
            `}
            </style>
        </Helmet>
    );

    ReactDOM.renderToString(App);
    helmet = Helmet.renderStatic();
});

describe('react-helmet', () => {
    it('Корректно парсит ассеты из react-helmet', () => {
        expect(getAssetsFromHelmet(helmet)).toMatchSnapshot('parsed-assets');
    });

    it('Корректно парсит метаинформацию из react-helmet', () => {
        expect(getHeadTagsFromHelmet(helmet)).toMatchSnapshot('parsed-meta');
    });
});
