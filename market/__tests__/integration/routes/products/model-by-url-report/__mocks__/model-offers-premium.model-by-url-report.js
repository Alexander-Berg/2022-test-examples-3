/* eslint-disable max-len */
import { REPORT_DEV_HOST, REPORT_DEV_PORT, REPORT_DEV_PATH } from '../../../../../../src/env';

const HOST = `${REPORT_DEV_HOST}:${REPORT_DEV_PORT}`;

const ROUTE = new RegExp(`/${REPORT_DEV_PATH}`);

const RESPONSE = {
    sorts: [
        {
            text: 'по популярности',
        },
        {
            text: 'по цене',
            options: [
                {
                    id: 'aprice',
                    type: 'asc',
                },
                {
                    id: 'dprice',
                    type: 'desc',
                },
            ],
        },
        {
            text: 'по рейтингу и цене',
            options: [
                {
                    id: 'rorp',
                },
            ],
        },
        {
            text: 'по размеру скидки',
            options: [
                {
                    id: 'discount_p',
                },
            ],
        },
    ],
    search: {
        groupBy: 'shop',
        total: 61,
        totalOffers: 61,
        totalFreeOffers: 0,
        totalOffersBeforeFilters: 61,
        totalModels: 0,
        totalPassedAllGlFilters: 61,
        adult: false,
        salesDetected: true,
        maxDiscountPercent: 0,
        shops: 61,
        totalShopsBeforeFilters: 61,
        shopOutlets: 2877,
        cpaCount: 1,
        isParametricSearch: false,
        duplicatesHidden: 0,
        category: {
            cpaType: 'cpc_and_cpa',
        },
        isDeliveryIncluded: false,
        isPickupIncluded: false,
        showBlockId: '',
        results: [
            {
                showUid: '16124480810149912768613001',
                entity: 'offer',
                trace: {
                    factors: {
                        CATEG_CLICKS: 3759,
                        SHOP_CTR: 0.02371642366,
                        NUMBER_OFFERS: 81,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpaBuy',
                            name: 'MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax',
                            value: '0.773695',
                        },
                        {
                            tag: 'CpcClick',
                            name: 'MNA_sovetnik_ctr',
                            value: '0.00795569',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 686779,
                    name: 'Seagate',
                    slug: 'seagate',
                    website: 'https://www.seagate.com/ru/ru',
                    logo: {
                        entity: 'picture',
                        url: '//avatars.mds.yandex.net/get-mpic/1705137/img_id474286622556761381.png/orig',
                        thumbnails: [],
                        signatures: [],
                    },
                    filter: '7893318:686779',
                },
                titles: {
                    raw: 'Жесткий диск Seagate ST2000DM008',
                    highlighted: [
                        {
                            value: 'Жесткий диск Seagate ST2000DM008',
                        },
                    ],
                },
                slug: 'zhestkii-disk-seagate-st2000dm008',
                description:
                    'Жесткий диск Seagate BarraCuda [ST2000DM008] 2 ТБ – изделие от производителя с мировым именем. Компания Seagate является одним из лидеров в области изготовления данных компонентов. Серия BarraCuda выделяется превосходными характеристиками, вы сможете полноценно пользоваться представленным HDD. Жесткий диск Seagate BarraCuda [ST2000DM008] 2 ТБ разработан для настольных ПК. Вы сможете поставить его в стационарные компьютеры и пользоваться данным компонентом.',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 91033,
                        nid: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                cpc:
                    '3WjNy5T3ru367QcprmBWHWCOPy8p_PhjTxZ_F7ZjVYTrbQh89Odkw4U1hMYARqyHiKwyXTdZL6FNOmRkJvf1dW5PPnZ_98Dt3Ce0IkehHdTkDFf8aiO5iQ,,',
                urls: {
                    pickupGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4bhGuAJ4_j5ABufsFpRqPaoLbfpNbb5QGa5_8tx6e1CnmBXJ9fMIfRYKmyqlvnCg_Eb8SFNmybH7Uf7FFXGPetcUO16O9XVLVo79dFOyhPxRuaiUN10uxESz7B4hEL53-FcOPezWhufxyibjBX0MXjd2o5WWb-3UvMITeMDgF3Jsg5XdA5Fi3J9f4y4nZAkauB4sY8sA0xVnBqsEXeCvd24L-JyTnOTlZcwY5vAetTDAvz9Sigd0_igkbGs6RQDsgO7OsgShwCrkXW8TJ7vo3sHFVsyyK1hPZrN3ENw4l2rP8eTdtYuwum6ZisHA8v_VvPJkMahh9clvutGvgbN_dHfvrrwpolsQu7wPiJ7KkROdZv9EA9q65-TcKWLX79NKEy1sVLLO3dSnhgVrt59bNVErg3BP5y7FbpSdaUPc2491UpRFTDOB0u1nrUL_86dSIGIqGY4T8cMYRxjtNwaQ84WZ1hPyW4_CQzfAys2VeJxiP6gaKLxfhuyHdNIg3MeykCz3QZvxmddB7rXLS953LA4OYdx6HLucHx5s0cvd9Nzzif0sOACmJlYPr_MHMSJ8XUoCBR_dy8hYLIyN7fKQrUX8luhqBfPYMfd-tJfz9zNpMI5bpDA0oMd_4Gg3UKTj5F6XofEAEjh9pN08POrbcy1ni1HzD9NFLHs_1HPDDCehZvHU8F2-siNKrEfNVkZ1xWc061nhiFdkEANoFNeDD7CXGpmhPjUBAMQ00H6uvHz9zV1_0jLIpWi9oAKv7vKpOTLAPLnH98pLXWjFkxXk_iE7N4E31lIL3HtfoUoHPAI9PBs89u6KwIYrIon7-dUrf-0Bbc0ML-d-jA9nE9bLwG8,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cwuGSC4cNbngR4R18M2rOnSVITHwE4DkFv0lc4m5Jn8EZdJbuaGk5mcuYoAWhC3IT6HvzbzQYNvCzAe2dOS8-oErvs1FZ3NQ59zx7naXIo33--SWA9o6Vc,&b64e=1&sign=b602b9ecd9f7ba37f2ec5aabe2168989&keyno=1',
                    storeGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4bhGuAJ4_j5ABufsFpRqPaoLbfpNbb5QGa5_8tx6e1CnmBXJ9fMIfRYKmyqlvnCg_Eb8SFNmybH7Uf7FFXGPetcUO16O9XVLVo79dFOyhPxRuaiUN10uxESz7B4hEL53-FcOPezWhufxyibjBX0MXjd2o5WWb-3UvMITeMDgF3Jsg5XdA5Fi3J9f4y4nZAkauDiWCbANnI8yBGee3LKh-MfnIhho66yPPhEDp9Ly9ZywhEPS6OPxbcdG9spCBPEfkEigqgFv3vjWdUIn_4ZPFxLOFPmVipgwRedYnnqoUOMO2goASKcBfWZszaBmiccEdW1jBO0mb62Ch8hWT_vH4MsrIaidGMQkRFPNKZBYVeIboeovEctJnPaThjnPQDiS8iygrIiY9Ekj8afJMFehjqhlQsE1CIL_HfkhYre8K4vC1OM4QyqCsqtAa9QyrqT2mSWzmJDXqbGeVrmuKlB9sQ2d3Uti0WfSkTJVsCxdcargmyhytFybOV-bVZESrGP-lKPusaa8bvveEV1Z1sz9DO2qsgy6EgjcDkUBjGOQjEDWXuBiukudsHGUOTIniu8iF47VW5k0gBFr8P0Xj65kp8CYFmKGjhztJWVxlBnb-QIJqdLPrhQ8urHTI48xVeJ-l9RYap-jxSQr3klRbzvrYbR-rrs6JIHbgJ8VPEYDXJhf3d3A9YetdJ32kEZUkk7lnP3EI-Yb1FrTDMmrLe6lRr1G0lu59FPFcNCR2b5fNarYSbfFJ3iMaFGt1GaZ-dU4T9h3HB7cBDrE4lcGKT6SzBWfgeKyM0qo_HJ0rFyUSRgJy9Lqx0hlNqlRhrq1QV1f1W4yOenEOLl0frHDaMzgnHo,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cwuGSC4cNbngR4R18M2rOnSVITHwE4DkOFEoifQiiU3ZeYJxMgnW2ZPkiSUqW6xqjZ_T2F2QYiihqhVqBB33vhKJewRju24aympBCStYceYqlog65mfxu8,&b64e=1&sign=cf5485dc60c8a8d8c237e873417298bb&keyno=1',
                    postomatGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4bhGuAJ4_j5ABufsFpRqPaoLbfpNbb5QGa5_8tx6e1CnmBXJ9fMIfRYKmyqlvnCg_Eb8SFNmybH7Uf7FFXGPetcUO16O9XVLVo79dFOyhPxRuaiUN10uxESz7B4hEL53-FcOPezWhufxyibjBX0MXjd2o5WWb-3UvMITeMDgF3Jsg5XdA5Fi3J9f4y4nZAkauEUGdN2Wc2yjcQzRD30qRNm9TKVP6CXNLUQrVWJJwgbEHr7E8z58rVhiaRO0ujC0femsKlOQ0l3TXvEtGaSp0YxadzrilwtW-3uNdFjyFLo-IGDuhM7wCwTxK6PdA5X9pwDEamGSVy-xYr-QPFAXOEAhWAx_SkUmyYhxGGiXuk7RrUi2QDFyaLIqMC8vDNF4O9xRm4_m2mtR2Z3WW-5Srbu7treRylQohiMSweSBp5D4KrGoYvuqYvarHFjZZYeO31gKvhGzYE6X7M-RkXPYG40B0N4BXwL6zKV20O6sN0l3SUl7cTjY7B5V1Q1obeVxqsvjcr2A2TkiaLxTfjIIvP5s9Iey0W-GLwMrXdd0Iy3LIfcl0ydCvS0Rc7ixln3DtvNnSFhZpsqIrCWwRTOdYT61UUA8I5mCTA5HN5j-cBHf1k_JhoU7uRATJA9hhXR1kW7_C6-30IIag5skGYNygNq7e-qTRxrDDxKY3ZdpEBJKpBq8Dgdc4SODESUqXsazxv9zNhpI6Cbs9dClhZxsqonzNX13LifzQITGdwJqOlVDpeN2ULSiCqHtNBBbLRtcHU9oecxlLscT-rgEGxmkBBfJj_lozumy3_yqDVcpdhHQpUMwSvree_aQssivzThhz6Q3kQw_jCtc3devCp4YMKk,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cwuGSC4cNbngR4R18M2rOnSVITHwE4DkCa57__lrF-OPo_AgdDpbHcMauYP6ETM_ZovW28fBVIpDZh3DZq4j7i_PqL5Gep9LfwzMen4QFsPZuWHTWLiAxfx8lK9uSW-eA,,&b64e=1&sign=caf51056db8683ecbf506c41e77df569&keyno=1',
                    callPhone:
                        '/redir/GAkkM7lQwz7vv7M_pnW8mSjMImd8MN4y0YkYLJZVYLQbHys5G2b0EI_ZoOLfjRezEm6GcvfI9Pao-x2OhLeB-vcRD6Ph1WASiSBf0yXUnfNutSzqyLhYdw7EJW5_P2Tc60lcxUpl_RvJNJrLkm5noFJY7X9NzgZ1pdeMNVuTpjGxI8Dx5kWw9vpxYaTuNvhI3HN9trTJqtaeqGjKTI1gEKzsWxrUPLRL-pJAnKSrY6Dphz620tzT9W0mBbygLkgn0umypNqtf85mgrk-cTXnFJTsZCK-frRvQDYYkDm95nTNp4BzqkMKGTXHvLLLEbojKWS7x59PdNSlTC-nzhRzsfLnS0dnlgWOE_d4oOUA2lWPNKShdx-aMhUAXJEiUiTo7hzf6frDUhm4RM3gFkbWa19JdSn1naUIQR4NeTEhlp3KX0figxLDh2_rjsn2oITkptlzMbC7qKfMeCjfpVgxHjIUw7RhR6gvtUKmG9Vf9WXlYnyVi8BheA58_hBKEJxyUGvSjoHGaprrNgf97zdvGZObdVuyPkEIgnJheTod1d_aH_Mm9eVhZdGypu6Y2FETWNuxdQI0N-q_wXmmefbYrM7AromJ1cG-kC86tG1x76QjD6COoxuuP8Qme-cFhEYmwR5MA2XHBtjztA1Ikm6GcfrXNtl5Q8jXT-tao90CqU0aafj1usMkHoUXR3TFcjKUwKYUY0D3fAxMGj1AWPJS-VO-19ETdzwdqmw81r5AlitcIPxdwMf-_SSloNRKB03G2b0gfc_2sqDGBc3-xEJHfV2RIWSOl5-wERAS8p0dNtP7SiP36SA_yohgaUKPvwhJKV54t9gFFJZDI0F3jHnp_Pr0a0hK_HGyCZCSqlJlLKtre9Ck2sLp4Q,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8gPZN8crDuWCX1gig0M5wt8j0MHprTVwpZe6JrCp5tHpVanCk5uuLOHp1HMU9sKRanpYGqb4K7M2RyYtXZog0YYIKGFAcxlklNIjc2javy3Vj4oM9am8XnSLY5jLQXpUfRW-3mhWzekJtQn68XpOr6cDyy_TFSVuS0YCBC1AX4Kw,,&b64e=1&sign=c5c0917e124781bcf202135e7f2c0f33&keyno=1',
                    direct:
                        'https://мой-пк.рус/catalog/kompyuternaya_tekhnika/kompyuternye_komplektuyushchie/zhestkie_diski_ssd_i_setevye_nakopiteli/6794/',
                },
                urlsByPp: {
                    480: {
                        pickupGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4bhGuAJ4_j5ABufsFpRqPaoLbfpNbb5QGa5_8tx6e1CnmBXJ9fMIfRYKmyqlvnCg_Eb8SFNmybH7Uf7FFXGPetcUO16O9XVLVo79dFOyhPxRuaiUN10uxESz7B4hEL53-FcOPezWhufxyibjBX0MXjd2o5WWb-3UvMITeMDgF3Jsg5XdA5Fi3J9f4y4nZAkauB4sY8sA0xVnBqsEXeCvd24L-JyTnOTlZcwY5vAetTDAvz9Sigd0_igkbGs6RQDsgO7OsgShwCrkXW8TJ7vo3sHFVsyyK1hPZrN3ENw4l2rP8eTdtYuwum6ZisHA8v_VvPJkMahh9clvutGvgbN_dHfvrrwpolsQu7wPiJ7KkROdZv9EA9q65-TcKWLX79NKEy1sVLLO3dSnhgVrt59bNVErg3BP5y7FbpSdaUPc2491UpRFTDOB0u1nrUL_86dSIGIqGY4T8cMYRxjtNwaQ84WZ1hPyW4_CQzfAys2VeJxiP6gaKLxfhuyHdNIg3MeykCz3QZvxmddB7rXLS953LA4OYdx6HLucHx5s0cvd9Nzzif0sOACmJlYPr_MHMSJ8XUoCBR_dy8hYLIyN7fKQrUX8luhqBfPYMfd-tJfz9zNpMI5bpDA0oMd_4Gg3UKTj5F6XofEAEjh9pN08POrbcy1ni1HzD9NFLHs_1HPDDCehZvHU8F2-siNKrEfNVkZ1xWc061nhiFdkEANoFNeDD7CXGpmhPjUBAMQ00H6uvHz9zV1_0jLIpWi9oAKv7vKpOTLAPLnH98pLXWjFkxXk_iE7N4E31lIL3HtfoUoHPAI9PBs89u6KwIYrIon7-dUrf-0Bbc0ML-d-jA9nE9bLwG8,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cwuGSC4cNbngR4R18M2rOnSVITHwE4DkFv0lc4m5Jn8EZdJbuaGk5mcuYoAWhC3IT6HvzbzQYNvCzAe2dOS8-oErvs1FZ3NQ59zx7naXIo33--SWA9o6Vc,&b64e=1&sign=b602b9ecd9f7ba37f2ec5aabe2168989&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4bhGuAJ4_j5ABufsFpRqPaoLbfpNbb5QGa5_8tx6e1CnmBXJ9fMIfRYKmyqlvnCg_Eb8SFNmybH7Uf7FFXGPetcUO16O9XVLVo79dFOyhPxRuaiUN10uxESz7B4hEL53-FcOPezWhufxyibjBX0MXjd2o5WWb-3UvMITeMDgF3Jsg5XdA5Fi3J9f4y4nZAkauDiWCbANnI8yBGee3LKh-MfnIhho66yPPhEDp9Ly9ZywhEPS6OPxbcdG9spCBPEfkEigqgFv3vjWdUIn_4ZPFxLOFPmVipgwRedYnnqoUOMO2goASKcBfWZszaBmiccEdW1jBO0mb62Ch8hWT_vH4MsrIaidGMQkRFPNKZBYVeIboeovEctJnPaThjnPQDiS8iygrIiY9Ekj8afJMFehjqhlQsE1CIL_HfkhYre8K4vC1OM4QyqCsqtAa9QyrqT2mSWzmJDXqbGeVrmuKlB9sQ2d3Uti0WfSkTJVsCxdcargmyhytFybOV-bVZESrGP-lKPusaa8bvveEV1Z1sz9DO2qsgy6EgjcDkUBjGOQjEDWXuBiukudsHGUOTIniu8iF47VW5k0gBFr8P0Xj65kp8CYFmKGjhztJWVxlBnb-QIJqdLPrhQ8urHTI48xVeJ-l9RYap-jxSQr3klRbzvrYbR-rrs6JIHbgJ8VPEYDXJhf3d3A9YetdJ32kEZUkk7lnP3EI-Yb1FrTDMmrLe6lRr1G0lu59FPFcNCR2b5fNarYSbfFJ3iMaFGt1GaZ-dU4T9h3HB7cBDrE4lcGKT6SzBWfgeKyM0qo_HJ0rFyUSRgJy9Lqx0hlNqlRhrq1QV1f1W4yOenEOLl0frHDaMzgnHo,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cwuGSC4cNbngR4R18M2rOnSVITHwE4DkOFEoifQiiU3ZeYJxMgnW2ZPkiSUqW6xqjZ_T2F2QYiihqhVqBB33vhKJewRju24aympBCStYceYqlog65mfxu8,&b64e=1&sign=cf5485dc60c8a8d8c237e873417298bb&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4bhGuAJ4_j5ABufsFpRqPaoLbfpNbb5QGa5_8tx6e1CnmBXJ9fMIfRYKmyqlvnCg_Eb8SFNmybH7Uf7FFXGPetcUO16O9XVLVo79dFOyhPxRuaiUN10uxESz7B4hEL53-FcOPezWhufxyibjBX0MXjd2o5WWb-3UvMITeMDgF3Jsg5XdA5Fi3J9f4y4nZAkauEUGdN2Wc2yjcQzRD30qRNm9TKVP6CXNLUQrVWJJwgbEHr7E8z58rVhiaRO0ujC0femsKlOQ0l3TXvEtGaSp0YxadzrilwtW-3uNdFjyFLo-IGDuhM7wCwTxK6PdA5X9pwDEamGSVy-xYr-QPFAXOEAhWAx_SkUmyYhxGGiXuk7RrUi2QDFyaLIqMC8vDNF4O9xRm4_m2mtR2Z3WW-5Srbu7treRylQohiMSweSBp5D4KrGoYvuqYvarHFjZZYeO31gKvhGzYE6X7M-RkXPYG40B0N4BXwL6zKV20O6sN0l3SUl7cTjY7B5V1Q1obeVxqsvjcr2A2TkiaLxTfjIIvP5s9Iey0W-GLwMrXdd0Iy3LIfcl0ydCvS0Rc7ixln3DtvNnSFhZpsqIrCWwRTOdYT61UUA8I5mCTA5HN5j-cBHf1k_JhoU7uRATJA9hhXR1kW7_C6-30IIag5skGYNygNq7e-qTRxrDDxKY3ZdpEBJKpBq8Dgdc4SODESUqXsazxv9zNhpI6Cbs9dClhZxsqonzNX13LifzQITGdwJqOlVDpeN2ULSiCqHtNBBbLRtcHU9oecxlLscT-rgEGxmkBBfJj_lozumy3_yqDVcpdhHQpUMwSvree_aQssivzThhz6Q3kQw_jCtc3devCp4YMKk,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cwuGSC4cNbngR4R18M2rOnSVITHwE4DkCa57__lrF-OPo_AgdDpbHcMauYP6ETM_ZovW28fBVIpDZh3DZq4j7i_PqL5Gep9LfwzMen4QFsPZuWHTWLiAxfx8lK9uSW-eA,,&b64e=1&sign=caf51056db8683ecbf506c41e77df569&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mSjMImd8MN4y0YkYLJZVYLQbHys5G2b0EI_ZoOLfjRezEm6GcvfI9Pao-x2OhLeB-vcRD6Ph1WASiSBf0yXUnfNutSzqyLhYdw7EJW5_P2Tc60lcxUpl_RvJNJrLkm5noFJY7X9NzgZ1pdeMNVuTpjGxI8Dx5kWw9vpxYaTuNvhI3HN9trTJqtaeqGjKTI1gEKzsWxrUPLRL-pJAnKSrY6Dphz620tzT9W0mBbygLkgn0umypNqtf85mgrk-cTXnFJTsZCK-frRvQDYYkDm95nTNp4BzqkMKGTXHvLLLEbojKWS7x59PdNSlTC-nzhRzsfLnS0dnlgWOE_d4oOUA2lWPNKShdx-aMhUAXJEiUiTo7hzf6frDUhm4RM3gFkbWa19JdSn1naUIQR4NeTEhlp3KX0figxLDh2_rjsn2oITkptlzMbC7qKfMeCjfpVgxHjIUw7RhR6gvtUKmG9Vf9WXlYnyVi8BheA58_hBKEJxyUGvSjoHGaprrNgf97zdvGZObdVuyPkEIgnJheTod1d_aH_Mm9eVhZdGypu6Y2FETWNuxdQI0N-q_wXmmefbYrM7AromJ1cG-kC86tG1x76QjD6COoxuuP8Qme-cFhEYmwR5MA2XHBtjztA1Ikm6GcfrXNtl5Q8jXT-tao90CqU0aafj1usMkHoUXR3TFcjKUwKYUY0D3fAxMGj1AWPJS-VO-19ETdzwdqmw81r5AlitcIPxdwMf-_SSloNRKB03G2b0gfc_2sqDGBc3-xEJHfV2RIWSOl5-wERAS8p0dNtP7SiP36SA_yohgaUKPvwhJKV54t9gFFJZDI0F3jHnp_Pr0a0hK_HGyCZCSqlJlLKtre9Ck2sLp4Q,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8gPZN8crDuWCX1gig0M5wt8j0MHprTVwpZe6JrCp5tHpVanCk5uuLOHp1HMU9sKRanpYGqb4K7M2RyYtXZog0YYIKGFAcxlklNIjc2javy3Vj4oM9am8XnSLY5jLQXpUfRW-3mhWzekJtQn68XpOr6cDyy_TFSVuS0YCBC1AX4Kw,,&b64e=1&sign=c5c0917e124781bcf202135e7f2c0f33&keyno=1',
                        direct:
                            'https://мой-пк.рус/catalog/kompyuternaya_tekhnika/kompyuternye_komplektuyushchie/zhestkie_diski_ssd_i_setevye_nakopiteli/6794/',
                    },
                    481: {
                        pickupGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4bhGuAJ4_j5ABufsFpRqPaoLbfpNbb5QGa5_8tx6e1CnmBXJ9fMIfRYKmyqlvnCg_Eb8SFNmybH7Uf7FFXGPetcUO16O9XVLVo79dFOyhPxRuaiUN10uxESz7B4hEL53-FcOPezWhufxyibjBX0MXjd2o5WWb-3UvMITeMDgF3Jsg5XdA5Fi3J9f4y4nZAkauB4sY8sA0xVnBqsEXeCvd24L-JyTnOTlZcwY5vAetTDAvz9Sigd0_igkbGs6RQDsgDGWTGnA6dhFDLcj1H0Bo5OQCCA-x87tey5umlnJmKE9wgK86aLypmWv7LJxEqpb4rxA_IwmCjS1AFYHNMHXq66W7euK25VbRLUSPd2Sb7_94rV7RCITkkpyNIRjJtrTfb87rdcq3q8V6TN86XkUTMYqOSyuFQKGtaKFMBOxDoUxtIKUjn3JbOgW4TzxF63JY4Hu1XdcX3o9Wi5MXXS39TmsmzQXQKUh2DZhD2ZzjAOeJumEionbDhFz5LD1ChjoURFAmjz7GiXFjy0jJqiZ0wAq2Uj5ifeAJkNVaqznwEFGBuXrdcibxStiSsSdBsKrdmdfXgx0s5-T0vN2GgCLmlYEGCVIGE6iDjV3GEs7zGzwnIKmXLTwCiGQ5awcOc2GnWzWwOGbfPidtF9F69gKiDearOpeS5WUhM2jDW0nnFr65J0dWonopT0t42C7Oh8HVooh-ZX3BFv-yRPJxTy6shFuDFM0zbEo8jdtgTzVOKOBWg0L8UJXYQSpepJWhkOguACWZWMHkuejiW9-kx96C4FxL2AhH92_AW7einDlN_DYJ7WUppVfegAwMwA0UTswHuIdOxqiUi5rhXGTrNoNv4Q,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cwuGSC4cNbngR4R18M2rOnSVITHwE4DkFv0lc4m5Jn8EZdJbuaGk5mcuYoAWhC3IT6HvzbzQYNvCzAe2dOS8-oErvs1FZ3NQ59zx7naXIo33--SWA9o6Vc,&b64e=1&sign=f1514f35a8069641d12c2b281e07e98a&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4bhGuAJ4_j5ABufsFpRqPaoLbfpNbb5QGa5_8tx6e1CnmBXJ9fMIfRYKmyqlvnCg_Eb8SFNmybH7Uf7FFXGPetcUO16O9XVLVo79dFOyhPxRuaiUN10uxESz7B4hEL53-FcOPezWhufxyibjBX0MXjd2o5WWb-3UvMITeMDgF3Jsg5XdA5Fi3J9f4y4nZAkauDiWCbANnI8yBGee3LKh-MfnIhho66yPPhEDp9Ly9ZywhEPS6OPxbcdG9spCBPEfkNklEknkuQFKpTTJpMlHGMjQH0hg-u7jyhqwKQuHYI7sWpulMVcAHgCf2n3WQtY8ge9o_pjp-MoRy_a5Yk7tpKhw_yYpDOtSUQHqUejjQ2k_WrTvinwGZk8vpqL1dd8jT-U29bNwoHgVqAOVjNqHXgOpPDnBPEXmFv9LZdyWvYfeBdBiXakY85hGCSoZbzNV3J_F4tz9nBFNQVlM9-i2GE97sADmsDnsLB0nbszi5joBUm_8LhxjJ7yOXTUbkIpgvxp_NpGxyZ9RZOldNpgbhzftqeJYU6PD6bm_rJoAx13WgLoKrvPaI0gUfRBWpnaL8M26z_Lbwj5MrQgKE6Z28uY3_CpqJXyOydd6XK8UH5bxJcsxi9LOWZOedmbJj9NXLg87NHTSVzPS0kl04UQ-U474R5NT-oopaNFCaf8y4hfGYnlnC6MkoDaHDwRQf-F4G-tT8n6B5Eu73SHFzfclbW8_53JtjD7T4A8dLYeFdWYWmc1scdY8HXj4xU6KRllGE41RnGvp5vDEIQ9qmkiN3f3qexvBZQja9QdpN_iYDAsBozR6hlUwHNh0xvFy_WCiLlloZorCswMyHdUs4Hf2wqo,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cwuGSC4cNbngR4R18M2rOnSVITHwE4DkOFEoifQiiU3ZeYJxMgnW2ZPkiSUqW6xqjZ_T2F2QYiihqhVqBB33vhKJewRju24aympBCStYceYqlog65mfxu8,&b64e=1&sign=56fa74cd9f115b236699c3413f837bb6&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4bhGuAJ4_j5ABufsFpRqPaoLbfpNbb5QGa5_8tx6e1CnmBXJ9fMIfRYKmyqlvnCg_Eb8SFNmybH7Uf7FFXGPetcUO16O9XVLVo79dFOyhPxRuaiUN10uxESz7B4hEL53-FcOPezWhufxyibjBX0MXjd2o5WWb-3UvMITeMDgF3Jsg5XdA5Fi3J9f4y4nZAkauEUGdN2Wc2yjcQzRD30qRNm9TKVP6CXNLUQrVWJJwgbEHr7E8z58rVhiaRO0ujC0fQRYTy88PjtlVRrYkJOhuVdhNAQ2GnY3b3GX6DJeB-N-ncvokx6piz2miIG3S0neMj7emowkAB-kXyrI6pOBDfSWquhUZp2GOHWTGXOq-ARZDk-h_5CIBlsewX_Lf2j9Y12bqDOXSsmZRh9sfErWcOU1ZSV0mY_943aNl-gGyLCFjCQgBGPUyhkNNxlj-yBCAMY9HxQnEr1zW80ZjyDiLhgAJxCQ9cvxe5mzffkv4PchS4N5amMQqhp2WGzqR0-fJxg9KIVtyQBgruwG3K-fcfbQ_KElbgWN0giJSaDjpfIEGAwwhfXeCVPW8ljogX6r30Aq8QPvpNfvFXemtT5rNrh8abcGNAi21ksqjt53fdSaujvyfKlZ6xxgmwDRrann6V-J6CT-cfrJrwlZkIxrtcSijeCo2olvgChL5rh9rQBZwRTIAVTp8t3WRAIlecTvr7E7rZcEqoNaxfZD3apcPOeDnI434Dol3yX5eAkjv3bTP8iqRUmPO2pFSbBuxlqi2vNJigBSa6LgQwCCvmFT83Epvt3PH59oOYxS3QaVOwccHT_1xTpCQwNd27YQ-gfsNV2txMRDgua7ojhQB3ULJMk,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2cwuGSC4cNbngR4R18M2rOnSVITHwE4DkCa57__lrF-OPo_AgdDpbHcMauYP6ETM_ZovW28fBVIpDZh3DZq4j7i_PqL5Gep9LfwzMen4QFsPZuWHTWLiAxfx8lK9uSW-eA,,&b64e=1&sign=76bdbed4a1e1403d59a55bb394d27122&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mSjMImd8MN4y0YkYLJZVYLQbHys5G2b0EI_ZoOLfjRezEm6GcvfI9Pao-x2OhLeB-vcRD6Ph1WASiSBf0yXUnfNutSzqyLhYdw7EJW5_P2Tc60lcxUpl_RvJNJrLkm5noFJY7X9NzgZ1pdeMNVuTpjGxI8Dx5kWw9vpxYaTuNvhI3HN9trTJqtaeqGjKTI1gEKzsWxrUPLRL-pJAnKSrY6Dphz620tzT9W0mBbygLkgn0umypNqtf85mgrk-cTXnFPEa98bvBtuvBFgr3hjK86hxtiRzpz2D080pMJoD6QJkrH5PEoN2alKoBf021WSpcuRR6-1bKIHWZg4yKNeyX5sszJ60XC8FVfqSkhzsklSOaYPmM_wj-7wxAl-cw0M-Vwum_DTRNoco-hewCosBqyQMvm-GcVf25fRxqUvhuzuta0zzFZw3AydnSoPx52Q33rc8TDaVYovl4NqDNgPqDahEC4LRLFLj5xmP8qLgeUYd8GwkWtmTemp-o8ISRd18Vl5pa0Qr70RMJvSj5iQdoZ3gv7BjR0yrGSrGsp-6pnB0mPIoETNtC7B3CD_sPR5zAYj69SFpWJ4uQkGomihPxxrT4gA_OLnScCvP-LAJb4607uxJelR2cXqxVtQQQ4tP8fUD-8Xm6ZR0owKy0I8HjNhu_eRnscdUa9vq06NrwysTqguODz8j7VsaBu_uBY-Io4Ck8AnVxSz5lLnxZWBqTrxZfy5wXGPjvPx4l6FmHro9oC8BUueA5XO6OB11pqnsWU84gZQCcj6HACagJtNkm2N4eP1xiDPNPUbOIHQscizNLimeo4gx1NrUqArfJezZxOl3LR4AD9WpPP0y-5aSv6uVqySZ1k8PiA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O8gPZN8crDuWCX1gig0M5wt8j0MHprTVwpZe6JrCp5tHpVanCk5uuLOHp1HMU9sKRanpYGqb4K7M2RyYtXZog0YYIKGFAcxlklNIjc2javy3Vj4oM9am8XnSLY5jLQXpUfRW-3mhWzekJtQn68XpOr6cDyy_TFSVuS0YCBC1AX4Kw,,&b64e=1&sign=bfa0f67a28add83bc2194d1f2bbea7be&keyno=1',
                        direct:
                            'https://мой-пк.рус/catalog/kompyuternaya_tekhnika/kompyuternye_komplektuyushchie/zhestkie_diski_ssd_i_setevye_nakopiteli/6794/',
                    },
                },
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        isLeaf: true,
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 500,
                            containerHeight: 500,
                            url: '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/orig',
                            width: 500,
                            height: 500,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/50x50',
                                width: 50,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/55x70',
                                width: 70,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/60x80',
                                width: 80,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/74x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/75x75',
                                width: 75,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/90x120',
                                width: 120,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/100x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/120x160',
                                width: 160,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/150x150',
                                width: 150,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/180x240',
                                width: 240,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/190x250',
                                width: 250,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/200x200',
                                width: 200,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/240x320',
                                width: 320,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/300x300',
                                width: 300,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1936023/market_T8qLPn-0xCOS_d7onxWX3Q/300x400',
                                width: 400,
                                height: 400,
                            },
                        ],
                        signatures: [],
                    },
                ],
                filters: [
                    {
                        id: '5127047',
                        type: 'number',
                        name: 'Емкость (точно)',
                        xslname: 'Capacity',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        unit: 'ГБ',
                        position: 5,
                        noffers: 1,
                        precision: 2,
                        values: [
                            {
                                ranges: '1.2, 300, 600, 1800, 5000',
                                max: '2000',
                                initialMax: '2000',
                                initialMin: '2000',
                                min: '2000',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127084',
                        type: 'number',
                        name: 'Количество пластин',
                        xslname: 'NumOfDisks',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 49,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 2, 7',
                                max: '1',
                                initialMax: '1',
                                initialMin: '1',
                                min: '1',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127083',
                        type: 'number',
                        name: 'Количество головок',
                        xslname: 'NumOfHeads',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 50,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 4, 16',
                                max: '2',
                                initialMax: '2',
                                initialMin: '2',
                                min: '2',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                ],
                meta: {},
                marketSkuCreator: 'market',
                model: {
                    id: 130084187,
                },
                isCutPrice: false,
                delivery: {
                    shopPriorityRegion: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    shopPriorityCountry: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {
                                genitive: 'России',
                                preposition: 'в',
                                prepositional: 'России',
                                accusative: 'Россию',
                            },
                        },
                        type: 3,
                    },
                    isPriorityRegion: true,
                    isCountrywide: true,
                    isAvailable: true,
                    hasPickup: true,
                    hasLocalStore: true,
                    hasPost: false,
                    isForcedRegion: false,
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    availableServices: [
                        {
                            serviceId: 99,
                            serviceName: 'Собственная служба',
                        },
                    ],
                    price: {
                        currency: 'RUR',
                        value: '300',
                        isDeliveryIncluded: false,
                        isPickupIncluded: false,
                    },
                    isFree: false,
                    isDownloadable: false,
                    inStock: true,
                    postAvailable: true,
                    options: [
                        {
                            price: {
                                currency: 'RUR',
                                value: '300',
                                isDeliveryIncluded: false,
                                isPickupIncluded: false,
                            },
                            dayFrom: 0,
                            dayTo: 0,
                            orderBefore: '20',
                            isDefault: true,
                            serviceId: '99',
                            tariffId: 4101605,
                            paymentMethods: ['CASH_ON_DELIVERY'],
                            partnerType: 'regular',
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    pickupOptions: [
                        {
                            serviceId: 99,
                            serviceName: 'Собственная служба',
                            tariffId: 0,
                            partnerType: 'regular',
                            price: {
                                currency: 'RUR',
                                value: '0',
                            },
                            dayFrom: 0,
                            dayTo: 0,
                            orderBefore: 24,
                            groupCount: 1,
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    deliveryPartnerTypes: ['SHOP'],
                },
                shop: {
                    entity: 'shop',
                    id: 556488,
                    name: 'Мой ПК',
                    business_id: 780646,
                    business_name: 'мой-пк.рус',
                    slug: 'moi-pk',
                    gradesCount: 1404,
                    overallGradesCount: 1404,
                    qualityRating: 5,
                    isGlobal: false,
                    isCpaPrior: false,
                    isCpaPartner: true,
                    isNewRating: true,
                    newGradesCount: 1404,
                    newQualityRating: 4.69017094,
                    newQualityRating3M: 4.613445378,
                    ratingToShow: 4.613445378,
                    ratingType: 3,
                    newGradesCount3M: 119,
                    status: 'actual',
                    cutoff: '',
                    outletsCount: 1,
                    storesCount: 1,
                    pickupStoresCount: 0,
                    depotStoresCount: 0,
                    postomatStoresCount: 0,
                    bookNowStoresCount: 0,
                    subsidies: false,
                    logo: {
                        entity: 'picture',
                        width: 112,
                        height: 14,
                        url:
                            '//avatars.mds.yandex.net/get-market-shop-logo/1523528/2a00000169968860384feaabfb5b28177afe/small',
                        extension: 'PNG',
                        thumbnails: [
                            {
                                entity: 'thumbnail',
                                id: '112x14',
                                containerWidth: 112,
                                containerHeight: 14,
                                width: 112,
                                height: 14,
                                densities: [
                                    {
                                        entity: 'density',
                                        id: '1',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1523528/2a00000169968860384feaabfb5b28177afe/small',
                                    },
                                    {
                                        entity: 'density',
                                        id: '2',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1523528/2a00000169968860384feaabfb5b28177afe/orig',
                                    },
                                ],
                            },
                        ],
                    },
                    domainUrl: 'мой-пк.рус',
                    feed: {
                        id: '569420',
                        offerId: '6794',
                        categoryId: '435',
                    },
                    createdAt: '2019-02-11T14:27:41',
                    mainCreatedAt: '2019-02-11T14:27:41',
                    homeRegion: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {},
                        },
                        type: 0,
                    },
                },
                returnPolicy: '7d',
                wareId: 'O4M2Uy2lJTqZ1ZIBWTVAkg',
                offerColor: 'white',
                isFreeOffer: false,
                classifierMagicId: 'e3dd0ff69a3e0b23f35c2adb0a1ee601',
                prices: {
                    currency: 'RUR',
                    value: '4125',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                    rawValue: '4125',
                },
                manufacturer: {
                    entity: 'manufacturer',
                    warranty: true,
                },
                seller: {
                    comment: 'Нал/Безнал, Картой на сайте, Быстрая доставка 1-5ч',
                    price: '4125',
                    currency: 'RUR',
                    sellerToUserExchangeRate: 1,
                },
                payments: {
                    deliveryCard: false,
                    deliveryCash: true,
                    prepaymentCard: true,
                    prepaymentOther: false,
                },
                isRecommendedByVendor: false,
                bundleCount: 1,
                bundled: {
                    modelId: 130084187,
                    count: 1,
                },
                outlet: {
                    entity: 'outlet',
                    id: '62584132',
                    name: 'Мой ПК',
                    purpose: ['pickup', 'store'],
                    daily: true,
                    'around-the-clock': false,
                    gpsCoord: {
                        longitude: '37.5938579',
                        latitude: '55.79275551',
                    },
                    isMarketBranded: false,
                    type: 'mixed',
                    paymentMethods: ['CASH_ON_DELIVERY'],
                    serviceId: 99,
                    serviceName: 'Собственная служба',
                    isMegaPoint: false,
                    email: '',
                    shop: {
                        id: 556488,
                    },
                    address: {
                        fullAddress: 'Москва, Сущевский вал, д. 9',
                        country: '',
                        region: '',
                        locality: 'Москва',
                        street: 'Сущевский вал',
                        km: '',
                        building: '9',
                        block: '',
                        wing: '',
                        estate: '',
                        entrance: '',
                        floor: '',
                        room: '',
                        office_number: '',
                        note: 'офис 205',
                    },
                    telephones: [
                        {
                            entity: 'telephone',
                            countryCode: '7',
                            cityCode: '495',
                            telephoneNumber: '6419947',
                            extensionNumber: '',
                        },
                        {
                            entity: 'telephone',
                            countryCode: '7',
                            cityCode: '495',
                            telephoneNumber: '6419957',
                            extensionNumber: '',
                        },
                    ],
                    workingTime: [
                        {
                            daysFrom: '1',
                            daysTo: '1',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '2',
                            daysTo: '2',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '3',
                            daysTo: '3',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '4',
                            daysTo: '4',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '5',
                            daysTo: '5',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '6',
                            daysTo: '6',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '7',
                            daysTo: '7',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                    ],
                    selfDeliveryRule: {
                        workInHoliday: true,
                        currency: 'RUR',
                        cost: '0',
                        shipperHumanReadableId: 'Self',
                        partnerType: 'regular',
                    },
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    deliveryServiceOutletCode: '',
                },
                prepayEnabled: false,
                promoCodeEnabled: false,
                feedGroupId: '0',
                isFulfillment: false,
                isAdult: false,
                isSMB: false,
                isGoldenMatrix: false,
            },
            {
                showUid: '16124480810149912768605002',
                entity: 'offer',
                trace: {
                    factors: {
                        CATEG_CLICKS: 3759,
                        SHOP_CTR: 0.007844789885,
                        NUMBER_OFFERS: 81,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpaBuy',
                            name: 'MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax',
                            value: '0.714985',
                        },
                        {
                            tag: 'CpcClick',
                            name: 'MNA_sovetnik_ctr',
                            value: '0.0119139',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 686779,
                    name: 'Seagate',
                    slug: 'seagate',
                    website: 'https://www.seagate.com/ru/ru',
                    logo: {
                        entity: 'picture',
                        url: '//avatars.mds.yandex.net/get-mpic/1705137/img_id474286622556761381.png/orig',
                        thumbnails: [],
                        signatures: [],
                    },
                    filter: '7893318:686779',
                },
                titles: {
                    raw: 'ST2000DM008 Жесткий диск Seagate 2000ГБ Barracuda 7200 3.5"',
                    highlighted: [
                        {
                            value: 'ST2000DM008 Жесткий диск Seagate 2000ГБ Barracuda 7200 3.5"',
                        },
                    ],
                },
                slug: 'st2000dm008-zhestkii-disk-seagate-2000gb-barracuda-7200-3-5',
                description: 'Жесткий диск Seagate ST2000DM008 2000ГБ Barracuda 7200 3.5" 7200RPM 256MB SATA III',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 91033,
                        nid: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                cpc:
                    'Yx7_CdRzDvaKRCxoqwunlSsxpfNLpMpq9rWNlXZPddgyXAZrvJO2fSW1OPhpx05WYMOSe9f7ZMW2P1Rc1UqYckdI31J-A_RMw_LTurfGizI4gLNBvMB1lw,,',
                urls: {
                    callPhone:
                        '/redir/GAkkM7lQwz7vv7M_pnW8mQf1kAyuyes1H4tQTQ_ehlpKO0IeSZkI2ybDf7i97yIJk5BopXL4aQt0rVp-m66j07rrAl9EmVOY44kCfegt4hC6GZLlpw8xg1DzUuGdkFhbdWZzhpoIsJ95urCkowWHqnxRbfvIYOz4lt06TZLw32AuGz2NLn7fzigW1U83s1awq7paELfyQY3MA8u79e88R2NYnU2nEWAPMg1aM3Qv-8IpztnKMIgSolgKXnf8f0Eacqf6toBfhoTCZwXw-u81EUEMm3CkLX6mdu4y4nWPid_2UuR9-D4y_BnHY0eWMAi-pHVW2-kvCdHr_9csXY7hYD35jkawTXzQOgJJpzGpNoAieOl4kDgz2TxtUuhB83cfzcSXMa4ZCdfjUqw349Lf4QpIQKs9kH9trGVZzl3YB77i3-VD9kGqCCFFgQqUNf2tSMN42e6pJ8cfEO8Zg0FntTcPCuPlo-5F1Z7L_n2jxx7QcgugW8AWmxKm2C_-e4feW-qA10RJ_A230ozcJGj2brlYaKU1VK6-scBEBKbpbH1hJJrEs3s_EJfC9AlBuLzfDEFTE151ReULGrXadhNfIo0zAmSQKzKZnl3EJAshTy4idq-6iObOO2qkZT7cyFiAAkGlzXwBGBPCb_-6A-theC3eg8DDXMdeHFOR-ccyePjOgMLp8v_W5ReJzoOc-CI03LFw4Br77dmm1EkVfgS8O-KuD6kfwaC4mHXcGFif4QICmL-T2E7fhPHb14xJLtJY9cWL3MB9HFox7jpNLoIRyIGiWDFnhV9TP188I0HPXoPF3fOHchfxqQm8n0qV89fGpU7KeUdSRmx4D-YxX2qmgxuIvCmYKtfv0C5eap7-dakwAH0AE6yz4A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-NGDkqX7z3JInrPVMJJPHZAYDR3jv9Yufv-grv9j7W4WYU6DXp9CBKkIYuWJ8M-yjmgbZRXZIIl-JOME_nLIcj8T67Si54xFGvFxuOVGAM_UoySiwP5LUdQcfbz89YLprb6ptkoG7fxZYcFdftIYITjK3Dzynwi0daS8KqZWy6Cg,,&b64e=1&sign=a40d70a4e102a97b900ca357f5d2e32a&keyno=1',
                    direct:
                        'https://impulsteh.ru/st2000dm008-zhestkij-disk-seagate-2000gb-barracuda-7200-3-5.html?utm_source=yandex_market&utm_medium=cpc&utm_campaign=shukshin',
                },
                urlsByPp: {
                    480: {
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mQf1kAyuyes1H4tQTQ_ehlpKO0IeSZkI2ybDf7i97yIJk5BopXL4aQt0rVp-m66j07rrAl9EmVOY44kCfegt4hC6GZLlpw8xg1DzUuGdkFhbdWZzhpoIsJ95urCkowWHqnxRbfvIYOz4lt06TZLw32AuGz2NLn7fzigW1U83s1awq7paELfyQY3MA8u79e88R2NYnU2nEWAPMg1aM3Qv-8IpztnKMIgSolgKXnf8f0Eacqf6toBfhoTCZwXw-u81EUEMm3CkLX6mdu4y4nWPid_2UuR9-D4y_BnHY0eWMAi-pHVW2-kvCdHr_9csXY7hYD35jkawTXzQOgJJpzGpNoAieOl4kDgz2TxtUuhB83cfzcSXMa4ZCdfjUqw349Lf4QpIQKs9kH9trGVZzl3YB77i3-VD9kGqCCFFgQqUNf2tSMN42e6pJ8cfEO8Zg0FntTcPCuPlo-5F1Z7L_n2jxx7QcgugW8AWmxKm2C_-e4feW-qA10RJ_A230ozcJGj2brlYaKU1VK6-scBEBKbpbH1hJJrEs3s_EJfC9AlBuLzfDEFTE151ReULGrXadhNfIo0zAmSQKzKZnl3EJAshTy4idq-6iObOO2qkZT7cyFiAAkGlzXwBGBPCb_-6A-theC3eg8DDXMdeHFOR-ccyePjOgMLp8v_W5ReJzoOc-CI03LFw4Br77dmm1EkVfgS8O-KuD6kfwaC4mHXcGFif4QICmL-T2E7fhPHb14xJLtJY9cWL3MB9HFox7jpNLoIRyIGiWDFnhV9TP188I0HPXoPF3fOHchfxqQm8n0qV89fGpU7KeUdSRmx4D-YxX2qmgxuIvCmYKtfv0C5eap7-dakwAH0AE6yz4A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-NGDkqX7z3JInrPVMJJPHZAYDR3jv9Yufv-grv9j7W4WYU6DXp9CBKkIYuWJ8M-yjmgbZRXZIIl-JOME_nLIcj8T67Si54xFGvFxuOVGAM_UoySiwP5LUdQcfbz89YLprb6ptkoG7fxZYcFdftIYITjK3Dzynwi0daS8KqZWy6Cg,,&b64e=1&sign=a40d70a4e102a97b900ca357f5d2e32a&keyno=1',
                        direct:
                            'https://impulsteh.ru/st2000dm008-zhestkij-disk-seagate-2000gb-barracuda-7200-3-5.html?utm_source=yandex_market&utm_medium=cpc&utm_campaign=shukshin',
                    },
                    481: {
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mQf1kAyuyes1H4tQTQ_ehlpKO0IeSZkI2ybDf7i97yIJk5BopXL4aQt0rVp-m66j07rrAl9EmVOY44kCfegt4hC6GZLlpw8xg1DzUuGdkFhbdWZzhpoIsJ95urCkowWHqnxRbfvIYOz4lt06TZLw32AuGz2NLn7fzigW1U83s1awq7paELfyQY3MA8u79e88R2NYnU2nEWAPMg1aM3Qv-8IpztnKMIgSolgKXnf8f0Eacqf6toBfhoTCZwXw-u81ET-3YLvPwwtoHFtODHfKOHVDR4Z-UkGldjoE47MItjeqGpJ6HuQey0l0uWeTSfojKIXUV3oUBopMPEOj2svuBC25fan3hmaNeZsfEhRIHyeJ5tMhLsZBewFkGsIj2X_DGFMB7ZCZTk7sOvFA_RE8BMDDXjBBPp84MEir4uJFrih7V3SgTvDlfbV7dVzvdufVBND7Uf4wHhJz78sSZhjpYqQzf0QzZlmc6UJwmyVHu5Ebz0ey4w5VP0L-KEyOxhDXm0oR4PrVAKJ41knmQm7h2mfQJ9VjEQCg3DC1OYh6b6IZm50KLdnaGrsUZDE7jRl88WrxDd7KtXU_1IXBQAeMJpncTcPUMc26-dbmTI1hXUA3N4URmeJrjzhDuqm1P_SrxLlInAtZU_MfhGxrNjquq6Pdh29gX3MaFjYzNuQdwLNc8SdTGXcUMwLYWt13jNT2Nff9ZrQ-WhJXfeYkpYMHudkaTLJIG_ldHswBVSTav_-QW7oUsu8ohQUxWupugjQoLiJtEdKgU-IDlFDLfw3JLBFYu8UQ0J29MvyYCPj8iPhXIiX4pewdk45bGk58x2TazyIFFl_orP8C9rXk69TPntZpyYWtonkD1w,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-NGDkqX7z3JInrPVMJJPHZAYDR3jv9Yufv-grv9j7W4WYU6DXp9CBKkIYuWJ8M-yjmgbZRXZIIl-JOME_nLIcj8T67Si54xFGvFxuOVGAM_UoySiwP5LUdQcfbz89YLprb6ptkoG7fxZYcFdftIYITjK3Dzynwi0daS8KqZWy6Cg,,&b64e=1&sign=cf6862842a1361310ea0dd52bf995e11&keyno=1',
                        direct:
                            'https://impulsteh.ru/st2000dm008-zhestkij-disk-seagate-2000gb-barracuda-7200-3-5.html?utm_source=yandex_market&utm_medium=cpc&utm_campaign=shukshin',
                    },
                },
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        isLeaf: true,
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 1500,
                            containerHeight: 1500,
                            url: '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/orig',
                            width: 1500,
                            height: 1500,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/50x50',
                                width: 50,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/55x70',
                                width: 70,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/60x80',
                                width: 80,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/74x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/75x75',
                                width: 75,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/90x120',
                                width: 120,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/100x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/120x160',
                                width: 160,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/150x150',
                                width: 150,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/180x240',
                                width: 240,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/190x250',
                                width: 250,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/200x200',
                                width: 200,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/240x320',
                                width: 320,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/300x300',
                                width: 300,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/300x400',
                                width: 400,
                                height: 400,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 600,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/600x600',
                                width: 600,
                                height: 600,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 800,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/600x800',
                                width: 800,
                                height: 800,
                            },
                            {
                                containerWidth: 900,
                                containerHeight: 1200,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1662968/market_LavjRnRilP00GhTCswJqfQ/900x1200',
                                width: 1200,
                                height: 1200,
                            },
                        ],
                        signatures: [],
                    },
                ],
                filters: [
                    {
                        id: '5127047',
                        type: 'number',
                        name: 'Емкость (точно)',
                        xslname: 'Capacity',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        unit: 'ГБ',
                        position: 5,
                        noffers: 1,
                        precision: 2,
                        values: [
                            {
                                ranges: '1.2, 300, 600, 1800, 5000',
                                max: '2000',
                                initialMax: '2000',
                                initialMin: '2000',
                                min: '2000',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127084',
                        type: 'number',
                        name: 'Количество пластин',
                        xslname: 'NumOfDisks',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 49,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 2, 7',
                                max: '1',
                                initialMax: '1',
                                initialMin: '1',
                                min: '1',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127083',
                        type: 'number',
                        name: 'Количество головок',
                        xslname: 'NumOfHeads',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 50,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 4, 16',
                                max: '2',
                                initialMax: '2',
                                initialMin: '2',
                                min: '2',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                ],
                meta: {},
                marketSkuCreator: 'market',
                model: {
                    id: 130084187,
                },
                isCutPrice: false,
                delivery: {
                    shopPriorityRegion: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    shopPriorityCountry: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {
                                genitive: 'России',
                                preposition: 'в',
                                prepositional: 'России',
                                accusative: 'Россию',
                            },
                        },
                        type: 3,
                    },
                    isPriorityRegion: true,
                    isCountrywide: true,
                    isAvailable: true,
                    hasPickup: false,
                    hasLocalStore: false,
                    hasPost: false,
                    isForcedRegion: false,
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    availableServices: [
                        {
                            serviceId: 99,
                            serviceName: 'Собственная служба',
                        },
                    ],
                    price: {
                        currency: 'RUR',
                        value: '500',
                        isDeliveryIncluded: false,
                        isPickupIncluded: false,
                    },
                    isFree: false,
                    isDownloadable: false,
                    inStock: false,
                    postAvailable: true,
                    options: [
                        {
                            price: {
                                currency: 'RUR',
                                value: '500',
                                isDeliveryIncluded: false,
                                isPickupIncluded: false,
                            },
                            dayFrom: 4,
                            dayTo: 6,
                            isDefault: true,
                            serviceId: '99',
                            tariffId: 4030721,
                            paymentMethods: ['CASH_ON_DELIVERY'],
                            partnerType: 'regular',
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    deliveryPartnerTypes: ['SHOP'],
                },
                shop: {
                    entity: 'shop',
                    id: 208256,
                    name: 'Импульс Тех',
                    business_id: 899388,
                    business_name: 'Бахтин Александр Сергеевич',
                    slug: 'impuls-tekh',
                    gradesCount: 963,
                    overallGradesCount: 963,
                    qualityRating: 4,
                    isGlobal: false,
                    isCpaPrior: false,
                    isCpaPartner: true,
                    isNewRating: true,
                    newGradesCount: 963,
                    newQualityRating: 4.152647975,
                    newQualityRating3M: 4.104166667,
                    ratingToShow: 4.104166667,
                    ratingType: 3,
                    newGradesCount3M: 48,
                    status: 'actual',
                    cutoff: '',
                    outletsCount: 0,
                    storesCount: 0,
                    pickupStoresCount: 0,
                    depotStoresCount: 0,
                    postomatStoresCount: 0,
                    bookNowStoresCount: 0,
                    subsidies: false,
                    domainUrl: 'impulsteh.ru',
                    feed: {
                        id: '757000',
                        offerId: '34273',
                        categoryId: '227',
                    },
                    createdAt: '2014-02-20T01:05:54',
                    mainCreatedAt: '2014-02-20T01:05:54',
                    homeRegion: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {},
                        },
                        type: 0,
                    },
                },
                returnPolicy: '7d',
                wareId: 'SNAHqKyTdVkJKN7vdfwFXQ',
                offerColor: 'white',
                isFreeOffer: false,
                classifierMagicId: 'a05e963d8ede1a4ca5130a76f9962e6c',
                prices: {
                    currency: 'RUR',
                    value: '3996',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                    rawValue: '3996',
                },
                manufacturer: {
                    entity: 'manufacturer',
                    warranty: false,
                },
                seller: {
                    price: '3995.62',
                    currency: 'RUR',
                    sellerToUserExchangeRate: 1,
                },
                payments: {
                    deliveryCard: false,
                    deliveryCash: true,
                    prepaymentCard: false,
                    prepaymentOther: false,
                },
                isRecommendedByVendor: false,
                bundleCount: 1,
                bundled: {
                    modelId: 130084187,
                    count: 1,
                },
                prepayEnabled: false,
                promoCodeEnabled: false,
                feedGroupId: '0',
                isFulfillment: false,
                isAdult: false,
                isSMB: false,
                isGoldenMatrix: false,
            },
            {
                showUid: '16124480810149912768613003',
                entity: 'offer',
                trace: {
                    factors: {
                        CATEG_CLICKS: 3759,
                        SHOP_CTR: 0.04408345744,
                        NUMBER_OFFERS: 81,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpaBuy',
                            name: 'MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax',
                            value: '0.757383',
                        },
                        {
                            tag: 'CpcClick',
                            name: 'MNA_sovetnik_ctr',
                            value: '0.00711275',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 686779,
                    name: 'Seagate',
                    slug: 'seagate',
                    website: 'https://www.seagate.com/ru/ru',
                    logo: {
                        entity: 'picture',
                        url: '//avatars.mds.yandex.net/get-mpic/1705137/img_id474286622556761381.png/orig',
                        thumbnails: [],
                        signatures: [],
                    },
                    filter: '7893318:686779',
                },
                titles: {
                    raw: 'Жесткий диск 2Tb SATA-III Seagate Barracuda (ST2000DM008)',
                    highlighted: [
                        {
                            value: 'Жесткий диск 2Tb SATA-III Seagate Barracuda (ST2000DM008)',
                        },
                    ],
                },
                slug: 'zhestkii-disk-2tb-sata-iii-seagate-barracuda-st2000dm008',
                description:
                    'жесткий диск для настольного компьютера объем 2000 ГБ форм-фактор 3.5" интерфейс SATA 6Gbit/s',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 91033,
                        nid: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                cpc:
                    'Zejig4QzrpXEdcaQiMWG-a4QTT0jXs9CKYQQQpKxHTYCIV8YQGPbONGc-VRlGgxgQwVAFEdrYpPHeNiXc_wi1GGOliB3Y01o5nIt3oiD2usGoZWNGM1FxQ,,',
                urls: {
                    pickupGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4RWC3n1j_EbH6bIjYDjpOO8iDyMC71VcS2Kht337rPjbeRpJROXhEuPEFXnGxcGhNCdUPOlWONE15qf1hX-VSXyVGR7cbi9Uzc7h7uV7Tq48jizKSe6dnMZty8nkR1mekfID3DqCsSP97IITMXcqEg_vr9g017k3f8vukE-xZUWi-QMJiO_AUYQHEUShE4Fi3DCkEfv-iTTIIxn_qZufaXuNGu7nvFTtNKNXY3ebChEOX2ocUsM_UG424B-wEK_4Uex3tJtP-NmoYEwppmrckzQmVZW8HdaCNz2M3XiSIUq9iEhAl6aLa7OyxD2oTKxIQPf5JJeavVS_fc563_1b_xHcEFhBt7XDu27xLrBF9i-gpwYqtD57fTeACArvaP5l52fZZVCFOMhuvwdCcyw6XXoIOOZwQPUH8iPTWDXh6aB35Edc_AnF9SvVBA0JFDrHB188ln7S8TItUr7B0ss19WKfN7CSBhdzWrBMPBAneJq7ndBIZ1sBPf5c8E6kr0DCGKOGl6C8NNIZLLRSDRABtD-H4azRC3qa14MteLxSCU6j7648ljkVrNYCMUL07zsSLKFiXfjo2ptWNDxSB015wqp-jSYrjgktMYV5Zwnqb48DK1BBTxfMhGsTcuDbqfCbRJykspHOcUGjcAErwgp7Y4g_gRXWFNLhZLFKMiLHDXjlbF2ANxrxqjnyymqW-AKS-jCYN8pkYh-eED2evsZaTW-Abdp13OQs6yqEIDTRkoHuYsp4Aw4SnXeY2LYZTlgXNwh-vgYyEATILJHwUWxxLn9N0D_RgI0PM8ykz4QR4MvKqSN-litRTd64sP1qMsWvx45ssPfcz3LYfxQbcJ1cooBHXXSYXBb5hQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a1hh39OU-bYf61S-gtgFCvlQ6Yiel0x4sGQfvkIalcHCKpz6lbH-qVhGE3j3-qR5u-iHofUkOW8S0kHKTZS5bHxBzk5BYCUzyHLtFy1w9fA1FGcaNWfTUU,&b64e=1&sign=97d73521307b9d6d4807b2b0cf2300c6&keyno=1',
                    storeGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4RWC3n1j_EbH6bIjYDjpOO8iDyMC71VcS2Kht337rPjbeRpJROXhEuPEFXnGxcGhNCdUPOlWONE15qf1hX-VSXyVGR7cbi9Uzc7h7uV7Tq48jizKSe6dnMZty8nkR1mekfID3DqCsSP97IITMXcqEg_vr9g017k3f8vukE-xZUWi-QMJiO_AUYQHEUShE4Fi3CIpyCDlYsssCR3FbaelK4J8kRxLKTHqUUqQrfEY3nR4OfCFAITNs3B_OSeZMIRONEDlZL685vIxSXjz-9B42YbyNCax74NxYdUDNJ4NW_4xBwnFomgL77fnQde8iz6D9zznC6QvAefd5P2oTUHlsiDXA0FW7s8ATKuCLM0E0_k-QMtd73H1zJEoatJmdpa49psWm5BNpGEZlnZ-Y63_Sz8MSlmhNj1UImVLnSSHZtdIiaPYfRaMlqMtrz2CFb3lM6QG6g9TJPIsTU8zdBak7vpNAGKVMyqcV0hp6fQROAvpPWhD0zAcfs7rsWuLTjzeSlEhu8h-RRZ9k_hXIcsqrjUO3Rq6FKYxP8v_f62YWV7RVdAbyN6nc16DWXt1qs3Wj1gPsCx-Fhh16BeNtdtpshtNcHY3m696dbnhkxipzv4rHLv1-hauJXd7p8jDvA2q2-7R7DCH5Bo5vJO9rNqWDnCFOHqqxBsMiMghFFYWfC9wC_QFNUa_1daM5GsavjmdzJmLAswlEHSmrrgAAJlk02n0-hUX2WlDzQNIgqb_O7CkrL6DtBAdFEH4ijSAKXumdVdsUp9q7Zss_filVbBytkONyKSHEYkXi-a2MRv_iyhJcuLKXT3X9rTQgleux4wH1RusJ4fPeInlJaJuXYhR70DTkNb-Lra15w,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a1hh39OU-bYf61S-gtgFCvlQ6Yiel0x4nHvz6v1BxCejWz2gYJ-KTSxEPezULEe5lFhtzXwrazmIZNT1nKg_tPANVyjTxsUJvFU48HIb8VzCPCpQlTcOTU,&b64e=1&sign=7e40e90e18b37a40d65d54b6f377c61c&keyno=1',
                    postomatGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4RWC3n1j_EbH6bIjYDjpOO8iDyMC71VcS2Kht337rPjbeRpJROXhEuPEFXnGxcGhNCdUPOlWONE15qf1hX-VSXyVGR7cbi9Uzc7h7uV7Tq48jizKSe6dnMZty8nkR1mekfID3DqCsSP97IITMXcqEg_vr9g017k3f8vukE-xZUWi-QMJiO_AUYQHEUShE4Fi3HU1lgi1CIb7neNHHQNE6vhL2oCa9aAso5aXX9T0deLQYK5PT1SELhVazIcSrpabuij_WkJkl9yy21RSsoai-DEZQ5ponGysdMx4QWS16e6oPWcq0JqX4jxlbJ92TN3d4ZNub_fE4j_zWO_iEWjn7Wemsi2tXKq18Syj1wrc8mLj4uE4Y8gcNSfCpyqjA1Izs0fo3PY6AKo8MMdwt-38nDZkz7teo-Io27ZaqUTEn0cwA5RmbYuMalx7lo8llP-K2CzGRHPFlEnp1BIuFzfw_oefNTbSvYftiLYrk5gu6il0dT18wdCBldl5t9NCzFNVfDeAQh9FWGEeYLFMw9JXVbm3oWsdN8dAZr9m_3pAVA_8XYteIYf1ZNaCa7rToHxDGwlzCodtuWKfsMl0xlEwkygEbjPTFo5_vbrhVzpXpTy87CBcBJLRJzuTVcpsIojsEWIxiumLUQ775F7KI5AOef4zzAaq0gwniLVupFLpvehBbZjEozh7mBkT9Wkp5c57kbMIIce6c4X9pmPdHJKMgJCeUq7uksfrBSwraWPGNAcZKTUBQamv8LGMiCS4nRX-9umJvlsGtoo6B7g1kaJUOb-fFzlS7zt_KaOJuPOhkhhxBcflPDEk7azIhL9qEAfeL2iR3mQ6Ee1xR1AFpRq8fyaw0aOBSfULmg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a1hh39OU-bYf61S-gtgFCvlQ6Yiel0x4lJedbIRGpwbXMVknwAaAp36plJlCdSdk85z4nkn8MH3713Kjvi2PzGJuyoGq46Uua5buPmhPtwzNayYmywhtFosy5Br7rNV8g,,&b64e=1&sign=5076a2771c06c01a59b1f687981196f3&keyno=1',
                    callPhone:
                        '/redir/GAkkM7lQwz7vv7M_pnW8mbr3ESmKjg1YH6qqjSjplJ1Ts6mUcfliXv2LhD6S_81c5iz0k0YGqnDgQbqqbpvnn1nUCBL4a6ou_nTpP7pvmTZazEMBkD3SUHvrrFMd4dNxFpbfH8SWFATtbaJPVfFi9havp-MatXbI6-9PnibhQghm2Es1q2D3dVyFek-oTF-JyRlN6IQy6QpVKPO1mRmBkXWMCv9lgyZ61rqjvN49GRZGtwrKEjVufjlo81jWuiIekF1x9M1nzhx_qXVUe6Y_5ecW-LaerZq2-CpZXfuEbJAPFBtkzOzH7JYspbn2a55HMO38f4YIfPBd_x_CqjvVw61wCH2PCFzPh-50VpAcs7iLNXaqFZ_MEyBUpDn7xdEbs8buFz9dzKijBjCTsCbofIu4cWQq4HNiPbf-B95xJpUbacOb4VjSTugAT-D6cjgGhQ3mWOnQ4bPWGb0syd1qJW2MhVOYhxwQTZF0I1AaZm99uojkxM-dKlhWlml-1xZvCV93PdSCZvNYg1sIEiAfmAOtBMQN8aEgzl2i0x9V4ydplyClTU0VKqyzWhBRI7QwXGgoEZC8mBCJcw9hoKB9FbyX8rI3zzNZX-ECCNojAs-IiU0txHoOzPERCEX-FivAJCeKT3-KD376g5pd40fTP5jmTEixkLTEsIwwGZGQthtAVfG9ltyPJA3CfAY0rQyIp1YTz3XPOFs2zO73DHEPzLYdys5hFSpyfnDzm-yQops35E3qfeVVSpnqX2TqPtqIgFmltmtRieGK9oqMTBQihmWLjuG7u9TCdT1_HBB-yeyaEv3Nzf7llYHpYxWu_7mAv8YPqpGIUklajGM-dU07_wX1a6wHSyaDoWHoJ7bZBHhEt43_e8I1aw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-3k2ucj-4H0v3oHr1BhwHq5AQ6WiKchUezhLWRB1JhC1AA7kJhbuLT5BKA3hvlXc8LQbS2N19Ak4hLN9IGQViVY1xU7K1AVFz_gAjyWyUBwMWIU-wTTmyOdtE4Qr4cW6mD0Zn1wnxXdUQlZbdmflM_Ie8cdzWhXyrpEkqxwXM8wg,,&b64e=1&sign=c2a08c8e821909b0ed6e0d851d826c62&keyno=1',
                    direct: 'https://compday.ru/actions/280774.htm',
                },
                urlsByPp: {
                    480: {
                        pickupGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4RWC3n1j_EbH6bIjYDjpOO8iDyMC71VcS2Kht337rPjbeRpJROXhEuPEFXnGxcGhNCdUPOlWONE15qf1hX-VSXyVGR7cbi9Uzc7h7uV7Tq48jizKSe6dnMZty8nkR1mekfID3DqCsSP97IITMXcqEg_vr9g017k3f8vukE-xZUWi-QMJiO_AUYQHEUShE4Fi3DCkEfv-iTTIIxn_qZufaXuNGu7nvFTtNKNXY3ebChEOX2ocUsM_UG424B-wEK_4Uex3tJtP-NmoYEwppmrckzQmVZW8HdaCNz2M3XiSIUq9iEhAl6aLa7OyxD2oTKxIQPf5JJeavVS_fc563_1b_xHcEFhBt7XDu27xLrBF9i-gpwYqtD57fTeACArvaP5l52fZZVCFOMhuvwdCcyw6XXoIOOZwQPUH8iPTWDXh6aB35Edc_AnF9SvVBA0JFDrHB188ln7S8TItUr7B0ss19WKfN7CSBhdzWrBMPBAneJq7ndBIZ1sBPf5c8E6kr0DCGKOGl6C8NNIZLLRSDRABtD-H4azRC3qa14MteLxSCU6j7648ljkVrNYCMUL07zsSLKFiXfjo2ptWNDxSB015wqp-jSYrjgktMYV5Zwnqb48DK1BBTxfMhGsTcuDbqfCbRJykspHOcUGjcAErwgp7Y4g_gRXWFNLhZLFKMiLHDXjlbF2ANxrxqjnyymqW-AKS-jCYN8pkYh-eED2evsZaTW-Abdp13OQs6yqEIDTRkoHuYsp4Aw4SnXeY2LYZTlgXNwh-vgYyEATILJHwUWxxLn9N0D_RgI0PM8ykz4QR4MvKqSN-litRTd64sP1qMsWvx45ssPfcz3LYfxQbcJ1cooBHXXSYXBb5hQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a1hh39OU-bYf61S-gtgFCvlQ6Yiel0x4sGQfvkIalcHCKpz6lbH-qVhGE3j3-qR5u-iHofUkOW8S0kHKTZS5bHxBzk5BYCUzyHLtFy1w9fA1FGcaNWfTUU,&b64e=1&sign=97d73521307b9d6d4807b2b0cf2300c6&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4RWC3n1j_EbH6bIjYDjpOO8iDyMC71VcS2Kht337rPjbeRpJROXhEuPEFXnGxcGhNCdUPOlWONE15qf1hX-VSXyVGR7cbi9Uzc7h7uV7Tq48jizKSe6dnMZty8nkR1mekfID3DqCsSP97IITMXcqEg_vr9g017k3f8vukE-xZUWi-QMJiO_AUYQHEUShE4Fi3CIpyCDlYsssCR3FbaelK4J8kRxLKTHqUUqQrfEY3nR4OfCFAITNs3B_OSeZMIRONEDlZL685vIxSXjz-9B42YbyNCax74NxYdUDNJ4NW_4xBwnFomgL77fnQde8iz6D9zznC6QvAefd5P2oTUHlsiDXA0FW7s8ATKuCLM0E0_k-QMtd73H1zJEoatJmdpa49psWm5BNpGEZlnZ-Y63_Sz8MSlmhNj1UImVLnSSHZtdIiaPYfRaMlqMtrz2CFb3lM6QG6g9TJPIsTU8zdBak7vpNAGKVMyqcV0hp6fQROAvpPWhD0zAcfs7rsWuLTjzeSlEhu8h-RRZ9k_hXIcsqrjUO3Rq6FKYxP8v_f62YWV7RVdAbyN6nc16DWXt1qs3Wj1gPsCx-Fhh16BeNtdtpshtNcHY3m696dbnhkxipzv4rHLv1-hauJXd7p8jDvA2q2-7R7DCH5Bo5vJO9rNqWDnCFOHqqxBsMiMghFFYWfC9wC_QFNUa_1daM5GsavjmdzJmLAswlEHSmrrgAAJlk02n0-hUX2WlDzQNIgqb_O7CkrL6DtBAdFEH4ijSAKXumdVdsUp9q7Zss_filVbBytkONyKSHEYkXi-a2MRv_iyhJcuLKXT3X9rTQgleux4wH1RusJ4fPeInlJaJuXYhR70DTkNb-Lra15w,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a1hh39OU-bYf61S-gtgFCvlQ6Yiel0x4nHvz6v1BxCejWz2gYJ-KTSxEPezULEe5lFhtzXwrazmIZNT1nKg_tPANVyjTxsUJvFU48HIb8VzCPCpQlTcOTU,&b64e=1&sign=7e40e90e18b37a40d65d54b6f377c61c&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4RWC3n1j_EbH6bIjYDjpOO8iDyMC71VcS2Kht337rPjbeRpJROXhEuPEFXnGxcGhNCdUPOlWONE15qf1hX-VSXyVGR7cbi9Uzc7h7uV7Tq48jizKSe6dnMZty8nkR1mekfID3DqCsSP97IITMXcqEg_vr9g017k3f8vukE-xZUWi-QMJiO_AUYQHEUShE4Fi3HU1lgi1CIb7neNHHQNE6vhL2oCa9aAso5aXX9T0deLQYK5PT1SELhVazIcSrpabuij_WkJkl9yy21RSsoai-DEZQ5ponGysdMx4QWS16e6oPWcq0JqX4jxlbJ92TN3d4ZNub_fE4j_zWO_iEWjn7Wemsi2tXKq18Syj1wrc8mLj4uE4Y8gcNSfCpyqjA1Izs0fo3PY6AKo8MMdwt-38nDZkz7teo-Io27ZaqUTEn0cwA5RmbYuMalx7lo8llP-K2CzGRHPFlEnp1BIuFzfw_oefNTbSvYftiLYrk5gu6il0dT18wdCBldl5t9NCzFNVfDeAQh9FWGEeYLFMw9JXVbm3oWsdN8dAZr9m_3pAVA_8XYteIYf1ZNaCa7rToHxDGwlzCodtuWKfsMl0xlEwkygEbjPTFo5_vbrhVzpXpTy87CBcBJLRJzuTVcpsIojsEWIxiumLUQ775F7KI5AOef4zzAaq0gwniLVupFLpvehBbZjEozh7mBkT9Wkp5c57kbMIIce6c4X9pmPdHJKMgJCeUq7uksfrBSwraWPGNAcZKTUBQamv8LGMiCS4nRX-9umJvlsGtoo6B7g1kaJUOb-fFzlS7zt_KaOJuPOhkhhxBcflPDEk7azIhL9qEAfeL2iR3mQ6Ee1xR1AFpRq8fyaw0aOBSfULmg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a1hh39OU-bYf61S-gtgFCvlQ6Yiel0x4lJedbIRGpwbXMVknwAaAp36plJlCdSdk85z4nkn8MH3713Kjvi2PzGJuyoGq46Uua5buPmhPtwzNayYmywhtFosy5Br7rNV8g,,&b64e=1&sign=5076a2771c06c01a59b1f687981196f3&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mbr3ESmKjg1YH6qqjSjplJ1Ts6mUcfliXv2LhD6S_81c5iz0k0YGqnDgQbqqbpvnn1nUCBL4a6ou_nTpP7pvmTZazEMBkD3SUHvrrFMd4dNxFpbfH8SWFATtbaJPVfFi9havp-MatXbI6-9PnibhQghm2Es1q2D3dVyFek-oTF-JyRlN6IQy6QpVKPO1mRmBkXWMCv9lgyZ61rqjvN49GRZGtwrKEjVufjlo81jWuiIekF1x9M1nzhx_qXVUe6Y_5ecW-LaerZq2-CpZXfuEbJAPFBtkzOzH7JYspbn2a55HMO38f4YIfPBd_x_CqjvVw61wCH2PCFzPh-50VpAcs7iLNXaqFZ_MEyBUpDn7xdEbs8buFz9dzKijBjCTsCbofIu4cWQq4HNiPbf-B95xJpUbacOb4VjSTugAT-D6cjgGhQ3mWOnQ4bPWGb0syd1qJW2MhVOYhxwQTZF0I1AaZm99uojkxM-dKlhWlml-1xZvCV93PdSCZvNYg1sIEiAfmAOtBMQN8aEgzl2i0x9V4ydplyClTU0VKqyzWhBRI7QwXGgoEZC8mBCJcw9hoKB9FbyX8rI3zzNZX-ECCNojAs-IiU0txHoOzPERCEX-FivAJCeKT3-KD376g5pd40fTP5jmTEixkLTEsIwwGZGQthtAVfG9ltyPJA3CfAY0rQyIp1YTz3XPOFs2zO73DHEPzLYdys5hFSpyfnDzm-yQops35E3qfeVVSpnqX2TqPtqIgFmltmtRieGK9oqMTBQihmWLjuG7u9TCdT1_HBB-yeyaEv3Nzf7llYHpYxWu_7mAv8YPqpGIUklajGM-dU07_wX1a6wHSyaDoWHoJ7bZBHhEt43_e8I1aw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-3k2ucj-4H0v3oHr1BhwHq5AQ6WiKchUezhLWRB1JhC1AA7kJhbuLT5BKA3hvlXc8LQbS2N19Ak4hLN9IGQViVY1xU7K1AVFz_gAjyWyUBwMWIU-wTTmyOdtE4Qr4cW6mD0Zn1wnxXdUQlZbdmflM_Ie8cdzWhXyrpEkqxwXM8wg,,&b64e=1&sign=c2a08c8e821909b0ed6e0d851d826c62&keyno=1',
                        direct: 'https://compday.ru/actions/280774.htm',
                    },
                    481: {
                        pickupGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4RWC3n1j_EbH6bIjYDjpOO8iDyMC71VcS2Kht337rPjbeRpJROXhEuPEFXnGxcGhNCdUPOlWONE15qf1hX-VSXyVGR7cbi9Uzc7h7uV7Tq48jizKSe6dnMZty8nkR1mekfID3DqCsSP97IITMXcqEg_vr9g017k3f8vukE-xZUWi-QMJiO_AUYQHEUShE4Fi3DCkEfv-iTTIIxn_qZufaXuNGu7nvFTtNKNXY3ebChEOX2ocUsM_UG424B-wEK_4UbS_ctX30cB1HLVvWRtqO_aT0D61FYvCo07f4g3mxzuwQ9QdkwKmBt_r1riJzBCz5K9-BIp4WJMPg6bQMT4-Sqdjl8a3oTNY_RHPhUKwjWVnMZuFNRXXXihnIeX6Iov_Qp8URj4OrnO-I6JpasONA5ISbzPViCY0XcMt2Jct3kVRnZxYtTSKGSVIDPKs3YI002dEwVhoUPAdRta3ye3JcWWMUpzlW1xFpBhYZxYv4IV_8kw4j5vZsbbDl8Q537LcjE7E20WMpDbxMy2MFWDND3aLFrOdeQAEuh28oduRsZbQU_QB_yEdEwoJFvlLKQwLf_hmvaP0Ortgqe4LYvNbhVaGTeVuVUYng-RP3af7bnkM1hGaiCO8-kaN-BgFe8XlNDrmBJrFif_u73mDnsbHNHlrANRJKT9brOc-A-ZIBl8DiOOQjlCtFkBUQUFDfO8bErHw7vO0wi90IpdxLojKS567KzX8mTzaaPEUVdaKpAjUHWcjD8yewaIKVNCuT6jTmMEgvUVTaKL_b5WcPYLTh99ax2-ngRMuZY3rXb-cDZ3VM34W7ZZjrH9gz7kmg0No66qNrpeX7rzB9FnWeTr06aMAVqak_5rU9Q,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a1hh39OU-bYf61S-gtgFCvlQ6Yiel0x4sGQfvkIalcHCKpz6lbH-qVhGE3j3-qR5u-iHofUkOW8S0kHKTZS5bHxBzk5BYCUzyHLtFy1w9fA1FGcaNWfTUU,&b64e=1&sign=72471257e99c32b939f77b6643ed1f08&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4RWC3n1j_EbH6bIjYDjpOO8iDyMC71VcS2Kht337rPjbeRpJROXhEuPEFXnGxcGhNCdUPOlWONE15qf1hX-VSXyVGR7cbi9Uzc7h7uV7Tq48jizKSe6dnMZty8nkR1mekfID3DqCsSP97IITMXcqEg_vr9g017k3f8vukE-xZUWi-QMJiO_AUYQHEUShE4Fi3CIpyCDlYsssCR3FbaelK4J8kRxLKTHqUUqQrfEY3nR4OfCFAITNs3B_OSeZMIRONNhMOdilXSKgfAvJdMXqTwlbc4AlYTNl6BypX-4SRBJPrOsJIQmRAzFdvLMx-OmFc3YZ_gy_woyiMYZZNl_lygL5B2vLxPFIPuTv1c8v79zuIEL05uJktKvHG4tFeZXv4WslnFATYbxWzcpKj7QcdVB_hv4EvmMbre-os28C53j6Qe9-VCt1ZM_OAlYrDI4L5osQfGTvFCDoyLSoOIeQ3QnqVM8mhoUFE3FTrTjqKxPAw37M0vZj82EvJrV8vOq__XORGBCmVKWQAPhXSVeOKieE6t-5rcoMxDlPu1v4Tp8aUBcGb61Kqu86eZAQwSxC5xnyyiE5WaL-JOSd8CLg0nJupINVghaduoijyTu5hnQ1JJ_u8F4WIYAfv01nP-6XqrVZFLGpaQ9yu6N_fJeF6JUe7gGFliOswkgJqFyLyJOt9Ssl6Ki8Nfa_sDZJSzNEaZaW8hA_B-3vBHspJYKG48yh8M1CFMv-0lu9eFCl7xRc91SNUeXuo7trQUOpWb62d-8XakfEgW9Z_ojjONqkm4MXMKpGC1szrOGdlke79T0c1pHWLCAQhpMlLPL3Q_2VitPe7oqAVLoyepFGH0MuL-5jtcAkEwY7qA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a1hh39OU-bYf61S-gtgFCvlQ6Yiel0x4nHvz6v1BxCejWz2gYJ-KTSxEPezULEe5lFhtzXwrazmIZNT1nKg_tPANVyjTxsUJvFU48HIb8VzCPCpQlTcOTU,&b64e=1&sign=81c527efa5b598ae5463ab7dee434d2f&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4RWC3n1j_EbH6bIjYDjpOO8iDyMC71VcS2Kht337rPjbeRpJROXhEuPEFXnGxcGhNCdUPOlWONE15qf1hX-VSXyVGR7cbi9Uzc7h7uV7Tq48jizKSe6dnMZty8nkR1mekfID3DqCsSP97IITMXcqEg_vr9g017k3f8vukE-xZUWi-QMJiO_AUYQHEUShE4Fi3HU1lgi1CIb7neNHHQNE6vhL2oCa9aAso5aXX9T0deLQYK5PT1SELhVazIcSrpabuste18vqhpxgE1MqAsaZy2jbSzouQl7UfuUYPRR_xN-yfO7taL3uzeiHNb93I_AaGarLFE1v4T_gzCmRg7mzXPmTyMwRH0akJY5IIGmr8dbsOOQIonpLJGtexEbGEvWpZ84HQQSFpbVaDx0RAFFgGB622kBkeyb3fNHX2hdUBELZwghqNhQG7Q2qgLvSKzAW0nnxs8s7kaLhsLeUPZewo8IM5MvXCNaMWaPXOOikgF7m54kd4vUD-F05eponRzpeO730c_WYaA09AJhZzzsCki9p0N2opgT5ycPilyfFiyBa7PgHwq_mrafWd8o4nDE3w9hfyX78OJZy4SZsH1K2Q9h_N_6SPMLM-oNQ2w8dzl8JROv7GmpJpkEZLYLmwhwmJ2Z1ofDVqQLEXpadK0y2Gr1dLGLeFFghELJq-wCFW9ExBIOuT4u8kd8EJhwjbn5CfDdKiKuZrfBjURBOdg9fBuvtdPEKjIOIhvXgkz_Q8tHmwwcpJoHw1OrL4EVwJwzYvO_lMqPOOeDvPiF-dBXa5PDE1y-Crjhn380NDbVcWH4ou8ilGo8-QkA1Haj7CjB_RH9YiTx0SWLtvehR-yv1-nram6FMEh5Axg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a1hh39OU-bYf61S-gtgFCvlQ6Yiel0x4lJedbIRGpwbXMVknwAaAp36plJlCdSdk85z4nkn8MH3713Kjvi2PzGJuyoGq46Uua5buPmhPtwzNayYmywhtFosy5Br7rNV8g,,&b64e=1&sign=740f812c2ffc5febb2705cb65e16b3d4&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mbr3ESmKjg1YH6qqjSjplJ1Ts6mUcfliXv2LhD6S_81c5iz0k0YGqnDgQbqqbpvnn1nUCBL4a6ou_nTpP7pvmTZazEMBkD3SUHvrrFMd4dNxFpbfH8SWFATtbaJPVfFi9havp-MatXbI6-9PnibhQghm2Es1q2D3dVyFek-oTF-JyRlN6IQy6QpVKPO1mRmBkXWMCv9lgyZ61rqjvN49GRZGtwrKEjVufjlo81jWuiIekF1x9M1nzhx_qXVUe6Y_5YafLwK9SFaE-M-VWyQMdO8GzvcPO8obBfAROwPdttlyCwW5YxRazSw6jR1RdPK1wX3DFeAZ0KeUVAM1AtC6h3grQrD9rxTYkSA4-4sQsKwyJvq-Aw6QLV1aqEuFh7H5jmkqkB52w5vohf0UFWFUq_OgMo5PjnzS1Hi3GlWAEDB1T2-V8xNglCXm_v_dfeCp634_u6SJmyJAjyn-B_-T_vLy6nsOb1nb-FaTuMDRZ96M1Bp_ovorHMawTvjURV6MT7VUj6etoeiug1v0xF-zPjiOO3zGbviKn6dvKD1TA79TXcbQ40EGn1S3Xgx26XjLfzTzJHoRLpcw3B-pGMtl9vGOfkr1TaP7OaQeOw1HeFDcMKKImG7CC_9pqYGcbmJvH6dMa3dmrXB1V4zMmT3aax_3OJ1E0pSnMF1NuI9BH6T5Tz7Nr4i8Qng-SWIU3jvELp7kFct6I5zMCPttEo0O4GEfwW-j_EbwB2KZsFixg_HSQuCwy_a2Bzj8tzILG4dRv5Usxm_mNo8x1SvWXhJ51od17pIiYEapX-xgIt1rLi7xUY6k-M6fpg_PtcKyakzq2Bd5KR3jnv0RAppdUvio04VsN92Gd2v0dw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-3k2ucj-4H0v3oHr1BhwHq5AQ6WiKchUezhLWRB1JhC1AA7kJhbuLT5BKA3hvlXc8LQbS2N19Ak4hLN9IGQViVY1xU7K1AVFz_gAjyWyUBwMWIU-wTTmyOdtE4Qr4cW6mD0Zn1wnxXdUQlZbdmflM_Ie8cdzWhXyrpEkqxwXM8wg,,&b64e=1&sign=c6001282b05aeb1d7812cba54f07e073&keyno=1',
                        direct: 'https://compday.ru/actions/280774.htm',
                    },
                },
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        isLeaf: true,
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 493,
                            containerHeight: 701,
                            url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/orig',
                            width: 493,
                            height: 701,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/50x50',
                                width: 35,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/55x70',
                                width: 49,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/60x80',
                                width: 56,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/74x100',
                                width: 70,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/75x75',
                                width: 52,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/90x120',
                                width: 84,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/100x100',
                                width: 70,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/120x160',
                                width: 112,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/150x150',
                                width: 105,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/180x240',
                                width: 168,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/190x250',
                                width: 175,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/200x200',
                                width: 140,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/240x320',
                                width: 225,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/300x300',
                                width: 210,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/300x400',
                                width: 281,
                                height: 400,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 600,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/600x600',
                                width: 421,
                                height: 600,
                            },
                        ],
                        signatures: [],
                    },
                ],
                filters: [
                    {
                        id: '5127047',
                        type: 'number',
                        name: 'Емкость (точно)',
                        xslname: 'Capacity',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        unit: 'ГБ',
                        position: 5,
                        noffers: 1,
                        precision: 2,
                        values: [
                            {
                                ranges: '1.2, 300, 600, 1800, 5000',
                                max: '2000',
                                initialMax: '2000',
                                initialMin: '2000',
                                min: '2000',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127084',
                        type: 'number',
                        name: 'Количество пластин',
                        xslname: 'NumOfDisks',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 49,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 2, 7',
                                max: '1',
                                initialMax: '1',
                                initialMin: '1',
                                min: '1',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127083',
                        type: 'number',
                        name: 'Количество головок',
                        xslname: 'NumOfHeads',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 50,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 4, 16',
                                max: '2',
                                initialMax: '2',
                                initialMin: '2',
                                min: '2',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                ],
                meta: {},
                marketSkuCreator: 'market',
                model: {
                    id: 130084187,
                },
                isCutPrice: false,
                delivery: {
                    shopPriorityRegion: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    shopPriorityCountry: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {
                                genitive: 'России',
                                preposition: 'в',
                                prepositional: 'России',
                                accusative: 'Россию',
                            },
                        },
                        type: 3,
                    },
                    isPriorityRegion: true,
                    isCountrywide: true,
                    isAvailable: true,
                    hasPickup: true,
                    hasLocalStore: true,
                    hasPost: false,
                    isForcedRegion: false,
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    price: {
                        currency: 'RUR',
                        value: '350',
                        isDeliveryIncluded: false,
                        isPickupIncluded: false,
                    },
                    isFree: false,
                    isDownloadable: false,
                    inStock: true,
                    postAvailable: true,
                    options: [
                        {
                            price: {
                                currency: 'RUR',
                                value: '350',
                                isDeliveryIncluded: false,
                                isPickupIncluded: false,
                            },
                            dayFrom: 1,
                            dayTo: 1,
                            isDefault: true,
                            serviceId: '99',
                            paymentMethods: ['CASH_ON_DELIVERY'],
                            partnerType: 'regular',
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    pickupOptions: [
                        {
                            serviceId: 99,
                            serviceName: 'Собственная служба',
                            tariffId: 0,
                            partnerType: 'regular',
                            price: {
                                currency: 'RUR',
                                value: '0',
                            },
                            dayFrom: 0,
                            dayTo: 2,
                            orderBefore: 24,
                            groupCount: 1,
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    deliveryPartnerTypes: ['SHOP'],
                },
                shop: {
                    entity: 'shop',
                    id: 104920,
                    name: 'compday.ru',
                    business_id: 895078,
                    business_name: 'КонтинентЪ',
                    slug: 'compday-ru',
                    gradesCount: 3420,
                    overallGradesCount: 3420,
                    qualityRating: 5,
                    isGlobal: false,
                    isCpaPrior: false,
                    isCpaPartner: true,
                    isNewRating: true,
                    newGradesCount: 3420,
                    newQualityRating: 4.725730994,
                    newQualityRating3M: 4.796803653,
                    ratingToShow: 4.796803653,
                    ratingType: 3,
                    newGradesCount3M: 438,
                    status: 'actual',
                    cutoff: '',
                    outletsCount: 1,
                    storesCount: 1,
                    pickupStoresCount: 0,
                    depotStoresCount: 0,
                    postomatStoresCount: 0,
                    bookNowStoresCount: 0,
                    subsidies: false,
                    logo: {
                        entity: 'picture',
                        width: 59,
                        height: 14,
                        url:
                            '//avatars.mds.yandex.net/get-market-shop-logo/1528691/2a00000167a84ee098984dd4b07f507efbff/small',
                        extension: 'PNG',
                        thumbnails: [
                            {
                                entity: 'thumbnail',
                                id: '59x14',
                                containerWidth: 59,
                                containerHeight: 14,
                                width: 59,
                                height: 14,
                                densities: [
                                    {
                                        entity: 'density',
                                        id: '1',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1528691/2a00000167a84ee098984dd4b07f507efbff/small',
                                    },
                                    {
                                        entity: 'density',
                                        id: '2',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1528691/2a00000167a84ee098984dd4b07f507efbff/orig',
                                    },
                                ],
                            },
                        ],
                    },
                    domainUrl: 'compday.ru',
                    feed: {
                        id: '455636',
                        offerId: '280774',
                        categoryId: '11297',
                    },
                    phones: {
                        raw: '88005002958',
                        sanitized: '88005002958',
                    },
                    createdAt: '2012-05-19T12:41:56',
                    mainCreatedAt: '2012-05-19T12:41:56',
                    homeRegion: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {},
                        },
                        type: 0,
                    },
                },
                returnPolicy: '7d',
                wareId: 'xqIN0ubXb_U1aDgLJuldbw',
                offerColor: 'white',
                isFreeOffer: false,
                classifierMagicId: '9d0d11f167603c02a6ade9829d6b7fca',
                prices: {
                    currency: 'RUR',
                    value: '4149',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                    rawValue: '4149',
                },
                manufacturer: {
                    entity: 'manufacturer',
                    warranty: false,
                },
                seller: {
                    comment: 'м. Савеловская, наличный, б/н расчет для юр. лиц. ',
                    price: '4149',
                    currency: 'RUR',
                    sellerToUserExchangeRate: 1,
                },
                payments: {
                    deliveryCard: false,
                    deliveryCash: false,
                    prepaymentCard: false,
                    prepaymentOther: false,
                },
                isRecommendedByVendor: false,
                bundleCount: 1,
                bundled: {
                    modelId: 130084187,
                    count: 1,
                },
                outlet: {
                    entity: 'outlet',
                    id: '268954',
                    name: 'compday.ru',
                    purpose: ['pickup', 'store'],
                    daily: true,
                    'around-the-clock': false,
                    gpsCoord: {
                        longitude: '37.59187985',
                        latitude: '55.79426385',
                    },
                    isMarketBranded: false,
                    type: 'mixed',
                    paymentMethods: ['CASH_ON_DELIVERY'],
                    serviceId: 99,
                    serviceName: 'Собственная служба',
                    isMegaPoint: false,
                    email: 'mail@compday.ru',
                    shop: {
                        id: 104920,
                    },
                    address: {
                        fullAddress: 'Москва, 127018, Сущевский вал, д. 5, стр. 1а',
                        country: '',
                        region: '',
                        locality: 'Москва',
                        street: '127018, Сущевский вал',
                        km: '',
                        building: '5',
                        block: '',
                        wing: '1а',
                        estate: '',
                        entrance: '',
                        floor: '',
                        room: '',
                        office_number: '',
                        note: 'офис F52',
                    },
                    telephones: [
                        {
                            entity: 'telephone',
                            countryCode: '8',
                            cityCode: '800',
                            telephoneNumber: '500-2958',
                            extensionNumber: '',
                        },
                    ],
                    workingTime: [
                        {
                            daysFrom: '1',
                            daysTo: '1',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '2',
                            daysTo: '2',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '3',
                            daysTo: '3',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '4',
                            daysTo: '4',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '5',
                            daysTo: '5',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '6',
                            daysTo: '6',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '7',
                            daysTo: '7',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                    ],
                    selfDeliveryRule: {
                        workInHoliday: true,
                        currency: 'RUR',
                        cost: '0',
                        shipperHumanReadableId: 'Self',
                        partnerType: 'regular',
                    },
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    deliveryServiceOutletCode: '',
                },
                prepayEnabled: false,
                promoCodeEnabled: false,
                feedGroupId: '0',
                isFulfillment: false,
                isAdult: false,
                isSMB: false,
                isGoldenMatrix: false,
            },
            {
                showUid: '16124480810149912768613004',
                entity: 'offer',
                trace: {
                    factors: {
                        CATEG_CLICKS: 3759,
                        SHOP_CTR: 0.01031796355,
                        NUMBER_OFFERS: 81,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpaBuy',
                            name: 'MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax',
                            value: '0.717304',
                        },
                        {
                            tag: 'CpcClick',
                            name: 'MNA_sovetnik_ctr',
                            value: '0.00422705',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 686779,
                    name: 'Seagate',
                    slug: 'seagate',
                    website: 'https://www.seagate.com/ru/ru',
                    logo: {
                        entity: 'picture',
                        url: '//avatars.mds.yandex.net/get-mpic/1705137/img_id474286622556761381.png/orig',
                        thumbnails: [],
                        signatures: [],
                    },
                    filter: '7893318:686779',
                },
                titles: {
                    raw: 'Жесткий диск SEAGATE Barracuda ST2000DM008, 2ТБ, HDD, SATA III, 3.5"',
                    highlighted: [
                        {
                            value: 'Жесткий диск SEAGATE Barracuda ST2000DM008, 2ТБ, HDD, SATA III, 3.5"',
                        },
                    ],
                },
                slug: 'zhestkii-disk-seagate-barracuda-st2000dm008-2tb-hdd-sata-iii-3-5',
                description:
                    'форм-фактор 3.5"; тип: HDD; интерфейс: SATA III; объём: 2ТБ; скорость вращения шпинделя 7200об/мин; буферная память 256МБ',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 91033,
                        nid: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                cpc:
                    'ekSjspcgYvJVIHYwFtesb-7nAUWDXow5nEQE9xYyXkW2AICLMFdoieLuY1wa26L3fSW51j-7Dwdl0OCQyiZCxd-eEB0Hrq20R8qhQC9WPFKRoolp47E0lQ,,',
                urls: {
                    pickupGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4UFDbqDfG43hso6kYxB8a8uVezXbCG0KaM7jDt9sa5cWdyy93byA8prDpa7WddnnWw4Bm-QWP9vWGDEYiU3Y1WmhW_Ob21ERH_0ttlF14CHnpz2599Z1Gti595b0VWmaIiymixzTWCaY_AOP6nlWeWC2uIwUIOhU3FVSdMshRUkE9S9ILaw2QdsmSb_CBaQ7UcSVLdjQPOCOiWjfiHPE3qhhxMMsjiR5KFcizB9mwtqoETQJOMBtQlAyLIM-Yucm5dSkGlPWzOYSe4_aQ35ZnoSNlWbZBoWFeKUpefcKoSZ5Wk7wtmDVQfzKQI9wT_HJjaIURO4pWyqs0wzZJKv4G_3HPcCBEKRUiwM19b6rwpB4vWJZAkOPKtXU_N2M7WbamL-6Xh64ZM1caQNLyZVu4e83ufcUeia3EO5PSEXipvraruyvjeyCQH8G-7M6viy4cJOebLhEQq1R_rHElt6rCvh-U1--YinuMin7NXaQYptO2w2wEEIo0YhDkKLfxTXdqLxe6ekzvWtkqF0H1orID6Yvy5qVfCkyebAPM_wbPEecAs36cWYSVoraO9HRk-aYEp6kJ0Q7JghMS7VfeLDIF9OJB9uH_iKOQhzp2yU7XuzmLEjHR2WNpIoFiNWsk-JA2nxc2OZoGlF6vaCQxhbjSs8rwkEptimXY0H3HfP9yUYlGWCSCoZMhx785NsQr6nXBI1SSgmLu9e36IanK7eO2u-zLyo_iODvCemwzrC15mO7Jr_DklpG7s0ltd9PIPbKJirdxjPR947uwD0thOPYtivZTXCT0bnOFWm2t5o7U9NClI5liROuKZnhSrUSumdBKBT2wi3mNtySgcNi2CLpErdO76dUi9UUbw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvNq_HrKIPi6Q0F3SLYvI7LedUGmiVoRmaYuelWTo4j2rOsE5CoZY5XLsW3poUYopPoRp7Cpo9rF24sD2z1Z-q5M,&b64e=1&sign=438d996c1d599d9df5323c7812a0c07c&keyno=1',
                    storeGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4UFDbqDfG43hso6kYxB8a8uVezXbCG0KaM7jDt9sa5cWdyy93byA8prDpa7WddnnWw4Bm-QWP9vWGDEYiU3Y1WmhW_Ob21ERH_0ttlF14CHnpz2599Z1Gti595b0VWmaIiymixzTWCaY_AOP6nlWeWC2uIwUIOhU3FVSdMshRUkE9S9ILaw2QdsmSb_CBaQ7UfBY_4HFN26Iz0u9VNM5M1Ny6KS2lodJpOu4G1vJE_rF9cOf2T0oZxZWorRpoEe9CEWrx69VJh41GUSZh4VslNfF2KPsi6LlH6GSoLh938lc3Am5r6dTWbpB2cBqv_DnwbxpBHOLDGiArfrp7WuCvUdWXfgk3rT95ilTO4SRnIh0Vw8CY_vSS9QYoisC8h0gLL-M1ca55w-7hv33tHqM0FqxTyufWqHYVx23oCJ6kIDBn5E75JxdayZ05P-6gulyWZ8N603vTeddJPaBKca7wA4Yne9fkNmqUnJYeQfWhw1K8R1G1BS0Q9rTXP319uXkFFikjjhaK6cdfgdg09h830ylC9gYibJTy3JlZmx8VmJ98wAgBrQuEaQmh_pU9DT6bU2lRbWFtqbBynzktscrVBWMg-m0uPDGzXYJF-pY7IavXlHBons3V1tWMx0AIjqeDf87fw78B3Fm5LKUZSiLFd0yHirwO0kkNkaQIX6HFBM8djCE4GeYMASLWJcddG0hJ6ydtOlXuko4i5VoI7RySuvG-M8rUIECxsCyVSj0bH6ilmUDFmo90a3-USZ4gDyYR3FuwF1YtQbihc5u-P1ECZOPpwa9ZfjW5U3zaMJMT9XON2KgPdN0zDycSqpxoRngFe7jZtD9yN3lOQhLC--LH5rC5tfDE_c8Bg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvC1BTlhscn557NF_osPORdG1gFNARI2Gn7Y6EgFWm3Jw3mRcg5-8cr_lJmH_5F3t8kJhpILHWlUWPFSuTjNCQXI,&b64e=1&sign=8259a45e234fd8fd61b549d3bcbfd924&keyno=1',
                    postomatGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4UFDbqDfG43hso6kYxB8a8uVezXbCG0KaM7jDt9sa5cWdyy93byA8prDpa7WddnnWw4Bm-QWP9vWGDEYiU3Y1WmhW_Ob21ERH_0ttlF14CHnpz2599Z1Gti595b0VWmaIiymixzTWCaY_AOP6nlWeWC2uIwUIOhU3FVSdMshRUkE9S9ILaw2QdsmSb_CBaQ7UYJRogsSSIdVcEwWnmUeF2_IQISbUx1tFGbCjJS1A5pAPeal1Dab6cbC7saR9OxcGjfSBXukNHgdaTCfND_I2w3p6dXroGbB9qoArfFEKifluERD7f3Ea-ap-7VVhe93QJD1bB4411CkQafmss7Lffwbmbh-T7SygYUhTmw_N0PHBcd7oe_5-d-rK_RsvKmY5Sm61kBvwngiHlxAKpsN4qEOlO-Hpd8JN1mWIzTSgWGuvmK9SeKyTjFA1digQX5IUnFx7s35wgHhF2v2_C-6xWfYmGNSnzXB-73t9pW73_MybVZ9KUhVakG79D0h5uQCtgvCAo9pnFayI0BRiOjLnXx7czIMs-fn_YS7VH2Q3y5KfQPRuO1ri2PJx1OiJd5tSy8GEJ8eVWnJmvuuWJDje4v5LsrVsGFX_ejqe9sCnTYzv-LC0CkBoa1meLCt0i_hhF31xbaHwffBWEPyeRvrPx1yIt3Bwcp9zQEfukzS3g0ByhU0YRc_MafHflfwe9z1DzYQaG1fq85HhIh73zUOCu_ze6_2UzGHfHcvl8QRJyzDKgnXowVsZjN0ujunW2Cy4Zkk4I0wn7hg2cLK7h24vP6nYpoHQKz7hLe5z--he9ypH10N0uEDu93zNRmZnujgLTH57Eeqr3l145AxtK4oyaN-u88QV_sTuw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvPP5MUHdVnEco5sBQJVtPoNFOJYQiMHmhlPhGiKtB_30AkSiTawPYAkB8QAPgWp_I6Plb-9c1maf0N6uoAuYpJQF7PnmOy3QBA,,&b64e=1&sign=d88b2a9da073c4396809822c22be4bf1&keyno=1',
                    callPhone:
                        '/redir/GAkkM7lQwz7vv7M_pnW8mQz3Kzm2Uh8ZeOVktJCWuh8e4boZMT90K8yJUyQzF9x6bN0Ik6cz6u7V00hNzUiiGMnLzD3SydMFetDQeBnXouPBBtbeGQ1bxEMNGtK38y9aSsGkXcgbLJEOhQ6YK_CEfdUN6E_M98dQ_kvAk14miJMM4m3x5xsdgjAxZ4BkHdg8XssTmWQUpMePnET2IKAmDNy9FLr5nVaYyhQ8sf6YUB-h8F9OyK0hupWqymbRAOkFJm67WQA8iSHDtB_FxVQjgituW8zyozu0JaD8tslrVpVRe9f-5bZQJcuFnkC6PZ4rM6x9edtUm9dVAG0QD2RLmkXclR32lSKmNZODCzAsJ4OdJ9JBsr9PEH4ccohwS5GTjzwCQ4--BeGbpXfGN8HljyaONy_5FFhGttIt2KUS9HTdqwBg67o2_Y5IqLNqFhNjxRCeGpWVtZFAda3m_s0A_7ciz1lmwxIUAdr_kkWbNg3Wj0zkppl3Cn4XADC9eYKzmwlyU768vwO8bLJid9VFbwl57SavIZAcYhR796wLTh69Tu972g8tMVXPOCEeXhXebcki_8qwM_DLW-q1I6JnSvedSZx9wDhjm1a2HUZWgFWGN7HHzohgjCzPyO71fmbLQkUzy5TYeGNf2AnTqlXowpAtEoiktVeqjQ8s3b5WbRhQQZ_3M2sUA_wiOBBUgw97zL050OqCqcVbddRf_cIs1OMxZ1aAlFkRB8_o6tzGIrY3U4mgRw6K22lhCcFbLH6G6G146HEMstZ6j44tCUy9_Yp-DrjbVmmJBsltPdtil8nT3x2w1n9GjaA3rXebzwq5ibWk9u4HZeu7l3E74_FcM7NelHgYmnbOY1Ejew91GfcE-hZSS2sLmQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9k7g2s419EJTtEvTe_zO-4A62nXUteVvIQYQZpO50hjD-Ep26sjY-t1b8iYKYDDwyuudFBuRL4aOlhZwb7GECwZhxv5nftlIIH6uP-il5tqdqDocCe9G0Lwv_lnkrOE0fFgLs8q6apCPWN37QZrBzcQ3O_m9BItnnjs66Ul6kMQQ,,&b64e=1&sign=41001fdcfdbe678d81c7f3738335804f&keyno=1',
                    direct:
                        'https://www.citilink.ru/catalog/computers_and_notebooks/hdd/hdd_in/1013006/?mrkt=msk_cl&utm_medium=cpc&utm_campaign=%25D0%2596%25D0%25B5%25D1%2581%25D1%2582%25D0%25BA%25D0%25B8%25D0%25B5%2520%25D0%25B4%25D0%25B8%25D1%2581%25D0%25BA%25D0%25B8&utm_source=xml_ymarket_msk&utm_content=32_SEAGATE_ST2000DM008&utm_term=1013006',
                },
                urlsByPp: {
                    480: {
                        pickupGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4UFDbqDfG43hso6kYxB8a8uVezXbCG0KaM7jDt9sa5cWdyy93byA8prDpa7WddnnWw4Bm-QWP9vWGDEYiU3Y1WmhW_Ob21ERH_0ttlF14CHnpz2599Z1Gti595b0VWmaIiymixzTWCaY_AOP6nlWeWC2uIwUIOhU3FVSdMshRUkE9S9ILaw2QdsmSb_CBaQ7UcSVLdjQPOCOiWjfiHPE3qhhxMMsjiR5KFcizB9mwtqoETQJOMBtQlAyLIM-Yucm5dSkGlPWzOYSe4_aQ35ZnoSNlWbZBoWFeKUpefcKoSZ5Wk7wtmDVQfzKQI9wT_HJjaIURO4pWyqs0wzZJKv4G_3HPcCBEKRUiwM19b6rwpB4vWJZAkOPKtXU_N2M7WbamL-6Xh64ZM1caQNLyZVu4e83ufcUeia3EO5PSEXipvraruyvjeyCQH8G-7M6viy4cJOebLhEQq1R_rHElt6rCvh-U1--YinuMin7NXaQYptO2w2wEEIo0YhDkKLfxTXdqLxe6ekzvWtkqF0H1orID6Yvy5qVfCkyebAPM_wbPEecAs36cWYSVoraO9HRk-aYEp6kJ0Q7JghMS7VfeLDIF9OJB9uH_iKOQhzp2yU7XuzmLEjHR2WNpIoFiNWsk-JA2nxc2OZoGlF6vaCQxhbjSs8rwkEptimXY0H3HfP9yUYlGWCSCoZMhx785NsQr6nXBI1SSgmLu9e36IanK7eO2u-zLyo_iODvCemwzrC15mO7Jr_DklpG7s0ltd9PIPbKJirdxjPR947uwD0thOPYtivZTXCT0bnOFWm2t5o7U9NClI5liROuKZnhSrUSumdBKBT2wi3mNtySgcNi2CLpErdO76dUi9UUbw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvNq_HrKIPi6Q0F3SLYvI7LedUGmiVoRmaYuelWTo4j2rOsE5CoZY5XLsW3poUYopPoRp7Cpo9rF24sD2z1Z-q5M,&b64e=1&sign=438d996c1d599d9df5323c7812a0c07c&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4UFDbqDfG43hso6kYxB8a8uVezXbCG0KaM7jDt9sa5cWdyy93byA8prDpa7WddnnWw4Bm-QWP9vWGDEYiU3Y1WmhW_Ob21ERH_0ttlF14CHnpz2599Z1Gti595b0VWmaIiymixzTWCaY_AOP6nlWeWC2uIwUIOhU3FVSdMshRUkE9S9ILaw2QdsmSb_CBaQ7UfBY_4HFN26Iz0u9VNM5M1Ny6KS2lodJpOu4G1vJE_rF9cOf2T0oZxZWorRpoEe9CEWrx69VJh41GUSZh4VslNfF2KPsi6LlH6GSoLh938lc3Am5r6dTWbpB2cBqv_DnwbxpBHOLDGiArfrp7WuCvUdWXfgk3rT95ilTO4SRnIh0Vw8CY_vSS9QYoisC8h0gLL-M1ca55w-7hv33tHqM0FqxTyufWqHYVx23oCJ6kIDBn5E75JxdayZ05P-6gulyWZ8N603vTeddJPaBKca7wA4Yne9fkNmqUnJYeQfWhw1K8R1G1BS0Q9rTXP319uXkFFikjjhaK6cdfgdg09h830ylC9gYibJTy3JlZmx8VmJ98wAgBrQuEaQmh_pU9DT6bU2lRbWFtqbBynzktscrVBWMg-m0uPDGzXYJF-pY7IavXlHBons3V1tWMx0AIjqeDf87fw78B3Fm5LKUZSiLFd0yHirwO0kkNkaQIX6HFBM8djCE4GeYMASLWJcddG0hJ6ydtOlXuko4i5VoI7RySuvG-M8rUIECxsCyVSj0bH6ilmUDFmo90a3-USZ4gDyYR3FuwF1YtQbihc5u-P1ECZOPpwa9ZfjW5U3zaMJMT9XON2KgPdN0zDycSqpxoRngFe7jZtD9yN3lOQhLC--LH5rC5tfDE_c8Bg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvC1BTlhscn557NF_osPORdG1gFNARI2Gn7Y6EgFWm3Jw3mRcg5-8cr_lJmH_5F3t8kJhpILHWlUWPFSuTjNCQXI,&b64e=1&sign=8259a45e234fd8fd61b549d3bcbfd924&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4UFDbqDfG43hso6kYxB8a8uVezXbCG0KaM7jDt9sa5cWdyy93byA8prDpa7WddnnWw4Bm-QWP9vWGDEYiU3Y1WmhW_Ob21ERH_0ttlF14CHnpz2599Z1Gti595b0VWmaIiymixzTWCaY_AOP6nlWeWC2uIwUIOhU3FVSdMshRUkE9S9ILaw2QdsmSb_CBaQ7UYJRogsSSIdVcEwWnmUeF2_IQISbUx1tFGbCjJS1A5pAPeal1Dab6cbC7saR9OxcGjfSBXukNHgdaTCfND_I2w3p6dXroGbB9qoArfFEKifluERD7f3Ea-ap-7VVhe93QJD1bB4411CkQafmss7Lffwbmbh-T7SygYUhTmw_N0PHBcd7oe_5-d-rK_RsvKmY5Sm61kBvwngiHlxAKpsN4qEOlO-Hpd8JN1mWIzTSgWGuvmK9SeKyTjFA1digQX5IUnFx7s35wgHhF2v2_C-6xWfYmGNSnzXB-73t9pW73_MybVZ9KUhVakG79D0h5uQCtgvCAo9pnFayI0BRiOjLnXx7czIMs-fn_YS7VH2Q3y5KfQPRuO1ri2PJx1OiJd5tSy8GEJ8eVWnJmvuuWJDje4v5LsrVsGFX_ejqe9sCnTYzv-LC0CkBoa1meLCt0i_hhF31xbaHwffBWEPyeRvrPx1yIt3Bwcp9zQEfukzS3g0ByhU0YRc_MafHflfwe9z1DzYQaG1fq85HhIh73zUOCu_ze6_2UzGHfHcvl8QRJyzDKgnXowVsZjN0ujunW2Cy4Zkk4I0wn7hg2cLK7h24vP6nYpoHQKz7hLe5z--he9ypH10N0uEDu93zNRmZnujgLTH57Eeqr3l145AxtK4oyaN-u88QV_sTuw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvPP5MUHdVnEco5sBQJVtPoNFOJYQiMHmhlPhGiKtB_30AkSiTawPYAkB8QAPgWp_I6Plb-9c1maf0N6uoAuYpJQF7PnmOy3QBA,,&b64e=1&sign=d88b2a9da073c4396809822c22be4bf1&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mQz3Kzm2Uh8ZeOVktJCWuh8e4boZMT90K8yJUyQzF9x6bN0Ik6cz6u7V00hNzUiiGMnLzD3SydMFetDQeBnXouPBBtbeGQ1bxEMNGtK38y9aSsGkXcgbLJEOhQ6YK_CEfdUN6E_M98dQ_kvAk14miJMM4m3x5xsdgjAxZ4BkHdg8XssTmWQUpMePnET2IKAmDNy9FLr5nVaYyhQ8sf6YUB-h8F9OyK0hupWqymbRAOkFJm67WQA8iSHDtB_FxVQjgituW8zyozu0JaD8tslrVpVRe9f-5bZQJcuFnkC6PZ4rM6x9edtUm9dVAG0QD2RLmkXclR32lSKmNZODCzAsJ4OdJ9JBsr9PEH4ccohwS5GTjzwCQ4--BeGbpXfGN8HljyaONy_5FFhGttIt2KUS9HTdqwBg67o2_Y5IqLNqFhNjxRCeGpWVtZFAda3m_s0A_7ciz1lmwxIUAdr_kkWbNg3Wj0zkppl3Cn4XADC9eYKzmwlyU768vwO8bLJid9VFbwl57SavIZAcYhR796wLTh69Tu972g8tMVXPOCEeXhXebcki_8qwM_DLW-q1I6JnSvedSZx9wDhjm1a2HUZWgFWGN7HHzohgjCzPyO71fmbLQkUzy5TYeGNf2AnTqlXowpAtEoiktVeqjQ8s3b5WbRhQQZ_3M2sUA_wiOBBUgw97zL050OqCqcVbddRf_cIs1OMxZ1aAlFkRB8_o6tzGIrY3U4mgRw6K22lhCcFbLH6G6G146HEMstZ6j44tCUy9_Yp-DrjbVmmJBsltPdtil8nT3x2w1n9GjaA3rXebzwq5ibWk9u4HZeu7l3E74_FcM7NelHgYmnbOY1Ejew91GfcE-hZSS2sLmQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9k7g2s419EJTtEvTe_zO-4A62nXUteVvIQYQZpO50hjD-Ep26sjY-t1b8iYKYDDwyuudFBuRL4aOlhZwb7GECwZhxv5nftlIIH6uP-il5tqdqDocCe9G0Lwv_lnkrOE0fFgLs8q6apCPWN37QZrBzcQ3O_m9BItnnjs66Ul6kMQQ,,&b64e=1&sign=41001fdcfdbe678d81c7f3738335804f&keyno=1',
                        direct:
                            'https://www.citilink.ru/catalog/computers_and_notebooks/hdd/hdd_in/1013006/?mrkt=msk_cl&utm_medium=cpc&utm_campaign=%25D0%2596%25D0%25B5%25D1%2581%25D1%2582%25D0%25BA%25D0%25B8%25D0%25B5%2520%25D0%25B4%25D0%25B8%25D1%2581%25D0%25BA%25D0%25B8&utm_source=xml_ymarket_msk&utm_content=32_SEAGATE_ST2000DM008&utm_term=1013006',
                    },
                    481: {
                        pickupGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4UFDbqDfG43hso6kYxB8a8uVezXbCG0KaM7jDt9sa5cWdyy93byA8prDpa7WddnnWw4Bm-QWP9vWGDEYiU3Y1WmhW_Ob21ERH_0ttlF14CHnpz2599Z1Gti595b0VWmaIiymixzTWCaY_AOP6nlWeWC2uIwUIOhU3FVSdMshRUkE9S9ILaw2QdsmSb_CBaQ7UcSVLdjQPOCOiWjfiHPE3qhhxMMsjiR5KFcizB9mwtqoETQJOMBtQlAyLIM-Yucm5d_QyIzt0wqKkTW-fylTKxSlRKsUmllrzot6EkRKtTDHTSyBWO2CsizU_Ln8qPuC8xYPNpJ0Nf0s4Of206UQCjzuJbAJV9RNQu-uPf3Br01CtLF71UeMEA3-kc6B4F1ohQBFnCLi-Acy_5-XMX31OAL27MlQaIWrXAxxWE4k7aa1e2QFDc_j_L_oFqNnCOvdofObd3nHhaHLVAuP2KpWR16CpA6dRdxHFMrcTOPXv_wIubtcWCAuGr8qV2lRNh7Ob3YIwnbSvT9GGBq1Swm2nB-fGkoK_VttnFGo7tnxJIINla01cNb3-f39gc5LTEvLCZQhooB952VxePzBBU4SIfvSRUM4ZNyOjOn_znGK3VhtHzZKXTOMb1BmLNf_6mCxFaORdDHg9kAL1xFTqmcWB4J4LWWi6xQ3xb9gqZsVUz2MGAQ6LedAp8t5uVSSqxLHj9LUfNHI_Qcu-hYIItYhUN62VX4VoepLtKB6zVYtWsafvOG62Ksc6SJBxDaF_ZiYZzyUFMCa6r2k8jP_vuLfVf_FY_cr0XtmrJzFLPlSbKkHOXVt9DZFVE70d4rrXKSbxl3q2MvKLaBp_UIPKVxanJgvnFxROZcSIg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvNq_HrKIPi6Q0F3SLYvI7LedUGmiVoRmaYuelWTo4j2rOsE5CoZY5XLsW3poUYopPoRp7Cpo9rF24sD2z1Z-q5M,&b64e=1&sign=82954ab00dad706f59ee78bd14ccb6ce&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4UFDbqDfG43hso6kYxB8a8uVezXbCG0KaM7jDt9sa5cWdyy93byA8prDpa7WddnnWw4Bm-QWP9vWGDEYiU3Y1WmhW_Ob21ERH_0ttlF14CHnpz2599Z1Gti595b0VWmaIiymixzTWCaY_AOP6nlWeWC2uIwUIOhU3FVSdMshRUkE9S9ILaw2QdsmSb_CBaQ7UfBY_4HFN26Iz0u9VNM5M1Ny6KS2lodJpOu4G1vJE_rF9cOf2T0oZxZWorRpoEe9CFM_PDLeLcyit7l2y7mJHmTcdlGr353FYsDBjj6xxdH8IW7gaoev5QOgIXv4ezscY5VboKvAzNsQHlTtJfzG9mpAbn6VbdICpUIWKRoTe-qsweB5qJS4YQ_NuGGxZz9iEuuP548bChcPJiGsXN9dWKv59-q-dQTbeAPTmIaJyDD4rW3VdhQ16o2yo2cYm1QpW6keHYefwsX_3QZYJCV261Ki9xryZtBCmrwSy0rGRIoaOL70t6s6kDb79K0vhlzVqDFhRCfGs-xHR028IGJkE7V0wfQSB1uq0qRhGQ5_msWfoX7vaVn1kICD4gjtR7Zg9CFNQZNDkVaMYS3R-BTBQZ6MGy6ZzvMRgvbJWtpMDEWrNQ1APr9Dpb_AbMBj55PKScBr0X5DkBwY9j2X2BVw3ux-ytPcZ-ev6FzfAfNyn_SIq8-5mlKNu2L6QF9OGbQMFlfIk598BuP6dqTB5JDLb5oI_ewQeOx7HOG7y6up493NpXIycBC6tPDD7-XrXg2zyMKVn-s0HJxQPZUwkSl-uBUgTUvHcRSCyFnuKax9i45WGqcCjoen9MqjoBGYJFeqi9kFN7l5G1ZBCpdYfaXtUMws9AmiGpilZw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvC1BTlhscn557NF_osPORdG1gFNARI2Gn7Y6EgFWm3Jw3mRcg5-8cr_lJmH_5F3t8kJhpILHWlUWPFSuTjNCQXI,&b64e=1&sign=72f5909888c23ebd7afeea0ce5891bf9&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4UFDbqDfG43hso6kYxB8a8uVezXbCG0KaM7jDt9sa5cWdyy93byA8prDpa7WddnnWw4Bm-QWP9vWGDEYiU3Y1WmhW_Ob21ERH_0ttlF14CHnpz2599Z1Gti595b0VWmaIiymixzTWCaY_AOP6nlWeWC2uIwUIOhU3FVSdMshRUkE9S9ILaw2QdsmSb_CBaQ7UYJRogsSSIdVcEwWnmUeF2_IQISbUx1tFGbCjJS1A5pAPeal1Dab6cbC7saR9OxcGn3HmDWw-7q_m5HDY43Eh8Z9OixpWPGGEbxtffjw-VUOkTiqi3FqeKPTGPzq3FG-3imb8RkTWUtITJ1ii2K9NVdY50O7Np7cUzE6Cm_oK_UH4fnoz5durxN1jV3dOAPTNKJgiXgHI0oC3bzHSSnxu_V-_R2_ujzP8UfBMoInSzKkV_8YWNx-60AUoDqhVfsLEF7DibahdOG6i1CXvTGTKB-6Jc8CQKPVaELLaZqiel5wUGFMXO2rInaIy331Nyp816er8BFCs5mk2T9JXmttAArXivdZwdzQ-rut95x1yBLH4LkgZWrnony5U81IyGjqJL6BzYn9NakQZEL7Q0FRinmV6aVk2ESA8Y3DWgxQGxX10aioCALpvzm_WxzD3qQv4I4zQaX9sZSUhSdo2dgWw2r49pQRmpLPGuuvUS4lXXpZvwcDbsfmBPAqiRHP2dFsccMeXw3FbyR-VIW1WeaEE8pExAGBhWfJX4KtKpKchwc60GgOhiw_Jyq97D8JyZmJaMLvcGDLivnA-zzuYUOFGvnl_EUi0mgdF5SIZHOJ2qoV4PDzlOQwng9MO3TAmeNa3TfY-qcE4adH_ZFyKmsRoCrxPHjmwmemNw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvPP5MUHdVnEco5sBQJVtPoNFOJYQiMHmhlPhGiKtB_30AkSiTawPYAkB8QAPgWp_I6Plb-9c1maf0N6uoAuYpJQF7PnmOy3QBA,,&b64e=1&sign=c45edb33501cc59636e28a133e855c88&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mQz3Kzm2Uh8ZeOVktJCWuh8e4boZMT90K8yJUyQzF9x6bN0Ik6cz6u7V00hNzUiiGMnLzD3SydMFetDQeBnXouPBBtbeGQ1bxEMNGtK38y9aSsGkXcgbLJEOhQ6YK_CEfdUN6E_M98dQ_kvAk14miJMM4m3x5xsdgjAxZ4BkHdg8XssTmWQUpMePnET2IKAmDNy9FLr5nVaYyhQ8sf6YUB-h8F9OyK0hupWqymbRAOkFJm67WQA8iSHDtB_FxVQjgstx8lGxzulhKYqLSGLtvlWplaSe9PphrjOHw06-o3RvzDrfeHC6ytGGoj4fe3_PuQ15PU897Z7jwM2M7shj7k6EWyhDAGQnoaCkwZ8qi5pKxlV_dsRa6xe4gwlVJeDDjlEnHK6wVS9seB-10hD0QyzjEuRD0Td1B5TQd5LNzWlvyd_uhPxn3uCOt8jD-VsL4j6SPJ590iu3vCxdM-rzxxo991Ol4AOrO_lsl56SSiH_KI3kBbmsPXg7NtKvVeNTaLak49fNFFZ1omlRfI9ee7kSRAgZ_2CQhXmazVv_m8NgTrfbBQ5D3Cu25DEx981MNDBBlKTc6X8RDxMPQOsNUtno8Ha4lvjOTgTnrnpHdZalb9O6ucdFbXSBXVTCUj4nmOMOn5YalhElI9npOU3QqucC81oNkuSu-t3xJ6kIF0UkjLzH12WxuuZEISumt1F8qbiPF8E884VuQK7M2HDmpuM6AVg8ux4uaVQ5wJCJNM3xvqu-ZWqLR9F1NYno0xOt-GtPdOKiJWOHZT_HxpPY0NQTN25JXw0gkLA2xgso6YJAydpfHG57LbUwSW_-VIB_QUnRAWuiRtGy5VO66g7LrIN7lCwfX1r8_w,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9k7g2s419EJTtEvTe_zO-4A62nXUteVvIQYQZpO50hjD-Ep26sjY-t1b8iYKYDDwyuudFBuRL4aOlhZwb7GECwZhxv5nftlIIH6uP-il5tqdqDocCe9G0Lwv_lnkrOE0fFgLs8q6apCPWN37QZrBzcQ3O_m9BItnnjs66Ul6kMQQ,,&b64e=1&sign=ff05d02de6560937905ca6f253af3288&keyno=1',
                        direct:
                            'https://www.citilink.ru/catalog/computers_and_notebooks/hdd/hdd_in/1013006/?mrkt=msk_cl&utm_medium=cpc&utm_campaign=%25D0%2596%25D0%25B5%25D1%2581%25D1%2582%25D0%25BA%25D0%25B8%25D0%25B5%2520%25D0%25B4%25D0%25B8%25D1%2581%25D0%25BA%25D0%25B8&utm_source=xml_ymarket_msk&utm_content=32_SEAGATE_ST2000DM008&utm_term=1013006',
                    },
                },
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        isLeaf: true,
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 500,
                            containerHeight: 500,
                            url: '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/orig',
                            width: 500,
                            height: 500,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/50x50',
                                width: 50,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/55x70',
                                width: 70,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/60x80',
                                width: 80,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/74x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/75x75',
                                width: 75,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/90x120',
                                width: 120,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/100x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/120x160',
                                width: 160,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/150x150',
                                width: 150,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/180x240',
                                width: 240,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/190x250',
                                width: 250,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/200x200',
                                width: 200,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/240x320',
                                width: 320,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/300x300',
                                width: 300,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/372596/market_lDFgrSlVN-U4Y5XIkIQ20A/300x400',
                                width: 400,
                                height: 400,
                            },
                        ],
                        signatures: [],
                    },
                ],
                filters: [
                    {
                        id: '5127047',
                        type: 'number',
                        name: 'Емкость (точно)',
                        xslname: 'Capacity',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        unit: 'ГБ',
                        position: 5,
                        noffers: 1,
                        precision: 2,
                        values: [
                            {
                                ranges: '1.2, 300, 600, 1800, 5000',
                                max: '2000',
                                initialMax: '2000',
                                initialMin: '2000',
                                min: '2000',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127084',
                        type: 'number',
                        name: 'Количество пластин',
                        xslname: 'NumOfDisks',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 49,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 2, 7',
                                max: '1',
                                initialMax: '1',
                                initialMin: '1',
                                min: '1',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127083',
                        type: 'number',
                        name: 'Количество головок',
                        xslname: 'NumOfHeads',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 50,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 4, 16',
                                max: '2',
                                initialMax: '2',
                                initialMin: '2',
                                min: '2',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                ],
                meta: {},
                marketSkuCreator: 'market',
                model: {
                    id: 130084187,
                },
                isCutPrice: false,
                delivery: {
                    shopPriorityRegion: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    shopPriorityCountry: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {
                                genitive: 'России',
                                preposition: 'в',
                                prepositional: 'России',
                                accusative: 'Россию',
                            },
                        },
                        type: 3,
                    },
                    isPriorityRegion: true,
                    isCountrywide: true,
                    isAvailable: true,
                    hasPickup: true,
                    hasLocalStore: true,
                    hasPost: false,
                    isForcedRegion: false,
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    price: {
                        currency: 'RUR',
                        value: '350',
                        isDeliveryIncluded: false,
                        isPickupIncluded: false,
                    },
                    isFree: false,
                    isDownloadable: false,
                    inStock: false,
                    postAvailable: true,
                    options: [
                        {
                            price: {
                                currency: 'RUR',
                                value: '350',
                                isDeliveryIncluded: false,
                                isPickupIncluded: false,
                            },
                            dayFrom: 2,
                            dayTo: 4,
                            orderBefore: '19',
                            isDefault: true,
                            serviceId: '99',
                            partnerType: 'regular',
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    pickupOptions: [
                        {
                            serviceId: 99,
                            serviceName: 'Собственная служба',
                            tariffId: 0,
                            partnerType: 'regular',
                            price: {
                                currency: 'RUR',
                                value: '0',
                            },
                            groupCount: 60,
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    deliveryPartnerTypes: ['SHOP'],
                },
                shop: {
                    entity: 'shop',
                    id: 17436,
                    name: 'Ситилинк',
                    business_id: 964654,
                    business_name: 'citilink.ru',
                    slug: 'sitilink',
                    gradesCount: 390820,
                    overallGradesCount: 390820,
                    qualityRating: 4,
                    isGlobal: false,
                    isCpaPrior: true,
                    isCpaPartner: false,
                    isNewRating: true,
                    newGradesCount: 390820,
                    newQualityRating: 4.518010849,
                    newQualityRating3M: 4.42221426,
                    ratingToShow: 4.42221426,
                    ratingType: 3,
                    newGradesCount3M: 41865,
                    status: 'actual',
                    cutoff: '',
                    outletsCount: 60,
                    storesCount: 24,
                    pickupStoresCount: 36,
                    depotStoresCount: 36,
                    postomatStoresCount: 0,
                    bookNowStoresCount: 0,
                    subsidies: false,
                    logo: {
                        entity: 'picture',
                        width: 95,
                        height: 14,
                        url:
                            '//avatars.mds.yandex.net/get-market-shop-logo/1677233/2a000001695239ade8d0eed0fb612531e060/small',
                        extension: 'PNG',
                        thumbnails: [
                            {
                                entity: 'thumbnail',
                                id: '95x14',
                                containerWidth: 95,
                                containerHeight: 14,
                                width: 95,
                                height: 14,
                                densities: [
                                    {
                                        entity: 'density',
                                        id: '1',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1677233/2a000001695239ade8d0eed0fb612531e060/small',
                                    },
                                    {
                                        entity: 'density',
                                        id: '2',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1677233/2a000001695239ade8d0eed0fb612531e060/orig',
                                    },
                                ],
                            },
                        ],
                    },
                    domainUrl: 'www.citilink.ru',
                    feed: {
                        id: '14595',
                        offerId: '1013006',
                        categoryId: '32',
                    },
                    createdAt: '2008-11-24T13:07:32',
                    mainCreatedAt: '2008-11-24T13:07:32',
                    homeRegion: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {},
                        },
                        type: 0,
                    },
                },
                returnPolicy: '7d',
                wareId: 'XnFLpaOLY1BEhLJidj4p8g',
                offerColor: 'white',
                isFreeOffer: false,
                classifierMagicId: 'bd65bd17087e789b999063319e1d44fd',
                prices: {
                    currency: 'RUR',
                    value: '4690',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                    rawValue: '4690',
                },
                manufacturer: {
                    entity: 'manufacturer',
                    warranty: true,
                    code: 'ST2000DM008',
                },
                seller: {
                    comment: 'Скидка 5% по промокоду HAPPYDAYS',
                    price: '4690',
                    currency: 'RUR',
                    sellerToUserExchangeRate: 1,
                },
                payments: {
                    deliveryCard: false,
                    deliveryCash: false,
                    prepaymentCard: false,
                    prepaymentOther: false,
                },
                isRecommendedByVendor: false,
                bundleCount: 1,
                bundled: {
                    modelId: 130084187,
                    count: 1,
                },
                outlet: {
                    entity: 'outlet',
                    id: '247905',
                    name: 'г. Москва, Лобненская',
                    purpose: ['pickup'],
                    daily: true,
                    'around-the-clock': false,
                    gpsCoord: {
                        longitude: '37.538422',
                        latitude: '55.889755',
                    },
                    isMarketBranded: false,
                    type: 'pickup',
                    serviceId: 99,
                    serviceName: 'Собственная служба',
                    isMegaPoint: false,
                    email: '',
                    shop: {
                        id: 17436,
                    },
                    address: {
                        fullAddress: 'Москва, ул.Лобненская , д. 4А',
                        country: '',
                        region: '',
                        locality: 'Москва',
                        street: 'ул.Лобненская ',
                        km: '',
                        building: '4А',
                        block: '',
                        wing: '',
                        estate: '',
                        entrance: '',
                        floor: '',
                        room: '',
                        office_number: '',
                        note: "(ТЦ''Зиг-Заг'')",
                    },
                    telephones: [
                        {
                            entity: 'telephone',
                            countryCode: '7',
                            cityCode: '919',
                            telephoneNumber: '7696792',
                            extensionNumber: '',
                        },
                    ],
                    workingTime: [
                        {
                            daysFrom: '1',
                            daysTo: '1',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '2',
                            daysTo: '2',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '3',
                            daysTo: '3',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '4',
                            daysTo: '4',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '5',
                            daysTo: '5',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '6',
                            daysTo: '6',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '7',
                            daysTo: '7',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                    ],
                    selfDeliveryRule: {
                        workInHoliday: false,
                        currency: 'RUR',
                        cost: '0',
                        shipperHumanReadableId: 'Self',
                        partnerType: 'regular',
                    },
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    deliveryServiceOutletCode: '',
                },
                prepayEnabled: false,
                promoCodeEnabled: false,
                feedGroupId: '0',
                isFulfillment: false,
                isAdult: false,
                isSMB: false,
                isGoldenMatrix: false,
            },
            {
                showUid: '16124480810149912768613005',
                entity: 'offer',
                trace: {
                    factors: {
                        CATEG_CLICKS: 3759,
                        SHOP_CTR: 0.006998632569,
                        NUMBER_OFFERS: 81,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpaBuy',
                            name: 'MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax',
                            value: '0.349353',
                        },
                        {
                            tag: 'CpcClick',
                            name: 'MNA_sovetnik_ctr',
                            value: '0.00298319',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 686779,
                    name: 'Seagate',
                    slug: 'seagate',
                    website: 'https://www.seagate.com/ru/ru',
                    logo: {
                        entity: 'picture',
                        url: '//avatars.mds.yandex.net/get-mpic/1705137/img_id474286622556761381.png/orig',
                        thumbnails: [],
                        signatures: [],
                    },
                    filter: '7893318:686779',
                },
                titles: {
                    raw: 'Жесткий диск Seagate Barracuda ST2000DM008',
                    highlighted: [
                        {
                            value: 'Жесткий диск Seagate Barracuda ST2000DM008',
                        },
                    ],
                },
                titlesWithoutVendor: {
                    raw: 'Жесткий диск Barracuda ST2000DM008',
                    highlighted: [
                        {
                            value: 'Жесткий диск Barracuda ST2000DM008',
                        },
                    ],
                },
                slug: 'zhestkii-disk-seagate-barracuda-st2000dm008',
                description:
                    'HDD для массового применения Объем: 2000Гб, 7200 оборотов / мин Формат: 3.5" SATA 6Gb / s (SATA-III) Буфер: 256 Мб',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 91033,
                        nid: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                cpc:
                    'OAOJ9_HEDs0lzdkVkTEmDHEUBy-MyFBsKFtbreUtHa2mVDNIg8EBPUtoLvxg6eWzUCbpjlAWtMQ4mgjq0utbGN1hxkuaPzNgjGbt575bkxTJdDkReKf9jQ,,',
                urls: {
                    pickupGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4dLMfiQa3OllNDIU9kTsfUwkmnG-LZh17_dRTzar6_7MV-ExM8Z8zjsOhZp59GT5Fqg-CuDkAJPi0X5KaDeqa7-d_wXUdHjKzbCk0FmEeNOSmvV9UoVdgYg-vFO2jDbCRvxGNK5ZefvI8_TPtVeFlZvWxIRbH4kATWKVnlhGdKAr-5RY6F3WkW_Vqc-ClAJ_5HWOWI2fAV_bSxsUvfk2_HzITCpCXmnjohJUlLmLQu7fJ7phOoZFz1mOduRov_2NrwKT4cHg8qeDogwQvJhnhkbfsCggifuz3t-RY-z8k7YkRrJI7nbHymE9oV-peBFqgh-zKSwNW7f7R_3-k0_HtcMQBLBRDO5cwkozX_SZlzhMeN-uh1KOL33SMmfwO38Bojbq5WSvIKL9E4VZrhghoch0jYvryWNQV47dxdzaTWe0G-ivJeWaw4YitBkjc9nZPQ3mpl-S-u1Z1Jud6FyHrwCFIxarqSJMrmTqBMZJ4UDWrDyUw-ONIcyC2vF3Ax2ZQxpa9CwS81IDZ2DaJtHYeF_4wGwD529s_rZKZYNWdLZKg0rL1RDz3TQ8hS_zO1Z-QUExS5H2LVOgulJ9m8YTtu9TSE-2lJ997r-yPVsHVt5mJq3RfhCgyc2-Mc7E3Z--nv4wLRvmkUbrOm1X48hX-ND97gtX4Et2F7FQayyUe0gOqDxkAimwJTXWLWibbbNf-Uu-bWTt1vh7b7lr9oC5taE13iDnjYZ3QtFhehDQ0q5LuC6PPQwQLuoqZaICrHnOGz4I9neVNBQu_Y0cEDZHkjKoITkD23y2aq2CZlY2n9HopOvItvHoksq1Z8oaomA5xDG6ws-MIUIcunoGuu9SKNpg6nCwBKsJvw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XM4hAUyQ1t41F7iW0v4Yux7OE0KrJdvKke6HYdZIyHNgyYkjvoODVcTuDm6QNBJFz7lhZ5N8eIOT1tJmx48bS18Vzzb7eIir1Vr02NiRGg8WFrwV6cgFL0,&b64e=1&sign=fa27dc85194d01323193055b3bb9f6ac&keyno=1',
                    storeGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4dLMfiQa3OllNDIU9kTsfUwkmnG-LZh17_dRTzar6_7MV-ExM8Z8zjsOhZp59GT5Fqg-CuDkAJPi0X5KaDeqa7-d_wXUdHjKzbCk0FmEeNOSmvV9UoVdgYg-vFO2jDbCRvxGNK5ZefvI8_TPtVeFlZvWxIRbH4kATWKVnlhGdKAr-5RY6F3WkW_Vqc-ClAJ_5FMy01U_Hx8yMisXuN_Ud7A0M91HpNeuy-oJ1w9CC9X6ViBIcG7C6b5J7cJcD6SoJZJ7C9EFb-2hhTBL3Unckt0ntXjdG_XzSun6AInErTxykWPrckzdS1EneCHT7SO9XCmWj0lEsNzP4wOvtRJAXyZ01vudEvQdRT_0OM9wFIKS-IMIPi4eW5K2ws50TllgjGSGhPBDuTNmvVIj9Y-d2lbvL_Xv0tayu3e9Kr60Xwg4HUOHgbnvgtPnmYGuPiYEKdtpniJ1mKuTQch7kNtPVEL1OK9W4PW-DR5EJb1yLqj7ZFlIFw-o6CIRhPlND_foN8lDY5TQriW4s_enM608GB6lLY7cWp2pZ-N-greNJDEB4WaRbb4svhsOEte7nKK60I4S8M9cEaQ8_SmRepdmOS4YEoBRyHBnpDQUWaNe7-TREq6GlvzD6TeTyd5wlQUmKvefLJ2qu2sa3kMtNNB-0HbZTsqh0xDhUTY0GFAgax0dMftwLVD0eyvalgK0We3GmmTMaRrNHYdVujH7h0NwUbBu4k-1lcphTjTvDHpJWXXuYQXLcROSzUNCcOZPbLbFqgME97IEl8rledDpuY8qaT_TI4VUJlujF3AbV-ZFXOy1vcuRqvaW7dWPqZPLKJBHN1HrUFErfduLeD8I81JtLaaKWqLv3v2wIw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XM4hAUyQ1t41F7iW0v4Yux7OE0KrJdvKvGARMP6bPScGDAfEvhjBG9XG7BSPUpmSAmcBr7pCsAwwNr4BVfTmLyVNllt__54_OWdmPI8Z60_53nmi5kwelQ,&b64e=1&sign=f46ad98d1cadf41377d31cbc6e8bc0c4&keyno=1',
                    postomatGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4dLMfiQa3OllNDIU9kTsfUwkmnG-LZh17_dRTzar6_7MV-ExM8Z8zjsOhZp59GT5Fqg-CuDkAJPi0X5KaDeqa7-d_wXUdHjKzbCk0FmEeNOSmvV9UoVdgYg-vFO2jDbCRvxGNK5ZefvI8_TPtVeFlZvWxIRbH4kATWKVnlhGdKAr-5RY6F3WkW_Vqc-ClAJ_5MFzm9FmkrNbJYoFUAzFFD1lRmZlZjQ51fGEHsq_r6tIK4kyS5r3Zhq2Yid-8s_bMK51ZKf2LQEU22UmAKI2YyjvewLqqrLI3oID1BigfO1TwqdqqaMCXQpVM_6OXFXDPsuhbBoqS9ywTNdX-SadeTpcG6NbDk-V9UsNLXBADDNrdaZqj47u_yirDaUzzxDTChNbqVMSaKYEwcZS0fYbJYBbAmEuQKCOmTx8Pr1DlNLzRCx6-BgQrepXXMlA7nBclnr7Nry8Rbg1QYIwAUvUnPSjsrAdspNQ_xt28scRgyKNtLhexLQ_e3y-5dqDqJ_vD0ZIk3PNuseCcOh0ojLjx4Mxuh_C5be25_zs1Tvcl8gEViNa5Si0QK3AWRHHvKQnHj6lFHf0gR9lTU_KK8vzEstRmBk87ePQNpeAZ57K_OJ5SCtC00z8zN2vXqoyhq9Ia87T8-zCEbylpssKTZuqbg2ifFLlxI55MM2WSnQXVpnw2u1ujnM8YzdZqcfja4SuEovD3iCBi3HCImJq6hHPLEz3FeXdakCn0dxqhcwb8PMtcdOFmjkxc8i6cyqB-FNFC3Haifvqhk60ujpFlhN9e6I-9G0rqZfByasitv4Vlqtt-j1J6fuHIwSAsyVz6BQehJ5EqfnkJ2dsI1TZ_s2bBCUtjuyhXXvLjg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XM4hAUyQ1t41F7iW0v4Yux7OE0KrJdvKu8MzNzQ1cCVorVVTvmn4nukU7FYzNh0MtPJJPlOE6QQtcUahi9vD0v0FGj5B-M-pylmAY2GQ-LWs0KbETWNQhdnNsDx-FwqVQ,,&b64e=1&sign=35a0dff1e2ae141bd1f994bd517d6dee&keyno=1',
                    callPhone:
                        '/redir/GAkkM7lQwz7vv7M_pnW8mcI6lk4uUlhu-9bydYk3R5PNthmEgQBwzXpzwNDy5guc6E4sJWRYIAi9jQNqoqhGyz0LWt-kl4IR9yyjq3Rp8lCXF70Kd9tQBwKGJWqsh-BZltNAJNraqqAvmJd1kxhD2OEzNOKQfDa5HfChwGXceGOhCrC4kq-kJEVjpeNh7wQ80dfw4wUqXcCjuw4WM2g8S3_P12HT3WpXyXhrL98tGcCVLBaeNh4pFroVTP9qdIF8G-lX2CJDjeOG3tA-0HiYgnCCpt_UUY2ZIYF_onsSLmhSi4Q1P5zXie1_3MFpjyw8g5A8ONW6iA6TzKDaoVO7ItuF2FZFPPzy7E-Nt7zeV7k9zrR2DFrrOL3-yu5GRU7gC9_1_tuxM6AiqiFwRflRXQoimoJjWAWBPKq3mTKz-CqdcYDEViN3T6ovZKOREbi0WGmZRlUBPMCaw88rgBUUc3TxmQtY-gkpBOd2KzbyoPHobV3hKAmhvS2X9swf-jeGctcUXm1PUutZ2yzkUnqbB4zGI4BNjUAeGZOfD4ka5dJ7UlT8ATUFVW1-W86vcOq5O4XLijY5o2u-liZ3QbOeiFGnCC88aD0zL6Qi01nD_K2_LRhIE_B_WkJJzkPxQz0WhkhMfDA8YSUD8Pr4sn2TvVUT5wtZW9J1-_N8B_puXf6P-_oMEbZHRsrg6Bz7nlWbwCHJ-Qdx_FFQRPsFQcYUv_6ztnEEO0qPlEr8ESO6YEZ4O106G9PW4aVxfL8laG9IiuLvHLMXbosMYOG73uACQkF5HucYyWsXvXZPd87ePj_tSNWURsndQx3e3ucJ09KCuaTOD-Gz3veQTZK4tApX1LFR-kGNg7DovG1iDKw5FKCou-h4qN85tw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-gRJbD3NKE0udNzlGS9c52CqkxytQxDBZlSNBNcLLHJ64ufsIsH6FZ5_Lc_FttdBXaKqukDnNOC1iIdMTLShmebqyIzJ_XN1dDzB2rQ-SekZVbOxTq_UCXyAbmyeovdB2PkfeljWL8D5VA6EVI8g98XSIN-5nsfkSaHucpJTQ14Q,,&b64e=1&sign=d9aa3c7d98f51d3e7d42a1812e084896&keyno=1',
                    direct:
                        'https://www.nix.ru/autocatalog/hdd_seagate/HDD-2-Tb-SATA-6Gb-s-Seagate-Barracuda-ST2000DM008-35-7200rpm-256Mb_365565.html?utm_source=ym&utm_medium=yml&utm_campaign=dated_202102041302',
                },
                urlsByPp: {
                    480: {
                        pickupGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4dLMfiQa3OllNDIU9kTsfUwkmnG-LZh17_dRTzar6_7MV-ExM8Z8zjsOhZp59GT5Fqg-CuDkAJPi0X5KaDeqa7-d_wXUdHjKzbCk0FmEeNOSmvV9UoVdgYg-vFO2jDbCRvxGNK5ZefvI8_TPtVeFlZvWxIRbH4kATWKVnlhGdKAr-5RY6F3WkW_Vqc-ClAJ_5HWOWI2fAV_bSxsUvfk2_HzITCpCXmnjohJUlLmLQu7fJ7phOoZFz1mOduRov_2NrwKT4cHg8qeDogwQvJhnhkbfsCggifuz3t-RY-z8k7YkRrJI7nbHymE9oV-peBFqgh-zKSwNW7f7R_3-k0_HtcMQBLBRDO5cwkozX_SZlzhMeN-uh1KOL33SMmfwO38Bojbq5WSvIKL9E4VZrhghoch0jYvryWNQV47dxdzaTWe0G-ivJeWaw4YitBkjc9nZPQ3mpl-S-u1Z1Jud6FyHrwCFIxarqSJMrmTqBMZJ4UDWrDyUw-ONIcyC2vF3Ax2ZQxpa9CwS81IDZ2DaJtHYeF_4wGwD529s_rZKZYNWdLZKg0rL1RDz3TQ8hS_zO1Z-QUExS5H2LVOgulJ9m8YTtu9TSE-2lJ997r-yPVsHVt5mJq3RfhCgyc2-Mc7E3Z--nv4wLRvmkUbrOm1X48hX-ND97gtX4Et2F7FQayyUe0gOqDxkAimwJTXWLWibbbNf-Uu-bWTt1vh7b7lr9oC5taE13iDnjYZ3QtFhehDQ0q5LuC6PPQwQLuoqZaICrHnOGz4I9neVNBQu_Y0cEDZHkjKoITkD23y2aq2CZlY2n9HopOvItvHoksq1Z8oaomA5xDG6ws-MIUIcunoGuu9SKNpg6nCwBKsJvw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XM4hAUyQ1t41F7iW0v4Yux7OE0KrJdvKke6HYdZIyHNgyYkjvoODVcTuDm6QNBJFz7lhZ5N8eIOT1tJmx48bS18Vzzb7eIir1Vr02NiRGg8WFrwV6cgFL0,&b64e=1&sign=fa27dc85194d01323193055b3bb9f6ac&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4dLMfiQa3OllNDIU9kTsfUwkmnG-LZh17_dRTzar6_7MV-ExM8Z8zjsOhZp59GT5Fqg-CuDkAJPi0X5KaDeqa7-d_wXUdHjKzbCk0FmEeNOSmvV9UoVdgYg-vFO2jDbCRvxGNK5ZefvI8_TPtVeFlZvWxIRbH4kATWKVnlhGdKAr-5RY6F3WkW_Vqc-ClAJ_5FMy01U_Hx8yMisXuN_Ud7A0M91HpNeuy-oJ1w9CC9X6ViBIcG7C6b5J7cJcD6SoJZJ7C9EFb-2hhTBL3Unckt0ntXjdG_XzSun6AInErTxykWPrckzdS1EneCHT7SO9XCmWj0lEsNzP4wOvtRJAXyZ01vudEvQdRT_0OM9wFIKS-IMIPi4eW5K2ws50TllgjGSGhPBDuTNmvVIj9Y-d2lbvL_Xv0tayu3e9Kr60Xwg4HUOHgbnvgtPnmYGuPiYEKdtpniJ1mKuTQch7kNtPVEL1OK9W4PW-DR5EJb1yLqj7ZFlIFw-o6CIRhPlND_foN8lDY5TQriW4s_enM608GB6lLY7cWp2pZ-N-greNJDEB4WaRbb4svhsOEte7nKK60I4S8M9cEaQ8_SmRepdmOS4YEoBRyHBnpDQUWaNe7-TREq6GlvzD6TeTyd5wlQUmKvefLJ2qu2sa3kMtNNB-0HbZTsqh0xDhUTY0GFAgax0dMftwLVD0eyvalgK0We3GmmTMaRrNHYdVujH7h0NwUbBu4k-1lcphTjTvDHpJWXXuYQXLcROSzUNCcOZPbLbFqgME97IEl8rledDpuY8qaT_TI4VUJlujF3AbV-ZFXOy1vcuRqvaW7dWPqZPLKJBHN1HrUFErfduLeD8I81JtLaaKWqLv3v2wIw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XM4hAUyQ1t41F7iW0v4Yux7OE0KrJdvKvGARMP6bPScGDAfEvhjBG9XG7BSPUpmSAmcBr7pCsAwwNr4BVfTmLyVNllt__54_OWdmPI8Z60_53nmi5kwelQ,&b64e=1&sign=f46ad98d1cadf41377d31cbc6e8bc0c4&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4dLMfiQa3OllNDIU9kTsfUwkmnG-LZh17_dRTzar6_7MV-ExM8Z8zjsOhZp59GT5Fqg-CuDkAJPi0X5KaDeqa7-d_wXUdHjKzbCk0FmEeNOSmvV9UoVdgYg-vFO2jDbCRvxGNK5ZefvI8_TPtVeFlZvWxIRbH4kATWKVnlhGdKAr-5RY6F3WkW_Vqc-ClAJ_5MFzm9FmkrNbJYoFUAzFFD1lRmZlZjQ51fGEHsq_r6tIK4kyS5r3Zhq2Yid-8s_bMK51ZKf2LQEU22UmAKI2YyjvewLqqrLI3oID1BigfO1TwqdqqaMCXQpVM_6OXFXDPsuhbBoqS9ywTNdX-SadeTpcG6NbDk-V9UsNLXBADDNrdaZqj47u_yirDaUzzxDTChNbqVMSaKYEwcZS0fYbJYBbAmEuQKCOmTx8Pr1DlNLzRCx6-BgQrepXXMlA7nBclnr7Nry8Rbg1QYIwAUvUnPSjsrAdspNQ_xt28scRgyKNtLhexLQ_e3y-5dqDqJ_vD0ZIk3PNuseCcOh0ojLjx4Mxuh_C5be25_zs1Tvcl8gEViNa5Si0QK3AWRHHvKQnHj6lFHf0gR9lTU_KK8vzEstRmBk87ePQNpeAZ57K_OJ5SCtC00z8zN2vXqoyhq9Ia87T8-zCEbylpssKTZuqbg2ifFLlxI55MM2WSnQXVpnw2u1ujnM8YzdZqcfja4SuEovD3iCBi3HCImJq6hHPLEz3FeXdakCn0dxqhcwb8PMtcdOFmjkxc8i6cyqB-FNFC3Haifvqhk60ujpFlhN9e6I-9G0rqZfByasitv4Vlqtt-j1J6fuHIwSAsyVz6BQehJ5EqfnkJ2dsI1TZ_s2bBCUtjuyhXXvLjg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XM4hAUyQ1t41F7iW0v4Yux7OE0KrJdvKu8MzNzQ1cCVorVVTvmn4nukU7FYzNh0MtPJJPlOE6QQtcUahi9vD0v0FGj5B-M-pylmAY2GQ-LWs0KbETWNQhdnNsDx-FwqVQ,,&b64e=1&sign=35a0dff1e2ae141bd1f994bd517d6dee&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mcI6lk4uUlhu-9bydYk3R5PNthmEgQBwzXpzwNDy5guc6E4sJWRYIAi9jQNqoqhGyz0LWt-kl4IR9yyjq3Rp8lCXF70Kd9tQBwKGJWqsh-BZltNAJNraqqAvmJd1kxhD2OEzNOKQfDa5HfChwGXceGOhCrC4kq-kJEVjpeNh7wQ80dfw4wUqXcCjuw4WM2g8S3_P12HT3WpXyXhrL98tGcCVLBaeNh4pFroVTP9qdIF8G-lX2CJDjeOG3tA-0HiYgnCCpt_UUY2ZIYF_onsSLmhSi4Q1P5zXie1_3MFpjyw8g5A8ONW6iA6TzKDaoVO7ItuF2FZFPPzy7E-Nt7zeV7k9zrR2DFrrOL3-yu5GRU7gC9_1_tuxM6AiqiFwRflRXQoimoJjWAWBPKq3mTKz-CqdcYDEViN3T6ovZKOREbi0WGmZRlUBPMCaw88rgBUUc3TxmQtY-gkpBOd2KzbyoPHobV3hKAmhvS2X9swf-jeGctcUXm1PUutZ2yzkUnqbB4zGI4BNjUAeGZOfD4ka5dJ7UlT8ATUFVW1-W86vcOq5O4XLijY5o2u-liZ3QbOeiFGnCC88aD0zL6Qi01nD_K2_LRhIE_B_WkJJzkPxQz0WhkhMfDA8YSUD8Pr4sn2TvVUT5wtZW9J1-_N8B_puXf6P-_oMEbZHRsrg6Bz7nlWbwCHJ-Qdx_FFQRPsFQcYUv_6ztnEEO0qPlEr8ESO6YEZ4O106G9PW4aVxfL8laG9IiuLvHLMXbosMYOG73uACQkF5HucYyWsXvXZPd87ePj_tSNWURsndQx3e3ucJ09KCuaTOD-Gz3veQTZK4tApX1LFR-kGNg7DovG1iDKw5FKCou-h4qN85tw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-gRJbD3NKE0udNzlGS9c52CqkxytQxDBZlSNBNcLLHJ64ufsIsH6FZ5_Lc_FttdBXaKqukDnNOC1iIdMTLShmebqyIzJ_XN1dDzB2rQ-SekZVbOxTq_UCXyAbmyeovdB2PkfeljWL8D5VA6EVI8g98XSIN-5nsfkSaHucpJTQ14Q,,&b64e=1&sign=d9aa3c7d98f51d3e7d42a1812e084896&keyno=1',
                        direct:
                            'https://www.nix.ru/autocatalog/hdd_seagate/HDD-2-Tb-SATA-6Gb-s-Seagate-Barracuda-ST2000DM008-35-7200rpm-256Mb_365565.html?utm_source=ym&utm_medium=yml&utm_campaign=dated_202102041302',
                    },
                    481: {
                        pickupGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4dLMfiQa3OllNDIU9kTsfUwkmnG-LZh17_dRTzar6_7MV-ExM8Z8zjsOhZp59GT5Fqg-CuDkAJPi0X5KaDeqa7-d_wXUdHjKzbCk0FmEeNOSmvV9UoVdgYg-vFO2jDbCRvxGNK5ZefvI8_TPtVeFlZvWxIRbH4kATWKVnlhGdKAr-5RY6F3WkW_Vqc-ClAJ_5HWOWI2fAV_bSxsUvfk2_HzITCpCXmnjohJUlLmLQu7fJ7phOoZFz1mOduRov_2Nr0GnPpzjETiPtuHs-vQCh9Npy2v_Wvj8ky7YM8PWTKfrU4Edb8BXiEx3uAvmyse8JKW1VCvRlCKKOvd8rgYEkqSXwQqBnS-BEeSoU9J593OYDwJzKnab2S-zBUvduZygTHp2mRRQbBMeDnWQjKPzGItdq4vgPHL5I-4EwcKWROr1UHWW6NIziX5Xz22MTvw8nGK3Jm1Pc_BZRGGHIN6nespuFK8VMJpi57HEge5GmE5GfpAQSPtYOJklXIDr89Z8NXe_3Mn4-4-Xgu_SMe3uju-o9Ix6kVGVdypD4FqnFc53oGQ9mzzZJaLA7G-XCJKi0kQzcbGn_vMp3puOtqLFK-WYqZh19BYqJbedH97dsGXvPvWfi4NWuZzb-rBd6Hk8tsdrUrtfHvee0fjOju3cKg4-08U22jsBjdDI_H-eEQzMZbzZtftYKFNON2lvq4SLEzcYd_lgCRoErIzocX1kbM9b0D3ShbpqhlC01NgJPD9nuOAJeFkgekL-Af7_sNlOkPiy1dLchzLs4refl_OsuWWPtUF3UMF0hjQbqhHa3nVCH4XgK2i28DQHHrMjRGqsZ0E049TREgps8Mcl7CS7nmlqzGiwEtKSEg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XM4hAUyQ1t41F7iW0v4Yux7OE0KrJdvKke6HYdZIyHNgyYkjvoODVcTuDm6QNBJFz7lhZ5N8eIOT1tJmx48bS18Vzzb7eIir1Vr02NiRGg8WFrwV6cgFL0,&b64e=1&sign=fac89a86f51af38de9147cc953346f0c&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4dLMfiQa3OllNDIU9kTsfUwkmnG-LZh17_dRTzar6_7MV-ExM8Z8zjsOhZp59GT5Fqg-CuDkAJPi0X5KaDeqa7-d_wXUdHjKzbCk0FmEeNOSmvV9UoVdgYg-vFO2jDbCRvxGNK5ZefvI8_TPtVeFlZvWxIRbH4kATWKVnlhGdKAr-5RY6F3WkW_Vqc-ClAJ_5FMy01U_Hx8yMisXuN_Ud7A0M91HpNeuy-oJ1w9CC9X6ViBIcG7C6b5J7cJcD6SoJX6C-l0YXbAfuCc3ykpQG5LmqNZBaivbEM0AIdyPBhqNKj7D3cUeEdQEP2hc9EJ_6ZwKn90QREyEaasHBtzJMVFP85MsO07sCLEpfX6B3cvGrjg7jv98vgJ1XBSiNMuAgS1VJ76-5_e6I_l-ft5gK6yRgtkzB8ylzeFeS_zCX-liMTMIvvvZuClKC5gZExG8uBsWglGMkE4j7LfXOqF6ewg2gW81K9yIy6aKzdCR6hEDEJ54kO7NmTs1AQGNR5P4yIoIVczngwwJs_EFkbo-8SHXuF8yFnWSwEKqjiif7yrldL8mU1F1apO0xo0KiAWnmaSQNeR0ATsUrr0ZNhy2Yzk-mX78UvljCqTl_tCJgVQtKwOC43aYm8yyhSS97gFhBA3u9K_FfnAgM49Oeeq0_bmGLXeFyUTUS06ZYVoTjmtzAducgQHZ-9TA7gsz8zhRJYFRwlULy2hozWTBuZZUV_tDxtsvNHslIwA8zQLqhXBNHAiq_GhMyU_IyFH7-N8OG_YADPGXOdBEIXtMfKOcogIEl1zoCshPj9s6MsQxOx-OwOp8xJ7QF0Mf1cbO_y6DnEybtWaZSOslhuk21IGac_JLnbevabesxQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XM4hAUyQ1t41F7iW0v4Yux7OE0KrJdvKvGARMP6bPScGDAfEvhjBG9XG7BSPUpmSAmcBr7pCsAwwNr4BVfTmLyVNllt__54_OWdmPI8Z60_53nmi5kwelQ,&b64e=1&sign=29c1f7ee1a11454c44f1d90b0f3e6318&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4dLMfiQa3OllNDIU9kTsfUwkmnG-LZh17_dRTzar6_7MV-ExM8Z8zjsOhZp59GT5Fqg-CuDkAJPi0X5KaDeqa7-d_wXUdHjKzbCk0FmEeNOSmvV9UoVdgYg-vFO2jDbCRvxGNK5ZefvI8_TPtVeFlZvWxIRbH4kATWKVnlhGdKAr-5RY6F3WkW_Vqc-ClAJ_5MFzm9FmkrNbJYoFUAzFFD1lRmZlZjQ51fGEHsq_r6tIK4kyS5r3Zhq2Yid-8s_bMEEV3HXdvXhly_uI8ZRGZ-nHGSC-b5nxtbInwiYfy0_uK2fQ5ogWCErp8Lua8TpiCTmmBINxaeGfp9KGjYqao3Zy2-O4zJSzNdrqtcy47-R718KhiYimHeZOsfCe6FeNutrNPk3gKb8iuPbDCbGCkR_OLaSNdHl1E-HS7oJTdtMCIN--YhgNyHOjATfIPPAEOPc2JV-d5SCFq497BDerBSt7Sc-0Uvk4JuL93Ak7_-21antFyLs9PHXCVC7cQChmWzXxhmc1_FpD0UGPv5wjTXkWNm9i-D6l5Dd64bobhMFba9GoZOMvL7Pn8I2rhB6KwXlQ1j3mh8WTsYmeZHclryDF9SXuDaSNnA1y9lLFBJMOKbzaSsKpmEJTnYL-eiG5mH3lT6jE4cLLeI05Cg8mIAqtg88UEKCXeZ2GxYtRdY-O_em7UKxtse6BEFouHUl5J0E4ClNwSCYayyzo_rnR6E-a5g3DVkqVzd-K9E5YAQ5ft9HPstfNvZg6-TedwrOW4Bkl39tDaymo6F3zCpuEhxgigRfCOdPJZ6tkNAE9jvwJypn8rM_sU0D_IHNd3TcMGFms2iOu6x7YJxsi8LpBR4hWQsGwnZCPww,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2XM4hAUyQ1t41F7iW0v4Yux7OE0KrJdvKu8MzNzQ1cCVorVVTvmn4nukU7FYzNh0MtPJJPlOE6QQtcUahi9vD0v0FGj5B-M-pylmAY2GQ-LWs0KbETWNQhdnNsDx-FwqVQ,,&b64e=1&sign=0bd3868d422a9302642be3e46ee20885&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mcI6lk4uUlhu-9bydYk3R5PNthmEgQBwzXpzwNDy5guc6E4sJWRYIAi9jQNqoqhGyz0LWt-kl4IR9yyjq3Rp8lCXF70Kd9tQBwKGJWqsh-BZltNAJNraqqAvmJd1kxhD2OEzNOKQfDa5HfChwGXceGOhCrC4kq-kJEVjpeNh7wQ80dfw4wUqXcCjuw4WM2g8S3_P12HT3WpXyXhrL98tGcCVLBaeNh4pFroVTP9qdIF8G-lX2CJDjeOG3tA-0HiYghwF8atqK0LH0O-MI1PKoCjc-SjyGBxuv8hdI-G25YdVWA4Fzqr48ZidmqUy2fIJxb-tj6edrBtfxFju2xVPZW3IkuCjBzkieWxD5G6P_nDY07b7GmOHjxsgSkt9Euc_RQh-3qnYUcNzpxHWVzPU-2mx-LATQC55QMP7KZX2B5ePr77OLLZrN8n1NY7EHAeDFu_EbpDkh5cECReA1jg2z3To3v6eZoLiwzrUN_nVp6qFZtzvTPxWeBqd6C3Sm1vQiMmhAsHgXAAwSDD6jdeiOJau6ggKQfX6Fha1Bako4-aXdHP7mgKMduj6IO0d1o-xZnW7H0AqssW65S8H05d_6xPeEH-IWUkiXiUHphsRBiEdh3tdttGqDuXqAT-rnXvznTBh-jB_qud0WXhOLg_F-mDpIs03jWjTIkZPLPV6jh9fs-R-KN9dyAewsT-owi9EpMCWd3U7L3RFLtXKpBWVKm7PNd_M5dJOUyTQ26r9R9NZHLMBrVHqZ9P_oyZPGQPDUhfOCof0gr7WjxH_l-95kR_WCbKw6TVn4J0YnPxUcQzsQYA1GFhEazp5SKRwlnExOCiRB5wvOFe9t7C08VbhW2gZepa5Dciihg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-gRJbD3NKE0udNzlGS9c52CqkxytQxDBZlSNBNcLLHJ64ufsIsH6FZ5_Lc_FttdBXaKqukDnNOC1iIdMTLShmebqyIzJ_XN1dDzB2rQ-SekZVbOxTq_UCXyAbmyeovdB2PkfeljWL8D5VA6EVI8g98XSIN-5nsfkSaHucpJTQ14Q,,&b64e=1&sign=b313e13210ffbf3d4e4269562adb4875&keyno=1',
                        direct:
                            'https://www.nix.ru/autocatalog/hdd_seagate/HDD-2-Tb-SATA-6Gb-s-Seagate-Barracuda-ST2000DM008-35-7200rpm-256Mb_365565.html?utm_source=ym&utm_medium=yml&utm_campaign=dated_202102041302',
                    },
                },
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        isLeaf: true,
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 452,
                            containerHeight: 650,
                            url: '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/orig',
                            width: 452,
                            height: 650,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/50x50',
                                width: 34,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/55x70',
                                width: 48,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/60x80',
                                width: 55,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/74x100',
                                width: 69,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/75x75',
                                width: 52,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/90x120',
                                width: 83,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/100x100',
                                width: 69,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/120x160',
                                width: 111,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/150x150',
                                width: 104,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/180x240',
                                width: 166,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/190x250',
                                width: 173,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/200x200',
                                width: 139,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/240x320',
                                width: 222,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/300x300',
                                width: 208,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/300x400',
                                width: 278,
                                height: 400,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 600,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/466758/market_2KqGxBICo3FPEsV9UERSYg/600x600',
                                width: 417,
                                height: 600,
                            },
                        ],
                        signatures: [],
                    },
                ],
                filters: [
                    {
                        id: '5127047',
                        type: 'number',
                        name: 'Емкость (точно)',
                        xslname: 'Capacity',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        unit: 'ГБ',
                        position: 5,
                        noffers: 1,
                        precision: 2,
                        values: [
                            {
                                ranges: '1.2, 300, 600, 1800, 5000',
                                max: '2000',
                                initialMax: '2000',
                                initialMin: '2000',
                                min: '2000',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127084',
                        type: 'number',
                        name: 'Количество пластин',
                        xslname: 'NumOfDisks',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 49,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 2, 7',
                                max: '1',
                                initialMax: '1',
                                initialMin: '1',
                                min: '1',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127083',
                        type: 'number',
                        name: 'Количество головок',
                        xslname: 'NumOfHeads',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 50,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 4, 16',
                                max: '2',
                                initialMax: '2',
                                initialMin: '2',
                                min: '2',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                ],
                meta: {},
                marketSkuCreator: 'market',
                model: {
                    id: 130084187,
                },
                isCutPrice: false,
                delivery: {
                    shopPriorityRegion: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    shopPriorityCountry: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {
                                genitive: 'России',
                                preposition: 'в',
                                prepositional: 'России',
                                accusative: 'Россию',
                            },
                        },
                        type: 3,
                    },
                    isPriorityRegion: true,
                    isCountrywide: true,
                    isAvailable: true,
                    hasPickup: true,
                    hasLocalStore: true,
                    hasPost: false,
                    isForcedRegion: false,
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    price: {
                        currency: 'RUR',
                        value: '290',
                        isDeliveryIncluded: false,
                        isPickupIncluded: false,
                    },
                    isFree: false,
                    isDownloadable: false,
                    inStock: true,
                    postAvailable: true,
                    options: [
                        {
                            price: {
                                currency: 'RUR',
                                value: '290',
                                isDeliveryIncluded: false,
                                isPickupIncluded: false,
                            },
                            dayFrom: 1,
                            dayTo: 1,
                            orderBefore: '20',
                            isDefault: true,
                            serviceId: '99',
                            paymentMethods: ['CASH_ON_DELIVERY'],
                            partnerType: 'regular',
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    pickupOptions: [
                        {
                            serviceId: 99,
                            serviceName: 'Собственная служба',
                            tariffId: 0,
                            partnerType: 'regular',
                            price: {
                                currency: 'RUR',
                                value: '0',
                            },
                            groupCount: 1,
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                        {
                            serviceId: 99,
                            serviceName: 'Собственная служба',
                            tariffId: 0,
                            partnerType: 'regular',
                            price: {
                                currency: 'RUR',
                                value: '0',
                            },
                            dayFrom: 0,
                            dayTo: 2,
                            orderBefore: 24,
                            groupCount: 6,
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                        {
                            serviceId: 99,
                            serviceName: 'Собственная служба',
                            tariffId: 0,
                            partnerType: 'regular',
                            price: {
                                currency: 'RUR',
                                value: '0',
                            },
                            dayFrom: 1,
                            dayTo: 4,
                            orderBefore: 24,
                            groupCount: 4,
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    deliveryPartnerTypes: ['SHOP'],
                },
                shop: {
                    entity: 'shop',
                    id: 141168,
                    name: 'НИКС',
                    business_id: 887584,
                    business_name: 'НИКС КОМПЬЮТЕРНЫЙ СУПЕРМАРКЕТ',
                    slug: 'niks',
                    gradesCount: 7723,
                    overallGradesCount: 7723,
                    qualityRating: 4,
                    isGlobal: false,
                    isCpaPrior: false,
                    isCpaPartner: true,
                    isNewRating: true,
                    newGradesCount: 7723,
                    newQualityRating: 4.123009193,
                    newQualityRating3M: 3.941320293,
                    ratingToShow: 3.941320293,
                    ratingType: 3,
                    newGradesCount3M: 409,
                    status: 'actual',
                    cutoff: '',
                    outletsCount: 11,
                    storesCount: 11,
                    pickupStoresCount: 0,
                    depotStoresCount: 0,
                    postomatStoresCount: 0,
                    bookNowStoresCount: 0,
                    subsidies: false,
                    logo: {
                        entity: 'picture',
                        width: 54,
                        height: 14,
                        url:
                            '//avatars.mds.yandex.net/get-market-shop-logo/1539910/2a00000167a7cb47db08c0d72ef9fa784c4f/small',
                        extension: 'PNG',
                        thumbnails: [
                            {
                                entity: 'thumbnail',
                                id: '54x14',
                                containerWidth: 54,
                                containerHeight: 14,
                                width: 54,
                                height: 14,
                                densities: [
                                    {
                                        entity: 'density',
                                        id: '1',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1539910/2a00000167a7cb47db08c0d72ef9fa784c4f/small',
                                    },
                                    {
                                        entity: 'density',
                                        id: '2',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1539910/2a00000167a7cb47db08c0d72ef9fa784c4f/orig',
                                    },
                                ],
                            },
                        ],
                    },
                    hasSafetyGuarantee: true,
                    domainUrl: 'www.nix.ru',
                    feed: {
                        id: '343394',
                        offerId: '365565',
                        categoryId: '119',
                    },
                    phones: {
                        raw: '+74959743333',
                        sanitized: '+74959743333',
                    },
                    createdAt: '2013-02-06T21:36:23',
                    mainCreatedAt: '2013-02-06T21:36:23',
                    homeRegion: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {},
                        },
                        type: 0,
                    },
                },
                returnPolicy: '7d',
                wareId: 'CUKUSDGA5Aph4QM5cFt0wA',
                offerColor: 'white',
                isFreeOffer: false,
                classifierMagicId: '4fba3fb980a1fcef555db6162570f39a',
                prices: {
                    currency: 'RUR',
                    value: '4752',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                    rawValue: '4752',
                },
                manufacturer: {
                    entity: 'manufacturer',
                    warranty: false,
                    code: 'ST2000DM008',
                },
                seller: {
                    comment: 'Visa*MC*Мир*безнал*кредит*нал',
                    price: '4752',
                    currency: 'RUR',
                    sellerToUserExchangeRate: 1,
                },
                payments: {
                    deliveryCard: false,
                    deliveryCash: false,
                    prepaymentCard: false,
                    prepaymentOther: false,
                },
                isRecommendedByVendor: false,
                bundleCount: 1,
                bundled: {
                    modelId: 130084187,
                    count: 1,
                },
                outlet: {
                    entity: 'outlet',
                    id: '56362013',
                    name: 'НИКС-Крылатское - магазин электроники',
                    purpose: ['pickup', 'store'],
                    daily: true,
                    'around-the-clock': false,
                    gpsCoord: {
                        longitude: '37.402749',
                        latitude: '55.755414',
                    },
                    isMarketBranded: false,
                    type: 'mixed',
                    paymentMethods: ['CASH_ON_DELIVERY'],
                    serviceId: 99,
                    serviceName: 'Собственная служба',
                    isMegaPoint: false,
                    email: '',
                    shop: {
                        id: 141168,
                    },
                    address: {
                        fullAddress: 'Москва, Рублевское шоссе, д. 52А',
                        country: '',
                        region: '',
                        locality: 'Москва',
                        street: 'Рублевское шоссе',
                        km: '',
                        building: '52А',
                        block: '',
                        wing: '',
                        estate: '',
                        entrance: '',
                        floor: '',
                        room: '',
                        office_number: '',
                        note: 'ТЦ Западный, 1-й этаж',
                    },
                    telephones: [
                        {
                            entity: 'telephone',
                            countryCode: '7',
                            cityCode: '495',
                            telephoneNumber: '9743333',
                            extensionNumber: '',
                        },
                    ],
                    workingTime: [
                        {
                            daysFrom: '1',
                            daysTo: '1',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '2',
                            daysTo: '2',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '3',
                            daysTo: '3',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '4',
                            daysTo: '4',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '5',
                            daysTo: '5',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '6',
                            daysTo: '6',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '7',
                            daysTo: '7',
                            hoursFrom: '10:00',
                            hoursTo: '20:00',
                        },
                    ],
                    selfDeliveryRule: {
                        workInHoliday: false,
                        currency: 'RUR',
                        cost: '0',
                        shipperHumanReadableId: 'Self',
                        partnerType: 'regular',
                    },
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    deliveryServiceOutletCode: '',
                },
                prepayEnabled: false,
                promoCodeEnabled: false,
                feedGroupId: '0',
                isFulfillment: false,
                isAdult: false,
                isSMB: false,
                isGoldenMatrix: false,
            },
            {
                showUid: '16124480810149912768613006',
                entity: 'offer',
                trace: {
                    factors: {
                        CATEG_CLICKS: 3759,
                        SHOP_CTR: 0.007052585483,
                        NUMBER_OFFERS: 81,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpaBuy',
                            name: 'MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax',
                            value: '0.593466',
                        },
                        {
                            tag: 'CpcClick',
                            name: 'MNA_sovetnik_ctr',
                            value: '0.00292769',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 686779,
                    name: 'Seagate',
                    slug: 'seagate',
                    website: 'https://www.seagate.com/ru/ru',
                    logo: {
                        entity: 'picture',
                        url: '//avatars.mds.yandex.net/get-mpic/1705137/img_id474286622556761381.png/orig',
                        thumbnails: [],
                        signatures: [],
                    },
                    filter: '7893318:686779',
                },
                titles: {
                    raw: 'Жёсткий диск Seagate 2Tb SATA-IIISeagate Barracuda (ST2000DM008)',
                    highlighted: [
                        {
                            value: 'Жёсткий диск Seagate 2Tb SATA-IIISeagate Barracuda (ST2000DM008)',
                        },
                    ],
                },
                titlesWithoutVendor: {
                    raw: 'Жёсткий диск 2Tb SATA-IIISeagate Barracuda (ST2000DM008)',
                    highlighted: [
                        {
                            value: 'Жёсткий диск 2Tb SATA-IIISeagate Barracuda (ST2000DM008)',
                        },
                    ],
                },
                slug: 'zhestkii-disk-seagate-2tb-sata-iiiseagate-barracuda-st2000dm008',
                description: 'внутренний HDD, 3.5", 2000 Гб, SATA-III, 7200 об/мин, кэш - 256 Мб',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 91033,
                        nid: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                cpc:
                    'dbQg02qunSi9i3YJuJBkPdcSOYTRshqn98NDkChyvxtbbRjc6bRYBlqpgj9YdeHeMs7ABlYZcLdY5T2s1vVPNiZEAGZlUJLuOl41wXuZbLv0hq8vnShGvw,,',
                urls: {
                    pickupGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4S3g2ldeJnp_Ol95rqxxRK_aIXLjkn0zUzUT9yCCm6h54AYhMJhriMfbvajeRS6yAPQGTInfswz2HQG_eyd5aiNE95EQp89mCpSyjl0JdL_pV-GlDvOGORIjLyIc_DstNcYrtTFH03Z8lBBdZ52AYgzsv6aVoJ1HQeuIJMZ3dxI5x1c_naJEwsJLALTgdzuB6P8qjEyuEjQA3tyu7tabPa-VcVSW2hxjZAy15Jz1bFoKgJTvcV2RPSEkZeN9T4C_x2S8W6L24GI0K1sX-bH26nIzoN4XRIAsNvO6kU_1u9O-MlOX01Ol_CUtmlxOz-au_0deHA6xqfOTrxh8hV63reAUdJ0XN-tU1PfroGnysgTLnzX834vQC5M14MvTLNQ0ul6HFDWE_rsSnRFC957v1HQPiVKP4bxe0YH_pgwNG4wKIZm-Z2zUuRmVYL5v4uofO5E5w_aymn7O-7D5G6_FHpM1MsHiTBY1H7673BI8RtbtvxGVKe1FKotZYtBS7uhU_p_YLlus4icARWuwzjby7Y86YG95jbBj3BC46djWJFNOL4ybF7ccEsKPueNBJKfPRVUvJgouEFHL375NBLsgLjmQLLSpvrmDFhm-UIjD-d5Ol97ljRGEtpe9DLkIp-SEzO458U2Jcgk2ScPR_Pvls_K_5sY6CAiNsOAPEmK_wfrkLdiv07Fse5918c7Evbu50Yxor8QHXaApSUZQD_XseW3O1sqet8kE3T8QjmDeHfhSaYP0qJqiPbiTONneykpEeVKwyE-QKNZb0DmqsNMxBvIPPBRafWfQozzZdabqnSmZJzAPaCVNg-xg4cikvhq9v89O6XzNq8HcrNmO-SzN-t4,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI1w9NSBQTcQhx0sgVplGnjb6Yy6obEu4igWL3mC2SkKh1z3WbxAyaZY-qwhVYiyoYD4kD3EQbWPmm8G7RlGqDj_I,&b64e=1&sign=f136da5cca98aa15b011019854546108&keyno=1',
                    storeGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4S3g2ldeJnp_Ol95rqxxRK_aIXLjkn0zUzUT9yCCm6h54AYhMJhriMfbvajeRS6yAPQGTInfswz2HQG_eyd5aiNE95EQp89mCpSyjl0JdL_pV-GlDvOGORIjLyIc_DstNcYrtTFH03Z8lBBdZ52AYgzsv6aVoJ1HQeuIJMZ3dxI5x1c_naJEwsJLALTgdzuB6LV177U9o21pMbYDXvdHRBCld1M2BhJuEvjnX3eod6fc1BAWOIX503OCZwbmuNLf6l5I82vDF3ZCCgq4hEBod_b_dMWmUiBYBNzuo4FhIpn4Z-8iZ9Sy0jMZYEk1DtBqqBU0O4vvcOmYdQ_KBHB_klssXD0Lha0pNk-z42xSlq4dQsDqNPVS5FJsCpk4134F22b8au752At2FISOawbfnWXf-nQ8ZUW5D-291WtGIKxb9xpSevCZ7YGaCiG38EO-6NRHySM8uq7MD6ESMSFtvgipHpigjwiV8XGw_0CIj479VVlgyN5nzPmSJTRDczNT_uY4m1vC0EUC_nbajO2x1xcfaEUrnExZQG4uA-srSa2t1q6I4cwoPqjMH-sEgLmmQDkEERv2cVIxCrrMDszBZY_GOkijKQqC-xEziwskCmpQQ0aSKhlk55xEnQXoHjXUeGUmLwtm_AbyXPWDwmOqzD-YiTAtwxkGpT0s0FsGpNrKy-Z1jQ0iTA_I4_2bD4oce7XZXu8CJBX0Yhy96nkAsqUtT9n3n3nnbjOX7oUKDXur8YD4TeMjokzSAsmPv5y99Re-qGFWc2uXEjwz1QDgDlsg55vN9UZ8KX1mnLFhgQNjjDdXZ4tGeKWm_tH_emAZQCkJmi4wBJvWRyA_sWrEPhg,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI10wbNm7M-3rr2op627_hNRT_tw_IraUfVm9rlpptu92lwdomvIPRjyK-G2zc1jmzH-9bFm0-vuPG_7o0ym_wn04,&b64e=1&sign=576565e895515b80f3fab8c4ac256ede&keyno=1',
                    postomatGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4S3g2ldeJnp_Ol95rqxxRK_aIXLjkn0zUzUT9yCCm6h54AYhMJhriMfbvajeRS6yAPQGTInfswz2HQG_eyd5aiNE95EQp89mCpSyjl0JdL_pV-GlDvOGORIjLyIc_DstNcYrtTFH03Z8lBBdZ52AYgzsv6aVoJ1HQeuIJMZ3dxI5x1c_naJEwsJLALTgdzuB6JYZV8UNpQhRBnpTk7hY9V1fMwnZYT8kLG47heNnwHkGTs4eVASgn_slQr6DAf2y861DjirUWP_AymIuyjkBLZC4tzPj7fhLNS-ehhxptSd-p91z3VXC8Fp0CjQaJg5Iqfb39thQwpO9ti3U7IupxyHRb1MNktil_5nOA-qyLIDxwhIn4iAQ_6rkq0uTVrmyuuoqAzAuwoPfKCo3GM_hrxJF4PXYxOOPFCZe3yjDaygGlaUkUf5auD27trIv9Cw3bxS0X1_afPzjvcFmvwAEcZoEo_Mgfl218T--rAhTsJSjdtRpytZvLsBetA9MxYuzQ5EN-AivDFVSxwlTadZrZiFJjfidrA6WdyUOdN_ea7mx6U7p8YPRyon74ZSijQpxb6Nz0B0tGmvRJOY6l_VUzwmqN9OF0yupOnVKujiihAI1SUt8Iz6xjOu74o5if5hxNqJq5_GQEX8mxBcfWO_XDeGMslb9Ljs1pqYCXJr_uiqPmuA4RskVm6CmlDi2Tqjx---reDROcOrnB5Ip9zsk30XIikUx3Xk8CXUiYuJjozCmkQPvDccjLU2AlX_LttVtCPjbUs-KST1pMRQJcmSz8zsyQW2y-I7CCgd3CsX9FO5HRjkHt1kOhYqBAIdE8n8pTSK10RI6dVO-m35TZIsz6nk,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI1x-Ca-AtyaBpbkakkkfKkXwdGJHYP_S-FWTFCPkLGwvcKcWLl_QJWNDwcWb88MSx8qsdj6eVPSBv1nCqPzLj3c4,&b64e=1&sign=505dcc47028e97acdb9d4e5e5ece2b09&keyno=1',
                    callPhone:
                        '/redir/GAkkM7lQwz7vv7M_pnW8mdsBqLoOXcnB11wf4tforbR8_A5K-lbubKHxPzBC0eF6li6orUfKapzRhrHswejdVdnA4sXWd8HP82ZLXZkWLxMHovfIhV7qLp81Y12iLF9LsvqAwB58ao9p5RP6slb7lRAmnv6yXe0qjItH-Y2qG_zmzJr3UgMlM-gHtb6Vu7ErlAjPan63x9JHCuaDBWRvzZutasPiVlwfWkSR9mLox22R9HqW5Cs5RSj5aUzXCjWNmZjyqxA_0wVspwDZEuXrxFZoIRvDdCwPoQ8HZrhzezkHhBfMzcBQKoanmsPop994nW6QeLBok3DWwNUde52s5wql3xPdOGuvf_Hddcjy0KRJSLn-y4oy8kmOJrUIMiEH-DjqRPJpclk2Use-H3cIiofsAKmjUnujwdnUxT29S2Uc2A8zsmycfpfa_J-ONWCGmruzN9kH0ueGJaciZ-GlFU9HOoHemf-M1OsWrNQRlAt0km2fArmNre17ykjMost6rS5xaOO0ExGGtG1Wd9XvvQlXfHqaiq4xz8kVUP3i6avTxwreBdxz8XJqyg_Idb2MgraAZieiRVGxZVqdOJ7duI2dExXqygFQ9cwOplvsotsX42g6wWHpf0q2-IEVzphezprilWyRWOi7DRqgKIVE8Wv9EX7HR7mgsFe2M-S9YGPMHVdfq8NImVmJzSQGl-BtUszdLw6NJIoQfZ0h0KXD8Mm-RxH87b2nGlWxcEC5AJih9qBKFO_YRC8ZdwG41dYwHL_5bZ9v3-sTIPqQb_5reZPJzfMv3kuWOr_LTNsNk1NZPeU27HF3jkwxm9724kd8_Tm76oFJXEgu3bMpK35XfyX1GoqO5n-SZRvC5bdRq0KvRFP1kWjyTw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-N1AtYwArH33D3JvxWG-Wln7h85THqhKBC0bgdMMiEK48GQ5mJTtgM5-bTfzwqxA3XpRzgVtwIJNxQaFB5eFcET9glph2ij9U9pvK2YkxG2Q27TGHtbYBkIFixXxOCwxFafJYEAf2IKkap9x0dc7rshWchR85k5b2s_jO8nAutgA,,&b64e=1&sign=ef892f484f38111eac8c602c0feec10b&keyno=1',
                    direct: 'https://www.regard.ru/catalog/tovar290048.htm',
                },
                urlsByPp: {
                    480: {
                        pickupGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4S3g2ldeJnp_Ol95rqxxRK_aIXLjkn0zUzUT9yCCm6h54AYhMJhriMfbvajeRS6yAPQGTInfswz2HQG_eyd5aiNE95EQp89mCpSyjl0JdL_pV-GlDvOGORIjLyIc_DstNcYrtTFH03Z8lBBdZ52AYgzsv6aVoJ1HQeuIJMZ3dxI5x1c_naJEwsJLALTgdzuB6P8qjEyuEjQA3tyu7tabPa-VcVSW2hxjZAy15Jz1bFoKgJTvcV2RPSEkZeN9T4C_x2S8W6L24GI0K1sX-bH26nIzoN4XRIAsNvO6kU_1u9O-MlOX01Ol_CUtmlxOz-au_0deHA6xqfOTrxh8hV63reAUdJ0XN-tU1PfroGnysgTLnzX834vQC5M14MvTLNQ0ul6HFDWE_rsSnRFC957v1HQPiVKP4bxe0YH_pgwNG4wKIZm-Z2zUuRmVYL5v4uofO5E5w_aymn7O-7D5G6_FHpM1MsHiTBY1H7673BI8RtbtvxGVKe1FKotZYtBS7uhU_p_YLlus4icARWuwzjby7Y86YG95jbBj3BC46djWJFNOL4ybF7ccEsKPueNBJKfPRVUvJgouEFHL375NBLsgLjmQLLSpvrmDFhm-UIjD-d5Ol97ljRGEtpe9DLkIp-SEzO458U2Jcgk2ScPR_Pvls_K_5sY6CAiNsOAPEmK_wfrkLdiv07Fse5918c7Evbu50Yxor8QHXaApSUZQD_XseW3O1sqet8kE3T8QjmDeHfhSaYP0qJqiPbiTONneykpEeVKwyE-QKNZb0DmqsNMxBvIPPBRafWfQozzZdabqnSmZJzAPaCVNg-xg4cikvhq9v89O6XzNq8HcrNmO-SzN-t4,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI1w9NSBQTcQhx0sgVplGnjb6Yy6obEu4igWL3mC2SkKh1z3WbxAyaZY-qwhVYiyoYD4kD3EQbWPmm8G7RlGqDj_I,&b64e=1&sign=f136da5cca98aa15b011019854546108&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4S3g2ldeJnp_Ol95rqxxRK_aIXLjkn0zUzUT9yCCm6h54AYhMJhriMfbvajeRS6yAPQGTInfswz2HQG_eyd5aiNE95EQp89mCpSyjl0JdL_pV-GlDvOGORIjLyIc_DstNcYrtTFH03Z8lBBdZ52AYgzsv6aVoJ1HQeuIJMZ3dxI5x1c_naJEwsJLALTgdzuB6LV177U9o21pMbYDXvdHRBCld1M2BhJuEvjnX3eod6fc1BAWOIX503OCZwbmuNLf6l5I82vDF3ZCCgq4hEBod_b_dMWmUiBYBNzuo4FhIpn4Z-8iZ9Sy0jMZYEk1DtBqqBU0O4vvcOmYdQ_KBHB_klssXD0Lha0pNk-z42xSlq4dQsDqNPVS5FJsCpk4134F22b8au752At2FISOawbfnWXf-nQ8ZUW5D-291WtGIKxb9xpSevCZ7YGaCiG38EO-6NRHySM8uq7MD6ESMSFtvgipHpigjwiV8XGw_0CIj479VVlgyN5nzPmSJTRDczNT_uY4m1vC0EUC_nbajO2x1xcfaEUrnExZQG4uA-srSa2t1q6I4cwoPqjMH-sEgLmmQDkEERv2cVIxCrrMDszBZY_GOkijKQqC-xEziwskCmpQQ0aSKhlk55xEnQXoHjXUeGUmLwtm_AbyXPWDwmOqzD-YiTAtwxkGpT0s0FsGpNrKy-Z1jQ0iTA_I4_2bD4oce7XZXu8CJBX0Yhy96nkAsqUtT9n3n3nnbjOX7oUKDXur8YD4TeMjokzSAsmPv5y99Re-qGFWc2uXEjwz1QDgDlsg55vN9UZ8KX1mnLFhgQNjjDdXZ4tGeKWm_tH_emAZQCkJmi4wBJvWRyA_sWrEPhg,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI10wbNm7M-3rr2op627_hNRT_tw_IraUfVm9rlpptu92lwdomvIPRjyK-G2zc1jmzH-9bFm0-vuPG_7o0ym_wn04,&b64e=1&sign=576565e895515b80f3fab8c4ac256ede&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4S3g2ldeJnp_Ol95rqxxRK_aIXLjkn0zUzUT9yCCm6h54AYhMJhriMfbvajeRS6yAPQGTInfswz2HQG_eyd5aiNE95EQp89mCpSyjl0JdL_pV-GlDvOGORIjLyIc_DstNcYrtTFH03Z8lBBdZ52AYgzsv6aVoJ1HQeuIJMZ3dxI5x1c_naJEwsJLALTgdzuB6JYZV8UNpQhRBnpTk7hY9V1fMwnZYT8kLG47heNnwHkGTs4eVASgn_slQr6DAf2y861DjirUWP_AymIuyjkBLZC4tzPj7fhLNS-ehhxptSd-p91z3VXC8Fp0CjQaJg5Iqfb39thQwpO9ti3U7IupxyHRb1MNktil_5nOA-qyLIDxwhIn4iAQ_6rkq0uTVrmyuuoqAzAuwoPfKCo3GM_hrxJF4PXYxOOPFCZe3yjDaygGlaUkUf5auD27trIv9Cw3bxS0X1_afPzjvcFmvwAEcZoEo_Mgfl218T--rAhTsJSjdtRpytZvLsBetA9MxYuzQ5EN-AivDFVSxwlTadZrZiFJjfidrA6WdyUOdN_ea7mx6U7p8YPRyon74ZSijQpxb6Nz0B0tGmvRJOY6l_VUzwmqN9OF0yupOnVKujiihAI1SUt8Iz6xjOu74o5if5hxNqJq5_GQEX8mxBcfWO_XDeGMslb9Ljs1pqYCXJr_uiqPmuA4RskVm6CmlDi2Tqjx---reDROcOrnB5Ip9zsk30XIikUx3Xk8CXUiYuJjozCmkQPvDccjLU2AlX_LttVtCPjbUs-KST1pMRQJcmSz8zsyQW2y-I7CCgd3CsX9FO5HRjkHt1kOhYqBAIdE8n8pTSK10RI6dVO-m35TZIsz6nk,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI1x-Ca-AtyaBpbkakkkfKkXwdGJHYP_S-FWTFCPkLGwvcKcWLl_QJWNDwcWb88MSx8qsdj6eVPSBv1nCqPzLj3c4,&b64e=1&sign=505dcc47028e97acdb9d4e5e5ece2b09&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mdsBqLoOXcnB11wf4tforbR8_A5K-lbubKHxPzBC0eF6li6orUfKapzRhrHswejdVdnA4sXWd8HP82ZLXZkWLxMHovfIhV7qLp81Y12iLF9LsvqAwB58ao9p5RP6slb7lRAmnv6yXe0qjItH-Y2qG_zmzJr3UgMlM-gHtb6Vu7ErlAjPan63x9JHCuaDBWRvzZutasPiVlwfWkSR9mLox22R9HqW5Cs5RSj5aUzXCjWNmZjyqxA_0wVspwDZEuXrxFZoIRvDdCwPoQ8HZrhzezkHhBfMzcBQKoanmsPop994nW6QeLBok3DWwNUde52s5wql3xPdOGuvf_Hddcjy0KRJSLn-y4oy8kmOJrUIMiEH-DjqRPJpclk2Use-H3cIiofsAKmjUnujwdnUxT29S2Uc2A8zsmycfpfa_J-ONWCGmruzN9kH0ueGJaciZ-GlFU9HOoHemf-M1OsWrNQRlAt0km2fArmNre17ykjMost6rS5xaOO0ExGGtG1Wd9XvvQlXfHqaiq4xz8kVUP3i6avTxwreBdxz8XJqyg_Idb2MgraAZieiRVGxZVqdOJ7duI2dExXqygFQ9cwOplvsotsX42g6wWHpf0q2-IEVzphezprilWyRWOi7DRqgKIVE8Wv9EX7HR7mgsFe2M-S9YGPMHVdfq8NImVmJzSQGl-BtUszdLw6NJIoQfZ0h0KXD8Mm-RxH87b2nGlWxcEC5AJih9qBKFO_YRC8ZdwG41dYwHL_5bZ9v3-sTIPqQb_5reZPJzfMv3kuWOr_LTNsNk1NZPeU27HF3jkwxm9724kd8_Tm76oFJXEgu3bMpK35XfyX1GoqO5n-SZRvC5bdRq0KvRFP1kWjyTw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-N1AtYwArH33D3JvxWG-Wln7h85THqhKBC0bgdMMiEK48GQ5mJTtgM5-bTfzwqxA3XpRzgVtwIJNxQaFB5eFcET9glph2ij9U9pvK2YkxG2Q27TGHtbYBkIFixXxOCwxFafJYEAf2IKkap9x0dc7rshWchR85k5b2s_jO8nAutgA,,&b64e=1&sign=ef892f484f38111eac8c602c0feec10b&keyno=1',
                        direct: 'https://www.regard.ru/catalog/tovar290048.htm',
                    },
                    481: {
                        pickupGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4S3g2ldeJnp_Ol95rqxxRK_aIXLjkn0zUzUT9yCCm6h54AYhMJhriMfbvajeRS6yAPQGTInfswz2HQG_eyd5aiNE95EQp89mCpSyjl0JdL_pV-GlDvOGORIjLyIc_DstNcYrtTFH03Z8lBBdZ52AYgzsv6aVoJ1HQeuIJMZ3dxI5x1c_naJEwsJLALTgdzuB6P8qjEyuEjQA3tyu7tabPa-VcVSW2hxjZAy15Jz1bFoKgJTvcV2RPSEkZeN9T4C_x0ePWgR5Qz1lUZcJwBudYYVk3Xer0cV4YiHzd-mDGy62b6epcZWkEij1auRX5jTLiMNumGXLRYtGlWMWlklVUPAXKb5VZB1bkTCuO8RZV563AP7XEZ6_mFlaCLNAQSjSuXLDhP8PJn6xcA_e1lbwoSkK2f-pTlDsoQ_UEBBh8MWSl0bKCM7BOcfE6IsNHUTk1QXa5N8ib-tTGm_SDGFYBTnlpcYQnj3sQ4-q7tQ9f5DCPtqjkfBbROG4T6bWLNDZ6HhEMgPnjAXtDKEE9p4-NYzsOcUz6NSgmqjUe_iI_OUfcry0-g4j5GgTArVZWKP81wcfwqi__IymsouXOX6hC0aJsi4ZfFkAJv4Motd3Zpw2Hu42BOaT0NpWgoKlCMOWnLN17D2wVXbRHEmhJMyUW-MAcaeq_R8PcOtkhjjwrz7kyDZBShuQiwA-zjCjBKlIwvf6CnvrRcTyEqvpbFSPFiThQRQCamtinBTukSsbw2j2lcVQc88Yjw24AXBipemFbcMcE9UhCTMofXsA0PiZmu6KBmlyYDPczrWqvQOilhbifVCPnCNIU50TG-pfNh_V3qulE4QZ_PWfW2_EqMjtU6k,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI1w9NSBQTcQhx0sgVplGnjb6Yy6obEu4igWL3mC2SkKh1z3WbxAyaZY-qwhVYiyoYD4kD3EQbWPmm8G7RlGqDj_I,&b64e=1&sign=47cdb8e43987f9920d63244ec5ea1a7d&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4S3g2ldeJnp_Ol95rqxxRK_aIXLjkn0zUzUT9yCCm6h54AYhMJhriMfbvajeRS6yAPQGTInfswz2HQG_eyd5aiNE95EQp89mCpSyjl0JdL_pV-GlDvOGORIjLyIc_DstNcYrtTFH03Z8lBBdZ52AYgzsv6aVoJ1HQeuIJMZ3dxI5x1c_naJEwsJLALTgdzuB6LV177U9o21pMbYDXvdHRBCld1M2BhJuEvjnX3eod6fc1BAWOIX503OCZwbmuNLf6tM274lXYxZn54x4YrMPKMIFeH_3u26BGKVQg_DUTlU2fXCqoKRz7lULwT9-IMAFHmd7XhI6s1F78zScQbUxuZAp776eALL6Mxvzbt_4AIbiaVicD46QYeFg3KQdhkJl-JTwyr9bxndvto8BdiY27PbI0WngNFgaHG5vjSljgQaa0_yC-JpCJgPfo39FBpAN1wPZeZ9SbYkMWCCQhfvb94PemkL2wTBV9cXWQFDIWH4_q8j-aSF1OyWd7RZtYXkM66sVb_mJf-qRB27nsRH86LZQqP2BWz4s-bT2bebWt9W3Sff2ECTIvuiTjbBMbrbtUdtkSwWEqQH2DqxjSHS9u9lmsUkzoi3-7suBzR3hoPWsv3jwMFK-4L3-Ie7Xkp0wyYwVtxNgPGLmekwo3zw0XSLQwBvSxFMz0kIPad9oA80NkeHK1hDwc2oalpPXLHdJCawn7Sj986aIN5_eD_GCst5WwSj7RIKZwolViBVpC-RxiWynw8BJIxx0sOx1txU9kmu_mxCd-f3tg9x4GM0BNbLIhy6d9xOZ1sAg7PKjM5kGDqG3J0zoTO82wBDF7ljJnMDXTDc7lIz9GkFoOmVmSAk,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI10wbNm7M-3rr2op627_hNRT_tw_IraUfVm9rlpptu92lwdomvIPRjyK-G2zc1jmzH-9bFm0-vuPG_7o0ym_wn04,&b64e=1&sign=65c61d35662383f4a8e341eb8b7e6cb4&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4S3g2ldeJnp_Ol95rqxxRK_aIXLjkn0zUzUT9yCCm6h54AYhMJhriMfbvajeRS6yAPQGTInfswz2HQG_eyd5aiNE95EQp89mCpSyjl0JdL_pV-GlDvOGORIjLyIc_DstNcYrtTFH03Z8lBBdZ52AYgzsv6aVoJ1HQeuIJMZ3dxI5x1c_naJEwsJLALTgdzuB6JYZV8UNpQhRBnpTk7hY9V1fMwnZYT8kLG47heNnwHkGTs4eVASgn_slQr6DAf2y8yZs-MOR4MCdzcgJpbQtMJ7LQUfMRWrt2tLVccm1WNAgCxKq_3gX5G8wFmTZJ1haiUIpAEcs7GlCFB8DLdUHXREyrLZut05VCAjmAsDD7gU79DHYmYbv9-Z0bjP3holWd6whIWkQeVO94FmGx_jO_ZplqPb5uuuo4Bstp8UEWG7lLotR-24um4D_OX-La_C6y5hvoFvJXVSHgQEC-00SvqiZVnv-EO8VfSlhbK_9iCrwksU22jF7SCqW8vN2GDoPHNzWmv5JYUAd0_E5FWxXn9_HWkSjQ9bc98bLFp-5JNbZEqoSKYP46g63U0zCp5WoC__rH1lJF5_h-c32jBU-f75EohxCmSsX1Bv-D91jnBak-LimMxdJWk_KUuS-1u8Zmx6_B7hMbVhvDzN45_eEEyGHEUNltACoW7mbDWfFaqLLJmAs21m9aWenB2Z6O3nvjuFDvWcxBFnrEU9sGYjMhxnJGF_0w_XqNSN_GAzzuD1T4fmTVzqWBkeNmGceO1jHP7u5cW5CBqD35DSQhHeQRHtuxUgrvLaXHm33yl3ZS-P_uG4uS6cQSEDb3a-ExICWg7ygZ-CcPoJCN1-7ODwTaO4,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI1x-Ca-AtyaBpbkakkkfKkXwdGJHYP_S-FWTFCPkLGwvcKcWLl_QJWNDwcWb88MSx8qsdj6eVPSBv1nCqPzLj3c4,&b64e=1&sign=8caedf4019ad45d93ccb5dac3f831c5a&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mdsBqLoOXcnB11wf4tforbR8_A5K-lbubKHxPzBC0eF6li6orUfKapzRhrHswejdVdnA4sXWd8HP82ZLXZkWLxMHovfIhV7qLp81Y12iLF9LsvqAwB58ao9p5RP6slb7lRAmnv6yXe0qjItH-Y2qG_zmzJr3UgMlM-gHtb6Vu7ErlAjPan63x9JHCuaDBWRvzZutasPiVlwfWkSR9mLox22R9HqW5Cs5RSj5aUzXCjWNmZjyqxA_0wVspwDZEuXrxC3ORLcY7GOwv5q5FUtwK8GyDaHEh9bvhkYK7S4THlvP5LbAWc-vIi8nt3JnF9J-qouCg65_0XhWUT4YtDbfJsmoOYdUI9SjCrOoeO6eumQh4qKdSVrPUvV3vEfJ5d4VTfkPiPFwbqGK4SCZ_55DU8e4dAnJRPjzsaMe4D-DXupVEqglzU3l8h6Wxdq0oAepliWBkhoGPRx_xMElZyH3Bu95qUAPslc6SxHXXrnInAmEtto4ErdFpK_Fof-92w_oE_dnheM5R8RaItUvBklniCI2XUgtAZR4sSrbRDirzIsXePDld_x3YJ21_z9H2T_WFzbCJpmdZux1hGg_aSZknKxuzpyztZ1tc-b8XlE1lEnb5_PJFRpmbMSEiCFPpTNYLSvHfSAMkGYcH_4LXTPd1JeZ9B9E0U4Tv1ocnnA4wJyMkrrhIrdDTiKi4aP-yZex02NHiC1ekq-77UHhCLEkRB8rmiaXsRYuz-QLxXdncmnrR6GH1BHTnYjCXD1uUOqJgPJaKGfo069LxKTH2sEPHGw2ucx9LLN09iTmrPkO85ppgYAxAJZE2cIW6yEH6hzHsXJVZGiPPFU9p1gK5nLjZuKnYofB-2Ed6g,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O-N1AtYwArH33D3JvxWG-Wln7h85THqhKBC0bgdMMiEK48GQ5mJTtgM5-bTfzwqxA3XpRzgVtwIJNxQaFB5eFcET9glph2ij9U9pvK2YkxG2Q27TGHtbYBkIFixXxOCwxFafJYEAf2IKkap9x0dc7rshWchR85k5b2s_jO8nAutgA,,&b64e=1&sign=bea030b04259a22915b0abb2e418aff6&keyno=1',
                        direct: 'https://www.regard.ru/catalog/tovar290048.htm',
                    },
                },
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        isLeaf: true,
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 681,
                            containerHeight: 995,
                            url: '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/orig',
                            width: 681,
                            height: 995,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/50x50',
                                width: 34,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/55x70',
                                width: 47,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/60x80',
                                width: 54,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/74x100',
                                width: 68,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/75x75',
                                width: 51,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/90x120',
                                width: 82,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/100x100',
                                width: 68,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/120x160',
                                width: 109,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/150x150',
                                width: 102,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/180x240',
                                width: 164,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/190x250',
                                width: 171,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/200x200',
                                width: 136,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/240x320',
                                width: 219,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/300x300',
                                width: 205,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/300x400',
                                width: 273,
                                height: 400,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 600,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/600x600',
                                width: 410,
                                height: 600,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 800,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/600x800',
                                width: 547,
                                height: 800,
                            },
                        ],
                        signatures: [],
                    },
                ],
                filters: [
                    {
                        id: '5127047',
                        type: 'number',
                        name: 'Емкость (точно)',
                        xslname: 'Capacity',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        unit: 'ГБ',
                        position: 5,
                        noffers: 1,
                        precision: 2,
                        values: [
                            {
                                ranges: '1.2, 300, 600, 1800, 5000',
                                max: '2000',
                                initialMax: '2000',
                                initialMin: '2000',
                                min: '2000',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127084',
                        type: 'number',
                        name: 'Количество пластин',
                        xslname: 'NumOfDisks',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 49,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 2, 7',
                                max: '1',
                                initialMax: '1',
                                initialMin: '1',
                                min: '1',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127083',
                        type: 'number',
                        name: 'Количество головок',
                        xslname: 'NumOfHeads',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 50,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 4, 16',
                                max: '2',
                                initialMax: '2',
                                initialMin: '2',
                                min: '2',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                ],
                meta: {},
                marketSkuCreator: 'market',
                model: {
                    id: 130084187,
                },
                isCutPrice: false,
                delivery: {
                    shopPriorityRegion: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    shopPriorityCountry: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {
                                genitive: 'России',
                                preposition: 'в',
                                prepositional: 'России',
                                accusative: 'Россию',
                            },
                        },
                        type: 3,
                    },
                    isPriorityRegion: true,
                    isCountrywide: true,
                    isAvailable: true,
                    hasPickup: true,
                    hasLocalStore: false,
                    hasPost: false,
                    isForcedRegion: false,
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    price: {
                        currency: 'RUR',
                        value: '290',
                        isDeliveryIncluded: false,
                        isPickupIncluded: false,
                    },
                    isFree: false,
                    isDownloadable: false,
                    inStock: true,
                    postAvailable: true,
                    options: [
                        {
                            price: {
                                currency: 'RUR',
                                value: '290',
                                isDeliveryIncluded: false,
                                isPickupIncluded: false,
                            },
                            dayFrom: 1,
                            dayTo: 1,
                            isDefault: true,
                            serviceId: '99',
                            partnerType: 'regular',
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    pickupOptions: [
                        {
                            serviceId: 99,
                            serviceName: 'Собственная служба',
                            tariffId: 0,
                            partnerType: 'regular',
                            price: {
                                currency: 'RUR',
                                value: '0',
                            },
                            dayFrom: 1,
                            dayTo: 1,
                            orderBefore: 24,
                            groupCount: 1,
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    deliveryPartnerTypes: ['SHOP'],
                },
                shop: {
                    entity: 'shop',
                    id: 4398,
                    name: 'Регард',
                    business_id: 920934,
                    business_name: 'ООО «Регард МСК»',
                    slug: 'regard',
                    gradesCount: 40794,
                    overallGradesCount: 40794,
                    qualityRating: 5,
                    isGlobal: false,
                    isCpaPrior: false,
                    isCpaPartner: false,
                    isNewRating: true,
                    newGradesCount: 40794,
                    newQualityRating: 4.492547924,
                    newQualityRating3M: 4.604221636,
                    ratingToShow: 4.604221636,
                    ratingType: 3,
                    newGradesCount3M: 1895,
                    status: 'actual',
                    cutoff: '',
                    outletsCount: 1,
                    storesCount: 0,
                    pickupStoresCount: 1,
                    depotStoresCount: 1,
                    postomatStoresCount: 0,
                    bookNowStoresCount: 0,
                    subsidies: false,
                    logo: {
                        entity: 'picture',
                        width: 100,
                        height: 14,
                        url:
                            '//avatars.mds.yandex.net/get-market-shop-logo/1531890/2a00000167a757e2e99b606d4d000da9ad33/small',
                        extension: 'PNG',
                        thumbnails: [
                            {
                                entity: 'thumbnail',
                                id: '100x14',
                                containerWidth: 100,
                                containerHeight: 14,
                                width: 100,
                                height: 14,
                                densities: [
                                    {
                                        entity: 'density',
                                        id: '1',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1531890/2a00000167a757e2e99b606d4d000da9ad33/small',
                                    },
                                    {
                                        entity: 'density',
                                        id: '2',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1531890/2a00000167a757e2e99b606d4d000da9ad33/orig',
                                    },
                                ],
                            },
                        ],
                    },
                    hasSafetyGuarantee: true,
                    domainUrl: 'regard.ru',
                    feed: {
                        id: '4805',
                        offerId: '290048',
                        categoryId: '5010',
                    },
                    createdAt: '2007-03-02T16:22:16',
                    mainCreatedAt: '2007-03-02T16:22:16',
                    homeRegion: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {},
                        },
                        type: 0,
                    },
                },
                returnPolicy: '7d',
                wareId: 'jTLT85zbZJagXtJz_fMh1Q',
                offerColor: 'white',
                isFreeOffer: false,
                classifierMagicId: '55c3c4b7ee8b2df6f350b50686e9e964',
                prices: {
                    currency: 'RUR',
                    value: '4610',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                    rawValue: '4610',
                },
                manufacturer: {
                    entity: 'manufacturer',
                    warranty: true,
                    code: 'ST2000DM008',
                },
                seller: {
                    comment: 'Нал, безнал, карты и онлайн оплата без комиссии',
                    price: '4610',
                    currency: 'RUR',
                    sellerToUserExchangeRate: 1,
                },
                payments: {
                    deliveryCard: false,
                    deliveryCash: false,
                    prepaymentCard: false,
                    prepaymentOther: false,
                },
                isRecommendedByVendor: false,
                bundleCount: 1,
                bundled: {
                    modelId: 130084187,
                    count: 1,
                },
                outlet: {
                    entity: 'outlet',
                    id: '161345',
                    name: 'Регард (Волгоградский пр-т)',
                    purpose: ['pickup'],
                    daily: false,
                    'around-the-clock': false,
                    gpsCoord: {
                        longitude: '37.68936239',
                        latitude: '55.72641414',
                    },
                    isMarketBranded: false,
                    type: 'pickup',
                    serviceId: 99,
                    serviceName: 'Собственная служба',
                    isMegaPoint: false,
                    email: 'sales@regard.ru',
                    shop: {
                        id: 4398,
                    },
                    address: {
                        fullAddress: 'Москва, Волгоградский проспект, д. 21',
                        country: '',
                        region: '',
                        locality: 'Москва',
                        street: 'Волгоградский проспект',
                        km: '',
                        building: '21',
                        block: '',
                        wing: '',
                        estate: '',
                        entrance: '',
                        floor: '',
                        room: '',
                        office_number: '',
                        note: 'подъезд 9',
                    },
                    telephones: [
                        {
                            entity: 'telephone',
                            countryCode: '7',
                            cityCode: '495',
                            telephoneNumber: '9214158',
                            extensionNumber: '',
                        },
                    ],
                    workingTime: [
                        {
                            daysFrom: '1',
                            daysTo: '1',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '2',
                            daysTo: '2',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '3',
                            daysTo: '3',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '4',
                            daysTo: '4',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '5',
                            daysTo: '5',
                            hoursFrom: '10:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '6',
                            daysTo: '6',
                            hoursFrom: '10:00',
                            hoursTo: '18:00',
                        },
                    ],
                    selfDeliveryRule: {
                        workInHoliday: false,
                        currency: 'RUR',
                        cost: '0',
                        shipperHumanReadableId: 'Self',
                        partnerType: 'regular',
                    },
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    deliveryServiceOutletCode: '',
                },
                prepayEnabled: false,
                promoCodeEnabled: false,
                feedGroupId: '0',
                isFulfillment: false,
                isAdult: false,
                isSMB: false,
                isGoldenMatrix: false,
            },
            {
                showUid: '16124480810149912768605007',
                entity: 'offer',
                trace: {
                    factors: {
                        CATEG_CLICKS: 3759,
                        SHOP_CTR: 0.003427620512,
                        NUMBER_OFFERS: 81,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'CpaBuy',
                            name: 'MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax',
                            value: '0.344767',
                        },
                        {
                            tag: 'CpcClick',
                            name: 'MNA_sovetnik_ctr',
                            value: '0.00206812',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 686779,
                    name: 'Seagate',
                    slug: 'seagate',
                    website: 'https://www.seagate.com/ru/ru',
                    logo: {
                        entity: 'picture',
                        url: '//avatars.mds.yandex.net/get-mpic/1705137/img_id474286622556761381.png/orig',
                        thumbnails: [],
                        signatures: [],
                    },
                    filter: '7893318:686779',
                },
                titles: {
                    raw: 'Внутренний HDD накопитель Seagate 2TB 7200RPM 6GB/S 256MB ST2000DM008',
                    highlighted: [
                        {
                            value: 'Внутренний HDD накопитель Seagate 2TB 7200RPM 6GB/S 256MB ST2000DM008',
                        },
                    ],
                },
                slug: 'vnutrennii-hdd-nakopitel-seagate-2tb-7200rpm-6gb-s-256mb-st2000dm008',
                description:
                    'Основные характеристики - Тип накопителя: HDD - Тип использования: внешний - Форм-фактор: 3.5" - Объем памяти: 2000 Гб - Интерфейс: SATA III Назначение - Для настольных компьютеров: да Электропитание - Потребляемая мощность при чтении/записи: 4.3 Вт - Потребляемая мощность при ожидании: 0.3 Вт - Стартовый ток: 2 A Параметры быстродействия - Скорость вращения шпинделя: 7200 об/м - Объем буфера: 256 Мб - Среднее время поиска: 13 мс - Среднее время задержки: 6 мс - Максимальная скорость чтения: 220 Мб/сек',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 91033,
                        nid: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                cpc:
                    'ZAoQYSvLCEcBiYPRFXLgmTVjPg5_YIVu2lDwuXsxtisRSbzPWSl6hiWwzgmYTdyh8D3x5LnGeUlH8WamMv8cPpyzBAQUsxHRRBwQPIUOCN4xbKJ6-KDhLQ,,',
                urls: {
                    callPhone:
                        '/redir/GAkkM7lQwz7vv7M_pnW8mQMLOX50vDT_pYBUtQ2vsfnAyewnaXjC-xkkGvqHHXDowADE0h8sq8bPN3rRaVHM4kd7lMGlF_RzAsFpPhWRGfe89aRn55rN-wbcq5-KdgREKGNyQX1iUiq00JxsbKz6G_kDupBUTwDmTq-MZcM2U0PKLbhzoKsq_v5TmKACXfIvFC4THFDmtT9l9WNJlrb8aAm_9KSj3EqBe9SbCvCUuGjX83Oz8V2ksC2WZCEWY8DmCFDFlt1L6Idk7ZmsGm_ll6Ri-13ikLMI2i7fKWlbrPN9DpCHDt3D-FyiBtVvJ2JSo3TWUM_4bze1GWgXaVrcJre1GzpOilWv8fHQGL6dNsLwlJzGQptozT-bfQHaqFBZ5vXpBKgfa9a3T--1_lsZSQkDmiC0xAbzrxuR_xNPXPBg0Rbj47HKtGvoJGljlzj6iZKC-KK-SVLrVdxATqM5vu4p1Xuv1Q5z1jxg-knrET5-iopb9NPvuQAdKvCDnX-4nt3m9-f70WppSdxjAOF7F3bEQjsMpUCrniivub0KCaxNVmvnkXsueqojfuexN5ohyn50cpArTdKWR0WzmWxaprXMcZfTOChwglIRHWx13PiFueGLYTmf9lynSOR1jcLMMG90Pv934Gp0itVkJtvzE7ENcs44A7_FyII5xg_sEyu8ZaZ9nubOhOEOwS5Bd5JrHpHWZBHIxt0UCY5jSZsngcBx4MQPQ3w5p-Lj_OmXEIsySZk3c4T_9VrdmjU66LkbWK0t7EfEmxQZOcO1oN_-IItLB57EAPXa7136j1n7kFi7ohuMcgkT0Wv-cLbS1dEgyZW4zxY2bVP1b3w8oBXwVuVam3Df0uU3DpLc2Epl1hKHocyPNA8AnA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_Ue_kOF2NgU9PEXUOWShwxOJZRsXnGON1FzdIiLDdDHeHrMwhKlbR_tXZQ_KWC3h5oexrGiMoslquZOI3qDtmRGc6aDuEXVayt5PEOuaauOHzfB7XHk940U8unUP7uie92_hSDXjJaMDWmfSFwzQIi18DoWGhYnSU-ugtxDkTvgg,,&b64e=1&sign=3b753a19fed7305227be7212d831a1fa&keyno=1',
                    direct:
                        'https://technopark.ru/zhestkie-diski-hdd-i-ssd-seagate-2tb-7200rpm-6gbs-256mb-st2000dm008/?utm_campaign=4&utm_content=4954&utm_medium=cpc&utm_source=yandex_market&utm_term=104544',
                },
                urlsByPp: {
                    480: {
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mQMLOX50vDT_pYBUtQ2vsfnAyewnaXjC-xkkGvqHHXDowADE0h8sq8bPN3rRaVHM4kd7lMGlF_RzAsFpPhWRGfe89aRn55rN-wbcq5-KdgREKGNyQX1iUiq00JxsbKz6G_kDupBUTwDmTq-MZcM2U0PKLbhzoKsq_v5TmKACXfIvFC4THFDmtT9l9WNJlrb8aAm_9KSj3EqBe9SbCvCUuGjX83Oz8V2ksC2WZCEWY8DmCFDFlt1L6Idk7ZmsGm_ll6Ri-13ikLMI2i7fKWlbrPN9DpCHDt3D-FyiBtVvJ2JSo3TWUM_4bze1GWgXaVrcJre1GzpOilWv8fHQGL6dNsLwlJzGQptozT-bfQHaqFBZ5vXpBKgfa9a3T--1_lsZSQkDmiC0xAbzrxuR_xNPXPBg0Rbj47HKtGvoJGljlzj6iZKC-KK-SVLrVdxATqM5vu4p1Xuv1Q5z1jxg-knrET5-iopb9NPvuQAdKvCDnX-4nt3m9-f70WppSdxjAOF7F3bEQjsMpUCrniivub0KCaxNVmvnkXsueqojfuexN5ohyn50cpArTdKWR0WzmWxaprXMcZfTOChwglIRHWx13PiFueGLYTmf9lynSOR1jcLMMG90Pv934Gp0itVkJtvzE7ENcs44A7_FyII5xg_sEyu8ZaZ9nubOhOEOwS5Bd5JrHpHWZBHIxt0UCY5jSZsngcBx4MQPQ3w5p-Lj_OmXEIsySZk3c4T_9VrdmjU66LkbWK0t7EfEmxQZOcO1oN_-IItLB57EAPXa7136j1n7kFi7ohuMcgkT0Wv-cLbS1dEgyZW4zxY2bVP1b3w8oBXwVuVam3Df0uU3DpLc2Epl1hKHocyPNA8AnA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_Ue_kOF2NgU9PEXUOWShwxOJZRsXnGON1FzdIiLDdDHeHrMwhKlbR_tXZQ_KWC3h5oexrGiMoslquZOI3qDtmRGc6aDuEXVayt5PEOuaauOHzfB7XHk940U8unUP7uie92_hSDXjJaMDWmfSFwzQIi18DoWGhYnSU-ugtxDkTvgg,,&b64e=1&sign=3b753a19fed7305227be7212d831a1fa&keyno=1',
                        direct:
                            'https://technopark.ru/zhestkie-diski-hdd-i-ssd-seagate-2tb-7200rpm-6gbs-256mb-st2000dm008/?utm_campaign=4&utm_content=4954&utm_medium=cpc&utm_source=yandex_market&utm_term=104544',
                    },
                    481: {
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mQMLOX50vDT_pYBUtQ2vsfnAyewnaXjC-xkkGvqHHXDowADE0h8sq8bPN3rRaVHM4kd7lMGlF_RzAsFpPhWRGfe89aRn55rN-wbcq5-KdgREKGNyQX1iUiq00JxsbKz6G_kDupBUTwDmTq-MZcM2U0PKLbhzoKsq_v5TmKACXfIvFC4THFDmtT9l9WNJlrb8aAm_9KSj3EqBe9SbCvCUuGjX83Oz8V2ksC2WZCEWY8DmCFDFlt1L6Idk7ZmsGm_ll_QFTQgxo2LyR2Lf01y3-dU1sCYemLds4Fer3MMzNvIjodDDOT0AQ4IvPhQ1xu-9aO-df8mRrB72DxmlDfDNenQl0YMB821uVsR89evXNPYo3Gl80KsW7_lfop7Tq95RjP1lvwO1IgaQYhvqA0aHslQv56UsbaN6dNCX7IU95AfncTE4AzkpT1SOF7R9pnoHX_kAVeIHCeqqspVWU4RIh3PbrLFBy6Er-hFl3Gk8nd318McpD0Tr8SSn1d29jXOJhWnjSJMOtOWhW3JQb2Fo44DjTm9aY-vi8B8m2NUkyYspghXasymVFrXZRHKVzd36Surj65XfRwGBnr3oH_5aVaajSSwiYhpPPkcIKJa7S6Sn8KFG5TdxopM0zAmErMQxvgmLYM-MqqV03xxX3CmUApwst3ux8h3R-zJ-MWwK4Qme40Nee9tWJwAZUDOkMASnI2g_zsCRVzS6MJAbCF6_89y_pK0ul_hSdXFbKELr1ID3zyacIAXGc-dsCqu0d316_Wi6eJQnOLesAOk3soDEcySs6oWUoU5Rq0sBoPe6Uuk9ojTGlYl-a0Qxwc4wuIYnUGDCMJ1UrABnjcPO1fZDuuxAb-liVmv12A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_Ue_kOF2NgU9PEXUOWShwxOJZRsXnGON1FzdIiLDdDHeHrMwhKlbR_tXZQ_KWC3h5oexrGiMoslquZOI3qDtmRGc6aDuEXVayt5PEOuaauOHzfB7XHk940U8unUP7uie92_hSDXjJaMDWmfSFwzQIi18DoWGhYnSU-ugtxDkTvgg,,&b64e=1&sign=b48a4b87ca18eef2e8bc77e5a33567f4&keyno=1',
                        direct:
                            'https://technopark.ru/zhestkie-diski-hdd-i-ssd-seagate-2tb-7200rpm-6gbs-256mb-st2000dm008/?utm_campaign=4&utm_content=4954&utm_medium=cpc&utm_source=yandex_market&utm_term=104544',
                    },
                },
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        isLeaf: true,
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 1000,
                            containerHeight: 1000,
                            url: '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/orig',
                            width: 1000,
                            height: 1000,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/50x50',
                                width: 50,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/55x70',
                                width: 70,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/60x80',
                                width: 80,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/74x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/75x75',
                                width: 75,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/90x120',
                                width: 120,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/100x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/120x160',
                                width: 160,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/150x150',
                                width: 150,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/180x240',
                                width: 240,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/190x250',
                                width: 250,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/200x200',
                                width: 200,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/240x320',
                                width: 320,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/300x300',
                                width: 300,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/300x400',
                                width: 400,
                                height: 400,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 600,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/600x600',
                                width: 600,
                                height: 600,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 800,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1864637/market_zvEwSID8BsFuxvgsOGKiGA/600x800',
                                width: 800,
                                height: 800,
                            },
                        ],
                        signatures: [],
                    },
                ],
                filters: [
                    {
                        id: '5127047',
                        type: 'number',
                        name: 'Емкость (точно)',
                        xslname: 'Capacity',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        unit: 'ГБ',
                        position: 5,
                        noffers: 1,
                        precision: 2,
                        values: [
                            {
                                ranges: '1.2, 300, 600, 1800, 5000',
                                max: '2000',
                                initialMax: '2000',
                                initialMin: '2000',
                                min: '2000',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127084',
                        type: 'number',
                        name: 'Количество пластин',
                        xslname: 'NumOfDisks',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 49,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 2, 7',
                                max: '1',
                                initialMax: '1',
                                initialMin: '1',
                                min: '1',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127083',
                        type: 'number',
                        name: 'Количество головок',
                        xslname: 'NumOfHeads',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 50,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '1, 4, 16',
                                max: '2',
                                initialMax: '2',
                                initialMin: '2',
                                min: '2',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                ],
                meta: {},
                marketSkuCreator: 'market',
                model: {
                    id: 130084187,
                },
                isCutPrice: false,
                delivery: {
                    shopPriorityRegion: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    shopPriorityCountry: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {
                                genitive: 'России',
                                preposition: 'в',
                                prepositional: 'России',
                                accusative: 'Россию',
                            },
                        },
                        type: 3,
                    },
                    isPriorityRegion: true,
                    isCountrywide: true,
                    isAvailable: true,
                    hasPickup: false,
                    hasLocalStore: false,
                    hasPost: false,
                    isForcedRegion: false,
                    region: {
                        entity: 'region',
                        id: 213,
                        name: 'Москва',
                        lingua: {
                            name: {
                                genitive: 'Москвы',
                                preposition: 'в',
                                prepositional: 'Москве',
                                accusative: 'Москву',
                            },
                        },
                        type: 6,
                        subtitle: 'Москва и Московская область, Россия',
                    },
                    isFree: true,
                    isDownloadable: false,
                    inStock: false,
                    postAvailable: true,
                    options: [
                        {
                            price: {
                                currency: 'RUR',
                                value: '0',
                                isDeliveryIncluded: false,
                                isPickupIncluded: false,
                            },
                            dayFrom: 3,
                            dayTo: 3,
                            isDefault: true,
                            serviceId: '99',
                            partnerType: 'regular',
                            region: {
                                entity: 'region',
                                id: 213,
                                name: 'Москва',
                                lingua: {
                                    name: {
                                        genitive: 'Москвы',
                                        preposition: 'в',
                                        prepositional: 'Москве',
                                        accusative: 'Москву',
                                    },
                                },
                                type: 6,
                                subtitle: 'Москва и Московская область, Россия',
                            },
                        },
                    ],
                    deliveryPartnerTypes: ['SHOP'],
                },
                shop: {
                    entity: 'shop',
                    id: 1925,
                    name: 'ТЕХНОПАРК',
                    business_id: 860099,
                    business_name: 'technopark.ru',
                    slug: 'tekhnopark',
                    gradesCount: 72424,
                    overallGradesCount: 72424,
                    qualityRating: 5,
                    isGlobal: false,
                    isCpaPrior: true,
                    isCpaPartner: false,
                    taxSystem: 'OSN',
                    isNewRating: true,
                    newGradesCount: 72424,
                    newQualityRating: 4.606056004,
                    newQualityRating3M: 4.558275679,
                    ratingToShow: 4.558275679,
                    ratingType: 3,
                    newGradesCount3M: 5637,
                    status: 'actual',
                    cutoff: '',
                    outletsCount: 0,
                    storesCount: 0,
                    pickupStoresCount: 0,
                    depotStoresCount: 0,
                    postomatStoresCount: 0,
                    bookNowStoresCount: 0,
                    subsidies: false,
                    logo: {
                        entity: 'picture',
                        width: 112,
                        height: 14,
                        url:
                            '//avatars.mds.yandex.net/get-market-shop-logo/1523528/2a00000167883a0c7714c93cd459c4c54fbd/orig',
                        extension: 'PNG',
                        thumbnails: [
                            {
                                entity: 'thumbnail',
                                id: '112x14',
                                containerWidth: 112,
                                containerHeight: 14,
                                width: 112,
                                height: 14,
                                densities: [
                                    {
                                        entity: 'density',
                                        id: '1',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1523528/2a00000167883a0c7714c93cd459c4c54fbd/orig',
                                    },
                                ],
                            },
                        ],
                    },
                    hasSafetyGuarantee: true,
                    domainUrl: 'technopark.ru',
                    deliveryVat: 'VAT_20',
                    feed: {
                        id: '2778',
                        offerId: '104544',
                        categoryId: '4954',
                    },
                    createdAt: '2005-10-26T11:43:33',
                    mainCreatedAt: '2005-10-26T11:43:33',
                    homeRegion: {
                        entity: 'region',
                        id: 225,
                        name: 'Россия',
                        lingua: {
                            name: {},
                        },
                        type: 0,
                    },
                },
                returnPolicy: '7d',
                wareId: 'XYN36mVtUV61u-asIPhxqA',
                offerColor: 'white',
                isFreeOffer: false,
                classifierMagicId: '02fabf5f05bace6f17b0eb12fd7fcd7d',
                prices: {
                    currency: 'RUR',
                    value: '4990',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                    rawValue: '4990',
                },
                manufacturer: {
                    entity: 'manufacturer',
                    warranty: true,
                },
                seller: {
                    comment: 'Возвращаем 250 рублей на следующую покупку',
                    price: '4990',
                    currency: 'RUR',
                    sellerToUserExchangeRate: 1,
                },
                payments: {
                    deliveryCard: true,
                    deliveryCash: true,
                    prepaymentCard: true,
                    prepaymentOther: true,
                },
                isRecommendedByVendor: false,
                bundleCount: 1,
                bundled: {
                    modelId: 130084187,
                    count: 1,
                },
                prepayEnabled: false,
                promoCodeEnabled: false,
                vat: 'VAT_20',
                feedGroupId: '0',
                isFulfillment: false,
                isAdult: false,
                isSMB: false,
                isGoldenMatrix: false,
            },
        ],
    },
    filters: [
        {
            id: 'glprice',
            type: 'number',
            name: 'Цена',
            subType: '',
            kind: 2,
            values: [
                {
                    max: '',
                    checked: true,
                    min: '1',
                    id: 'chosen',
                },
                {
                    max: '5740',
                    initialMax: '5740',
                    initialMin: '3995.62',
                    min: '3995.62',
                    id: 'found',
                },
            ],
            presetValues: [
                {
                    initialFound: 53,
                    max: '5000',
                    unit: 'RUR',
                    found: 53,
                    value: '… – 5000',
                    id: 'v5000',
                },
                {
                    initialFound: 8,
                    unit: 'RUR',
                    found: 8,
                    value: '5000 – …',
                    min: '5000',
                    id: '5000v',
                },
            ],
            meta: {},
        },
        {
            id: 'promo-type',
            type: 'enum',
            name: 'Скидки и акции',
            subType: '',
            kind: 2,
            values: [
                {
                    found: 3,
                    value: 'скидки',
                    id: 'discount',
                },
                {
                    found: 1,
                    value: 'промокоды',
                    id: 'promo-code',
                },
                {
                    found: 0,
                    value: 'подарки за покупку',
                    id: 'gift-with-purchase',
                },
                {
                    found: 0,
                    value: 'больше за ту же цену',
                    id: 'n-plus-m',
                },
            ],
            valuesGroups: [],
            meta: {},
        },
        {
            id: 'cpa',
            type: 'boolean',
            name: 'Покупка на Маркете',
            subType: '',
            kind: 2,
            values: [
                {
                    value: '0',
                },
                {
                    found: 1,
                    value: '1',
                },
            ],
            meta: {},
        },
        {
            id: 'manufacturer_warranty',
            type: 'boolean',
            name: 'Гарантия производителя',
            subType: '',
            kind: 2,
            values: [
                {
                    found: 30,
                    value: '0',
                },
                {
                    found: 31,
                    value: '1',
                },
            ],
            meta: {},
        },
        {
            id: 'credit-type',
            type: 'boolean',
            name: 'Покупка в кредит',
            subType: '',
            kind: 2,
            hasBoolNo: true,
            values: [
                {
                    initialFound: 1,
                    found: 1,
                    value: 'credit',
                },
                {
                    initialFound: 0,
                    found: 0,
                    value: 'installment',
                },
            ],
            meta: {},
        },
        {
            id: 'qrfrom',
            type: 'boolean',
            name: 'Рейтинг магазина',
            subType: '',
            kind: 2,
            hasBoolNo: true,
            values: [
                {
                    found: 61,
                    value: '3',
                },
                {
                    found: 59,
                    value: '4',
                },
            ],
            meta: {},
        },
        {
            id: 'free-delivery',
            type: 'boolean',
            name: 'Бесплатная доставка курьером',
            subType: '',
            kind: 2,
            values: [
                {
                    initialFound: 1,
                    found: 4,
                    value: '1',
                },
            ],
            meta: {},
        },
        {
            id: 'offer-shipping',
            type: 'boolean',
            name: 'Способ доставки',
            subType: '',
            kind: 2,
            hasBoolNo: true,
            values: [
                {
                    initialFound: 58,
                    found: 58,
                    value: 'delivery',
                },
                {
                    initialFound: 45,
                    found: 45,
                    value: 'pickup',
                },
                {
                    initialFound: 16,
                    found: 16,
                    value: 'store',
                },
            ],
            meta: {},
        },
        {
            id: 'payments',
            type: 'enum',
            name: 'Способы оплаты',
            subType: '',
            kind: 2,
            values: [
                {
                    initialFound: 17,
                    found: 17,
                    value: 'Картой на сайте',
                    id: 'prepayment_card',
                },
                {
                    initialFound: 7,
                    found: 7,
                    value: 'Картой курьеру',
                    id: 'delivery_card',
                },
                {
                    initialFound: 30,
                    found: 30,
                    value: 'Наличными курьеру',
                    id: 'delivery_cash',
                },
            ],
            valuesGroups: [],
            meta: {},
        },
        {
            id: 'delivery-interval',
            type: 'boolean',
            name: 'Срок доставки курьером',
            subType: '',
            kind: 2,
            hasBoolNo: true,
            values: [
                {
                    found: 2,
                    value: '0',
                },
                {
                    found: 16,
                    value: '1',
                },
                {
                    found: 45,
                    value: '5',
                },
            ],
            meta: {},
        },
        {
            id: 'fesh',
            type: 'enum',
            name: 'Магазины',
            subType: '',
            kind: 2,
            valuesCount: 61,
            values: [
                {
                    found: 1,
                    value: 'MaxMemory.ru',
                    id: '63768',
                },
                {
                    found: 1,
                    value: 'msk.you2you.ru',
                    id: '626446',
                },
                {
                    found: 1,
                    value: 'Алекомп',
                    id: '84753',
                },
                {
                    found: 1,
                    value: 'Империя техно',
                    id: '343057',
                },
                {
                    found: 1,
                    value: 'Импульс Тех',
                    id: '208256',
                },
                {
                    found: 1,
                    value: 'Компания РЕМА',
                    id: '59778',
                },
                {
                    found: 1,
                    value: 'КотоФото.Москва',
                    id: '281111',
                },
                {
                    found: 1,
                    value: 'ОКСАР.ру',
                    id: '252831',
                },
                {
                    found: 1,
                    value: 'Ситилинк',
                    id: '17436',
                },
                {
                    found: 1,
                    value: 'Современные Системы Безопасности',
                    id: '402710',
                },
                {
                    found: 1,
                    value: 'ТехноГид',
                    id: '108546',
                },
                {
                    found: 1,
                    value: 'ТЕХНОПАРК',
                    id: '1925',
                },
                {
                    found: 1,
                    value: '123.ru',
                    id: '5570',
                },
                {
                    found: 1,
                    value: '123.ру',
                    id: '323308',
                },
                {
                    found: 1,
                    value: 'B1-Store',
                    id: '598524',
                },
                {
                    found: 1,
                    value: 'BeCompact.RU',
                    id: '6363',
                },
                {
                    found: 1,
                    value: 'CIT.ru',
                    id: '779610',
                },
                {
                    found: 1,
                    value: 'compday.ru',
                    id: '104920',
                },
                {
                    found: 1,
                    value: 'CompYou',
                    id: '25017',
                },
                {
                    found: 1,
                    value: 'Flash Computers',
                    id: '3534',
                },
                {
                    found: 1,
                    value: 'IronBook.RU',
                    id: '28484',
                },
                {
                    found: 1,
                    value: 'JUST.RU',
                    id: '17019',
                },
                {
                    found: 1,
                    value: 'KAUF',
                    id: '599',
                },
                {
                    found: 1,
                    value: 'KNS.ru',
                    id: '493',
                },
                {
                    found: 1,
                    value: 'Lanbay.ru',
                    id: '309708',
                },
                {
                    found: 1,
                    value: 'MITcor.ru',
                    id: '145766',
                },
                {
                    found: 1,
                    value: 'MVA Group',
                    id: '262986',
                },
                {
                    found: 1,
                    value: 'Myshop.ru',
                    id: '582',
                },
                {
                    found: 1,
                    value: 'netshopping.ru',
                    id: '1380',
                },
                {
                    found: 1,
                    value: 'NEWMART',
                    id: '50106',
                },
                {
                    found: 1,
                    value: 'OfficeNeeds',
                    id: '577092',
                },
                {
                    found: 1,
                    value: 'OLDI.RU',
                    id: '12138',
                },
                {
                    found: 1,
                    value: 'PC4games.ru',
                    id: '295248',
                },
                {
                    found: 1,
                    value: 'PC4YOU.RU',
                    id: '538217',
                },
                {
                    found: 1,
                    value: 'QUKE.ru',
                    id: '7076',
                },
                {
                    found: 1,
                    value: 'RASM.RU Владимир',
                    id: '260041',
                },
                {
                    found: 1,
                    value: 'Realsystem.ru',
                    id: '64249',
                },
                {
                    found: 1,
                    value: 'SafeAround.ru',
                    id: '537323',
                },
                {
                    found: 1,
                    value: 'SLY Сomputers',
                    id: '339',
                },
                {
                    found: 1,
                    value: 'Store.Softline.ru',
                    id: '107253',
                },
                {
                    found: 1,
                    value: 'TopComputer.RU',
                    id: '5205',
                },
                {
                    found: 1,
                    value: 'TTT.RU',
                    id: '68728',
                },
                {
                    found: 1,
                    value: 'www.Pleer.ru',
                    id: '720',
                },
                {
                    found: 1,
                    value: 'XCOM-SHOP.RU',
                    id: '704',
                },
                {
                    found: 1,
                    value: 'XPERT.RU',
                    id: '3141',
                },
                {
                    found: 1,
                    value: 'Дельта Механикс',
                    id: '429796',
                },
                {
                    found: 1,
                    value: 'Железа.НЕТ',
                    id: '341561',
                },
                {
                    found: 1,
                    value: 'Компео',
                    id: '661088',
                },
                {
                    found: 1,
                    value: 'КомпьютерМаркет',
                    id: '12065',
                },
                {
                    found: 1,
                    value: 'Мир USB',
                    id: '557289',
                },
                {
                    found: 1,
                    value: 'Мой ПК',
                    id: '556488',
                },
                {
                    found: 1,
                    value: 'Мультимаркет Nicom',
                    id: '587240',
                },
                {
                    found: 1,
                    value: 'Неогид',
                    id: '31208',
                },
                {
                    found: 1,
                    value: 'НИКС',
                    id: '141168',
                },
                {
                    found: 1,
                    value: 'Олимп Сервис',
                    id: '371504',
                },
                {
                    found: 1,
                    value: 'ОНЛАЙН ТРЕЙД.РУ',
                    id: '255',
                },
                {
                    found: 1,
                    value: 'ПОЗИТРОНИКА',
                    id: '385207',
                },
                {
                    found: 1,
                    value: 'Регард',
                    id: '4398',
                },
                {
                    found: 1,
                    value: 'ЭЛЕКТРОЗОН',
                    id: '40885',
                },
                {
                    found: 1,
                    value: 'Энергобум',
                    id: '262978',
                },
                {
                    found: 1,
                    value: 'Яндекс.Маркет',
                    id: '431782',
                },
            ],
            valuesGroups: [],
            meta: {},
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
