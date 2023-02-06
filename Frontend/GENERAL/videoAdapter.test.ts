/* eslint-disable no-console */
import { videoAdapter } from './videoAdapter';
import { normalizeVast } from './utils/normalizeVast';
import { BSMetaVideo } from './typings';

describe('videoAdapter', () => {
    it('should return empty string if bsMeta is invalid', () => {
        expect(videoAdapter({})).toEqual('');
        expect(videoAdapter({
            bidId: '123123',
            bids: [{
                settings: {
                    '2': {
                        linkTail: null,
                        viewNotices: null,
                    },
                },
            }],
        })).toEqual('');
        expect(videoAdapter({
            bidId: '123123123',
            bids: [{
                settings: {
                    '2': {
                        linkTail: 'linkTail',
                        viewNotices: ['viewNotice1'],
                    },
                },
                dc_params: {
                    data_params: {
                        bs_data: {},
                        direct_data: {},
                    },
                },
            }],
        })).toEqual('');
    });

    it('should return empty VAST if no ads', () => {
        const expectedVast = normalizeVast('<?xml version="1.0" encoding="UTF-8"?><VAST version="3.0" ya-uniformat="true"/>');
        const resultVast = normalizeVast(videoAdapter({
            bidId: '123123',
            bids: [],
        }));

        expect(resultVast).toEqual(expectedVast);
    });

    it('should return Ad', () => {
        // Пример данных из прода с небольшими изменениями
        const bsMeta: BSMetaVideo = {
            bidId: '1231313124',
            bids: [{
                settings: {
                    '2': {
                        linkTail: 'https://an.yandex.ru/rtbcount/1K-WJkUj0Tm100000000U9nJ39ZPLBVzyyPmmwCdPUhttjvWQJab-sS20n1umaH25PaYqs8_7s9ZI6K4YcSUFMbI0H8lPGBoQgy2YLP643cJKMKHAqWdmqB62NqiODPAHWzt9Z1hB-DY0tw6es3-LKQGGNSP6UOmCFnblFTiczDS9WwWo5cc_q3mYacWTdNP-QCd6HXE_BbmS-jnLeQ_J223Mvb1P2-p8f2Soim59ESoWmnVoSpbeC00Cc0ZyvflQwild0s9FyaCyzbLPh1bp6n0yYbp0Fi1pfVC2evWDp7yP7PmuWUpDh0mxc1XFi32V9AbckFBH1DkYxqNM9ujU_ZyshHTYyHkGdp3yC7-8PJrmFuj2yWh2rWvLx0sVDdmQOt-9bO54mEB52D3uzIKD7CBYvJ450lJL5Epn4JJfBHK0yka5BC3h6V-_OS0nfEijVeGiowmAdnbPGGQ_8kLqz3CsDJSqjp8ee7jGb-3tmIs2vkwldp_zkT6-0osZmUsC2vWU_Ayitl7bxKFybQomPnoW2tv11lB8pPUyo9Bgmpto2pFyAeWSm3SDg08',
                        viewNotices: ['https://ad.doubleclick.net/ddm/trackimp/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;ord=1717109281;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=?', 'https://1717109281.verify.yandex.ru/verify?platformid=1&msid=msva5b55_5-63579728-10917332540&BID=10917332540&BTYPE=1&CID=63579728&DRND=1717109281&DTYPE=desktop&REF=https%3A%2F%2Ffrontend.vh.yandex.ru%2Fplayer%2F408d213edf788dad85e56aa5bfe09bd6%3Fuse_friendly_frame%3Dtrue%26vsid%3D9188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%26from%3Dya-weather%26reqid%3D1626958739556578-11259208775600116834%26flags%3D%257B%2522player_api_v2%2522%253A%2522false%2522%252C%2522disable_autoplay_slow_connection%2522%253A%2522false%2522%252C%2522disable_autoplay_save_data%2522%253A%2522false%2522%252C%2522poll_timeout%2522%253A%252221600%2522%252C%2522poll_show_before_midroll%2522%253A%2522false%2522%252C%2522version%2522%253A%2522undefined%2522%252C%2522progress%2522%253A%2522true%2522%252C%2522start_position_confirmation%2522%253A%2522false%2522%252C%2522force_unmute%2522%253A%2522true%2522%252C%2522restore_playback_progress%2522%253A%2522false%2522%252C%2522hide_brand_play_button%2522%253A%2522false%2522%257D%26stream_url%3Dhttps%253A%252F%252Fstrm.yandex.ru%252Fkal%252Fweather_moscow%252Fysign1%253Da9f3470057779456717984b3b3295b6c122b621e4efd76d0f8782bab1ce8f824%252CabcID%253D105%252Cfrom%253Dya-weather%252Cpfx%252Cregion%253D10000%252Csfx%252Cts%253D60fabd13%252Fmanifest.mpd%253Ffrom%253Dya-weather%2526partner_id%253D443123%2526target_ref%253Dhttps%25253A%25252F%25252Fyastatic.net%25252Fyandex-video-player-iframe-api%25252Fjs%25252Fplayer-api-adapter-loader.js%2526uuid%253D408d213edf788dad85e56aa5bfe09bd6%2526video_category_id%253D1025%2526clid%253D495%2526yandexuid%253D6319099721604569424%2526slots%253Dnull%2526imp_id%253D1%2526reqid%253D1626958739556578-11259208775600116834%2526content_id%253D408d213edf788dad85e56aa5bfe09bd6%2526from_block%253Dother%2526channel_id%253D1551792230%2526content-genre%253D%2526content-category%253D%2526brand-safety-categories%253D%25255B%25255D%2526sandbox_version%253D0x836c75a4440%26additional_params%3D%257B%2522from%2522%253A%2522ya-weather%2522%252C%2522reqid%2522%253A%25221626958739556578-11259208775600116834%2522%252C%2522vsid%2522%253A%25229188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%2522%252C%2522adsid%2522%253A%2522d4406f3b827c8108d733370b4db28a244117dc985457xWEBx7099x1626958738%2522%252C%2522content_id%2522%253A%2522408d213edf788dad85e56aa5bfe09bd6%2522%252C%2522from_block%2522%253A%2522other%2522%252C%2522channel_id%2522%253A%25221551792230%2522%252C%2522content-genre%2522%253A%2522%2522%252C%2522content-category%2522%253A%2522%2522%252C%2522brand-safety-categories%2522%253A%2522%255B%255D%2522%252C%2522sandbox_version%2522%253A%25220x836c75a4440%2522%257D%26partner_id%3D443123%26category%3D1025%26distr_id%3D0%26video_content_id%3D408d213edf788dad85e56aa5bfe09bd6%26video_content_name%3D%25D0%259F%25D0%25BE%25D0%25B3%25D0%25BE%25D0%25B4%25D0%25B0%2520%25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%26video_genre_name%3D%26preview%3D%252F%252Favatars.mds.yandex.net%252Fget-vh-cv%252F2424227%252F2a0000017ace447bfced2796b7c72daed5fd%252Forig%26host%3Dfrontend.vh.yandex.ru&SESSION=6869341626958739116&hitlogid=4157064062874995141&page=443123'],
                    },
                },
                dc_params: {
                    creative_params: {
                        crypta_user_gender: '1',
                        crypta_user_age: '2',
                    },
                    data_params: {
                        '72057604955260476': {
                            bs_data: {
                                sad: 'sad',
                                targetUrl: 'https://ad.doubleclick.net/ddm/trackclk/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=;ltd=',
                                domain: 'nestlebaby.ru',
                                bannerFlags: 'milk-substitute,animated',
                                bannerLang: '1',
                                adId: '72057604955260476',
                                actionButton: 'http://an.yandex.ru/count/Wq0ejI_zO0y3jHa0z2ncMG8XsZbYe0K03mGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm0760TW1e9s0d07W0RA9xvG1w06y0m8SRDBHF7n85F02ZCtDW_iPY0NAO905oc2m1SSOk0N76C05jJx81UyFq0MZ352PWdwtyx5NgGV5oJHulTkmERW7W0MQYe21mf211k08yUtJ0e0A2iaAtk9yRD_2-3_u2e2r680BfC9qY0i4gWiGM2KoDCN9002mPT7PjSVe2yfWy0i6cmQO3RYwNk0DYe21c0xGiehqz_Rys5UW3i24FRaEa8u4000000313aX70W35a_Fur_Uss3cu3mR83w220O0GtuBW6eWGp_EMY0QG48Rsoy3xiEAO8f140x3e_C0GwH9x2FIHD0iulV0I5eWJ19WJ1gWJvgRXvzQMkg8Mu1F7680K8A0KnnYe5CfWXlssXmRe58m2o1Mhx_IChWJG5ORzjeS6s1N1YlRieu-y_6Fme1RGyjo71gWMw0Um5gS5oHRG5ipSthu1s1Q15uWN0P0Na1oO5xcTiYke5md05xOIs1V0X3te5m7m5u3qiFAiBw0O4x0OyhhbXmQu60B2620qC3XaCZ4pPMHcDpWuP65aE3LbDJPXOJLYPcKmEM9aDj0O8V0Op07u6CkHclJhZhUwlW606OaPVI-W6Qe3i1cu6O48qXaIUM5YSrzpPN9sPN8lSZKqDoqow1dw0_0PdGBu6UZuenw16l__H-texdH_a1g0We20W820W0IO6l70j06m6kJdYOkuzAtWRlKQ0G000FWQkBfUWXibCJOoDZarE3SpEJKrDZKtE2qnCJ8rEJ8mE3StDJOmC34nDZWpD9WRzSnHi1jZk1i4WXmDDt8vEcX7MMf9LtfID-aSy3-07V__Vnu0OHkaKxCXhRUKX0sYPqhLbcSbyIV1dQMSPqCMNRP7vizShIruBZeS7g32qL37c5z2Zd-lmh1Ci0MmdkggADr7Qx4S4m1gTtgPMDw0rHwVpJt743JwK-Thad6q-a6JMoKP2dq6~1',
                                resource_links: {
                                    direct_data: {
                                        targetUrl: 'http://an.yandex.ru/count/WpyejI_zO0u3hHa0v2ncMG8XP8gx9GK03WGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm042s06WdO2S0U01iedlb07e0Rm30Xniqj4yV4WKy0ACpSs3-nc81SfWa0NAOB05nnYu1SSOm0MrFiW5xm_G1QCCK9c2VhVpiLUf1yN9D7Yzsx0vk0U01PgAW872a846u0ZnxTC2W0eAoGhUudnityBuF_WAWBKOW0kamdI82mIg2n1O9J8qnSa00B1bqTcrn-WBoc3m2mQR1fWDkBfUu0sAW86O3j2oYlJtzlpOLw0Em8GzkGwGZWG000000C4EI4S20CMJy_ZNzxROERWF1iWFe881W13VWk0QY13FyvQ81f0GXlRBmFkmufWYa4G3iEZym13f4di8z94q2pYzy18MY1C4c1C6g1Fcfk7drfQweXRW4ySOW1GWe1J76AWKoc26_RQ71kWKZ0B85Qllz8ok1D0LXlssXmRO5S6AzkoZZxpyO_2W5j3ot8S6g1Re1x0MfmN95j0MpDpUlW7O5e4NY1S1a1UG79WNkPsoAwWN2S0NjXBO5y24FUWN0V0NWFImygmle1WJi1ZokkM71hWO0i8O83GmE6GoCJDbP6OtE3XaOMGuDMKrDc5XDM9cPJ0vOcGsq1WXy1ZC0VWOov6QzEkEjxg-0O0PYHbzBw0PgWEm6RWPWGZI6H9vOM9pNtDbSdPbSYzoDJGtBJBe6Ve3y1cT0lWPwFYZ7e4Q__z7xUZkT7-G6e22W820W82019WQyS2q0R0QvEU9YxZqhU1kzHe10000-1gukbw26oKnDZ8sEJKuDpCvDJKsDJSuBJ4nCZKvCZ0uDpSrDZ0mCJ4sE3Cqc1lrp56m6sEu6mI270qtSZawQ4TPQabNUb8twHpmFu0T__z_7W1X6wHJio6jjvI43Q9dIjMKPoNn9y6SfPndGnPTjYVcprobBNWcEXmUeABHKCSONqAEVg_2e4om1R1UwgeesKThiHmJ06btUebOta3L7f_9FSSGBFfJvsgISPJwGPDJ8naAVGO0~1',
                                        actionButton: 'http://an.yandex.ru/count/Wq0ejI_zO0y3jHa0z2ncMG8XsZbYe0K03mGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm0760TW1e9s0d07W0RA9xvG1w06y0m8SRDBHF7n85F02ZCtDW_iPY0NAO905oc2m1SSOk0N76C05jJx81UyFq0MZ352PWdwtyx5NgGV5oJHulTkmERW7W0MQYe21mf211k08yUtJ0e0A2iaAtk9yRD_2-3_u2e2r680BfC9qY0i4gWiGM2KoDCN9002mPT7PjSVe2yfWy0i6cmQO3RYwNk0DYe21c0xGiehqz_Rys5UW3i24FRaEa8u4000000313aX70W35a_Fur_Uss3cu3mR83w220O0GtuBW6eWGp_EMY0QG48Rsoy3xiEAO8f140x3e_C0GwH9x2FIHD0iulV0I5eWJ19WJ1gWJvgRXvzQMkg8Mu1F7680K8A0KnnYe5CfWXlssXmRe58m2o1Mhx_IChWJG5ORzjeS6s1N1YlRieu-y_6Fme1RGyjo71gWMw0Um5gS5oHRG5ipSthu1s1Q15uWN0P0Na1oO5xcTiYke5md05xOIs1V0X3te5m7m5u3qiFAiBw0O4x0OyhhbXmQu60B2620qC3XaCZ4pPMHcDpWuP65aE3LbDJPXOJLYPcKmEM9aDj0O8V0Op07u6CkHclJhZhUwlW606OaPVI-W6Qe3i1cu6O48qXaIUM5YSrzpPN9sPN8lSZKqDoqow1dw0_0PdGBu6UZuenw16l__H-texdH_a1g0We20W820W0IO6l70j06m6kJdYOkuzAtWRlKQ0G000FWQkBfUWXibCJOoDZarE3SpEJKrDZKtE2qnCJ8rEJ8mE3StDJOmC34nDZWpD9WRzSnHi1jZk1i4WXmDDt8vEcX7MMf9LtfID-aSy3-07V__Vnu0OHkaKxCXhRUKX0sYPqhLbcSbyIV1dQMSPqCMNRP7vizShIruBZeS7g32qL37c5z2Zd-lmh1Ci0MmdkggADr7Qx4S4m1gTtgPMDw0rHwVpJt743JwK-Thad6q-a6JMoKP2dq6~1',
                                    },
                                },
                                count_links: {
                                    abuseUrl: '//an.yandex.ru/abuse/WD8ejI_z8EnD1W042sPP0Y4C3vN01G3i036KQ_bW000003ZcYpPmyAQpyiG9a07ucCghqO20W0AO0VYOogjHs06WdO2S0UW1l0EW0kBRfmN91b2PWdwtyx5NgGV5oJHulTkmET070j08W820W0A02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G2v3f2E10313aX70W3O5S6AzkoZZxpyOu4Ny3_u680PWXmDDt8vEcX7MMf9LtfID-aSW1r_201K8DXK9BYKXhDaKCJmBcKEm8h00b80~1',
                                    tracking: 'https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1',
                                    empty: 'https://an.yandex.ru/resource/spacer.gif?',
                                },
                                impId: '2',
                            },
                            direct_data: {
                                targetUrl: 'https://ad.doubleclick.net/ddm/trackclk/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=;ltd=',
                                faviconSizes: {
                                    h: 16,
                                    w: 16,
                                },
                                domain: 'nestlebaby.ru',
                                measurers: '',
                                trackers: {
                                    '0': 'https://ad.doubleclick.net/ddm/trackimp/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;ord=1717109281;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=?',
                                    '1': 'https://1717109281.verify.yandex.ru/verify?platformid=1&msid=msva5b55_5-63579728-10917332540&BID=10917332540&BTYPE=1&CID=63579728&DRND=1717109281&DTYPE=desktop&REF=https%3A%2F%2Ffrontend.vh.yandex.ru%2Fplayer%2F408d213edf788dad85e56aa5bfe09bd6%3Fuse_friendly_frame%3Dtrue%26vsid%3D9188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%26from%3Dya-weather%26reqid%3D1626958739556578-11259208775600116834%26flags%3D%257B%2522player_api_v2%2522%253A%2522false%2522%252C%2522disable_autoplay_slow_connection%2522%253A%2522false%2522%252C%2522disable_autoplay_save_data%2522%253A%2522false%2522%252C%2522poll_timeout%2522%253A%252221600%2522%252C%2522poll_show_before_midroll%2522%253A%2522false%2522%252C%2522version%2522%253A%2522undefined%2522%252C%2522progress%2522%253A%2522true%2522%252C%2522start_position_confirmation%2522%253A%2522false%2522%252C%2522force_unmute%2522%253A%2522true%2522%252C%2522restore_playback_progress%2522%253A%2522false%2522%252C%2522hide_brand_play_button%2522%253A%2522false%2522%257D%26stream_url%3Dhttps%253A%252F%252Fstrm.yandex.ru%252Fkal%252Fweather_moscow%252Fysign1%253Da9f3470057779456717984b3b3295b6c122b621e4efd76d0f8782bab1ce8f824%252CabcID%253D105%252Cfrom%253Dya-weather%252Cpfx%252Cregion%253D10000%252Csfx%252Cts%253D60fabd13%252Fmanifest.mpd%253Ffrom%253Dya-weather%2526partner_id%253D443123%2526target_ref%253Dhttps%25253A%25252F%25252Fyastatic.net%25252Fyandex-video-player-iframe-api%25252Fjs%25252Fplayer-api-adapter-loader.js%2526uuid%253D408d213edf788dad85e56aa5bfe09bd6%2526video_category_id%253D1025%2526clid%253D495%2526yandexuid%253D6319099721604569424%2526slots%253Dnull%2526imp_id%253D1%2526reqid%253D1626958739556578-11259208775600116834%2526content_id%253D408d213edf788dad85e56aa5bfe09bd6%2526from_block%253Dother%2526channel_id%253D1551792230%2526content-genre%253D%2526content-category%253D%2526brand-safety-categories%253D%25255B%25255D%2526sandbox_version%253D0x836c75a4440%26additional_params%3D%257B%2522from%2522%253A%2522ya-weather%2522%252C%2522reqid%2522%253A%25221626958739556578-11259208775600116834%2522%252C%2522vsid%2522%253A%25229188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%2522%252C%2522adsid%2522%253A%2522d4406f3b827c8108d733370b4db28a244117dc985457xWEBx7099x1626958738%2522%252C%2522content_id%2522%253A%2522408d213edf788dad85e56aa5bfe09bd6%2522%252C%2522from_block%2522%253A%2522other%2522%252C%2522channel_id%2522%253A%25221551792230%2522%252C%2522content-genre%2522%253A%2522%2522%252C%2522content-category%2522%253A%2522%2522%252C%2522brand-safety-categories%2522%253A%2522%255B%255D%2522%252C%2522sandbox_version%2522%253A%25220x836c75a4440%2522%257D%26partner_id%3D443123%26category%3D1025%26distr_id%3D0%26video_content_id%3D408d213edf788dad85e56aa5bfe09bd6%26video_content_name%3D%25D0%259F%25D0%25BE%25D0%25B3%25D0%25BE%25D0%25B4%25D0%25B0%2520%25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%26video_genre_name%3D%26preview%3D%252F%252Favatars.mds.yandex.net%252Fget-vh-cv%252F2424227%252F2a0000017ace447bfced2796b7c72daed5fd%252Forig%26host%3Dfrontend.vh.yandex.ru&SESSION=6869341626958739116&hitlogid=4157064062874995141&page=443123',
                                },
                            },
                            constructor_data: {
                                VpaidPcodeUrl: 'https://yastatic.net/awaps-ad-sdk-js/1_0/interactive_viewer.js',
                                Theme: 'video-banner_theme_empty',
                                Duration: 15.0,
                                MediaFiles: [
                                    {
                                        Id: 'https:/strm.yandex.ru/vh-canvas-converted/get-canvas',
                                        Delivery: 'progressive',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0.m3u8',
                                        MimeType: 'application/vnd.apple.mpegurl',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_240p.mp4',
                                        Delivery: 'progressive',
                                        Width: '240',
                                        Height: '240',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_240p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '363',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_240p.webm',
                                        Delivery: 'progressive',
                                        Width: '240',
                                        Height: '240',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_240p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '333',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_360p.mp4',
                                        Delivery: 'progressive',
                                        Width: '360',
                                        Height: '360',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_360p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '507',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_360p.webm',
                                        Delivery: 'progressive',
                                        Width: '360',
                                        Height: '360',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_360p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '467',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_480p.mp4',
                                        Delivery: 'progressive',
                                        Width: '480',
                                        Height: '480',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_480p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '992',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_480p.webm',
                                        Delivery: 'progressive',
                                        Width: '480',
                                        Height: '480',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_480p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '883',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_576p.mp4',
                                        Delivery: 'progressive',
                                        Width: '576',
                                        Height: '576',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_576p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '1953',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_576p.webm',
                                        Delivery: 'progressive',
                                        Width: '576',
                                        Height: '576',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_576p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '1801',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_720p.mp4',
                                        Delivery: 'progressive',
                                        Width: '720',
                                        Height: '720',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_720p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '2848',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_720p.webm',
                                        Delivery: 'progressive',
                                        Width: '720',
                                        Height: '720',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_720p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '2656',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_1080p.mp4',
                                        Delivery: 'progressive',
                                        Width: '1080',
                                        Height: '1080',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_1080p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '3830',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_1080p.webm',
                                        Delivery: 'progressive',
                                        Width: '1080',
                                        Height: '1080',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_1080p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '3918',
                                    },
                                ],
                                HasAbuseButton: true,
                                SocialAdvertisement: false,
                                PlaybackParameters: {
                                    ShowSkipButton: true,
                                    SkipDelay: '5',
                                },
                                UseTrackingEvents: true,
                                IsStock: true,
                                AdditionElements: [
                                    {
                                        Type: 'TITLE',
                                        Options: {
                                            BackgroundColor: '#000000',
                                            TextColor: '#ffffff',
                                        },
                                    },
                                    {
                                        Type: 'BODY',
                                        Options: {
                                            BackgroundColor: '#000000',
                                            TextColor: '#ffffff',
                                        },
                                    },
                                    {
                                        Type: 'DOMAIN',
                                        Options: {},
                                    },
                                    {
                                        Type: 'BUTTON',
                                        Options: {
                                            TextColor: '#000000',
                                            Color: '#FF0000',
                                        },
                                    },
                                    {
                                        Type: 'DISCLAIMER',
                                        Options: {
                                            BackgroundColor: '#000000',
                                            TextColor: '#ffffff',
                                            Text: '',
                                        },
                                    },
                                    {
                                        Type: 'AGE',
                                        Options: {
                                            BackgroundColor: '#000000',
                                            TextColor: '#ffffff',
                                            Text: '',
                                        },
                                    },
                                    {
                                        Type: 'LEGAL',
                                        Options: {
                                            BackgroundColor: '#000000',
                                            TextColor: '#ffffff',
                                            Text: 'bla-bla-legal',
                                        },
                                    },
                                ],
                                InteractiveVpaid: false,
                                AddPixelImpression: true,
                                CreativeId: '2148470331',
                                StrmPrefix: 'video_5f982fb15cffc68d5e867ca0',
                                ShowVpaid: true,
                                ShowVideoClicks: true,
                                SoundbtnLayout: '1',
                                AdlabelLayout: '1',
                                CountdownLayout: '1',
                                UseVpaidImpressions: false,
                                AdSystem: 'Yabs Ad CPC Server',
                                PythiaParams: {
                                    Extra: 'PythiaParams.Extra.${AUCTION_BID_ID}',
                                },
                            },
                        },
                    },
                },
            }],
        };
        const expectedVast = normalizeVast(`
            <?xml version="1.0" encoding="UTF-8" ?>
            <VAST version="3.0" ya-uniformat="true">
                <Ad id="a34sdf">
                    <InLine>
                        <AdSystem>Yabs Ad CPC Server</AdSystem>
                        <AdTitle>Interactive Direct In Video</AdTitle>
                        <Impression id="direct_impression_13"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=13]]></Impression>
                        <Error>
                            <![CDATA[https://an.yandex.ru/jserr/3948?errmsg=auto-video-error]]>
                        </Error>
                        <Creatives>
                            <Creative id="2148470331">
                                <Linear skipoffset="00:00:05">
                                    <Duration>00:00:15</Duration>
                                    <TrackingEvents>
                                        <Tracking event="start">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=0]]>
                                        </Tracking>
                                        <Tracking event="firstQuartile">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=1]]>
                                        </Tracking>
                                        <Tracking event="midpoint">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=2]]>
                                        </Tracking>
                                        <Tracking event="thirdQuartile">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=3]]>
                                        </Tracking>
                                        <Tracking event="complete">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=4]]>
                                        </Tracking>
                                        <Tracking event="mute">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=5]]>
                                        </Tracking>
                                        <Tracking event="unmute">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=6]]>
                                        </Tracking>
                                        <Tracking event="pause">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=7]]>
                                        </Tracking>
                                        <Tracking event="resume">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=8]]>
                                        </Tracking>
                                        <Tracking event="skip">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=9]]>
                                        </Tracking>
                                    </TrackingEvents>
                                    <MediaFiles>
                                        <MediaFile width="0" height="0" delivery="progressive" type="application/javascript"
                                                apiFramework="VPAID">
                                            <![CDATA[https://yastatic.net/awaps-ad-sdk-js/1_0/interactive_viewer.js]]>
                                        </MediaFile>
                                    </MediaFiles>
                                    <VideoClicks>
                                        <ClickThrough>http://an.yandex.ru/count/WpyejI_zO0u3hHa0v2ncMG8XP8gx9GK03WGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm042s06WdO2S0U01iedlb07e0Rm30Xniqj4yV4WKy0ACpSs3-nc81SfWa0NAOB05nnYu1SSOm0MrFiW5xm_G1QCCK9c2VhVpiLUf1yN9D7Yzsx0vk0U01PgAW872a846u0ZnxTC2W0eAoGhUudnityBuF_WAWBKOW0kamdI82mIg2n1O9J8qnSa00B1bqTcrn-WBoc3m2mQR1fWDkBfUu0sAW86O3j2oYlJtzlpOLw0Em8GzkGwGZWG000000C4EI4S20CMJy_ZNzxROERWF1iWFe881W13VWk0QY13FyvQ81f0GXlRBmFkmufWYa4G3iEZym13f4di8z94q2pYzy18MY1C4c1C6g1Fcfk7drfQweXRW4ySOW1GWe1J76AWKoc26_RQ71kWKZ0B85Qllz8ok1D0LXlssXmRO5S6AzkoZZxpyO_2W5j3ot8S6g1Re1x0MfmN95j0MpDpUlW7O5e4NY1S1a1UG79WNkPsoAwWN2S0NjXBO5y24FUWN0V0NWFImygmle1WJi1ZokkM71hWO0i8O83GmE6GoCJDbP6OtE3XaOMGuDMKrDc5XDM9cPJ0vOcGsq1WXy1ZC0VWOov6QzEkEjxg-0O0PYHbzBw0PgWEm6RWPWGZI6H9vOM9pNtDbSdPbSYzoDJGtBJBe6Ve3y1cT0lWPwFYZ7e4Q__z7xUZkT7-G6e22W820W82019WQyS2q0R0QvEU9YxZqhU1kzHe10000-1gukbw26oKnDZ8sEJKuDpCvDJKsDJSuBJ4nCZKvCZ0uDpSrDZ0mCJ4sE3Cqc1lrp56m6sEu6mI270qtSZawQ4TPQabNUb8twHpmFu0T__z_7W1X6wHJio6jjvI43Q9dIjMKPoNn9y6SfPndGnPTjYVcprobBNWcEXmUeABHKCSONqAEVg_2e4om1R1UwgeesKThiHmJ06btUebOta3L7f_9FSSGBFfJvsgISPJwGPDJ8naAVGO0~1</ClickThrough>
                                    </VideoClicks>
                                    <AdParameters>
                                        <![CDATA[
                                        {
                                            "HAS_BUTTON": true,
                                            "BUTTON_TEXT_COLOR": "#000000",
                                            "HAS_DOMAIN": true,
                                            "HAS_TITLE": true,
                                            "HAS_BODY": true,
                                            "HAS_AGE": true,
                                            "HAS_LEGAL": true,
                                            "HAS_DISCLAIMER": true,
                                            "AGE_BACKGROUND_COLOR": "#000000",
                                            "AGE_TEXT": "",
                                            "AGE_TEXT_COLOR": "#ffffff",
                                            "BODY_BACKGROUND_COLOR": "#000000",
                                            "BODY_TEXT_COLOR": "#ffffff",
                                            "BUTTON_COLOR": "#FF0000",
                                            "DISCLAIMER_BACKGROUND_COLOR": "#000000",
                                            "DISCLAIMER_TEXT": "",
                                            "DISCLAIMER_TEXT_COLOR": "#ffffff",
                                            "LEGAL_BACKGROUND_COLOR": "#000000",
                                            "LEGAL_TEXT": "bla-bla-legal",
                                            "LEGAL_TEXT_COLOR": "#ffffff",
                                            "TITLE_BACKGROUND_COLOR": "#000000",
                                            "TITLE_TEXT_COLOR": "#ffffff",
                                            "theme": "video-banner_interactive-viewer",
                                            "duration": 15.0,
                                            "mediaFiles": [
                                                {
                                                    "bitrate": null,
                                                    "delivery": "progressive",
                                                    "height": null,
                                                    "id": "https:/strm.yandex.ru/vh-canvas-converted/get-canvas",
                                                    "type": "application/vnd.apple.mpegurl",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0.m3u8",
                                                    "width": null
                                                },
                                                {
                                                    "bitrate": 363,
                                                    "delivery": "progressive",
                                                    "height": 240,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_240p.mp4",
                                                    "width": 240,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_240p.mp4"
                                                },
                                                {
                                                    "bitrate": 333,
                                                    "delivery": "progressive",
                                                    "height": 240,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_240p.webm",
                                                    "width": 240,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_240p.webm"
                                                },
                                                {
                                                    "bitrate": 507,
                                                    "delivery": "progressive",
                                                    "height": 360,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_360p.mp4",
                                                    "width": 360,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_360p.mp4"
                                                },
                                                {
                                                    "bitrate": 467,
                                                    "delivery": "progressive",
                                                    "height": 360,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_360p.webm",
                                                    "width": 360,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_360p.webm"
                                                },
                                                {
                                                    "bitrate": 992,
                                                    "delivery": "progressive",
                                                    "height": 480,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_480p.mp4",
                                                    "width": 480,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_480p.mp4"
                                                },
                                                {
                                                    "bitrate": 883,
                                                    "delivery": "progressive",
                                                    "height": 480,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_480p.webm",
                                                    "width": 480,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_480p.webm"
                                                },
                                                {
                                                    "bitrate": 1953,
                                                    "delivery": "progressive",
                                                    "height": 576,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_576p.mp4",
                                                    "width": 576,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_576p.mp4"
                                                },
                                                {
                                                    "bitrate": 1801,
                                                    "delivery": "progressive",
                                                    "height": 576,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_576p.webm",
                                                    "width": 576,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_576p.webm"
                                                },
                                                {
                                                    "bitrate": 2848,
                                                    "delivery": "progressive",
                                                    "height": 720,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_720p.mp4",
                                                    "width": 720,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_720p.mp4"
                                                },
                                                {
                                                    "bitrate": 2656,
                                                    "delivery": "progressive",
                                                    "height": 720,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_720p.webm",
                                                    "width": 720,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_720p.webm"
                                                },
                                                {
                                                    "bitrate": 3830,
                                                    "delivery": "progressive",
                                                    "height": 1080,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_1080p.mp4",
                                                    "width": 1080,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_1080p.mp4"
                                                },
                                                {
                                                    "bitrate": 3918,
                                                    "delivery": "progressive",
                                                    "height": 1080,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_1080p.webm",
                                                    "width": 1080,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_1080p.webm"
                                                }
                                            ],
                                            "socialAdvertising": false,
                                            "AUCTION_DC_PARAMS": {
                                                "sad":"sad",
                                                "creative_params": {
                                                    "crypta_user_gender":"1",
                                                    "crypta_user_age":"2"
                                                },
                                                "data_params": {
                                                    "72057604955260476": {
                                                        "target_url":"https://ad.doubleclick.net/ddm/trackclk/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=;ltd=",
                                                        "unmoderated": {
                                                            "faviconHeight":"16",
                                                            "punyDomain":"nestlebaby.ru",
                                                            "faviconWidth":"16",
                                                            "warning":""
                                                        },
                                                        "count":"https://an.yandex.ru/resource/spacer.gif?",
                                                        "click_url": {
                                                            "text_name":"http://an.yandex.ru/count/WpyejI_zO0u3hHa0v2ncMG8XP8gx9GK03WGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm042s06WdO2S0U01iedlb07e0Rm30Xniqj4yV4WKy0ACpSs3-nc81SfWa0NAOB05nnYu1SSOm0MrFiW5xm_G1QCCK9c2VhVpiLUf1yN9D7Yzsx0vk0U01PgAW872a846u0ZnxTC2W0eAoGhUudnityBuF_WAWBKOW0kamdI82mIg2n1O9J8qnSa00B1bqTcrn-WBoc3m2mQR1fWDkBfUu0sAW86O3j2oYlJtzlpOLw0Em8GzkGwGZWG000000C4EI4S20CMJy_ZNzxROERWF1iWFe881W13VWk0QY13FyvQ81f0GXlRBmFkmufWYa4G3iEZym13f4di8z94q2pYzy18MY1C4c1C6g1Fcfk7drfQweXRW4ySOW1GWe1J76AWKoc26_RQ71kWKZ0B85Qllz8ok1D0LXlssXmRO5S6AzkoZZxpyO_2W5j3ot8S6g1Re1x0MfmN95j0MpDpUlW7O5e4NY1S1a1UG79WNkPsoAwWN2S0NjXBO5y24FUWN0V0NWFImygmle1WJi1ZokkM71hWO0i8O83GmE6GoCJDbP6OtE3XaOMGuDMKrDc5XDM9cPJ0vOcGsq1WXy1ZC0VWOov6QzEkEjxg-0O0PYHbzBw0PgWEm6RWPWGZI6H9vOM9pNtDbSdPbSYzoDJGtBJBe6Ve3y1cT0lWPwFYZ7e4Q__z7xUZkT7-G6e22W820W82019WQyS2q0R0QvEU9YxZqhU1kzHe10000-1gukbw26oKnDZ8sEJKuDpCvDJKsDJSuBJ4nCZKvCZ0uDpSrDZ0mCJ4sE3Cqc1lrp56m6sEu6mI270qtSZawQ4TPQabNUb8twHpmFu0T__z_7W1X6wHJio6jjvI43Q9dIjMKPoNn9y6SfPndGnPTjYVcprobBNWcEXmUeABHKCSONqAEVg_2e4om1R1UwgeesKThiHmJ06btUebOta3L7f_9FSSGBFfJvsgISPJwGPDJ8naAVGO0~1",
                                                            "action_button":"http://an.yandex.ru/count/Wq0ejI_zO0y3jHa0z2ncMG8XsZbYe0K03mGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm0760TW1e9s0d07W0RA9xvG1w06y0m8SRDBHF7n85F02ZCtDW_iPY0NAO905oc2m1SSOk0N76C05jJx81UyFq0MZ352PWdwtyx5NgGV5oJHulTkmERW7W0MQYe21mf211k08yUtJ0e0A2iaAtk9yRD_2-3_u2e2r680BfC9qY0i4gWiGM2KoDCN9002mPT7PjSVe2yfWy0i6cmQO3RYwNk0DYe21c0xGiehqz_Rys5UW3i24FRaEa8u4000000313aX70W35a_Fur_Uss3cu3mR83w220O0GtuBW6eWGp_EMY0QG48Rsoy3xiEAO8f140x3e_C0GwH9x2FIHD0iulV0I5eWJ19WJ1gWJvgRXvzQMkg8Mu1F7680K8A0KnnYe5CfWXlssXmRe58m2o1Mhx_IChWJG5ORzjeS6s1N1YlRieu-y_6Fme1RGyjo71gWMw0Um5gS5oHRG5ipSthu1s1Q15uWN0P0Na1oO5xcTiYke5md05xOIs1V0X3te5m7m5u3qiFAiBw0O4x0OyhhbXmQu60B2620qC3XaCZ4pPMHcDpWuP65aE3LbDJPXOJLYPcKmEM9aDj0O8V0Op07u6CkHclJhZhUwlW606OaPVI-W6Qe3i1cu6O48qXaIUM5YSrzpPN9sPN8lSZKqDoqow1dw0_0PdGBu6UZuenw16l__H-texdH_a1g0We20W820W0IO6l70j06m6kJdYOkuzAtWRlKQ0G000FWQkBfUWXibCJOoDZarE3SpEJKrDZKtE2qnCJ8rEJ8mE3StDJOmC34nDZWpD9WRzSnHi1jZk1i4WXmDDt8vEcX7MMf9LtfID-aSy3-07V__Vnu0OHkaKxCXhRUKX0sYPqhLbcSbyIV1dQMSPqCMNRP7vizShIruBZeS7g32qL37c5z2Zd-lmh1Ci0MmdkggADr7Qx4S4m1gTtgPMDw0rHwVpJt743JwK-Thad6q-a6JMoKP2dq6~1"
                                                        },
                                                        "text": {
                                                            "banner_flags":"milk-substitute,animated",
                                                            "domain":"nestlebaby.ru",
                                                            "lang":"1",
                                                            "warning":"",
                                                            "age":""
                                                        },
                                                        "assets": {
                                                            "button": {}
                                                        },
                                                        "object_id":"72057604955260476"
                                                    },
                                                    "misc": {
                                                        "target_url":"http://ru.yandex.auto-video",
                                                        "unmoderated": {},
                                                        "click_url": {
                                                            "abuse":"//an.yandex.ru/abuse/WD8ejI_z8EnD1W042sPP0Y4C3vN01G3i036KQ_bW000003ZcYpPmyAQpyiG9a07ucCghqO20W0AO0VYOogjHs06WdO2S0UW1l0EW0kBRfmN91b2PWdwtyx5NgGV5oJHulTkmET070j08W820W0A02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G2v3f2E10313aX70W3O5S6AzkoZZxpyOu4Ny3_u680PWXmDDt8vEcX7MMf9LtfID-aSW1r_201K8DXK9BYKXhDaKCJmBcKEm8h00b80~1"
                                                        },
                                                        "trackers": [
                                                            "https://ad.doubleclick.net/ddm/trackimp/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;ord=1717109281;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=?",
                                                            "https://1717109281.verify.yandex.ru/verify?platformid=1&msid=msva5b55_5-63579728-10917332540&BID=10917332540&BTYPE=1&CID=63579728&DRND=1717109281&DTYPE=desktop&REF=https%3A%2F%2Ffrontend.vh.yandex.ru%2Fplayer%2F408d213edf788dad85e56aa5bfe09bd6%3Fuse_friendly_frame%3Dtrue%26vsid%3D9188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%26from%3Dya-weather%26reqid%3D1626958739556578-11259208775600116834%26flags%3D%257B%2522player_api_v2%2522%253A%2522false%2522%252C%2522disable_autoplay_slow_connection%2522%253A%2522false%2522%252C%2522disable_autoplay_save_data%2522%253A%2522false%2522%252C%2522poll_timeout%2522%253A%252221600%2522%252C%2522poll_show_before_midroll%2522%253A%2522false%2522%252C%2522version%2522%253A%2522undefined%2522%252C%2522progress%2522%253A%2522true%2522%252C%2522start_position_confirmation%2522%253A%2522false%2522%252C%2522force_unmute%2522%253A%2522true%2522%252C%2522restore_playback_progress%2522%253A%2522false%2522%252C%2522hide_brand_play_button%2522%253A%2522false%2522%257D%26stream_url%3Dhttps%253A%252F%252Fstrm.yandex.ru%252Fkal%252Fweather_moscow%252Fysign1%253Da9f3470057779456717984b3b3295b6c122b621e4efd76d0f8782bab1ce8f824%252CabcID%253D105%252Cfrom%253Dya-weather%252Cpfx%252Cregion%253D10000%252Csfx%252Cts%253D60fabd13%252Fmanifest.mpd%253Ffrom%253Dya-weather%2526partner_id%253D443123%2526target_ref%253Dhttps%25253A%25252F%25252Fyastatic.net%25252Fyandex-video-player-iframe-api%25252Fjs%25252Fplayer-api-adapter-loader.js%2526uuid%253D408d213edf788dad85e56aa5bfe09bd6%2526video_category_id%253D1025%2526clid%253D495%2526yandexuid%253D6319099721604569424%2526slots%253Dnull%2526imp_id%253D1%2526reqid%253D1626958739556578-11259208775600116834%2526content_id%253D408d213edf788dad85e56aa5bfe09bd6%2526from_block%253Dother%2526channel_id%253D1551792230%2526content-genre%253D%2526content-category%253D%2526brand-safety-categories%253D%25255B%25255D%2526sandbox_version%253D0x836c75a4440%26additional_params%3D%257B%2522from%2522%253A%2522ya-weather%2522%252C%2522reqid%2522%253A%25221626958739556578-11259208775600116834%2522%252C%2522vsid%2522%253A%25229188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%2522%252C%2522adsid%2522%253A%2522d4406f3b827c8108d733370b4db28a244117dc985457xWEBx7099x1626958738%2522%252C%2522content_id%2522%253A%2522408d213edf788dad85e56aa5bfe09bd6%2522%252C%2522from_block%2522%253A%2522other%2522%252C%2522channel_id%2522%253A%25221551792230%2522%252C%2522content-genre%2522%253A%2522%2522%252C%2522content-category%2522%253A%2522%2522%252C%2522brand-safety-categories%2522%253A%2522%255B%255D%2522%252C%2522sandbox_version%2522%253A%25220x836c75a4440%2522%257D%26partner_id%3D443123%26category%3D1025%26distr_id%3D0%26video_content_id%3D408d213edf788dad85e56aa5bfe09bd6%26video_content_name%3D%25D0%259F%25D0%25BE%25D0%25B3%25D0%25BE%25D0%25B4%25D0%25B0%2520%25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%26video_genre_name%3D%26preview%3D%252F%252Favatars.mds.yandex.net%252Fget-vh-cv%252F2424227%252F2a0000017ace447bfced2796b7c72daed5fd%252Forig%26host%3Dfrontend.vh.yandex.ru&SESSION=6869341626958739116&hitlogid=4157064062874995141&page=443123"
                                                        ],
                                                        "object_id":"72057604955260476",
                                                        "impId": "2"
                                                    }
                                                }
                                            },
                                            "playbackParameters": {
                                                "showSkipButton": true,
                                                "skipDelay": 5
                                            },
                                            "pythia": {
                                                "extra": "PythiaParams.Extra.1231313124"
                                            },
                                            "encounters": [
                                                "https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=14",
                                                "https://an.yandex.ru/rtbcount/1K-WJkUj0Tm100000000U9nJ39ZPLBVzyyPmmwCdPUhttjvWQJab-sS20n1umaH25PaYqs8_7s9ZI6K4YcSUFMbI0H8lPGBoQgy2YLP643cJKMKHAqWdmqB62NqiODPAHWzt9Z1hB-DY0tw6es3-LKQGGNSP6UOmCFnblFTiczDS9WwWo5cc_q3mYacWTdNP-QCd6HXE_BbmS-jnLeQ_J223Mvb1P2-p8f2Soim59ESoWmnVoSpbeC00Cc0ZyvflQwild0s9FyaCyzbLPh1bp6n0yYbp0Fi1pfVC2evWDp7yP7PmuWUpDh0mxc1XFi32V9AbckFBH1DkYxqNM9ujU_ZyshHTYyHkGdp3yC7-8PJrmFuj2yWh2rWvLx0sVDdmQOt-9bO54mEB52D3uzIKD7CBYvJ450lJL5Epn4JJfBHK0yka5BC3h6V-_OS0nfEijVeGiowmAdnbPGGQ_8kLqz3CsDJSqjp8ee7jGb-3tmIs2vkwldp_zkT6-0osZmUsC2vWU_Ayitl7bxKFybQomPnoW2tv11lB8pPUyo9Bgmpto2pFyAeWSm3SDg08",
                                                "https://ad.doubleclick.net/ddm/trackimp/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;ord=1717109281;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=?",
                                                "https://1717109281.verify.yandex.ru/verify?platformid=1&msid=msva5b55_5-63579728-10917332540&BID=10917332540&BTYPE=1&CID=63579728&DRND=1717109281&DTYPE=desktop&REF=https%3A%2F%2Ffrontend.vh.yandex.ru%2Fplayer%2F408d213edf788dad85e56aa5bfe09bd6%3Fuse_friendly_frame%3Dtrue%26vsid%3D9188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%26from%3Dya-weather%26reqid%3D1626958739556578-11259208775600116834%26flags%3D%257B%2522player_api_v2%2522%253A%2522false%2522%252C%2522disable_autoplay_slow_connection%2522%253A%2522false%2522%252C%2522disable_autoplay_save_data%2522%253A%2522false%2522%252C%2522poll_timeout%2522%253A%252221600%2522%252C%2522poll_show_before_midroll%2522%253A%2522false%2522%252C%2522version%2522%253A%2522undefined%2522%252C%2522progress%2522%253A%2522true%2522%252C%2522start_position_confirmation%2522%253A%2522false%2522%252C%2522force_unmute%2522%253A%2522true%2522%252C%2522restore_playback_progress%2522%253A%2522false%2522%252C%2522hide_brand_play_button%2522%253A%2522false%2522%257D%26stream_url%3Dhttps%253A%252F%252Fstrm.yandex.ru%252Fkal%252Fweather_moscow%252Fysign1%253Da9f3470057779456717984b3b3295b6c122b621e4efd76d0f8782bab1ce8f824%252CabcID%253D105%252Cfrom%253Dya-weather%252Cpfx%252Cregion%253D10000%252Csfx%252Cts%253D60fabd13%252Fmanifest.mpd%253Ffrom%253Dya-weather%2526partner_id%253D443123%2526target_ref%253Dhttps%25253A%25252F%25252Fyastatic.net%25252Fyandex-video-player-iframe-api%25252Fjs%25252Fplayer-api-adapter-loader.js%2526uuid%253D408d213edf788dad85e56aa5bfe09bd6%2526video_category_id%253D1025%2526clid%253D495%2526yandexuid%253D6319099721604569424%2526slots%253Dnull%2526imp_id%253D1%2526reqid%253D1626958739556578-11259208775600116834%2526content_id%253D408d213edf788dad85e56aa5bfe09bd6%2526from_block%253Dother%2526channel_id%253D1551792230%2526content-genre%253D%2526content-category%253D%2526brand-safety-categories%253D%25255B%25255D%2526sandbox_version%253D0x836c75a4440%26additional_params%3D%257B%2522from%2522%253A%2522ya-weather%2522%252C%2522reqid%2522%253A%25221626958739556578-11259208775600116834%2522%252C%2522vsid%2522%253A%25229188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%2522%252C%2522adsid%2522%253A%2522d4406f3b827c8108d733370b4db28a244117dc985457xWEBx7099x1626958738%2522%252C%2522content_id%2522%253A%2522408d213edf788dad85e56aa5bfe09bd6%2522%252C%2522from_block%2522%253A%2522other%2522%252C%2522channel_id%2522%253A%25221551792230%2522%252C%2522content-genre%2522%253A%2522%2522%252C%2522content-category%2522%253A%2522%2522%252C%2522brand-safety-categories%2522%253A%2522%255B%255D%2522%252C%2522sandbox_version%2522%253A%25220x836c75a4440%2522%257D%26partner_id%3D443123%26category%3D1025%26distr_id%3D0%26video_content_id%3D408d213edf788dad85e56aa5bfe09bd6%26video_content_name%3D%25D0%259F%25D0%25BE%25D0%25B3%25D0%25BE%25D0%25B4%25D0%25B0%2520%25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%26video_genre_name%3D%26preview%3D%252F%252Favatars.mds.yandex.net%252Fget-vh-cv%252F2424227%252F2a0000017ace447bfced2796b7c72daed5fd%252Forig%26host%3Dfrontend.vh.yandex.ru&SESSION=6869341626958739116&hitlogid=4157064062874995141&page=443123"
                                            ],
                                            "trackingEvents": {
                                            "start": ["https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=11"],
                                            "trueView": ["https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=19"],
                                            "returnAfterClickThrough": ["http://an.yandex.ru/count/WpyejI_zO0u3hHa0v2ncMG8XP8gx9GK03WGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm042s06WdO2S0U01iedlb07e0Rm30Xniqj4yV4WKy0ACpSs3-nc81SfWa0NAOB05nnYu1SSOm0MrFiW5xm_G1QCCK9c2VhVpiLUf1yN9D7Yzsx0vk0U01PgAW872a846u0ZnxTC2W0eAoGhUudnityBuF_WAWBKOW0kamdI82mIg2n1O9J8qnSa00B1bqTcrn-WBoc3m2mQR1fWDkBfUu0sAW86O3j2oYlJtzlpOLw0Em8GzkGwGZWG000000C4EI4S20CMJy_ZNzxROERWF1iWFe881W13VWk0QY13FyvQ81f0GXlRBmFkmufWYa4G3iEZym13f4di8z94q2pYzy18MY1C4c1C6g1Fcfk7drfQweXRW4ySOW1GWe1J76AWKoc26_RQ71kWKZ0B85Qllz8ok1D0LXlssXmRO5S6AzkoZZxpyO_2W5j3ot8S6g1Re1x0MfmN95j0MpDpUlW7O5e4NY1S1a1UG79WNkPsoAwWN2S0NjXBO5y24FUWN0V0NWFImygmle1WJi1ZokkM71hWO0i8O83GmE6GoCJDbP6OtE3XaOMGuDMKrDc5XDM9cPJ0vOcGsq1WXy1ZC0VWOov6QzEkEjxg-0O0PYHbzBw0PgWEm6RWPWGZI6H9vOM9pNtDbSdPbSYzoDJGtBJBe6Ve3y1cT0lWPwFYZ7e4Q__z7xUZkT7-G6e22W820W82019WQyS2q0R0QvEU9YxZqhU1kzHe10000-1gukbw26oKnDZ8sEJKuDpCvDJKsDJSuBJ4nCZKvCZ0uDpSrDZ0mCJ4sE3Cqc1lrp56m6sEu6mI270qtSZawQ4TPQabNUb8twHpmFu0T__z_7W1X6wHJio6jjvI43Q9dIjMKPoNn9y6SfPndGnPTjYVcprobBNWcEXmUeABHKCSONqAEVg_2e4om1R1UwgeesKThiHmJ06btUebOta3L7f_9FSSGBFfJvsgISPJwGPDJ8naAVGO0~1?test-tag=136"],
                                            "showHp": ["https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=21"],
                                            "clickHp": ["https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=22"],
                                            "adsFinish": ["https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=24"]
                                            },
                                            "isStock": true
                                        }
                                        ]]>
                                    </AdParameters>
                                </Linear>
                                <CreativeExtensions>
                                    <CreativeExtension type="meta">
                                        <yastrm:prefix xmlns:yastrm="http://strm.yandex.ru/schema/vast">
                                            <![CDATA[video_5f982fb15cffc68d5e867ca0]]>
                                        </yastrm:prefix>
                                    </CreativeExtension>
                                </CreativeExtensions>
                            </Creative>
                        </Creatives>
                        <Extensions>
                            <Extension type="controls">
                                <control id="adlabel" layout="1"/>
                                <control id="countdown" layout="1"/>
                                <control id="soundbtn" layout="1"/>
                            </Extension>
                            <Extension type="CustomTracking">
                                <Tracking event="onCreativeInit">
                                    <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=11]]>
                                </Tracking>
                            </Extension>
                        </Extensions>
                    </InLine>
                </Ad>
            </VAST>
        `);
        const resultVast = normalizeVast(videoAdapter(bsMeta));

        expect(resultVast).toEqual(expectedVast);
    });

    it('should return Ad Wrapper', () => {
        const bsMeta: BSMetaVideo = {
            wrapperUrl: 'https://path-to-vast',
            adBlockId: '123',
            bidId: '1231313123',
            bids: [],
        };
        const expectedVast = normalizeVast(`
            <?xml version="1.0" encoding="UTF-8" ?>
            <VAST version="3.0" ya-uniformat="true">
                <Ad id="123">
                    <Wrapper>
                        <AdSystem>Yabs Ad Server</AdSystem>
                        <VASTAdTagURI><![CDATA[https://path-to-vast]]></VASTAdTagURI>
                    </Wrapper>
                </Ad>
            </VAST>
        `);
        const resultVast = normalizeVast(videoAdapter(bsMeta));

        expect(resultVast).toEqual(expectedVast);
    });

    it('should return multiple Ads', () => {
        const bsMeta: BSMetaVideo = {
            wrapperUrl: 'https://path-to-vast',
            adBlockId: '777',
            bidId: '1231313123',
            bids: [{
                settings: {
                    '2': {
                        linkTail: 'https://an.yandex.ru/rtbcount/1K-WJkUj0Tm100000000U9nJ39ZPLBVzyyPmmwCdPUhttjvWQJab-sS20n1umaH25PaYqs8_7s9ZI6K4YcSUFMbI0H8lPGBoQgy2YLP643cJKMKHAqWdmqB62NqiODPAHWzt9Z1hB-DY0tw6es3-LKQGGNSP6UOmCFnblFTiczDS9WwWo5cc_q3mYacWTdNP-QCd6HXE_BbmS-jnLeQ_J223Mvb1P2-p8f2Soim59ESoWmnVoSpbeC00Cc0ZyvflQwild0s9FyaCyzbLPh1bp6n0yYbp0Fi1pfVC2evWDp7yP7PmuWUpDh0mxc1XFi32V9AbckFBH1DkYxqNM9ujU_ZyshHTYyHkGdp3yC7-8PJrmFuj2yWh2rWvLx0sVDdmQOt-9bO54mEB52D3uzIKD7CBYvJ450lJL5Epn4JJfBHK0yka5BC3h6V-_OS0nfEijVeGiowmAdnbPGGQ_8kLqz3CsDJSqjp8ee7jGb-3tmIs2vkwldp_zkT6-0osZmUsC2vWU_Ayitl7bxKFybQomPnoW2tv11lB8pPUyo9Bgmpto2pFyAeWSm3SDg08',
                        viewNotices: ['https://ad.doubleclick.net/ddm/trackimp/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;ord=1717109281;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=?', 'https://1717109281.verify.yandex.ru/verify?platformid=1&msid=msva5b55_5-63579728-10917332540&BID=10917332540&BTYPE=1&CID=63579728&DRND=1717109281&DTYPE=desktop&REF=https%3A%2F%2Ffrontend.vh.yandex.ru%2Fplayer%2F408d213edf788dad85e56aa5bfe09bd6%3Fuse_friendly_frame%3Dtrue%26vsid%3D9188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%26from%3Dya-weather%26reqid%3D1626958739556578-11259208775600116834%26flags%3D%257B%2522player_api_v2%2522%253A%2522false%2522%252C%2522disable_autoplay_slow_connection%2522%253A%2522false%2522%252C%2522disable_autoplay_save_data%2522%253A%2522false%2522%252C%2522poll_timeout%2522%253A%252221600%2522%252C%2522poll_show_before_midroll%2522%253A%2522false%2522%252C%2522version%2522%253A%2522undefined%2522%252C%2522progress%2522%253A%2522true%2522%252C%2522start_position_confirmation%2522%253A%2522false%2522%252C%2522force_unmute%2522%253A%2522true%2522%252C%2522restore_playback_progress%2522%253A%2522false%2522%252C%2522hide_brand_play_button%2522%253A%2522false%2522%257D%26stream_url%3Dhttps%253A%252F%252Fstrm.yandex.ru%252Fkal%252Fweather_moscow%252Fysign1%253Da9f3470057779456717984b3b3295b6c122b621e4efd76d0f8782bab1ce8f824%252CabcID%253D105%252Cfrom%253Dya-weather%252Cpfx%252Cregion%253D10000%252Csfx%252Cts%253D60fabd13%252Fmanifest.mpd%253Ffrom%253Dya-weather%2526partner_id%253D443123%2526target_ref%253Dhttps%25253A%25252F%25252Fyastatic.net%25252Fyandex-video-player-iframe-api%25252Fjs%25252Fplayer-api-adapter-loader.js%2526uuid%253D408d213edf788dad85e56aa5bfe09bd6%2526video_category_id%253D1025%2526clid%253D495%2526yandexuid%253D6319099721604569424%2526slots%253Dnull%2526imp_id%253D1%2526reqid%253D1626958739556578-11259208775600116834%2526content_id%253D408d213edf788dad85e56aa5bfe09bd6%2526from_block%253Dother%2526channel_id%253D1551792230%2526content-genre%253D%2526content-category%253D%2526brand-safety-categories%253D%25255B%25255D%2526sandbox_version%253D0x836c75a4440%26additional_params%3D%257B%2522from%2522%253A%2522ya-weather%2522%252C%2522reqid%2522%253A%25221626958739556578-11259208775600116834%2522%252C%2522vsid%2522%253A%25229188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%2522%252C%2522adsid%2522%253A%2522d4406f3b827c8108d733370b4db28a244117dc985457xWEBx7099x1626958738%2522%252C%2522content_id%2522%253A%2522408d213edf788dad85e56aa5bfe09bd6%2522%252C%2522from_block%2522%253A%2522other%2522%252C%2522channel_id%2522%253A%25221551792230%2522%252C%2522content-genre%2522%253A%2522%2522%252C%2522content-category%2522%253A%2522%2522%252C%2522brand-safety-categories%2522%253A%2522%255B%255D%2522%252C%2522sandbox_version%2522%253A%25220x836c75a4440%2522%257D%26partner_id%3D443123%26category%3D1025%26distr_id%3D0%26video_content_id%3D408d213edf788dad85e56aa5bfe09bd6%26video_content_name%3D%25D0%259F%25D0%25BE%25D0%25B3%25D0%25BE%25D0%25B4%25D0%25B0%2520%25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%26video_genre_name%3D%26preview%3D%252F%252Favatars.mds.yandex.net%252Fget-vh-cv%252F2424227%252F2a0000017ace447bfced2796b7c72daed5fd%252Forig%26host%3Dfrontend.vh.yandex.ru&SESSION=6869341626958739116&hitlogid=4157064062874995141&page=443123'],
                    },
                },
                dc_params: {
                    creative_params: {
                        crypta_user_gender: '1',
                        crypta_user_age: '2',
                    },
                    data_params: {
                        '72057604955260476': {
                            bs_data: {
                                sad: 'sad',
                                targetUrl: 'https://ad.doubleclick.net/ddm/trackclk/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=;ltd=',
                                domain: 'nestlebaby.ru',
                                bannerFlags: 'milk-substitute,animated',
                                bannerLang: '1',
                                adId: '72057604955260476',
                                actionButton: 'http://an.yandex.ru/count/Wq0ejI_zO0y3jHa0z2ncMG8XsZbYe0K03mGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm0760TW1e9s0d07W0RA9xvG1w06y0m8SRDBHF7n85F02ZCtDW_iPY0NAO905oc2m1SSOk0N76C05jJx81UyFq0MZ352PWdwtyx5NgGV5oJHulTkmERW7W0MQYe21mf211k08yUtJ0e0A2iaAtk9yRD_2-3_u2e2r680BfC9qY0i4gWiGM2KoDCN9002mPT7PjSVe2yfWy0i6cmQO3RYwNk0DYe21c0xGiehqz_Rys5UW3i24FRaEa8u4000000313aX70W35a_Fur_Uss3cu3mR83w220O0GtuBW6eWGp_EMY0QG48Rsoy3xiEAO8f140x3e_C0GwH9x2FIHD0iulV0I5eWJ19WJ1gWJvgRXvzQMkg8Mu1F7680K8A0KnnYe5CfWXlssXmRe58m2o1Mhx_IChWJG5ORzjeS6s1N1YlRieu-y_6Fme1RGyjo71gWMw0Um5gS5oHRG5ipSthu1s1Q15uWN0P0Na1oO5xcTiYke5md05xOIs1V0X3te5m7m5u3qiFAiBw0O4x0OyhhbXmQu60B2620qC3XaCZ4pPMHcDpWuP65aE3LbDJPXOJLYPcKmEM9aDj0O8V0Op07u6CkHclJhZhUwlW606OaPVI-W6Qe3i1cu6O48qXaIUM5YSrzpPN9sPN8lSZKqDoqow1dw0_0PdGBu6UZuenw16l__H-texdH_a1g0We20W820W0IO6l70j06m6kJdYOkuzAtWRlKQ0G000FWQkBfUWXibCJOoDZarE3SpEJKrDZKtE2qnCJ8rEJ8mE3StDJOmC34nDZWpD9WRzSnHi1jZk1i4WXmDDt8vEcX7MMf9LtfID-aSy3-07V__Vnu0OHkaKxCXhRUKX0sYPqhLbcSbyIV1dQMSPqCMNRP7vizShIruBZeS7g32qL37c5z2Zd-lmh1Ci0MmdkggADr7Qx4S4m1gTtgPMDw0rHwVpJt743JwK-Thad6q-a6JMoKP2dq6~1',
                                resource_links: {
                                    direct_data: {
                                        targetUrl: 'http://an.yandex.ru/count/WpyejI_zO0u3hHa0v2ncMG8XP8gx9GK03WGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm042s06WdO2S0U01iedlb07e0Rm30Xniqj4yV4WKy0ACpSs3-nc81SfWa0NAOB05nnYu1SSOm0MrFiW5xm_G1QCCK9c2VhVpiLUf1yN9D7Yzsx0vk0U01PgAW872a846u0ZnxTC2W0eAoGhUudnityBuF_WAWBKOW0kamdI82mIg2n1O9J8qnSa00B1bqTcrn-WBoc3m2mQR1fWDkBfUu0sAW86O3j2oYlJtzlpOLw0Em8GzkGwGZWG000000C4EI4S20CMJy_ZNzxROERWF1iWFe881W13VWk0QY13FyvQ81f0GXlRBmFkmufWYa4G3iEZym13f4di8z94q2pYzy18MY1C4c1C6g1Fcfk7drfQweXRW4ySOW1GWe1J76AWKoc26_RQ71kWKZ0B85Qllz8ok1D0LXlssXmRO5S6AzkoZZxpyO_2W5j3ot8S6g1Re1x0MfmN95j0MpDpUlW7O5e4NY1S1a1UG79WNkPsoAwWN2S0NjXBO5y24FUWN0V0NWFImygmle1WJi1ZokkM71hWO0i8O83GmE6GoCJDbP6OtE3XaOMGuDMKrDc5XDM9cPJ0vOcGsq1WXy1ZC0VWOov6QzEkEjxg-0O0PYHbzBw0PgWEm6RWPWGZI6H9vOM9pNtDbSdPbSYzoDJGtBJBe6Ve3y1cT0lWPwFYZ7e4Q__z7xUZkT7-G6e22W820W82019WQyS2q0R0QvEU9YxZqhU1kzHe10000-1gukbw26oKnDZ8sEJKuDpCvDJKsDJSuBJ4nCZKvCZ0uDpSrDZ0mCJ4sE3Cqc1lrp56m6sEu6mI270qtSZawQ4TPQabNUb8twHpmFu0T__z_7W1X6wHJio6jjvI43Q9dIjMKPoNn9y6SfPndGnPTjYVcprobBNWcEXmUeABHKCSONqAEVg_2e4om1R1UwgeesKThiHmJ06btUebOta3L7f_9FSSGBFfJvsgISPJwGPDJ8naAVGO0~1',
                                        actionButton: 'http://an.yandex.ru/count/Wq0ejI_zO0y3jHa0z2ncMG8XsZbYe0K03mGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm0760TW1e9s0d07W0RA9xvG1w06y0m8SRDBHF7n85F02ZCtDW_iPY0NAO905oc2m1SSOk0N76C05jJx81UyFq0MZ352PWdwtyx5NgGV5oJHulTkmERW7W0MQYe21mf211k08yUtJ0e0A2iaAtk9yRD_2-3_u2e2r680BfC9qY0i4gWiGM2KoDCN9002mPT7PjSVe2yfWy0i6cmQO3RYwNk0DYe21c0xGiehqz_Rys5UW3i24FRaEa8u4000000313aX70W35a_Fur_Uss3cu3mR83w220O0GtuBW6eWGp_EMY0QG48Rsoy3xiEAO8f140x3e_C0GwH9x2FIHD0iulV0I5eWJ19WJ1gWJvgRXvzQMkg8Mu1F7680K8A0KnnYe5CfWXlssXmRe58m2o1Mhx_IChWJG5ORzjeS6s1N1YlRieu-y_6Fme1RGyjo71gWMw0Um5gS5oHRG5ipSthu1s1Q15uWN0P0Na1oO5xcTiYke5md05xOIs1V0X3te5m7m5u3qiFAiBw0O4x0OyhhbXmQu60B2620qC3XaCZ4pPMHcDpWuP65aE3LbDJPXOJLYPcKmEM9aDj0O8V0Op07u6CkHclJhZhUwlW606OaPVI-W6Qe3i1cu6O48qXaIUM5YSrzpPN9sPN8lSZKqDoqow1dw0_0PdGBu6UZuenw16l__H-texdH_a1g0We20W820W0IO6l70j06m6kJdYOkuzAtWRlKQ0G000FWQkBfUWXibCJOoDZarE3SpEJKrDZKtE2qnCJ8rEJ8mE3StDJOmC34nDZWpD9WRzSnHi1jZk1i4WXmDDt8vEcX7MMf9LtfID-aSy3-07V__Vnu0OHkaKxCXhRUKX0sYPqhLbcSbyIV1dQMSPqCMNRP7vizShIruBZeS7g32qL37c5z2Zd-lmh1Ci0MmdkggADr7Qx4S4m1gTtgPMDw0rHwVpJt743JwK-Thad6q-a6JMoKP2dq6~1',
                                    },
                                },
                                count_links: {
                                    abuseUrl: '//an.yandex.ru/abuse/WD8ejI_z8EnD1W042sPP0Y4C3vN01G3i036KQ_bW000003ZcYpPmyAQpyiG9a07ucCghqO20W0AO0VYOogjHs06WdO2S0UW1l0EW0kBRfmN91b2PWdwtyx5NgGV5oJHulTkmET070j08W820W0A02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G2v3f2E10313aX70W3O5S6AzkoZZxpyOu4Ny3_u680PWXmDDt8vEcX7MMf9LtfID-aSW1r_201K8DXK9BYKXhDaKCJmBcKEm8h00b80~1',
                                    tracking: 'https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1',
                                    empty: 'https://an.yandex.ru/resource/spacer.gif?',
                                },
                                impId: '2',
                            },
                            direct_data: {
                                targetUrl: 'https://ad.doubleclick.net/ddm/trackclk/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=;ltd=',
                                faviconSizes: {
                                    h: 16,
                                    w: 16,
                                },
                                domain: 'nestlebaby.ru',
                                measurers: '',
                                trackers: {
                                    '0': 'https://ad.doubleclick.net/ddm/trackimp/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;ord=1717109281;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=?',
                                    '1': 'https://1717109281.verify.yandex.ru/verify?platformid=1&msid=msva5b55_5-63579728-10917332540&BID=10917332540&BTYPE=1&CID=63579728&DRND=1717109281&DTYPE=desktop&REF=https%3A%2F%2Ffrontend.vh.yandex.ru%2Fplayer%2F408d213edf788dad85e56aa5bfe09bd6%3Fuse_friendly_frame%3Dtrue%26vsid%3D9188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%26from%3Dya-weather%26reqid%3D1626958739556578-11259208775600116834%26flags%3D%257B%2522player_api_v2%2522%253A%2522false%2522%252C%2522disable_autoplay_slow_connection%2522%253A%2522false%2522%252C%2522disable_autoplay_save_data%2522%253A%2522false%2522%252C%2522poll_timeout%2522%253A%252221600%2522%252C%2522poll_show_before_midroll%2522%253A%2522false%2522%252C%2522version%2522%253A%2522undefined%2522%252C%2522progress%2522%253A%2522true%2522%252C%2522start_position_confirmation%2522%253A%2522false%2522%252C%2522force_unmute%2522%253A%2522true%2522%252C%2522restore_playback_progress%2522%253A%2522false%2522%252C%2522hide_brand_play_button%2522%253A%2522false%2522%257D%26stream_url%3Dhttps%253A%252F%252Fstrm.yandex.ru%252Fkal%252Fweather_moscow%252Fysign1%253Da9f3470057779456717984b3b3295b6c122b621e4efd76d0f8782bab1ce8f824%252CabcID%253D105%252Cfrom%253Dya-weather%252Cpfx%252Cregion%253D10000%252Csfx%252Cts%253D60fabd13%252Fmanifest.mpd%253Ffrom%253Dya-weather%2526partner_id%253D443123%2526target_ref%253Dhttps%25253A%25252F%25252Fyastatic.net%25252Fyandex-video-player-iframe-api%25252Fjs%25252Fplayer-api-adapter-loader.js%2526uuid%253D408d213edf788dad85e56aa5bfe09bd6%2526video_category_id%253D1025%2526clid%253D495%2526yandexuid%253D6319099721604569424%2526slots%253Dnull%2526imp_id%253D1%2526reqid%253D1626958739556578-11259208775600116834%2526content_id%253D408d213edf788dad85e56aa5bfe09bd6%2526from_block%253Dother%2526channel_id%253D1551792230%2526content-genre%253D%2526content-category%253D%2526brand-safety-categories%253D%25255B%25255D%2526sandbox_version%253D0x836c75a4440%26additional_params%3D%257B%2522from%2522%253A%2522ya-weather%2522%252C%2522reqid%2522%253A%25221626958739556578-11259208775600116834%2522%252C%2522vsid%2522%253A%25229188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%2522%252C%2522adsid%2522%253A%2522d4406f3b827c8108d733370b4db28a244117dc985457xWEBx7099x1626958738%2522%252C%2522content_id%2522%253A%2522408d213edf788dad85e56aa5bfe09bd6%2522%252C%2522from_block%2522%253A%2522other%2522%252C%2522channel_id%2522%253A%25221551792230%2522%252C%2522content-genre%2522%253A%2522%2522%252C%2522content-category%2522%253A%2522%2522%252C%2522brand-safety-categories%2522%253A%2522%255B%255D%2522%252C%2522sandbox_version%2522%253A%25220x836c75a4440%2522%257D%26partner_id%3D443123%26category%3D1025%26distr_id%3D0%26video_content_id%3D408d213edf788dad85e56aa5bfe09bd6%26video_content_name%3D%25D0%259F%25D0%25BE%25D0%25B3%25D0%25BE%25D0%25B4%25D0%25B0%2520%25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%26video_genre_name%3D%26preview%3D%252F%252Favatars.mds.yandex.net%252Fget-vh-cv%252F2424227%252F2a0000017ace447bfced2796b7c72daed5fd%252Forig%26host%3Dfrontend.vh.yandex.ru&SESSION=6869341626958739116&hitlogid=4157064062874995141&page=443123',
                                },
                            },
                            constructor_data: {
                                VpaidPcodeUrl: 'https://yastatic.net/awaps-ad-sdk-js/1_0/interactive_viewer.js',
                                Theme: 'video-banner_theme_empty',
                                Duration: 15.0,
                                MediaFiles: [
                                    {
                                        Id: 'https:/strm.yandex.ru/vh-canvas-converted/get-canvas',
                                        Delivery: 'progressive',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0.m3u8',
                                        MimeType: 'application/vnd.apple.mpegurl',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_240p.mp4',
                                        Delivery: 'progressive',
                                        Width: '240',
                                        Height: '240',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_240p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '363',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_240p.webm',
                                        Delivery: 'progressive',
                                        Width: '240',
                                        Height: '240',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_240p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '333',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_360p.mp4',
                                        Delivery: 'progressive',
                                        Width: '360',
                                        Height: '360',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_360p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '507',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_360p.webm',
                                        Delivery: 'progressive',
                                        Width: '360',
                                        Height: '360',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_360p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '467',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_480p.mp4',
                                        Delivery: 'progressive',
                                        Width: '480',
                                        Height: '480',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_480p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '992',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_480p.webm',
                                        Delivery: 'progressive',
                                        Width: '480',
                                        Height: '480',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_480p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '883',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_576p.mp4',
                                        Delivery: 'progressive',
                                        Width: '576',
                                        Height: '576',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_576p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '1953',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_576p.webm',
                                        Delivery: 'progressive',
                                        Width: '576',
                                        Height: '576',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_576p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '1801',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_720p.mp4',
                                        Delivery: 'progressive',
                                        Width: '720',
                                        Height: '720',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_720p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '2848',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_720p.webm',
                                        Delivery: 'progressive',
                                        Width: '720',
                                        Height: '720',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_720p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '2656',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_1080p.mp4',
                                        Delivery: 'progressive',
                                        Width: '1080',
                                        Height: '1080',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_1080p.mp4',
                                        MimeType: 'video/mp4',
                                        Bitrate: '3830',
                                    },
                                    {
                                        Id: 'video_5f982fb15cffc68d5e867ca0_43_1080p.webm',
                                        Delivery: 'progressive',
                                        Width: '1080',
                                        Height: '1080',
                                        Url: 'https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_1080p.webm',
                                        MimeType: 'video/webm',
                                        Bitrate: '3918',
                                    },
                                ],
                                HasAbuseButton: true,
                                SocialAdvertisement: false,
                                PlaybackParameters: {
                                    ShowSkipButton: true,
                                    SkipDelay: '5',
                                },
                                UseTrackingEvents: true,
                                IsStock: true,
                                AdditionElements: [
                                    {
                                        Type: 'TITLE',
                                        Options: {
                                            BackgroundColor: '#000000',
                                            TextColor: '#ffffff',
                                        },
                                    },
                                    {
                                        Type: 'BODY',
                                        Options: {
                                            BackgroundColor: '#000000',
                                            TextColor: '#ffffff',
                                        },
                                    },
                                    {
                                        Type: 'DOMAIN',
                                        Options: {},
                                    },
                                    {
                                        Type: 'BUTTON',
                                        Options: {
                                            TextColor: '#000000',
                                            Color: '#FF0000',
                                        },
                                    },
                                    {
                                        Type: 'DISCLAIMER',
                                        Options: {
                                            BackgroundColor: '#000000',
                                            TextColor: '#ffffff',
                                            Text: '',
                                        },
                                    },
                                    {
                                        Type: 'AGE',
                                        Options: {
                                            BackgroundColor: '#000000',
                                            TextColor: '#ffffff',
                                            Text: '',
                                        },
                                    },
                                    {
                                        Type: 'LEGAL',
                                        Options: {
                                            BackgroundColor: '#000000',
                                            TextColor: '#ffffff',
                                            Text: 'bla-bla-legal',
                                        },
                                    },
                                ],
                                InteractiveVpaid: false,
                                AddPixelImpression: true,
                                CreativeId: '2148470331',
                                StrmPrefix: 'video_5f982fb15cffc68d5e867ca0',
                                ShowVpaid: true,
                                ShowVideoClicks: true,
                                SoundbtnLayout: '1',
                                AdlabelLayout: '1',
                                CountdownLayout: '1',
                                UseVpaidImpressions: false,
                                AdSystem: 'Yabs Ad CPC Server',
                            },
                        },
                    },
                },
            }, {
                settings: {
                    '3': {
                        linkTail: 'https://an.yandex.ru/rtbcount/1K-WJkUj0Tm100000000U9nJ39ZPLBVzyyPmmwCdPUhttjvWQJab-sS20n1umaH25PaYqs8_7s9ZI6K4YcSUFMbI0H8lPGBoQgy2YLP643cJKMKHAqWdmqB62NqiODPAHWzt9Z1hB-DY0tw6es3-LKQGGNSP6UOmCFnblFTiczDS9WwWo5cc_q3mYacWTdNP-QCd6HXE_BbmS-jnLeQ_J223Mvb1P2-p8f2Soim59ESoWmnVoSpbeC00Cc0ZyvflQwild0s9FyaCyzbLPh1bp6n0yYbp0Fi1pfVC2evWDp7yP7PmuWUpDh0mxc1XFi32V9AbckFBH1DkYxqNM9ujU_ZyshHTYyHkGdp3yC7-8PJrmFuj2yWh2rWvLx0sVDdmQOt-9bO54mEB52D3uzIKD7CBYvJ450lJL5Epn4JJfBHK0yka5BC3h6V-_OS0nfEijVeGiowmAdnbPGGQ_8kLqz3CsDJSqjp8ee7jGb-3tmIs2vkwldp_zkT6-0osZmUsC2vWU_Ayitl7bxKFybQomPnoW2tv11lB8pPUyo9Bgmpto2pFyAeWSm3SDg08',
                        viewNotices: ['https://ad.doubleclick.net/ddm/trackimp/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;ord=1717109281;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=?', 'https://1717109281.verify.yandex.ru/verify?platformid=1&msid=msva5b55_5-63579728-10917332540&BID=10917332540&BTYPE=1&CID=63579728&DRND=1717109281&DTYPE=desktop&REF=https%3A%2F%2Ffrontend.vh.yandex.ru%2Fplayer%2F408d213edf788dad85e56aa5bfe09bd6%3Fuse_friendly_frame%3Dtrue%26vsid%3D9188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%26from%3Dya-weather%26reqid%3D1626958739556578-11259208775600116834%26flags%3D%257B%2522player_api_v2%2522%253A%2522false%2522%252C%2522disable_autoplay_slow_connection%2522%253A%2522false%2522%252C%2522disable_autoplay_save_data%2522%253A%2522false%2522%252C%2522poll_timeout%2522%253A%252221600%2522%252C%2522poll_show_before_midroll%2522%253A%2522false%2522%252C%2522version%2522%253A%2522undefined%2522%252C%2522progress%2522%253A%2522true%2522%252C%2522start_position_confirmation%2522%253A%2522false%2522%252C%2522force_unmute%2522%253A%2522true%2522%252C%2522restore_playback_progress%2522%253A%2522false%2522%252C%2522hide_brand_play_button%2522%253A%2522false%2522%257D%26stream_url%3Dhttps%253A%252F%252Fstrm.yandex.ru%252Fkal%252Fweather_moscow%252Fysign1%253Da9f3470057779456717984b3b3295b6c122b621e4efd76d0f8782bab1ce8f824%252CabcID%253D105%252Cfrom%253Dya-weather%252Cpfx%252Cregion%253D10000%252Csfx%252Cts%253D60fabd13%252Fmanifest.mpd%253Ffrom%253Dya-weather%2526partner_id%253D443123%2526target_ref%253Dhttps%25253A%25252F%25252Fyastatic.net%25252Fyandex-video-player-iframe-api%25252Fjs%25252Fplayer-api-adapter-loader.js%2526uuid%253D408d213edf788dad85e56aa5bfe09bd6%2526video_category_id%253D1025%2526clid%253D495%2526yandexuid%253D6319099721604569424%2526slots%253Dnull%2526imp_id%253D1%2526reqid%253D1626958739556578-11259208775600116834%2526content_id%253D408d213edf788dad85e56aa5bfe09bd6%2526from_block%253Dother%2526channel_id%253D1551792230%2526content-genre%253D%2526content-category%253D%2526brand-safety-categories%253D%25255B%25255D%2526sandbox_version%253D0x836c75a4440%26additional_params%3D%257B%2522from%2522%253A%2522ya-weather%2522%252C%2522reqid%2522%253A%25221626958739556578-11259208775600116834%2522%252C%2522vsid%2522%253A%25229188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%2522%252C%2522adsid%2522%253A%2522d4406f3b827c8108d733370b4db28a244117dc985457xWEBx7099x1626958738%2522%252C%2522content_id%2522%253A%2522408d213edf788dad85e56aa5bfe09bd6%2522%252C%2522from_block%2522%253A%2522other%2522%252C%2522channel_id%2522%253A%25221551792230%2522%252C%2522content-genre%2522%253A%2522%2522%252C%2522content-category%2522%253A%2522%2522%252C%2522brand-safety-categories%2522%253A%2522%255B%255D%2522%252C%2522sandbox_version%2522%253A%25220x836c75a4440%2522%257D%26partner_id%3D443123%26category%3D1025%26distr_id%3D0%26video_content_id%3D408d213edf788dad85e56aa5bfe09bd6%26video_content_name%3D%25D0%259F%25D0%25BE%25D0%25B3%25D0%25BE%25D0%25B4%25D0%25B0%2520%25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%26video_genre_name%3D%26preview%3D%252F%252Favatars.mds.yandex.net%252Fget-vh-cv%252F2424227%252F2a0000017ace447bfced2796b7c72daed5fd%252Forig%26host%3Dfrontend.vh.yandex.ru&SESSION=6869341626958739116&hitlogid=4157064062874995141&page=443123'],
                    },
                },
                dc_params: {
                    creative_params: {
                        crypta_user_gender: '1',
                        crypta_user_age: '2',
                    },
                    data_params: {
                        '123321': {
                            bs_data: {
                                targetUrl: 'https://ad.doubleclick.net/ddm/trackclk/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=;ltd=',
                                domain: 'nestlebaby.ru',
                                bannerFlags: 'milk-substitute,animated',
                                bannerLang: '1',
                                adId: '123321',
                                actionButton: 'http://an.yandex.ru/count/Wq0ejI_zO0y3jHa0z2ncMG8XsZbYe0K03mGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm0760TW1e9s0d07W0RA9xvG1w06y0m8SRDBHF7n85F02ZCtDW_iPY0NAO905oc2m1SSOk0N76C05jJx81UyFq0MZ352PWdwtyx5NgGV5oJHulTkmERW7W0MQYe21mf211k08yUtJ0e0A2iaAtk9yRD_2-3_u2e2r680BfC9qY0i4gWiGM2KoDCN9002mPT7PjSVe2yfWy0i6cmQO3RYwNk0DYe21c0xGiehqz_Rys5UW3i24FRaEa8u4000000313aX70W35a_Fur_Uss3cu3mR83w220O0GtuBW6eWGp_EMY0QG48Rsoy3xiEAO8f140x3e_C0GwH9x2FIHD0iulV0I5eWJ19WJ1gWJvgRXvzQMkg8Mu1F7680K8A0KnnYe5CfWXlssXmRe58m2o1Mhx_IChWJG5ORzjeS6s1N1YlRieu-y_6Fme1RGyjo71gWMw0Um5gS5oHRG5ipSthu1s1Q15uWN0P0Na1oO5xcTiYke5md05xOIs1V0X3te5m7m5u3qiFAiBw0O4x0OyhhbXmQu60B2620qC3XaCZ4pPMHcDpWuP65aE3LbDJPXOJLYPcKmEM9aDj0O8V0Op07u6CkHclJhZhUwlW606OaPVI-W6Qe3i1cu6O48qXaIUM5YSrzpPN9sPN8lSZKqDoqow1dw0_0PdGBu6UZuenw16l__H-texdH_a1g0We20W820W0IO6l70j06m6kJdYOkuzAtWRlKQ0G000FWQkBfUWXibCJOoDZarE3SpEJKrDZKtE2qnCJ8rEJ8mE3StDJOmC34nDZWpD9WRzSnHi1jZk1i4WXmDDt8vEcX7MMf9LtfID-aSy3-07V__Vnu0OHkaKxCXhRUKX0sYPqhLbcSbyIV1dQMSPqCMNRP7vizShIruBZeS7g32qL37c5z2Zd-lmh1Ci0MmdkggADr7Qx4S4m1gTtgPMDw0rHwVpJt743JwK-Thad6q-a6JMoKP2dq6~1',
                                resource_links: {
                                    direct_data: {
                                        targetUrl: 'http://an.yandex.ru/count/WpyejI_zO0u3hHa0v2ncMG8XP8gx9GK03WGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm042s06WdO2S0U01iedlb07e0Rm30Xniqj4yV4WKy0ACpSs3-nc81SfWa0NAOB05nnYu1SSOm0MrFiW5xm_G1QCCK9c2VhVpiLUf1yN9D7Yzsx0vk0U01PgAW872a846u0ZnxTC2W0eAoGhUudnityBuF_WAWBKOW0kamdI82mIg2n1O9J8qnSa00B1bqTcrn-WBoc3m2mQR1fWDkBfUu0sAW86O3j2oYlJtzlpOLw0Em8GzkGwGZWG000000C4EI4S20CMJy_ZNzxROERWF1iWFe881W13VWk0QY13FyvQ81f0GXlRBmFkmufWYa4G3iEZym13f4di8z94q2pYzy18MY1C4c1C6g1Fcfk7drfQweXRW4ySOW1GWe1J76AWKoc26_RQ71kWKZ0B85Qllz8ok1D0LXlssXmRO5S6AzkoZZxpyO_2W5j3ot8S6g1Re1x0MfmN95j0MpDpUlW7O5e4NY1S1a1UG79WNkPsoAwWN2S0NjXBO5y24FUWN0V0NWFImygmle1WJi1ZokkM71hWO0i8O83GmE6GoCJDbP6OtE3XaOMGuDMKrDc5XDM9cPJ0vOcGsq1WXy1ZC0VWOov6QzEkEjxg-0O0PYHbzBw0PgWEm6RWPWGZI6H9vOM9pNtDbSdPbSYzoDJGtBJBe6Ve3y1cT0lWPwFYZ7e4Q__z7xUZkT7-G6e22W820W82019WQyS2q0R0QvEU9YxZqhU1kzHe10000-1gukbw26oKnDZ8sEJKuDpCvDJKsDJSuBJ4nCZKvCZ0uDpSrDZ0mCJ4sE3Cqc1lrp56m6sEu6mI270qtSZawQ4TPQabNUb8twHpmFu0T__z_7W1X6wHJio6jjvI43Q9dIjMKPoNn9y6SfPndGnPTjYVcprobBNWcEXmUeABHKCSONqAEVg_2e4om1R1UwgeesKThiHmJ06btUebOta3L7f_9FSSGBFfJvsgISPJwGPDJ8naAVGO0~1',
                                        actionButton: 'http://an.yandex.ru/count/Wq0ejI_zO0y3jHa0z2ncMG8XsZbYe0K03mGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm0760TW1e9s0d07W0RA9xvG1w06y0m8SRDBHF7n85F02ZCtDW_iPY0NAO905oc2m1SSOk0N76C05jJx81UyFq0MZ352PWdwtyx5NgGV5oJHulTkmERW7W0MQYe21mf211k08yUtJ0e0A2iaAtk9yRD_2-3_u2e2r680BfC9qY0i4gWiGM2KoDCN9002mPT7PjSVe2yfWy0i6cmQO3RYwNk0DYe21c0xGiehqz_Rys5UW3i24FRaEa8u4000000313aX70W35a_Fur_Uss3cu3mR83w220O0GtuBW6eWGp_EMY0QG48Rsoy3xiEAO8f140x3e_C0GwH9x2FIHD0iulV0I5eWJ19WJ1gWJvgRXvzQMkg8Mu1F7680K8A0KnnYe5CfWXlssXmRe58m2o1Mhx_IChWJG5ORzjeS6s1N1YlRieu-y_6Fme1RGyjo71gWMw0Um5gS5oHRG5ipSthu1s1Q15uWN0P0Na1oO5xcTiYke5md05xOIs1V0X3te5m7m5u3qiFAiBw0O4x0OyhhbXmQu60B2620qC3XaCZ4pPMHcDpWuP65aE3LbDJPXOJLYPcKmEM9aDj0O8V0Op07u6CkHclJhZhUwlW606OaPVI-W6Qe3i1cu6O48qXaIUM5YSrzpPN9sPN8lSZKqDoqow1dw0_0PdGBu6UZuenw16l__H-texdH_a1g0We20W820W0IO6l70j06m6kJdYOkuzAtWRlKQ0G000FWQkBfUWXibCJOoDZarE3SpEJKrDZKtE2qnCJ8rEJ8mE3StDJOmC34nDZWpD9WRzSnHi1jZk1i4WXmDDt8vEcX7MMf9LtfID-aSy3-07V__Vnu0OHkaKxCXhRUKX0sYPqhLbcSbyIV1dQMSPqCMNRP7vizShIruBZeS7g32qL37c5z2Zd-lmh1Ci0MmdkggADr7Qx4S4m1gTtgPMDw0rHwVpJt743JwK-Thad6q-a6JMoKP2dq6~1',
                                    },
                                },
                                count_links: {
                                    abuseUrl: '//an.yandex.ru/abuse/WD8ejI_z8EnD1W042sPP0Y4C3vN01G3i036KQ_bW000003ZcYpPmyAQpyiG9a07ucCghqO20W0AO0VYOogjHs06WdO2S0UW1l0EW0kBRfmN91b2PWdwtyx5NgGV5oJHulTkmET070j08W820W0A02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G2v3f2E10313aX70W3O5S6AzkoZZxpyOu4Ny3_u680PWXmDDt8vEcX7MMf9LtfID-aSW1r_201K8DXK9BYKXhDaKCJmBcKEm8h00b80~1',
                                    tracking: 'https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1',
                                    empty: 'https://an.yandex.ru/resource/spacer.gif?',
                                },
                                impId: '3',
                            },
                            direct_data: {},
                            constructor_data: {
                                Duration: 15.0,
                                CreativeId: '444444',
                                AdSystem: 'Yabs Ad CPC Server',
                                SoundbtnLayout: '1',
                                AdlabelLayout: '1',
                                CountdownLayout: '1',
                            },
                        },
                    },
                },
            }],
        };
        const expectedVast = normalizeVast(`
            <?xml version="1.0" encoding="UTF-8" ?>
            <VAST version="3.0" ya-uniformat="true">
                <Ad id="a34sdf" sequence="1">
                    <InLine>
                        <AdSystem>Yabs Ad CPC Server</AdSystem>
                        <AdTitle>Interactive Direct In Video</AdTitle>
                        <Impression id="direct_impression_13"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=13]]></Impression>
                        <Error>
                            <![CDATA[https://an.yandex.ru/jserr/3948?errmsg=auto-video-error]]>
                        </Error>
                        <Creatives>
                            <Creative id="2148470331">
                                <Linear skipoffset="00:00:05">
                                    <Duration>00:00:15</Duration>
                                    <TrackingEvents>
                                        <Tracking event="start">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=0]]>
                                        </Tracking>
                                        <Tracking event="firstQuartile">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=1]]>
                                        </Tracking>
                                        <Tracking event="midpoint">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=2]]>
                                        </Tracking>
                                        <Tracking event="thirdQuartile">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=3]]>
                                        </Tracking>
                                        <Tracking event="complete">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=4]]>
                                        </Tracking>
                                        <Tracking event="mute">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=5]]>
                                        </Tracking>
                                        <Tracking event="unmute">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=6]]>
                                        </Tracking>
                                        <Tracking event="pause">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=7]]>
                                        </Tracking>
                                        <Tracking event="resume">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=8]]>
                                        </Tracking>
                                        <Tracking event="skip">
                                            <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=9]]>
                                        </Tracking>
                                    </TrackingEvents>
                                    <MediaFiles>
                                        <MediaFile width="0" height="0" delivery="progressive" type="application/javascript"
                                                apiFramework="VPAID">
                                            <![CDATA[https://yastatic.net/awaps-ad-sdk-js/1_0/interactive_viewer.js]]>
                                        </MediaFile>
                                    </MediaFiles>
                                    <VideoClicks>
                                        <ClickThrough>http://an.yandex.ru/count/WpyejI_zO0u3hHa0v2ncMG8XP8gx9GK03WGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm042s06WdO2S0U01iedlb07e0Rm30Xniqj4yV4WKy0ACpSs3-nc81SfWa0NAOB05nnYu1SSOm0MrFiW5xm_G1QCCK9c2VhVpiLUf1yN9D7Yzsx0vk0U01PgAW872a846u0ZnxTC2W0eAoGhUudnityBuF_WAWBKOW0kamdI82mIg2n1O9J8qnSa00B1bqTcrn-WBoc3m2mQR1fWDkBfUu0sAW86O3j2oYlJtzlpOLw0Em8GzkGwGZWG000000C4EI4S20CMJy_ZNzxROERWF1iWFe881W13VWk0QY13FyvQ81f0GXlRBmFkmufWYa4G3iEZym13f4di8z94q2pYzy18MY1C4c1C6g1Fcfk7drfQweXRW4ySOW1GWe1J76AWKoc26_RQ71kWKZ0B85Qllz8ok1D0LXlssXmRO5S6AzkoZZxpyO_2W5j3ot8S6g1Re1x0MfmN95j0MpDpUlW7O5e4NY1S1a1UG79WNkPsoAwWN2S0NjXBO5y24FUWN0V0NWFImygmle1WJi1ZokkM71hWO0i8O83GmE6GoCJDbP6OtE3XaOMGuDMKrDc5XDM9cPJ0vOcGsq1WXy1ZC0VWOov6QzEkEjxg-0O0PYHbzBw0PgWEm6RWPWGZI6H9vOM9pNtDbSdPbSYzoDJGtBJBe6Ve3y1cT0lWPwFYZ7e4Q__z7xUZkT7-G6e22W820W82019WQyS2q0R0QvEU9YxZqhU1kzHe10000-1gukbw26oKnDZ8sEJKuDpCvDJKsDJSuBJ4nCZKvCZ0uDpSrDZ0mCJ4sE3Cqc1lrp56m6sEu6mI270qtSZawQ4TPQabNUb8twHpmFu0T__z_7W1X6wHJio6jjvI43Q9dIjMKPoNn9y6SfPndGnPTjYVcprobBNWcEXmUeABHKCSONqAEVg_2e4om1R1UwgeesKThiHmJ06btUebOta3L7f_9FSSGBFfJvsgISPJwGPDJ8naAVGO0~1</ClickThrough>
                                    </VideoClicks>
                                    <AdParameters>
                                        <![CDATA[
                                        {
                                            "HAS_BUTTON": true,
                                            "BUTTON_TEXT_COLOR": "#000000",
                                            "HAS_DOMAIN": true,
                                            "HAS_TITLE": true,
                                            "HAS_BODY": true,
                                            "HAS_AGE": true,
                                            "HAS_LEGAL": true,
                                            "HAS_DISCLAIMER": true,
                                            "AGE_BACKGROUND_COLOR": "#000000",
                                            "AGE_TEXT": "",
                                            "AGE_TEXT_COLOR": "#ffffff",
                                            "BODY_BACKGROUND_COLOR": "#000000",
                                            "BODY_TEXT_COLOR": "#ffffff",
                                            "BUTTON_COLOR": "#FF0000",
                                            "DISCLAIMER_BACKGROUND_COLOR": "#000000",
                                            "DISCLAIMER_TEXT": "",
                                            "DISCLAIMER_TEXT_COLOR": "#ffffff",
                                            "LEGAL_BACKGROUND_COLOR": "#000000",
                                            "LEGAL_TEXT": "bla-bla-legal",
                                            "LEGAL_TEXT_COLOR": "#ffffff",
                                            "TITLE_BACKGROUND_COLOR": "#000000",
                                            "TITLE_TEXT_COLOR": "#ffffff",
                                            "theme": "video-banner_interactive-viewer",
                                            "duration": 15.0,
                                            "mediaFiles": [
                                                {
                                                    "bitrate": null,
                                                    "delivery": "progressive",
                                                    "height": null,
                                                    "id": "https:/strm.yandex.ru/vh-canvas-converted/get-canvas",
                                                    "type": "application/vnd.apple.mpegurl",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0.m3u8",
                                                    "width": null
                                                },
                                                {
                                                    "bitrate": 363,
                                                    "delivery": "progressive",
                                                    "height": 240,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_240p.mp4",
                                                    "width": 240,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_240p.mp4"
                                                },
                                                {
                                                    "bitrate": 333,
                                                    "delivery": "progressive",
                                                    "height": 240,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_240p.webm",
                                                    "width": 240,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_240p.webm"
                                                },
                                                {
                                                    "bitrate": 507,
                                                    "delivery": "progressive",
                                                    "height": 360,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_360p.mp4",
                                                    "width": 360,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_360p.mp4"
                                                },
                                                {
                                                    "bitrate": 467,
                                                    "delivery": "progressive",
                                                    "height": 360,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_360p.webm",
                                                    "width": 360,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_360p.webm"
                                                },
                                                {
                                                    "bitrate": 992,
                                                    "delivery": "progressive",
                                                    "height": 480,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_480p.mp4",
                                                    "width": 480,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_480p.mp4"
                                                },
                                                {
                                                    "bitrate": 883,
                                                    "delivery": "progressive",
                                                    "height": 480,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_480p.webm",
                                                    "width": 480,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_480p.webm"
                                                },
                                                {
                                                    "bitrate": 1953,
                                                    "delivery": "progressive",
                                                    "height": 576,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_576p.mp4",
                                                    "width": 576,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_576p.mp4"
                                                },
                                                {
                                                    "bitrate": 1801,
                                                    "delivery": "progressive",
                                                    "height": 576,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_576p.webm",
                                                    "width": 576,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_576p.webm"
                                                },
                                                {
                                                    "bitrate": 2848,
                                                    "delivery": "progressive",
                                                    "height": 720,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_720p.mp4",
                                                    "width": 720,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_720p.mp4"
                                                },
                                                {
                                                    "bitrate": 2656,
                                                    "delivery": "progressive",
                                                    "height": 720,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_720p.webm",
                                                    "width": 720,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_720p.webm"
                                                },
                                                {
                                                    "bitrate": 3830,
                                                    "delivery": "progressive",
                                                    "height": 1080,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_1080p.mp4",
                                                    "width": 1080,
                                                    "type": "video/mp4",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_1080p.mp4"
                                                },
                                                {
                                                    "bitrate": 3918,
                                                    "delivery": "progressive",
                                                    "height": 1080,
                                                    "id": "video_5f982fb15cffc68d5e867ca0_43_1080p.webm",
                                                    "width": 1080,
                                                    "type": "video/webm",
                                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0_43_1080p.webm"
                                                }
                                            ],
                                            "socialAdvertising": false,
                                            "AUCTION_DC_PARAMS": {
                                                "sad":"sad",
                                                "creative_params": {
                                                    "crypta_user_gender":"1",
                                                    "crypta_user_age":"2"
                                                },
                                                "data_params": {
                                                    "72057604955260476": {
                                                        "target_url":"https://ad.doubleclick.net/ddm/trackclk/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=;ltd=",
                                                        "unmoderated": {
                                                            "faviconHeight":"16",
                                                            "punyDomain":"nestlebaby.ru",
                                                            "faviconWidth":"16",
                                                            "warning":""
                                                        },
                                                        "count":"https://an.yandex.ru/resource/spacer.gif?",
                                                        "click_url": {
                                                            "text_name":"http://an.yandex.ru/count/WpyejI_zO0u3hHa0v2ncMG8XP8gx9GK03WGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm042s06WdO2S0U01iedlb07e0Rm30Xniqj4yV4WKy0ACpSs3-nc81SfWa0NAOB05nnYu1SSOm0MrFiW5xm_G1QCCK9c2VhVpiLUf1yN9D7Yzsx0vk0U01PgAW872a846u0ZnxTC2W0eAoGhUudnityBuF_WAWBKOW0kamdI82mIg2n1O9J8qnSa00B1bqTcrn-WBoc3m2mQR1fWDkBfUu0sAW86O3j2oYlJtzlpOLw0Em8GzkGwGZWG000000C4EI4S20CMJy_ZNzxROERWF1iWFe881W13VWk0QY13FyvQ81f0GXlRBmFkmufWYa4G3iEZym13f4di8z94q2pYzy18MY1C4c1C6g1Fcfk7drfQweXRW4ySOW1GWe1J76AWKoc26_RQ71kWKZ0B85Qllz8ok1D0LXlssXmRO5S6AzkoZZxpyO_2W5j3ot8S6g1Re1x0MfmN95j0MpDpUlW7O5e4NY1S1a1UG79WNkPsoAwWN2S0NjXBO5y24FUWN0V0NWFImygmle1WJi1ZokkM71hWO0i8O83GmE6GoCJDbP6OtE3XaOMGuDMKrDc5XDM9cPJ0vOcGsq1WXy1ZC0VWOov6QzEkEjxg-0O0PYHbzBw0PgWEm6RWPWGZI6H9vOM9pNtDbSdPbSYzoDJGtBJBe6Ve3y1cT0lWPwFYZ7e4Q__z7xUZkT7-G6e22W820W82019WQyS2q0R0QvEU9YxZqhU1kzHe10000-1gukbw26oKnDZ8sEJKuDpCvDJKsDJSuBJ4nCZKvCZ0uDpSrDZ0mCJ4sE3Cqc1lrp56m6sEu6mI270qtSZawQ4TPQabNUb8twHpmFu0T__z_7W1X6wHJio6jjvI43Q9dIjMKPoNn9y6SfPndGnPTjYVcprobBNWcEXmUeABHKCSONqAEVg_2e4om1R1UwgeesKThiHmJ06btUebOta3L7f_9FSSGBFfJvsgISPJwGPDJ8naAVGO0~1",
                                                            "action_button":"http://an.yandex.ru/count/Wq0ejI_zO0y3jHa0z2ncMG8XsZbYe0K03mGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm0760TW1e9s0d07W0RA9xvG1w06y0m8SRDBHF7n85F02ZCtDW_iPY0NAO905oc2m1SSOk0N76C05jJx81UyFq0MZ352PWdwtyx5NgGV5oJHulTkmERW7W0MQYe21mf211k08yUtJ0e0A2iaAtk9yRD_2-3_u2e2r680BfC9qY0i4gWiGM2KoDCN9002mPT7PjSVe2yfWy0i6cmQO3RYwNk0DYe21c0xGiehqz_Rys5UW3i24FRaEa8u4000000313aX70W35a_Fur_Uss3cu3mR83w220O0GtuBW6eWGp_EMY0QG48Rsoy3xiEAO8f140x3e_C0GwH9x2FIHD0iulV0I5eWJ19WJ1gWJvgRXvzQMkg8Mu1F7680K8A0KnnYe5CfWXlssXmRe58m2o1Mhx_IChWJG5ORzjeS6s1N1YlRieu-y_6Fme1RGyjo71gWMw0Um5gS5oHRG5ipSthu1s1Q15uWN0P0Na1oO5xcTiYke5md05xOIs1V0X3te5m7m5u3qiFAiBw0O4x0OyhhbXmQu60B2620qC3XaCZ4pPMHcDpWuP65aE3LbDJPXOJLYPcKmEM9aDj0O8V0Op07u6CkHclJhZhUwlW606OaPVI-W6Qe3i1cu6O48qXaIUM5YSrzpPN9sPN8lSZKqDoqow1dw0_0PdGBu6UZuenw16l__H-texdH_a1g0We20W820W0IO6l70j06m6kJdYOkuzAtWRlKQ0G000FWQkBfUWXibCJOoDZarE3SpEJKrDZKtE2qnCJ8rEJ8mE3StDJOmC34nDZWpD9WRzSnHi1jZk1i4WXmDDt8vEcX7MMf9LtfID-aSy3-07V__Vnu0OHkaKxCXhRUKX0sYPqhLbcSbyIV1dQMSPqCMNRP7vizShIruBZeS7g32qL37c5z2Zd-lmh1Ci0MmdkggADr7Qx4S4m1gTtgPMDw0rHwVpJt743JwK-Thad6q-a6JMoKP2dq6~1"
                                                        },
                                                        "text": {
                                                            "banner_flags":"milk-substitute,animated",
                                                            "domain":"nestlebaby.ru",
                                                            "lang":"1",
                                                            "warning":"",
                                                            "age":""
                                                        },
                                                        "assets": {
                                                            "button": {}
                                                        },
                                                        "object_id":"72057604955260476"
                                                    },
                                                    "misc": {
                                                        "target_url":"http://ru.yandex.auto-video",
                                                        "unmoderated": {},
                                                        "click_url": {
                                                            "abuse":"//an.yandex.ru/abuse/WD8ejI_z8EnD1W042sPP0Y4C3vN01G3i036KQ_bW000003ZcYpPmyAQpyiG9a07ucCghqO20W0AO0VYOogjHs06WdO2S0UW1l0EW0kBRfmN91b2PWdwtyx5NgGV5oJHulTkmET070j08W820W0A02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G2v3f2E10313aX70W3O5S6AzkoZZxpyOu4Ny3_u680PWXmDDt8vEcX7MMf9LtfID-aSW1r_201K8DXK9BYKXhDaKCJmBcKEm8h00b80~1"
                                                        },
                                                        "trackers": [
                                                            "https://ad.doubleclick.net/ddm/trackimp/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;ord=1717109281;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=?",
                                                            "https://1717109281.verify.yandex.ru/verify?platformid=1&msid=msva5b55_5-63579728-10917332540&BID=10917332540&BTYPE=1&CID=63579728&DRND=1717109281&DTYPE=desktop&REF=https%3A%2F%2Ffrontend.vh.yandex.ru%2Fplayer%2F408d213edf788dad85e56aa5bfe09bd6%3Fuse_friendly_frame%3Dtrue%26vsid%3D9188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%26from%3Dya-weather%26reqid%3D1626958739556578-11259208775600116834%26flags%3D%257B%2522player_api_v2%2522%253A%2522false%2522%252C%2522disable_autoplay_slow_connection%2522%253A%2522false%2522%252C%2522disable_autoplay_save_data%2522%253A%2522false%2522%252C%2522poll_timeout%2522%253A%252221600%2522%252C%2522poll_show_before_midroll%2522%253A%2522false%2522%252C%2522version%2522%253A%2522undefined%2522%252C%2522progress%2522%253A%2522true%2522%252C%2522start_position_confirmation%2522%253A%2522false%2522%252C%2522force_unmute%2522%253A%2522true%2522%252C%2522restore_playback_progress%2522%253A%2522false%2522%252C%2522hide_brand_play_button%2522%253A%2522false%2522%257D%26stream_url%3Dhttps%253A%252F%252Fstrm.yandex.ru%252Fkal%252Fweather_moscow%252Fysign1%253Da9f3470057779456717984b3b3295b6c122b621e4efd76d0f8782bab1ce8f824%252CabcID%253D105%252Cfrom%253Dya-weather%252Cpfx%252Cregion%253D10000%252Csfx%252Cts%253D60fabd13%252Fmanifest.mpd%253Ffrom%253Dya-weather%2526partner_id%253D443123%2526target_ref%253Dhttps%25253A%25252F%25252Fyastatic.net%25252Fyandex-video-player-iframe-api%25252Fjs%25252Fplayer-api-adapter-loader.js%2526uuid%253D408d213edf788dad85e56aa5bfe09bd6%2526video_category_id%253D1025%2526clid%253D495%2526yandexuid%253D6319099721604569424%2526slots%253Dnull%2526imp_id%253D1%2526reqid%253D1626958739556578-11259208775600116834%2526content_id%253D408d213edf788dad85e56aa5bfe09bd6%2526from_block%253Dother%2526channel_id%253D1551792230%2526content-genre%253D%2526content-category%253D%2526brand-safety-categories%253D%25255B%25255D%2526sandbox_version%253D0x836c75a4440%26additional_params%3D%257B%2522from%2522%253A%2522ya-weather%2522%252C%2522reqid%2522%253A%25221626958739556578-11259208775600116834%2522%252C%2522vsid%2522%253A%25229188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%2522%252C%2522adsid%2522%253A%2522d4406f3b827c8108d733370b4db28a244117dc985457xWEBx7099x1626958738%2522%252C%2522content_id%2522%253A%2522408d213edf788dad85e56aa5bfe09bd6%2522%252C%2522from_block%2522%253A%2522other%2522%252C%2522channel_id%2522%253A%25221551792230%2522%252C%2522content-genre%2522%253A%2522%2522%252C%2522content-category%2522%253A%2522%2522%252C%2522brand-safety-categories%2522%253A%2522%255B%255D%2522%252C%2522sandbox_version%2522%253A%25220x836c75a4440%2522%257D%26partner_id%3D443123%26category%3D1025%26distr_id%3D0%26video_content_id%3D408d213edf788dad85e56aa5bfe09bd6%26video_content_name%3D%25D0%259F%25D0%25BE%25D0%25B3%25D0%25BE%25D0%25B4%25D0%25B0%2520%25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%26video_genre_name%3D%26preview%3D%252F%252Favatars.mds.yandex.net%252Fget-vh-cv%252F2424227%252F2a0000017ace447bfced2796b7c72daed5fd%252Forig%26host%3Dfrontend.vh.yandex.ru&SESSION=6869341626958739116&hitlogid=4157064062874995141&page=443123"
                                                        ],
                                                        "object_id":"72057604955260476",
                                                        "impId": "2"
                                                    }
                                                }
                                            },
                                            "playbackParameters": {
                                                "showSkipButton": true,
                                                "skipDelay": 5
                                            },
                                            "encounters": [
                                                "https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=14",
                                                "https://an.yandex.ru/rtbcount/1K-WJkUj0Tm100000000U9nJ39ZPLBVzyyPmmwCdPUhttjvWQJab-sS20n1umaH25PaYqs8_7s9ZI6K4YcSUFMbI0H8lPGBoQgy2YLP643cJKMKHAqWdmqB62NqiODPAHWzt9Z1hB-DY0tw6es3-LKQGGNSP6UOmCFnblFTiczDS9WwWo5cc_q3mYacWTdNP-QCd6HXE_BbmS-jnLeQ_J223Mvb1P2-p8f2Soim59ESoWmnVoSpbeC00Cc0ZyvflQwild0s9FyaCyzbLPh1bp6n0yYbp0Fi1pfVC2evWDp7yP7PmuWUpDh0mxc1XFi32V9AbckFBH1DkYxqNM9ujU_ZyshHTYyHkGdp3yC7-8PJrmFuj2yWh2rWvLx0sVDdmQOt-9bO54mEB52D3uzIKD7CBYvJ450lJL5Epn4JJfBHK0yka5BC3h6V-_OS0nfEijVeGiowmAdnbPGGQ_8kLqz3CsDJSqjp8ee7jGb-3tmIs2vkwldp_zkT6-0osZmUsC2vWU_Ayitl7bxKFybQomPnoW2tv11lB8pPUyo9Bgmpto2pFyAeWSm3SDg08",
                                                "https://ad.doubleclick.net/ddm/trackimp/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;ord=1717109281;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=?",
                                                "https://1717109281.verify.yandex.ru/verify?platformid=1&msid=msva5b55_5-63579728-10917332540&BID=10917332540&BTYPE=1&CID=63579728&DRND=1717109281&DTYPE=desktop&REF=https%3A%2F%2Ffrontend.vh.yandex.ru%2Fplayer%2F408d213edf788dad85e56aa5bfe09bd6%3Fuse_friendly_frame%3Dtrue%26vsid%3D9188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%26from%3Dya-weather%26reqid%3D1626958739556578-11259208775600116834%26flags%3D%257B%2522player_api_v2%2522%253A%2522false%2522%252C%2522disable_autoplay_slow_connection%2522%253A%2522false%2522%252C%2522disable_autoplay_save_data%2522%253A%2522false%2522%252C%2522poll_timeout%2522%253A%252221600%2522%252C%2522poll_show_before_midroll%2522%253A%2522false%2522%252C%2522version%2522%253A%2522undefined%2522%252C%2522progress%2522%253A%2522true%2522%252C%2522start_position_confirmation%2522%253A%2522false%2522%252C%2522force_unmute%2522%253A%2522true%2522%252C%2522restore_playback_progress%2522%253A%2522false%2522%252C%2522hide_brand_play_button%2522%253A%2522false%2522%257D%26stream_url%3Dhttps%253A%252F%252Fstrm.yandex.ru%252Fkal%252Fweather_moscow%252Fysign1%253Da9f3470057779456717984b3b3295b6c122b621e4efd76d0f8782bab1ce8f824%252CabcID%253D105%252Cfrom%253Dya-weather%252Cpfx%252Cregion%253D10000%252Csfx%252Cts%253D60fabd13%252Fmanifest.mpd%253Ffrom%253Dya-weather%2526partner_id%253D443123%2526target_ref%253Dhttps%25253A%25252F%25252Fyastatic.net%25252Fyandex-video-player-iframe-api%25252Fjs%25252Fplayer-api-adapter-loader.js%2526uuid%253D408d213edf788dad85e56aa5bfe09bd6%2526video_category_id%253D1025%2526clid%253D495%2526yandexuid%253D6319099721604569424%2526slots%253Dnull%2526imp_id%253D1%2526reqid%253D1626958739556578-11259208775600116834%2526content_id%253D408d213edf788dad85e56aa5bfe09bd6%2526from_block%253Dother%2526channel_id%253D1551792230%2526content-genre%253D%2526content-category%253D%2526brand-safety-categories%253D%25255B%25255D%2526sandbox_version%253D0x836c75a4440%26additional_params%3D%257B%2522from%2522%253A%2522ya-weather%2522%252C%2522reqid%2522%253A%25221626958739556578-11259208775600116834%2522%252C%2522vsid%2522%253A%25229188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%2522%252C%2522adsid%2522%253A%2522d4406f3b827c8108d733370b4db28a244117dc985457xWEBx7099x1626958738%2522%252C%2522content_id%2522%253A%2522408d213edf788dad85e56aa5bfe09bd6%2522%252C%2522from_block%2522%253A%2522other%2522%252C%2522channel_id%2522%253A%25221551792230%2522%252C%2522content-genre%2522%253A%2522%2522%252C%2522content-category%2522%253A%2522%2522%252C%2522brand-safety-categories%2522%253A%2522%255B%255D%2522%252C%2522sandbox_version%2522%253A%25220x836c75a4440%2522%257D%26partner_id%3D443123%26category%3D1025%26distr_id%3D0%26video_content_id%3D408d213edf788dad85e56aa5bfe09bd6%26video_content_name%3D%25D0%259F%25D0%25BE%25D0%25B3%25D0%25BE%25D0%25B4%25D0%25B0%2520%25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%26video_genre_name%3D%26preview%3D%252F%252Favatars.mds.yandex.net%252Fget-vh-cv%252F2424227%252F2a0000017ace447bfced2796b7c72daed5fd%252Forig%26host%3Dfrontend.vh.yandex.ru&SESSION=6869341626958739116&hitlogid=4157064062874995141&page=443123"
                                            ],
                                            "trackingEvents": {
                                            "start": ["https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=11"],
                                            "trueView": ["https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=19"],
                                            "returnAfterClickThrough": ["http://an.yandex.ru/count/WpyejI_zO0u3hHa0v2ncMG8XP8gx9GK03WGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm042s06WdO2S0U01iedlb07e0Rm30Xniqj4yV4WKy0ACpSs3-nc81SfWa0NAOB05nnYu1SSOm0MrFiW5xm_G1QCCK9c2VhVpiLUf1yN9D7Yzsx0vk0U01PgAW872a846u0ZnxTC2W0eAoGhUudnityBuF_WAWBKOW0kamdI82mIg2n1O9J8qnSa00B1bqTcrn-WBoc3m2mQR1fWDkBfUu0sAW86O3j2oYlJtzlpOLw0Em8GzkGwGZWG000000C4EI4S20CMJy_ZNzxROERWF1iWFe881W13VWk0QY13FyvQ81f0GXlRBmFkmufWYa4G3iEZym13f4di8z94q2pYzy18MY1C4c1C6g1Fcfk7drfQweXRW4ySOW1GWe1J76AWKoc26_RQ71kWKZ0B85Qllz8ok1D0LXlssXmRO5S6AzkoZZxpyO_2W5j3ot8S6g1Re1x0MfmN95j0MpDpUlW7O5e4NY1S1a1UG79WNkPsoAwWN2S0NjXBO5y24FUWN0V0NWFImygmle1WJi1ZokkM71hWO0i8O83GmE6GoCJDbP6OtE3XaOMGuDMKrDc5XDM9cPJ0vOcGsq1WXy1ZC0VWOov6QzEkEjxg-0O0PYHbzBw0PgWEm6RWPWGZI6H9vOM9pNtDbSdPbSYzoDJGtBJBe6Ve3y1cT0lWPwFYZ7e4Q__z7xUZkT7-G6e22W820W82019WQyS2q0R0QvEU9YxZqhU1kzHe10000-1gukbw26oKnDZ8sEJKuDpCvDJKsDJSuBJ4nCZKvCZ0uDpSrDZ0mCJ4sE3Cqc1lrp56m6sEu6mI270qtSZawQ4TPQabNUb8twHpmFu0T__z_7W1X6wHJio6jjvI43Q9dIjMKPoNn9y6SfPndGnPTjYVcprobBNWcEXmUeABHKCSONqAEVg_2e4om1R1UwgeesKThiHmJ06btUebOta3L7f_9FSSGBFfJvsgISPJwGPDJ8naAVGO0~1?test-tag=136"],
                                            "showHp": ["https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=21"],
                                            "clickHp": ["https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=22"],
                                            "adsFinish": ["https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=24"]
                                            },
                                            "isStock": true
                                        }
                                        ]]>
                                    </AdParameters>
                                </Linear>
                                <CreativeExtensions>
                                    <CreativeExtension type="meta">
                                        <yastrm:prefix xmlns:yastrm="http://strm.yandex.ru/schema/vast">
                                            <![CDATA[video_5f982fb15cffc68d5e867ca0]]>
                                        </yastrm:prefix>
                                    </CreativeExtension>
                                </CreativeExtensions>
                            </Creative>
                        </Creatives>
                        <Extensions>
                            <Extension type="controls">
                                <control id="adlabel" layout="1"/>
                                <control id="countdown" layout="1"/>
                                <control id="soundbtn" layout="1"/>
                            </Extension>
                            <Extension type="CustomTracking">
                                <Tracking event="onCreativeInit">
                                    <![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=11]]>
                                </Tracking>
                            </Extension>
                        </Extensions>
                    </InLine>
                </Ad>
                <Ad id="a34sdf" sequence="2">
                    <InLine>
                        <AdSystem>Yabs Ad CPC Server</AdSystem>
                        <AdTitle>Interactive Direct In Video</AdTitle>
                        <Impression id="direct_impression_13"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=13]]></Impression>
                        <Error><![CDATA[https://an.yandex.ru/jserr/3948?errmsg=auto-video-error]]></Error>
                        <Creatives>
                            <Creative id="444444">
                                <Linear>
                                    <Duration>00:00:15</Duration>
                                    <TrackingEvents>
                                        <Tracking event="start"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=0]]></Tracking>
                                        <Tracking event="firstQuartile"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=1]]></Tracking>
                                        <Tracking event="midpoint"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=2]]></Tracking>
                                        <Tracking event="thirdQuartile"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=3]]></Tracking>
                                        <Tracking event="complete"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=4]]></Tracking>
                                        <Tracking event="mute"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=5]]></Tracking>
                                        <Tracking event="unmute"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=6]]></Tracking>
                                        <Tracking event="pause"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=7]]></Tracking>
                                        <Tracking event="resume"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=8]]></Tracking>
                                        <Tracking event="skip"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=9]]></Tracking>
                                    </TrackingEvents>
                                    <MediaFiles/>
                                </Linear>
                                <CreativeExtensions/>
                            </Creative>
                        </Creatives>
                        <Extensions>
                            <Extension type="controls">
                                <control id="adlabel" layout="1"/>
                                <control id="countdown" layout="1"/>
                                <control id="soundbtn" layout="1"/>
                            </Extension>
                        </Extensions>
                    </InLine>
                </Ad>
                <Ad id="777">
                    <Wrapper>
                        <AdSystem>Yabs Ad Server</AdSystem>
                        <VASTAdTagURI><![CDATA[https://path-to-vast]]></VASTAdTagURI>
                    </Wrapper>
                </Ad>
            </VAST>
        `);
        const resultVast = normalizeVast(videoAdapter(bsMeta));

        expect(resultVast).toEqual(expectedVast);
    });

    it('should return correct result with BannerStorage creatives', () => {
        const bsMeta: BSMetaVideo = {
            bidId: '1231313123',
            bids: [{
                vast: `
                <?xml version="1.0" encoding="UTF-8"?>
                <VAST version="3.0">
                    <Ad id="a34sdf">
                        <InLine>
                            <AdSystem>Yabs Ad CPC Server</AdSystem>
                            <AdParameters>
                            <![CDATA[
                                {
                                    "HAS_BUTTON": true,
                                    "theme": "video-banner_interactive-viewer",
                                    "duration": 15.0,
                                    "mediaFiles": [
                                        {
                                            "bitrate": null,
                                            "delivery": "progressive",
                                            "height": null,
                                            "id": "https:/strm.yandex.ru/vh-canvas-converted/get-canvas",
                                            "type": "application/vnd.apple.mpegurl",
                                            "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0.m3u8",
                                            "width": null
                                        }
                                    ],
                                    "socialAdvertising": false,
                                    "AUCTION_DC_PARAMS": {
                                        "creative_params": {
                                            "crypta_user_age": "2",
                                            "crypta_user_gender": "1"
                                        },
                                        "data_params": {
                                            "123": {
                                                "bs_data": {
                                                    "sad": "sad",
                                                    "targetUrl": "",
                                                    "domain": "",
                                                    "count_links": {
                                                        "empty": "bs_data.count_links.empty",
                                                        "abuseUrl": "bs_data.count_links.abuseUrl",
                                                        "tracking": ""
                                                    },
                                                    "resource_links": {
                                                        "direct_data": {
                                                            "targetUrl": "bs_data.resource_links.direct_data.targetUrl",
                                                            "assets": {
                                                                "button": {
                                                                    "href": "bs_data.resource_links.direct_data.assets.button.href"
                                                                }
                                                            }
                                                        }
                                                    },
                                                    "actionButton": "bs_data.actionButton",
                                                    "bannerFlags": "bs_data.bannerFlags",
                                                    "bannerLang": "bs_data.bannerLang",
                                                    "adId": "bs_data.adId",
                                                    "hitLogId": "bs_data.hitLogId",
                                                    "impId": "bs_data.impId"
                                                },
                                                "direct_data": {
                                                    "targetUrl": "direct_data.targetUrl",
                                                    "domain": "direct_data.domain",
                                                    "warning": "direct_data.warning",
                                                    "age": "direct_data.age",
                                                    "title": "direct_data.title",
                                                    "body": "direct_data.body",
                                                    "green_url_text_prefix": "direct_data.green_url_text_prefix",
                                                    "green_url_text_suffix": "direct_data.green_url_text_suffix",
                                                    "assets": {
                                                        "button": {
                                                            "key": "direct_data.assets.button.key",
                                                            "caption": "direct_data.assets.button.caption"
                                                        },
                                                        "logo": {
                                                            "format": "logoFormat",
                                                            "someLogoKey": "someLogoKey"
                                                        }
                                                    },
                                                    "faviconSizes": {
                                                        "w": 100,
                                                        "h": 200
                                                    },
                                                    "admetrica": {
                                                        "someAdmetricaKey": "someAdmetricaKey"
                                                    },
                                                    "tnsId": "direct_data.tnsId",
                                                    "trackers": "direct_data.trackers"
                                                }
                                            }
                                        }
                                    }
                                }
                                ]]>
                            </AdParameters>
                        </InLine>
                    </Ad>
                    <SomeExtraTag></SomeExtraTag>
                </VAST>
                `,
            }, {
                settings: {
                    '3': {
                        linkTail: 'https://an.yandex.ru/rtbcount/1K-WJkUj0Tm100000000U9nJ39ZPLBVzyyPmmwCdPUhttjvWQJab-sS20n1umaH25PaYqs8_7s9ZI6K4YcSUFMbI0H8lPGBoQgy2YLP643cJKMKHAqWdmqB62NqiODPAHWzt9Z1hB-DY0tw6es3-LKQGGNSP6UOmCFnblFTiczDS9WwWo5cc_q3mYacWTdNP-QCd6HXE_BbmS-jnLeQ_J223Mvb1P2-p8f2Soim59ESoWmnVoSpbeC00Cc0ZyvflQwild0s9FyaCyzbLPh1bp6n0yYbp0Fi1pfVC2evWDp7yP7PmuWUpDh0mxc1XFi32V9AbckFBH1DkYxqNM9ujU_ZyshHTYyHkGdp3yC7-8PJrmFuj2yWh2rWvLx0sVDdmQOt-9bO54mEB52D3uzIKD7CBYvJ450lJL5Epn4JJfBHK0yka5BC3h6V-_OS0nfEijVeGiowmAdnbPGGQ_8kLqz3CsDJSqjp8ee7jGb-3tmIs2vkwldp_zkT6-0osZmUsC2vWU_Ayitl7bxKFybQomPnoW2tv11lB8pPUyo9Bgmpto2pFyAeWSm3SDg08',
                        viewNotices: ['https://ad.doubleclick.net/ddm/trackimp/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;ord=1717109281;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=?', 'https://1717109281.verify.yandex.ru/verify?platformid=1&msid=msva5b55_5-63579728-10917332540&BID=10917332540&BTYPE=1&CID=63579728&DRND=1717109281&DTYPE=desktop&REF=https%3A%2F%2Ffrontend.vh.yandex.ru%2Fplayer%2F408d213edf788dad85e56aa5bfe09bd6%3Fuse_friendly_frame%3Dtrue%26vsid%3D9188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%26from%3Dya-weather%26reqid%3D1626958739556578-11259208775600116834%26flags%3D%257B%2522player_api_v2%2522%253A%2522false%2522%252C%2522disable_autoplay_slow_connection%2522%253A%2522false%2522%252C%2522disable_autoplay_save_data%2522%253A%2522false%2522%252C%2522poll_timeout%2522%253A%252221600%2522%252C%2522poll_show_before_midroll%2522%253A%2522false%2522%252C%2522version%2522%253A%2522undefined%2522%252C%2522progress%2522%253A%2522true%2522%252C%2522start_position_confirmation%2522%253A%2522false%2522%252C%2522force_unmute%2522%253A%2522true%2522%252C%2522restore_playback_progress%2522%253A%2522false%2522%252C%2522hide_brand_play_button%2522%253A%2522false%2522%257D%26stream_url%3Dhttps%253A%252F%252Fstrm.yandex.ru%252Fkal%252Fweather_moscow%252Fysign1%253Da9f3470057779456717984b3b3295b6c122b621e4efd76d0f8782bab1ce8f824%252CabcID%253D105%252Cfrom%253Dya-weather%252Cpfx%252Cregion%253D10000%252Csfx%252Cts%253D60fabd13%252Fmanifest.mpd%253Ffrom%253Dya-weather%2526partner_id%253D443123%2526target_ref%253Dhttps%25253A%25252F%25252Fyastatic.net%25252Fyandex-video-player-iframe-api%25252Fjs%25252Fplayer-api-adapter-loader.js%2526uuid%253D408d213edf788dad85e56aa5bfe09bd6%2526video_category_id%253D1025%2526clid%253D495%2526yandexuid%253D6319099721604569424%2526slots%253Dnull%2526imp_id%253D1%2526reqid%253D1626958739556578-11259208775600116834%2526content_id%253D408d213edf788dad85e56aa5bfe09bd6%2526from_block%253Dother%2526channel_id%253D1551792230%2526content-genre%253D%2526content-category%253D%2526brand-safety-categories%253D%25255B%25255D%2526sandbox_version%253D0x836c75a4440%26additional_params%3D%257B%2522from%2522%253A%2522ya-weather%2522%252C%2522reqid%2522%253A%25221626958739556578-11259208775600116834%2522%252C%2522vsid%2522%253A%25229188973a978d8047adcbe4ca904c482f225f3fb22a44xWEBx7099x1626958738%2522%252C%2522adsid%2522%253A%2522d4406f3b827c8108d733370b4db28a244117dc985457xWEBx7099x1626958738%2522%252C%2522content_id%2522%253A%2522408d213edf788dad85e56aa5bfe09bd6%2522%252C%2522from_block%2522%253A%2522other%2522%252C%2522channel_id%2522%253A%25221551792230%2522%252C%2522content-genre%2522%253A%2522%2522%252C%2522content-category%2522%253A%2522%2522%252C%2522brand-safety-categories%2522%253A%2522%255B%255D%2522%252C%2522sandbox_version%2522%253A%25220x836c75a4440%2522%257D%26partner_id%3D443123%26category%3D1025%26distr_id%3D0%26video_content_id%3D408d213edf788dad85e56aa5bfe09bd6%26video_content_name%3D%25D0%259F%25D0%25BE%25D0%25B3%25D0%25BE%25D0%25B4%25D0%25B0%2520%25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%26video_genre_name%3D%26preview%3D%252F%252Favatars.mds.yandex.net%252Fget-vh-cv%252F2424227%252F2a0000017ace447bfced2796b7c72daed5fd%252Forig%26host%3Dfrontend.vh.yandex.ru&SESSION=6869341626958739116&hitlogid=4157064062874995141&page=443123'],
                    },
                },
                dc_params: {
                    creative_params: {
                        crypta_user_gender: '1',
                        crypta_user_age: '2',
                    },
                    data_params: {
                        '123321': {
                            bs_data: {
                                targetUrl: 'https://ad.doubleclick.net/ddm/trackclk/N1280539.286831YANDEX/B26114764.308785963;dc_trk_aid=501523879;dc_trk_cid=154349501;dc_lat=;dc_rdid=;tag_for_child_directed_treatment=;tfua=;ltd=',
                                domain: 'nestlebaby.ru',
                                bannerFlags: 'milk-substitute,animated',
                                bannerLang: '1',
                                adId: '123321',
                                actionButton: 'http://an.yandex.ru/count/Wq0ejI_zO0y3jHa0z2ncMG8XsZbYe0K03mGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm0760TW1e9s0d07W0RA9xvG1w06y0m8SRDBHF7n85F02ZCtDW_iPY0NAO905oc2m1SSOk0N76C05jJx81UyFq0MZ352PWdwtyx5NgGV5oJHulTkmERW7W0MQYe21mf211k08yUtJ0e0A2iaAtk9yRD_2-3_u2e2r680BfC9qY0i4gWiGM2KoDCN9002mPT7PjSVe2yfWy0i6cmQO3RYwNk0DYe21c0xGiehqz_Rys5UW3i24FRaEa8u4000000313aX70W35a_Fur_Uss3cu3mR83w220O0GtuBW6eWGp_EMY0QG48Rsoy3xiEAO8f140x3e_C0GwH9x2FIHD0iulV0I5eWJ19WJ1gWJvgRXvzQMkg8Mu1F7680K8A0KnnYe5CfWXlssXmRe58m2o1Mhx_IChWJG5ORzjeS6s1N1YlRieu-y_6Fme1RGyjo71gWMw0Um5gS5oHRG5ipSthu1s1Q15uWN0P0Na1oO5xcTiYke5md05xOIs1V0X3te5m7m5u3qiFAiBw0O4x0OyhhbXmQu60B2620qC3XaCZ4pPMHcDpWuP65aE3LbDJPXOJLYPcKmEM9aDj0O8V0Op07u6CkHclJhZhUwlW606OaPVI-W6Qe3i1cu6O48qXaIUM5YSrzpPN9sPN8lSZKqDoqow1dw0_0PdGBu6UZuenw16l__H-texdH_a1g0We20W820W0IO6l70j06m6kJdYOkuzAtWRlKQ0G000FWQkBfUWXibCJOoDZarE3SpEJKrDZKtE2qnCJ8rEJ8mE3StDJOmC34nDZWpD9WRzSnHi1jZk1i4WXmDDt8vEcX7MMf9LtfID-aSy3-07V__Vnu0OHkaKxCXhRUKX0sYPqhLbcSbyIV1dQMSPqCMNRP7vizShIruBZeS7g32qL37c5z2Zd-lmh1Ci0MmdkggADr7Qx4S4m1gTtgPMDw0rHwVpJt743JwK-Thad6q-a6JMoKP2dq6~1',
                                resource_links: {
                                    direct_data: {
                                        targetUrl: 'http://an.yandex.ru/count/WpyejI_zO0u3hHa0v2ncMG8XP8gx9GK03WGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm042s06WdO2S0U01iedlb07e0Rm30Xniqj4yV4WKy0ACpSs3-nc81SfWa0NAOB05nnYu1SSOm0MrFiW5xm_G1QCCK9c2VhVpiLUf1yN9D7Yzsx0vk0U01PgAW872a846u0ZnxTC2W0eAoGhUudnityBuF_WAWBKOW0kamdI82mIg2n1O9J8qnSa00B1bqTcrn-WBoc3m2mQR1fWDkBfUu0sAW86O3j2oYlJtzlpOLw0Em8GzkGwGZWG000000C4EI4S20CMJy_ZNzxROERWF1iWFe881W13VWk0QY13FyvQ81f0GXlRBmFkmufWYa4G3iEZym13f4di8z94q2pYzy18MY1C4c1C6g1Fcfk7drfQweXRW4ySOW1GWe1J76AWKoc26_RQ71kWKZ0B85Qllz8ok1D0LXlssXmRO5S6AzkoZZxpyO_2W5j3ot8S6g1Re1x0MfmN95j0MpDpUlW7O5e4NY1S1a1UG79WNkPsoAwWN2S0NjXBO5y24FUWN0V0NWFImygmle1WJi1ZokkM71hWO0i8O83GmE6GoCJDbP6OtE3XaOMGuDMKrDc5XDM9cPJ0vOcGsq1WXy1ZC0VWOov6QzEkEjxg-0O0PYHbzBw0PgWEm6RWPWGZI6H9vOM9pNtDbSdPbSYzoDJGtBJBe6Ve3y1cT0lWPwFYZ7e4Q__z7xUZkT7-G6e22W820W82019WQyS2q0R0QvEU9YxZqhU1kzHe10000-1gukbw26oKnDZ8sEJKuDpCvDJKsDJSuBJ4nCZKvCZ0uDpSrDZ0mCJ4sE3Cqc1lrp56m6sEu6mI270qtSZawQ4TPQabNUb8twHpmFu0T__z_7W1X6wHJio6jjvI43Q9dIjMKPoNn9y6SfPndGnPTjYVcprobBNWcEXmUeABHKCSONqAEVg_2e4om1R1UwgeesKThiHmJ06btUebOta3L7f_9FSSGBFfJvsgISPJwGPDJ8naAVGO0~1',
                                        actionButton: 'http://an.yandex.ru/count/Wq0ejI_zO0y3jHa0z2ncMG8XsZbYe0K03mGnb6lvO000000uveisG0n80c2C66W4SF2ci_B42O01mVg_0uW1wkw13901-9ZAgz60W802c07ucCghKR01l9ktgmYu0TZPbRmYm0760TW1e9s0d07W0RA9xvG1w06y0m8SRDBHF7n85F02ZCtDW_iPY0NAO905oc2m1SSOk0N76C05jJx81UyFq0MZ352PWdwtyx5NgGV5oJHulTkmERW7W0MQYe21mf211k08yUtJ0e0A2iaAtk9yRD_2-3_u2e2r680BfC9qY0i4gWiGM2KoDCN9002mPT7PjSVe2yfWy0i6cmQO3RYwNk0DYe21c0xGiehqz_Rys5UW3i24FRaEa8u4000000313aX70W35a_Fur_Uss3cu3mR83w220O0GtuBW6eWGp_EMY0QG48Rsoy3xiEAO8f140x3e_C0GwH9x2FIHD0iulV0I5eWJ19WJ1gWJvgRXvzQMkg8Mu1F7680K8A0KnnYe5CfWXlssXmRe58m2o1Mhx_IChWJG5ORzjeS6s1N1YlRieu-y_6Fme1RGyjo71gWMw0Um5gS5oHRG5ipSthu1s1Q15uWN0P0Na1oO5xcTiYke5md05xOIs1V0X3te5m7m5u3qiFAiBw0O4x0OyhhbXmQu60B2620qC3XaCZ4pPMHcDpWuP65aE3LbDJPXOJLYPcKmEM9aDj0O8V0Op07u6CkHclJhZhUwlW606OaPVI-W6Qe3i1cu6O48qXaIUM5YSrzpPN9sPN8lSZKqDoqow1dw0_0PdGBu6UZuenw16l__H-texdH_a1g0We20W820W0IO6l70j06m6kJdYOkuzAtWRlKQ0G000FWQkBfUWXibCJOoDZarE3SpEJKrDZKtE2qnCJ8rEJ8mE3StDJOmC34nDZWpD9WRzSnHi1jZk1i4WXmDDt8vEcX7MMf9LtfID-aSy3-07V__Vnu0OHkaKxCXhRUKX0sYPqhLbcSbyIV1dQMSPqCMNRP7vizShIruBZeS7g32qL37c5z2Zd-lmh1Ci0MmdkggADr7Qx4S4m1gTtgPMDw0rHwVpJt743JwK-Thad6q-a6JMoKP2dq6~1',
                                    },
                                },
                                count_links: {
                                    abuseUrl: '//an.yandex.ru/abuse/WD8ejI_z8EnD1W042sPP0Y4C3vN01G3i036KQ_bW000003ZcYpPmyAQpyiG9a07ucCghqO20W0AO0VYOogjHs06WdO2S0UW1l0EW0kBRfmN91b2PWdwtyx5NgGV5oJHulTkmET070j08W820W0A02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G2v3f2E10313aX70W3O5S6AzkoZZxpyOu4Ny3_u680PWXmDDt8vEcX7MMf9LtfID-aSW1r_201K8DXK9BYKXhDaKCJmBcKEm8h00b80~1',
                                    tracking: 'https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1',
                                    empty: 'https://an.yandex.ru/resource/spacer.gif?',
                                },
                                impId: '3',
                            },
                            direct_data: {},
                            constructor_data: {
                                Duration: 15.0,
                                CreativeId: '444444',
                                AdSystem: 'Yabs Ad CPC Server',
                                SoundbtnLayout: '1',
                                AdlabelLayout: '1',
                                CountdownLayout: '1',
                            },
                        },
                    },
                },
            }],
        };
        const expectedVast = normalizeVast(`
            <?xml version="1.0" encoding="UTF-8" ?>
            <VAST version="3.0" ya-uniformat="true" ya-has-banner-storage-ad="true">
                <Ad id="a34sdf" sequence="1">
                    <InLine>
                        <AdSystem>Yabs Ad CPC Server</AdSystem>
                        <AdParameters>
                        <![CDATA[
                            {
                                "HAS_BUTTON": true,
                                "theme": "video-banner_interactive-viewer",
                                "duration": 15.0,
                                "mediaFiles": [
                                    {
                                        "bitrate": null,
                                        "delivery": "progressive",
                                        "height": null,
                                        "id": "https:/strm.yandex.ru/vh-canvas-converted/get-canvas",
                                        "type": "application/vnd.apple.mpegurl",
                                        "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0.m3u8",
                                        "width": null
                                    }
                                ],
                                "socialAdvertising": false,
                                "AUCTION_DC_PARAMS": {
                                    "sad": "sad",
                                    "creative_params": {
                                        "crypta_user_gender": "1",
                                        "crypta_user_age": "2"
                                    },
                                    "data_params": {
                                        "bs_data.adId": {
                                            "target_url": "direct_data.targetUrl",
                                            "count": "bs_data.count_links.empty",
                                            "click_url": {
                                                "text_name": "bs_data.resource_links.direct_data.targetUrl",
                                                "action_button": "bs_data.actionButton"
                                            },
                                            "text": {
                                                "banner_flags": "bs_data.bannerFlags",
                                                "domain": "direct_data.domain",
                                                "lang": "bs_data.bannerLang",
                                                "warning": "direct_data.warning",
                                                "age": "direct_data.age",
                                                "title": "direct_data.title",
                                                "body": "direct_data.body",
                                                "green_url_text_prefix": "direct_data.green_url_text_prefix",
                                                "green_url_text_suffix": "direct_data.green_url_text_suffix",
                                                "dynamic_disclaimer": "1"
                                            },
                                            "assets": {
                                                "button": {
                                                    "key": "direct_data.assets.button.key",
                                                    "caption": "direct_data.assets.button.caption",
                                                    "href": "bs_data.resource_links.direct_data.assets.button.href"
                                                },
                                                "logo": {
                                                    "logoFormat": {
                                                        "someLogoKey": "someLogoKey"
                                                    }
                                                }
                                            },
                                            "object_id": "bs_data.adId",
                                            "unmoderated": {
                                                "punyDomain": "direct_data.domain",
                                                "faviconWidth": "100",
                                                "faviconHeight": "200",
                                                "measurers": {
                                                    "admetrica": {
                                                        "someAdmetricaKey": "someAdmetricaKey",
                                                        "sessionId": "bs_data.adId:bs_data.hitLogId"
                                                    }
                                                },
                                                "warning": "direct_data.warning"
                                            }
                                        },
                                        "misc": {
                                            "target_url": "http://ru.yandex.auto-video",
                                            "unmoderated": {
                                                "tns_id": "direct_data.tnsId"
                                            },
                                            "click_url": {
                                                "abuse": "bs_data.count_links.abuseUrl"
                                            },
                                            "trackers": [
                                                "direct_data.trackers"
                                            ],
                                            "object_id": "bs_data.adId",
                                            "impId": "bs_data.impId"
                                        }
                                    }
                                }
                            }
                            ]]>
                        </AdParameters>
                    </InLine>
                </Ad>
                <Ad id="a34sdf" sequence="2">
                    <InLine>
                        <AdSystem>Yabs Ad CPC Server</AdSystem>
                        <AdTitle>Interactive Direct In Video</AdTitle>
                        <Impression id="direct_impression_13"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=13]]></Impression>
                        <Error><![CDATA[https://an.yandex.ru/jserr/3948?errmsg=auto-video-error]]></Error>
                        <Creatives>
                            <Creative id="444444">
                                <Linear>
                                    <Duration>00:00:15</Duration>
                                    <TrackingEvents>
                                        <Tracking event="start"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=0]]></Tracking>
                                        <Tracking event="firstQuartile"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=1]]></Tracking>
                                        <Tracking event="midpoint"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=2]]></Tracking>
                                        <Tracking event="thirdQuartile"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=3]]></Tracking>
                                        <Tracking event="complete"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=4]]></Tracking>
                                        <Tracking event="mute"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=5]]></Tracking>
                                        <Tracking event="unmute"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=6]]></Tracking>
                                        <Tracking event="pause"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=7]]></Tracking>
                                        <Tracking event="resume"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=8]]></Tracking>
                                        <Tracking event="skip"><![CDATA[https://an.yandex.ru/tracking/WKyejI_zO840BGe0z1400000TwNvJGK0WG4nb6lvO000000uveisO8mOQ0I00S7wlmE80UhkWGoG0VYOoglHW8200fW1-9ZAgr6m0RoRjwi8k07OsPMy8jW1e9s0d07W0RA9xvG1e0BYswS5-0JAOA05l1se1SSOu0LDK9c2VhVpiLUf1yN9D7Yzsx0vq0S41j08W820W0A02Wg02wJ2T8WB1AeB45WbCZJ5oG00i6NHsRN71G3m2mRW3Og0WG6O3j2oYlJtzlpOLxWF1e0GtuBW6iWGa4Jf4di8z94q2pYzW1GWo1G4s1I6_RQ71jWLmOhsxAEFlFnZs1Q15vWNkPsoA_0NWFImygmlq1WXy1ZC_W7u680PWXmDDt8vEcX7MMf9LtfID-aSW1t_Vmi0NJC61H9SfG5Ht2VoDG9f0f8Bx5ZcVQoOzzb9BhTTSbkD0W00~1?action-id=9]]></Tracking>
                                    </TrackingEvents>
                                    <MediaFiles/>
                                </Linear>
                                <CreativeExtensions/>
                            </Creative>
                        </Creatives>
                        <Extensions>
                            <Extension type="controls">
                                <control id="adlabel" layout="1"/>
                                <control id="countdown" layout="1"/>
                                <control id="soundbtn" layout="1"/>
                            </Extension>
                        </Extensions>
                    </InLine>
                </Ad>
            </VAST>
        `);
        const resultVast = normalizeVast(videoAdapter(bsMeta));

        expect(resultVast).toEqual(expectedVast);
    });

    it('should throw error if BannerStorage creative is invalid', () => {
        const bsMeta: BSMetaVideo = {
            bidId: '1231313123',
            bids: [{
                vast: `
                <?xml version="1.0" encoding="UTF-8"?>
                <VAST version="3.0">
                    <SomeExtraTag></SomeExtraTag>
                </VAST>
                `,
            }],
        };

        expect(() => videoAdapter(bsMeta)).toThrowError('Cannot update AUCTION_DC_PARAMS in BannerStorage creative');
    });
});
