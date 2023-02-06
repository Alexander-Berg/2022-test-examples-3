import assert from 'assert';
import {
    makeSuite,
    makeCase,
} from 'ginny';

import {RETURN_CANDIDATE_REASON_OPTION} from '@self/root/src/entities/returnCandidateItem';
import {
    HEADER_TEXT,
    DESCRIPTION_TEXT,
} from '@self/root/src/widgets/parts/ReturnCandidate/components/PhotoUpload/constants/common';

import PhotoUploader from '@self/root/src/components/FileUploaderBase/PhotoUploader/__pageObject';


const TOUCH_INDEX = 1;

export default makeSuite('Загрузка фото.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-18149',
    story: {
        async beforeEach() {
            assert(this.reasonsChooseScreen || this.reasonTypeSelector, 'PageObject with Reason must be defined');

            const parentPageObject = this.reasonsChooseScreen || this.reasonTypeSelector;

            if (this.reasonsChooseScreen) {
                this._setDefectReason = () =>
                    this.reasonsChooseScreen.setReasonForItem(TOUCH_INDEX, 'bad_quality');

                this._setUnfitReason = () =>
                    this.reasonsChooseScreen.setReasonForItem(TOUCH_INDEX, 'do_not_fit');

                this._setWrongItemReason = () =>
                    this.reasonsChooseScreen.setReasonForItem(TOUCH_INDEX, 'wrong_item');

                this._getHeaderText = () =>
                    this.reasonsChooseScreen.getPhotoUploadHeaderText(TOUCH_INDEX);

                this._getDescriptionText = () =>
                    this.reasonsChooseScreen.getPhotoUploadDescriptionText(TOUCH_INDEX);
            } else {
                this._setDefectReason = () =>
                    this.reasonTypeSelector.setReasonBadQuality();

                this._setUnfitReason = () =>
                    this.reasonTypeSelector.setReasonDoNotFit();

                this._setWrongItemReason = () =>
                    this.reasonTypeSelector.setReasonWrongItem();

                this._getHeaderText = () =>
                    this.reasonTypeSelector.getPhotoUploadHeaderText();

                this._getDescriptionText = () =>
                    this.reasonTypeSelector.getPhotoUploadDescriptionText();
            }

            await this.setPageObjects({
                photoUploader: () => this.createPageObject(PhotoUploader, {parent: parentPageObject}),
            });

            await parentPageObject.waitForVisible();
            await this._setDefectReason();
            return parentPageObject.waitForReasonInputVisible(1);
        },

        'При изменении причины меняется текст заголовка и описания блока загрузки фото': makeCase({
            async test() {
                await this._setDefectReason();
                await checkText.call(this, RETURN_CANDIDATE_REASON_OPTION.BAD_QUALITY);

                await this._setUnfitReason();
                await checkText.call(this, RETURN_CANDIDATE_REASON_OPTION.DO_NO_FIT);

                await this._setWrongItemReason();
                await checkText.call(this, RETURN_CANDIDATE_REASON_OPTION.WRONG_ITEM);
            },
        }),

        'После загрузки фото появляется превью, которое можно удалить': makeCase({
            async test() {
                await this.photoUploader.uploadPhoto(`${__dirname}/files/validImage.jpeg`);

                await this.photoUploader.getTilesCount()
                    .should.eventually.to.equal(
                        1,
                        'Появилось одно превью'
                    );

                await this.photoUploader.uploadPhoto(`${__dirname}/files/validImage.jpeg`);

                await this.photoUploader.getTilesCount()
                    .should.eventually.to.equal(
                        2,
                        'Появилось второе превью'
                    );

                await this.photoUploader.closeNthTile(2);

                await this.photoUploader.getTilesCount()
                    .should.eventually.to.equal(
                        1,
                        'Второе превью закрылось'
                    );

                await this.photoUploader.closeNthTile(1);

                await this.photoUploader.getTilesCount()
                    .should.eventually.to.equal(
                        0,
                        'Первое превью закрылось'
                    );
            },
        }),

        'Нельзя загрузить фото тяжелее 10 МБ': makeCase({
            async test() {
                await this.photoUploader.uploadPhoto(`${__dirname}/files/bigImage.jpg`);

                return this.photoUploader.getTilesCount()
                    .should.eventually.to.equal(
                        0,
                        'Большое фото не загрузилось'
                    );
            },
        }),

        'Нельзя загрузить что-то кроме изображений': makeCase({
            async test() {
                await this.photoUploader.uploadPhoto(`${__dirname}/files/notAnImage.txt`);

                return this.photoUploader.getTilesCount()
                    .should.eventually.to.equal(
                        0,
                        'Не-изображение не загрузилось'
                    );
            },
        }),
    },
});

async function checkText(reasonOption) {
    const headerText = HEADER_TEXT[reasonOption];
    const descriptionText = DESCRIPTION_TEXT[reasonOption];

    const headerTextValue = await this._getHeaderText();
    await this.expect(headerTextValue).to.equal(
        headerText,
        `Текст заголовка "${headerText}"`
    );

    const descriptionTextValue = await this._getDescriptionText();
    return this.expect(descriptionTextValue).to.equal(
        descriptionText,
        `Текст описания "${descriptionText}"`
    );
}
