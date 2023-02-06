import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import Main from '@self/platform/spec/page-objects/main';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import Viewport from '@self/platform/spec/page-objects/Viewport';
import {setCookies} from '@yandex-market/gemini-extended-actions';
import deleteCookie from '@yandex-market/gemini-extended-actions/actions/deleteCookie';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'JournalWebView',
    url: {
        pathname: '/journal/knowledge/kak-vibrat-komplekt-akustiki',
        query: {
            appmarket_webview: 1,
        },
    },
    selector: Main.root,
    ignore: [
        {every: Counter.root},
        {every: `${ScrollBox.root} ${Viewport.root} [data-zone-name="product"]`},
    ],
    before(actions) {
        // DEFAULT_COOKIES выносим в новый массив, чтобы не аффектить другие тесты, т.к. это импортированный объект
        // Не выставляем куки yandex_gid и gdpr, т.к. хотим проверять, что попап выбора региона не появится на странице
        const cookiesToSet = [...DEFAULT_COOKIES]
            .filter((cookie => cookie.name !== 'yandex_gid' && cookie.name !== 'gdpr'));
        setCookies.setCookies.call(actions, cookiesToSet);
    },
    capture() {
    },
    after(actions) {
        deleteCookie.call(actions, 'x-hide-parts');
    },
};
