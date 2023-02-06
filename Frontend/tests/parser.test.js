const Reader = require('../unistat/lib/fs-reader');
const parse = require('../unistat/lib/parser');
const store = require('../unistat/lib/store');

// Мокаем Date т.к. он используется внутри
const DATE_TO_USE = new Date('2020-03-05T11:41:46+00:00');
global.Date = jest.fn(() => DATE_TO_USE);

jest.mock('../unistat/lib/fs-reader');
jest.mock('../unistat/lib/store', () => {
    let state = null;
    return {
        drop: () => { state = null },
        get: jest.fn(() => Promise.resolve(state)),
        set: jest.fn((name, newState) => {
            state = newState;

            return Promise.resolve();
        }),
        initialize: jest.fn((name, initialState) => {
            if (state === null) {
                state = initialState;
            }

            return Promise.resolve();
        }),
    };
});

describe('log-reader', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        store.drop();
    });

    test('Возвращает распаршеные логи, если больше нечего читать', async() => {
        const stub = [
            '{"timestamp":"2020-03-05T11:41:46+00:00","status":"200","host":"turbo.gazeta.ru","method":"GET","uri":"/social/news/2020/03/05/n_14119417.shtml?brand=news&sign=41680cc2fce44be58938614d369db13de104fa9e73d5fb23cf36a826c81a2e4c%3A1583408358&turbo_uid=BQDUsjUq%2FNCHGZpiiCLCEJoKbuGONr6dwNSrG8sAqMVd0MgIoy2SjgwoPzKU8USJ%2Ba6W4b9VUetwncVcd53cJEI3xYBc%2FH4scIXkeErW5%2Bcy","upstream_connect_time":"-, 0.000","request_time":"0.542","upstream_response_time":"0.500, 0.040","reqid":"ad1770d230fdac2ba504b9d30a0aed15","user_agent":"Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)","x-forwarded-for":""}',
            '{"timestamp":"2020-03-05T11:41:45+00:00","status":"200","host":"turbo.gazeta.ru","method":"GET","uri":"/social/news/2020/03/05/n_14118889.shtml?utm_medium=mobile&utm_referrer=https%3A%2F%2Fyandex.ru%2Fnews&brand=news&no_friendly_url=1&ajax_type=related&load_method=related&turbo_uid=BQA8Yj1RSpvkJr7q3C%2ByktyKoyU5ph3iXjZcO7pH4qI%2FWx%2FnEonNQniaNtDD4xKjfZI%2BpJRqwAXt6RB6op4P7bnlmMmEtMQ4DzSTTsHykE9a&ajax=1&fallback=1&new_overlay=null&depth=null&parent-reqid=a501e1f6d6394736a15bfda9375284df00067-man1-3512&last_related=1&event-id=k7eojyloud&bundles={%22AdvertBottom%22:1,%22LoaderThemeGray%22:1,%22Image%22:0,%22Divider%22:0,%22ImageViewer%22:1,%22Source%22:0,%22Footer%22:0,%22loader%22:1,%22sandwich-menu%22:1,%22modal-handler%22:1,%22link-like%22:1,%22link-like_type_turbo-navigation%22:1,%22share%22:1,%22autoload%22:1,%22modal%22:1,%22sandwich-menu-container%22:1,%22page_with-host-override%22:1,%22unit%22:0,%22unit_rect%22:0,%22unit_text_l%22:0,%22unit_text_xl%22:0,%22page%22:0,%22typo_regular%22:0,%22typo_bold%22:0,%22typo_text_m%22:0,%22typo_text_l%22:0,%22grid%22:0,%22grid_row%22:0,%22grid_cell%22:0,%22grid_col_2%22:0,%22grid_nojustify%22:0,%22grid_wrap%22:0,%22grid_unwrap%22:0,%22hydro%22:0,%22button%22:0,%22advert%22:0,%22icon%22:0,%22cover%22:0,%22header%22:0,%22link%22:0,%22title%22:0,%22title_size_m%22:0,%22description%22:0,%22paragraph%22:0,%22button_size_m%22:0,%22button__label%22:0,%22typo_text_s%22:0,%22related%22:0,%22spin%22:0,%22spin_size_l%22:0,%22spin_progress%22:0}&icons=[%22label%22,%22close-gray%22,%22turbo%22,%22fb%22,%22ok%22,%22tg%22,%22share%22,%22tw%22,%22vk%22,%22close%22]","upstream_connect_time":"0.008","request_time":"0.053","upstream_response_time":"0.056","reqid":"ac99ca5b787ba77983ac9ad17535a21c","user_agent":"Mozilla/5.0 (Linux; Android 5.1.1; SM-J320F Build/LMY47V; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/55.0.2883.91 Mobile Safari/537.36 YandexSearch/6.10","x-forwarded-for":""}',
            '{"timestamp":"2020-03-05T11:41:44+00:00","status":"200","host":"turbo.gazeta.ru","method":"GET","uri":"/social/2020/03/04/12989005.shtml?utm_medium=mobile&no_friendly_url=1&ajax_type=related&load_method=related&ajax=1&fallback=1&new_overlay=null&depth=null&parent-reqid=3ccac37e050a95383cdfe9c8a8f4a06800067-man1-1221&last_related=1&event-id=k7eo7xnfgg&bundles={%22AdvertBottom%22:1,%22LoaderThemeGray%22:1,%22Image%22:0,%22Divider%22:0,%22Embed%22:0,%22Source%22:0,%22Footer%22:0,%22loader%22:1,%22sandwich-menu%22:1,%22modal-handler%22:1,%22share%22:1,%22link-like%22:1,%22link-like_type_turbo-navigation%22:1,%22autoload%22:1,%22modal%22:1,%22sandwich-menu-container%22:1,%22unit%22:0,%22unit_rect%22:0,%22unit_text_l%22:0,%22unit_text_xl%22:0,%22page%22:0,%22typo_regular%22:0,%22typo_bold%22:0,%22typo_text_m%22:0,%22typo_text_l%22:0,%22grid%22:0,%22grid_row%22:0,%22grid_cell%22:0,%22grid_col_2%22:0,%22grid_nojustify%22:0,%22grid_wrap%22:0,%22grid_unwrap%22:0,%22hydro%22:0,%22button%22:0,%22advert%22:0,%22icon%22:0,%22cover%22:0,%22header%22:0,%22link%22:0,%22title%22:0,%22title_size_m%22:0,%22description%22:0,%22paragraph%22:0,%22i%22:0,%22button_size_m%22:0,%22button__label%22:0,%22typo_text_s%22:0,%22related%22:0,%22spin%22:0,%22spin_size_l%22:0,%22spin_progress%22:0,%22page-platform-desktop%22:0}&icons=[%22label%22,%22close-gray%22,%22turbo%22,%22fb%22,%22ok%22,%22tg%22,%22share%22,%22tw%22,%22vk%22,%22close%22]","upstream_connect_time":"0.008","request_time":"0.070","upstream_response_time":"0.068","reqid":"5d8ccd666146084601732505eb60ea88","user_agent":"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:68.0) Gecko/20100101 Firefox/68.0","x-forwarded-for":""}',
        ];
        let i = 0;
        Reader.mockImplementation(() => ({
            init: async() => {},
            readLine: async() => stub[i++] || null,
            done: async() => {},
        }));
        const logs = await parse();

        expect(logs).toHaveLength(3);
    });

    test('Парсит только новые строки', async() => {
        /**
         * Эмулируем чтение файла, сначала читаем первые две строки и отправляем EOF
         * Затем добавляем еще контент и ожидаем, что распарсятся только записи,
         * до первой, встреченой на прошлой итерации, строки
         */
        const stub = [
            '{"timestamp":"2020-03-05T11:41:46+00:00","status":"200","host":"turbo.gazeta.ru","method":"GET","uri":"/social/news/2020/03/05/n_14119417.shtml?brand=news&sign=41680cc2fce44be58938614d369db13de104fa9e73d5fb23cf36a826c81a2e4c%3A1583408358&turbo_uid=BQDUsjUq%2FNCHGZpiiCLCEJoKbuGONr6dwNSrG8sAqMVd0MgIoy2SjgwoPzKU8USJ%2Ba6W4b9VUetwncVcd53cJEI3xYBc%2FH4scIXkeErW5%2Bcy","upstream_connect_time":"-, 0.000","request_time":"0.542","upstream_response_time":"0.500, 0.040","reqid":"ad1770d230fdac2ba504b9d30a0aed15","user_agent":"Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)","x-forwarded-for":""}',
            '{"timestamp":"2020-03-05T11:41:31+00:00","status":"200","host":"turbo.gazeta.ru","method":"GET","uri":"/social/news/2020/03/05/n_14118889.shtml?utm_medium=mobile&utm_referrer=https%3A%2F%2Fyandex.ru%2Fnews&brand=news&no_friendly_url=1&ajax_type=related&load_method=related&turbo_uid=BQA8Yj1RSpvkJr7q3C%2ByktyKoyU5ph3iXjZcO7pH4qI%2FWx%2FnEonNQniaNtDD4xKjfZI%2BpJRqwAXt6RB6op4P7bnlmMmEtMQ4DzSTTsHykE9a&ajax=1&fallback=1&new_overlay=null&depth=null&parent-reqid=a501e1f6d6394736a15bfda9375284df00067-man1-3512&last_related=1&event-id=k7eojyloud&bundles={%22AdvertBottom%22:1,%22LoaderThemeGray%22:1,%22Image%22:0,%22Divider%22:0,%22ImageViewer%22:1,%22Source%22:0,%22Footer%22:0,%22loader%22:1,%22sandwich-menu%22:1,%22modal-handler%22:1,%22link-like%22:1,%22link-like_type_turbo-navigation%22:1,%22share%22:1,%22autoload%22:1,%22modal%22:1,%22sandwich-menu-container%22:1,%22page_with-host-override%22:1,%22unit%22:0,%22unit_rect%22:0,%22unit_text_l%22:0,%22unit_text_xl%22:0,%22page%22:0,%22typo_regular%22:0,%22typo_bold%22:0,%22typo_text_m%22:0,%22typo_text_l%22:0,%22grid%22:0,%22grid_row%22:0,%22grid_cell%22:0,%22grid_col_2%22:0,%22grid_nojustify%22:0,%22grid_wrap%22:0,%22grid_unwrap%22:0,%22hydro%22:0,%22button%22:0,%22advert%22:0,%22icon%22:0,%22cover%22:0,%22header%22:0,%22link%22:0,%22title%22:0,%22title_size_m%22:0,%22description%22:0,%22paragraph%22:0,%22button_size_m%22:0,%22button__label%22:0,%22typo_text_s%22:0,%22related%22:0,%22spin%22:0,%22spin_size_l%22:0,%22spin_progress%22:0}&icons=[%22label%22,%22close-gray%22,%22turbo%22,%22fb%22,%22ok%22,%22tg%22,%22share%22,%22tw%22,%22vk%22,%22close%22]","upstream_connect_time":"0.008","request_time":"0.053","upstream_response_time":"0.056","reqid":"ac99ca5b787ba77983ac9ad17535a21c","user_agent":"Mozilla/5.0 (Linux; Android 5.1.1; SM-J320F Build/LMY47V; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/55.0.2883.91 Mobile Safari/537.36 YandexSearch/6.10","x-forwarded-for":""}',
            null,
            '{"timestamp1":"2020-03-05T11:30:14+00:00","status":"200","host":"turbo.gazeta.ru","method":"GET","uri":"/social/2020/03/04/12989005.shtml?utm_medium=mobile&no_friendly_url=1&ajax_type=related&load_method=related&ajax=1&fallback=1&new_overlay=null&depth=null&parent-reqid=3ccac37e050a95383cdfe9c8a8f4a06800067-man1-1221&last_related=1&event-id=k7eo7xnfgg&bundles={%22AdvertBottom%22:1,%22LoaderThemeGray%22:1,%22Image%22:0,%22Divider%22:0,%22Embed%22:0,%22Source%22:0,%22Footer%22:0,%22loader%22:1,%22sandwich-menu%22:1,%22modal-handler%22:1,%22share%22:1,%22link-like%22:1,%22link-like_type_turbo-navigation%22:1,%22autoload%22:1,%22modal%22:1,%22sandwich-menu-container%22:1,%22unit%22:0,%22unit_rect%22:0,%22unit_text_l%22:0,%22unit_text_xl%22:0,%22page%22:0,%22typo_regular%22:0,%22typo_bold%22:0,%22typo_text_m%22:0,%22typo_text_l%22:0,%22grid%22:0,%22grid_row%22:0,%22grid_cell%22:0,%22grid_col_2%22:0,%22grid_nojustify%22:0,%22grid_wrap%22:0,%22grid_unwrap%22:0,%22hydro%22:0,%22button%22:0,%22advert%22:0,%22icon%22:0,%22cover%22:0,%22header%22:0,%22link%22:0,%22title%22:0,%22title_size_m%22:0,%22description%22:0,%22paragraph%22:0,%22i%22:0,%22button_size_m%22:0,%22button__label%22:0,%22typo_text_s%22:0,%22related%22:0,%22spin%22:0,%22spin_size_l%22:0,%22spin_progress%22:0,%22page-platform-desktop%22:0}&icons=[%22label%22,%22close-gray%22,%22turbo%22,%22fb%22,%22ok%22,%22tg%22,%22share%22,%22tw%22,%22vk%22,%22close%22]","upstream_connect_time":"0.008","request_time":"0.070","upstream_response_time":"0.068","reqid":"5d8ccd666146084601732505eb60ea88","user_agent":"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:68.0) Gecko/20100101 Firefox/68.0","x-forwarded-for":""}',
            '{"timestamp2":"2020-03-05T11:30:14+00:00","status":"200","host":"turbo.gazeta.ru","method":"GET","uri":"/social/2020/03/04/12989005.shtml?utm_medium=mobile&no_friendly_url=1&ajax_type=related&load_method=related&ajax=1&fallback=1&new_overlay=null&depth=null&parent-reqid=3ccac37e050a95383cdfe9c8a8f4a06800067-man1-1221&last_related=1&event-id=k7eo7xnfgg&bundles={%22AdvertBottom%22:1,%22LoaderThemeGray%22:1,%22Image%22:0,%22Divider%22:0,%22Embed%22:0,%22Source%22:0,%22Footer%22:0,%22loader%22:1,%22sandwich-menu%22:1,%22modal-handler%22:1,%22share%22:1,%22link-like%22:1,%22link-like_type_turbo-navigation%22:1,%22autoload%22:1,%22modal%22:1,%22sandwich-menu-container%22:1,%22unit%22:0,%22unit_rect%22:0,%22unit_text_l%22:0,%22unit_text_xl%22:0,%22page%22:0,%22typo_regular%22:0,%22typo_bold%22:0,%22typo_text_m%22:0,%22typo_text_l%22:0,%22grid%22:0,%22grid_row%22:0,%22grid_cell%22:0,%22grid_col_2%22:0,%22grid_nojustify%22:0,%22grid_wrap%22:0,%22grid_unwrap%22:0,%22hydro%22:0,%22button%22:0,%22advert%22:0,%22icon%22:0,%22cover%22:0,%22header%22:0,%22link%22:0,%22title%22:0,%22title_size_m%22:0,%22description%22:0,%22paragraph%22:0,%22i%22:0,%22button_size_m%22:0,%22button__label%22:0,%22typo_text_s%22:0,%22related%22:0,%22spin%22:0,%22spin_size_l%22:0,%22spin_progress%22:0,%22page-platform-desktop%22:0}&icons=[%22label%22,%22close-gray%22,%22turbo%22,%22fb%22,%22ok%22,%22tg%22,%22share%22,%22tw%22,%22vk%22,%22close%22]","upstream_connect_time":"0.008","request_time":"0.070","upstream_response_time":"0.068","reqid":"5d8ccd666146084601732505eb60ea88","user_agent":"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:68.0) Gecko/20100101 Firefox/68.0","x-forwarded-for":""}',
            '{"timestamp3":"2020-03-05T11:30:14+00:00","status":"200","host":"turbo.gazeta.ru","method":"GET","uri":"/social/2020/03/04/12989005.shtml?utm_medium=mobile&no_friendly_url=1&ajax_type=related&load_method=related&ajax=1&fallback=1&new_overlay=null&depth=null&parent-reqid=3ccac37e050a95383cdfe9c8a8f4a06800067-man1-1221&last_related=1&event-id=k7eo7xnfgg&bundles={%22AdvertBottom%22:1,%22LoaderThemeGray%22:1,%22Image%22:0,%22Divider%22:0,%22Embed%22:0,%22Source%22:0,%22Footer%22:0,%22loader%22:1,%22sandwich-menu%22:1,%22modal-handler%22:1,%22share%22:1,%22link-like%22:1,%22link-like_type_turbo-navigation%22:1,%22autoload%22:1,%22modal%22:1,%22sandwich-menu-container%22:1,%22unit%22:0,%22unit_rect%22:0,%22unit_text_l%22:0,%22unit_text_xl%22:0,%22page%22:0,%22typo_regular%22:0,%22typo_bold%22:0,%22typo_text_m%22:0,%22typo_text_l%22:0,%22grid%22:0,%22grid_row%22:0,%22grid_cell%22:0,%22grid_col_2%22:0,%22grid_nojustify%22:0,%22grid_wrap%22:0,%22grid_unwrap%22:0,%22hydro%22:0,%22button%22:0,%22advert%22:0,%22icon%22:0,%22cover%22:0,%22header%22:0,%22link%22:0,%22title%22:0,%22title_size_m%22:0,%22description%22:0,%22paragraph%22:0,%22i%22:0,%22button_size_m%22:0,%22button__label%22:0,%22typo_text_s%22:0,%22related%22:0,%22spin%22:0,%22spin_size_l%22:0,%22spin_progress%22:0,%22page-platform-desktop%22:0}&icons=[%22label%22,%22close-gray%22,%22turbo%22,%22fb%22,%22ok%22,%22tg%22,%22share%22,%22tw%22,%22vk%22,%22close%22]","upstream_connect_time":"0.008","request_time":"0.070","upstream_response_time":"0.068","reqid":"5d8ccd666146084601732505eb60ea88","user_agent":"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:68.0) Gecko/20100101 Firefox/68.0","x-forwarded-for":""}',
            '{"timestamp":"2020-03-05T11:41:46+00:00","status":"200","host":"turbo.gazeta.ru","method":"GET","uri":"/social/news/2020/03/05/n_14119417.shtml?brand=news&sign=41680cc2fce44be58938614d369db13de104fa9e73d5fb23cf36a826c81a2e4c%3A1583408358&turbo_uid=BQDUsjUq%2FNCHGZpiiCLCEJoKbuGONr6dwNSrG8sAqMVd0MgIoy2SjgwoPzKU8USJ%2Ba6W4b9VUetwncVcd53cJEI3xYBc%2FH4scIXkeErW5%2Bcy","upstream_connect_time":"-, 0.000","request_time":"0.542","upstream_response_time":"0.500, 0.040","reqid":"ad1770d230fdac2ba504b9d30a0aed15","user_agent":"Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)","x-forwarded-for":""}',
        ];
        let i = 0;
        Reader.mockImplementation(() => ({
            init: async() => {},
            readLine: async() => stub[i++],
            done: async() => {},
        }));

        expect(await parse()).toHaveLength(2);
        expect(store.set).toBeCalledWith('parser', { firstLinePreviousInvocation: stub[0] });
        expect(await parse()).toHaveLength(3);
        expect(store.set).toBeCalledWith('parser', { firstLinePreviousInvocation: stub[3] });
    });

    test('Не падает, если нельзя распарсить лог', async() => {
        const stub = ['{}}'];
        let i = 0;
        Reader.mockImplementation(() => ({
            init: async() => {},
            readLine: async() => stub[i++] || null,
            done: async() => {},
        }));

        const logs = await parse();
        expect(logs).toEqual([]);
    });
});
