/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/1732210983\/offers\.json/;

const query = {
    text: 'Мобильный телефон Apple iPhone X 256GB',
    category_id: '91491',
    price_min: '40475',
    price_max: undefined
};

const result = {
    comment: 'text = "Мобильный телефон Apple iPhone X 256GB"\ncategory_id = "91491"\nprice_min = "40475"\nprice_max = undefined',
    status: 200,
    body: {
        offers: {
            items: [
                {
                    id: 'yDpJekrrgZHyR2wK6Be0xyMaaINpuXbPz0qehflYoNf8mFf6g_QqMA',
                    wareMd5: '5JGgV_T3ons2PgBHxOAjCw',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256GB Серебристый',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcLUPdc9iU5ATutFyGAFh5hGMRk50PNalI8f3eTLZS4JwPPAMlwMgVMsalNwVTJFSJiC7kaIzcdt8uzU7gQBMj70jg358zO2jnNt58t2MdEMMx0d56HhVl-fS6WBO3Vu7UEHKVmi78SiJbO1DBhcCxGm7CAsWA1HoQvor95HP1-vaxRMCNKxIb_Zhv3V-evqmhfQ8WRS3eMuC7XMEGL1T8A_9oCuAl8SJuStxoDQ5YbWvC5nfyzfNEFQQDUtEZ02wyRRpM9j0pgqx-ZZ5dH86UXwu40I0KwlU20EK8667D4zc_tOqtBPcOKNvlRQ3LHG6VoOYsXfyW6QZXMZWxt6IQt7lZ7_tDxcNQU5Npx-7LfrZPDgHiuqS4noPurtmvbCjOZjzwbQJWj-FVl08AaQHG9-qF_nPjc66xMRI2XxsfjSLH9D9quYe196Zg04y7C5DEHEnunSiv-jo6uWU-LnmaW_wGdIQjCz9Gsvao69iTk3kSRXs5jZrGjgnZOi4A43kF3PxYPYJKRCaetKtE73OaARpODC0f3diNbXqlTcL6dUwzXeE3VRJJHzDZKKPKAc8qFuMKxE1UPk5YhWxxPWMU1gKjTUUTo3z_KGYJJe7fQqSRw24D84xwJTsow8FoUfJgPysXBjxEY50iqjZziZ5aWdi-54PQqeWZpcrCMbVRwxQuiAuySnVzzhm5SP7psmGgqcVVf0MPhjgJbpNBC51cq6FWSSea8rUdT6FVaorIe_P6wtdzuxM0DocK5yqhb4m4JrZpZNKYIRiwW7sINUbMgNW9SPUgPx_7tK5deiJtRsO2I8SS3ZC_rX9pCytFm79Sx2-fn_Bqkpabp75tu6e7Sqz1JWgaGj9GuXPuoom4Af?data=QVyKqSPyGQwwaFPWqjjgNrIMDGWpvFEh7dNP3s0Nt-FDxTtOP58BPC_MqCwRFimL_Ybq6Wy_E2fkz5oJQbnfd-nJvIHHuc3sV3RUaIbTuRHjJy2Dgk3p8Pv01CLWgaID19xfTX-LTGY8tfFZSGgnSEs7DRlbbTw30pIxRsLD78CyhYpC5YuyZn19syqQvY2Qf1oL0KgdwBeu_K78t3dRvZ45Ajlle7a5C2EKop_MHDF8BxFZDMIJbFzK7q3DZGaxkuYwYPESIqJglsmAroBQcOznWP_s20l0GaNmpC6wBYxJk9kkXkFmvadKBt8Vnx_C17LzFNI0ZYTqJ2q9WVWdPjhHGCWDksfIPOaV9g2r2vwEZ2p_JYdbHxMtpRKPI5Bd3cnJiUYQtTPlDhkdP5vbkuQU3OiSPA_5GRgDDG1HXkkPqV8LVYkyVnXjrZ4XMBu5OwqUinGghdi6wDrDdgSu3vbKrAcY5citCOgBRO5kFpqf9PnKvJ0Aig5ytn9K285CLIXnHdqoOv-KVNlDO-YwkhCIYH41oaL1KLcBvtNhi8pMbHB_cDo3mJ4E9i7eW1LIqVsTHDCKuLWffb-BymGL-Z6FwSE6eVXA0cMD9U9mWN404uoTgEMY-vjJkiv1iyXF8CVY4o4C9tA1NZaB1Xt01LmsrwGaiPF6NgEiWiDxnV4j4bFJ_pwE98P3srFxpLlD&b64e=1&sign=8c96109b838ebad4d7d373e08e29d8d0&keyno=1',
                    price: {
                        value: '87590',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 42315,
                        name: 'МТС',
                        shopName: 'МТС',
                        url: 'shop.mts.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 9116,
                        regionId: 213,
                        createdAt: '2010-07-09',
                        returnDeliveryAddress: 'Москва, Малая Дмитровка, дом 5, 127006'
                    },
                    phone: {
                        number: '8(800) 250 05 05',
                        sanitizedNumber: '88002500505',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcLUPdc9iU5ATutFyGAFh5hGMRk50PNalI8f3eTLZS4JwPPAMlwMgVMsalNwVTJFSJiC7kaIzcdt8uzU7gQBMj70jg358zO2jnNt58t2MdEMMx0d56HhVl-fS6WBO3Vu7UEHKVmi78SiJbO1DBhcCxGm7CAsWA1HoQvor95HP1-vaxRMCNKxIb_Zhv3V-evqmmSRBHWor1DC5coSmuy1aZOapajXu5kVq6sRIUPj3hy6bi3pkVKGLwyMcchR6wRN9tzh6z_POs7h23DtJpAMj1fFBjcwBqzSfX8nx7agagQtiPqX8ps18DSzeClc6UWoe_S2goqSwccr9x7Ek2xlUX547lV05F8SNksJT-a5rCnNAwnPLHPlxSgrmUhP9Ov04uefKz9XJqCqHxYEeljjt0WDMlsZ_PzbYzbpM5w4e3p0pJwOuAgDIQiU4zzZkQ3TOBXAAZ_dUROLUvd5j_y4OePScsH3wSkJ7u8jgGquX1ijBTxO4b3lOyum1NNLeAOtrgGJh0Mv355fqOhOCZKnNoc6VA4pGgAb2qZpr6I1xPKcHEd6XhV6LBKvvxC4V4Aud9t-t8nuQEQLBn1zZGJPprtHYamVRLo-1SBYsJTAzP9qAoKXlZplGO49298wKvqqmqQXXVmcB1bByFfYjIGHPB9-P1cAzs37dsQoBf5rG7j86mUir_WG5AltVTSJNWGuUa-kegUJ2bBxMUIzcsM7dV4r71gYMZBhdeTQIzUGX7DfeVlyWYSfD-rTKNycEiO_QB_JgGQPiHp1dR5SOy7offa49n2fj6wkUXq55PsC9hLpCVxJC9ZXdhxOIcg2gDVhvHuNu37B1eg14DCIFTu7DxEnurMEap3O7G4vkmaiXdZE?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9Dxr_VaESTKI0vXfrgyz5eLC216pLrE2AF0Qk3VR3DQg2pcV9R4XB_YE8QV16X4lrAJdDehbbMu9HNQ78qR5tn_ofrMyOhjhqKmPEZxmxo-KDDR7sgSJxZrYFmv3kuPLKnH1d5wEgKoDrpYgsmw0zozRypAPEDS__7ztP9Eq8G1A,,&b64e=1&sign=a7e206859e29ac34feaeb060847732f2&keyno=1'
                    },
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: false,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: false,
                        brief: 'не производится (самовывоз)',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/372231/market_1eH877qRs-ZI3lFT3prRCQ/orig',
                            width: 347,
                            height: 691
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_L4caNdJbeHSfGf7FrYSuvA/orig',
                            width: 431,
                            height: 858
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/230810/market_-Nt2R9PDMifApwTr5EFvWA/orig',
                            width: 346,
                            height: 691
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market_nD_nD5-G7Jm1YY8ifqo_oQ/orig',
                            width: 47,
                            height: 688
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/232366/market_ZswU0Ng1QTbv4VVrpPMU3w/orig',
                            width: 857,
                            height: 184
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_YxWpuuSmSkAdXhvyBlzVhA/orig',
                            width: 437,
                            height: 856
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/372231/market_1eH877qRs-ZI3lFT3prRCQ/orig',
                        width: 347,
                        height: 691
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/372231/market_1eH877qRs-ZI3lFT3prRCQ/190x250',
                            width: 125,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_L4caNdJbeHSfGf7FrYSuvA/190x250',
                            width: 125,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/230810/market_-Nt2R9PDMifApwTr5EFvWA/190x250',
                            width: 125,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/406938/market_nD_nD5-G7Jm1YY8ifqo_oQ/190x250',
                            width: 17,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/232366/market_ZswU0Ng1QTbv4VVrpPMU3w/190x250',
                            width: 190,
                            height: 40
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/219743/market_YxWpuuSmSkAdXhvyBlzVhA/190x250',
                            width: 127,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '8',
                            city: '800',
                            number: '250-0505'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '9:00',
                                workingHoursTill: '21:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '19:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.605373',
                            latitude: '55.767909'
                        },
                        pointId: '211242',
                        pointName: 'Дмитровка М.-5/9',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'улица Дмитровка М.',
                        premiseNumber: '5',
                        shopId: 42315,
                        building: '9'
                    },
                    warranty: 1,
                    description: 'Всю переднюю поверхность iPhone X занимает дисплей Super Retina HD с диагональю 5,8 дюйма и поддержкой технологий HDR и True Tone. Передняя и задняя панели выполнены из самого прочного стекла, когда-либо созданного для iPhone, а рамка — из хирургической нержавеющей стали. iPhone X заряжается без проводов. Смартфон защищён от воды и пыли, оснащён двойной камерой 12 Мп с двойной оптической стабилизацией изображения для отличных снимков даже при слабом освещении.',
                    outletCount: 134,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/5JGgV_T3ons2PgBHxOAjCw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=HKThQAB93TjwmuUp9O1xM0D6zY2vv_fS2z9uOVVAeBkPze6NEp-DKbWVQJg_AYSLARZBHMRUJUbsChAlucVjW_WDTAEV3gsqzeniif1SBgW8iXOGUhdfHAKvpOLI3e1l&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZHSKFrIQZapKpxbfmEP5O_wGZ1FH6SN_CT3d552xikj0Q',
                    wareMd5: 'pedakky50Mv6Vg7kUuUEdA',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256Gb (серебристый)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mWkEv_xiYjHNz5H4roLKROTdjvc156TCeYnVMALhYnKnFGIWUTtr7Ef0Xoe6grVqC567Ue5hNxAt9gZ2hrCz2iQkW-M1iV_IFOPN8iOeIL8urinkzvpuWe13DMyy7dp4JB35vTf-_K499DtSdF2TiqQ0FnVa1qdozUJngoH_JN0D7WRPQsDy2zwOON4TM-Ofj45MxJy2lIjPqT4iaU3NXnei3_J_nzJ3-aQ_03nA6ENBi7Gov4V12MJVXuff4cQ78jQ8Oe5Q6eT4z_bi0grKkEAr6cpkj20XucW-8KuMSXZsJoto3r-PwW6fhc2hmTA-GMTkhNim19OqnZhI1fqVPoQkeU5pymCbdoctsq_VJs-Ivx7FVXrGgz2PZshRyh2iUAOKNRbJoo7URsrC0vXCNVXUXa0PXUpxC0PXN_cdG6dgt8h9gF5u8IbsGWwKGjQ6fzX5-vacTtrohNbvborFjuszb5YVo4li2jTCT2BpBSjL_zuM14vxIuJ92rV45n-Tna8EArwMtepg-1_nJx_HRe-6370J1p0eeTDIKYWvgaVINiCjN0Orh3n7F9CUkElIIwjHsoGEr6wdTLNCfXzEn_CffnFwJ5lqDlWN6fZ-FKzeKYsYXZiYFQFzEhTRAdMcDnmzx5iPlv2s0TC6rdguNEz5M0KN4KECwa4Zgw3fLZpWUZ7z7DCRTy6nlG1uTSEhc6kvTqOB5rL7p2gIZNXnTfwAdJ_1Jy57Phm9B3kbwBzCBYPMLRuw9Jd8eHF7uzHd2ACyBSiDT1YI6ycgwkOxqagRG26_c9nd8XTrqINusCejMbbZ8h2DGI1U9ibL9iRGnW2v_8z0xez2i4JFCbk9hSEfK4PQtBMyhvJ9baVBbyTR?data=QVyKqSPyGQwwaFPWqjjgNoe-qUzSZBvplXRgyomtJwj-OzRWftaPVjbjizrwNWk7UxSvtJ4beBQu7amijvgLv9B0FhuhDMQJKsXjgnPZfh7cbqpWGjcWWJNmcy8K25IsjllDycIg_gZhwpt8ooNWSpCNFqKseoAchtzLrkMnvuyj2oAmR8OLbNqukq2GVAjUaYBlQXRB0Ji8uIsIH2CK4lMeD5XQax9OtsFh5yhcLaxoY1CJ2rU4XEoOCTG761YZqag1-v2gG7wzzkAyYeJb8zcYQvQwq9LDrtc33m54QO6Dl7R6H_DbiKfH75G6Vc1kLTR4juDFpKL38txMfJaxFPrG01aYFFa-GV3khYAWaTgV_Qhy-GvXPDms-4jo3PE46gn9l9LsvOIy5Crws3VoPxbsl7LWFs4Tz7acZ7Ukmu4kTw8rBbK5bXfqRLItUyhzQhgoSIdmQKn1O8_g_kcw5Os_DNw1jDnMuGEgIOSVlnSPpvSn31yvemXk7Gay6IrE2vOrkaSCjlo11BnblQRuYCNG3_Bv33OSLgGxi_ko4KIJ0ibxhpyMrh0NNN6Nn4g9S3gu5hRo2jyz-h6OPL9uhKTNjQ_RWzpFMVfLi2gW5LWIaoN9TyXVhgVyO4giTp0NKlbtTjlJMFQ,&b64e=1&sign=4015d4c3b6033475415fe73934d8f2d1&keyno=1',
                    price: {
                        value: '89190',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 2117,
                        name: 'ЕВРОСЕТЬ',
                        shopName: 'ЕВРОСЕТЬ',
                        url: 'euroset.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 10528,
                        regionId: 213,
                        createdAt: '2006-01-16',
                        returnDeliveryAddress: 'Москва, Беговая, дом 2, 125284'
                    },
                    phone: {
                        number: '+7 (495) 771-73-12',
                        sanitizedNumber: '+74957717312',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mWkEv_xiYjHNz5H4roLKROTdjvc156TCeYnVMALhYnKnFGIWUTtr7Ef0Xoe6grVqC567Ue5hNxAt9gZ2hrCz2iQkW-M1iV_IFOPN8iOeIL8urinkzvpuWe13DMyy7dp4JB35vTf-_K499DtSdF2TiqQ0FnVa1qdozUJngoH_JN0D7WRPQsDy2zwOON4TM-Ofj4KzgI3zNd3qaybYpcVdHBVt4vR0lVycee9EB38X-ZPPIEClfARTv-enfrSl-LuPZ6zPIgsGURLcjaonja5ySnjPCQ4XE42-bqRxyf0A5M4hX26GewqPb7A96_1Ku_ZhbH-_j8mNRSIbfd4gqqctW4SoQD-nCHFX2cXgIZNnYI0VJ5CMv6hsLtXYSUuCVxLCe2faoqd6Kj4elOGFbqDEQvUHwqZ77Ija88b-xj4MdJxuUg6TSPG3UWFCgIaMQG2tRmt9-SyNIlmQCG4GauRAQ40ovINBaMb5WkyTfBq1Wd63hjsFFdPETR8JZ3s3qdstDMArhNiw8jsXpHxk87qdPmxSI5AULC_RzLrW1zib7oaQaL6W1rJQZNbaZPx1qes9Teqb8HzEPatumAu7YrKl6yxInFxIj2AbHThGGWJzMjx6FbSxNPK6MNV5AYzSit0FYeXDDxVH8eRFLJfeu485AF0fEZBYfzDG-4jklwNNfB9xs1cuAhE5Q7g4126s1G5Rg1pvu6DduRvRfz1-GduSKbUzjRt9JbLE9wlaBUAKbBwgq-rHH71N2jzIjdGDVS7gC4_jWmF1K92yQsvdoIn-bztrjS45Oet8KDZqoJUSaDYfKwT9C7EEUZZR0hwgwReG7ID-ioQc2yBZ2kPEEn6Ahs-u1FeVR17w8oR84ycgq63o?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-Q0mrLZxVFi62SWGsjACUFwolxBm9q-Hb-i0jdLXHQt_SLzencRsf37F4vIhm2YQ8sBnnyKz25_DpShdDqUaY0df5PFoPsrX6RfT-RMHA7MgY97eR2o-PfhWju4MLkNW0YrULr6osP0toY3WXt9WJbXXN4yGLn4bL880CJNR5WFA,,&b64e=1&sign=67ea3bc0f2c72381dc721a4d5c0313e2&keyno=1'
                    },
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: true,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — бесплатно, возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/208277/market_-9-cskVvPknx73Rk-gIkZw/orig',
                            width: 189,
                            height: 200
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/208277/market_-9-cskVvPknx73Rk-gIkZw/orig',
                        width: 189,
                        height: 200
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/208277/market_-9-cskVvPknx73Rk-gIkZw/200x200',
                            width: 189,
                            height: 200
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '499',
                            number: '777-7710'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '9:00',
                                workingHoursTill: '22:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.678137',
                            latitude: '55.772406'
                        },
                        pointId: '500096',
                        pointName: 'ЕВРОСЕТЬ-МОСКВА',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Бауманская',
                        premiseNumber: '32',
                        shopId: 2117
                    },
                    warranty: 1,
                    description: 'Apple всегда мечтали сделать iPhone одним большим дисплеем. Настолько впечатляющим дисплеем, чтобы вы забывали о самом физическом устройстве. И настолько умным устройством, чтобы оно реагировало на прикосновение, слово и даже взгляд. iPhone X воплощает мечту в реальность. Это смартфон будущего. Чтобы создать дисплей, на котором нет ничего лишнего и отвлекающего, Apple убрали кнопку «Домой» и реализовали навигацию по-новому, но с использованием знакомых методов.',
                    outletCount: 277,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/pedakky50Mv6Vg7kUuUEdA?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=lDX8XqFax4P7QAuXNar3_FQteKqbHUu4dIPcRrP0PKIppIPmdWPJrcghQhiIe__b3-5Z2H6DtQhYfn_V1R_6HL2UGhak5Q5LDkSCa8XOohkU71xfcBtjFElSx7CcVDhR&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGc7jTfe4A_NDIC-IsjW1sQe74fKmEqsA8-pyVeFULkpHLig1vExhcYvzDhlCOPuDQ-x-5I8m8_PDWu-iUlRnLNThZP327gOW1riyoiEkjx6ICE6Q3ilnvHh9Dp1l35HoYvQXydOnYTHfPHIyAlVF3X57LdJNsZFuwrV0AtOF2KGKfzoPg15f6oa-WYQ-mn5Hqv8guSex8-u5nc6geuxsIEDkb_Pv-n4g9ieInG8OGjA7e5dSEkqK3GnQrUwKOdYPOZ_Y20t2lSfrtv7t1Pbbp2acef1x-lY0dBz_rafkO6oeHhGjZQMVv5',
                    wareMd5: 'kTshSNh2tWh0jVBtgi5BeA',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256Gb Space Gray',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4cbFtLcv3ob-WcjVkG2jrUD_v-Jr-H4-LcjfiBb1Zkhuv8Cv2HSP3PFPuOy7vn7XaoquzGAsaRtokYD_FFrrMp9U_BLGGR0iOrU5Dy1RyuN7uWQNQOHe7VkH8aisL_ZkeINQpE83e6M7kv7b6qZdheLIvY0GxUlOWyFf8P4o3PCyM9-1w4y9vahnyioLhDdcdT94xhBzAts6SwMvwcVNc9HuCw4CxY6dzB56b18pQ6JVZVuCt-FsvnTmOlvvZwis9FTRC5G-lonbFHjd8TsbrAABwrcdAUyxAfGtqQndtbvRwvv50LtpOCRqsvG47c_fleDBmq5ESAF_w7-XSZwVPK8I4LR9rMXN3uBO21gbv6lGtUe3P7RjImtlEqGCAItj24Wp2LbOoB1g_aSr-1_x8ek4LsmW-OuDpyBZUPPceQWlcoDf0m4RmOfhvG8sVIyhaHijaJUB1qJRlYSgv5NzGeoekJu8jHPZGBtceasNRtLtDQK-A-ZIq9qrT3ydljjfs6U4Vgux96qis1RoniY6V8GXtJGlC2me3ixRYnHYbUxeFxBQzEclPh3O3LytEfJ5HdHcti5MXbNR4FEVJBft-TYlFQTsHCnu_KmD5O-_DrA5LXTsF0s1x9b5N9Tm7gK_y4WqPhlpKZ7jFHL4WpeAu_bccCK65sqKmxVZB2otNhdXmKWVeIpNRzX?data=QVyKqSPyGQwNvdoowNEPjeqxtNJ5afkeRQvx2thedR6IdI3R17ha7XzWhNx6F6100LHopHSV3du3pf4mC6YxjcPWayzjL0erRk5RFp_BQp0uxFwrEjc_ymsJHfcnCTlsj2NBFn7-brZqNOLa72kWaUeb6Tx8Ii9AMEy1njMcwz9jjL45s0mTLJ3xjrBjMHO2LNxaSr06BlttxFDX9nBg_G6zmhamUx2YAqkY4tT7Ug-JLmx2oF6cb0d9_VrgZUCK7SM8LKVpTN-gleImm_rY088v5qzLoXkpxJPvmMANvU0RhdOUHwQ4PH2YUgD7GthTWnhiqF1uI3RIEs8L0QVLTlKSuvzCF40Tamkv8_N0E_T0ZqYhBMySXcJKQra2l5XcK5BCkobmVk3WEUhq-QxVJS603G8kd6n-uIspNo-PjLRJ7EuCVJSpAMKkI0Jf_PplnGDL521CXaApBvKG11sP6aNbLgaMl1KLnLSeoHRmyxAfVCbor1116Qh1YaDP8NS-sTzeryVLPoJH5b4F0VC4d2sCQfUmeYIAWQ279sadDIhnnuxtgXO-bC52A8VQX_biwT7W4HI0KP6gWigdrCB3yAM7evV8SY8-FQ4eQ5S6zMgv1182i6vn0hxMg7NH1IuI&b64e=1&sign=7aa1790ff2ef7310f11a940bb23d1aa4&keyno=1',
                    price: {
                        value: '91989',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 59861,
                        name: 'MediaMarkt',
                        shopName: 'MediaMarkt',
                        url: 'mediamarkt.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 21685,
                        regionId: 213,
                        createdAt: '2011-03-29',
                        returnDeliveryAddress: 'Москва, Ходынский бульвар, дом 4, 125167'
                    },
                    phone: {},
                    delivery: {
                        price: {
                            value: '250',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '250',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 250 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/235963/market_QbS_7hkCbvneBFNQ5OGqZw/orig',
                            width: 500,
                            height: 500
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235963/market_QbS_7hkCbvneBFNQ5OGqZw/orig',
                        width: 500,
                        height: 500
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/235963/market_QbS_7hkCbvneBFNQ5OGqZw/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '970-0505'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '8:30',
                                workingHoursTill: '20:30'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '18:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.718372',
                            latitude: '55.799699'
                        },
                        pointId: '241027',
                        pointName: 'Media Markt',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Хромова',
                        premiseNumber: '20',
                        shopId: 59861
                    },
                    warranty: 1,
                    description: 'Совершенно новый дисплей Super Retina с диагональю 5,8 дюйма, который удобно лежит в руке и потрясающе выглядит, — это и есть iPhone X.Дисплей и элегантно закруглённый по углам корпус интегрированы ещё лучше — мы смогли добиться этого, используя несколько уникальных технологий.Это первый OLED- дисплей, соответствующий высшим стандартам iPhone: невероятно яркие цвета отображаются с поразительной точностью, чёрный цвет выглядит естественно, а контрастность достигает показателя 1 000 000:1.В миниатюрном модуле',
                    outletCount: 264,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/kTshSNh2tWh0jVBtgi5BeA?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOjKKsnXh9GFcYIFpensdc5fj2O2MD5HsKn2UR6yKrDB0EAmdDVN05h-zKaz6FHs4BjdYkaV7GlUprDLUNX2GwrFjnRnlDBAo7F62hwGdMXvGr2ftkA4E3JNkF8zMbGcUjw%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgf-LS_WUVGWFGqbLcHgYnF7K-pK_bulhsifOyjrzeeHrnFWB-ZWOcTgtAU7YHGZyPXHQULVSdW143n__25AD5pURTVdsUwAi0rEsBLbweGza_JlZe2M6Nztv_InFvMs0cXr5qUwhfohRAjzZg_imp1MA8RKFWZEM7rIy68vr4ZiJNNbi85szdNXvjnoYd7h3uXnpTfqku2wObgCXNZq0GNT0kOxFz_tuefLMJZ_sdJGTHJphJavuT4bPYVhL77DhQpeh728RInCdKitoKvs6zDjopTv8CRrAFeB5oMsf__IMHjZVQSFcXiWzBnpwvDKKAFB-A589FgrgtZE6Pi0_QfAq20gZQqXiQFMTzpGPrSMsg2JFpkJy6mv2o6uDyxW7TYWWvKC62zHuEXundJspyPV9AElzWh7Z1N3cv4r5w4kBn2-ftNOj_MxRXW37ssTZUF_R8qLTBk8rAnJBcFVGD3ZPtNc3SURmUYgBimh4j0600cF1tuFIJkBzb4YloKe71qD5GMHR9YTKj9_m4RcT-UnycxOgqjyN3HpAG5mKcyf3f1QC37b_QUnjkaNRhoWvjSuLbXpc7e507KoCA4SE7ilgVIpQisSsJ4XAynaNY-ebEd1C0vQWuJECAj2qu_wwjlNi8jpS2J05KLTUFoW3b5igu-7IbtJh5pk73NbbCegUPoHSdAr4Im4PJDKcbEX65Zy9-oovSRCy8Jn63YtzC9uqpx2g3NSaegMlG2G63vjjQcIFVF0WPb3e1n9Oz4rm7gIVhCPgd53GFiYiy2VH9vbC8hLZs-MMUxk-8dCPVptQDM_D_FqRx1c5Znu-2ML4GzYziVK7ff_p-WrOle0L952t8Paajlj0YdREkDxMOR4CnfGbI5Xx6zM9GlEWUHpEAeXWm0y7lUie?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6orXHt76MtosQNlDKyll7eLn9UrLTrnbEpRmJT1dHWbNFPwLbKUdl754zkanBTlNljP6b4-rRJ4VPYHO7YH04pMWWvjx3aThXy16UdcGdUZO6F8nWfif1YasqCk_6IT6-cP9SiK4o2rjQCBuyHZHIllr6r6Rr_A14I81xWAIrVNuOMchbc8t0aTbuEym8xzDt_l80xILNA_Ry2QoMwsO0HZteaWVDSmSKhLdBYH2vf-YqdFYa6tAbRHLJCVBFHWO-LmsjR_82ddVbtD5hpTQiDvHgwTqoxTCGz6byRmU8gG3Snfb1ZsLp3D564ty6cPUOpZ_AyQUgKJG2F_0coZiOOr7DUR-jB5xE,&b64e=1&sign=c67417551c12f0eda9978be7f53ddb0b&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGOo_gWdvTGiUEgWBEWp4G4_8oXCPR6Ccgw2DK0Id36pBHLbX76M8xVxIC_I7-mUnQ6h7xouTYbrrDxWbJBFzsWMLzAQuSq4e9DrgnZiZWQdiGThahND89qIFfgJiyA6Y65XR9JytxNkTfkP4mJIymhUXKdcHbYx1WUQSJRsYkfB7uPzuZK15lCLfhkm2oNZ3QGbW-hLvsxzAgU9_juPcY9UFsfyatAuZF3zpAFX1AykfIYQWnAsmwynfVTkVo4NQcgjH9C9F6A5UqFXkIAyCBbTDZ84HFoIlY',
                    wareMd5: 'NVBFEnRNRBza2G7hFHsRDw',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256GB (серебристый)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQHrtNtzAfiBVqPpwRDImCqPeTT2zzjPk9zoijyZNsOIZb-Sqmug4WPlQLKV1HP_0zxZLR1BZ3J5Mr9F7g7d9dCdtBg9mCoJe2Nd7vwCyovx3kkbA-O8L85ajVtEBnmESojE-7DkLYXgbB0X8v_y_4T4K8v8x6q01z2yoYvtaO_OHHdub-Ak6c_qWYwnUJU-FbtrnB3NhtFVa6rbQks_3d5ubV_rJ_r9wPeSuF9g9dbnTx-CpEszWQacdIyTAGReCzi4kSlU7UQqGHCpb4ZFX_Wa2RU8oPVJRO3bP8abnjal5W5Ud4ZXNFDACDP2iKbuEv5bD1ZWQNfe9Gm_U4PChWnyNJGXepnUc393qv9zLBfjHmzJwGoRI8gJDfw1dfOkcsqWE1KHcKBsVRlMxrxUxKapXebB9MraOAM7uGgUDvdNjjS7hvDZ8fLhg2g1wunB1cdEKARXoWSAV--_k5rYYryrHT5_w7v6xdSC26Lk2Fmr7eSaYkQkzeNwrPmckaZjJnmSoGMbQgSGq8JsvdmadJ5zy6I9QaLzu6pQh5vQi1gbTcuJVWJqGJcagmcvW3pFUohjTV1r2TYzEgPbDwgBUNBU5Rzj9ynVp58vDTtu428tqFFqNifCnRLnKBhQEYZdvwReQkohgMkjl2dfFTUWZIZQuBQg0TJr3XjNjpwqfcinh029ugifeC5VGH0iZMRFqevZ_107osQ-N449jPs2J8tNB1jZZC6ze1ctDgl1du2bKJZUSGyPypAhinVvajVG5HLAiKp1OfyfZRRxvKSN-giedhX4vEnZDWKFjZaw68sLJ86scztAa3dT84prIUTo7z6E3CY3H6OtBdQGwdV8AHTaAZY5J0aJs0OBLxqraHlw?data=QVyKqSPyGQwNvdoowNEPjc5E6bN97n5WuzEawsno-KfoDQ6UAyPM1XeViHlvhvx_uFM2or2wshNbea5jwMt5vbqEQhO2Lh24mSANzAd1AJfptqt_WIKTBMwTlGEgRiGZPHz4eNPk4Sgl8oLMhrUtxRucddgNurayrt5MoSE02UKGRB65P8H9Gqz2NyYNK17T18gHC28QYwiLlVwOl-KjVog7ftkwgE10Uj8zm7pGt8vpHJ4mU3CYptLIf1JLL_N9nGPe9FYu3_5qxwZhGvF5P07TNV05fBBcWWbpVOfCw8BbUtSi-BUtpbVWVl-26aMEGKE5vnxoCjRIUxQuXSNQFEgq8gjwHOn_KNUYaqeiA2c,&b64e=1&sign=268ef6584b34d5e8934b9c545ba973d4&keyno=1',
                    price: {
                        value: '89990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 3828,
                        name: 'CВЯЗНОЙ',
                        shopName: 'CВЯЗНОЙ',
                        url: 'svyaznoy.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 60556,
                        regionId: 213,
                        createdAt: '2006-11-13'
                    },
                    phone: {},
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: true,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — бесплатно, возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_o_PGznWxeOWGjEBik6k5oA/orig',
                            width: 344,
                            height: 689
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_o_PGznWxeOWGjEBik6k5oA/orig',
                        width: 344,
                        height: 689
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_o_PGznWxeOWGjEBik6k5oA/190x250',
                            width: 124,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '8',
                            city: '800',
                            number: '700-4343'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '9:00',
                                workingHoursTill: '21:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.542985',
                            latitude: '55.793414'
                        },
                        pointId: '206461',
                        pointName: 'Связной',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Ленинградский пр-кт',
                        premiseNumber: '37',
                        shopId: 3828,
                        block: '6'
                    },
                    warranty: 1,
                    description: 'Дизайн И дисплей. И только дисплей Как создать высокоинтеллектуальное устройство, корпус и дисплей которого образуют единое целое? Этот вопрос в Apple поставили себе еще при разработке самого первого iPhone. С iPhone X его снова решили. Совершенно новый дисплей Super Retina с диагональю 5,8 дюйма, который удобно лежит в руке и потрясающе выглядит, - это и есть iPhone X.',
                    outletCount: 156,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/NVBFEnRNRBza2G7hFHsRDw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=YaJG7MICme5cKHKkYrlL2ISjp23HVGe9M98P8SOxI9L53g4rxd6jXExRZecTLav6266XgWj9HK_DnBqLeS7l1BLFJgWeOoEYRzpp5CZN8tlicbT9R95NRN-5hMalzlyt&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZF7iu178jPMcRkcA8IZdTZdp9n6gHDH9VdiuDRUi44VeA',
                    wareMd5: 'Mh3oV9XkEPGOxaMd475gQw',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256 GB (Silver) Серебристый',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSURRgVpJnoogB2Cjwn35hkf_NgvbpVCxmMobBQL2etouJPJ5Mss8hkV43DJGGswp6_awogUey3bPObVnsSov0QdTPmbe0w-7hRs9yF6kMnOxjV-IoTBpodn_rcxe3jSnnnd9Gc9yFsa2PlPrUk0O4UQ35zfEPOwa27zk3eDdNEEQJLBjzpNpMlQr6NchXi4KLYy1VorWJFhy0Vp-HDdNZohIktAKhpXBH0V-tovWKJQmSlaox4VX4y_9JJkBGLSwdaMzdt89XVUX2hS6nRKDCk8uQbKs07oDMEc5AeXu3KAXBpjFp2XlEeZ__ENvCHMmlc4T_3IAA8uLWIOQWAj5VhSEGbsKDFmswgnqNBg3-QlB6ld3_-LiNXMCzVao-ucBG10muV27NWqVn7VotsNh4mA1YAmPq3Zgd2wh8ikqJZLQTg8aJF5cRLgAbv8YfTthraoTk9p6DvpUZaoK09mrsboKhUGI4ncr8dek53E5QKj7B3SPSgEUhwDHsM-qHYZ3YtgHQqT1hLCyWa-z0lG7401_MC0UsEOg85XdajId0kGO6SFgEJS3VECgW93n0E6PoeMpm686YOWcVtywT14KygtNIxNL1yUZOkLVylsXrwoJmcS-rlIAz0qIu_1e9qzirK_zFYe2PiL-pD7uX1htRSDjuwGZ_D2ZM71IhgfuYsTTJ_peuPjCCmj6MLoEUQBhwdtMQzLyW1pka-jH49C4O36qzwjaXUbNrSg_dZUHogeLY1Xx41vAtaNbeA8EfLDn0mBBkMmY1usRFwnYEcKN4Xd2QPb2xlo9dlIgdEc6QqQhVRfceSXKpE3D1WnWMnRcuDw_Aii84bJF7I6UdJvOqWIQ6ztm0LR84Vb_bNtLaCt?data=QVyKqSPyGQwNvdoowNEPjY19xQRQQW5JrMEmExzXqz8o08AAKLpDrS47b7ogAkPaiwEzNCpuOOIDBW774FmaPViM2StdTMD-3J7nliavHW2SKZNbrYIf3dfYxgWqd-9Xa0ZwsOr5FARFSsCY9X2FOyWhDSXGcIYqxw0NU_U0Nj1fXfjfMtvNsWLRAAHM0RLqsGfoOQaw_Qf7CGQz-uOc4mjA5kNDAiCD0N0vzknNLpiHXnZDWr50sSA4TjdCBi9XWgeN63ZhR6nPubwg4q9SmPBKUL0NOdk-10cCKJ_Lqv9a4nNYOIjg4jxXCDdWpWxH1wVOrE_-YcOvCoJ1LBkghBO50nrnFxpAjvIEL7NtXRE45YPBwm5JzIEToasdD79svFwooJw_xakh7MPFM43qYBootGCmVwGrO9NhSYVgsD3GSuqbLK7TvoqdbRi6Z5-qwS3nfknJ_uw_o9TG6s2LELWqxBV4MucWT799a9a8KI5s3ajXFfZXUtenIBkMWhSnvMicXg_woG0UEsw2gdWudADYYMYVVdBx&b64e=1&sign=cad0887dfc9077a39a2cc1be256c2c71&keyno=1',
                    price: {
                        value: '88000',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 111736,
                        name: 'ANT-SHOP.RU',
                        shopName: 'ANT-SHOP.RU',
                        url: 'ant-shop.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 589,
                        regionId: 213,
                        createdAt: '2012-07-17',
                        returnDeliveryAddress: 'Москва, Багратионовский проезд, дом 3, Вход со двора офис 13, 121087'
                    },
                    phone: {
                        number: '+7 495 642-20-94',
                        sanitizedNumber: '+74956422094',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mSURRgVpJnoogB2Cjwn35hkf_NgvbpVCxmMobBQL2etouJPJ5Mss8hkV43DJGGswp6_awogUey3bPObVnsSov0QdTPmbe0w-7hRs9yF6kMnOxjV-IoTBpodn_rcxe3jSnnnd9Gc9yFsa2PlPrUk0O4UQ35zfEPOwa27zk3eDdNEEQJLBjzpNpMlQr6NchXi4KHLH0-f5-hm3uxJ478qI4xF-Czrb-ZPG5uVhUa4Mu_QqCHo22gMaYaJAjvGrN4InULMRSyG3DyvhVQFZcrSlJ7TzMkHQnlGOTlbRpmF4brRgoCfBaScHD50fAaJJkDwQzkkk9zW3Zy49AyeMUMsN06yq5v1jwYTOWa57hDnxEa5msFPiACa36iAxwDnFmxzysRkYsph7Gzt0iEA8IA9uzRns4VwBne1ZW-L0I6c_UtKeOCf6j9CiXaF4KRyGdNmaj02ShtX6hvuHLKN034cUCbaYJUERxNTDBkGDJ68ZINUbem1mODV8ZURjhFmQ_WkDGh3bj9w9BBLvnp2h4qRuxHKqxBaXPxDZwivZQlFbiZtWt0P-cxgy_bZpMU3TUZd8o5uguzbWLAXbwbEA6SwTE9Rjkx1aDeOIW3wY8iL1UTVfH8ENqueDSbCnVUNCaCYaqBmVuMVyDoSYbI1lXNfdQOuEqgtKQYx9-0YN7ZJMjaNi_ce5LeVaHZ6FoYebwwc_ZPQm5TYD_YY8Zz50FPLQf8HVfiEIZWVseGrvVc1wsx37ZKadgYxo3I93I5stdpjilT7BpnrTLCF62ZmcAUiplZvS0LnR2u1XieBhwErECziw15-Mn0nX_IXhJDg9p9oQleX1tIsMt6gRZr-vWxRS5BN1hUOMFwJ_8EuILspu6U3r?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-7sTgx_OzsuTJihsAqKQzHeHxy9TZBCO9ajMtJiNRy06SOtMsZEAo76a-nyP9uUakK9PnF1VddAgRUGd3sJMfi8rfT_LjkF4JyMR-8aZPKQhgp2YXw0ROMssS4tY2AD8htor4H7D6nKXLKH3L6oA15cfOg5ROqJRPmoBdCKkfQVw,,&b64e=1&sign=b0fa15cc1ebf0884bd35875b6a8537d0&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '400',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '400',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 0,
                                orderBefore: '18',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 400 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/202387/market_YPWxQKABwFOyu5l2cOEWDQ/orig',
                            width: 700,
                            height: 700
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/202387/market_6-L_GE4bpgbbFO0mQ3Njag/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202387/market_YPWxQKABwFOyu5l2cOEWDQ/orig',
                        width: 700,
                        height: 700
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/202387/market_YPWxQKABwFOyu5l2cOEWDQ/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/202387/market_6-L_GE4bpgbbFO0mQ3Njag/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '925',
                            number: '888-0298'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '7',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '19:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.510152',
                            latitude: '55.744992'
                        },
                        pointId: '273901',
                        pointName: 'Интернет-магазин Ant-Shop.ru',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Багратионовский проезд',
                        premiseNumber: '3',
                        shopId: 111736
                    },
                    warranty: 0,
                    description: 'iPhone X: Здравствуй, будущее Дисплей Super Retina: Совершенно новый дисплей Super Retina с диагональю 5,8 дюйма, который удобно лежит в руке и потрясающе выглядит, — это и есть iPhone X. Инновационные технологии: Дисплей и элегантно закруглённый по углам корпус интегрированы ещё лучше — Apple смогли добиться этого, используя несколько уникальных технологий.OLED для iPhone X: Это первый OLED-дисплей, соответствующий высшим стандартам iPhone: невероятно яркие цвета отображаются с поразительной точностью',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/Mh3oV9XkEPGOxaMd475gQw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=njIyhqIfl0dpLEECGt9Q6juKt7D17h_Da35Gjxm62zdl9vSJlvHf0d2V05WhPuSaFIez-gFD8Y8zUqyOuMScs9zc9BOlYdomcmQeRRZmY49HvO7lsMAgWHLjOmhkspp6&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZFNpj2jvJaEeRgJzTcGbfRBfjxoWpHVBHh_wgaEJAeh-MjxI68wpQbBQiWV0rFFwYAfZd2tVmXUeFmbG-hdeW5AkcuGEih6peTxzfdgm4zzeGUjrXzm6POR5zqOgtjSkw1OXvE5fmghlbtAHnbhdKMJSDxwuLHgj5xbKqXjTBH0lwsOn6p3a8kTv17jH_WFLd1c0KjX1sZrqHA5EnMK9fEDnye5vvoQaviBmUFWK3gMF4CaXAVDmOmLRFkvJN41Jau6kKEfgJYAYfHRjQ2C59NQXqi3mdTu15ESPrgMcOBIIW1iwe0nQHIg',
                    wareMd5: 'm3B0lYbYsIgP-F5nCMey1g',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256GB Серебристый',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6tpoTCrJQNhef6gb4MCCJ5yF5K4botyjmOAFqJM1zdG4H8AM62SKG0rGoFFKIOzLq6_By7FoX520aBV27jpIQ68uFgWmOJCk4MaI-qYddZ9TMn8FAaRx0uT8SF_OcGyKwk4_h2se7i5tgHX7G3TIHHdYtf_8CIR9hZiiUb8nrwhxuf08-90y2j0Xrc_-yrkTM5-PWJJ-w1HTBv8o7XXHw-4VuhKtmZQ4vgmUu5LqUm5bL5pqDEDtWiTKcQhRjBRDqKtOgcHk3IxbIC0Ls23T7QkRr53ferAXmoxxiThm09eiUMv3b11dXkezpCJcMTZZ2SmxNsBn6kgobBY46TD5hRmqq3ujVwPpfrHP53ul4ycG3JaWHq_JRoRhPwVuZuEsnn03mLWa7Kyf8qNF85ZsIiW3M181VxiDVXq6sciT6C9yxusobblxZE4xrZjiKzmgIDp2gPlRLDplE4-bbGfb_VRqFjnluo_RlLhPxe9lUG6tvdOc5ECq9OhDR09n6biq8ytL4sZ4TbevMJWv5W0dhpnlJzlrxVJDg6u6O3nsckw71ZyhQc9RTbtNVoSbRlWwEU5tOQ9nQfL32_Yg8cS-6C-qz7o90RZPUajpT0pYwb8TIBqzFgWqlaNUm5nyIWWpqwegIyZTq2HyXqm4cosNYv40QM4cru83EdoPV4ldzrhBH_dC74sLkjlrBRTBTXQNUb2c1HujJiZe-xKolusD6n75P6jBeIIdw9GO2IDFEWtg_YoUvSzj0CtSgByhG34xjz4nmELk87UyKCsOJXnGBSi17Wcril10OpQ8CFFY1uHQmioRssyJ-c52j1R-D0vhdybMS4TUec8rNmnEUuh5EschnbHAbrnP1gxqZ_7FIa?data=QVyKqSPyGQwNvdoowNEPjVpPboYa0ioUFVtKkPAY2mq21KJUlQS9zG9cYuf2iXOwjYXTqRTLJfjpvRXMaSYAL7w4a_6kGnswDOcgRx1dn7Yz-IH0nZRT_ldVl2kiZuA0Qe5DBp3_1dESNlirlA1Lm4XcydVZBbF5iKlXIv2iMasgg07VeUignZJyEtLa5CrodE08jeMLTSncl7Yb0gkJpc72DWA6GwblMv3csYWNy-Ydlw-NKRGYFXYcyYUGGDmOLpxZ-3NHuVo5KMt1OKWhP2DH_vyqT793rWUPN7sDQWG8Zg2LzgPm3oOFmTehKe0QhVNrTGb-Sy1-h4bQrfx7_5Pwlp1QEpohjvzp_jpJpyWeg4AzSZNHfIuwOUkkjjcD9j1g5YCXRyW4BbSwKdUEeKdlr6CXPMm2--2w6L8U7v8tIKQdG-_ZCDag2s5i4Z0SCzo03_DgDWY,&b64e=1&sign=d4fc4b8b6f5928d999647b1a64351633&keyno=1',
                    price: {
                        value: '91990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 74832,
                        name: 'Phonempire.ru',
                        shopName: 'Phonempire.ru',
                        url: 'phonempire.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 2738,
                        regionId: 213,
                        createdAt: '2011-09-13',
                        returnDeliveryAddress: 'Москва, Юных Ленинцев, дом 83, корпус 4, Офис 16, 109443'
                    },
                    phone: {
                        number: '+7 495 221-79-11',
                        sanitizedNumber: '+74952217911',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mU6tpoTCrJQNhef6gb4MCCJ5yF5K4botyjmOAFqJM1zdG4H8AM62SKG0rGoFFKIOzLq6_By7FoX520aBV27jpIQ68uFgWmOJCk4MaI-qYddZ9TMn8FAaRx0uT8SF_OcGyKwk4_h2se7i5tgHX7G3TIHHdYtf_8CIR9hZiiUb8nrwhxuf08-90y2j0Xrc_-yrkTkkqqV4zSE8jg_2mIi81acdLcTgeY_ABBeTYUzsbez0i8hYaRQ_E_Op2L7d1qQbNtvGuA-9LbyS0nBTkkqioXZ2IEIZ1jg7K58s2EhSvn8NZOMFv4Ucdinn2gXALiBdyX5h8vO4SP_bYOjMi60E-aXpmeca9aXpijIqneacHhXAzmN9Csko9682yZCFv_eFglVMa08Ahv3FUEcV8jR73puN7VYJxZuFggQHueAlLThjQhp8TXWEez53ZPLc-nC2JFg_Lkj0WrCUIbaSFZN5nRBmA3yW7hRIvPfDTBO_r42f7oLDw7gVOWk9Tk8DGrgkIjnBD9zYbiFTMKmH7aX7ba1JeZjo_Cq3MyC53nMveaFFkM7Vruy68J4ZiVuMonfe7ERJpUmlcMUAHA7U0wOT4NOfnG7q1IuYpHKFsQJDln7AvdcgyI0CAmOqAoZAGpDftJ4KfhIshduiaFKDi0yTqdTuTG1Limi6SiuDv4X-IlrO2IjiI9FxZFmPDzKtPrZ_OnKoSbL9cy23WNkqb06nrwFwvZZUg3pR_tbVn909pF8gYPwc2JGZs1JvoRuHmiYD4ulXsbkBzqTSsPLHM3qNvOtvMbWhQbv1wF50_FKquN_pSddr-waM9HlOgxmV8DuW1jGxQTKX3wJ8tM35t7s-fKKgBhWcJNhfPfXOZ9zFVgEB?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O84g3V9WzXhpMJsX5ODJMpiqzShBOst1ajAS765fIhBVwj4JpNGjUmDBg9jzQ0WTtXM7tj84JmQIY-Ss-5jGhZDAzVUStjxEel0D6cKMYqm6z5BmGhef6QF0Ea5SRvLNTvnByzKG569PxeuRh9qvllgImay8EDUaoPa23HkvNvqpg,,&b64e=1&sign=14fb8ed263610c8c9e40529e326865f0&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '390',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '390',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 390 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_j8dSDt6hFZn4QuaIXWC2wQ/orig',
                            width: 800,
                            height: 800
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_j8dSDt6hFZn4QuaIXWC2wQ/orig',
                        width: 800,
                        height: 800
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_j8dSDt6hFZn4QuaIXWC2wQ/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '221-7911'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:30'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '12:00',
                                workingHoursTill: '18:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.77384292',
                            latitude: '55.70235181'
                        },
                        pointId: '297606',
                        pointName: 'Офис на Юных Ленинцев',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Юных Ленинцев',
                        premiseNumber: '83',
                        shopId: 74832,
                        block: '4'
                    },
                    warranty: 0,
                    description: 'смартфон, iOS 11; экран 5.8\', разрешение 2436x1125; двойная камера 12/12 МП, автофокус, F/1.8; память 256 Гб, без слота для карт памяти; 3G, 4G LTE, LTE-A, Wi-Fi, Bluetooth, NFC, GPS, ГЛОНАСС; вес 174 г, ШxВxТ 70.90x143.60x7.70 мм',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/m3B0lYbYsIgP-F5nCMey1g?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=YaJG7MICme4FeVPg9Rx9pRrpaEprXIxxBgU3wi9PPNobfiJHrHCXgm3zbhYxeyDOIj3njEe6ZOWeABsBJ8wizsAGalnSU-3kFMbY6Yt-UzBCnzsceMP82YNKwbCKAR4_vLTTuyC_Ttc%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgbHb6joFcwnl0GEDJt_cLT7d9YGEiK9jA3lyiu1cpeJtkEGnwVDCUMY2Cc-81-yFL2Dux3DJMGKlSFbpoUz44uzYouk5oXtOsK6EPtw85GOiG7YIDQfZYbLA8-2f7oVMNWcbwbvTBWRsC3sTa4VbZZ2T9SWdGQGf9GXCwYD1HTbR8c1qKcVeAEpuhuV76R_xgHl-y4nfHlB4o-4MH61WQM19wazz1sC6GugyxYnevgx-1-3PbDjgsCOQIvO_jQ3dndLEEGKlKSjBTz6DV3ydiKACHHbuQquSusSztJcP14arwoZStDVU8lf2W1x3BB-J8o3ZNUNalHE9Z_dP3Cx2nw80mj2OycBZF9-RKuINDsnt6vTkmwLp4359osYC6XtefTYOmb4dJ9e2FucOaUysNAybl1mLgCphMycQLZRyph79EOAktOK7xBQIWgU6xXbxkH2mRRK6q7yrS3JZEKY4n1sYnQi8PlRh2rs0UeQ5Xj0U6oR44oqK4YGqp7Bcv_KjFJW5LwcvoFdQyvaFhZG_gVxAFxAPzAc8Zrqz7DmA230Jap4HaXDbmIoUfn9ASONgtWSryyA67RNLn39jQ2SuNEt-9f0aF5DDmZmkcqfuQKDl7Sj7BKioZSdBe9kt1zl8nLE4x-jtw0--jZFyBZQFN-SXcZSQP4G2TF12SCsFf8HG1WGnb3JMZdzSFf8AVTmXcev4zQA4Wia92iv1kMrKFEp7lUAu5xTv_w-FETuhBA1RGhitjrqm66gkQ6aVZ4m3k32tclhAGmQoyfD-4vwIFKl5yCQ6CgERHeToz3jVuSswRLTgFdWlyS9umGFMxiaS65Ot5bvy--W7kIFKc8zcrj6NKsj8yYx5jn_eQi_tVTyVj-gmykcnoUKYBDpSvBR4JuamHGuXWRHM?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-620yvVzTqRVPhBZ31cCTd7YzxdqZWnuzJ7-UFmiGPRneHewG2ooY7L0GKzlUNulrBEeqhxcMsTtaCB8qwj3NAoVrz3ht8mqjQX7a_xDZC4FH1ptcGauczW_8pbYl39aliqX9CHv1lnhBJ0t2DNEgKHNUqKPLWDuM8U5iBI7CiQWgGZXok68ZAYciREOhc3V9XgQUpVljsx-gvJlVn1bgEZzy_5_lzCkFVSTPMi3VLjym1hjDmgftxxOIvDb_vDcTdawpGKuLUy38ke4w7ejNNoGteYYpFOktEIjwP43KgyZ_DvP-PNVUMkmMchktj_WVjdMVYvU3VKpw-2Xbnt-HlewE-UPtXxTaM,&b64e=1&sign=bba9c082f533a8cd4b25c94254a77329&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZHS7ITIm-pb6kMjFMVC5y7ZX-CoSupKeH_vSUzWIL1M3A',
                    wareMd5: 'PLCjg2ESyelxO_9xt2dydA',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256GB Space Gray (Серый космос)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRAywF0MjqeADEL_UmXTAFJ206O6OB1WZ7r8lozoor11gUqoI-W0Ms37jtJ_ZIznZA215w4cIeY9rSOP_N7L_hZypCoT7DZjtW38kS9D4p0Oj3rG2D72bIwasJZpeD9AP60JOr5T3eD6vZxNI53UzVjttmD3bQh2m2Vwm-XDwXkbDCdL1hMjJOtb4DyHhJhEnYOLH2R8Mh6GtsDPa3b0XgsOKgoFH-iVHAlPF_ph4kruFtOarHqTi02SoKEVrDe89Z5_o6HQE3X9QqAfGMrijatfo9suLhQXse6xzt5N0rPOK5AnidETxsjqOklZBjUXLK_t2prBLn-_R5IHzW6eF_8BCJHwxhUZmOzzuZThXVg_Cy7QrB_tpc3YUFogOo3FP-x5kJC6QaVUzgt1zHF-HmXuVn3XAYUqntTfYyRH1ZlQyHKCJY-gdm0DVZutVzG10etQRwbwwAAF_4ULeLzAenhMb4jo3G1dpJe0I6vgcKppJv5h4-BdwhbyEIWwe2Mmr93DKswXjwDQP0lG39HXllC8pOIJJ90QqNTfieJRPQgCUvwePKKSk5mHd65r5q3ZaX2Y_yA49_uYVUCZUD9nADSLIBf8OXv-63-XoxDUHyQfsLzkVeLw_xt6d8fOXEG5_6eB2YIG2OeES1Jp4yHRv82d4bZrkfRD2ga9n6Sa0JoR_ZaFt1RipSC0wyyqqVGfBt5MhQycBhU7bzGCUMTLjUcpEA7uDRYYrz4gDYKGM32wXDaJjzHd6s6DVoQI5wAktW8WYJILs1smNagt6CCm4Y-QJA6Ex5VJukOgdvd-jrqoMvDP_8Pq0O0m7eFPQHRIJgwW-FeXCxV37Lz5fMGYcy-ejY4Tbz_1sDlVSp8IIb1j?data=QVyKqSPyGQwNvdoowNEPjfhSuoOT0P5-RIj9xcemuM5C6pTOcmaqXHkRBNKtw26eVts4jJrFNuVzAbQrlmLunxttGyrW5wDEMOTueypm5rHbChWNVezio3mG6Zzg4swZSa8Sa687e0cO8gvZWxHMFt2u18FYd0d5doj6mIUXllXwwH-xZDeB6OA9YAPhiu9AqhQsXjZw2Y4pEq75A7df4OInECx7_E89POkK4rSq_OQR6axAGK-FC8RQGQ_RVTE5s_9VrHXeGs3dgSqMp0JTIwqu_u7HM6qkW9C9aeRKE_O1kgNgl7c9YTGc_hNQ0LFHxzQehBvBLsm8jlm1HVShLM1VtmmtJpsgU0mwFimmJ6x2Es31zCGpgJW-qwxqMQYI46hiQPz7rCQcqia9xBeHOKaEoeAx9w3Ff15S8P-AeYC0a5ZyD-CbWhZth_2UVLiDC1HF3bB6Pi_2CBrwfjaIOUxPlQZROkU6&b64e=1&sign=1cf8736f4c3ddbdac739f14db6d2fb94&keyno=1',
                    price: {
                        value: '88390',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 37758,
                        name: 'МегаФон',
                        shopName: 'МегаФон',
                        url: 'moscow.shop.megafon.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 4067,
                        regionId: 213,
                        createdAt: '2010-04-16',
                        returnDeliveryAddress: 'Москва, Большая Полянка, дом 30, Пн-Пт с 9:00 - 20:00, Сб-Вскр с 10:00 - 19:00, 119180'
                    },
                    phone: {
                        number: '8 (800) 550-58-58',
                        sanitizedNumber: '88005505858',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mRAywF0MjqeADEL_UmXTAFJ206O6OB1WZ7r8lozoor11gUqoI-W0Ms37jtJ_ZIznZA215w4cIeY9rSOP_N7L_hZypCoT7DZjtW38kS9D4p0Oj3rG2D72bIwasJZpeD9AP60JOr5T3eD6vZxNI53UzVjttmD3bQh2m2Vwm-XDwXkbDCdL1hMjJOtb4DyHhJhEncG_nG1PoGmcLXLZmZjLrLfWbYMQTjZZVTliDB_dvO6AzihfCKZekexWwoTRZHY1syIq7m0yEccOfljgRbABPaT8E9lSz5R2D43sCluwA7N7jLkTlEQDY54Wd4LyxfqXnD83NezS8oAXzeonRlm58AcUSE1iEt_Ov6BFNKYmtvokKz4C6ysUzHJYMdBddDtCBAAGy0SFprIL2O-OM8bA040nTUHKDTVsfSmmdINSs-0JR8GMjJVSlI35E2Cq8jWwoiv3Y8-mYWs6fNufSuTGTKECQmayihslLFmeT1GWVDZNvzdn2X0pHkQ-2RV7P6NjtHsSr-WUvgohK0g1F98oHIRZ5O_Ztu6K-bL_EyA4XoMAxzxSJ6L-ZWjx8um9jbnwVaXrAuhnOSp339yac0EHIjVQ6G_hTfisAXxXn1gnRpfFa2xA3z_2HkZJTdGZgCHR8NULQeln67zTao7zy3P6LKMUOaohQ_ItMHhfcLtIUKd0m5_CKiqeIxsxDyqhQBoKxURERcbO_Oel7SiBk9dQTq810EjJVFaHF8fsJSXFo-Cq4TV0KAmsClYZpBcn6baPwLfly487kRNIW7gnVRKv5hMiAMB3bWIah8HGJRcvWygNEudKZQ1Bp2woHqLi3KpdBiQpo-ChZTryWcwiBtf6OaS-aA9iccxIRNXH-3R7X-LV?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-3rkZ3Dc5mte-Yo4b1_ASj9IWD-1k12hWmMIT8q6BvdlgRCGj7FhA3P3OG_LrgL5bBNpgWGnweBNnNNouBeTgCUM3cVCU4DwIOY4xYZak0Lj7P_H6689GF6LqR_EQ8h75jvRmvmMQrTiZxUpfjBpsCuth72X2EDwO_KcR-uFW1eg,,&b64e=1&sign=11e1d2977c1d66ebb2d24e515e92be2d&keyno=1'
                    },
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: true,
                        delivery: false,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: false,
                        brief: 'не доставляется в ваш регион',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_TU_cHtGUlloDMxeK1MkHVg/orig',
                            width: 400,
                            height: 675
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_tRwnu1UQL8uphaAB1-JgsA/orig',
                            width: 900,
                            height: 675
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_LZSzNOl0vlyAwB4Ex-V9Rg/orig',
                            width: 400,
                            height: 675
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/205271/market_5bZhvuLv19BQudEnrUaObw/orig',
                            width: 900,
                            height: 675
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_Q0bJKpf_pbXAp1HUX3I3oQ/orig',
                            width: 900,
                            height: 675
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_TU_cHtGUlloDMxeK1MkHVg/orig',
                        width: 400,
                        height: 675
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/219360/market_TU_cHtGUlloDMxeK1MkHVg/190x250',
                            width: 148,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/173412/market_tRwnu1UQL8uphaAB1-JgsA/190x250',
                            width: 190,
                            height: 142
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/204557/market_LZSzNOl0vlyAwB4Ex-V9Rg/190x250',
                            width: 148,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/205271/market_5bZhvuLv19BQudEnrUaObw/190x250',
                            width: 190,
                            height: 142
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_Q0bJKpf_pbXAp1HUX3I3oQ/190x250',
                            width: 190,
                            height: 142
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '800',
                            number: '550-5858'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '9:00',
                                workingHoursTill: '21:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.618947',
                            latitude: '55.73404'
                        },
                        pointId: '462051',
                        pointName: 'МегаФон',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Большая Полянка',
                        premiseNumber: '30',
                        shopId: 37758
                    },
                    warranty: 1,
                    description: '2G, 3G, 4G, Wi-Fi; ОС iOS; Камера 12 Mpix, AF; MP3, QZSS / Galileo / ГЛОНАСС / GPS; Повышенная защита корпуса; 21.0 ч.; Вес 174 г.',
                    outletCount: 188,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/PLCjg2ESyelxO_9xt2dydA?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=ss03YTt_lY4hnzepY3ifsRTvrm4FVvOpiY4UeeiYbvioaSYNfCPdPrsB4l7JTAPRRdhk9Dbe_fk-QBky5eX94wDF9UpDEr8j4gYwUZi5zh8Fq0_MCoVjRCkpYh1s_Hd2&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZExjpQO0Q5I548aUxv1rMmhNDThTSn5qoLcbpF2zUDO-jbbAundqKP5OjEcPq9FkLi2Ke-beAlWuoHvZd4d1ITM8TrDwNwNj-BHxfnd8fMzjTGagJKyXiYhdteHtyAXMSAA-oiE66PZm6zsqTnfaTk7M8zuDjZLCmC4dmg4xBypCcQ5tdlkQ8g6QXTsfAqhRBCJddNs_VtTDm_A7bjf04qjPBZaCBZa_4Bm5wH9cvUY-tCj83LLjg1zPeC5QFgLIgsIhNjKFfJRXiTJ2_ioc9tnqDg0YBeXGJjSMnqtdEuM9Ea0O_fsQbsX',
                    wareMd5: '3CFAr8VVxCfVy9DweoiJKQ',
                    modelId: 1732210983,
                    name: 'Мобильный телефон Apple iPhone X 256GB black',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mfy_tSMWrC53cJrmliN8LOQL06XCKNWUSIxE89czKE_vueRfwyffWq58QvmbJsePDCr-zZhb6ohlg6CnfJS-NTdNUrm7AqnxjlNgwqA_NmdVDydfPL1md0DxsfzMh4iHl-003Vd-ETntQF51t1NEjwEWAK1fQM1mZEsh2BUmYbgXOVqw4qEx9C_5CVsAdpd2ECVqplkIM7mfS7AsbVfBTpX8mgbM0RnXY-rTHUKj_eh5Maz8S7ApqUScwpvjOOtv3b1BtcVgjRflKkng4b8uMyRuCT9CwnGdHjUW5HgaNJU0y72WHs5WfQ9Wa-cmRhhWIOErSbPsOGnlayj3qc8e_E8O6miDMAGRdObYbQ-4Ixt1gOJQ8YYcyUH__wEKjgmaD3SCEoM7xvdoiqYuJS9fMVjFUSEOHlw8a11qDCDHe_uFkuKN4tAhbFU5zyhAkomnq3mwAinpmpOrRYPcMDsSG_AWEMY0F0UBj0lKa0RZ7nowQ5dFWNK1-2xAHAhp5JZyLL49tDsQxx3r6zOurPOBnEKhFOTEvdMNtq2mu9r3fs3iD2RhQ8NXmDfNMhBYBqheIh7nv4BE7E7etnqaZJjluXct2S91dbjvPIpFuonEKmQ4vspYQHeVmbBVWNzc7pPsN1ujFL0_Zw1a4tQJG7XUg7swMx6Vg0vVvGnRLRnEWMTvcV7e8_Guq4cgQzqBVccxXPeF336UP8HeG1UOIUck83dD6ZbzuUqGV0HI97bJqlsf9VcfeAgBYGibZgc_rSX_96WweRHVaBUa5oIv4ys_KWk2fvVCBPbs5DqRG-YukksZrD6jBAj-mgixcj7ZgJMdbJKI3PMh7ZBAm35o9JFfqbsSRMV9tmpimS2oI7bSHkkk?data=QVyKqSPyGQwwaFPWqjjgNoGyA5V7skirTZyxRGDBkNjOk4UjGD9z6CHMTL7sx9x2HOils0CeY31sqX_fi417mpXYhROHuqVlK2iV487Q_bfJ-cfumSesuu-QKauvITjdHctCnNBsyxZF9kTIO4oe6YJApH-KV6zWq8WtorMniy2WURa1nhQjSMjzv69fLOfFys7nIMnRjl4CU4scFFxLDM2hUyh0Xwpse8oC_6T7tp0dY1uF3cl1dZ7wl55_Gi5GvRUFz7HcoXYjpDAKqF1GVH_ZZlbFePRjFY52HS0Z4vlxNm9ryj6WiNZIjAiouhsyL8gTGvZJqgHCj5w7CDSa7MCM_ZCHruBad9EObCx4cNXNDIw9FZx_8de24w3jIEmTY_nDPu-EGbUSz0RWS-zrbUMaz-5JGxeB6PgsxItsgCU,&b64e=1&sign=e07ff2f79507fd55a5eb3a1118d077ca&keyno=1',
                    price: {
                        value: '83990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 58825,
                        name: 'Video-shoper.ru',
                        shopName: 'Video-shoper.ru',
                        url: 'video-shoper.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 10658,
                        regionId: 213,
                        createdAt: '2011-03-18',
                        returnDeliveryAddress: 'Москва, Пятницкое шоссе, дом 18, офис 172, 1 этаж, линия Г, 125464'
                    },
                    phone: {
                        number: '+7 (495) 648 68 08',
                        sanitizedNumber: '+74956486808',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mfy_tSMWrC53cJrmliN8LOQL06XCKNWUSIxE89czKE_vueRfwyffWq58QvmbJsePDCr-zZhb6ohlg6CnfJS-NTdNUrm7AqnxjlNgwqA_NmdVDydfPL1md0DxsfzMh4iHl-003Vd-ETntQF51t1NEjwEWAK1fQM1mZEsh2BUmYbgXOVqw4qEx9C__HMuR1TzjttqDlpRHDgKhZosyQF0jeNd_f7byJZff1Sxixj75qv4uWBD9fEYrWOUAtpQGRLiLWap4WgolCxP9nfhwUKLB44HuSZxvisQ-6pNposcFJ11Aeh2PgF1ItVSJpJVsI2jFHFqaJAzZf5JVjXK3eDU3M407Zvi2i1lbMG81twWmkQtDAsxvFewi3fbdhb8ZhNYpwd4xwDgrv6_BZRHlkVEuQtlIk5LAKcd6eO1MIl2TZ-jVS7bhEh77F37ngxGu2FYEG8sIWT9U5suP_pYL93jLXtdC_kxIn4Rlout_gQsZYn_Piv2imqwsL50DWT5_V_94ZOLavzEq8nAKYy9JrwIwTmZQaDJJPNWPIevnKfN66wu_y0SWGcULUrPzoNB7QxN5gB1GjhQIU8H0OWaaMFRnQMzMDmN_-8hvFx_N9f8llVomGtHt_KWBU2Twuap6RBy4Wi8v8Sux_isOdaUn4RnF6X19WJsd_U3VG1iUDn89jG_mRvIhaD4HMitXsJIiU6PowbCkSb-tlVXCa76m32xSxIVIEv1zwZAAcsOzsoXMHfr7N-vDJZN_HYSQ7Rxs_nmrc3sWg_C1u8b0GJpCCKT8d8LpIFto3y_Jg2prwYWhqF4i5T7czRChFMqO-zzymj-y6MJhBGQIDGGDIAvIl2M0Tt8iw_s61Datex8GW7cLwF48?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9LIm6yq-crnzeQ5_zUQVYOuk1qgfazczPUPsOHqKug3mIrbjMzTOt6VWiL59maYGdXUxj1VL0LR5I3iHWqV_KlxHVs_u0Ridukcs6RQbzZwR78QM61bIHl6UGYZwDHXQPtbX07uu_GuxxFUN2d_HqfXOmlxDFWzg3Vv9yvR88EdQ,,&b64e=1&sign=fadabf61b24ddb040ba74e5d89f3365c&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '400',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '400',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 400 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_ULfbbkMTv41Lu1DJf9IxQg/orig',
                            width: 175,
                            height: 350
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_ULfbbkMTv41Lu1DJf9IxQg/orig',
                        width: 175,
                        height: 350
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/207337/market_ULfbbkMTv41Lu1DJf9IxQg/190x250',
                            width: 125,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '648-6808'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '18:30'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.383642',
                            latitude: '55.844324'
                        },
                        pointId: '224347',
                        pointName: 'Video-shoper Митино',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Пятницкое шоссе',
                        premiseNumber: '18',
                        shopId: 58825
                    },
                    warranty: 1,
                    description: 'Стандартный набор, USB кабель 3 метра, карманное зарядное устройство 2600 мАч.',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/3CFAr8VVxCfVy9DweoiJKQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOj2_kg1YpqB1R5Kg4oWrcZo9ywH5Hg_lvIwN53tqoHPeG11XKLgQc0zZ6fp99ygTw0f_DsSBsZFZ16G-6nDzixSaWvC87pBCDAzNLJA14UtNZIxlz-JY5MBDnnpAEjDyT0%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgbPeSrepLMU__58MBSH0PIHieKQMKwMpD8ouuIr4B8mDg-hyVF0lI-ebndIJjkw8J7HqemVaLQ9kwxca6XkpjdrA0H-CK6ppxiXcLza5FmwDWkY4Y1iHIdOv1crZTtCDJQ56FHRel_yjyV8jzFveR53yxPD-LEfpSiYaChScdHNyDa2ccjQ_3lPc_rr4-7os4VgP02F10l9djnCVuyrDQ-8q_uvbTs2hRKatCDG-ygBIEH-ZCQc2G0gnbwI9M8judqR90sZOpCKN8oJAdWcHTXjBYsJs0xJ_xNji_CwySgCzDXY4CokO1VRsv_MT9z88wFrGKlrbOWszrQ5813q2eXh4qLa9VYCTxQC3Dc_iaOZjxpstbmAuDIJj5105ZzKjeExj4e-1UbrTLIlXFv-RwXx0hIfsMU7fqQMqrB49fyZgaMpybSCfNUiiGAuZlw33MIo8USUZMpvUU_fOjOKg17TLtpckXAeJatZXXrba2VRVanmIt5y74VXYIslNFlXIYFtHG8_wGHZQknxipv5Q3KnYelSVBX11nmUrGvWhc-qVXqkNCXNdn5OLg6Vgyeum9uxomKy452f-gL0tFxt5-iCf91zXbTenDfkm5NhoPurjhejp3PViridAHgGQLyd11Q9_KTk4cdH4rvl87sX3EOvtA8OhKKeyxYj1W1EGXVoVfiJegOtjyeF55ltLv_sD0VhD0DUdI0tXrIWy35Qmqcf8M2TTShYXqormjDvg3zrYE8JnPJ2VNiAD7iGBhCOWZm2SApWnYhYtpusefSNtzzK5aABD1N2S62uHxiwVmRXKaRjLL8u0DqExLoJLQH6JqdMpAL57Uk6rhApV91-s8AWOQaOo5fiZ5HwTnIh9vHE4XF9c5W4HfEiiiX3sOzCcFb9qH0rKfeRL?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-5aYXSC3otoARnMxV6Iy5D4JvjFjbQbBO0S8x5ckYyZCsfLTxBBuIRxJs_0JOEE6aF4_K0jFu2zx1Ix0gcrQM-QQSWbNXY2DoHhyfwxqCSN9YHTE6kiNGVNVRn53pvh9-0b-yLm6SOS2UioO-xJzosdIbahtdgLqk1oHNMZkbbhHZ-ckuAunYFswnhSfnKRH1e7x7lkNjhIZ1ehU6sQmvDVi7gUd2aWnP5Dmc7Vd2KPPFwWwtzwwGhPRh6gU7wOUc1UwyGCJkAtwUSNO2siywtVRbL__VB5lv07W7skrVHMjh1CRM0USR0C0n9Y4ZE88Ebq3WpSXjAQljIi2ugpOctE2biL9uCuFyI,&b64e=1&sign=bcc49481a2a07081e2f0642b45477432&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGCE2BVV1WsIzEZHUBZSadOvSDOiMeDAUhCz3ITsdFM3ybqtxV2a3Mkl1Tl4O4dj87i1koIiG85VYRd18CIfIq1alPF5NvPhu6baGQ-BmKa9XCH7_vSJyoK5eyRox6q4WMIMJav2Qs9trzUeP7G1asfw0XIq0Z10JcHGXMXT5BH1o_sbIwmSklX-yJbZo2pfiSgNbasz43RGyB2Y5ElEf8Br1jENAlOCU_mUcy73-rZYLw69jwUn6U_qL-3yBV66dmu0zp0YPGsb7_7HauNfXifFQvuT-zpT_4',
                    wareMd5: 'q0m8FUrO_yjq85GKbn3IXg',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256GB «Серый космос»',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mfJTlfI-MDmU0RQQ2tn9_4tFnouRrI5FQsPf2YAlbQy2_wD_gjjM-PEsjC1gJ5BVImybeVcpORn4YghHf1jpWomNiI0cQ4_u7U_iPFYwvoSlVzMd7poXYvTSD5VxaSuh61S7XB-gSuSpLNm0Be10jaQkW64ggeb3VyHLQtLsuZYpFa7-fbUm2qk35-_EKmMvT2fy4cRhZp73VcXu-7falkYaCH6q7m8HD8SU8GRSnH0vtmPqW1uyx6Eqn8l1UicnmdCbkp1dy254Pcx1mJtDpaJLEkGRKzylYlX7zsLUlwfHAhQdu93mBYqVTUQheDz7h-K7wr7YKcz4KFNTUVvrlBSR4YEOvv_5UxRrylyYMienzw_x-kadYOpGIq7mekC4n03VqQOkiPaWvw-C2XN9Zuc-WMHsYmUwbDMMX_Uy-UQ3Wg22jcRZubf7BwxeXSqPXo3S08kqV5vZv-BJPPbOl_31KvvKsvujdGpYGD1gisLfQASQD3WDobLQTkGmvFChFw91QN5rm_f63M7a-FgpJXF_6j8x5NYbkHqzUWMeRsyAKVdX88ztKx1cESZ2Y6YN9WstHNZWDoU_rTO0v3vLV0uJBTsna2yf_xsqERPpq7t9CoqtZuB4Qk0u1xVQNveIvqt8zhf15LfKAYFyCe5opP4blkfeQsTigso8j79jOLktQ47tdcdslcW7Lo3B-DfB68TfsMDXX451ZtZxbScqwfFoaa4xDeJ1ap0_qGIYEmtvQjKwZ6v-vgxpS13EDvRs_P04-_eCpnp2M-3V0Of6qGNRfs0y-odQvWo8r6obbiapNhJ_Nb7HIkDw_JPTDiQqIjn8hhcSBYtA9AMsFbqRwVK8TKVBhiMgqFax_WgY0RJ9?data=QVyKqSPyGQwwaFPWqjjgNgJuAg6CpnlG8BD9bJ2lhmjAveWIfLI0rpqTPZgux8GcRtuECh6z3s__wW9tvEjtGDHViykjngVV3Ls7YOJT_TiM0VZIAuPNMAM_8xyvMvgjCcn0bCV5g314VKfbuXhjHSY4dMqtxPou-gsyudNodlv4yYOf9NhJFJ0DMw4ggll-VhFuLyMY9husr-sn_IgPWXSzHoH8vtIFoscCOyNXzynrRnTzwyyUmPZbkWB8dc30EgaDUZiGtsuw2rste4RuSj5_AJdEahYey25k1OwRTpEJ-E9GNY11Fp-C0cQYinN-c4-F0f2ynZYHD2PLT6U4bjiZBMKC1j7JEv496qzYQsTRde67WAQSz0HBYJcaBeYPMBOOpzUdtyiE_2slPlXHQL8PhIGOGkJHT4524TjoDxUCa5DBaBpDDbxT7y8NVz504-6eRFGw3uVKx5QK97i_cBs6e7fB_iY2UVFrEJ9w-chpsDGVqS2v0fvuQ8KZylZ4-FnmqDyF3smkeoaVAuig3a8L9LaAW5nkc-wRNMrKfG04xKGqxlubOE4e5ZJVXg6-URr8Pq9IIBR9w-f9n-fKYDSjsyrDgnySqSYFXlX_k_5E4dVnLHt8cdg0uGU6TbDKP8Y2TAwUnzltUYwltXOFxX_BtpXIz2-J&b64e=1&sign=570f36b7bb6bf40144a1e7f9ee522e0f&keyno=1',
                    price: {
                        value: '88970',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 259847,
                        name: 'БИЛАЙН',
                        shopName: 'БИЛАЙН',
                        url: 'moskva.beeline.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 2502,
                        regionId: 213,
                        createdAt: '2014-11-06',
                        returnDeliveryAddress: 'Москва, ул. Тверская, дом 6, строение 1, 125009'
                    },
                    phone: {
                        number: '+7 495 725-57-25',
                        sanitizedNumber: '+74957255725',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mfJTlfI-MDmU0RQQ2tn9_4tFnouRrI5FQsPf2YAlbQy2_wD_gjjM-PEsjC1gJ5BVImybeVcpORn4YghHf1jpWomNiI0cQ4_u7U_iPFYwvoSlVzMd7poXYvTSD5VxaSuh61S7XB-gSuSpLNm0Be10jaQkW64ggeb3VyHLQtLsuZYpFa7-fbUm2qk35-_EKmMvT98AMcbAwp5l5w9pkIQLr_hJcJotnJfTlVQ8MFIhsh5wqjXj5mMU4rMQ2tCNUwQpriog3UXCkHzj_nPFeu9Y2pz2yihf5tjIL5Wt_HZaJfRhoOpt-6Tj-xiHk2ikaVsYXOd3HISlOISqiOCId78IQsY49k74xCCDlwdtMQ4ZPkkHCSbL131TG7enLJ_8DlPfVOnHmBeKGs_qFTzAfHi3vh5is_Y_Tl923_G8ZzY9UFtQKLH5n6nRJRG0haXVDdtCYx4ataSNDJn9w-2FTqtV1ZC4OwPmf6hWNJnLv0VKC6sMBxPyYGbVytP6iCEwmkRtVCWO-V9rHpS2kcFKe4OOnVZyW0rLMUT5pmCiUvwhqI3PktUSQgllUuwBlVEmMDpOcyN4cNOWA-tMKpLi4M1DmY3_Ho4Q3J8DMk80_2Cx23X8rlo98eFZoByMcMwKGrpo9WsUHV4-GyoVBSelu6WUBw0ZCyHETf4Y9AD5cXFFIw8dG_4l4NK3bEcmbFyv-XLFe0b8j83FXADcBbLh6JUQoSIaDw-Ogox2tf8JS7IWTyilo2-sGHruoqbjhxfRlo5T59GGzHiUwW6r3mHd0e3fzQcA74auW_ObYKNSgHcljyC4JttQ_ZXwT_5C_U8l-TsOgZIdzPFFPj6ezaw2YBlurdGOnUIuSciT6CCtwXs8kiYn?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9vh583TAW6QcTPiZHOWPo0DqMJy4xgQOS3GcFsrAhB5WP1G3W1v6Joe7WI_Jx10-M1gxnFzqOhOkkMO-NGAGTT9nfdexxMVKl60iwI0JYf13YMyJYVccDg0f8AT1smmyCXeBFo7fGL7cnlG_jSgB2zbBVMge8DYmtLhbNj7OKENw,,&b64e=1&sign=5d990387144fa63a705d45e5a79bda07&keyno=1'
                    },
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: true,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                orderBefore: '18',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — бесплатно',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/176166/market_kUQREeVOxOQmwWMEbdlHcQ/orig',
                            width: 433,
                            height: 855
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/226074/market_vBqa8uXguwUUc2LcRbPzyQ/orig',
                            width: 343,
                            height: 690
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_J-LqLgx-fNDnRMPnoOUAfA/orig',
                            width: 45,
                            height: 687
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/365133/market_tsUw54FX-r8YO67F70btZw/orig',
                            width: 344,
                            height: 690
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/176166/market_kUQREeVOxOQmwWMEbdlHcQ/orig',
                        width: 433,
                        height: 855
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/176166/market_kUQREeVOxOQmwWMEbdlHcQ/190x250',
                            width: 126,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/226074/market_vBqa8uXguwUUc2LcRbPzyQ/190x250',
                            width: 124,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_J-LqLgx-fNDnRMPnoOUAfA/190x250',
                            width: 16,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/365133/market_tsUw54FX-r8YO67F70btZw/190x250',
                            width: 124,
                            height: 250
                        }
                    ],
                    warranty: 1,
                    description: 'Здравствуй, будущее! Apple всегда мечтали сделать iPhone одним большим дисплеем. Настолько впечатляющим дисплеем, чтобы вы забывали о самом физическом устройстве. И настолько умным устройством, чтобы оно реагировало на прикосновение, слово и даже взгляд. iPhone X воплощает мечту в реальность. Это смартфон будущего. Дизайн и дисплей. И только дисплей. Совершенно новый Super Retina с диагональю 5,8 дюйма, который удобно лежит в руке и потрясающе выглядит, — это и есть iPhone X.',
                    outletCount: 0,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/q0m8FUrO_yjq85GKbn3IXg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=zVtsgmjYOR_IeMqQJC9IvkHbIAyQNBgGNYIpn-urafGJz8jq5IEc89ObFqjqVZTp7I_BXFjRcuT9RHNh_VWOECrxQUBkPrtahAuZ2PXIX3lxO1-fNOEhmV-ExbzvLqBD&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZEnSdCCUDL_ajYCHm62IcImtHiBjzZPiu7gdGYzrqppHkzs5cWO5ljUEw9qygBvn9ZrnJvA6ddhbddJXbYk2IvYoaJmXeuUqgvt96_fxQFzm8E8cVy6lK6QkIO8Zhi9pXfHp8ZIIMpsD4Xa1qsDDojeUk4IgAUHSCp2wAXf9bkGr1fYTmNjTHewvSdTGSn-vKYaFxXX5KeerkhNVQ1dluMDFb2ZlY-R8toODdJ5vpBI5L1wo15dCqvJKjCVqb1awSec_Ddo5mDTQECJ5YYbOBJl2wVeD_iF18s',
                    wareMd5: 'nlZIEPv-lsjvzk2lnLqFEg',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256Gb Space Grey',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8maOF5dEvyzqcm60yUQqcvbMPdwFMT-gOqS_0aVeoeg_eFcyh4noZ-FCRZUe3aW73Rq0pjSXfwWJQlJxpUmqsWhhpHJ_sxDZBLhcH76jWQBgN4_-GmrBtCaEdQ-R5Rzil3Sqm76SgAlrbrvj_Gg-fwETpxgH-E0D8RswF7NjgraHts1VvZ-R79dte9DBz3xdaQMAhlMjumwD1uIQxhWV-r6tS6d3UrjKbIdUNhdktquAoEDHBaC0R0SIirWXrKtDqrafcCIlleO6dIf2pTwo11fcEFx4tkxzZ89m-bzmq7P53o6XV6E78rgkKXWBYYpLSoj5_L0Y6B1E1CFfMaYuHEd-OLQCG-gHuJDSRblnTC9CmSqDJVUOe-j_nkoKf4sWYM0_qGIi0hiJg82ntfNuptUIpYL6lqciosP3ZB59M_s0U0Yf09gY1SdPWI8QKpA5sL5tTyjC1WktceF_IB7FdAXmys9tCEH15s092mz2DSZeGtDhZsLa93pFxkQefxF-iDqjdfqs_2IzkLaD6pPbUpMmiVK5sekA_S75DruWWXd2c7q8HZWfC4puLd9ax0KgEpsbfzY-yWXO5sNNmHeXjG4wwuaVe9C_o_nK7T-yIxwhHsrpLrf4sqELm6PrOscwJmXap7mFw9w-Z5Ay78sBumeoXjvFZqgFXnlQfAyrVl7WXUtJihfTjuT9UKQFqFomVY-M73hX4Cc8CIOFRmfpwh0iTSQcFDaVcePRwk-tD5yGj1V24VyKX8s2l4fdQ8e4KZLTEmbMjSxtTfRzM9EuNf9K1J8iJKaHS_x_c3YvoQM3QKQOXTH1DvN6EgwSnjsALgMzcJFIZlZYKrxMbTXzR-I9zkNS-p9CDRbP8qy9p7UsegvtUXWi3BJs,?data=QVyKqSPyGQwwaFPWqjjgNq31P5uLbHikJZEo5WiNoG2mS6qG_3OF35JHS79IeerLCuls9ZBZuu26PWQBanieLDTVOzXKkUNlDLCu_qjcfxPM_2C6Qc3dl1DJNWr37Q23bccBe3lV9NZNXWrP9yIrV9xw7mbxba8M_wUSzA8chHbPaOPQ8soeRIvv67C3Be_GZOw6SvCtE3hzqx5_PLL2XhFZMwLhWO9AcWUnChpheM79QK88EalkLFMpEOE8ozyynvPJAc2PiJzsjjh3oNILoOLH5RdMzNYIqePXXaWIRS7dQYPDsQVe4PmNohAorcPt2USFdbafpR8bhtR4GQGmoRd9FoMH1A1Lnkgpd_RRVxSD8-T0kn26pFPjewPXmegYZklcLv_TpHse0tNdXf5LuMRmyEvkkzgMU2UyfFSS6IEN6aOe1TuXIPfTfi3uhNo_&b64e=1&sign=50c1cc5bb5a4ebc9fc2803c82c6a81e5&keyno=1',
                    price: {
                        value: '91990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR',
                        discount: '6',
                        base: '97990'
                    },
                    shopInfo: {
                        id: 350233,
                        name: 'КЕЙ (Москва)',
                        shopName: 'КЕЙ (Москва)',
                        url: 'key.ru',
                        status: 'actual',
                        rating: 4,
                        gradeTotal: 2377,
                        regionId: 213,
                        createdAt: '2016-03-31',
                        returnDeliveryAddress: 'Санкт-Петербург, проспект Энгельса, дом 124, корпус к1А, ТРК «Вояж», супермаркетов цифровой техники «КЕЙ», 194356'
                    },
                    phone: {
                        number: '8 800 5005 074',
                        sanitizedNumber: '88005005074',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8maOF5dEvyzqcm60yUQqcvbMPdwFMT-gOqS_0aVeoeg_eFcyh4noZ-FCRZUe3aW73Rq0pjSXfwWJQlJxpUmqsWhhpHJ_sxDZBLhcH76jWQBgN4_-GmrBtCaEdQ-R5Rzil3Sqm76SgAlrbrvj_Gg-fwETpxgH-E0D8RswF7NjgraHts1VvZ-R79dte9DBz3xdaQErPgj4TCIfwM1_7M4LpNmCxIGlWV7-EZ4HCZAvEy9EwIrZA57DjYzoOeUA-ERlBXzbQB0sb2lNlHjXekH9k59O7Czd6n0wK1RbDbs6h7WrfsTURhB_0isG47oluCxEQX27MDC_7XaB7Qln5M42Hw0R_mZTEOh-8U1o_m4W2kJ_6Y8B3-KuU742OF6n8HWzOE0DEcowsZFtZcFuVXggDQYmFcEgS5-30FOgpct_dnD-9w3mpRJ_wGXd7ZhgL-mRJYaO5K6wldsYz28q80fE_4c6WRNSv14D3DX-gDHbsx_NaJdUDs5iSBkWzjjX4Xs4pFNk78r3XHArq_8HZ6lc0h8sohYkWpkS2Rlk4UYPnNpf316jlXnnZoAuXA5EYHZPAzOU7z9P1zIvEnkZoIrYXeNkRDovHtCtI_fbFBiSm-CFUq5sOhQNb6dEIjSGnB2hOtP_VwhtUDBLB1dGofFLsCPtUOB5K3UXzxZzOkEXHdNiN29wLm_z6025As0m4Oe1nVnvhgJ9FecIafAWHemYWxJ_vfRzMYLaTNnP5-maH7yvddF_L7bgZ9l2JCOJsbfxzbk1sG5rmXu5KtkrFCQ03HQXkK6GWcf47BFUvcB_dyrkDe42sq7nN00DUnr28NP7eH4A99ln2QkItBDcNR9WV3DYrbKm_05wjl5t8D-THHzQJCQw2wwVzJnI,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_SxHCWSigKW24HBqThhKt4qNZj3x9Ev9L9Oey-kFBK6w9UmXD4e_9uw3wTlwUBwAAWzoThi7CO7rK192aXiEDl_L43UFwoqoq4xxgypXa5HzvYMUHQkYsHV-11ywbn2lJOYmBmjoghTWh5OFC3RDuhlmCVuhfiEPLlR9LNrquQ2A,,&b64e=1&sign=c0fad6bf5d4523c9186b01d1c6d6d398&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '390',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '390',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 3,
                                dayTo: 4,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 390 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_sGVO05BuMFFtx44JAcoCxw/orig',
                            width: 322,
                            height: 470
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_sGVO05BuMFFtx44JAcoCxw/orig',
                        width: 322,
                        height: 470
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/168221/market_sGVO05BuMFFtx44JAcoCxw/190x250',
                            width: 171,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '800',
                            number: '500-5074'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '16:00'
                            },
                            {
                                workingDaysFrom: '7',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '14:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.529268',
                            latitude: '55.602983'
                        },
                        pointId: '2811925',
                        pointName: 'КЕЙ',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'ул. Тарусская',
                        premiseNumber: '18',
                        shopId: 350233,
                        block: '1'
                    },
                    warranty: 1,
                    description: 'Новый Apple iPhone X 256Gb, 5,8 дюймов Super Retina HD (2436x1125) IPS, процессор Apple A11 Bionic, RAM 3072Мб, LTE, GPS/ГЛОНАСС, NFC, Bluetooth, Wi-Fi 802.11a/b/g/n/ac, двойная камера 12Мпикс , Touch ID, iOS 11, серый, MQAF2RU/A',
                    outletCount: 81,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/nlZIEPv-lsjvzk2lnLqFEg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOhqhgwxExawrWeucFqvSK0b2S9GLfkXaHn4aCefSaILMiRuNbAi0Gy_vQrqlv_WDst2KZ8l0KFUohtHcWgDH3oLY5QLC4yL-9KySJTFHozTdBEi3rrbEvqDNKtK3GQGcEQ%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgZhi0eeiI3FW7SsJvXtDwg9hCb1uh1ltCzSpLnwn7OZBwzQ4rRw_93td2XvlxjvDAUIpriv8ebd2vLR1eoEMramSjXCCxoWHmCYzw9IK3UD57PPIYVGqIeuZnm86EZuh6Ath7reVC0kdK8VqK4lYJRgwZtYLnGhHQKmey5gRHPsFjWsVN3rryzxnZaXjB6rhCjBL3_FmMe2WGifWzvlQg_3QwGHl24HHb4V8CXMo1YJieFNIwNrUc4ub70wzIv28rHNmr4Ifzl5yLvmMAG7WohSU0wQrC4-d09Y4x5gssMbiBo_-6pkSGQRNw3if_C-hGEs79ZhjoWQ_6mO9gRKLmROyCHutR_H8tCmmxlgF2v-88HyitnsQyDE3XGrpDRHEYLQzQ5PZZRtP8_cRX4k3ngoB7XrWpRkPHJ-o8Oolear7SY2kBBo23jxaxWsIchIGtvcqm2BLO2-v4uk4GDaLtR9JEfrzAXpZS6jvdupi90yGgaXHaj8PtRyBg2uD4pxjJGJrzfcDCQIglv5PDoWkNoHdIKvhrC8F3UpSqBdYs4frqOwtFq5ParR7WFtRIu11c5uS610m7oQreVLEX6LlcZ-ws1y7Kaf3ClGjh_-tAjp7mAuR6U9q-p3nvKLfqHlUvd1of4aza2CkKHEKdWPVS_A8TYOg1VQVT-VAoUVEj7NeA6SY2yPFGyQ-IpBgyZe-pkJy7Z5kbCqUWXh1XnQOv-CP70ciS7G6uuGHSqdcg5ZFvMUsZaxzg2V9U-wgjjQmK-ZVGLdvNIJwSstzICXBbUVfUmbjJa8QAr8IFUdseUFX1ud8mcCMN1uDNRpbbvDI7XeC4bJQFQ1Gs-gSdnmIrssB6cIxtchjZuO2_PBkZl8aNYGFtA6P4jTVNtfLDYLLv9cElxNoBt_V?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-7F252SdQ2Jq06uRldzoYAD5MQ0aUM5GS814oYvKPqxxsDPYSXnehOG7vJ-8T8QxEAC-84M8VJQvhHbSeFoqWpqPaa9dDxpqDYSOV2YfhMm_rPxhzWvaGeVZFcVwn694-bIcZz6xigdoVDn2KB7R4iyVZevtkCP2zCwOY4bZqkZRuSQt-2FmGFoYWq5No-bmxSdIBE0RdfUXyNoVuFTYVlqrAp-DpGjzFD3C1bJuTos7bLqYB7do599WC8fd4ZPNOFe5W-HN7XYJ19E_cjT4yIrB9Kg5nYU4HwtNVxAlQuU2ahjIu0AUa7Ck4hWkA0a_ISYLtjYBzmqKA,,&b64e=1&sign=842b16a637c598ad0af92be57fce28e9&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZHEZgnT0_-U4JcgkWR5sjIWseb7U4ZFKrVqtHrjwfcdG7L9K-u2BF3W5l422eOby_sT1qIjGkGH6C8StmkpfyjridOzTa608dKy_Fymia-_wruWSo_NZqiL1jS_cI2UveBfXKH3g6oU5w0gm96BqaOnkuJ18L1ODmFtUzgW2DPIW72VcQ4k64XXvGXHGGKskyYXK4oTMq_UsCxIBtDemOvzeX0q2oiq9KirHvKsLkTjkIDBVAp7RZrQLikT515lSbfqxFHQmbhuLrCKH7xnkUVdbAqTMGY1Au8',
                    wareMd5: 'CxxX-wIr_o8ZVw2yqtdVJw',
                    modelId: 1732210983,
                    name: 'Сотовый телефон Apple iPhone X 256Gb (MQAG2RU/A) Silver',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcCk8AQueBRfIRDfFxsk6v9edN5tnFfawHppBcKOROpxSNbiJSlDWB8yDAzatrDu8sEKwLh0hGWKE6k8xsyOzXlayBV1Nd6GsRtH873TqkRrY803WDfBeZFm10s05dY0Z9Yf_nMc2Rb7XP8D0NSzCuSKy9LYVF6e8L02wi9z_93DrQnLIrOg81ULC5bxXQM2fm-ssdFVkTNJcIR4v7QjCERl05YCSfFwLOA4PQhAR8NH3rX8BG9RJSLFj52TdEsl6OzM_Cmo44v2RIoX_WwpvMeLmVhos-cmO7KfH_0BhVDwdfdfesnzwJD55h7SlXTpOFi9js7pZe44_P-WAVdR-ocfkYv_QQnPBVzK13Mbj1MCGpag7Zri5SrJVUuh8fyD04wr3NBTVV33cK4o_uCZK-BX89AOmOJ8rIgzbSry7DCA05Fxaf6EoTeyY_2b1eGPlAf89fCr2qjufSDJOXGDqu4XMwKo_rEPmqmUh0CkawyyxlBARNLPpRfLVMC0IgcgYSgUKmm6rQ_TshDRNJZIfGo2x5v70ckfhejUKpPBW8-LoC-t8rSXaEPxx_fcUVLWmh8dZqyP0MygbVduYEwX5ZTKGMQCmYVbCdcJgK9tL6QzZHJDxciHPxvA8LP9vVaPZU7Tjyr61VlWRhhkn4U72U0bXV6_itlLaAAizz68UQlf0tXVB4peISidJMB5wtPrtWZ6xhhEK-cSaFvlNYvZi6B_5YOUBQwO8TVLisv9iFRNMVysPw-NdlFkNhQpOsa68Qr2y43jzQ8xu_uFF2LPIVOyu-FUwNHVULVI1TGUpbHv8EjTQUa3eJP_CsMpCIxTC0TKkIA1I6HYDedv7BwxaUvRP8foRWiK6MjL0K3fKOPU?data=QVyKqSPyGQwNvdoowNEPjepPkxt1Vqcos9cWvPI22tg0wCjd75HgObocn-VkR-9ty9kgea7JlqOTwblPNbhRmVs8VQhcci9sr6QI-_si1Acq1alZCohR7B5hj6M2P47E2AaxeFQ7h5X3QbJRmPVJusK84mPYxTMR_rbR5OmnGd0csD9cyQXfyi9qLcg3O6Z5Ho2C1vJIQlmH6cdBjaPig2JImS1TuvWwjpJamyYDuz52s_u6qiq3HZ8jIO1ORXfHK7CC4rgrOXhVjQYVRek78RafDQb8SB6JUffULMz7AgHFQex8chk942i6Is_JMihKdUo8v84L4Egpx8dJ4FNoSlHI684aG9rPuTXKq3N9TLckPbGFRmfIDA8EsCzWyQfbxZb7nMMjbvN5Bn8UHZ8D18NFIMQeYoZVy4dkMXw9CpMteg1zoF3ipg,,&b64e=1&sign=6e2108d4f098606e5ed71b02fbd77f16&keyno=1',
                    price: {
                        value: '84899',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 6537,
                        name: 'БОЛТУН.РУ',
                        shopName: 'БОЛТУН.РУ',
                        url: 'boltyn.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 6058,
                        regionId: 213,
                        createdAt: '2008-03-14',
                        returnDeliveryAddress: 'Москва, Марксистская, дом 3, строение 1, офис 413, 109147'
                    },
                    phone: {
                        number: '+7(495)545-4227',
                        sanitizedNumber: '+74955454227',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mcCk8AQueBRfIRDfFxsk6v9edN5tnFfawHppBcKOROpxSNbiJSlDWB8yDAzatrDu8sEKwLh0hGWKE6k8xsyOzXlayBV1Nd6GsRtH873TqkRrY803WDfBeZFm10s05dY0Z9Yf_nMc2Rb7XP8D0NSzCuSKy9LYVF6e8L02wi9z_93DrQnLIrOg81ULC5bxXQM2fhUwcE-d0pxiEpWfFE7pVkpLIy_OklxwJsrbH9q2V7W6MDLO5wQ2bB6dcDJwBbCZ4irBI2DOad1brIJ2NtcxXe8YoO6NVoFnYd00LR8RxWTVRYTA3RHQFDYmL6V-9RrDMCR51UGcPWJ8aoPTMMbDl5xjqvK5za07FzWj7m-POjjxRtbVIRRH-ADk6ZMfLk9VVaqd9OAXaZULwdJQ3H2J-pbbR-zKSL3pyj36YgQIXIs8EI-YvZYUlM-0l3q3xxMjHkJtXkLj4Nl2ecEw1d9HBsKN_fVFAwd3mmJuFZN9cE51NQRt2vFaDeMzgTD1AHgS9_IqG0AM4tp5NLViBuv2iUfw5hpySiVQcpfopFLkV5IQF5GrGKZnSwxXv47dV0L0Pr6XozNwE1QZLtir3MSzlm87WFKsQyRNdJoRBC06vGJlx4TeONA1lqPnPsC2F1EAb8KY_Wy68ejowlPKaH1lFhBMf08lgaYnnM6I8UUVR2VJOUnFAhNxgEWv-vwRbXX1z1ZaMl3i7LFFxuaWSjmPZY9U3JmzwFmi4w8t_s4RPBDQlsAdneVWBsdpXGp42UQhwb8g3nDyFyB6Hukg1UUH0jLV-gE-5sD1Hw9pg6yTKooQOvSI8P0fXa6wEJBWUkgalYxg3f8axogtbSrUul-2Mzl54taTxCOoO_WzrX26Cy6r?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-APBr5xIEb2S5xWxbAQ7HtHDeypUPBhs_QBDOVdr8_5qwZ4o7gYr2ZTKXs9_xQfh9OI_sQpe-wt-qFAIL1N2c9Jf0YF0B_fH9mqGaoyjswJxzvauWPvaiEpfaa8r45u0PkXmQVAZTHoqvGUI8i2FILgLTAAR_QJyb-qnQs-WKWGg,,&b64e=1&sign=d4b4c42240b96ceb6e29c28f696c997d&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '399',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '399',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 3,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 399 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_yY7mCkEmZQVR93kiGcfBbw/orig',
                            width: 397,
                            height: 572
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_yY7mCkEmZQVR93kiGcfBbw/orig',
                        width: 397,
                        height: 572
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/175315/market_yY7mCkEmZQVR93kiGcfBbw/190x250',
                            width: 173,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '910',
                            number: '000-5825'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.59796746',
                            latitude: '55.78306774'
                        },
                        pointId: '317341',
                        pointName: 'Самовывоз М Новослободская',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Новослободская',
                        premiseNumber: '26',
                        shopId: 6537,
                        building: '1'
                    },
                    warranty: 1,
                    description: 'iOS 11 Тип корпуса классический Материал корпуса стекло Конструкция водозащита Тип SIM-карты nano SIM Количество SIM-карт 1 Вес 174 г Размеры (ШxВxТ) 70.9x143.6x7.7 мм Экран Тип экрана цветной OLED, сенсорный Тип сенсорного экрана мультитач, емкостный Диагональ 5.8 дюйм. Сила нажатия на экран есть Размер изображения 2436x1125 Число пикселей на дюйм (PPI) 463 Автоматический поворот экрана есть Мультимедийные возможности Тыловая фотокамера двойная 12/12 МП Фотовспышка тыльная, светодиодная Функции тыловой',
                    outletCount: 5,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/CxxX-wIr_o8ZVw2yqtdVJw?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=HeeUzl4nRjk3p6NBKKic5kHsknttJ1m3YhKZwqY4bccwVUgs64g7KRqg9QcinUf5h_s3eMQmnwhKzy8tQ-XlGY3imESsoUFuyGMepSYp7NSz-bT5UeNB-F0oif5kSzqBq06SmklG5MU%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cwrsJ7QqN8hEcGh-ZCb9r6jl7LyO6EiefZOF-Ne-BwBj4C8pmtrCVhjJarPGej1I3vTBjkNSua3RGATOAeLwzNYA8K9ewE04dJh0hh_wIcgVWFvdSXi7Q31sS_sIULt5zWiSPnk91b6BPqWHW1p-egcw7KAEw_UKScZFxgIc1AA4WCyTW5gneVXJfP--GEO1217wnSfrjJHlh65gJ91xr92m6g24taBOCwk6Qiu2ZEzN_FB5IcQdCes70-2iVcr5tVk2jwYi5SPA4T-O_bacliKB4lGJIzAoAylx1XQ5RPnOeDB9SmbeVuN3DHDQmSY8lVgp5Zr6H-mRARiTGpu2YSD2U04hUFshOlpFwjEgBfdiIc3n7iahQTSjphKw__fsizHFL_L0x27L5NCsZkugS6XCYFh-uq374_XV2D28FLKdpbb-9kiDMpEpI0qRbpB_7VJDvaOOtmBZs1bGCoJV5qmAL6Ch1gZiHx68-lfpychNKfmVJyFR8dW1jQRNdJtBRmyEJ5gdsC1ZEopN2K0DTrCbRICaepVB9LuFGgisAoJwzABMgvfoN6tae2mrdQvi8iTRbQBIwPA_Gz_dQ9_z008sYye9eMk4qf6_B1t69iccDuVjYC4sViagoMqqck-bKWCQhVZvin2BEnq79Q5r2EUUfcOrD0tbOcoSzE6HM66vA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXpA30pNt7jplGKl3g8HMbB09LSHqok6QyxC4nKMm5NBFdk5M_GX-gWbGtLcWT5wErIjo-VCpzIjC_ahiIzgXebYYViwEVPJZQzlNaW7OFMIS9pmbepYQfDRiaNQY-M60tvczYLqeW0fJ4QSWPYN0CixVByuu0XdmxfXVLM7yRRJkJ3pWac72tPxQ4R7ULEBON3gpVfxvvi9EqqhQBL74YZtKSBXRUl8GuZbRpXFfP9hxKhiDOctqKtNLpXzWUBViGsBsnYHaw6D8OzdrZcR8GbIwLNSf1E-aHEjkz1RWt8SEUy12w10fbgw,,&b64e=1&sign=57364abed20511e6ee504e09ec533e52&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZHM00iXZ0US0IkMx2cX3zczfpYCQTjEdhtx1T4YR2EVFNj0tRltVQZPkOyjMADTxfMT-fr1s3vj5YFMwgebBejUvVgJyf7frA5eZx0238t0wQqRIqB85dbCK5aBJNjlP0RF1ooLZERGfFVZ26_L_hfHxy_M58YfC76IrMvsKutugSNgKuxwD1sl89nH79YWMYebhlxzWz-J1r_xkxwNadFxUHBsLkB9T-n3mAxakAT7psPHau90JlIeA1MeGZqbI28r6kFuZcV3CdFofoaBX3EgLWACzCOKCno',
                    wareMd5: 'e_aznLMlC-RJFd1KgFIDRg',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256 Gb Silver (серебристый)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZihEDHmDjHFAwN7X1qt-YSnbjsiGBwVLCTJefDyXDyQpshR_nEs0BovM5F2J-4EFbWDDSCqnJ1rpMsGJiQHfSpKwZ3QODtJ6-bNiX_HMo5vd5vC31EueUvtMOVgBQJTz3-haY96Wy9LCzA-FKpwRLKnX4hsSKNAxfRdHSJsNBWYZYmcZ5QaR77jO5quBmg3QcYswLQKXNzY45JJolKZLWwHEIsLM_GTHfZD55_x7CsBoA0uGkEUgbiXvLydMKb5ZWg9poCSnxpl68VbgvifDW_QPnMX7hoBSSmEnr-AO9L4UeXHlmR1LDHxwqKjp8NUcIf8sJ8jt0RGU3cefm52fHJU0E37rp3FUBGs_MXf4iVfUB5uVt8IqTf1R7JC1pr2_2cCnoXq6x_gqeX_9VjHsqwTWMoEj5FlJj2hTKo9kBgpa6TJhsHxbcNjV228XMrUQBKtaT4mt-yC6D_8R_mp3SMZMb0ZuVPtoTpN4SPQavuIHYcvx_5Im2Pm8o1IIaSAYp0iPlOx5_PvVOWkNxdp0H0DbPuC-_thNmAZu6ARDi9IfvbSnj9a-_yeZXahbZFFw-SsEFvFQscNF3MJjkUifJSOe_0fH1X0YSWMWOnnvhBjXfNLEomUTcRlRkoQCG6z7LIehpeDcXZHMeXi0eLqHV_PrD44-6xHikKmcExajcm8__NV9iq4uVTSVi5QGQaeiz-oL6lOHAmF_MlwJ9gsds3XJGiSKUhq5MhKWNr9gyFmj0LfhTMCLLGLVbx1zJn5X0MhF7I3TAdfqWCK4vkm5ZWq2TRh2lLxUgX9_v4nVKW9xiXTfcwFG46NRSfTifWpOcoJM2a1DbwdEw9omP8HhOP-TZtmmhmFOiRbc8cgVusL?data=QVyKqSPyGQwNvdoowNEPjcgkQeHnFjAO2XvzPVK5SY1taZEKypYQ8j3_0UTF6PWJrtNCIAoypW2yyf4XecpkedP3QfgyxiyEB1Q6vCiaUJC9VMaeoSUuj_YlwLvfNO8Fj0PhtEvZvPqa0PBum9H4c4gZnQjaLNvmmTqST2liS5R962Yfx24TK-fFApg2NH6oTLcAvnFaTikqlqTUv8Drf7BL8tjzOdTiP5WKH7qIAXYHhRtadpx_68tteWFwKh5De4FYMus_2FNgwouXdbfjfevz-LatXWdz&b64e=1&sign=0431ac8959db1088be596f011d4a87e5&keyno=1',
                    price: {
                        value: '83900',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 397551,
                        name: 'HEY APPLE',
                        shopName: 'HEY APPLE',
                        url: 'heyapple.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 44,
                        regionId: 213,
                        createdAt: '2017-01-14',
                        returnDeliveryAddress: 'Москва, Москва, ул. Барклая, дом 8, ТРЦ. Горбушка. 3 этаж, офис 336., 121087'
                    },
                    phone: {
                        number: '+7495 205-32-28',
                        sanitizedNumber: '+74952053228',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZihEDHmDjHFAwN7X1qt-YSnbjsiGBwVLCTJefDyXDyQpshR_nEs0BovM5F2J-4EFbWDDSCqnJ1rpMsGJiQHfSpKwZ3QODtJ6-bNiX_HMo5vd5vC31EueUvtMOVgBQJTz3-haY96Wy9LCzA-FKpwRLKnX4hsSKNAxfRdHSJsNBWYZYmcZ5QaR775I20O_jUhXRJrEeimH1YVGM5vrOluEgGuKr9PA1MOkO-x0f5scVUwpKn7BBFNO7MUT3ejqn5YrbZBMqjIpBC6QwNoCxHmNh47YVa3aCtwB5NcFBKXvPajNENbksUK1C4aCxTT7CQTc1kFvrMiztTJvLJrc1ucxZ_dJHMWUwONvCNGC-l1HPLdg0HnGk4ywTfc_HIhb37E-n6ggnNGhfPvepDYjEOqnD5fkJakLuU6CWNYwFnv6T7WTkMlLBXLyGgbF4lzsOEWmEVUK3apObnGanC3OvqcSxyUcISXnYOulWJIyaB7RaPo7NfoRL7DdHRRR55k37HtPVC9ZMtO49ikIhjXETUsuR_14Q3cRkjr8_ZreLUs2VSvIg32ZtjaH8DhRLknZnbpDelu3IaEHNExT1b1x2R6qmEO1pke9QsNa2JUeMJ3iQHQHzOrOWxB63qrlIvGDPdR6WO2vaPZPQ1jc3CTgqLWk17iKYTypaGocg7BmrXssqoQgM03a5zcryJWX3JBwQRDVIrpymz80E7wFUfzVE3KzLLdY8VOLShbjT7pW3Z6-lMXjW0brQs78fmZQdmlEWwCRZaJgbz4m693HIw7pzSw7njuGG5NxT5MEt6QErVbf9k90Wu08FzHZqkShlvMl1VtVmER9FEdvbvgAzyn3m2yogZy-JL2Ls0zWNJ7V0S25AbJ?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_N6An_PhzfD1Ud64-pFj7MUudVy1zbm7TVG7UPaG56g7B3LC4nR3F8EilRJZipjYdtDsU24c054ZvLGVEssNeJsOY2KHB-h4NppOSpP26b6afljLLg3t9M9oLre_1FBfdL25O9acQTi2mjDxnHZgfzBLqQJj3abtxepsEVS-b0Eg,,&b64e=1&sign=6cb6533234124eb5513e6bbaa2b177ca&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '390',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '390',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 0,
                                orderBefore: '18',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 390 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/226983/market_KqMa25ZHEzIzuRJ8ZxgxAQ/orig',
                            width: 528,
                            height: 608
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/226983/market_KqMa25ZHEzIzuRJ8ZxgxAQ/orig',
                        width: 528,
                        height: 608
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/226983/market_KqMa25ZHEzIzuRJ8ZxgxAQ/200x200',
                            width: 173,
                            height: 200
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '205-3228'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.502786',
                            latitude: '55.741135'
                        },
                        pointId: '2591567',
                        pointName: 'Hey Apple',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Барклая',
                        premiseNumber: '8',
                        shopId: 397551
                    },
                    warranty: 1,
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/e_aznLMlC-RJFd1KgFIDRg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOjV1LJaschrJwbK8M9xSM9ojuAaz6eCSVHK12Rz0_W0b-u-xH0-9eRASoGH2PeD84A4V8WIsSkohlGpwfdYiu48bfV30QnACudYuJpOLw2oXwgxdVUI8qh3NVYsEvbwyIQ%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgQtnjI2BDeozfm2uzIgi172UNsIjdZvslfxQblucMsEYORjHHKjnXU68c4oX2c4t0ZKynTs818CBWr4nNwWl5wm7UelgrYmnAgwhHCcx_oz8S7P99Tyst3BGD4EiAubSIThyoZyFURqLufCdEG8kCEiCxmJ-4oaZm7L7UgZ4VcoVDK1aQqGDBVpZhv6W8bOd-dEgrRSjieePxcYWk6BeYvsZYf7_2_chqhCiYuPrgPrZvGtayO9qN5PvvM3cHKed1gH6n6R4UTAfVAxmUwNRs21CErgJO7dLELzoqQY7pler3otWzM_BAjt_w-MKHGPVdb2y35AVdbMowgacokQwaYFJhrBuMaL3yect1VDkoqQj5WdN-2geMRujeVbV8bliTV8l-Boa9X6t9KQYqO_GA0QBQb6bCWrEwE-403QsBOsd5nQQ9ADoqhHTFYXKhPXH9OvQSDxQSHSf9nWI-IzjFTSHiEZIKcLFBklSA-vZ-PLMzvSA2896gIjCzCxqWpbEngp22GR_QiHn-cdXD4z89bZTolTjllwGesQBOWrS8Sq7O0NoY-bDQioB2HkuVH9IOh46ERsd40ur6Qbb2Y1wN-1NlMWWJD6q8euDN8-07zGsxWmKtEI7k04IYj_rftSLFyM44VTULlU83SWJlkKOGEJc2yTgU1Ib42wjGNSxcJvk8OUg6qBwVlUwdXSlAiVnfULVUFhxTng1cTPPOHZH2USgF75CFcsWIpEJsi8-eUmb-L-HG5iW3nz0LmbHc4mq1k9bnTnPZjzuSRo_ACk8kXiFqj5ER1neYicb2v-Px5SJAWlKQk1COyz5xiGypnFiUS0aAMQdV1cKTQembDkCSqcUKLbNheD3w0ChFt7WbU8-poGsp0aknpHE51-VBw_8uGo8mGNPbxpY?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-5N3-ZUI4cYPzJES-pN-0toVI0jGX7B_bssvo91CeaiRNdxHg_qIbEmrF_JqhFlFS4eqqpIURoFzfrr-79-x5_g9IDrXLIyRvFsxQ-S4HQ7xPp2aBFMso_7Wl1C-Zy37frJvdshanigYUmu8JUyS8ZQJ_mbznn0L9SB3mKWrp01blQ8xI1HQsifH2V8Z3RbNp0U3wjqRuovUwg4_OyCnqcRKA0fuqnkvFcZlRJ32R2sEnU11gxkjCOIlWSMuf4F5b_KFj75_OwL5i_VUTM9RsnRXJQqBXjiep2YKknGJI5HDLOLfI4YxBpXjqCjtAAxLt1TSYNJt7LIEw,,&b64e=1&sign=e9d8a8b53080d17466aa4c0763401d85&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZHLuZWNcw2e0r3J2pAhzuxHii4yNzRhYDBoZnqvvH5b_kXTVRtXxw05sOZGV02SFIfMAN8oQbmBYvIg78OI3gaW8rRq6G2MWEM2DNO0q6zP6MwdjI24h21KdeJG88svoiAen3Wnkgb12g1Lon9HaB8rpeJu2XrbpMqe54PpVpnLr4S82CCqa13QoxpElH7l46QNSDQowGIp3JE1so-LzrhNREJm_9ugtp0ZcP8SpbLnQue_GutN3hg5YKInKc-9hN-KMSHktUNNHSXLnLMNWv5KSbyFztbObZ8',
                    wareMd5: 'NYcIyNF_h5bVnTMj5LFTwQ',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256GB Space Grey',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4cbFtLcv3ob-UPwmIpyQ-U_PpuiiLvm_BEenQ44PYlKM6NL53Xu42DAeGwZpQUqymd5XeQHLIOE4U1B8ZqdbLHAMenP0gzHB2OCmzjnLeFo7YBhPL84o8ejaUfCz7vflDYleSeel2Dn_9F9QOvA9l-MsZlvEVnJWz6_E7V9Ym3QvYtjm9Uyyo-QMWt9ziVAgiUj-Zn3QkLIe2EE84abs7ToonPP3_BKf3uttt0DiGdzDwuH8msBGUjyBHww7EPUdvwEvxtMgfHWQS7LQPJ04aXydDGymxhqUu7bKinIwpohJtfMJRYdBXZivyZubggXikEm2vhJR7Ojsb478F1mvga9muCPMNtFIaDUNqeQwYLTUiVlPPPIxipc5YzLZuO-XgZwH0CsXYchfI610ns6cx6o-u6khWRz-LVcAQRxS7Ql8-M64ENEhp92K1SCcUuqbtmrQQelcnIT1E3oR9Y1b_kFAI-2K2yNUJ0wL1hW0IiZDZGQx-qZ7SUoJ4mdf4pFkNse0dF7-uhLiNrJgNFXrvEIvjSomoYH5tzN040WdzXw1ywJxCSKMUVZZwwh_cV_o3VXGFRIS6LNwp0ONNBF364ryXVkFaP_7p-yiiRK_dYVRbxUm3Ubx3acBoXyjHFuNNJWa35qYSHg5wdTCOpgoeG2ilHaytyLB90Ae8Ot-mi072_SAlQruDvw?data=QVyKqSPyGQwNvdoowNEPja0uQkWSxAKizEBbSXMNJ77r-RoTFYQC7lKBJrXfHL3BGSFR9X5idu36MKbWA9lGBstGOO2CDDTni6d9tLnhb5uEumLOkQu_uZbFk9ne50oRForgaUJruk813alI-ud0ltvgm2L7AgZ8tsbXM9RKBaQ9P9-R3Df7-JBOXlcnx0iJnczl9MN9BvGYL5hEp9JNPSsm7-y0bX84DZSAKpfNNuj_tqNjpqg1vqGG7IXRmmUkhDuYklblFjCel8C0OOdfoxCc5j7ek3XEqU1_8edCbp6V2kPtTtx0OZZKM6MjlrmrrmC75SJSSPZ5n439H7L13M8Nbp6lbMQ5e_eKo5oa60B7yOVvyXwgDH-u8ePX2uEPTaEcJkurQ5Z82SiEQXlBDp332QFao0J_nBGCaaf1pwyoVD7ZF7Yl4ePnGg88yQT_&b64e=1&sign=821782125fd476ccba5de266451d6918&keyno=1',
                    price: {
                        value: '87000',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 178623,
                        name: 'Club-Phone',
                        shopName: 'Club-Phone',
                        url: 'club-phone.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 941,
                        regionId: 213,
                        createdAt: '2013-09-10',
                        returnDeliveryAddress: 'Москва, ул. Сущевский Вал, дом 5, строение 20, (ТЦ Савеловский, здание \'СпортМастер\'), 2-й этаж (балкон) павильон N31, 127018'
                    },
                    phone: {
                        number: '+7(495) 789-03-40',
                        sanitizedNumber: '+74957890340',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4e_tb7h4VQnaQk_H5CZgHk2gUQ1KJJXFCtQqhIXPJGjre5peI0IX7vvV1cZwAz5D_TUBdgNHlSUoa6qLQWNAPKfbwC-ms1i37s19wVxVJrb2XI2nJke_RS-mWQpLkL2zFxnvOsyBCJVHtwayaS9kTq09XWNyRxhFhEo3m-8Mo3liS5DnuzxtQ_OKS2ip3wc1bg0U30N7S5n6ZdWykfXDBBx970C5OZQ9FL2WhKCtGA4Zv5F20SUQ6zRjFpm7TeKVYJMVd7-Fm_3gr0tNTQGhLxDADKauHzYH_2TuE1oJ6DDVD9iFgAwi9huvnb6urWWv3O9gVnQchJvKwBA4aRVSG04x12J9BCn_cqVNqYIQGaZljaEeLOjtEznoFesOqdIxmSwaWfB3Y33S2DfRRMZve39RekiZtOsniNpWZRMKXTPJhGLHn5wwatrfpy-96F2G4oXViRatJJFSYcSPE8okFHiXQIfTGZCOc3wVtL48RiC35ry9wmPqI_xvgEibq070-WL1OOu5EEz1yB9QRcSYoZ0sYy9bNQbZuLdqHoFJrXv6OBS_XCO_v31rEKLlClI8eLC6NoxCWuRZdZuXaMbYUddyLFf2bJzy2vTFqj3ma6nzKeTRYiJic-PlbYc8TeF8oH3WIMvNvbpvm8oCgseYvOohqayI1CSF9CwC9zx3XYQMssHqCrO4oAW?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8OXtjmehSCEucWxsXt66GSxmNYk566OjtHMX32ANdK6X_m7Sf-TXZWiYST-tU1TWcExekXoLKxcptvYZ6kgoj6zsOOeL4F3Q3BNbode8-HiK42gyT6RUvhHWUrEItPJRw9umzSRHQFz5f-nPEq5xpLrhnKwDWtbPhTq0Ljl8TUWg,,&b64e=1&sign=302095e7e0c0e513110e0e7d3f394b1e&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '360',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '360',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 360 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/202381/market_1W3YgDIQmAVbY1SxernagA/orig',
                            width: 200,
                            height: 133
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/202381/market_1W3YgDIQmAVbY1SxernagA/orig',
                        width: 200,
                        height: 133
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/202381/market_1W3YgDIQmAVbY1SxernagA/190x250',
                            width: 190,
                            height: 126
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '789-0340'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.59355959',
                            latitude: '55.79388164'
                        },
                        pointId: '339598',
                        pointName: 'club-phone',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'г. Москва, ул. Сущевский Вал д.5, стр.20, павильон N31',
                        premiseNumber: '5',
                        shopId: 178623,
                        building: '20'
                    },
                    warranty: 1,
                    description: 'Операционная система iOS 11, материал корпуса Материал корпуса стекло Конструкция водозащита, тип sim-карты nano SIM, количество sim-карт 1, диагональ 5.8 дюйм., размер изображения 2436x1125, фотокамера двойная 12/12 МП, запись видеороликов есть',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/NYcIyNF_h5bVnTMj5LFTwQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOhQHAsFv35tsejtWcvpeiDoJYNxClKcxs4IKkCx39VA6IGjy1d6llc3MIpTvzt8fCkul2nURZrj2Kx_5S8WP_hl_aUpHVClKl4KMApaGns4xzNvotMKAV799qbSanGQFTc%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cz1er5s4HbEZf-DtJkHCMuwGoLKvnriOGblNEkXThxI8Bv4Nhbch7ChYKpHSMK9Mp99Mz4os05NFWRPNKakJYDHCktfosB1gQ8khN0HV-IMNWeuShsB44xvphIvi9l75gPjAOQT_L3KWwkunffu0DtUh7gGa77Fa2YoYHn5FOIV5El7-bQv9qYRXUcw_RCiQUnBzeZl_VYa15cvA37vrCfnFWgerITq9wbSprx3s2xeLW4uMtEgF3QAvbVxJE_bAIJqn_qAuJlkvFXtyIvO0oVN3BMHYEZtiLL1q7IEM3CE9yqILX-Mxe1hCRcOgKysZ3fcPZbiIaH0UtcgarGMPmOrwgJ9A5Kd-sCJ9xcpBvc20Ck8CZBmAHI35P5T5LwPqSs_W-iDLweNR88rq2tFxncDnSeMgkOEGan4x8eAJT5uuXpzAP_-68HSvznQLSXdsZdHxA6ke5dS5Ehqj2IpJy8HxuuhflLdNVz4N6CYkC-7ggu52-4H098AM5irPTU88Dns-jzrXWaSg8un5ohEtOQppZGruu4AiqhpbulBKphr-wiUqS7GaFRlhMPNpW0zergpclZ-lCTZWOFaOoGXnF2o_xYvLREvc0AcW1WQdV2Pgjaz-dC91b8goBd2tf7-v1_MPGsMUUgEl8d5lO5ualN-e8n6L7jv2P0XMO5trv8mplperxQN69_d?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXmN62vem880Aftoe0LZVR67niGFYHZZ2QwlmtTrz4P9_SEqz2ZOk2LH-KIxjPNyU0aBYcEPJYpOs_LZ0d1UKNy26XkzXajYOH11rQrHsXXAo4UHEb5SyC1s_im_7j8OAs2t5EfMS8_yrb3hgRZo7RKg4oTSIXFlJgw2a8ohY0u4B5t7zat6HYgBRH-HTPfhIBybn-2VyYLSO9Jb5q3feOG2sIBVK8Kd8C1i9jDgaTNzRIVbtFU7Fqur5HDh8f4TxxfgiLzgxtigUr1kqmEqnvjVUW0Wx6oQrmLTAoIVQEt8fVwSquTdScsw,,&b64e=1&sign=027a8413bf3a3e76454ab98946ff62ae&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZEoAZPfM-BUBOzY3T-rWaoXGWayQN7iWROVOspK-EIVhwzCQRYzV7JEHa7ScOEf4R4dqBiI978A1Fhqx357DeiDf0I7XyEp2RyEqdw--rL_ytkfVJlYbbGKn_9pTr0Ve5p7-9M2BgQqYQFYJt920arF-npe7hNf_kFaqx-KC_puZPtha-ZcZ7n2e0zFF8LGt76ieeDWnUFV_tXIINo8zIkveHlBqO2V2cOO91uNImq0_csHzqTeOEDeQdjYEyRs08fpKDIPoP80HRsYB24vIvL8mmhT0z_Mu0o',
                    wareMd5: 'jb30j9qlkwEbkhX-lsf44A',
                    modelId: 1732210983,
                    name: 'MQAF2RU/A Apple iPhone X 256 ГБ серый космос',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYMn97RTPI8QwsPCPoXTkWT4tx9G8VNZXdHo5Bf_cxjoElIAQw_u-3iBCYAWKmKqJQdC2VJg58vOq3GayR-Ms8hBfdkEaA4xOw2O31VCdG8ygDxaevJQYL9Fd8f_Lid365OnAWQZchCIDqo3rYDhbBpI7liheSCAb7iMEle8eTAQYVhfWIaYQvR5OKFz54xZpVetRxf_B2Xkn5CYlvjub79l-kxrpYABnXI_5L-nl__hLMto1XPrf9omQodhpfCgDEkiAuD3pIpBnnJdrNrp06_QZb1INqZDPJC9pS0GAslXyCwvLdpp-Qr-whMWDZi_wAAN9hmANw-acLklLmI3Nj1XhVwVxCcGvYenMiOZo0oKNFARAZJ0gBjb0MtEEfqp9mDzoBiDXv3QC7Z0s0GJMs8MAjevnA8cR15B82iLkv5yS5ILBNO-Wh-oQyFQQ_er1RCwWC8nOoKBUvZcMY9mnKc-2ztImrOJzSTZtqaJhbUNydr-VzvDxvnCFO3wNWrBkAMxm-If0dsliicB42lBk5WNuUV_Xd7-8A2wAHIwiqoeTWEkQo-bP-3cXcw2YVMBZPEqfHj_sthmT1RV0OTMYu6hvSRqUsD4hL5Ft6FCLT-c4onf5jSStfRW9MmXR4DIIovbi5t0gO8i10j3MVUGuXz9Un5aZxg4IM3lhzO6JAEZGuQEsMEXPWOP1pjkOir8htyTYb3d_yvokFHiPFA4orknjswFnv5lwSaATRjNzAHRRBFgZoOTCCMtU-3OO8fN0k3Wgv_4hjZ1_TeW2OIxhefTLWlELNuQJ8sqZfUbMOOiKjBtRCyFN2ttUuTPyJgNUSnzm65eVzuakpE4n62J-ueiaHtVt6WoVtvoAc9_TIKd?data=QVyKqSPyGQwNvdoowNEPjUIWDKj54iqaRMlYqhrzwcRnUKdtofsUcTFR5lpuR8V-4z_-68Oc0nSgqTuwPMxKZn5fO0ozUPsMWgnmDjjpv0jkwSTwrg5V-O1KERZEPXwaK0Rb2fQAOjXuNd-eImWrNscwF5yym8scuKt1-71IRtTUrH6CKpfHk54WUjYYFykcGwnM7DPAOTEb70NExmMowjhGdwM8Nr5S7cKM3BLB4uaxpplCIjDMG9anGp_l9k-dGy3HXD1peTJJA8rZQdJMCQSR2xegNsnLm-R15HerTfPXRmLCRg0y8FHtZ9Y9UBehDYZV0gx3efJmMxJkUqjznC-WYoHDGEKbZTd_zRhH-tedloMa9nXPBAFB5vHapJY9KzfeyY5HRlwGQFePkBq4k0eDg_bOhDTtunma1s2_XNmyHaI5tO4SPg,,&b64e=1&sign=038b2a415bfab86566bfdedd5c1ecbf7&keyno=1',
                    price: {
                        value: '91990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 412488,
                        name: 'Doro.ru',
                        shopName: 'Doro.ru',
                        url: 'doro.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 0,
                        regionId: 213,
                        createdAt: '2017-04-08',
                        returnDeliveryAddress: 'Москва, Лавочкина, дом 23, строение 5, 125502'
                    },
                    phone: {
                        number: '+7 (495) 120-66-34',
                        sanitizedNumber: '+74951206634',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mYMn97RTPI8QwsPCPoXTkWT4tx9G8VNZXdHo5Bf_cxjoElIAQw_u-3iBCYAWKmKqJQdC2VJg58vOq3GayR-Ms8hBfdkEaA4xOw2O31VCdG8ygDxaevJQYL9Fd8f_Lid365OnAWQZchCIDqo3rYDhbBpI7liheSCAb7iMEle8eTAQYVhfWIaYQvR5OKFz54xZpYEp6PyLYroU6UaEu3rPjcKe62sMPtlvFxZ862MmPZlkrWUuoQVKsvZ7LQCoeqvitzaZnpQaa3j5e_PpmHVFi56VjUHnbY75suDnquLpI3EyAzT2ks97ttTB9FVTWxishUugFA8OrLbRsjo00nSJL9inYfpZYCAfyQNjRAByt7sNX8VMIMHb_I1dAHnWVIvfY0T0ghEhIgU0xkE6LE3IT7KHT76JBDqplxPIKUc_qDyJ4-_8WHDQkEQ0DUNB9MujteTquItqE7Pkf-TIVqOLT4NQoic4Mqfvqm3SRhHKzINdjj_esm2mKA9hc9o8vLaCvGIPlaxgr3X7V-ze7f7qzy8ZsY3U2CO5GprVhtwBcxyL8ec2tIbhoupKbQpsDVGBNvy-AXUIhOC7eVogFainBum-1V6JNmkuqH7fPxlHOO3fmhHCd0MRm5NfKONFL27QFmh-x8snOnHv7dYSDFZb4-ZckjnocqIJx6g9XSXzsj7jerFengtuonUR66V0-G0TY6nY6FNTpIKH1U-JVykq6l006OjyIq24iyP6P3X6VU0c7vriPOuUgMO6J7Ts-04dUL_RTcA64tGu1nrJ4k2be98MjTZtFgNna__mgg4iKh-yKva3IfEpx5gB0-RMpgcVavZtY_h0v3CJYPbTn--LkbH0-Xp5lBPl-BbdfjJ6J9gy?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8bvF64TGNOJOLxN5d159gOAWZMpHZtyuqsjeF2CVQdUmAa-2GXlRBMFV-y0p7jaBgpZMeSk7ZfHDGwDIwhLzfW02_abljU6VSxoziDJDwWN-MUhxynBuKpwRUAgknk5pDYXm-BhwJK7Iq8W5gMuQK9xm7y_jpRT6kkuOeFU43nKg,,&b64e=1&sign=7405a8897879168ca9647ef9ac4569db&keyno=1'
                    },
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: true,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 14,
                                orderBefore: '20',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — бесплатно',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [],
                    previewPhotos: [],
                    warranty: 0,
                    description: 'Коллекция: X Ширина: 70.9 мм Длина: 143.6 мм Толщина: 7.7 мм Цвет: Серый космос',
                    outletCount: 0,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/jb30j9qlkwEbkhX-lsf44A?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=uFKZVedV1zjTx51YG0VWtwLkQxWQhhvwNGP8NMpQU4JpgoJ-YCqVU-tPXJmVP4CHyj__ezG2aaRlLyVxg7aW5l6x1qADpzF6zc-cW9-NGuhJgt9wBFroC59uciCcr2ZB&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZFsrW3PGFsO7q49QWzX39tpXw4n2xglxpoEZ-Cm8t6-xc4veBYbZSLYsW0g-M4pWnw9qhiVk5MhPOTsAGfdDA5xueQ-i3q8wXQLrvoiwN2v95mqZAbrI-LO-3lyTpH-Cn7H0Whdn1PRjeAYpiIzWkTQkxFZ0uZ_9EU02ADaOvSOaOJooxQ6rM1Rll8Jd15bgCTK412EVCYjNez85FEkchegTRO82ukrmJrnvZcGrxfAos1SB3t00VAeByNjAzqXTiuOL-C3JoBhfLdS_7wg7Q5Pi0yofJpfs-w',
                    wareMd5: 'n18jPvrviAmgvYTb_eJ4YQ',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256GB Silver (Серебристый)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8maiGDVT3vAXtjtzdZ2DCzqBaTJR2KozVM-fKkkXYvIg4AXSzH9syMrJve3Bn6R35vLuaA6jI2cxYZSspGXAoAoSUG8q0iILM_jXpNrk45TnvcJ5Z_bNph3hpoeg9Sln-G0jyxVRzFwlXkX94I60lX3A-8sOhZQTu2B3nvW5Rhux-OCBoPZbELnb7Ccm7ojWpglVyyA5PkXTEol9N-xRl-_DeJVMMEN1M6-YR9PQpuVGEX7juzfLlvwjdq7WswoNjUPjsKD7eh-IL_9Lg_k70hqqi62naLp_TzCUfKeSchDj2nMOe5GEWMzb94XS7bsA-qUQArv0ApbajA_cPW5wZxjHmzpu4UhNV_ls-i1rKwqvQHuJqAZvcLCf0zuAtZTkOkbC9290hKy__U0R8wywAULcK1a4xzTWI7UvGieLl9Yu_cgesNDmMpH8rECjiFfmJE331q_zUZWInayAiLp3aPAOiJ2gkTXsv80NVCIahiheERkU-1e7Nf2uVkKHKZPYibmdmqzlYZpd0SdunIqWDz-d_uJwW_n2ilFRqsVPbB8-4IjePgHUS4xhJednB2H_6nQ6onRZEgF3fG3B4Ockz7EZFH00GtUZdcSxtcO8C3KmYyiNRngGo-U0z9geEv8nmXQkR9rnrEmMkiYnB_6_XEUY5825WqutIwr8IzMa1_4_WAwmwfz1TvZtdD93kx3kfWfnzIII8pv5RDeUV6c53bJ2byoCyqWwRupj94jreZNfohDB100V_de5tpm2KnFu5efc6XHuY_ZK14ac6lMgJbDLtevVZlcr8d07wMSjw_CoiKRa106Xnj-8pkIVojsU8HLXK9sDbZTgNBE4YHJkmb4DZsYXEW1Km4RrixGlywaPaIrSu9uBLO-E,?data=QVyKqSPyGQwwaFPWqjjgNn1SsEi-TOfXfc50ougSXv7bIE4XenC-H_KRsjswN4Bsww_Qaive-QCfGVSN2bFg645MYZWbvtTR2wocKto3MVC974A8wwAXCruKGePqTKKMtL9JEc0hlTw1BcES4AgfvPBvcIRLLEgIwe7ZLG3M2XKaEfCutezDVlfxACW4D3fbFlzgNZFNv6c3NILk-YjJC1Qes-TCG4h_vi0T8uO3qzJDzTNzpc_gecFSfYnRxzod9SeUfw1t0ec,&b64e=1&sign=ea7737a46d4b48db9f62b4ac4e7fb263&keyno=1',
                    price: {
                        value: '85250',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 248777,
                        name: 'APPLE-ROOMS.RU',
                        shopName: 'APPLE-ROOMS.RU',
                        url: 'apple-rooms.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 348,
                        regionId: 213,
                        createdAt: '2014-09-08',
                        returnDeliveryAddress: 'Москва, Кожевническая, дом 1, корпус 1, 115114'
                    },
                    phone: {
                        number: '+7 495 188-08-28',
                        sanitizedNumber: '+74951880828',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8maiGDVT3vAXtjtzdZ2DCzqBaTJR2KozVM-fKkkXYvIg4AXSzH9syMrJve3Bn6R35vLuaA6jI2cxYZSspGXAoAoSUG8q0iILM_jXpNrk45TnvcJ5Z_bNph3hpoeg9Sln-G0jyxVRzFwlXkX94I60lX3A-8sOhZQTu2B3nvW5Rhux-OCBoPZbELnbdhXu0JevfmaJ7A6xEaWkLfvaA7M2L-8-40DXfJAafl8Lqj7mpTNxqj_AZw5eHmlPW4TJv1AXKYSbAZ4pqfbH4Bw8Nqfqfuc4Ut8vCTdC6XECbp0gSGHduHPZSA2-3RFpy1THpBtrqP_chT8lohDDP2fJRhzx0XSy6G6uoOWhElMLhJmERZeZE-dCZ-DCwiC9CF-m3fEll77kDKEJzl3fQqgUuX3dPdZ2MmmoUpLw0m5ERbYhoyuywvv5hydzQ2tMR_W2XLR6-UqjbbRXMS6zPic-5TdYulB2K1KznlljulwCB_baUqijJzwTC7h23_PRlbs9BvhL4DrrjXZ_aTxm9tCHpxPGQmnYcs9ROERTuZ5bpT9_QHU31we0UjOwfRPbKmYEWvjxOPuBOUP7aB5tpSUh0ZurvchVUmj6GEJsidhupDnNndpG1sVhM7BnVxsnoWLyQMlltqviIwd-qO4JK65OarqBdu6KNL2LHiWX1Ivf7LKkbKOw8Un7Bff7HMr-2AXrowGW9YVjsc3_gAZ1mZzVErWt6_X46cPU2dx-nKNvcSFkkqxlTbYvDFU_LYeQYL9UaigDDVq4XLqXgDcXfL7hC_8bLRvhvoxgqUpA4wi1oqa7L5fRlAsTQ3puhumhrqieyTTBKXaqQBlf2sxgqPujCiv47hMUsqJKfO-JVwlmJAR0qKbDrG8OMzh9i9Fs,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8f1mpnjvgVBsSCAaULd1fJV9XBqo7LKCc3u8gt_aP-nNFgwQl8ilmj7OSJOKc_pRrA1VqyLS6i_5ie2Zg_IpxTKehJSZOHg0pCB5p_HhFDxP1FAnIDQkneplSlIxVaNLMC-3nrmijmBgp4rz_hk9-I3PjJgim-cj1QXArU24QHCg,,&b64e=1&sign=8f1defdc25fbaaf886e8cd53d60b9a68&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '390',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '390',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 390 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_w7x8BFCtZ25pS4mP0gQqew/orig',
                            width: 500,
                            height: 702
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_w7x8BFCtZ25pS4mP0gQqew/orig',
                        width: 500,
                        height: 702
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/220472/market_w7x8BFCtZ25pS4mP0gQqew/190x250',
                            width: 178,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '188-0828'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '17:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.641845',
                            latitude: '55.730801'
                        },
                        pointId: '365183',
                        pointName: 'Аpple-Rooms.RU',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Кожевническая',
                        premiseNumber: '1',
                        shopId: 248777,
                        building: '1'
                    },
                    warranty: 1,
                    description: 'Цвет: Серебристый Объем памяти: 256 Гб Совершенно новый дисплей Super Retina с диагональю 5,8 дюйма, который удобно лежит в руке и потрясающе выглядит, — это и есть iPhone X. Это первый OLED-дисплей, соответствующий высшим стандартам iPhone: невероятно яркие цвета отображаются с поразительной точностью, чёрный цвет выглядит естественно, а контрастность достигает показателя 1 000 000:1',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/n18jPvrviAmgvYTb_eJ4YQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOjaZ0_l2ArHHhe3Ll4pyVLboZx_eqyt0nIROM8-dukwa7NMmEd8LswjGWvJ7qc0MfnNCLGr45aV2i8QgVK6nYQ6nN3yQAPqZteYGMZD8nBw1xRlSZInhRTL8G9uL0gJKro%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cwvBLpoBy8EyKID1ieujTJoq80A1SuRjlYKJkFU8DaCUVL2jkL633GS40jx9NXrjAHUJEg4-bCFVvkO4Grqe-OTLDQg96sJamwl5JtGGOa_vRVbS_niOmIA4WMvSguxoSBuN2HRjphvB-DdxeFHKCJn7UrCwokI0gtwp8UZ0ZXJfoOJrcA7YtFPsW93KiTZocYc4RxfXXatTP5WIJtTB3pxFSrCeBBoPL_MRrrLXqPd1COFBR5ywl6fXoq0ardjlKsyN6z1S_8uqmDgqGn_GSFabf7T02H6JIbtEeT-A9zP3pi-jhlakEgLgxBroTWFuFAADmcqYNG_pvjRpMaJupvrZ2uP5U34gy6A1WSHJJShvMWk16d-xmWTYTh4UmpIjjmuVqsoOIb_fD202Wvw5h3yjzxzBV8IVrt5pbRlu5155xMg8gkRn8LWvxuf9Si5w7-qGrsbKDmrheNHf2gYQWa2FiwT96Cc3t-AGFw4vAfnXQeOs3ibC-RnLFzCFq4v6nVfuFaWClgW3xKzJ4b7mSAfG2aFXkvU4I34O5tlKMF_YQkbEFS1ph4saoPP5g-ZIpnmh0hYgFI7uL824_Vl1Ducp10-q5yZuAZ3lKX6STrLK1x1U2NZIpQVtLwizfpni6SsIdARrE-DqCaXGIe7Vj8g491oI-p-vjGZ9fqDHiaPP_NI91C7rJ4NNMChHdl6MF0,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXhCMqrzS3moUcTCnbdMD-fxpaFzr5-Jy8XL4zODgTgJ1Adjwe96TZpMC_6HnpxukZSGGmdKeNqA30frm3EREVWRnbOvN8K31d8f7IkymBpaVhEPHBx65fIVmLf-xE-zH_3-k3TPq9WT_gK00R71oJPnKO4RLxPN2XuGpXKJbsLhKEUtuaW8pMzbRbeqSx9PukPwKcBw0bxH0d-YYvwwKLllr_gieXSgcCtiSZtv6EeKCtz9QG08IFyJPjnLa349y18XYVqj2UEiFNIXU4CfFI9d8MJ6PnDz6dcOPtmJVK_GU_zbtBHj8kdw,,&b64e=1&sign=2b9ebf863c79d3f91f54e0cb96fdeb2a&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGvPkw0_pvLDzFXLF2wRVsS3Rh8fwl7Mx0JcqpFKmWFzQke_Ofw8IrZrtqc3Jig8JVkyi9uBVVhgWkNcseQIZo2e4JAuuoiNWDwhBZsIMFY1z-wCh3fbBWkB7QOfhBbCEl6LbwrKj0j09rV3ZQVmX_D_UoTG9co_r833CsCwiSBsDYs4QQoUwfgh0ebCH-Y4RQ5dHGEMy6tHwcN1u1Da7PhiazSfB77VL6hs9IS3aLy1VQKHpjs1A5iLqWTFNJOO4pesUAHd19LKckLqve2HovRvuTZz3NyiW8',
                    wareMd5: 'TVeDFEPrb0QAhFKMh_nsoA',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256GB Silver (MQAG2RU/A)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4cbFtLcv3ob-SlLi4Evvy8U6xGjTpCRt-zOAB7yCwnDe7f9mNQ2A9VDNLT-sAQul-mGceOWkNGIt6M15sJEgS1K0FoGqNycd3a4JqvHk6NCIQzhiiyNIzZ-hLgADA_h862gZQyrhNEq0vLCmSF4ZADSjudDbRnu7oqmAuE7l6dnpl7PkkXZPqjmD9PhhgXwR4xocw_DbvhNxo0grxA11GHfoFQdquiIbtye96IV-6V1bqWoE4W_OGhGKbBCp7i0GxXIted0UUQQjNHjlyY2aImE_cLu2XWiX-Gg_w2CFouBfpuLXemqbK5qBqVcKmLvbQkKOF5ihkMgKIuu7Tm64PXIvJcCDGQXrwBziCYDBRbGfpo2gOcdQwla8u4E3tHzHavDF5U15xCwQJlbN_d5ZnXNlcnhdlbW24EN1Ebo-jyRHVWsWEb6Ejy2iuSD8Uo1psR35Lboww85AgTNvuaXtTcJiZ-1m-Tel9OBs-wJmasS8kzP5wV6SGlzJfnJWAnOC5wiEAfxSLc1SycKBoQlsPZ2rHYpNMQ1Y7z9Acxo8ZO_qbNYj0XADG-h6-idgy9O6e9_NfPoGxyIlIqerRXd7TQLyu4r1c3dK6FQ8y9A-dP6WPCJWJCUPSsV0k-_pp8BDKgwwSEJjpabo4-YtdAGxwTDCo-XRwCqnQsbuh6voEIlkt0Qg1EF2vmJ?data=QVyKqSPyGQwNvdoowNEPjaaZR_STU3giDJ6oHNwOTagb7FrtVFunoJrPlAty0IYC5vzw3Wwsx23M4BU9eY0oEf7m184IK9cb5UV97p_jl2mDunUAfVCspVLOTuirpaZfOThcXhdw6Z--cVhjSTqCnZHFsm61rBHmHINl-CHfe3MCGuFIPDylKltHpcYs8Q3rGoibk8FyJTGwSKf1xRhxFiT5aKirKb3gCpGocxKYKHsSFYuLHr3w0Q-8_5aU8oImzeuGGQODy027MUt2IY7e2oK0QCdLtS-uHA-Ys1L39ahAGIJHh_Gna5SYjHotEMJpcBdlwA9K-FHz3Gu4pdP6iop7dkZbjC0hHPMi9x89v6ICoxnCiK9-iPNCUZJ9bdTzQJHbk6vMx_wpgAys5o1HPA,,&b64e=1&sign=69c72d5eec45a6ab3d42cb3af2f5bf3a&keyno=1',
                    price: {
                        value: '91900',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 3534,
                        name: 'Flash Computers',
                        shopName: 'Flash Computers',
                        url: 'flashcom.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 1285,
                        regionId: 213,
                        createdAt: '2006-08-24',
                        returnDeliveryAddress: 'Москва, Мясницкая ул, дом 15, 101000'
                    },
                    phone: {
                        number: '+7(495) 228-0906',
                        sanitizedNumber: '+74952280906',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4e_tb7h4VQnaVdS72FhPD8xp03i2D-XvrBiZgtRs25xBpyPRAUXmFoth1O8g6sgMQK69L2wNVquL-uv4a9SIIAIUsEk8hVrpM--HvtZMcwhkzp4Y2z043C5H_CPG9n41uT1NWE-IcmBPLRmvz6_FIsyFf3HgDsix9heQaY41ZJucP4IR_X_y3FXHZLAOt1pphegWNP8Wz8-lIKdTiIdTBlqFQKMfRAbiHX5etOkg8RXiwGbdXE9xiRzUS7iL76HfrRCE0X7jyWDGndCNTGh7z8EmMYwqGcbwy84sZ8-vpa5zpvRfd-GTucFYnIwwIcrkqXM_n3RsHrSVTBS4jaaK-1_VsQNHmR3myxRqLqKWNZi_icSy4NJb6R--xzXga1KZxyrZikq6E2cXQjJkUgHcPO2sg65utzvCmTDalf-Qh6PWr3lPTj3w7OfGYYI8jZmwK0hjLr3jON3oUZ8MCEqmiSN_NDF5pN2kL-YmrHh32-tuHBAOAFVwMyEAFhjLnGXM_nR9oY2L8L9C1E2Yc96D9s8DhFLK36vfnfURxeQzY2P2_RmuJDDa-6npah4ov6LA3iGlKpngqTi1sgqryootSYbhymDJlrPbBVkrlZaaCr-u1jW0EPHG6Q2-rPXZGpKihmsXzky9F1sbYk4PYnjogm_CHlJa_3_jFTGZvKe8IyymLIw5u2AIPNB?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_I18hOnulTQvUV70jFnwA-5QzzhGxpz1Y7sr3lmncMywnZn8QAYNEYiV2B-Uy2CRCoy-T-lo5l9U-kjkcKX3TXXyNvq1RXm_285YIzv4OxeeO4dOKwqhZ7S2jIxtZiohxs6ktUNygkjWoQgAI1OvtjluWmiFfDsvKVOGunjHfmQw,,&b64e=1&sign=c92a02bdc95331d24b82e6272b88824d&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '450',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '450',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            },
                            {
                                price: {
                                    value: '650',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: false
                            }
                        ],
                        brief: 'в Москву — 450 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/477791/market_w7Tnhh4c-JkQR3o3aMBv_Q/orig',
                            width: 700,
                            height: 700
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/477791/market_w7Tnhh4c-JkQR3o3aMBv_Q/orig',
                        width: 700,
                        height: 700
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/477791/market_w7Tnhh4c-JkQR3o3aMBv_Q/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '228-0906'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.634542',
                            latitude: '55.762996'
                        },
                        pointId: '20375',
                        pointName: 'flashcom.ru',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'улица Мясницкая',
                        premiseNumber: '15',
                        shopId: 3534
                    },
                    warranty: 1,
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/TVeDFEPrb0QAhFKMh_nsoA?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOh0FCQr821I3WvzT0OJPO_ngJDwp3PCjxbQkKVkKT_PLA6uCF7Q0B024vYy5uu9PlpQnAiYCbqVv_mv3Z9ZqX-nNXovQEWbrYQR5HZ2U014FXQJOSX0mtQjcw2TebcB_Bs%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cwGii34trLhHlV3lfCViNAzac2aXwTDfvkehZPEUokdacPpKjcoi33liL0rjH_jRt6sonH2iRLtJHGeppoLrDrkv-gh5iHdijJClnN_p-mI-3DiymNHvqXfGfPYbnJ2dFGgXlDRb4hZIFqrNncCfKiLcDF_5sK321ekMlOpwY9u4kaMu4TdpdJ7uIhbtA0hu4kZ1h4rQRpvs8nd4DHIK1fuawpiiNqrr9euvTrjjvogA1n2MUbT0XI66xmaFn1BqgtIOlt79FiDc7gjr5KA7jKMFXgZ5k_tJggEwbKNc33HYKTaGLzHCiq2UIReS24qRbkZavuuM2S9hoMxfOR7IBCbyd_pqelhFbLxQc60ALjO_x-wFnBaPdBjNmroZWg4nwO26-SWdXbk5OijJn_c-z2Ntp3UVjRGajr3FUyEBPC4AqfV4ZRfX8wZ0rFk2HbgrbWLkcrCoaWRsD43IUlj4uMmmaYBftMCr1hw0n9a7s2DOWSYw7gB66VFPDHN_qLVaZ6V5TtLtLU1zTZjOVKhWMAM6BuUu252-QvBy31qhUnk844xve6xUDrVC1wfPxE7xwT6P403xiRej6SmG94CPlx0hZPqEbUI_PAHV8ROand8Hw6MH22yJcxtxZHwbkHFFdGfvbGQ4AYYVhJUqxI-cZdB4MRX1JqaqdVpz2p1cnlne2SNs7mNNdB4?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXr48hhJeElAsmofQMr_chvErddP7yxRFkFUdp9GiktP79P3ddAhjeFYSUWcMBQKHFPLECHKCS2_MkHg7fTLTjr-DqcyFlhZM76J3zra1pqjzbyejyvC2rlwV_QtpNV4LvHctQhOp-RkINyG1sIh_Vq6P5sAgigp4WfMdq05X68z3xM1yCKHuELBlfVHXRQeX51WrfXqKe8LfGPEjl8znci4gNRh_aL0HyOd_ZefT50LUd6dchaVxyItit0uqoLIXZ7i1Gnjw-p1h6-1snyXjwr7Hr2apx9FBOXWxpq2NvEyDxoC033hEFRQ,,&b64e=1&sign=f2bd2c21b33dd0c8763c97fb618da60b&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZH9HI-WKuE93-oROIcIDKnyp0iPH7jOkqpqtwUFAD-hIrjdWQ6Lo8j4owSLJf7Lvu7HmBqXiNWvyhV1Cm28FnBXElpxApaildJET9VzEw6_ixLotUTRg0Vrj3LnoKi3ASCtsiXOn9LKUfmq4XrpEoYq5d49_Cl7eychu2gWiqzzaKj05UWJoA_A6rASWpzhuv-Ccqx3P0fOnqi6Tj1wAiH5SebvB9zFJWN74Gz-U7LKwN5dvj2oDZiqWK5lc00-xZ1QOOLSJqaRJ4xBF3g_QNH20FMZmoCok7s',
                    wareMd5: 'M--HwbOer1tARvBsdTXrjQ',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256GB Silver',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mdL0yBxy2UoT2mOotedOPn78PKBi5qc6dAsu_SeSI-c1Qmi77yEsDW25I35qFqNmyw36shvnuEVf2kgBlL6h4inUCi58HXsKH1_FYUjPvik5f0CU9k8E0D2Q-D19OumiYs8z1RNrwYmtrC1QkLs_NtE9GJ5n1yB1qk9tTFlEkOjPN8VX6Dgp_UvMqksodEL83e1tMCT7UbZDubeo05BCzRCLxXU8iFX0DZqhbvxiy-S5VQplUoUgyyHrd5jse7IsQS3-TVFbWjJTEps-eUTwrvLLVl6aOkvI0NiXFKkAF8aNISHTFRiyWZwtYq8wXRpcafV2nOThuSr9aUqxgCXytCZHY4S5Jyz-n8wW5tHxoOZGHbjrhMDVHI9c4X26XeaWgcwEaoX7LG1_NyHs0roZ38SxqPBvrB6GAbUzNHrlpfVL7GhKBz85QOnpvEgda1lbNRZU6HDnu_wF43PK4Fr11NGSXhV9U2pHoNLucHJuJek0lWXjHW6HNaPd8eoNPnAhKAWpLfnaEmLfLVCQp9Kv1Tw8qMXomA4w5Z7so-O4E4DCm3K53nVom9ViB3PSyPPQvPX0I7NTh0srgnjgKEYib5pYXqEVFL8Unhqs0r46ocf67ZulaSEoU5ccL0daAY7G8Q9biWU_mqsxQ34dNZ_QRhHgeDzPZauxJVyUcIDSwD6c-rZKqSjwmhWwTm-cbXUq5x1rmkDvv4GDpSgltiPTcbiBjCH_mIOw5lKWdPIxHIfidLNx6h6JPPHnGvu8j9OK6FCdIriUPoRVkVga3GWNki_Fw6oKQTPSVk0_HewZHOZ4XGm4d4CW74FpiU7dhVDTRD9zUrcqkuhv9Y7xTeCONUvXDizLDmWxnJTjlFgdlpRRZ1nngpQTACI,?data=QVyKqSPyGQwNvdoowNEPjXSYFXgksUq5RzxjumjV84tJ0eRDZjmE5imTEXgI8WGoLlwii71zHKnNCx5HJbpK2TF71yKrMwhZvGFeRzh7d3rTVtMOXtaYmb9WJFVjIXfemcivHXSQ-Q-HaB8VWIqXmfIYHzNxE_VEtN3M0wJAeNfoi-5FiEjhJpDmRdFGJLiRHiVV4CgzO9CeXMjBCpbWN49yGzBEnFXP6JZcJrIBz4O6lsSJavYpVuazCLHMLtwmANngZRXXMG-Fkbk8f_Cs-91S2KSYe3PIo9BsZg54Z29YXs9jTBGZG-2wr3I46GZTavC-lYNgzfmg6ulQt4yJRwI1ZlUCzQIH1RwihtBknEmKbQGvS-FyLSgUFrAyPoiN9HmrDyBk2x86lmCWEjcQ8bo6OTb7SAJNcys2sfIqJ1PnxpxAIaDckTgFR-rDUJl5Y5cLlYgwxJes3798jb76-DBX04v482GLW9uBc-CszXdxUNW5ZMNw-EBUmbZB8m4C9_kFUQl7t-WR0M9qy07imZ5erzs_jomYgtOESc0ypFjaHn4A6Ahh4NzVo-kLGfwDXDvKT7qmEO3QhPrjb1S0dw,,&b64e=1&sign=ec24efcd2ad91e9cad31f3696aa31ace&keyno=1',
                    price: {
                        value: '83700',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 112249,
                        name: 'Gsm10.ru',
                        shopName: 'Gsm10.ru',
                        url: 'gsm10.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 3054,
                        regionId: 213,
                        createdAt: '2012-07-22',
                        returnDeliveryAddress: 'Москва, Сущевский Вал, дом 5, строение 12, Павильон Л101-102, 127018'
                    },
                    phone: {},
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: true,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 0,
                                orderBefore: '17',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — бесплатно, возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247921/market_XdwP-FDuNY8FPQwuDU7P4w/orig',
                            width: 344,
                            height: 689
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_MFa3Hr_rMtvw0Ebk-isM0A/orig',
                            width: 344,
                            height: 689
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/216195/market_9rrnhlz6Y41q47wYk_0pGQ/orig',
                            width: 365,
                            height: 725
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/247921/market_XdwP-FDuNY8FPQwuDU7P4w/orig',
                        width: 344,
                        height: 689
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247921/market_XdwP-FDuNY8FPQwuDU7P4w/190x250',
                            width: 124,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/247272/market_MFa3Hr_rMtvw0Ebk-isM0A/190x250',
                            width: 124,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/216195/market_9rrnhlz6Y41q47wYk_0pGQ/190x250',
                            width: 125,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '374-8057'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '6',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:30'
                            },
                            {
                                workingDaysFrom: '7',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.592671',
                            latitude: '55.796192'
                        },
                        pointId: '333184',
                        pointName: 'Пункт выдачи ТК Савеловский',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Сущевский Вал',
                        premiseNumber: '5',
                        shopId: 112249,
                        building: '12'
                    },
                    warranty: 0,
                    description: 'Смартфон, iOS 11.1 Количество SIM: 1 Диагональ: 5,8\', 2436x1125 (Super AMOLED) Память: 256Gb, 3Gb оперативной памяти Поддержка карт памяти: Нет Аккумулятор: 2 716мА*ч Поддержка LTE: Есть Размеры (ШхВхТ): 70,9x143,6x7,7 Вес: 174 гр',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/M--HwbOer1tARvBsdTXrjQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=GHCtf2_oGQvRcXHoHUZErOU6MPRf3hnYh5_oxoioNLDgGd5OnPnMH1OmpDXkIid3lW6EwbCOvR4NrX_IiIcsVG7p-F-9p0b-zeeEsHj37dOkAUw9v1GTXFawYnEvYxCz&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZFG0I00atFJVnm1PS3stQ9nacDTMUsQJFB8ofcdX1vp_57-K56BTkxXGltBMpat8GOvSEoicsoVXAopgWc_ys3oD7PV7-iDuJhuHUZsI5zwbvONKBVj-Hi4smOyGVaHCqUuu0leFBzubLBQudc4QAbi--IwC_zhYsAFs8D2fS2riUZ6SkmkgnoWud3zKP0kXQIItyHbtPZIvuSol4IFfNBd58glD3UoZQQet4PgtVKTCcpUQeCb1DJTosF14ldPmybZ3AEqEVnxw0XUNbfm9H8UzFqFlCM7MDg',
                    wareMd5: '72wdBsTgNnmD0ymwE5xmDg',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256GB MQAF2RU/A 256Gb, серый',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4cbFtLcv3ob-ckwBRwj5iw3JlkL_caNdFumrvcP-zB9R1eNLeqbbIkjG33cDtMdo9gIwPC5wSmR7D5gzSXqhz9l_RTFRsD4C1__sg8MOz1ZapruNuKgFM45czINw5HrMT_ZsbQPir70k947mZXbYKCKf5svx3X6m4iamKpxaJXe8ztCcNh9DhY2-yIFRabZC2PGje9GfuL9FEyC0qV0DU9IGNS-CiafnIlrjIEH9wJStj4cuWuZk_2QNCDOb4coyGvKUCrwigkrZh_K7QIAXzM542H3w9DrFJc5lhTRHpC0sJlhRPyIpWk8xR3fiC6VgvJzGNLaKGKe-N7iKBYQoI8WTUBTaQda5EA7oaDH9e67c_edGlZ-iLbCK7I6KpTnq0Y9BOjLLZXkg8fQvPzTWsR2I8hXp5P2-bPXN_Ttgo5PVqKkX0-hFLdrK315bMIN1kJmF1XViPVSiBmZX0ervsqwo9RJjZD0lvCX9ek12EWt_7QdCvRE18mxdg17KR8bgoPPrys-KKXKWlI-MxVB4LdBlMoCmYKmLQ_Qhcu_CXCC1uVXllvC4OHir5geLdKo0HEst3f5mySV770Ly1SMGXNouKjhRVyO9yWEL66u9gvZ4PWWrq0Ch80Sqvq49ptWaqBhnArrr0qjhbLSfq42Dd_8yuID70yPi-R12SRyDrZq2CsRkEEaOD-T?data=QVyKqSPyGQwwaFPWqjjgNuOY_JmPgeu8DQgiSGNKXauLLjdXJQUlv5RU5OWY8D0n78NvCCDlLTY3N9u4js5vs4fBUZa0HPHggB-dY5Sqen0K0aEUjvwmHNmJszI2fnsAo1OABk5TOKnymfH0hEzvmgV5VBmn9Kjtl1Po3K7_2Vn7oURV94OzVmjtX-s8VSBEXkDyBDUxX3j_3RPS0jxScDWOI0XErfWM2xIpXeW3MM4J5LwKe2JgdDBNclrbGm5i&b64e=1&sign=afc7960a25b1678aecb33b33043b79fb&keyno=1',
                    price: {
                        value: '89990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 400536,
                        name: 'KO-BRAND',
                        shopName: 'KO-BRAND',
                        url: 'ko-brand.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 341,
                        regionId: 213,
                        createdAt: '2017-01-31',
                        returnDeliveryAddress: 'Москва, Береговой проезд, дом 3, строение 9, 121087'
                    },
                    phone: {
                        number: '+7 495 005-78-91',
                        sanitizedNumber: '+74950057891',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4e_tb7h4VQnaYXZiJKmNGKnH_0eCS4etRgrv_3HWkKNOqqQQVgWTkBE8lqsYxxIZfiR1luJzhIx9ZUWrG2YjIPkHF6t2hPQbhnye-F0OBLWZOf27k38tOonIQZ9cwQUPWsK0edCRsjcjO-kgRszZBEUjvenKhj9XPveOKP4_i4A3mbqjFaP7_ObwDv-BPxvhrMVcXmmY-qvaNubnKg3neEROdZrMalcmE3hzWhDu2DZnnqVs0NqXjvewCGoEEP5-xadOkdzM8uiIQG5NbnHeFFINKlHxQSbOwZrwYbMX0BzG8dBHYqodzJhjogQ9vjl95jjHiqu8I3DZThied08XnE_H5lhkW6mgMaY4zh3CdzOkyonZe_Tl2s803ixrwIsSSDr6_arZT7-xvWMadav-FRqC0Qj_M05hUtXTMFJQKb-guqE29kIF6DO1xPPLPrcHaW3s_sorrDkrg2o2n-6wtwKoz7AIO-QCPjft6MZfF9GeImk9rcccHoCz_mFfvdkhkhVk1zPqxtFiPWNijWbiz6csftvGXg-AHZHoJwsDHOxIoPgNoqUgqVNH24T6vR40mstSWCxPI9NzCT4k7k5CT0qmGYVqJr5Jxnib9yuNH7TqmWOTCNuqhf7n0RpVNybnOJcdnx3AtZmRnZBmhJ4oE1L4puw_xuXzvH9GgysKbZii7LrH0SVXo4i?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_fdcAibuP-dyj4WHyvZH_4eddseMXTWbIwXuwlDu7KykJqS5u7dhIqRetCXCy-ONQQ8To2Yls7_T7nB-WjhoavxtgI_6zHpX7CkCtpXc-SZxu0g_Yb7Ic3TotCXdr-VAHjZ3ZKiRzhazXUI5g6N9MszI1-nooT3KzUr-Qv3tfFcA,,&b64e=1&sign=773b0cb30f3bf399e1cff5ad8a494ad9&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '390',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '390',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 3,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 390 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_4Iebr_bYWk3bFiVg0Mqfew/orig',
                            width: 200,
                            height: 200
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_4Iebr_bYWk3bFiVg0Mqfew/orig',
                        width: 200,
                        height: 200
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/167181/market_4Iebr_bYWk3bFiVg0Mqfew/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    warranty: 1,
                    description: 'Общие характеристики Класс смартфон Тип корпуса моноблок Технология GSM 850 есть Технология GSM 900/1800 есть Технология GSM 1900 есть Технология 3G есть Технология 4G (LTE) есть Тип SIM-карты нано-SIM Операционная система iPhone iOS 11 Дисплей Дисплей 5.8\' Разрешение дисплея 2436×1125 Число пикселей на дюйм 458PPI Сенсорный экран Multitouch Защитное покрытие экрана стекло Конфигурация Процессор Apple A11 Bionic, + Встроенный сопроцессор движения М11 Объем встроенной памяти 256 Гб Камера Двойная камера есть',
                    outletCount: 0,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/72wdBsTgNnmD0ymwE5xmDg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOhqH_Z7tL63ct5AqkZ92K_6N-L77FXxxSwNeKhP5MbG079q8QoUFnfx_TAL7rGk_vcfZY-u-x5PB5Sh4JaZBSMJKBAZ5xkbLaYEki_JbX_ABWArifvAa-ORQS3SJ1dxR2g%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6czRXdvm4bG53xqfh7fZpbFz1zReMnb0t72pjnh7uNpMFvpuEztiblhoCOPKC7CLtz2r6kCgsHQ7xFXzuGTI8Wo4_V6guz8ipTf3ysWgUrZO8jqvTlYJ9gx2L7N9dmQly-OOK04I-IkNOQdg24I3gSRRWwexcjqQJad_6kVE5P3JXnc5SFbZwwsREAZaYpWhQkiltwhR36H-uFkBPAqOmXblk5dmkYYoASw-1N9FhGbZDO49Z67pyFqGp01mPUtoVOS-sHUIG85OTPFFJfwDjEjoN_H9qHQRY_qOPRFcKhPI8r3dsTyEXzlMuj-Rfp33O3RCKKoMN08AQ6LWU7F1WUKNzO5RvU1Dy26wz2vC_74C8yYszsuoP4DAlKDfxmPoRaPVM2AgForcQWZ6NeILTDb0aDpdcUD8nbIwObNV7JHWHTDsqLhg9ZnXSQ0H9jzExkwJb6sXrmINBR4082ObjFsIfbJc_dIXFrnT3CpF1JUPMFuNY3yPVHDOurZiULXawppSHP6msV0-eDc8TNK01CFocBIyCGUOjqSuA9RyQt8lbtXzcT2Qx2xA8ChC0DKrsBbAzPWpod3ItMSS3VyLug6lKYLO6enuIBJ4zBLya80vQyVbfLaQIn-RSl0CD8bDlILnoCmWWbeuk-AWaxMFb51CBoD1Kq5plG1szATnIkuSSXALsu1p6i8A?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXsDrkjwszivSOy2-JNP3Jf69vuZuotchvCqfwZAbX2tpkAM8QiGv34H6TOSOUtSQqMZH-8cXYHIcrT4WUD2KCTw0FNbdrVaWNZJxXY0yyvzUK6PUT6BUbkP7RlgYRo4ixN06LB7EkTU1j88y95nHHkkj5rt6WifGIWSaLcWx1IM2Bby9SnzwfrBNhkk4YXVVgARZMYlnPr4o0y4GMZ8R-uChWiaKWsEQOiiVCm8poYhweR0fmDcgYMorcNk9jBteXPWH7XKmaTkPSV6PkhUDWXbrAWMjAN4idpFDkWszfZg_CdYl5nGuEkA,,&b64e=1&sign=7e2c85beed326030205ab927d9b097d0&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZE02ST-arCxoIpsYT9DLZBh538wRLJYFP5gLLK2WxS3bBy3lshkCfFuLB9Xn0LmMjn792OWScle1J64QhbqHWVC2EYX1h91MwJGwhui6G4VrxB1JhTtGyOpTKmkGwO47QHxoaJIhpfL-B1ulfMVMv42amDG36HU_O-8wLYLyn1f805QdxZLyUUxXsXcjxnzW4qUP7SDxWtG7hrxNWfG4bjAnEz4lG7zebq_3aVE0akD7W_D0tXkSRSrsUmx6wkZmwz3-C4wAoPhjM39OInc-noFjyWrVzyGtzg',
                    wareMd5: 'kGIf8XCLARka-0pmOf2OLQ',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256 Gb (Silver)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4cbFtLcv3ob-elo1y20DY0NZ12geFfzf-WqcW_0DHDBWMKHM7Fsm8-Ikb_rZVB8t0wx8eCOPZRxqhARq1m_rnbKxTqghzv-66BclrHyFUSuW_kVyVzOmLyQJR9CGTe1PJXPWk5wHwN8po3bNN7a9R21TRFV0hqfMSIJ3QNgwWkC_bGcUEGPA-J6g3Iz9a80uF7nm9AUOx0i6s8hfIzjnzmfvEgZpp0dP9iBtBlB6wH5kkQHB7y1taLPHdLHrmqFysUEQ4i2X7pJeY5A1D6X7BEhvmMuPXA6fNeTCOfGyxWjwC5PJYnGi3S4iKdF1zbY_OaUjS1L5aPKkUYNZFukakGQPTVRf8X4FNN9t_a6jl5BEqAUyfsqLsSkdK9hJ77nDFmKDESxOpFi0VOP_xMZp2efkYUU_J_OY5t6mhy_NSJTA7mlh8p-fxYcPj6l8-eBbwU_pJ3JQ2FCbyOR3uD70UbLazgEM4i7fehnNqWlNA4oEYtAIgZdUqj84E-C2EfSnPBYt90B4CLYj1Ku02LIJJUtNnw2k4xaGnw-cOzrL6UeXv9CS3j1i9aQxh4yAaS0z75Sx-UC0ypEPZ7JCxrCFIpGmlNCqfU9syZU-hIubpWaypnl61EFHJpjAFz--bCt4LcEPH5zhfxnVpIiNlJBR3NvNnYBQSxE8meLlzp7LLfqAc03GALY3qt8?data=QVyKqSPyGQwwaFPWqjjgNrVaHtSmepG3pGZqAxVC9q0JHk65nWyPH1MXvqZa-DawkYZHomex5haK0vhYOFcwCGVhKrpOKdZMAd9tZJApc9kH0neS_2iU3ivLam4CeVez2zuv65YGQe8SEn3ChCdd0E_7dLGEB3c9Av9rVDPoD8ucmjMZaBHyF19oZFdLfPOhKs2122k3i8eaFPryGqf8jUfY2bagV57MiAAbq9prJsjVbb8sFTqaBuH2YVSpR_pil55K88jMO68TMSY_mbEnjrOzaSlXuxtZMNK_v5b-zTGl0A73rxHiEasbn1huOh7GDxPvgzhGTU9mXcHDFWNbVlY6_wj1JPanZO7KkpZUr0z-fLUqM9PvW1uWfy53wGc_kKIVvx09g97nDPMvrQSgoA,,&b64e=1&sign=c1d7b15ed496f2a003657126173f3461&keyno=1',
                    price: {
                        value: '85890',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 320503,
                        name: 'MoscowApple',
                        shopName: 'MoscowApple',
                        url: 'moscowapple.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 83,
                        regionId: 213,
                        createdAt: '2015-11-11',
                        returnDeliveryAddress: 'Москва, Багратионовский проезд, дом 7, корпус 1, Вход со стороны улицы Барклая, 121087'
                    },
                    phone: {
                        number: '+7 495 968-28-49',
                        sanitizedNumber: '+74959682849',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4e_tb7h4VQnabawhKE2oKlMrtqGgviR-HLrQL8DaydKvQHMgYsTRByUW7_DUpD5FA_nfFIRcKF7sm6vjLwA3u2gJoBpPp85MS0ESoKKPZmfLgWtjB48LUI-Jr_B_i1yi5zdOVpmB5n0_3p_L4GmwAkTZ2Xh15a4a3JEmHPSXov_tdamp2xyZrwunNSnLuNLv6CG0lJP1sgUTqQx30o36OqUA7dOsYt1d7ObHOj7_6oF2Duu9ouYKkt7PWYG_ijLH3UBzPMlAnFKLhjw4h6cBopDgr4y_jtjVoZf6Y8e6_KnnErabtkMiX6Z2ZjqLe1nSyWm3vrKxaXe0oEeNK_GHytJoLWKqKEV6-uXILztCRUG67n1aN0W07NrXgGv7HaAChyCqMVIJCd3C22vkdKzpZbWCDcJPS6C2SYI3DtMcrl3lcy1LwcDoSgYMjLG6zxbrF4YmO2dQHSbOZFunlmUt3otM_zSI6Wi9ccWh5fxht0SIGhqx8hyXiouf3AzLhQIZ77OZPQvzsuFgbudePV4jOSf4BPrJ-fZjW8XcSmN9v2K-OxmzcxmFuxONWmQd_gfUVWK-9DdoCs0TdtA6xFwIIBxXxwnj3vyCOHRJmtXeEmEix-purcboWDoHMDGP5KBYi2duwyoFAjpueNlxsy33VzWJ49FNv1T0vNDHuUWbM-4AX0rT7bH7Cba?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_8IT7yFadZsG7jc-1mJeFygQ_XZ1SMqd5ZJfH7mS6Cts9Olt-1UDxvMRq2__C3D97c6KBfKMfX88ZtQmL8I4qxi2eYQo9asTII0dnnedy-S2NXTKeoAvnZfkn4v0bR6qBFhj0MXWNAQPgj1tMkKRwu51rzDJWu6W0fS1JUhfIxsg,,&b64e=1&sign=67f94620d9b85ebf7ea4611e08f4b3dd&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '390',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '390',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 390 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/234366/market_8yTa3uP1Uo3VzA_pH-dUuw/orig',
                            width: 498,
                            height: 660
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/234366/market_8yTa3uP1Uo3VzA_pH-dUuw/orig',
                        width: 498,
                        height: 660
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/234366/market_8yTa3uP1Uo3VzA_pH-dUuw/190x250',
                            width: 188,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '968-2849'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '12:00',
                                workingHoursTill: '19:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '12:00',
                                workingHoursTill: '16:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.5026664',
                            latitude: '55.7407924'
                        },
                        pointId: '423688',
                        pointName: 'Пункт выдачи заказов интернет-магазина MoscowApple',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Багратионовский проезд',
                        premiseNumber: '7',
                        shopId: 320503,
                        block: '1'
                    },
                    warranty: 1,
                    description: 'Разрешение : 2436х1125 пикселей, 458 пикселей/дюйм Дисплей: Super Retina HD Процессор: A11 Bionic c 64-битной архитектурой (М11). Камера: TrueDepth 7 Мп f/2.2 ; двойная камера 12 Мп с широкоугольным f/1.8 и телеобьективом f/1.4. Корпус: стекло, края корпуса выполнены из нержавеющей стали. Face ID.',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 4,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/kGIf8XCLARka-0pmOf2OLQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOiCC03qDQ1lhdNc_wXJ-7aRbmJkq-aEUcn50TQvPaeHtHMIe-nOP1I-IDkjE2RQkhhKNr7fagYYsouTgJ-WJlFKkXHdm8zRpsYG-zIdD8Fpc2LsEFD1q4h9z96oQsnda2o%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cx36BdosZe8OhLGUEbASJ5vYc0dMq_GEV7-lB2Hat5pEBSgei6KZkyb5zma7y6sMB4jpWfhIl7lFLAHK2k7770IyWGuMTNa9xQV1CHYKRLMHg2aq4Vu_3tuaViyy_XTuJ3g8d_gs6cKqgbr4h0zK3HEHxKOQlmhA1WStWdlCnFSKAe-BfbeiI7bJDyzerDTJJ6k8hULct-uXl5ENnfhnZHEvznTgObs3Etpf0UotIfwNFwlaTA9U7-7LMcWXVGff64xE1_yaU-4YgXW8rrazjCYjAkw3ysB23bW5TqWT515HLPpaKTAIiDuNMfVndsvMNM5S07pcsLbSkO8Ak0SO0EbGs4t1k3jb209dV0mt63ICJ2ycqBMnjn6QpwpMH19uUjLv1_2-QF-s9FJS0wxoyaYyeTuGi9Bc_CcNJ58M9bRo_boa3focNP7XW5DpyFvUL65efFG9gqoeMOQjFNOONGoSTSaPnFQsk7S7GO5_yp96Np-bmMT0VAn8FfefJWKaHL6DtEM-mqE7EvDFb23O_ZA0QPUtZodGa7HLBrkDNKkaUJL-1mZKOqgKA49ZJiV8ET9J2cAzE2JIAsy4Hewyy1jxlBoXzerUhKjTrTONLib9EldrMWhLfax6RFvVrSy0RgXuhWb6PTEPPyQm-b7C2cUXwUpMDVbQ42bnu-Gg64XxFUD05GUpKE_?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXYe1wFMELulNAxaMbAtKYoOEGNiU6PP6RZ4Ki1WcKh6WEGjmMvuxDSU4Kd4GBtHozqH9UgGSLyVGUOzsbyAwq9dWan17lib4ekPzGieYrE7ZxQQOsPHj4WdGDXOl1IJflCQs0h_1lDqbOZFkYEAuBbHeZ44tbCBmE1Q7GLBqMvWL1YxOCE3Luo8D8WiFyuzsbmN1pw044EF1t5R5Q60ZZJpBi2w7zt0-mTKGxipDqiVTf2I46hvEoaHR_d2QhpwXJRgSUwd_sVmkn7cIQ0dhi4jD7bie6ib02w_EAr5sR3nCXfu5rHbEf_g,,&b64e=1&sign=3d5459b054947d0891f863fabb03c290&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZEfO6FE99TupXIj_wjXfaoqnj01K2PyvaB6JL1jhfuwuYSBd2gq7yvxkNTV-fvDh7h37NrUe7hhQFRDiu-v41pYDqbU6CnoMrNfFJ8Ll51XuENthuqS8cDrEGXdfW5RfZVGln6rhtwt0O2nWjOTgydxFOANynT-jsdnDf_n6X4PqGrg_SbTI_dKnCRIIKThe2-7-Ncc6T1FT_dEzPkRO_hwBg6I_VxeyozMscR_Xf4kQBoveU7WfdDjcpricTVmXPN67qzX8Ve7WvsYFpd44mbFUyHBykZ82KE',
                    wareMd5: 'a4y6Gp3w02SbLGtiRMbc6Q',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256GB',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mR7UDlM_im8_EYiwv3egYLzJ0qyJoFLaR70WeSk0KPNN2DGG0YwgvOrHGHM5ZabV1Px8Nn12D9zLx2Da2PGVukLdl31KfVdqT7q-QPWj7Fx4LOTdftygw9bRDylohykpTIUXpcpty9JAUvUwWeShMUExvLjh0inSitIlSFFNJZJU0LOAnxsNKSUACGT7LcU9Es2_XzGgkfou-UABGcS0mpHHUJvJdomO5BY8p8DqjZ-PnWmf_AsenJnEiz8JKE_w6nzN3UF_ynEvG5U8TtX1Sho6nC2MvnKgnPmPPn3rE4gt-rf9r0-6DhMuUkXinjXDsGKvHdo1jrir-K8bwBDGe4916HV93Js4ggL4wvaq7A2YTlZVYEJSni5mcvkSNHDwpuLMSWpFh9pqC6EhAVXgcFyBDCT8fKb1OLJ6Ljt4jZhwySV2I-hkSiH-Xdf83RQaYVEAj-oFJH7YXxb6EJzrmBxsaCXQvxU8Lc2QdBN-7uIchrPtvLUkRYrsw-RvDiwxL2O8DTTaVeynGBw-QDzHCzcV-lGykxzcsiFC5y3xHnHkygVoty5s3AwU-9pp_tLYPJ81P7E80DmKclDBCD5UxNFNbqIfkmHKvgWMy8-DfNjmkTCGn4b_1xqxzyxqHKKy6fo5aGOPx6UqNoq9DObILyJnWYO9dxnFeInJhl08fiRPoPsq2wZ6n00dE_VM2JgodN_ZgZv_j2btV2QqoL4mILZnVMzuz3IIbw4asjfQCDUxdJZ-09abajUWqmJZQ5YMBAiYV4ufQH6NTBlxVOmObL6r8mnn8V1F6mlAEWS3kI9YZwudEU8CKQeSlJjtzFZH1KOguldT1OeP_bq-qAWw_3a1vYEfQLcwUfmU64JLn9tu?data=QVyKqSPyGQwNvdoowNEPjcM8GPt40qvJ9-NmwvQ2hmP8r2xuoVSLrcwypVOpCnq7__iGE4JbAUX8rQApINAGghHj5mgWjOTqEr7a9EH4VQk6fKe3GjZ8D7afhyAHqbxV46IjwmmsKv4ncLdt3XAloSV3Z2EmvNTvQOz9uAhJLm5gkBLlg_348QipCKbvF_j8HHCRad9eBaDFnxEjxmcCc4bRwGtip-9-CsKN5lwype63_h99KoCcZ5-QoHXl4lj59SbVjWqdk4xMHxZo1ryvNYNAU5emHpdhY1iz39b629RNIUBXfy00szRufS9drOx6NCknoOZ5HS4rrkhmsP29Zwqk1jfyWkxViwSFovpTggjrOhfHJJJsk05bzJEMGsFs&b64e=1&sign=8ea7fac17493dc18b747ec87caf96013&keyno=1',
                    price: {
                        value: '88900',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 108733,
                        name: 'LOCCA.RU',
                        shopName: 'LOCCA.RU',
                        url: 'locca.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 534,
                        regionId: 213,
                        createdAt: '2012-06-20',
                        returnDeliveryAddress: 'Москва, Барклая, дом 6, 121087'
                    },
                    phone: {
                        number: '+7 495 792 08 26',
                        sanitizedNumber: '+74957920826',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mR7UDlM_im8_EYiwv3egYLzJ0qyJoFLaR70WeSk0KPNN2DGG0YwgvOrHGHM5ZabV1Px8Nn12D9zLx2Da2PGVukLdl31KfVdqT7q-QPWj7Fx4LOTdftygw9bRDylohykpTIUXpcpty9JAUvUwWeShMUExvLjh0inSitIlSFFNJZJU0LOAnxsNKSVqt5FCfiPblupr2Fn2Mp7YXRINNhLo0A6tIx6G8CVpgkRZhvPX29pMc4DHWLiD06GLiYxJKe4f1qyfM0PPjd9rMXnGWkGRLHqCcK4p6mlIvWyUF7MJ5sku7ISWdyw7orogSUDqc1VRJaeTp9TLpZhBcHAVY5xI_HPk5jHktIr7VbsR4omMXHEJb2vXzW0ascriMz2CiYjjUrShFo1rvXuQ_V9E-gGYKqGcd6SUyItIDPSkIfZg5H2r8fmeeGi3sgo8Cx23McmuvQesym7UdO5x4Ivk-_rJF3-m3OCusHQiTfczCG-8-H2oYY4THDx2ZF41mXSiYyomaxVucvuRXbGFBSJRasZeqYC6ktSMCyRMSdfsLNrX5b0nM6updX2g3s2SsDKpKanPZfeHknVmH-q9_U6PnW4MIPMq6pSpbEppBJZ_kswCrvfsXUYRbYS_y3qRZ1Qn5eYeEVGHcnH1dtUMnZlNGQB5rPjj3tBNP7d_uksyi7AsKLX1oSW_dYi0m3sjhDQBl6rsMMeG0Yd0I-RPV4Ad1_afJ6_h2T7mTPhhw2ehc6zJcDFvHisrC6DIdHb42cuAZLmPXvhAMUNTBedGKtoOLSDawznHFzmaXxDHnn2rbbWUtoIm0_L812cOHTqrk2Pv_YMWpF3bCjb_h3PcVWraVLCSeq6pSC1Ntb5JAc9dME7RW_IR?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-WjyUPNvvoZr1xU_tGzn7hotVwF0yichuXdchENYJoe4LrkasGUi1CLe5csSdR0b6_qbszppPe_j3U0bXtN8BgcbDXsXyED2cMaG8fUBK6TtZDpjFYalH4gCvyY8z_wi8uyHQyWxkTfaunAeSfwABTDRUB3K5kjEn9a0gm2P9TAQ,,&b64e=1&sign=1a8b455f57db3ec95fafc9f5f2bddf6b&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '390',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '390',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 390 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_SwoTfeTt4pWdDC198XRhdw/orig',
                            width: 103,
                            height: 200
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_SwoTfeTt4pWdDC198XRhdw/orig',
                        width: 103,
                        height: 200
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_SwoTfeTt4pWdDC198XRhdw/200x200',
                            width: 103,
                            height: 200
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '792-0826'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '18:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.504079',
                            latitude: '55.740563'
                        },
                        pointId: '293004',
                        pointName: 'Пункт выдачи заказов',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'барклая',
                        premiseNumber: '6',
                        shopId: 108733
                    },
                    warranty: 0,
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/a4y6Gp3w02SbLGtiRMbc6Q?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOjxRQIla4LlE_AQgIOq-iIdo0pj8IpFHfyTVJCXj3-pXZ_WIyB2OJwicgDm5ptgNhubalZYU1dvjMBK6KxjZv3bCUKc7hhUqVsJMhDJyut2xqjaI_TljSg0VeuTni0oIJA%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cyrKPKLPfb7k3L_LFGbGJ-GFYEzxpmxul8rbdRsbGyhStxTDIbz3hBFwfeQ0QV_RhjlNHCnoznQsfK3O3SH8qgvip8SxWAuxCLOA_kRENoKZxPLeIEeO_IkFHxantrQJzrFEe2jVxkAut8FyQNctbGNwxrJaY75ChWd4aXFO0e0_3E3_UQ3snp7FeCqFY-FCIDaXT3W7buzZ3j84PylPR7_em1dLHkdoyPFs8CuuqO_7n4ymVOvFAD3tqHvUPdwNQ9x0UCWqAYYIcbaIjJL-mqeGaFzDBHuKGeBhkN_NngkN9WlHKlhjzrgwDXebYwdt51dUWG3Laq_YwJMMsqp2Dt5bd6nHauy6VDv7yB5hm2Ct9INDB0syn4c-5I_wuuy_KdPQR1gK5Dn722syDlKHHd7LjHgrc6hzavFKNlBwwCak9obw-mpLbI7iCZshBc7iVaAHaQ22sd4LoAct0_Tbwxopd7X07lAPOfdt4IHfoi4a1PfTCYt6d1WUpLXT7fCncWxZ1D8OmQZfnfKuJnSCGo1IL8yM0KMvx95HQfOogiq5_DBJ1S-UMLUs8JL6uGLtgeSiBlJMZLTm4tmQrGG0-QczRpWkq9bV9qv7aGYCldc_YHMR-75PifFVdSKiN0Lzlv-d6mZ3zPlfn5CwkfIpqj0IEnq72S-iB8oSWMopzmJZNN1xHFAlQ2D?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXv2QABCB_kCaGutzpQli42MxMaw4wl6l8GpdFjj0ySdrxQixyH8g96zbrBMv5KFQo5mY3fflVgIuD20WFems6ov4D4decSKT9slZxTqBb5wccBTqrPXhxnPFCJ8pcokQ2UvtFzCu3bfxUWiLmxjDWfkVLFj5XHJvstkmLlKT2xxHYKKHwpmkx8eYmXB0DNV7Oe3FcbX3tjau7GtTQRL8zSvIDbnHuyWVmE5vZ6ro17pcijuWTUU5VdZSuHJMz_iD6lojAl3GbUotWzw5_Z6sIWClZVx8OTOwwbzS707NzAZWxcOB3EfVxvA,,&b64e=1&sign=c29fc66760da08991fe0247d2da3bb5d&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGuGaK6ASPCHAzZAu3EXG4saGWPUcySmEgpQX0Ha18svqTixVX1Q1KDfwW1BQk3GQ6XlJDPA0Q3QoDD0ihu1lPso9cjLTLhuY9dqgAP0aFqbUQKnZH5lshgNSj86cLnQOGz4AlfF2_apbPBdrweJRDCOFcctq6P7PeUa28L8Y7V5USczhoZhMpbdZKxZdp09j7cZ45OpZXJL17lRH52vN3CB_uZz_2PMkYFkSv2zdTqTIWzo0zCB1VRciWX43GtaAsg9FXBMkCGWAl24e2j5ewbS5wPX43u1jI',
                    wareMd5: 'Ur5ho5jRULlEpBvm1RiYaQ',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256Gb (Silver) A 1901',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4cbFtLcv3ob-WZTKDKaoYF2RezRwUHdgbro-w-1o2gfikIO_QT0vGs9h5AghjibBR5maDfzlEYmFUHPenOQCVJj8FAd3MLz3OUE9s_em5WFRqXy9oPtLauHAY-xRmMYsoIc3vC_JwN5K15eFz7Zisdhpyjjc_h47hlFYskjnG4ETRGzp1k2PI28fRlMcc2JziudT58fYBE6qZhuRelkRMhKsM_eqTL7Sr-g9Sp47b0gRDwjNldznLyZcToieDEnsfBsbdIZKk-_kUaYaLK56aHCsvix2Shxv_YP9SzHtUsalaIvF7_MdDyasQp3xmIErWFu70nJ2Cb6DK41RaSQQiNBfzrV-y49FWrCDOOYGgVYQbpAg_n383k8IPpERFE_uCj8ldTyCImcIYZZ3XuQ25ZWP-H_bDn-w95jjDJ-TLi6SLD5sBEfZdWWY2JnQ2ld3JnMxlDKEEZnytWsKIhi-J4C-Qss0g9ReTsD2I7Nuyvv26My39QGZEud8Nj9EkKrAbiImz5K1pS_6PBKm3cALrD8pAUl403QidWkGpkgEy9mLs1St5mWTYRCQ_XfJSvkvCz0QqUsM7Ce0NlLPVuvuudZbod-pshWUHzVwXRc_LexjHPTtN9TA84vGbO1oRY86dGdTh3PpJl9cd-C1VoiB5q1zSf5XWfBq3RPJLaP842sClHmDzQoXE1FSzA-0Kd39LA,?data=QVyKqSPyGQwwaFPWqjjgNlIMG5RYs0Fai0g5zTBYZJd2vVPW_6UNkdieyMFOZFhmJXYXtaRYcXzOjseVf1Jua7CeazUYXaakW_86qF6XYekubEtyfpTvfWGy4rRUxhsGWMIGsPUNB2PnMkiDDBoV7opYRFPKCyJ0cfzFlmdciigNAwzeee6wDt6fVeQVBavlNOfEGJN-fD00MzJNP-RrfzsMuDvTxN0_GIwhbKBEh-XMrD5cpFqphA,,&b64e=1&sign=f2f43875d61ccc49dfaf8b471e84e2c7&keyno=1',
                    price: {
                        value: '83895',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 122732,
                        name: 'BlackBerry-Mall',
                        shopName: 'BlackBerry-Mall',
                        url: 'blackberry-mall.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 138,
                        regionId: 213,
                        createdAt: '2012-10-12',
                        returnDeliveryAddress: 'Москва, Орджоникидзе, дом 11, строение 19, 113035'
                    },
                    phone: {
                        number: '+7 495 969-17-60',
                        sanitizedNumber: '+74959691760',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4e_tb7h4VQnaY-SXHQV4ykRobP7XLMc7rhhNdOt7YHMA-KRyDCZOZ836FpzBPBQ4LTwyLWY6wtg1E_3VuFkux53jL4J1rCQYDuSkJGZSAqnff1eX2zkC_k-wBSyS77msaD0pzlybsyjEuM9CvM6az4KZGF_Dp5LnKoyt_EX5xGAJ39en5k9MMbO-5fSI3h7CIpomwPTsrTY11h491-zZmwfdLWDD4Yu0xr6Z1LEIEIhKH2ELZKxfMztsgzj3CCO9ECx9jXZZLnJgDzNiSpcLvIhsInkehTQpmQjftJt7-twr6aRYqhFrq8-2fszF0qQh7eemqv3CBRczcyOQGy6io7i6QONUi7UuStBS_XqimF2XJ2xu6iT11itg3n53swKbw_6lk-GFxYq2hf6kywnGDUSaULGTSno-7OvHJJIgvSEEGNSwQLAbbwV_hFkqwGMgxSGPoeq3aFUZ_8X7svybX9evsvnBTLaDIZCugmKqBCFIrojvC9EZzBftY_fNTmjTSDjqlErJ3uWhn1zqW4Cp_L5QFsJL3CJU8Sj-K0VAw-fJlUCMpE27pBtC3v_rjI3Yk1DYPjpyI_qzGMXfkqXzL75Iro07Jq0otEazm9XfYT7fZ917QvTOAgO8JwDto-RYpyZPY5oB7GAAnNbExfqYDuwmOLRpbswisx0zEQM_dEywAL2qVb5UbX9dm6nno3XzYs,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_MSkqXFoX4vZdNsrasez4x9cJoHiLtTl-IKN44_LonptX7Abx5kcecDwmP9znedawAmXNVqI0TU2lxNZKLMR-coNoABOaxXv8otGxw3-Onqe_HyGabX1jZ6uZeMMPuWwP5JYLSlWRsucVMAYjedYfWagD1WOuXRM8BzCq70nbUlw,,&b64e=1&sign=69452add065abda73de49bca1d3c68b3&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '400',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: false,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '400',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 0,
                                orderBefore: '17',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 400 руб.',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market__qmhiggiegltpc0O8X2pjA/orig',
                            width: 600,
                            height: 532
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_iZNYeCj7T0qxfE1k3dLPnw/orig',
                            width: 600,
                            height: 487
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market__qmhiggiegltpc0O8X2pjA/orig',
                        width: 600,
                        height: 532
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market__qmhiggiegltpc0O8X2pjA/190x250',
                            width: 190,
                            height: 168
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/225051/market_iZNYeCj7T0qxfE1k3dLPnw/190x250',
                            width: 190,
                            height: 154
                        }
                    ],
                    warranty: 1,
                    description: 'Cмартфон, iOS 11; экран 5.8\', разрешение 2436x1125; двойная камера 12/12 МП, автофокус, F/1.8;память 64 Гб, без слота для карт памяти; 3G, 4G LTE, LTE-A, Wi-Fi, Bluetooth, NFC, GPS, ГЛОНАСС; вес 174 г, ШxВxТ 70.90x143.60x7.70 мм.',
                    outletCount: 0,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/Ur5ho5jRULlEpBvm1RiYaQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOgaHjxxppMobqpXddYgt48EoLd3mfMZc4eYlMJ7JBo9v4fK7xxDRVHYt5UPjZVW5XHPgafY5SnaxdoMBrM6TENDHvun8vzecOrYw_PUIol42AL1UJtlPaY2aRD_odp_V8E%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cxJpXPeCbVJUAsJ8upgaaR26kbBflWxOBOOLqDZlkjAfpmvr4zNissvXFCAS5Tvz6Th6DIAq92KxwABSr8gAsPFPWxZmoLKnYAQXXDlvM36Xljwf1y0W_Eo08kfdyCSp0aIWfGsWncYgzvWpX-TY-VgYHdFdc7Ag8OZEvTBECU4pI4FFASMJiCvVRlVNf4BOeypFzW_kJZUBfsE_rdDeXtmmOEkrWCPntIneTyU3q-GmmNyHKbhk7o33b7Cb8k4FYe7Th0yNXNzMqx7YGjmyl39MWpGZuTxORiM36mIU4GBnjzfTh317fDBkM8kv_B3BtkpWb29UXsxEqp_W-wM3sC7q0QnEMb0T-8au8XAkPb7RA5vWCUpdXRAbgZQNJgsV4aZc8UevIIkAynZqldWvfpJ_U-nxlmoycQswciClBXm4MwMN-1438ahhlxcO820f_NrbYCuheOxqvBiEng_0nKZdJG4bTPFqArpkBkghpJVs5CGQP9cT6Yr_GxvTgQzZy1Z3lUO9f4NSCJklyTFwn_m6O2H54pRP-mvF9a-GACRbKxnbj6ssTzsQDFtymYSVz1u3Iko3wRTqn6myCtTUKGdp_vxHM979ZBoQKNH7GV-eCaDDwqSn3y1IR9nllEw6cclOR3WvK-Spf18GHQWIyEk67tgHsXUIHJPiAONlcDv-LOZCNzhNcuo?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXMFoNnSdLdJCQmV3rskYsPQIghY-O3-hYqYt26cvHgL6WK3sPk1LaiU6R9G9EsHbhtBWU1FIvx7SHQmhN1HE84Hhmw5Xrb6hWs8Zdtf7xUqffQiuLHjV-7SKDMGiIMRrpyKJ8cuJE8yWFFHBDjQ6t-40dM3aWP4xQGe8Qq3nlZaZsxSlY1KzKkTkIJ_uSBixHaxR92f1b638NdZ-lPtksw-LtYO-1NWvXzNkApEyIdVq-DkFlL5zoURz7oJlFYgfLmRaClc4aXp9DLSopgj7DMLnPYutXwKJZGL6uXmbpPYxGktI9MYqFQQ,,&b64e=1&sign=dfd61c432603803c692ffc2b5c52f893&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGxLQh5HgwklPtxGUnVC0pnF-7rwW5m3oNm0hjlU4vGA7P5Eh0J4bP_rlC8TUg5-K90oLbBbZJMprIPwjHKop9Hrl7_vEjaeaPj631pCyQxszDylgD6ZLMAfcEtjFo3diBAVfuhUMk01y304XXqeXEe1EMkk6Vayv-SfzE9b9cBBlTcM2xwD1rGzXw51fEYq2ireSw9CZZSHumtuj7Fvg8NEtwEgHABIrnzzF5a8mu-Bw9Kc9MqjhBed0wj7mfTLUPcJnSz8zD98iOBCfISZcLYv5cBpXnnR7E',
                    wareMd5: '3WAwkH2D0lwdikcRn1jlqQ',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256Gb (Silver)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZihEDHmDjHFAwN7X1qt-YT760ilDGsv_9Lmyf9v8fYgS1FfdYW2dha9DJrf9iuwc3bAlx1KK2ih-kioukDPxiBnEuYwfumv0xC-yE7LQXbeRsK-txTQvxA6siEJ7EUUdpy6laiLWFgA1sdH1qZwwKIDW1Ca8qA7qeqCBDN7EXMxtUCF3WXk76mdTFtenAGryZ9wewdqmrz5E-nEFIoIM0HIpMm42WxK0m8bOhY1Ir2vIA6s5LIwm3h_PE2_wSZykAgvXld9bAJ5fKmJc0oFWfqjjYynhK1HGoOBUwsL46Gj44h5ripGFbefNNyEqouhPThqNb6Onn-ZT14r3luWLifXlwMII3TXB9w2uRHL8M6K8h-BdXR_MzjVbSj-KSUdH-WJrXU3xgF2LfxQTo_48raBOFl_mW5TUj2PD-3wJug-71jnjXKYiGlTfPAU3aa4VPcdJu4lvOkcDaccYKiSAHfZy2wwrLJO-D-7GAVevYW58FXOqssfuFqhySc9uSXYh_J2x5Vm3kPn7wayA4b-OPzzJnxih-sb89eOQAVcdebY4uPEoRhS7Ef5rmb8_VE5NnTqMVjq4rNn9RtsAO9xZKKeh60inWqAuZw1TlUUEa3Te2fm-_N56F7IxR-LM7i0SVGvsJWARoUAqfMvn-p8PJm9yzc2GCGgTEppbZu6k9rsgmTUzJTM-C5rYEYupccuAI4KPXNpIdpgMQ44GZEi1UElxTvgOUp_bOAA9uw3vu8og8PJfearwZsbulVZYw4Q3Zs66FFqEIkej5KAJeyOtJDd9slc2Lz0Q8fbeSY4nJSx7ZaiXnYXJqsWRVN4xV1CT8At9xWs7NgO-wdHeHAz0Q7G38_nhi24juvEL1T4MYPf?data=QVyKqSPyGQwwaFPWqjjgNjD2J3UBWUhNQ5CCUGKjrJ483ybr5qcXg0Kvxi_lqXm7_NhgLXCSRR3R--rBcMEh_OLniXbHWI7y-gY0rwAFzGujDYVQ58-WZalF3piYgfwI-qZ0KPj5_v54gAirKU9GxPZUVsFM4DEZow7FB2PM06FZ_UfCzYtH4oIHoDaGkEprRCdnFVP1OxPQcKtS-ExUHMEGXBZJ53cAvcdGeWvR2pisj5QR2L0Re7Ex3ZHoFwx81oemHoxEEjX62PLCCU8ACgcm_SS1fnB9AYa2qDnA5Q4Sll5kNpW9a-k0N8MRnJ5UZacK8ZBj0QmCXmQ9rSCyMewOCVLhT7wuCQKhQs-9J9giu-W7TUngU5yrt6m8Fe7ojwearjaWTaK3TQcGrEI2ja5V_C031fn1QBIpIbu2BVfKnS8zzx41JQ,,&b64e=1&sign=a33ebbacc7811bbd6a1b758f6e4b3392&keyno=1',
                    price: {
                        value: '85950',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 228797,
                        name: 'Teek Store (Teek.ru)',
                        shopName: 'Teek Store (Teek.ru)',
                        url: 'teek.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 430,
                        regionId: 213,
                        createdAt: '2014-05-14',
                        returnDeliveryAddress: 'Москва, Сущевский вал, дом 5, строение 12, Торговый центр Савеловский , павильон П58, 127018'
                    },
                    phone: {
                        number: '+7 495 151-10-11',
                        sanitizedNumber: '+74951511011',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZihEDHmDjHFAwN7X1qt-YT760ilDGsv_9Lmyf9v8fYgS1FfdYW2dha9DJrf9iuwc3bAlx1KK2ih-kioukDPxiBnEuYwfumv0xC-yE7LQXbeRsK-txTQvxA6siEJ7EUUdpy6laiLWFgA1sdH1qZwwKIDW1Ca8qA7qeqCBDN7EXMxtUCF3WXk76l9beXEJWKYWpBqmM4RGgrdVPwc5QY9WlRP-Tr2BWwwlUolFSI6mI_uZCFl0Bc5VfRyr-O0udE7T8r8xgA1AJjsIQWhQbIxZG5pYIQ3C3QrH1Cdn5Cj_EpZ7vmI3LGztOG9eXHg4XKo2FJuIVv-X2OWRIbkZIRYDVVRzOZad84oO9o9vOG9HDqnR_ALGWWv4wjjhHawGLrjqXnag7wYMm7nitZDvKSzwfc1IJDsutcJmlyYgXvMdurmUL3X902WPJYcJApVzijKvlEuhsmIoEyLFobpqoHvef_561-dpnyLatjerUx0O1LSHU5sOPfIzUxegML4O1_GLVNlvcLXnDZUJesiXLgqZV101ydKbe_58IxI2gLKh9scrGZGm2pShNPBYAKMZbYddMvgiwb8CS8MG4etLU0bGwOwHWH09kGOPimgFgTbzbwIv44ojYaqrb8dn23PvXon3m1qq1EK0sAJpkoJ7oLW5Gjwn-ZmYhJV1E-16l_AStna9259UDDH1bj6Uj7YOA2Yi2P_SkX8JOhUDfyEUletd6LSnyk7Fx7q2_otEE6HieSANp9o5ztwRTFSpARKqq_X-sAnxMVZpvKfIvIFMvg4p-IC5CXPoT3LghyaJ1AdfS25VSukoKZQhBz-H0wLlAuqDJMfaNJ6GcQlATjGFqqoRJKVKBmD24mZuwEwZICzfQJN?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_PO3TMsQW80qe52PfxJjoRhY8mX-gHE6qvtbXJjZZ1YBGcDO44dxhZJ8Ogj8iFGKZqVGWvlqTFJIhbtQPLoB8EPaRRGCiCgwO78BnDyxcYj3ox8-S1aVZwRg_mLGTaD7i0FuzV7izNsKWq5pPxzdC04vlWrXc9pzzAm01A7W48Aw,,&b64e=1&sign=bf35de0add025a356a67410f32837816&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '350',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '350',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 350 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_xTHEPUBtensuoY9E13mnZA/orig',
                            width: 310,
                            height: 450
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_xTHEPUBtensuoY9E13mnZA/orig',
                        width: 310,
                        height: 450
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_xTHEPUBtensuoY9E13mnZA/190x250',
                            width: 172,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '151-1011'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:30',
                                workingHoursTill: '20:30'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.592671',
                            latitude: '55.796192'
                        },
                        pointId: '342866',
                        pointName: 'Teek Store, магазин на Савеловском',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Сущевский вал',
                        premiseNumber: '5',
                        shopId: 228797,
                        building: '12'
                    },
                    warranty: 0,
                    description: 'Apple iPhone X 256Gb (Silver',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 4,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/3WAwkH2D0lwdikcRn1jlqQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOgtPT7POuirPJmHjUdth2H2uPkeat-F3ghhRDPB0ckneOnNao7efhqlZg72_0djqIavM5DMqcO5c0G1wsaaNKWNbpzlu68N_MZ3wgMBu5KrO0mOLjRH1gZAHeQHJ4LzoE4%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgVy2Z-7dybbPrxJjZ3lKHXxTUpfxbWUTGWdD6XOPTLupHs2rhqgPp8pkFUYGzSfFqSsSeETA8Wn_Eb8o5BHrccup7mWuvWmwALGKX7VdFki8eHfetTBrEuoyotclICkqICM8tgAtDroUIfLsto47ajG2GGcN3sNZ1cpdDDz4VRrzRr2UIL9qnY9ZStSXfrzP1xamniLMWhVPVlvMfry88rEIQ0Q2WnMdCOiflf-fXOCt6etZux8tbRV_Y2Qqyh0vdN4qdnM79r1K6OWq2P18QYrgGO3I2SUkwo8P9AHCTRgbAipHCXv86cLJ6XKmI4djJcv_72sHArkpHsXFSJqpbR7lbTURPHWXwYMfstUPdqndp8CFn6bMOYTY1BM73Xd7j7Rlwi6W9w6jbBGZX5hFGTQKxFZ6So0NtTm3qA3DRw8H0IISIonmYoihVF0GDxSeQSMaupi_eBcegn6AX6RS1z_gf3MRRdoO8GGFzG0DdQy9B_Qmw9aqjVTIDJK1yYrjH1COa1WOn47QsFN-zbKnE68nrJGP8G9dNlFIGqmC2C2PIzgG-il9CoOrWbTnwImP-GC0r3QqB-e6WuEl5ipv0sCfnGi4Ea2O0K8PfiyD6Up8OgaZJmatSJTMXVo_qZiBl8zPLCLOF6sQg7ckNx8v_sKbjAIEaX9MaMtezdCfJAdv5x741OQk52FAwdkxA7ao0ZIuw7lLXzx0OVJyMZRpwkYAn7fIR0FfIgUYst3Vif-9ZqC9RYmFJHLxccVEvxwOu0-x63xUyrVwTi-DJj926RordBiOM9vC1sSDdI4UMO_n0rpVNOA5VczNMTOOF4yEJLwRasKxxOvZDA-3ucjSc3rp6JggdxFhsuCRj5gEW-C2i5QFDjhO_P-_pRyIIL0yngIoKOsr56oV?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-5HLjAIShU3gpSoj_uvouap8uN_SeIK8BhNJtpHQKlVBI5uIo3P0Zr-MLwHFl08Xd7wzQE3bFW64Bc8SSz7kUSIDbaqqcAI3GL8r0tUnwaXknVmnWeRIQgXj1gfG4xV5NQNkzgIv0bRK5wkjcUCJtQp3o4Z9ndh8OhqZ5Q-en6ooqsqsr4eXR4948-dHuhzYF0wKmaMJsov4eYFc3bgmW4h-TUe7v-2jlmJmdb4VPK4GxRsKiCoHYSqiSRM3Ze-aIz9zV-rLVw-oOVFb8fCpb6xZgnUoxDTqxtOhx9lMG0Pz_JZqwiuNSjZ5HbG9fs6qsFQlZvoWZoo5Q,,&b64e=1&sign=11f5177087088f4b38c30aef77c7bbb3&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZFqlFIzDDVJLWJHCOsBI8SjLon_TJndEmbQBTOMMxdi_BZNhgIaMtdVmOy6tV5oM1F6xUUvrbBRgPPwZZS9uic1--BjD5Cd5aZbaCRpHRKQiBAUoKG0WgkLt8pQzrenB1KFbuAjdoBPCLm2qypi2-ApfIR6LM3R7qHRm8TAnbygdwt0iRczGCMRmKuBf_LFQgrIwqJcOSf6OUZHMJuWRaHDEvukN4RCK3ZyMFtkx-hcI-fGTMVd0ghPBnW0UbxLkCn4yG6ogQ7a70uPrvtnTbiADzZN39qqwi8',
                    wareMd5: 'U-AmAHzgZyPsc_iqqxyq7Q',
                    modelId: 1732210983,
                    name: 'Мобильный телефон Apple iPhone X 256GB Silver (A1865)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4cbFtLcv3ob-aaQ3g0XIlHK4gl0gBAWM9Gow-53o3pEWEwialmgCpk5O3V4tdVc62eXSfcT1PXF6Owb1lhBuF1lSzdSdQZXpWgWHWZGC0WuXX80mk-e7dCoeInf8xNzkyarljMZ_Ua6SONBqIPii9_nW1MV5--y3KfaDW03VMFWrS_nKJmQS46hld2QLHjh16w4uEhvpgi3y20VoQY4AX1ktjtoURJMdvZLhiRsRGda11aWLdXw_wZWgEKUa5eOwhiYuAoG7fLFG2tZ6ldn5hWZg52efLTGsivLKcCIYCX_xwTeb_CBdq-abjzimp0sh5nJMA6rGOC8aOP3upope7IQhk3NT-e34gy_B18WprfHVRgfZWnvGB0v69KtD0VbAh7D0HI1a6Sr2siBnOjVUCU7cC4wE2sKfYMew3x8uuT7R0MoFQ1M27Lc0S7uQFIeg4312yWu4GfHd2_E3YKqIXJ00Vj9EKpwgFhy1Wv4F0XHoAYnEf4s1dwy_zPTKBtYC10HRlR1FZ46jiPGKo2Qcri79QakKwEgvxpxyAPReLck--SwMKy5yOVtTPxl0PT9qjtLZSl7Gq2LKf_KBTFvBBxzEhSwDqKs98z2ayQw7UkpvonxlGUnuTkfxa2WHPEl9WMIpIV_grWeE9WaTGYJ-wzPUYKC7JEL8QlpnOc9OThq01aVcUZ3feUb?data=QVyKqSPyGQwwaFPWqjjgNg9J8lIJ2Oz9gk_rKEXyxA3xLCCap9MlHYLA9QmCyiEDOb9USHfWBqukiqjB5d1CJxeYsPsry0r7HNLe43liS8BPtnJZp7Pb7Xafx6vVulrKZhrMom7lkoUKKQdXuy9pTGZtXJOQX2wkDQsloRe24zXf7Ih9K-lrXLKuUJysKJmF69RA8U60TkjsBWB24OjrCyiI3CIUPIu7DCxkHDb7ombNy_EvjVNJ6cg6zNPf4tZtRIyEDjMi8Sars3bL_FPeOp8RNs-_BjIzMlR7NGxTH0oyfaP1P3cCssn_QKq17QEekY2QrPq-t70BFemCqqsW7F7A0BE9zFZLLUx3cqysDii4mApQTIqhou87h-5CY4-XFvxwOwQMrAp71Fn88d8ydoLdU1fjhWBc9HkGE1uL4phx_RhA-JFMTKpwDD1YhLbgEwGJxZPY2S3PqMOs1Plnm1UtHUolbCOYpB2n54SyQ_iuUURf1OyUvReJgRCBTHJ-S0kNIcjlqRmwoeZz416Op_iRmx_pxs-sIPUMPV_soTrEfnN_PvXw6D1ypysEhW7NlcC4dgc9VhVPdUfI6-br2-kLMOFcomfP8Nyl-ihwqaxlugn3U_MhkVBHxAWyvy3cmVBwX2Sr-iz_36cvMpJgZ037LzbFNkzTUJ7CX6QYygzxGojIFjJNMd1YOzJ6SvFcOAbUSjLPC3F-stvZWcWHV1pxStnhiOTF3Qxsl8tMVyc,&b64e=1&sign=cbce92e8ccb37d41c22621653f1bda8f&keyno=1',
                    price: {
                        value: '89310',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 38615,
                        name: 'Mobilfunk.ru',
                        shopName: 'Mobilfunk.ru',
                        url: 'www.mobilfunk.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 3858,
                        regionId: 213,
                        createdAt: '2010-05-18',
                        returnDeliveryAddress: 'Москва, Шоссе Энтузиастов, дом 31, строение 40, У выхода из метро, слева от Вас будет «Торговый Центр 31». Заходите в ТЦ, и проходя его насквозь, Вы выходите на задний двор. Как выйдите во двор, увидите вывеску «ИНТЕРНЕТ МАГАЗИН МОБИЛЬНЫХ УСТРОЙСТВ И АКСЕССУАРОВ». Далее следуйте по указателям на вывеске., 111123'
                    },
                    phone: {
                        number: '+7 495 215-15-10',
                        sanitizedNumber: '+74952151510',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4e_tb7h4VQnaXwRxBRsrv2-PyUaPtNqJxkUekzeqkB_BgU2Yvs9TXHTjtRufRDuj6OnBLxPqihGbedn-Hip52h1YUzKHxxJ4Nck2GEwK-21CUY61DGgxmH-WwG97Wo-xE6tFjpMfMCCfKb197ttIKDBztn0VqaiYlHrUuQsC4CpYnUICcUq9w2KLSn-RTM83zxNtQ2A2cCS30JQa5IF6VtjcRVRU0bJvb1e6IAJygoGQKNBxPOiqtOD5cLLgA50uRc932GQV2DAsvt9ZtsqmxGlUVSpweVhcqdXhNmfYux07LXPdSDPXouwTYooSKq91djRN-w2uMWgrAgQ3Gq2mNug6KIuNex2t7JaokSiSNyXx5Ed0BG9AjVqN6hmefAbXVLjQy-yR6UHhVHMl8voJtwWRaAwGlMrZlQoLtKtdJppwXZCfMcg9s9v63ZV7meY0khn14DrJymWwufvFt0pJulAp-NZl75YidQtFao9jF--yPRPETeBVNnNVXwdC-cDlevRFW6ObcncaWonsWDGyNuvnBbK570UEybP2Qh-3SvI528cgiHgHVzoCjsvOrzWpzeNWqjTR7EQ0mM9cpwEN_EKGhCYkMeYU96RHb5s9kCf_dlz2x5Ro9ZDA9mLbhsJBfQe78WJjNyTfQlKW4YRRlPOJiWyJ74Q4SblH8ZFcZXyPEdZ-Qoqy0V_?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_f9Olgz5lysvPiXYZN3MoPZunAgvToBt7wbTlUXZzpHADusjh8xkiEZagJIQj1HNqkkRXq1sDrQEOoLa7sXrlgG6NbYn1TwJCsrlLD0wlnk2aHzIwjdw4nkGBuBwmrH3jWCnrzxd0WJ6kL1wTtfsCe2f6c5zGAa0xyxKuyZLnkcQ,,&b64e=1&sign=34e668925cc5523117002a87e4402841&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '390',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '390',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 1,
                                orderBefore: '23',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 390 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [],
                    previewPhotos: [],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '926',
                            number: '963-3384'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '19:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '17:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.7517',
                            latitude: '55.760003'
                        },
                        pointId: '291016',
                        pointName: 'Mobilfunk (м. Шоссе энтузиастов)',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Шоссе энтузиастов',
                        premiseNumber: '31',
                        shopId: 38615,
                        building: '40'
                    },
                    warranty: 0,
                    description: 'Вес: 174 г | Конструкция: водозащита | Тип корпуса: классический | Тип сенсорного экрана: мультитач, емкостный | Аудио: MP3, AAC, WAV | Спутниковая навигация: GPS/ГЛОНАСС | Аккумулятор: несъемный | Датчики: освещенности, приближения, гироскоп, компас, барометр | Диагональ: 5.8 дюйм. | Время работы в режиме прослушивания музыки: 60 ч | Фонарик: есть | Версия ОС: iOS 11 | Материал корпуса: стекло | Функции тыловой фотокамеры: автофокус, оптическая стабилизация | Диафрагма тыловой фотокамеры: F/1.8 | Процессор',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/U-AmAHzgZyPsc_iqqxyq7Q?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOjby0sq4CbvoaljFpdlt7dVnbuaL9EZWHYr7J7ZoHFTKF2dj0HZ97YA0AihMn_2qc-O-Or5JOcocnOK1QzoUbQfFyWZewYXBDZSBLLm6IkZ5NqcWo2vudj87Xh4hDdQmEc%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cxoGOnruapOh1iGuJi9Hf4d5iHxZ1_jJedmylpIvo04BWi36xWP97m_7b2uTJsozeDcQvT27Mjr2J19Uv_EpdfUMbFQI_34__3uMrNxaC8jJSQhVm3eM8JgjpVv--BUgzZ4JsTkhoDN4hp7ogwbUTxS_uguplt9Ai9sH0PTl_z8SuAxXPjEUObtStpl9H1Yv7z3ME8yZkuBIkfZ9j35kiXXOyqMT07P3A09AGas4dyt1kV1WN_Pmq7DlFn2axcehPfZ3gqG4sstBCcG1DxhIeMLcxOryDsvEssMLvUTgMxo93iLdC824XTbA8c2b8XiZ8df-JjHYkaPjx3eNvLJN0VF92zeK-iDfpyUmrYHk9GjIG_xR9ctE1CB0NXvediBLTD2OUVCBLa1iSekWnqbsA1oTDmkhFQoPNquEeMhaMeq45CiPTmWQwS8z6xwWgRb-pNMWh2nH4xdZp5prSX2PgqpSQI_KVkVDge8LphgEKZxBuHN4r84QTABLKTERfXx-7J4O_LGtYA6VyL_bxee4N5tgRfq4Hhc9njTURq97YWwziIL_CZmKcHbJ-AruAlMVDx0dwQRW_00brsjlQhRc3WFPoebwvYmjoY1Gp-JGdS9EcyqIo68rgpJCPR9AQkc5-UrreTlPbXb1zot4ZzV6V-t6GHfG6rExuZnf-M9upqPi_fAe_aShocc?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXxh-CxkOgE4lr9sxHO3TxRtDUX58Q6OXW6UsHFPXWfJhO29C_-z_r1VJ_38r4QTvZrCiHlnA3xWkrgVbBH6QCvT7C6DqiIWg2g-Hy93nNsGAA-gChCbAXoY8oFl9MhFrDG0vz-YNOg5QNBOY1nxn4ULais69sI4DxNY44FVIrB2bY1naYZZ1JM54gn_QJfYF5jjvNnid0p-bsdqtqoafXExsCAvhRSdUVQBr7eUllZ0NLSYizjBjE2ixdYxZ7sLu8-W_QkHaoY8UU7e6qFQeHwfebitGFG2Qtzp1xtDmLOvo8s795emJkzg,,&b64e=1&sign=a21a1aece4276fee06bbb91e88a61b46&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZE3uBHCmJ0j3UKt5g8C_l9LW0p-gZy2vaFx5KDU4yh06W7zvz1s4JtYlRehYV0y7iWt2bRONQCmPQt-JDdpbisZTsPWMI83-5vd9E9vTWe42Ee8kYUG2lddsRrL5jBCweN2yj0rpnBJyhRbnVqH0QoTepdp6aGNMKPP7-PKR1W07RBFuvxRkDdMbu7UTJ89JrXjtq5sawAHqW6JAg7mhW72DzSb46V_goZ4sGYU2PSHuKWKHD7ydsuoaJGkQ8sCWZ4eVZd5x9GhRoq_LYLOpqEl-tqfPFBp-XU',
                    wareMd5: '_exNeMYl8amUfBTuQTinnQ',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256Gb Silver (iOS 11/A11 Bionic 2400MHz/5.8\' 2436x1125/3072Mb/256Gb/4G LTE ) [MQAG2RU/A]',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ6WyT4Rt0E1xd7AXQS-FFzeIhk8_ZNs6ZM0OX5XIrXbZIyAt6AkBijySLpZByd9vzd2SL_IzWmEhnwHTNV5prPVhwstY84U87S5DRNoYmXTUXJ4HZjoFeAH4jsr7B1CmXzJMpOxijdzx6pXyOOmxPEdf7xkigYtZcB6lFpMkHGhKDFL0h21ebD1Xfz4bMNgYDJAtEpXCQ5Qu7xwfpgjHNC-UPnLTUJcNzTLHUkURRN6Ow0xgaOGS-i-EVC7rLdwxqraK_xMMEDQrfT_YQQbvTTEa5KrN21yLhqNsGtgfnjha6DFl5Nw3s5eewZWf-Um3V5s7_NWe0UrpTLROV53rmlxIhtJmD_G_6dvOy4Ntxx8xWKTK5zjYC-e8WzQamohVxY44hO0HkJ0gZV9eerhdcOT7qBvCj10dDWU9vlNIBAZ0TXXlyUP6u_x0VTsn2g_EG1MoiH8Bm518iG1AN_ki6AJ5KSgOk5yEoP3Ua2GaGt3ZTBD3JDgExwbuh46ZFC4JrllH-LYUWMyKut5aT17wrZZafKKqWlGNB64HtX9kvalPr0a7mVE5zOb3TNxU0HxBHi7X8LTRwdfJWjfDTsHY5T_KJ1P3b0fVscImdKLTsR2aiU4_JfUPLU7ne0KurD0cTL0Ko3S3JHaH8M_YZwyiaioX7CLDDVgkVu7hmdk8Fj9MSvC9Prhd-IbppqE_4lkr4ZPbLDuWcKZUFq8NUqM1DoZHufH8C2Ionwc64VHDgRSAoBjrbObsSMkT5Z4_xFAnkhuQPhhRrhSL07p2baYyqXwouw57afFDZYOz9dJ-lHJcuU8uMNJ7i1NFkG5jc8elensSdZ8WCLuBYU5Fgou4H5MlJOVxOM3gAFfIEfCiCUQ?data=QVyKqSPyGQwNvdoowNEPjRFszCtQAnqImxtkKwlR54xlozQrLYZO7XWmkzAhm4ysYWi3l8FVgte1NJ5WewQqUqzmYvFTrubeMKXfaqMdOebjkcVMDr6494437jtRGtyOobeXUSDMscUAo0gkotqLD1p39oDIXrHEWL0g2TINnBUV9srWO3mO34MwVfLgSjfbaVOnWXk2Jnet43aj_0K4CkrRPayskUW6kofeBgpQC5B218nZmt0ha19vr-wWBddrRdKCUT5Y_XAjkHo5b0QCXYLr7ADQo8fn_YWyJBWmlqDqBC88msh0ZB-Bp6ol57O5yHccCJ2LK6s3mXBz42jJ092yBsMoNZ5HCvZYKglVNEM7hqHzFlTBhRMBocchShXE1DBKeRGEnl6CZOXHFTW9Wqva5k54a-l_HJ5TO6I0Rynmg24Vb0tC1f_uaBaeUTstqfWBnb5qnMt1q2n5W5uLKRY0m0ZQA0ydeVCRD5o1l2lsCJ5gAtUIwwvbXjxfVUQTeh0ZsIOgYvkRrnddF1ClCSmFaN9H7JA_5gWVSkmGERDvSW2o_9Y1VnPYhBMotTB15Qv_qhmD28OOokax25glEd2FMAg1Poty62oHacTLr7g1dEq-Ximysh5F1Q0-RhRJWsrNYUqByF5uPZPTjyMhkj5UK3iWO-lZnRiGRPTsm5VNcWC2A4DYTYmILTfXn1lG7DfnUM-j0beuJZUzFtO9-3O1yXsr35rsX4Dgs1OS8kpXDPB4h6rdqA,,&b64e=1&sign=53a5246e307b5f66e1aa589f6c58b888&keyno=1',
                    price: {
                        value: '89490',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 826,
                        name: 'НОТИК (notik.ru)',
                        shopName: 'НОТИК (notik.ru)',
                        url: 'www.notik.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 6400,
                        regionId: 213,
                        createdAt: '2004-03-16',
                        returnDeliveryAddress: 'Москва, Новодмитровская, дом 2, корпус 1, 1 этаж, помещение магазина Нотик, 127015'
                    },
                    phone: {
                        number: '+7 495 777-01-99',
                        sanitizedNumber: '+74957770199',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZ6WyT4Rt0E1xd7AXQS-FFzeIhk8_ZNs6ZM0OX5XIrXbZIyAt6AkBijySLpZByd9vzd2SL_IzWmEhnwHTNV5prPVhwstY84U87S5DRNoYmXTUXJ4HZjoFeAH4jsr7B1CmXzJMpOxijdzx6pXyOOmxPEdf7xkigYtZcB6lFpMkHGhKDFL0h21ebBSkw6l0dNHM39PC-xikKZ9r_-NrWWFmQxqEwBIZZ9YvXbe_A2zy5hhRaCjlH9i3gy-pXxWx5LILqDmri2nxkYqFq89CA3r5xt0kKHWCyKwbJbumq0sYl-x87nkraxeDk5JS2MewhZonFUExKx-NJPI9qX13SEZVT3DEELlywNQeLbavAUYnmtdUcbwfY35J6iT5uW9l0FCS4CKYdbDhHzs8MepERykbjGm2ye425kEvf2b0G_4Y-JtuQ8qz6t_hGPvfbcAFNc4wcGd63yw3QG2It3ODegzr9XR1pewr4nQxpIB87XuybuVe9t0O310wrb4csr7sDUs88Fr8t4cLCXPP_JCurKQJgFLTnc5xI3oWeiMtmWu-GAccZjEFu7KUdVYhxQ-kOLLXNdlOqnVaItyFADvsJ3q5X0xLMzkbHwETLYVyBm1Knq_0PoD3Js2LiY1gXxn-5LIAmx115teN6RP4MY8oD4xMjJo3MOawFAPyONQnQwuGXiubOv_CCdEgh1fZNEE0qPY9H02IlvBZ1S50I31YLbHQrFOPAsTOE1fBxzp8HN28m08Sfi0iQEhPNlxWsJCCGHLrIPiYO7TXUzmLd441rMB_Q-BR0QheJl9jMQ-unCGnhAPVbNecp-oN9ZQYFjbs7gQ_r6QzZmdT9CcyrnPWn49n97dokdDt-AsI6V7hL276apC?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O82Ek66EfLGWKLUq3KQhG9T_-q2bhh3jRMiE73D3jLYX5MGwdxArT2Lt8z4RnLsSErgy_jd7TfPK5RdMN-yE0jVEV3AwEtRxTRPbJ26oEmQevD0ugoQSo77MnCmD2lDTch62ZKvhykJ4VhijvwAlm14PKM6TY3EdVnSmVl2qiXsFA,,&b64e=1&sign=2780eb8fb9c9f6a16d7414c94698df7d&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '350',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '350',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 1,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 350 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/362398/market_--Oz57NfBXeBVkWMuvHonQ/orig',
                            width: 450,
                            height: 350
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_Nc5SEHn9otOX5vxtpu2uFw/orig',
                            width: 450,
                            height: 350
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/466758/market_1zTiMQhpyD_Ksw-Qn8qZ_g/orig',
                            width: 450,
                            height: 350
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market_yMSU3Q88icnXCckq90qseQ/orig',
                            width: 450,
                            height: 350
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/362398/market_--Oz57NfBXeBVkWMuvHonQ/orig',
                        width: 450,
                        height: 350
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/362398/market_--Oz57NfBXeBVkWMuvHonQ/190x250',
                            width: 190,
                            height: 147
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165430/market_Nc5SEHn9otOX5vxtpu2uFw/190x250',
                            width: 190,
                            height: 147
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/466758/market_1zTiMQhpyD_Ksw-Qn8qZ_g/190x250',
                            width: 190,
                            height: 147
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/486815/market_yMSU3Q88icnXCckq90qseQ/190x250',
                            width: 190,
                            height: 147
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '769-9803'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '21:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '7',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '19:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.633392',
                            latitude: '55.788778'
                        },
                        pointId: '1216',
                        pointName: 'Нотик: Магазин на Рижской',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'ул. Гиляровского',
                        premiseNumber: '68',
                        shopId: 826,
                        building: '1'
                    },
                    warranty: 1,
                    description: '5.8\' Apple 2400 МГц 3072 Мб Flash drive 256 Гб 0 iOS 11 бат. - до 21.0 ч Серебристый',
                    outletCount: 5,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/_exNeMYl8amUfBTuQTinnQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOizMOJHGbVN3q9qBrfEdLOnuybWyXHWJ84sEhfxrBmcVSpVteTUShBAn_tpR_O3_bg6RbDbOevN8sCMDkMGdqDhOyoKwDGIdZ626Tdi35ugRoXp9unJwCqS1B_XS9F1ieA%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cwZvFetln1QQTjsTscU3rYCAm5-XI1HahAxWoq24OmOrRTnOyq1A9kNpQMt3Yk8rvncH7W3MVU6bsUVBHbBKUD3kXFYgniLl-c3buujxIqRrOLW5K573PTA5DxRbz820DkbOp6qZ46VVnbs502KG1gftoDhsd6oI_dBrGJAC0RaqNngVQGcQ6NLaW273xlmL2I8IoZbUX23JSR8hOMF-KLZNzZrJLzEMTkIVii0X8p5sutgnEqPzz-I3qOd7C42FToKhAxqoqmqSOb0RhruRkFrp0yR1igflZL3i4_-U3xZNF908mxb_SU7H_uwRHhw2qlCD0rk4h0Cjth_1BRceCYAd8CHZ_rhxIaYvPvxWVDYOJNP2xUBClRuJFFBcVI1L5037L70g_45tJkkciH5poMFiygh79dacZZ7iWJJcRVj2vAlZUF3rw4B0FgOP_2_Qu1RS3-t5mmRgUyBg56NEjkAmiZQZdIlohBnJvqihu-amZQN2ScxXMDrPev_w0PDvXrDjYx7_vLxbhE6rDLXKyawb0eI5tATsXZz54BALBoeR6L8ce5Bhf7uARBXVEMoZRZBE1ldYgSgPv91jytvHQs2aQCpH60uPBbdR4k9xcDMLTAZwKrd0KBwlAJzgyJRlZ1wUI2FR6f2vaEhoKyzy8dKxwYpaOYZDQrwn0_JTJ6xuA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXcb28IYq4zywo1RJZSlP-cWB9-4sUpTmOhXZneEisdQBtvV0ffxvdg8MY4iCLcYYfa-L1YwaMcE3FcCKLXNg6ftKrjI95m6_sBJb7ohmnCUHcFO5G69wSasziegRO72ARRxbBkJ0kIt7_-Mm40cELFDt-Uj0Chy0WpCOVk9KV1bdqbKPHMcAP4b2BkRZWfcBArUOoXgHf60KbKtlHAJQorHwWCBahylmVTdjRwIsTtXbk1rJNupVV_6Udj1iN-2u6TMVbImZMCNhuzd4xPNB_icJ-nuMTajRVwKwa8gseKV4ZbMPJ34mwJA,,&b64e=1&sign=ebf7dc6c032f66887fd736fa8be8c2e6&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGPd3UWG2vrs-KU8CU47FVQmjOsinETq-76Ujrb6xEcszTMNaZbTx3DWVdnyJk4F8Ff2itN9kI3nAiFBeeln10Fs7_poEu4UiVWm-BKMUBgU16SAR9mgcki0F3Yhlajx87rfhsvzUtywTOyL7fiG4uV7tO2aUsT-7I3UUnrgXlttemdJxHUt9Dsss7TmnG1mlGnrt4GpOGrlW7Uhj88H_Nm1Tz6eeHx3TBdtlPS09nNEy7iV92ZoSLQq9vq2DuA9HhTE2mthUOqHSDVLJScBaK1VYHw2afHEkA',
                    wareMd5: 'j5gdrPjAidkpmJHca0-ThQ',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256GB Space Grey (A1901)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZihEDHmDjHFAwN7X1qt-YSnbjsiGBwVLCTJefDyXDyQpshR_nEs0BovM5F2J-4EFbWDDSCqnJ1rpMsGJiQHfSpKwZ3QODtJ6-bNiX_HMo5vd5vC31EueUvtMOVgBQJTz3-haY96Wy9LCzA-FKpwRLKnX4hsSKNAxfRdHSJsNBWYZYmcZ5QaR77jO5quBmg3QbSS1pik9i4zSW_8t119qdCZgEXIavbeg7ptZGEnw8dinzqzS279hZiK_3wpujPOZs1Q8raVzK5m_I353q5L0eM_-IsQGiip8xFpCkKEgTNYfjNTrePjEeFDXcexaMkwVcf9IwNea5IECQMYzs_zcpX1mzLLojofGTLszPEPQfxUuHFCXI-bR7X5O5OBdsRhbgfhoY3KRLPiiP3eAAyyoW7Hq8QvF0u590KJJ1fdZkvpQ7D0w1iOinqkwSN_rpoVjhtGBDd5ufEM5XHtNvN-j6kFqbzgnEzYaAb9uEiFORBTcWCz0jnpcDci45MXyLvmxM53FtldhgRYXK6t8DZ0a0V_omboe6Ikw7QpLQiDrQDwjK1MjACUoWXm9M6KFTQJErdeXGirwTajvGN_yETlLmS7UH5FD7xqcFnUlgJNRw2qcPwQhpcwLDo13LIamduVGdM68csVnF3anz-VELhK20E_4L8u03DLetZBV3DtrX11xLt_zT1Do1hzroCtEADOIvEaLuMTmLkVgRAkX70qZsai-W8HMirl-VGlX2qOqDWLofHOmHg3RS1KBgZ-XG-DJb2EeOl1ULfNfSaiHU8BJEVQGsTQGY99fshCs83maMN6uvUxRx0CmeYmfz_kVmU-W_zSBZr-BJDLMt7rjkz_7nl4XSBMaK-MdF6tyP3ALajS?data=QVyKqSPyGQwwaFPWqjjgNq5ob6Jq70TJiGoeif5hpcbP8YccfY02ogWNdXvVXQnUtSG2m_TC7d2bPi2wXHRJMk1-9eyQ9C3OjxI_WLbq-QOakcXe6CkTnKDV9gB-s-l3EX1g6T8IZZHSKkquhMSh2tmfCl27G9JeuNpNzI84wQXWJUGgejZCk5fK4lGJxMKnU-rih888IVyHUj_7Csa7dKt8u7NFhMn9pIno8NLod9nWtqali73zanYBA4ajZS-yb2YpoiNWP4CzTzXiVHGNwBBni0PK9SmeHsSZ24b3po5cnU0Wzv-GeOOTW_4BnUBzUB9kzPwev_FDI9qv4161TUmugCHs59euO_8_a6o8A7mdCtXyYIYyyMYjY3uIMIKPcKuZw0ySW-IqPycjhZgbHvBzcyn-t2ZpXrxkr6IoSxf4sibDSd7-1QedAgmRafdN_Kqy64w3Qf6thPVMb-KkyxYo70sESAHpKo2pcClhRXCpS5aUgQOfHsPgQJNgoed5vRbhrH5bIFM,&b64e=1&sign=a8fd70ef9ea807ccc0808db942d8aa39&keyno=1',
                    price: {
                        value: '85990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 262,
                        name: '1CLICK',
                        shopName: '1CLICK',
                        url: 'www.1click.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 4928,
                        regionId: 213,
                        createdAt: '2001-10-15',
                        returnDeliveryAddress: 'Москва, Садовнический проезд, дом 6, «Пассаж на Пятницкой», 1-й этаж, 4-й павильон, 115035'
                    },
                    phone: {
                        number: '+7(499)990-77-77',
                        sanitizedNumber: '+74999907777',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZihEDHmDjHFAwN7X1qt-YSnbjsiGBwVLCTJefDyXDyQpshR_nEs0BovM5F2J-4EFbWDDSCqnJ1rpMsGJiQHfSpKwZ3QODtJ6-bNiX_HMo5vd5vC31EueUvtMOVgBQJTz3-haY96Wy9LCzA-FKpwRLKnX4hsSKNAxfRdHSJsNBWYZYmcZ5QaR775I20O_jUhXbbLDNbFhxYwgkDKRTlLbZ9sx_yJx0LTTGI2nzLEWjQSfc_Yn7qN6yp1TkhHwWdLsMgByr7LTqHjTJk_N3hhVRuGJabjw3sOhqIfINAADL8NSCrQuHytP24wx48V4-g1mP6ttnc6KTvl_JOqR6OlkVdH_A73aFiaEpOFfsXwFx4Upb8hxmtbJWMrdmoTIHFoLWMArYKZJidspGLry2kvfOcSv8atNemA6Xg7pG3aH4gRZ2XxksXCFdmuqkvuRu2bJs5f4SFux3BfPNkfuAx8xKO7odnDn0UOgaSXlt8QQil-YhUU3JE_HFXgn4VbA_TGWFK0ndQnbR-RW3eu7y8fcyNIinAGPJt356osM_7vx_DeCMWDumAX4fNSAh2-G3vuY0IMLlRKaGrnMkkGBGHzmpEJh-fdj_BD1QstpKNGpNduY-Gu1EQx70oxOhohlWWHObaz-sS6RS2BS7UJHPNhl4by23kXPxyiphIsMU4O0coIeiu9U4PMgxNxHwrUEPk3L9XWU2EuNgOkoorO9_EMEwATCfG1gfth8-reKrSDabqugkGkUa22VWh8LkvjaII1TdbQwIqLmmG9eZAeZd_0IV6mPc8Jyp4BuWeNukTSpcQlljkEGbKPozVbogwdIZkpo7eFoI_SYx50G1oxLhyf-REo3hbROcOkoiMNQH93n5ue?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_11pDnxvgQDNFLTtHppF9DnkEXWtVLwaiB8YQgsTSIS3q-23fiQwUniAMdRibsk3tLtyPzhNdSUx78d9r7-ik-Qy4e1fXp_gCNyrAm_KB3ioWFPTenjI9bG2h2d-kLprQYB2D4KrykseEJE7U9DD_-JuDDk571-cfFQJ3BEC99hg,,&b64e=1&sign=08d4a2fb1d18ff41e73fa83d373c6b82&keyno=1'
                    },
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: true,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            },
                            {
                                price: {
                                    value: '300',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 0,
                                orderBefore: '17',
                                defaultLocalDelivery: false
                            }
                        ],
                        brief: 'в Москву — бесплатно, возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/224162/market_8rv8ZjCXt6IHlib9XBM7Kw/orig',
                            width: 183,
                            height: 350
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203037/market_QK5oryjCTEu2SqraJikeQQ/orig',
                            width: 183,
                            height: 350
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/224162/market_8rv8ZjCXt6IHlib9XBM7Kw/orig',
                        width: 183,
                        height: 350
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/224162/market_8rv8ZjCXt6IHlib9XBM7Kw/190x250',
                            width: 130,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/203037/market_QK5oryjCTEu2SqraJikeQQ/190x250',
                            width: 130,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '499',
                            number: '990-7777'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '7',
                                workingHoursFrom: '11:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.629206',
                            latitude: '55.741571'
                        },
                        pointId: '336309',
                        pointName: '1CLICK \'Новокузнецкая\'',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Садовнический проезд',
                        premiseNumber: '6',
                        shopId: 262
                    },
                    warranty: 1,
                    description: 'Apple iPhone X – юбилейная модель бренда с рядом уникальных особенностей. Это 5,8-дюймовый OLED экран, обрамленный минимальными рамками. Это распознавание лиц, отсутствие кнопки Home, анимодзи. Процессор A11 Bionic, усиленный нейронной системой и чипом движения M11, легко решает все пользовательские задачи. Ресурсоемкие 3D-игры, графический интерфейс, приложения — все «летает». Автономность гаджета высока. Заряда аккумулятора хватает с утра до вечера при типичной нагрузке.',
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/j5gdrPjAidkpmJHca0-ThQ?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOhJdIWs9KZDnTl4FsG2XVAqYWnqVp8oLtbB_0Z4FttX1nFggpwcVUGOGpI4pYZi56JydSTU3oCXqV4tGO6BsX0SsZ2L52iYQsg5ZE790N-UU5qtzaUE_F0qUvz2XnVIt0I%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cwLcnX7_zjGAXyzf_nADxJv6em9gnbFavrsW3g4vLh6MU3jvYeI7FdXCVHZgik0WENC04W5dduBZJVb18iL0tPVQSXpMMGh9KkMNmsbZ8K9eeeYrRiD2sn6t1tXk1cDkYyx2_Nb5T3K7OzAIRjTkVvRajMPlhtEwa2zzTIgbP4_5Ofdy8gq4dOOhMOOQCPRSeK0AH0qgkJrGNYs7JyQ3Uu4-shkpM5Mh7nCRS6e_CsJSSBEhgA_4p7EIHphdl56-conLxGoBOtm8htSaSfONgYIH0Vrvlw6ItyjuimT9pRyn58wuOPrQWPIuhouS2qXnd0ngK5nAf15TL9oRkAjxmv-4K0Z3VzpkWlOH01aWs7JeHLstgTbHzTgiWe40pRe9ho5M3IbngE5gPchoax_6ld5AyuiysEswZfIngVzu82MkvSf2ZuQMNGOKmcp5MD3i4t5vNVCDg7SBGeuf4GmgxiKIH3_UiGxiBbqv5JmoURnKfNZJ7wFtF2jAH_-GiVHdVsAWigEbis8qYt656Yypdtn3yCvmG9icFQb5o1Gf6712gDq5BVD43PlgnQVGmTfURONZYAiFwIlNRFcgrjz_yEgWXtu-xDDOSYVHu4bGKMiendf4hh4E5Y7zyi7gSDYHfvSczQj4H-wpOAdQH6N3EJjRb290AC0s1h12FpOqYuzgmttzabtgUZD?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXR8uSLuOp3g3eSjV71WAZHASkoFCTbrwtIqXvwChQjOfuRsFj7ClXMHG8OEY70Z89rNWykSKZG4wZ446hv1tNmnoDAPekqL51w0Vgj3CgSSDplqHMBizxTF-BZZgWJMSgocSKvAOU0c6Y2qQeD_-ndx-duWPhi4bX9UKCMD2HXD584pvqsArRkRCn-5UwCLNsi-xQePeIDbSPjA6_AOVDfKif_8PjaFfV518wI-wZcNWPg4By65ZpSr2QFyC0tBdTR_nByMbOfH4Mi7wUUaQdL0rA1aPxN0aCA_I-W4OKaRxKaHxDUFYrmg,,&b64e=1&sign=a4110955f757309bb3b7ec7e2f92bb0e&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZHGgVS-xyP3sXeilRpYG7-uyd8PIha3BEHgWTcD5kZF1HA9-WdbaJ_rwGfegCccymsetRDIl1Y4KzJCjC1gO9Xk-c2HhexQuqgcRUviCJD6BE2GLd4cLMuZ2qRppJdrmTWE7hpUbluX-qlnN-0aUuolNAuCpZybA0V9zi20ygrZ9_EOAc-oj2QZHGqrAcfecJdmrDmBmicTup2q41XZD1uzduVwO9mL3L6x6hlJeb97GVYz2zr7hQpEXsoDTaCMyj4sa9CuCvlHk_ZDyPIkJPOsEmMRPHt2DCc',
                    wareMd5: 'AfQbuTIbx9ZyoE9exezE0w',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256GB Space Gray',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4cbFtLcv3ob-ec5ykyqgvOkzHlt7Xay5Z2he8WXeLCQ91gbyIAX-0SGi_ZSEzTiwXpopFLAcniEmeuY7WZE68a9y0G1pH62KAO1lV0CDCeQHkXbXVm0aLKK3ymJG335ND_S7rcrd41BpAao2TVaOHpmGmfU6ypMH6shjGWIOicQStqRPkhCGhyGCS-scVM5V1SlAnv-fJvWTwZI8-myzO-NdkUNfZpV6UdQYZd6UPeG8v56gB_xa8O0tINV0h9j7chEoauOje8jNLQwXDaJQ-uvDOMpCV2GlusC9KCWWuNpUIWJWaYpmAIEVlxOAvMfaqfSyt-tWxc-PBzOlFHX-rxus2FEM3HiSYAb-BpNQLA6PaGIvpke9gK_mgEcQ6ULEmKKTdNgDHTkcShHhyTcs3a1g4dTX9FtE1a_1Ah-JTszgihGIYvIHtDT7tb14u9XanKqPwWwrHbnP3ZHBjG5dHziaGg12QSgUZQsIUe2ns5tgFrc5rbXE3qLiZ_2VeDSEpr9AVUeTCC7atr8mYhQGprCm7y7tXHPAgPiQnnxgHqYwKBc_3NQE6wiftULe2fSfsM08RXYyZm43K3ifMN1Xxw9912u6YMY14hr4-vx4jSng1FzKsA6_Z2ArvB61zqzsLPzTx7N5ClV3ydJciT0U-3xQQLvz60mmvjnkvzsrlwGxZ7DwKUuTzTH?data=QVyKqSPyGQwNvdoowNEPjQmrLsV74GQgggGEO7emY2x8CNp48nmC_fp1kB4HXNmfu2scOe-zsn_yomtlVSFwlKwdyDZou6qo0S48V6wAKaGrra4En9ENPJC3KCG82n0WDLIAAJWfXo2k_lbn-ZMXYaov_uQfkOXy0OcdgK2AiyT7lBTWuz1BkR9zcJCwNLgFpq7qArN1HtR98wv_LMa6u5VAKnqLbUQDJYf9A7rUdGPQW06U0n_ShALhNzq7WBFTz6oF29EkO48KxRqg2GqQyTv9E09-rMiqu0iYAnwcjK50L5vPVvJkyS0y-eS4HAeMnvnco3pUH7wTlcmwtOON770m7Bul8cUJ5q7wLGcCO7ECChdckv6fgZdfgAIuVAvejusiRUTZecfGEfkqEqvlmA,,&b64e=1&sign=9d36f3837a429d53a3a2a2771ccc7bcd&keyno=1',
                    price: {
                        value: '85290',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 398748,
                        name: 'GadgetBaza',
                        shopName: 'GadgetBaza',
                        url: 'Gadgetbaza.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 180,
                        regionId: 213,
                        createdAt: '2017-01-20',
                        returnDeliveryAddress: 'Москва, Барклая, дом 8, БЦ \'Рубин\', 121087'
                    },
                    phone: {
                        number: '+7 495 255-12-48',
                        sanitizedNumber: '+74952551248',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4e_tb7h4VQnaQTnZTw9mPVRt7JR-nRQvEFTEX3HGX5fZOo9ce1ljpbmvhzBq2diJPurU6lJvlZmMzUePU_aN1RGu7GxFHtHFuYLEPs4M_dZRR6qOhqaulxzsC3H7f4PQ21TBP6YoYhLiYZAJA3UtQ1tbUSu1shGbrp-ydHav1qtWnOpoYPXJW5khFpy_BHNeh8aYhaUFLINQXl_Tx37NuGss0lEiTtG0xoZ95k_AmOjxxnrd2k7IEQzVhQKIhbYDmBvx7N7DNiagWUZXjhxbKYnXaU6iUEFuBfnJehX8H5dKbwLdXZsbEZgbo4i2R0ESBsyJTsBD-iorLPGBPth-zyr_2QbAtwTR20uEmVy7iLt2Q0yAvR8OqoeJmZ6M2Bm8kb8-6WP4d5f2Rk0gD2hSa_IWKdbdbULAs_XK1KOtfAcQNoPLipthQLjfwFyKaTweETSnUHte0sxKPWZ3JQ_jY5XWzEWjg8qQfjAyrBFSs1joJf2mOZ0fXagBi54aXXuhx2oOnldIhk-_yRwDz8tCY8Xx3gspEo6RLJZ5dN3qP5yjLZsdhaPtM4FfzRnRFA7g5MxuU0ALX8jun3FsCJ-_IPIie2VFocaM8MytuZBjZzegAD9KsUN3_1eTSZjpGvXh85KlaATTD8Qmii3CNAXhxSXcE7LavJlz2r0yu0QG_82PU2SQLQwjmi1?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8G9f1Vijdq2qoOUNv8J28WvN7VxVBfYgAwJ8QaQgKYqGhH0RyAT2XZRG18i3EyaSvESLX6bRPDH2LWY-GrYNxMq-eDfzhBzzca7cLOIWykUU2qf8zNccyqxMcHElLkZBXfPp72G6rnizuQVZqyT3FOh08qDRn4omEWj35mgtAlvg,,&b64e=1&sign=8f0a4c48f8b4a9b6cb715b2ea45a1c1c&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '300',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '300',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 300 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165839/market_2SHEEkJ0karmMtb1xXp0IA/orig',
                            width: 200,
                            height: 200
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165839/market_2SHEEkJ0karmMtb1xXp0IA/orig',
                        width: 200,
                        height: 200
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165839/market_2SHEEkJ0karmMtb1xXp0IA/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '255-1248'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '20:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '19:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.502786',
                            latitude: '55.741135'
                        },
                        pointId: '552062',
                        pointName: 'Пункт выдачи заказов',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Барклая',
                        premiseNumber: '8',
                        shopId: 398748
                    },
                    warranty: 0,
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/AfQbuTIbx9ZyoE9exezE0w?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOjeuejhRnoRJV18Xx9rqoTN_9raBE8VNv_aqnf3lr_src59-GF8cSGrcB1yeZ0VnNfa5Xhh8jXd9Ew7duswzPB4Mm0Tfb07-OalRNwUpE2mvMe0pExwwQeWB1L2FdZs440%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cz_16cW2FZe_TPoITCDXAU2KSJCBotX4wF-0_k_ruqEDr3iNbrv2MXbCko-2xZdo-44vAczxMxAmoNurtrkMVFeu2WyHQN-nL57dXjo1XvXvW4p8pMKWgddDB52TJV9jh5tBmOcjJHH_22JHDvVuSBV4SZJAQV1paYCoWiaOzIV4T7kBwVPJYdU4YwlfjTpWmbKOvx9HoQ1sAdoxOABfQKfSRLXNF6KIKoeL2jN9h2zPhie_FfS4MQ2a_59J3e19-k4XVEZz0W68COGQWq-cH2BmVLKZi_X70n6FoDdSzS9n0-HAs9pmJSHCeOa2IZrzTjmIIQ65kRNx1TcjLk0VSkatHtML2bhDl_UtP9l4iugV-0goURH6pW9aE2I4wz7Tt6tsyGdZBqM8b2czqjo39RaR_6Alm7YUV8uTYU3Hm9ZwQc1jhjwFue7ckjhH-xXDkhukFNR7Q3_AgYBMs9qqLvR_unHH9XUc9GasSqpmDF4Cu-noGBhYo-ntkVsWEPkzsawTFXeYZvI3FHmBhu2Z-gyVyv5p-PzUYhjDIVfRc7bhx-kbrMVbzv8oroLRYwsNpK35IypobAWWNT8oCpIl8_M05-hBM-INPEcDip-MMlZYZk6_vPylB-k4A2eR2l0swSA54lHtxqIvGEdZAYoJVi-2MOIpH59VQm2wOpGZ3NWDqOWPv2fUXxi?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXCmxepjeaTV3FBJX_PP77k_5BHM2SdWLzqK693XfeQoI2a412lHM7kjy7I_N4aiZ5Kxeee4Pvhlu0Tjc8xKUWbZ9TMrnP4mlRNM0QkoTowsGBi-rDzzqpKZdKT2r-5AsnGHB7fwVg0uHHT7XCXZ0G0JgvFeqjojvy515b6m_yZZTU2Pm1hp3gEm1sgfWOvANFimr6qSQUn_PfToNvxRXgHSePRAPAdgpZHZudbSgkCurbutXg2HHq3sTDWEx9DvE60i2IyxIdRxzR5Xp0j5YGoFgkKFuYrFWCGY-1DVRu0Vo6doq6jQuQPQ,,&b64e=1&sign=b8fe98d6a741408ddc1da3ba5e56cb39&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGbTDaeGVphXafpchzqTZpabd91xzyc3EmvrMdlOQe4T1COLCs2gJcw3AG-pJNg51r-7yOKzX6BGBu-A4ynFqkHMJBjH_Su2PlLxBCK5powxI_S6bSWZsucQ3VZJHnA0y3KbqYn6Ipd4thnm2cAgbdW8xTnhC9Gf7Z3X28Ud56qAMCcVYVSRjtKVGvH8whCfcYdkF2ggK6gr1MYUe7ydU1IdUSTAu1xpsyRVukMudPIyq_17gjwg0A9i5Nx3yf5H-ROOH2Acw7IuCLkAV6-8EfP_GNHJvRVsMo',
                    wareMd5: 'xVerSmT-uyldgHiic6nVOg',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256Gb Silver',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZihEDHmDjHFAwN7X1qt-YSnbjsiGBwVLCTJefDyXDyQpshR_nEs0BovM5F2J-4EFbWDDSCqnJ1rpMsGJiQHfSpKwZ3QODtJ6-bNiX_HMo5vd5vC31EueUvtMOVgBQJTz3-haY96Wy9LCzA-FKpwRLKnX4hsSKNAxfRdHSJsNBWYZYmcZ5QaR77jO5quBmg3QUcjh0jmx4CBCzRtQ9ElvesKohVdPNLnIr3rlSAKZy0LyZhxNo0-sFXP_Pl9mmFBTtUy7k6JqmIwq8RqL_RcQN7_pkKj-p9ks7DKlYbLWIVujOLBeBicS-MCUlNhEZlNRPEDy0-cYaJWNpHEBr9I9acQm_QcD68nIU4Dx1qTJ1q5Dz70fr5enC6ugPx-h7Fn1Gcp-aW98jRiPhWNGrZA2k0CPMb4Iib7pfNptA_jm_gvDf723FqEL4khwdmcDrIr9npqh8NwZ-aVPrJY2HCARHYLznXVZt13hvc0gpNxq8pBAMgOvlIJolhpowPQNXPWKKWBZVhV5qwqKn9iRDcqKmCwo5xy-3OCW2kKSfZ12wJQTnXY53wDymrWraKvDjDUXO1EUNamSAeG6f14MYMIbR7J6VCBjcSD-i1e0cTFeoM3cDQorxDi_Q1qvnIJCMWlOz5nVBD8WZw6p3kcwGN51sg6aqOeYDsH8oBp_f8kl0w9mkhuUnmTXN1yJu5FjS9l77GO2HwRSAZBuUf0_o1RxGkajpjsMKwM29kKKdveqg1x3d-abKZnYOm7ngWRicDBfHGE_X7RBYdTxFCo7pzIdR8T17CVqqPwsrWP7Lp87MJxIN3vufR104cjrgVLASzkHDhu0NI2dLOdOyF8ajNQigfi2h4JHgUgIMls8N3ypeKR?data=QVyKqSPyGQwwaFPWqjjgNkwLdY28uY2uOsPcZdVd60oa36EpW3g6cQo9XDpjATDlG-JOgWN8ugvj25h39hIhRZlUitEPhSn71aZhG8DYt-aRh0OfZr0r5OKrVC5_SHIh03_prJ44OWaeZBIfRWH_7K2Vo85agxcB64NHkAsAJetLz8MX9d28kvlpuYZSvGjxEl658zhlv_ZuQhvn3jnICbgGJ-RngG4chX7GRbt9PvSSlZt39SIqCW26yaeXzf343kCeyfDH_2DOr60Vk7POxnHBPyu9XnyL0eVjDQrj1rdclQZE0JNrUhy1pUBkn8roKUd7xSxukh7EmgZqrgi7nTFJvyR39XXlN-k_g5pJIQlNEkEZSLPPbip0gkxntifNgXfeM0BnYE-3xXc1ZuKUq_hOeOVPp41r&b64e=1&sign=9dd4129d9531d83aad4d0814fdb928f0&keyno=1',
                    price: {
                        value: '84702',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 324920,
                        name: 'СОТОФОТО.РФ',
                        shopName: 'СОТОФОТО.РФ',
                        url: 'sotofoto.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 101,
                        regionId: 213,
                        createdAt: '2015-12-04',
                        returnDeliveryAddress: 'Москва, Барклая, дом 8, 121087'
                    },
                    phone: {
                        number: '+7 (495) 969-50-06',
                        sanitizedNumber: '+74959695006',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mZihEDHmDjHFAwN7X1qt-YSnbjsiGBwVLCTJefDyXDyQpshR_nEs0BovM5F2J-4EFbWDDSCqnJ1rpMsGJiQHfSpKwZ3QODtJ6-bNiX_HMo5vd5vC31EueUvtMOVgBQJTz3-haY96Wy9LCzA-FKpwRLKnX4hsSKNAxfRdHSJsNBWYZYmcZ5QaR775I20O_jUhXd0pwTv0wM95SM2xuZqxleyrZlCoYxIHrgVTxFnMHrqAUxtfTHTktA_R5S9IPbvd3f7V7_83isW7CpxRkOx7I2u2JU3qSMhUc-u0WZOn8MKk4aMsYSm2OX3wDXZFfDAZhrosraJstwajrIQ79ODDDU_QGAxaMGb4kBuexDGfJQDiz5kkltPQCOCrKmG-wvOp5b2dOgoaSpuJIt8rCLsHgqTX5BDtkAOPxU_3Bt1dlpRdNgDZe4ubEnVR76S5cSN5b3fvy0uNelZSh2Rze6oWfLf81aWzNNr6FIiWQcB4sKsHJllE-JaJD9p55BIGH9FgV0YX6JejfiYiC9CYZVVyB78G07WLzO0Op49yuaa_HqVYTp_LfY-fPnOuAx2OyCgp6Olm1jmlTrNVM1vy0gejZ4PE3e6vMP8ap8yWtd8luE0PKipKF3kT9SmdEvsYRfj-fxUoM1HqOe9w2C8ZXsIphJnSFq5tRXJwFWWcUEuwhdaQx5idTxBMokrgR0-aQejmEEvxBgfbwX0I_kjVd2S04P-5xMip5XFVGuz3KTX8VWTgE6kISpwUDuRKQ3ISNNiXO8jLklPvn4WJ6NqtR3fNSQhWZs5H_4akW6SV1Vzo3MnVaAysTP8hTCBnIU79zvvooaNSzHZqO-1_J2NiCkL8PzbRyrQkk6ofewggLe5VuSOi?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_cEs0UhvD49MzGpXsTqQ1SZQQg-xAV-Hf5YHSZArde4eW1COfW8ERUdmRoWZdSjquNpYw43Czq3XP3_FmiPSHUIOw81BuJyut9jtNr6PcVeptLDoD1-tNv2yaga6OjdUNPOCDr-OAsyOBReN4zq-VQ6gM-eEyh-55ikhnBWHq0fg,,&b64e=1&sign=3352ce0e63ba46c2bf44ef33f93a2560&keyno=1'
                    },
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: true,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 1,
                                dayTo: 3,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — бесплатно, возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_ORZIqerB4tCJODKgUtODvA/orig',
                            width: 600,
                            height: 600
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/240755/market_JZsECtiYQo0dvSvxWNdrsw/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_ORZIqerB4tCJODKgUtODvA/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/217087/market_ORZIqerB4tCJODKgUtODvA/120x160',
                            width: 160,
                            height: 160
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/240755/market_JZsECtiYQo0dvSvxWNdrsw/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '969-5006'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '12:00',
                                workingHoursTill: '20:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.502786',
                            latitude: '55.741135'
                        },
                        pointId: '443330',
                        pointName: 'Сотофото в БЦ \'РУБИН\'',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Барклая',
                        premiseNumber: '8',
                        shopId: 324920
                    },
                    warranty: 0,
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/xVerSmT-uyldgHiic6nVOg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOgcpdYuq2JeMqO5ym6RdQYinlzMHB1f9K6ci07dauRuVpbQGmgLw3W0rl6pJgdmx4CTNwIxiead2xmpMTMrm8I46ZVJiS2nhHI3wp4me4iTCJfkqwKEhkvfjNfcG3s3vUg%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6czUdIhPe28acuW2C3Gcz7eOwflcMWmlNs-Bnidj9Zs1fNDV7Ed3NzADipFVyeqFhEpdoy3ZIy9a1l0s882cDHkyHZA8H0RJfhmaDddfIbKcKyxKCYD2PHpw7ZqfS8z8J_jDs2Gp49c-73EGNl28HC9pGaFrihcQpSbu0XHlkMBB0AbdX4WYkAqrF1aFTmk2BfLJ7GpfHDpOmzo9bShHyowTDEFuFAvERLKbb9A24iEhDx7mHXN98jvbX15hzzdpm9la5KHDMAddAiyaOVbJmpGbBzdoiHAqTVMp4UpYQXQKfyS8DUdgvV-BRQTg1v9mEl622ALtielveed4nV8kOCl3K4lLPO39fARods_NkUOtYy0y-JmNScH-dem_lY4NWdcL677jscMT9VDMOjdczx41mzWOU72--9OCxvZGmjAosDZZRIVcN7gSJlwPxa3fVrmZk6X81YOS4tviul_M809JOI63pojnfULCW43cF_yLjTkUsXXCoeKsqnab3XFT4LMt_mADFXw_-0V6sAUvy_VKwPqM8fhQ7LmKF46qcims7zrQETO1klwTHNI7UJplaTD0YsZFmnc8XmbyZ-tLRZcB5TXTzDzEqXfDem4B8CHemkVE1LUvYfLi-d19B8ibADUCucW_O5ZiaG3QiwiDKgWY-GuqBPSNcQjQf6iU0zoS8-kLACmDL3GY?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXfSOxIq4TRqYzTSE740btHnNqRZdHlmhKf7dg8izLGJmwTIQl61LHr6lw7dB36GuR79M4sxg2hfpSd3q-fFkM8axmOXB0SKMoN3AzzaUfL07H-tsdzlXOlGkiu4ESUOG8uLsWblAl5IHoJsEE9mALof4DvKKso2WRuz7n8JwRxrMyU1xpuo-aJxQrZffo0a9BU0ou2xhioFfLJSPrQAFlw4Aswn-lIkuBeSvJXqjpNUh2gIGp2wAMeX5OdfBH1PQATX0vux7nqrEAbuf887gXK8ZoAgHT5jsCMsKfQJBbrx-1ScWGdlb60w,,&b64e=1&sign=ab2b9f9374c81e567fbbd848aeedc914&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZHDwzXwJmRsY9HxhoILuBGrPku46bjy4P0AE83vQZbB1vnwTGpXreDPbYCOW01H3MKvbpzYThru-I7pnbDApSEhhVlepEkEYu4Ak-weX3fPtuetpkumDziCLaZ1cCDT95mt2WnxASPCyJT2f5lqeKTFF-lXnT8xkdhKKT-j2KiXLrAfBDVtOE6uw1niThMeOPAru2Mwx-RMbRA-CnR9RJm2arCt2dLVSH8BEbf0bimfL-vm9jTpXiM-fnsJUgvM7A97ES3xEnGRBS9YhEnr2tcYuIensvfqGZU',
                    wareMd5: 'MR8hwPpj9i3zXj5sou3o6w',
                    modelId: 1732210983,
                    name: 'Смартфон Apple iPhone X 256GB Silver серебристый MQAG2 A1901',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4cbFtLcv3ob-d98nslNyl5-T6AR5ecFghY5kbOkZ07wsuh7J1olSjR4mjVfQDLk-OzKJfy5UI-l44e77zyGo_H01WBF_b-1I-Ygx1ZaxZSKibrhS0ybxV5hm9ZbUJ_p1AhML2TBIc44EgAOFHwR3GkTqdBHksglhaREbt_4Vv4jZ2p_4XvgR5AdJl-OoXWl-QSTiJrjLF0p-5thXOpxQLT3h4ab7aT8RBMEMM1rweBAeX-v3bzLEQp3ynSWcoBAV8B5UValENp2Kxo2WrRc1lS2fw4XMGq5v-KV8j4Inlb1xoBjbj9yeq1YF_tps8E1jjKOx5-LH9AJCg2GAdEoHxPcsFxhYip4fxwJMW-9KKeBKBrUcGR_2UqlqKzQ7Ak32pzxVQUAaxN3Z9gcJhVKRbV22sYNgv9gckb-7uvPdIMl_GOdh1EFI9NPSfEIXln0jV2lVTniYc3aIylhiZ5P_36M_Faim0q_bdUd8p_5iuoAoY4to1hl4_mToiqaTENC0kRIAd6i0o7ZV1L5ah-oQOXAICvBTf1W14bjDEIpPIaIpMwqC2J0aYwc-FhilT5gYGqq7DEbtOzYGnkU_t2pAUu1OPmSeGfJsG5GkKjswNZjYWw0QuRybM5vaskh8gCr3wIcOx7Nu8w8_afsi4f5-AxJD2dJmUh47d7nDT7sCEZsiD6zuOpGSy7O?data=QVyKqSPyGQwwaFPWqjjgNqnG3t6z_QgIspgTFPe60Mz_U_FkJ9EAvzZcm-VpHyNDsM9th8Pt97R76Iohyi7mTLowVLF2jIzkvKI8lmIC4tGpCIQxY0iwdoXtN6niigQAjUyzUACjzKl04_nzlFqkzGE-Sr1lWpqwFWi26648AmO9FyNXheZHtuLCGX3WxdxSue-xtquNsBzvDO3cOg1xJszCcJZh0JRmdg_rx2VNDv9MvtnfBRkv8m0dmzwrhZEnDiP63xHhaygP87I7V4THIPs1OXmQT8-h77H4-cV4UiqiVr19iocyzKAfr8O2LLzHzYCyTNMzNOzJvKazq3lG_fy8G3T6_M2uhrXcdX0n4Mk3nZTgvqY01m1NNH-RoQi91bFIaiagVQb2Js1UzmhEj6uVixTWY7cvJrPNnQ_pHtzBP0gYY9lOwZ0CNnePTN9p&b64e=1&sign=a0c983a75b657956fd6fdd01909b1b79&keyno=1',
                    price: {
                        value: '83990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 8185,
                        name: 'iCult.ru',
                        shopName: 'iCult.ru',
                        url: 'icult.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 1304,
                        regionId: 213,
                        createdAt: '2008-07-07',
                        returnDeliveryAddress: 'Москва, Багратионовский проезд, дом 7, 1 этаж, B1-018, 121087'
                    },
                    phone: {
                        number: '+7 (495) 6496990',
                        sanitizedNumber: '+74956496990',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4e_tb7h4VQnaXuxa6mkPmwsXnGFD57RnzgDW0W61vnypBy14mci15FRbxrd5hCHRfwtc6I1xg5f_BUcCvsedshYxbyDxsVA2nG3l5CUsiS1MGqXYOKkENqfSYBMfEZkdhMbvku2KqaP3exfNPDa_TbiZG_pezgUvBOryO_v2MuLCEon9Q55PQrypJc1Ar2NBcnZXrpbVlXcx764VZcW-0MFSpb86N9eNpRbIh4zJqk_Uy9HsbeUv4AzXGWkXsZfRaHSISWw3-ELjakWQm_wOlBlohBm0t0Ws4_egByPuBRBUASyc9g0-46Be76YMLHJThV_YUST3Pt4vrW_GQV9DmLVp4aDGTVTcGPFyNmOa9-TCvdcbAGTASfLc_I2gqLyb9tkrVOzDAhbG2vYL0nSBHFCsqfzfWQhu_rQoL32fmZlkJoQ68GDcdeKME4jbRFUlSbb0IU8fMPPgz2ry-qYLaXYAV8CN8vdO6Ii54tI-h1WyDi54sCM60PBdfnD6WaTP6tOeOtIwKC5N7vWKt1dCsfV6sioBFg1Lo4rn3iu54pz3VXSKwnVEjqT7pdwV3HqS2UxX28OR4uoKCNmw-wn_vViRZG2S5-Uqa-h7i_7D-SQdAsr-8i391_E_hLxkEd8o1Wp0vVa8Z9GqFpFBzHkRVfZ_9ifNklprTvsaWYRe0H48eMH2-a7p7qt?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8r-agZdH0LRUHZKsH71F9rZp9K84hlvU3GcDVE-nSr_F9NzHXeq9nY0JDWqT9hIe9E8wo_Ppzd13qzhfAlYb2U3zL80altumh6TAxeDaHRoA63C1DZuGJiqDcUTsdDU0OuarB3esp0WCMgXUqfh4PgC0hfowGbLLJwI92jzB_UZw,,&b64e=1&sign=e405ad123c0ce8efb73c55a8904d1f33&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '300',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        methods: [
                            {
                                serviceId: 99,
                                serviceName: 'Собственная служба доставки'
                            }
                        ],
                        localDeliveryList: [
                            {
                                price: {
                                    value: '300',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 2,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 300 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_5jxE2EpIcoiC8bsMgDNWXQ/orig',
                            width: 600,
                            height: 600
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_5jxE2EpIcoiC8bsMgDNWXQ/orig',
                        width: 600,
                        height: 600
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/165151/market_5jxE2EpIcoiC8bsMgDNWXQ/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '649-6990'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '21:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.502786',
                            latitude: '55.741135'
                        },
                        pointId: '265047',
                        pointName: 'iCult.ru',
                        pointType: 'MIXED',
                        localityName: 'Москва',
                        thoroughfareName: 'Барклая',
                        premiseNumber: '8',
                        shopId: 8185,
                        building: '1'
                    },
                    warranty: 1,
                    outletCount: 2,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 2,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/MR8hwPpj9i3zXj5sou3o6w?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOipybXg3628aiU7awY2q5Nn8W6hCXDJ35mPE1svL97BHzD66MrlptYdIZMlxIsoF1hYq4fJfkf8lyvIg0h9yTvoa7rzYLuIYPv2ZN3bGDrY6JXQz4AOv5f_2ZvckoFPzic%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6cxKVtcScPawvLz_iTtsA-HDWYBbwQJpbPxRdLHeQIM0oq7tSCvPbgXLm96uheKYN5eUGNk7MmkoaWyWHCi5bb-ysOy3itCGxWIKJt7QYzvlXzmnci4soBIdJICrN9w5SsMpHpL_UvfanFnRjah3HEZ3gVVDBQvfc1r15s5JJAUCeH-yKz0JOY2qVwBj_FL8g3jmhgrp1TeCjX3xNdIpfvSLfT86DZwp_hakzsqSBKj8PP0YruXVtkRkKn88ZfRk79Wb2wbLVtPHkYZrW0nXYzr7ko_KiBRs7mYf8jrgIoO70BgwH3TneDFmo0cmKhOWWGhPMgLQAk1ReLjO8m-T3SXwwkpNxqxybfNtFXCe1iQ7mn63LseIaYQLdUOW3KCGbp6ksJDo-U13O1SqLPJBSxs1lmx9qqpye6KxiGf3u8cu4B8smOEu297pXfkr_HpzI-fdfCQA5ChEDxAZw1raonpSyRQETryP1bSQC42C3EjOGPpdiCpspie640WmRC7foZ2b2OaKomztL4a01TPEbTbNbMZhcus54nWXtyAqNl0GhfOVE4DFX2jUyOzWaX8yMKiFlc7m4nTbBa8dMq5G1nnvOGlElvB6oNC7-lAO-pniP2ZSkc7R0IzwExgy667MIC65lE6wUqp_mI99OUdD6FoQ5isuFxtT-lAqv6MoIDs6gg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXboE-FHcb96cFpH1T5Mn5BPcEBqimbNnwWbT9aBsFsWJ9PhddNSLSxE04LPVgtw1jLjkTIfh3QP4Tq2pkVHSrDC2GXlVuMfQdHHJsdjZqTsH-Hu8FFbtUo-dk6mGqLiQN4pfhFMLOsQ0c3GVFlRfX6Flc9-rL9XPdYs_xiVKA5gkf8GdpoKR7ioy7oTVGqPmED1daLcMbJu-nDMem7YunQlDdyt4tCojokR207OcoIu5FpCLWf9T_EZ9_VVbb6ah8a4BrftM221NgzbarBxPlgSQtUSGwL8tWf35BsRnlMW-_bFemjhVShg,,&b64e=1&sign=1f127a552a52b26a17e0ec60737315e1&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZGuJUugZx94CQz4MLphutlT8nJn0oSKQkLyQ6MowEkPFP0IqowZgtUvrsQxRscoZDjAnse2ml9V1agWXRxAv8J6sUW0dUvzCSAgjSTuT8ZnOeRfGHm2eYn0uxhrnm9-AP9aiHzGhPrf-UbUWlK_ny0GSXURzqbTrryta7lo0rWi3ZQ1jJ81dZ74Y6jJNT9phemcDZakrPvX5Elvd9IVtU02_BuWo5xb4tODNLc_me21TFd6Mk87T_7kw-WcPDCBCXKz_7Y1YBA9ujAS1pAYNSYK6VzOZK3fNnc',
                    wareMd5: 'W4WGye1DamvSVH9sDesZOg',
                    modelId: 1732210983,
                    name: 'Apple iPhone X 256GB Space Grey (A1901)',
                    onStock: 1,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4cbFtLcv3ob-eawCqJ5CjPplOmjKIg9xLQZYSx71yZ-8TtRWACqBo2YWsuWbS1YSien9vTWlnmKMxBC3-sO5XskbvNShbqVyIPTpJKtxkTzwsJJSMrCssFR1Rqv9BHDajDaQKKgGw_R0zMLqhdCH9Kum69Qc-RqYSbJ9TJy8pHGCyWafvvH7jpHsSruyz098bVMCDpGKFUg6DsbY-XJwsjyy-BdZNAbqxjVy45tuAbs76l5mmarOo6VI2st3pbbZpzs4iVTkAF4UDIsYoUpbB0gS4E5DCWhEHxOuCiTnUSdvcD4PquXuxrscADUzc4nRnHAEOt176n1wlYxQB9l57UnXsRcQdE8AuoaB7mxctOtEOKkZdwCN656x-DnYvsSUaej_a9G0bYzeWi0izVyHDrlrhH-cBaZtio-N7FAhPX19HzCw4htzOFMRrmaJp1h2gs6dPjgrNd1qiY2KL3oMzc-vaD-cPMOWuGGv-SHzKbMcx9vmOtiypGy-uTaW8NZu7sjMMEcZG8UHpGQlX8VolHn3Ru1lAW0QHmFsx9QWY0h__AduSwnoAwHizuRaeNcRKKihVHdSVbcEUaLir3J30tZXr9TXUKBeJI6_rbQJI9x2jgb2xZzZMG6CogUng_OcV0ofkwEVDOH5SDi-GLpmvTPUyNXR7fEvKLSaWni1pOJQYY0nyzAtl3u?data=QVyKqSPyGQwwaFPWqjjgNsR692ugY0jPYkvqWKcD46MDfdBSsZS6r2NcWc3E2dfmpeKYyybk09Oc9x1I4Y-3iOvzvgO6LlWhuVBAIflc9xp6M_AhkOasGFcQBjn8VS3p_lXoVuIa4upbZigPesTcueWmZk1yMWy_KiZABFgpCeqDdncC9nAXI-wa67jxzQKMTthTnZ1TDJEtuCWpluLopJGcelRvWNmenGZcOacgrIrbImq_fAgcVNmq7bTnMQ-f8QjRZbVqXPTAe0jQJ72zSAjIUj4DuE4R8XgSre2-XGCoKfE3eV27kADUW2F06_AKwq9lMf7rByMl0nmjyQIwcZkzY6esdbiTMaTDVlJnTffnltgb1InxwhMCKm86aI2BAahiMUbAmiXJhB1uLivWMNxwhqINopC3wohC4bJI1pSpDhhOLngRCQ,,&b64e=1&sign=1e50c1e8f3e6a7e3a2e84fd01788af6d&keyno=1',
                    price: {
                        value: '85630',
                        currencyName: 'руб.',
                        currencyCode: 'RUR'
                    },
                    shopInfo: {
                        id: 141122,
                        name: 'Цифроцентр',
                        shopName: 'Цифроцентр',
                        url: 'www.cifrocentr.ru',
                        status: 'actual',
                        rating: 3,
                        gradeTotal: 284,
                        regionId: 213,
                        createdAt: '2013-02-06',
                        returnDeliveryAddress: 'Москва, шоссе Энтузиастов, дом 31, строение 38, Пав Б2, 111123'
                    },
                    phone: {
                        number: '+74951375157',
                        sanitizedNumber: '+74951375157',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mQ1anAOpsgym_KEbvKYm8_ipxGn9me1bA34ZpS_pi_ym0RblnEheH-1g_vJxMiygpQtD8pbtyC1v8gDxKXfMPjlgKVRaRitqaCkAcWnTuJuLfGQHXh-L6lwyjAyKhBaanEKBQ7NnG-eK_9xTTkKNE6pfLV1uExSrA0tVX_d04REzxKS_4795Q4e_tb7h4VQnaVRRqHsSisdF_9dDXWkXhIkXEyHPB-BpQjOjb_C0Ce9quYPJhYcxDo_meYqeIPBmGpOVdG0NiPgvNcJgqHegX4eQls2a-kjSLNywaP0iiWTaxXD9Li4OO4IJ4-1TB_S9okafCZVIl7fD2fw5a0uKXW9JinZR1T-rbEd_faNFzm6Usx2a6L0n0oB1AmlvgWyNuaed5maHuvfvNw2LDnMwygm0ir9mD7RwvOHhiW_qOMDGbkMW_tos2_fRoAcCQ3xdLZhTRCWz5P7inhsjzbsFUhBsctkYGP0GLF8KzBLpDbeZyb8JW7_s3dmdDA8zj_R_ZQyxOOCS9n275BPqnj0mcLI2HcZw4ESDqw2R0jEXcB_dm-8DXwa7AMZr6-Zbe06lljHpeuYadNwv-aKl9wfGjAi3IhR7zjP_zRlGO9QM_9n_1Fnm2FMin97OATadj6XWrLOHpQniZqbydaXoi7rz4LBsYX5_iigJP7QZCqZlW_3h0xSy9_yU1HbfPUwN2czvP4qiYcXe_XI5i1ykQpG1vqw_tUUemDtFKHXpEORJX-fAD7afo8xtOuCnK-UTcWYcJOHRs8xZv0pENNEA10KvPdeUYhKRLdFJlgPUIsVyTwGlF1_sLGnRIaUfj9rLQAuD5CdDaSeT4QOq_RruEZVHbSNf4eMRsgGG5vZv8vSc75cU?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-t8n4yQdf-HKBy4sDfXnuOLH9bvXNygL_ikeIU1ioy3Ju4NAD2lOKVvgw84Ns3FJSyfDc6GIsu3vCm0pxk57n8KeLgAhEmE-6YZZFZF_Igjpj2TiTOF2E0fY5pG8yk11vsTvdoIdoAorFotkISsTdTvjf1f2PxCUPEPpxIafDhKQ,,&b64e=1&sign=eaeac0d9958b01f991487974ba796660&keyno=1'
                    },
                    delivery: {
                        price: {
                            value: '400',
                            currencyName: 'руб.',
                            currencyCode: 'RUR'
                        },
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: false,
                        delivery: true,
                        free: false,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '400',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 0,
                                dayTo: 1,
                                orderBefore: '23',
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — 400 руб., возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/241976/market__k5jcRjyEeXK3VEOa5vxww/orig',
                            width: 225,
                            height: 225
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/241976/market__k5jcRjyEeXK3VEOa5vxww/orig',
                        width: 225,
                        height: 225
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/241976/market__k5jcRjyEeXK3VEOa5vxww/120x160',
                            width: 160,
                            height: 160
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '137-5157'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '5',
                                workingHoursFrom: '12:00',
                                workingHoursTill: '19:00'
                            },
                            {
                                workingDaysFrom: '6',
                                workingDaysTill: '6',
                                workingHoursFrom: '12:00',
                                workingHoursTill: '17:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.752293',
                            latitude: '55.759172'
                        },
                        pointId: '411853',
                        pointName: 'Пункт выдачи cifrocentr.ru на Шоссе Энтузиастов',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Шоссе Энтузиастов',
                        premiseNumber: '31',
                        shopId: 141122,
                        building: '38'
                    },
                    warranty: 0,
                    outletCount: 1,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    cpa: 1,
                    link: 'https://market.yandex.ru/offer/W4WGye1DamvSVH9sDesZOg?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=efo7ImjRYOjBQKzmtNeOYg_viXV9Ephk7z88vu2JUtnzBoh7pFU5Kx4nOoXt1GfvcqfO0S631-zJQa-85h4CfHL25DBPOmB5GOZurwguMdtaf49o_nEaLDkpPo_h4apYfTIWexgIMQI%2C&lr=213',
                    cartLink: 'https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgedRGNKu_EabTF2VqNed0mGl3H6UZ8eM0yi0Ps6KtPktrUFy7y-sAUOenOCHR_PPOSrVb_7eSBsEVz-Fl7GNoGhF1o10tsMPhjz4cz_1_9WAC6tJwFe5xc46J8KEct0LZ1gTt4Aak3ozF-U7EQyVzK7kyqdd5TFzWogLgvWYAemJ1h0e4EBiumHUy2XEvWP97Dc8GqIDEAi1BciN_7kp6czVDlXTidwdh0nYmUxwjpU3Flc6iwG1Uh-HRSIE0xAOpt4LicE_BNiTZdwvSsDj5YQ_yEGNv-ODdQ0FokwQX34R1E1G5KuSw5N8fyJsc3AaX13ANzP1Mr56Q8UR9eeoIfqqVpeU4FFF5juLAvZeDLNQAdwUZNgp12DUWHHKbXn4yY9LOZIzW8OBEfGs1IE-RuDp4u8-1fEM-lXkUNQbOpSMmgBp0GWZgYUUMZ8HLVKNWOMQlWo8MYWI3o2AJ2i-25kqMhiqgCJOIy4ubB95fn5GLwoRJ1x0LOqGh51Br6jJYw8JZrEoUvHfexC1uBhyUseAlHuetpz8903p7suiJnRgEhaPxrAene6W-sc7aBkyeSJVmlPDPyZu46Bk2CKDOv7cqUC0DAGDKSXIqEKjFFd7P2yvS4L0XzOGdn1oL3mq0qf1ipBxb3BJ1ZVXhFW0zVVNzg0IjQNteyy7JcyJ0ePDVLb_6hQ0ttg74QbSbVBhLgSeiRmfg1Ch9Zi9J7z3wMPmPtf8penTxnf5nJrB7-y4fZoMjpOiyj-fS_YXQdpvf4WtJSauUvDwTAPe7Id3X6Og3aunqUAvZNXIBh6_TFhh_eU6lj-L1bYjT_u3hemyQRZm2DMau5uBCNfI3-U8Wz3EQoj9-JGU2nP3Sr7JGKCG0ylVsiN4xvR5iGeV3hC_Lo7IA7GRWv4w?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL4jZQUbtmy_7Gb93klMZRiIrwr5yCX_mcl19iUuny89DZ1QPCO84oOKSzVqbBJWWlSrkM36s1M-6JvJ8332LGTZpZFCmQjTsXX-e7V9U7AqdOyZbtI04XMM1wgyM4uCtY0Af5w02eszMpZvi1AntnL5yG2LdySB09pltpCpfAZvbw_X26un0nSxtdmR6Ztw_m9JSEr0FlqrZKHBON0mvfAMkZB5pyohcY-fiHQWvuyA7YelYQB_G2PhnQzLAQkyN3DpLOrfKAbk4YLV8KFiVP5_UGLN2Ko38yhMHNxoxug_NmrEXlx7h-O4L6K2R12AHWRWpcu_pld0vqkVcJ1ZoZ1KuOVoPOw9KuzyD2TpMIR0ztKV_UsJMzKFfE5i0Eg9c68XhcnbsUBvtVx1Cs14IUgA,,&b64e=1&sign=bbdc23d88dd8395a74eb32b16491a2c4&keyno=1',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                },
                {
                    id: 'yDpJekrrgZFH-sbQ8eqT800WyypVbh8wZWb2etrFa9fw7DjxievH5g',
                    wareMd5: 't6G7dOKnKQkPNBizQ4j-6A',
                    modelId: 1732210983,
                    name: 'Смартфон APPLE iPhone X 256GB Серый космос',
                    onStock: 0,
                    url: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mbzcCEFkc4kgfmSM8Oy8TIkgcSMEO3XEI9dBLaK9DnfAuxiNNA46OROAQaw08d-wO6NkYa_DOaLOWB7R_sGEaJvn51Aitb6JK1fCpnINn32uRcrOpctyy33MghKGq7JWynTiJR4iY6FMoqDAu1oRJbRjiGfFuBxD9HWnlCOwtglG3hVlwpKJIDHqSIPUGi0M5STke4UHHVr7j3MEx9OGa6laYcudDDPjJv903GjAea4369JiisuEaihotP8f9UeWEG_GGHl_5ORlFjOtzNvWEolEPINFMHvIZuG0bevUi9aRD20aNxm9UV5YM9XZPZYJOLH-xuk6QcW41ydW8A8dOV-rfBrNDOkoBjyhIxFSXd2IHOQVY1MBS9QUA4xWqjRA1-fnONe3sP3aBvHRoy78Q2ed-LlK10NU-bVgZfmTBE_HqbAVSPCCKs472IoF9b23oXpLrTZjNUAH-mqy4N5Sam7kyTiH_i1nTy_sr95WU2x8eMoYbToEfJwdPvMpx4jcTvGQbIBifhPxDcsAk4uCAexqpgYv26sru-LSTTEhr0rjGkuz90ViW-JPPpsC018eoE3xEyj9PVVkCad5rm32JR7ZF30wIOfTPwImN-X8gpFECiVqvR4TDnqU3eZADQiDrTdTkGyYU-z_C7GcxaFzNzzI1tDe2l6peQyjRQ6sFa6Llt4DtJ3TuqNMTbHIgJIYZDHh_YGuuVNfsUdTaQMJTYUAs6C-xBZXFFuG47jxLeYSNTO9lxsbgQPEcezZj73cF-gEa7Ywxlap1XcDSN2x5i9Njnousk4DFgIMt-BvrK8fPloQ_vFj9SIog_L4lmqI40s3OvDxgX9PP5zKZaUKUx9K85lcFlZguE18DeZ7wOsT?data=QVyKqSPyGQwNvdoowNEPjeHCAo-a2AEEMgvw6Gv7rodwqTifDSzaDcFSJWWZewOkDXpDqhZ-M_AHJZ1AjI7qCnx8jOxe7-xVjw1iIo364VkOHvNaifAyde0sGVn__kA65tuE9CwnCzvdFeiMLRrMMZT0OGzbGsnkmDi7ohEl6SPeqk3xj5EYjGr7v2Xvjwv-X0Ve0krt3cqZbA_R5p_RmWjlOucWR7fwRYZA0NF9pVuuU63s1uiZJcBQnLFAE2y95fprVupaa6xqm08jAYBSiF6i3CUaAqKCKfWiN7PZrTwTCpbMiP2pZ5XhOt9-3wYG2DrlrXyiC4UOrGYH-F1UF5my3O9v99He6b2fyQwGAvI0wekuL_D1OFZbxO_0uVnWPZmmJA5Ia4Q4z5pkUpjOoDgHvA4qL1YCrnH81Y6Bs-hAxnJvV0V7x_sLdAV1exBJik-azqtXEheqtiPi_rwhZ1NBTA4UJBz-aNPt2g09uisDhX7MIchSAaHjJqxByTrk-97Sqhwf_63V7JLW13kzFTZWs26Fu838Gjy-qua3aeChT9VQwOpXOXUerAuHoCTXqi-0_ST1QcNH5RQ87uOe99nX1-IFKDAwJhzwqLeew_bqcPDrMYloNA,,&b64e=1&sign=0abff83f7233e0cc5e0eea2b4bd82faa&keyno=1',
                    price: {
                        value: '91990',
                        currencyName: 'руб.',
                        currencyCode: 'RUR',
                        discount: '8',
                        base: '99990'
                    },
                    shopInfo: {
                        id: 255,
                        name: 'ОНЛАЙН ТРЕЙД.РУ',
                        shopName: 'ОНЛАЙН ТРЕЙД.РУ',
                        url: 'www.onlinetrade.ru',
                        status: 'actual',
                        rating: 5,
                        gradeTotal: 65284,
                        regionId: 213,
                        createdAt: '2001-09-07',
                        returnDeliveryAddress: 'Москва, Ленинградский проспект, дом 80, корпус 17, вход в магазин с Балтийской улицы, ОНЛАЙН ТРЕЙД.РУ на Соколе, 125190'
                    },
                    phone: {
                        number: '+7 495 225-95-22',
                        sanitizedNumber: '+74952259522',
                        call: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz7vv7M_pnW8mbzcCEFkc4kgfmSM8Oy8TIkgcSMEO3XEI9dBLaK9DnfAuxiNNA46OROAQaw08d-wO6NkYa_DOaLOWB7R_sGEaJvn51Aitb6JK1fCpnINn32uRcrOpctyy33MghKGq7JWynTiJR4iY6FMoqDAu1oRJbRjiGfFuBxD9HWnlCOwtglG3hVlwpKJIDHqSIPUGi0M5RNXrC3OHQ4qu2ZO3XZLdX53HAtOpmI22qi7qnWqQoKnDYY_cbSqDlCUwIUFhN6u9RdNuEvORkY_CXsezhq6zQQogFZlW47Oh49lWVkGBCRd3SYmw2hRi9Y1ICroNNdwvmlx7sPo3Jfgk46sKtH49mOJ0fZuUyaCjmE0E1O6uX4hLOEmmNxJ7tfQE10_dIKdtmjtZq4k-CDJw--qLo2dHB6YWUlytKJohjr-fn1jsRzlLbYhdZI7iCOwl8xaojLv76MSLLGM2TvQ2gyfl3RJEHgKRPkGLe586mcUFXFc1u0QMkcX7LW0cQzSNUIOtxnfqwOuAQNNFtIcbhn85SbcliG3M2TKd051p01RrzVKYHPHATFBIaNHAFOLlpXSx4gaTjTXHvNUDe4HkKq-jrsCP2T29RBlOlJmIQabi9xd9piJlKdR74FK3e07e2LxMXKdgMMgtHGyIWJ3L9HOSLbNnWQEQ6RO16UDBNRhwmhnjHinxkGYkeBAF2Hry8ut2ctv_Fwx4hwm9Cy6u7JcB1D_7tiBQHk6IIfVNs55yjzJB2cSx29a-uk7Qmhw38fil4PQivYi0Rm7rBoz01b0uJYIft6zhC6Q9_V5_S1wqF_8fDaG5Il6m3gPON7cFlmcfOyC0VWLP-SCCW_U6X6i0u7vBiQ1gD6Zjaq1bwaKMMuBIXbd?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8S0S5Gp6pjONUpBBHc-qs-fZehNzlEEIkYX5szcanZybhxXP8VvBl-xdJrWPMooVTmS-ZAy3cfnYP5yMHrAZ4jvxk89r6KoN0-brYdqTu-rNpLoZmD9epjdcEWPnYTuQ76lWJ9DJuz8Tsr7U6RTb06xpNSm3IzldGv5kaXj4skdg,,&b64e=1&sign=2e1502805fab44133fd64e473c20fe88&keyno=1'
                    },
                    delivery: {
                        shopRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        userRegion: {
                            id: 213,
                            name: 'Москва',
                            parentId: 1,
                            childrenCount: 14,
                            type: 'CITY',
                            country: 225,
                            synonyms: [
                                'Moskau',
                                'Moskva'
                            ],
                            population: 11612943,
                            main: true,
                            fullName: 'Москва (Москва и Московская область)',
                            nameRuGenitive: 'Москвы',
                            nameRuAccusative: 'Москву'
                        },
                        pickup: true,
                        store: true,
                        delivery: true,
                        free: true,
                        deliveryIncluded: false,
                        downloadable: false,
                        priority: true,
                        localDeliveryList: [
                            {
                                price: {
                                    value: '0',
                                    currencyName: 'руб.',
                                    currencyCode: 'RUR'
                                },
                                dayFrom: 2,
                                dayTo: 4,
                                defaultLocalDelivery: true
                            }
                        ],
                        brief: 'в Москву — бесплатно, возможен самовывоз',
                        full: '',
                        priorityRegion: 213,
                        regionName: 'Москва',
                        userRegionName: 'Москва'
                    },
                    photos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/235963/market_cRm4Mn1ArNZYKzfF3QoMeA/orig',
                            width: 344,
                            height: 689
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245020/market_3dhjqRy2nbnXj3kRYvu0rQ/orig',
                            width: 344,
                            height: 689
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/208277/market_wqI1gudYdQOA7bCqGsJFPg/orig',
                            width: 44,
                            height: 690
                        }
                    ],
                    bigPhoto: {
                        url: 'https://avatars.mds.yandex.net/get-marketpic/235963/market_cRm4Mn1ArNZYKzfF3QoMeA/orig',
                        width: 344,
                        height: 689
                    },
                    previewPhotos: [
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/235963/market_cRm4Mn1ArNZYKzfF3QoMeA/190x250',
                            width: 124,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/245020/market_3dhjqRy2nbnXj3kRYvu0rQ/190x250',
                            width: 124,
                            height: 250
                        },
                        {
                            url: 'https://avatars.mds.yandex.net/get-marketpic/208277/market_wqI1gudYdQOA7bCqGsJFPg/190x250',
                            width: 15,
                            height: 250
                        }
                    ],
                    outlet: {
                        phone: {
                            country: '7',
                            city: '495',
                            number: '225-9522'
                        },
                        schedule: [
                            {
                                workingDaysFrom: '1',
                                workingDaysTill: '7',
                                workingHoursFrom: '10:00',
                                workingHoursTill: '21:00'
                            }
                        ],
                        geo: {
                            geoId: 213,
                            longitude: '37.50502496',
                            latitude: '55.67525905'
                        },
                        pointId: '8560154',
                        pointName: 'ОНЛАЙН ТРЕЙД.РУ (м. Проспект Вернадского)',
                        pointType: 'DEPOT',
                        localityName: 'Москва',
                        thoroughfareName: 'Проспект Вернадского',
                        premiseNumber: '39',
                        shopId: 255
                    },
                    warranty: 1,
                    outletCount: 51,
                    vendor: {
                        id: 153043,
                        site: 'http://www.apple.com/ru',
                        picture: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig',
                        link: 'https://market.yandex.ru/brands/153043?pp=1002&clid=2210590&distr_type=4',
                        name: 'Apple'
                    },
                    variations: 1,
                    categoryId: 91491,
                    link: 'https://market.yandex.ru/offer/t6G7dOKnKQkPNBizQ4j-6A?hid=91491&model_id=1732210983&pp=1002&clid=2210590&distr_type=4&cpc=mTU2lK7rCr7wysuBHNsMz-MVtSm2kWbo4LNt4pe0mK3psaZEAOOxSgHrZnTDozKmfFmj4_KY0IrMn8NnjtN7e2TwgMrBg-I0-ADx1g3wIMqbOrQ007rb3RWGKZjPJFRU&lr=213',
                    category: {
                        id: 91491,
                        type: 'GURU',
                        advertisingModel: 'CPA',
                        name: 'Мобильные телефоны'
                    },
                    vendorId: 153043
                }
            ],
            page: 1,
            total: 103,
            count: 30
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
