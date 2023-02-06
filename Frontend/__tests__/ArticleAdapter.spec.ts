import { flowRight } from 'lodash';

import { utilContext } from '@yandex-turbo/applications/health/contexts/util';
import { requestContext, IRequest } from '@yandex-turbo/applications/health/contexts/request';
import { withContext } from '@yandex-turbo/applications/health/utils/context';
import { IUtil, TRawArticle, IHealthArticle } from '@yandex-turbo/applications/health';
import { transformSingleArticle } from '../ArticleAdapter';

const UtilMock: IUtil = {
    reportError: (value: string) => value,
};
const RequestMock: IRequest = {
    data: {
        cgidata: {
            scheme: '',
            hostname: 'yandex.ru',
            path: '',
            args: {

            },
        },
        reqdata: {
            device: 'desktop',
            headers: {},
            cookie: {},
            reqid: '',
            is_yandex_net: 1,
            url: '',
        },
    },
};

describe('Парсинг статей', () => {
    // создаем runner, так как адаптер статей использует разные контексты, необходимые для работы
    const runner = flowRight(
        withContext(() => utilContext.provider(UtilMock)),
        withContext(() => requestContext.provider(RequestMock))
    );

    it('Преобразование статьи', () => {
        const data: TRawArticle = {
            articleId: '3652',
            article_title: 'Болезнь Бехтерева и ошибки диагностики: успеть остановить болезнь',
            article_description: '[\"Содержание\"]',
            article_url: 'https://medaboutme.ru/zdorove/publikacii/stati/sovety_vracha/bolezn_bekhtereva_i_oshibki_diagnostiki_uspet_ostanovit_bolezn/',
            host_name: 'medaboutme.ru',
            publish_timestamp: '1502247600',
            turbo_json: '[{\"content\": \"\\u0427\\u0442\\u043e \\u0442\\u0430\\u043a\\u043e\\u0435 \\u0430\\u043d\\u043a\\u0438\\u043b\\u043e\\u0437\\u0438\\u0440\\u0443\\u044e\\u0449\\u0438\\u0439 \\u0441\\u043f\\u043e\\u043d\\u0434\\u0438\\u043b\\u043e\\u0430\\u0440\\u0442\\u0440\\u0438\\u0442?\", \"block\": \"link\", \"url\": \"#chto-takoe-ankiloziruyushchiy-spondiloartrit\"}]',
            image_url: 'https://avatars.mds.yandex.net/get-health/1750891/aca7e5ab-08c1-42c0-af6f-e5fe79a65ab5/',
            has_annotation: '1',
            logo_url: 'https://favicon.yandex.net/favicon/v2/https%3A//medaboutme.ru/zdorove/publikacii/stati/sovety_vracha/bolezn_bekhtereva_i_oshibki_diagnostiki_uspet_ostanovit_bolezn/',
            author: 'null',
            related_articles: '[{\"article_description\": [\"Американская\"], \"article_title\": \"AGA. Гайдлайн по лечению синдрома раздраженного кишечника 2014\", \"host_name\": \"medspecial.ru\", \"image_url\": null, \"resource_id\": 2932}, {\"article_description\": [\"Баланит\"], \"article_title\": \"Баланит. Симптомы, диагностика, лечение.\", \"host_name\": \"medspecial.ru\", \"image_url\": null, \"resource_id\": 3235}]',
            doctor_annotations: '[{"doctor_id": "250", "doctor_has_profile": false, "doctor_photo_url": "https://avatars.mds.yandex.net/get-health/1756408/doctor_250_photo_190422/", "textbox_line_1": "Статью проверил врач-гастроэнтеролог, терапевт, к.м.н.", "textbox_line_2": "Парамонов Алексей Дмитриевич, Кандидат медицинских наук, Клиника Рассвет"}]',
            ad_blocks: 'null',
        };

        const expected: IHealthArticle = {
            articleId: '3652',
            sourceUrl: 'https://medaboutme.ru/zdorove/publikacii/stati/sovety_vracha/bolezn_bekhtereva_i_oshibki_diagnostiki_uspet_ostanovit_bolezn/',
            sourceName: 'medaboutme.ru',
            title: data.article_title,
            url: '/health/turbo/articles?id=3652',
            category: '',
            canEdit: false,
            imageUrl: 'https://avatars.mds.yandex.net/get-health/1750891/aca7e5ab-08c1-42c0-af6f-e5fe79a65ab5/',
            text: ['Содержание'],
            analytics: [],
            // @ts-ignore
            turboContent: [{
                block: 'link',
                url: '#chto-takoe-ankiloziruyushchiy-spondiloartrit',
                content: 'Что такое анкилозирующий спондилоартрит?',
            }],
            adBlocks: undefined,
            timestamp: 1502247600,
            hasAnnotation: true,
            annotation: {
                avatar: 'https://avatars.mds.yandex.net/get-health/1756408/doctor_250_photo_190422/48x48',
                avatarHD: 'https://avatars.mds.yandex.net/get-health/1756408/doctor_250_photo_190422/96x96',
                title: 'Статью проверил врач-гастроэнтеролог, терапевт, к.м.н.',
                text: 'Парамонов Алексей Дмитриевич, Кандидат медицинских наук, Клиника Рассвет',
            },
            logoUrl: 'https://favicon.yandex.net/favicon/v2/https%3A//medaboutme.ru/zdorove/publikacii/stati/sovety_vracha/bolezn_bekhtereva_i_oshibki_diagnostiki_uspet_ostanovit_bolezn/',
            author: undefined,
            relatedArticles: [
                {
                    articleId: '2932',
                    title: 'AGA. Гайдлайн по лечению синдрома раздраженного кишечника 2014',
                    url: '/health/turbo/articles?id=2932',
                    imageUrl: undefined,
                    sourceName: 'medspecial.ru',
                    sourceUrl: '',
                    text: ['Американская'],
                    analytics: [],
                    category: '',
                    turboContent: null,
                    canEdit: false,
                },
                {
                    articleId: '3235',
                    title: 'Баланит. Симптомы, диагностика, лечение.',
                    url: '/health/turbo/articles?id=3235',
                    imageUrl: undefined,
                    sourceName: 'medspecial.ru',
                    sourceUrl: '',
                    text: ['Баланит'],
                    analytics: [],
                    category: '',
                    turboContent: null,
                    canEdit: false,
                },
            ],
        };

        expect(runner(transformSingleArticle)(data)).toStrictEqual(expected);
    });
});
