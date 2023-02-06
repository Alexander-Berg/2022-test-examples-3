import USERS from 'spec/lib/constants/users/users';
import buildUrl from 'spec/lib/helpers/buildUrl';
import permitCreator from 'spec/lib/helpers/permit';
import {patchSuite} from 'spec/gemini/lib/gemini-utils';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import P from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import menu from 'spec/gemini/test-suites/components/Menu';
import contactsForm from 'spec/gemini/test-suites/components/Settings/generalInformationTab/contactsForm';
import usersTab from 'spec/gemini/test-suites/components/Settings/usersTab';
import notificationsTabContent from 'spec/gemini/test-suites/components/Settings/notificationsTab/content';
import notificationsTabHints from 'spec/gemini/test-suites/components/Settings/notificationsTab/hints';
import {MakeSuiteProps} from 'spec/gemini/lib/types';

const permit = permitCreator(ROUTE_NAMES.SETTINGS);

export default {
    suiteName: 'Settings',
    childSuites: USERS.reduce((suites, user) => {
        const vendor = 3301;
        const url = buildUrl(ROUTE_NAMES.SETTINGS, {vendor});
        const {hasAccessToPage, permissionsByVendor, has} = permit(user, vendor);

        // Таб "Основная информация"
        const tabSuites: MakeSuiteProps[] = [
            {
                suiteName: 'General Information',
                childSuites: [contactsForm({user, url})],
            },
        ];

        // Таб "Пользователи"
        if (has([P.entries.read, P.offerta.write])) {
            tabSuites.push(
                usersTab({
                    has,
                    user,
                    // В проде не проверяем Маркет.Аналитику, так как услуга не подключена у вендора 3301
                    // @ts-expect-error(TS2304) найдено в рамках VNDFRONT-4580
                    testMarketAnalytics: gemini.ctx.environment !== 'production',
                    url: buildUrl(ROUTE_NAMES.SETTINGS, {
                        vendor,
                        tab: 'users',
                    }),
                }),
            );
        }

        // Таб "Уведомления"
        if (has(P.subscribers.read)) {
            const notificationsTabUrl = buildUrl(ROUTE_NAMES.SETTINGS, {
                vendor,
                tab: 'notifications',
            });

            tabSuites.push({
                suiteName: 'Notifications',
                childSuites: [
                    notificationsTabContent({
                        user,
                        url: notificationsTabUrl,
                    }),
                    patchSuite(notificationsTabHints, {
                        user,
                        url: notificationsTabUrl,
                    }),
                ],
            });
        }

        if (hasAccessToPage) {
            return suites.concat({
                suiteName: user.alias,
                childSuites: [
                    menu({user, permissionsByVendor, vendor, url}),
                    {
                        suiteName: 'Tabs',
                        childSuites: tabSuites,
                    },
                ],
            });
        }

        return suites;
    }, [] as MakeSuiteProps[]),
};
