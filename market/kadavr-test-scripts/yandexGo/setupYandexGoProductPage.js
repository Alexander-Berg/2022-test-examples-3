const {run} = require(`${__dirname}/../utils/basic`);

const YandexGoProductPageState =
    require('@self/root/src/spec/hermione/helpers/yandexGo/pages/YandexGoProductPageState');

/**
 * url страницы товара
 * /yandex-go/product--naushniki-sony-mdr-xb550ap-zelenyi/1723182048
 * ?skuId=100126194561&sku=100126194561&filter-express-delivery=1&offerid=So8ZNXwQlhY-DsR28rA4Nw
 */
module.exports = run(async ({browser}) => {
    const state = new YandexGoProductPageState();

    await state.createSonyHeadphonesState();

    await state.setState(browser);
});
