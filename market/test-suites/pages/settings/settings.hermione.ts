'use strict';

import _ from 'lodash';
import {mergeSuites, makeSuite} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import {combinePermissions, excludePermissions, allPermissions} from 'spec/hermione/lib/helpers/permissions';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import subscribersState from 'spec/lib/page-mocks/subscribers.json';
import authoritiesState from 'spec/lib/page-mocks/authorities.json';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

import AUTHORITIES from 'app/constants/authorities';
import RestrictedLabel from 'spec/page-objects/RestrictedLabel';
import MultiTextInput from 'spec/page-objects/MultiTextInput';
import TextB2b from 'spec/page-objects/TextB2b';
import Tags from 'spec/page-objects/Tags';
import BrandForm from 'spec/page-objects/BrandForm';
import AdvancedSettingsForm from 'spec/page-objects/AdvancedSettingsForm';
import ContactsForm from 'spec/page-objects/ContactsForm';
import ManagerCard from 'spec/page-objects/ManagerCard';

import subscribersSingleValueState from './subscribersSingleValueState.json';
import subscribersUsersState from './subscribersUsersState.json';
import categoriesState from './categoriesState.json';

const userStory = makeUserStory(ROUTE_NAMES.SETTINGS);

export default makeSuite('Страница Настройки.', {
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [PERMISSIONS.settings.read, 0], 3301);
            const params = {
                vendor,
                routeParams: {vendor},
            };
            const getAuthorityAddSuiteCreator =
                // @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
                    getRootSelector =>
                    // @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
                    ({suiteName, meta, role}) => {
                        const rootSelector = getRootSelector(role);

                        return {
                            suiteName,
                            meta,
                            suite: 'Authority/__add',
                            params: {
                                login: 'spbtester',
                            },
                            pageObjects: {
                                // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                                multiTextInput() {
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    return this.createPageObject(
                                        'MultiTextInput',
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        this.browser,
                                        `${rootSelector} ${MultiTextInput.root}`,
                                    );
                                },
                                // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                                tags() {
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    return this.createPageObject('Tags', this.browser, `${rootSelector} ${Tags.root}`);
                                },
                                // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                                tag() {
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    return this.createPageObject('Tag', this.tags, this.tags.getItemByIndex(0));
                                },
                            },
                            hooks: {
                                async beforeEach() {
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    await this.browser.vndOpenPage(ROUTE_NAMES.SETTINGS, {
                                        vendor,
                                        tab: 'users',
                                    });
                                },
                            },
                        };
                    };

            const getAuthorityDeleteSuiteCreator =
                // @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
                    getRootSelector =>
                    // @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
                    ({suiteName, meta, role}) => {
                        const rootSelector = getRootSelector(role);

                        return {
                            suiteName,
                            meta,
                            suite: 'Authority/__delete',
                            params: {
                                login: 'spbtester',
                                confirmText: 'Отвязать логин spbtester (изменение будет применено немедленно)?',
                            },
                            pageObjects: {
                                // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                                tags() {
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    return this.createPageObject('Tags', this.browser, `${rootSelector} ${Tags.root}`);
                                },
                                // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                                tag() {
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    return this.createPageObject('Tag', this.tags, this.tags.getItemByIndex(0));
                                },
                            },
                            hooks: {
                                async beforeEach() {
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    await this.browser.vndOpenPage(ROUTE_NAMES.SETTINGS, {
                                        vendor,
                                        tab: 'users',
                                    });
                                },
                            },
                        };
                    };

            // @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
            const getAuthoritySelectorByRole = role => `${RestrictedLabel.root}[data-role="${role}"] ${Tags.wrapper}`;

            const makeAuthorityAddSuite = getAuthorityAddSuiteCreator(getAuthoritySelectorByRole);
            const makeAuthorityDeleteSuite = getAuthorityDeleteSuiteCreator(getAuthoritySelectorByRole);

            /*
             * В блоке управления пользователями по услугам есть добавление балансовых клиентов, поэтому
             * для однозначной идентификации поля с добавлением ролей пользователей используем расширенный селектор
             */
            // @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
            const getProductsAuthoritySelectorByRole = role =>
                `${RestrictedLabel.root}[data-role="${role}"] ${TextB2b.root} ~ ${Tags.wrapper}:first-of-type`;

            const makeProductsAuthorityAddSuite = getAuthorityAddSuiteCreator(getProductsAuthoritySelectorByRole);
            const makeProductsAuthorityDeleteSuite = getAuthorityDeleteSuiteCreator(getProductsAuthoritySelectorByRole);

            return makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    params,
                    pageObjects: {
                        logo: 'Logo',
                        footer: 'Footer',
                        contactsForm: 'ContactsForm',
                        form() {
                            return this.createPageObject(
                                'Form',
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                this.browser,
                                ContactsForm.root,
                            );
                        },
                    },
                    async onSetKadavrState({id}) {
                        switch (id) {
                            case 'vendor_auto-512':
                                // кейс на ограничение бесплатных услуг
                                return this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        hasFreeProducts: false,
                                    },
                                ]);

                            // Кейсы на кнопку Ещё/Свернуть (много данных)
                            case 'vendor_auto-759':
                            case 'vendor_auto-762':
                                return this.browser.setState('vendorsSubscribers', subscribersState);

                            // Удаление подписок (одна почта и логин)
                            case 'vendor_auto-760':
                            case 'vendor_auto-763':
                                return this.browser.setState('vendorsSubscribers', subscribersSingleValueState);

                            // Добавление логинов (коллекция пользователей без выбранных логинов)
                            case 'vendor_auto-761':
                                return this.browser.setState('vendorsSubscribers', subscribersUsersState);

                            // Удаление пользователя из роли
                            case 'vendor_auto-930':
                            case 'vendor_auto-272':
                            case 'vendor_auto-992':
                            case 'vendor_auto-993':
                            case 'vendor_auto-994':
                            case 'vendor_auto-995':
                            case 'vendor_auto-996':
                                return this.browser.setState('vendorsAuthorities', authoritiesState);
                            case 'vendor_auto-938':
                            case 'vendor_auto-941':
                            case 'vendor_auto-871':
                            case 'vendor_auto-933':
                            case 'vendor_auto-935':
                            case 'vendor_auto-936':
                            case 'vendor_auto-264':
                            case 'vendor_auto-263':
                            case 'vendor_auto-524':
                            case 'vendor_auto-258':
                            case 'vendor_auto-262':
                            case 'vendor_auto-279':
                            case 'vendor_auto-259':
                            case 'vendor_auto-261':
                                await this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        categories: [
                                            {id: 90401, name: 'Все товары'},
                                            {id: 90402, name: 'Все товары / Авто'},
                                        ],
                                        brand: {
                                            picture: {
                                                // eslint-disable-next-line  max-len
                                                url: '//avatars.mds.yandex.net/get-mpic/1912105/img_id2910750236376969175.png/orig',
                                            },
                                            id: 722706,
                                            name: 'Cisco',
                                            site: 'https://www.cisco.ru/',
                                            foundationYear: 1984,
                                            country: 'США',
                                            description: 'Описание бренда',
                                            descriptionSource: {
                                                url: 'https://www.cisco.ru/',
                                                text: 'Текст ссылки',
                                            },
                                            recommendedShopsUrl:
                                                'https://market.yandex.ru/journal/info/rekomendatsii-magazinov-cisco',
                                        },
                                    },
                                ]);

                                await this.browser.setState('vendorsCategories', categoriesState);

                                return this.browser.setState('vendorsBrands', [
                                    {
                                        id: 153061,
                                        name: 'Samsung',
                                    },
                                    {
                                        id: 722706,
                                        name: 'Cisco',
                                    },
                                ]);
                            case 'vendor_auto-934':
                            case 'vendor_auto-940':
                                await this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        title: `Название карточки вендора ${vendor}`,
                                        categories: [
                                            {id: 90401, name: 'Все товары'},
                                            {id: 90402, name: 'Все товары / Авто'},
                                        ],
                                        brand: {
                                            id: 722706,
                                            name: 'Cisco',
                                        },
                                    },
                                    {
                                        vendorId: 3302,
                                        title: 'Название карточки вендора 3302',
                                        categories: [{id: 90595, name: 'Приготовление блюд / Микроволновые печи'}],
                                        brand: {
                                            id: 153061,
                                            name: 'Samsung',
                                        },
                                    },
                                ]);

                                await this.browser.setState('vendorsCategories', categoriesState);

                                return this.browser.setState('vendorsBrands', [
                                    {
                                        id: 153061,
                                        name: 'Samsung',
                                    },
                                ]);
                            case 'vendor_auto-284':
                                return this.browser.setState('vendorsLastBrandEditRequest', {
                                    id: 58459,
                                    status: 'CLOSED',
                                    lastUpdatedAt: new Date().toISOString(),
                                    closedAt: new Date().toISOString(),
                                    newBrandData: {
                                        foundationYear: 1666,
                                        country: 'Соединённые Штаты Америки',
                                    },
                                    oldBrandData: {},
                                    comment: 'Исправьте год основания',
                                    fieldsStatusData: {
                                        country: {
                                            status: true,
                                            comment: 'Страна указана корректно',
                                        },
                                        foundationYear: {
                                            status: false,
                                            comment: 'Год указан некорректно',
                                        },
                                    },
                                });
                            case 'vendor_auto-41':
                            case 'vendor_auto-44':
                            case 'vendor_auto-53':
                            case 'vendor_auto-251':
                            case 'vendor_auto-252':
                            case 'vendor_auto-253':
                            case 'vendor_auto-254':
                            case 'vendor_auto-255':
                            case 'vendor_auto-256':
                            case 'vendor_auto-257':
                                return this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        name: 'Евграфий Пискунов',
                                        company: 'Прекрасная компания',
                                        phone: '+79876543210',
                                        email: 'auto@test',
                                        address: 'Бенуа, литера Щ',
                                        offer: false,
                                        guaranteeLetters: [],
                                        trademarkDocuments: [],
                                    },
                                ]);
                            case 'vendor_auto-641':
                            case 'vendor_auto-744':
                                return this.browser.setState('vendorsManager', {
                                    firstName: 'Vendors',
                                    lastName: 'Manager',
                                    photoUrl:
                                        'https://s3.mdst.yandex.net/vendors-public/manager-avatars/robot-vendorsmanager.jpg',
                                    email: 'manager@yandex-team.ru',
                                    phone: {
                                        officeNumber: '+74956666666',
                                        personNumber: '6666',
                                    },
                                });
                            case 'vendor_auto-642':
                                return this.browser.setState('vendorsManager', {
                                    // фейковый признак, чтобы ручка вернула пустой ответ, а не дефолтные значения
                                    isSupport: true,
                                });
                            default:
                                break;
                        }
                    },
                    suites: {
                        common: [
                            {
                                suite: 'Footer',
                                meta: {
                                    environment: 'kadavr',
                                },
                                pageObjects: {
                                    footer() {
                                        return this.createPageObject('Footer');
                                    },
                                },
                            },
                            {
                                suite: 'RestrictPanel',
                                pageObjects: {
                                    panel() {
                                        return this.createPageObject('RestrictPanel');
                                    },
                                },
                            },
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Настройки',
                                },
                            },
                            'Menu',
                        ],
                        byPermissions: {
                            [PERMISSIONS.settings.write]: [
                                {
                                    suite: 'Link',
                                    suiteName: 'Ссылка на условия оферты',
                                    meta: {
                                        id: 'vendor_auto-42',
                                        issue: 'VNDFRONT-2134',
                                        feature: 'Настройки',
                                        environment: 'testing',
                                    },
                                    params: {
                                        vendor,
                                        caption: 'условия оферты',
                                        url: 'https://yandex.ru/legal/vendor/',
                                        external: true,
                                        target: '_blank',
                                    },
                                    pageObjects: {
                                        offerLink() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('OffertaLegalLink', this.contactsForm);
                                        },
                                        link() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('Link', this.offerLink);
                                        },
                                    },
                                    hooks: {
                                        async beforeEach() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            await this.allure.runStep(
                                                'Ожидаем появления формы контактных данных',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                () => this.contactsForm.waitForExist(),
                                            );

                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            await this.browser.allure.runStep(
                                                'Нажимаем на кнопку "Изменить"',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                () => this.form.editButton.click(),
                                            );
                                        },
                                    },
                                },
                                {
                                    suite: 'BrandForm',
                                    pageObjects: {
                                        brandForm: 'BrandForm',
                                        form() {
                                            return this.createPageObject(
                                                'Form',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.browser,
                                                BrandForm.root,
                                            );
                                        },
                                        countrySuggest() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('Suggest', this.brandForm);
                                        },
                                        file() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('FileB2b', this.brandForm);
                                        },
                                    },
                                },
                                {
                                    suite: 'ContactsForm/edit',
                                    pageObjects: {
                                        guaranteeLetter() {
                                            return this.createPageObject(
                                                'DocumentUpload',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.contactsForm,
                                                '[data-e2e="guarantee-letter-editable-document"]',
                                            );
                                        },
                                        trademarkDocument() {
                                            return this.createPageObject(
                                                'DocumentUpload',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.contactsForm,
                                                '[data-e2e=trademark-certificate-editable-document]',
                                            );
                                        },
                                        unlimitedCheckbox() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('CheckboxB2b', this.trademarkDocument);
                                        },
                                        expireDate() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('InputB2b', this.trademarkDocument);
                                        },
                                    },
                                },
                            ],
                            [PERMISSIONS.subscribers.write]: {
                                suite: 'Settings/Notifications',
                                pageObjects: {
                                    notifications: 'Notifications',
                                },
                                hooks: {
                                    async beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.browser.vndOpenPage(ROUTE_NAMES.SETTINGS, {
                                            vendor,
                                            tab: 'notifications',
                                        });
                                    },
                                },
                            },
                            [combinePermissions(
                                PERMISSIONS.entries.read,
                                PERMISSIONS.offerta.write,
                                allPermissions(PERMISSIONS.opinions.write),
                            )]: [
                                // Роль пользователя ответов на отзывы
                                makeAuthorityAddSuite({
                                    suiteName: 'Добавление логина для роли пользователя ответов на отзывы.',
                                    meta: {
                                        issue: 'VNDFRONT-2645',
                                        id: 'vendor_auto-988',
                                    },
                                    role: AUTHORITIES.FEEDBACK_FREE,
                                }),
                                makeAuthorityDeleteSuite({
                                    suiteName: 'Удаление логина для роли пользователя ответов на отзывы.',
                                    meta: {
                                        issue: 'VNDFRONT-2645',
                                        id: 'vendor_auto-993',
                                    },
                                    role: AUTHORITIES.FEEDBACK_FREE,
                                }),
                            ],
                            [combinePermissions(
                                PERMISSIONS.entries.read,
                                PERMISSIONS.offerta.write,
                                allPermissions(PERMISSIONS.questions.write),
                            )]: [
                                // Роль пользователя ответов на вопросы
                                makeAuthorityAddSuite({
                                    suiteName: 'Добавление логина для роли пользователя ответов на вопросы.',
                                    meta: {
                                        issue: 'VNDFRONT-2645',
                                        id: 'vendor_auto-987',
                                    },
                                    role: AUTHORITIES.QUESTIONS,
                                }),
                                makeAuthorityDeleteSuite({
                                    suiteName: 'Удаление логина для роли пользователя ответов на вопросы.',
                                    meta: {
                                        issue: 'VNDFRONT-2645',
                                        id: 'vendor_auto-992',
                                    },
                                    role: AUTHORITIES.QUESTIONS,
                                }),
                            ],
                            [combinePermissions(
                                PERMISSIONS.entries.read,
                                PERMISSIONS.offerta.write,
                                allPermissions(PERMISSIONS.modelbids.write),
                            )]: [
                                // Роль пользователя для услуги продвижения карточек товаров
                                makeProductsAuthorityAddSuite({
                                    suiteName: 'Добавление логина для услуги продвижения карточек товаров.',
                                    meta: {
                                        issue: 'VNDFRONT-2568',
                                        id: 'vendor_auto-990',
                                    },
                                    role: AUTHORITIES.MODEL_BID,
                                }),
                                makeProductsAuthorityDeleteSuite({
                                    suiteName: 'Удаление логина для услуги продвижения карточек товаров.',
                                    meta: {
                                        issue: 'VNDFRONT-2568',
                                        id: 'vendor_auto-995',
                                    },
                                    role: AUTHORITIES.MODEL_BID,
                                }),
                            ],
                            [combinePermissions(
                                PERMISSIONS.entries.read,
                                PERMISSIONS.offerta.write,
                                allPermissions(PERMISSIONS.brandzone.write),
                            )]: [
                                // Роль пользователя для услуги брендзоны
                                makeProductsAuthorityAddSuite({
                                    suiteName: 'Добавление логина для услуги брендзоны.',
                                    meta: {
                                        issue: 'VNDFRONT-2568',
                                        id: 'vendor_auto-991',
                                    },
                                    role: AUTHORITIES.BRANDZONE,
                                }),
                                makeProductsAuthorityDeleteSuite({
                                    suiteName: 'Удаление логина для услуги брендзоны.',
                                    meta: {
                                        issue: 'VNDFRONT-2568',
                                        id: 'vendor_auto-996',
                                    },
                                    role: AUTHORITIES.BRANDZONE,
                                }),
                            ],
                            [PERMISSIONS.entries.write]: [
                                // Роль администратора
                                makeAuthorityAddSuite({
                                    suiteName: 'Добавление логина для роли администратора.',
                                    meta: {
                                        issue: 'VNDFRONT-2645',
                                        id: 'vendor_auto-271',
                                    },
                                    role: AUTHORITIES.ADMIN,
                                }),
                                makeAuthorityDeleteSuite({
                                    suiteName: 'Удаление логина для роли администратора.',
                                    meta: {
                                        issue: 'VNDFRONT-2645',
                                        id: 'vendor_auto-272',
                                    },
                                    role: AUTHORITIES.ADMIN,
                                }),

                                // Роль пользователя спецпроектов
                                makeAuthorityAddSuite({
                                    suiteName: 'Добавление логина для роли пользователя спецпроектов.',
                                    meta: {
                                        issue: 'VNDFRONT-2568',
                                        id: 'vendor_auto-929',
                                    },
                                    role: AUTHORITIES.SPECIAL_PROJECTS,
                                }),
                                makeAuthorityDeleteSuite({
                                    suiteName: 'Удаление логина для роли пользователя спецпроектов.',
                                    meta: {
                                        issue: 'VNDFRONT-2568',
                                        id: 'vendor_auto-930',
                                    },
                                    role: AUTHORITIES.SPECIAL_PROJECTS,
                                }),

                                /**
                                 * кейс должен выполняться только для менеджера с правами settings.write,
                                 * считаем, что у менеджера они есть
                                 * (согласно профилю пользователя manageruserforvendors)
                                 */
                                'ContactsForm/offerta',

                                // аналогично с campaigns.write
                                {
                                    suite: 'Settings/AdvancedSettingsForm',
                                    pageObjects: {
                                        advancedSettingsForm: 'AdvancedSettingsForm',
                                        form() {
                                            return this.createPageObject(
                                                'FinalForm',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.browser,
                                                AdvancedSettingsForm.root,
                                            );
                                        },
                                        suggest() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('Suggest', this.advancedSettingsForm);
                                        },
                                        suggestWithTags() {
                                            return this.createPageObject('Suggest', Tags.wrapper);
                                        },
                                        tags() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('Tags', this.advancedSettingsForm);
                                        },
                                        tag() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('Tag', this.tags, this.tags.getItemByIndex(0));
                                        },
                                        message() {
                                            return this.createPageObject('Messages');
                                        },
                                        popup: 'PopupB2b',
                                    },
                                },
                            ],
                            [PERMISSIONS.manager.write]: [
                                {
                                    suite: 'Menu/managerCard',
                                    meta: {
                                        id: 'vendor_auto-641',
                                        issue: 'VNDFRONT-2790',
                                        environment: 'kadavr',
                                    },
                                    pageObjects: {
                                        managerCard: 'ManagerCard',
                                    },
                                },
                            ],
                            [PERMISSIONS.offerta.write]: [
                                {
                                    suite: 'ContactsForm/offertaAccept',
                                    pageObjects: {
                                        checkbox() {
                                            return this.createPageObject(
                                                'CheckboxB2b',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.contactsForm,
                                            );
                                        },
                                        trademarkDocument() {
                                            return this.createPageObject(
                                                'DocumentUpload',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.contactsForm,
                                                '[data-e2e=trademark-certificate-editable-document]',
                                            );
                                        },
                                    },
                                },
                            ],
                            // Кейс нужно выполнить только для читающего менеджера
                            [combinePermissions(
                                PERMISSIONS.entries.read,
                                excludePermissions(PERMISSIONS.entries.write),
                            )]: [
                                {
                                    suite: 'Menu/managerCard',
                                    suiteName: 'Боковое меню. Блок менеджера для читающего менеджера.',
                                    meta: {
                                        id: 'vendor_auto-744',
                                        issue: 'VNDFRONT-2790',
                                        environment: 'kadavr',
                                    },
                                    pageObjects: {
                                        managerCard() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('ManagerCard', this.footer, ManagerCard.root);
                                        },
                                    },
                                },
                            ],
                            [PERMISSIONS.roles.write]: [
                                {
                                    suite: 'Menu/supportCard',
                                    pageObjects: {
                                        managerCard() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('ManagerCard', this.footer, ManagerCard.root);
                                        },
                                    },
                                },
                            ],
                            // Кейс нужно выполнить только для пользователя брендзоны,
                            // потому что только у него всего один вендор (важно для теста)
                            [combinePermissions(
                                PERMISSIONS.brandzone.write,
                                excludePermissions(
                                    PERMISSIONS.entries.write,
                                    PERMISSIONS.offerta.write, // исключаем админа
                                ),
                            )]: {
                                suite: 'VendorsSearch',
                                suiteName: 'Саджест в шапке приложения для пользователя с одним вендором.',
                                meta: {
                                    issue: 'VNDFRONT-3898',
                                    id: 'vendor_auto-652',
                                    environment: 'testing',
                                },
                                params: {
                                    expectedCount: 1,
                                },
                            },
                            // Проверяем только для админа, потому что нужно проверить саджест
                            // для не-менеджера, и у всех таких пользователей разное количество доступных брендов
                            [combinePermissions(
                                PERMISSIONS.offerta.write,
                                excludePermissions(PERMISSIONS.entries.read, PERMISSIONS.entries.write),
                            )]: {
                                suite: 'VendorsSearch',
                                suiteName: 'Саджест в шапке приложения для пользователя с несколькими вендорами.',
                                meta: {
                                    issue: 'VNDFRONT-3898',
                                    id: 'vendor_auto-653',
                                    environment: 'testing',
                                },
                                params: {
                                    expectedCount: 20,
                                },
                            },
                            [excludePermissions(PERMISSIONS.entries.read, PERMISSIONS.entries.write)]: {
                                suite: 'VendorsSearch/emptySearch',
                                meta: {
                                    issue: 'VNDFRONT-3898',
                                    id: 'vendor_auto-655',
                                    environment: 'testing',
                                },
                            },
                            [allPermissions(
                                PERMISSIONS.contacts.write,
                                PERMISSIONS.entries.write,
                                PERMISSIONS.manager.write,
                            )]: {
                                suite: 'Menu/becomeAManager',
                                pageObjects: {
                                    managerCard() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('ManagerCard', this.footer);
                                    },
                                },
                            },
                            [allPermissions(PERMISSIONS.contacts.write, PERMISSIONS.entries.write)]: {
                                suite: 'Menu/transferToSupport',
                                pageObjects: {
                                    managerCard() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('ManagerCard', this.footer);
                                    },
                                },
                            },
                        },
                    },
                }),
            });
        });

        return mergeSuites(...suites);
    })(),
});
