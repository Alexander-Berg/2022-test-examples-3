import { ERecommendationsCardTypes } from '@yandex-turbo/components/RecommendationsCard/RecommendationsCard.types';
import { IRecommendationsProps, IRecommendationsResponse } from '../Recommendations.types';

export const exampleProps: IRecommendationsProps = {
    url: 'https://yandex.ru/search/itditp/?filter_viewed=1&client=turbo&format=json&model_type=ctr&referer=https%3A%2F%2Fmedvisor.ru%2Farticles%2Fimmunitet%2Fkak-dezinfitsirovat-produkty-iz-magazina%2F&platform=is_mobile&os_type=Android&yandexuid=3189080911565775666&puid=118590156&region=213&left_doc_title=dNCa0LDQuiDQtNC10LfQuNC90YTQuNGG0LjRgNC%2B0LLQsNGC0Ywg0L%2FRgNC%2B0LTRg9C60YLRiyDQuNC3INC80LDQs9Cw0LfQuNC90LA%2F&turbo_urls_options=e30%3D&feed_session_id=1592584871963386-564632540425134220500130-hamster-app-host-vla-web-yp-7&only_docs_with_images=0&recommender_max_recommendations_override=10&platform=is_mobile&candidates_options=W3siSG9zdCI6Im1lZHZpc29yLnJ1In1d',
    recommendationParams: {
        recommendationSettings: {
            additional_cgi_params: '&only_docs_with_images=0&recommender_max_recommendations_override=10&platform=is_mobile&candidates_options=W3siSG9zdCI6Im1lZHZpc29yLnJ1In1d',
            domain_settings: {},
            need_ajax_recommendations: true,
        },
        documentTitle: 'Как дезинфицировать продукты из магазина?',
        platform: 'touch-phone',
        osFamily: 'Android',
        originalUrl: 'https://medvisor.ru/articles/immunitet/kak-dezinfitsirovat-produkty-iz-magazina/',
        passportId: '118590156',
        yandexUid: '3189080911565775666',
        reqid: '1592588619294035-999164734435750800300225-hamster-app-host-man-web-yp-66',
        tld: 'ru',
        region: 213,
    },
    type: ERecommendationsCardTypes.snippetWithSourceBtn,
};

export const mockExampleResponse: IRecommendationsResponse = {
    data: [
        {
            construct: {
                recommendations: [
                    {
                        annotation: 'В России с подозрением на коронавирусную инфекцию COVID-2019 были госпитализированы двое граждан Китая. Именно это стало первыми случаями коронавируса в России...',
                        hash: 'ZAEC99F9759857F69',
                        host: 'medvisor.ru',
                        image: {
                            height: 791,
                            image_sizes: {
                                '244x122': '/max_g480_c6_r16x9_pd10',
                                '366x183': '/crop_g360_c6_r16x9_pd20',
                                '488x244': '/crop_g480_c12_r16x9_pd10',
                                '600x': '/crop_g360_c12_r16x9_pd20',
                                '732x366': '/lc_desktop_768px_r16x9_pd10',
                            },
                            image_source: 'image_snippet_turbo',
                            project_id: 'get-turbo',
                            raw_url: 'https://avatars.mds.yandex.net/get-turbo/2904219/rth88d1dacf4b86d6d3ab09665dcc26f2ac/orig',
                            storage: 'avatar',
                            url: 'rth88d1dacf4b86d6d3ab09665dcc26f2ac',
                            width: 1181,
                        },
                        logurl: 'https://medvisor.ru/articles/gripp-i-legochnye-zabolevaniya/pervye-dva-sluchaya-koronavirusa-v-rossii/',
                        title: 'Первые два случая коронавируса в России',
                        url: '/turbo?text=https%3A//medvisor.ru/articles/gripp-i-legochnye-zabolevaniya/pervye-dva-sluchaya-koronavirusa-v-rossii/',
                        publication_ts: 1591650000,
                    },
                    {
                        annotation: 'Ажиотаж вокруг туалетной бумаги в супермаркетах оставляет нас в легком недоумении, а вот закупиться дезинфицирующими средствами от коронавируса – это на самом деле неплохая идея. Осталось выяснить, какие помогают, а какие не очень...',
                        hash: 'ZB57F18DA553CBA6D',
                        host: 'medvisor.ru',
                        image: {
                            height: 791,
                            image_sizes: {
                                '244x122': '/max_g480_c6_r16x9_pd10',
                                '366x183': '/crop_g360_c6_r16x9_pd20',
                                '488x244': '/crop_g480_c12_r16x9_pd10',
                                '600x': '/crop_g360_c12_r16x9_pd20',
                                '732x366': '/lc_desktop_768px_r16x9_pd10',
                            },
                            image_source: 'image_snippet_turbo',
                            project_id: 'get-turbo',
                            raw_url: 'https://avatars.mds.yandex.net/get-turbo/2813055/rth9438acd2475489bd3e7951ba56371e8c/orig',
                            storage: 'avatar',
                            url: 'rth9438acd2475489bd3e7951ba56371e8c',
                            width: 1181,
                        },
                        logurl: 'https://medvisor.ru/articles/lekarstva-i-protsedury/antiseptik-dlya-ruk-ot-koronavirusa/',
                        title: 'Антисептик для рук от коронавируса: помогает или нет?',
                        url: '/turbo?text=https%3A//medvisor.ru/articles/lekarstva-i-protsedury/antiseptik-dlya-ruk-ot-koronavirusa/',
                        publication_ts: Math.floor(new Date().getTime() / 1000),
                    },
                    {
                        annotation: 'Неизвестные применили против них перцовый газ. Этим утром экоактивисты попытались остановить экскаваторы, которые начали работы в зеленой зоне. Они утверждают, что у строителей нет на это необходимых документов.',
                        hash: 'Z920B2B45DF5E4785',
                        host: 'echo.msk.ru',
                        image: {
                            height: 216,
                            image_source: 'images_thelenta',
                            project_id: 'get-zen_doc',
                            raw_url: 'https://avatars.mds.yandex.net/get-zen_doc/1908497/-3049027974146160417/smart_crop',
                            storage: 'avatar',
                            url: '-3049027974146160417',
                            width: 352,
                            image_sizes: {
                                '244x122': '/max_g480_c6_r16x9_pd10',
                                '366x183': '/crop_g360_c6_r16x9_pd20',
                                '488x244': '/crop_g480_c12_r16x9_pd10',
                                '600x': '/crop_g360_c12_r16x9_pd20',
                                '732x366': '/lc_desktop_768px_r16x9_pd10',
                            },
                        },
                        logurl: 'https://echo.msk.ru/news/2717605-echo.html',
                        publication_ts: Math.floor(new Date().getTime() / 1000),
                        title: 'В Москве задержали противников застройки парка в Кунцево',
                        url: 'https://yandex.ru/turbo/echo.msk.ru/s/news/2717605-echo.html?utm_source=turbo_turbo',
                    },
                ],
            },
        },
    ],
};
