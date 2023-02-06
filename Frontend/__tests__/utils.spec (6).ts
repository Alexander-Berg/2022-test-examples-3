import { processVHEmbedUrl } from '../processVHEmbedUrl';

describe('processVHEmbedUrl', () => {
    it(
        'Возвращает урл для плеера',
        () => {
            const reqCtx = {
                request: { params: { from: ['newswizard'] } },
                slots: '213795,0,60;213794,0,16;213792,0,30;213791,0,48;213790,0,2;34097,0,2;196561,0,49;213146,0,49;42248,0,56;213793,0,46;215859,0,3;214938,0,20;135688,0,25;88170,0,51',
                reqid: '1582918890384537-843412931350836948303480-production-news-app-host-35.sas.yp-c.yandex.net-NEWS-NEWS_STORY',
            };
            const actual = new URL(processVHEmbedUrl(reqCtx, {
                from: 'yxnews',
                autoplay: true,
                url: 'https://frontend.vh.yandex.ru/player/9871754705510729045?rtx-reqid=1%3Athompson%3AqP0nj8V6%3AGhQKCHRob21wc29uEghUaG9tcHNvbjKmARoBMEIZCg1wZXJzaXN0ZW50X2lkEgg4OTM1OTIwMUJxCgV0aXRsZRJo0K3RgNC00L7Qs9Cw0L0g0L_QvtC30LLQvtC90LjQuyDQn9GD0YLQuNC90YMg0L_QvtGB0LvQtSDRg9Cx0LjQudGB0YLQstCwINGC0YPRgNC10YbQutC40YUg0LLQvtC10L3QvdGL0YVKEzM0MjMyNDQwNzE1NzA0NTg4OTcr3g',
            }));
            const expected = new URL('https://frontend.vh.yandex.ru/player/9871754705510729045?rtx-reqid=1%3Athompson%3AqP0nj8V6%3AGhQKCHRob21wc29uEghUaG9tcHNvbjKmARoBMEIZCg1wZXJzaXN0ZW50X2lkEgg4OTM1OTIwMUJxCgV0aXRsZRJo0K3RgNC00L7Qs9Cw0L0g0L_QvtC30LLQvtC90LjQuyDQn9GD0YLQuNC90YMg0L_QvtGB0LvQtSDRg9Cx0LjQudGB0YLQstCwINGC0YPRgNC10YbQutC40YUg0LLQvtC10L3QvdGL0YVKEzM0MjMyNDQwNzE1NzA0NTg4OTcr3g&recommendations=off&from=yxnews&reqid=1582918890384537-843412931350836948303480-production-news-app-host-35.sas.yp-c.yandex.net-NEWS-NEWS_STORY&slots=213795%2C0%2C60%3B213794%2C0%2C16%3B213792%2C0%2C30%3B213791%2C0%2C48%3B213790%2C0%2C2%3B34097%2C0%2C2%3B196561%2C0%2C49%3B213146%2C0%2C49%3B42248%2C0%2C56%3B213793%2C0%2C46%3B215859%2C0%2C3%3B214938%2C0%2C20%3B135688%2C0%2C25%3B88170%2C0%2C51&autoplay=1&mute=1&play_on_visible=1&from_block=news_wizard_hit');

            expect(actual.origin).toEqual(expected.origin);
            expect(actual.pathname).toEqual(expected.pathname);

            actual.searchParams.forEach((val, key) => {
                expect({ [key]: val }).toEqual({ [key]: expected.searchParams.get(key) });
            });
            // на всякий случай, если верхний по параметрам не нашел
            expect(actual.search).toEqual(expected.search);
        }
    );
});
