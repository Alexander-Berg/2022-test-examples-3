import LinkPO from '@self/root/src/components/Link/__pageObject';

import {initContext} from '../helpers';

const widgetPath = '@self/root/src/widgets/content/cart/CartYaPlusPromo';

const LINK_HREF = 'http://localhost/plus.yandex.ru';

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

    const link = container.querySelector(LinkPO.root);

    if (hasLink) {
        expect(link).not.toBeNull();
        expect(link.href).toBe(LINK_HREF);
    } else {
        expect(link).toBeNull();
    }
};
