/* eslint-disable max-len */
import { REPORT_DEV_HOST, REPORT_DEV_PORT, REPORT_DEV_PATH } from '../../../../../../src/env';

const HOST = `${REPORT_DEV_HOST}:${REPORT_DEV_PORT}`;

const ROUTE = new RegExp(`/${REPORT_DEV_PATH}`);

const RESPONSE = {
    search: {
        total: 2,
        totalOffers: 1,
        totalFreeOffers: 0,
        totalOffersBeforeFilters: 1,
        totalModels: 1,
        totalPassedAllGlFilters: 2,
        adult: false,
        view: 'list',
        salesDetected: false,
        maxDiscountPercent: 0,
        shops: 1,
        totalShopsBeforeFilters: 1,
        cpaCount: 0,
        isParametricSearch: false,
        category: {
            cpaType: 'cpc_and_cpa',
        },
        isDeliveryIncluded: false,
        isPickupIncluded: false,
        results: [
            {
                showUid: '16164085614077774375000001',
                entity: 'offer',
                trace: {
                    factors: {
                        CATEG_CLICKS: 392,
                        SHOP_CTR: 0.002863149391,
                        NUMBER_OFFERS: 3,
                    },
                    fullFormulaInfo: [
                        {
                            tag: 'Default',
                            name: 'Sovetnik_loss_562159_045_x_Click_438131',
                            value: '0.379687',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 17840014,
                    name: 'Cobalt',
                    slug: 'cobalt',
                    filter: '7893318:17840014',
                },
                titles: {
                    raw: 'Джанни Г. "Corpus Monstrum"',
                    highlighted: [
                        {
                            value: 'Джанни Г. "Corpus Monstrum"',
                        },
                    ],
                },
                slug: 'dzhanni-g-corpus-monstrum',
                description: '',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 90829,
                        nid: 20599010,
                        name: 'Комиксы и манга',
                        slug: 'komiksy-i-manga',
                        fullName: 'Комиксы и манга',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                warnings: {
                    common: [
                        {
                            age: 16,
                            type: 'age_16',
                            value: {
                                full: 'Возрастное ограничение',
                                short: 'Возрастное ограничение',
                            },
                        },
                    ],
                },
                age: '16',
                cpc:
                    'EzU3zIjbeCB0K2fXkhiIVhxq5j0HF2nCTJFjaDD0Fdr6iYfJfGuRCbQoSyafFXQEIlnqKhio48PWfANpFinFN2aGPoyYaGcySBtJRdynbRl5B7Xwre5u_xofMtsKKdeM1PBLVHNJhHMWGRzZ8OzqMA1cmL0f3Gvi',
                urls: {
                    encrypted:
                        '/redir/GAkkM7lQwz62j9BQ6_qgZpD52Nhro5eXrDbW7UBUAplQE3-HztsnBqK1VN0cu7cyjb3MdoW7m7UbofT45HWAzWNRnUaIY82QgRODLumjVTlu7YjhrZzuCD8koQ6DwvrvNusme2oYW93S4ESJ48gNGnj096F--pNyz7Ihn_t2kJIi5C2ZxXURhrNf_vTcS-uUky9FgSM7B0_HfwzOa9cOL51zmYRw1scnbbbS-JZwk_GhSaxSPyi_bch47Capo1BJ64nd88-MYWQTccaMvyr02MlCyYmbYsygw_IzFsRiH9EylZ4fheE4phQ9hcuMxa24SwijueN52U3UhYxel9a47LnMgabxTID0d0CYv5dq2onhSK2sDFJFUue7466hrbA2vldijBGPL4R6uAgLYWEB5y3c3ZQ93wPPpzlk-iMPWRgb-TdlIZIGNfkxosWMxZBI1i1b5DuveiC4ynzECK-hRdUh2bsY6RszdqvFnOJvaMG0lzjF06vGG_MgYusBeMEJ6zA1CRUvVCgorpYmPPHYxzKGqeJHM1jmjb1lsmrCV3stBc4JJlInss9vJZQGlhUy2CGJ1tscqVYpNifnMFVU3NXUstwnrq0-Ty6NL5myIvpI2Tlp1OW-jCCYYnOYk06QFulV2-PWC2IrO92GW6NWuvX3asFviAfsY-f-X35YZRHLnCKOBb49mEjAdiw8O4FZeAp5FmVdzQHB29vL5ZeX9ksi_z1A0fxoPsGwF7sXGvxiX-jjZQoOL52kIXnuGv6r15KhzYmPAqwh9ES7QYfrNvli8PCWC6YV_WrYhE0cJ42m9LADZJSm-kT2hVQIBqStk6C23yqMDy6BOiGjHdcDdvXVP_3yFVQFWK-xsJgVMFoN1NEKs0a1OTYX30Ptp6QTlOwx-fL3RDS-516EHlVvzuFoTHpjyOuDX9bf_ZR67sziJb6vqaUgtv2uLAqDCsXw2Z6a2LXKXfUfKsCPbxly7fv8zeCabMz-b2p-dt2SxyXr9GbadNfnSw,,?data=QVyKqSPyGQwNvdoowNEPjR0m4NEYLutGDAnRnUrQe1Psq2dw2fiu1GpNtwBlXYByuIQ8nDZJ94GEole814vloR4SMj4UIZ8Zmw2pGo2LY6wZwTsTUZUy_rS3ishW9T5vqS20kmj5Cen-AbHXJa03uaPicMPtJvk170bgFvcKbApGuenwF6Jg8MDKKyKIAu8D16aRsN42-dwuew0fbrxXA-Hgno9oBuR5KYsfkMnP80btXZKLUDTmGAaM7Z2eCeVRJdUQAmKsEwWlvoMbmRvtWdhaEA6pMe-YjEpl0FL9diLTXZ_kX_GcIqkSy7z8bvx-JVFSIe2ez000PmUtUbe4ev0Y6UcZsLrzSpBQ65K0yUL7DlDCy1uc0UIz6JUR8N08guyFRxc7x3R7jQCX6KAu3Aiu6sCW70f_KFCxi7LXl4SfwkZdSnL79mzj1cjGwAHDo6WWdnsY4uP21NauX1sVhl3VH5UM2bo8LAlKGiZ2PzdB4qqcpG7YKmavA_OFoGPu6GNyN2dUQzMkvxv-H1bIAei7nyicJ53IIkO5wF1PSDo,&b64e=1&sign=0304fd9520ef7755f843e76a3cbe2349&keyno=1',
                    geo:
                        '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkdxv-ZiGhHx1LSTvygfiORml26D0Y9zhEUKk-ZrBOu_z45pN9zR99BGxGB58clHqiEEgxMs0MArM50_F-eip5wwANs2azq814_Hns62t16YMLjKNcz97NQdlHqLK9IsTqETBTP7_jKvSSn7UT6dyEQOGw1sR_BG74mXqY7fvewfRpcssK9R0AYPZnwBiJoWw4TEUAi_FVvHEejRBES0v7srbu5JiaU_PRZMm7Q85y21ZrYhhbg588q6Wse6IUXUhLW1YZr2WYC6XopTvbr2SJMs6kM386Go_E-LljXCbyyD6fxRvnEgTHuPCCohoIMAjegGGNerDoihDlnz2ykbtTBXy2Uid5ZAbRSP4LBIWjBWeiMx-EwSAYagOsqLoo1xQZDe_CFol0eh-Hf3dYWPa_-FoFO5ibpBG-QXmxN1x-2G_MyynxLKxvPHXy79NjvnzRXOFi9yEccZMXouFSnbEU3C62-hQig1984PzncGC-h57r4U-zinXEfxxFDeG8fELIUr9ISH3RWMTL0mjNw1ViTZNjk33gnPOuW21H-O2Z_cbkDgv9IYFDT3qYYsa-tdJphFKreIuw_Pq5SIfratzkEn68ODYeoMeRX5E_K86jgjVD3_KWF6ypoTbSGWphxhEl1HIpq8JGiwsUwiinzhc-DWStr-yDljA8KGzjJ7h7wZNFcCRQ4Dx91UH12JE5NdMRPx_tkLbVoBikiY828eesLmGoEnNPd75jQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGxwYx9vKne1S___j1xPStlgiDMttRDwpivZdK72ECnrWP0Z05pUXshlqMLW7Br6QhM,&b64e=1&sign=2e0461be5610633a33c4d588b3d8d752&keyno=1',
                    pickupGeo:
                        '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkQwNGuO8AXRKYZn0oqehQ7k0A7kMzMRVe73V43iKdZ6wzvmbiR8oiaKCaITo6C8SW7ObYRUYzNOq9pDZ5HVrOFIIHvNIaILP6BCUmbg10NHj-9tiOsC7Jn6fffQAE7BFeP707XXkDHLXFQl61S_cLaIcsbzTY6HLckk9wtd6IL94IT1LEuEoniZKcYreezL5tdfyBfMxjIfIa3dZCwnP3edgcbCAUl37cinznVieEhcfoVjKuzbEc1HGehI2-2UIxvr6rutyR3s-bvAD_a1QIoO1SJHR8aQ8f4XHReQQGbJNUy0nSlNepNpr9TdTOFjK-kNoCsLK4u5iIkyGppqLgABs6vBBv_7mBfUxk3rH5MCmXWlNNskh7w0ZDCZxbcBSdjdhPLcJtwrQSnzYf6ujfgXBB2iK80VR22IUyNGaESCfP3NtgLSK48aaIlDd3A5xfdr6Ku_iifgWiBTxeSCwH0GXBj5ZJlpOR_I0WnTOSuLptOg_ruJnV1ZhMQfv4UN8EQ_Usgemi5l3KCVgNG8p4UKbcwUA_OFazqY9Le22B6eCqaZgjAeSrfIdkCUnfm_baiLO1eMo27lvWnULtfSEULkydrjRiOLtBjIlxPlMoClVLT-9-Bk7uIT2_tzo7OeUwCwQvwfSRcVAmYdXjk7ZzhE42Omz4exWKe_Ka6KVGzGwLC4B0jqPhPPBNNMauQDXtWjV9-vQC5DRoQDjbYZ99icJy8OLvzcLJw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGycTaM9i_q3Y4gDzd346ya_dT5ST_Oih4knXaqC0w93AXidMfw7eVAlaHESByweHbIbgHT05GAlSkchkkeXMCzocV1YQ870mBA,&b64e=1&sign=ff724c35b4bc7f80a6baa1acc1962035&keyno=1',
                    storeGeo:
                        '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkYb7gKMhx68B1bNoAn5TElN_lx4CIX0qnewat_KnCLMzUBkZvxG1gq3Q-YtnIGx2KJGGsPI0gXwPWANU2kTqr0xJF8D3aV4GVLJgL8HG4rpBMqBOvD4Nm1XuzbL_yiUvCw1ymHBq_iJx0w10Yf6lIZ6d93U6NRgxpVf_r3f0AraEz1XEarQyK6yofiSKK4sMQ6ssMGml9Y2YG-re_hwjivjHuQggKs1YpSWAAJl3X1m1z6oifrXVXiJOqBGFHIfCrq7xwbJQwluYdSr3Hw_ozcXbiC7b6hVQlJxYYlQCp7u4WYv1D_Wd1obRAiQNbt4rK6ybew-K40FgCALIXcGTW5n5E9qfpCPQ-vjm23gxylZRDUqkdawgAh6dFHZmMJeHKpY_PgvnD-wvbJN6KBjq9zOTMU4TaTnDH2u5LcwQY2lJJiG3elARd1ct7l6_3yW7Mpmzm7KvXgnOzSJOaeHWUuhT8M7rthUlYQQ6J0u0WrxBEPfcitXMUoGG0uvhonhvyNd6vdAXgbPuVRiOl2eYCimWEz44ZTqG8piyCFBvJlPAnem2oLTwBBLHGaW3cYNWIk238BpArf0XtA6U8_dfFYBKrGa-kOVnTMP6ve3yqu1z7kz2GZ4Ku6lJzGyIwUM8xMf44sJI-322nruu2K2jPrEjLmJ3K6ufstOq5kqGQEoUYOZ9IavMMrjLg-OURufWm1-RHiVrPMgZ2uhWtU7HnogRgjGN2I7wHg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGycTaM9i_q3Y1gGOVTpVr8vgSLGyGoJxVm4AntHySuyuBCB1y_8lO-IsioFKuhwce0tlkSKnOG3wBs8Ae9uHcYlYJpUD7tAQgs,&b64e=1&sign=e8f313855520a73bafbd4dc4494d8fb8&keyno=1',
                    postomatGeo:
                        '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkcpV6LhC0tukLFKo8Z_L-SOZbmt9naL6Bnj_2xEFET1tvS-H-YzSZdtoSiR0SrCjvSs8OzcYCIN81x4CdIT0ebQ6j6Eo9alZOXrqFFdM06naVAWMvyPc5xAz9wQN5IUmRjJYnvhAc2-aCxYE1NEMzO18kcCcnWAKMX6YEX-huwfBWWSRYaX_OWg4oNIl-rN7POGa94wAcmWJjW8yjwOByb55iTdJJIvbtBe6flgjZjeEyiAIrQuCFn1svx3ujJtuApcXo5BgDHJALtS8hEs_5o0eiZj8BR7lLOT9JOoY9Nac3bJU4xX0yaha-4_lWa-IOMF3vAkZTT5v18QQfYCo-mvD42Uk9JpNbBhAK8z6y3aIKvbHdyWHljMI6TOADuwe08_UtIIM8Wz40k1mbWyRECS13aOxNolXVzXomwgyYWZX1N5diyxACwLfXByuEYoCfKbER064rNWyZ1HrPoFiKYXnlegRlPUGSYmjfLIz8-Nepj4VJecRrmZSRMc7ohWjVr1yWEn4vNYdbZLlmdDY7-itdQRushmYLZdSHYudLvhYG1beL9Ok_FDc5nO9_zukV5nisj8EM-PIlC3dWN9FQry5m4Vvcw5JM0qevZ4PqrBTbdGbAmo1keV6qIlqLZWXpQMCJQ1I9HHkAZQWwYcVPMC7gs7GD8Wqacsa1Oe4Uw6XbihUuR48oG-GrGogRtNFxQFxLx9B1NqZygTKegt-VRwrvrbcmF_b7A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGycTaM9i_q3Y-pMaLuW01Pp6UZLw0VvySvaae6DF90AU5N5n-sS3GDl5oUuSicebhtHGyFg9EHqsTKrxL2aPqObcyZtc9-_AQW_Clti3sRJRQ,,&b64e=1&sign=15e7900ed2d3645742090e72de2501eb&keyno=1',
                    callPhone:
                        '/redir/GAkkM7lQwz62j9BQ6_qgZpD52Nhro5eXrDbW7UBUAplQE3-HztsnBqK1VN0cu7cyjb3MdoW7m7UbofT45HWAzWNRnUaIY82QgRODLumjVTlu7YjhrZzuCD8koQ6DwvrvNusme2oYW93S4ESJ48gNGnj096F--pNyz7Ihn_t2kJIi5C2ZxXURhrNf_vTcS-uUky9FgSM7B0_HfwzOa9cOL51zmYRw1scnbbbS-JZwk_GhSaxSPyi_bch47Capo1BJ64nd88-MYWQTccaMvyr02OSbPjqgynIEm34V_qzDNHp_59iwp1UhDRkE02KNtlo0lAisa0Mf0YGG19xJ0RQJ2K-epYAhpl6zxy9mzYRxH2kC1y1ttFbVOGLWtAIXL9-tN73N_29TRfHK2xPQ1xaHHuYClhKSuKrgsRPnE9wW5SZUoKfrWaqUSKBGTuANpidMJ8b8G2QyqjUzpdsyPkp_HILuu7zmv67OzooN_B3_x31tdljl5n15b-ohy0VrzwNRfyBFuFwwBaJyFEqMjVXfX5eCuMTbeoxmLiYiX6Xdh8uBTVFKfpyMlMbVFyKim2BvKBUZ9fvHdPKZRY2njbWFV5qFAnDly2nkWcN5PZCsYoVlZejumRBJrI9eQ3Hy4iqCCmWz5zk0wmqbKJmiqg72AnX9Mv4uugoR9IYl3ur7BYHzD2MZn7hGmgk6sLRdQRJD-rxACGWkH6PuNGZuMgt1Wr4m7LKXdsPW9GX5T_fPTbLVVdd5DjkJXK9_vp2RvNW4psds8lw7BKs5CsCKUtmAPTRyEga9oWN09luyVf1PuGIB-HW_XQhYKJtpw4uDzsVpLhNwnb4tiHdPdm3phJsHowQtZ5nCgfVZAJ8fIiytzlsisexSarZ9gNMrfnwxTuoYZd8Uf8cKPaGw8SNYAz_vz-vIrV2_GPotsIpYQNaq5AIp7pqpabPpz5NYm8pIDSH2iDH9rX1hr1aiY_C55bkRaXJG-1nZZEtPV6nKhfa3QMGQVurSajHusw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_nvaauUitOq6z7ZkQLdYt7suAK1_Iuv8xDuidW9JEV5AyJqy10DqLBLzrRfIQZUPvHbsjxS_psmt-VwO-eoO1snFn4eoWEkbJzFr2cJxzvrbIY8aeCm9Bw7D2WWMmtr8nqNF1l1sd-LIR0hqK6k7BD32dptJbFMm9bN_McN1dXESDtQoxUXBJe&b64e=1&sign=e83567e2cfe19d3b4e9fa290dba521ad&keyno=1',
                    direct:
                        'https://www.chitai-gorod.ru/catalog/book/1209904/?utm_source=ymarket_MSK&utm_medium=cpc&utm_campaign=9646&utm_content=9703&utm_term=1209904',
                },
                urlsByPp: {
                    '490': {
                        encrypted:
                            '/redir/GAkkM7lQwz62j9BQ6_qgZpD52Nhro5eXrDbW7UBUAplQE3-HztsnBqK1VN0cu7cyjb3MdoW7m7UbofT45HWAzWNRnUaIY82QgRODLumjVTlu7YjhrZzuCD8koQ6DwvrvNusme2oYW93S4ESJ48gNGnj096F--pNyz7Ihn_t2kJIi5C2ZxXURhrNf_vTcS-uUky9FgSM7B0_HfwzOa9cOL51zmYRw1scnbbbS-JZwk_GhSaxSPyi_bch47Capo1BJ64nd88-MYWQTccaMvyr02MlCyYmbYsygw_IzFsRiH9EylZ4fheE4phQ9hcuMxa24SwijueN52U3UhYxel9a47LnMgabxTID0d0CYv5dq2onhSK2sDFJFUue7466hrbA2vldijBGPL4R6uAgLYWEB5y3c3ZQ93wPPpzlk-iMPWRgb-TdlIZIGNfkxosWMxZBI1i1b5DuveiC4ynzECK-hRdUh2bsY6RszdqvFnOJvaMG0lzjF06vGG_MgYusBeMEJ6zA1CRUvVCgorpYmPPHYxzKGqeJHM1jmjb1lsmrCV3stBc4JJlInss9vJZQGlhUy2CGJ1tscqVYpNifnMFVU3NXUstwnrq0-Ty6NL5myIvpI2Tlp1OW-jCCYYnOYk06QFulV2-PWC2IrO92GW6NWuvX3asFviAfsY-f-X35YZRHLnCKOBb49mEjAdiw8O4FZeAp5FmVdzQHB29vL5ZeX9ksi_z1A0fxoPsGwF7sXGvxiX-jjZQoOL52kIXnuGv6r15KhzYmPAqwh9ES7QYfrNvli8PCWC6YV_WrYhE0cJ42m9LADZJSm-kT2hVQIBqStk6C23yqMDy6BOiGjHdcDdvXVP_3yFVQFWK-xsJgVMFoN1NEKs0a1OTYX30Ptp6QTlOwx-fL3RDS-516EHlVvzuFoTHpjyOuDX9bf_ZR67sziJb6vqaUgtv2uLAqDCsXw2Z6a2LXKXfUfKsCPbxly7fv8zeCabMz-b2p-dt2SxyXr9GbadNfnSw,,?data=QVyKqSPyGQwNvdoowNEPjR0m4NEYLutGDAnRnUrQe1Psq2dw2fiu1GpNtwBlXYByuIQ8nDZJ94GEole814vloR4SMj4UIZ8Zmw2pGo2LY6wZwTsTUZUy_rS3ishW9T5vqS20kmj5Cen-AbHXJa03uaPicMPtJvk170bgFvcKbApGuenwF6Jg8MDKKyKIAu8D16aRsN42-dwuew0fbrxXA-Hgno9oBuR5KYsfkMnP80btXZKLUDTmGAaM7Z2eCeVRJdUQAmKsEwWlvoMbmRvtWdhaEA6pMe-YjEpl0FL9diLTXZ_kX_GcIqkSy7z8bvx-JVFSIe2ez000PmUtUbe4ev0Y6UcZsLrzSpBQ65K0yUL7DlDCy1uc0UIz6JUR8N08guyFRxc7x3R7jQCX6KAu3Aiu6sCW70f_KFCxi7LXl4SfwkZdSnL79mzj1cjGwAHDo6WWdnsY4uP21NauX1sVhl3VH5UM2bo8LAlKGiZ2PzdB4qqcpG7YKmavA_OFoGPu6GNyN2dUQzMkvxv-H1bIAei7nyicJ53IIkO5wF1PSDo,&b64e=1&sign=0304fd9520ef7755f843e76a3cbe2349&keyno=1',
                        geo:
                            '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkdxv-ZiGhHx1LSTvygfiORml26D0Y9zhEUKk-ZrBOu_z45pN9zR99BGxGB58clHqiEEgxMs0MArM50_F-eip5wwANs2azq814_Hns62t16YMLjKNcz97NQdlHqLK9IsTqETBTP7_jKvSSn7UT6dyEQOGw1sR_BG74mXqY7fvewfRpcssK9R0AYPZnwBiJoWw4TEUAi_FVvHEejRBES0v7srbu5JiaU_PRZMm7Q85y21ZrYhhbg588q6Wse6IUXUhLW1YZr2WYC6XopTvbr2SJMs6kM386Go_E-LljXCbyyD6fxRvnEgTHuPCCohoIMAjegGGNerDoihDlnz2ykbtTBXy2Uid5ZAbRSP4LBIWjBWeiMx-EwSAYagOsqLoo1xQZDe_CFol0eh-Hf3dYWPa_-FoFO5ibpBG-QXmxN1x-2G_MyynxLKxvPHXy79NjvnzRXOFi9yEccZMXouFSnbEU3C62-hQig1984PzncGC-h57r4U-zinXEfxxFDeG8fELIUr9ISH3RWMTL0mjNw1ViTZNjk33gnPOuW21H-O2Z_cbkDgv9IYFDT3qYYsa-tdJphFKreIuw_Pq5SIfratzkEn68ODYeoMeRX5E_K86jgjVD3_KWF6ypoTbSGWphxhEl1HIpq8JGiwsUwiinzhc-DWStr-yDljA8KGzjJ7h7wZNFcCRQ4Dx91UH12JE5NdMRPx_tkLbVoBikiY828eesLmGoEnNPd75jQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGxwYx9vKne1S___j1xPStlgiDMttRDwpivZdK72ECnrWP0Z05pUXshlqMLW7Br6QhM,&b64e=1&sign=2e0461be5610633a33c4d588b3d8d752&keyno=1',
                        pickupGeo:
                            '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkQwNGuO8AXRKYZn0oqehQ7k0A7kMzMRVe73V43iKdZ6wzvmbiR8oiaKCaITo6C8SW7ObYRUYzNOq9pDZ5HVrOFIIHvNIaILP6BCUmbg10NHj-9tiOsC7Jn6fffQAE7BFeP707XXkDHLXFQl61S_cLaIcsbzTY6HLckk9wtd6IL94IT1LEuEoniZKcYreezL5tdfyBfMxjIfIa3dZCwnP3edgcbCAUl37cinznVieEhcfoVjKuzbEc1HGehI2-2UIxvr6rutyR3s-bvAD_a1QIoO1SJHR8aQ8f4XHReQQGbJNUy0nSlNepNpr9TdTOFjK-kNoCsLK4u5iIkyGppqLgABs6vBBv_7mBfUxk3rH5MCmXWlNNskh7w0ZDCZxbcBSdjdhPLcJtwrQSnzYf6ujfgXBB2iK80VR22IUyNGaESCfP3NtgLSK48aaIlDd3A5xfdr6Ku_iifgWiBTxeSCwH0GXBj5ZJlpOR_I0WnTOSuLptOg_ruJnV1ZhMQfv4UN8EQ_Usgemi5l3KCVgNG8p4UKbcwUA_OFazqY9Le22B6eCqaZgjAeSrfIdkCUnfm_baiLO1eMo27lvWnULtfSEULkydrjRiOLtBjIlxPlMoClVLT-9-Bk7uIT2_tzo7OeUwCwQvwfSRcVAmYdXjk7ZzhE42Omz4exWKe_Ka6KVGzGwLC4B0jqPhPPBNNMauQDXtWjV9-vQC5DRoQDjbYZ99icJy8OLvzcLJw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGycTaM9i_q3Y4gDzd346ya_dT5ST_Oih4knXaqC0w93AXidMfw7eVAlaHESByweHbIbgHT05GAlSkchkkeXMCzocV1YQ870mBA,&b64e=1&sign=ff724c35b4bc7f80a6baa1acc1962035&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkYb7gKMhx68B1bNoAn5TElN_lx4CIX0qnewat_KnCLMzUBkZvxG1gq3Q-YtnIGx2KJGGsPI0gXwPWANU2kTqr0xJF8D3aV4GVLJgL8HG4rpBMqBOvD4Nm1XuzbL_yiUvCw1ymHBq_iJx0w10Yf6lIZ6d93U6NRgxpVf_r3f0AraEz1XEarQyK6yofiSKK4sMQ6ssMGml9Y2YG-re_hwjivjHuQggKs1YpSWAAJl3X1m1z6oifrXVXiJOqBGFHIfCrq7xwbJQwluYdSr3Hw_ozcXbiC7b6hVQlJxYYlQCp7u4WYv1D_Wd1obRAiQNbt4rK6ybew-K40FgCALIXcGTW5n5E9qfpCPQ-vjm23gxylZRDUqkdawgAh6dFHZmMJeHKpY_PgvnD-wvbJN6KBjq9zOTMU4TaTnDH2u5LcwQY2lJJiG3elARd1ct7l6_3yW7Mpmzm7KvXgnOzSJOaeHWUuhT8M7rthUlYQQ6J0u0WrxBEPfcitXMUoGG0uvhonhvyNd6vdAXgbPuVRiOl2eYCimWEz44ZTqG8piyCFBvJlPAnem2oLTwBBLHGaW3cYNWIk238BpArf0XtA6U8_dfFYBKrGa-kOVnTMP6ve3yqu1z7kz2GZ4Ku6lJzGyIwUM8xMf44sJI-322nruu2K2jPrEjLmJ3K6ufstOq5kqGQEoUYOZ9IavMMrjLg-OURufWm1-RHiVrPMgZ2uhWtU7HnogRgjGN2I7wHg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGycTaM9i_q3Y1gGOVTpVr8vgSLGyGoJxVm4AntHySuyuBCB1y_8lO-IsioFKuhwce0tlkSKnOG3wBs8Ae9uHcYlYJpUD7tAQgs,&b64e=1&sign=e8f313855520a73bafbd4dc4494d8fb8&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkcpV6LhC0tukLFKo8Z_L-SOZbmt9naL6Bnj_2xEFET1tvS-H-YzSZdtoSiR0SrCjvSs8OzcYCIN81x4CdIT0ebQ6j6Eo9alZOXrqFFdM06naVAWMvyPc5xAz9wQN5IUmRjJYnvhAc2-aCxYE1NEMzO18kcCcnWAKMX6YEX-huwfBWWSRYaX_OWg4oNIl-rN7POGa94wAcmWJjW8yjwOByb55iTdJJIvbtBe6flgjZjeEyiAIrQuCFn1svx3ujJtuApcXo5BgDHJALtS8hEs_5o0eiZj8BR7lLOT9JOoY9Nac3bJU4xX0yaha-4_lWa-IOMF3vAkZTT5v18QQfYCo-mvD42Uk9JpNbBhAK8z6y3aIKvbHdyWHljMI6TOADuwe08_UtIIM8Wz40k1mbWyRECS13aOxNolXVzXomwgyYWZX1N5diyxACwLfXByuEYoCfKbER064rNWyZ1HrPoFiKYXnlegRlPUGSYmjfLIz8-Nepj4VJecRrmZSRMc7ohWjVr1yWEn4vNYdbZLlmdDY7-itdQRushmYLZdSHYudLvhYG1beL9Ok_FDc5nO9_zukV5nisj8EM-PIlC3dWN9FQry5m4Vvcw5JM0qevZ4PqrBTbdGbAmo1keV6qIlqLZWXpQMCJQ1I9HHkAZQWwYcVPMC7gs7GD8Wqacsa1Oe4Uw6XbihUuR48oG-GrGogRtNFxQFxLx9B1NqZygTKegt-VRwrvrbcmF_b7A,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGycTaM9i_q3Y-pMaLuW01Pp6UZLw0VvySvaae6DF90AU5N5n-sS3GDl5oUuSicebhtHGyFg9EHqsTKrxL2aPqObcyZtc9-_AQW_Clti3sRJRQ,,&b64e=1&sign=15e7900ed2d3645742090e72de2501eb&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz62j9BQ6_qgZpD52Nhro5eXrDbW7UBUAplQE3-HztsnBqK1VN0cu7cyjb3MdoW7m7UbofT45HWAzWNRnUaIY82QgRODLumjVTlu7YjhrZzuCD8koQ6DwvrvNusme2oYW93S4ESJ48gNGnj096F--pNyz7Ihn_t2kJIi5C2ZxXURhrNf_vTcS-uUky9FgSM7B0_HfwzOa9cOL51zmYRw1scnbbbS-JZwk_GhSaxSPyi_bch47Capo1BJ64nd88-MYWQTccaMvyr02OSbPjqgynIEm34V_qzDNHp_59iwp1UhDRkE02KNtlo0lAisa0Mf0YGG19xJ0RQJ2K-epYAhpl6zxy9mzYRxH2kC1y1ttFbVOGLWtAIXL9-tN73N_29TRfHK2xPQ1xaHHuYClhKSuKrgsRPnE9wW5SZUoKfrWaqUSKBGTuANpidMJ8b8G2QyqjUzpdsyPkp_HILuu7zmv67OzooN_B3_x31tdljl5n15b-ohy0VrzwNRfyBFuFwwBaJyFEqMjVXfX5eCuMTbeoxmLiYiX6Xdh8uBTVFKfpyMlMbVFyKim2BvKBUZ9fvHdPKZRY2njbWFV5qFAnDly2nkWcN5PZCsYoVlZejumRBJrI9eQ3Hy4iqCCmWz5zk0wmqbKJmiqg72AnX9Mv4uugoR9IYl3ur7BYHzD2MZn7hGmgk6sLRdQRJD-rxACGWkH6PuNGZuMgt1Wr4m7LKXdsPW9GX5T_fPTbLVVdd5DjkJXK9_vp2RvNW4psds8lw7BKs5CsCKUtmAPTRyEga9oWN09luyVf1PuGIB-HW_XQhYKJtpw4uDzsVpLhNwnb4tiHdPdm3phJsHowQtZ5nCgfVZAJ8fIiytzlsisexSarZ9gNMrfnwxTuoYZd8Uf8cKPaGw8SNYAz_vz-vIrV2_GPotsIpYQNaq5AIp7pqpabPpz5NYm8pIDSH2iDH9rX1hr1aiY_C55bkRaXJG-1nZZEtPV6nKhfa3QMGQVurSajHusw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_nvaauUitOq6z7ZkQLdYt7suAK1_Iuv8xDuidW9JEV5AyJqy10DqLBLzrRfIQZUPvHbsjxS_psmt-VwO-eoO1snFn4eoWEkbJzFr2cJxzvrbIY8aeCm9Bw7D2WWMmtr8nqNF1l1sd-LIR0hqK6k7BD32dptJbFMm9bN_McN1dXESDtQoxUXBJe&b64e=1&sign=e83567e2cfe19d3b4e9fa290dba521ad&keyno=1',
                        direct:
                            'https://www.chitai-gorod.ru/catalog/book/1209904/?utm_source=ymarket_MSK&utm_medium=cpc&utm_campaign=9646&utm_content=9703&utm_term=1209904',
                    },
                    '491': {
                        encrypted:
                            '/redir/GAkkM7lQwz62j9BQ6_qgZpD52Nhro5eXrDbW7UBUAplQE3-HztsnBqK1VN0cu7cyjb3MdoW7m7UbofT45HWAzWNRnUaIY82QgRODLumjVTlu7YjhrZzuCD8koQ6DwvrvNusme2oYW93S4ESJ48gNGnj096F--pNyz7Ihn_t2kJIi5C2ZxXURhrNf_vTcS-uUky9FgSM7B0_HfwzOa9cOL51zmYRw1scnbbbS-JZwk_GhSaxSPyi_bch47Capo1BJ64nd88-MYWQTccaMvyr02MlCyYmbYsygw_IzFsRiH9EylZ4fheE4phQ9hcuMxa24SwijueN52U3UhYxel9a47AMirRZMBcXRex_AqIeufMupMNf1lIXDQRqxX9kcpYcWVpDi74SsYthuVLwkdLXuGzFz1-Vlc-vXJMVIBGArueVJb7AaSe1Tv37XNCg4B7hhfBg_HjfG0yRB-sCSA4k_hVNpb7ydzJK3kIMVKpOFlyhKoSwPMSEJiJ8L1G_-tm5XlA9o7xUA24zEW5dPXeY6TNTNinNizx0KLFW2vx_hgLiqAm5uZiLuiis49JknPpyGdahLp7PCPlx8uJmmm6fwP8KP-XOqZsarBsxgQApuFb14gW9YQUiVtwoLIo2alf_h-cLBQqHYUp20Brq1AJoWozDa05dBAJj2hqT1G6lJBsFOIliVB58xDRcCsmwZ22nKgi50TFaMC64676tJ814Lq7QAHnQmQyVBT_SvcRr_nfx3hMxfvymceS4JM7QoCMzj4GoiHIsPObPTn1YrnmOPgra9TMPIiypGf9ezjqBlqULaUeeCPmMdvtGAT8FSwUgVmh_g07i1BJXRqN3BzgaJY1ZdArq6Cdl3NekjBuOBCQpvOtmUJ_kFIbdlo-RvWV8wLyNvRaK9LgzV2B0zCoE4_Ns4Frxb8SF8ojaL8IpTEANecMpzINd7SntjaPb0nZGKeG-HQIT1of8jItC3Z1UcdPooHrFSLddDquKZxgJpa7tUJAVDnCSHbg,,?data=QVyKqSPyGQwNvdoowNEPjR0m4NEYLutGDAnRnUrQe1Psq2dw2fiu1GpNtwBlXYByuIQ8nDZJ94GEole814vloR4SMj4UIZ8Zmw2pGo2LY6wZwTsTUZUy_rS3ishW9T5vqS20kmj5Cen-AbHXJa03uaPicMPtJvk170bgFvcKbApGuenwF6Jg8MDKKyKIAu8D16aRsN42-dwuew0fbrxXA-Hgno9oBuR5KYsfkMnP80btXZKLUDTmGAaM7Z2eCeVRJdUQAmKsEwWlvoMbmRvtWdhaEA6pMe-YjEpl0FL9diLTXZ_kX_GcIqkSy7z8bvx-JVFSIe2ez000PmUtUbe4ev0Y6UcZsLrzSpBQ65K0yUL7DlDCy1uc0UIz6JUR8N08guyFRxc7x3R7jQCX6KAu3Aiu6sCW70f_KFCxi7LXl4SfwkZdSnL79mzj1cjGwAHDo6WWdnsY4uP21NauX1sVhl3VH5UM2bo8LAlKGiZ2PzdB4qqcpG7YKmavA_OFoGPu6GNyN2dUQzMkvxv-H1bIAei7nyicJ53IIkO5wF1PSDo,&b64e=1&sign=d9189a622511388e75b5b8a2dc30589b&keyno=1',
                        geo:
                            '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkdxv-ZiGhHx1LSTvygfiORml26D0Y9zhEUKk-ZrBOu_z45pN9zR99BFqS1cawJw0n-9thKnOPIub9TVDA9F3Dvpj6-njqKU5IEY__EK5Qov3uB0TH5m4yuRD1yp_FGuuNv3Psk2Hd6QEexH3nueA0XZWBilZvKX1dM_42sYZWRbLvuf8H_l3cj2kG8wOxAFWoF8puBHSRjz4z1MlbYzG9gxVpw2oV-J_JQWXE8bn22fpONWqY2y7eOIQ2ftW8OCseedMsvvx834rZ0a_ka5-eejssCSf0T1Bs4iSaNQnPx8zDEegwfVM5oKgONZ940qCTFsgys-yuLUI9kC4qI4Q65OgjGvirpaoaosSeaO-9UAFtOJU2SaFxVh-UMfTLHt1aPU5TodeH90zGiwEwuiPORr_ctEOuTTRZkmCYMc2GTLbgh-E9dzxRIFjr7yf9OC34rAQpKJTcELZGAiH7Gsx5jOq85KXHEeofc8po3Fzgh9yJUG-8V0rkm7zvY3TurDi4wNAF-24UEC6b8YARDwp2Yfjncav7sWZBfUgizt2ZKq4ljuebe_1ZlEV8hgKvhQ1XGlBJFCVYjtUmPpVh2N5ZMuUc1BZWn0XVTqLY5VgPvLXjKzisekyzMxGm2WhbJuoMEtBrzuUuMdlHK8k6Me-tT9I0U_rBfeq0dZmm4uG0ph0kAmDVUwHVNBmZ7Tog1CidBuMvjA9ebIFdciUJ9f1yxqfkKV3cA8gXA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGxwYx9vKne1S___j1xPStlgiDMttRDwpivZdK72ECnrWP0Z05pUXshlqMLW7Br6QhM,&b64e=1&sign=c90d5404be7f08ab1695741ee8621114&keyno=1',
                        pickupGeo:
                            '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkQwNGuO8AXRKYZn0oqehQ7k0A7kMzMRVe73V43iKdZ6wzvmbiR8oiaLfJQ0FmZFm6k_SJaWi8beQvsvIWb8MObhjtHI6itTk_Fv2xlnYzg2puPK2cfU0fo3YqOGO6fVrN-8cNvN0HkgJFiiRXx7AIWfMOLELX0QtwiipA1vtPoJ7c2-fTw4zIong6vjfEzGOWCJw8eiaa6ZkDUIffKZukBDl4amcx92zp437fq5AYAVN2teV6EKn2togM_kI06-hBowAokAQKawNWzIeQxWqf-oAuoC9K6r3HgqvTv-Woktx9BkVSvP1mhzskF04Mz_r92Ufc3AygGwdz6soLFyvBedHck5DBZtgO45A39796Wcyll3LN07xOBVgNLnOG8F-F_eB9x4M6Vq8mxxlULRr808Zws1bLaLzk2D_6SZAVClQZ1ARr5QSmXQGOaWD75wGBBPM27uUedYHiuayaq4fNgFqXaFfwwtlcZtOc7TYhX2_JKfgNxboVYx6eNIfvGiy0FBwKDLHZ3Td1NbbeC39W07XQb3oAeXHhJQOZSDDBuEE-ijoz1prowXr9ASBP2Z0FClqFi9ZY9jDwDoEjqq6Sk3ncBLLJ00CfKaeHhj4oyX1bZ8HH7a4GwP7IKetfnZKVvNIecKFTA4-lk7KgGvEoeF9kWxUgl5zAA_oE57b-N1RGjEYmPSdZyJB9Yw3KBVnGb_fJRyzcgcIq4dhv4c7vCsd8UGqFVEMdg,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGycTaM9i_q3Y4gDzd346ya_dT5ST_Oih4knXaqC0w93AXidMfw7eVAlaHESByweHbIbgHT05GAlSkchkkeXMCzocV1YQ870mBA,&b64e=1&sign=27cca87cc17dee3166a5b84f2394fbfd&keyno=1',
                        storeGeo:
                            '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkYb7gKMhx68B1bNoAn5TElN_lx4CIX0qnewat_KnCLMzUBkZvxG1gq0qq_VXD42IU7Zj0YUQF7uV8dtPZTO5LG9YEGlvYaYT3R3i1FOFpglgMQDL-Tg24OcKkEpaZOW0wT4D1ZHQ02axQes-wyziMcXGkl4uQXnxSFlyQYjKcCi25-ebV-4nrO33bXuQUSMLwjKRtug2kf9cVvEEA7xVdTCag3EO7o5a_4PBnmq4RDXvA3BaUGrwE4KMGV1hTwpamZLRAG9JFmGAQOI0VLYfUIERjLw8lNY_ycHsxrQb_z5wktgjQXdPgOpMlpGwKQz-c8NjNs0C6a6kvXJRUqj8ZDOIsSxqF0AzQF1XfqwPbculROncnKKvtDjXpoEKkPNvOhFQQ-Avoqn4H9-8PeTQOlrMiUN2cwvG9e66UZOvwovoCnx8AnPAJC91-kB9hwkZipZQHmAnV7F0V3_B9-cFfWHliORUPHCbQWhMi6mhIfxOwd8rIDKusYKg5jUlJphSeNO6pslS3_MFqozs8bH8OErW0lsRYVeKUMN9VaMqch6OpFr2DLanrI73APJy_mUYcspdDHDWfD7u3Z8gK7Ib6sPAc5gtvQ1zNypPkiUNCTlF4vAHhf0o9EUhT2w48jLSjGeBmTTWweW1HExeSDQiP7fgz3Wo-5NTcQB5Xe4w_kqiHXcyK48j_zjPIUANYySKOETloa8K_zTXKrD8ddTq0_u7Y_xhAfsyxw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGycTaM9i_q3Y1gGOVTpVr8vgSLGyGoJxVm4AntHySuyuBCB1y_8lO-IsioFKuhwce0tlkSKnOG3wBs8Ae9uHcYlYJpUD7tAQgs,&b64e=1&sign=916e7d2d187951761fd0f1ac00e684bb&keyno=1',
                        postomatGeo:
                            '/redir/338FT8NBgRvcZjcbtIHwKtW_GXmUSgQOHaljPLDXj3-lXD9dbgB1mzXE2CVTqo9GZ1V3ZRNlD4YBQMHgqerP9SBNUoyyt11k5NqBJAPltXBrJqq9zVBEyyNbXVIuj1yqYa2coH8eek8S6EIVpf-I4SKeXqjW1y1n75SO_7oyiK8xBDBWRxB76orozv-6_VvMDwxSyOf0_O5aMAkiMoSvuJAJjJ2UXpFKSlV2to7f-3cea8pqOE1porXjFqY07hvmUnDJHQo5aeqir4XJxHCmkcpV6LhC0tukLFKo8Z_L-SOZbmt9naL6Bnj_2xEFET1tvS-H-YzSZdu0rBPl4m9s2jI4bCZY5H6KCApdOiLUqofL5gwnQszt0kDyJ5Lkn9_4f-K2WLdOLsTQf1BpP5vg3AWFTiY0pnj3dcM3xSJ7HENDNKe4qiD7x8C-UfSng8pvehQO8m0DxlmFZZlDhfWYPkpzoZNImMKOpIYl0pE91fPT5KtrN72IgDlIHJOQK4fs4-svjg5qpDW3F1o0SxVnUNuCxnVHlD3qaRcncDf6q4hbVVEpdtRxUVL4AUpUH7BQU0oMkGTuT-zRZWxTKrJxQk6k8YytNKZTTaKcKT1_9x8zecxxMsv4sm96uFNaDtrf2tFKROCjjRM_MLv3pil4iZUrBsqxW39uTlBicW_zvGqxk4QGy8l526u6Dx3e4odkDi90Neqz_ixNDxPuTOlI5UGYR8-PkIiAy45zZexaCUPrK-UxYzb1YJopiNWLdChVp_vaT09dLEVsG-26oj1LKwYf-9yNkZ8xsRHmNJ2flL9v9I2ky2z4gE9-UF340e7XBvQPZb9fovckzjn2kGXt2wL_co__xx7fkWnAwX9iyRwXxiRIIQgcFjbRx0LJbby7Xm03h3AaA-hyyA_0ksj1Y3h4rW-Cv2cpMWGWglGfdmruEVwfRWs5hdbhI7FhGoPzovQp6RUGbBMHFeCzpYWCkRJzJv_1XfWq4xDCfEl-xYLDQL6Qlu5BqA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2fLVvE0TnXzmdrwYSd1vdGycTaM9i_q3Y-pMaLuW01Pp6UZLw0VvySvaae6DF90AU5N5n-sS3GDl5oUuSicebhtHGyFg9EHqsTKrxL2aPqObcyZtc9-_AQW_Clti3sRJRQ,,&b64e=1&sign=4ab084b9672714a1d2dbc26237123e3d&keyno=1',
                        callPhone:
                            '/redir/GAkkM7lQwz62j9BQ6_qgZpD52Nhro5eXrDbW7UBUAplQE3-HztsnBqK1VN0cu7cyjb3MdoW7m7UbofT45HWAzWNRnUaIY82QgRODLumjVTlu7YjhrZzuCD8koQ6DwvrvNusme2oYW93S4ESJ48gNGnj096F--pNyz7Ihn_t2kJIi5C2ZxXURhrNf_vTcS-uUky9FgSM7B0_HfwzOa9cOL51zmYRw1scnbbbS-JZwk_GhSaxSPyi_bch47Capo1BJ64nd88-MYWQTccaMvyr02OSbPjqgynIEm34V_qzDNHp_59iwp1UhDRkE02KNtlo0lAisa0Mf0YGG19xJ0RQJ2BmlU-I4c3hN1LwcudEBjphJMjElE8C439r4k9QEBayMmvgxwgaLz6Tld3SrlOFIboQcvPyHRH8ZpLczsDwnDsx48ZByDEimPeFF5dWyUrs_xO0S_Vc3ikwUghjgRqOK2RyufXHgMAc1nGBM4Oxf11PdGqKH6IAvGsLHpHq2kbfupZEJqVyh2v55gorOH1_d1kxBDM64iLcBhLNLtyuAZCTHtUGEzYoI7n3ID1i9_lS_8w33xjlHNoBaujmqGrkH32NQ9wry3qP46w1jrJhT4M4eMCtDrmDUCnz6egfS-CITtB3dRODgdKZiA9TEGWJBj3DGGMvrGGzjMMW-WC5jqvikpnFb56wZx4YHDZYAmFeNyiKbobgkpq3WMsVZCw9N5gpGgsvj_5cYtQQEgJm9yTtfnhUQA4fRY-TFffvxeqFpdcHFM413JdPgCoch0Npq4gYn0fpTqr6Feuw4rhvrSIAOyZ8SkRV7MQ12YomAWQiUecTl1Eye656yN6vKSvrIQ4ej-nuo9VPwQWARd0wRTPikdE0V_Zf40ERubk30jFo-Te6QlamHhQ70xb37opKS7f9nP55Q7q3JCkd5HYuNGSZ-8n-FD42nWE1t_5tTJD5VvsItETIJY2RtkWn4aPIVbvTiBW1U6kiBKNdfGTHC8cPE2MSSJkvQag,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5II-gGm8_O_nvaauUitOq6z7ZkQLdYt7suAK1_Iuv8xDuidW9JEV5AyJqy10DqLBLzrRfIQZUPvHbsjxS_psmt-VwO-eoO1snFn4eoWEkbJzFr2cJxzvrbIY8aeCm9Bw7D2WWMmtr8nqNF1l1sd-LIR0hqK6k7BD32dptJbFMm9bN_McN1dXESDtQoxUXBJe&b64e=1&sign=790ceb775396d66d35e1df9194ec3ebc&keyno=1',
                        direct:
                            'https://www.chitai-gorod.ru/catalog/book/1209904/?utm_source=ymarket_MSK&utm_medium=cpc&utm_campaign=9646&utm_content=9703&utm_term=1209904',
                    },
                },
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 20599010,
                        name: 'Манга и комиксы',
                        slug: 'manga-i-komiksy',
                        fullName: 'Манга и комиксы',
                        isLeaf: true,
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 506,
                            containerHeight: 699,
                            url: '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/orig',
                            width: 506,
                            height: 699,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/50x50',
                                width: 36,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/55x70',
                                width: 50,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/60x80',
                                width: 57,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/74x100',
                                width: 72,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/75x75',
                                width: 54,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/90x120',
                                width: 86,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/100x100',
                                width: 72,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/120x160',
                                width: 115,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/150x150',
                                width: 108,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/180x240',
                                width: 173,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/190x250',
                                width: 180,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/200x200',
                                width: 144,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/240x320',
                                width: 231,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/300x300',
                                width: 217,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/300x400',
                                width: 289,
                                height: 400,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 600,
                                url:
                                    '//avatars.mds.yandex.net/get-marketpic/1674429/market_hJWQ73tGjMAvDV3ZPihzVQ/600x600',
                                width: 434,
                                height: 600,
                            },
                        ],
                        signatures: [],
                    },
                ],
                filters: [
                    {
                        id: '7893318',
                        type: 'enum',
                        name: 'Издательство',
                        xslname: 'vendor',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 1,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 85,
                                found: 1,
                                value: 'Cobalt',
                                vendor: {
                                    name: 'Cobalt',
                                    entity: 'vendor',
                                    id: 17840014,
                                },
                                id: '17840014',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['17840014'],
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '18670550',
                        type: 'enum',
                        name: 'Тип графической книги',
                        xslname: 'grafic_book',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 2,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 2,
                                found: 1,
                                value: 'комиксы',
                                id: '18670591',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['18670591'],
                            },
                        ],
                        meta: {},
                    },
                ],
                meta: {},
                marketSkuCreator: 'market',
                model: {
                    id: 664242706,
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
                        value: '275',
                        isDeliveryIncluded: false,
                        isPickupIncluded: false,
                    },
                    isFree: false,
                    isDownloadable: false,
                    inStock: false,
                    postAvailable: true,
                    isExpress: false,
                    isEda: false,
                    options: [
                        {
                            price: {
                                currency: 'RUR',
                                value: '275',
                                isDeliveryIncluded: false,
                                isPickupIncluded: false,
                            },
                            dayFrom: 1,
                            dayTo: 3,
                            orderBefore: '18',
                            isDefault: true,
                            serviceId: '99',
                            tariffId: 4020733,
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
                    id: 178899,
                    name: 'Читай-город',
                    business_id: 920035,
                    business_name: 'НОВЫЙ КНИЖНЫЙ ЦЕНТР',
                    slug: 'chitai-gorod',
                    gradesCount: 42834,
                    overallGradesCount: 42834,
                    qualityRating: 5,
                    isGlobal: false,
                    isCpaPrior: false,
                    isCpaPartner: false,
                    isNewRating: true,
                    newGradesCount: 42834,
                    newQualityRating: 4.55612364,
                    newQualityRating3M: 4.598728814,
                    ratingToShow: 4.598728814,
                    ratingType: 3,
                    newGradesCount3M: 4720,
                    status: 'actual',
                    cutoff: '',
                    loyalty_program_status: 'DISABLED',
                    outletsCount: 76,
                    storesCount: 75,
                    pickupStoresCount: 1,
                    depotStoresCount: 1,
                    postomatStoresCount: 0,
                    bookNowStoresCount: 0,
                    subsidies: false,
                    logo: {
                        entity: 'picture',
                        width: 103,
                        height: 14,
                        url:
                            '//avatars.mds.yandex.net/get-market-shop-logo/1539910/2a00000167980d230dbbc7cefd525807be15/small',
                        extension: 'PNG',
                        thumbnails: [
                            {
                                entity: 'thumbnail',
                                id: '103x14',
                                containerWidth: 103,
                                containerHeight: 14,
                                width: 103,
                                height: 14,
                                densities: [
                                    {
                                        entity: 'density',
                                        id: '1',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1539910/2a00000167980d230dbbc7cefd525807be15/small',
                                    },
                                    {
                                        entity: 'density',
                                        id: '2',
                                        url:
                                            '//avatars.mds.yandex.net/get-market-shop-logo/1539910/2a00000167980d230dbbc7cefd525807be15/orig',
                                    },
                                ],
                            },
                        ],
                    },
                    domainUrl: 'www.chitai-gorod.ru',
                    feed: {
                        id: '574923',
                        offerId: '1209904',
                        categoryId: '9703',
                    },
                    phones: {
                        raw: '8 800 444-8-444',
                        sanitized: '88004448444',
                    },
                    createdAt: '2013-09-11T17:58:20',
                    mainCreatedAt: '2013-09-11T17:58:20',
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
                weight: '1.1',
                returnPolicy: '7d',
                wareId: 'lTMtqBZMft9CQCh0oTJ2Cg',
                marketSku: '664242706',
                sku: '664242706',
                offerColor: 'white',
                isFreeOffer: false,
                classifierMagicId: '0d210321f58890907d7fa0cd267d6cec',
                prices: {
                    currency: 'RUR',
                    value: '1155',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                    rawValue: '1155',
                },
                manufacturer: {
                    entity: 'manufacturer',
                    warranty: false,
                },
                seller: {
                    price: '1155',
                    currency: 'RUR',
                    sellerToUserExchangeRate: 1,
                },
                payments: {
                    deliveryCard: false,
                    deliveryCash: false,
                    prepaymentCard: true,
                    prepaymentOther: false,
                },
                isRecommendedByVendor: false,
                outlet: {
                    entity: 'outlet',
                    id: '219600295',
                    name: 'Читай-город',
                    purpose: ['store'],
                    daily: true,
                    'around-the-clock': false,
                    gpsCoord: {
                        longitude: '37.485421',
                        latitude: '55.840897',
                    },
                    isMarketBranded: false,
                    type: 'store',
                    serviceId: 99,
                    serviceName: 'Собственная служба',
                    isMegaPoint: false,
                    email: '',
                    shop: {
                        id: 178899,
                    },
                    address: {
                        fullAddress: 'Москва, Кронштадтский б-р, д. 3а',
                        country: '',
                        region: '',
                        locality: 'Москва',
                        street: 'Кронштадтский б-р, д. 3а',
                        km: '',
                        building: '',
                        block: '',
                        wing: '',
                        estate: '',
                        entrance: '',
                        floor: '',
                        room: '',
                        office_number: '',
                        note: 'ТЦ «Гавань», 3 этаж',
                    },
                    telephones: [
                        {
                            entity: 'telephone',
                            countryCode: '7',
                            cityCode: '800',
                            telephoneNumber: '4448444',
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
                showUid: '16164085614077774375016002',
                entity: 'product',
                trace: {
                    fullFormulaInfo: [
                        {
                            tag: 'Default',
                            name: 'Sovetnik_loss_562159_045_x_Click_438131',
                            value: '0.362241',
                        },
                    ],
                },
                vendor: {
                    entity: 'vendor',
                    id: 17840014,
                    name: 'Cobalt',
                    slug: 'cobalt',
                    filter: '7893318:17840014',
                },
                titles: {
                    raw: 'Corpus Monstrum',
                    highlighted: [
                        {
                            value: 'Corpus Monstrum',
                        },
                    ],
                },
                slug: 'corpus-monstrum',
                description: 'издательство: Cobalt, ISBN: 978-5-904662-34-9',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 90829,
                        nid: 20599010,
                        name: 'Комиксы и манга',
                        slug: 'komiksy-i-manga',
                        fullName: 'Комиксы и манга',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                warnings: {
                    common: [
                        {
                            type: 'age',
                            value: {
                                full: 'Возрастное ограничение',
                                short: 'Возрастное ограничение',
                            },
                        },
                    ],
                },
                urls: {},
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 20599010,
                        name: 'Манга и комиксы',
                        slug: 'manga-i-komiksy',
                        fullName: 'Манга и комиксы',
                        isLeaf: true,
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 510,
                            containerHeight: 701,
                            url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/orig',
                            width: 510,
                            height: 701,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/50x50',
                                width: 36,
                                height: 50,
                            },
                            {
                                containerWidth: 55,
                                containerHeight: 70,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/55x70',
                                width: 50,
                                height: 70,
                            },
                            {
                                containerWidth: 60,
                                containerHeight: 80,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/60x80',
                                width: 58,
                                height: 80,
                            },
                            {
                                containerWidth: 74,
                                containerHeight: 100,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/74x100',
                                width: 72,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/75x75',
                                width: 54,
                                height: 75,
                            },
                            {
                                containerWidth: 90,
                                containerHeight: 120,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/90x120',
                                width: 87,
                                height: 120,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/100x100',
                                width: 72,
                                height: 100,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 160,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/120x160',
                                width: 116,
                                height: 160,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/150x150',
                                width: 109,
                                height: 150,
                            },
                            {
                                containerWidth: 180,
                                containerHeight: 240,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/180x240',
                                width: 174,
                                height: 240,
                            },
                            {
                                containerWidth: 190,
                                containerHeight: 250,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/190x250',
                                width: 181,
                                height: 250,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/200x200',
                                width: 145,
                                height: 200,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 320,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/240x320',
                                width: 232,
                                height: 320,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 300,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/300x300',
                                width: 218,
                                height: 300,
                            },
                            {
                                containerWidth: 300,
                                containerHeight: 400,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/300x400',
                                width: 291,
                                height: 400,
                            },
                            {
                                containerWidth: 600,
                                containerHeight: 600,
                                url: '//avatars.mds.yandex.net/get-mpic/1657306/img_id4831389026512680612.jpeg/600x600',
                                width: 436,
                                height: 600,
                            },
                        ],
                        signatures: [],
                    },
                ],
                filters: [
                    {
                        id: '7893318',
                        type: 'enum',
                        name: 'Издательство',
                        xslname: 'vendor',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 1,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 85,
                                found: 1,
                                value: 'Cobalt',
                                vendor: {
                                    name: 'Cobalt',
                                    entity: 'vendor',
                                    id: 17840014,
                                },
                                id: '17840014',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['17840014'],
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '18670550',
                        type: 'enum',
                        name: 'Тип графической книги',
                        xslname: 'grafic_book',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 2,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 2,
                                found: 1,
                                value: 'комиксы',
                                id: '18670591',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['18670591'],
                            },
                        ],
                        meta: {},
                    },
                ],
                meta: {},
                type: 'book',
                id: 664242706,
                modelCreator: 'market',
                skuOffersCount: 0,
                offers: {
                    count: 1,
                    cutPriceCount: 0,
                },
                isNew: false,
                prices: {
                    min: '1155',
                    max: '1155',
                    currency: 'RUR',
                    avg: '1155',
                },
                lingua: {
                    type: {
                        nominative: '',
                        genitive: '',
                        dative: '',
                        accusative: '',
                    },
                },
                retailersCount: 3,
                promo: {
                    whitePromoCount: 0,
                },
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
                    min: '634',
                    id: 'chosen',
                },
                {
                    max: '1276.000051',
                    initialMax: '1276.000051',
                    initialMin: '1155',
                    min: '1155',
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
            id: '7893318',
            type: 'enum',
            name: 'Издательство',
            xslname: 'vendor',
            subType: '',
            kind: 1,
            isGuruLight: true,
            position: 1,
            noffers: 4,
            valuesCount: 1,
            values: [
                {
                    initialFound: 5,
                    popularity: 2250,
                    found: 4,
                    value: 'Cobalt',
                    priceMin: {
                        currency: 'RUR',
                        value: '1155',
                    },
                    vendor: {
                        name: 'Cobalt',
                        entity: 'vendor',
                        id: 17840014,
                    },
                    id: '17840014',
                },
            ],
            valuesGroups: [
                {
                    type: 'all',
                    valuesIds: ['17840014'],
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
                    initialFound: 4,
                    found: 4,
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
                    found: 2,
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
                    initialFound: 3,
                    found: 2,
                    value: 'delivery',
                },
                {
                    initialFound: 3,
                    found: 2,
                    value: 'pickup',
                },
                {
                    initialFound: 3,
                    found: 2,
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
                    initialFound: 3,
                    found: 2,
                    value: 'Картой на сайте',
                    id: 'prepayment_card',
                },
                {
                    initialFound: 0,
                    found: 0,
                    value: 'Картой курьеру',
                    id: 'delivery_card',
                },
                {
                    initialFound: 1,
                    found: 0,
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
                    found: 4,
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
            valuesCount: 2,
            values: [
                {
                    found: 1,
                    value: 'Лабиринт',
                    id: '1550',
                },
                {
                    found: 2,
                    value: 'Читай-город',
                    id: '178899',
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
            relevance: -0.291444,
            category: {
                name: 'Книги',
                slug: 'knigi',
                uniqName: 'Книги',
                hid: 90829,
                nid: 54510,
                isLeaf: false,
                kinds: [],
                view: 'grid',
            },
            intents: [
                {
                    defaultOrder: 1,
                    ownCount: 2,
                    relevance: -0.590032,
                    category: {
                        name: 'Комиксы и манга',
                        slug: 'komiksy-i-manga',
                        uniqName: 'Комиксы и манга',
                        hid: 90829,
                        nid: 20599010,
                        isLeaf: true,
                        kinds: [],
                        view: 'grid',
                    },
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
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
