// flowlint-next-line untyped-import: off
import {queryByText} from '@testing-library/dom';

import {initContext} from '../helpers';

const widgetPath = '@self/root/src/widgets/content/cart/CartYaPlusPromo';

const LINK_LABEL = 'Подробнее';
const LINK_HREF = 'https://localhost/plus.yandex.ru';

export const checkLinkPlusHome = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer,
    mockFunction,
    hasLink
) => {
    await initContext(mandrelLayer);

    await mockFunction(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    if (hasLink) {
        expect(queryByText(container, LINK_LABEL).href).toBe(LINK_HREF);
    } else {
        expect(queryByText(container, LINK_LABEL)).toBeNull();
    }
};
