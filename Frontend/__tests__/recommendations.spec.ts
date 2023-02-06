import { makeRecommendationsUrlForClient } from '../recommendations';

const params = {
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
};

describe('makeRecommendationsUrl', () => {
    it('Возвращает корректный url для заданных параметров', () => {
        const url = makeRecommendationsUrlForClient(params, 'https://yandex.ru');

        expect(url).toBe('https://yandex.ru/search/itditp/?filter_viewed=1&client=turbo&format=json&model_type=ctr&referer=https%3A%2F%2Fmedvisor.ru%2Farticles%2Fimmunitet%2Fkak-dezinfitsirovat-produkty-iz-magazina%2F&platform=is_mobile&os_type=Android&yandexuid=3189080911565775666&puid=118590156&region=213&left_doc_title=dNCa0LDQuiDQtNC10LfQuNC90YTQuNGG0LjRgNC%2B0LLQsNGC0Ywg0L%2FRgNC%2B0LTRg9C60YLRiyDQuNC3INC80LDQs9Cw0LfQuNC90LA%2F&turbo_urls_options=e30%3D&feed_session_id=1592588619294035-999164734435750800300225-hamster-app-host-man-web-yp-66&only_docs_with_images=0&recommender_max_recommendations_override=10&platform=is_mobile&candidates_options=W3siSG9zdCI6Im1lZHZpc29yLnJ1In1d');
    });

    it('Передается параметр snippetsHash', () => {
        const url = makeRecommendationsUrlForClient({ ...params, snippetsHash: 'ZAEC99F9759857F69,Z4113AC3F739070CC,Z8E66DB8E5F03CF09,ZB57F18DA553CBA6D,Z39EE8A1779E742B8,ZB25F1EC096B7F61B,Z8CDB4AE87CC4A4BF,Z5BC5E956C93D4C33,ZE28EDC83D0FBD242,Z18226DA1014F972C' }, 'https://yandex.ru');

        expect(url).toBe('https://yandex.ru/search/itditp/?filter_viewed=1&client=turbo&format=json&model_type=ctr&referer=https%3A%2F%2Fmedvisor.ru%2Farticles%2Fimmunitet%2Fkak-dezinfitsirovat-produkty-iz-magazina%2F&platform=is_mobile&os_type=Android&yandexuid=3189080911565775666&puid=118590156&region=213&left_doc_title=dNCa0LDQuiDQtNC10LfQuNC90YTQuNGG0LjRgNC%2B0LLQsNGC0Ywg0L%2FRgNC%2B0LTRg9C60YLRiyDQuNC3INC80LDQs9Cw0LfQuNC90LA%2F&turbo_urls_options=e30%3D&feed_session_id=1592588619294035-999164734435750800300225-hamster-app-host-man-web-yp-66&snippetsHash=ZAEC99F9759857F69%2CZ4113AC3F739070CC%2CZ8E66DB8E5F03CF09%2CZB57F18DA553CBA6D%2CZ39EE8A1779E742B8%2CZB25F1EC096B7F61B%2CZ8CDB4AE87CC4A4BF%2CZ5BC5E956C93D4C33%2CZE28EDC83D0FBD242%2CZ18226DA1014F972C&only_docs_with_images=0&recommender_max_recommendations_override=10&platform=is_mobile&candidates_options=W3siSG9zdCI6Im1lZHZpc29yLnJ1In1d');
    });
});
