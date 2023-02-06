// eslint-disable-next-line max-len
import {screen} from '@testing-library/dom';

import HeaderPlusBalancePO from '@self/root/src/widgets/content/header/PlusBalance/components/View/__pageObject';
import YaPlusBadgePO from '@self/root/src/components/YaPlusBadge/__pageObject';
import UserPlusCashbackBalanceBadgePO from '@self/root/src/components/UserPlusCashbackBalanceBadge/__pageObject';
import {BadgeNotification as BadgeNotificationPO} from '@self/root/src/widgets/content/header/PlusBalance/components/BalancePlusIcon/__pageObject';
import {YA_PLUS_BADGE_NOTIFICATION_TYPE} from '@self/root/src/constants/yaPlus';

import {mockHeaderPlusBalance} from './mockFunctionality';

const widgetPath = '@self/root/src/widgets/content/HeaderPlusBalance';

async function initContext(mandrelLayer) {
    await mandrelLayer.initContext();
}

export const checkHeaderPlusBalanceContent = (
    getLayers,
    mockParams,
    expectedParams
) => {
    let container;

    beforeAll(async () => {
        const {mandrelLayer, jestLayer, apiaryLayer} = await getLayers();

        await initContext(mandrelLayer);
        await jestLayer.backend.runCode(mockHeaderPlusBalance, [mockParams]);
        const widget = await apiaryLayer.mountWidget(widgetPath, {});

        container = widget.container;
    });

    it('Бэйдж отображается', async () => {
        const headerPlusBalance = container.querySelector(HeaderPlusBalancePO.root);

        expect(headerPlusBalance).not.toBeNull();
    });

    if (expectedParams.subTitle) {
        it('Бэйдж содержит корректное описание', async () => {
            const badgeSubTitle = screen.getByRole('alert');

            expect(badgeSubTitle).not.toBeNull();
            expect(badgeSubTitle.textContent).toEqual(expectedParams.subTitle);
        });
    }

    it('Бэйдж содержит корректное значение кешбэка', async () => {
        if (expectedParams.cashbackBalance) {
            const cashbackBalance = container.querySelector(UserPlusCashbackBalanceBadgePO.cashbackBalance);
            expect(cashbackBalance).not.toBeNull();
            expect(cashbackBalance.textContent).toEqual(expectedParams.cashbackBalance);

            if (expectedParams.cashbackBalanceWithVisibleHidden) {
                const cashbackBalanceWithVisibleHidden = container.querySelector(UserPlusCashbackBalanceBadgePO.root);
                expect(cashbackBalanceWithVisibleHidden).not.toBeNull();
                expect(cashbackBalanceWithVisibleHidden.textContent).toEqual(expectedParams.cashbackBalanceWithVisibleHidden);
            }
        } else {
            const yaPLusBadge = container.querySelector(YaPlusBadgePO.root);
            expect(yaPLusBadge).not.toBeNull();
        }
    });

    it('Бэйдж содержит корректную нотификацию', async () => {
        if (!expectedParams.notificationType) {
            const badgeNotification = container.querySelector(BadgeNotificationPO.root);
            expect(badgeNotification).toBeNull();
        } else if (expectedParams.notificationType === YA_PLUS_BADGE_NOTIFICATION_TYPE.RED_CIRCLE) {
            const redCircleBadgeNotification = container.querySelector(BadgeNotificationPO.redCircle);
            expect(redCircleBadgeNotification).not.toBeNull();
        } else if (expectedParams.notificationType === YA_PLUS_BADGE_NOTIFICATION_TYPE.FIRE_SIGN) {
            const fireSignBadgeNotification = container.querySelector(BadgeNotificationPO.fireSign);
            expect(fireSignBadgeNotification).not.toBeNull();
        }
    });
};
