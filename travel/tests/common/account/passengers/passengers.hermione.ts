import {assert} from 'chai';
import {passengers as passengersSuites} from 'suites/account';

import {MINUTE} from 'helpers/constants/dates';

import PassengersPage from 'helpers/project/account/pages/PassengersPage/PassengersPage';
import isPassengerInfoEqual from 'helpers/project/account/pages/PassengersPage/lib/checkPassengerInfoEqual';
import {
    prepareDocumentFieldsData,
    preparePassengerInfoFieldsData,
} from 'helpers/project/account/pages/PassengersPage/lib/prepareFieldsData';
import Account from 'helpers/project/common/passport/Account';
import passengerCyrillic from 'helpers/project/account/pages/PassengersPage/data/addPassengerCyrillic';
import passengerLatin from 'helpers/project/account/pages/PassengersPage/data/addPassengerLatin';
import passportAllFields from 'helpers/project/account/pages/PassengersPage/data/addPassportAllFields';
import passportRequiredFields from 'helpers/project/account/pages/PassengersPage/data/addPassportRequiredFields';
import foreignPassportFields from 'helpers/project/account/pages/PassengersPage/data/addForeignPassportRequiredFields';
import birthCertificateRequiredFields from 'helpers/project/account/pages/PassengersPage/data/addBirthCertificateRequiredFields';
import militaryPassportRequiredFields from 'helpers/project/account/pages/PassengersPage/data/addMilitaryPassportRequiredFields';
import addSeaPassportRequiredFields from 'helpers/project/account/pages/PassengersPage/data/addSeaPassportRequiredFields';
import otherDocumentRequiredFields from 'helpers/project/account/pages/PassengersPage/data/addOtherDocumentRequiredFields';
import addPassengerCyrillicWithItn from 'helpers/project/account/pages/PassengersPage/data/addPassengerCyrillicWithItn';
import editPassengerCyrillicWithItn from 'helpers/project/account/pages/PassengersPage/data/editPassengerCyrillicWithItn';
import editPassengerCyrillicWithTitle from 'helpers/project/account/pages/PassengersPage/data/editPassengerCyrillicWithTitle';
import editPassportInvalid from 'helpers/project/account/pages/PassengersPage/data/editPassportInvalid';
import editPassport from 'helpers/project/account/pages/PassengersPage/data/editPassport';

const {name: suiteName, url} = passengersSuites;

describe(suiteName, () => {
    it('???????????????????? ?????????????? ?????????????????? ???? ??????????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(passengerCyrillic, true);

        await page.openPassenger(0);

        assert.isTrue(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerCyrillic),
        );

        await assert.isTrue(
            isPassengerInfoEqual(
                passengerCyrillic,
                await page.passengerInfo.getData(),
            ),
        );
    });

    it('???????????????????? ?????????????? ?????????????????? ???? ????????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(passengerLatin, true);

        await page.openPassenger(0);

        assert.isOk(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerLatin),
        );
        await assert.isOk(
            isPassengerInfoEqual(
                passengerLatin,
                await page.passengerInfo.getData(),
            ),
        );
    });

    it('???????????????????? ?? ???????????????????????????? ?????????????????? ?? ??????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(addPassengerCyrillicWithItn, true);

        await page.openPassenger(0);

        assert.isOk(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(addPassengerCyrillicWithItn),
        );

        await assert.isOk(
            isPassengerInfoEqual(
                addPassengerCyrillicWithItn,
                await page.passengerInfo.getData(),
            ),
            '???????????????????? ?? ?????????????????? ???? ?????????????????? ?????????? ????????????????',
        );

        await page.editPassenger(editPassengerCyrillicWithItn);

        await assert.isOk(
            isPassengerInfoEqual(
                editPassengerCyrillicWithItn,
                await page.passengerInfo.getData(),
            ),
            '???????????????????? ?? ?????????????????? ???? ?????????????????? ?????????? ????????????????????????????',
        );
    });

    hermione.config.testTimeout(4 * MINUTE);
    it('???????????????????? ??????????????????, ?????????? ?????? ???????? ??????????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(passengerLatin, true);

        assert.isOk(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await page.openPassenger(0);

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerLatin),
        );
        await assert.isOk(
            isPassengerInfoEqual(
                passengerLatin,
                await page.passengerInfo.getData(),
            ),
            '???????????? ?????????????????? ???? ??????????????????',
        );

        await page.closePassenger();

        await page.addPassenger(passengerCyrillic, false);

        assert.isTrue(
            (await page.passengers.count()) === 2,
            '???????????????????? ???????????? 2, ???????????? ???????? 2',
        );

        page.openPassenger(1);

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerCyrillic),
        );
        await assert.isTrue(
            isPassengerInfoEqual(
                passengerCyrillic,
                await page.passengerInfo.getData(),
            ),
            '???????????? ?????????????????? ???? ??????????????????',
        );
    });

    it('???????????????????? ???????????????? ???? ?????????? ????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(passengerCyrillic, true);

        await page.openPassenger(0);

        assert.isTrue(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerCyrillic),
        );
        await assert.isTrue(
            isPassengerInfoEqual(
                passengerCyrillic,
                await page.passengerInfo.getData(),
            ),
            '???? ?????????????????? ???????????????????? ?? ?????????????????? ?? ??????????????????',
        );

        await page.documents.removeAllDocuments();

        await page.documents.addDocument(
            passportAllFields,
            '?????????????? ???????????????????? ????',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(
                passportAllFields,
                '?????????????? ???????????????????? ????',
            ),
        );
    });

    it('???????????????????? ???????????????? ???????????? ?? ?????????????????????????? ????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(passengerCyrillic, true);

        await page.openPassenger(0);

        assert.isOk(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerCyrillic),
        );
        await assert.isOk(
            isPassengerInfoEqual(
                passengerCyrillic,
                await page.passengerInfo.getData(),
            ),
            '???????????? ???????????????????? ?????????????????? ??????????????????',
        );

        await page.documents.addDocument(
            passportRequiredFields,
            '?????????????? ???????????????????? ????',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(
                passportRequiredFields,
                '?????????????? ???????????????????? ????',
            ),
        );
    });

    it('???????????????????? ?????????????? ???????????? ?? ?????????????????????????? ????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(passengerCyrillic, true);

        await page.openPassenger(0);

        assert.isOk(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerCyrillic),
        );
        await assert.isOk(
            isPassengerInfoEqual(
                passengerCyrillic,
                await page.passengerInfo.getData(),
            ),
        );

        await page.documents.addDocument(
            foreignPassportFields,
            '?????????????????????? ?????????????? ???????????????????? ????',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(
                foreignPassportFields,
                '?????????????????????? ?????????????? ???????????????????? ????',
            ),
        );
    });

    it('???????????????????? ?????????????????????????? ?? ???????????????? ???????????? ?? ?????????????????????????? ????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(passengerCyrillic, true);

        await page.openPassenger(0);

        assert.isOk(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerCyrillic),
        );
        await assert.isOk(
            isPassengerInfoEqual(
                passengerCyrillic,
                await page.passengerInfo.getData(),
            ),
        );

        await page.documents.addDocument(
            birthCertificateRequiredFields,
            '?????????????????????????? ?? ????????????????',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(
                birthCertificateRequiredFields,
                '?????????????????????????? ?? ????????????????',
            ),
        );
    });

    it('???????????????????? ???????????????? ???????????? ???????????? ?? ?????????????????????????? ????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(passengerCyrillic, true);

        await page.openPassenger(0);

        assert.isOk(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerCyrillic),
        );

        assert.isOk(
            isPassengerInfoEqual(
                passengerCyrillic,
                await page.passengerInfo.getData(),
            ),
        );

        await page.documents.addDocument(
            militaryPassportRequiredFields,
            '?????????????? ??????????',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(
                militaryPassportRequiredFields,
                '?????????????? ??????????',
            ),
        );
    });

    it('???????????????????? ???????????????? ???????????? ???????????? ?? ?????????????????????????? ????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(passengerCyrillic, true);

        await page.openPassenger(0);

        assert.isOk(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerCyrillic),
        );
        await assert.isOk(
            isPassengerInfoEqual(
                passengerCyrillic,
                await page.passengerInfo.getData(),
            ),
        );

        await page.documents.addDocument(
            addSeaPassportRequiredFields,
            '?????????????? ????????????',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(
                addSeaPassportRequiredFields,
                '?????????????? ????????????',
            ),
        );
    });

    it('???????????????????? ?????????????? ?????????????????? (??????????????) ???????????? ?? ?????????????????????????? ????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(passengerCyrillic, true);

        await page.openPassenger(0);

        assert.isOk(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerCyrillic),
        );
        await assert.isOk(
            isPassengerInfoEqual(
                passengerCyrillic,
                await page.passengerInfo.getData(),
            ),
        );

        await page.documents.addDocument(
            otherDocumentRequiredFields,
            '???????????? ????????????????',
        );

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(
                otherDocumentRequiredFields,
                '???????????? ????????????????',
            ),
        );
    });

    it('???????????????????????????? ?????????????????? ??????????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        await page.addPassenger(passengerLatin, true);

        await page.openPassenger(0);

        assert.isOk(
            (await page.passengers.count()) === 1,
            '???????????????????? ???????????? 1, ???????????? ???????? 1',
        );

        await assert.isOk(
            isPassengerInfoEqual(
                passengerLatin,
                await page.passengerInfo.getData(),
            ),
        );

        const currentDoc = await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(passengerLatin),
        );

        await currentDoc.edit.click();

        await page.documents.editDocument(editPassportInvalid);

        assert.isOk(
            await page.documents.formError.isDisplayed(),
            '???????? ?? ?????????????? ???? ????????????????????????',
        );
        assert.equal(
            await page.documents.formError.getText(),
            '?????????????? 10 ???????? ?????? ????????????????',
            '?????????? ???????????? ???? ??????????????????????????',
        );

        await page.documents.editDocument(editPassport);

        await page.documents.isSingleDocumentByFields(
            prepareDocumentFieldsData(editPassport, '?????????????? ???????????????????? ????'),
        );
    });

    it('?????????? ???????????????????? ?? ???????????????? ????????????', async function () {
        const {page} = await getOrCreateAccountAndLogin(this.browser);

        const checkPassenger = async (title: string): Promise<void> => {
            await page.passengers.searchInput.type(title);
            await this.browser.pause(500);
            assert.equal(await page.passengers.count(), 1);

            const passenger = await page.passengers.findPassengerByTitle(title);

            assert.isOk(passenger);
        };

        const {title: passengerWithoutTitleName} =
            preparePassengerInfoFieldsData(passengerCyrillic);
        const {title: passengerWithTitleName} = preparePassengerInfoFieldsData(
            editPassengerCyrillicWithTitle,
        );

        await page.addPassenger(passengerCyrillic, true);
        await page.addPassenger(passengerCyrillic, false);

        await page.openPassenger(1);

        assert.isOk(
            (await page.passengers.count()) === 2,
            '???????????????????? ???????????? 2, ???????????? ???????? 2',
        );

        await page.editPassenger(editPassengerCyrillicWithTitle);
        await page.closePassenger();

        /* ???????????????? ???????????? ?????????????????? ?? ?????????????????????? ???? ?????????? */
        await checkPassenger(passengerWithoutTitleName);

        /* ???????????????? ?????????????? ???????????? ???????????? */
        await page.passengers.searchInput.clearValue();
        assert.equal(await page.passengers.searchInput.getValue(), '');
        assert.equal(await page.passengers.count(), 2);

        /* ???????????????? ???????????? ?????????????????? ?? ?????????????????????? ???????????????? ???? ?????????? */
        await checkPassenger(passengerWithTitleName);

        /* ???? ???????????????????? ?????????? */
        await page.passengers.searchInput.type('Unexpected text');
        await this.browser.pause(500);
        assert.equal(await page.passengers.count(), 0);

        await page.passengers.searchInput.clearValue();
    });
});

async function getOrCreateAccountAndLogin(
    browser: WebdriverIO.Browser,
): Promise<{page: PassengersPage; uid: string}> {
    const accountManager = new Account();
    const {
        isNew,
        account: {uid, login, password},
    } = await accountManager.getOrCreate();

    await browser.login(login, password);
    await browser.url(url);

    const page = new PassengersPage(browser);

    if (!isNew) {
        await page.removeAllPassengers();
    }

    return {uid, page};
}
