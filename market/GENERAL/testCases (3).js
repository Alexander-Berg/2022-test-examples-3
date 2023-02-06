// eslint-disable-next-line max-len
import {waitFor} from '@testing-library/dom';

// PageObjects
import YaPlusMenuItemPO from '@self/root/src/components/YaPlusMenuItem/__pageObject';
import SpecialMenuItemPO from '@self/root/src/components/SpecialMenuItem/__pageObject';
import UserPlusCashbackBalanceBadgePO, {
    BadgeNotification as BadgeNotificationPO,
} from '@self/root/src/components/UserPlusCashbackBalanceBadge/__pageObject';

// constants
import {YA_PLUS_BADGE_NOTIFICATION_TYPE} from '@self/root/src/constants/yaPlus';

import {mockYaPlusMenuItem} from './mockFunctionality';

const widgetPath = '@self/platform/widgets/core/ProfileMenu';

async function initContext(mandrelLayer, user = {}) {
    await mandrelLayer.initContext({user});
}

export const testsYaPlusMenuItem = (
    getLayers,
    mockParams,
    {
        expectedPrimaryText,
        expectedSecondaryText,
        expectedCashbackBalance,
        expectedNotificationType,
    }
) => {
    let container;

    beforeAll(async () => {
        const {mandrelLayer, jestLayer, apiaryLayer} = await getLayers();

        await initContext(mandrelLayer, {isAuth: true});
        await jestLayer.backend.runCode(mockYaPlusMenuItem, [mockParams]);
        const widget = await apiaryLayer.mountWidget(widgetPath, {});

        container = widget.container;

        const root = container.querySelector(YaPlusMenuItemPO.root);

        // Дожидаемся появления меню пользователя
        await waitFor(() => {
            expect(root).toBeVisible();
        });
    });

    it('Пункт меню содержит корректный текст', async () => {
        const primaryText = container.querySelector(`${YaPlusMenuItemPO.root} ${SpecialMenuItemPO.primaryText}`);
        expect(primaryText.textContent).toBe(expectedPrimaryText);

        const secondaryText = container.querySelector(`${YaPlusMenuItemPO.root} ${SpecialMenuItemPO.secondaryText}`);
        expect(secondaryText.textContent).toBe(expectedSecondaryText);
    });

    it('Пункт меню содержит корректное значение кешбэка', async () => {
        const cashbackBalance = container.querySelector(UserPlusCashbackBalanceBadgePO.root);
        expect(cashbackBalance).not.toBeNull();
        expect(cashbackBalance.textContent).toEqual(expectedCashbackBalance);
    });

    it('Бэйдж нотификации корректно отображается в пункте меню', async () => {
        if (!expectedNotificationType) {
            const badgeNotification = container.querySelector(BadgeNotificationPO.root);
            expect(badgeNotification).toBeNull();
        } else if (expectedNotificationType === YA_PLUS_BADGE_NOTIFICATION_TYPE.RED_CIRCLE) {
            const redCircleBadgeNotification = container.querySelector(BadgeNotificationPO.redCircle);
            expect(redCircleBadgeNotification).not.toBeNull();
        } else if (expectedNotificationType === YA_PLUS_BADGE_NOTIFICATION_TYPE.FIRE_SIGN) {
            const fireSignBadgeNotification = container.querySelector(BadgeNotificationPO.fireSign);
            expect(fireSignBadgeNotification).not.toBeNull();
        }
    });
};
