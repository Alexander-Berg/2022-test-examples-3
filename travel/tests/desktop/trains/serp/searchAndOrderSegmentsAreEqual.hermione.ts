import {order} from 'suites/trains';
import moment from 'moment';
import {assert} from 'chai';
import {random} from 'lodash';

import {msk, spb} from 'helpers/project/trains/data/cities';
import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import skipBecauseProblemWithIM from 'helpers/skips/skipBecauseProblemWithIM';
import {TestTrainsOrderPlacesStepPage} from 'helpers/project/trains/pages/TestTrainsOrderPlacesStepPage/TestTrainsOrderPlacesStepPage';
import {isTrainsSearchAndOrderSegmentsNumbersEqual} from 'helpers/project/trains/lib/isTrainsSearchAndOrderSegmentsNumbersEqual';
import {ITestOrderSegmentInfo} from 'helpers/project/trains/components/TestTrainsOrderSegment/TestTrainsOrderSegment';
import {
    SAPSAN_TRAIN_NAME,
    ITestVariantSegmentInfo,
} from 'helpers/project/trains/pages/TestTrainsGenericSearchPage/components/TestTrainsSearchSegment/TestTrainsSearchSegment';

function assertSearchSegmentAndOrderSegment(
    searchSegmentInfo: ITestVariantSegmentInfo,
    orderSegmentInfo: ITestOrderSegmentInfo,
): void {
    assert.isTrue(
        isTrainsSearchAndOrderSegmentsNumbersEqual(
            searchSegmentInfo.number,
            orderSegmentInfo.number,
        ),
        `Номер поискового сегмента (${searchSegmentInfo.number}) не соответствует номеру сегмента на странице выбора мест (${orderSegmentInfo.number})`,
    );

    assert.equal(
        searchSegmentInfo.company,
        orderSegmentInfo.company,
        `Перевозчик поискового сегмента (${searchSegmentInfo.company}) не соответствует перевозчику сегмента на странице выбора мест (${orderSegmentInfo.company})`,
    );

    if (!orderSegmentInfo.departureDate) {
        throw new Error('orderSegmentInfo.departureDate is undefined');
    }

    assert.match(
        orderSegmentInfo.departureDate,
        new RegExp(`^${searchSegmentInfo.departureDate}`),
        `Дата отправления поискового сегмента (${searchSegmentInfo.departureDate}) не соответствует дате отправления сегмента на странице выбора мест (${orderSegmentInfo.departureDate})`,
    );

    assert.equal(
        searchSegmentInfo.departureTime,
        orderSegmentInfo.departureTime,
        `Время отправления поискового сегмента (${searchSegmentInfo.departureTime}) не соответствует времени отправления сегмента на странице выбора мест (${orderSegmentInfo.departureTime})`,
    );

    if (!orderSegmentInfo.arrivalDate) {
        throw new Error('orderSegmentInfo.arrivalDate is undefined');
    }

    assert.match(
        orderSegmentInfo.arrivalDate,
        new RegExp(`${searchSegmentInfo.arrivalDate}`),
        `Дата прибытия поискового сегмента (${searchSegmentInfo.arrivalDate}) не соответствует дате прибытия сегмента на странице выбора мест (${orderSegmentInfo.arrivalDate})`,
    );

    assert.equal(
        searchSegmentInfo.arrivalTime,
        orderSegmentInfo.arrivalTime,
        `Время прибытия поискового сегмента (${searchSegmentInfo.arrivalTime}) не соответствует времени прибытия сегмента на странице выбора мест (${orderSegmentInfo.arrivalTime})`,
    );
}

function assertOrderSegmentAndOrderSegmentAfterRefresh(
    orderSegmentInfo: ITestOrderSegmentInfo,
    orderSegmentInfoAfterRefresh: ITestOrderSegmentInfo,
    params: {withoutER?: boolean} = {},
): void {
    assert.equal(
        orderSegmentInfo.number,
        orderSegmentInfoAfterRefresh.number,
        `Номер сегмента на странице выбора мест (${orderSegmentInfo.number}) не соответствует номеру сегмента на странице выбора мест после обновления страницы (${orderSegmentInfoAfterRefresh.number})`,
    );

    if (!params.withoutER) {
        assert.equal(
            orderSegmentInfo.hasElectronicRegistration,
            orderSegmentInfoAfterRefresh.hasElectronicRegistration,
            `Электронная регистрация сегмента на странице выбора мест (${orderSegmentInfo.hasElectronicRegistration}) не соответствует электронной регистрации сегмента на странице выбора мест после обновления страницы (${orderSegmentInfoAfterRefresh.hasElectronicRegistration})`,
        );
    }

    assert.equal(
        orderSegmentInfo.departureDate,
        orderSegmentInfoAfterRefresh.departureDate,
        `Дата отправления сегмента на странице выбора мест (${orderSegmentInfo.departureDate}) не соответствует дате отправления сегмента на странице выбора мест после обновления страницы (${orderSegmentInfoAfterRefresh.departureDate})`,
    );

    assert.equal(
        orderSegmentInfo.departureTime,
        orderSegmentInfoAfterRefresh.departureTime,
        `Время отправления сегмента на странице выбора мест (${orderSegmentInfo.departureTime}) не соответствует времени отправления сегмента на странице выбора мест после обновления страницы (${orderSegmentInfoAfterRefresh.departureTime})`,
    );

    assert.equal(
        orderSegmentInfo.arrivalDate,
        orderSegmentInfoAfterRefresh.arrivalDate,
        `Дата прибытия сегмента на странице выбора мест (${orderSegmentInfo.arrivalDate}) не соответствует дате прибытия сегмента на странице выбора мест после обновления страницы (${orderSegmentInfoAfterRefresh.arrivalDate})`,
    );

    assert.equal(
        orderSegmentInfo.arrivalTime,
        orderSegmentInfoAfterRefresh.arrivalTime,
        `Время прибытия сегмента на странице выбора мест (${orderSegmentInfo.arrivalTime}) не соответствует времени прибытия сегмента на странице выбора мест после обновления страницы (${orderSegmentInfoAfterRefresh.arrivalTime})`,
    );
}

describe(order.steps.places, () => {
    skipBecauseProblemWithIM();
    it('Совпадение данных о поезде на странице выбора мест с выдачей. Нефирменный вагон', async function () {
        const afterMonthDate = moment()
            .add(1, 'month')
            .add(random(0, 10), 'day')
            .format('YYYY-MM-DD');

        const app = new TestTrainsApp(this.browser);

        await app.goToSearchPage(msk.slug, spb.slug, afterMonthDate, false);

        const {searchPage} = app;
        const {variants} = searchPage;
        const variantAndSegment =
            await variants.findVariantAndSegmentByOptions();

        assert.exists(variantAndSegment, 'Нет нефирменного сегмента');

        const searchSegmentInfo = await variantAndSegment.segment.getInfo();

        await variantAndSegment.variant.clickToBoyActionButton();

        const orderPlacesPage = new TestTrainsOrderPlacesStepPage(this.browser);

        await orderPlacesPage.waitTrainDetailsLoaded();

        assert.equal(await orderPlacesPage.orderSegments.segments.count(), 1);

        const orderSegment = await orderPlacesPage.orderSegments.getSegment();
        const orderSegmentInfo = await orderSegment.getInfo();

        assertSearchSegmentAndOrderSegment(searchSegmentInfo, orderSegmentInfo);

        await this.browser.refresh();

        const orderPlacesPageAfterRefresh = new TestTrainsOrderPlacesStepPage(
            this.browser,
        );

        await orderPlacesPageAfterRefresh.waitTrainDetailsLoaded();

        assert.equal(
            await orderPlacesPageAfterRefresh.orderSegments.segments.count(),
            1,
        );

        const orderSegmentAfterRefresh =
            await orderPlacesPageAfterRefresh.orderSegments.getSegment();
        const orderSegmentInfoAfterRefresh =
            await orderSegmentAfterRefresh.getInfo();

        assertOrderSegmentAndOrderSegmentAfterRefresh(
            orderSegmentInfo,
            orderSegmentInfoAfterRefresh,
        );
    });

    skipBecauseProblemWithIM();
    it('Совпадение данных о поезде на странице выбора мест с выдачей. Сапсан', async function () {
        const afterMonthDate = moment()
            .add(random(10, 15), 'day')
            .format('YYYY-MM-DD');

        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, afterMonthDate, false);

        const {searchPage} = app;
        const {variants} = searchPage;
        const variantAndSegment = await variants.findVariantAndSegmentByOptions(
            {
                isFirm: true,
                firm: SAPSAN_TRAIN_NAME,
            },
        );

        assert.exists(variantAndSegment, 'Нет фирменного сегмента «Сапсан»');

        const searchSegmentInfo = await variantAndSegment.segment.getInfo();

        await variantAndSegment.variant.clickToBoyActionButton();

        const orderPlacesPage = new TestTrainsOrderPlacesStepPage(this.browser);

        await orderPlacesPage.waitTrainDetailsLoaded();

        assert.equal(await orderPlacesPage.orderSegments.segments.count(), 1);

        const orderSegment = await orderPlacesPage.orderSegments.getSegment();
        const orderSegmentInfo = await orderSegment.getInfo();

        assertSearchSegmentAndOrderSegment(searchSegmentInfo, orderSegmentInfo);

        assert.equal(
            searchSegmentInfo.firm,
            orderSegmentInfo.firm,
            `Фирменность поискового сегмента (${searchSegmentInfo.firm}) не соответствует фирменности сегмента на странице выбора мест (${orderSegmentInfo.firm})`,
        );

        await this.browser.refresh();

        const orderPlacesPageAfterRefresh = new TestTrainsOrderPlacesStepPage(
            this.browser,
        );

        await orderPlacesPageAfterRefresh.waitTrainDetailsLoaded();

        assert.equal(
            await orderPlacesPageAfterRefresh.orderSegments.segments.count(),
            1,
        );

        const orderSegmentAfterRefresh =
            await orderPlacesPageAfterRefresh.orderSegments.getSegment();
        const orderSegmentInfoAfterRefresh =
            await orderSegmentAfterRefresh.getInfo();

        assertOrderSegmentAndOrderSegmentAfterRefresh(
            orderSegmentInfo,
            orderSegmentInfoAfterRefresh,
            {withoutER: true},
        );

        assert.equal(
            orderSegmentInfo.firm,
            orderSegmentInfoAfterRefresh.firm,
            `Фирменность сегмента на странице выбора мест (${orderSegmentInfo.arrivalTime}) не соответствует фирменности сегмента на странице выбора мест после обновления страницы (${orderSegmentInfoAfterRefresh.arrivalTime})`,
        );
    });

    skipBecauseProblemWithIM();
    it('Совпадение данных о поезде на странице выбора мест с выдачей. Фирменный вагон', async function () {
        const afterMonthDate = moment()
            .add(1, 'month')
            .add(random(0, 10), 'day')
            .format('YYYY-MM-DD');

        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, afterMonthDate, false);

        const {searchPage} = app;
        const {variants} = searchPage;
        const variantAndSegment = await variants.findVariantAndSegmentByOptions(
            {
                isFirm: true,
                excludeFirms: [SAPSAN_TRAIN_NAME],
            },
        );

        assert.exists(variantAndSegment, 'Не найден фирменный сегмент');

        const searchSegmentInfo = await variantAndSegment.segment.getInfo();

        await variantAndSegment.variant.clickToBoyActionButton();

        const orderPlacesPage = new TestTrainsOrderPlacesStepPage(this.browser);

        await orderPlacesPage.waitTrainDetailsLoaded();

        assert.equal(await orderPlacesPage.orderSegments.segments.count(), 1);

        const orderSegment = await orderPlacesPage.orderSegments.getSegment();
        const orderSegmentInfo = await orderSegment.getInfo();

        assertSearchSegmentAndOrderSegment(searchSegmentInfo, orderSegmentInfo);

        assert.equal(
            searchSegmentInfo.firm,
            orderSegmentInfo.firm,
            `Фирменность поискового сегмента (${searchSegmentInfo.firm}) не соответствует фирменности сегмента на странице выбора мест (${orderSegmentInfo.firm})`,
        );

        await this.browser.refresh();

        const orderPlacesPageAfterRefresh = new TestTrainsOrderPlacesStepPage(
            this.browser,
        );

        await orderPlacesPageAfterRefresh.waitTrainDetailsLoaded();

        assert.equal(
            await orderPlacesPageAfterRefresh.orderSegments.segments.count(),
            1,
        );

        const orderSegmentAfterRefresh =
            await orderPlacesPageAfterRefresh.orderSegments.getSegment();
        const orderSegmentInfoAfterRefresh =
            await orderSegmentAfterRefresh.getInfo();

        assertOrderSegmentAndOrderSegmentAfterRefresh(
            orderSegmentInfo,
            orderSegmentInfoAfterRefresh,
        );

        assert.equal(
            orderSegmentInfo.firm,
            orderSegmentInfoAfterRefresh.firm,
            `Фирменность сегмента на странице выбора мест (${orderSegmentInfo.arrivalTime}) не соответствует фирменности сегмента на странице выбора мест после обновления страницы (${orderSegmentInfoAfterRefresh.arrivalTime})`,
        );
    });
});
