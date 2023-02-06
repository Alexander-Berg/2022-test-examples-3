/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v2\.1\.0\/models\/1732210983\/offers/;

const query = {};

const result = {
    comment: 'models/1732210983',
    status: 200,
    body: {
        status: 'OK',
        context: {
            region: {
                id: 213,
                name: 'Москва',
                type: 'CITY',
                childCount: 14,
                country: {
                    id: 225,
                    name: 'Россия',
                    type: 'COUNTRY',
                    childCount: 11
                }
            },
            currency: {
                id: 'RUR',
                name: 'руб.'
            },
            page: {
                number: 1,
                count: 30,
                total: 9
            },
            processingOptions: {
                adult: false
            },
            id: '1516630065986/2926ffd14b3c95b581727e1d10b1ee1f',
            time: '2018-01-22T17: 07: 46.444+03: 00',
            marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4'
        },
        offers: [
            {
                id: 'yDpJekrrgZEIsTnxRU25zIgUIzFhcwM-3kpiloEsV6RnMEaao4omqiw4iZ1wFacBwAcYu0biHumVBTOVZIMw-0h7C_W34R3nX5IduqQzDuPXbm8z5XFBCdoHHdC4q6tjHIqQpOEE4evXvjyR8A2QJkh8mhLlwIf29kgp-z9-goXMPZKVDuSO-3NkH5TPJaJ9ufxjx3bV0aKiveA5bK8dmcOEayPfsMhNkvq9CHTc18qmmFN9Ddn5k3-G7RJ9E7tDvLnPZp5qp1-VX2i2kAQyowXHO-QLQdv7_X6GwUUG7BFd88wbl3MR1lPhqfl2wjfs',
                wareMd5: 'IizhzKDVQQs9h30GuZlCBA',
                name: 'Смартфон Apple iPhone X 256GB Space Gray (Серый Космос)',
                description: 'Всю переднюю поверхность iPhone X занимает дисплей Super Retina HD с диагональю 5,8 дюйма и поддержкой технологий HDR и True Tone. Передняя и задняя панели выполнены из самого прочного стекла, когда-либо созданного для iPhone, а рамка — из хирургической нержавеющей стали. iPhone X заряжается без проводов. Смартфон защищён от воды и пыли, оснащён двойной камерой 12 Мп с двойной оптической стабилизацией изображения для отличных снимков даже при слабом освещении.',
                price: {
                    value: '89290'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQrHhhAd4kGioKCYyDzwA4dZQCcoOFMwjqHqjjD9YJWjTIl64xpHtlJ1_gcjZ1-z0CtCGRUlR-Y1dCOF_W9OhuuDDiEC8Do0J3vj90kGgppWVFPlh1PydYWpr6npoeQVBZ68QcEBb67nWBRT8H-Junupp4X2P-JYrsu1DS_zU3A4_f3dg_T7T4XQZYytCGYsK5VZoi3BYXt40azzxoL8BYl5YeknLmmDIwZb8BVY1HmlkoWYciKxeos3fQ67pHNnIgMrm6ZLxu_lebTRdQX-t-8uclBHUBTMRgPoE3D5_XkaT3B8uF6gLPro7wniJnbo_1kp4d_Hv53movWVS3tNRfhzvxMx4vJDthWG7UfH4LbftAqEDMpa7S_vw3qg11OFLqSTgbp7XnwP3n-xb5z1-yOPfgNt2mXyojkko8TjiiDG8Kb_8zF3IBdRL97HLKaCrWiLJSYKjEi0iONiYjm6-2H2U9g01gjDCpVelhVcqbKt1f_wth92ZAnMEyiYKZ1CSLJ50aGPOmpOARyyq5bD3QjKuWqtImnNeaupVAQhPNcCWZly5nC0nm9Bu2nv1v2npW-tRlyxUXPJLIbklqrnEeW7x2-2c6xiGEFYeFNuneuIJ7-XwsRYL1-akOl4_DDu5Dw3fgxtL1ipxXplD8kE52Y7OGjfCB2ojhzJLoJZsN8BEWMjs01gmm7vhwswMi_uoSIIb2OZ_GcQa5d3aAurbA1QH96DNJFLxbeLKcwfX9L-kju_O-Zk-Eb_iaDEbwLQwJt7yl2VuvRnqxitjH55VFcpQxE6FuIqpIFLvvtM6JiSDG0d3DuxCuJA7-SL-q47W4qasDQx6AXFa7GLdXy8nHLLRFe45oaWDz-ntOTnV9LU?data=QVyKqSPyGQwwaFPWqjjgNrIMDGWpvFEh7dNP3s0Nt-FDxTtOP58BPC_MqCwRFimL_Ybq6Wy_E2fkz5oJQbnfd-Y3xQKZiL-vWq5SJeH33zhNXvXNymhnzXdx2qg6SEM2-UrA2DzMZ4BSHgTXRLrb4g1mU7iMOeY9ZjqKfM6ALTS9w67qez-K5dLn-COo15Kuwy0mzOT8fD52G9JIgZxkMwIT-VXQ6ACiYRjQmuF9KbuUHylQeLoBTrT1NlKWWlbF1_Ir5qZj0KC7uL5HfsBydy2AcZzHGwUdDGfGmLK2nzstRoPRCK95lrwT8Cpn35UtL8mVyRX2KLKuGp4RmItCAWrznQa3cyApt7jP8B_3yQ6q6pPtWjDO6kl3EtQO1cKXCVL73QmhwIxXIfv6VYG6VBlbLMXXLjcyraqYQv_MRco9ZsCE-5cPcRxhSYxBpJ-Q5iRUjHuMAhz2gjou5kTO7dep34qpB77IGu2vburOYPVbw76wViFm40Vs8tVQv2RkNn5wvsG6W2onbQKbWSNbDRj-_AvbF_rGL8NLm5T7MY4ZQTkxxyI0uoCqIKuiC6OxHCxFxjEhfDU2YdGjo9BQTi1IBO4Z_b7KQZ2XZ81-EBLoOq33hOIqL0PB_QE52h_cLPDk4fQoFsazGHbq0yP3YxQOkwN4eC6jl-RJbg0MTkv0207RO0ArzUN0BAn8ns7eulSWzTt_i-3FUMxa_wGhiw,,&b64e=1&sign=e574c5497da02c2b1b994a5c804575fc&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVOvUx33PfuawqzhAm7BwsFX8Rx4pxWX4pMYBg0M158hY9wY0kcZaxXarLNvDiwHTcDyibec-LYEYjMF_sM2Kdy1hERkLUlI2ZwBigcWzzyr1rbXe3wyiluBaeQf3pZst2H3ZaEzkMxHZu-bf6ZL-QhT8bw5KHeKplNJ65zEKRQWrtkPJ0a6jA_25EGxzh_emj3uh3-cignBkB7ZUCwh0BmvoXeaSEwr3C39BgwLJJHzw6qMA53SmkIF8SntNNNVh-D-1aGzOopUO9t8n-9aPStpWsPvoLfzvZLZq3xQ3BMn0IaxIXcjmb2R8bGl0VQ29rrXNYTSiKQyLoPCfQ2JXsBR-xPDXfdJdhXctDcTprjmzCtn7udHXktjnIDWzI7o2_fFbax8UqrYykj_1Gr70-0aLGSRYrj9f2VnbNFsMvLL326yDzqDitiTGtBdzLH2kCH0qPDnRwHiDYLzeXT540CGh8qS8WUPsrW3-Z_nvc4m8Bj9hSapkE6_7Ot0MPIM-Hlwybv-ygpbQ60c89xWw_16DMnffGPMDi5St2K6p_v_QVBC0KSLRLd5t9sHKYZ7BHrH5iP1QwAC4HTC_-iIYuhMslocWiFabQul2Azknusu6935PGYiMVWlcBgC4OG1NGLGX5KV3zoKIL46O4hv8xyacFkpqzCGNJtTKWhdZBCN6oTB_oCsUDrfe-RWGbTJPskOLx3JlgqH4ApfMUqtPD9K0pQdXry1Gczs_VDkGKJjJ7l3D6a-3ueue4J9u7zwFPObNyWpaCN7kPsaIBq6faijsvKanM3VxVEsjh50CtRkQ-UWLXNwg4P4M1kK_fgP3ZbcAedqeAvrSo9tMC61AVJYaEHVcJTw1DDA_cHNj3BU3?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cmOq2b-AHQGJr9nFniWZaKSSvN-A50VuTEZpE9POjor8HqbkGTEO_t24CCbwljaulmDr8jqpJc3gVuNDuSyTu67WXP46f7kHAMu9uCvUZ4EYYDQ0D7_ooo,&b64e=1&sign=4c0f99cf330ac92829503a9e922dcda6&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 2,
                        count: 9378,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 2535,
                                percent: 27
                            },
                            {
                                value: 2,
                                count: 547,
                                percent: 6
                            },
                            {
                                value: 3,
                                count: 547,
                                percent: 6
                            },
                            {
                                value: 4,
                                count: 956,
                                percent: 10
                            },
                            {
                                value: 5,
                                count: 4800,
                                percent: 51
                            }
                        ]
                    },
                    id: 42315,
                    name: 'МТС',
                    domain: 'Москва - Московская область',
                    registered: '2010-07-09',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Малая Дмитровка, дом 5, 127006',
                    opinionUrl: 'https://market.yandex.ru/shop/42315/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '8(800) 250 05 05',
                    sanitized: '88002500505',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQrHhhAd4kGioKCYyDzwA4dZQCcoOFMwjqHqjjD9YJWjTIl64xpHtlJ1_gcjZ1-z0CtCGRUlR-Y1dCOF_W9OhuuDDiEC8Do0J3vj90kGgppWVFPlh1PydYWpr6npoeQVBZ68QcEBb67nWBRT8H-Junupp4X2P-JYrsu1DS_zU3A4_f3dg_T7T4XQZYytCGYsK4avmQWXfoIOp-Q2ELw_chW6Ahe7gWFkjWZH3CXEVXTVUwBkJnYbiDgINEic5Q7e1zoQeNGyOCEgucg5VxpTdUmmTJJjS5ceXputLcPmMynclLf5qPXFVH1dVoWPLIdOMd5C6_1LNrPCs2bulA654BlPjtvHHI_b6T3FpidIrHKD8-8y4oOlnglM5TmB4z1mCx4g3ZtrBc84IR_6ojS7PnJogo_fBEkF42bAsKSV01h3DIt85njaNYbIx70nH1VY5jShzyzUljkBV_nEtu608nG32NXoO71795QAedwjjlvMPhWR0-mdjfiFAas33y9MrARRSHum_5Ov7vwfv_Kz8jg8OKQv0yTXbcZcivgyp_rJ4T2Gxg6ksR4VK5B330dEyV4yE2QIMpkWFcEwiMRri7NVemAa_avJW4AuQLTl9c-6-vxt7SRbYrWjDAP81031PYK25lFjEop1-G80jaKIG57NYitOltVaBoCZrXl3qjlwA4fheRYkjnhYcu-dI9aPm_tUDH1ni7Yegcmq1j7RYPJxnW4BQJq_5Lg5UGMUbWtbPhdK--oKhk75xKsBBI9QmzDUQeO16FSya-oh3vfSxkCa3lMX2TNROQB5B-dwoABvAfYxhEMvLwc5pS-3dRZQB62b6FK-DuZRiOQ2S5PgNjIqVKViGK_6SJnxb_FCgLQ6?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-PSWjgvpbZNqzaTmPECsKk-mVHxO5Wvl-f78nZtrhCmoGsdpuD9xii9TpoFaQGjVG1AAgrKqvh-zFRcJldHd6MQhdfr_RuwabteEyLgy13odDusnjt6JqwSFI1qAWwYkxzOrx_u495JkE0ukiGLR4QvXrV6hZojvUtgm8uplUBvA,,&b64e=1&sign=dc2c6f6b5b6678ebb97a1d817761f9a2&keyno=1'
                },
                photo: {
                    width: 347,
                    height: 691,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_gBF2Sp7f2HUJS97fPqbATQ/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 2 пункта магазина',
                            outletCount: 2
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/IizhzKDVQQs9h30GuZlCBA?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=pVbk0QK8D1D-mbd6pOMLNO-KvQDD9143acOpWqre-ZAToK-XmFeRGiqVx-EuLCkQ3RLXBR9xnKQjvlJ0oPMDbRL2Pcx9KqklzQKT0iE4phwOraeknAfiJRhehJCs3rnQ&lr=213',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=42315',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 347,
                        height: 691,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_gBF2Sp7f2HUJS97fPqbATQ/orig'
                    },
                    {
                        width: 347,
                        height: 690,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/374599/market_iFba-2TXRmgTwMURUSnYQw/orig'
                    },
                    {
                        width: 47,
                        height: 689,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203634/market_h-xmrybFzEtCHeIdoYKyOw/orig'
                    },
                    {
                        width: 437,
                        height: 856,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_e9BnPMXMjrmjwpV_ZxEpzA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 125,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_gBF2Sp7f2HUJS97fPqbATQ/190x250'
                    },
                    {
                        width: 125,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/374599/market_iFba-2TXRmgTwMURUSnYQw/190x250'
                    },
                    {
                        width: 17,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203634/market_h-xmrybFzEtCHeIdoYKyOw/190x250'
                    },
                    {
                        width: 127,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_e9BnPMXMjrmjwpV_ZxEpzA/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHyR2wK6Be0xyMaaINpuXbPz0qehflYoNegBZzyhUisjIsscbwewp80_3kc0jz80rqlLoBNaWc3z6Fm_3gjiyvEg-SdNM9QJEdJk15XGG4IA8hzW88dKzQqjKOIx7XPsKJRl5NFdpLaI2V3K2b4ecTT_VfRnJeJxKyHxoy4ZN8Oz6CRsMRlTaLqoVcXozjBVqvQo3Arq8gPHYppLPRGPsUKvN47fYywbwqlV_6nWr2KtDs8D66Q-AF4_UiC92RSGXG12cg-gB36sjdlZC-p0-NeSZwK4tQ4948',
                wareMd5: '5JGgV_T3ons2PgBHxOAjCw',
                name: 'Смартфон Apple iPhone X 256GB Серебристый',
                description: 'Всю переднюю поверхность iPhone X занимает дисплей Super Retina HD с диагональю 5,8 дюйма и поддержкой технологий HDR и True Tone. Передняя и задняя панели выполнены из самого прочного стекла, когда-либо созданного для iPhone, а рамка — из хирургической нержавеющей стали. iPhone X заряжается без проводов. Смартфон защищён от воды и пыли, оснащён двойной камерой 12 Мп с двойной оптической стабилизацией изображения для отличных снимков даже при слабом освещении.',
                price: {
                    value: '89290'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mbQ0I7P-jnj3QPm2sABQW74AEHyMAPZTlTFpH8d52bG3aMTbddwojbDGIjZcSmdtoTHSeBqxUSdbcq70ndzdRptHq_OkCetZUK-JolPR7mpVLVdvIWv8z9xuzSPlNCWM7eGjQAjC3Y41oqPQg6a-LMejmchvmPTU8kDQcxP_MLV-JRooyxy51tUt1CXXbOQRJ6ZhFZAICEe0gAIU-RaQTlFA1El5-UZg2hFU0xYryagZ7IgmB_MYkFH7kjhG3WhNk4BSDGfqnvKc6ucpq_9GB2J9oqFrczqGcidP6rsqbYZVjNU6FqwAV5osEnU47S7IJYx8clksv_OpU2_u4pv5tcyPi3w2ZqZiAMfZnJ0I30xWQgjY_nZBNnAeGTqFppYYA9grTj6bWTw4C2gsCYn7_g48W-Lj0aXjQ-E7zhmIJu83Fz6jwWuIc9Fq-IWRdpTA0P2Njp8vE_eNNStr3qjuxC6idQCQRtNW5n4cO9kHOQ286U_18K0Be1WEv835sfTkRBLOMEBQ3t1pecNjtJPxx7RzloA4Fsw90WwQDrFZHLrfZ9mxyU2K8IrCDhsLGZIY8Uu2luijEhSqizvSmtgxNYXFCp0AIClT7a5nQORGQxmBeu6urhLfiWeDicR2DTi4_9RKxrIvzo667r41eHpFqEF9OMVIkUdSTcoxowPC1IrE0FtOJDDP34g2RMzEEL_HpYrp6KEfCNt0-0SFVSmwHcbyq3lsIr3x9lwo0gCxHaAZloOmGkinBWBKwk8O6XfMHPm08KlIPIeT8u3i6RcxIaTsGU3yKLbtnosdTeIG0DndoW781CKUFj-Wo0poU6gFNg2eHabYcwVQfjEVi7reekPzKkBG0GtzpVOrxXVY6SyM?data=QVyKqSPyGQwwaFPWqjjgNrIMDGWpvFEh7dNP3s0Nt-FDxTtOP58BPC_MqCwRFimL_Ybq6Wy_E2fkz5oJQbnfd-nJvIHHuc3sV3RUaIbTuRHjJy2Dgk3p8Pv01CLWgaID19xfTX-LTGY8tfFZSGgnSEs7DRlbbTw30pIxRsLD78CyhYpC5YuyZn19syqQvY2Qf1oL0KgdwBeu_K78t3dRvZ45Ajlle7a5C2EKop_MHDF8BxFZDMIJbFzK7q3DZGaxkuYwYPESIqJglsmAroBQcOznWP_s20l0GaNmpC6wBYxJk9kkXkFmvadKBt8Vnx_C17LzFNI0ZYTqJ2q9WVWdPjhHGCWDksfIPOaV9g2r2vwEZ2p_JYdbHxMtpRKPI5Bd3cnJiUYQtTPlDhkdP5vbkuQU3OiSPA_5GRgDDG1HXkkPqV8LVYkyVnXjrZ4XMBu5OwqUinGghdi6wDrDdgSu3vbKrAcY5citCOgBRO5kFpqf9PnKvJ0Aig5ytn9K285CLIXnHdqoOv-KVNlDO-YwkhCIYH41oaL1KLcBvtNhi8pMbHB_cDo3mJ4E9i7eW1LIqVsTHDCKuLWffb-BymGL-Z6FwSE6eVXA0cMD9U9mWN404uoTgEMY-v6Mi424DUdevQMeewzDTtLdnnx_THtnsYotqtkJCJdCur-DrbKEIYA07_ph3e-QdsIKvxVmQQKM&b64e=1&sign=beaaa6aeb8b16a769f3535862d049f6a&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVFKoe-_MBTRXNJTH59ZQduag9LyBv_yhiZ0-ct5RFnpIkm-kRhR1UONnon5ZzqwzvjilyTuDITkmEHA6zJWPmQ8CVEoh-GaQAlkTA1PvUo-wd0mFR_vcdoZFBgYGDR9NYHqwZTfF47qbXrLMO4iiL7yWNJ-xCayp7DXq1T4_u0SlctTtlfN4q3pv_U3cGc-kRsBEQo1xeHreqeUGdHgucai5jYbGbh1bk3uduLWZNGjpH2rYWlNH9yitkB8mwZfxHuhqvGsvcnYMpZ0jJOE_2TPvZ6865gVlG0BAHF9V_bLficCDB8xSdT4T382s8q8gCnCUkGiXwNooRljHONf60glJQOx_C3bvQdgW8OOHKUOzTPpaeaTW56RYlS2kchYEHIFkqvmsCZxKRKlwv-fIID5vPPLUf6EhtccSgAzmXijBJn8kUM2ahFGPFVWLR8a7NdpQFJgcdFrChWzCztVFwQMi_K_K7kNZu9DKSGyum_3cQPQMldRk3uO4UighCpqXRR6gg5xNco7L2c74YFOtKvXEi9U0Yv6xfZl2ERHX4gICDWgkxKrJDFzc3SwFazTC_9mSAtDSjY6T-j3NMSD6Ff6IlVpdEGyAwfxJO3EaGBR1ADW0Zj84UvSFF-UlZFkQQJTNzO6LciEiAKKkw4rxXNCvXTPdFnZiw3nJm4wA3ysH5IzS0umbEUS06i2_OUkqVlr8ss1VNG-VUAscW84BgE-ujXdAyepDytPKqfc-Qwi-eYYBsLueevxcypwDzpSD9Tj_l1PUEuRY-L1jEXL3Pg-iNWuo8Wkt_ZuAJpG6_QcuIRnQ1tNw8YWhOKkPlwOPl14FLhNUdYiwzoSrf1WaaHXaZ1mtVW3i-przRrpPocle?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cmOq2b-AHQGJr9nFniWZaKSSvN-A50VuTEZpE9POjor8HqbkGTEO_vE2SK4R9EIHu6dYen4-nBVjUnBFNSGDkdQDTbewcI1_-GB-A0VEvc_utz6-Ovx72E,&b64e=1&sign=7bf2560042ff9584fe37ea78386e1b0c&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 2,
                        count: 9378,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 2535,
                                percent: 27
                            },
                            {
                                value: 2,
                                count: 547,
                                percent: 6
                            },
                            {
                                value: 3,
                                count: 547,
                                percent: 6
                            },
                            {
                                value: 4,
                                count: 956,
                                percent: 10
                            },
                            {
                                value: 5,
                                count: 4800,
                                percent: 51
                            }
                        ]
                    },
                    id: 42315,
                    name: 'МТС',
                    domain: 'Москва - Московская область',
                    registered: '2010-07-09',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Малая Дмитровка, дом 5, 127006',
                    opinionUrl: 'https://market.yandex.ru/shop/42315/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '8(800) 250 05 05',
                    sanitized: '88002500505',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mbQ0I7P-jnj3QPm2sABQW74AEHyMAPZTlTFpH8d52bG3aMTbddwojbDGIjZcSmdtoTHSeBqxUSdbcq70ndzdRptHq_OkCetZUK-JolPR7mpVLVdvIWv8z9xuzSPlNCWM7eGjQAjC3Y41oqPQg6a-LMejmchvmPTU8kDQcxP_MLV-JRooyxy51tUt1CXXbOQRJ8MxwEONtTZagmjby1N0bhRyhcwimdGxonY1OHbuJTUFlL0-UXEofsT3JWjqfgq9RsMR9JP0rzy3O6iDL5b1-ZIY3t8j217jBkf-alUG3whwHMm5VUGWbVFl9NAfSBH6gWPJNCnjHlJR4wXMuutVpYnswEEsNtx7_mUZz9ubdIIQz9f2l2iIxcPBsvSvb77CwWEryINYjyExg0mel0HKyAPpeH0G-W6857PMSdU0JAZbtViCLoR8N_b5LlDFxxFcUz-gbmUNELLYn200Mbi6p1miceehwLVy6-AUlNkDeQLIWe6cnDo0RjsZGQUUyDMlecY_qyI_KGvFOol7mBr8lcfauA7DWyXbryHmYcdRK-scOHcjv2iJ-3pzGkfPYjkhbVTaJ1x5K5uV9rzdKQNzdH0rdPLYvEP0VnmHY7tVQ-_Bx45UFT2TWyHWkcA1PdmueY_qTdzLKV-uKQg5grHr-5dKqn4EUASfUiDOvjQfS8F78ip4_0Vdz7VJs7wPy1gNVBA5_c_6M0Dq1ZwJre21D2uirDLYHWSfuU08ShtfgoPa2J_-JZPHhDhHzkrU04mJNu4oYbjLIhUx9Ds8qR6Zw2N8LeluqdDT8x5uzG5GHEYqpDStkvK09k9KOFV1xsT_45zbWdRPcYmfX4mAfaUrJZcmI_bI6NDxrjvZuPSHEI3P?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9Dxr_VaESTKI0vXfrgyz5eLC216pLrE2AF0Qk3VR3DQg2pcV9R4XB_YE8QV16X4lrAJdDehbbMu9HNQ78qR5tn_ofrMyOhjhqKmPEZxmxo-KDDR7sgSJxZrYFmv3kuPLKnH1d5wEgKoDrpYgsmw0zoy6OLTQdmbwA9zXH2E1SjsA,,&b64e=1&sign=40211eaaf7451c460778070003e07c2f&keyno=1'
                },
                photo: {
                    width: 347,
                    height: 691,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/372231/market_1eH877qRs-ZI3lFT3prRCQ/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 2 пункта магазина',
                            outletCount: 2
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/5JGgV_T3ons2PgBHxOAjCw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=MfY7Uz3MnPoGZaEkL0t3_hCsm9eRvms-aPjRRv0Vm6Ojje6m2n0LZ55RdA5cVXEGWF-W9jZO_I-g5tBbnhAGj4I9v2evTH56JlTvPLt8XhlxtnQ7pMb8wOiKQdmyx0l7&lr=213',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=42315',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 347,
                        height: 691,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/372231/market_1eH877qRs-ZI3lFT3prRCQ/orig'
                    },
                    {
                        width: 431,
                        height: 858,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_L4caNdJbeHSfGf7FrYSuvA/orig'
                    },
                    {
                        width: 346,
                        height: 691,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230810/market_-Nt2R9PDMifApwTr5EFvWA/orig'
                    },
                    {
                        width: 47,
                        height: 688,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market_nD_nD5-G7Jm1YY8ifqo_oQ/orig'
                    },
                    {
                        width: 857,
                        height: 184,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/232366/market_ZswU0Ng1QTbv4VVrpPMU3w/orig'
                    },
                    {
                        width: 437,
                        height: 856,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_YxWpuuSmSkAdXhvyBlzVhA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 125,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/372231/market_1eH877qRs-ZI3lFT3prRCQ/190x250'
                    },
                    {
                        width: 125,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_L4caNdJbeHSfGf7FrYSuvA/190x250'
                    },
                    {
                        width: 125,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/230810/market_-Nt2R9PDMifApwTr5EFvWA/190x250'
                    },
                    {
                        width: 17,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market_nD_nD5-G7Jm1YY8ifqo_oQ/190x250'
                    },
                    {
                        width: 190,
                        height: 40,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/232366/market_ZswU0Ng1QTbv4VVrpPMU3w/190x250'
                    },
                    {
                        width: 127,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_YxWpuuSmSkAdXhvyBlzVhA/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZF7iu178jPMcRkcA8IZdTZdp9n6gHDH9VdiuDRUi44VeA',
                wareMd5: 'Mh3oV9XkEPGOxaMd475gQw',
                name: 'Apple iPhone X 256 GB (Silver) Серебристый',
                description: 'iPhone X:  Здравствуй, будущее Дисплей Super Retina:  Совершенно новый дисплей Super Retina с диагональю 5,8 дюйма, который удобно лежит в руке и потрясающе выглядит, — это и есть iPhone X. Инновационные технологии:  Дисплей и элегантно закруглённый по углам корпус интегрированы ещё лучше — Apple смогли добиться этого, используя несколько уникальных технологий.OLED для iPhone X:  Это первый OLED-дисплей, соответствующий высшим стандартам iPhone:  невероятно яркие цвета отображаются с поразительной точностью',
                price: {
                    value: '86000'
                },
                promocode: true,
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTpeGwlKp1_Tm1vyl-SYUyAt3g0RJjw3d02gYuPEIIGKA7SmppKw8ATYrLV8PhC6ACd0BzQnfX1SeQNSd_ae1QiFwAXUY1uxVImz3PY7xJTaXay3txI7VqYClOLUjGy02NjuH6H3y3uZEeIWgZFvncMrPzvvt4NczhCCyIElMyXej8mJvBnSnAY8Py1x1_YrXXW34RBM2K4sy-o4EDIE1eX8BFq206_HgNqYcP3S0YLsE1SZOwExl83mU6YPYoMIaq5YLZfKHnm7oxbJlMhswFo4piTbqDpwLBCDLH14-ATLQoCMnW70Zfs697KoOKgl3lzOUqVN3c7u3s8IlQJioAwqeyll9jnIgDAdDYbT6tmvGzCbiVqO7mxhZ395h_0xYIuEICmxrElei0GTM4eAnT-xMo5m0YmJwPGMDAClVsS_4GN0SRxXEcp0l-lg5NXSxwsrC-NL4BrjIezmfnGUmBt3fhel1kMsChCSW8o9Wd_vOvFqX5LOWTly4QTibk0fs_EJBUykKYj0Z0nKWmWtOdTCatDQaWaFUew3I0DPREuUpXmtEFdcEbBdAqxnzdX5yKnAy19k301tqjYbObslLfgLnFyiLz8NvnYPOIqRFaC5FtH1yfLDpWXO3YS8g7lSDTpRQumduInPHz1fw-SSdIj-BYeCsz-iHEKeB8QFrXiMCRXt4x0NQinY0vM9aEEUYZCKn0YNyyul2quXlUHKzaZ0SmZ7BxlkujVXe8xSJ8qjiKNN6DKkgtlPXi5GZU-WtEi8JDKnvSxyZC30GQZhZK61ydru6YLbLVKaUgAGGukx08yYv9PCpwGTiLKWFrnXcUknOG4S3EVmIgQ8LZD6ZkWKcu7ujBAGl0xcBU8TMLuk?data=QVyKqSPyGQwNvdoowNEPjY19xQRQQW5JrMEmExzXqz8o08AAKLpDrS47b7ogAkPaiwEzNCpuOOIDBW774FmaPViM2StdTMD-3J7nliavHW2SKZNbrYIf3dfYxgWqd-9Xa0ZwsOr5FARFSsCY9X2FOyWhDSXGcIYqxw0NU_U0Nj1fXfjfMtvNsWLRAAHM0RLqsGfoOQaw_Qf7CGQz-uOc4mjA5kNDAiCD0N0vzknNLpiHXnZDWr50sSA4TjdCBi9XWgeN63ZhR6nPubwg4q9SmPBKUL0NOdk-10cCKJ_Lqv9a4nNYOIjg4jxXCDdWpWxH1wVOrE_-YcOvCoJ1LBkghBO50nrnFxpAjvIEL7NtXRE45YPBwm5JzIEToasdD79svFwooJw_xakh7MPFM43qYBootGCmVwGrO9NhSYVgsD3GSuqbLK7TvoqdbRi6Z5-qwS3nfknJ_uw_o9TG6s2LELWqxBV4MucW9XRZshg1XioSoRKrg9VMv0xEjbTmVsXy87qGW6ukZEHyc9nLQeXS57STfdJMYmFV&b64e=1&sign=623cfeb838a7544972c42c53315a3f1f&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRufkc5G38e6HXSgg2c2gxGcYi4W-1mwsZLnKOa4EJU8ODY7SsGGz1Tc_h9PsFdq-uOROsJ8L1Zs97j0YZeuf10Zn1633pEwMGj5Oq-YqCxeTMqN1fgx1Wzo5xCCJWhahrREe3PWLb6IkqHYHLCxskNETCeoHxrRy1aSSojhkCdrgtf5RguLpq-ddZEh5BDJOd_8B92JGdsdMbXhyK6TVc9J6VU9e55NriNqKJGVTuTtmRMGUh6o4N9UJ9l1HSJO7_fxlWeuPH5eKvG2D8747MHLqb_RyiUMjLVeQASycgaTugT-eEUHm1yO-SuPCIZpoQLWVjyM9gpaHry-EdV6DCWbKP1EWGZ8WHuemm8-5Esa6gQ0galibPSRs4CWsTUYUkXyTwFvEVgjgAcurD2OxsQN2gG_J2EaOCcIwFibleNOnflENFm1RN8lSeHqfwVdF5tsgBdf7v7QPMO-NZt777kKgZF5hBdaHtJfQV0euCilGhlNiFZtrJJBYSS-wH-zqR1aDNZg95u8jMelYiH6duLxTNmK834ZFpjObhlh3-GsbEe23pitZxC2cyMzru-EU2IXp3Y90n8rsnTIErhzASnbwojrks9uLtZ1lKWy7xtHYdW08Ru5XfFz56LZiDsRxSSc7d49Ba1rFZ00GnWw04VS7F8LsdvYjR1kZv93q3D4wb64M6rAzG0fzYoncNevvhCl9Yqvt6fO-AkYAhiVhh6bGCVE2oJ-cY1baSnyWFmXCVamzB2RXYKYurK-mgQ_kzFbs5B0NnLKJuFbd1Q07VpUE2khAnSyy0Qg_05z4pUw-anRyBPfV29N8OCOTPI7q0GMonrF-VDdIQ_TZHPNOIP-TCAtn1KuRlnqc_F0xv2YpDTbw5bUGK8p?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cImLXmT0WIMOnG9WFgToqSqRzbBeM4o48LekX-OedwWU4Rfo_L50FASPatLdeCfv0F47LmD9Eafu_qFLkQutnaPz4-VQ-_hdv7DSFpmT6YYK-jbpxpyWvE,&b64e=1&sign=9d0becbf77cea7dcaea4397ce6b96f7e&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 592,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 5,
                                percent: 1
                            },
                            {
                                value: 2,
                                count: 1,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 2,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 12,
                                percent: 2
                            },
                            {
                                value: 5,
                                count: 572,
                                percent: 97
                            }
                        ]
                    },
                    id: 111736,
                    name: 'ANT-SHOP.RU',
                    domain: 'ant-shop.ru',
                    registered: '2012-07-17',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Багратионовский проезд, дом 3, Вход со двора офис 13, 121087',
                    opinionUrl: 'https://market.yandex.ru/shop/111736/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7 495 642-20-94',
                    sanitized: '+74956422094',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTpeGwlKp1_Tm1vyl-SYUyAt3g0RJjw3d02gYuPEIIGKA7SmppKw8ATYrLV8PhC6ACd0BzQnfX1SeQNSd_ae1QiFwAXUY1uxVImz3PY7xJTaXay3txI7VqYClOLUjGy02NjuH6H3y3uZEeIWgZFvncMrPzvvt4NczhCCyIElMyXej8mJvBnSnAY8Py1x1_YrXemtMbjx81atRlQPx7kvL86PuD8yWnAJo9CH7ToNZP1T7ZLONjSmFIpUT67n2cwDC3-_ekV8x7AgCTalCrTtQ6EmGL3lbefCPTwcEfzJl8I9X3T1qgjrXGw-txQFPG-3OljTGY-Bxn2GpHZJZ4kZiBW2S01vyOAwz6mY_OlDW19cpvtZ4IQl5i_tic6EJIG2DBK_6sETROwbbHoeNTVfRJLAl8fHoddlQRI3xKQMO4Ookue3QMxktpsoJWsGZ_CyK5sdUVescGlmRpNqRzOuc8X59OH7nGvyDDBgW46KEq-c5kXR9bRQ_ApKyXwupkzYZ2DXPbgaQgsPXmOrdFktgC6wOBbfUT7uISx_bREhh_9zzitWK7eCe_pbKdftjGpFnnJGhSSUKcEXgoBtkrv6G3jS7L10NdHE_2yX_3tA9YeTttqppIgTBcECQiF5N2573PV39EaIjFG_skvHNgm-BqnirefHDK1JO1U8zrRoudWSRDaJ_Gevc4UbSyBk68P6lwcjVOT43IIvfp7vzGIqjuYVgPVSrcBMNKw5OqJqtIJKomOmdtIeIJHRmrTFhjK2OiybsbIjL46P5D13mitIOIAVvYSzjcMKYrLfC0cm_2Y6FFAoYbPrE7Y8hOq20Z0dHGYJHGsq1ndHNJJqjzsnk-7Qs1lRYq7wrB2VpHxX7KpC?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-7sTgx_OzsuTJihsAqKQzHeHxy9TZBCO9ajMtJiNRy06SOtMsZEAo76a-nyP9uUakK9PnF1VddAgRUGd3sJMfi8rfT_LjkF4JyMR-8aZPKQhgp2YXw0ROMssS4tY2AD8htor4H7D6nKXLKH3L6oA15nM9wggVfkkvNu00XO94iWw,,&b64e=1&sign=9ab7794544c1ff4285e3d2cdfe789c79&keyno=1'
                },
                photo: {
                    width: 700,
                    height: 700,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/202387/market_YPWxQKABwFOyu5l2cOEWDQ/orig'
                },
                delivery: {
                    price: {
                        value: '400'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 400 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 18
                            },
                            brief: 'до&nbsp;2 дней при заказе до 18: 00 • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '400'
                                },
                                daysFrom: 0,
                                daysTo: 0,
                                orderBefore: 18
                            },
                            brief: 'сегодня при заказе до 18: 00'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/Mh3oV9XkEPGOxaMd475gQw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=FZVa4sZ6HJvTQKoPd1ZUOxtHno-cQ-BR9QrX1BfH8r3ZeDjIYMEf5PjE_x73y4ohrWRjkH46v9sd7HpwFjaVtDEuf56ByOZtLp9F_b5bQbTn_KDoAWQjUCCtrtvXINaf&lr=213',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=111736',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 700,
                        height: 700,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202387/market_YPWxQKABwFOyu5l2cOEWDQ/orig'
                    },
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202387/market_6-L_GE4bpgbbFO0mQ3Njag/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202387/market_YPWxQKABwFOyu5l2cOEWDQ/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202387/market_6-L_GE4bpgbbFO0mQ3Njag/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGOo_gWdvTGiUEgWBEWp4G4_8oXCPR6Ccgw2DK0Id36pBHLbX76M8xVxIC_I7-mUnSayub2xBKosfjnGO2KCBiXdvKs2z_RFtkOEuT2Fyj-WDgJ6_7abHHy2G0VtPaj5emnjxYDfZTCcbZZ05lK-BvxaEFY_0U8EHubPK7nI2kiGDgTFpAb-_Apz0uvGJDfgfNb-A_lYfOQ1kb0im2zy_hkQZg1vglYpK05gfbjXaaknxCN_lHg_Ed3pr0Th4PZ3iWeFseB8yey_PFee72Ei1SCnezwlJCT2AE',
                wareMd5: 'NVBFEnRNRBza2G7hFHsRDw',
                name: 'Смартфон Apple iPhone X 256GB (серебристый)',
                description: 'Дизайн И дисплей. И только дисплей Как создать высокоинтеллектуальное устройство, корпус и дисплей которого образуют единое целое? Этот вопрос в Apple поставили себе еще при разработке самого первого iPhone. С iPhone X его снова решили. Совершенно новый дисплей Super Retina с диагональю 5,8 дюйма, который удобно лежит в руке и потрясающе выглядит, - это и есть iPhone X.',
                price: {
                    value: '89290'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRdqR2jrUfyhKvz36OGz8JtQjNAoo8Mx4BdQ9-gqVE-5Bb5Fs_s8LFEeeSRauWqE1DkcauYjppGUdtGBuj53rbkhljSJQKeJnqkuUNsvIJRt8qT8-eY_ypyB1cuIjEahlMGGvNVEgVx-s3g2Tvuhwa8WOo3cCl6Sv8MQo0WV8Xc6a9fDrNsaqDIbccMLbCRzvo6weltA38gtdIoWZiDL81HZsbhP8xASRzBZuMyS7_QJPIa1n65g9XiH7BzBlMS2abE45YT6APf0MWGcWndewzyYt3raidI_bNbvbOx9sHX8uyxlybYyuyikl8x6MxBiDgc2oKKon0DzvKQsgY03MyUe3WdzL4HsWjxq2b0kAhntZt7e_mbki64Ps-JQ8nu_6f3ERQoxS5KFrl9fRPuji0YPyJIWYAS8o3HesiKC6IBF3BXLlD_TBfdxP8Pzq0-FS0Yl54oOdIJtjU5r5oiSMDlFTEfaziowNKQLgQWjcfCS-eMYdph7hXpQpA0ubERVnye1Y_13RdgveUTgQKvB0eF-C64OnjW8_9snFWT8a81621FRlzNvouRRD2PggBWQNhw_YnNbERADZ4DhIsQYuQPJf53QiXGJfwJ4X6H-w9OxDZVa5-jnjCzgKvweRoMqlptJLIP1EJvAi_atI0zfLrq_Q7XSdcdTt8OzqNCzjN2jdfP8jdgOBXfCE2wXtJ56Obt27chADKP-oA35j-Wb5GSpEUXyb3SC5qmKH9RhVrHYybF4t7kG35JItL6JU3DUKjALSOWXCNXHC6qjyA5dscJLoVrNg1a8TUS_VnoR7CUZ5JSu7imxLqfsv1pAuNNXIEF3hF6eUpL44DxcyeoZ9H1zNbmL6oEZQ2mChKHcpRvb?data=QVyKqSPyGQwNvdoowNEPjc5E6bN97n5WuzEawsno-KfoDQ6UAyPM1XeViHlvhvx_uFM2or2wshNbea5jwMt5vbqEQhO2Lh24mSANzAd1AJfptqt_WIKTBMwTlGEgRiGZPHz4eNPk4Sgl8oLMhrUtxRucddgNurayrt5MoSE02UKGRB65P8H9Gqz2NyYNK17T18gHC28QYwiLlVwOl-KjVog7ftkwgE10Uj8zm7pGt8vpHJ4mU3CYptLIf1JLL_N9nGPe9FYu3_5qxwZhGvF5P07TNV05fBBcWWbpVOfCw8C__1qO-6bnNZFUfcTLAGF__iQWQPK7En5rUHdeZf_lQuzfK4lB_PP44eXn_03p2-c,&b64e=1&sign=90050b716d7c9f1f2249e2f93f5290ae&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVDyzGPbXeC5kbhewaiGnYn79fnqQzvXqM2v0QphRl51wWluDcppZ2bN7w7GLRxEsMXNJDi2VR-oZ-Oo1qxNsNZZ79CcEy9HjmviwVUirR3pxS9Zp3CmZtXtFIPaNSE_VhL5lPnMFk63qr0u_ewlAcZt3iqyJDEaklG4IfePVSt107Ub9DPfB7n7JVrQILJsV0KBOckqCQcmmEjtqYSz5dswF_xj6behWSD-_hqiSTsf1oPv2QY1Bx6ldP1JLeCtpLW-J28Xi98fNm5bGRe1hMaG2OcHiSSRhP36MiK_9KyI6AMbYVFZKDydf_zgo8xVDfD_Ua9PsMRFGUvo9K52uOoAbwQ29VLM3qka7iHVhWiry9WuXZs7e8QLTx8jpPzBte6jsxkp_BLAnLPLIM2k3ilBYMmwzHjB_8-K7fQ6e__pXw0kXmqefpIuXgjBXZAYM9P6fag-UPD1oYjebvCZmcjvp6-zithKeZboFYezRfD68PzlR99g_gtg1w-dTsZwoE4GbBIiuxnkmHx0wCz940M6XyqUj02KS9bx7y-i8hWyKPs0D3COh68O633tOIFAhNclk9pz1oUX-hnPDr-ggFC6G1np8yvanD_w_vmfnFkKrrraoWPopJ3Vyi5h_bn_ESHZRKNsnYhR7V2zRD5bqzezsFwkM6VrNJRxSr6_X71PF5H0xtiAYy8SG7zz2ieJZWAirD2AOxBIXHyMosskXILpVEzrWBW1cFLPePMDvO1kPb0TzWCiKq4vrnZT-PpRTx-KpFaDgdOI08nzpGvi687igqK7w7PRHdF5NZjw_dZRlOugB6hYGrW9RyDQYiWU-07ZmTPtc_-tqkhbozlFmWPI3oe0NCgxT6WWmx901I_wR?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2R5v7GR2bn4Fbj6U39g-yN1-PykEu7XOk_w25vI7M3pzuTeIPWuvD5oyraRexRYEmSlcNHTqlFnpsEd2H_KnJxfKSWTxOJhgShavVhz8d2N30v2W0WO6AH4,&b64e=1&sign=850f015386d01752399bdc9033e76450&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 61583,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 6976,
                                percent: 11
                            },
                            {
                                value: 2,
                                count: 1790,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 2116,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 6996,
                                percent: 11
                            },
                            {
                                value: 5,
                                count: 43743,
                                percent: 71
                            }
                        ]
                    },
                    id: 3828,
                    name: 'CВЯЗНОЙ',
                    domain: 'svyaznoy.ru',
                    registered: '2006-11-13',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/3828/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                photo: {
                    width: 344,
                    height: 689,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_o_PGznWxeOWGjEBik6k5oA/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                }
                            },
                            brief: 'Срок уточняйте при заказе • 1 пункт магазина',
                            outletCount: 1
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 155 пунктов магазина',
                            outletCount: 155
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/NVBFEnRNRBza2G7hFHsRDw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=cecDNGxTi8rmZAKABiiFzff3PlrdloKkTSkWqLsEELEH-5mjbaMinC6FY2zvgkvTCSQv1TY5npQnWjKbIiMi9vbXc4lVRpvdM3jkpQWStXmMD1u1T10XWEQrNPkQw3eU&lr=213',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=3828',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 344,
                        height: 689,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_o_PGznWxeOWGjEBik6k5oA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 124,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_o_PGznWxeOWGjEBik6k5oA/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEZtWYoDuCCCn3uqBW92EhnXvL35RsG78KQj-OT1gDKP1ttbfccIUt1iwLDoQ0oTv0RuuPA2RIxdJUqvnqMy-M6T4VTTiA4X5CJ9PFAVISmuJT5AJJj1kajVgqW8PuH-dJHkMpH6-ErYjKx75o1rVlyAowpILMWVeOW7ZYAM1Vf-zDE5RhUVlKMwOgz3fg8upksQEFF7jVN_lqWoB3smtu3ZV9EeMZJ907cLMRcQXuu4BtHX7nFZ36fEvsqRrYqXTps-qTkvKl5wbem5U8VpED6uaFrhOFon50',
                wareMd5: 'MeJDun9Rki-SoK_NcF04vw',
                name: 'Смартфон Apple iPhone X 256GB (серый космос)',
                description: 'Дизайн И дисплей. И только дисплей Как создать высокоинтеллектуальное устройство, корпус и дисплей которого образуют единое целое? Этот вопрос в Apple поставили себе еще при разработке самого первого iPhone. С iPhone X его снова решили. Совершенно новый дисплей Super Retina с диагональю 5,8 дюйма, который удобно лежит в руке и потрясающе выглядит, - это и есть iPhone X.',
                price: {
                    value: '89290'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mXCyrT8JvdLE-jOiTvvEEc40CAyJARRrVneRQT0oOTPU_1qLCMDLWWVsUJNRBthq723lmzOlDBcEZHhiRkqvnJsWiFhNsRjZ8flARdnrbdMuYJUYgX_Ey_fPtVw0oPxWGyyiUc5nO4VPegi_TUvQZWKZl_zv7Jmj68ymVXIK8fo5dtPSlb8aaiDg9XrysuS-iLtt_foN7m3QSmbyYAs8LldOmfXoii4m0SKbyJSQIPGlUCFJHRad2Ipml8QEgbXLJmZKhZE9YHgAZO6juNPgPemUEeNRClK6O_bpWqiY1dtzqop5kMkVfYlqJZLxf53KNlEeqyVdPKZPtL_K9W6j-0bloM1-oPmqLJo7qfyfO4X1xFhuRBjzwWFreu4KPBL1wl0dZzithRlEXcCUTJ8oz4CDgI-Xyu2fyMr8SADRJvD5HxHMpWskhuzTUFfQjytmTwM_X_OnkNngHC21BIhhziny9cUxbxOmW3it2aJJiRElTdExBsgRQQD5AkOs8DhM0viGXR0-UyEqaBijUUmfYWZ-RheqpLRsPkprwoXK99vXpxCmrwH-EPusol1VmEpZPKTtk_PCWtxCq2tpRRoUcdMqoeWn63LvVGkaF8nEJSd5m-TB530WQz5IErVZXowyBgoKCj_AKaD4Yj42k6WXRTIukCV757GrA9t_3OcK93ya3-FxyOlN2F_g5hReaxRtogaQcRIEMtKOboOZSvxBgiSN3CcXtA9wPEyMuEg_i8vzLr-v4VlfnfWNCQH64f4WRCNGZoFHrA-M2i3NvfMU3INEH9lrjYFoSCOsd1aR8iT3XABaaqbu0mk-RmuwQJCyRUlc4b4-mYBTg6kr3n0kxfcPiF7TfoT40U6b4IJv48d3?data=QVyKqSPyGQwNvdoowNEPjc5E6bN97n5WuzEawsno-KfoDQ6UAyPM1XeViHlvhvx_uFM2or2wshNbea5jwMt5vd38kNb5WESP4F2zSqNaOVuiz-YJHr5_rHgrE03J-dT4FLk70C6zTUBe-SkYTij9qsx4rX8qh67c5gnz4dLnQaMZemsFDE8U8Dk-cEMOsSa0Y_LudKduKP9At3sxozzIhxTax5HS1cvrJiB-MMlsltTbryGeakPQOjqcF0MnsRxaICTTL2J0ok0N9GndWIrT0mU3lOA78wiQo9Jl-jH3gkUyksFgJdXxbAhbJtAjyPyazb6lxd-9yAu9ub451MZy966uZSOsVRSL0EaDdLNuxHQ,&b64e=1&sign=0dad93c0ce18926ffea05a324b82abb0&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRv0g-viNRgqVKbA3c9zIv9BgGz5InTvAk2GwaA9OpbE-s8MrC_W_JteHOdq5iqXIj-G_OrW8zOZLRJ24guKedeoH6eGsjJJYD0iXsv1xd6jw9eCjWwHpbtVc1rRxma32aV3uLt3GTzppr9ot1_Y8KVp_Ae_knhjtHQiGl6JjUGjB2tIGHNti75Yg6d31bcR_7MzAmr2_7TGYHRm15Hb3dTvpF6RFaDLsy2ss5x_Qiikr91rxPQ-VCaflhgFUpz1EaDOda_VnkwUMXZec96N0jpJTj0kN15E3Tzk490FrOj044yzZ-K12Q8NTeoUKMC0UWi9uqsJcYJGXtnIXT1YOilf8pIj8nLSMY6bONUU484LItz81v68tNziO91-gtQd8yn6ZK2B6csTFmvmMXdS0UvrPg1SduM1HEPMj68nqOF2titDcIwXggwstvxkaR8i3yBt1906zUGEVVgl0itZ9MNcHpkd2Oo7-yTCr4g0OHo429f3vYdDOIPkVa6e9sa15pFb2XXoI1c_bNt8olgm7tWVdrenf78n6Syn9SnMlhw9Mt4sxeeQKhRQJp1nc4AzSrgBsiTRHKf0nxP_S_WxRHnaAUmgssf-dyhjxkm2-HVOcfs6q-eibj3EpylO8g2tzb3KRP_hGIkEli-BJvhxjEvz5aMe0NDa-NE7Xt_9FUctR8W7eIeq9yvgvvnsi56qIU3Nhu-URUEuHLQxpmJTcZA_mBVlAmn7Jvl9c0yqWdY6NJ63DHqc0XP2gmJIEZSEoYa369KTA-wvoh0mhoBa7Mk8AYqma8o0pkqpkqQcuDUF9I7R7ob8JEnmVt0HgpeMXWU6j2Slv9_JMe4AVOEiov_fTyA5wAdcJd_zjE7nNvp-H-7tb8wQh1do?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2R5v7GR2bn4Fbj6U39g-yN1-PykEu7XOk_w25vI7M3pzuTeIPWuvD5qXxFdEVWw469yT5qxWNx7VsTt3SwcODrSKfvptpghsQAH05Qp3d6o4yAaljGp1JnU,&b64e=1&sign=f1d8468a5a45e0572cf45b9199aa3f54&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 61583,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 6976,
                                percent: 11
                            },
                            {
                                value: 2,
                                count: 1790,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 2116,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 6996,
                                percent: 11
                            },
                            {
                                value: 5,
                                count: 43743,
                                percent: 71
                            }
                        ]
                    },
                    id: 3828,
                    name: 'CВЯЗНОЙ',
                    domain: 'svyaznoy.ru',
                    registered: '2006-11-13',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/3828/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                photo: {
                    width: 344,
                    height: 689,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/231668/market_55Cyg-zXeW8p_fkC6RIKKg/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                }
                            },
                            brief: 'Срок уточняйте при заказе • 1 пункт магазина',
                            outletCount: 1
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 155 пунктов магазина',
                            outletCount: 155
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/MeJDun9Rki-SoK_NcF04vw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=gNNYcdQCqBQOUn9R2gmPplek6IIeSD0a2OrF5EuWGbKE9y4GHsiE7PslIksdclTz3j_QO5wZvtIwKbVjfEtlLLMS-fkJXBj7iMMbOhg45Tw5qyC-0CdaNftUAhAcmwDR&lr=213',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=3828',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 344,
                        height: 689,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231668/market_55Cyg-zXeW8p_fkC6RIKKg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 124,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/231668/market_55Cyg-zXeW8p_fkC6RIKKg/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHKK5VJLcLldAwa23oMqFsh5yChP7WCs0v5A2-WiSXd2jBBIm1cNSb72YYhPxrX9rMPpp238EFButKDEemH45VDN8hsmkZNLNNumgNNqlARyTsCTHZt4YM8aSLm8WPyLzynjNuSAmZk9Bldw3aUrX3tBRk62PYINIvy8pp5JnhDRHKZx8xPOS2HxT7_dzLEQfPO6QUVzf_fWnTnn2GHceXO0MDEeqTz0pYtMoo-be8H2oIIG7vluF8EUZ5UpvZHjNhVheQ4nbfxkT_Fh8yha2EgRy_vzMggDU3EnpxHHuEm_gsmv4qnNP6G',
                wareMd5: 'rT_28eTnT-zV-pL8tZoO3Q',
                name: 'Apple iPhone X 256GB Silver (A1902)',
                description: 'Apple iPhone X – юбилейная модель бренда с рядом уникальных особенностей. Это 5,8-дюймовый OLED экран, обрамленный минимальными рамками. Это распознавание лиц, отсутствие кнопки Home, анимодзи. Процессор A11 Bionic, усиленный нейронной системой и чипом движения M11, легко решает все пользовательские задачи. Ресурсоемкие 3D-игры, графический интерфейс, приложения — все «летает». Автономность гаджета высока. Заряда аккумулятора хватает с утра до вечера при типичной нагрузке.',
                price: {
                    value: '80990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mVA1VqI-lNavN4qAlJjQbu1hLgNg5JNMm4747e0FQvKcRW8Ijqnpfi1kfnVs6Ifoz6rmhUO522fRGW9Od2nddv59qoDshGJ0YWR9pUAw98LbU2FZzFGsGXxPX8zM0ELhWwGQTdIEFpngFtnz_wxW-nebpokNKSifIIcv_Z3oW93nH51zKhi-bbqGD0aspM20taM8gvJ9JWXHfdRJgOip05tHGRXPAV-hKnG1O4unztXhdqv_BRSMiYRdX_vdVQU3dhj6osLxfXeTTn-fiIoHVTOTfc-2Z3yX_lsEqi_nMW4KURylbKzJxzqnIer_w6_UMtD-Ef6Nr5pXRiiQKwRK4-rLvAj5yZ68-cUPFp7Vg3l4BLWTUit-S92aBVlTH4otzbpUqzfh9s0gdhYjj5SLxf6N_ktMRcroVmrSwy0XjOrpZ2QakJN6EfwsGMZc4c895JhIKtkNtB-Ob4YjrQWnqrwAShehieFtN6S8CGJpdLPZyJz0gSsYGy0AURERwHa4O3d7FSw2oXVYTgSsQ3-0PD-ibW0lpW-oQm5NGILA0TLILToQJ9sdHS0WFTzUURRxcbueblv1T4PF4QN6X5YKP2K-_lAtuSLAIiNf-unwzjGrTuvvLEZaYm0huiSai_xwwgAO1Ww18MvuI9Unhh7_EivIWK7HQfpCq2GqlTuq9UaA3Hg98VEmyEiaqVKSsy425Knsh0bP-gEX7-8GBY36UDVoBefenRHP8NQPoTIYNm09jUu64765sNFo8wmivzM54hRr1nMNFSo4CCCYAtdUl0XTU3ZtoxCxnK_gU0lJ8-0T_--Yjp2P-fx4q4vIpnHqx3cUiGdfeT0CKEdHm3_qkwg05jsO8XUFyqjxUTx3fC1A?data=QVyKqSPyGQwwaFPWqjjgNq5ob6Jq70TJiGoeif5hpcbP8YccfY02ogWNdXvVXQnUtSG2m_TC7d2bPi2wXHRJMk1-9eyQ9C3OBhHHbBmX_iBeICcWLUBqv09Ehbgxn0S1uNldSYWEpu8FZDbxWIHOZ9KdbNL0iQEBY0-Ytz55H2EB7ILP9nSQMKDgitI4R2L_mwQ5bGe8wsFFpX87t80mCsbTieXP9O_nyeo069l8iHFiAh31z7DOfZzqEZ2I01gZ9tNgemyrOvPY9WBtCEgwlyibICBhgw0R1mEcJP_LB0VmFpbDybQZY1lBT8MQjF7jwmYrhXrI1_UEJP3a9Eo_XfYanPJso1C80xjvN-GWtrEadqfDbeE31dwvGRrQnRgb_5yENkh6TNpeVkr49CndjGk-PWfDG-ifvA2UoIBRTjcd3Eh1kltTFwwp8yDTVbqTB2nJIqWW7kQXJafxMp88ycxwxpSOPOJf-WYQgywNRR_M9mq_rBi4et3RKGCmj1J5&b64e=1&sign=05ba1ace7057a22231c03f59c767c279&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRufkc5G38e6HS0fBknVCVo6q7v3iHXoEi7j1wf2Kzx74l72cxHRAiu1WpslqO1-6LYt2rNuGv-iHiiCE0bRS1Kn8qLyMkV_wxaMwBrdUIxoIUnWjEECfmkosWXAumLxAiakv__HMJF1cjJ2s6VJKGn9w4vSpMRXslHIbHm3_S9Rc8IWOfzRgCVXyTxjWV4d9uCebZqscxtSe-bdQt605VG4RCH5cka7CYuaTBd1SvvAKq5smzeC4GTxLHoQb3vufLJwIRIjastY-CpLiCuT2UAPW8X0OEO8pMbOkVAEhDpt1VM6tn2zpiip84TK9rxhemvjmB-VbmJlxAj2YGdFmoyi6aXy1aEY8PWigXAV4m3z-cTFNhnMfpZEbasDol4mneuTrLVYuWbjjJIcd7DSHBQA5utCEZKSnVOVGH7q0y7x50p2jo9FPEb1qlA9vagegGHSeCGc_B6cDv8tySz-isfPXAu8Ym8T98VWAxha6nVLMZpplANlR8tok3fa-nEIYcUQniJbzGvUI8fK89Xis5AcUf9l28DCsnlOlEK95rokQM10WUrPiWRdSEMigkkoIINFn8eKc1XoRivI_iiw9szzi3bqcwwKUhztXn__7Zz928n6vWAMRXGOwqlUgCOLp-HTc2iHkc_8KwcN3HoUJVSWB8zMqeBcSb4EmeTLrQ1HbDhHxjGa-j7ab62aSORpOH7XL75hl6rD_MWNtTl2QJTfku6b6vTe2tWFp09OzFpX9FYUXLgmvU9r2wjKqd_NT8gcLtRyqXtnqYduRO8Gb3jZqncWMOqWO8MBKR9nDh06ShxtTLgU_6emuv5slaJ7oOrUJyp53S-CjznK5qZy7sRs9oPx06d9cgD49l0LYFXIA3db-y8H3vYM?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YtduRh6BN2pHixvlE0VEivK1RYRTgNLzEOjbbrFiS4JceM5hSFPnToC3A930lnyBVpGwOyBdN81sf2rMOcKkVT6Rg4_8mem5yNnKp-AjP7IOFTlYvkDprk,&b64e=1&sign=13b18436a5be182052801956ee67cb2b&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 5004,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 160,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 56,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 63,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 378,
                                percent: 8
                            },
                            {
                                value: 5,
                                count: 4349,
                                percent: 87
                            }
                        ]
                    },
                    id: 262,
                    name: '1CLICK',
                    domain: 'www.1click.ru',
                    registered: '2001-10-15',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Садовнический проезд, дом 6, «Пассаж на Пятницкой», 1-й этаж, 4-й павильон, 115035',
                    opinionUrl: 'https://market.yandex.ru/shop/262/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7(499)990-77-77',
                    sanitized: '+74999907777',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mVA1VqI-lNavN4qAlJjQbu1hLgNg5JNMm4747e0FQvKcRW8Ijqnpfi1kfnVs6Ifoz6rmhUO522fRGW9Od2nddv59qoDshGJ0YWR9pUAw98LbU2FZzFGsGXxPX8zM0ELhWwGQTdIEFpngFtnz_wxW-nebpokNKSifIIcv_Z3oW93nH51zKhi-bbqGD0aspM20tWyYhrKMjdX4JwOod2xWWwEX4cwxkaPJ0aa-DOgpkL6VToxMSPNa2DxNP3vXjZL7wgLbEC6QMlhX-rRiJj51pvMzYdQnYoMEYPbtCZhfJIDKILci4fEwAsVpLywB63Z1t4idGi9i3yEUTGhNkx31rYRLTtzg71YXgzE6CLXzn6svOmttLSwHWuGwVUyllGyfSNzMamHe68tgxv8R9L7OikkKSNwGuCohdn11uYlSy4U9iTuHGBACQ3hAkLqwwfdvyG1pUXhIjYttHBDm6yEnUqX4FUX6afeGL3TqFyePF2ElXrVLPvAT7NLNilykxXLHv77Zt8RQejlFosZ8MmrKodBUTCy53I2fnowHAJYT5M5Ayz-iSjT9xNE5kitRdrX6cTRW8CQGlYFh6Ai5RiO2VwB0gudpNMq9D2YSteesLoiOBrro0oUkB05BlN9HkZjcqy3ZMPwRu_nwlMfadm3JAV9Q_hNaHmRpgshO2v_8RFHNIPHrz5GyoQmjGn-77xuGQ6AUykk15CXkYF_9h1cag1sODjThnrvyYGzfEXiX6ymA65SyRU1dYinQYEMqOtkCWMsGGHmf9FANesurBRfHHQQXl30d2JDNZfTjIYucTiz5HlFG3z0JHU9TWKnlWDKnpdxSsJCgRQW4qjB3r3CYQEEQlGYL2NTGf0awne7FOgVr?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-76yrscgpkczVi_W65repTctRVe5mHKsSRhJwytAzVUXNNtyuwgZbR0VNAJliIAN1yWPURwL8OOi6Lx2qEwtkH6b4n74ptajGevmxEozkFFYWAJ7x33ZkVQiOqr0BWX9blZ57ANf91Idea8Z4qP82_KKq8e0cfjHgzGSZZ1sliwA,,&b64e=1&sign=201a1bf64ccac271ee719335c466a3bf&keyno=1'
                },
                photo: {
                    width: 183,
                    height: 350,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_C4QeB98Ex-PKPnOV4sBfAg/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            brief: '2&nbsp;дня'
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/rT_28eTnT-zV-pL8tZoO3Q?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=JVhiPZj4OTpAOw7l3uEYy5OQGTdSGEH-0b57UltOSk-kuJ2u-ruyp-cU7K48eHBRAelAFwgQ3MdX0abHM4wMHF12Nf1729-kwWn0efJ8fRzjgZff1B8wwlp-pMhZEXHNn1vfal94aWM%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgcaKPFUN5NJhNKf_z8RL5sA2WVzaJXx6S43aVfGY3tT_ZsnMJlTjrgGPEt_PPLcCLsrTMUoUdcr5BmmjGkkW7uBmASUhFMzBN67JKUSp8QpXDIkfUz2zAutZbLz9iElfHEwwMiEZHyS51CpTQ3liMwTtfoTxAVZMpT9uv-UQI1EAUPepxYSmucCd4mIskkcKsQCCDAIYyBEhY-9_U-Bv5EhLYCeLLuv06FEFja9NW_z0W3yQI53HjB4bRflMF05GSK-7-DZTbsOIA-OFTA4rmb28P8i2XMBxNsFLTzxaYVklzekUm8tRpyI9si6l7IT_AnUXgbh4OxiLUT6q0emS19DKrEQ4BxPNMw7IHv8XK25IOYmR5Re0OaYiiBZ6erviwGOKZ1bBS82OxRtNXJbVkX84ygwh-jgOzLvibequnuiW_XrAGTNEE3hrw2rxKrmrGWaEsMrayJE-_bU33q_W6CE7CQaAAW5oltO8b27mK-XrAmzZuRa8Ma5zchurBq2FtSa8Y8DPL-oE_B0-I7alpXnljulwLMnRSbpNhrZdffYodDWObWCkmX9wH3dwlH8BLmMj6adWt0Ft70NFbDhrJBt5PzMSm5jM6BPGpEQj38gUSFvXoDG6p5sBb3L5wAy-PNSxBuwpgqDGz7nnD18RHgCeAAn7Mq8W7n_gNz5Ytil58urmejsx5a--HUWd8HSKzKHJ8SJ3sfDrt6SDoLwcHy7B8K69meiENfmajxxjGJDILzhb-oR7rP5YZMinAt0Rxeq_zZ5_yCcGEciI1UyV1uaSzPiWHoV0tVCl5OmnVwyinLBa6yZvo4rO3dBA9A6NhaqFPr-ZpnIv5Hs5rQrUWgordfPCojrsaQZ-Nv2FAU8P6n_Tx2kDs-PESCtUqu87ww,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-4T48gqqHUsxLZfCW42GjESDEBuvSATqWQv01zCKb5nqlAyi6BN62cC-Pvoc7w_aRDUOdyz3y-6G8KLFpn6SZnuxdJy0dA9kHBHMG3RNd8PfquhMeNDCG25PhYQYm5BydDePcT1wIp1ixW2PPNhI5tEzxSfbBk5Jl-l-yWOFoZmgwtX9atQpPhf8F7Q6XhGt7gLBNVV6MfvWpnX8Wq-4_GEH5onoIHQLY6r1oSATtkXKMP3K8Lcvf_2ezd6izT8iD33osgnIXhgUCxcrKMWL2tEDIpGjP9-NUZMc52zfQa4EKOCL_z-AXIsDvAr19f3UsnT_wQRMTIGb_OaYE41Fl2JU8LXZrrJt8s,&b64e=1&sign=0d8d3eca97bb16527a69f04b4135cf42&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=262',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 183,
                        height: 350,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_C4QeB98Ex-PKPnOV4sBfAg/orig'
                    },
                    {
                        width: 183,
                        height: 350,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/331110/market_7BTNP5W_tB9Fyr-iAl5Jlw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 130,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_C4QeB98Ex-PKPnOV4sBfAg/190x250'
                    },
                    {
                        width: 130,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/331110/market_7BTNP5W_tB9Fyr-iAl5Jlw/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGaVr2CEwdjUlFkNCcGnuZRm5DUJarextK7_iwY3L0eDvaWAaU8xTlZZ9mQ68Xg7pr05-BxFidCtNkH-UKlHkHcdHsRpX5AE4TMzS22XJ-_4PpuEjUpUbf_gJNVgjiCi67drP7gE0wjpVD26MuVK862RJ0GPTkAK8LGzATiyOcxTCaGcIjsX7Y3IPuvPF3kn5OM6sGP_ZsZzz598VMf9dQI_nooxLbuV5MyxD-8BBidPFvSCJDrnOk2IWP3MLpAQxqqq7rRoZA2rrCTgHpd8FihI4QtK7Rt3lkkT6_yHnFpbx9FVwCx5mlf',
                wareMd5: 'qv28iYMMksLdmI7oC5KlYg',
                name: 'Смартфон Apple iPhone X 256GB Серый космос',
                description: 'смартфон, iOS 11; экран 5.8", разрешение 2436x1125; двойная камера 12/12 МП, автофокус, F/1.8; память 256 Гб, без слота для карт памяти; 3G, 4G LTE, LTE-A, Wi-Fi, Bluetooth, NFC, GPS, ГЛОНАСС; вес 174 г, ШxВxТ 70.90x143.60x7.70 мм',
                price: {
                    value: '91990'
                },
                promocode: true,
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSJRPhyIkHxCy3dqDU-tABKfuSFliC8Rl2WZROkuwmHi8e39eNMwlS-TYyH-p4uWSinraExXRiaIPkOnyFfEruWOR8Uzv2lEg8e1A9xHDqZWFHYE0clZii8WgWclA6dx2ylbf8Slj8V2QjKzMOXOjE6SyOKmTyvmkFwJ_zKP_67hf6nbT4IifSp02mLVY9zj8yeGE5vNcbECPbSaw0txhJ7pEw7uE0NKPIh1MHnxroJWU8WuXInEl1FbL2TrAaedUwzTczZEeko5eFNRpNGJnajPlkevtf2kkpqseTKT3BfD6_Fqgl3sC4RABCADs1FqrnZs_HBzkgX6av0npSo6qRfbG4Uoa4xWOpGQ8PvNxGa23HvelbFx3RTp7qMKsEM7LvGSpL9_l9xLNLlAE3TAryN_WAiHw4ouQxm8qoA11W_Vkr81FjDa71IPoV-QchrYmQzeJffwrjCEVvSoMTDvPw1HnfGvJueuPp3wZeE9ZDxmtu00FOWQqwpbkTpdilrXWz00viZUT5K41-4DzZkEPSnj2XNqmXXYbDXRrXnmxMQvUN8vw4uNcTD1C71xTmy4DHI0Kla3bLavFOkSFIM-_cvzLm8Lse-Cf_Qdla1zG9pDB0TN0e8jBZQRHjHjbbn2UH5HBLghXgkNDH7iZHvw10W3hJ9Z54bpFf6bNOnfqyPiJvGXPibQ2fP-737-YKJbQ7uDGQ7ZBY8mqObQ5cJF2g2GRO-Q4vgrdNBWDxq4fF6cJsZsUIDDl-w5N9EHUS6qcrq9xomdzJJkbwTPaewjQPAUe_KKtQvdn5NY-jmAalkPogrdEU1TNVIKrJjReg3S23eNHUJo3af0Hpz8gnF1oe0UPwlxuLv0h6oAhjhMc2Uk?data=QVyKqSPyGQwNvdoowNEPjVpPboYa0ioUFVtKkPAY2mq21KJUlQS9zG9cYuf2iXOwjYXTqRTLJfjpvRXMaSYAL7w4a_6kGnswgsh3cTQw7xchq_FJwX6MiDLZNxxlVT0zuMclv8PncjSvMsjTc1cPPO3alVovxRcKmWS0ZtOn5WAUS4s8l-7_yA0xFflFm-BXgXeUb_TXuHf0FeYAb7c4UyZSa_yCFAVKWajQZhrtSZhm6pl-CIW2ipL4xBceYUR7Uk8ft2mai7pY5OahCPtQvfhATnRpTysSunYYl7zEicZ24hE-yeuqnlRK2kVTuFxMVMalZ9jJK2VaUuHA7_1oOXT4OECzDmI3XJGTdC35v_D-4mwPoOU_6_7I2QZqd8LPkUjDFiqP9dZTQeMVsjnmQkgY1tIbFC3Zb5nIacU6ouF43_NOU9SyBNrCEZ3MN4BxvNrKVj4OZKk,&b64e=1&sign=f4765b3456b4bf732e1415756945db27&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4ieKqBCMGOfMdFRhyED7UeXwCszXJRwXNPSIOgHSTZOOMpBpT0suaXRxwygr_jCSQH2LKn2I-guREqM0TRL_xMBCJyAcJyRpWyHnKlBHxH2qbmmNGB_gaRJtUi3exaRLIY1X2UXJ2r-4nbTpcCoG15z0CDAst0Ta0PGRfqIFSD-uSI3_AbvNwl1-ffVVHKH7jtoLxxOX2WNaBpySFE93q93VaNcUGOQAmtqch81lh3DUSp1izbopBz3kFBKapqtWf2gfhdCR9NW1EpbDBM1juTQLt6cSFwFFVutPmdn-XbUzqfF1bthmANiNFKJ8rfaI5AGNdMmsIDXGOx1_KQhKjCW_XTOG21fV9o9OvLKL9M3pbuJ4v9-Un_Qvj_r4_Arq4WWy7G1AzumKqFLTT_KRoZ_UaooUIYLzrbw9NLuuveP7OOvqE32BFN5o-JQxRxeHK6QvqAPU0yC2X0yyTGdbTDWjlh46gHI7H1dkmLjMQZUSYLSYLpORvZRf2yrhZA2VPt3yaim5Sh7Nk_HJlDu2IPYUG70Vnsc7J4Q2jCsPSqFwicYMxytmZ8jz8065IvdFhahVzPHuwO76xionikg8b4wj1ploQRwyItQtd-szi1lgD2-BdG69MBBvYOfM3ElTsjin2cV9LDpw96_L9FNtYbL89nq3pPD2U3eN-fhKbFJ8VZHN2q8MCazAgzE4QSfdTA1URFfUmrDiP8K-11os9fRVLSIQ2x1_x8zPNwQ4T1ydQXfjnR2sH7xs-BzXmqTu8sDEiwwhVAaBShfhyAhJotkw6KkA_DuB_XEHJT1oIdfE4e4Ln9TFyod7tiXtvTNwB_ssPCk91JlI3qlQ55YkkIJE1wlTo5TZZrRzdE1jzFHkV?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2f-J_WgtEnz5P-9XSUDTliZ91RzMkyW3a_AQM33yEH2pVTNG3dFwQnoDbHvJamcDpDvTA4XI1Rk-NE8a-jeQNJCiIRZyVuOhpVNhYtb2IubsOQfl36Fkewo,&b64e=1&sign=2afe7112fc5b36c55f3b57342a275591&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 2770,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 90,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 35,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 39,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 204,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 2404,
                                percent: 87
                            }
                        ]
                    },
                    id: 74832,
                    name: 'Phonempire.ru',
                    domain: 'phonempire.ru',
                    registered: '2011-09-13',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Юных Ленинцев, дом 83, корпус 4, Офис 16, 109443',
                    opinionUrl: 'https://market.yandex.ru/shop/74832/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7 495 221-79-11',
                    sanitized: '+74952217911',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSJRPhyIkHxCy3dqDU-tABKfuSFliC8Rl2WZROkuwmHi8e39eNMwlS-TYyH-p4uWSinraExXRiaIPkOnyFfEruWOR8Uzv2lEg8e1A9xHDqZWFHYE0clZii8WgWclA6dx2ylbf8Slj8V2QjKzMOXOjE6SyOKmTyvmkFwJ_zKP_67hf6nbT4IifSp02mLVY9zj89toYH6ydC73wYTAIDU2k2RrhaYLH9xc4DF0ycAaE-5CF2viUf0j-MIJI4FrGuPeTeB2h5qZX9huCmlKwA_hmgvia02nLE9EQdY_j4xboH4lPzSi92VE8ErWthOcC9S6Yut449ZHIgQF2WUK90sqatBkhCBU5pKf1aOiPwkoOQcwneEZJAkV7-h8U3CbLUPbjA1NFwO2MuDP6Zqt157lKK72lk2TeiBZ1QFnLLg3Y-UDyWX7GFwNomygOieHbk34tBMczKgJ-HW04Z8ZIQ8r_ZWLJczmEyMQAxqHLVp20RzLqYB0dHa7QKsDmL30YD9uCb1ieI0JPPkqhhFYAxZitgYNeAaKViiCpUDlMCv8OkQOHfzXXqqiH2Pzx3at3XU55GFh_Qu8Fc2wgheWeB5DGQ2895lyXl2PBbC7nt4-8iaTBLNyra8xEU1dnggFhYEIZ9tLDFLcTho4iCHnlH12GYTpYsocf8aAcqNhO7m9HAIlxgKQXGHMtQTSOrgfGiT9vZ4W9hiRtItbswib260yEdfUvL0NDCnRLvf6A1mHJCLddigQLEu9Isu2mF4pJW6vY0Tbgflx7jFzDWn93m9KhutadZFEJafu_QrbrXpQhBcSexpsJJy4BEKF1HRWioyg7TVQToIo1KIaddUUkoNcX4kreN8jE6Ed0aB5KpXKbcQS?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9cnNvW9RXqiR02PVXz_4wWSDYf8HTR0NnUslKP3TroB-I_Bttt132uThI9zFj7QhU-0NFzqGGkHOPjTNsPoBVLFw4mkiwQT_GovgQArs3eySLhiA7XghG457bCVlovF_Jcg5SYCYaUGM0BTLPiHf6x66oNovHDZbsiJTwetOEDGQ,,&b64e=1&sign=5dd40d9356ec2f95c41084dc5dc2e5e1&keyno=1'
                },
                photo: {
                    width: 744,
                    height: 744,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/362398/market_8Z-xXYZnPnt5y8-6CVy39Q/orig'
                },
                delivery: {
                    price: {
                        value: '390'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 390 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '390'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/qv28iYMMksLdmI7oC5KlYg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=ru1X1faOnjAzHGEplzXS_vkt8tu91QnMkRCMKiws640o7wKTe5IHlnCjf3j0UwKbQneIeVnZe3kfTDyRPi2VAZpmhT_uiihGsUNFCIAlWOOZqPZ8p4Dds2LTh_ALhosb1Cz3KdUzclE%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgbHb6joFcwnl0GEDJt_cLT7d9YGEiK9jA3lyiu1cpeJtkEGnwVDCUMY2Cc-81-yFL2Dux3DJMGKlSFbpoUz44uzYouk5oXtOsK6EPtw85GOiG7YIDQfZYbLA8-2f7oVMNWcbwbvTBWRsC3sTa4VbZZ2T9SWdGQGf9GXCwYD1HTbR8c1qKcVeAEpuhuV76R_xgC-4dNmc89Z5Fm5TxH4-gL707Mtea3wF85PbSeGAFHjvmNnHnzAXuX62h5Sq8QlApkXYDx0k5179Ik8U9v45xM7qsUFGqMp4-1FwqDfCxDc8IXRc24FLJYyFpXtO_ET12Rw8tt9xkmcM-TgJWiL4KNI44SBwwH5fg5Rl4rDmzoCIh9-iDwK1hJw93Gp0uyIMMgR7uHQiZ8J7pXkFaDdcAQcfi4Q2YSl4_SAlRTfXRScEXLDvOxMoFv36vv3N4aGS_I9edMLA4v1gWIXHoOwWsBZBmmC_MQP6CuXLqyDVi6kiG41dDqaWbr4wgevkhFcpMhXCStmGCU-69Qb-OVNEX_aw4BvLquxvFExD6U1MOHVqmV066R8fyTdwHaTqbXwZF78dNySf5ecKeyJFsPPP60hFRzuKJCg7h4RAyOMrIhGZq39QIueIE0y1I6RcR_XuMU4NHPA0n1IZbMIEzgR3Xu2rGeaAswJEaRuqTGgYX6RKrdoRHkzwfxZY8lDE6rxXTlKeL-imPnIXFPWjHCxZBv8Y9wHe8qfiDBEXOjrLYLoKvdllE2figSEICyDXkE2BP5uSdhcMezbpX4Za5xnR8ZivU3Ch_9fYYg7oxMjNz2Z-crrW1r2Ii9rBSuc41zsSSYjbvpnJy1KBvNoGFMBsHRbr-HAVoAVmnLQ8wus7S_2cvoY2TXu8WVOW7kz8yfOlsrgl5YY9y8MO?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-620yvVzTqRVPhBZ31cCTd7YzxdqZWnuzKqqZHJfZrnH00nvgnW3hHoYsuz1PKVM7o2lrY1bfc1dTsn8sgN-3YVhyPu7rHpoe-gIrCltwmijb5aS0C_biA79_xAJEAQ0BMxyBEOwtRJuFHv7N2juF7Hbk6sJs3xDGmV2HUXKcI62UpvqT_PtaYr7KWrX3NVmCpd0EIzLN5MUslLIAJqZ-u7BnnSc0O8nS_mizJnPWkUnqul_5yDwsraetsMbYMDJM5He_9YlqIg28ejw6r8X9l0SEU-QqBe1yrcbchfw6_se7FIwe2aSk3Cd8KSwa66HPZF08ZeUk7FuUnfeptH3x4ejIG_g8taLlo,&b64e=1&sign=9b877a412ba4f6d4cca713465193fe8a&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=74832',
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 744,
                        height: 744,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/362398/market_8Z-xXYZnPnt5y8-6CVy39Q/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/362398/market_8Z-xXYZnPnt5y8-6CVy39Q/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFNpj2jvJaEeRgJzTcGbfRBfjxoWpHVBHh_wgaEJAeh-EZX1rShZiyP_oNJlH8AGv4xnImuweW58dFRnGe2Gp94P42kHQRnbYEgKdhYdiL4rCc3x6MsN6NTZjJLYBWWzEE-U0zfdnVkR1YjUiXVEkLKW7OcRvEKTDr0hHpqG6znoDcuXULBhUrCg_lCRmn0uO_bcKQfFiskf71ofTIk7MPZm8UQSNQrfKTNcmpQ2KqBjxyfi-8UfcBRdBNp_ChNdBKaT47Ih_oE9s5iCsyVrWD-ztWQcp-OD8DBy2ZQg-wwSZOHrxmfYENk',
                wareMd5: 'm3B0lYbYsIgP-F5nCMey1g',
                name: 'Смартфон Apple iPhone X 256GB Серебристый',
                description: 'смартфон, iOS 11; экран 5.8", разрешение 2436x1125; двойная камера 12/12 МП, автофокус, F/1.8; память 256 Гб, без слота для карт памяти; 3G, 4G LTE, LTE-A, Wi-Fi, Bluetooth, NFC, GPS, ГЛОНАСС; вес 174 г, ШxВxТ 70.90x143.60x7.70 мм',
                price: {
                    value: '91990'
                },
                promocode: true,
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTXYo5iQpxJ6J5jGjMCuFNQQRCKCksb5AZHuUvZTMtEjayYzOGWQxEcicSasAQ3xiYuX85JY3_Z5QZirjpKqfclKZGc3dYe4oLnphDM2au1ph6uJ-ZwSedaRrHIxGUOm7yEwcu70F5Z9Y_4JK0dpMqkEhK23xnTbSIPuEY8cucktQ7ewRJsSHrIG_UuW6fQgR4mkzWj4Cevq9UbsXD0yxNQtWYV-2pfy2wvv8iS8bwgTqMasTNxk29DCDiiSNNVCRQvefThbhvaOTRJYy1Q2oEEOl0CHEz1-l0BGFkHuhoXNgl1BcV-YkaQRMOfS3wsqz8yQhMTJimJsGeh75yBNhAJRc3cov_cp-xZbI-8FCmALYu3f-V_gjias1Oy-5L-inJjVfybWN1MmRWpsLKxEC5BKs1bKf_ZpTy8HnTjxf5QhXhQ9I4mT_mnXixT6LinobHYmmJ8L82KltuZX3Q7OxUY4Jeo7dMbTvMAxp8q-ZJ4ldS0mYuw6gQg3ZDqCsszKo5Fc9YRzfpO1VdPCXZUpBQRIT46_Cu7bobSkWdRmziOIIT7b2XmWDlpIVHn6Ybsxa8kOppOPgKmsPwQpkHg7iBG35RRggGkWiG9d_S4eay4dRmZF4oD6g2rQqk7YVy3ictB47C2VQLQ-SnJvDhXCszC7FHkiOq5GYdPPYLLRhdF2FGs6fwe6JYd1cmKg6QqIEg7swoX5F5niAWcl4z5g6MOUW-naz3chE8Np6TlaA1FC4Aj8id7Lw25WDT5nG3WABDbGy3nhcL0OpBYpMNE9e8kT_IMKVVTIOS3DwetbocC2NTxqNGMAg3EF8kyhIsWLP0oaxu9d0aWzwWj5yVq28DVWOQMLW_PQhhDIUNKy5mOr?data=QVyKqSPyGQwNvdoowNEPjVpPboYa0ioUFVtKkPAY2mq21KJUlQS9zG9cYuf2iXOwjYXTqRTLJfjpvRXMaSYAL7w4a_6kGnswDOcgRx1dn7Yz-IH0nZRT_ldVl2kiZuA0Qe5DBp3_1dESNlirlA1Lm4XcydVZBbF5iKlXIv2iMasgg07VeUignZJyEtLa5CrodE08jeMLTSncl7Yb0gkJpc72DWA6GwblMv3csYWNy-Ydlw-NKRGYFXYcyYUGGDmOLpxZ-3NHuVo5KMt1OKWhP2DH_vyqT793rWUPN7sDQWG8Zg2LzgPm3oOFmTehKe0QhVNrTGb-Sy1-h4bQrfx7_5Pwlp1QEpohjvzp_jpJpyWeg4AzSZNHfIuwOUkkjjcD9j1g5YCXRyU3WdevCbdzz__3Yr5L6idjAiioLMagpU4ZcjO5ERIFVsxI7M5sh7gR5HYKVWMADF0,&b64e=1&sign=a3aa7bb48b5fdeb84604831fb12d4a0e&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iaATZ9tjWPvMWbh6ZMzgOcAy7fygbXHlk2HgSBGPqb2M0bwFJwaSZrBT7M1NCfLmu2opFFV9kMdmXAOZIhyXVwfupMH4TouKYEcqFSErda-rID4pNiCzrg8fYoUPm-OL8BiNs_yLGwGlco8yXlo6HyLq_K5BVsSEG0Z8oNpArhg70geJq_3bAseExZN-Kyq1qZVP2B-7_z-b-3N6ovAaMQaxtcttpBES6Xf5YiZj8klRvSPRh2B8X44ZDZmWTLdGTE5zFrcC53PtNZbLQ3ngKXs_rccy3oiEAgiFh1DnkxTUGiVwkG79Gkan3L3xVykOgdkwN0ZVH2iEKyjQbV-_fhEXJMqqM8c1-to1qenE8ttnt8ZZd0776M32NvWJKNfxkLS1tXy1GVPb7lbhs9mVm_olYz8YsPlwl8cgUG5g6AaoYtkWlb9xbqeAX-C-6rAod9pd8HRoQ2sRMszLJqET2ovwJwwL7WWDg5DpPQcGe1LJmc443HJD1PiWqw2znHgb4AC_yimxb-nX1lXNbvCENdDtJXg2Aioo9lfSGwNUksKrG3HtV-aLeXthgab3EAdY4gQAAOIyPrZqnqM-I0PlYuFgifGmuYVmMgxXj7ZwF17CiYDWVb6WDtFnhMn_42PWJjYcrtISORQFY3I7RFr9c4FxFZqp0OcEfS0-LGnMXyIQ7vbJ_SVAVdOXnT0qzRMOsl_74Ss6ZsMHB3PCmCXAyzsegzDHgxycID6xQZGm273bcktSqrxQZiZUqaYwbT2iIE8jrcN5CGNZDFB1IroE7ArO444y9Ke7HOsnypz9opW6LKpQJDCZYsY1eRAzG3yzodfbPDznJQV0z2P5nKt1vfr4qFoFdH3WdoRYFBYxaTUF?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2f-J_WgtEnz5P-9XSUDTliZ91RzMkyW3a_AQM33yEH2pVTNG3dFwQnp-tkfuKSeIPRia6xy2MP6sUUV86AgCgNagHEq8Jf9kzNGtjbyUn4tVC-A11s3TPz8,&b64e=1&sign=826136256ca8e42fd976ae0e6fcb5144&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 2770,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 90,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 35,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 39,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 204,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 2404,
                                percent: 87
                            }
                        ]
                    },
                    id: 74832,
                    name: 'Phonempire.ru',
                    domain: 'phonempire.ru',
                    registered: '2011-09-13',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Юных Ленинцев, дом 83, корпус 4, Офис 16, 109443',
                    opinionUrl: 'https://market.yandex.ru/shop/74832/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7 495 221-79-11',
                    sanitized: '+74952217911',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTXYo5iQpxJ6J5jGjMCuFNQQRCKCksb5AZHuUvZTMtEjayYzOGWQxEcicSasAQ3xiYuX85JY3_Z5QZirjpKqfclKZGc3dYe4oLnphDM2au1ph6uJ-ZwSedaRrHIxGUOm7yEwcu70F5Z9Y_4JK0dpMqkEhK23xnTbSIPuEY8cucktQ7ewRJsSHrIG_UuW6fQgRxwcJLDsl_Xoiwk1Ev4vCpGiLGUR4lcKDizBj4W3af1fgLkwkxDPSWCMlNI7QYHJbFaoVOw99PZ49nKKjB7TDDbIlboXl9rgg_wxivmtDuLMjnZx-6UmM8snjKsxp4MSfjTkE3ToByPQaqqHT-PdSgRbZg1wJVN3W9Ji3UVb2BAqbPFvXvFzMAsx294X0kdfiZNM6XVbiJz3ScfpeROpv8h1flwv580xVB86xd6-7XE-6f0h15rnMXfxGiRkyR1n2eii2doVZHnZVPm_f7kIIWRURtcuAJvfZvWut3HPH-Gqd0uepuPlIR1nN-nU3SHzwWXWt14ewepAzu5imBYLGwKcr6kovmh9ucJQOFR09fPs7efOWxMjYHFzlGaqWShtM7m5tE2KXHp8wQi0oEl9FWwndwzCR7Ugd1h3WlxqerAeqAKe_CDfWRbBboKJJswVqUyixPHpVqNksuGFMeCEFXm1g26670Rm_L0txo0M_bvwRII0iL3ohJExlBSXpjjhnNaxuimbaNb_leit4gZZ-mJRJWl-Hmx0zMIz0dIuBqPxobzm04RVPd2l74daxDaaSUUeLWXa9SXG4X1CYOq6f24buQoVZRO6UbzvUDqouAxt_0YJNXFrhDPwS-AUUMGJj0Xrek1bUOoZeo2eOR7B4b-NIcu05CgSb0Xz3yMCt5nu?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O84g3V9WzXhpMJsX5ODJMpiqzShBOst1ajAS765fIhBVwj4JpNGjUmDBg9jzQ0WTtXM7tj84JmQIY-Ss-5jGhZDAzVUStjxEel0D6cKMYqm6z5BmGhef6QF0Ea5SRvLNTvnByzKG569PxeuRh9qvllgM6XGNTaaUK5tyPR8OTV3gA,,&b64e=1&sign=0924b8ca86df97b2d5a9c3637a5a6662&keyno=1'
                },
                photo: {
                    width: 800,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_j8dSDt6hFZn4QuaIXWC2wQ/orig'
                },
                delivery: {
                    price: {
                        value: '390'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 390 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '390'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/m3B0lYbYsIgP-F5nCMey1g?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=DNJNqwc8zi3xD82uDf4P6yu6KtZLLTPgOgrpRE_QY4kqQo_ZRvZAIyMd84HxY-F6K390vgTrc95G82b1N88XJi6q5Xfwaw6sD93eMtAEnLIbzhmnrwidzx-fWScMClL9vYL7WwNwMF0%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgS9gajGlvgNdXQLTTvYEL7ZFoz_Pzc_qorUA23HLiasjEV64kkqO7RYpASxi8WvAvSUiBXSiI-Xx-Po2KtSanKsatvsk-T6rKShz_9JIOc_rer1SAEz4T4haOufZ8_bohxFmoggky_1o1-oVNIGm17YOvN-cIkVCqMuO4s2RJSwQhTkChkAofEi95gcD6_UGctpMR4JA4fic0sPJQ3x7VXaUZAoIanlncpK5NkxFONe8eZom7BY8HmYMqT842BIm2Fy7hfqXlHXvC8rF80XvbydF7zNSNcaN8XVd-tQYXdncUfPU-a5fuzot2OtOZOtdsvcAeSIZ1D68kGuzIiSeJgGxltOdrBpAnXSGzec8uix9XLd-CqP-wNfnSaxkZuQRrnoLNP7vKZ4RXTNETbwA2SJ-57f5TXYFs3r6g_IIPf_PX3XcIx0JqRBrN1Dj4Eu1wIrnP7mK7HUekEV45nD-KuYhzA3Vdm85z_qdcLpdzrrVXNgSdcPHc6I-iPMt8yakY_Ve2RdH6v_Jg9XBELsI_agMRsQEuEvuG_sfLP5sDU6HXbJKKchw0DUJ8XdQcIwHgyJERAgx0O1cZu87Hq50r3Y_c0eyK7OpQhDT_A-wmH3iVz-cwvitN5fn01CIWYFaydklbMS6HDqmnq2HBZveCALgJylq6qFKyIlsfRgSP1N9J8Xscuu9KI0dSP5o8A9qYlfkNMZKAvCo8fnnFZeJPuURrwYVWkN-YDMRjnD9icw0JCfC9qN_bgifbVfx3DZSChO02sDgxm7SF9CNIORWZ8-Pg7AMP5hhXpEjhPezrO3d0S0XcR2Hk_ByJyGPGPMaOKysVkb92awvQWWMtAABqNeQCD-V6Mb-BDUJIW8PPdo38O5vkCl0Tk3Lxufn1-idoUBrsYJ1Ae4z?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-5p3j1nIZpLT7k7UBurSVKWXonQi9eEOGAYrPZBxG4t0aQUrVvJDXiVVJLyjVJAHx-QxUGJ2AMoqQOj_2r5Rhuu1tBKUT05UCeSKB7a5OmERP4idyjtj1SSIWKnlKiicblOcarlGIq0tjRNCGodR5l01DL5osbS6ecvV5V4jpq1DmyEjwWE-3pKNQhKA4iPixjtb7bEIsTJ0JbvaUvsSYZ_Eit00mHNqNZGEcGDVW_WTUpuCSMtPBcA4tA9C5XIQjAVCTUMC_Vrf3_7lWMJwFebpdMoazv19Nvw7u55RMdKH19I6iMuCegqiAKusCHWwbmgAALMcrO6ChwMf7URrYiyZHoDxH7blK8,&b64e=1&sign=fd4defd8ce58f8b84877ef44bdabb4d7&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=74832',
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 800,
                        height: 800,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_j8dSDt6hFZn4QuaIXWC2wQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_j8dSDt6hFZn4QuaIXWC2wQ/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHS7ITIm-pb6kMjFMVC5y7ZX-CoSupKeH_9xkPNz69kXaRb0L1vUOZtghOc5A-D507_tHQr-B3Zqqmu6kAnXAbS8dqrRvH003kXNQwyjR2__kL_UREwcVENRDGmk1qG0Llm6EjynD0HSPMBFlapY7Z-w8grv-Z_E4hEyhUxkXf-kBsSvXr00NTgApEOG7gdojvcSBUKctnJzy_K91zWTPxFcxokLh8YCJTPdqsyDM4idv0yihRxIzZE5fVngyVldfhnoPYAhbbmA1jpkCn-OtEOnhPdwVhqPdFncR9cvhgUf4cP0hSNFCEq',
                wareMd5: 'PLCjg2ESyelxO_9xt2dydA',
                name: 'Apple iPhone X 256GB Space Gray (Серый космос)',
                description: '2G, 3G, 4G, Wi-Fi; ОС iOS; Камера 12 Mpix, AF; MP3, QZSS / Galileo / ГЛОНАСС / GPS; Повышенная защита корпуса; 21.0 ч.; Вес 174 г.',
                price: {
                    value: '89290'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mehRahswkml8QwZMTPvTF6be1ihcOCeOrc8WR6XcZQF1rKDS_1uDjupf6WhHF6r8H1eJkCX7j8DwSa7nQiQtxWaaY6EWzyNyVs4st-C7dmfqYbSZWPGLx85z8VGjeb3oHNJFrE3Th9oHJKa0tNPhKzVbGfJm_Ta7TOpKQiQx6iR-DxJACk7zB0843OF0x6EHTB3dL2c5rRej0AP_CpNJMg7UBj-YMQ3S7xrqp3oQTTCU59LAoCroE37gTUveyPKvPtnpfZSEeFHScHLU_aTiCXqHr0GxIHYbuwc7WIrm0S3bIlO5sssGekSgpGLteZEVwcT81gzbqQA8M5on3okog6R2JsoRKaor5UzBxof1QVLy0OVB2dfB78f8qPWjUjy2e7kwNF9NJ6CIAo1sySdL5qSbOCoDWuFrkfDVFJumXqo0BOzVSjdxkk--2XSbVE5-vokiGoVMqGZ6i4DslwsNHjI_1FD9eOh7S-0wyXisE9083stVnLQSO5X7szQI6ly442Sh-Zxv6ayyCxeHjMjs0Su7gSTUf1gJj3Txdn7_jw3M4uTc6-tV-xOeof_R1XBFdlqM1YcdaN2Ui-SVPQCx8fDe1ZqNDC-CGAyIXLTRU5t8JnWcULQ9k08PsM5nNnboZp7RkNra7l0b4u0w6Phgnegaq7TAUSOd6IRyPV7Hcq2BGX3cvHXrauhP0Ba3dvFlQrvnjlWvIMFnPi1jfU6XXaUvs47QtQef5GAI13BbjPWP8NF8BRO5b_4HVzX6QEhODar5PRgxjPS5IBSUZxi3Qtgcdf32iv-Naw1Mq_dYH4zrEQ59tgN3HU1wwtipKsM68tj5SL6DUn-55uKtRa50_PFpzPH9DQD_JQC1FoT9Zl6Z?data=QVyKqSPyGQwNvdoowNEPjfhSuoOT0P5-RIj9xcemuM5C6pTOcmaqXHkRBNKtw26eVts4jJrFNuVzAbQrlmLunxttGyrW5wDEMOTueypm5rHbChWNVezio3mG6Zzg4swZSa8Sa687e0cO8gvZWxHMFt2u18FYd0d5doj6mIUXllXwwH-xZDeB6OA9YAPhiu9AqhQsXjZw2Y4pEq75A7df4OInECx7_E89POkK4rSq_OQR6axAGK-FC8RQGQ_RVTE5s_9VrHXeGs3dgSqMp0JTIwqu_u7HM6qkW9C9aeRKE_O1kgNgl7c9YTGc_hNQ0LFHxzQehBvBLsm8jlm1HVShLM1VtmmtJpsgU0mwFimmJ6x2Es31zCGpgJW-qwxqMQYI46hiQPz7rCQcqia9xBeHODRHzYN4X7PQbhoo2eNhTpLikq-ewrdKUoRsESJVvNJhj30AyWenG7J5XpBz4DcMEU_CJicMHUMc&b64e=1&sign=a8fee60ed7afd2e55af520d5fcd8ac80&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iQKg9ijkV3IR4dMvzpGgKMyTqIIo6gwsm5HX2hFcDzx0cu-zoWV-1csjmuqpttuI0he5b8BfH_8YwzXKujms2ulSz4jDZRJL_hj2mI8-sFwhIoc3wUdjIPrSBJGi27XS-RocyKHqVcBz9xVHS6Ji1CBKolbNSMje9ycLTVA0glzMIZ-WBPc5LKcBqhko6JSaOeNn00-i5SQIwDLuSIM7Pvw9b-YZVaXf8UvHiXpXQUs2AFRG0BLCEtpelxD3BFTPZbGsJaMyvG8opWgz94clZKKfWKnrbiPk_AYMG3WmFeRvdr9nPzyBMZ6S2X_GlY4ykNRmZ98CzVLbabK8zOSfnU7waG9VevJY5bDGzuoOimp9wWF2CJ7rWeldm6P-w0ex2udPx5soTMmDuiXTf806EqcLNRnondAuuHNsAXADLGzVZpXzdX-U3NgGsM0waU_ush_cisOXJ7gLDIkKjSIOKMsMhQaxmfWyPE5mbzdbFvmJ-YdM79W-NmlgUy9bq8W5Hm-mB4DdIrWexLkB1vlYYNmm4mdWXBNd8ozK02h_XYVyH7_8mIq8pJqGp41YS2qTmyrq8WrjokXdSySc0-nddJqsebmrmsfJjsDHXsvqOl-KmnfElQ1UoPN5iaHHmt-72kLHGhBYa5c13pFONe6SqV75iP2pcR1RU4ycaxe86XmGPJ2EsaC5z1LBBFWvdrM4JE6WLXi6nABowy3MOt9PTfe5QO3eo3-0dXB8ry4OJEyJfqT7lYhHpxkA36k8oiVqcpx_YEgdLGmAC2YlRizY6WLbBvgY_x7ZXpYLOnGKcU9qWmrLy690PVs4mgqDNthHj2UDJwkGvdJTgdxUMzKcKcc3JHVvikAkT88MiRuXVtoV?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2Q7CtyIBLwa8J8scPHUSSAvna32Q06P78uqC-ozuyV64fyRIWnK886xgaaLsG3E22HntpuO6Q_juqT3k4nfknIismOkpuE1Shd5TEGX2CqNBVX-sbEj77xs,&b64e=1&sign=b8608521d48baf77988895fa5994c4c1&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 4,
                        count: 4105,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 730,
                                percent: 18
                            },
                            {
                                value: 2,
                                count: 146,
                                percent: 4
                            },
                            {
                                value: 3,
                                count: 102,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 228,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 2899,
                                percent: 71
                            }
                        ]
                    },
                    id: 37758,
                    name: 'МегаФон',
                    domain: 'moscow.shop.megafon.ru',
                    registered: '2010-04-16',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Большая Полянка, дом 30, Пн-Пт с 9: 00 - 20: 00, Сб-Вскр с 10: 00 - 19: 00, 119180',
                    opinionUrl: 'https://market.yandex.ru/shop/37758/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '8 (800) 550-58-58',
                    sanitized: '88005505858',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mehRahswkml8QwZMTPvTF6be1ihcOCeOrc8WR6XcZQF1rKDS_1uDjupf6WhHF6r8H1eJkCX7j8DwSa7nQiQtxWaaY6EWzyNyVs4st-C7dmfqYbSZWPGLx85z8VGjeb3oHNJFrE3Th9oHJKa0tNPhKzVbGfJm_Ta7TOpKQiQx6iR-DxJACk7zB0843OF0x6EHTHKWVfv7GWsmMmORc5ceWsslMPwTB8NqiLGlI7j1ofeu_p84F7B1WvSrnaISf8pJ7OG8VWg0C-xTS7f8ahIe-cXpwjy7TFmSpCM6Lx1KvCbksIeH0pUiot1yr0FsaHbHeVaqahIo9b3VddCkotKteeTpp0_ZYSrkEiwMFvO9TZptMBFau_uTogJKjhXzdf1y_ZqGkr6kfoHBGxj5HRZ7efPjA9Kas0U02CGdRkUa7Unb7dlmKHIZ9uFokpXgEKup27qFLeSgG9bUKFdraPY3nS9blHEm1uCIXKnKXMJaJfU19ghV3zIRI0QedIM07IGuSlCQTbANLake_S9-WH9uXZy7FZTf4-ul8LU-baUMEaEe0dgCP1OIjrVAfkjpfxg2WqUZolJQbtbtyUPm2J9N3xmLnXos6lNBWP5CXEgV9IttRS5AXy-DoJUVAvqzJeB7JdLCX3ViWGQnt0npHNrG5ikHAIGDbS8FvF-l_FNXpLxIi4sJ7bwu-it_yjva2QYBprOoQhBI0SAteJ_blE_XSv6gha-XqN1UVyfW6QMT88EtMete-CMWzef5VX4ZyhhKD5-MokxA0mj2MS9ocBPmVJ_Sej19BKhConrmegqD5zbhiMQC-tqzNP3XHDw8aIzaZLuPjZKp4owBiNy_t4t1fxxcWh-YQZvUUQyvQKAm_LIq?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-3rkZ3Dc5mte-Yo4b1_ASj9IWD-1k12hWmMIT8q6BvdlgRCGj7FhA3P3OG_LrgL5bBNpgWGnweBNnNNouBeTgCUM3cVCU4DwIOY4xYZak0Lj7P_H6689GF6LqR_EQ8h75jvRmvmMQrTiZxUpfjBpsC1i6LCjSTjK9EgKiugXfUmw,,&b64e=1&sign=2966c63ce739da33e7f55e50e1a50bd6&keyno=1'
                },
                photo: {
                    width: 400,
                    height: 675,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_TU_cHtGUlloDMxeK1MkHVg/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 0,
                                orderBefore: 18
                            },
                            brief: 'сегодня при заказе до 18: 00 • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/PLCjg2ESyelxO_9xt2dydA?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HLGCjPFAv9teEp2tyEDs0bx1yIs1r9eZI0gsowtaXZIPVpXVww84VRmoNx6OjtlvC50yhjFGSRjQ3fHtewrBkKIaJxGFdMTGT8jIvk40vOfJKiSNIGaIGCH7LlSnoQJ0UA%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgZk7UQJqOMqoJpy-1U4WxD6S4YyX_yCyTt3eFYMsXGDxyQJZAazAGTx_Xh8bl4R4KwOl0lIktSmNgkQn2fh8g-IPIht0MGhKEgmUbIWFLYzRu9WBsJUE53XztGhyh6n9EsaS06YZQrSxyOMmiCUKHtFopOIqMKogfHZBDs5EjwaB9SfSgAheGtuD3CxOB85dg3FPcI3BGpXtE9S0LU_CMPFOHp1BPd-LKyqO4iKo_W_muX14BPGd9SpmchKCCu8SCA-Im0Ko4fl_k7qTCagCZ3yc7cUSOTx8EgDzZ9P2JMI_qXoYm10bAtdU7pOWf7HoZQRJ8IiyGeLUM4Y_JXJ-CCdPXpM6sISKEnJbBJfYRaTpTDg8KhMt8n_IGHLXV3iJZpCOFfHxvIB0Duj-vqC9hSk39xKLl1A-rUoBwQOPA-s0UNTJdNjly-f-FJN8DwU2F4Af-cCXQOiwkhCzl84qhfre9kNEtLd9UgYoOMryx8l3PZiKY1gGRB3fXU7w7RSOMyCZCrSbARZLx0HYR5Twk7hBPVNQFF-D4rcbp9sDrZMC2fTCMQRZONbl0aWlDHLvtAHTXjpyX3uQnO2Hm8gW12vCx6B-ILElEcsIh2Q-aJNeieTUZTj-itR-AMI4NZrnQ8Fi7U1NM95mSX8c-gZfGJPI6mJHo9tD57uLMJmWMSiD-YPg2mo4dcavCbEb8XVmwWQ6N67RNLuAO84RbJ_ew7YL560ePdNw_tAMTp9ygcRBeloLcpaZ9ExIMlRaU4qr6XJJD2el1jvEQP88X2egF6e3p5RTwPIT-TLxIwDoZKtck-Ddec4pCtZ0tSqVmzULoOUwAHuj-cRb9L8hmzxIWzR0BQfha4koZgEoa4zE5Z_vyolfx_6LSv7EEJU6dtHDpjFqRLUn8g51?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6kGVNJODIp8-hwXvnA6OXM_ZBh7AsVLZIXTwrWDZn0tvvaGUY_39dW62o_ZvLQcA1xVcj6JaKDOfW_XOox0zPZ7Iyrw-1Pcd5MlOCZCczbXAUy0gJeH0taD_RhZMEQeUliI6K4PGaS16Zsa3lmTHhJE9WXF2_yQJGOuqNODjG2Ik6FCtWJQbxD2hW07n0dsiI_lVytjyLjRjzEhM62pLSWi4hMOC2mx-q-LYUSJZQrUjtqByk8pkBx2dzUAebhil_m8r9eimjAakhtPbSqL8DzbV64l8oS2G_TsYNnMn2mbu22R5Q6mq2bAG810Y1B8Dv4ZEgVNAq-14WG4uZ4bsdMp-K5NY3IpcM,&b64e=1&sign=179754e9de256ee02b2563900c26beb9&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=37758',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 400,
                        height: 675,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_TU_cHtGUlloDMxeK1MkHVg/orig'
                    },
                    {
                        width: 900,
                        height: 675,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_tRwnu1UQL8uphaAB1-JgsA/orig'
                    },
                    {
                        width: 400,
                        height: 675,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_LZSzNOl0vlyAwB4Ex-V9Rg/orig'
                    },
                    {
                        width: 900,
                        height: 675,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/205271/market_5bZhvuLv19BQudEnrUaObw/orig'
                    },
                    {
                        width: 900,
                        height: 675,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_Q0bJKpf_pbXAp1HUX3I3oQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 148,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_TU_cHtGUlloDMxeK1MkHVg/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_tRwnu1UQL8uphaAB1-JgsA/190x250'
                    },
                    {
                        width: 148,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_LZSzNOl0vlyAwB4Ex-V9Rg/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/205271/market_5bZhvuLv19BQudEnrUaObw/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_Q0bJKpf_pbXAp1HUX3I3oQ/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZH8u0VSgcOzD1YuQQRO6hRg3Zpni_JCPRmtdBh6Z5jg9WDN9AsaJlxEmPK_tVibDmcuKqBnh0gAVF0UhWnu7Zng6bJuOSCZuWRJt6-y6NthUt9JwgRvB_h9uMDDyE4UDK0jxHtslEPI18T2hUXv_11tsNvzupwBla_P95IVY6oy-ic-XrxhbRs2oyyY65bo9VLUIbaKjkiH2WHjSMgFOzBYB802McmiRUHRuax7txW2b_EnqCoTr_9_sjtnwXFaUSYOKc82ZmN4_q1qs228DMDxNfeiSmGNuCz0JQqOaMZkSa2e-g0zr7C7',
                wareMd5: 'nlD47H4r3xtomCCn6IHv4A',
                name: 'Apple iPhone X 256GB Silver (Серебристый)',
                description: '2G, 3G, 4G, Wi-Fi; ОС iOS; Камера 12 Mpix, AF; MP3, QZSS / Galileo / ГЛОНАСС / GPS; Повышенная защита корпуса; 21.0 ч.; Вес 174 г.',
                price: {
                    value: '89290'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQF0Ps_NdkBhKIuAXl3xfuurxEZnB77Nm8mO2hb0st9a4Rl7iB9X_1XFv-ST0bl2HXUY76sLTJ93CjCW6VGhZEh_6CW9UZgR0YVu1b5QGCOF_e9X9N4ttYrQ6S3QMCj2Tgu-w5rih-jiOWZsSv9xJe7L2EEf7s5JqKw-88JCf64kTvC0nZL0qEHBRGhqhJHFk2V5iubNTBfZx8mYKaIkS09sU1vi1iiFSatH9G0tGnTB8EqfA0GM211dAb2fxILgGtdBOm82XtldlcWwkyp4F0CHd3GY6XCRwIFGdZw4GEptbjE2AioeHRln59l0lH-HNw3lKT9asoAFF_VscWMYYgJaOygoNbrCVHgwtmuTefVMeEsHFVjsOLdOruhABMHZlx3Fq5zrMn8Rm6VwcOHyVyJR_fCSKTb4mR6Q6xhuYFoVN6IbGQ4AEH04cOvcJ0B9nGqQ0Xyh8Sus2GZhYmdHZPH5JCDmYtBXv21sdHhOPX1pusPHx9Ov0uHITgZQ4HgraAkvrEji52Yb7Gu_LZx3R4Np_yBzI9cCe4zT3EbZ17Jf_w0AlyPR_RwsZfGCGikRlgY91kgvNUoUqkTzsG2kSaxxmSq5_dnEaPj936qLDuNWSCVa8LH3IGNQUmHjb5GOQ7fs1wgLKCW-fOmeWhnikIswyXA4QEHmneGzlNRPdllosPGJkxE0IiQpd3wCvShIeZF7r0X5Q5ObGpu1TI1KB3TLV__ue6q27VUtA2142-5lLkXua4iva4qzKK40pykM6DuEMbBz6aeLjtzC_Ok5qnS27UCEpTHGzkPyt2zVgWtqYHXnw1h8N5XJ4ON9voBSj7wP7HeuOwUXhO6y8dQ9qSHcLVNZietQ9F0UFqILRdH2?data=QVyKqSPyGQwNvdoowNEPjfhSuoOT0P5-RIj9xcemuM5C6pTOcmaqXHkRBNKtw26eVts4jJrFNuXvpexMntciqFMmo3eAOhVOu1VBmm8SmngKTM-WiLE6jPwAOwK-vNOnYSw0DAOToF4GZo0MlL3ALYePMhaAaOzBjFfSEHUPG-OM7cNwnBPPZnE0gsBhpfzVrkgn31c-eYzjEzI8nP-wpJaximBVaRArE_wwmezW2EotGG48uAnZIQfLmnUt8uCN0w9OBgRYh_TRAQAW7x3i2-9W2yJoSEVF6jHhaj47N0Wm2n33NqPQccNTjrqSjzo9V67GfofuzRR_bUMlt8rXwXW_ZJfd1B7RFQCTuK3XQTMBp6LA1QOdFYyjrI4OxBUq3LF3J5ioHaVOBwCDes6f84DPh7jex_hxVgj3AU1CnZIULMfJSleg6JU5QRwwHFwftCQ-Usry-6Z9r6uvrEknxzmfrn2LeFcG&b64e=1&sign=5a5e2ab6dc3c4f80a62893b116b897f3&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRvVL8634LDQ-Yov6qA-AEwOLZqN2t2V2vlLEsykkZFOj569uVZ_cYJFEOMUm8D8Q28BfviKqyQka_gqNmIjstcjXi927FEFdCT-0VaFUF5dyrMSzgeLD3mJBYye48Iz7hbJwOT_g1I60yuTzkvqK6KPOxUS_gF9537LxyWcJh0HW4Q-NXcbIrkr-81xHoTeIRAjrEJDb6KoI_X2kjO7ooYzUzJ1w9dHAd96-W3ZwNvvmrLjynx4TOQi8DMd8Ti8GVzwgQ2qvM-KNs31Tu0EjrjaDhpD28gwYkTYpxQEYE5_yRa8X8zAzCkmlRWb0DUXw-IUm_MjCLziv2MJUO4OH8OQexxl0nUvH3k9hwVt0G1F_pUdplG14XqTGzRC4mo_o8kEFA0XehZDBmgYre6SbEZlJ29itRKrjr0UWZP1NT5C-FHgD3FlzBZ3QTkw9ImBYqP38dfggLn__3JTcrp6YWSf26XBA_THAlUn2u4gKoDuCf3AwSdR0HNLYr9ZAAtPZFvZx3oGBA3GGmdFmhHHqMrOL7nX-BMveRoRCPs5L7sFNNVdf3M7Oi9gQCnmtwjyeeg_rTptPoHNHtn44ieQl57X0vkXwiB75Q1hB642sVbQkobCV1b4N80arjLyeBTBPb4MFU-2cBtqlVpnGuZE8Ph3GeEG6j2gVuvufCeRuEHXvMVPmprP0aS-HoQRJAh_JfN51tnNpL7PSXUXmU_b7et4EKz_gvI_GAvfnW8tJ8izATsMmDsLWyxYVDXs2hX6F05OsSg3cEIQjFVCQeLbq1ftY1Tw0xdn3E3xGj6ZXJzJyTw17V3N6vFYqSYhXvc_VT9c2zLuhWQGTb5zQv49vYNOiP-OwFmhd1oLWUmSP9aw7kNi5qD_li_B?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2Q7CtyIBLwa8J8scPHUSSAvna32Q06P78uqC-ozuyV64fyRIWnK886x3ECQ-0f4659TQluRtRvrIplbXmvpoaIJnaxT_SFj9kQozt6NmD3I5eOGnmXf9tPI,&b64e=1&sign=babd0d24ac6f764e8eb611fcf5d15b7b&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 4,
                        count: 4105,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 730,
                                percent: 18
                            },
                            {
                                value: 2,
                                count: 146,
                                percent: 4
                            },
                            {
                                value: 3,
                                count: 102,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 228,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 2899,
                                percent: 71
                            }
                        ]
                    },
                    id: 37758,
                    name: 'МегаФон',
                    domain: 'moscow.shop.megafon.ru',
                    registered: '2010-04-16',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Большая Полянка, дом 30, Пн-Пт с 9: 00 - 20: 00, Сб-Вскр с 10: 00 - 19: 00, 119180',
                    opinionUrl: 'https://market.yandex.ru/shop/37758/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '8 (800) 550-58-58',
                    sanitized: '88005505858',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQF0Ps_NdkBhKIuAXl3xfuurxEZnB77Nm8mO2hb0st9a4Rl7iB9X_1XFv-ST0bl2HXUY76sLTJ93CjCW6VGhZEh_6CW9UZgR0YVu1b5QGCOF_e9X9N4ttYrQ6S3QMCj2Tgu-w5rih-jiOWZsSv9xJe7L2EEf7s5JqKw-88JCf64kTvC0nZL0qEHBLeoaXAZQrg4sjc5GLUsv6F2p2UslEZVCbRArZ9dOqQ_in5XYWhwhif0ppoYW_tQGtR_zVhFrF_D_J4KQmNkVPmplzxgQf61TXiQtHuOS8qsSL6pPWgpwR7uK4egw3o_lCltgsFWaViccsdvrNYr95qIO-NjcbzyPc0QZN2uVUeGU2AXrIX_Tv5YzykDgISZN6jIyGtJ6VJKamZC2dQV1mp6tS_ctuQTWmlPpUbP4pFxf41HxYJ3kNIBmI_n4cKezHBaMV7U2g_E5n3RHt3YdR1x2n56oXc0t00JrAQ8CanqgTE4a0wUmrpPjzlgnEdingr_SIozV5IdmR9B6BIvSdeLV9Wqju00yWDkbxajfyWLk_uym6in1bs8wKXepPcDLH122IBT2bCQowY0LfD8HYKGB_dYMXN1Q34irRRy-XZwo6fOowlOGafH_ywxs7P_p_jNTInw-oUF3nh994lNSZlhx5Tx7ar-6jeUNZbmjMpJTeU4yZoq3-c-4aebNYacdObb2sHjA9bDvVDk8mYaPQ4F_f7x7iZ2ycc-749BhtMbG31OzBsjA0jSmyalxBJLEa_gZnfWwIEcESyOc_oMow0lNv43CUXFwdw2OgQlpJhtNQs2plijuvboejDWrqv6aRkKgwolMl8M8OuqLHJzMZsXFHRwexumGSSJe0R6An7XtuDeRlusw?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-ikKaBuD1IinDJ8YVqji6JviBUP2R6r1bk3Wod5CTlfsjSMIM-TOBaVzVWu3DjFl3iDyseXAoZ82s-knMLrRcrwDGoC3pOWk7LONZOOAd6pOM6NTdsCw_Ggo_sd7HVPWpI3JyVuH2TIJnbEV0EU-shHFbCrJ2vuAYkVmsd6dXFvA,,&b64e=1&sign=7f3a7e3060a0975d04b1e605a535d21e&keyno=1'
                },
                photo: {
                    width: 400,
                    height: 675,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/248830/market_96gwsN0vC0tIqVfxZEwaow/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 0,
                                orderBefore: 18
                            },
                            brief: 'сегодня при заказе до 18: 00 • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/nlD47H4r3xtomCCn6IHv4A?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HLO3hlXXzCUlgYBBmfVgBssdqw2qwSRBNZ8Pz9fpzFuBwgJb0HnMBB3BQtoR75kWt-MHW-J0mOLc_QLW8I2br6r6gbMblE4fIWCj5iyjBCcUqixJUoSo5O9kWTxS1xS0DU%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgd4wY1WdDXy-AwV2k_sHmfjDp12yba6bf8ysapCnvgiS3Y_jBz_U7RqIA7hmxis-Tvt-GmuKCUMnJAegNaFovzV6D9M666MfoTxnleMVVg931isRNfc7P4JzSxpKhG5touLdxgoX2GXbZneFyWgYAz3Z2CN_fX7sGp8GBBFjohOYZX3HbOR6oM20ES-tVm2LKGr5K5qgWCh-Jh7qt1qe-19aEjHr2dh5505AfL65m4PGGXVSTWuKdJXQKLBh4wpkIIfmnhCorlcembHCkr-_VVoNmZZqa9G9mGsXhVnfzyAsxx3ADVPGmcWEv9lYxyOtMb2TLMRbn8yHJwVTTPEA4RUUSgdooacbGr0HR07gjD7zJb5GPPpEvvBICs3BDguy0StNKNdo079geBmty-sUT-8sGBVpC43T6vNSgQuluJ6udLz7_ZUE5-V6wORjh8ufQXb_GRQWqUj5aNht7PA9ff_uU4rS-go4kargClXhZ2-LQMTa5Tok0k6kAdGHpy0LZcqvAH5cLTVH0kYQ-bOaHEF4UAjh_H3V194X7GNLe9HDEc8gIyjA-Nm3MXnK5hBik1wFuTHc7fJZoYfJ6MupwN1ailm6P_GeVmIUoMavVqzq8mUvNvMiSaidXnI0xdy49Nx1KYCHPx0WJeWyRDh_EqaGhqaOyGHWH9aYocHDE57neZbrJ3UHPlV7ny47ai_916ARJC_bzUurRP_DAKy1hzqdRAfP2vEapFYSdCvA8vBgTRJKKor640oUOu-s5io8hrNzFI6dHoJqQl9uspNavMWBEBhi-eDNeVztqA7XSEystb1mBduh-7AHHdQZIcZrrYGIZ2qiGVF-bVDdqk8HnhBAj7Z-NcOUO2lzphgvh-xpS0o3SxxnNFY-Jn6CvrdApumLP9-hfNw1?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-5tRCaJeNdJjnNctfBLvvuR_EUASCJhnJ4FYj6BLT-GHwE09u_JTaSzX8ho_z1OsySQifvO7d6HopCWSEWZN5LJqPmFnfdP-Q0hAUuC9qcbZwIY_jKEzLnku_U0XY5B20Z30xEEBShiczjoz_ylS_EVKuWTprcC_feqEjF_YpJB8iEiAMFFhjvtRC2mMcMLi7LoOF1SH4q9CLfZ8rydjz-sfvvU3go1bd6O-_em7c6sEOUWTGpF9KmS9m0yVinmZHaP8J_Vs5oqTGjto2T_AwOLRKnpa49u_gHKt0EEvjypJ_M7cVMN8L2PK2UPW8tIs_k2feJ_8t9qbVYieI0cn00RwvAQ1rdCvjA,&b64e=1&sign=fd29d3dd20a6c28c6761626a3d4249e1&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=37758',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 400,
                        height: 675,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/248830/market_96gwsN0vC0tIqVfxZEwaow/orig'
                    },
                    {
                        width: 900,
                        height: 675,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203634/market_JnELqNsNdYwoRxE-vNUkZw/orig'
                    },
                    {
                        width: 400,
                        height: 675,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_fivfkpDSj7d9Z90oaD_wYg/orig'
                    },
                    {
                        width: 900,
                        height: 675,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_eaIC6uI7BuuJvnABn0g_gA/orig'
                    },
                    {
                        width: 900,
                        height: 675,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175127/market_35zv5EVrdppV_3PRNM-t5w/orig'
                    },
                    {
                        width: 900,
                        height: 675,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_rPrr3pSAH1THjLleAv20Ow/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 148,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/248830/market_96gwsN0vC0tIqVfxZEwaow/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/203634/market_JnELqNsNdYwoRxE-vNUkZw/190x250'
                    },
                    {
                        width: 148,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_fivfkpDSj7d9Z90oaD_wYg/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_eaIC6uI7BuuJvnABn0g_gA/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175127/market_35zv5EVrdppV_3PRNM-t5w/190x250'
                    },
                    {
                        width: 190,
                        height: 142,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_rPrr3pSAH1THjLleAv20Ow/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEgj5XdRwrVTQsR1FWEAtwxHfjRiNpcQzFOJXKWYsF_fNQLDnfVBnNJcjAmdp66o0PuyzcKx1CYK1Kv-hOP-93dmHv3lW5S154QPyXRpV9-RHK_0porEoDeDJu_Yv3oUp38rs7OpdSojpwXuqULpMaOyAonbeiREJuXvB3gQaOp3OCL8Y0RCRugxuk9sA9xc0ZP11KXGn9XNn5Btq8r0lYpDxpiigrFD_sv0yLkYRflTY8MEA4ZEtCAY8P7UtmoqZ3Za6i_xUyoxWA9K_DDkWVgtNAbaIj9qm2pk50KhN9buVvDYABhhn-K',
                wareMd5: 'm-qG8tdZSo5swGIpbWIjjg',
                name: 'Мобильный телефон Apple iPhone X 256GB A1901 black',
                description: 'Стандартный набор, USB кабель 3 метра.',
                price: {
                    value: '79990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-4mW6Z3uIuuA807c3Q31KGW9ABDBzGI-O7uYmVWnQJN6OO8QMZp7MIh4cy4FH-4PtrnQUktT232rmi8Ca44SJqXPSkDgDAfMrB7Xin-IkT9pdwMPPoa7foHSvAu-wL3MUabONm2lVGaSFDovdsRMKpoftEqtFqyi758C7in1zJDIO1T3mauG2Oiq1Raq1Mp7nPJlGQvLF3aqcES0vpRSUWo35vBrurCvw23SleF9ddfqDWpIYxh_36rPTu-m6rfEWLxs0bDSngOHNbKAXYCYwaKeUlLq6BlOcEd6wi77dwgRnYeSfYLW_YBDAVDs3eRSCxKMjYPqEwqzfR7DY3JAGmplLtLtCBpO4d2yS7OLAgumjZEhtpKCd7oBayxq8z8pYVO3kCdlZyMkW6inmcuyRSU7iAFT82A4ReK9ix95WrFqIg-CZH2q7l8t87D697bxCilUh7MrVZ1Gc9eYPJCvq4aOz2HAPFJFi-z3KJW8BUVPGsYLnYFiq02ut202pRc6UQ26jn5rPyVE6j2zYSpVg7brNjurp3pgVlfInPES17l6M3IVQP3_fGeAn8FxwqHYgJsoRyXA3JeSItxNMTW2A2UM5chS3K0guRoeFyrelD3NgQzVUTjkEQqt-BmSuuqV5VvSZ84xrVFBD2BdHARGd160Avw318t7RBSwq1ZXSwTRF8e-4xXdGSp?data=QVyKqSPyGQwwaFPWqjjgNoGyA5V7skirTZyxRGDBkNjOk4UjGD9z6CHMTL7sx9x2HOils0CeY31sqX_fi417mpXYhROHuqVlK2iV487Q_bfJ-cfumSesuu-QKauvITjdHctCnNBsyxZF9kTIO4oe6YJApH-KV6zWq8WtorMniy2WURa1nhQjSMjzv69fLOfFys7nIMnRjl4CU4scFFxLDM2hUyh0Xwpse8oC_6T7tp0dY1uF3cl1dZ7wl55_Gi5GvRUFz7HcoXYjpDAKqF1GVH_ZZlbFePRjFY52HS0Z4vknIItVdQHQ1dl3hWeCyJb0tb77Yn0JY3RdGNyAdtHsjYyjtWqJsJWmPGFYXxk3MlUetRee1-O4GbhElRMkba9_IBNYJ-aA43hPD_xkmi-7NjWbe9s4RvbUKpTb8ld_ozEnuEfnifQGGg,,&b64e=1&sign=5d91d58fdce1ced89566e750e460d0a8&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCIuUwUF7cHox4IE8nmKF8jBMVcquLntQ_qAlEBPajtVXkj7v7gT3hPi5uKXkAvPwmB0hxKNnHVIBTyL__lXZbNCn7hM6HAe83Hv7FRKdZutPPj0Rp73mmORQtOkbZXyPUh-CYU2y4OVCmObxJ0_Q-K8p8dI0-Ie5KcVlo2EKXSf_08PImof4A8EqD4_pZbjnrXCsu5GRI6WBanKOJeNLwPTNf6MYjiGG7FvqCT90d-HH5NoB_qwQ2DF73KjSkLlhg1vwnNz09ovNhQTLFT9u6KrIGryen6YoAlgXEWaKQ66qhka243vVDejxPe4FCTLuP1E-Dk7xLcFmY7A9fm1hX5_0ViwjZxaZLNkHGX-rTmdgHirpVwQ7WSvbpq5TQCsAs5SCI_l-EIqRXSoF7zlf219nwM22-8zalZ_p3Kqkek0dHZ93Z0TrDVCUxjQR9ExnzcjtmHDbuJUVuz3EkqTlVWzlCrXevlJPtMchwFUk9y3z-CVYJ4Qp_AhU8GOO93qKEDxvnCLH592itaqSsPSkc4R0tukQHspwJ0lMrVYCvvEYGW0q9Z3CMaINXtFNwuQ5FpiZciZrRE-ALDobhgXR72IcSsIIazVgfyrCggLmTIzjXK3lNXKbq4V_m5ELz3QCl5UcKDA9EuACo_9jyJFXOMWcZVBPyBJG3gTt1bPEZHxNQnG7lsp7FiR1DA7o3gZtV98I9rEavkWkNMfiGSyjjMDSdmUFqVhQQQW6S-J3GpJqiADN_lnxZbA8_P-MGU6lNCFPQD9SjXj2jW1ulzBCJe572Uu6CMSqGof8eUptrUAw6-tl1DjVOIGs_GNPEvhbK6BnHSIJV8DenaLSIbPqdWsxMS65fBGXqc091zMiJC3c?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2ZZc-WxqCUj7rYEKMiZ8u2-NjMuMe-2WgxWWMbyb4YGslt6ZrdQs_RYM4P2i5o-kkYQ0eF33SkTh60EYH2K_7xJsjHJcISQ3NrvD5BI1F55GQiKGtqtKDkY,&b64e=1&sign=8e9bb69a5f0d9d269fab053fbb21458a&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 10876,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 66,
                                percent: 1
                            },
                            {
                                value: 2,
                                count: 34,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 54,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 633,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 10101,
                                percent: 93
                            }
                        ]
                    },
                    id: 58825,
                    name: 'Video-shoper.ru',
                    domain: 'video-shoper.ru',
                    registered: '2011-03-18',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Пятницкое шоссе, дом 18, офис 172, 1 этаж, линия Г, 125464',
                    opinionUrl: 'https://market.yandex.ru/shop/58825/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 648 68 08',
                    sanitized: '+74956486808',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-63mfMKlMSfoJkNveENq9-CQ2bcfwRzIzDzay-pvwGRTGT2Qx-LcjKRZCiDJRjhVZi3_FcRa9mZIb5uCUXLtlyuoaXRQ0BV_i8Gr5PI_I75f1vc9uCIxmTBSbn9h_ZzJpY0tnN7RZfdyI0AktdlZJXlz3VSgEDMxOESOJIaoErspK8zfBFp5ukF8pPJ5OsC8ZAhANHxUoTDKBev-iXYpj3s_1TI7yFouEO8Ji8sYB8pwXiNAt2rO2jNNCMHo6ahSe6W7hIC-4JagbieHW_DJRKnw-9rzvUxTYUUQhznb1mJFC226v-P9p1X40vRj5awoHI3YvL1B0_zOaCgNSypDwnmIoibTecSvqr7KU6yCsGlW4hgBDVppfcgNUcUgmy4nexiW9EA4pn4mbj7dMEvpWx9WOLVIPkonOaTwfO0MojpgSQ8WRgNanyEUbOIE9b0xzdwag6znrbt6zEsmV7B2JuKYwi0GnNbW4uHbpZ84Wegdhm9c0YT3sXv1mnprfqs7Mlvjf3wGtcCpdA5eTy3vNcwwv77Zqau6v5zrWGryCO1bxjIywcq9KxLo8jbwP14ZjTMGYI8AnbvJHka1a4AzWHP_4d_wbZOX8hEYjjDIodBS4kohPIDoKM_xvcDP62cki-G9ESNNpzzho55KJs4LqYdfagJs3FO_Wkvgc1VldIc3PW7yjSS1-zF?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8Y9n-LzxtoJ0ZuuItr-DRnGOxxZWneFI8SWfAgppRNZVltuAL76fslLucWYfV2Og0fUvB8j9ItA0hCtmm7wU8lOOxDch-BwxC8YBj1pahbgkjc6q6SdiCbU_LE7V4v10KsIxFJMVgE4D_2pkyLXmWkJwr2vyNYFQxVZV_8nqCuHA,,&b64e=1&sign=dec7e828391f1a7307c867f558b5f816&keyno=1'
                },
                photo: {
                    width: 175,
                    height: 350,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_ULfbbkMTv41Lu1DJf9IxQg/orig'
                },
                delivery: {
                    price: {
                        value: '400'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 400 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 0,
                                orderBefore: 24
                            },
                            brief: 'сегодня • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '400'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/m-qG8tdZSo5swGIpbWIjjg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HLwgBm2NTTK6wuJ_WRZfFIPMfP0RNDpqIWl9UjxJdFzWxY4a-3zWACKIYYglx7MuLxh0uWGVrc2JxY70jUYeptpXYwzsfHRw0_gjnnXcTVFygohJ60BBCXivY2IQnXuj0E%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgX5HU0qyhkBPiM1JEjStNXVwIskeDcy7WEE60AgWheBm5dYo1p3jzwwo4uP9EI3v30wFDv5GB2M3lV7rNR8N-XPV7gsM1SuYeZceUzxUMwYx4vW-Mw2Z25DxfAjDMRELjlNYmlXu55V4S42cDu8jbjRYn227tCgbq9_txSQCS4YmcUKPFEB6UM87JXZfm5Ksj9io6Jnyg3tvmSh3u-UQxoWlI8E1vRg3cswnXBieXvhDSeSdJumvVNLLlY7DpEPdRZJDVhEJGxtRb1BUkI0YuBmPgEG88oz-t5QDj4tJQJDEhDVUbF5RDqLd5oOEq6W5JZAEHBeWbrg0IWVc5uEry8m5_821Urdb_SFyDhuvpp00Pp2uk9NACDjvzliQ9DrtwJ6RWcwlB5J3aj_6CG38AjzyqtJge7eyBnKEadvUxCA8-MFwfVtIe_3yDssphu3PnQvlIZxDqUeVmo0ADiZX9EgC15x-3PnO5G99IXAbX3SDXlbuJJr2NEMS2OxwoMPHcG8KHxmDjAzlbuJUuH1FHjHSdMM2qt5Nyta21PZVz1L6IQ06BgKRy4sCqVGL1wBz6dkm54MntoVZRXBeSxST5dmcXKHvuqQbVufVdkya2jwkK8LqVrt8MHdhKG7ojhb-InU0HShaBOLFpELPbNLAnzR23laTDyWxjfeSi0Oz5d-kQxMloMOPlvciff4ZRKGGV1PIURR4Lp9msvXF6boOhfK1MrxGuejBm7zEQ-Ef9oFZ75vv6UM3bN7UC4BUhTTdxVPwNbOjs9LJ4tnI2vp-Om1MTCWVN6hFUCybFxG8b3KhMpx27A7dhhejMU1DBenvL8eOfFGT4azuaJfzyRTzikVjvn5XIuhbk7kN93TKbmxM4waRtzEpW-u9je4TSfX13HxkEB79uIcF?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-57eCy49sOCSVA3bgpdKV4HGFxUDM0WeksGQW_QJvo-cTM329FYzLPe-AhTLamGdg150BMAKPq05BfpXH58wm_oXYjh_LsdHtkYQeq-KMkI1YFnj2Xlqb-YnUXtgqy8LH_lM4bvdjIcSnrc2qqKTw8ZcewAVlry16JiHII6kRhbeQoDZ3l3e4MCzbV2Qhhd8TAj2Ad5EyRBpLPFwLum8a5ruAYYqnvFm-pGeXcSYQmVz8NJOynI_oduxKZj8zwpN2YrbVd3RIExNSbOXg11o3YNlUUv-3rF3yboVEa1M56OmciZo7cn71Vikpn8m9vGP3oOPKDY92YquyQP8UTNiMaQ4Qs8zbbMrwE,&b64e=1&sign=626e035392a4d003d6d65557e9566090&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=58825',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 175,
                        height: 350,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_ULfbbkMTv41Lu1DJf9IxQg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 125,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_ULfbbkMTv41Lu1DJf9IxQg/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFr9LFE4KRJNus34IfG6N2NbnCT9KR1NVF_uVnXhwStBgfqBx-IGG0tOI8bcawaB2vMj05YHK1iXpzB50PftBx2E-tC8lR1fQp7stre_WyNfSDWgAcA-_GvZJyGhwFdtitaqwHL-jzchF097RYN4cWqjQYXG3BycKhYjGAawismBvTgcQ0O3pnNa_Z0g8pdtOCPqYChbrfJQZH3L1u1oeb_eBPvBiQy6sPIKgQXUl_5pY9w34DN9FmWdTwA1AnVS-Mk76nHWEtF2LZeB2zDWNOi1spDOfWPYQmXP1okhK7c4cZQGmncaukd',
                wareMd5: 'b8l0pKqxcck6o8ldO2L5SA',
                name: 'Apple iPhone X 256GB Space Grey (A1902)',
                description: 'Apple iPhone X – юбилейная модель бренда с рядом уникальных особенностей. Это 5,8-дюймовый OLED экран, обрамленный минимальными рамками. Это распознавание лиц, отсутствие кнопки Home, анимодзи. Процессор A11 Bionic, усиленный нейронной системой и чипом движения M11, легко решает все пользовательские задачи. Ресурсоемкие 3D-игры, графический интерфейс, приложения — все «летает». Автономность гаджета высока. Заряда аккумулятора хватает с утра до вечера при типичной нагрузке.',
                price: {
                    value: '80990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mX9NfvwWCqa8OXwlMIAmpgigO_BF6j3uVH1Sghc7L8_olPnEblLo7UhliYsXOED_bDQ2A5y1OjooEtIppRf_pAR3AHC6Hylgg1V4eZBwqNP41XukWg7_AzCuCo2dn_9UslC4o7Z89jZS20UX9EUZXtiCr-A7ZxJkFC7ZhIuK0Xp4TJYr46lECaZ74cCqgZAthC4KzfvPrGl-06krnkwQ5WgUBvFjVd_4srIUG_ZoZYEJjzljAV96j1xtyIlrmbS_RIbpjr1LbVP4PCSpPlhjYtSXH0Cmq6ta4CcMaolP5-o05CGs9l42Zz6BH-EHiJAja4N7aM1OCqXj_opmhqnBlZJu8E0mbcXeRALNNLcZlath4Uy2elSdCgsq2nUEJrTs0nJ15D_1TYIO3bzBWnTUNiSKOwi25jvdT8GST_ndzLKX-Fz1z55H82UrduZz5wVgfYMsWyG4dgQt5LaT02yNwf4GuPk1_xhpYfF72KRwgk7SfHVKrs5oiOspU57dzEE2wiTslMAmbjBx2nR30K4unOnINXBFCy2ITeIsg3tNqlED1IaYgkZHsIGuLmMhgqxD066phXC2uJvf4S-uEGB2RyrbifVfz0QPehshOyZIcRWBOW61IihsxztHaSXd9fKDE0lPBe04QWF9dTxkHvL4DoapGbsDl2e6HP9oPRwTCBVLc5xgpfFXfX0ir7PkmHx2br6qdWA1SxuQf2JwE7ahGERFQCT8S6mqZ330Q-Ijxzr6GZ01rGVA8yKGmx9_CcmFHoCmkCD3WZhJdv8GL_WpGLLWYOmosAQd6psdNRUYA5eeRy-uNS_AfBLKz8cDmlJjlawTsBYu1uCdGIWLeI4mfgxIa9_iOHKLKkoXAQS-Ax94?data=QVyKqSPyGQwwaFPWqjjgNq5ob6Jq70TJiGoeif5hpcbP8YccfY02ogWNdXvVXQnUtSG2m_TC7d2bPi2wXHRJMk1-9eyQ9C3OBhHHbBmX_iBeICcWLUBqv09Ehbgxn0S1uNldSYWEpu_aCmQf8Nc1MPUMGJtJutk2X2SklPMpaGRuXOPdvIRLIzopwQSJEF8ipVSgTCCRbscXRItiGzQ4eenfRXIOdalAKL6qoA3f6ExfEYMUVkXCFtLamqUy078iUlE7u9TRrJjbVYHPg_wvrEYCr-aAUvJMSFT0aAmEttznzyvdUQMfX-lQa3JcW3Pcf0tQATzDgcqwBBil1BFzTAGilFFTsOvj2hdeHuI_7BMVqps_98Hew3QFPOYFQ5alClkFXz3i8MX8H5tW7BNtpvkQBBZ3KLOUXhnWj4FAxs9PabSJFBDyhD2KuCRuYxZGc8VXIsCjkNRQOR2zrT0oT5G31Ki6Rxeg3MGAQ8_LYy4l1MQ5GGyBlYBQQdkFr4rk1MKx4S_di4g,&b64e=1&sign=c6c2658c0df9d513fd9b20c9e5b52eec&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iTFtwtGbqyGB_-nHtp7ICYyHAIkWrAXY3sxPKa-Bsw9fVRg6eulpLqHrsQA32mq2vsKcP6J-Xat2ElCbZBNWZah4DXYCk5VvEXeCIeS-j6tjMR1o8U9TKcuF55eQqV7XeR6fLNSr1dtnfL_dNbGp3roV02PLXxfW0ePr_KC5YKzkx2PFnATKlgAZBm8Wefw434kK3D2hpp2L957DnVMA6ei1h9pC6BcYtLrq1dVW8LUl-CaB_5KgSFcyHbNhsidpUA98RKlVZmYr4oUyP2-6KwRX9ga8SoXv-rouwDFIPGFN_UJZ0M2C_b6rYtIuFaJW-qOU2vgwBOCmkK_l3JQQRuQXXOSURFnlcMknoYtjgwjI2X8Xmiskfo85Q3DSkdI4PIj--dQGkMgqucUAmHQfMQN3nSVgKYLP0Viz3zLYCJbrSbZrv6fyZSEYQvZl_fFkSW39CU6Exl2r7qDsS1FrUWDscqEKA_IZHZo5umwdrQr2w0mrSqX52hNpv7csQmZgWKzc-9HU4E0TtZ-KN_WLVorXfI1GwDGXvViLYvdnPJPbwN-DeviQAY24FccIRWcdgwHzZSEwO980CNE67slFsQHwbkDd8NQyF3nU-ltAvfaIX0vh-zWhAIwhFMM_Csb2jXWvXbO8wsWh1VW50Zex71SenySZrKMqRxmXY6vYP8flz8aprIKQk0fXR69VqHw1x7oCduD_xtZkQHbfB-0Mjy9Q5F7zRC6boA8WxNH_goThJHS6Suk2JDaqei4GNmNY47o_MKx6hoU6HQGRIL9myPX4t-vSWAhobWaQJLIG3xvb_G6KRtPCZVFcigEgUC_4li0-tsbxt7A9cWVvKpaQH_jyKEQzbFnZPmv8YaUtZlNC?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YtduRh6BN2pHixvlE0VEivK1RYRTgNLzEOjbbrFiS4JceM5hSFPnTpkwFm7k5jH5RopgDbOCLX94Tdx2P9TEozGT2mYuY87yqi-BrXcWPnaFVLwOF9rQ08,&b64e=1&sign=58fe0a66437b7773f17ac5e05a88a012&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 5004,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 160,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 56,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 63,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 378,
                                percent: 8
                            },
                            {
                                value: 5,
                                count: 4349,
                                percent: 87
                            }
                        ]
                    },
                    id: 262,
                    name: '1CLICK',
                    domain: 'www.1click.ru',
                    registered: '2001-10-15',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Садовнический проезд, дом 6, «Пассаж на Пятницкой», 1-й этаж, 4-й павильон, 115035',
                    opinionUrl: 'https://market.yandex.ru/shop/262/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7(499)990-77-77',
                    sanitized: '+74999907777',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mX9NfvwWCqa8OXwlMIAmpgigO_BF6j3uVH1Sghc7L8_olPnEblLo7UhliYsXOED_bDQ2A5y1OjooEtIppRf_pAR3AHC6Hylgg1V4eZBwqNP41XukWg7_AzCuCo2dn_9UslC4o7Z89jZS20UX9EUZXtiCr-A7ZxJkFC7ZhIuK0Xp4TJYr46lECaZ74cCqgZAthJKi6FPpTljRqtvGEuKjbDmZemi9OyUswIWwKqoYgcP-CK19pCtvwCB7xmzwzr87zje0L7yPdMg2yjemDOeYZFyhMTQxujjhxVs6LhkvZLLCpW-LtGGs660isegNG50F2NFuIfaNBmlllNjwrNbfw4HtK2O-Wdv_A0-fZuBxCAknI8aZ8YzKM7nS1XqMJLRA9926ugiabxDYVLuADrmY2z-Tx0X0noa8EATaMTDZEKEp08stwqqVKE9j_kY0WQjVK-rygJcTP8qlKGGmHmHH90df7fqpMcMWCv9dIyZ7Z5mwQda6x0baI8Mj38vnNcCH8G3m3BzSseGQEE7OSOX3hPb_UhLrucwbQz1zD5rrvPBprWPeqBE2fUre1TsjxHSL-u26_WmFyzBlBBSSwu4cz-uc_hxvc1UZWdpeQ_sIG6hQ6iVjW3lPlUfKIB3OlMg92U932uR6b-T3eaNZX_AqwwX01O-5mpZTiGcA1yCEq512uXfX3BPG7aM115m4JhLlYYDveHTDJifMA2kwzYBsLHmr-3apJPV0F1O50jxPo5Egms_A6hiKrPxQ6C_LFGtSpzEYSDCg0RarVKw30fVUtJ0g8-HuVaCxWgAIQ19wIo2py--tyQFKlSaSyC-zRO2kKNGQR6o26aTN2e7770eFX5uOziESfn-NaLVMycyf0e_y?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_HOGDA05kUb3FzG03qOU5HuDUAYZ5g4noLcBT5rNI8Pe9Z-ze84G2wNK8xLWnTMcLX6-G6BTf5osNJT8ivn7OehzPYBzY84dcVB3_IbOrseLA2v2iLs5XeVJNoFJzdVB7W_7sI29ZXMIlrMsAgJnpT-dInqYpY7v73TnIB1T834A,,&b64e=1&sign=43d7247d88897939351826b548259a86&keyno=1'
                },
                photo: {
                    width: 183,
                    height: 350,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/229641/market_6pZoLJLfcVCrmkgVi5OVrQ/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            brief: '2&nbsp;дня'
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/b8l0pKqxcck6o8ldO2L5SA?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=pzMCtUXAcq3brL-MEKihlne_G9n4VMCON-7nL-2VtzmyfXymoWF-DFyGh4wZ8Y4TemNsF71EcIwhUz1vnQ3JZJZiLFtfDsaIt3Q5d5ffd1lVE9L4GgwsxcCPRmXlzqZaeFihw2ov8OY%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgUEiSuxM35IOgUyJfB1eNU6p264SWO4CPUZdKHI0ULGImVlYNCM8EiAQna9k7VlXe9irSW2hYvsuOi5A4HL99kLzjFpqqJmnTRyzgsAYO06cxabO8FgBNtkY4AukvS8VjdE-qvTqwKqb-aCegWo_cRcCs-C9Gdl5Ia_nIu-lOrCGjH_0WcfM-IvKjOBaFkxKcjJSyeI91dVHMNExdrMm9t92DL1q75ikfmn9a6xs2yso93fkvWbI1f6qMWFi33Nq_-B8Ijq4KlhEu2YpE3QdfVx98jpowSCxlWpPjOjrsIA-l6aZkVIG9KEiSourozkeTT5aUDqy45otsUFqp-6MrJjmjdh8h2xPfg4yDb332DcxsFmTNRxsLOiO0PYfmNbfuWjuZuXcuZ8snxN0hUe55WT9r-DGfYE-HRvRrTogdZ9KIJGPeb4FfEKclRv9efXaqazbu2QDZey084sf9QT97ti01YqE5KEwXRB5XSLRb39pMiZA7sepDbXxEs3YKDcMDTQb1Y9Y4c2Wz12bNBEkch5lDHfMWwerAe7GbmvXXLA_zMMJd8kck8LG50zdRybVM13VlEZd8yZxReiUj_4W-GhyRQ2zY2yNSR5GM2pfL3isi_xWcXmQce_V-v2j6H_FyaN3FwDb5r0YvXRmxI1PprEa3Mjj5cSrRQr32WQPvOWYjhtP-eFioTKXuRUgfqn0B1CsEAIAx76gqI4WWwKjAZ8MET9K7YX0mSgrqPGoPPvlR9_7i9RzoD5Cu83RI6B9tb0d4IM6arg3ms6s7oJ3OAiZayRXzDgeABE58Cc7Lp2eoruegyGqd-V-DGCG1_Cty2RWytWY61LoqfFwgu06V1vSpeUaaY7LC_GeR2nudisnMu75RQ2iiFECWQWjzJZLgYOoGmFngdHS?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-5f_KGQRbmzyZc57cafVKctItu7OtbW78-ZW0EFWqRI6qjAp0D-ETVS_6X5AQcveRMQ4ugqwqXMywzRiaoz4-JWMiioXpg1XVVkIYgmHNksrs6TpJnQCb9Di4wQrbbzwVfRRtq6RLds0VEmyIN09afUYMlDbCHI1MJm4b0M4QelX7S04-MNpVrMg_1WDpcfo4_hIUE8hEE4aznFyRk2-g60Ul7i1Rb_-spWwdChMJwwpMRwJD3Q5JwCYuCdAchwl7qPkkGkL_dvp40VncWURpcHchbyBqAXLeR4wQi9EWmmPCDmuvDNixw1-eYcTwJJwXh5iX7wzy7HYr-_O_SzilTqaEC3l9o-i1k,&b64e=1&sign=53658f6a24792f45e25b8b20cb671a02&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=262',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 183,
                        height: 350,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/229641/market_6pZoLJLfcVCrmkgVi5OVrQ/orig'
                    },
                    {
                        width: 183,
                        height: 350,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/229641/market_R4aCP88FL3eS7ysrBEGciA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 130,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/229641/market_6pZoLJLfcVCrmkgVi5OVrQ/190x250'
                    },
                    {
                        width: 130,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/229641/market_R4aCP88FL3eS7ysrBEGciA/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHDKQb5dyjkaHzQ6nTzpRKZICIZ7lVAF4w5jhJHXLy2mqo84t2i_AuwT0KTEABYroKqcBo2Cij1t_DVCasdRc2rSSFJ6WPcdiDZJQ2DTUrTnR8zP1kckiu3vLkbllbYGHZvlQHfWT8I-42VBeu_j1_LTQ0ShiyyMO5CWhyXcAoDhkMhKB-g_ZkE14KnvlBn4TWL53HekTavbBo3isDqGfi-RU3mv2fsfLbK906qxjVvSKSRe2YLsgE1LaOfxovkOAN0LeofnEdnOfTLLQoHuKx7td-MIIKkkGo',
                wareMd5: 'yiLyit6upu_KvHl80XfxXg',
                name: 'Сотовый телефон Apple iPhone X 256Gb (MQAF2RU/A) Space Grey',
                description: 'iOS 11 Тип корпуса классический Материал корпуса стекло Конструкция водозащита Тип SIM-карты nano SIM Количество SIM-карт 1 Вес 174 г Размеры (ШxВxТ) 70.9x143.6x7.7 мм Экран Тип экрана цветной OLED, сенсорный Тип сенсорного экрана мультитач, емкостный Диагональ 5.8 дюйм. Сила нажатия на экран есть Размер изображения 2436x1125 Число пикселей на дюйм (PPI) 463 Автоматический поворот экрана есть Мультимедийные возможности Тыловая фотокамера двойная 12/12 МП Фотовспышка тыльная, светодиодная Функции тыловой',
                price: {
                    value: '84999'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcD-yyQuM0ZndTQ5ePdr1gvQN8dUtahQW1Mnti3PxMggy26M70cisvg-h8W6fvvD6sdX56UfL1cNMC9swsMbScIwhx7pe_ftq1l3ws9M2bGmxyHtaZVLCcefFK10Ql_K35crbQrQEo3W9U1WWch27SnZl2u1Bg-VUtwQXlxUSXKSrzEHODWaNwcFVl5uQ3G4ZGBxfNG7qqlFlIMacrotkDQ2e0w4i9VsVTC-Q18azyJsdcsFedSXMLPX6aFV_hUhu4c4VZMFOMG7NorQ3UF4ruHmR93TiaY9967j8CcM_EawmXWWY4ylk-mKbBrTEIOjdkAcOMWkWWbNZ6VJpfhWd8dEEzyeB6jmoihOWqxnf_ukGT3f5CmTji_zkHqHD4Z6XeggBjW1w4u32THNnKbzzaKWop_mzAvMqiXNg_MXSRAQg2xk8adcULGRDRKQwKtjYs_FEkGCAT5f6aL-ysyzkEk4xNj7qREuejmV5Ker93yu4G7styF2_PPJC6bTpZZhZlSp_vGC2GPixS50GZhePVz3v3eoovigbrhKwog5RIDSAXv9upKvDJaOt-SGn1UFgcr4ZvohDJXInEoRFhgB5tz2auuV0CXfHfG3RUkVlIoN9P9Gr3mQoLKTXNL-ZG9a-7exX8n7g8_RUJLwGPxN644xtZZg9wRfiMUH3_nDH_oKG0C9W8Vih-F3XxMSdTdtaanHe7HOovLou0ble16kbOIq1VlVZmjo3ba-nv66eLP0AwXCUt6pM5i5dj5SO-2sS6RAwRXFFIbRdzfnqHrYHrMFaWown7i_MSAv3IvlDwVwNienSDTC_3EXNCBPyA8EAnvbB85LyA91HDPvV8UtE1V4FUbn0I0LcdLCwXmSUAVC?data=QVyKqSPyGQwNvdoowNEPjepPkxt1Vqcos9cWvPI22tg0wCjd75HgObocn-VkR-9ty9kgea7JlqOTwblPNbhRmVs8VQhcci9sr6QI-_si1Aek67Oj8sMzQ-vb7v7ZWVP99rZBahceWkdV7EKCbsu7JpD5o-X_r4LcFtj2bX67vMK0vlv0g5gcmE6GLxqPbsUAWxojdxLMAmHaBYI_B6X2RbNsp-uD2JGBRm2l_NEB-X405tiUkunuSxywvctnjCZM3FvjNyrXZDIsCPveFh0J_BB8xb8cQazHanq15tSpDFAj2uGWzbohcmBBZl4vA9-FgOjF-IcfBzLIBo3T-TCJSQob5h8bbh4ly72p69KcASEYWkwK-L32-8KuUfqG_63VBcibdqiXmLsBda369OkZNis922yYvRfZzDoCKC8RBQkArnbJEgnETg,,&b64e=1&sign=7772d50fb72398917e3b9558b72f50bb&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRvVL8634LDQ-Y4o4VLKZEoUm3zKIfEXWR4xVrdns9DPx7Elg8Uv8rLXfkxBEOc_x4jaLwlLaknG3LUulKNym598-QpMKCnT6fNQWUh_AsrJyMLkgvbMT8sQI86YBDufec-UNeKZuEyGvbm_35MV0BNwjLMHIc9KMtEZOSn_T0XqbBIKUh1bwqtEv4EzyPUCPHyyEze5wYJGWGgpgJrk3CXvC0ownG_ki0TNj653u0byIdZC5Zrq0qM3p1O1VTpfYruTHW05PuGxee0B7OPxQOTiVY7hnEHJMB21l7YHWnGnjlFWa8NNleLSEWpTA04fbnPmuPx5UqJttu6nZdVgdOCB6gdeJqH02ebOdfF0hV2MxSMVRNlbQgwdkaZtxKV5bJDkJbwN4Xg56NhOlG0MU9_1ICoceE1VrPLZgcgmnsPXEnl0ls4sXvheGPgks0ytDU3HIgzqIhbPjvDlz7_fiAcOUE6WgTp6pK7LDVfQDqFpqw6O7525ojnrqNKcdltY4hh_Q6vbLKkQQ1S1pLYye2FYvIFT0QHqhjVjDs1BIIZonfJOWizqWMLeRpjnYgndDChN-q3_BnuRmWKmBHWq8sff5CbQOmo_G0RlRJqnDI1peoSAwjqDKN0KJfqHwpixKKr3Fx03eRKGUJzltxFEi-Bieoqdoz12-zNd2IfaK5ho1C7lpkOUB7TlmIg_vl_-owGKpsXAozZi_2EiMe6Tp3uo-tbvlqn-aD7_RS1pROKTitMCDzJoua2s-nJqK-Xl64mPxmvmNjpvfXDtutcuVPlOi80J_sx9IgeGE5aqT_MGMqWRF9yTQVminqZMEaV1STkOGZa8d0L9X9yiA9f1U_S0eT70Oac9UNBuDoJF4QStfw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2WevCIXpjIc5u3bZ9aDdMPRD-Gd5VeIhtwhU1jWcz08MYHswn9lAojeSsQ0H_nvmt65bTR8evjrJZkylpMMQ4SChrBCGllqS1GE-EUEe8ZaiGkkJVCwiPhQ,&b64e=1&sign=7433ff4378c7b707f7816b5322e6e8e0&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 6154,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 265,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 77,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 69,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 371,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 5374,
                                percent: 87
                            }
                        ]
                    },
                    id: 6537,
                    name: 'БОЛТУН.РУ',
                    domain: 'boltyn.ru',
                    registered: '2008-03-14',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Марксистская, дом 3, строение 1, офис 413, 109147',
                    opinionUrl: 'https://market.yandex.ru/shop/6537/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                phone: {
                    number: '+7(495)545-4227',
                    sanitized: '+74955454227',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcD-yyQuM0ZndTQ5ePdr1gvQN8dUtahQW1Mnti3PxMggy26M70cisvg-h8W6fvvD6sdX56UfL1cNMC9swsMbScIwhx7pe_ftq1l3ws9M2bGmxyHtaZVLCcefFK10Ql_K35crbQrQEo3W9U1WWch27SnZl2u1Bg-VUtwQXlxUSXKSrzEHODWaNweYMi26vSG-1jdT2ROmOaq-ZtZv8vPH0HyA7fz36H0ygwCMXGJdnb0bbMS-EJ4vkH6Oc9F_SNbwzX1DE4qp6I1mdloRgEdYXW1hs8sNcDPquWtHnApWQ_Yi7ctUC3xC-PVGPsGdabgSaXNAzxqj9joeq37Mcn7tTxFgPbHuhsOKz9BxYqbeJ6w_PaVP38P6G_YGyW4531CroKGWu4UUrx4VPaVhapmjPdbDqf2BiEY4ZmPV5yjQhdzkSWQ_0Pw8vPtuYEwYYJglUWB0_2PNWyjJhrYsloGyy_aTrkYa4M5F6MRoaA-ZO3IR0BjpHP77eztpcMfMf3errSlnHV0r-Xn_SIH3sb_XiGe37SoZtHmjw0mnfG2I-_0huvHMNaoN15w25jeQoi7bSr7BAhJnQS8WWqiszrJGXUmJuJ6xo0d-tVX9eeT-8CtUzNMxyIytJms0z8j8RvQEE2vr-FakIquBr5DdK-dKAtVCwX20F4xIQtOd2YuRJExDLYZ57tTDli55egofOhtAb8ohQtvX07-HYbbaAcr3Y16ES_aBDdneYrfwV1lVpeVXC3rzfk7Ls9TKn2-TYO18g6EcbJd-hbkKlJJFGAx5dQaC7wjdHwYnQwvvvrTtU3SRTWhWTGIpXyyMU2Kg-kr1CfDUsHWc5xccWQ_FMGlKPg9qdblajXjnAODPl0ih3gP3?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_vqCqlnrnGE6jGv0hSbpzhY3xhS0oR_ZEE-zZquanKLpedyNUN9tInXsVfjpeQh0MhthhONzEAzFwZMNnUEzb6hvjI3Vv-tDxrB1NHxtowwB42a1l8FKG4k3kFSa3X3vuhM1FNswr6kk4ZVx4d8N9EIcKlmYBb7GHCtrpORoZEoA,,&b64e=1&sign=4423df86b1d67b8f3ee566faf34ebe0c&keyno=1'
                },
                photo: {
                    width: 254,
                    height: 393,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/365133/market_vo9zl6fw818Q6yzb7AuhTw/orig'
                },
                delivery: {
                    price: {
                        value: '399'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 399 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '99'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 5 пунктов магазина',
                            outletCount: 5
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '399'
                                },
                                daysFrom: 2,
                                daysTo: 3
                            },
                            brief: '2-3 дня'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/yiLyit6upu_KvHl80XfxXg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HLRPyEGYq5AKeE0Udwi03m03l-4vd0ltRtLrg5_cnDPprZxsmlEQaF1q1qDHAoIYuFh5fsARsGDrWByPbfckpZZKOxbcZaaJN-joBNpRd5oarhmEy3spKLiKweQPZempOU%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgfkJB6DrN4aZwtDXLpCfoBcnZad1_okYkMaEjyOFcS6ZlkVQIO9CURjk1XKYJHcG-j7UxKcwXys2xSnF6g3eEVNk8pTbud-D2U3jkrxtACRk6ZNiU7-GZs4At6vdf8FSB0JTkF-WOBNoV85FvdHa5HZD_KpYuVTNRqYDEu9OsGJupx8dvLwA1CbXIAGz4omstWLyhL1znPXkt8r4DYH_cesMuIDNH-ygAvqLR6CCq4_M_qz4S3g_Rw2ZQTiWwOI0Hat4rPsE_dWpdCtoNokGaSGIGOlt6As2TsaIoyLjFerADiwkAmzX6BvsbaWzgSC9hMKVGQZtiu68i0d32vyi-Bf5JKA--Nu7yd6wDFZxNH8Q4jaXoA85m0tSq0IkKKz5PDYavQLQr271nnjKvXKfB7KQNFrwHb-PACmTy59sFhFGSgCkg5UP3bNz1Qq0xOQbU7GsR9rgxGcs3EvWE4bSOSJzGwcoA4VD2zzaUHEj96rBejPH8JPCVXsCh_9JW2W6lG0MTEncXml-lrgQODoF5w0ZrjfQ01yFXfeKF4YQ5arE5k8Vr1LtiRJjNPQv55kaFUrx9W7CuYnX3hFBiXgTQGWxM4uGvxj1JBWjXVu6Q-bJo-vh_iOXrTJY7ziI0nqnBpMfsMeWyRRybt5pl46A7PaRKmN6foZtknF5phv9WknxmN02_vMJ72AESyXFgL4AdW894W8Dg0gmlwy1jzGNPmUnWPILzLxxBBYItysGOnqrxXJVV0hs8YGCLXnnq9ZBnEN1NSseVfCsyL3v8427hUgRCRVrMo3oAb6-U8nNgzj_WIlUIdQ7tjayhA3JhmB55B2vUrZPXTdrjIPvy-QH4p9x3lxz5o8s6fkkzwHoOnDtbf4pUHXnjGXueYOC44wYVw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-4kg7jA4muw2kax7hqonoLhOjeqWyucVA5_vzh6JB_2Cd7tlxZvQiwJuFbhwybxUcey5np1ByrTZO458KKRCXQRL6wJck7X8pveDqgx07eXnXdV_ovCLHuTRKcnMiQ7JP_uWO09Guc9ehxVGiOM4afJ7UefqLQ0X4gDOhvgV2MSjq0dDgZw9n9lbJWrvmXdpLNUEgXqAiZQoAGtbWtCmEAD44hy08m9Ru1AvD-yND23ZyfEvsJSV2uICmNomo3V8FKibdiZLGpe_iJBysxXvfmqYg9MfJJRliNy8YytTzWC4bOySkid7OmTVXPZW6QK0ksI-XDk4srDqQ,,&b64e=1&sign=f028d2a0066e95232c2ce8da0342a368&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=6537',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 254,
                        height: 393,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/365133/market_vo9zl6fw818Q6yzb7AuhTw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 161,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/365133/market_vo9zl6fw818Q6yzb7AuhTw/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHp87u_YOoSyJm5wsyYsubGxu1TatnPGmijGN5UQfX_eQ',
                wareMd5: 'BBGj065I-JEtAEcijJz4mQ',
                name: 'Смартфон Apple iPhone X 256GB A1901 (EU) Space Gray',
                description: 'iPhone с большим дисплеем. Умное устройство, реагирующее на прикосновение, слово и даже взгляд. Устройство распознает владельца по лицу. Технология Face ID позволяет разблокировать телефон, выполнять аутентификацию и оплачивать покупки. С соблюдением требований безопасности.',
                price: {
                    value: '77980'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mX9NfvwWCqa8OXwlMIAmpgj-7V5QJwbZFTg81uzOvmBugaDZ2h56mpWAK3-WzXU1rYAU6mHaSEnfTXlpmvlYni03KOoxhTME4y47tue_TsMjpYoXaDug8bNdz2L3jsm3kJCU6zbSQc6ojvwTgpPKcFH6fbQihTWqU0v_w_nW6X_zUZmCum56YYmALWWfWI6QFSKqiIAaFwwqzjNgesOwmBcuZK9vA2Ss8UmTcYqZZa10CMgsEdowXgZreTMU4oNSf6ygNfAmTpfN4YlxuSiArMsvUWwVMzDBKtXCnBZLitTf6EuhygHZN5_zkjg0_IKirvfZObuSdrZ_T4D-AmBQPSvqALofVPSZQL7jNHXow-GrW8xT6zyDfp6ixY41EkC7cZrKxbvxGiwJgM2RBR1RjNzQMjha1hpxwuibJrkhZ0GOknYYx0hKHMsaQqryhWWdRNA7mCjbQsARLksLjYnTWNavrA_yPylhFz0-aV94h7maC4H3HN7YYHNQ5y07mc-oGOzIDV8CjVjHuom0bG3a5pdwIkpkJptIDP-TAnBWeh0Au4PfxHRI72-PR0Vtq_BvPse4SjSsNkeLxIJ7NoqULL-Gc5xsZ-0KbRefQuErG8GUJuvDPvFsOZ5QTFgWonWOLG7fBThKrqjG0pDXYM5KPNYYMglD8WKFvwaUKmfHjhWa2hhOWpTE7OW3KCCLPhM9toKqyccHJK0d8mvOKEwJTJ3BkGceGNkwbIPYIrL-Xte62NSpuOe6htrIZjWGtraARedxK1f-nGzfkIV5peXz4VDqf1unkooOa08DzrS2lHVLatHvBko8Oz043tRFpJjE5585pt47h_Ap3sSlrQ_RmDoAgp8T4GizvZM1yyz2OTW2?data=QVyKqSPyGQwNvdoowNEPjW6hlxdK140Bwg9TPKVp0qTnTHBN9pOPUqTO7YbUmn2EAkmbxg59FdhsofdoloHqW_hDZWgPArUzWF7Xm1tVp5-fnZvvDwGNf1bWwohB-u-IWs_ky8WEQvPXI2lls3ftgkJM-pv09ZU79gu4NKsZYGCpyWQkNHPWjvgu-2SVbzbOkKu4knvYpxTCws4GO5bO_0vlhWGKJuv9FQgeDeJO9lxlKAu32V8JqMA1rPI8sHj01lQUn-huovPyxOUZV11Yo5rHL9vgMwMb72grVyuxfUIeehp9jHRo_39G26pj-GWsinpAqpOhPn7PHO3fDIi1cQ1IhE_0Paw_P13otZgeECj7Rh6HpVLhRjEh7V_RsMWGOz4r9QiPGgRIWYD4EpS9bd45XyO63oobw2_DAqJ9EEIbmHm6KApUf9tDxEz2Cjd4IYr225Ni7Akxna6xSLCm-daWqzSPHO6Sc9a7DH2sbLV9mhxPPVSJSSerbpZDAo6zRLq0_78HhFzbJ6BVCgLsA2LG_QcZrwD4rrkpioYVeR-ywP_FFU0oIZssp1DURGcbNm2j1N8NX8bXn8DPgzEP6KPbdcUwHXbUit-mxFj_wRy-st7LnMSVb0TxjvtpnCYpVmusHWT-MHMjemFjuEqzK8kcu3c1HHElV9DXnoko5jM,&b64e=1&sign=5dce6ee3055538f0dd50f1ce8052941d&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iTFtwtGbqyGBQ7bIi4U8HRRa9x3RbQMogO5WZEQNul-klL1GdxiW4Vvme6JEARI3e5-UyQC4N6f0KlSp8YcJR4_vPx2qxHAxKkUwuWDZVbJJ0LbOwUGG0IfY2_T22MeY2XQw0zZ-U-_VE-RzoKNrOXbjJBx43kbkpeNdO7NeUUP_qldGjWjhs-J-IA6n06MPzC4XwLdJ-u7XczqNUy2OXqosU7rlIQWwvIrNz1zI0VZDepdl9izl5EiYdRz0xHIP8tfXUCVmcpHqKQ2LwQOxnI3olc9n12-o6Qy0f0OieXAj9G6hEpgPHv0KwEhjYFnbe5Cd874metOqR9qOQJzeuNdq6Uj2LKcun4hdOTno0Qjh2gKYkDQ_p6M8_ASSLimz2VYNbUF0Qcll9KV_pYZj0MqYuehBisoTBQeQuDFVL8pheeWdxpJzivigimtVmqhxX8JLRsrM6syyhJVTK9S6-rpWF-fJxdNdAgFTjQZ1Xr-17sTJAwxVxDlhgVa1qqLGcj6fmWXmJFogFT_sc5_cT3kAz66B4JP7hby24kRUa6U-rg1a5LxpKRmS_E2kzdC1AqjM5Qom3f69VW-1PwVyq9NfrU3vvOnBcydfGain_ttd8Uz1NCRhQrG2jroEGuap6UB_VZVMnwOr040ZNGQx5emMhCQcBtrD53nxHLdhcOCsN8wUeMGegtvFpT-ThL8a3ZxyIBM-4EFk3R_4nQG1XxJmjiILlrrrYnrwZA-l8_R-0o8NVcHS-YOtsNUfZ_buedahbr9b6WO-TJjlV-SU5uoMiIg-NZoB-T7yzxlvZCEtvs8wl60D_ezxidURvSaqUT9SUM6dEak3NY5bKHd4wg160vJvYx2MfUEJtljZbqn-?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2b6p8ZOVL6yBwxhrV4wKMUUNG0YGXBBv1JfGuM8SfJ3tnfxAS338EMLlSIQS4Z12KpgGQn7hjnz-OLPTxOZmG_Vn2XjZH5z2r_x6qxzlTSHIX7MuIWCvm18,&b64e=1&sign=ec20475a548513540369840d955e0a00&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 7664,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 645,
                                percent: 8
                            },
                            {
                                value: 2,
                                count: 158,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 107,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 485,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 6274,
                                percent: 82
                            }
                        ]
                    },
                    id: 281111,
                    name: 'КотоФото.Москва',
                    domain: 'msk.kotofoto.ru',
                    registered: '2015-03-11',
                    type: 'DEFAULT',
                    opinionUrl: 'https://market.yandex.ru/shop/281111/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                phone: {
                    number: '8 (800) 505-43-75',
                    sanitized: '88005054375',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mX9NfvwWCqa8OXwlMIAmpgj-7V5QJwbZFTg81uzOvmBugaDZ2h56mpWAK3-WzXU1rYAU6mHaSEnfTXlpmvlYni03KOoxhTME4y47tue_TsMjpYoXaDug8bNdz2L3jsm3kJCU6zbSQc6ojvwTgpPKcFH6fbQihTWqU0v_w_nW6X_zUZmCum56YYmALWWfWI6QFYv1MV2VCVzKbpCfSIO09Zsgs4zIp_ULwHhXPEi_1U91sBrtutspP5HI2D1L8b3i90wizVOUHr6_7o3BEEvV3YlOiPL89j4c_wWnDN-v-LkkJaHIu_iNkNGhGTcyA99QzvPn0Z4uqQZ0bEqbLb31fg-Rjyx9TK4ivmd9bXYHfMjt8YZo4zFFiYjtccPGaDcL8bf5UbBiuDaknGzEX6QO7KGPDEN_ofVWD33pn0sKjEu8ZQC_iMoG2ij_QRC8Y18Xn7qvxIsyFzb-kVTrK5MqSmGh_j3GObcOy8ZQp8KZ6HnSQSj95kGsD73S1R6rfvEeOncgU0XDoKYWoKbpLP9RoPZv4N78Q7ofeb7FhiW_EaEe4J6ZNZyhBumQlhiQSjLji9KMxjM1SM-R3ZWy9wqMnOdhu4WUytYbfy2G457RUQA9trJIE6hl5rhhcymNKUZrOG5SfW0d3jZiuSkOGTZsRqe06Lw0AHhknwbtFYwdUDnshse-NjYhC6RcB9y7ITRonPgAjHWeqXcw2uCareC-N67aziSVQojzX9MqJpiX0XnjCcHO26Q4iGYA39voroHcmnROzbusRYrimziFKtk3UQrOkKX12nd7FC33yi4sN6qT_jHOULoA4gW4jQgmOgl1b2-V0auAC2zq10zhRb4Zc1FRCF7Ft0hvTYW9cW7abVpR?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9jVkqdv0WAPOnG2RbRdlDmaJQIKYecuoJo0aR4vfEiUoDUmfrsIzF9m_69J1HXD3IIP3ZSxZ9HmFMIm6NhsDO7gNcAA8JzJzmpbmC0n3avVoikEdl_X2JZ4CgrNDkBVEwhALARLtaEwRZieYRLbXe-QJ78GuSgsj4EBLuaRnx9HA,,&b64e=1&sign=e6c31352387583dd9524563e0a512b0e&keyno=1'
                },
                photo: {
                    width: 900,
                    height: 900,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_Rs39FRsFlpnd3vTnvbPEiA/orig'
                },
                delivery: {
                    price: {
                        value: '300'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 300 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '1-3 дня • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 3,
                                daysTo: 3
                            },
                            brief: '3&nbsp;дня'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/BBGj065I-JEtAEcijJz4mQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=pzMCtUXAcq1kco0te-BSNSkcg3WYm7I_Yfy9JhohWUwf7oDW7QT68FerrhvGvRC3Nh5pTkn9pA8mxrWMLC9EqkQ5Qck3fshD8-AGgzyBLvvl4BGeBX3-29-EsHx3jSn-&lr=213',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=281111',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 900,
                        height: 900,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_Rs39FRsFlpnd3vTnvbPEiA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/213450/market_Rs39FRsFlpnd3vTnvbPEiA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHreoETNW1kysX95XRvQQmKh0K2xMiJIfOODaudx7VoTmtc_jOBa08dgT4Siwbne0aMKp5NMhdpsVlfh09uJ-odPyirMb869DdOSy408JhBl5qjrpRzJxMDSthmQ2pr6WHG3cajjuufh2RB9vqlv2Grx6isVfygf_foXt2C_sE_Qg6bhNVOkwOoExPFNvRpYyldvz4b0IW48m7UPA5ErQUepKlwmfRYv65WOwBQWZqNB5iNokzDWEHMKDI5l6M3o4StfMWHH6Vt5E_IfPz_m4A1kjK8wK8l7t4',
                wareMd5: '2LrgyqgFNi0PbzkY34FJDg',
                name: 'Смартфон Apple iPhone X 256Gb серебристый',
                description: 'Как создать высокоинтеллектуальное устройство, корпус и дисплей которого образуют единое целое? Этот вопрос в Apple поставили себе еще при разработке самого первого iPhone. С iPhone X его снова решили. Совершенно новый дисплей Super Retina с диагональю 5,8 дюйма, который удобно лежит в руке и потрясающе выглядит, - это и есть iPhone X. Дисплей и элегантно закругленный по углам корпус интегрированы еще лучше - этого смогли добиться, используя несколько уникальных технологий.',
                price: {
                    value: '80850'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ3vhdDSmkPVqoVAyJ-TCQSg5IfHqBsdnbqC8Urf_HLBKoBBpjD2zTHwwYRtre86OvO9p3DB0Rxd53bUxkFrsbEfVsc17MIzl6fRTKG8-Ck1XxSdWpEiDquW77U3E9L6KVfw5Qtmg-LFVGanGrztxbw6ZZnXfW9PZYsaoPYjg8vPprnuPhmutcF6V5XDluGaRl_cMF8yKlo1Ng7vmdDkATpz95PmGJO-cTc-Py00Jk4vVkHEXHIqLK3ME27KpFND1yHi4CCgFCjooiwRgYlzF85T_qULfus1X1VSx_u5UzVbjy6ka6dAIeMm87isUfT9ZPLcsRutebEttCWSsMopcVvvXxN0r3pdOSS5qjyojMjsUpoHeXcB-qUO3sdXBAdRSniHGTqjUWHJhshUCfBoGR6R4iU8V1F3TYS3Ut9qRCExdmk9eXR8m0AkGPAYv9Gi8aivhMynCxRvcfxuYQHjpACT19CUAlzT9tOtOE9Si9JUL1TDeh9mf3Wm1Rh4e6xp_0zjVeRxapJEbC_VO_x690G_VbNFrdq6dDb3ASIbc_U-ZiSao6UBbg6kaPy_q4wL6uymSJVKylsT4tBpj4-9gjJIzO2kMF-6eJiI3pmpFrW8cYWbsC46VUP9gIRqOfzJGyx3EWRclHgss_Kb7toohTkwpw1DHVcwEZK8NUqjGrh4UKqUo4JmxzgrLiMArMhbDjtloPvAZMLa3qVb5ktwA41mbtBA-r6OpykOUg9OrFo18W_-a7ZO0IQ66yS1fZdgUjJTHWiN9dByrqKoJtQBx8O7wSkeh5PDh3s5JYreF4y2d1saPY-Bj3A5vFh7Q_0knfyZMPXKjcNfhWbTGX2w4WG-XH-aZp7rIQ6IJjyqDFhn?data=QVyKqSPyGQwNvdoowNEPjdVgIq_drmvP3qILYj6T-nei_jyWVYNPxQ60Oy9asP32SDpSUNQOKltSJ5KZ25Epfuj9Ma_IUfaqKXhiqvjZ3SP6ND7ek3FNPaq9xa1-hEGdeTY42OWgEpgU8YalLslbb6jkX4iSACZPn9VxH9rUhA5BN-jbwfuAXVw5ji84Xv6XG5jwZA5XiutXWPxrEye1Z0j5Wa5rxBmNUmXgZJYmsqdgAgTTRH18b8LzieUPXREJkfcJA7Ju8zaxpBpwPmJUfBKGcIvmjFtC_hUP_0Y9rjfD8vzB50APuicAg9D9f78_yFLWr4-C26Owj-P7PKQxoZxCPa_TcFFeFMlefXx3u1IYp_MyDJKDl0eUJ7sEqnCRswx1ANYzRdHnpjeORQSWQpxGeHtPDfTYzXupbnMOu2GTCqloi9mucyohTU0k8ulWv7UfVMrroULQhPPDLPTW8h6U42xo6m7SHrODa0kfELGllumcIbp4mmieCFw9t9qBqW6piLC9lc8o7TY06t1c25ZT-rFwkXl96UMw0AHbq8yP-3sIuEa4IOknSGjUSwzd1z3uSxBeNe2nOPBb6bm-Ow-olaYmWkdFlVYVkbuKzaw,&b64e=1&sign=835638a2e25a6fbe564f7e5a54ac3554&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iaiQKkGMjjB7lTnJcp-uWNkfbWCzz_PBGgIzt_9L9n9QJX87cU9TMqur96zaPDGxhUk48ENSJZIYw5XqYjYqLrbQGpq7374pH1cylR-yh5n4O7f33KZg-wBgvaVG_HFVocmJGplZ8bNSZPMvcLgICytKha1HVqBkAy07xwYcnv-vyPdkYbfqFLjNbFJ2Tx-SBC8KlUJaWSS8OpuoMqnHtHGx3WdDh8TUegl5Tey3C0O1IN1RZq4gAfueqHSR7-g47j_7a9CVXHaANhH1jyDm6bD_YwgOhmE3DU15xkoeIoscFZ1EPreMhbnPSBDl6UV8Pyt_1sI_ruxz13yCQYX5KpmimcjczpcqrlzD7AtdDKMKKVQk1z3xp3UUfOvWtSqE5A6cUaLupu_PqWJZyAjfUWzcu43zbuJVBo5A6wZIcO4F1Wf_0kEUfPinDwijr5Y7g-ZDc_jOoefRuvhr3qXGwW7nS4TWIJRGgPUChzFFalDYsay0iQgP-lafZmcjlXHgkpD6_EYVyNdHtnFVKuK_po1FARvAZWDsiRWnrbOreVzG8qaEa5n8p8X6Vv-EJr3L7HyTEN1DcJOpICvUa3YV3iguApvbakMRjmZSR8ZgMo2RcEURQZKi50X5ZD5B8vA-up1DJC-wiWiVL0j3OOdIGcTKQcGYUR7fkwRRYhSo7KvqMNc1kQ2TX3n8DoQS1MNpUcXCraVDZsDVpwaWhctJEEvV5pZSj92dgGGlc-MLwEM2y-usPueLPfOhCo6J7mYXOl8G-yO-SiVhPEPZ5wgkmFHKMLV0ZJZMetIW7v2dksF479DmvSRS2SqqNh1du0lqvjQh9pcRyDQet2oldxmfNRs-clJ0ePB7cP1r55pLsRDK?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2U0M3Ioan8dzx8vYNSzAg_b00r_ASf7r2MAt86-MSWZYR0gIaW59QzurjD3Sm7eJq_hAontjmR7r2nqFuXyyyTG2NIK1v7fJRcPdoHjDGPU0FH1TvrfqICU,&b64e=1&sign=37d110e58821fbf30cf62174b8f2c7ec&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 342,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 3,
                                percent: 1
                            },
                            {
                                value: 2,
                                count: 1,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 1,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 15,
                                percent: 4
                            },
                            {
                                value: 5,
                                count: 325,
                                percent: 94
                            }
                        ]
                    },
                    id: 385599,
                    name: 'TEHNO-STORE.RU',
                    domain: 'tehno-store.ru',
                    registered: '2016-11-02',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Пятницкое шоссе, дом 18, ТЦ Митинский радиорынок, 125430',
                    opinionUrl: 'https://market.yandex.ru/shop/385599/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7 (495) 298-22-28',
                    sanitized: '+74952982228',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ3vhdDSmkPVqoVAyJ-TCQSg5IfHqBsdnbqC8Urf_HLBKoBBpjD2zTHwwYRtre86OvO9p3DB0Rxd53bUxkFrsbEfVsc17MIzl6fRTKG8-Ck1XxSdWpEiDquW77U3E9L6KVfw5Qtmg-LFVGanGrztxbw6ZZnXfW9PZYsaoPYjg8vPprnuPhmutcF6V5XDluGaRtofCtKcVAZPsZL0plZov4dLUr6KuM0vyMIw8RMY4MG2C2THhiCU0btJH2Xv_lKQ5XhlGljdubY3Gjrj7lOexAfwIF3snJHqDA4o3TQj5N7kNQAK-5CX3RYwJAAuECxE6MIT41wEvWnOUWNKV8cQq3Z40xpACrXNKkVVwltAGgnpdKOvaFwRTI8XVK8vp-6ncyr8FQ_TUL8CAX_J5HDZGZpy0in3znzMFxPCeQIxLz9-7g_Ix6suFVOCmmtQJR7SBaVgaq6w2GlBFiwa8m3DenRI33yDC2zrD76j7_fgqoJzYSxtTmWW7tHJYrrNOXYV0XM38r-ZEsyUox_dTClaq9DIr-s-KW_H0You7Ytkwcdxz9BEh7z6Bh0Yja7A_WYVTEb_LUfGcB5yjSVVIeyVCAP9uaT0puYmlR0iX9-6yP6ZFFQwLvRa30Y3c0Qpq9a6bpPfoCQX4c_U4ssYdWAqpFXBcLfCydjNf-EiZ3xbwLD61acF6LM1_MExjHQLxyOZwnIcRArFXvbvmWs_2ggzBtp2vlbM4Xo6pjlMa6QJ3kgg3wD2sTJPLJ6ELLE-_TZTjp_4g36MGWuf27ArpLS-LDg32cX_x57CwTg_JWfR9G5oJSRLoh10IDzT3y-h5jVKwy6DcEVrTkIanFst_9ZIzmqD3LpC59vDbxH0OoT5XQuO?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-I7vLSNZOdOJy-6ze26ciDLXWiCrXvLhEtjJ-ESjZscZpMEDOQSHhDqnWq5HzJW2Amck6nuKNuwbRBuJDJaqSEZo5ROCX4EuOUORrGuVc-Icf6mIEvXjZu26Av4QDdbxgw0ewidUdBBMEwM3oMatobGQ2xncqlQddKMom1Mtxmqw,,&b64e=1&sign=a6ea380a8767e4024f98d530e4d96ab2&keyno=1'
                },
                photo: {
                    width: 800,
                    height: 800,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_W4zlbqxLyoj3gKHHKON5Kw/orig'
                },
                delivery: {
                    price: {
                        value: '400'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 400 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 1,
                                orderBefore: 18
                            },
                            brief: 'до&nbsp;завтра при заказе до 18: 00 • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '400'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/2LrgyqgFNi0PbzkY34FJDg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=w7-HJIBFE20uVGCEQw5zJ4VSlo4fX1KltlykxDxbu0Wg2I65QvXjxNoLSLVz2gfulmIHbip140H5Pt1N0zfUCJDqsULtMVyHIL3o_4Epmvv3rsQSWLjPiNVEwPK9QW-d&lr=213',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=385599',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 800,
                        height: 800,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_W4zlbqxLyoj3gKHHKON5Kw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_W4zlbqxLyoj3gKHHKON5Kw/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFH-sbQ8eqT800WyypVbh8wZWb2etrFa9fw7DjxievH5g',
                wareMd5: 't6G7dOKnKQkPNBizQ4j-6A',
                name: 'Смартфон APPLE iPhone X 256GB Серый космос',
                description: '',
                price: {
                    value: '89990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdPsFoSWnCME1O6qbzg8u6xfYGZvUu2MUS0KJejhukC9f_zc2uIbNTWUj0m51R1z0ibm4KraP-AQ6NebJNcxqrycmNK1UBzfMU--RKhrtsAMoOFoi5V2uf4hBrfIqdkpY2s9Grnwro5FKE2iuYrStLjHuX-mg5-Kl7U9cuIN54gXT5y2-_4TRyfQvbg-0_of3DUPqsglHNYzwdc7qMCEiudakbfESgWJswoFG-TleZBgavzcrM6hxpoNLwmmooqqijEnrg46vBiOrPvLXKrSHLT6FvK0Zy0SrdDXh2dUXNjf-9J7d59eAZfARfmPnKdBQa2KMd7YLP1F7QG7PB8XIcRGHpNw3TxVv8Au0yAFpDV_C4VC5M090D59YdTuiA02nLvdg6B9Dfa0efTJrLYlyjLp106Ck0jS4b5iVYz-sBdF38fxtvfwcZ4R5krJ2VKWAHjLkB1cYiqBZXFX8c2jKaVOkG0ygfCHdgxDYmL1ciFXpVy1be97eGjLLuWWjDucKk4jb7b897aYOgqLJxO-4SN8De5YvZmKcc-pOCxmg9j5jveLBGf7Oo8qfowm-WSBaWvwNbpFeVMmQLRU1Hfa_auH5X231feKWdh1T5VGVMa7SfW012emo0azESIPif9IXnEswbhK7x7csx6yc7hF-70vR88WAxMecGD0xqEEFGHBFSiNTvm2BE8Yaks9DCLob70UBS-1ib3cUCUhCvJF8s3p6DRWg1wbRnnGxZBIS6PSjsgNg531CPh3qgG5Kdw9giJmoe9BEgFyZ1kqf6LBXKifDpZ2Jhl0cZfbZM82EX7XaESWzfJ44t_xzEEYBlf7GAcN8KAy2g8Ay7PlZNWul-Ea35VC80zMrxXZxjmh-nNr?data=QVyKqSPyGQwNvdoowNEPjeHCAo-a2AEEMgvw6Gv7rodwqTifDSzaDcFSJWWZewOkDXpDqhZ-M_AHJZ1AjI7qCnx8jOxe7-xVjw1iIo364VkOHvNaifAyde0sGVn__kA65tuE9CwnCzvdFeiMLRrMMZT0OGzbGsnkmDi7ohEl6SPeqk3xj5EYjGr7v2Xvjwv-X0Ve0krt3cqZbA_R5p_RmWjlOucWR7fwRYZA0NF9pVuuU63s1uiZJcBQnLFAE2y95fprVupaa6xqm08jAYBSiF6i3CUaAqKCKfWiN7PZrTwTCpbMiP2pZ5XhOt9-3wYG2DrlrXyiC4UOrGYH-F1UF5my3O9v99He6b2fyQwGAvI0wekuL_D1OFZbxO_0uVnWPZmmJA5Ia4Q4z5pkUpjOoDgHvA4qL1YCrnH81Y6Bs-hAxnJvV0V7x_sLdAV1exBJik-azqtXEheqtiPi_rwhZ1NBTA4UJBz-aNPt2g09uisDhX7MIchSAaHjJqxByTrk-97Sqhwf_63V7JLW13kzFTZWs26Fu838Gjy-qua3aeDsWMds4sIDN1UB9X-YmCvaMobBJr8zi6WMT7QqJhsoL0VTsf6j-jeYVb10wEclQwl_hHGJuUhfDg,,&b64e=1&sign=ddfa0a022ba641bee0337fdeeaac1197&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4id2ok1lfdy_OsA8i0LprTdKgjcy_YH0ygvMHrAq7Gax3MqCInPPBkyAe7oR8WKWzwKLBWvUNnuFRaapxkk8CM8gnhZbM_BMaVnpDi_u6tQ40HsWc-caEiuX9QA_hfsF6CT2uht_fymKUUzJwYJAitGsogATjv8pjtu3AXvvy1IA0zIKLA8nO7NB4V5JUCtkeRiZE0KjxhFVG273rRfkKEIM_qF-iugTjRRoN8OM9InGdWYCvKzdhW7tVSeHt5saYxSZAtuxd8oMmGGsUvGUa6xZwxs2FFqdCt81W--2POvo6VKbHGjlIN9fBYBPLPb3NB1UaVLeed3pFtIztDiavsRxzWXqxn6a6n5eI_cVQ9EkOD-9ES5Jh5fROhxxPfcDUHKOCBdnXns5PFKZu6vSQhG1v4Gxw78c2OhRceGjfv455puikZwPhVeTXihZufNH94mNkcNKS4Oj4oKUbLtVMRN_qlLtM0kPyI7pGgv0NiBCoURsxWPAxVZskLMJbxIOBSukLd7kOqiXc0w_NfXWaHh5JCNZU-5XECMNnD3sSoyRipPqoNLHAaILLIIkgPGwUkVIeF6RsPsqHIbOtsExEuLYRWw1lm0XL3Kv6j_v0z1heEQdPyUgd6_hTbwHjyVW1aU47VWOPGtbvdUMsBT96Zxc7E_qc_FokbHlqGLT2RoFNvSSJU2FpMMzjR5B6poI9Z54TbNo75sAzjIelCk1nLVyDDu1TKh_YFqrGzG5Xrp9qYZB2NXkH9Ln9T35trA8f93ErleDkSewtv1bFvf0-eQw82-ICWBwvTZUeXK96rFIP92Mpro5N0LiRJ5ghiDUG_Re1_1uAu9zzKaJgJBl1pWF_T4fM2A80N551nxB20FyL?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2UaWiSVZ7Yv2fDnW3KYsBTjPn7vdqsEDhDbCPCCT8pznaNEVdINzm8ccNjcep3vUKHD6PDq3Cy27vV_B8KAppTQlJ4UVc6c66yfOa69PHg56Q12w4Gd5Hvc,&b64e=1&sign=739f2b6bd6752f81828ec6b648b2f529&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 66894,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 2971,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 1057,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 1219,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 6397,
                                percent: 10
                            },
                            {
                                value: 5,
                                count: 55316,
                                percent: 83
                            }
                        ]
                    },
                    id: 255,
                    name: 'ОНЛАЙН ТРЕЙД.РУ',
                    domain: 'www.onlinetrade.ru',
                    registered: '2001-09-07',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Ленинградский проспект, дом 80, корпус 17, вход в магазин с Балтийской улицы, ОНЛАЙН ТРЕЙД.РУ на Соколе, 125190',
                    opinionUrl: 'https://market.yandex.ru/shop/255/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                phone: {
                    number: '+7 495 225-95-22',
                    sanitized: '+74952259522',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdPsFoSWnCME1O6qbzg8u6xfYGZvUu2MUS0KJejhukC9f_zc2uIbNTWUj0m51R1z0ibm4KraP-AQ6NebJNcxqrycmNK1UBzfMU--RKhrtsAMoOFoi5V2uf4hBrfIqdkpY2s9Grnwro5FKE2iuYrStLjHuX-mg5-Kl7U9cuIN54gXT5y2-_4TRyfQvbg-0_of3BL60ZeJZxOrJhDViOx2mD3d7S1S9QEP0bfe0bqJ6DYFIZd3-51N7iBYRe6K6SHtmep_eyNtJRbyEJUY10HgZC5zb2gFEtnDAIseKtdtcnVXjKC39J_XfaQMxCKlZF0NaEsEUzl_yusD24yEPgN-tFDCNk1BXz1SAj_Y481xrYAeneoidkYPvSMBFqnohzizl7T6X97HNEXv7dW5azVMg2UaiQIKQ0Jtg8pDdaCbD7U9SsvihnJjqtupUnLBQFjERxmw3k9FrT0mPgATG3SC1BhZRKBLM4I7Hym9T4tnzsggU5FnB6fz0q_E26dEZZW3rfRxImcD0HggOcKkw5WR6XFMAHwTq2t5KjIH8128ejPl_oSYfz2GD7dXnHez1hlcfqsdABvHeXTzGcHjT1Eh-X5WsQfxaI4YMhRfYZbLKIK_1t5AaE_eMqMAkBm_6uc9EDgCJPn9mLywtbBjB6jFPuZegQWGL3FUpW1-7FGg4yuWsWLoYFKdyG1irWX4MO4RBFh14Sw5cvGbw2dpv7RxLREKPl7pV1gn5_diz8Km1SKbljbI-XQv66tmvppcWxaVrRsn45O0pFOWIXvd3DB1LKwortgF4fGxh2ak3SYmBk0svoE_Kos6aurJxDDZMF8H8FGMMs9O7HZGsHJTwvM3qA1c5ziT4mm3_KObKIcKDhlX?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8S0S5Gp6pjONUpBBHc-qs-fZehNzlEEIkYX5szcanZybhxXP8VvBl-xdJrWPMooVTmS-ZAy3cfnYP5yMHrAZ4jvxk89r6KoN0-brYdqTu-rNpLoZmD9epjdcEWPnYTuQ76lWJ9DJuz8Tsr7U6RTb065TqqKB2z-fNgkuW7U-6LMw,,&b64e=1&sign=3e029c424fcdf2e6df07eeccc7be8e29&keyno=1'
                },
                photo: {
                    width: 344,
                    height: 689,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/235963/market_cRm4Mn1ArNZYKzfF3QoMeA/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'завтра • 50 пунктов магазина',
                            outletCount: 50
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: '2&nbsp;дня • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 3
                            },
                            brief: '2-3 дня'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/t6G7dOKnKQkPNBizQ4j-6A?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=ouSlx-8LsVmipKOLPnIkDG_ETCx1QqxSGtOs3UIvT9FVJdVXYl5ZO8DmltcjqjEeTlmGPbKxD9Tw_tc65XJoFx78w-5gEs25SBE9dH6jse1y0z0K8g2sIDNYmIckK-NP&lr=213',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=255',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 344,
                        height: 689,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235963/market_cRm4Mn1ArNZYKzfF3QoMeA/orig'
                    },
                    {
                        width: 344,
                        height: 689,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245020/market_3dhjqRy2nbnXj3kRYvu0rQ/orig'
                    },
                    {
                        width: 44,
                        height: 690,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/208277/market_wqI1gudYdQOA7bCqGsJFPg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 124,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235963/market_cRm4Mn1ArNZYKzfF3QoMeA/190x250'
                    },
                    {
                        width: 124,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245020/market_3dhjqRy2nbnXj3kRYvu0rQ/190x250'
                    },
                    {
                        width: 15,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/208277/market_wqI1gudYdQOA7bCqGsJFPg/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZG4qtGklHAqpCCzIPOkqvWHe1ASjMSa9L6pjUWsXimDrg',
                wareMd5: 'J4M883jFqRWqhi_puoQhQA',
                name: 'Смартфон APPLE iPhone X 256GB Серебристый',
                description: '',
                price: {
                    value: '89990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcs8gIrBjZUFblRDVqu5Cvvz2roVn1H03gmAYiN3XbPt0te0pwxXXuPtlGti3LF3fJGF5UGLsY3_GTiJxioZq6TahEqH6Gf3AOfXjGG01VMnPXFbqHK8rMPKgDiE71adY6mrYyC79-blyhpanWo7_Sipk2GJsYpxfnx8WOawzEs9xYvw7maRavhKdcTOHDv6aQVfA9YQTwn1SUKSFzENk2xGVhCDepyvrEuru9N7ellqhBzFk6M0smWehxcqqh6sF-skkHSC3il3hrT16CapL7KXmIGZdNrtzSUS9XzGEslRBfk9AL6w9NxMg5Y1uN9-AyHor-N6DxCkhpG0GWMKNGHGe_KE6TSHpY1faA2qh2fRPoCiqLkYY8r5hG24aFpcCcAexG1sAMgqLNxedG2n-8IDdO9U7RA6m5HQYdwLYqGYyL55mtBogfPnflSirAB4mOWEiJxpTtP55ZHOfcHfDOw5-WhS0SY-jSytlWyAuTvn9Kz71FGVm6rWtcutGZYXvVZe4690NgjCmCrnpmD9s8sisQdhbo1EkW9fVlZtGePMxYcHZ3up1y31UVPuA5IbJYG4w83jfw6wX3DsP4tepYtdCJj5S2QPDYIzhgZC3qnMmll9QUq9P2CMvgpFcieKUNfVIp9I6iIN7MbDt7HTlBTVCy3AQsy5ByBL8xeVAGtdvhfJpLxtHj5GcpNfUBGbJ71TP3O_XX47mv7zh_dJYMYz9Vb0DYLH18gspcNdweqhgBZqL1IgO3mGR-7Z5FVlb5oSo4y6zTsiM8fzpjNI_9DoM753Dec4J-vHDoRRUaUG3-alosnQvt2wFeE0-OzXL0e5W811kcEX4VXKun5sriLA-ql8uKBmO6r-VrQZtoHL?data=QVyKqSPyGQwNvdoowNEPjeHCAo-a2AEEMgvw6Gv7rodwqTifDSzaDcFSJWWZewOkDXpDqhZ-M_AHJZ1AjI7qCnx8jOxe7-xVjw1iIo364VkOHvNaifAyde0sGVn__kA65tuE9CwnCzuYeYziUganWXFle-zsPHgwImlkT4PktKhGphb5sDq187tRDoTTCfjdx7c_NFIT90OH8yfbBBLcNTBzFfihOPfE2VlEmlm-xjqAYO9dac76ybaLCu7tCuh_TkNvZSiK6DUjhQBi6dej5lEkW7J97dO6-ZzrmijQ3kLd5SzdVelUpoo_wT7QVx5HadhztceogeW4nD1OSxbXXYg_CuXk_kflr1lOje1QAwN74-3QAOignE8monGD1Lat3HZGMWC4EtvI5T_ZWlSa1Ip6VhAOgnFc7Zf9LIZrBqvTLJO6qE6RWAKQMpjpkw20wAbqSmp5ILGIpf1kLfMzy_FFpjqdazVbQ24kiXlADSvA4dvLm019-uRJC6Km_i2xXWLNAL9RWOOA23sYLwgRsLgsWbb2EmT6VXFmc8QEYNSPtN_xOtUIKOWRv_7iRUAtHzduVMMGezlrEi_xBObu6fWtE_MVo8ueWgKhWTa4ris,&b64e=1&sign=90f0cc532e74562235ccc28b1f211eba&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iZ6wpaUy4mkTV5_wq65FhubUPJlGG5HV0miu8xbsaBTFflQtpc8-R6Taemg9G_XdwgGcBhHINu8CUt2Q9GgpZp_fe1LQuirW0ypleWbdemR_LnFeWrRNN4v5YSqWIABXj4ta473ETjqDXPDd8DZ0gzf7bYy_egorzOfdyQvDX66rd9r8yKZs-4Lc2k-THMreSXV75h5Wo9YIjEWwpyAtZAXLL7sMER8_rizM9DJT2nO6PAtgjb7oN0kBFMiCZhxxI8EEEgpBbferGr3aRVH1FB1meX5DebKvZqRaZIX7dUhAdm5hMmqBQDzJAwp_eLj5CdyzZxm09VU7KzHFf1YZIZQVp-MtkI_GwdxREA6oEASq-r8SFOGeyqqAulgwaDpU7bQ2xivdZIR3z3TOGXJiIYbIuBQHI-ItgSZ6JUGFw7uGjYfM6SOnddRQ_ouu2OqCxZkowtSrQQcJ9zunhA11H7ZBIhJskCwgtJdFXvuKTMJXFBQPiX7ijlsWlwBQiDi0_Dzc18TCMUJ0VHZgw431-cj6SYguOVP_DQ0843QSgTRaaerw4G-9vVVo-rbnq7yzPiJ1-op4zLzKbSnQvo2Q1-p10W1qSiiTEIJsjw39BHOdmsz8Uo-RmbfKHrFQTXE7HiWcwgUco1Sz3cQ0k35N-lw9li0GT-VSEhFETU0U9B0xaOQWiRDIZeb9yP4pdMzDSExu6jp53svVgXnIQOs6BXxQnpscyYTK7ZM8Imq1EkR_K_6D7I4_IlwQbjVrp5YLCec2UYagyTWIRU2WDehpftDtVH8wMiNvrSfhVhWBPeJ4TsoyshU-lh-C6uendu80kGTiQqm3u2m_IKDWLdG3K1VRXtKJcTIwMnT-UDmj4YDW?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2UaWiSVZ7Yv2fDnW3KYsBTjPn7vdqsEDhDbCPCCT8pznaNEVdINzm8f6CavHtcEuEBIRIz48Xs5URFCbmTuE_j2f3e0nUmDSwh9-nK3txgND-JxBWRceVXE,&b64e=1&sign=6471fa0d2783d78dea303b5c2389cd7b&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 66894,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 2971,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 1057,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 1219,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 6397,
                                percent: 10
                            },
                            {
                                value: 5,
                                count: 55316,
                                percent: 83
                            }
                        ]
                    },
                    id: 255,
                    name: 'ОНЛАЙН ТРЕЙД.РУ',
                    domain: 'www.onlinetrade.ru',
                    registered: '2001-09-07',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Ленинградский проспект, дом 80, корпус 17, вход в магазин с Балтийской улицы, ОНЛАЙН ТРЕЙД.РУ на Соколе, 125190',
                    opinionUrl: 'https://market.yandex.ru/shop/255/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                phone: {
                    number: '+7 495 225-95-22',
                    sanitized: '+74952259522',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcs8gIrBjZUFblRDVqu5Cvvz2roVn1H03gmAYiN3XbPt0te0pwxXXuPtlGti3LF3fJGF5UGLsY3_GTiJxioZq6TahEqH6Gf3AOfXjGG01VMnPXFbqHK8rMPKgDiE71adY6mrYyC79-blyhpanWo7_Sipk2GJsYpxfnx8WOawzEs9xYvw7maRavhKdcTOHDv6aU9yr6hM8-9jF4jnABrlyUzQbyZLUwfUMK38ITFlqyh4MoWLBFzird6-4w97mI4_A1ZWMDwRypCEPuI6DdyOBOmvNdVGw54JlnkJdpqBGPyKAmIArD_nwV9fqXxLufap8lrtHI4PwKPMFtcQz-aS5nQx7fkQOHuFcoeb1P72JfsKiJmQotKFVsFrb6Cqrzt8DzVmfjRBsGZIpBxoo-5DNxuXm3Brd92DxmgtiL-cb5ze2hjquSEPG-KF539dXdOjPPcRzjK-yyxMxile0d9aSd_j-WHYds-5Gt4GHO4sbQobYc9eO9FBS2o6W38cV5nBJXsYz6CoIOanJJ6ZN_X-29zBtTUFjLgitjOwtA8H22uLq2BSuyiMhKEyrZQXR04r2lky5wmMLF4HZ4C21hDXMG7NsZEDUYnD6VHSWFBYL_VmmPHsSGO81yMoATFBu_-L0AslzpYnSy7GdAqfA22ZGFJ19rUfHGjzAguiSWNawCydPx-LZJ3i4p65ZL5cQoMjnuTl7T9fciOYwBwfPorATXbZXGfkN94CtKgjthfUR4E_XtGIeMUH3rUXm60BkKGKYJoibGyhEevwtsysodKpUWCVTKgaijq07_DaTdLR40jHzLLpF_rKk25Je5BRNfFPcV3_yK-Hq4nrnrmE6pfgqb6mJC4mmw5xYEWkV6YRnt6u?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9555C6Pki3uChf9dOpAbMQ6ONlMGnX5L6xnfSnv-AZki8wmFyMigavYKgxMa3m1Rg9YNrRqJEvQ_NLTtp02GM--qsvS_kelPrwtqNc6TqG6VleUr_RcMUOQgbC7oXa07dbeTHPQnyj-R_czDwBEPuYICtNAwb7kHE1UDMkSI-VUA,,&b64e=1&sign=1b70fe3a4c7f1d06791a12d493d8b034&keyno=1'
                },
                photo: {
                    width: 344,
                    height: 689,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_SXhmzvVAmO5Ba7Dnmex_rg/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'завтра • 50 пунктов магазина',
                            outletCount: 50
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: '2&nbsp;дня • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 2,
                                daysTo: 3
                            },
                            brief: '2-3 дня'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/J4M883jFqRWqhi_puoQhQA?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=ouSlx-8LsVmPxfke0fjmXELzrQlcSnPx6jDIwD1rQNjL8aER36wa71qzWANToxenhKfFSbyg9SWEBRcj7o8yHvxutExgmtAxPQsl3WaNwmYc-LZX_B_7WJzMcltnfLIs&lr=213',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=255',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 344,
                        height: 689,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_SXhmzvVAmO5Ba7Dnmex_rg/orig'
                    },
                    {
                        width: 344,
                        height: 689,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163651/market_X9pkaa-VZOa0X91vg1byPg/orig'
                    },
                    {
                        width: 44,
                        height: 690,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_29-Dxvo9XEPopQ8OOVeq-g/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 124,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_SXhmzvVAmO5Ba7Dnmex_rg/190x250'
                    },
                    {
                        width: 124,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163651/market_X9pkaa-VZOa0X91vg1byPg/190x250'
                    },
                    {
                        width: 15,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_29-Dxvo9XEPopQ8OOVeq-g/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZFRcmoCgycVB6zHgGDu0nP2UNyz7iEdrm6pQxiOmq3UI_XPs8Jp2_U1aaJ2uKbbG9-VRhUKiE6nvjP8ndXl8lyPKhPmIl4SKjVOkvNjX-OFwLLvtDAFqKKKwdhB4VsdB6we5v_vHtK_kN3X83yH1lH4OMDJM_queYPgjK51BfSEfbWYLO7Kg1PC4xnMbuMNxu89hAI_xF_uNp1n2VBeTYc7evuWk49Cv-UD-uQITWtkMSbHhhAKNXLbwczHuh_WCGaG6zEX0qn2t60n55_VTw-0lA09N6ejH1s',
                wareMd5: '95p8QdG0GsZRO-EbcGSDgw',
                name: 'Смартфон Apple iPhone X 256GB Silver (MQAG2RU/A)',
                description: 'Гарантия- 1 год',
                price: {
                    value: '92570'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-4mW6Z3uIuuA-9LDCOGW3If5ejbXN6xs_0fYvrZzUWwgLBxT5adpxvob2xhgwdSyWJ68_52NEVQDjq1HaUJEfrzkLQXdNRlgRdrWU_33rL-SS-IByk9A0DdMktfVEdSCTT-tac5OniN0AYId0Vlw00-5uOPJBG8D3ZKxQdSE6uWveqOtCmOBoS0e0nwjMvOVYfodQrHbMatySO6_RdKP4gXQglXLfbFdU8IVcslEkPesQfENlU3YLoCexHNs-u7K_9GS8ear7PhyOwHGNSXhCeSrcHZENt1PnpG2L6Cui6B5A0Fv3MPy3SBVodtAy5bAPNc0kK3-okpdg2mBzfuSeGA6i_a70zW2nd2WSZAmZ6EafmPwZKilflLLwtE1532g2OJcYT6dQbXjgM0U9lYd5M9QqRdTpnvx-rA78LrQo9FP7ESq_qei_KMP-MmJ6K-zXxUsg7dzbiOIb1IuSWqhlRRVa2NR6J-5wuNGbJn_0Y-gC0z4ZKRsjxs8hLTSH69Aq-8AwhEK54jAXQGII7P16w91kT_FbegeJb2l5w8xmSzdTwLqD5_IOFI4iHNTOTovsZ2v3_e8yHX1uA3sz0wiB6WfvR-MB2TJ0vyFME2Ymn69j7bdxI741dofbp9Fe85fwwGtWBk5TZz-r9XR4USQSjurFiQLb00o3CSwKoeewySm5HimuYw_qTy?data=QVyKqSPyGQwwaFPWqjjgNgv4iI009xpgCq3CekWjgfOyHxu350b62RI2engJKOv8el2id9Vn6RNI25k5-GkJSatrS9p6d9qQOq2kCxoFe7Y0Q85a8YHGd0RQcTeuWMkvYJYIZRbExScEtA4JraPdwEFc-53Nk7pq8vtohWIRkms04e97Q2kdBi0cWu8KzMe6i06DnVua5J8RpVyCREAHVKDHSxO1_1yZVzxL_iUdv-GMPyIGLgjFl7lGrvdCusMiUj-jUuXbGXV4hG8AaPt_QfVkq2Z4NCqXSUjht3hVPJOiEqPjwuE--iV0mkEp4ALdiwzx4ltNRL3XK_LfZ1aj4LGechqrX78xGl7qbyksQ5P4y6lSSSguz1JyEjIIAE_wjOw5xM_YZdzetnFEScQAQ75z0oXJLFu2UNQOR7ObKy45ne8UWAiDrPxlton5mCN9Ezg8v24J4PCplTXLSXt9tLiMG8yMtd5t&b64e=1&sign=db6b15262af101b11d5f366fd4ef890f&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCIuUwUF7cHox4IE8nmKF8jBMVcquLntQ_qAlEBPajtVXkj7v7gT3hPi5uKXkAvPwmB0hxKNnHVIBTyL__lXZbNCn7hM6HAe83Hv7FRKdZutPPj0Rp73mmORQtOkbZXyPUh-CYU2y4OVCmObxJ0_Q-K8p8dI0-Ie5KcVlo2EKXSf_08PImof4A8H8rMEUzgnA2U2Pd6tZ0Dw_fB_ICxhW5R7I5LBp2uTP0vtyOXjbM-oDeXHD_IiW5LisF3TaBSO9_ZwQV40kcSilqGxsfxLUiWKSGOfqr9VsPylbdqQIfiI5njeRmgEGhYihdjmAbZdYVr-NDkxUszjz8jS3T1kM7xNaj4GQZcc8Bcy0fW4sKAHwoMcmOW4keWWB5kSynURUFSRFZ5XaQLo_sSs-Fmqjw5SAI3SmhHnmbj2IGTlnIKjT4QnERHSwhvQzaekljlMCOFNlwyBHhceF8mfYfglq4Sf0kd2NuHXIcg72jgMrVZtxkPoOFl8D5breF50qzaoZx-avWDIVq26m0WRHe7KwdWRrgtjLsohf4Yy-EwAXVAWBQNLZnzpoKVB97aPCHY71qYNFa5NYkY76qBOhtqkaHFUr4Q2Jb7kX16yDScbKCbr2jSQr1yh4T5Xu7mxB9cCXCv-K7jB-8BfKRTDfGjOY4_CAt4V0M9cJA6Ap7u5QIGCgpNT0fZ9eshmJ6J1ro1rYv2gX-_31SxssNuZVOtERrCRTeNn8P4ssAdIykWupbtpaDRgqfKg49_PzdqYP9dRbtU4J_38a9V_Nij-KsQCUzRiOf1t92KUPirAT_8TDuJEg_omRM2mojgMhmscuvfQCqxaX-GIPslIOTBjXGdVZMcvy93kdPIUlpGitNdkq1PIO?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2Tfqh6fPTXablwqyW0aumDfT1vbZi0tOlTw8zyic1t9TBd5VgAbV-iJFQr__ZokYoN68wogthHIztdFaaAMvy_0_bDy785tgLNoJExASszlYrvzsdWkUqtw,&b64e=1&sign=e3e0790299ae79a88ae399f81404af3d&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 589,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 17,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 3,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 33,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 536,
                                percent: 91
                            }
                        ]
                    },
                    id: 245614,
                    name: 'Kinobay.ru',
                    domain: 'kinobay.ru',
                    registered: '2014-08-19',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Дыбенко, дом 32, корпус 1, 125475',
                    opinionUrl: 'https://market.yandex.ru/shop/245614/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+74951336289',
                    sanitized: '+74951336289',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-63mfMKlMSfoPTLGZzHgtuEo1thSROz1SVIkMVMYImvP110egtCCuULvBLD-UYTeqMxQ12RygaEYkVkBPqf0Uvbdfzc36nhKZFrjexEaazpLxDeZLPjBipW_ZnlUlOD1sxeZUcKgQh1xRSiOEro-67izaAh8SSLRRykECCo18ZMC7xptR0w4L8qRFKaunTF-gP5WojExfDtwIqL3ZA82PsMr-bEvYBs0EbWyDVo3IPFL4tM_Nm4XnK2msUQRFHHcRrzYkHfjvJGnBMOx1i3OB-V-yW-ZSW5ohj0edoqGXDDBMYtEueoRxDQJrAiKjoT6iSxTnXxEZZ0L7dIaMvip0ZBy-YTNOFCcBIXTLH1v7v85fLen43UZS5VCexwSFgNFWgdGJVzG5y3PS7lbUKRiB7sbkmYZQwpzEnFfwBZoMrNKEZFB8xY243IUMj-ZDy61_97Z7ukMQa9SRUTTKcxWbK6NT1-6Ae-uL-cD52eESwls9OFQjwH3rM15a3F9REyPGH_KcLRmfOThrI9gsOurdJKD_NNkuZc6LHK8Jd4lT8nTeQHGWuMvN10gKEQib8gPFEgAIr0-h6y3ACH-ifIpv63qHZFlpjaTpbpCU5Si3_CN_vQp9hUQE3Kb7S6lDJEKcrER4VTleT1NwjBqFeF3HXorpjIAQo6px7vRoMQ6Nrggib6VfSnWXlC?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8eT4eg0LTxjvwHtB26g4iBQQ9Wg_EGxqhFm4ubH5uZcCgtOzzQbp8z0Kta43KpZOT9JOblhBz1CZJle4cnZSNRBhXVpr84Fzs7nBwtvPEtnQHutkbiRz4rLBvorTRhC0iARKKmS0X4DgTphYJgxQ08TSXfxHJEt3LZrXVeG_0Gcw,,&b64e=1&sign=2f691224623c32d5d0142a3f5f113058&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_ygT1sKRZrdcrZO_jfaItxA/orig'
                },
                delivery: {
                    price: {
                        value: '300'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 300 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'завтра • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/95p8QdG0GsZRO-EbcGSDgw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HKRFOmC3sw1CFkhHJ5mIPd0hwY2ps3nhGXV2OJzS2pK33Yw69Xk_t_AgZ7oM5LBXLCXkWPbO7TjOnnlgVFBOMlHZ609j77sd-ZFh4L9aPUEercLZY9BE-dcziVIqQu4C8k%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PDrOPCmWRBOgKMoMNhCLHtExi4sZO5mHOJCUgFQ9F8fZ1mVtiUpR9KVKkpPmYdWfNjV1OyvT72e3D_a4GA_1PiDRv8oIeQxW4W-dOf00_jCG3hyl7j6uouIKNpb-3bZ0az9ihFq0dlWDaTa8G5bAXoJ8NSuG19tUjNGncTckaFnR0stI2IuOyklduvv-iavMI6K29dQ353nUtXiRBoqVqYCvONttvub-vSQ7XLpKlVh_aOhSppYSEOXIDa-_wmc7OWXp082qsFG40apL3087_Qa9MPc4-JNmaIKRUDWqa8LYxG6dcZwQd_t7VrhaMhvg5SsIgnMX5Re2U2QLFDQ2QU-kpXmISS0bGLk48rT-9CyS_ke6OieQ7Mc4UxEZGUJfDUAxrbR8S8GRLucbNS1-akETKAoLijUZ1dxY4hXNVpxdAO9w5hRgoeUKZan-C4PlwjCIKWtUkspFYf6t7YACqtLLCoZWqaTG7li82Yi4R5CK5RUOiHcrxc6jZDtoipoMPG_s6xOZNW8Nybo8LOgqLjbUeVBZB4XgnbR_o13HIXOvj96o7XeJznkRP1usz6fl29QtFapcdfQqBid7mDA-vN8P2SafUUhatkWC3w72ZinUIibZX9tOTaD5yBykRidoshgGeG3ER1sYGBVx2FvAmaLcTqN7g8b8GPjKTsYQRWpn_42lM_ltJ2LHn2PvsqVjNYMobN1sFVp?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXQ0NtgSpuUdc88jcHmGSKsocl2Dr6x5cQeVxcN5fLc3Dj6XlWEbJWE-UDtdgnS3lOEvKYBIxcRA--mUfhz8Olg7rte72JPcRgKkgVcQJWjFpYKJvrAEGmSmKAud3Bd3VvUG6Bsbi6LdIn2p3qYL_-hNyWGYbBB9McUHwym73NgLCSNOdQLdYR_hdKrKa7HkMTVQngOJI8NLF1C-x7ZmAkRPpw1HUqQmS9VIWBSDgi-dP6EaDhgjF7vxDw_fD_jpZnqnEiZSnSXAwBYSiKKxJpb22XjLrqxYBpCRavtmjPbTm9p05BhCJ-aA,,&b64e=1&sign=42acdf8665c7a5718c81e43e04f85476&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=245614',
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_ygT1sKRZrdcrZO_jfaItxA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_ygT1sKRZrdcrZO_jfaItxA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEhA-zXVZ5NdpqbzRoYUKIbaREveOCNXttegVPr-ITYL9yc0tC7ezI_6RzeaFrnrYCZtgQ4BMpPegnDqQRM7RYrT1Ir9fT1omW04MzBbpSS5PyTwmo4LeTDHIT9lczVKKDDmEuIWJsRLrDfrKLZ44Aey9bg2Zy2g6_8N-OiezEA-Ekyu3IhFnCc92PAGXZ6uiwsaXxEfM_0ou5M_1u8O4J0KHHXSe30TB1ejUY3PmTSx27aJNrRy39akW6CgOl-5yJ8goCLaWPY79DC-lxVB3-VtM667I4H7uc',
                wareMd5: 'm9DKeJIDKU7PjxyZr_eZkQ',
                name: 'Смартфон Apple iPhone X 256GB Space Grey (MQAF2RU/A)',
                description: 'Гарантия- 1 год',
                price: {
                    value: '92570'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-4mW6Z3uIuuA07Ek-SJfYGfMLy_c5JpqKpAfjyjAWVSwZp5XWvV90SMUJcXL9-u4bG5VQENMQIEiPFYJK9gB59CE8A7Oa9UWVRlswqSOy5W3SHAJmFirvg2F8C_XbpfNUDCyQSd_VcI2fY26oyc8OTh-ujFg2n60H8wah9KNhIXoS6A06AoUaXwXPZQ_NO5PeaV9-mCqbcP7aU-6mfZ_uTZzESwV_Y1Tmv8tkdmhW9h3d9Hi-XlBbK--RhNHQHzpPWkZiqRgtN-aLuw0QyviU-fIC6Wr6eX7DC-GOI_VsUU9u9-MybHn7s4IJ5HpUrp3dq31f5mEjkRaMS4Zen5lnoKx0SZ-jd9pWLvWMXPADCUqFtmTlWO6ulXbw34gf8FQRad3hZUA0AB2inLl83Hri8Le3hKR3t0NdI80--3BPvn3GafNSdFRm2Crc4gCj3nFxvVL3rW5O5Jt5Vl2ujOOb3JyGgE929sS5GqfnuuTTsiH7Arwpv8cFNpUdZ6dmPs5STlp3TeBnyzWwaoxA_FBXbyxknwVMWRXunlEpM9_HAyCo2kUzRDW8vnWuslyzi5efIjMhcdwVZdeT2CPsKzzUULpLJZ_xIBF-LQKB0rO3vTNbcULXaNOD4g5nmMhKOMGJmPtB7GQjOjclDKmIp2Gq2H4U_dIbGe4sHOrID_BYGEMNbXjwK7zYKG?data=QVyKqSPyGQwwaFPWqjjgNgv4iI009xpgCq3CekWjgfOyHxu350b62RI2engJKOv8el2id9Vn6RNI25k5-GkJSatrS9p6d9qQOq2kCxoFe7Y0Q85a8YHGd0RQcTeuWMkvXo9RJdaqs98AIAM_41LM5gEYTPhFlVaFm_af4OWhOQnr7JXjn7LFr-7yHnN-isbC0B0fLKq8SsLcM8XRRm85Jx4pKQbKe06wWym4k2Y847HngE6qIqVcEGxAUKPgmLEKlCGOshFR5a2OmXXJkYoJCN9jxv6hYtgMrL1TwmNMH0-NTya9l75-Yga2g1If1xKqWsftKjdjjFHejbM8rGRMtXcGTADMdFDOQkEFcqBMS4lnBc5u5uo6RxWjz-jmjxo29VfO2D8gcbGbA_Q2-NFKyzc59EPF6g1nUtqeiQSf-pRX_w8ZpT9MqQeMyxBSk3WEjVrjnQu9N1mJsK2TCtthHeTQVQvlwoutgtwHbV-071o,&b64e=1&sign=4c48bb1f8c2741a495e6264c64585cbb&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCIuUwUF7cHox4IE8nmKF8jBMVcquLntQ_qAlEBPajtVXkj7v7gT3hPi5uKXkAvPwmB0hxKNnHVIBTyL__lXZbNCn7hM6HAe83Hv7FRKdZutPPj0Rp73mmORQtOkbZXyPUh-CYU2y4OVCmObxJ0_Q-K8p8dI0-Ie5KcVlo2EKXSf_08PImof4A8Gv-ali1VWjAZEtLeOxiJip1ji73-1k_YZ9HtSYrCUEOnFcR9FQ2nWK7DIuMjCzTDZ94h2mR2cxmyYkFrxPcfLnw324-2Tbq3XrCNgBa_kVejXHKB2TNnQOEplpAKO2ZuU0yq5NY7U89Hcdcb1xRAmSqUKSugc0u2LudV9EJzM8oUfYBrj5K6dUZbGwdMgVAw-v3M5C5wCi0cmGsePecWxe_4n4TVeRh0cnvm7OYij4lfewPYrueSxO56TU6kVvKBp4njkDBQ5iVc5Sp5_wvC0wNhea3K7iNsdZkcS49OJ8RFY8qeLKhedw9ptzkf20jOvvaXw5XUDeyM_QuOlwCPq1To8b8g_tgfoJrFVxXMe0GGgm3_Iy2B_rzM2MrnGDaZ0vlbw5UAJh7tzIOVX00BqODoB_gZ0kjF0eidN-sum5ZzJJsUxwCkeuy9mav1OcydpdBMQp4xD59X9kMXdtpPLo_f_r3IWNq2yry4EiptPJMv-mA_KoOqnIRDeiT_YYODeqKKTRJll8c_oiNWIE953cAPqJFY4t-aVEFxDsyaXbMfXlgzfVLLwVAJ5GT-RRwDr19buw-63aycaUNGOXvI9n2jbgmoxlc6SDxq-sgu_JbWSGZ3RlN3DHPx5937u8yYbvuLoAXZqarWSCQHyalXxf9xsqlQLOifCOtdprX7rgWqeTGixQtdQv?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2Tfqh6fPTXablwqyW0aumDfT1vbZi0tOlTw8zyic1t9TBd5VgAbV-iJvnukjfkA1KakyXaLpGNXAwcHjA03hIHgRrPUJhNlHsHXKM-tsVFYB-nMQFBOIJ0M,&b64e=1&sign=47b2e194034f2da95477727d86fb7eaa&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 589,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 17,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 3,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 33,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 536,
                                percent: 91
                            }
                        ]
                    },
                    id: 245614,
                    name: 'Kinobay.ru',
                    domain: 'kinobay.ru',
                    registered: '2014-08-19',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Дыбенко, дом 32, корпус 1, 125475',
                    opinionUrl: 'https://market.yandex.ru/shop/245614/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+74951336289',
                    sanitized: '+74951336289',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-63mfMKlMSfoFDrTvxrI7DhokjHVlcUycxJ8uRmU5rIDntAytL40kMzMUtSah3fbJaQ0rAGhyIvQEbeIHaAA2mMXZnbjn64SPqG74sMudXnNDIgqzVkyvA8GsGM4D5ZUrHNaxqPvf9QzrMUGMOlB4nA5bO6i5HfILht4afneFCwucdulHLaYOMFngLatlYn1SWQQphIqVSffGb2POKYaykKyJ1gfBEb2hWOGXNPJHP9AChtKLNVg2pkJN5F1MBRipsbXq5NbPwr6KG8HL61xxsyTRrN2JxfxS7yLKZ1b7o904Oufq31jHo0DG17x1z9FHghuKWVrI5_POlDkjG6_PY61nJEqgPo1EGUlbJI34-T2-7m8_1yjK1JOHZnAo-P94dngxqd1xFt5EHKRyBnL-mD8lKYf_59vcmXpFVmSBsfJ-LzcXbY8LERJbcRyCLIog2md74sFi2s1VvHkUO3lzBIg5hgop1SnnxtEqyV6BmJmQvumB69B--lISaiZaZZRKpuprwXqdwlMGKlNTQfM37sEaoG4Xcm7-pXQZOkTicUOqoKmOpn4NBeh7u4upS6Z_hbNlhKlFnrOHvelIHUMEmnEysxmMEaFMCWpNTDXb6C345lXYpHenx2F14WzbLlN_9ULDEGa3DQDdUwTNE9WT1M8L7v9zDvrRfO6608qO_6uZcDGPkuUguo?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O97ROaTcoShpkFjBhK8H3xgs3Xvlp6evtAg91rYMMJUvR0qNuh8ubzppbvIMBYCETJoFfnaZo70UAMuxNFRtjCf-zzHCOKxt0aRrONWEdl_8_GSZBgk27GSaLyRuAbPtri7Hw2ytNYfGouTKmLBH6mwHfzqLbgtt51qFmwCemIaUw,,&b64e=1&sign=91325cea43f0caca335e0ec10179de04&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_Gmi6ukqIRj29pyjiGqT0GA/orig'
                },
                delivery: {
                    price: {
                        value: '300'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 300 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'завтра • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/m9DKeJIDKU7PjxyZr_eZkQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HLBVe52KHU8yR7AdMvhCSGemkyF4kbQkHLTgYJFPbsD3-HvWNi_oyj3nthsWY1nU_2t2aJXWXbxyB9iIpH49WVmxPvD02lgfZrdxIMXejxBy6WN5WvexwAf2KhIwef8z9k%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PDrOPCmWRBOgKMoMNhCLHtLikowjbrKe5H_YsB0aJjZnrMaWFhP4F5O0l3QypLVI8Nf8tvZZe-gTRvFDNTi16D3HYEw9f5Tyrlv0PjBtoy_BF4DT-3Ym0Kox_kl1O-jmOv3QfCuDSbYI77UBNTnia81uy53jWj7vNhgew-bTRjyGXn_yq2eNKRAuZMyAR9Gzq-15epsOZ2yvWhi6mOLqiSNWcadn2z7r9LYvRhHIpGw8NLHZfiz9rKl4X0SStzqvJ3-ui-GswG6KdpJrkQQjg0IDwvotqVpvevZe7egZmsCmrUged8qsb_zyAMpi4zwoeCH4U2fplg3-8u9yccIMwzH7tEGYkx4jIqgjXUEQp3VLNEygPLSTMCjuM_KmtBkjMNXi5V-PK-F-vxvylybaueTLjzubot_ZQUa_cDpn4IShXcVdcM3PSmTwT2fdyW98FOib_lyJ3ofCjEO5e2u54rrGV7E5kx4D5XSWsqPNqZ1QWFjR8wTPh75BO8NarcBo9LTql7xPfZegCn1_LmyB5ayeZ6OXxwXKTyK8L8SVVWF4CgHHFmID6lxKvAeMl1LQE8GRyCegMbBQWYwD9Se9yliKyKssGF0NvwbZnYKqKmCzmJ1x0JbxEEG5tu7bEeqFBquuFHpaeAbrfzvZfRIwo3VECVNNQHp4JpW8rmTZgzHXXSZTxW-mHrJCXBtB0ePIkAmJVx4DAq_?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXQ0NtgSpuUdc88jcHmGSKsocl2Dr6x5cQeVxcN5fLc3DM84K7q2f19TYvNtm9UyWWjBQN9SYg_BcoaUSGs42lzPLqI38Wfy1AfSQkIb2K1fvBCDTd5AEDm3rw-ZWVYIEGXqctcIG6MCO4ABhOkzbOZN1VevPPD4uVDRsf05ecIQ8tMcs8RRwF0UsVCSoRHihdVVS9eW6UwEl90iyWp1cNjkgxKUAkAV1Lq7tZgClsJZ9-b2-bin9YOTTWNTj3MorsS4O58a3tCVuLySZe2dqAHWfY4fqXkXxJQa9wtTps1QSJ47rsX1AUWA,,&b64e=1&sign=2ed1946b990a19b6c8c051dfb6c890cb&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=245614',
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_Gmi6ukqIRj29pyjiGqT0GA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/250456/market_Gmi6ukqIRj29pyjiGqT0GA/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGtVBoScrS3ryV-OrD6FOPTfj7mOj_MhvZ8YqmthX8mGU3rMiMFdHAax2hrzBkgkrztxI7Obao8SItJOn0NG-OjmVpt-U_bSdLwQtncHLCXWXgb4JdLItq7KumMwrM6AJd0kv5YX4_jxVMWed_e5vo7iPlVzcJ5CgCDulgUyaCxtviE4QT4qRxk7jj-6WBdBvjLVEgqNm6_-LX1yLEz7-7W20KMM_trotUY3CqXFmnl2aBxkBbfnAaIO5Qle8vGjQfMrZ1kLASa4c2subFs3n0lzaVTUQJLMXg',
                wareMd5: 'atE2UVKPHgSUYe7Sv2sQ8g',
                name: 'Смартфон Apple iPhone X 256Gb Silver',
                description: 'Новый Apple iPhone X 256Gb, 5,8 дюймов Super Retina HD (2436x1125) IPS, процессор Apple A11 Bionic, RAM 3072Мб, LTE, GPS/ГЛОНАСС, NFC, Bluetooth, Wi-Fi 802.11a/b/g/n/ac, двойная камера 12Мпикс , Touch ID, iOS 11, серебристый, MQAG2RU/A',
                price: {
                    value: '88990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mehRahswkml8QwZMTPvTF6be1ihcOCeOrc8WR6XcZQF1rKDS_1uDjupf6WhHF6r8H1eJkCX7j8DwSa7nQiQtxWaaY6EWzyNyVs4st-C7dmfqYbSZWPGLx85z8VGjeb3oHNJFrE3Th9oHJKa0tNPhKzVbGfJm_Ta7TOpKQiQx6iR-DxJACk7zB0843OF0x6EHTAKQe3xJT-Qhxm8_QJQYmW6w5YoG7P0LlHxoWeuNRkwgSIfdeSPxceUuASCXQK2Bf1srSJCWNyJvNYN5szBzfQRR420An4myX2WFVU_biNXvbmuZ5pouuqbGu3iuH6ymU9dyyVX389rE__xCn_ArrfFQp2PF946pHh_ONk1zmYPV2puLP_7S5bLJo9xinD0B0hoZ4Ot4JZsgKy7rt2Y1A_0Hfz4X6yj1gflVJl0W7OR_ZFVMMTEGrIJn27f6wTaaWp9U21iyjyPNWA9w4Y9YHTG2gWN2vw4kKzj51dTyfWDy6NW3XJwzSu9poSBZgmDvK8c_i_kpsbDD-9LalJb50xnTska4TROxEIWb87RLytpFIbhaeafBIyFfBoCfsDlD5vXQvt71ksfFHmFiKvnrjfL637hN37f61DsDG2iM691aZ8G36cQkT9FQJ6PCgXyOM8DBCoOU0PqqAIAifSnuq-95c7LJIawS3eZ5FIUbOCRtR66BZYJWFGUIuhtC8djYrg6iUEHr2Ybt8l9ILkJ3TxrHkk3zxEqv8HMV62PIgT6mYoFHsC2xNtdjPftJzx1ld7pZlBIcc05lHRspt0y4L9ajviCmireZO2WHiWAmNXWqQRHRdYfVSQz3Qp7T5BsOYJD9fDI_Mk9Q42qPV5iLm801y5QGgLQ6WpH4hc5Pebnv?data=QVyKqSPyGQwwaFPWqjjgNq31P5uLbHikJZEo5WiNoG2mS6qG_3OF35JHS79IeerLCuls9ZBZuu26PWQBanieLDTVOzXKkUNlDLCu_qjcfxPM_2C6Qc3dl1DJNWr37Q23UOCPemjic9UO3V1EbNGArkP6YX9nz8R6WCs2RkTW74xPE8JWPoL95BAKAH_tqnsVG03SGA_eWng857B_pr3DY70tJYJrYX9bNAI_qEcKTBOMedCy2jiCGVVRm3KdlqSaLRf7w8yor9aNxowjUjQjSOfIviCw5m59ugYqraukmufPKRp0OyVSB29kACNBpols6pmMkyJ71y94kbzfVVheqeoB76TKzG9hGO7ZjGmqBAAiUdY2tMuPnRCvRr133G1lSTh7HCJFEGELtBhZNc2WAbPHJen1Kc3_NtTNXPIpggOG3Tme8lie-fGanfGOWSa4&b64e=1&sign=7e17687ba815c20619f3616e12002f96&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iQKg9ijkV3IR4dMvzpGgKMyTqIIo6gwsm5HX2hFcDzx0cu-zoWV-1csjmuqpttuI0he5b8BfH_8YwzXKujms2ulSz4jDZRJL_hj2mI8-sFwhIoc3wUdjIPrSBJGi27XS-RocyKHqVcBz9xVHS6Ji1CBKolbNSMje9ycLTVA0glzMIZ-WBPc5LKebUbHab1yrpugmTvUrv7kGol7BCjALvILgbGWojZMUqyza4QPbKBzd_tdGk8b0vu1JkQmU4rMzxIbRB7AzEfr3wiO3XFRncEdqOzwmzE_0sfwPohpG4M0xOko5jFyVXq-t8TcXXE9EwyZnsfb4M_QSub56HStM_GU_Dj40oeJOwtSCoFE1_2X0DmM0n3zRkMRINqRvxT6fk1GuG9zIvtD7vQMrovL0FywifXOc45bPX6VYdlQazGvnTAymr-n22a2Mi1XdPm4qXGfMgC3jDUPyj5SzY0zVN70uBTikJbB8PhlyUg-NUZO5NUA8qNF8dPsekoGlxVI7MGSf9MgYHIg4EbZuw29VYRsEQ9pA8BvOPRc4t2VT3gKGD7WovhXfPdy_HctHTrdU83qbDxjbIuXH1dlGHt1i1PoqXl0zfSwjLPS1AmZ2zzW4fivkUExN5T1jb1ig3ZekYwc2BoaBIFNwM0ZiVVFojZ1FnPzhuWlywhux83VZBvlb1oKL2ehHQK-Zy3IglzKwnDFHxqU3URfiU-43-TpPMJNwAr4Jp4q8tN06FPiFWKw1E_g4x05lSShvvTwgD02VJpNIB9fQiI9uDDTo2MTziV9ZghRTpZ2AJix0zu-VFDBzczTnokfX2Ehu9BqYOrBWYfTyF767bjMEXpIye9mYaQVo6xPya8vOwyAuRC0W6FEq?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2TbkMHEibMcglLw5MngND0HIVH9-Brm02ArcNk90MyssAtQjrncm4IkZZp_RYJfrNr7XV3fRKWj4XEGp-HIcv5XO5XML_nDwWG1s9gGD1aSvu_PaWqaqokY,&b64e=1&sign=956b23c4bc47229e7cf72e140415c3c4&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 4,
                        count: 2460,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 291,
                                percent: 12
                            },
                            {
                                value: 2,
                                count: 83,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 57,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 168,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 1864,
                                percent: 76
                            }
                        ]
                    },
                    id: 350233,
                    name: 'КЕЙ (Москва)',
                    domain: 'msk.key.ru',
                    registered: '2016-03-31',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Санкт-Петербург, проспект Энгельса, дом 124, корпус к1А, ТРК «Вояж», супермаркетов цифровой техники «КЕЙ», 194356',
                    opinionUrl: 'https://market.yandex.ru/shop/350233/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                phone: {
                    number: '8 800 5005 074',
                    sanitized: '88005005074',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mehRahswkml8QwZMTPvTF6be1ihcOCeOrc8WR6XcZQF1rKDS_1uDjupf6WhHF6r8H1eJkCX7j8DwSa7nQiQtxWaaY6EWzyNyVs4st-C7dmfqYbSZWPGLx85z8VGjeb3oHNJFrE3Th9oHJKa0tNPhKzVbGfJm_Ta7TOpKQiQx6iR-DxJACk7zB0843OF0x6EHTJJmofah5rI0dA9gJuDj0kbyRQ0dosQ6TdoIKod-6fWYWRL5YJGQMjEjglJP-KHBSHGyccIG5EiGtd-mG8HzyO8Vo1W198aaAdYqWQMpHOrJK6ZXfcYVx2ENcsT9J3Lg3ddun2TVGtZaxgIh6xqB5r3IohNOXXzxuzcSnG0GK5dmvnjpBkAYRXtXlHrMOK9vv1gBlBDbFF9-DInOGBnsGZSpUHmg1pnPjytDjBp11wjmfaTNPgVYEDMZ6nYOLePd12ilPzLLgH-z2UaflH5kJdtgnM2hyTGCD8HQkPisRtMqvNg2rxHEXilM3zUnoRjiUXFmaZK_8FAYYheIclPj2EysqLzvB0OfZFwMGJm9amM21ZBmNX-hePeysJVzNDjgEzqgj6qbu_3iqehBve_OKdjlxCT-Gx64HE1rBNjoPpwJ8--5TMTEKmS9H1ofXCRhqHcdbWL2IZ66DL11p1vFlttFdjIia25cepCe-ORAMfPbkfqOX5ff8eE98ghBanoLPYBBNCDnQ_8SPxDVDVN8lFb1_9jfnNli7TcKcuUB7wKhoNK8LoXEPywG-tcuVMpbEL38Vtnuw_6LzFJN-EYBJjoppySCsWtOmXEfpQmRRrRd4sjtgG9bD0bdj5Jc68vTOaJBTYjaeB6XmorG5UADuy3AJcO_GIQwdtIXAOvLemAu?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9eIvsVOfzPDPfqudDaLN-eDXmpSIJIr-Uap1xe6KC2qXyU3OGZwJ4zpTCVIzxAVdeSWuCyNGsHmX9oztOF91wUSOGd77TXOAJFBd3Ix3TmWzb5_1SFQkt7SLkxk5fcy7mttqQTGbIXYi3T-6zH9xhiVXiyrxbtq8pfo-Idt7-kcA,,&b64e=1&sign=c276c9efe18dbffc7bf596dcdc24da15&keyno=1'
                },
                photo: {
                    width: 320,
                    height: 470,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/173932/market_1f7iCYheBkKYIC6DXoJv1g/orig'
                },
                delivery: {
                    price: {
                        value: '390'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 390 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 51
                            },
                            conditions: {
                                price: {
                                    value: '290'
                                },
                                daysFrom: 3,
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '3&nbsp;дня • 1 пункт',
                            outletCount: 1
                        },
                        {
                            service: {
                                id: 51
                            },
                            conditions: {
                                price: {
                                    value: '290'
                                },
                                daysFrom: 3,
                                daysTo: 5,
                                orderBefore: 24
                            },
                            brief: '3-5 дней • 90 пунктов',
                            outletCount: 90
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '390'
                                },
                                daysFrom: 3,
                                daysTo: 5
                            },
                            brief: '3-5 дней'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/atE2UVKPHgSUYe7Sv2sQ8g?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HJQTrmP5ilsRUk3eYG8FGFb6BdXgHkJ2dIiMKFtqJEOIsiYXJ7QsZxBS-CWCYISC7GHNWhGOjgrl5b6_qesijQH7BJay6nv42ONkRp-RPy3ydJ39SDfK8zYjUakz_Pc3_Y%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PDrOPCmWRBOgKMoMNhCLHt-s_LL7G8H8DwrXVD49V3P3fTlRGNiKECvyN05NEWBtVwSiu0y0XvSjs3x7RLqzHuege11Nd1IuKH84rLrY3A-CuhQVCYkTIB_R86XmDuA0hlloOVjHtEO32jPxyMWn5C4bb19O4Jttc1kZSakJhl1v0gy06XIcdV8o7An3MBBuvYwh6LmLjh5esOdAh-y9PIxr1RW-cyzGDZLJSJzKW0fjD2y2suQYkf7YlapEvUBvGncj7TFW0ewIuA1CUXq2lV5t7-CNx6_jArCRhTLRtF0LolngVqB0vwd_0bcUHqhl-B5U24xFYmUFBsS0iu4GRhyejHTwxfUdp6fuDvDWnNOAqdmcnULVsFKvCn1IWkPXM7heDgW9VxnUH020Z_HGQXliPE6UdVcpEagRCjKzIayV1OSTS1EQNDc_XjbpSjSGw0ZqblwDIXN_H37wPlatpxMM6JwRPJOx8q9x-WGaYL9RGHPY5pIOp6eEldNmuDi7K3WhoiIX8-nBLgku8u47uR6NuTFCSLiZtHvp_vume5oF6TtyahuJXF0oshobJ6xOvKTGg7xDuk1frE_pw9-_TGASIrw4xyuE0L5KRm0tv7j1YEqVLXJ3pcUc6tmWzCpzwPnoxsbXubT6Nh2Jtv2fhdrcDO4bj65HSQIA4we9mCIize-tiVCEUlPrG8ONFmStXPxX3BjAvL6?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXsOkEm0m8pcUeq47zVwUcYkSpPxQaCEW1hv6U52asjUMktOvExek-ixEk2gloV3xDuAqDOyQt19Rj46__ZZeb0YaQuBj4r3oMkzzIgRILDo1R4EdxtrU79PEU7LY06R8UAkyX-SerjbQlhdcmidLqaKksexL-Zwv2p1VddzcbpoOb5znNA0rEmRxwIejzvvOOmLjfwWGFjOaHJa31tCyV2OoxYN8v2fccitCfGK_tyCYvOPiDuIy7WPHY7mHlTlqqZivW7UuzVEBaeU8pHiyiwXNhmGkhA3kPZ6lxXNBfiW4GpKkHaPwbTA,,&b64e=1&sign=0031461d8bbac2f89a8482281f709ca5&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=350233',
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 320,
                        height: 470,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173932/market_1f7iCYheBkKYIC6DXoJv1g/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 170,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/173932/market_1f7iCYheBkKYIC6DXoJv1g/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEnSdCCUDL_ajYCHm62IcImtHiBjzZPiu7gdGYzrqppHlbp8c8ws883lJWsLAQCBwC-bo_cW_BUfOsMvBKTPQEFfGcqW0XVc9wRVe9IxVagSaZKhamLPRZgdB6N0O61WURXdnczTL5uC8Zql-3UMsyKVwPY65HahfH7EVz1VbDskG7YUEDtjwmUHwrHWDD-yX2uMmvV98Jj2hj8Ecd5znHRjS8ZQ3vXA88ClRTtPnV_KQUgqIAMAGo7Kvc0IzBoRUUhHjwY-o2hyrFV0g0ssuo0WiMbMqBQEZE',
                wareMd5: 'nlZIEPv-lsjvzk2lnLqFEg',
                name: 'Смартфон Apple iPhone X 256Gb Space Grey',
                description: 'Новый Apple iPhone X 256Gb, 5,8 дюймов Super Retina HD (2436x1125) IPS, процессор Apple A11 Bionic, RAM 3072Мб, LTE, GPS/ГЛОНАСС, NFC, Bluetooth, Wi-Fi 802.11a/b/g/n/ac, двойная камера 12Мпикс , Touch ID, iOS 11, серый, MQAF2RU/A',
                price: {
                    value: '88990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mX4gT5L4EZsyT-HVHBMqKqn0i1A4-GObLW1s5W9owhbFLcI2Ux4HIvdavfZSo8Gleebfp8cgiEPLcvJpwlkD_tOoXjwzi9oeBPnfudAYPyAIGO7FNgJmRVrm9QbEUZn-P-P8_6iYHrKzXPrkzpoJAXxv3xi6Zxnm4uDYky14V8J8o80Cm7uvoYlP4ytMlRXqVtFfKVlDf1vG5GP866t4-b6ruBmvJFrn1wsEQjjnLFrs3uANYYq3SVIEVj0UD7toD7j2bgUzo_63hv0IEmEIk9_JsaXenSwgHuNvgJBS-P9eCDY9zS2T23IjGJ8WCeC-_HoOJqtCe57_6NaqgdWQrgHJiyOBAyHATbNVUXXvv1l54pGrUy8B4CQc2Rg43lH2KfDxCOHjfM9NzNuoBMWj0dFuVx1WeYS1Hw6dpRLqbWPxkRba0kaXFlgDzgsJMDBDXAfUGpY_nSwCUIL9xrkowYAi-8k4GZ8DMW3CsqPlbDLZg6T5UVGpIP-yn86fDOf0uZM58efIa_igUt2-h2b92SCGdkcInXXjEoijBYY2NlXHLS0CFuG8dBqrHVOkoP3nEB5E1P9iADbCCReTo9RyW3d5k3ez-0nUO1CdYy3rF9BW-e5YgFNFQpCzn70IlFdMahZSVU0t4VSuWSEtF1zoz1ncV_Q-uL0oA6WJMo1kBs7BiUF5JcZCK6xKo-uX1K7dZwCJ6cCi-JtvFSYyZuk6SQ0Yith4WW0hwgb6aDxBeoJp082psyOkBlAOO_0CjhZOzJefZV8dP7hoKjtaRbVNCkfdvQAA_7PXUte9EwZonM3Z5iWQTdO0lXgxB7OwtFDIMVGKdmg7YWxg3EWEmA06vbGAim-sE27Z-hZrGhqZufuv?data=QVyKqSPyGQwwaFPWqjjgNq31P5uLbHikJZEo5WiNoG2mS6qG_3OF35JHS79IeerLCuls9ZBZuu26PWQBanieLDTVOzXKkUNlDLCu_qjcfxPM_2C6Qc3dl1DJNWr37Q23bccBe3lV9NZNXWrP9yIrV9xw7mbxba8M_wUSzA8chHbPaOPQ8soeRIvv67C3Be_GZOw6SvCtE3hzqx5_PLL2XhFZMwLhWO9AcWUnChpheM79QK88EalkLFMpEOE8ozyynvPJAc2PiJzsjjh3oNILoOLH5RdMzNYIqePXXaWIRS7dQYPDsQVe4PmNohAorcPt2USFdbafpR8bhtR4GQGmoRd9FoMH1A1Lnkgpd_RRVxSD8-T0kn26pFPjewPXmegYeW2cCRZ9xHQuyOSSq6FqYaJlYKGa3sPabkHn1eNsdkOVi5DFZ296OeliVXYBGuYY&b64e=1&sign=d873084cf962982a619b604975b4a192&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqw2dUY23spcGbDVrv9fjlifZaUGIomynj6VshRSHfQXE4-NYH74uGjiE1UvkFm_C2owms5KTepy0-BUT7rPK5l7SdK1Z6CCG9Zq5tviVsLUmg0pWy7k2czyP1yoZtpkMb1nlULjatMU-J9d8Uz6fdADDcaM1aOl9tIxWb5DINEDMrS2Bvq9Pb-WeXlYwBxdfQIx_W4ECm4oFfueSmaauy9fT4yT4iB5gP7rHOxG3LuU8WmV-Ms92zCT-Ng3Sppi9XYT05mNYR7Nyulr1JkiBK7J3xA2RviaMyb5Lxu-YsQhjQbsPvppaQ_arCvpFLKrioEAIZYhbGvyPYfTqiqpA0unKhjXY9hLl4NpU0BCHmBvPm9N-FwlsEPQ6YC9GSvYRr5MaYvoqydpajN4xJWb79LDolqWdgBk7osiG2dt_JXPJVkeiKX50K7FfXHmlNZfSM0M-v_bc3--vXXCN6c-reGUZp4RUY4E_fAPhpJE5HHwqrs_MN5umEk6ImmCc_0RM4BzikJaRXQOMBbFlcVUERWaklwu4TxoDKLi_flNjlzvgqfiVzwCFy9wWRQm5Ae0VG-B_jksnDkZnIxiMr4usnR1AZdk9rHVLFpecmeSVMR7VbM1-7XIzJIhShAn1Pq7kmtyovEvFaF2FJZv-NCghYQNO3K63b1G-CZJ1c2of4ic-_KoBneHidN5cQsQmEsfBvz7PKif96Rx3cLYl08LfneNQbDPkMDU_nkG7yUhXCMXv85thRRVVxDg8DYo28XejrAmRt7PPoov1s_8ejqUBViFk8mcWWlW69KB5bm_orpOvn_TNrumDqZOKU5--4gMcLPiD0tIu_0WM1N2ynzg-Cw6s63gA_WqASRLs_LH4_U7ESBOrpq-G4?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2TbkMHEibMcglLw5MngND0HIVH9-Brm02ArcNk90MyssAtQjrncm4IkXwIIy8k0ZGpZUUIMbQcJOqnV7GjQX5dJEkYdtyY1xoej9rLJP-L3tVN8JwvZduzM,&b64e=1&sign=a52c039d651727bf9730ceaca9b772bc&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 4,
                        count: 2460,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 291,
                                percent: 12
                            },
                            {
                                value: 2,
                                count: 83,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 57,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 168,
                                percent: 7
                            },
                            {
                                value: 5,
                                count: 1864,
                                percent: 76
                            }
                        ]
                    },
                    id: 350233,
                    name: 'КЕЙ (Москва)',
                    domain: 'msk.key.ru',
                    registered: '2016-03-31',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Санкт-Петербург, проспект Энгельса, дом 124, корпус к1А, ТРК «Вояж», супермаркетов цифровой техники «КЕЙ», 194356',
                    opinionUrl: 'https://market.yandex.ru/shop/350233/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                phone: {
                    number: '8 800 5005 074',
                    sanitized: '88005005074',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mX4gT5L4EZsyT-HVHBMqKqn0i1A4-GObLW1s5W9owhbFLcI2Ux4HIvdavfZSo8Gleebfp8cgiEPLcvJpwlkD_tOoXjwzi9oeBPnfudAYPyAIGO7FNgJmRVrm9QbEUZn-P-P8_6iYHrKzXPrkzpoJAXxv3xi6Zxnm4uDYky14V8J8o80Cm7uvoYloihBMditc9EHm2yWKAoE7JP4vMMAUkE5YZOHpEobRdKGp816HfBVKRFPDLyUQqg0YUBZN69C_Hti9nOgBwgGIRgbyCiE7BE4LKLHd2427QtkicLomxROr6M_7rwI55HSN6tjiSlh5bqREDv2ZUBM8lZWsA4Q832hFZ2R9ZfSIfs8p1fQPB-IgJon1hsRvmRNVyExfpW5Hbdy9iJP3ze2D9LDfIE-YVl9jvP6w_e_AAniQjX7g6S1AXVmvHZjmQXDt5fnnWjFCfvaBVetSRpIacsUyr05pPkk6WjV2dWUnzFY9hFHq_l1omUEj-21Ud7q3q8x_WWijM7b4B8m_pGaeyR6OKwt9_pnXzzNh6zAO8LhcV6oMvPzJESuIcZUKMfvn603rIfoVs9gHXN5FocT6_R2-Kx1yaQ7LglArBrNS33jGQnP5-vMg30Kz73akZTtOp_DJ2m9RL4E_zZJvoYvrbJWKaye-bKKYGbvpa5KPEioEPLYpl39gnP3Zyrblw4i_tLNr4wk71vvwlorbZxI8CKOMC4QLsY61UyyutRmNJibg9EQVTHva02oqRVQoN-CZobaMz9E3X2dNqKUDivvqbQc7IPN3sQ_zy3UavhjfVfGVsRSB9GZoyyMX465P5VG3Wk0ujupgRgmqmdS3wSnBRY_-X4r9bDVPcG_Nlx-m-E10Ei-upjnh?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_SxHCWSigKW24HBqThhKt4qNZj3x9Ev9L9Oey-kFBK6w9UmXD4e_9uw3wTlwUBwAAWzoThi7CO7rK192aXiEDl_L43UFwoqoq4xxgypXa5HzvYMUHQkYsHV-11ywbn2lJOYmBmjoghTWh5OFC3RDuhBhq8u0WzqmBMKT-0QfDJ3A,,&b64e=1&sign=1ce1cc518ab51b5bdc18cc791d2a468b&keyno=1'
                },
                photo: {
                    width: 322,
                    height: 470,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_sGVO05BuMFFtx44JAcoCxw/orig'
                },
                delivery: {
                    price: {
                        value: '390'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 390 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 51
                            },
                            conditions: {
                                price: {
                                    value: '290'
                                },
                                daysFrom: 3,
                                daysTo: 3,
                                orderBefore: 24
                            },
                            brief: '3&nbsp;дня • 1 пункт',
                            outletCount: 1
                        },
                        {
                            service: {
                                id: 51
                            },
                            conditions: {
                                price: {
                                    value: '290'
                                },
                                daysFrom: 3,
                                daysTo: 5,
                                orderBefore: 24
                            },
                            brief: '3-5 дней • 90 пунктов',
                            outletCount: 90
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '390'
                                },
                                daysFrom: 3,
                                daysTo: 5
                            },
                            brief: '3-5 дней'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/nlZIEPv-lsjvzk2lnLqFEg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HIKolonD_tEOmIF-ri5Q7ojZ2In9dnocZs7fNdClVfyfpDb0pKPJxNhlj9VP1minr8elw5BnpQ-GbGzoAdPc-f5h7DGLSZoRcsJ-vVceyOIp44FseUPcLEu3mIp7ItLJps%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PDrOPCmWRBOgKMoMNhCLHutkP_kBxXOWSxd7mPE-RzP5Oj1LEVPSNKQwb8el7XZ6lqU6_khjNd-xAYvLrib77q3pJpbgKFY3ESpMKbqG7eylOk55Us0FWJ3Z5U61izNTKKtGjPFPcasD6V-ZtkIZvP9ns0cluhWsbD8Usc4A8vOQN7tzap3Hrx0uiI4Q8UGfgI7yLC1P1KxMHEiIE_WR5aQ1yFAqPKwXqMevpeFkvQ0v6La6WvXDCRcrLuBlD3I-QOUl9GYg5pdP3VOQ1H1FfVm3oc0_ZXmsmRtHsAxaOWBHtZDgUSDBLL6X534Z2_0y2y0a3Tiw4tuC3YnuaoHrJQ1_Q2wZ1HDH2GTauYwFP-N3dfSTp6eJKumq-cS7Pj6IBMwDE7chLQUE5DouGluqcCnb55qWKB6_2dtikrxIm1dPktbpaXaNSUtPAb1LXd724RL9NkUU25HgFq602OnsISy6110HAZw0nc7S_pZCts88jnDIiiZ-ORoackF1sde6xtVtmnPIODyw2ePvIq_jxiDKnilxw_6EEgLkQpxNucIQFm37NG9D1ebqgmJ4LcqKnuCdsyio6k87FdPsh6lC0eBLnX21KesJRXKbzwIslI_de3ChIDtfoobgBT3TnttxQO9b5xxs_HbzEuacW0ZO0c4EdJQgqB9644tICmb4x494xVvL09QQ1H2nUSNthZs2eU-HS-xjIIx?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXsOkEm0m8pcUeq47zVwUcYkSpPxQaCEW1hv6U52asjUMybFD2AGGQ_C9I7iOaBsAkrmy8J3oQnHEaxSeXkwtirePWbvR0469LnKcJDjfxVjbe4SX5zVuzWbe41z3N6Tsie6PxiEa4tAd-9cs8oEmrG1Q53ogCRF5h3mP6rop924RA4oeqkd7PZ0UJKeiYRhyPizuN_MMPfs0Kg1Xc8b4V6q60D9qUwyB8vlmHtcZpCAxzbC4nn4dDmhR4sqXkUhfXDzmuklYSx1WzpVnucoM4awk38_KqqDhZU-reKcHYfo7Qlmmb-31TmQ,,&b64e=1&sign=8cd4ec563f6da6dea672f9f7abe1f3f8&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=350233',
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 322,
                        height: 470,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_sGVO05BuMFFtx44JAcoCxw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 171,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_sGVO05BuMFFtx44JAcoCxw/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHLuZWNcw2e0r3J2pAhzuxHii4yNzRhYDBoZnqvvH5b_kXTVRtXxw05sOZGV02SFIeZxyWCQVEfv8v8viY8VSQDT0au8x9JBHFoyG3svVftCyvG6WkorWCJhZx9L3lNF3oTaiwF6NqVgpb9fy3S6Ul7a63_iWgc252INefHmurNdUx8akJzXbq-yU4ULayDl_Hlm6M0G1eUZb-0CNELRzrb3BKjCE6mtN3-nVNh3z0sXV02mBXUFVntv0hAO7zp618Mf8hHybeFmvPOmd93SsHTtZ6ZBh3f6G0',
                wareMd5: 'NYcIyNF_h5bVnTMj5LFTwQ',
                name: 'Смартфон Apple iPhone X 256GB Space Grey',
                description: 'Операционная система iOS 11, материал корпуса Материал корпуса стекло Конструкция водозащита, тип sim-карты nano SIM, количество sim-карт 1, диагональ 5.8 дюйм., размер изображения 2436x1125, фотокамера двойная 12/12 МП, запись видеороликов есть',
                price: {
                    value: '85000'
                },
                promocode: true,
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-4mW6Z3uIuuA2jVdy09pVynD7s6emrnqLMkXn3mQcvHhSkMGo0D8EfhvJFEBb7uwMnqOa5j2cCMk4lDYjYAKOEsgiQK1xmAVnOg1URn2oNzeVT8iH0A4UxOcGjaws2nMLXfxuaGk4ePXnQlJj2aIuRwbtY8RFn9Ws6VOe14LMB0Ug7a9ejX7q45fKUA5MPji0lZd1CK60faa9EPW7fDuAiIgn-e_FhsjyAL7XHIEoV_PD3jM5nv3dNrb9_dWwXfyZHqcMZg-01nK-wJTyp6E0Jf024At0LhnqbcYXWMfAYaLqUcdb_ABxRRpJsx70lHj_SgJpenuOvCrUye6RntK-k0psyX0xXe1ZmZQKs_MDGsIyOcXIbtlQITQdbBNSPen2zX5pUTWOGxD69DR93YAfXyWjJpCLjoOjp0AqEnhxqfX5uUKGp1Pkk3gt5QLOfShhI8KLVHr_x8VyWSfNgDQi8thw2EsR-kguGn3fs84wFNq3VPMs3ofRb_PdGTQvYoWhW2YVv9mr12Uz1Yi5-Bo_qJAhMBJQakWi5h5A4xSm42ESrRPqaQtIHbWMJzNSHeNsNLXr0I86odLP_aFzeKH-NsutdB5WeJe1cJ9B41hZbjqp9QBVkyzLCVOnhjpV-z90f_vIsAsCcbx-k7jWTVoXq_nJTmw-HREzaFsLvZd-SDowuOZXlwZxWR?data=QVyKqSPyGQwNvdoowNEPja0uQkWSxAKizEBbSXMNJ77r-RoTFYQC7lKBJrXfHL3BGSFR9X5idu36MKbWA9lGBstGOO2CDDTni6d9tLnhb5uEumLOkQu_uZbFk9ne50oRForgaUJruk813alI-ud0ltvgm2L7AgZ8tsbXM9RKBaQ9P9-R3Df7-JBOXlcnx0iJnczl9MN9BvGYL5hEp9JNPSsm7-y0bX84DZSAKpfNNuj_tqNjpqg1vqGG7IXRmmUkhDuYklblFjCel8C0OOdfoxCc5j7ek3XEqU1_8edCbp6V2kPtTtx0OZZKM6MjlrmrrmC75SJSSPZ5n439H7L13M8Nbp6lbMQ5e_eKo5oa60B7yOVvyXwgDH-u8ePX2uEP20IjU0_mfIwsruuw1pUa9ySEONC6ICDey6fd7BhbIiziUXa9lzYFkvNcuBoCz_Oo&b64e=1&sign=cdac6e153195c769032d2a2406e5e7de&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCIuUwUF7cHox4IE8nmKF8jBMVcquLntQ_qAlEBPajtVXkj7v7gT3hPi5uKXkAvPwmB0hxKNnHVIBTyL__lXZbNCn7hM6HAe83Hv7FRKdZutPPj0Rp73mmORQtOkbZXyPUh-CYU2y4OVCmObxJ0_Q-K8p8dI0-Ie5KcVlo2EKXSf_08PImof4A8E2MnKGvmoG3YI95EahWAmCSacBNueezMpBw3rPovPYY_TiXwywpG7EhxIc-XWPZJhuRouRGevUf6RIA89rng3CVnVh-Up21fy_a2_p8ot_hTOaXQGKckh561GihY7R_DPzC1oidlQPaWCPzC_7vuVkXZzoL628pSKAnLB3rH3Kh3ZEUU7bYS-i7qZKCyl4uDt0KJJ5URG7FzPncnyKIVZicE0OllmNVtxyZnd5wOktcfkv6ag9v776iRFYePE3haPds-ypB8VfUfnCBr711XMnX8xwsCSC08R-DkNeuxbQiklMDCQvYIYsj6CH_vsyNzIPnDc8evdYMs8e7S4afyT2Yp0fZM5oCOenNDXCS__aA4MncyWBo7jf8-njUPpVX2SsKzofqCKf7fSON5ijEhWnQ87NT2kX3HYmGrFs3hIZ5U7zagmezsJarah-BvupxH3ai7KXIQTiVgveEHA1VqwFrspMvpfqmKV2iLPxFPSnemuLTeGSXr47FUMFiOzcHukiexJsDHBRqdSEeTFUWn2l-HoG1459RNB5b1RSSs-jWF3hArjNFEA4J9RaECucDJTemnA5T4SIviz2EjVkRT0_FSmGY0mcvJ0PDILIeB-O17MLP1eCBTil77gJuKDMG_XdUnTN7R9JJbno5_22X_A0EpNC5ogEbAYmkEHLpQ1w2y0stN6H5_RR?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2Q0CtvRM4OXRGtSzmV6uMQI-O-cRWI1k8Mr5omBSKMoWOuxio6E2TyZ0MEQ-szjAxBY-JJuFCm8ESwZRPkrSFauFpviyNnruYggYbPbzmrtr0ExJrYBRgrE,&b64e=1&sign=016a472eb41f17fd91d037920ab1c281&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 981,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 25,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 42,
                                percent: 4
                            },
                            {
                                value: 5,
                                count: 902,
                                percent: 92
                            }
                        ]
                    },
                    id: 178623,
                    name: 'Club-Phone',
                    domain: 'club-phone.ru',
                    registered: '2013-09-10',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, ул. Сущевский Вал, дом 5, строение 20, (ТЦ Савеловский, здание "СпортМастер"), 2-й этаж (балкон) павильон N31, 127018',
                    opinionUrl: 'https://market.yandex.ru/shop/178623/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7(495) 789-03-40',
                    sanitized: '+74957890340',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-63mfMKlMSfoKHgPvU5QxxXCZG_hTw_-HtKDWqTvhT4yzO3HWlTfTIPyfX2VyI1aOrkKYhAE7ZuEGFajOlDG0y7Y6ltJsoR-JXe91EoJgAjX2fbjyK2VkDybc9rthYbxqYgVbu0KQ5Jnb9fJAnIKUrpU9iW-q33mkTPVNrfUFyUApxa9OcyrmJWxYSWLnKZMIVGr23ljN5NXRtxfKRWnx6A8FfC-NzjutcYsTKWEzOFOOuMaiyINHrplqXLhcvzhCoqOHpsLjhak9VaGsunYOLrm4hjsV_1WDBPZ8R1dnScl6DJ_3OSgRsVg6Ft5FIi0hGKT61_5WfQ-IxfjLxNjqVmVSk0QdW6-mFd9jCN_Pky2th7IsDYAzz_Ek7S1MIF9LAOm4m5-jnRY9YInV8CGlupxZQmM8GQfU49bMVBljuLakCqpYLEZrukxKvE4QlIdJWMmZiWNIxv8nh7NF_JMVqmeGAqpZ3S3XS0nqPAs8ZukeZCPCsAfJmBQxQptzfkVMlJ44ykw_NBiNjZaMbbbd5Rinen_29YC-AsZ6Lp-DxSalHLdOlIKztCAqBZ9QbgTnlZsL9Hsys12uCwQ7yS0Aix2Wy_EOk3Xo3ZMIiymzy9xHySRFxnCsuq8-EB8xAGXZW_5B8ghf4F6IsEhRoT9FVb9KWTDjdcqCHPNq7-u1Pt20rSPND01cWM?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8OXtjmehSCEucWxsXt66GSxmNYk566OjtHMX32ANdK6X_m7Sf-TXZWiYST-tU1TWcExekXoLKxcptvYZ6kgoj6zsOOeL4F3Q3BNbode8-HiK42gyT6RUvhHWUrEItPJRw9umzSRHQFz5f-nPEq5xpLt0mSUpC-7ZjB1yK_OZkjUA,,&b64e=1&sign=ba63943056a282cb620fae01d9005fb0&keyno=1'
                },
                photo: {
                    width: 138,
                    height: 200,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/205271/market_H73QTJql5lw-OzI53oGGKA/orig'
                },
                delivery: {
                    price: {
                        value: '360'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 360 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'завтра • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '360'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/NYcIyNF_h5bVnTMj5LFTwQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HJjufkHjIEOLHuJv_DGc6lycfFLUGHWOKNJIPcg2cA4VdULXwAKxs6bczAqDu3BqJMgKjf_dcNITfW6mi9l9pFQFK5Qbd9DYgkZk_Tyf_IW9SY8J0_K0zdPx-iaEpZexhY%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PDrOPCmWRBOgKMoMNhCLHuFZLxPAt6RCsvGkx3bOu0Usf1V6IMRb_quWw2du47Hh7TCFdak0wPhTLUB-ha-BHVykKCm9tNKrJWjFIeUDJ4nzpCrJ0v8gt5N8CGmunRJZclKQ0FMHX7E2Fk4pDJaDBQurhRrtiOp8-yxJ0UX_Q598uYFxXEgDPOgLu27gTwkuS-rjPki6GyS2KUxppsxXjTargwNIkKTISenDxHPZ2cGvdTTWVtTrBNQ2oe-w5JtarM7eLDNfZKmtiZiqiQeAcelDBS-MEktHSC9nPKWUh4bIyDuiyebfYUfGGZMvZp8LZnSt0pTgE4yp1JsIrqPTSPTQbtrEBYNFGLKbBuGVHlU8YYYvOWtxiQ4EaIImKJeoOEHk7xR70konED2p_9XQLC6H-aLz7zb7WYPgIhwiLBx6ezOJ207rWZO5OnSyG942M_8X_7n2qMJ6bWPfsKcYnd8impdb1IEi28_AVRNi2N-AyI3ZnFM6A2i3x9jzDJxeqv_0CMw0c98X7-P9W-n94exTxGmNNytaGLIPoVwg30Vj5SJXmP_7vwYBHuFlIa3RuLpb8nQFEXdXORRV90GSCerP7DBfqJ_ySW-Y2CsKpoJZV5bb_1uBk_lxRHK92v2E2aHvpT_qwU4NCQbxIblkLSQ0K-ccWhHMjUGpuT9X_jpm2Fw68wWoeNKPrnu9i-KzdBR_R07S9eO?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXashgT_p9Lbiw-1Ki4cbZZUOU_CfnyNv-b6PPVXCv0ofjcK0RCv97w8i01hxbXsPsZ78IV--GECOJaDg4p9Kz73XM45BAFFwpB1hM8RnN8yOx_YWxFw7xxhc6aybNOzNRMe4aSqMgkY-KATO-nKcvnlFdk0X7FVzW5m0vLxoGKztZoKg9gyOVRIz7D4qesJ-464f7aRlPDAzgsgqxgP8jx8X8dRJn7L8KAyr1PdDc10dQOpmYpViAsns7aa2kTYFPO5DlFecIwSP5iKAN5Csz8kqAhY8QSHFcqrax9x1D2p5H2LJ5frePCw,,&b64e=1&sign=e1a68ab02f755aa3e4548a9640ada79d&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=178623',
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 138,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/205271/market_H73QTJql5lw-OzI53oGGKA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 138,
                        height: 200,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/205271/market_H73QTJql5lw-OzI53oGGKA/200x200'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGoq-ALYOkeBE3LuOtJWqqmjIcWFfH1d3m2LPNL9c-TfqV_YT06tJp_6bDMPhhtcrJNXyox4RVVaunx5gkWbyKWyHiSy91k2-mmUgMxYCSia_FedTFoXCJM7kcYmI8UOIifryou_tapndCSZftzIgaksC7v7LqwF8Gx5rUt9Zyp4-sdV9YMnKMnxjMShZNolONpXJc6stGuFVYEKIohWQRrcyWyhVq4DCqZdZOVzKeIFQrMHF5yM_LZd6_V8HPky51sJJziSIhkHqpt3GjIkbSPKx5MWoN9Z6M',
                wareMd5: '079P6vBQQ3HTkPK5DvD8Tg',
                name: 'Смартфон Apple iPhone X 256GB Silver',
                description: 'Операционная система iOS 11, материал корпуса Материал корпуса стекло Конструкция водозащита, тип sim-карты nano SIM, количество sim-карт 1, диагональ 5.8 дюйм., размер изображения 2436x1125, фотокамера двойная 12/12 МП, запись видеороликов есть',
                price: {
                    value: '85000'
                },
                promocode: true,
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYhk2WNfW11bKT1f8Nfd00waQVJiX-Dq8dveLPH9pN_JIyGonKFKF8zJV2cX6OxicwbbafP5QvsertHDFI4fmfbEHNxobRJB-pgpK1QTbOD1Cnfolu0ZD3lShc0i3Bt4wp_jNQK4gwvYOl3K4qkYZep0CCb9QbGeNwsyDw2r2-e5S3DO7F8kNT4bLki-vbzXj9U25fPV_qjbyF0JEiIzoCK7H3nkhlyjYI7bRCXRqjYuNH24Uq3Eku92uFNS9d-FzVvmUe0ZDqj8FxA4T4nooGqCvLmJNHK9TQQ6952wiYpFCbu3ROzTaIx0znngJ9MfAotc_xJq52_9quYafAzjhKobgNj2mJvUGJA-3VmxL8t8qSv-pOUCCW4o2Ew1eCESwHnfcs0jZ51XhVob0byyCvMwe17Bej0DzgT1eAULzFWLNv4rSfccGFZX189YTq42SUCXSnG3qRfRz6dJXTycRcpOjcfTcYJVQXWuHUYAOZRu17fNnJR3pH5ZmfD9znGKfHLYdIqOu1Wlx_UbJ_Rhs0nga-nMZSEH9wO-WJVm0R8p_x0PFJBeUj-dqigjyUJUVoR2VwqwVDKD_ZoWbKYoc_kQRLwMWZ46zg-3d8L5E-mqIODFEsyg-kbPoJ_4LDQORNA8UBUXOSUNXF8SYLhL9kHFdi1z72djhP1dD-UGc8_A1-cbImsInhgfUOhEJSmr8Lvk2e5_mMt4-a9FvEI-fme9hNklpgKvTMNJNvHpHXYsYAagNuy4NnnO3zjv701C0-c80KoL_3DkX8FQRu0KlF3jpQ-xXMWiqvKn_LGCQ_hm2Awx-GK80AOv5xycVDZ3YaHYzRrBoiZNFTKtHmZbVGlAt_vzIOJVDnd6R5d0yzPC?data=QVyKqSPyGQwNvdoowNEPja0uQkWSxAKizEBbSXMNJ77r-RoTFYQC7lKBJrXfHL3BGSFR9X5idu36MKbWA9lGBstGOO2CDDTnSpn-lBzRcjtO1LlQI4GRxGvo9k7I2Y_VBl4Q2s-moKHU6pNm6kr4r5HVROu05qO_MLt1PUYm9DNjz5QIe8uR419S7jXykwAfQHgG5voUKkBiXAvH_jJVVP3600Qz1iRWlSIyoORb7e5AYMoLEOap5semWbTW_-7tnXOdoI7NHZZCFEeRj2K_cXJmvFZtgs4gPctjkLB__lyAfVo8pguUhZ5h5KrFuFlfo1hfE3mICmhhFJvgGVBtcUlNu_y-ONzBrzBJT0elaovZgL0vFGGyjCfQnLXvs1KAW1oIQUTl5x__QMky5ztk0eHBqp0Ocva-FG2UzjLVnNn0lImiZrHDHA,,&b64e=1&sign=3c1ee520baa05e2b70ee8df73f8cf9d8&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRvVL8634LDQ-Uoq7X7ksUDr5cqf-Qh-V3d5nJIGpCxaXBx9WgaPPCda4KcRnkDUWdR-Bfrm8Gy68owAeDlXVEHuGWA51RmPCWfu8Dvathej4yp-jBl-3p_OT2EnFDGnDY-ckijPR1Ok0c1OeoWNItEfUE57_R-Qt_9qiAbSbP5ccgEQ77B4fUHx_V-ymRmfjVz-AddgyvpvcyaAgjz8hNamJOpHiGt10xSBd7qXfGpRtmsAcd8igeQBM1-uU7yrigjTOySIe54gIRPLtnA8CtDlW3V5h46SUz1jKlrhcAOwnViwWdRWeIsBsIfINHdOuIZ7jIPLlbMYfp0mVdp3Axbj7NkQIBDoH2mH1bahGT1sh25K5lVN8x9819qBju96LsfdBIvMGsJjrbFpfwvMsYDy_10TqWYyOfEDyv2eY5vrCFwfhvWk1uunpMlT5ssBzBaLQdtfuKWw5D4d3QfdxSP0IgvgfT7KKUyiMyvQTBLUTS62lSWXshRIbUyvGnZND8aiFuIaHEcEBZZPSJSMf1LiAgDdL54pSELK91zedaKlX9NbHqjZ01zcVnXL6dXAUGtGK65I0lRRidcJg132gRTfONAfBpS5HD19btqmgwPzeeP-us-2qzkJLuyvjG8hdQ3xq7IsX8RhZYclSopd8zUgh5fe5pvEb3Sg10sKhQpMtjZ8xugQFeDZA1sjs2TvCvQj-sc1Wf5tIEJW3uGPvvgOPTi-zRg6annWW3pP09W2dBvoBCtMi8jAfToKLfLgA0unV_-4qQRx8IKDyQcITwC0R3W9R8kjYuNCsEJIVjE8f9CZBS-bRbNNAUKPE-D02y1mCQbE1dFc-mdMsrOXUhSFtgH79xmsjH8Do0R-5r2J1dFIIKF4ZVHd?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2Q0CtvRM4OXRGtSzmV6uMQI-O-cRWI1k8Mr5omBSKMoWOuxio6E2TybDGEfYIJ4Uyw_jYB_zrYDwQtD3LZmioZmT3GXbz9efjul4t29sRxo7uku42fx3llM,&b64e=1&sign=f1d3d490ee4b75111f7f5c7185c1d40d&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 981,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 25,
                                percent: 3
                            },
                            {
                                value: 2,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 42,
                                percent: 4
                            },
                            {
                                value: 5,
                                count: 902,
                                percent: 92
                            }
                        ]
                    },
                    id: 178623,
                    name: 'Club-Phone',
                    domain: 'club-phone.ru',
                    registered: '2013-09-10',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, ул. Сущевский Вал, дом 5, строение 20, (ТЦ Савеловский, здание "СпортМастер"), 2-й этаж (балкон) павильон N31, 127018',
                    opinionUrl: 'https://market.yandex.ru/shop/178623/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7(495) 789-03-40',
                    sanitized: '+74957890340',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYhk2WNfW11bKT1f8Nfd00waQVJiX-Dq8dveLPH9pN_JIyGonKFKF8zJV2cX6OxicwbbafP5QvsertHDFI4fmfbEHNxobRJB-pgpK1QTbOD1Cnfolu0ZD3lShc0i3Bt4wp_jNQK4gwvYOl3K4qkYZep0CCb9QbGeNwsyDw2r2-e5S3DO7F8kNT7pxyaeW10oHgL569YuikHtIUDfdBK7yRHer8Sk8OLTXbVHbjTgNEYyyhbFfYqsk45m94IQ-Wx0bQlKltyFazn0ZFvv0svByhJhy_QMwcdnKBE1MltJnJ3uF_M4IelFy8BNYG4nMNBJ2U0RzVrggO7z5d_jOkOnayRtXq0lpII_rwHzokRaMkpxaulLgiICkh39UVK64RsWnApMH3cNnHekwh_Yk3th-dS4ss5Mi4DP5vFNUoD_k0Okxt4BAMmu1YZGxdr0L_c7317CY3SmFW_-9yD_nQVYk00tfC9hREn9rkvui9L_0ITldKTu5K5S0UGaq5m_lHzrDmdHMqEDKsiheMvVlS5eFWa0SU1yQ0alVHTdMTFb4xEFrc5luil7ArvrDaDJbWNtpdVdemm7MjG8MaSZuhiYQ0pBHOFiqtLXSWNtWoB_WQf-KjpTdteJMLeqfnP-XtG-Ic6UkmifxVSm0ti9MYSP6xV-Zrp3vJYQD4HwIA8zS6mx8kGGMQVtz5AJhKtGSys2cdgCEzo030rKmFw4uhLJhk3wjf23WjpaCZmzMrZ2oag5lbjuYRoF_jSE-qbkNMjLaiBTVZW3L8-rebH1P5WYqlcluRkd4iODrOSDIe9W7ecKgtSQHPwiFwQgw13Xdt9Jd6aHoiVuoh-Ng2f0sY2iX5GMOVKG2X8-vUS2aQ0BDnUr?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8mDBR_suXoRdalSUPDqTxu7cy6dWbmr_-GRW7WQfh4X9LhEc6qk3g383ynSLsgu0t9qXG7zCzxq7TBGsWCU-4j6XoBWetRrx0MVYEfsl1h24v2W1VGUeIafawekzKyD1jkRfX0_T8CXtUZLwIfzQClt37toBSx8Mfmr2_4dTT-VA,,&b64e=1&sign=5bcba8d386a0bd105226a674b2c6a7b8&keyno=1'
                },
                photo: {
                    width: 200,
                    height: 195,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/174398/market_0rV66g7O8j9dQlqvks1s6w/orig'
                },
                delivery: {
                    price: {
                        value: '360'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 360 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1,
                                orderBefore: 24
                            },
                            brief: 'завтра • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '360'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/079P6vBQQ3HTkPK5DvD8Tg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HIbYYo6n8grCrb_bYWD32GFlcank9WkOG61dDVsCu6XchJkPmxuawq89oPZU7KI4Sbt-Kbk9OIsJ-JjVaDpF3Fp06G4Co6FDSgsyIw8r7bseIYksKN6Qdck8G1gZxD_Mls%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PDrOPCmWRBOgKMoMNhCLHs1d1xEF6A-s2IuNCY2UJ2aPmpe1CKdqzDlWscCD0wE0UDgsmltAWm68yYgY1Pcy_0lNz9LD5WK3PPpI5mfSx8sPI6NKo-i1RHMHZq4KbRiTq3UOCfWBhs9bUTifrPjvgmjqGb3SJHdD1BTb3fTHhIcIoLHU5HCrDy50G786gOTFhmMux_eEI9KkGuOtl_h0WP-h8L-aU92I9SYoXZ_uh0-7BK7zdGpj4rOrLz_MXhTabsyRETHyfUGyzg81Uuc_C2LATpLzcUCg6M7QfOsMVvT2QY9uqoOE6oO1XIAyUG1A2oVw-OA0KVPBG3bW0N4hY9Cf4ryPS6NW8i2sG7rvGv-1CYJj1wfLtz7E1Uc0nnxc6fulJe8pQcUcuO2lXQm9gdztxXi2H9BtfNqz6Fvy5_8l1qpHChedmiOof_Z98WalaUFBYbzvABKScXaMqefC3vAdI9jK_bCofSD9X1bJb2yzdkLgrHUN_Ig2c3s4GV2jy9lY9FPlIAitGqxXOGhiXZPqCHKBlReNyWERr9HG5zfpj4LlTrK2NwKUYcmGPX_uYoB8FE--H899gn-ftgrgjoe3ZN9qlLoEGm5rCvSve4zWd3SxmrxC8gFji1olHl76-0_1q-X5BQWjHRkpGU3GMVxrhbNEz_mnmNaGcg1uCCtHvpAwmC8bCo001zrg7_D3dsMo9SEvdFf?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXashgT_p9Lbiw-1Ki4cbZZUOU_CfnyNv-b6PPVXCv0od0oVw5yR05Wd5IFHL7kIBik16HMtfb6DBAjfe4xGUrB4oX8xlmQMmCoXDNaZg0-0HEc_vEtqO2vy30qsN-zvGSdEnRQN-sDmMX0Y_k2-WCMNFt2STyyQPqE89WPq_ERHleeolQWx814OM-Sz6C-7_LCwdToEbR67r4q5xbBmBEF8torosDd9O4p3zLW7eK959eJdGhUAPwqbpetVcajc3w9DxllRbjNmGgv9XiCf884eta5mD1_DA7VSXvRnZ04VOqDaLSYHPDvg,,&b64e=1&sign=24d941007a154bbf0113123a6c5136f0&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=178623',
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 200,
                        height: 195,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/174398/market_0rV66g7O8j9dQlqvks1s6w/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 190,
                        height: 185,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/174398/market_0rV66g7O8j9dQlqvks1s6w/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHEZgnT0_-U4JcgkWR5sjIWseb7U4ZFKrVqtHrjwfcdG7L9K-u2BF3W5l422eOby_sT1qIjGkGH6BK8B8R1eHmIZArK8e2hEQUmT0bhRnvkx5jbNAHrw0m0dJR4oDR_u4z98reV7X7LvZNqobcu9G5mbKezAPbPhvHujhct8I3I7JtOy0INEzzpGhmOwql46uA77GFKSMHEx1SOtt2KBJLWV7sYavkuSfpK-N3INIT1cnwezEZVKiMaDitu0l1BexyIY9cxb8vCd7e6SzhiKXzQhvF4N2_ykjQ',
                wareMd5: 'CxxX-wIr_o8ZVw2yqtdVJw',
                name: 'Сотовый телефон Apple iPhone X 256Gb (MQAG2RU/A) Silver',
                description: 'iOS 11 Тип корпуса классический Материал корпуса стекло Конструкция водозащита Тип SIM-карты nano SIM Количество SIM-карт 1 Вес 174 г Размеры (ШxВxТ) 70.9x143.6x7.7 мм Экран Тип экрана цветной OLED, сенсорный Тип сенсорного экрана мультитач, емкостный Диагональ 5.8 дюйм. Сила нажатия на экран есть Размер изображения 2436x1125 Число пикселей на дюйм (PPI) 463 Автоматический поворот экрана есть Мультимедийные возможности Тыловая фотокамера двойная 12/12 МП Фотовспышка тыльная, светодиодная Функции тыловой',
                price: {
                    value: '84899'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ6WyT4Rt0E1xd7AXQS-FFzeIhk8_ZNs6ZM0OX5XIrXbZIyAt6AkBijySLpZByd9vzd2SL_IzWmEhnwHTNV5prPVhwstY84U87S5DRNoYmXTUXJ4HZjoFeAH4jsr7B1CmXzJMpOxijdzx6pXyOOmxPEdf7xkigYtZUXB1MZxZPbhYlh1RiAFT0_655d4ZPoiuWB3mWvAZh1KygUTa8szfOPpXnjGbsAVU2wazs-aGYx1jlPTcmI9YM-qChO88rYG2b7mMr3K0XuSCArNeYNsgLdcbumSshkXGvi3OkJONnHrzjfqh1gQXxpSCM3QcJk70iCgI9cwJM8L5SaMXyup68ZScMagRIkpguLSHuX0Kp-pKtkSYBNuy2WNNqmmkGH-kh2ai7CyOPKyNSpMgE7PRZBu9V4_RO2TKsObZrhCgAaMUxBApuzKWchmksy_yRKPvE_5rZbNZ9mWemhTy95ewpKyW1CWGU-r1zbzAOLHuSZ4pdpSGBBMci-PIR_0xQnv1ZGfE2RD8Aq-d29To6axAC6Jkx5ijrlyY46G_BL1SefRZtFmxONkC86iR-iLD5Q83UHmg2Anz7jQgz49_J5xWpfbJSNrKKVYfed3uNF3_p9Z4DHGBe5SXV1T1W7HHSGn1Ra-i56ZEXags3X1UPKhRRt3WvOrZY1CF7_HwfFdLz07QAJfVTbhVOsgIqToW8--MbRFUNHR6SSOeSyT_VIfK8lZ4vbkZsMUXuEhRD8P-XAWnWRdwuvuo3WnSWJ_IIkKozcVDK4C0D9XlMJeseH3JCVd7GMQOcl97lee8I3OMVNKB_5WyHs_JM8czrrPu3gmwchsTz6HmmmwK7AntHnd_4w81k3QdfZSA1uJ2X2ccV01?data=QVyKqSPyGQwNvdoowNEPjepPkxt1Vqcos9cWvPI22tg0wCjd75HgObocn-VkR-9ty9kgea7JlqOTwblPNbhRmVs8VQhcci9sr6QI-_si1Acq1alZCohR7B5hj6M2P47E2AaxeFQ7h5X3QbJRmPVJusK84mPYxTMR_rbR5OmnGd0csD9cyQXfyi9qLcg3O6Z5Ho2C1vJIQlmH6cdBjaPig2JImS1TuvWwjpJamyYDuz52s_u6qiq3HZ8jIO1ORXfHK7CC4rgrOXhVjQYVRek78RafDQb8SB6JUffULMz7AgHFQex8chk942i6Is_JMihKdUo8v84L4Egpx8dJ4FNoSlHI684aG9rPuTXKq3N9TLckPbGFRmfIDC0mJBFcEZn1u3JzROEoeLLM-NHNGMwv9-En-RNZRveMey9Q5VIZzH5bZJsXcHeuWg,,&b64e=1&sign=0c0090151f5214a5477e29a41c5b24e5&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRvVL8634LDQ-eTkoK7cCCszJNTWVYxNY0flQ9lZ2BrpNeYcoU5AVzZASjn05PRtVGZuaWx-powqgMoTkV3pB4o-cA5Sh8pXXSw2WwGxgXmz9h3s8gIHm_Xt021_55DAMPfh9_UJw2_eBdrkZnSZOE1aiRy5Ls8NgAmLMKYm-3i8U5Fq3kqysmmrypuEuf1_x0Gmq6CSq9DH2tqkIrzhSM54_dz1eqK7T5UXubKtH_orzB5G-FtVktLS5QCc78VddUhouOPDuE2OnT-ndBnkeCwuK3Vy4cI7Y0HXkQvJDL1uzhrn44ugprw7vOyq1XIkaHiQbeckrX13AFBkXothluX6oc0gm7gH8YQcdTy7aFnrOmnXoUVcRRInXnrfvcnIPPkZ9DNSKJZr6HSFjPGsmYsDVXvaiqkPVO-6HBeV5J2KoFSkPjJzYh66G1rueyrpNSgJsG-Og7ggcZUoTtbsL24T3fmJB1STPPf-BTsC9Qo9LZ9GiV75TVWA-xExrnib3NiMlKLhPxPuDzlQg8mJ9j7kEiKrFX2aIbbSpX5ji4Iy3dyoMcrrALFX1ygDF9RXgr3s6hs0JamDbvseqd5ew_JNey-t1hTLKjy4pyUWqx3t1yBZiDg75KeM4zkhQpn9cqvboiYf44h24rgOPuusRQ_89q73TvGM2fL9qz5v0vFY-PrIhx6Q6zAXWp4PFP1p_uHYxQzbQi5Ox2PkhBrgbQt64FDNu5TrvOgAYKiFge1bhnHL1V9JY3032RPkLUKpm4tFGeOGbe0_qdcGZfFQlDlA9LtcL-si4sTBekkJQFzxL4lEd1eOhyRCfG15U9j2j8f2IlhX17ohKWO1ndkOKfVcivzfmvi11Xae4SvyQk-Q6A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2WevCIXpjIc5u3bZ9aDdMPRD-Gd5VeIhtwhU1jWcz08MYHswn9lAojdRNiHd8Z1-oFI2A10y0EMyNqbCmsTYz4kXdnaGslQEsrMOSRyqAJmHiy0DF9pAlKI,&b64e=1&sign=b0bce9befc3846dffb060a55c7836e5b&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 6154,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 265,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 77,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 69,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 371,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 5374,
                                percent: 87
                            }
                        ]
                    },
                    id: 6537,
                    name: 'БОЛТУН.РУ',
                    domain: 'boltyn.ru',
                    registered: '2008-03-14',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Марксистская, дом 3, строение 1, офис 413, 109147',
                    opinionUrl: 'https://market.yandex.ru/shop/6537/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                phone: {
                    number: '+7(495)545-4227',
                    sanitized: '+74955454227',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ6WyT4Rt0E1xd7AXQS-FFzeIhk8_ZNs6ZM0OX5XIrXbZIyAt6AkBijySLpZByd9vzd2SL_IzWmEhnwHTNV5prPVhwstY84U87S5DRNoYmXTUXJ4HZjoFeAH4jsr7B1CmXzJMpOxijdzx6pXyOOmxPEdf7xkigYtZUXB1MZxZPbhYlh1RiAFT0_sIWykrIon8VglLYrmUcSc8vP988bkYdwA-TYFMfl4NYmjFZMDxqxx1JVUdxQj5S5arwGYeuia3ZvsD8mY0CiY0bcLIWA-9OdXyuiWdZ4eNpQ_sohi3ul5hzsyNLqn0FK0IHp0yyCJJfdIbZe3gydFFN4sCtTJKGttpdC87lI8Okd_Mht7jMdqaW1SYAK_4gvs5bCdiSH6eOBqhMR328eGBOxksw3uEVE6oEknVe20LTzL-DuxZziN1A8crRJAC3xwdjl8FigGIvHZIY-wmsGeDa2lRyY11lwnCUQjAI6osrDSr9xWEcN3KV-lKa71bMA1UpT_GLwFACb8kAHMcM91uUDnJsYx_Qai6oZW8d6tLbVDaU4Kcjoh1Oe0XwOuL05XAP8BopF5efQ2jm6EMAqkoDYsyP6wdnI8BzSWpn14i-CJMqJFvG4goloo_ho7SnJWNf4iQRqPLwTQFW_OV8apEUT_-Zl4H8LK0SMJQe10RPFo4N30cau-ueRXdX82iCrvoyIlt0s_Aj1y-XqXceEAg9KB1C6dEPmUCmYYCoyrzVfeg40HL7i31IOGV_PGmLnZAxcfwFUBmM9VRhNupP0iiKx2wCE6LeDeiFNbpqp3O604XhrVFBYREB1h-vvq1AdEJcT3jwzHlSbExeQAreHTIf-ZGZde44gnEJvKupZRSGYfW_Pi9Ob3?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-APBr5xIEb2S5xWxbAQ7HtHDeypUPBhs_QBDOVdr8_5qwZ4o7gYr2ZTKXs9_xQfh9OI_sQpe-wt-qFAIL1N2c9Jf0YF0B_fH9mqGaoyjswJxzvauWPvaiEpfaa8r45u0PkXmQVAZTHoqvGUI8i2FILqvVUDdR1V5TJH7-w2i35TA,,&b64e=1&sign=e1b99da6102d18e2fb2cb2f578ee276a&keyno=1'
                },
                photo: {
                    width: 397,
                    height: 572,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_yY7mCkEmZQVR93kiGcfBbw/orig'
                },
                delivery: {
                    price: {
                        value: '399'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 399 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '99'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 5 пунктов магазина',
                            outletCount: 5
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '399'
                                },
                                daysFrom: 2,
                                daysTo: 3
                            },
                            brief: '2-3 дня'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/CxxX-wIr_o8ZVw2yqtdVJw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HL5Yt0PVgE6zxrc6u8IAtEXR3kBW7bm6Zbhh4FeVJNzuKCG4U64eJ9thSplMSy6FriE-k62dSOjGFUdPwIQGOgn9bem923AedDDyZGrVDXIvXBodZ7_mfCzkDq-JOPGWuo%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PDrOPCmWRBOgKMoMNhCLHuogpvX9K-MBz27dr6J4_8lipoWsfL3zDMh6KlLeR0bKvyLCJHZEhAjfvI_XFhCpUjTuAjSL5ueedvEDYsmIXcC-_Z2Qel6-R83u15oec1XaWOixPBPKcodaUbsLn4BHaLlBheryPKYIeZsam2jHPjKWeUH19phK4zcyHigRi7V-TMJYwGfh0miKyNHH5BIKK-hBROM1sshXoG8ZW95OAsI6dxseR7isyyxLs60SQk-H9UAUWJTgj0XZBWog87b8PVImyDHmMQL9G2DJJCrHxAaONXejMV4Aw-r3N_cNyOfS_EXWTP9egUz1YR5UAQJCFmYTDi5QUDDqlfBw7dXshtlMef_DcoiEVzCtPpUlqM32JVvll_cT3zO7IysxGo7wHYjGXAuXlXc9-GHcB32VqOmQ3DBX6JQHPbxBtiElMMpEqfSCA2EXZMkvWDPYP8YwEZUkLbXTI8piPxQepux2XZwFkc-D7TyZBb2Lh3BdLJY_p_CkD7nrgouAP9ssDuepxZAfkOhJ5a9-OoW-h-eUlg4UCB_qgeTjD3KkoN2HXbPeNYQ4nKr4xMzP8W2bX1vkUthsFpMpx_uIDHm_laAAVGHAjMQ5gc_ptGgXDZyLypbL0HGhHO30NQh0tySuYJYYgIaZNpww4x_DwzT88SjbDh5nU1Y9HE0s7I9R7km45TmvQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXpA30pNt7jplbXIaTQPgkMZ-iy3ZxArOAmrBBe_zolZXO_80BL1Gt4BcyjbBArtc1Np7ntfoY_5nzllIvsPb4vw7i0XMxMf_QuoEGYdMWM_zeGx7brPiwn0nqey-B1VawotDdkPb1gEB9tk_syODsgytOfN9hE3KZ6tZNNaEBMxhNRfTzc3glYSJ8Ktqwsd5FLw9soHMp5g2AyX5Nuu1tVy0dykIvCEeEP_rbfWeGLDtjY5fiaTFbAHJNBQqD077zGL68uHYyt1qfYgARGxhNgHo3g_mWPoo7t4XISU0OllIGEzr2wICZvQ,,&b64e=1&sign=fcdaa635fb5589b125ea0f82ab3252db&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=6537',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 397,
                        height: 572,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_yY7mCkEmZQVR93kiGcfBbw/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 173,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_yY7mCkEmZQVR93kiGcfBbw/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZH8rOiQznknGDKVeP9lU-ZQZZnfizyMsEgQZ5D8aIQuUWObn1YNuARr1yT1J53fF64kI9QFI3uddcm9k57kgADrNVDDO2HuyuFDpovLAF-1_1muDtiE533u-hi9ZakcvOmfQYWh9LL5iIlDZMwMayT0z60yXpl6I3-0MouTal3_eCU4IobYyTf3U6wl4DTZwniI7kzbGW3jPbSubSF_bq3ESi-gfzTRq_-Kv-zPhzgSCZ7shcHQgVJqpsUz1LWkXJo4ldJxmYsqFypCtuDJUV3IUsS3lWrjCtE',
                wareMd5: 'GwNv9s3xfq2HdoXXQC9EJA',
                name: 'Apple iPhone X 256Gb Space Gray',
                description: 'Дисплей Это первый OLED-дисплей, соответствующий высшим стандартам Apple:  невероятно яркие цвета отображаются с поразительной точностью, чёрный цвет выглядит естественно, а контрастность достигает показателя 1 000 000: 1. Для сравнения, в iPhone 8/8 Plus это 1300: 1 и 1400: 1. Также в iPhone X есть технология True Tone, она с помощью датчика RGB на фронтальной панели оценивает условия освещения и смещает точку баланса белого, цвета получаются более естественными.',
                price: {
                    value: '79990',
                    discount: '13',
                    base: '91990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTWM6wM0TqfEVTZNwdtnpNXATtNor7LCeqPHTglUalrC5h_BhnK0gx2GdG3hD_9mi7jMm5fjZSQZJfBbX6Gcs_U21LnaDM3DzvggE09KLmjPFb3cwjp6VEpI3FlDtGW1QtVsAwMeBCX8qYSCRcJWfbtQSTFJajmlTF9gRUgB9NCKRS5s3a9V3P2JJwGBR1qrHKhlA2Jf9l6nuCO2PjlXPyLD-9KTfxiMcbG9uelljqi2SfP0J9Oyp_p4KLunqtYteSZd5GhOG7UewBKESzV5uUXfdniDrULo9pxykfO1eciNckaN3nFmDOhMV8BjMQCjh4b5Q215mHappI9lEIAS0gsDRbvbUbHj0qvgLUobXX9wzYi4mMzlb6lrlr1S5lzbgoqCz0q8PQDe9wRmSlnVK8lSTBFWzaqA8SK9ELCg1u4ynmyDXw6R2Ot3bX6w7HPOvhRmD9MqCJRmqeWlt3-iccjCuS7UTY_LveaJ1fg7KB5omBtvjFiSH0XXfRljXlfuqqcOa221eDNlEQD_XLoQ-aqMCXTAXCsQKenpZPs84RIeM_CgpYs5VImH4tXYkCb5RPwL2yJ_My9fKwA40bp_Sfblt6YKVABjl8UESFq7GmQeP7ulI9BdwXKv-QHEYYujZRgDKE2uhle09C7EPwoEjV5Q5qh-Q7cC_Z-d5nWkqLgu0fkBRbSZsnlVcS8VNqMN_MsmgED9zvKAwffkDIA4LHmnmNPFrJi7-p1CdCRIdaw9aT_vp2IH3IPl_185X4LOV-Rahzru4hdIQiAUYr0sWFE5J1LyVxEC6r5EwCGfNpekOtIJEaeoTafiUh_mbXt4Fuu3R_b7q8tiyATzFZImXci750NG-mimioXZ9zfhcR5S?data=QVyKqSPyGQwNvdoowNEPjdMnGw2AwFdOUTgKDAUMZB9IiYRvnvVReaH_cPLMUCsKFzpJnjSFu0IoRs_AU9Lr8QVMR3dbbCh7g7-l89XsPjCAeQ_-W0dxfasVSabYOzYGowcH1BsdR42gLpMRuBnwtQ,,&b64e=1&sign=9336e1284d75ae07af6d40b8a42c3508&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4id7F92YLBAGL_Duip6ewEE84TK3ApnR2OqzT0ueab1cnfa2fwYQEak4RdftKO5uI1Qwa2wdY9bgOW9xXd7T-jV-HCmgPVIjGcE7YjnzDgjqdRUazCiVuDbU-asCk6Zaas8-3R4Ju20CwOcW0AMQI7Mw3r_6B6C2jBq_nA8iR-Wa8zDexqj5dQyEcRJT2uCVwfMoI0ZV1qRsq39pnm21BktWUDAuvr0EaKWuT5jiCmDt0IFytLJvVlOGK5Gl6Z2vs2kz1tj6y-1wkkEO--7iQZABv5MiRhYoORmO9tQJ2hJedWDT5ShX_95v0RWLaHPt5-YLArufcre1rvlIXuExy7yxp9WScahY5qRrxMpkaTemv8cJxRA4c8hPb-4gzy1Kg1ppqC0SAGWSojS7tN6vYUuJw6fqmYrpsvAGgpxZIaIkcgPOtmBtTDn_Qh5LlDGZEqHJ4iIJXpJ6s7wF2r7BYsLD5TC4WM9wBUe7BYSAnwIuZhbeeoczTeQe-XVDCdw3-OcRvSKLLf5cxUXyjIAnmBOGYDk_DerARZkroW_EalxYCsVABScQ3RiGFm8WK2-cCIZ3wPpysgLqMoaZHOY6r94_y1iCRSJ92WQU19Hz2n5KNTueqDT2yh11oms16kT51EXEz7SpbZi2h54pjotdaaqNa547A3tuqF_YZyO-aS87N4g6heAZxOJVCgUTUsSftZCv9nV-cHujTra2kk-1I9sIO2ASNBZ45KC03uqD8-0mzjHEjWKbt5apef_cFi6XelkWeL8JztI87PVeQareqJwDSYXnoCWWY0XqG2BpheX8pDC7vfLSJy5uu1yPzSyj1SmmknQ2ydSo_pit46XB4geK9vz5zlB5unN8DOQajnQAK?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2SHJCIWXcrAiRKRsunOIfOrS-vsK5QHx3kim2QX_EvzJBoTJlQqQ2ScFGY5hw6DNHE2QyKC8bpUDxiFIYCeGZPFkhp8uO5Tb9lBVpqsgqVz5_bxElT5UjO0,&b64e=1&sign=e4a9407e474990aafffdbecdcbe66775&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 642,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 2,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 6,
                                percent: 1
                            },
                            {
                                value: 5,
                                count: 636,
                                percent: 99
                            }
                        ]
                    },
                    id: 77535,
                    name: 'save and sale',
                    domain: 'savensale.ru',
                    registered: '2011-10-09',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Буденного проспект, дом 53, строение 2, павильон Д-19, 105275',
                    opinionUrl: 'https://market.yandex.ru/shop/77535/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7 495 664 67 77',
                    sanitized: '+74956646777',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mTWM6wM0TqfEVTZNwdtnpNXATtNor7LCeqPHTglUalrC5h_BhnK0gx2GdG3hD_9mi7jMm5fjZSQZJfBbX6Gcs_U21LnaDM3DzvggE09KLmjPFb3cwjp6VEpI3FlDtGW1QtVsAwMeBCX8qYSCRcJWfbtQSTFJajmlTF9gRUgB9NCKRS5s3a9V3P2JJwGBR1qrHG6ep52lEYYGB_vPK1PhrxgyuqAcfzH5ZGW5uO-QL4K6OudppyIQkt7nLqnoFOqOZtoZrrUUQmSn0KVxYPvmX3xT-OFgy9PVWuWcPS4GrbGZ_4QYgHMIYvioUZAk2koUU14QAmrXk3sPF2Y4C5-YSjE06q-V42QglM0aBwktI6YjDEr5_T6wqGIQP6LRG_bWpcaSiz96zBNgUdpccGTsITeDuqHRF27gefdSQDqab2B0YPU_kmqXXyzn1sMd8eVJ2PRoVrfibLj49AUnOrD5Ju0t0cDrEGUW3jSumuNwn2Us_jjBX7ly04n1Yk8dFEQpRl39pJU_1Wbpd2UyydUyGMOcLnWxhtkwyLPnsDhJlW1Lo5iDvD4UkZZNktFe2uJEm9YXcWv9KOcFilVhTgKtFLy8R7ZmUZyzJpf4cofsQn8z_FdFfQAydsGUMv579bJC4exD_jLA8FLJs6dRrSc3Ku7jWvSYXOv6SxTkdxPv4pccAsVOUTGjonNAJtV_yQUleTcHvFNJTCjfKsmaRJwNeTgkaWxcpbJXN_Z6o72ro-t-0z8w8SjNLbjdegURPXYgqjGzVnuqqBuGR9p122idRVg7EGzsQY0Hh3Uh5raoPTdQVhddF7CXYtARzfIrsAHZxOObP8kBa3x6Ny9ciOYfgXRUiS7RxB1GZmNMfKAoQETY?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8nh8IoVCYrHzMxanldLaqouSxbUDWE2YhbWrY6c3NO_1NHmkQoj0E2SfhiZRJOsVqgFD144wuCPyCgaFcBtVw0i0nByfjIUSrUcAL3PM-Fv_Hvj0RdmGmFl7wHoWzmFa0B89KSJpjUcQFqLbwAvolvbD-16xYLfcJzJ4xFbIAs9g,,&b64e=1&sign=aa893a29be5848d9c8ed1279b9ccc1a8&keyno=1'
                },
                photo: {
                    width: 100,
                    height: 100,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/196766/market_-7ikCUHludFSZo0TIiZJzA/orig'
                },
                delivery: {
                    price: {
                        value: '300'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 300 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 0,
                                orderBefore: 24
                            },
                            brief: 'сегодня • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '300'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/GwNv9s3xfq2HdoXXQC9EJA?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=AgY7Dhp8I0gwC2aBUp2pPPbVOk8JWd07OtWV_EaUFmBqcXPOowH0TlhXOj2zUQ24pj0Z3pGykxzQhkZ9fOEwNbmKdG1QLDjuC3fcZl04POPZksqRGrD9VCjbJBP6M5px&lr=213',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=77535',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 100,
                        height: 100,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/196766/market_-7ikCUHludFSZo0TIiZJzA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 100,
                        height: 100,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/196766/market_-7ikCUHludFSZo0TIiZJzA/74x100'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZEd3LuKOxt-iUbev2fzN95aZx9IAcKnqYi96aEx_-wIAhIoH0hSt0I0mb1pPP__HNpm7AGUv4PZEDDBsohMG6X3sh7x9WI8ZdYZQvFD0PtkmiSGt7aSQxNqlwj1Bi3ezBs4Zwcjn8SwuVjQXOP0WIz7tKahKCYaAmJftD6OdUEY1vqv3N5pXwQKbwQU2xgw-MfiuZasefSvSI6g1gG55QFb4Yf5xVto-TitjfHYwPkLRbiBOTJCVSEwg2fRx2HvAGgtV_0gJw04k7YXJ6-F0t6lilKj_8-8dec',
                wareMd5: '_JI7NJnYZ9J3pLQ0TT-LCA',
                name: 'Смартфон iPhone X 256GB Space Gray MQAF2RU/A',
                description: 'Цвет "Серый космос" (Space Gray) Ёмкость 256ГБ Размеры и вес Высота:  143.6 мм Ширина:  70.9 мм Толщина:  7.7 мм Вес:  174 граммов Экран Super Retina HD 5.8-дюймовый (по диагонали) широкоформатный экран OLED Multi-Touch Экран HDR Разрешение 2436 × 1125 при 458 пикселях/дюйм Контрастность 1000000: 1 (стандартная) Экран True Tone Экран с расширенным цветовым охватом (P3) 3D Touch Яркость до 625 кд/м2 (стандартная) Олеофобное покрытие, устойчивое к появлению следов от пальцев Масштабирование экрана Защита от брызг',
                price: {
                    value: '90990'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-4mW6Z3uIuuA_rKWAt01THlcKvWhWfgrX0zsZ8WtYO6XryPnp6nHFBovi3Z22PHJYKlB6uRIZlkyCl7FiRTLFqLHnlwdqk1OTGTo4dy-bze3H19oehoSH3sWgcKyyJ4ZiJtTQsM3Xdc2hQ3ZvlPJ5f3yPhJU5Z36A3mmaUbb0cvsiuplUSJHUkUXqxWeAzGVptFnddN78ut3eSsDJvkMSd0mhbf-Hdht6dnPt1hKqW0hX301v3iCuB2rracCff8iiHCSgxFTsrH_rPJPqN5GBfrkGxjTwQuulL6iG6q4cOn41Xm0KUXd_viHy4oYszkLGK6WSVjKfYqhskMlWS277Etmt2VOzDF3qbLjUgfJEcVButIH39DdSVYtmWsYJrV1gyyavsELIowqXwZ4HHleIJbQfYIm6n715LnNtuHTXrTTPo2I7MZC3dZUO04ZjtvPA9Rmq6zLydrIjztChAZSOl6VX4NUJHmwNo-jqWEJYu1Mt7caAScWGlb9Ni_7OfBeXVm5u2OqzlnTIhAGnsYzshNs3uOHVYM7uQlUDAEzH9oiSVqfX6nz5odWbYOhb0NgczffRwqMqbiyxjL19_GRe2cHMqhOUFWn_t4DA__o6aUObdYlFPzzYh2RW0rqZ0W7Rdq42CK2dyzHdrXn7nVdDGVAqu0VZIhf1cETJHE3wLQl-sqA9dg4YEN?data=QVyKqSPyGQwwaFPWqjjgNrb9P1OigUKIIPL6sh9bQMSHMNwv4zs4V8kXzDCOoVMlDNDn8K4e_c_D9XWWe2eXTFhzEzNqszo9V9jbxoqiUgvbanmKPLdk_8B-Fkq4HfMbCXwD5fiRfElHxHHQ4VsbmJOyYwq5qyBo0DrKbLx13xOmWCJX2UNzUYjoQccnnCZIsM9l5Jz7foynnFGThOeEmcPALpTepq5Wnf2UtLDUOCExxuQF0iY-NI7BOR3E_7e--DpOTScIHBiUJ7CWUihv_yYTJEOknS3fQcz2coGBNyV_J_fXFfjHS3_YnO2eDaC3niJjceyGHEiPN70sgc-g38QI90IVOdKrL3o8vt1vENluJRYps3kpyrC6GDU7QDq9a8xKutEe6bsH6RYBKajBrOTDxtYg7BFnmFFMojtQZGpPDMwVjjJ5Pxs-TrNJyvBoAsdlwM2fLqyNxfhHzUUJZlXMWz2Duj0c-hyzV_zk3AZ0TgksaoyBwpYakhSmQ2zFElejlRpCovPdxyM4Kl8tbTFQWQRMyF9lh6MK4VtvoN15r-ffutdFyR0YO8eLgkBe-tqa-Nj8W9g,&b64e=1&sign=619cc79c239f6dfe1cf75e1e40a1ebff&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCIuUwUF7cHox4IE8nmKF8jBMVcquLntQ_qAlEBPajtVXkj7v7gT3hPi5uKXkAvPwmB0hxKNnHVIBTyL__lXZbNCn7hM6HAe83Hv7FRKdZutPPj0Rp73mmORQtOkbZXyPUh-CYU2y4OVCmObxJ0_Q-K8p8dI0-Ie5KcVlo2EKXSf_08PImof4A8FZ1VZ5l8o50fvVB8rkL3c49t9jAFmb3vccL4Q4RKDGa526SuLRK26EH77_udYmidl0P4vg6sO7ToGrMT6lOwtWtiLl7jRWlrOi7s7NUcm3vHOG0hfuYzFX-vQ411aWyA0rfTR1iApaMDOS9Bi5SVnDPHkfo-fokZYeO3KjABxk01nW2p2EZNUMpzO_hbC1Y-cnUi_SZL9zvr6RzdcoyP4P4xt3MN3rLoPfSLX1cYU6zh8gHYj-bMrf5CQi3qY6pI8U4eh8ImNbeKfI77Q90aMy-P5pMe6MABemtk1ueHPAuvoOQ0cWQbE5a6hsqUg5LLoU16g56EAva8w9oShhka1S2HkdnasMTuBr1gJxD19ypMalxZIf9MkcamwMX92izvjs3v7Yv9ZVqpkq_6FVlkc8mwViTM5hDUVX9sxKOJBXo4JZOtYMeo_uo4bmLlUjCpmEQJcyvxj6k1YulLyztXs2LyMNZc5633-wUTbkSXmn007wQDsmLca9ep5UQQqghpShpv_W1IaUJNGt2rhGEW8e-qSrc8LXhspwUihKg0r08fdQJCKsrTeI1SQLUGETBHEvj4x6PXSsbDKAbEGnD7d361-iNtEy6Pj1AhfiD2FK_EQjQt25KvdZUO_WoPTh7mmm74PzGW1fQSyORC4LRQenb0D9v6W85DePs46Qfwnam2rgqSSN1WhV?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2WL88NyjVmO42tmFuOz8hghMaU0veOKbOnnCebbCDiq57HgN49FvtFU6oaoRi_VJKKy2JVVct6VVySzm1WPIgnu-TnrlZ70FDRlrgz8GS02TpwbjbNT7l48,&b64e=1&sign=73ccaa9322c6a3998a5479ebb0a871de&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 46,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 2,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 3,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 4,
                                count: 0,
                                percent: 0
                            },
                            {
                                value: 5,
                                count: 46,
                                percent: 100
                            }
                        ]
                    },
                    id: 264809,
                    name: 'МакЦентр',
                    domain: 'maccentre.ru',
                    registered: '2014-12-01',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, ул. Школьная, дом 47, 109544',
                    opinionUrl: 'https://market.yandex.ru/shop/264809/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7 495 956-68-88',
                    sanitized: '+74959566888',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-63mfMKlMSfoCc6mdWpFQhvfZQdVp4vcCmn4KU6ltuQehbqw4_oYExka9rdP6N9K3UixPwyxpU-hsAZLWm2JYEn92WMEqgbJiKqOV4jjh2RauzHfdkzIWwawB5p410EGHpmotmUDI7CP3QjM8v5oqmLeNx2l_frYBVrnjeLjZUBScgGZojjMLky4NU0FmDbs_TSQJupjBaSZSf3yijZjW4QKNCeo3JgUFdK_tf2xVmWddKgQnIJikZmn6fCmyTeDzHv-FwY97AhtiXV4Dz05_CbFLFC4nYwv8uDmYc2Sb8Sboo4KXrxwTv2VDDE9jEWaCSySwmV0V5v1ZFbRHfWjfz1WuaiHcHve_4mttXI6Q2r7cCU4UrM4uIyiE_gmWYmdLkd_b8x374pj00RmDRvWEHfG2O9Qd_dUHPyvU8aBGNBRuvjQQszDM1iYZG_gZb9_TLRWoMIP9M7MAc2xcq3IDvTwTEjQkk0R4aqC5Td9BwNRR7BC08uRwuUPXEQ_HNnEl1jyF8syfqQki2Gxhza68xupVN69wbyNfCgJ3FxMHKB59TpgqQfPr870Q9FYPI1qbu9ujwUhEhp2Q4oHjnR78kXoix3gTz-PAa8i4tdZjQzubsMX-P6cOz7GB47qrcHv7g0n0pNCJjnnKgArK64NHpxlLmrEiBfGamoCdyrq1o5T72dXIODS2XV?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_q5oZokwkCdnEyf0DwgGFhzUzI9cFrNduf-BpmyUl2x5Ft8yopOuPsEZNhvYSQ6HwzYMWIF-IqEWHSyjiPnWl4zAqwazRZy_TBOyoYIc_tohVxKxXLhSfCEkj8MU0dA63GJR5KEIkqeRg9J0vhBV3BNQ1e6VR0rYLAmv6jELYb1w,,&b64e=1&sign=145673f928561305cdaae5878489a886&keyno=1'
                },
                photo: {
                    width: 600,
                    height: 600,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/202381/market_3tGTA-ZKy3z7RIbRr06P0Q/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/_JI7NJnYZ9J3pLQ0TT-LCA?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HL3mXpTdJqrB9_ZHDMjFeyyF7vWYyIewK8CohJ9gkDZzVzOMvQz7BDvCD_nL4PnTBfXA9o0axY_h26xeascd5MabDwyBNpTZZ5IvBdI3i2wDjcR6nsG2hSACp5zLrK-RQU%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgVy2Z-7dybbPI6Hpde-GT9KX5y63PKAt40k5djrXt19O5yLyyik_9eyHNNtH9H4UdV6x7kCHM7NyjThwEVRc89Iha6heBNQPeFsC177j4LtE4FgTC3k4z1EfF_haQ4Q57fy0qkTO7Zge1RD5cqZU4mh4nwaXAsVoSDIGOVhhN_8YYBGq7ZevK9jIAwiEH57hWivGxYXj297nOpnFSwhBkaSxhr07cNH0J7hevwRFJnbUowjc8n0OooBcHsSySDqLdMY0XJP6oISjh028btiLI4-VaK06fhXBQfrcAioPeBTYphJrAMRqUASghQZyPT2ZsgTv4qGG5UXoqV9sqaQ10ZSQePfj5hEUuhg8ufTbTTKRqAkRk21Wfj-O-gFm91IztYMHa7J1ON4dHMh7MUlo2g4dJY4I7KgouRkcWCs1EgCT0aaFpf16qeV-61XlIpdCG-kfXhCwEjR08sdwLJ4U40Yytxax9-YUUZg7EIdrq7ahMOh_DDDdI0aaGs2GuoeRWljE_b4UVXiwMjsrI94l0G2buhM4HgXPA5BEqT-DUPV8kPardAJzNZ3L2eR6sll9YLtWr6BsUCcEF6BId7or8ajRlomVzftvFHP5wQvTNquYRd1FMyxQROJkVvYk3jnBnkXfuM-KuXa3E8qgGbfnGftlcmoN7LLnoDh0lSYP_7Uoze0zNhTP3vO3GPYMslE6-Cf6aIT0RBWYJJ3kCUe3xqBi71xA20PDCAVttofnxKw-CdQMriCHkM3Nu29VniRWBzQL897TZxLWyBbwn6TyqMt_7oIfIEAlel_2PA7kvs7WVEUp0QgYoPxVXW-slclcjC5pk-A-1xmJykEAbUn1zoaPSAAaa9N8rtNazHFT7eAe46DfON488p_nvNtdVDJdfGsQHipG2XL5?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-5HLjAIShU3gpSoj_uvouapot232CkytsdtBklQp73B0I8dAUWUTmiHnCfCpVZ66ORnoKQfcy-qPppPH6vUH4UNkq0Gj8E8vODq9nypf0jE4DVrZyZgVsx1JyS8O01S8LXF_QW6Lhw_cqax9vIo_37H9FTerrvxxbM2VXlzsl2Nfo3eTrieH6_kN8zxavc1UqDh4teVlvuQ6AGOWCv8AwafpkMGRUd7KRnfM2CeqiGDwe3xXQVXxYw1Yi41KdEMkAeDjDrCDdy31RPkmD7CvSZ9krmWGVQU1bva578G__EBOmU2NHRfNanBXM__ZAOHuJZW3LlEjAfaEA,,&b64e=1&sign=a2bcb319ff753ab8ab599d92d30d32f0&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=264809',
                paymentOptions: {
                    canPayByCard: true
                },
                photos: [
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202381/market_3tGTA-ZKy3z7RIbRr06P0Q/orig'
                    },
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_ToPa26jRGXYdXBfsSFuElg/orig'
                    },
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202381/market_ekuoO5mu8H_nd1is_mFd-w/orig'
                    },
                    {
                        width: 600,
                        height: 600,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175127/market_4QX2j2qA1KUL0XQusZpgyg/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202381/market_3tGTA-ZKy3z7RIbRr06P0Q/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_ToPa26jRGXYdXBfsSFuElg/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202381/market_ekuoO5mu8H_nd1is_mFd-w/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175127/market_4QX2j2qA1KUL0XQusZpgyg/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHx221izwYiFNCERQht9iPWJgzLxuLNZyT1SOxECRN8mWDdfwsG3SXZEWoT2MN0Z69KLqK6Kmto20uzH8NP22bOXYPjDj0pgXV7PTLY86et-CWYKz2JYnyHkM3iISu4qmhckP_1MmX4by1JVQqn9rSQYPT0CNfd8a0dXHYpYnjPARhjP2OvwdwXg5NAUg-dvycbL1vtuBiB_vf0xFIAEb7SeZNOVGiWobA8ed_p3weFsC_hitcRTx0Q1d4aRpH48QzQQNtSlToRUZTt-PHoIRppHNDhQzI2CqI',
                wareMd5: 'AIiGB2Qn2IXurn3wV2htFw',
                name: 'Смартфон Apple iPhone X 256GB Space Gray',
                description: 'Смартфон, iOS 11.1 Количество SIM:  1 Диагональ:  5,8", 2436x1125 (Super AMOLED) Память:  256Gb, 3Gb оперативной памяти Поддержка карт памяти:  Нет Аккумулятор:  2 716мА*ч Поддержка LTE:  Есть Размеры (ШхВхТ):  70,9x143,6x7,7 Вес:  174 гр',
                price: {
                    value: '77990'
                },
                cpa: false,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mfvaAqkgRh5xBn4g1wJm-MTFtBkRPawpkIZNS7EbIE1lvNcI-W-kMC3QS8DnZE2L2UcvvgU7gr9_SCtG9rQbdAoGWPQphbrGdcZeW8_jPCLfk5lkGWGR20dpbYvXmPViYGIdaJM7nfCxUnQQI_NKaSJ0MCZXK2rRzOsTj6N6z8x4SiiwCLjpjBcwJEsz63-vGe4IZ2bt0kdZzkjHsTviKpSEUJ0yNu9p42ObLlB4FKkA4WqHRldwtYfpCL9icyGvO3503jyXv-uFPpmK1Ggpytqv18_Dmcqh4SUKoXo3QJ8ioWM2djYaQUBTn0rMPRw-dBtzVFjuFtkvOvWAOM-e-W_E_LfsPS2Jz7KpQBxshBqWA16kFAWPd7RuWB9J8-jg5CtnKNapT514lTDZDr40uwBt52ejs9EtN8VnjIuIBSAQjlchvvlCgem7u5htY3aDrMSFJwfvom5qw3ghxRmoTN_hmIvN4QR7xNgdIqLWrusMOB-D7t72DZUXfmiUcqWRJYHuZ_CybGT7xGusEw0cW2exU_p8edkS7-BAAEPwURv840DCRGw9--aMBegCLFzoPb0xHqdXMZK8d-dudNDVH-BxLdrbtNsk-8vRKS9Jpq3UZjYqqIKV8FyYZvmslFijbYKR9pb8Kpy7e3MPp-GRcs9g4Vc2nuUvf5QnRGSg1MTL_m95m9QSuYyBqO9YvC_hp78UpWT9yRKgtOjHEdRcZyvFQJgt2gjkJuDxnA2IeZXTL76XTi3gu7Od1zsIbly_7ripVVfbJ1iMNpSkKcecsF93bgQ6JcPYkled4ZnfbGBcqPZyM6Dej_H12XQTSbIZK2WQzMkeCzbH7rKSWTr3yyd6BzSnBAkGvxB0XSxFVtwQrZZs1nQrBh8,?data=QVyKqSPyGQwNvdoowNEPjXSYFXgksUq5RzxjumjV84tJ0eRDZjmE5imTEXgI8WGoqEsqE-IlbiOXESabp75kLUhI_KtIjBqAWlkKsOzIg9ZF4-K-5wk4NgWihTegYuqas25UkQUP8JMA0eozF_gFBgRnfwXroT7VapIeJVovtZ1cKPCfMMDiN0F5zzcs7LklQ50PCHX28N8pe7hZcbomYa6DUI-ECDO8Hi43IqPrwKEGi7q_XYdArfx4QYlJyBals-d78IPd1l3s97RX7LvwjFDjYmsVyLKZQ0cguwCCMdJGBMmuqDkjALZhrAiYuzLUj2EumPod2DyVMg7I2mPJo7_7asQa2AKQ3EscW7nn9dqc3Ubck9sA3PM7gUCTtr_30c-NRO_s1nAk5XQr4INpf3wxicZSOAqmMj_1y_F1N8we8gcL52UdFMSpVauFjM82mWfWsyqPCuLpcaOrj5kucvFcbOGMmFnL-af8q8-cAfDxuyTjy79w_1lfT9TcqsxRUJhrHU1DOj-KSsVHYG5E3RLIrHs0FsLFPQcPdtmwN0_Jh_WiYB7st7TokM1mfUJ899EewpUaEjClb9dVAhkoyg,,&b64e=1&sign=7d5fe4f394240d6f44cee2d1fbd0d201&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iZD-92KaC2L6PArX_SvIUUeRIf5EbGv8qPl8MLsjMG2Q0embOlgub4VBWPOPHkARbE6vna6Z1JBSC_TW4JTfoEPFIWLqLZ989mkIOpfz5qQrMtmRDibvRnHB3pGj-swkI5U9Hh2EskoqgOl8vpCE3gsAISWtpFTtjkCy-rydJWn-SV84-SqM1_aXtrHN9P2jOzziCJ6JDlJv3o1jkG7yWpMCskKvoyO17JoHd2EUJoeBsquIv_4XNjbKkKWwzYDubpUrlR0dMI2I_A-qUv14pOpLNjGLpoQtPQ8uQsEFyyDOVp3hrTiATjGxDV21G_i5g8n8Vs4punHm5FCLx-33iJffmexEb10sTdYA-8SOBlOiSHUp1KkJAV7AcqCMyjYntTU_C5BHtzxouJWA6tLf3hGqbOUEuVOriQSOUA1ChRD7NwpqgbFDsWqi1pKEejQYuqoJMVHgkICp4lyU6yZpx1BvpWRcUYqKIo5SRu9VM1nAP4I9hIU-sDts4bdBVeB2UPJ9bkRv7_EM7PaoAhu2-DQLpjegxQVD5QCR1AgVg-orj_c7Nhe3_wGXvy3P3XF7diFbVmzIuaPnTWzMUd1QdbB7tqjEp1upFQa_5_7sTIcCvVqJTBzdGBv317u4Mcl78Ru5zUt0YRh_mQjY5rKD0bqNyw8sk3yKd71Y5njG3zFyrUluy22kRji5HJsPydLIs84dqHJbwS9f3z21tkfRIVYoooIXLzxxZPw43TsqGEJzL5CZIpKR3R400O67X9V7-vVfbSxksCkjfAjDJClf7S4GJw_2IKSxKTRw_dabt3L0Q9zn3KAkCnQn-RNbfzhnkA9HsK145JqBC-lH4qmuh7BrpicN31lolWAXbrfdkdWesj729mmutgU,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2dL66RHcfutiQubt9jg1Sx9_Mpmp9_tD_anujbHTIXzTUOIEGkuurWPCCO1SoInaeC56N0M4UL0hZKrlif5IrLTb-UnRc0J0JPXEW-jkVSVV8tEvuOnFCz0,&b64e=1&sign=a297d47e138b988b78c9cd10cf355ee2&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 3101,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 178,
                                percent: 6
                            },
                            {
                                value: 2,
                                count: 56,
                                percent: 2
                            },
                            {
                                value: 3,
                                count: 52,
                                percent: 2
                            },
                            {
                                value: 4,
                                count: 139,
                                percent: 4
                            },
                            {
                                value: 5,
                                count: 2678,
                                percent: 86
                            }
                        ]
                    },
                    id: 112249,
                    name: 'Gsm10.ru',
                    domain: 'gsm10.ru',
                    registered: '2012-07-22',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Сущевский Вал, дом 5, строение 12, Павильон Л101-102, 127018',
                    opinionUrl: 'https://market.yandex.ru/shop/112249/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                photo: {
                    width: 344,
                    height: 689,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_c9XSmO_NeWkjHW4k0lhYiA/orig'
                },
                delivery: {
                    free: true,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: true,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — бесплатно, возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 1 пункт магазина',
                            outletCount: 1
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '0'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/AIiGB2Qn2IXurn3wV2htFw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=uS4PoV-SX5NUGrpxF_KDkiAJPrtZpTTV4-hbZ3prJ1F5b5eQV4gE8asnTUJbLK6W36phsfgNg3dUd_KvXJ-UJbuLfF-rtf0riRi3LIhfeYW70uYeRRtX8OGgLZFe6lJM&lr=213',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=112249',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 344,
                        height: 689,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_c9XSmO_NeWkjHW4k0lhYiA/orig'
                    },
                    {
                        width: 344,
                        height: 689,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_cgbQyUhXaGqHHpXzMqFhMQ/orig'
                    },
                    {
                        width: 365,
                        height: 725,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/216195/market_HvYZVa9MDLgD3LT0g8gslQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 124,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/249073/market_c9XSmO_NeWkjHW4k0lhYiA/190x250'
                    },
                    {
                        width: 124,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/163900/market_cgbQyUhXaGqHHpXzMqFhMQ/190x250'
                    },
                    {
                        width: 125,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/216195/market_HvYZVa9MDLgD3LT0g8gslQ/190x250'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZHCeKivy1H2EmTMa80TMMqwrw0AnRX82_Rmley-dbPKdBXkfoQTlHMllXeeY1bMGeLJOGFsciYe2EUefou6tG1IV5pu666P5i_5eE8tmIzNNnaeTpXYHQFcL_aUeHIBENKPj94Qx3NPG3rXQVmkOorutRIdmQUHPCtF2J2k4CzIxtyI9ZmKAINsf9RfFSYsXGeZgviquGXRPivRkj2PJeRehnanoUxUYV-aKSedcwmXMb1vtvexAFl2oeyfbPmAjg7eZq9kdhlEcsvXFJslTyCTPWyZfbfoWx4',
                wareMd5: 'XSpntTluZi3U_N88c0DFgw',
                name: 'Apple iPhone X 256GB Space Grey MQAF2RU A',
                description: 'Описание Современные смартфоны от производителя Apple работают под управлением операционной системы iOS. Мобильное устройство собирается по принципу ПК и справятся с множеством разнообразных задач. С помощью телефона iPhone можно осуществлять звонки, просматривать ролики на YouTube, общаться в видеочате, социальных сетях, прослушивать музыку, смотреть фильмы, пользоваться Интернетом.',
                price: {
                    value: '90787'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-4mW6Z3uIuuA3HOaQhHaAOUr2t9Zd90luU34af_1ImaXS0grti-1tvwUrQQ8zvOKzOIdZ4y4lZOBS26LNHYqWszJNwoY7SeclxBD0xbVoxH4V_4G369wwJVgapxDmZ9QgmpWoLmTdCoZ0BH-1RlUCEsW5Qm9JPDbknDimVUX5E5xzkqrmveM4G7CwcfchkePZMGBVm46dEtcsmx-Nmwj3ZHvj5zGbt6k180nO4Yg1K8r2JRxSJZsfeP4xBiWu-M7DWunJB5AJFSSRDWhuESSjF-bLuXL_iBoTnnYYlZ0wHpSgOmptm9eWI7_O0LArh0okK-nhsUSWSVz-MjfFGwrZrDy290NE0BWj9T4SQMRupTf1cfCOBl7XlLZvLujG61EOpbIAt3XDX5439xllum-uf3r0iKI0O7GTBhd2J1YsF5SpMsI4DYbLA6IG-blG9C1HTgkMLE6WkwXC0TnV9bFT3g6BXqOuimSkNMJg3yIjApXGNfRKzaPFgvnOFqdkbljZaWFYWKISNO0kRKD2JURHpMNHxaJAA9H6tDBE76J2gGNZiezOA4yTPzdQC6hmyuCDiCf5WgBjCrntC2KxJ_-Tt__IkotaNHqB-M93Mj3b1v0kNnmf3I1GqIn7q3PWePuaoci9JfCebVDZGZcATfjMAIrEXfL6vOvjkPPRO1XpG714Jj5ICTb-fa?data=QVyKqSPyGQwNvdoowNEPjbkz63lNVRWJ73iIoEpyZeIXjRayNWjPggHw--K0ItNRvF_7BpfNqxlWHngTKH2R4hikdp5Y6Gv07uQ6lvwxLItpOZrq_kGu35twkRWiSDzbL-i7m10HuCe4nJrhb3vvp9icASeDd3_WjJWGhahHfBcIdoYbJHT77Gxyg-zs4tZlQKUfLXRCywk,&b64e=1&sign=7b95351dd4c91563404489aec5b8e8e3&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCIuUwUF7cHox4IE8nmKF8jBMVcquLntQ_qAlEBPajtVXkj7v7gT3hPi5uKXkAvPwmB0hxKNnHVIBTyL__lXZbNCn7hM6HAe83Hv7FRKdZutPPj0Rp73mmORQtOkbZXyPUh-CYU2y4OVCmObxJ0_Q-K8p8dI0-Ie5KcVlo2EKXSf_08PImof4A8GxUUnR9nwfY5ECin6cJ0jPhPbx4gcIyVzfi-sMcrTfofhNJQeVR2qiNtqfsplzQoWcEWoS_bSPArQweuUetk2bnmK2MDGNDo0_7SbJLhMDgYd9cWfXOtvXRhrH7dhgXHW0COvBcL8_Ontd0Qy5XzBT5jhZHJeGe3ZuTGLLGSA1rl13aQe7wRlw77vjatWeEzQ9i_7NwOiWdLFIWrHwSPVVN-8-OfnwgrRUiRXCBhlqzy8myivBY3TWB4LqI1bDN5rhaygGM3_t3EbQ-ig-tY3cRhzhur8VhOeFJfJiTrdxnl7FeGB0F4h3_fUyErA2bl6S_SCncW35VPo1qS5fkKfC5FWpuD5ByXfVlOAuiLrMvTHLh6yYtJnRO08fKfb2508L-mAGbi53S_eoDTWXZ0zBhtYtUcjHjEh4TiTDZPgzzO4Xa9bouYE93EJblGqbR5f7MUd58xRjPhx8j0enjGYurOs-yRoI293pZgwjl1dxVkrOKxSOkkByiTjRqbe3-n63URBv6qAFsNAN26qSnFrEyXJn86l8o3zgS_T69x7OBTbBj6Fgq40TviIiDBl01Ce5JQ_0VqY022UjlyMg2ZMSjxPVsZjd0Wvqg6eW1KwG4dZAfEw_xpAgjkM0Xg9NF8Ug0CxUhOXMoGkKH4AMrJhiVu3EoQvhQ0Q1_V6bqBzzrpLwKIgGCRIu?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2QFDV7d2AiVOlNBERPKVNxMCMLSLcmA0CSx4jrYVlkSoc3HeGLSuWS2Rz78t2Bi1qgzYd8JjfUV5Pwbi8MDDi49iIMbpIK321E3xcnXQaRUXrMqreOF8EgE,&b64e=1&sign=565161558d2b613a65b69a26031a1788&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 3806,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 637,
                                percent: 17
                            },
                            {
                                value: 2,
                                count: 124,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 103,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 519,
                                percent: 14
                            },
                            {
                                value: 5,
                                count: 2424,
                                percent: 64
                            }
                        ]
                    },
                    id: 12065,
                    name: 'КомпьютерМаркет',
                    domain: 'computermarket.ru',
                    registered: '2008-09-01',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Бусиновская горка, дом 2, строение 1, Москва, Бусиновская горка, д.2, стр. 1, 125599',
                    opinionUrl: 'https://market.yandex.ru/shop/12065/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7 495 500-01-75',
                    sanitized: '+74955000175',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-63mfMKlMSfoEC-RMeRpJU3HrpRJEsBCE_T-WTULVhTdnKTLZBPhNsJwbs8Zn9fdUeloa4B6ANuZlcnkF9tyH1mPdONirLd26qfRyWd7c57aK4-kPOIjdnD376tx2LK6HnOrsPMEarRDdVqm_V5P6NBckat6QBJV3JlnTt6xeIvkxTz5cYorbEbsh99hSQS6y38iBsEjISi2zrimZC4nK1n1OxSnM-1M9SgxbCgyzC0dVTddG-ohUO6ezchIZR1C7VxDUhrLNzGXUyyhqo3ocF2eIfeg9AUcKnCj35BasuCvtixAVucEe43pkxM8uKfcq8xbpV6yV2BXJdrSCCfT6W1bFsJXL4ptmAhQEEGGSHaniLzpy6feNb9Sb09m891hvC9VBpRYFdclBon1wkC8nnlXvFG0PAko2wCy8cEenHJCNvsEQ3wwtYAmi3Ix3SugfXnxvEij3ffOuBGcx0szzrMIDu0tOkiabUusP97hWaxpfRCZJoQmN32aXlxc84ZShGTzyxC0OOO7WvKda69oRSJdGKGhDN_VwHHFcd2UGDkjmKVPMRQ09QDQkFvVvMmeldwJrcn69DtjozsBRyTDCXoBkrarKfzeFLWXSgj_Y6QuYWL77Q4o0s-BoImsKcgj1JWA604YjY_vTDLFqlW0rXAAYxYfBvYR5jPYUmD-EmWlGo02yIcgnuG?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8cIzLUiMlfPz8ei03BBbNg1bb5xuE7vclavysgQc9urfJU5guV1y905KW04s3VysVq81Snm1YoxkH0urSC2wuxgKcqj4s6P3ZoI7U-453ab8hmKnToWwKV7ru0z_oFwPet24FinkPvx-7076li_A3pZZqZ98JOoHt4GG3tcuLbPA,,&b64e=1&sign=95c67b417ae1064046a8a454804315ca&keyno=1'
                },
                photo: {
                    width: 347,
                    height: 347,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/239999/market_QL1auBg3OQM36ks0wWlRrQ/orig'
                },
                delivery: {
                    price: {
                        value: '295'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 295 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 119
                            },
                            conditions: {
                                price: {
                                    value: '100'
                                },
                                daysFrom: 2,
                                daysTo: 4,
                                orderBefore: 24
                            },
                            brief: '2-4 дня • 23 пункта',
                            outletCount: 23
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '295'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            brief: '2&nbsp;дня'
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '365'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/XSpntTluZi3U_N88c0DFgw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HIL4JwNr7_3LKEIh7J9Ab_26NXdAxKue9S1GnOM-vC7uOtGSnNyNHd8g6_7OcNpCFSixrOqlgrIK7H-kgnu2RNfv6blevCGAw3TjyIfkzAGvkO_qaoO4ciaqm8igHg1t8Q%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PDrOPCmWRBOgKMoMNhCLHu9YHviNKN2brKunRmzDXTVklLtivWrKMEv1Vl6OfLwGtFJ2apOicJFnVt4NJUZ_ljzY0jcPtm2YUZEUl6lUdVoptdwHkiYefGmpXp3iwoViTVba0NsEDCBfwMzfpt8n4hOzWMTCaWqHWCkgS5-I1blRqe40Aeg857IQ53v_M08Fm3WXdtpKrr1EinR8fKV-RDcTL6oPA4oVhi6ykbtEmes9cNkF3DtnyF8zHblFfpvRp_5IdaE6oEg_qr696kGEHw49X5SI-YH9AsjXDk39QBIjeOFnrfxisbtu_ySbsFLCs90vtfCAQXpTibcNeClBC9srQNLoJOC0nw3tsgZ--Noq_8FASO6yS1M1kQpr1kQXQjCdOXDfEXeqkEIs0DU_nLVuJREBu58ePk3QlZwfGKH5AlLu3tmgMGDZGMItp9aUwqtJcA___ILaNWMC71Lv9Wqjy6QwrvuErBhEWCD5J1qyRrSHzKgqCv0Wcr2CJv2JykvuWYdnQVOBkPjLXXxlud-t5VTqbPKtqJRpDoBBbYNCycs6cbwpUDy8x9kIZ1yY6qPu30SuL2kUcs6MoYSPRngih3Q_jXIwKiVnMlNsN2O4dYwfmyuCEku9M4XROHGfIznL721zM3vK2wrThEj7LaM9oLnXy_2gOGqNH0gmeQgOr5SXI-sUFaAcxzW91Onw_Ndw6HIxtnH?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXUdWXEAbQxtsxWdun60_HnpDXqb6k4DelbescQGpZ4RYnuAns-Ux9tne75rOiSb8MFtMq-OVPq6dRjl6qp0K58efeHwEojWhUhZIPCETf-GjRe7RfdGYWqgsp6o7SJkpAQ5QAY-kFTNcNoBNV-RVDmIl8db7WTPq4Nx8n69jESXKi-g72uhH8vwZfOP88NSEylK7xiYW_B_YFzot84FLrg0iDMMwXgSqW_clQe4ptV2a7J0M3XVp40AlewpJOvU-2kVw80qukQVTv53TDRmF4kYGfMiULZq-d6SqXVDRRDAc0Kn9rLg_VWg,,&b64e=1&sign=3c25abaaaa7096271b34aaa53fe04498&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=12065',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 347,
                        height: 347,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/239999/market_QL1auBg3OQM36ks0wWlRrQ/orig'
                    },
                    {
                        width: 347,
                        height: 347,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/372231/market_kuRZ1HiiyUcO3ROLK_epmQ/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/239999/market_QL1auBg3OQM36ks0wWlRrQ/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/372231/market_kuRZ1HiiyUcO3ROLK_epmQ/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZGMqjgMFcHpbOTRXLZf4An8wdpu6mtfZWfqfYMylRpzXRn1COYVsB931036pRNSTdLMQyGic4jdbCpAdrjxyX0czcP-C9m8e7dz8EHXwf5kKiUn2-Xs9zwmO6V3vCyjkV2jDFCvpVhPGUkoiDph1oTTAavZ9EEqMhm8pGY3R-cMmu8ySEsjNgWy9OrYS2g7MV5LSYFUKeOReIiwpHTVu9Z0Zre5nNKi1gj5O_DUJqNEdgfg4wQ2WYJOf2FY-8_8MnpdWcg5INx1iRVaZSCDmkMwGrKLR1Dq31I',
                wareMd5: 'OLu2_AMb6DJSlvQlZIX2og',
                name: 'Apple iPhone X 256GB Silver MQAG2RU A',
                description: 'Описание Современные смартфоны от производителя Apple работают под управлением операционной системы iOS. Мобильное устройство собирается по принципу ПК и справятся с множеством разнообразных задач. С помощью телефона iPhone можно осуществлять звонки, просматривать ролики на YouTube, общаться в видеочате, социальных сетях, прослушивать музыку, смотреть фильмы, пользоваться Интернетом.',
                price: {
                    value: '90787'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-4mW6Z3uIuuA3VBq774KnHNJq1UMaMG914TU2na_pBTs8MFmKJToqe7kuy8IbzTsz0zhhGMcIksY639Zmz9Ea7K2KqAOBzIZST6DMV3ScRSQ9hYxlCQ3vwmW3xlsp3vy6T1TA-YX6DOG5MZm6MVqyicIHpHwhcflZzh5XvqvgBWs-GzNHF2N3ioanQOaI1oROKUWZ7yKzRXpw3jhtQIim1Xtko4GCVjjW5gNJy_q6-_fzSiRbuciN69IZLc-B928p-mfTDQItNUwh0sSPXlmMdtCRbwOcZEFOcW2KNcdAdRwxgoVrCIuF5z_LGb9mb64-F-frAT7nHDKz42N1Jwp41yPMsfaOV5z85BnLmbxIBhTDFIVeH6q5326JbViSCvSGliacchQtX-1i_EKIfijf4PfZb1Vjn2Pn4oWuPkepEpJFfJIGH9qMMYeMh2xH_hwAGzpXSWbDBZ-S6_TkAA1TNBITEZszZLSHdMWVsi1n4ZPqggHmmuLDtt6VTuXHOxBfdKBWqIuxBgDSFiPF97s-b8Gr2hU2BKU9bD4qOz6tzOXwuyL_1HJIlDOhgWuOC6x-1dqmS7yHf-1gXVID75-ML6k9Ye3IrEt7rEf9QUlu8UOkLXvYytJ5WXrZrXoM5cR-iOoXPmh3uyUK3cQJunl-UOlF_UYuVwaOA1YAHZTiEvBSSOfVnxKrcI?data=QVyKqSPyGQwNvdoowNEPjbkz63lNVRWJ73iIoEpyZeIXjRayNWjPggHw--K0ItNRvF_7BpfNqxlWHngTKH2R4hikdp5Y6Gv0X9f3ClSx3Y14vp6lGOzAUN_ILvdES4lu7vB3_NYQk3zeC0kxdVdlp41Qk_Dkba01TUHlnqnHRuNapkHc6BUKg5loRj05DKBf5gIRgVEXb0o,&b64e=1&sign=61938aa77bbdb17b924f93906e58c2c5&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRtNkb9mq-LhCIuUwUF7cHox4IE8nmKF8jBMVcquLntQ_qAlEBPajtVXkj7v7gT3hPi5uKXkAvPwmB0hxKNnHVIBTyL__lXZbNCn7hM6HAe83Hv7FRKdZutPPj0Rp73mmORQtOkbZXyPUh-CYU2y4OVCmObxJ0_Q-K8p8dI0-Ie5KcVlo2EKXSf_08PImof4A8E2mknx87XeeJ-J1XWDMZxmtS5S4XYykbDgZTnKbe7PYtilQn6FH4pIEwvTCkgRw_qs8W60tFj3xR8QLYW8QnZi0uVho1pH8ZN0l9FeT64uTZrR8Il-s-yfE2Nnxs_Ju3SX6hArCAqb1tuNCREKtkotggWn-mqmi83baHq0Ixc7OwMZtHGyUPA0ZVMvUZ5uuQ9U7gLe7NTsqF6nKkwPS1MJCpPAD0MXdOuSX-EEXJ-eAZSOeFXal-emZoEEQQfkfvghFcsPM8i2EoiCUgZOrcA34rLJQbqdURG0xS-5Zwn6LQoDNtmtc7VqZvNvYta-0yEnNq0wi30WSatHGW3jZNVbqINM4BShsOvLWdq_hSWzyKuHafmXLoO0cWdDjeAyVxYSPvB8dHVgs2GQvc1Jw_60mJsotOuLTa-I59J8ehgVdeyOdA3eJnold1tDPblP_Yuo88mnDrq8kUiIamr1YOTpG_IGvmINLmmpyqvbXtIyAvq_VGU_LH1ilK5uyjSzjc5b9QnUQu6QEYu8iF7qv6EUySPL9U_Wz6abqHnJ1a46GzCUmzTNCuNWDU3c4-zpPnuGAmG58C5hetd383qGeNFRTRKHiJMU3jyf0GT6PR_q3x3QgzIiix2K6o2axQaEVxL0CZk8lZOOv8oAVfuQxmcTScVq_12PIxmV8iHUfCV-VATtyVca34x_?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2QFDV7d2AiVOlNBERPKVNxMCMLSLcmA0CSx4jrYVlkSoc3HeGLSuWS3CPQ6IiN_0ypic-Zoo_7Cgdp_ULmq_YTX-4p0Vvh0ddtQV01gu3jl0MzR3E7lYNl8,&b64e=1&sign=adeeaec4dd06f430394f0ee9c5a831c6&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 3806,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 637,
                                percent: 17
                            },
                            {
                                value: 2,
                                count: 124,
                                percent: 3
                            },
                            {
                                value: 3,
                                count: 103,
                                percent: 3
                            },
                            {
                                value: 4,
                                count: 519,
                                percent: 14
                            },
                            {
                                value: 5,
                                count: 2424,
                                percent: 64
                            }
                        ]
                    },
                    id: 12065,
                    name: 'КомпьютерМаркет',
                    domain: 'computermarket.ru',
                    registered: '2008-09-01',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Бусиновская горка, дом 2, строение 1, Москва, Бусиновская горка, д.2, стр. 1, 125599',
                    opinionUrl: 'https://market.yandex.ru/shop/12065/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                onStock: true,
                phone: {
                    number: '+7 495 500-01-75',
                    sanitized: '+74955000175',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6sFdOvPydhzdI0tHOdqw9BUPX2N-DDQQrUoVwCIYB4CWDbyAp2pTWrX6n-MVhE6PQgT1RD4WP80qE7Fsy0nh3Qxz3x5i2OuATV_obsESzJOWb6vLwumMyC_lQq8z9du-GQSVQAi1ZcRbvHpM80fdrUvlzJq4z46p9H6R_Dp0f8N0mMLAVVH-63mfMKlMSfoFihcqdHMAT1TQ6owy0qP1VQdv0Uso2zHXJmJXUOADrEr8wcSF9gphEVgL02-vwfjulUvwxqdRZLYtIVL1VVtXKWApidjdXgWFu5tyy3VtyyWyh4Ism56slARoMPMYZPjzn3VZ_BYRJV0VzeReCc3Y3Cbe5yLsGha451Q4vd_1VlIImIdbOObEzAqcn83flSEuft8lRZa9xpxV_-tTZV_SixT2Vstd_FbqXKahVxFWCe79EEuryrJPH79mVo10_aGNaKrpL7yklYI8uZqcoWXB0uFf_acXUJGghwTDVNgrsDh6YHe7fdsRRThLYz6Zw_WDS2wvhVYzy-4eEsa6AX7R94QAbXt0WmJFgxyO6s6Ze5UQ1Y_QDFcvpk7yo8MTdb9yDphV9TQShfqBvJ2p94CpCA8NG0R2C0YKwJPa-hgInGMtKTvdKE7AlzK7XD-ux9o5uRo4F78VAwZD7coMLx9MurwG7D8tR3LygTBZrooJn4GrhZRnAFk1dM_kFkc77lu4uEJkDWSgYA8D11mCAIrs3tRzPogpCm82BZ6uAuE3-dBjuB1wnmzXCGKwEKNA8EW3iwSvmdQZH4PYXptvGBSXQeOH69jY9gH9s8q9HOjvWWY9VBLDt-QcfzbjYJCNKNiTJGD356jFjnatyQhg5taTc6tOZa20qWWZUNtOoun6rD?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8xeTvL50fBK9pawxdzhZKtzE2YpwjTp0kLZi_BpFBiHcCNkir4AwoykgLKXgXKaJqriUGB9Ljzn_GtlzngAYmokN7wmFKUI_wYF32Jfvcr7_unluGJg8KLkQFiY_l3T9vV8MoPy2hP7xupCaAe9sAdZYmfeyb12vC0kov1emyMEg,,&b64e=1&sign=9bce09aded236e9272ec1058bc2d9d8b&keyno=1'
                },
                photo: {
                    width: 362,
                    height: 362,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/245136/market_2mPQ1qmH6SRny4Fg0DNKvg/orig'
                },
                delivery: {
                    price: {
                        value: '295'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 295 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 119
                            },
                            conditions: {
                                price: {
                                    value: '100'
                                },
                                daysFrom: 2,
                                daysTo: 4,
                                orderBefore: 24
                            },
                            brief: '2-4 дня • 23 пункта',
                            outletCount: 23
                        }
                    ],
                    inStock: true,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '295'
                                },
                                daysFrom: 2,
                                daysTo: 2
                            },
                            brief: '2&nbsp;дня'
                        },
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '365'
                                },
                                daysFrom: 1,
                                daysTo: 1
                            },
                            brief: 'завтра'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: true,
                recommended: false,
                link: 'https://market.yandex.ru/offer/OLu2_AMb6DJSlvQlZIX2og?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HJXaMklqFS5cP5gWY7ROH-WfMF1iHdJJwN6FsfpsVkHiUZbW_GoHD0SXycu3lKcivqZWAkXPBf0Fdr2jQ_LgZePFi9t-i3Rw5GMQyKsv2XbfUU5gF8hpcX2RvDXxPSw8ZQ%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PDrOPCmWRBOgKMoMNhCLHsyVOT0TtuR0Czoc4VKbh6Rf2s8xt-C5gcx7R1X-D4wQuMt8xcmYR85cy6pL_ntSbmZc1kwJ1LVIWR_QbBz_I_8H4MMdQA34paQFgR4RTtB6t-YolDcx8h2SjWruP7FY_ya9wFe2kDOcwD16WGJPfSCl1PW8A-n9-rnNGJn7Cz59VpWCxFyBC1568Ml6FMaIvC5mn5oHJzN5CO9w_p_Yejawg7ngEbTBovAaNWyErRaavk-KHw4PodMIYROeWcJ_vXgPLQbtyUxNccUVvRLT66oQ7ut4xDEIs8I9U4lkMWr8wR70As7HUHmtBPz6l59DunlFmpCX83fW1XjUbY7hDFqOP_bvzTyGjGsHxWXLfcngDHqKKjgzyQYx3JdhEK_LGycUb0jLoDpBfxcGyfjeVsUCU3qoUu393-QtP0iT5lMWfH4q17Pfmyus5D_fTIHtNozfpdh7Gys0UfbpRL9bMjirJ4TQ9Bil__mR9bmoLsZLDKR3FX2cZVcE7j-6KAFyfJondOr-YWogqu1l-nRpLCHoX1D70hw9n7S7y5s9Tl6e8vhytuBgM5F0Y5MJI5eBkqd1sqJzdzIDfbRePTy-PwbC3H4FjK_-h5zv8UXi3OHz_NlJ-ARgNZ6H34RF3e8_ywQaS8AZqNS_LBrh1Ll8B4ERUttzf8W6QiUmqr1TtjvRy3n44-RuzK9?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXUdWXEAbQxtsxWdun60_HnpDXqb6k4DelbescQGpZ4Ra0po_vU0T882xzHWJEBn_2RFl1Wxc96Nw_gI_Z8NNBI9m2pyCiLSfDKVcKvUgOJmoniJFjxiZ9rBMRt2CK09v_SD_A4HbTJ2sgTiMNgwU390avRWyYPJlhp2tbrku8kQO0xcEhtoUectycZrXhKIGGfSaDYngDOaiH0dhLkXC1fHiQ6NGonSQ5Ss7QrJjtT3qttxcHuEFeVdHrqX2A3cOUGN6FaJeCrH11P774futSqn2nLhdeAIk9SI3DRLYxcikrwvWb0RFLnQ,,&b64e=1&sign=e53cabeb75aabede198ad43f3fc14d08&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=12065',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 362,
                        height: 362,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245136/market_2mPQ1qmH6SRny4Fg0DNKvg/orig'
                    },
                    {
                        width: 362,
                        height: 362,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_hD71raZxRqIXdaPp7x_-5Q/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/245136/market_2mPQ1qmH6SRny4Fg0DNKvg/120x160'
                    },
                    {
                        width: 160,
                        height: 160,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_hD71raZxRqIXdaPp7x_-5Q/120x160'
                    }
                ]
            },
            {
                id: 'yDpJekrrgZH7ErC9gfayMnSt60jbpS2Vrwfy6-leadb5SUGKRPleXhpfMaiusItVeyuyRY4wVujnfEZnJ0jEEnjxs4gdtegIhusTJEdQAPJRettRh4cH0alT7TjHY-jvfQMe65MrYqttHFR-U6RgbsMoBOCH6wda9_1UpnYMR5uXG3h2KfZlvN_nILRD8lPd3ImqrRAiYIwEdUzE7946yK9HcoprVKO50-TgQOeZqS-S6wkE120hGIG6PdUW5zqixqJRhIPRnv8vhki3NFV_mUuOASsFbvLwjYUFbLAi-OA',
                wareMd5: '3669T8MLCHnsuMxGUyZh-Q',
                name: 'Сотовый телефон Apple iPhone X 256Gb (A1865) Space Grey',
                description: 'iOS 11 Тип корпуса классический Материал корпуса стекло Конструкция водозащита Тип SIM-карты nano SIM Количество SIM-карт 1 Вес 174 г Размеры (ШxВxТ) 70.9x143.6x7.7 мм Экран Тип экрана цветной OLED, сенсорный Тип сенсорного экрана мультитач, емкостный Диагональ 5.8 дюйм. Сила нажатия на экран есть Размер изображения 2436x1125 Число пикселей на дюйм (PPI) 463 Автоматический поворот экрана есть Мультимедийные возможности Тыловая фотокамера двойная 12/12 МП Фотовспышка тыльная, светодиодная Функции тыловой',
                price: {
                    value: '76989'
                },
                cpa: true,
                url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mehRahswkml8QwZMTPvTF6be1ihcOCeOrc8WR6XcZQF1rKDS_1uDjupf6WhHF6r8H1eJkCX7j8DwSa7nQiQtxWaaY6EWzyNyVs4st-C7dmfqYbSZWPGLx85z8VGjeb3oHNJFrE3Th9oHJKa0tNPhKzVbGfJm_Ta7TOpKQiQx6iR-DxJACk7zB0843OF0x6EHTJGgcVu_PeGpphkWoitWLhvFNCPUM1bsLle1BgiMJcR9Gje9JVjByDEmSGFxyoKnkIXGfQJDF15mdRSitCEb8rHFmb-lABFrsZCH7KdEd0fJ4bTafUcQ0LKZ1aKhFMntG2si-X-w_jKXBbGDyZi1-0kCiH-iWVfts5uig9yfU8LbML8TBsnvyeNiEt1Y6opqkOTGT95QxiSVevMmzol-3TbubDwTrxHWehk18YzDmzgsqhfbeKAQqe_Jn5dB2KB2yUFcvJFiBrrC99ENWCdro-fNnm95RNaT5I_Mm7_vI6vFhVW6aeVWEEEP2Ys-UFWlomFWC0DHgfggNqQiBxWkoD9v3-JN0-9h0KYRrp0BhmeZXpH_uHH-l4iB-lKgYYs3BP2EEVt2MdalK_sa2gqtzGFSmRincQ0aM41_DqT2S7W7U0_xPI5YTb45Vn6guhAHJOuJ0OnKFn1tuBVQ_FHAtlKDVZDPJHjbX6X0SwlnZVIuIJxd8ocJqC2CWpbB_fkuskojXvm0OIerbNw-BtkF4f2TWFF4R4tZzaY3BezKpee9wGyf6aLO3yw-BMsSh3d2nU8Sd-7qPkgUk6XYc6OCreI9fLR_A7oho-_1_7pAT0u1lo9G1ZHc2iVVtNq0Hg5FBPXEsd40q1wOZabIklWR1lbYBZ6uljl3irosA-2cuKAB?data=QVyKqSPyGQwNvdoowNEPjepPkxt1Vqcos9cWvPI22tg0wCjd75HgObocn-VkR-9ty9kgea7JlqOTwblPNbhRmVs8VQhcci9sRRSqg1JMmciNAk2d4k6yS22dH0VrvJZ9Jvz-BC4IGZCpX8p36t6X3ib8HNeNpp54jRaQz6Ao9n_m--O_RGfpr6Bp9ABNxIJtO8PI2IYXbhr-EFBiY8hfqdtT_CKHX0YgZOhqw1W6AQZDyoMG89M_gD7DPMC7qFIdCqfymxCpd9vX7l3GRYxrppxi26y6raqlv-ToSpRtx8wlIfk6pAt51H0742HRm_WDx1OqmyxoLeRysVY1uLMkRNdSxOfopZYEb7b1tM--rBV1Ed6Wur2HFdqXYYgwf3LTq041ZjMIcD1wevySzWQQpVRBMfRTTUGAKXVspah5ViA,&b64e=1&sign=6fd8dc3a4e2e13a97c24045f016a5004&keyno=1',
                outletUrl: 'https://market-click2.yandex.ru/redir/338FT8NBgRt9zAvRSXY4iQKg9ijkV3IR4dMvzpGgKMyTqIIo6gwsm5HX2hFcDzx0cu-zoWV-1csjmuqpttuI0he5b8BfH_8YwzXKujms2ulSz4jDZRJL_hj2mI8-sFwhIoc3wUdjIPrSBJGi27XS-RocyKHqVcBz9xVHS6Ji1CBKolbNSMje9ycLTVA0glzMIZ-WBPc5LKfJX0Dhl0aOM0vJev5IlpXK6FUc9zKhFAZj6Ov6dLSLUG0Xnn2HgEriGCLSESKkpvhxPP4wpCyV93ROICJsGnTRD4F09o6ihYz_dc_E3S2Vn_eN9vpF4Au2fUT48Z7SSylpEQobaDpd2C-zyv7RDsVIhG8K0VmY3ZP1zLY2_GGSvKHO2FnWlw1f-hoMTCmwbAaDKq_BpoRCo3Nzok7I-OA1HlvMClmR8nSZt2hPiOV4th9sReVjUP13a1mROlnZAkXnoQo5YhujFPvohPtmp2bTCZfe0V_QulkkXTCnoLwBY-zVJyPkvdLpMf2GV6KHko2XmgFVGPTiwaedj83G2SNDc_X4WJEMSvBr_R3BG21aROoLbXxdkthE2VkgOz9Ox8QsMZWwMKk5UsCA7Z10dItzcPd4nU0l1VZVCf_L5SzncH5VRSjYbc50hJ_QhycEZ-YTiYzc5VA9M6tb8_aZc3jz8dPLGtLkcrkLD64OGTwY8tdxcioQ7q3ohxd7A8n-9L4ft2KMGVhcUJ-tbCQt8C39mtolN7aacqbzG88juWEAjJdK7s2TrzzvSSmnnVZZxqpaUdgUlQ7jfnkV6JWfDgyT2VU9ud8QkDu8FJhqelCdh-CYl52Mf9Ot2E8wfX4oKfe773NDCZq3BAdspXJb--GaSBOe6QLJCLTX8x_e9KcqZYiN6vA5CtEu?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2WevCIXpjIc5u3bZ9aDdMPRD-Gd5VeIhtwhU1jWcz08MYHswn9lAojewiuPPQDs3QSktTpXJBMUYIj0y85tvJMNDDkyG1PbWLtc8nikKzuBGD4SWregk-iU,&b64e=1&sign=3541e552eea22b35d001f5e1adc871e1&keyno=1',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11
                        }
                    },
                    rating: {
                        value: 5,
                        count: 6154,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан'
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 265,
                                percent: 4
                            },
                            {
                                value: 2,
                                count: 77,
                                percent: 1
                            },
                            {
                                value: 3,
                                count: 69,
                                percent: 1
                            },
                            {
                                value: 4,
                                count: 371,
                                percent: 6
                            },
                            {
                                value: 5,
                                count: 5374,
                                percent: 87
                            }
                        ]
                    },
                    id: 6537,
                    name: 'БОЛТУН.РУ',
                    domain: 'boltyn.ru',
                    registered: '2008-03-14',
                    type: 'DEFAULT',
                    returnDeliveryAddress: 'Москва, Марксистская, дом 3, строение 1, офис 413, 109147',
                    opinionUrl: 'https://market.yandex.ru/shop/6537/reviews?pp=1002&clid=2210590&distr_type=4'
                },
                model: {
                    id: 1732210983
                },
                phone: {
                    number: '+7(495)545-4227',
                    sanitized: '+74955454227',
                    call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mehRahswkml8QwZMTPvTF6be1ihcOCeOrc8WR6XcZQF1rKDS_1uDjupf6WhHF6r8H1eJkCX7j8DwSa7nQiQtxWaaY6EWzyNyVs4st-C7dmfqYbSZWPGLx85z8VGjeb3oHNJFrE3Th9oHJKa0tNPhKzVbGfJm_Ta7TOpKQiQx6iR-DxJACk7zB0843OF0x6EHTBuhuGcOwVu1U2zwEKRt7uzHpPcA5VvSqnncxTkXd11l9FWQVO3DUIjqDHztYDcDHvV6LhrACiUrRJmsPIW65EKWGYrcW8rRWs5CtAgqK52B4EZIfVB5VwOGuuqFULuMnzd7wBpMNpaMDcqsLfIiZkCprs0PG-flBrfoDRdtka9T_F5y2XsyUYRCfs2l99PtAYq8Bm_9qHdpSHkBHZmHsW00L9GbjUxJFCONtDqXC558abNOVaaGfZSVlfQJZDNgfgIiop63pOXWGTlzrsMVFMcY8jmoDNjHx8oOxyhMUttiX7vgK1xiEFMZfEhKcRxq0FT9pjt-HgzVGqCs_iESj6TwJYBtQZF5mHRRrvX5ZX4gMQ7ItOW2KNYCzUv6Aim2QVJY2WnrPtQ8q6jV6GHyPKx4UQJz84rblXwGzCd8tZDyAHq4sgndjpg6ci04znCyrSN9-1M7qfDxsaxN1cLmesxeMcqlLTdBuN-8HEn2OH9-T17MO1n4cJ9z3h6I3ubRBJt2VkzyKxwcGKPHA-8O2WFAmKl98c9oYuG-V3UnxSa6qh7ZSJDBdYHycS7bywWzObWADFngSDOSMXQrh8WuvTc0sndfmnAm7cCYPlEUHy14NIA8V5VfmsVvACBbMORynpIUNglU4WqqbFK06uwqt3KdbF45NfVUaIdiCj5y-eV1?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8_-QffmvPNPSPys8rV_RWaEFvtCt9HBMzkeqLtzPt-KsyXSBuWweR6FJurinH4RETa-u86Telbdz3WJ2l8VcxHsznzql2rx2LBMDi2qobx7iuhyIrcxM0jNYCMv8EXdtKexTxFxi3eTgCLyb6thWQShIeJYcLtt3ukBQKm6RCZlA,,&b64e=1&sign=026e2372da338591e5144f399f8e7c34&keyno=1'
                },
                photo: {
                    width: 254,
                    height: 393,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/366186/market_BJH8CPCsioVrrQudZ1DPaA/orig'
                },
                delivery: {
                    price: {
                        value: '399'
                    },
                    free: false,
                    deliveryIncluded: false,
                    carried: true,
                    pickup: true,
                    downloadable: false,
                    localStore: false,
                    localDelivery: true,
                    shopRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    userRegion: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 11,
                            nameGenitive: 'России',
                            nameAccusative: 'Россию'
                        },
                        nameGenitive: 'Москвы',
                        nameAccusative: 'Москву'
                    },
                    brief: 'в Москву — 399 руб., возможен самовывоз',
                    pickupOptions: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '99'
                                },
                                daysFrom: 0,
                                daysTo: 2,
                                orderBefore: 24
                            },
                            brief: 'до&nbsp;2 дней • 5 пунктов магазина',
                            outletCount: 5
                        }
                    ],
                    inStock: false,
                    global: false,
                    options: [
                        {
                            service: {
                                id: 99,
                                name: 'Собственная служба доставки'
                            },
                            conditions: {
                                price: {
                                    value: '399'
                                },
                                daysFrom: 2,
                                daysTo: 3
                            },
                            brief: '2-3 дня'
                        }
                    ]
                },
                category: {
                    id: 91491,
                    name: 'Мобильные телефоны',
                    fullName: 'Мобильные телефоны',
                    type: 'GURU',
                    childCount: 0,
                    advertisingModel: 'CPA',
                    viewType: 'GRID'
                },
                warranty: false,
                recommended: false,
                link: 'https://market.yandex.ru/offer/3669T8MLCHnsuMxGUyZh-Q?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=dVeiZLjz9HIuj8a5yH7BDx9KWOp836CE3VFlQL7ekKSOdDz3uMpqAf95XeUpAzWp6dPTYKi91a0jIlx8EyUzS4_ZeKCflI_CNBfbiizypmFGY2Gy1ns5AyQood0CfZi-1KsKzb9wGq8%2C&lr=213',
                cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97PDrOPCmWRBOgKMoMNhCLHvMAKxyZhzhgM0H6aEmffOesbomtz-hpMMZkBML2d3gVBovflMnt_dU6z0xJ7z1RW03kp_HoKVvX58OahSPtluYZiio2uiRVPfAryiTlK3aHr955nISYPmYz7OtF_Fzqoogzlcy2FrnOdXMWCSlM6FAX6uKepRjuhb3P0bBHVy4WWiob7DfKCTv51hMG-7zDS5Eviyp0BzfSQVnZ8XuIiRMJt6u8twQO6Mc1wYiqq3j242WDegaTdN7teVdj2OIYlAG5BXj0SueZL1zTan9cawJCYWNMEPyP-U-DhI2pj7Vs8sA1DXG208gWsRmNxrftTq6NrhS0cWPdfwRpUPD3D0K6VRq7BDyuh-E3X_oGFyAlWGYVlyIaq5qrLOaMyy_7fC78q3A9JY0fiXGD3efI0_kYZv5XZWIU_hZzKW_K-pvY12CH5tg5IH0S6QIKdQR32mVc4FTO3ZOlUMGHHRPw1VBywSeK2hJRAERwsHL9uv1s74IuVi08F_pBwW0CQyFnj4uy3iRSEM_kGRckvjQluirmTBad8xzk5_KTdTvv4cuXH7L06gzkylY0MiWosoR8Y-sAAT6X-JOotHk1306L2Gs9IeuFMAzYlJKtDZhNdfCT1JzCtv7QrzlsTx1-vf5hT-7BdqIIzmetgFBInQYjXULoMrxMLqf2TYOXMEx6K08OQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXKOMtBl_JdHBQ_Ien6IfPJ-itp8jzZ4hOVQnB5l0847bo-YJMJphcGtl1Z7eZfKUAITFRSDWFlicM2qvuYZScV0phVky3j91swhQoMzo341wkea06rOiWPT7cjTT23HRJR4q2jPOoKfEG_JtbBXmvF83Df0F0FLnBRIiMaxdOUuQ-xBnMjTvFu8aPtcL-7gnS-E1UNYOISlFtG7-IKz8p7_Uj3Te9HW6d4rc2gub_v23bE1iP5YUa4Rmg44q4pxbYM5mjpJPJ7cIY4haWhEX4eK2Enz6XFgJ6QQ9tjBsX62hFVSZ-qlpExw,,&b64e=1&sign=3690b05976e3181e1d9b2c9bc4b7f522&keyno=1',
                offersLink: 'https://market.yandex.ru/product/1732210983/offers?pp=1002&clid=2210590&distr_type=4&fesh=6537',
                paymentOptions: {
                    canPayByCard: false
                },
                photos: [
                    {
                        width: 254,
                        height: 393,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/366186/market_BJH8CPCsioVrrQudZ1DPaA/orig'
                    }
                ],
                previewPhotos: [
                    {
                        width: 161,
                        height: 250,
                        url: 'https://avatars.mds.yandex.net/get-marketpic/366186/market_BJH8CPCsioVrrQudZ1DPaA/190x250'
                    }
                ]
            }
        ],
        filters: [
            {
                id: '-7',
                name: 'Заказ на маркете',
                type: 'BOOLEAN',
                values: [
                    {
                        id: '1',
                        name: '1'
                    }
                ]
            },
            {
                id: '-17',
                name: 'Рекомендуется производителем',
                type: 'BOOLEAN',
                values: [

                ]
            },
            {
                id: '-9',
                name: 'Наличие скидки',
                type: 'BOOLEAN',
                values: [

                ]
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
