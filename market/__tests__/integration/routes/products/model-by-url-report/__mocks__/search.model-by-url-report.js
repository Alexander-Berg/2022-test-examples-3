/* eslint-disable max-len */
import { REPORT_DEV_HOST, REPORT_DEV_PORT, REPORT_DEV_PATH } from '../../../../../../src/env';

const HOST = `${REPORT_DEV_HOST}:${REPORT_DEV_PORT}`;

const ROUTE = new RegExp(`/${REPORT_DEV_PATH}`);

const RESPONSE = {
    search: {
        total: 1,
        totalOffers: 1,
        totalFreeOffers: 0,
        totalOffersBeforeFilters: 1,
        totalModels: 0,
        totalPassedAllGlFilters: 1,
        adult: false,
        view: 'list',
        salesDetected: false,
        maxDiscountPercent: 0,
        shops: 1,
        totalShopsBeforeFilters: 1,
        cpaCount: 0,
        isParametricSearch: false,
        isDeliveryIncluded: false,
        isPickupIncluded: false,
        results: [
            {
                showUid: '16080524213588067317500001',
                entity: 'offer',
                trace: {
                    fullFormulaInfo: [
                        {
                            tag: 'Default',
                            name: 'MNA_fml_formula_445728',
                            value: '0.479891',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 924933,
                    name: 'AeroCool',
                    slug: 'aerocool',
                    description:
                        ', вентиляторы, мыши, клавиатуры, аудиогарнитуры, многофункциональные контроллеры управления вентиляторами.\n',
                    website: 'https://aerocool.io/ru',
                    logo: {
                        entity: 'picture',
                        url: '//avatars.mds.yandex.net/get-mpic/1589815/img_id4257945698949466180.png/orig',
                        thumbnails: [],
                        signatures: [],
                    },
                    filter: '7893318:924933',
                },
                titles: {
                    raw: 'Блок питания AEROCOOL KCAS PLUS 500, 500Вт, 120мм, черный, retail [kcas-500 plus]',
                    highlighted: [
                        {
                            value: 'Блок питания AEROCOOL KCAS PLUS 500, 500Вт, 120мм, черный, retail [kcas-500 plus]',
                        },
                    ],
                },
                slug: 'blok-pitaniia-aerocool-kcas-plus-500-500vt-120mm-chernyi-retail-kcas-500-plus',
                description:
                    'размер вентилятора 120мм; ATX; мощность: 500Вт; активный PFC; стандарт 80 PLUS BRONZE; питание MB и CPU: 24+4+4 pin; питание видеокарты: 2х(6+2)',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 857707,
                        nid: 55313,
                        name: 'Блоки питания',
                        slug: 'bloki-pitaniia',
                        fullName: 'Блоки питания для компьютеров',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                cpc:
                    'aTJMDYqbHMAbfTKTSAc1kXPeiYmnsRZepfLTmJLTgYSisHrTVoe5fSmCrKeG2HtF2CESaB8KN50RXIdOjT2Y3aVvpaKeztjjVMZmry4r0P45soQBP1fuYTlR4ImR3OUC0Fmh4ylq-nbrN_-ksinpIMB1ieizM1EY',
                urls: {
                    encrypted:
                        '/redir/GAkkM7lQwz7vv7M_pnW8mcqOHomqR5MxuVgvBAoV5qqvV_2xOtq8FObjyyLc4Qulp91H_vVRceVrCNr2GExU_A4DM5bpctq4vSz-7VusyyVBQtFCxyknk3n7Y8jWxP5sRKo0h1SRYAIcxW9jV303AwK2m1sBgjm6kS55W4QMhzHVvkWoZpHAH-iSY9FjJ8IO-CAIHs4TTDiXF-JEDCiQu66kS4brYP3GDUXq2DNDuJq2IIwXNRcmfAQ_BqJbfWqvp61cnS53dSc4QV4ZrT1ugYkI3h2gE3SM1bqaCrUFXhGyIlDddfcBSZtzdQ9pGP6qhi472L477yYfkKhE7bJfCzZ_EjNpxirBCP8x2UnzJXeqMf9QD6WKEtZ57M_RECSGHROye3Gv0czoHCI4upGuHFMX4Cxgkw9p5xgYBh3OWebGxOmBj0tQZQN9ZB67Qmzx94Cvp6DamxG_Lqc6141Gn19Q1BIXFyA9k8q9VvTvud1BAOR2CefJrVsG0eWE-iTuuQM3bbQ-nL2abuNEPF_6GlT8odufVlBC3daEH1GnLZA-CZfWv_LEGgoqIePVhe4PDuOt0slpX0t4QbYpLhA9niQUOwB-rUlgSuvD0-kBLLHg6cd2S_Nn1i4pfjyvvJ-5n3eP0NBqQdD3tC85sqtQ0mp1b-XdsizfSFil2hS9frkeckKHhDn0EDAqggjb-WQJFKtW9Hnz_nh5xVko73o0KenmY05kE_YZ-JpOOixxeUttLwnsdvHzsVwOwldZQ9RxjTagJr23DQDDR6ezOmQcy9juaLhqvvkPKDBGnNlcUiJkPwnlWvfoFjPRQGhgRftS4PVoQoJ_mYZnNCB7xVIW3XRWnIJ9wwVLzP-q30xMy5JWUWh59N0uSQNMq9D--oiR9BiMpxw2l1rDkSYn8iU_CQ,,?data=QVyKqSPyGQwNvdoowNEPjc7wlEUqfa6bq7hSZROC9KAnrMWHwKzysxnEakqqyTrx1qc4hxTdIQBYa8fpA4Wg4CHmx6PYQv3RVHLbBJMlltlyq3a7e1PcOkoP_fkU--qMu3mL-H35DBULgSskeent14198r0EwstXeM6lH9KBlKvwU2CyUS10ZhSoWQa1FBS2LJ3y65Jg9LhwQT25EyoXInk-rXRBGE5wTDEKRXswko0ZwFpMoWNwd2Zmh4avFJQdaocvJ_3nDNKKyBr3ztHtdU5t1dvgHXiF0EsCtx8oiEwTWiGMQjJpje0yQ1W8r6ILjWn6Madwb-wmT5uVNb0hYmiGMZbqcpXWb7MBO0hrEE4LFzP5XgToP8zdB43ljg4QoigC5D8GaDdpXKajAKA23CCNawlItZqneQtGs1CPtedcOWHb1XxfnhS_7r6urv7SgPTsi-RybcuRFyHiKSVKYulXHzFn_Y-cyZLZRd9pwgQrTkyLV-igvKV32-HUirJ5UDzJ3X1spV8aadBsTU7wfYcWSb36wVH9i8sqsbZDqp846GrcfmXlXubLxe1JFqvH9b4eGFOqozEs00sPFmOnLG_Wgo84jX5YdZegmJtl7peN4x1_8LU_emSB12GV69Z3z8Uz2w73Tow-DNGLQjvp6nUsNoNJ0i3EgdYz98F3w_sNemEodmagCK4l_NJmZ3FmR-P30g3VA14r7I9IlJk4gVo0q28_ReO-&b64e=1&sign=f2bcb86d5f2290c6bb0603d145ca9c79&keyno=1',
                    geo:
                        '/redir/338FT8NBgRv5c2SV5MTj4RCmaNJKFJKDNCWKIpX0zHWtDxjLGtmSeN8mbm3RfP6K7TgdWDdg2T2nqgOqMAyp_5LXkA7grh2-FMsgGxj0AWyYdsZikk6qpRe-UG2Cze_NMhQS483euTK8ZzzQ5N_N0xhdx6kDjDtkZ_dMJmS-3YWGvRTkiHBiRSiYUdZ2B6kF2757DvJPM4PbJcHGUFmG8SE4b7ElS3JI3cd-QORFXXnPMwxc7nwlQxOe0hyf3Bvuuo2N_dgjbvP4VBwhmu3L1beo_Zkn1NKCcXYaCHDQONb2D_Z4t75UOt9H78rjj1QwS355kYFUUAHhyQwRDqcY-xAw4rt6nEnSh5i1c9vTl7dgpEDBI9hSUzs0pm0KdaUiXPmVdtOzBD1mGG3a8Axb-sMpu_dpPcu_8FTkr1TAa1RqSfD4ipeIygZAEM9t7mch2riwLboQQBEIBGA_4fGdSkcjeXugbgq6TOwDVipG3HrOo55wggsbRHHxPEEL9q2w1bqbslQyjKmFE2MrXoM_3p6CWL9zN3s0gqry1VCvHuc0kvG0erLOtKv4qAh_WIn0MiWI4a2iCe36t0ji_8YntKzHwacvRmxehu__5R0cK50c_fGt-1P6ay7hVxw6KAf-F5H8LWCd5ysgoMI4KAoqsM14YcjGS0UN33CAgj8-v0J8sKWBWE43yXS3GTErptuNoIiNLak-ewfjRW57LaxjWjEoDh5jRdZEG7Ooe_werPuFeR0LOGmX5vThDjaJ8Ejzj3Y1-C4jHZVJpZ6T3aX9QJmSd2-xZn-B0oJ-JSWyEvZii4CSlgf9iaoHR3aUFIMqJrvrAJgIjGDtIFv83vo0ZnDys7KUdyHeaUoi_GIdRX7FdmQy_E7Ue6QcXO2ljGr-zKcKXEIgdWGQFvVQxBJ0ew,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrUYhIcKyNdjqYYVEyC8ngoiLSJlGpSxTEfW-sQgAgBxauGyzpbNJRBk5e-bZ3ubGwuryWjIZgexE,&b64e=1&sign=3cb50f4bb235a55efab74643d644ad69&keyno=1',
                    pickupGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4RCmaNJKFJKDNCWKIpX0zHWtDxjLGtmSeN8mbm3RfP6K7TgdWDdg2T2nqgOqMAyp_5LXkA7grh2-FMsgGxj0AWyYdsZikk6qpRe-UG2Cze_NMhQS483euTK8ZzzQ5N_N0xhdx6kDjDtkZ_dMJmS-3YWGvRTkiHBiRcF_I8UQ2DM-vB4IEUwt6bPd6xSGa8J0KnJI_Jl_kktOYCU9jeWAVWexsNecqA4X1kgux0GD5cGM8GMwM_-OPEKijc9Zf2mcCX3i9XRrPwMGYCFsjN2YVUTRmrCa8RwyIU8VoLpmdVduZsoFzzntWfh0fm3PHJDacOBE7SNqjk-h__GW8BAUh2HgqRt3Gk4pCaL3uyYxAMABSLI7u9LirP94kQvbhS2nzsdRdVM3XllxYR8HtcfTO1p55TY4QAgcpoGiLTTUuYfusw8rwGPInGe2itr18T0hcSU9qS5OztoNnAaErt4oaKlA-kONq3JznTZtOsPdcxvaGSXIw1XCDaqL7RSGl3lrVDIdZk0RE_y5D5-0a_sGc_iz0rGLaF92BcW52Wr3F7TxBDvNBixEpNJ0NePnmCWywOBwwK5_fXClVgYk_l9TB1yPh9ojZqNcEOqdQcFHgVMOFuT9rdmy5APe8egMwae-wTC__MGTV5kuaUnwj4mpJu1ejfZjgvAvspAV5GQISIvA1xVUBD4cJQS6MiKXo4ZnyVCtNrIoareLb7Ss8nK60w226W-NHPR_BcDIoioV766PIeb67SUnFsbZxXUJAdAAYXOeWamJ0rdFWTBOIkXoa2NQpTaw9EKmfRO5f0zY6xcGOWTvaAbcwRsfKGpDoOyJAQ56zrbzTp6DjW97cGKTg7GWNIHJtnRFotr9BkiXSjtCML68pLAP2fxHR0HbPLCvvg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvNq_HrKIPi6Q0F3SLYvI7LejtSplhWdOfvYr1dtGEFsUkNe7zkFGV14wVy83GFejzC_hSlOGJsVlEasotAJQWvQ,&b64e=1&sign=6c8ca2baaed6ce392c5386f606c85231&keyno=1',
                    storeGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4RCmaNJKFJKDNCWKIpX0zHWtDxjLGtmSeN8mbm3RfP6K7TgdWDdg2T2nqgOqMAyp_5LXkA7grh2-FMsgGxj0AWyYdsZikk6qpRe-UG2Cze_NMhQS483euTK8ZzzQ5N_N0xhdx6kDjDtkZ_dMJmS-3YWGvRTkiHBiRcF_I8UQ2DM-EUMyEQKLx-no2cJNEs-CKguyXamyW0ZVCyrXIJOO4LbQELjjJc7cE1R__YY4F0SPPARHD9Qd5d6Akyp6NZKqJH7LLe7Bxz2NtqVuvzWsDJV9m7OTKMbO_5GYw0KruSfWnzVq_n3_KpU63tH-YDibgPAjZRCBfoOBq_vIR9nsyAPP_bOF5_MmgtmqyCnQ5F4knx-EfBjPq-UuFHBf6JsBXlHlJpQk9WsobDKmUx_SnWRQ1vQ0suRKljvC8CPe2iDPiT5A9ltqGw73Q8zDxtuJlGZ2j-9gdgDe8ISCYnS9q5VBsFRc6UvnOzSdiL0AR3IK_W-o-8U1lAAKbr0Pdw5KInVL50CTqyJ1kZrew_pJLxzjvDUX9pvc9OcwcrLGw-PzYbKqTn_FdEDDx-YabK0GQ-yzTr1-mmaDuTckvo5WeyVZcEPxGJRw9VqygEEiYlj5eN7ikriFvKzpJHFN8BX6Zk3mKu_MubkX9xdHSZUtklo9BVvGGpettGUyE9GH_fMBf-SxncEDVj9eoI2PnMIF2YRPUMTTc6zo_Qy9lKInisIr5lCtJ8HUTrpCX4YjAk2FNkuxMRWtJmRtnJT9XU0ylkWxBkviEpRHkUXs2fXq-gALRpYYn5fLtUlbjjuIhaI3PO2f4FCFC6sSyL4VQL2ovYxNqKLro_YTDJaVeJSB2-99IP2IsQzSj01ouGI18wJ1byaZfkJzoS_jvWaBYA1Tcg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvC1BTlhscn557NF_osPORdHFHiy2knYnqMGQNHSfZLgQXnbjiMYdRrlnG2SP1Q2LVBzWicnpcPP6BTlMEee2Kdg,&b64e=1&sign=841863fd438e144c0dec9e44793714f9&keyno=1',
                    postomatGeo:
                        '/redir/338FT8NBgRv5c2SV5MTj4RCmaNJKFJKDNCWKIpX0zHWtDxjLGtmSeN8mbm3RfP6K7TgdWDdg2T2nqgOqMAyp_5LXkA7grh2-FMsgGxj0AWyYdsZikk6qpRe-UG2Cze_NMhQS483euTK8ZzzQ5N_N0xhdx6kDjDtkZ_dMJmS-3YWGvRTkiHBiRcF_I8UQ2DM-QFqMvkV0zW1b1I4XAWRO2uD8CrJ1z8T9Vu5PTCknj9Oi9BstpoXw_PguM3-FxAc1SfK1G6IeClrP3uVqrZJRUblB7bdbf6QsHABh0a7wBLPR3H9rKwB0d7eeqcDz8qaBvaRXCCtEWZp2gwva-A8oFo7gaQPxYND1e-lVyFkU5XQSbYheLQhE7b_P5Ska2c4fXQZEdayuwohjFw50CAIIYMlkt9rU3xQ8FLUqQbYdxq5s-kiF8DVZ704vSUyWx47N6sw718B7AkscIpECD_E6eMA103jDv5XImDK6gqd50J1pLIudd89og9g8-GFG5gkvnv5BSRx1NFanTIzB4nC6nE7nwaxKmlBdh8MzWj7gtw80HjV91cWVQdu1Ur0jutp2h0vNGERP70fFQHqN0-dMWYVVcx_czNykD38IGu5UVCtVJSOxnooi_ZIklcrOM_9OOO3FXdE1yrYxNSIUTWB7BKV1sqOr1qN5HkdmqDYHy-cjvt556ZwlcnkdjVHxkOK0I_TfWQmk-8j-hlDlY9KSD-DyWx7ba0tCrg018o3Hb1GU_sV9sAPj6ghgxAlefG7yNUS7zBOlyRcwOcWbDJllQDOzieHRIUcyfJl3hyiyfx8IbvZpVhuoSrRmW-VgWHzxQ7hnc0LdtUyO0y4SDHAgbmvvIkqmMVVbqvpGpojoyskbaGJuGZvqN3pO5Krp5ODl86P8Bk0fpFJybVGK0kIRCw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvPP5MUHdVnEco5sBQJVtPoNFOJYQiMHmhoUKR8u4swdfwtr0TCk4D7-iCjLYwOySIZ13oL0nmb_Q_4GnEkAv5oWZnm5XgYrI2Q,,&b64e=1&sign=a3ca74a5c4e723479c06ede2a954288b&keyno=1',
                    callPhone:
                        '/redir/GAkkM7lQwz7vv7M_pnW8mcqOHomqR5MxuVgvBAoV5qqvV_2xOtq8FObjyyLc4Qulp91H_vVRceVrCNr2GExU_A4DM5bpctq4vSz-7VusyyVBQtFCxyknk3n7Y8jWxP5sRKo0h1SRYAIcxW9jV303AwK2m1sBgjm6kS55W4QMhzHVvkWoZpHAH-iSY9FjJ8IOKOEDPtUlgNsBgaTjaMd5G6E2upkDg1wHyoxbv492q-PGw-uKp9r8te94v0shxC9slLHFbSZpysm0fggxmE4SsGIMqLcNfiSPZuX7V_zGbFMEc93hbBlWFGhOUNNqFZGzzgd-VVCkiZ_A2l6qD0KTKs7kbaGhMTUds63KWShKaCs5cwpELaPLXb3twhulA4OIeAyNrm13ji1eEH_xRsrDM_kJO2MUyqoSWAbV2WZDg7xFH8N0hl5FW1HuYA2zZX4qdSjs-Ps_AY57AssGz5wdBDg6HyleUW3-fLgoxs14v5CAOeBvA-JUTNjlMRQcGOH1ElLWAKCWdqWnTYq4l43sjFGkl9X9WYIuZO90ysB4c0EqgzLiZx2Vi50tC6qIu2xdGds09Z1VhVA0BOqwB3gw1xy-pKgwR09wuBWdwek0J4lbm-BdT5wTcRemTsKUnaZqzFK6vplXnh2g-fasI0irGXrvCkEeZYo_w_xt3yGpiOne9cBMOtjVJNqje6cUC9MADPlCIGVS849G-n_z9uVK_jlGUdaX0gzGQByeLyA7HzMEizl1aL4EcYImQOwT9Mx8oB3gRcK4wtSYQuVk_MqPjp3YXGmJQa00suoQR_ZLjGrmPV7n4KG8gZFXcXtwiAkUgFDzmoH4BEJ7diP5-sK5Dt2nSz9NhGH8O-cV9hJ35ptEFbLqh5WBF6JSbmsryQS1sNwf9jmrAFM1_IhCA7hRig,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9fMgFyxrbEb_DQBH8KLf6ROXtt_l14lyynDyjIF_EUEkELaHSGLF1bGzCgOtM_B6wvt9iN95aW51AzvK-iwEQhl94Qp6Bo6O2CJz9g2ZQjBdUQiA3KASCvG3oZ1VG01pEx7QDOXyrRrWiekhKTX4QHs4hlTT-D-I5o1_9E1UXmJQ,,&b64e=1&sign=b91487c2c6583b4ed0a394dd8804836d&keyno=1',
                    direct:
                        'https://www.citilink.ru/catalog/computers_and_notebooks/parts/powersupply/1049262/?mrkt=msk_cl&utm_medium=cpc&utm_campaign=%25D0%2591%25D0%25BB%25D0%25BE%25D0%25BA%25D0%25B8%2520%25D0%25BF%25D0%25B8%25D1%2582%25D0%25B0%25D0%25BD%25D0%25B8%25D1%258F&utm_source=xml_ymarket_msk&utm_content=40_AEROCOOL_KCAS-500%2520PLUS&utm_term=1049262',
                },
                urlsByPp: {
                    481: {
                        encrypted:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mcqOHomqR5MxuVgvBAoV5qqvV_2xOtq8FObjyyLc4Qulp91H_vVRceVrCNr2GExU_A4DM5bpctq4vSz-7VusyyVBQtFCxyknk3n7Y8jWxP5sRKo0h1SRYAIcxW9jV303AwK2m1sBgjm6kS55W4QMhzHVvkWoZpHAH-iSY9FjJ8IO-CAIHs4TTDiXF-JEDCiQu66kS4brYP3GDUXq2DNDuJq2IIwXNRcmfAQ_BqJbfWqvp61cnS53dSc4QV4ZrT1ugYkI3h2gE3SM1bqaCrUFXhGyIlDddfcBSZtzdQ9pGP6qhi472L477yYfkKhE7bJfCzZ_EjNpxirBCP8x2UnzJXeqMf9QD6WKEtZ57M_RECSGHROye3Gv0czoHCI4upGuHFMX4Cxgkw9p5xgYBh3OWebGxOmBj0tQZQN9ZB67Qmzx94Cvp6DamxG_Lqc6141Gn19Q1BIXFyA9k8q9VvTvud1BAOR2CefJrVsG0eWE-iTuuQM3bbQ-nL2abuNEPF_6GlT8odufVlBC3daEH1GnLZA-CZfWv_LEGgoqIePVhe4PDuOt0slpX0t4QbYpLhA9niQUOwB-rUlgSuvD0-kBLLHg6cd2S_Nn1i4pfjyvvJ-5n3eP0NBqQdD3tC85sqtQ0mp1b-XdsizfSFil2hS9frkeckKHhDn0EDAqggjb-WQJFKtW9Hnz_nh5xVko73o0KenmY05kE_YZ-JpOOixxeUttLwnsdvHzsVwOwldZQ9RxjTagJr23DQDDR6ezOmQcy9juaLhqvvkPKDBGnNlcUiJkPwnlWvfoFjPRQGhgRftS4PVoQoJ_mYZnNCB7xVIW3XRWnIJ9wwVLzP-q30xMy5JWUWh59N0uSQNMq9D--oiR9BiMpxw2l1rDkSYn8iU_CQ,,?data=QVyKqSPyGQwNvdoowNEPjc7wlEUqfa6bq7hSZROC9KAnrMWHwKzysxnEakqqyTrx1qc4hxTdIQBYa8fpA4Wg4CHmx6PYQv3RVHLbBJMlltlyq3a7e1PcOkoP_fkU--qMu3mL-H35DBULgSskeent14198r0EwstXeM6lH9KBlKvwU2CyUS10ZhSoWQa1FBS2LJ3y65Jg9LhwQT25EyoXInk-rXRBGE5wTDEKRXswko0ZwFpMoWNwd2Zmh4avFJQdaocvJ_3nDNKKyBr3ztHtdU5t1dvgHXiF0EsCtx8oiEwTWiGMQjJpje0yQ1W8r6ILjWn6Madwb-wmT5uVNb0hYmiGMZbqcpXWb7MBO0hrEE4LFzP5XgToP8zdB43ljg4QoigC5D8GaDdpXKajAKA23CCNawlItZqneQtGs1CPtedcOWHb1XxfnhS_7r6urv7SgPTsi-RybcuRFyHiKSVKYulXHzFn_Y-cyZLZRd9pwgQrTkyLV-igvKV32-HUirJ5UDzJ3X1spV8aadBsTU7wfYcWSb36wVH9i8sqsbZDqp846GrcfmXlXubLxe1JFqvH9b4eGFOqozEs00sPFmOnLG_Wgo84jX5YdZegmJtl7peN4x1_8LU_emSB12GV69Z3z8Uz2w73Tow-DNGLQjvp6nUsNoNJ0i3EgdYz98F3w_sNemEodmagCK4l_NJmZ3FmR-P30g3VA14r7I9IlJk4gVo0q28_ReO-&b64e=1&sign=f2bcb86d5f2290c6bb0603d145ca9c79&keyno=1',
                        geo:
                            '/redir/338FT8NBgRv5c2SV5MTj4RCmaNJKFJKDNCWKIpX0zHWtDxjLGtmSeN8mbm3RfP6K7TgdWDdg2T2nqgOqMAyp_5LXkA7grh2-FMsgGxj0AWyYdsZikk6qpRe-UG2Cze_NMhQS483euTK8ZzzQ5N_N0xhdx6kDjDtkZ_dMJmS-3YWGvRTkiHBiRSiYUdZ2B6kF2757DvJPM4PbJcHGUFmG8SE4b7ElS3JI3cd-QORFXXnPMwxc7nwlQxOe0hyf3Bvuuo2N_dgjbvP4VBwhmu3L1beo_Zkn1NKCcXYaCHDQONb2D_Z4t75UOt9H78rjj1QwS355kYFUUAHhyQwRDqcY-xAw4rt6nEnSh5i1c9vTl7dgpEDBI9hSUzs0pm0KdaUiXPmVdtOzBD1mGG3a8Axb-sMpu_dpPcu_8FTkr1TAa1RqSfD4ipeIygZAEM9t7mch2riwLboQQBEIBGA_4fGdSkcjeXugbgq6TOwDVipG3HrOo55wggsbRHHxPEEL9q2w1bqbslQyjKmFE2MrXoM_3p6CWL9zN3s0gqry1VCvHuc0kvG0erLOtKv4qAh_WIn0MiWI4a2iCe36t0ji_8YntKzHwacvRmxehu__5R0cK50c_fGt-1P6ay7hVxw6KAf-F5H8LWCd5ysgoMI4KAoqsM14YcjGS0UN33CAgj8-v0J8sKWBWE43yXS3GTErptuNoIiNLak-ewfjRW57LaxjWjEoDh5jRdZEG7Ooe_werPuFeR0LOGmX5vThDjaJ8Ejzj3Y1-C4jHZVJpZ6T3aX9QJmSd2-xZn-B0oJ-JSWyEvZii4CSlgf9iaoHR3aUFIMqJrvrAJgIjGDtIFv83vo0ZnDys7KUdyHeaUoi_GIdRX7FdmQy_E7Ue6QcXO2ljGr-zKcKXEIgdWGQFvVQxBJ0ew,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrUYhIcKyNdjqYYVEyC8ngoiLSJlGpSxTEfW-sQgAgBxauGyzpbNJRBk5e-bZ3ubGwuryWjIZgexE,&b64e=1&sign=3cb50f4bb235a55efab74643d644ad69&keyno=1',
                        pickupGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4RCmaNJKFJKDNCWKIpX0zHWtDxjLGtmSeN8mbm3RfP6K7TgdWDdg2T2nqgOqMAyp_5LXkA7grh2-FMsgGxj0AWyYdsZikk6qpRe-UG2Cze_NMhQS483euTK8ZzzQ5N_N0xhdx6kDjDtkZ_dMJmS-3YWGvRTkiHBiRcF_I8UQ2DM-vB4IEUwt6bPd6xSGa8J0KnJI_Jl_kktOYCU9jeWAVWexsNecqA4X1kgux0GD5cGM8GMwM_-OPEKijc9Zf2mcCX3i9XRrPwMGYCFsjN2YVUTRmrCa8RwyIU8VoLpmdVduZsoFzzntWfh0fm3PHJDacOBE7SNqjk-h__GW8BAUh2HgqRt3Gk4pCaL3uyYxAMABSLI7u9LirP94kQvbhS2nzsdRdVM3XllxYR8HtcfTO1p55TY4QAgcpoGiLTTUuYfusw8rwGPInGe2itr18T0hcSU9qS5OztoNnAaErt4oaKlA-kONq3JznTZtOsPdcxvaGSXIw1XCDaqL7RSGl3lrVDIdZk0RE_y5D5-0a_sGc_iz0rGLaF92BcW52Wr3F7TxBDvNBixEpNJ0NePnmCWywOBwwK5_fXClVgYk_l9TB1yPh9ojZqNcEOqdQcFHgVMOFuT9rdmy5APe8egMwae-wTC__MGTV5kuaUnwj4mpJu1ejfZjgvAvspAV5GQISIvA1xVUBD4cJQS6MiKXo4ZnyVCtNrIoareLb7Ss8nK60w226W-NHPR_BcDIoioV766PIeb67SUnFsbZxXUJAdAAYXOeWamJ0rdFWTBOIkXoa2NQpTaw9EKmfRO5f0zY6xcGOWTvaAbcwRsfKGpDoOyJAQ56zrbzTp6DjW97cGKTg7GWNIHJtnRFotr9BkiXSjtCML68pLAP2fxHR0HbPLCvvg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvNq_HrKIPi6Q0F3SLYvI7LejtSplhWdOfvYr1dtGEFsUkNe7zkFGV14wVy83GFejzC_hSlOGJsVlEasotAJQWvQ,&b64e=1&sign=6c8ca2baaed6ce392c5386f606c85231&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4RCmaNJKFJKDNCWKIpX0zHWtDxjLGtmSeN8mbm3RfP6K7TgdWDdg2T2nqgOqMAyp_5LXkA7grh2-FMsgGxj0AWyYdsZikk6qpRe-UG2Cze_NMhQS483euTK8ZzzQ5N_N0xhdx6kDjDtkZ_dMJmS-3YWGvRTkiHBiRcF_I8UQ2DM-EUMyEQKLx-no2cJNEs-CKguyXamyW0ZVCyrXIJOO4LbQELjjJc7cE1R__YY4F0SPPARHD9Qd5d6Akyp6NZKqJH7LLe7Bxz2NtqVuvzWsDJV9m7OTKMbO_5GYw0KruSfWnzVq_n3_KpU63tH-YDibgPAjZRCBfoOBq_vIR9nsyAPP_bOF5_MmgtmqyCnQ5F4knx-EfBjPq-UuFHBf6JsBXlHlJpQk9WsobDKmUx_SnWRQ1vQ0suRKljvC8CPe2iDPiT5A9ltqGw73Q8zDxtuJlGZ2j-9gdgDe8ISCYnS9q5VBsFRc6UvnOzSdiL0AR3IK_W-o-8U1lAAKbr0Pdw5KInVL50CTqyJ1kZrew_pJLxzjvDUX9pvc9OcwcrLGw-PzYbKqTn_FdEDDx-YabK0GQ-yzTr1-mmaDuTckvo5WeyVZcEPxGJRw9VqygEEiYlj5eN7ikriFvKzpJHFN8BX6Zk3mKu_MubkX9xdHSZUtklo9BVvGGpettGUyE9GH_fMBf-SxncEDVj9eoI2PnMIF2YRPUMTTc6zo_Qy9lKInisIr5lCtJ8HUTrpCX4YjAk2FNkuxMRWtJmRtnJT9XU0ylkWxBkviEpRHkUXs2fXq-gALRpYYn5fLtUlbjjuIhaI3PO2f4FCFC6sSyL4VQL2ovYxNqKLro_YTDJaVeJSB2-99IP2IsQzSj01ouGI18wJ1byaZfkJzoS_jvWaBYA1Tcg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvC1BTlhscn557NF_osPORdHFHiy2knYnqMGQNHSfZLgQXnbjiMYdRrlnG2SP1Q2LVBzWicnpcPP6BTlMEee2Kdg,&b64e=1&sign=841863fd438e144c0dec9e44793714f9&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRv5c2SV5MTj4RCmaNJKFJKDNCWKIpX0zHWtDxjLGtmSeN8mbm3RfP6K7TgdWDdg2T2nqgOqMAyp_5LXkA7grh2-FMsgGxj0AWyYdsZikk6qpRe-UG2Cze_NMhQS483euTK8ZzzQ5N_N0xhdx6kDjDtkZ_dMJmS-3YWGvRTkiHBiRcF_I8UQ2DM-QFqMvkV0zW1b1I4XAWRO2uD8CrJ1z8T9Vu5PTCknj9Oi9BstpoXw_PguM3-FxAc1SfK1G6IeClrP3uVqrZJRUblB7bdbf6QsHABh0a7wBLPR3H9rKwB0d7eeqcDz8qaBvaRXCCtEWZp2gwva-A8oFo7gaQPxYND1e-lVyFkU5XQSbYheLQhE7b_P5Ska2c4fXQZEdayuwohjFw50CAIIYMlkt9rU3xQ8FLUqQbYdxq5s-kiF8DVZ704vSUyWx47N6sw718B7AkscIpECD_E6eMA103jDv5XImDK6gqd50J1pLIudd89og9g8-GFG5gkvnv5BSRx1NFanTIzB4nC6nE7nwaxKmlBdh8MzWj7gtw80HjV91cWVQdu1Ur0jutp2h0vNGERP70fFQHqN0-dMWYVVcx_czNykD38IGu5UVCtVJSOxnooi_ZIklcrOM_9OOO3FXdE1yrYxNSIUTWB7BKV1sqOr1qN5HkdmqDYHy-cjvt556ZwlcnkdjVHxkOK0I_TfWQmk-8j-hlDlY9KSD-DyWx7ba0tCrg018o3Hb1GU_sV9sAPj6ghgxAlefG7yNUS7zBOlyRcwOcWbDJllQDOzieHRIUcyfJl3hyiyfx8IbvZpVhuoSrRmW-VgWHzxQ7hnc0LdtUyO0y4SDHAgbmvvIkqmMVVbqvpGpojoyskbaGJuGZvqN3pO5Krp5ODl86P8Bk0fpFJybVGK0kIRCw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2a-7Bj5iCfJrfTeKGkP9grqOvpvRq02JvPP5MUHdVnEco5sBQJVtPoNFOJYQiMHmhoUKR8u4swdfwtr0TCk4D7-iCjLYwOySIZ13oL0nmb_Q_4GnEkAv5oWZnm5XgYrI2Q,,&b64e=1&sign=a3ca74a5c4e723479c06ede2a954288b&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz7vv7M_pnW8mcqOHomqR5MxuVgvBAoV5qqvV_2xOtq8FObjyyLc4Qulp91H_vVRceVrCNr2GExU_A4DM5bpctq4vSz-7VusyyVBQtFCxyknk3n7Y8jWxP5sRKo0h1SRYAIcxW9jV303AwK2m1sBgjm6kS55W4QMhzHVvkWoZpHAH-iSY9FjJ8IOKOEDPtUlgNsBgaTjaMd5G6E2upkDg1wHyoxbv492q-PGw-uKp9r8te94v0shxC9slLHFbSZpysm0fggxmE4SsGIMqLcNfiSPZuX7V_zGbFMEc93hbBlWFGhOUNNqFZGzzgd-VVCkiZ_A2l6qD0KTKs7kbaGhMTUds63KWShKaCs5cwpELaPLXb3twhulA4OIeAyNrm13ji1eEH_xRsrDM_kJO2MUyqoSWAbV2WZDg7xFH8N0hl5FW1HuYA2zZX4qdSjs-Ps_AY57AssGz5wdBDg6HyleUW3-fLgoxs14v5CAOeBvA-JUTNjlMRQcGOH1ElLWAKCWdqWnTYq4l43sjFGkl9X9WYIuZO90ysB4c0EqgzLiZx2Vi50tC6qIu2xdGds09Z1VhVA0BOqwB3gw1xy-pKgwR09wuBWdwek0J4lbm-BdT5wTcRemTsKUnaZqzFK6vplXnh2g-fasI0irGXrvCkEeZYo_w_xt3yGpiOne9cBMOtjVJNqje6cUC9MADPlCIGVS849G-n_z9uVK_jlGUdaX0gzGQByeLyA7HzMEizl1aL4EcYImQOwT9Mx8oB3gRcK4wtSYQuVk_MqPjp3YXGmJQa00suoQR_ZLjGrmPV7n4KG8gZFXcXtwiAkUgFDzmoH4BEJ7diP5-sK5Dt2nSz9NhGH8O-cV9hJ35ptEFbLqh5WBF6JSbmsryQS1sNwf9jmrAFM1_IhCA7hRig,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O9fMgFyxrbEb_DQBH8KLf6ROXtt_l14lyynDyjIF_EUEkELaHSGLF1bGzCgOtM_B6wvt9iN95aW51AzvK-iwEQhl94Qp6Bo6O2CJz9g2ZQjBdUQiA3KASCvG3oZ1VG01pEx7QDOXyrRrWiekhKTX4QHs4hlTT-D-I5o1_9E1UXmJQ,,&b64e=1&sign=b91487c2c6583b4ed0a394dd8804836d&keyno=1',
                        direct:
                            'https://www.citilink.ru/catalog/computers_and_notebooks/parts/powersupply/1049262/?mrkt=msk_cl&utm_medium=cpc&utm_campaign=%25D0%2591%25D0%25BB%25D0%25BE%25D0%25BA%25D0%25B8%2520%25D0%25BF%25D0%25B8%25D1%2582%25D0%25B0%25D0%25BD%25D0%25B8%25D1%258F&utm_source=xml_ymarket_msk&utm_content=40_AEROCOOL_KCAS-500%2520PLUS&utm_term=1049262',
                    },
                },
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 55313,
                        name: 'Блоки питания',
                        slug: 'bloki-pitaniia',
                        fullName: 'Блоки питания для компьютеров',
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
                            url: '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/orig',
                            width: 500,
                            height: 500,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/50x50',
                                width: 50,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/55x70',
                                width: 70,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/60x80',
                                width: 80,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/74x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/75x75',
                                width: 75,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/90x120',
                                width: 120,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/100x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/120x160',
                                width: 160,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/150x150',
                                width: 150,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/180x240',
                                width: 240,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/190x250',
                                width: 250,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/200x200',
                                width: 200,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/240x320',
                                width: 320,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/300x300',
                                width: 300,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/248830/market_56GpefspeYo9WrByWzYZog/300x400',
                                width: 400,
                                height: 400,
                            },
                        ],
                        signatures: [],
                    },
                ],
                meta: {},
                marketSkuCreator: 'market',
                model: {
                    id: 43055885,
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
                            dayFrom: 3,
                            dayTo: 5,
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
                    slug: 'sitilink',
                    gradesCount: 370744,
                    overallGradesCount: 370744,
                    qualityRating: 4,
                    isGlobal: false,
                    isCpaPrior: true,
                    isCpaPartner: false,
                    isNewRating: true,
                    newGradesCount: 370744,
                    newQualityRating: 4.520461558,
                    newQualityRating3M: 4.437228898,
                    ratingToShow: 4.437228898,
                    ratingType: 3,
                    newGradesCount3M: 48183,
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
                        offerId: '1049262',
                        categoryId: '40',
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
                wareId: 'bXMbw7h_FOjFTTeWOkwCvg',
                offerColor: 'white',
                isFreeOffer: false,
                classifierMagicId: 'ec675dfdb276e747045711f837f7780e',
                prices: {
                    currency: 'RUR',
                    value: '3410',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                    rawValue: '3410',
                },
                manufacturer: {
                    entity: 'manufacturer',
                    warranty: true,
                    code: 'KCAS-500 PLUS',
                },
                seller: {
                    comment: 'Дарим клубную карту Ситилинк при покупке от 5000р.',
                    price: '3410',
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
                outlet: {
                    entity: 'outlet',
                    id: '266337602',
                    name: 'Москва, ул Раменки, д.23',
                    purpose: ['pickup'],
                    daily: false,
                    'around-the-clock': false,
                    gpsCoord: {
                        longitude: '37.492985',
                        latitude: '55.689832',
                    },
                    type: 'pickup',
                    serviceId: 99,
                    serviceName: 'Собственная служба',
                    isMarketBranded: false,
                    isMegaPoint: false,
                    email: '',
                    shop: {
                        id: 17436,
                    },
                    address: {
                        fullAddress: 'Москва, ул. Раменки, д. 23',
                        country: '',
                        region: '',
                        locality: 'Москва',
                        street: 'ул. Раменки',
                        km: '',
                        building: '23',
                        block: '',
                        wing: '',
                        estate: '',
                        entrance: '',
                        floor: '',
                        room: '',
                        office_number: '',
                        note: '1 этаж',
                    },
                    telephones: [
                        {
                            entity: 'telephone',
                            countryCode: '7',
                            cityCode: '926',
                            telephoneNumber: '8002688',
                            extensionNumber: '',
                        },
                    ],
                    workingTime: [
                        {
                            daysFrom: '2',
                            daysTo: '2',
                            hoursFrom: '11:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '3',
                            daysTo: '3',
                            hoursFrom: '11:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '4',
                            daysTo: '4',
                            hoursFrom: '11:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '5',
                            daysTo: '5',
                            hoursFrom: '11:00',
                            hoursTo: '20:00',
                        },
                        {
                            daysFrom: '6',
                            daysTo: '6',
                            hoursFrom: '11:00',
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
                isDailyDeal: false,
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
                    max: '3410',
                    initialMax: '3410',
                    initialMin: '3410',
                    min: '3410',
                    id: 'found',
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
                    found: 0,
                    value: 'скидки',
                    id: 'discount',
                },
                {
                    found: 0,
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
            id: 'manufacturer_warranty',
            type: 'boolean',
            name: 'Гарантия производителя',
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
            id: 'onstock',
            type: 'boolean',
            name: 'В продаже',
            subType: '',
            kind: 2,
            values: [
                {
                    initialFound: 1,
                    checked: true,
                    value: '0',
                },
                {
                    initialFound: 1,
                    found: 1,
                    value: '1',
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
                    found: 1,
                    value: '4',
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
                    initialFound: 1,
                    found: 1,
                    value: 'delivery',
                },
                {
                    initialFound: 1,
                    found: 1,
                    value: 'pickup',
                },
                {
                    initialFound: 1,
                    found: 1,
                    value: 'store',
                },
            ],
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
                    found: 1,
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
            valuesCount: 1,
            values: [
                {
                    found: 1,
                    value: 'Ситилинк',
                    id: '17436',
                },
            ],
            valuesGroups: [],
            meta: {},
        },
    ],
    intents: [
        {
            defaultOrder: 0,
            ownCount: 0,
            relevance: 0.296731,
            category: {
                name: 'Компьютерная техника',
                slug: 'kompiuternaia-tekhnika',
                uniqName: 'Компьютерная техника',
                hid: 91009,
                nid: 54425,
                isLeaf: false,
                kinds: [],
                view: 'list',
            },
            intents: [
                {
                    defaultOrder: 1,
                    ownCount: 0,
                    relevance: -0.171547,
                    category: {
                        name: 'Комплектующие',
                        slug: 'komplektuiushchie',
                        uniqName: 'Компьютерные комплектующие',
                        hid: 91018,
                        nid: 54536,
                        isLeaf: false,
                        kinds: [],
                        view: 'list',
                    },
                    intents: [
                        {
                            defaultOrder: 2,
                            ownCount: 1,
                            relevance: -0.350432,
                            category: {
                                name: 'Блоки питания',
                                slug: 'bloki-pitaniia',
                                uniqName: 'Блоки питания для компьютеров',
                                hid: 857707,
                                nid: 55313,
                                isLeaf: true,
                                kinds: [],
                                view: 'list',
                            },
                        },
                    ],
                },
            ],
        },
    ],
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
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
