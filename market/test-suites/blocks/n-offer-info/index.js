import {prepareSuite, mergeSuites, makeSuite, makeCase} from 'ginny';

// suites
import PhoneSuite from '@self/platform/spec/hermione/test-suites/blocks/n-phone';
import TabsSuite from '@self/platform/spec/hermione/test-suites/blocks/tabs';
// page-objects
import Phone from '@self/platform/spec/page-objects/n-phone';
import Tabs from '@self/platform/components/Tabs/__pageObject';

const ENCRYPTED_URL = '/redir/encrypted';

/**
 * Тесты на блок n-offer-info.
 * @param {PageObject.OfferInfo} offerInfo
 * @param {PageObject.ProductContentBlock} productContentBlock
 */
export default makeSuite('Информация об офере.', {
    feature: 'Сниппет ко/км',
    story: mergeSuites(
        {
            'Должна содержать': {
                'цену основного офера': makeCase({
                    id: 'marketfront-947',
                    test() {
                        return this.offerInfo
                            .getPrice()
                            .should.eventually.not.to.be.empty;
                    },
                }),
                'верную цену офера и информацию о доставке': makeCase({
                    id: 'marketfront-1998',
                    issue: 'MARKETVERSTKA-26798',
                    feature: 'Цены',
                    async test() {
                        const textPrice = await this.offerInfo.getPrice();

                        const price = typeof textPrice === 'string'
                            ? Number.parseInt(textPrice.replace(/\s/g, ''), 10)
                            : Number.parseInt(textPrice[0].replace(/\s/g, ''), 10);
                        await this.expect(price)
                            .to.equal(5121, 'Цена в попапе дефолтного офера верная');
                        const delivery = await this.productContentBlock.getDeliveryInformation();
                        return this.expect(delivery)
                            .to.equal(
                                'Бесплатно на заказ\nСлужбы доставки:\nДоставка из Москвы',
                                'Информамация о доставке верная'
                            );
                    },
                }),
                'название магазина': makeCase({
                    id: 'marketfront-946',
                    test() {
                        return this.offerInfo
                            .getTitle()
                            .should.eventually.not.to.be.empty;
                    },
                }),
            },

            'Название магазина.': {
                'При клике': {
                    'должно открывать ссылку в новой вкладке': makeCase({
                        id: 'marketfront-950',
                        test() {
                            return this.browser
                                .yaIsLinkOpener(this.offerInfo.shopName, {
                                    target: '_blank',
                                })
                                .should.eventually.to.be.equal(true, 'Ссылка содержит target="_blank"');
                        },
                    }),
                },
                'Имеет корректную ссылку': makeCase({
                    id: 'marketfront-4204',
                    issue: 'MARKETFRONT-25074',
                    async test() {
                        const url = await this.offerInfo.getShopNameUrl();
                        return this.expect(url.path)
                            .to.equal(ENCRYPTED_URL, 'Кликаут ссылка корректна');
                    },
                }),
            },

            'Заголовок офера.': {
                'По-умолчанию': {
                    'не должен быть кликабельным (ссылка или кнопка)': makeCase({
                        id: 'marketfront-948',
                        test() {
                            return this.browser
                                .yaIsClickable(this.offerInfo.cardTitle)
                                .should.eventually.to.be.equal(false, 'Ссылка не является кликабельной');
                        },
                    }),
                },
            },

            'Кнопка "В магазин".': {
                'При клике': {
                    'должна открывать ссылку в новой вкладке': makeCase({
                        id: 'marketfront-949',
                        test() {
                            return this.browser
                                .yaIsLinkOpener(this.offerInfo.actionCPC, {
                                    target: '_blank',
                                })
                                .should.eventually.to.be.equal(true, 'Ссылка содержит target="_blank"');
                        },
                    }),
                },
                'Имеет корректную ссылку': makeCase({
                    id: 'marketfront-4204',
                    issue: 'MARKETFRONT-25074',
                    async test() {
                        const url = await this.offerInfo.getClickoutButtonUrl();
                        return this.expect(url.path)
                            .to.equal(ENCRYPTED_URL, 'Кликаут ссылка корректна');
                    },
                }),
            },
        },

        prepareSuite(PhoneSuite, {
            pageObjects: {
                phone() {
                    return this.createPageObject(Phone, {parent: this.offerInfo.getCardInfoItem(3)});
                },
            },
        }),

        prepareSuite(TabsSuite, {
            params: {
                actualTabs: ['ОПИСАНИЕ ОТ ПРОДАВЦА', 'ФОТО', 'ДОСТАВКА И САМОВЫВОЗ', 'УСЛОВИЯ ПОКУПКИ'],
                selectedTab: 3,
            },
            pageObjects: {
                tabs() {
                    return this.createPageObject(Tabs, {parent: this.offerInfo.details});
                },
            },
        })
    ),
});
