'use strict';

import {makeSuite, importSuite} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

const vendorCreatePermissions = [
    PERMISSIONS.contacts.write,
    PERMISSIONS.opinions.write,
    PERMISSIONS.settings.write,
    PERMISSIONS.recommended.write,
    PERMISSIONS.modelbids.write,
    PERMISSIONS.brandzone.write,
    PERMISSIONS.questions.write,
];

const vendorEntitiesWritePermissions = [PERMISSIONS.entries.write];

/**
 * @param {PageObject.Link} link
 * @param {Object} params
 * @param {number} params.vendor - идентификатор производителя
 * @param {string[]} params.permissionsByVendor - список пермиссий текущего пользователя
 */
export default makeSuite('Кнопка добавить бренд/партнера.', {
    // @ts-expect-error(TS2345) найдено в рамках VNDFRONT-4580
    meta: {
        feature: 'Меню',
        environment: 'testing',
    },
    params: {
        user: 'Пользователь',
    },
    story: importSuite('Link', {
        hooks: {
            async beforeEach() {
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                const {vendor, has} = this.params;
                const hasPermissionForEntriesWrite = has(vendorEntitiesWritePermissions);
                const hasPermissionForVendorCreate = has(vendorCreatePermissions);
                const exist = hasPermissionForEntriesWrite || hasPermissionForVendorCreate;

                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.params.exist = exist;

                if (exist) {
                    // хак: подменяем ID-кейса в зависимости от пермиссий
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.currentTest._meta.id = hasPermissionForEntriesWrite ? 'vendor_auto-46' : 'vendor_auto-507';

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = buildUrl(
                        hasPermissionForEntriesWrite ? ROUTE_NAMES.CAMPAIGNS_NEW : ROUTE_NAMES.NEW_VIRTUAL_VENDOR,
                        {vendor},
                    );

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.caption = hasPermissionForEntriesWrite ? 'Добавить партнера' : 'Добавить бренд';

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep(
                        'Дожидаемся отображения кнопки добавления партнёра/бренда',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        () => this.link.waitForExist(),
                    );
                }
            },
        },
        params: {
            comparison: {
                skipHostname: true,
            },
        },
    }),
});
