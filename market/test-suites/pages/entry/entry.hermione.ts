import {mergeSuites, importSuite, makeSuite, PageObject, Suite} from 'ginny';
import {isEmpty} from 'lodash';

// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import USERS from 'spec/lib/constants/users';

import checkAvailability from '../../components/Page/availability';

const Layout = PageObject.get('LayoutNewbie');

export default makeSuite('Страница Анкеты нового производителя.', {
    story: (() => {
        const suites: Suite[] = [];
        const pageRouteName = ROUTE_NAMES.ENTRY;

        USERS.all.forEach(user => {
            const {permissions, description} = user;

            suites.push(
                makeSuite(`Доступ пользователю ${description}.`, {
                    story: (() => {
                        const userSuites = [checkAvailability({...user, permissions}, pageRouteName, {}, true)];

                        return mergeSuites(...userSuites);
                    })(),
                }),
            );
        });

        USERS.all.forEach(user => {
            const {description, permissions} = user;
            const hasAccessToPage = isEmpty(permissions);

            suites.push(
                makeSuite(`${description}.`, {
                    story: (() => {
                        const userSuites = [];

                        if (hasAccessToPage) {
                            userSuites.push(
                                {
                                    async beforeEach() {
                                        await this.browser.setState('vendorsEntries', {});
                                        // Авторизируемся и возвращаемся на страницу PAGE.routeName
                                        await this.browser.vndProfile(user, pageRouteName);

                                        // Ждем появления объекта Layout
                                        await this.browser.yaWaitForPageObject(Layout);
                                    },
                                },
                                importSuite('Logo', {
                                    meta: {
                                        environment: 'kadavr',
                                    },
                                    params: {
                                        user: user.description,
                                    },
                                    pageObjects: {
                                        logo() {
                                            return this.createPageObject('Logo');
                                        },
                                    },
                                }),
                                importSuite('Link', {
                                    suiteName: 'Ссылка на условия оферты',
                                    meta: {
                                        id: 'vendor_auto-36',
                                        issue: 'VNDFRONT-1653',
                                        environment: 'kadavr',
                                        feature: 'Оферта',
                                    },
                                    params: {
                                        caption: 'условия оферты',
                                        url: 'https://yandex.ru/legal/vendor/',
                                        target: '_blank',
                                    },
                                    pageObjects: {
                                        newbie() {
                                            return this.createPageObject('Newbie');
                                        },
                                        offerLink() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('OffertaLegalLink', this.newbie);
                                        },
                                        link() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('Link', this.offerLink);
                                        },
                                    },
                                    hooks: {
                                        beforeEach() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.allure.runStep(
                                                'Ожидаем появления анкеты нового производителя',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                () => this.newbie.waitForExist(),
                                            );
                                        },
                                    },
                                }),
                                importSuite('NewVirtualVendor/Newbie', {
                                    pageObjects: {
                                        form() {
                                            return this.createPageObject('Form');
                                        },
                                        entryForm() {
                                            return this.createPageObject('EntryForm');
                                        },
                                    },
                                }),
                                importSuite('Page/title', {
                                    params: {
                                        title: 'Анкета нового производителя',
                                        user: user.description,
                                    },
                                }),
                            );
                        }

                        return mergeSuites(...userSuites);
                    })(),
                }),
            );
        });

        return mergeSuites(...suites);
    })(),
});
