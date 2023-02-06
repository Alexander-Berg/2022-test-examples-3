'use strict';

let stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    data_stub: {
        supplementary: {
            generic: [
                {
                    passages: [],
                    links: {},
                    headline_src: null,
                    headline: '',
                    type: 'generic',
                    applicable: 1,
                    by_link: '',
                    is_generic: 1,
                    attrs: {},
                    template: 'generic',
                    counter_prefix: '/snippet/generic/',
                    passage_attrs: [],
                    types: {
                        kind: 'snippets',
                        main: 'generic',
                        all: ['snippets', 'generic'],
                        extra: []
                    }
                }
            ],
            domain_link: [
                {
                    green_tail: 'search.xml?…',
                    green_domain: '…',
                    domain_href: 'http://market.yandex.ru/?clid=806'
                }
            ],
            url_menu: []
        },
        favicon_domain: 'market.yandex.ru',
        green_url: '…/search.xml?…',
        url:
            '//market.yandex.ru/search.xml?clid=806&cvredirect=0&text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8',
        server_descr: 'MARKET',
        markers: {
            WizardPos: '0',
            blndrViewType: 'market_offers_wizard',
            Rule: 'Vertical/market'
        },
        doctitle:
            '//market.yandex.ru/search.xml?clid=545&cvredirect=0&text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8',
        url_parts: {
            hostname: 'market.yandex.ru',
            scheme: 'http',
            cut_www: null,
            anchor: null,
            link:
                '/search.xml?clid=545&cvredirect=0&text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8',
            __is_plain: 1,
            query_string:
                'clid=545&cvredirect=0&text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8',
            path: '/search.xml',
            __package: 'YxWeb::Util::Url',
            port: null,
            canonical:
                'http://market.yandex.ru/search.xml?clid=545&cvredirect=0&text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8'
        },
        host: 'market.yandex.ru',
        num: '0',
        _markers: [],
        mime: '',
        signed_saved_copy_url: 'http://hghltd.yandex.net/yandbtm',
        is_recent: 0,
        construct: [
            {
                button: [
                    {
                        text: 'Еще 160 предложений',
                        url:
                            '//market.yandex.ru/search.xml?text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8&clid=545',
                        urlTouch:
                            '//m.market.yandex.ru/search?text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8&clid=708'
                    }
                ],
                counter: {
                    path: '/snippet/market/market_offers_wizard'
                },
                favicon: {
                    faviconDomain: 'market.yandex.ru'
                },
                geo: {
                    title: 'Адреса магазинов в Москве',
                    url:
                        '//market.yandex.ru/geo?clid=545&text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8',
                    'url-touch':
                        '//m.market.yandex.ru/search?clid=708&text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8#tab=map'
                },
                greenUrl: [
                    {
                        text: 'market.yandex.ru',
                        url: '//market.yandex.ru?clid=545',
                        urlTouch: '//m.market.yandex.ru?clid=708'
                    },
                    {
                        text: '\u0007[Шампунь\u0007] \u0007[для\u0007] \u0007[бесконтактной\u0007] \u0007[мойки\u0007]',
                        url:
                            '//market.yandex.ru/search.xml?text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8&clid=545',
                        urlTouch:
                            '//m.market.yandex.ru/search?text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8&clid=708'
                    }
                ],
                offer_count: 160,
                priority: '14',
                showcase: {
                    isAdv: 1,
                    items: [
                        {
                            greenUrl: {
                                text: 'InSafe.ru',
                                url: '//market.yandex.ru/shop/6640/reviews?cmid=aD0I0hF1llFD9pFY5gKZKQ&clid=545',
                                urlTouch:
                                    '//m.market.yandex.ru/grades-shop.xml?shop_id=6640&cmid=aD0I0hF1llFD9pFY5gKZKQ&clid=708'
                            },
                            price: {
                                currency: 'RUR',
                                priceMax: '2091',
                                priceMaxCent: '0',
                                type: 'average'
                            },
                            thumb: stubs.imageStub(75, 100),
                            title: {
                                text: '\u0007[Шампунь\u0007] \u0007[для\u0007] \u0007[бесконтактной\u0007]',
                                url:
                                    'http://www.insafe.ru/product/SHampun_dlya_beskontaktnoy_moyki_PLEX_STOFIX,_21_kg,_Sikmo.html',
                                urlForCounter:
                                    '//market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZv9WTxC1Od9yVudW-3hbjKFrknkg_tgJvMaLYm8hK4q8gONt817g3bzDj1oFIWA88Od637rLx1ridiT4tdc7OiM7HWo2b_uGQgju6klia1EizUDFxkPOPwZyJwLBYGq7B6ZIpDy37eJpLdlwrMW4b2B1PtM6VG9YuL483PNKKhu5WQYbd4meQhVSbb_U-E6zHlBI_dSaQXLeYLKYftjhGTSdOSUHvM-iRpWiDmWEUoYEFHc6G2WFsH0K1cV8Co77_pcgHkKlX23zUm9ngYA0oVIbBmZUtQR3wT0d60W1_m3RoMSDcXyvKVij95QQwv6yAnJDZ_-P_D1JYNcPn_PZ1I85eZ7IDG9pQXdsJfPEmSFc2hk32MroCE3fcWs5N8s5cKnyirLH5iO6u8OXvvXRokjB0BoZnLLuJ_ZpmPDCwA8oWuSERPS4lJ9wIOf7AVPEejP73xzhxeyv0Q03fRzTC0fCnzHb0z5uHq_kx3_z_nymnhHyH5JXx4i06FUJfKVBvgiPI6T4ajilMWMijmq6KlZ-uOGLqY3hYOkjcLIXPtT_QJY4IArHK3jYZgBllo1h8u3DkyPzW5kc0cyZh1ZngRDQEY8D8spVN5n5YYtXx62KxGs46o7Ke5ce-1wsFbWJztiNya09LrEA3s_1HbREl0QPX8iAq14jD-9LcmivrFCVQgEW8uCs-xx5dECgZ7ugIl3BUM2epQx827s_rucYulqr-0eQwPQ6LeXWwfqV4jSdmAtlA67GaBBE7ujek1QkQR3ZZ7-twwNQdcu81pzy_D36puLaepAhZCCL6E8ZnS_0OM1u3MdTWALHiBZNV_fRAY4ZeXQTeIyKaXi_HfIb4HTNapy1P83PcsmKd7j5d4vMrAo5e2iLleWy0ikWm4OI-O3C4MP8-bGEEzz1_BqdIxmTH8XuJFW65t6uzihNCcNc?data=QVyKqSPyGQwwaFPWqjjgNoPKm3bwzvSSIthF2gA2srGZA5NyX3oxd4SVCW71E1HlvqImTnBT9Lua9Hl0kIgrWz0MhRLhv6GKnc0Z-DA-5l92z3fr1OMW8Njm8NxjsMnwnNnRJbEilJL4AieXNe-zVmvXlQe--cyuNhzIK2yZvuoknLN1ritRdXYHBs5ePiIZ1tGKmd3xonhF_pLk3wemDeRlVt4kdd5IWCzXHWIs-bRzfAuXPBcaP1_PSMVGBU-cp8P34IgEGLYYokWbB6OrR3mvfyECUOGYbfvgTsi7NTcS67-O4Upxj7CjmYMuv99WHMLNrGxa_0p5S5peoslUMG6aTX7-TExqeBYtaUFAZ4PJaxE7OCUEkMAB0CI3SZexXVbjS4QD2C9nw3lFpK9ySahzl0xZEvXfKoMW59FkMOnhG844lZZPvl8DNWq9C4syEj1CNJQ7Wk-9mrDNq6YudFVgi8eulnPqH6FkQYwltFbGcT13r90EYe1NPVOn4tcoyywAZag4pgg-qW11cy1-RjIYrUIM5SWFprFMGwFDXIPcpnuY3-pLOF7BrHlcExJbhO1LbcSklZk,&b64e=1&sign=7c9f9b6a59813253b3174414cc51e5be&keyno=1',
                                urlTouch:
                                    'http://www.insafe.ru/product/SHampun_dlya_beskontaktnoy_moyki_PLEX_STOFIX,_21_kg,_Sikmo.html'
                            }
                        },
                        {
                            greenUrl: {
                                text: 'Ашан',
                                url: '//market.yandex.ru/shop/175488/reviews?cmid=nRbRFN-0mPjUGHgOhsYoIg&clid=545',
                                urlTouch:
                                    '//m.market.yandex.ru/grades-shop.xml?shop_id=175488&cmid=nRbRFN-0mPjUGHgOhsYoIg&clid=708'
                            },
                            price: {
                                currency: 'RUR',
                                priceMax: '471',
                                priceMaxCent: '0',
                                type: 'average'
                            },
                            thumb: stubs.imageStub(75, 100),
                            title: {
                                text: '\u0007[Шампунь\u0007] \u0007[Для\u0007] \u0007[Бесконтактной\u0007]',
                                url:
                                    'http://www.auchan.ru/pokupki/avtosham-d-besk-m-got-optim5l.html?utm_campaign=Moscow_YM&utm_content=http://www.auchan.ru/pokupki/avtosham-d-besk-m-got-optim5l.html&utm_term=976838&utm_medium=cpc&utm_source=yandexmarket',
                                urlForCounter:
                                    '//market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZtml8gbwaw_e26rxvUWbbaj0vShk7C6Ui_GBjj625ZXPmkFVeu_-COGuxRq7QqVWR7trTgPOh0onqt81S3AXeoYSczKnO4-s2150vRGFkELvy6JlO3cSd2NwLLsKROeV7meG1jm-RwQEAvD8GXnvNSP4VFzT0axedGuAILiN3XHF87UaRXlUrZ0yjVsO1tj4cVcQfv5Q5Wb_wl4xwmOD0W3BwOwDxksKY3vB94gnD45fLfs7RDiLPFO4_hg15hcM2BvwGw4Sv5m-cCymn7syPNMgMzMPtxoQDdhencgb4SXfKQyFcxXyOahrOMAVfiVqzZ4XJUBkqjaTrgmbdTE_v8da7xxVwx1MAN461OExP9Pc5luZsQvEWVJG2oLbvqSoe9B5h_5KtOCEpHAQNgzHnDNMjQdWyWAxpTjlpz4jDXy4ImL9rod3SUnE1AkZXEgGa3WHCtWCOpk8Qd1041B6zCEdF3olj05fo4Qi3llqYSbi5mkMIjIOQk8U5KLEbHB3CHtzNCQqYjcE_KXgHrXDhFQFMShZNuuXm8W3agSTyVsz8-I8SoCnHE1LguFvW7Zaz-5LVqnC3nacws6fKSabGMtaWfNEh13lbF1K5M_izEQnMakKRsDFssLHD_Cx6L6o4maRX-yslAjeAQWYIBjX0uWbYGSBBUzDgL1fyYfRb-ip6-lK7wfptXF70595mr-inoVbswOe2timXZYz_r6olSm5l8tIU6mXtc_5FvCytA20LB3RWkhDbnuE_0XpyGCfLjA0unOf3IEv5xohZDarfHwq7HtEkfLFzmVfoxBjT11QoICOIBjS5xDwS9do-YegkWdg4Xi1CGhwFFbnOiS1FnVs3qKtUPTFEaingEnJ30r4KldbeotvvDKV9Q7XjAgLIjRC7_oAlNIWpZY4uKInCYbx5at5q9VGhf_qFaHpkjXD?data=QVyKqSPyGQwwaFPWqjjgNjRJCxuqRWIErIjtKT9XSwIhEfnzMLUa-sR5xQsPsFS90umPFvKxpQPEUy6DNgvjxJdmLyCikDdMDY-Qsc-0umDvmjDQ3kcjqf45iloXJRbdGKMwn8yUVee79TCn7a2lfXSMBPycDNCM4FI1sqMHAZXWuSdO0y6xJ5vxWaYZAAGAaGb6kmjO6zfaSEgqlnOP459Cmah64w4QVzSoCSYYNbFuD4Bzjw00hT7ofM9RinF2rd8cd1xiyAgioS5erUR3znfjHvXEMmtxGrtTEvPj9EHNxbNNbUvQCF6qHaU9sv3avGP483i_aCEJLklJ1Z9jKLx5Jz5oBXN0GaqFi0GGz49h8q4DFRuPCc1fvM_PdZOBQ5hAgIAatmOslzoE6rlpxeB2bnC2mCqyKYOjw-EtkGVZ2z6JrupG1qiNFECUiXi1X1hnPoVA1aLWGTVuH_r64aGoLX-OfpJMuXnQP9m1i85oNbGMQ4gqFalPAWWqeT7_DO9msVU5vpE,&b64e=1&sign=20ccb7371f1b4de82cb0c3c06edcd6b9&keyno=1',
                                urlTouch:
                                    'http://www.auchan.ru/pokupki/avtosham-d-besk-m-got-optim5l.html?utm_campaign=Moscow_YM&utm_content=http://www.auchan.ru/pokupki/avtosham-d-besk-m-got-optim5l.html&utm_term=976838&utm_medium=cpc&utm_source=yandexmarket'
                            }
                        },
                        {
                            greenUrl: {
                                text: 'Кувалда.ру',
                                url: '//market.yandex.ru/shop/37437/reviews?cmid=3mOW9G3_D75hC3ChS6rDKQ&clid=545',
                                urlTouch:
                                    '//m.market.yandex.ru/grades-shop.xml?shop_id=37437&cmid=3mOW9G3_D75hC3ChS6rDKQ&clid=708'
                            },
                            price: {
                                currency: 'RUR',
                                priceMax: '230',
                                priceMaxCent: '0',
                                type: 'average'
                            },
                            thumb: stubs.imageStub(75, 100),
                            title: {
                                text: '\u0007[Шампунь\u0007] \u0007[для\u0007] \u0007[бесконтактной\u0007]',
                                url:
                                    'http://msc.kuvalda.ru/catalog/5688/35394/?utm_source=yandex.market&utm_medium=cpc&utm_content=81684&utm_campaign=atas',
                                urlForCounter:
                                    '//market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZsAn40UVHe-MCmeuQIjl5c98BiMazohc6A4bemF_GTFEeN13Aa3-hHoqKbOqpxsVwFDjWdD5_9ejwZVlUdMgWr5BTHHKLrPUA2TwMEEsUTEpElpfQ3uwdpb7c995eHT9roVbP104nZdpsgGWJ1fQDDYr-kGtsyWcrLd_b39_1spg81Hl91VPzaQNcNtZ8YiwVfDCHfFTokWHN26XADyP71PNwgV1FXNTE5vViHFOvpvpS7cZH3ixGjQEPicQSR6pkUE9gwPBxik7JScs3PR72kpCOpjK9L0s0ByNvkGi8PTM3MLXzAxYj4bjDUf6zRnIi9Nn0zepNNfRxqCGgYQNrOEAK9uJTpjKpNBFk4Qd5uYmnVmi4pZAYp4UoMVZBpYTbIlFkuhOoZiCnVRnMMpBWALJTGXoC_6MGiPnZZ2wSJGSjLh4pX3NsmZTsKZRIu4-yokW9OIuYixck0dtFhZYSs6IJqgs6URKln7LcFzJYTpZQqsYKd8FTPispG98Y351IQzWWQTScFlORV-JP7nF3HeTSp-NhNuViquxDgh70QGhf4zRVjTRMGhPlR4qu-5LZlhGJypjU6ZbOY8Vos2xWth8_p4OUsvYADiPyYyD-cGk1fO99dqm5p2pWTalkJr3G8L6868bQ7HmJ5iO9fhV91s0mFUQENoKE4bNZlixfM7O_TXsU8OaHgoXua8x6dMwKtwLzQR-SV2gaEOcQZcm2Fm0vhAs7rdr0qYChyXt4eur-Ug61MebRHjcBTmdgzvnO5-6yZ0biVr0rZJmXlJwzZXIoUEdp5JWLVjXAkPvIvEzqxB2QSfesm6pD5i3AQ-cQYKGTW6ZjLXzW0oFzIY0pMXdc-lSBzmGLvF7SDdE1dWLNkM-fIVWf_mhu3i3ja10nrftiX7CLBsvhOEkklClDwOoElrIkLjmcRGzjjtpgZeh?data=QVyKqSPyGQwwaFPWqjjgNkVT5XsGzs9IbqnsbGsCVNR31YoQqbQZum9MrkG0ktwVFtk4pnkh2wVAqNrzzh0ezmk5yzA-KaisYt9H2FCjwpKnHo1HQVoGtFSUP8ZTlV1PZHU8VOD4noIXOtYdzTobSbk4MSRbpo9dgfEQ0suYPUWEZLV-n5B5WJJLGj1qw0DM0ZcR7PO1SYMCR_5HxXsobRA4n8jZKTop57zC5Y2CR6ySC_if0nYsTvo0oDhnV9ZzvzUtCjIlqoo9viN94FNNNrVo9-aDcMmTLquRnmzGcdCoCxpzQ8o48lHm3LUNvPcZZV0244P4sZXMyFuZLffdKj6UYt8MYwMYKE0pTktCrrZSXJKrcwmZLGNDdxLSIAlY&b64e=1&sign=907f5bc46edbb1bd2a027028019abdcb&keyno=1',
                                urlTouch:
                                    'http://msc.kuvalda.ru/catalog/5688/35394/?utm_source=yandex.market&utm_medium=cpc&utm_content=81684&utm_campaign=atas'
                            }
                        },
                        {
                            greenUrl: {
                                text: 'NiceBike',
                                url: '//market.yandex.ru/shop/145570/reviews?cmid=g0bwwduKw1Z8D5-1iXpA_w&clid=545',
                                urlTouch:
                                    '//m.market.yandex.ru/grades-shop.xml?shop_id=145570&cmid=g0bwwduKw1Z8D5-1iXpA_w&clid=708'
                            },
                            price: {
                                currency: 'RUR',
                                priceMax: '200',
                                priceMaxCent: '0',
                                type: 'average'
                            },
                            thumb: stubs.imageStub(75, 100),
                            title: {
                                text:
                                    '\u0007[Шампунь\u0007] \u0007[для\u0007] \u0007[бесконтактной\u0007] \u0007[мойки\u0007]',
                                url:
                                    'http://nicebike.ru/accessories_dlya_velosipedov/velohimiya/velohimiya_daytona/shampoo_for_touchless_wash_daytona_1l.html#16998',
                                urlForCounter:
                                    '//market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZhByOn2HHLqXFukLohlqPI7do25tW1cfV4dBfWrnrL2J4LMuBgnHV7twa0nD-SAE9UsC__QrLU0uQ2b1pnKxSLGXIhboAxCOcJ2IhslcJKJxSo8sPMHkOxjF_8VA_p3O8OoKZqao-7BQzWEYTftQi8RwsKBQVLq-n7mhfHIszGRvYy8OpDnmGhzr4Mbs2NqxoaahMYfQCdu5gISwvhb18VO_92VcsCsWiDgNoKy4IR-D06b0uIzDuCbSDwexhYS0csaZkLO8Kf60xxmZcPI_0_kQeKTOiz3EHeTNJQGvJGBF91OAluqiYYjK9PXxCzMo9RPjsYLsGuDJhWFluty0zrLAt38-Zy2VpCLsXZ04nE5pubeKaWRv8Hl9QUjbPVzTEDJVSnzYOx3Qq89x9NrRjY8qVo1rsTNlj3TBWhhffLg85JdyRflgA9Qf0F2rN_RsXoyMaTibzSjWhqWeSb2AtJRIlqXQ9HR-wo5aYvIWeH12vfm-o_c9yoiPImuCYbsKGA9F0TqYxGKlwh7GdIO-UW322jTZhaxLeMLbf0-PuiobU0Qk14SvIXP-XnV1WP6awfA3P6nyp0ixh03Y9tSzER9lpZ3VdbzRutU2IouQdorx45eMeqj2gMV4nLEJxJfhEX8SLQtmVmMhJlhOfaG8WEiTa5kbs3ABFsaiMcdPpK10dVEXBFImMypO7vXva4jZgeQr7VBGJrt2J3QkDrYK7ACbZFzZ8D6Ii4omHsXePXLnJfnew7hTjjaGUyKlpE8nY9Mbi7j3W0YRrNKw2fBHoGj6d0u8Q7P5eaXhMpjol7rD6PsBwPOBRsWuaNswTl4SCeL0QqjFvnOksJZHh7IiarThqIqlBgBKxA1amNUEG_W4Uyi7v04RxXqifSpzmy3lj27oHofivWE9GEVUJESQkxIcj46PCVxutSSjoFr1JTwH?data=QVyKqSPyGQwwaFPWqjjgNh8XYNZoPRgij2_PFQamqbBYLGHLv4DqqKE_aM93UNwbdcUjkHa__52HmkWhX1Oss8fJeuEG7AVYYZnxqKCEKgyhovbMveseWp7QYXHbs695lcE2ncyfMr7f2lfJjHF5tASkMqfVm5d21QarcyySBGySvkpbkgKOQ5AJmrTdsckpr4EpgGLbtKMo7xLftyj80Xf_jq27aY39M5iJP1rDXy1Ydo23ymh6PJ9dwuvFxfmHpZcHf7o3-YS3w5zHoPoml1fIuMtfbFt977z3JMNIlryUyiIKtql3P2qwEHAkTNpGTF0Hz7si2sNH6_sYk6rzRYerm208P_JmopDXCrsVt3Cy764zWzWkVhiqiB6kn9q_yTWpXzjjcKIS01ebqkYg4gDeImnm_Z4oVN9Nru3ImQrXETBHJwyLi8vxxaf7kmkzyM3_zpeqHZfSba2OR03aFYYooLNzUE-ee3DYi61fhxuMHYbuE1dw_BfiFLTwJewVzogB8s0oJ2NfXKWMt0TNBtQcYT1p6DCPzTiFm0ITeO8pfqFLTjIXHmVjZPFs00OZbCVFQfm0t9CbrsY6eVSuC6cG8iLN-x0ilweFFk2rShkpFUcyef1nzLbxlPji3Ra6&b64e=1&sign=bd885174eb76b32ade858676af86a990&keyno=1',
                                urlTouch:
                                    'http://nicebike.ru/accessories_dlya_velosipedov/velohimiya/velohimiya_daytona/shampoo_for_touchless_wash_daytona_1l.html#16998'
                            }
                        },
                        {
                            greenUrl: {
                                text: 'ВСТРОЙКА СОЛО',
                                url: '//market.yandex.ru/shop/30297/reviews?cmid=8lYrAknijDt1dQXMvPmY6w&clid=545',
                                urlTouch:
                                    '//m.market.yandex.ru/grades-shop.xml?shop_id=30297&cmid=8lYrAknijDt1dQXMvPmY6w&clid=708'
                            },
                            price: {
                                currency: 'RUR',
                                priceMax: '313',
                                priceMaxCent: '0',
                                type: 'average'
                            },
                            thumb: stubs.imageStub(75, 100),
                            title: {
                                text:
                                    '\u0007[Шампунь\u0007] \u0007[для\u0007] \u0007[бесконтактной\u0007] \u0007[мойки\u0007]',
                                url:
                                    'https://vstroyka-solo.ru/?p=goods&act=card&goodId=302395&utm_source=yam&utm_term=302395&from=yaMarket',
                                urlForCounter:
                                    '//market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZoofENoCBxXwlGLLTvY932HZGsVH4xMZeQWFu35Da-HcVmL6Pt-VS2QwebelZzHqSRN_2WRY1gM4afzz2EJSYcO9X7UsKU62Od8kh6er36EayFwU-LU6nM_QyG8D4sk3cnZtVNHRkN_f7Lo9nkpUoI20NZr5pooU4fL9ShzooBVfQDaG-NfwTwDziJFrXUEujgfkLwMbyQa-NNctgYukMsxh-7M39Ayy38FtVm4r5DCKikjrdWdl4Jfu5iS38m7BdZsd09qU57tS3P-2YXT_WUGfwfgScWe-AjQdUDU2Qm5ouZFUbcVkz6JkvPddGpusKPC16V7qaBwjnpDdtdYR-am_NDjHFDKFr4E5VihDB2gDIbL3iVzsTNWYAHTVDumRc-SEZbtVyMkiwXCQLvh7B-vhgfhaWBhBF8_ZUB2yAdXMS7WaMh5IHzWxNxI4FE-qw5an4wq_IjPEL_08UcL-EwBoJWpoUj1l9BpvdWXT-ttunuLav5kSP6pTaeOpVKEfbxwlYp93V-saxeehjLFSYmcpzABV_UWPmCgM69BGKceqRsloG4j2K0M6syCdH0bRlVvNU-UBzQJqnIZdSWZe6SPagOlyREqTplsESt0jjFd6Qt0i2eeJiMAc1sGiwxchQKDmfVlGJyx1friOHKCppGVXRHenRwVjar6_kGuIRUdVmwDpK_oeA7bkvBR458osv3DJerpziraCo8UikVp6Fe4H1Rz8KUfl9PVnttN-LUyrZ0vsiDdfvwPFqcnmw-0-A-CjAXNZkZ9c2u-M8s2ZLASDNWCf5s4sY2BBDfyH0FlBaur1scvEIl7OHJdsQYzuCFi3x12h5BZ2p3ghhuPdrd-9wIjnzCXpPQ8niMbtqeq20WhW7SlniuVsOSVEMbPRinSUmrRqRhvS1UOsMz-hrZlrmL0pSW0KE6_6VrhsN_7b?data=QVyKqSPyGQwNvdoowNEPjeQL9NT42zkWV293sml-HIEidGZ3FpexR_X3kqUhIwNYNGMtPo1sA0usJUDj_4vDVdbzvbFjGeXgaP36677eD3s3MhWt2a59OS99RAVQcURaBbx3Mbj4VIT2puR9_jyl84Aut59GvsGsqzggAMc4EOAHLqqZctR8vakEZTbLiqWGloRck36XZZ9D8VSbheHVDlravsWH9J3GEtEJpy89T18N-SBU_0is40vapsY1qL6Y3eVeXuuL3sgs87sB-lFTSKqf5D6KRKP9zy11E4qBVXdmAsG8U0vQlLuO5jtDGJOSnXcolxuUi40PhSRHeSHbAF0jlD85Uod93Ie3WKnS37aO8r0Teuhk3zh4-SiHDXuMEJvKkxAayCNRCiOQ7HQqVU14iwbeaeQjcY23u7IHWkHLbp8GdBAXg-aVJ2qn5IGKd8gljfM-wyLD0KmO3Qwbugm_rpy9J4nwcyqRLxUEaFc0h4okd9n5NE4pJgfgsNL5lxKZ4F7sJsJmh93LbVz-4U04Qp1wRe011YZspJSdeGupftieNJGcnH9qOtjRfTnk4FNoARcRxCVfPL2CwfGaLT6r3ZBT9Ze7GgaAKLglen7vTvV9O-LWHSfgnYmvpdhTyguMioMYJ0w,&b64e=1&sign=479bade6451cefd9b7ddb2294917b810&keyno=1',
                                urlTouch:
                                    'https://vstroyka-solo.ru/?p=goods&act=card&goodId=302395&utm_source=yam&utm_term=302395&from=yaMarket'
                            }
                        },
                        {
                            greenUrl: {
                                text: 'ОНЛАЙН ТРЕЙД.РУ',
                                url: '//market.yandex.ru/shop/255/reviews?cmid=c78SSXdXChSMlVDMg23ulQ&clid=545',
                                urlTouch:
                                    '//m.market.yandex.ru/grades-shop.xml?shop_id=255&cmid=c78SSXdXChSMlVDMg23ulQ&clid=708'
                            },
                            price: {
                                currency: 'RUR',
                                priceMax: '230',
                                priceMaxCent: '0',
                                type: 'average'
                            },
                            thumb: stubs.imageStub(75, 100),
                            title: {
                                text: '\u0007[Шампунь\u0007] AUTOPROFI',
                                url:
                                    'https://www.onlinetrade.ru/catalogue/avtoshampuni-c1787/autoprofi/shampun_autoprofi_dlya_beskontaktnoy_moyki_1000ml_150802-575856.html?utm_source=market.yandex.ru&utm_medium=cpc&city=1',
                                urlForCounter:
                                    '//market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZn1-wos-Xx6Tqhgy5ftyu59Cb0R8ouzZivfPMp7uAjdPQjG3QD11Vdhja9UD8gAtb1TWGliHbHWCptsgbFrnzYQyZ7-v-i25_FiZY44zWluiKRvNWZ5Nk8dIAm1fNN0Stg_BgxBxbARUbSdGfJiUV4Lo-xpJlfKk7OJRkjXiHjhWAqyOWBOE1_sDbI8ses6jwDmU0wzDP4VjvTN0pBbpFOut2ctEZojelArpcp94Gq3LmWxNg-4bTBUuGXWhtP0qtCh9mY2tDBFSgUnxd3_ky5WhIDxcoGnRetiLlQ-LAlCrvR-dMOTgojD4XqT3H4jS0V2-un7D95LdUlhHaBK-VqCgCOpnPTCLtPArtGZbmxOBexNvwaXlNr_icIvFxLqiB9h3yUuS-T2oCDZdzHhmg0aw0oH5oTZKHMhtN1xrq4QmHeFzzbR_7336-QHvjWO0Qd-DrndQDuJrfueJCvlA5NLnqmKz6yJRjyNwQWgZSY9rQQ5HQtiOz_W-4qWJDwX6GEe_LBsaiGRy_EF7p_ej4JOvSiHuw-ayYpkVkhJTjAuA7-pkXTKUmqhEGCbByIcS9nTsUaGE0h4Grqf_UGA7IZevAI6IK6l_Ni3PNGxFG64_He1dLL2xZavdLM0QSisHlocfMB_bk21U3pichvVmn58IiAi4cmfgI-JIXmrgT6YLzY1Lt12XepYb8G8kz02Np2rMCCUjMzpKLIxiwbqTRiNKnODwI1vnMPU7j-R_wiKgoOI0IFb_tzy7Hadt8WfRek61m4CSF_sgh8U5OGG-E-8YXToFpymbfCszaYsWgutRS4c2o7uVhT7_S9qZufkCMtaXENHwdxnZkVsnSyeCyZjskrhAdckSroMm4WDlQrxIZe1-mD2za6j49N53Pv761DxrSpkCwTdxByQ9l2vR-duMPTkTmkiorqO696kgaZ14?data=QVyKqSPyGQwNvdoowNEPjeHCAo-a2AEEMgvw6Gv7rodwqTifDSzaDcFSJWWZewOkQ3BIk3emtrS4yVR2VFF2n-znqQ8sxWo5h91TMkRTyYrj5wiRXMLzpiRy6MV3yXwTLOBMnVK2QUlgut_D2EncmunWxfiA7UcZWn-ttpE45Tcj8S1BBOndwMV7HQXDKyYgNjuy1Sbjg8uUy1HGUejKyOaTc46u8tTOAa_PGFurdu8LC6ehUkB_u1VCnbF7qNkA5kHKxPLkJA19v4eIOxTWuQIwQ3BHT02TMo_C9PvKuaViGgtegDBF6I9R8vP6DY0tTQHgjzoEA14z9vlc9p2w_au-Kr1GLmkit3qPeOSt8D8UzVLosO8HJJCiyRREdu-tlsGwCZSxy7f7YHpwF2qHaBOOCQLA-p4QrR724WSlPeI-Gw1-PlkDPfZEvCuJ2EcsTgDmG0q01LmMYDqaTKhNuBBAiiTPHj-G-XCyBY4j8kDtuThOatc5fEZo4pQ6dHtDBK186rAfmfkc8i9pWXY0c8iiHjVbQ2JOjZzvJTOYsu_r6_l4N8ignM5NGJkxZRLvnCK2lQ3p8YDel8GQkDHYq357vX94q1Vqqu93btCipyrWIlHnt7agP3mmkezMMyumkQuT9_7JakqH2lQqvSuSfQLwN2vldmhtXz1qrTUHnvafmssWllPw3g,,&b64e=1&sign=2be38ce31a812d2a8bdcccf9ce146874&keyno=1',
                                urlTouch:
                                    'https://www.onlinetrade.ru/catalogue/avtoshampuni-c1787/autoprofi/shampun_autoprofi_dlya_beskontaktnoy_moyki_1000ml_150802-575856.html?utm_source=market.yandex.ru&utm_medium=cpc&city=1'
                            }
                        },
                        {
                            greenUrl: {
                                text: 'TOP-SHOP.RU',
                                url: '//market.yandex.ru/shop/3749/reviews?cmid=BfCRQzhyMeX5fWtFkRwtRw&clid=545',
                                urlTouch:
                                    '//m.market.yandex.ru/grades-shop.xml?shop_id=3749&cmid=BfCRQzhyMeX5fWtFkRwtRw&clid=708'
                            },
                            price: {
                                currency: 'RUR',
                                priceMax: '3088',
                                priceMaxCent: '0',
                                type: 'average'
                            },
                            thumb: stubs.imageStub(75, 100),
                            title: {
                                text:
                                    '\u0007[Шампунь\u0007] \u0007[для\u0007] \u0007[бесконтактной\u0007] \u0007[мойки\u0007]',
                                url:
                                    'http://www.top-shop.ru/product/576501?csu=general&cex=1559807&med=om&src=ru_gen_general_om_sa_yandex-market_v1&referrer=yandex-market_om_1559807&utm_source=yandex-market_ext&utm_medium=Off-Site_Merchandising&utm_term=576501&utm_content=20002&utm_campaign=msk',
                                urlForCounter:
                                    '//market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZmdnUMRPVYlTq24a_bxomRg9pUVagzzaXpIg-_nS0L4eLkhlNcmPiElpD-uvEythjuWiCmG2U4hwKb3MbKc0RNRFNa8bHesUFK7Upcqkgxe7lxmVfvTIF05OtD0esyTh198NmcY2Xktr8fr9DUnRxdtviv1dp-emL-GdN1Nu6NIhH1rkH7PHTBQQpQFtFk7iuuyQfC80N2HJZ6EXKHlJevjzg1AQcp3U-8w9ySBrlUWnJQj4jiDXVZMuIfLG3gLNBNxJ5xhRz8CQXMiCloY58aumJVEUILnYq3wzrj2teTkHIe35E27x6q999vbXxk3l6EMK19z8q2IyLtLgTcTgpe8zx3FSx1mj71omgQtSpre4efHMuWo65JJupY5biaAQ2nKgHXBXmWyBc9NdS6vILqZfFIC8iCtfO1MQwf5k0miHUSTfm_ZEt9Nf3mhcH-bnWZaijQ-MG8CSEkiQw9AgmyT_azsBZRUa9nAIWTgTRcp9sngIL2Z60ca2-mIYxKt18nXdmTuoAMAxc4IV1sLpmVQBgiClG6kYvVie_bweJpEJaU5p1uwhg2E66BKRtYKKgOe-q7i9o9Aq8EFneTFWFHxqxEgv2mDmJxDEWg4wynzWvPovEap1NvqqC88GZR-YkmrV8NeQ6uBFozpMMdWXAHW3SJa95rHRrnc_PIRXXCLxEle1XpJgHLP1HXrVCqVoDO_vbHNk4_Y0sPiZ33DGBV2TH0CYDb-s2FTuw8EZx0Q0EQRhFiP-0Hx7qJ1VlS03cCh1rN8G9F_U6y7wC66yWmyYc2MnCbmVmozXb606wbRlpq0kaHiMBY7mFGeVZGTm1Q0DIj1OGdPMKg8Kvh-W9zgJ4ZL11_QxRBBlKLqUoSxsacl8MdRY6PfJMS80wmamV9p4vHj7V6rI1a9EeT-cC9TkktFZDKbV3asbWMVvJhQg?data=QVyKqSPyGQwwaFPWqjjgNvVzaQAfgtdSlx5fc8MZ-M7ZvQLQO3fpC3tgZkOQikVXWH8PW9T2zZgamXTDbSSZzZVcpOV3-WEqKL4fkBLXpqw17j6qsuEBObcZRjEKM0M1nx59BS0hUdgJMkNXc6AbvXltd42K9tK50sE_LJvPbeAcHK9MlzIlYFLBCSfYQK4-6yDUE-V7eZkNhtjeBzBurpwiKd50G9hlgpjHykbmYMy2KzQb6WEwgP5ShivoKnMHaySTPsV0_G86Hbwpo6Y21R8PnKb9HD7VbjzbGvXmSsY4e0l2kNcDxtn2ZxeqHSVgE_drPAq6mqH_ejLrWthJoesj14XdUQPH4xAxalvDk4E0s-PvRNHuXHr2fYvcV42hT77JI3U3XhS2ZrwVKOYO4ryXGR_ZR-hBDO8OaqUoxpZcjkrn94aRBDCm3-J4IXSPgmdUqNqBACupdaOLbSRGESXeveHM6mIcwrGpsTX-4XbUD8aG7QJnxfiEOCynpH7Vn4jOKKAvrSonRxGKbMSfJnMC6xmlYJuYYwMe5GkOVTYVvIknR_u1fjZE-hbRMulZ6xNQBDjJ2e1ydTRRFLYO8khGatMuOYOJUhPK2832M1SFg0o8mGheM7KHuWEAwyaFQCbKVcnAVOqzsz8kuzYSXZ5hgHFDTSQcvilBCuIc8JTT9ZZkwxlHmLoMr4hRuI1gtqFjQuhsHz1rQEblKF3VFZoc62amyzCsiwaanJbeyJ7aIPscR9oeWzmS4Ggg1IumG7LRM6qYeHp-yviXDWbwjBO4GefXK9xNOtzWNuUzEpMTD4sGhSyE-63o5X8c_gbaO0wmS87-vmiri8hgLmFhIRP4HBzWnmJBQVZnEZarcEXFc2WD4WFnEGJU9l10qbVJPfD3ExQTSEw,&b64e=1&sign=72d6d260bc17afe96c68756af39ee493&keyno=1',
                                urlTouch:
                                    'http://www.top-shop.ru/product/576501?csu=general&cex=1559807&med=om&src=ru_gen_general_om_sa_yandex-market_v1&referrer=yandex-market_om_1559807&utm_source=yandex-market_ext&utm_medium=Off-Site_Merchandising&utm_term=576501&utm_content=20002&utm_campaign=msk'
                            }
                        },
                        {
                            greenUrl: {
                                text: 'OZON.ru',
                                url: '//market.yandex.ru/shop/155/reviews?cmid=RvbxPrAB3fXUFYBJlMMWpg&clid=545',
                                urlTouch:
                                    '//m.market.yandex.ru/grades-shop.xml?shop_id=155&cmid=RvbxPrAB3fXUFYBJlMMWpg&clid=708'
                            },
                            price: {
                                currency: 'RUR',
                                priceMax: '243',
                                priceMaxCent: '0',
                                type: 'average'
                            },
                            thumb: stubs.imageStub(75, 100),
                            title: {
                                text:
                                    '\u0007[Шампунь\u0007] \u0007[для\u0007] \u0007[бесконтактной\u0007] \u0007[мойки\u0007]',
                                url:
                                    'https://www.ozon.ru/context/detail/id/135948063/?utm_content=135948063&utm_source=cpc_yandex_market&utm_medium=cpc&utm_campaign=msk_div_home&utm_term=135948063',
                                urlForCounter:
                                    '//market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZsAn40UVHe-MCmeuQIjl5c98BiMazohc6A4bemF_GTFEeN13Aa3-hHoqKbOqpxsVwFDjWdD5_9ejwZVlUdMgWr5BTHHKLrPUA2TwMEEsUTEpElpfQ3uwdpb7c995eHT9ruf4i8LOOT445ENdoz5BM4Xfx8DFxZvO6x7mz8B-0kfGjM8-RgYnLbn8UihYD5FgEvVku6kcOSoWgOv-uUSJyEfYILsr5Ng7D26yodchlwqQe2OvZI75WI-Gg342SRj-4K-grcEleN_FpM8DfxcZ5N_4DwtBDjpQjhn75a7JNGaM3TGFYsK-kj8ecD_FBt0rPOwGldA9EfaAe-LZE2d-Ibtv7j-G7N-eU2QEamMSsSBaluIRuNoy-sM2w6P1cMAqZ4i3oTKWF4CipTtumvb9aXMvnCXOcX0KLqeaOdKvaEaV3GIPlf39uUKcs91-B-ld2Woyl0kGeeAluUYDND6-dmKbhTyOwtBZmEHLZ_TbrubdquJG3zSAskNPo_dgDebRbKtwH4AKExZbUAmLXfQihEjlBtrcFRYcAVVmtTsUhHVkKxCyLol53gTSP-pHrfoxqwiaK3rLMTcXQj1Wh359-iNrLmDVmMK1Lfe5FFLZXv5Appr6gksZxNqLdk1QUPSYF1Ah-I2Axze_qsRLKKHp3vdZWqabHDDtx_RHjHxiv_1U7bi_oA-trgQEJLmvqqOjHvYGJVmvuIdy8sq2S-Gvi2rQIPJ-_4nKMuGaZULWa3CcJtZME029DQue2cgNMgNnyBAzOksoO6ckccWHqoebk3Q51Bj-TkgqCAG_PMHJsoESdWX2mkRf_aUTHTlMteBKs8jqKD-nIWclSAcgJLN7SSkFttHdo6J2vQ6c__0VSWpw4ev9aRlpvxPDpfWbyrJeTidc4OmOtQxKqb2eaZW_rUORdFYbHMkXlduP5sT3n2qf?data=QVyKqSPyGQwNvdoowNEPjUO6Lp2FicSHwuLrkBRU9MB2L71swKP9DwSZnoHPlJhRZwJIdUCMFHTaNhKmHElg7BrkM0dLm4lMNr7x2LbalF1BeDBzqTS0_1dB57bYcPue8FXvuP-kx51H8eEyWOEye_YJ0kcuHUWaeoz5OSCIr2LYdS7gbsoDy1cXSOP2qfWufZcy3ZfqUfSelGoG_JCkWAOHrdpj0Odq9bFcgr-UQ9j7_td5tChE754-4m7VUF5Gdo-dJjdxqLHDs3uW-rqmCwByeplF9NdQi0ZCG0mk051Z6miN2KCOCmhtrlay2S1AX280WR2uek4E_BfOx107Kf-ohQFUzOm-KTurhmGtSHkD4pEUWQUtPfGWc95KSGGakgz_1NcID5JwvvRqUmJe4FstG9tQedvuY9KxLZI0fWlAOVLbjvAwzqn4XOywtAe5&b64e=1&sign=ae10b93094581ec1aa6a95d292e9a258&keyno=1',
                                urlTouch:
                                    'https://www.ozon.ru/context/detail/id/135948063/?utm_content=135948063&utm_source=cpc_yandex_market&utm_medium=cpc&utm_campaign=msk_div_home&utm_term=135948063'
                            }
                        },
                        {
                            greenUrl: {
                                text: 'AGA-АВТОМАГ',
                                url: '//market.yandex.ru/shop/443/reviews?cmid=4_-FWjWNk7JFFsM0OeaVsQ&clid=545',
                                urlTouch:
                                    '//m.market.yandex.ru/grades-shop.xml?shop_id=443&cmid=4_-FWjWNk7JFFsM0OeaVsQ&clid=708'
                            },
                            price: {
                                currency: 'RUR',
                                priceMax: '699',
                                priceMaxCent: '0',
                                type: 'average'
                            },
                            thumb: stubs.imageStub(75, 100),
                            title: {
                                text:
                                    '\u0007[Шампунь\u0007] \u0007[для\u0007] \u0007[бесконтактной\u0007] \u0007[мойки\u0007]',
                                url: 'https://www.amag.ru/catalog/259581.html?partner=3',
                                urlForCounter:
                                    '//market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZkqM7SmAeSs8s36E6dKs9t1xQDpBnWlBcYHX2pE1pzYgTSdjaPhRxDsngwNGkfZZ6_wCUOMSli0CZIBHX0SrDhyiX64oGC-1T11k1HHAhXgOitkvN8eWf_2VC_S1JrWFQteu1qEV3L6_OMmsd7UxlMN29205Bx6d_kZwyYsdvFd60rhpDAcmiTIV3LyXNmNPmajMOxU9mIfNX_L78hGN3j_gYxNMKOf21vCaK8n6b37195JS0b8mH899kQC1VQ297aLjONC6PbpS4C6UbkPCdasMzxjnmOa4sGzSz1ibU_zX_BRSuxx3gjXgmMzpHteXM2aZJGzyc9eoYihyHKrzYrKY1HDBryFTcVczgafo3CV4n5qQDv0UyGDDk5YOI8xo7tfihjvNECPSN8hJdXnx2Q90STRw-IG5DGKcMEiFiHK8Mvw7Zaju2tdHWKcP8ZCJH2rFkJMcjRc63kAIwGU4wyzYzfy0rBaq2Nnzgcpa8-0vYmX9L3Pnb81NrBNCtCHfEtRiejWJ37XyK6p7bqaxePabgknO2O3HPZ5jwYpjbeKr9DDsffQwyywnf49d--GtjYxbLD-SsuvJqBUJpIpvV0qqHFCo0TEyj6jAIHPbnTJAgRna5Ds5OzaRwkkHH-o9RK9Q_yiDHGrB4kIav1g8HNKUVkTY2yPliEOLhAPgENMuFiP8GzVKH26V69CHHBL6msmNwL7XIVAwxq74miHDRE9NLeAR2yRC6Mi40UKpB4WIHplspl77Lm9_H98tB8yh7UlCIRjX1Lha3OOabAdkpOiu3pWBTLn7L0fR_7YHz6m2sHg4WoeYnDwj0P6KcjYUArtCOOM-IE2b_mPaalC4JpaCKsFU0WgKgnVtrgt1L7Y_mBf0c3W5uW3184cWL48OGNl4N3l5jSMAtKJ5Zz6UERqNEegy85VG5k5UpUqi-6iO?data=QVyKqSPyGQwNvdoowNEPjfLb_Gzgfx24rFuvKr-So0kqh1LuLjnRh2f015IyqS8ii1sizPesjGK4Pnd6_Jn3vyPzccxjv5PlJH6w0MpTpSuGWGKjzeGe0UrUAAK-3TEuDVXW-11t0uggnSjj0BH5KxK1s9_22-bA_lwc2fVMi4bubGRDVJ2ok7o2QFFFCsP8QteveTg8l-oBg-j0c9zQQO1tf88r8-FTL8hCmBse9dHlX4RA3wzBR041qkR8KY5AYNPOYnbo6bY,&b64e=1&sign=f0be2258d5b409c498e251721b43bf05&keyno=1',
                                urlTouch: 'https://www.amag.ru/catalog/259581.html?partner=3'
                            }
                        }
                    ],
                    top_models: [],
                    top_vendors: [],
                    widgets: []
                },
                subtype: 'market_offers_wizard',
                text: ['33 магазина. Выбор по параметрам. Доставка из магазинов Москвы и других регионов.'],
                title: '\u0007[Шампунь\u0007] \u0007[для\u0007] \u0007[бесконтактной\u0007] \u0007[мойки\u0007]',
                type: 'market_constr',
                url:
                    '//market.yandex.ru/search.xml?text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8&clid=545',
                urlTouch:
                    '//m.market.yandex.ru/search?text=%D1%88%D0%B0%D0%BC%D0%BF%D1%83%D0%BD%D1%8C%20%D0%B4%D0%BB%D1%8F%20%D0%B1%D0%B5%D1%81%D0%BA%D0%BE%D0%BD%D1%82%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B9%20%D0%BC%D0%BE%D0%B9%D0%BA%D0%B8&clid=708'
            }
        ],
        size: 0
    }
};
