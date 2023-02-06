import { normalizeVast } from '../videoAdapter/utils/normalizeVast';
import { OLD_SMETA_VIDEO_BS_META_MOCK } from './mocks/OLD_SMETA_VIDEO_BS_META_MOCK';
import { oldSmetaVideoAdapter, isOldSmetaVideoBSMeta } from './oldSmetaVideoAdapter';

describe('oldSmetaVideoAdapter', () => {
    it('should return correct value', () => {
        const bsMeta = OLD_SMETA_VIDEO_BS_META_MOCK;
        const adaptedBSMeta = oldSmetaVideoAdapter(bsMeta);
        const adaptedBSMetaVast = normalizeVast(adaptedBSMeta.rtb.vast);
        const expectedVast = normalizeVast(`
            <?xml version="1.0" encoding="utf-8" ?>
            <VAST version="3.0">
                <Ad>
                    <InLine>
                        <AdSystem>Multibanner</AdSystem>
                        <Creatives>
                            <Creative>
                                <Linear>
                                    <AdParameters>
                                        <![CDATA[
                                            {
                                        "trackingEvents": {
                                            "start": [
                                            "https://yandex.ru/an/tracking/WNqejI_zODC0dGi0z1C00000IOVfNmK0qm4nHUo-OW00000ucALIO8mOQ0I00O6tWmM80VBQjm-G0PgSby3QW8200fW1cfoNm5gm0PQ__Qq8k06WjPxE9DW1ekMifW7W0ShtgwC1e0AoOuW5c5oG1PXSi0Mu5xW5k1V01PXSo0Mu5-05J4Keeciq4duxgGS81bKm94SMCxW7W0NG1mG6a846w0a7y0c05FW9e0k02Wg82mYg2n35Rz8H20O103wBli2zuWK0y0iBu0s2W971W80Ge8q6g0-8Z8Iwm-IHYpEu3m-04D7mlYECW0IO4Rw49O0K8QWKc5pGvVQM1jWLmOhsxAEFlFnZWHUO5yghgIxO5y24FT0O4FWOW1cu6W6270rPSaGwM4nMQt1iKM4twHo07N_O7eIh7w0VqV2-8x0V0SWVqRAMK-0W0T0X3m2woTf3yAGb0hakfJ5QW20GTiBFlCHb7jRCZG-dika4LVVcbE4ox6XtMoNhSMlTH1G0~1?action-id=11",
                                            "https://yandex.ru/an/rtbcount/1VH0zSfu0Rm100000000U9nJTDLOb6qYL6TjoC7cQAxYBcPiwVfcNn8347Z2H4BrjotrYVNnv698PGIAPtAmXHg0n4jPWBpQgq2YbH54Te9a6GGOc1YcCW8fXfcC5z9OGK8C2nclJMO1irR2MNiP7Ppu6Ow2-MSPcO4YLnb11jnbP91XOFZBU7hzVQUvJ1n0aQLC341ZrZ9TO69WEClq7mXUCGaSpRyaoC76o63NlU_0taUi37-Pk40EOGPPsCiCj9rXAZD8P2upEyXQPYO75GuE1w0bp61BcFc1v5Fcm9cfN3-PGGu1WGqiZ3jOc0-mCDs4E97XW_r3E6i1A-z9rirFEC3S2woANrb1Bld5ocbeRc9YR61gOjh0Td7JjE0EjWQM6MnN3WSlODkzwU6RhXUV_gndNCa6C-O0cyS9DkP7RBoHHIvM4JvXsGKvWOdcB-7oF-6iYUmmrWPsfXtiZ2VOHHwmpzguzzxFROSQxNwmym00Om9qwG00"
                                            ],
                                            "trueView": [
                                            "https://yandex.ru/an/tracking/WNqejI_zODC0dGi0z1C00000IOVfNmK0qm4nHUo-OW00000ucALIO8mOQ0I00O6tWmM80VBQjm-G0PgSby3QW8200fW1cfoNm5gm0PQ__Qq8k06WjPxE9DW1ekMifW7W0ShtgwC1e0AoOuW5c5oG1PXSi0Mu5xW5k1V01PXSo0Mu5-05J4Keeciq4duxgGS81bKm94SMCxW7W0NG1mG6a846w0a7y0c05FW9e0k02Wg82mYg2n35Rz8H20O103wBli2zuWK0y0iBu0s2W971W80Ge8q6g0-8Z8Iwm-IHYpEu3m-04D7mlYECW0IO4Rw49O0K8QWKc5pGvVQM1jWLmOhsxAEFlFnZWHUO5yghgIxO5y24FT0O4FWOW1cu6W6270rPSaGwM4nMQt1iKM4twHo07N_O7eIh7w0VqV2-8x0V0SWVqRAMK-0W0T0X3m2woTf3yAGb0hakfJ5QW20GTiBFlCHb7jRCZG-dika4LVVcbE4ox6XtMoNhSMlTH1G0~1?action-id=19"
                                            ],
                                            "returnAfterClickThrough": [
                                            "https://yandex.ru/an/count/WqCejI_zO083pHa0f2jCfROlO3M8IWK00WGGWg0GCKNilc800000E9YbKa0CI09WZ1Xe172wX-xriz-Qylu1W061juC5Y07oshSFa06Qd9V0se20W0AO0PgSby1Qi06Ml_sj2BW1eBMUpYJ00GBO0QBbhAO1u07AzwkZ0UW1LlW1ffkoO_02zUoVg0cu1Fy1Y0MON905c5om1RWNk0Mu5y05c5p81RWNq0Mt5-05JDkrHIYYQpGIVZkf1mW6LJ0aHnOpk0U01T071DW730QGWGRW2Danw0a7y0c05FW9e0k02Wh92AeB4CLlqX481W40Fek-mBtYw0kONF0B2uWCW9C2c0t0X3tW3OA0aS60W10_e0wWZGQ8Z8Iwm-IHYpEu3m-04D7mlYE84C_QcvUCW0JG4BoB6fWHleGby18MY1C5u1Eu5u0K8Q0Kk1Ue59XSqENsbWRe58m2q1NGvVQM1jWLmOhsxAEFlFnZy9WMyBRwbGQWg1Re1yaMq1Q0-Dw-0O4NY1S1c1VAgwakg1SDm1UsbW7O5y24FUWN0Q0O4B0OchNxbGQu607G613e607u68l3XPNrWOQmpm606OaPi-IG6G6W6Qe3k1d___y1qXaIUM5YSrzpPN9sPN8lSZOuCYqnu1a1w1c0mWFm6O320u4Q__ythYvNlmUO6jJ3Kx0QkxBXx8heylol0RWQ0VKQ0G00088RJ34sDJOsDpaqEJCvCJWrEJCjCJOsEJSoEJCsCpCmDZKtCpGnCJGmC30uEIrmSczaTMDqQMzkBM5mS2reRtDqBNPiOIrmOszaPIqoCJMO6y24FR0RIBWR0u8S3LboH3fOJ5PhS6nHOJVf780T_t-P7SWTe8q6s1s0wxK7u1sMWSS8g1u1s1w4gn-87____m6W7z7mlYEm7m787z6obbE080A8806m8820W07W807G8V__0IGWCGGLnIYb56JAfknSERzVZe53yj4ojdKSaMenZbtJGTJ_0Wd9GImfii6nROw-prnR3NGGEOSry_tpceQOMFapipolJLUFu8JAiw8nMXGDpWnmTAz4C7RhWNTbuv727gPzhNOyYtgq80wb6640T4pR8umQ12SyP1tt9w8e~1?test-tag=136"
                                            ],
                                            "showHp": [
                                            "https://yandex.ru/an/tracking/WNqejI_zODC0dGi0z1C00000IOVfNmK0qm4nHUo-OW00000ucALIO8mOQ0I00O6tWmM80VBQjm-G0PgSby3QW8200fW1cfoNm5gm0PQ__Qq8k06WjPxE9DW1ekMifW7W0ShtgwC1e0AoOuW5c5oG1PXSi0Mu5xW5k1V01PXSo0Mu5-05J4Keeciq4duxgGS81bKm94SMCxW7W0NG1mG6a846w0a7y0c05FW9e0k02Wg82mYg2n35Rz8H20O103wBli2zuWK0y0iBu0s2W971W80Ge8q6g0-8Z8Iwm-IHYpEu3m-04D7mlYECW0IO4Rw49O0K8QWKc5pGvVQM1jWLmOhsxAEFlFnZWHUO5yghgIxO5y24FT0O4FWOW1cu6W6270rPSaGwM4nMQt1iKM4twHo07N_O7eIh7w0VqV2-8x0V0SWVqRAMK-0W0T0X3m2woTf3yAGb0hakfJ5QW20GTiBFlCHb7jRCZG-dika4LVVcbE4ox6XtMoNhSMlTH1G0~1?action-id=21"
                                            ],
                                            "clickHp": [
                                            "https://yandex.ru/an/tracking/WNqejI_zODC0dGi0z1C00000IOVfNmK0qm4nHUo-OW00000ucALIO8mOQ0I00O6tWmM80VBQjm-G0PgSby3QW8200fW1cfoNm5gm0PQ__Qq8k06WjPxE9DW1ekMifW7W0ShtgwC1e0AoOuW5c5oG1PXSi0Mu5xW5k1V01PXSo0Mu5-05J4Keeciq4duxgGS81bKm94SMCxW7W0NG1mG6a846w0a7y0c05FW9e0k02Wg82mYg2n35Rz8H20O103wBli2zuWK0y0iBu0s2W971W80Ge8q6g0-8Z8Iwm-IHYpEu3m-04D7mlYECW0IO4Rw49O0K8QWKc5pGvVQM1jWLmOhsxAEFlFnZWHUO5yghgIxO5y24FT0O4FWOW1cu6W6270rPSaGwM4nMQt1iKM4twHo07N_O7eIh7w0VqV2-8x0V0SWVqRAMK-0W0T0X3m2woTf3yAGb0hakfJ5QW20GTiBFlCHb7jRCZG-dika4LVVcbE4ox6XtMoNhSMlTH1G0~1?action-id=22"
                                            ],
                                            "adsFinish": [
                                            "https://yandex.ru/an/tracking/WNqejI_zODC0dGi0z1C00000IOVfNmK0qm4nHUo-OW00000ucALIO8mOQ0I00O6tWmM80VBQjm-G0PgSby3QW8200fW1cfoNm5gm0PQ__Qq8k06WjPxE9DW1ekMifW7W0ShtgwC1e0AoOuW5c5oG1PXSi0Mu5xW5k1V01PXSo0Mu5-05J4Keeciq4duxgGS81bKm94SMCxW7W0NG1mG6a846w0a7y0c05FW9e0k02Wg82mYg2n35Rz8H20O103wBli2zuWK0y0iBu0s2W971W80Ge8q6g0-8Z8Iwm-IHYpEu3m-04D7mlYECW0IO4Rw49O0K8QWKc5pGvVQM1jWLmOhsxAEFlFnZWHUO5yghgIxO5y24FT0O4FWOW1cu6W6270rPSaGwM4nMQt1iKM4twHo07N_O7eIh7w0VqV2-8x0V0SWVqRAMK-0W0T0X3m2woTf3yAGb0hakfJ5QW20GTiBFlCHb7jRCZG-dika4LVVcbE4ox6XtMoNhSMlTH1G0~1?action-id=24"
                                            ]
                                        },
                                        "mediaFiles": [
                                            {
                                            "width": "640",
                                            "height": "360",
                                            "url": "https://strm.yandex.ru/vh-canvas-converted/vod-content/7519605285043286424/d136df62-b9eec164-9eeec77a-6523ad9a/mp4/H264_640_360_900.mp4",
                                            "type": "video/mp4"
                                            },
                                            {
                                            "width": "1280",
                                            "height": "720",
                                            "url": "https://strm.yandex.ru/vh-canvas-converted/vod-content/7519605285043286424/d136df62-b9eec164-9eeec77a-6523ad9a/mp4/H264_1280_720_2880.mp4",
                                            "type": "video/mp4"
                                            },
                                            {
                                            "width": "854",
                                            "height": "480",
                                            "url": "https://strm.yandex.ru/vh-canvas-converted/vod-content/7519605285043286424/d136df62-b9eec164-9eeec77a-6523ad9a/mp4/H264_854_480_1510.mp4",
                                            "type": "video/mp4"
                                            },
                                            {
                                            "width": "426",
                                            "height": "240",
                                            "url": "https://strm.yandex.ru/vh-canvas-converted/vod-content/7519605285043286424/d136df62-b9eec164-9eeec77a-6523ad9a/mp4/H264_426_240_500.mp4",
                                            "type": "video/mp4"
                                            },
                                            {
                                            "width": "256",
                                            "height": "144",
                                            "url": "https://strm.yandex.ru/vh-canvas-converted/vod-content/7519605285043286424/d136df62-b9eec164-9eeec77a-6523ad9a/mp4/H264_256_144_200.mp4",
                                            "type": "video/mp4"
                                            }
                                        ],
                                        "theme": "video-banner_interactive-viewer",
                                        "firstFrame": {
                                            "images": [
                                            {
                                                "url": "https://avatars.mds.yandex.net/get-vh/5399164/2a0000017fdad9d9372e85d60945a2e38187/orig",
                                                "width": "1280",
                                                "height": "720"
                                            }
                                            ]
                                        }
                                        }
                                    ]]>
                                    </AdParameters>
                                    <MediaFiles>
                                        <MediaFile height="0"
                                                width="0"
                                                type="application/x-javascript"
                                                apiFramework="VPAID">
                                            <![CDATA[https://yastatic.net/awaps-ad-sdk-js/1_0/interactive_viewer.js]]>
                                        </MediaFile>
                                    </MediaFiles>
                                </Linear>
                            </Creative>
                        </Creatives>
                        <Extensions>
                        </Extensions>
                    </InLine>
                </Ad>
            </VAST>
        `);
        const expectedDataParams = {
            text: {
                body: 'Покупаем лодку',
                domain: 'ya.ru',
                title: 'Банк',
                age: '',
                warning: '',
                bannerFlags: '',
            },
            assets: {
                button: {},
                logo: {
                    x80: {
                        url: 'https://avatars.mds.yandex.net/get-direct/4733431/NprXVw4PiuEOizMMkWyKUA/x80',
                        width: 80,
                        height: 80,
                    },
                },
                images: [['firstImage', '40', '50'], ['secondImage', '70', '100']],
            },
            click_url: {
                action_button:
                    'https://yandex.ru/an/count/WqGejI_zO0C3rHa0j2jCfROl-G-IdGK00mGGWg0GCKNilc800000E9YbKa0CI09WZ1Xe172wX-xriz-Qylu1W061juC5Y07oshSFa06Qd9V0se20W0AO0PgSby1Qi06Ml_sj2BW1eBMUpYJ00SO1s06YvQoc0U01olUhem7e0LRu0QQRicFm0lNidwW9k0J_0OW5c5oG1PXSi0Mu5xW5k1V01PXSo0Mu5z05jnVW1KpRjKKeeciq4duxgGS81bKm94SMCxW7W0NG1mJO1mm6a846u0ZPCUW91_09W1Ju2Q0BW0eAoGYg2n35Rz8H20O103wBli2zukWBc5pm2mk8382J0fWDm8Gzu0s2W971W80GFw0Ee8q6Y8o4kiFaaOipk0yFW13HyBuZY13FsfkNZ804q12yYngO4Rw49V0I5eWJ1U0Jk1U0526W5BWNg1IOND3bzfO6w1IC0j0LqENsbWRO5S6AzkoZZxpyO_2O5l2s-fK6eAWMw0V95j0MWFZUlW615uWN0PWNogkfBgWN3S0NjfO1s1V0X3te5m6W612m69gr-vK6k1W1q1WGw1W1-1YBmuMLzO66iCy1W1c96RFaa1a1e1cg0xWP____0T8P4dbXOdDVSsLoTcLoBt8sE38jCU0P0UWPWC83y1c0mWE16l__DwukLxy7c1hKmrEm6hkouUoAwFByhm6u6W7r6W4000226qmnDZKsDZSvD3apEJ4uDJapBJ4sDZatCZapDZCpC3OrDpCqCJ4qC30mE3ajS79lP7LZT6blRYrXS70jQ6zpT2rsR64jS6DlP6KjCZ4rc1l0X3sm6qYu6mE270rPSaGwM4nMQt1iKM4twHo07Vz_cHt87Q2D1jWTWEkr1-0Tbe772AWU0TWUXAiVY1____y1e1_HyBuZi1y1o1_HifPJW202Y201i220W801u201q27__m4a83445SKefHHaogRiN3c_Nuw1G_BHChPr795hCSvTqq7K_m89oK4iARB1iMtElizSNGrq63c7HVFz-vg6e5ZvCyCyhqrOZ-2KohEYCLiK3T0CS7IlH41swv5tPUkHmYAcVQzsF0jxj20FfHY107HC6oIC6YGdFAGjzoUYA000~1',
                text_name:
                    'https://yandex.ru/an/count/WqCejI_zO083pHa0f2jCfROlO3M8IWK00WGGWg0GCKNilc800000E9YbKa0CI09WZ1Xe172wX-xriz-Qylu1W061juC5Y07oshSFa06Qd9V0se20W0AO0PgSby1Qi06Ml_sj2BW1eBMUpYJ00GBO0QBbhAO1u07AzwkZ0UW1LlW1ffkoO_02zUoVg0cu1Fy1Y0MON905c5om1RWNk0Mu5y05c5p81RWNq0Mt5-05JDkrHIYYQpGIVZkf1mW6LJ0aHnOpk0U01T071DW730QGWGRW2Danw0a7y0c05FW9e0k02Wh92AeB4CLlqX481W40Fek-mBtYw0kONF0B2uWCW9C2c0t0X3tW3OA0aS60W10_e0wWZGQ8Z8Iwm-IHYpEu3m-04D7mlYE84C_QcvUCW0JG4BoB6fWHleGby18MY1C5u1Eu5u0K8Q0Kk1Ue59XSqENsbWRe58m2q1NGvVQM1jWLmOhsxAEFlFnZy9WMyBRwbGQWg1Re1yaMq1Q0-Dw-0O4NY1S1c1VAgwakg1SDm1UsbW7O5y24FUWN0Q0O4B0OchNxbGQu607G613e607u68l3XPNrWOQmpm606OaPi-IG6G6W6Qe3k1d___y1qXaIUM5YSrzpPN9sPN8lSZOuCYqnu1a1w1c0mWFm6O320u4Q__ythYvNlmUO6jJ3Kx0QkxBXx8heylol0RWQ0VKQ0G00088RJ34sDJOsDpaqEJCvCJWrEJCjCJOsEJSoEJCsCpCmDZKtCpGnCJGmC30uEIrmSczaTMDqQMzkBM5mS2reRtDqBNPiOIrmOszaPIqoCJMO6y24FR0RIBWR0u8S3LboH3fOJ5PhS6nHOJVf780T_t-P7SWTe8q6s1s0wxK7u1sMWSS8g1u1s1w4gn-87____m6W7z7mlYEm7m787z6obbE080A8806m8820W07W807G8V__0IGWCGGLnIYb56JAfknSERzVZe53yj4ojdKSaMenZbtJGTJ_0Wd9GImfii6nROw-prnR3NGGEOSry_tpceQOMFapipolJLUFu8JAiw8nMXGDpWnmTAz4C7RhWNTbuv727gPzhNOyYtgq80wb6640T4pR8umQ12SyP1tt9w8e~1',
            },
            punyDomain: 'ya.ru',
            faviconWidth: '120',
            faviconHeight: '120',
            adId: '72057606184822541',
            firstFrame: {
                images: [{
                    url: 'https://avatars.mds.yandex.net/get-vh/5399164/2a0000017fdad9d9372e85d60945a2e38187/orig',
                    width: '1280',
                    height: '720',
                }],
            },
            abuse: 'https://yandex.ru/an/abuse/WCWejI_z8EFz1G1q2aobjY-IHa-61G3Z0355xBvY000003YOfL9mkeVkzRFVclB-0P01cfoNmDg0W802c06Qd9V0MhW1eBMUpYJO0QBbhAO1w05Me0AoOya6HIYYQpGIVZkf1mW6LJ0aHnOpY0i8gWiGnM_I4GW60G0-Yxx0lU850DWLmOhsxAEFlFnZWHUO5yghgIxu680PWXmDMN94EbXCLcjmR55XD-aSW1r_q27___y11m2iCRH0iAn8S2dDfyIobFXw8YwV0G00~1',
            targetUrl: 'https://ya.ru',
            newImage: {
                '0': {
                    val: 'firstImage',
                    w: 40,
                    h: 50,
                },
                '1': {
                    val: 'secondImage',
                    w: 70,
                    h: 100,
                },
            },
        };

        expect(isOldSmetaVideoBSMeta(bsMeta)).toEqual(true);
        expect(adaptedBSMetaVast).toEqual(expectedVast);
        expect(adaptedBSMeta.rtb.data_params).toEqual(expectedDataParams);
        expect(adaptedBSMeta.settings['4'].videoInComboDesign).toEqual('morda-tzar');
    });
});
