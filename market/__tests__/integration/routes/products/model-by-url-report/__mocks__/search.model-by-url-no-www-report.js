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
                showUid: '16080674021704273138900001',
                entity: 'offer',
                trace: {
                    fullFormulaInfo: [
                        {
                            tag: 'Default',
                            name: 'MNA_fml_formula_445728',
                            value: '0.5516',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 7776310,
                    name: 'PowerCase',
                    slug: 'powercase',
                    website: 'http://www.powercase.com.cn',
                    filter: '7893318:7776310',
                },
                titles: {
                    raw: 'Корпус для компьютера PowerCase Alisio Mesh M White',
                    highlighted: [
                        {
                            value: 'Корпус для компьютера PowerCase Alisio Mesh M White',
                        },
                    ],
                },
                slug: 'korpus-dlia-kompiutera-powercase-alisio-mesh-m-white',
                description: 'Midi-Tower • ATX • БП нет • Отсеки: 3.5"(внутр)-3',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 91028,
                        nid: 55319,
                        name: 'Корпуса',
                        slug: 'korpusa',
                        fullName: 'Компьютерные корпуса',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                cpc:
                    'I0eXihpXOY_QN5QTvP1VpJ4c6th7CRPJ5eV23eKHlD8sLyYGxHYl6Zaq-H_XjQOLu2SeKPGRhmjDZKBLmYMjzzBJhWYHv1so8PpqvBxA8Rob9obwEVfaY-x1oMmF_yRHTsST_43PI1hGHrHUDG5KcIdOxN5oV5yItc1hz4baxo8,',
                urls: {
                    encrypted:
                        '/redir/GAkkM7lQwz62j9BQ6_qgZjo5r7eapfkKd5dp7FUbcNVt-42fig-1QurW2LOJIhOfaUhkYP-0CTyEXoM7OE5KQhw4BUFKx1sT1y51SAcGOShv2BBm9SdERmrUPusShqrHtL4WGCoHI9xvSA0fquXW6TfWRmhQkNnSvoyDcU4DR1u4_cVkaY_IzWE89PPVemOJuIGEbe1a3Mbb61ZXKEv-ZgA9btcbvMY2jbVPIyQTKOd8oCAXQws2p0JhkkXwWcDZM_yghdxrvi4MzZek7YBqYbTe7jNSWgP2-OSofFjxJ84CnjpmZfunf4npMIbTsI_ElecWk9NOaUQYvKNkmBGZ5-QKBWBSR59KlyJxtANnjvua0teAbx_Y1DHHS662FyKRS-EZIjcwImNLhyh9VDxOXTzoJPGONcoNY_U5UUER3JBIAknJ4GHTrlMAyRlyoMkutRH3hQQyWx3tYUbirSzU5io_RmJd4pN8d7nEQf4rnzzLXtWlTpmMDL_zFTU8Q4YklfBsid1r_0WCpS2POTGjc_lenEGkHN_KSFwoMI2Gblkz3L2oONXj0MM74yr_LYzCR4ICZi4aHPdKvc-hXHZX_K-7Bw5fnmuTOaiIzx1AWi3dwXzbB-qBmN1aCam1c4HQmG8a_uYFJvtuPj1CO-sg_ZpNmvZp3HUd01X5geEu1zAhscMKjVOZFlpwyD46K6Exvfs3Xy_ImNVtWZb28Qrkw1sSJNbjZUj6pGD0Z7isxD1cMSf3wTOFUDpsM8gjyyXqqEMmCmPq_4L9gE1fyMyvlta0d-nibGqHInKRAdZPwsirbYSixcDM06E4vsCo4zEsJmnweL37_7T_85HKVm2c2E_g6NGd0b2DGa0CdX32K1x1vBQWyc9o2DdXMY5AFJ3VhjQuJXNwxt8,?data=QVyKqSPyGQwNvdoowNEPjbv5NDGkDw-huUpGHotaEKMse75hWj8mPDTfaRky_s55A58suaRDQtxvFXLOyDXmUnzhvnPV4kzWHz1cxBes3r5apEtmEVn9x-eSJNqyD0rcmc736RnsoUVSrCU5bhONhrlEtG4kdK10TiGb5wp4lgjtUPM5HDpF0iJLZQxUgIVO5G1o_US5drSFw3IVL7MUM0-hizF1gsA9KIkrP-bZ7UhNdlCeUsG3t_7fuKPoKuYWNrWrYlbnQxLGGNImvrl7919Gk4whipSivZroJO5YoJNsVsPcTsOlD-I1COD5Z6HbhOFM28JElJXC7xDu6LDoNY7THkr0UbGJFyEWf1BET4-M5g1gOuFytsjpieaLdBaIPqU_nQWOyEVhqp4bzl0qubFmNvOnE1zp36qVcM5NcBYBJ9bXtod5uy323jtHx4zeyXDu5lAr0gOCAMdz4cUJjyUbxV-aT38ktXB9CwbYwoK2hlOm-6Q8bzM42eGX6SzK&b64e=1&sign=b802b40d4eab98dd24ea401466152ac7&keyno=1',
                    geo:
                        '/redir/338FT8NBgRtZwUfhCY-hTXHnsd-hU4JdbUwsJ3YY-0x1CleVwyxHUuggXdEDHJN87oZsCqPuZ8fTCxYZo7ekU52O3D4dERkJ0DkkZOunLSBH_5qnxJL29yARkgpyqrGlC_IPAuFrYOeW6flj0UVpNWT3VcV_xA2Ej4q3WLwpz9LGV4l5j4IpaLhaQqqyZs1sJ2OlJVUEDwyO4HbMl8LqcUbvYLMW7Akq5IiMFMtEvBvuAS3k-6pGg6tPs2NANi9-plqc2uT8dpjUA6B15AWwHyh4FKpVmP8VcQfr4OqF6QD0OdYZIwgc9dA7Jj6_WTOyn2CjBxmXanO3Orfef7rwqN-K0S6K5YJ3yigrJsQC8WA1WgSbF1ApG2IdVnkxIjDK7JIDzgGJWp6zoXi6t-p7gygwCCgOxdsNJDMiajhU26QPnMtoTUs36LaaYGjxxbFMJ_ohcEHPtoZIbVPsGlZndAFp8SDhbQ1FDJyE5H0ZIxOa5GrgynA8YhwoW9wJpzni9MrqwJv5KFjfQxLNTue81zVO9Ht3bZ2mdAVMNaNhzyvAk1A1PDD6TL4RnT9Zc7FoH-NNyXcVrVpPeolcq0G6Ccvvg5nZB4UgQBQ9pQn0YTWtJRZjbr-x3AspKRiHqTkiJ2eODY_UFW4U5mp8Gs69rtcky8CagZKlq13hFr8bbWcm-DzUcfzNTznVUgNAgIPt0Eor0NKjYOzByvhyA01AWX1uTVomPAytBkCyYTsts9PpoNtfRdO2LNBHowfZkMMnS6nJqG8slhr5J53Z9g1wlAwA3XxNZP7yv2N_rt6jVLCJ-mu3XBTp7EouV5A66KPtTDw3X9yZOFI1CnoE6m2T5nMKA6SLqI2LoGAr8Meuqdg6PAzFfwNYdtNSWho4WNPF?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bvDfds-sUcJ0iKSu_dtt81pla87zQCvgVapB6PIJlG2tCoO-fnLfq8lr-J5LUtT0RsttmcReARfjBM-Bmoh8HU,&b64e=1&sign=6ebbbfc029166385e9c8bcb1e58b10ed&keyno=1',
                    pickupGeo:
                        '/redir/338FT8NBgRtZwUfhCY-hTXHnsd-hU4JdbUwsJ3YY-0x1CleVwyxHUuggXdEDHJN87oZsCqPuZ8fTCxYZo7ekU52O3D4dERkJ0DkkZOunLSBH_5qnxJL29yARkgpyqrGlC_IPAuFrYOeW6flj0UVpNWT3VcV_xA2Ej4q3WLwpz9IsxMeO7W5FDaN7wIwoGLbMQB7VZk6jUvXbBh71_zLHqSC70Dj01dsYGYW6RQ84tVKSXTYlQil_XSH8P0BRj_o864PSbL9LIN9Pl1BHw9g6kPi2vpQTUx7P1mOZ7YEchP-tmaVgl37io4nmhyrhfITA-Bbx1bdRwP4gG4HVnlXkVOV_l38YlHA2P52D4G-Z5ZrK0Kupb1iD2wvMlzYKTUlEscejibE-34q-xeKEEm98dBjFQDTZuDCTjjQiGqsLvcdoQ1S9ZRSRwdF1iu-m22wXlX-Vle4KvixQ5Q0R5aRgXvzlHNC6kXSAv_9-bh9B66HCCXpKi4yGS19WL8aniCgBBJ2BB7LE3l1GJKM_6aqySgoaG5UcO7kyPiuCqU-LrfpUZ8hrgVgUl4toYJpTBoAcIg5Sr69JDLC8eiLj244NhRaPzojKSNNvMLMr6vuUbUGdv1Bf6YzRmaqLnnXhWeMNrf6aW37qtoiP0shzfKCczzxZ5FSNAfisPfGL31dn8lYksQgSfggaLUy1s5jR3UDXrpCbY4hadnsB2IwKz0QRRKYWSTP_H2NOSBKjwpuXv2waY2kN32S5Y18l7_J5HIP1mCkEEUjxcPNDO0mBNCB0O-YVPVlbpiTpJDeddi_NySXyb4l7LehySgT7hXJWr9qY_pCtvC-WIxZBE5IaHcwqOlZTlc0_PE0-JbcNo6hnKUG-FbO5_7xzf9a6-juO0M0Z?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bvDfds-sUcJ5VvvbvvUrhojXUySvgxksYCCAOx4yDB091gW7KiISaC19s4gW8zXpgWbb-fnIW4YYKTy4dj18UpGw7A0r0_2Wrj1JzHz_8X4GnBf2G1DEz4,&b64e=1&sign=4aaf7b360645ca1253213c76e6c7b32d&keyno=1',
                    storeGeo:
                        '/redir/338FT8NBgRtZwUfhCY-hTXHnsd-hU4JdbUwsJ3YY-0x1CleVwyxHUuggXdEDHJN87oZsCqPuZ8fTCxYZo7ekU52O3D4dERkJ0DkkZOunLSBH_5qnxJL29yARkgpyqrGlC_IPAuFrYOeW6flj0UVpNWT3VcV_xA2Ej4q3WLwpz9JTkab7-Wha2NMMJwqYi4TmuDstZpfnINbtSQgLTLjR1Iel7JiVAGprqJ6rizsdPxhBTnEN2dG1EPlBvX-qIHx6dy1LEbsfY5cDtDHiOWZEIxAffhIwVGxYb00n-Fr1bRGsRsP8SJo_ZG45vjd1KmWuqfVpNpglrSYIkPRkj9UezpUDAhHd8klEeAJmJJP1WNgQWOY2iInXYtBLLKcuqCgtm38eLQl-daeixGnxb5iQ_ZPNWb9aILmjBwl4swAwGRpStJhDkTuMCymJZE3K-C6yHEJ__zgqe0ktptNYAPRDqk4xXb9cDWEKCX-nYjXZ1cYvHoDqfJq_oZE500m6_hdygh1KebyAhOeb96idNdg2bvwKlTp_XI2j_hwRxggfzK0mmPp4PUN7iqou35YqP7jojL6WUvR3fGZk1NNwwU4xctaaXDcQNggtVSp-KJ3bJhe6fnr7bHViLB8bslGxLFWc9k9nDELQm0pCjpoLT9QlgzcfnOtBbBQJ-1amsrjVqTpkuSq5MlZqQtTZ9oTrIN47nKUU7BtgBCwv1wwu4jaTu1DpTXsL6IIJs6aXW3vGkzMCa2B_UY6IsFaBhooQoLpKHH-wWb6qzHX8JDC2a98G7R6XCV6yeFvlgqp6jaKdjE_SDbIJIfe2s4m5uk7QjsI8VT9IC5qtwUYrt2V3Qr5Tlw9qwTrYgq8-HGmZrQFpVbxcbaP7N6YbndUfS2y_UtdW?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bvDfds-sUcJ5VvvbvvUrhojXUySvgxksYZlt_cYH9e6FFZhI2kR1t7ze9V_6ECoiHnWCMwA1u_1jwCQz1KyYPrRRxgy9dNE6EdI61RKgnWAyG1oE0t1neI,&b64e=1&sign=b7e68099195afb368a8c3470c7a54291&keyno=1',
                    postomatGeo:
                        '/redir/338FT8NBgRtZwUfhCY-hTXHnsd-hU4JdbUwsJ3YY-0x1CleVwyxHUuggXdEDHJN87oZsCqPuZ8fTCxYZo7ekU52O3D4dERkJ0DkkZOunLSBH_5qnxJL29yARkgpyqrGlC_IPAuFrYOeW6flj0UVpNWT3VcV_xA2Ej4q3WLwpz9KHSYjWCEnDvGRjJKuDZe8DY8xvKLPo8U-gOc_GX5S5uh_nM5nKKY5x81-kQPq6Hri1_GF6Owl6xr32XOupehPvHJjr-5HosPZiB8zxCWHZtZrMbIw3hfG8EhU1b89OWPlj1vtSoodDTRItjq1vMGeWQaKWtfwzbMUPP1jDy_3vXhy6iYJ56kMNMsCJs7CeioL7aqurMw_SAm5CH-OW_kJrDESbeItiZ16TweOBLZxd4Av9SKl_hUKa14BTrS2G2TDFe5ymx6Ilh6okLoAwtfNKVAA192rqg-RSNjbUO7xzJfNChVL9pZfem2QMctBa0md0usVlK-QAf50n08ug30kLOLmC-B6YeJwJZLHK2SY2ptwxzGTpj-1ES6Kq3_BMtCmgnnuvylz7YBSu3FlSuU4g1GBy7y2w012SzGo2BVVR57wxh19jq9w4_Gc2f5dlaTGId8aZww5zFvouOvbgv_rVEgPmK8LVCIB4FAO1Q2HnxpPDL1z6sqGiNBktmLE1TsTL3t9_wAhkQWxSRZuP5YbztPlVAedfDprngTJMwf24XCfn3ufwaggxOfpY2EEtftWBAp6T_gNnxELv9rMPi3b5VHE5c0Dk11c798BauhR05wKQx6BxzC4RMi273UbumPDeH-vtO2di2Q5qYsv-IwqtthS8zujkGJLqmQhUJ9dSBxAZqucarOPuYWLCC1oNEZR5PMscdp6ct1Ge2XvM6N6M?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bvDfds-sUcJ5VvvbvvUrhojXUySvgxksb5Ncoz2Y5U9unQyXb5mDYmA6wAlPBYEZ3DAuZn7b8oGFedzz-4D2zxiLuhz-YRkgW0LLdNfTVQPV2OejbOxN3Q,&b64e=1&sign=247f8f2e3a70007949b9e2895f3ee40e&keyno=1',
                    callPhone:
                        '/redir/GAkkM7lQwz62j9BQ6_qgZjo5r7eapfkKd5dp7FUbcNVt-42fig-1QurW2LOJIhOfaUhkYP-0CTyEXoM7OE5KQhw4BUFKx1sT1y51SAcGOShv2BBm9SdERmrUPusShqrHtL4WGCoHI9xvSA0fquXW6TfWRmhQkNnSvoyDcU4DR1uBxfElLD3EoxVoIgBigZ3X8lE6bKxvsXHW4eQkaJ8Qv4dzv9FkiPz1bFh1eR-1OUsexkkgcC5PwWhmJ_9fqiyp0nubXr1WrFm7-MdCqkKGiTUZyqV2LTCEC21RI41bYj1KLKlRdwUZ8siqbeBmpx4E3pK3v7iiLCI__UUJI3yhv4peMdEWY8xfAPWs7YbV4ua_zpbHe9YPc0K1mzNSXLxKUiUGXTFmmJ-FDf6K9E0gm2J_1A3sjvDcTKbaEoomNFGQs3iD3EuRZa5RvWzrLGVEsNO4Dyksin1vyQBU12B_Cb96VGqyUIXXUI-drP8m7OFM5ZCp39ZFLqymXuiovKnhWkiTsmqagpYz3sJfMYOp6jI9-6D3OYr7Q-dW6kTuSzLoXJXE-7Kn4KqruXGjLzrgjxYcOU31vah_PY3egstidkQgsIsTMJhEH2OHH6sFl_rQZl82FPkmFcTPBT5wK2f66MxjIifAH9rA6W7wOKs3rfLWUZ2NWES43KUspETuF0LIyxidBmgMx2LOWFZrE6JrLHEszyg9iiTfG6zNsqBUmgvqeuj8Lmarn3MooI0uuWInsGLdMzNU6KlTVRSUv5r5hIlYb9Dkfox65N3bok_Jx2NOTIcwTSrSMWoCwq4w8cXQDrC3HnJQ5zyGWmHTBdNFSTpNUSCiHY-kllxDliGL3Nb1tkO9U6PhlHyfzogDCdcxRF0msz62UJGyfhrA6bgyO1HLL5mWx2U,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_1zRUUtXPDO4eX-s_yYbVh6A6d8q-rMnfJfWWLN6CLaPAiCkgiNcVV4kKNe3qMbHrd-vsKMjFko_ztiSvexzSeaVa6UpUCMD0qWSdYw7mpRD9cAJ0zY_xgdm6WtWIGX-LG1wn-EYAIRCR7-4oUedGssWM3Uc9EQQB9jR4JwZsj8w,,&b64e=1&sign=d1ab475d08a0827b16a4236754e5e5d8&keyno=1',
                    direct: 'https://topcomputer.ru/tovary/1585606/?r1=yandex&utm_source=market.yandex.ru',
                },
                urlsByPp: {
                    481: {
                        encrypted:
                            '/redir/GAkkM7lQwz62j9BQ6_qgZjo5r7eapfkKd5dp7FUbcNVt-42fig-1QurW2LOJIhOfaUhkYP-0CTyEXoM7OE5KQhw4BUFKx1sT1y51SAcGOShv2BBm9SdERmrUPusShqrHtL4WGCoHI9xvSA0fquXW6TfWRmhQkNnSvoyDcU4DR1u4_cVkaY_IzWE89PPVemOJuIGEbe1a3Mbb61ZXKEv-ZgA9btcbvMY2jbVPIyQTKOd8oCAXQws2p0JhkkXwWcDZM_yghdxrvi4MzZek7YBqYbTe7jNSWgP2-OSofFjxJ84CnjpmZfunf4npMIbTsI_ElecWk9NOaUQYvKNkmBGZ5-QKBWBSR59KlyJxtANnjvua0teAbx_Y1DHHS662FyKRS-EZIjcwImNLhyh9VDxOXTzoJPGONcoNY_U5UUER3JBIAknJ4GHTrlMAyRlyoMkutRH3hQQyWx3tYUbirSzU5io_RmJd4pN8d7nEQf4rnzzLXtWlTpmMDL_zFTU8Q4YklfBsid1r_0WCpS2POTGjc_lenEGkHN_KSFwoMI2Gblkz3L2oONXj0MM74yr_LYzCR4ICZi4aHPdKvc-hXHZX_K-7Bw5fnmuTOaiIzx1AWi3dwXzbB-qBmN1aCam1c4HQmG8a_uYFJvtuPj1CO-sg_ZpNmvZp3HUd01X5geEu1zAhscMKjVOZFlpwyD46K6Exvfs3Xy_ImNVtWZb28Qrkw1sSJNbjZUj6pGD0Z7isxD1cMSf3wTOFUDpsM8gjyyXqqEMmCmPq_4L9gE1fyMyvlta0d-nibGqHInKRAdZPwsirbYSixcDM06E4vsCo4zEsJmnweL37_7T_85HKVm2c2E_g6NGd0b2DGa0CdX32K1x1vBQWyc9o2DdXMY5AFJ3VhjQuJXNwxt8,?data=QVyKqSPyGQwNvdoowNEPjbv5NDGkDw-huUpGHotaEKMse75hWj8mPDTfaRky_s55A58suaRDQtxvFXLOyDXmUnzhvnPV4kzWHz1cxBes3r5apEtmEVn9x-eSJNqyD0rcmc736RnsoUVSrCU5bhONhrlEtG4kdK10TiGb5wp4lgjtUPM5HDpF0iJLZQxUgIVO5G1o_US5drSFw3IVL7MUM0-hizF1gsA9KIkrP-bZ7UhNdlCeUsG3t_7fuKPoKuYWNrWrYlbnQxLGGNImvrl7919Gk4whipSivZroJO5YoJNsVsPcTsOlD-I1COD5Z6HbhOFM28JElJXC7xDu6LDoNY7THkr0UbGJFyEWf1BET4-M5g1gOuFytsjpieaLdBaIPqU_nQWOyEVhqp4bzl0qubFmNvOnE1zp36qVcM5NcBYBJ9bXtod5uy323jtHx4zeyXDu5lAr0gOCAMdz4cUJjyUbxV-aT38ktXB9CwbYwoK2hlOm-6Q8bzM42eGX6SzK&b64e=1&sign=b802b40d4eab98dd24ea401466152ac7&keyno=1',
                        geo:
                            '/redir/338FT8NBgRtZwUfhCY-hTXHnsd-hU4JdbUwsJ3YY-0x1CleVwyxHUuggXdEDHJN87oZsCqPuZ8fTCxYZo7ekU52O3D4dERkJ0DkkZOunLSBH_5qnxJL29yARkgpyqrGlC_IPAuFrYOeW6flj0UVpNWT3VcV_xA2Ej4q3WLwpz9LGV4l5j4IpaLhaQqqyZs1sJ2OlJVUEDwyO4HbMl8LqcUbvYLMW7Akq5IiMFMtEvBvuAS3k-6pGg6tPs2NANi9-plqc2uT8dpjUA6B15AWwHyh4FKpVmP8VcQfr4OqF6QD0OdYZIwgc9dA7Jj6_WTOyn2CjBxmXanO3Orfef7rwqN-K0S6K5YJ3yigrJsQC8WA1WgSbF1ApG2IdVnkxIjDK7JIDzgGJWp6zoXi6t-p7gygwCCgOxdsNJDMiajhU26QPnMtoTUs36LaaYGjxxbFMJ_ohcEHPtoZIbVPsGlZndAFp8SDhbQ1FDJyE5H0ZIxOa5GrgynA8YhwoW9wJpzni9MrqwJv5KFjfQxLNTue81zVO9Ht3bZ2mdAVMNaNhzyvAk1A1PDD6TL4RnT9Zc7FoH-NNyXcVrVpPeolcq0G6Ccvvg5nZB4UgQBQ9pQn0YTWtJRZjbr-x3AspKRiHqTkiJ2eODY_UFW4U5mp8Gs69rtcky8CagZKlq13hFr8bbWcm-DzUcfzNTznVUgNAgIPt0Eor0NKjYOzByvhyA01AWX1uTVomPAytBkCyYTsts9PpoNtfRdO2LNBHowfZkMMnS6nJqG8slhr5J53Z9g1wlAwA3XxNZP7yv2N_rt6jVLCJ-mu3XBTp7EouV5A66KPtTDw3X9yZOFI1CnoE6m2T5nMKA6SLqI2LoGAr8Meuqdg6PAzFfwNYdtNSWho4WNPF?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bvDfds-sUcJ0iKSu_dtt81pla87zQCvgVapB6PIJlG2tCoO-fnLfq8lr-J5LUtT0RsttmcReARfjBM-Bmoh8HU,&b64e=1&sign=6ebbbfc029166385e9c8bcb1e58b10ed&keyno=1',
                        pickupGeo:
                            '/redir/338FT8NBgRtZwUfhCY-hTXHnsd-hU4JdbUwsJ3YY-0x1CleVwyxHUuggXdEDHJN87oZsCqPuZ8fTCxYZo7ekU52O3D4dERkJ0DkkZOunLSBH_5qnxJL29yARkgpyqrGlC_IPAuFrYOeW6flj0UVpNWT3VcV_xA2Ej4q3WLwpz9IsxMeO7W5FDaN7wIwoGLbMQB7VZk6jUvXbBh71_zLHqSC70Dj01dsYGYW6RQ84tVKSXTYlQil_XSH8P0BRj_o864PSbL9LIN9Pl1BHw9g6kPi2vpQTUx7P1mOZ7YEchP-tmaVgl37io4nmhyrhfITA-Bbx1bdRwP4gG4HVnlXkVOV_l38YlHA2P52D4G-Z5ZrK0Kupb1iD2wvMlzYKTUlEscejibE-34q-xeKEEm98dBjFQDTZuDCTjjQiGqsLvcdoQ1S9ZRSRwdF1iu-m22wXlX-Vle4KvixQ5Q0R5aRgXvzlHNC6kXSAv_9-bh9B66HCCXpKi4yGS19WL8aniCgBBJ2BB7LE3l1GJKM_6aqySgoaG5UcO7kyPiuCqU-LrfpUZ8hrgVgUl4toYJpTBoAcIg5Sr69JDLC8eiLj244NhRaPzojKSNNvMLMr6vuUbUGdv1Bf6YzRmaqLnnXhWeMNrf6aW37qtoiP0shzfKCczzxZ5FSNAfisPfGL31dn8lYksQgSfggaLUy1s5jR3UDXrpCbY4hadnsB2IwKz0QRRKYWSTP_H2NOSBKjwpuXv2waY2kN32S5Y18l7_J5HIP1mCkEEUjxcPNDO0mBNCB0O-YVPVlbpiTpJDeddi_NySXyb4l7LehySgT7hXJWr9qY_pCtvC-WIxZBE5IaHcwqOlZTlc0_PE0-JbcNo6hnKUG-FbO5_7xzf9a6-juO0M0Z?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bvDfds-sUcJ5VvvbvvUrhojXUySvgxksYCCAOx4yDB091gW7KiISaC19s4gW8zXpgWbb-fnIW4YYKTy4dj18UpGw7A0r0_2Wrj1JzHz_8X4GnBf2G1DEz4,&b64e=1&sign=4aaf7b360645ca1253213c76e6c7b32d&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRtZwUfhCY-hTXHnsd-hU4JdbUwsJ3YY-0x1CleVwyxHUuggXdEDHJN87oZsCqPuZ8fTCxYZo7ekU52O3D4dERkJ0DkkZOunLSBH_5qnxJL29yARkgpyqrGlC_IPAuFrYOeW6flj0UVpNWT3VcV_xA2Ej4q3WLwpz9JTkab7-Wha2NMMJwqYi4TmuDstZpfnINbtSQgLTLjR1Iel7JiVAGprqJ6rizsdPxhBTnEN2dG1EPlBvX-qIHx6dy1LEbsfY5cDtDHiOWZEIxAffhIwVGxYb00n-Fr1bRGsRsP8SJo_ZG45vjd1KmWuqfVpNpglrSYIkPRkj9UezpUDAhHd8klEeAJmJJP1WNgQWOY2iInXYtBLLKcuqCgtm38eLQl-daeixGnxb5iQ_ZPNWb9aILmjBwl4swAwGRpStJhDkTuMCymJZE3K-C6yHEJ__zgqe0ktptNYAPRDqk4xXb9cDWEKCX-nYjXZ1cYvHoDqfJq_oZE500m6_hdygh1KebyAhOeb96idNdg2bvwKlTp_XI2j_hwRxggfzK0mmPp4PUN7iqou35YqP7jojL6WUvR3fGZk1NNwwU4xctaaXDcQNggtVSp-KJ3bJhe6fnr7bHViLB8bslGxLFWc9k9nDELQm0pCjpoLT9QlgzcfnOtBbBQJ-1amsrjVqTpkuSq5MlZqQtTZ9oTrIN47nKUU7BtgBCwv1wwu4jaTu1DpTXsL6IIJs6aXW3vGkzMCa2B_UY6IsFaBhooQoLpKHH-wWb6qzHX8JDC2a98G7R6XCV6yeFvlgqp6jaKdjE_SDbIJIfe2s4m5uk7QjsI8VT9IC5qtwUYrt2V3Qr5Tlw9qwTrYgq8-HGmZrQFpVbxcbaP7N6YbndUfS2y_UtdW?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bvDfds-sUcJ5VvvbvvUrhojXUySvgxksYZlt_cYH9e6FFZhI2kR1t7ze9V_6ECoiHnWCMwA1u_1jwCQz1KyYPrRRxgy9dNE6EdI61RKgnWAyG1oE0t1neI,&b64e=1&sign=b7e68099195afb368a8c3470c7a54291&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRtZwUfhCY-hTXHnsd-hU4JdbUwsJ3YY-0x1CleVwyxHUuggXdEDHJN87oZsCqPuZ8fTCxYZo7ekU52O3D4dERkJ0DkkZOunLSBH_5qnxJL29yARkgpyqrGlC_IPAuFrYOeW6flj0UVpNWT3VcV_xA2Ej4q3WLwpz9KHSYjWCEnDvGRjJKuDZe8DY8xvKLPo8U-gOc_GX5S5uh_nM5nKKY5x81-kQPq6Hri1_GF6Owl6xr32XOupehPvHJjr-5HosPZiB8zxCWHZtZrMbIw3hfG8EhU1b89OWPlj1vtSoodDTRItjq1vMGeWQaKWtfwzbMUPP1jDy_3vXhy6iYJ56kMNMsCJs7CeioL7aqurMw_SAm5CH-OW_kJrDESbeItiZ16TweOBLZxd4Av9SKl_hUKa14BTrS2G2TDFe5ymx6Ilh6okLoAwtfNKVAA192rqg-RSNjbUO7xzJfNChVL9pZfem2QMctBa0md0usVlK-QAf50n08ug30kLOLmC-B6YeJwJZLHK2SY2ptwxzGTpj-1ES6Kq3_BMtCmgnnuvylz7YBSu3FlSuU4g1GBy7y2w012SzGo2BVVR57wxh19jq9w4_Gc2f5dlaTGId8aZww5zFvouOvbgv_rVEgPmK8LVCIB4FAO1Q2HnxpPDL1z6sqGiNBktmLE1TsTL3t9_wAhkQWxSRZuP5YbztPlVAedfDprngTJMwf24XCfn3ufwaggxOfpY2EEtftWBAp6T_gNnxELv9rMPi3b5VHE5c0Dk11c798BauhR05wKQx6BxzC4RMi273UbumPDeH-vtO2di2Q5qYsv-IwqtthS8zujkGJLqmQhUJ9dSBxAZqucarOPuYWLCC1oNEZR5PMscdp6ct1Ge2XvM6N6M?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bvDfds-sUcJ5VvvbvvUrhojXUySvgxksb5Ncoz2Y5U9unQyXb5mDYmA6wAlPBYEZ3DAuZn7b8oGFedzz-4D2zxiLuhz-YRkgW0LLdNfTVQPV2OejbOxN3Q,&b64e=1&sign=247f8f2e3a70007949b9e2895f3ee40e&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz62j9BQ6_qgZjo5r7eapfkKd5dp7FUbcNVt-42fig-1QurW2LOJIhOfaUhkYP-0CTyEXoM7OE5KQhw4BUFKx1sT1y51SAcGOShv2BBm9SdERmrUPusShqrHtL4WGCoHI9xvSA0fquXW6TfWRmhQkNnSvoyDcU4DR1uBxfElLD3EoxVoIgBigZ3X8lE6bKxvsXHW4eQkaJ8Qv4dzv9FkiPz1bFh1eR-1OUsexkkgcC5PwWhmJ_9fqiyp0nubXr1WrFm7-MdCqkKGiTUZyqV2LTCEC21RI41bYj1KLKlRdwUZ8siqbeBmpx4E3pK3v7iiLCI__UUJI3yhv4peMdEWY8xfAPWs7YbV4ua_zpbHe9YPc0K1mzNSXLxKUiUGXTFmmJ-FDf6K9E0gm2J_1A3sjvDcTKbaEoomNFGQs3iD3EuRZa5RvWzrLGVEsNO4Dyksin1vyQBU12B_Cb96VGqyUIXXUI-drP8m7OFM5ZCp39ZFLqymXuiovKnhWkiTsmqagpYz3sJfMYOp6jI9-6D3OYr7Q-dW6kTuSzLoXJXE-7Kn4KqruXGjLzrgjxYcOU31vah_PY3egstidkQgsIsTMJhEH2OHH6sFl_rQZl82FPkmFcTPBT5wK2f66MxjIifAH9rA6W7wOKs3rfLWUZ2NWES43KUspETuF0LIyxidBmgMx2LOWFZrE6JrLHEszyg9iiTfG6zNsqBUmgvqeuj8Lmarn3MooI0uuWInsGLdMzNU6KlTVRSUv5r5hIlYb9Dkfox65N3bok_Jx2NOTIcwTSrSMWoCwq4w8cXQDrC3HnJQ5zyGWmHTBdNFSTpNUSCiHY-kllxDliGL3Nb1tkO9U6PhlHyfzogDCdcxRF0msz62UJGyfhrA6bgyO1HLL5mWx2U,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_1zRUUtXPDO4eX-s_yYbVh6A6d8q-rMnfJfWWLN6CLaPAiCkgiNcVV4kKNe3qMbHrd-vsKMjFko_ztiSvexzSeaVa6UpUCMD0qWSdYw7mpRD9cAJ0zY_xgdm6WtWIGX-LG1wn-EYAIRCR7-4oUedGssWM3Uc9EQQB9jR4JwZsj8w,,&b64e=1&sign=d1ab475d08a0827b16a4236754e5e5d8&keyno=1',
                        direct: 'https://topcomputer.ru/tovary/1585606/?r1=yandex&utm_source=market.yandex.ru',
                    },
                },
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 55319,
                        name: 'Корпуса',
                        slug: 'korpusa',
                        fullName: 'Компьютерные корпуса',
                        isLeaf: true,
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 900,
                            containerHeight: 900,
                            url: '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/orig',
                            width: 900,
                            height: 900,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/50x50',
                                width: 50,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/55x70',
                                width: 70,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/60x80',
                                width: 80,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/74x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/75x75',
                                width: 75,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/90x120',
                                width: 120,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/100x100',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/120x160',
                                width: 160,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/150x150',
                                width: 150,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/180x240',
                                width: 240,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/190x250',
                                width: 250,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/200x200',
                                width: 200,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/240x320',
                                width: 320,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/300x300',
                                width: 300,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/300x400',
                                width: 400,
                                height: 400,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 600,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/600x600',
                                width: 600,
                                height: 600,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 800,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1582349/market_XmCyIlVUy7Gd-wZdLLMIUw/600x800',
                                width: 800,
                                height: 800,
                            },
                        ],
                        signatures: [],
                    },
                ],
                meta: {},
                marketSkuCreator: 'market',
                model: {
                    id: 677868159,
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
                        value: '180',
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
                                value: '180',
                                isDeliveryIncluded: false,
                                isPickupIncluded: false,
                            },
                            dayFrom: 1,
                            dayTo: 3,
                            orderBefore: '16',
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
                            dayFrom: 1,
                            dayTo: 3,
                            orderBefore: 16,
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
                    ],
                    deliveryPartnerTypes: ['SHOP'],
                },
                shop: {
                    entity: 'shop',
                    id: 5205,
                    name: 'TopComputer.RU',
                    slug: 'topcomputer-ru',
                    gradesCount: 36879,
                    overallGradesCount: 36879,
                    qualityRating: 5,
                    isGlobal: false,
                    isCpaPrior: false,
                    isCpaPartner: true,
                    isNewRating: true,
                    newGradesCount: 36879,
                    newQualityRating: 4.480083516,
                    newQualityRating3M: 4.502066116,
                    ratingToShow: 4.502066116,
                    ratingType: 3,
                    newGradesCount3M: 2420,
                    status: 'actual',
                    cutoff: '',
                    outletsCount: 6,
                    storesCount: 0,
                    pickupStoresCount: 6,
                    depotStoresCount: 6,
                    postomatStoresCount: 0,
                    bookNowStoresCount: 0,
                    subsidies: false,
                    logo: {
                        entity: 'picture',
                        width: 112,
                        height: 14,
                        url:
                            '//avatars.mds.yandex.net/get-market-shop-logo/1598257/2a00000168617c2e14cf48477fcd6a0307b8/small',
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
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1598257/2a00000168617c2e14cf48477fcd6a0307b8/small',
                                    },
                                    {
                                        entity: 'density',
                                        id: '2',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1598257/2a00000168617c2e14cf48477fcd6a0307b8/orig',
                                    },
                                ],
                            },
                        ],
                    },
                    domainUrl: 'topcomputer.ru',
                    feed: {
                        id: '5615',
                        offerId: '1585606',
                        categoryId: '253',
                    },
                    createdAt: '2007-08-21T15:57:23',
                    mainCreatedAt: '2007-08-21T15:57:23',
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
                wareId: 'aD1LnBWBHTtsVYdf9x7k0Q',
                offerColor: 'white',
                isFreeOffer: false,
                classifierMagicId: '3731534ad04d7c37da204c1e292f2d76',
                prices: {
                    currency: 'RUR',
                    value: '3050',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                    rawValue: '3050',
                },
                manufacturer: {
                    entity: 'manufacturer',
                    warranty: true,
                    code: 'CASMW-F1',
                },
                seller: {
                    comment: 'Выгодные условия быстрой доставки',
                    price: '3050',
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
                    id: '217389441',
                    name: 'TopComputer.RU (м. Тимирязевская)',
                    purpose: ['pickup'],
                    daily: true,
                    'around-the-clock': false,
                    gpsCoord: {
                        longitude: '37.576192',
                        latitude: '55.812887',
                    },
                    type: 'pickup',
                    paymentMethods: ['CASH_ON_DELIVERY'],
                    serviceId: 99,
                    serviceName: 'Собственная служба',
                    isMarketBranded: false,
                    isMegaPoint: false,
                    email: '',
                    shop: {
                        id: 5205,
                    },
                    address: {
                        fullAddress: 'Москва, Дмитровское шоссе, д. 5, корп. 1',
                        country: '',
                        region: '',
                        locality: 'Москва',
                        street: 'Дмитровское шоссе',
                        km: '',
                        building: '5',
                        block: '1',
                        wing: '',
                        estate: '',
                        entrance: '',
                        floor: '',
                        room: '',
                        office_number: '',
                        note:
                            'м. Тимирязевская\r\n1-й вагон из центра\r\nИз стеклянных дверей- налево \r\nДо конца перехода и налево\r\nДалее вдоль Дмитровского шоссе , по прямой , проходим здание Центр обучения 1С, далее дом 7/1 , по окончании дома, на пересечении улицы Всеволода Вишневского,  переходим пешеходнвй переход и поворачиваем направо , через 50 м етров, слева ворота во двор, заходим во двор , сразу при входе , с правой стороны железная дверь , под козырьком\r\nВы на месте',
                    },
                    telephones: [
                        {
                            entity: 'telephone',
                            countryCode: '7',
                            cityCode: '499',
                            telephoneNumber: '3220317',
                            extensionNumber: '11103',
                        },
                    ],
                    workingTime: [
                        {
                            daysFrom: '1',
                            daysTo: '1',
                            hoursFrom: '09:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '2',
                            daysTo: '2',
                            hoursFrom: '09:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '3',
                            daysTo: '3',
                            hoursFrom: '09:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '4',
                            daysTo: '4',
                            hoursFrom: '09:00',
                            hoursTo: '21:00',
                        },
                        {
                            daysFrom: '5',
                            daysTo: '5',
                            hoursFrom: '09:00',
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
                    max: '3050',
                    initialMax: '3050',
                    initialMin: '3050',
                    min: '3050',
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
                    value: 'TopComputer.RU',
                    id: '5205',
                },
            ],
            valuesGroups: [],
            meta: {},
        },
    ],
    intents: [
        {
            defaultOrder: 1,
            ownCount: 0,
            relevance: -0.678534,
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
                    defaultOrder: 2,
                    ownCount: 0,
                    relevance: -0.759787,
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
                            defaultOrder: 0,
                            ownCount: 1,
                            relevance: -0.554311,
                            category: {
                                name: 'Корпуса',
                                slug: 'korpusa',
                                uniqName: 'Компьютерные корпуса',
                                hid: 91028,
                                nid: 55319,
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
