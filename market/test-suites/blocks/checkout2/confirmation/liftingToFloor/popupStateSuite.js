import {
    makeSuite,
    makeCase,
} from 'ginny';

// pageObjects
import {TextField} from '@self/root/src/uikit/components/TextField/__pageObject';
import LiftingToFloorPopup from
    '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/LiftingToFloorPopup/__pageObject';

/**
 * Проверяет состояние попапа подъема на этаж
 * PageObjects:
 * this.liftingToFloorPopup - попап с выбором подъема на этаж
 */
const popupStateSuite = makeSuite('Состояние попапа.', {
    params: {
        manualLiftPerFloorCost: 'Стоимость подъема вручную',
        elevatorLiftCost: 'Стоимость подъема на лифте',
        liftingType: 'Выбранный тип подъема на этаж',
        comment: 'Комментарий к подъему на этаж',
        isExist: 'Показывается ли чекбокс',
    },
    defaultParams: {
        liftingType: 'NOT_NEEDED',
        isExist: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                commentInput: () => this.createPageObject(TextField, {
                    parent: this.liftingToFloorPopup,
                    root: LiftingToFloorPopup.commentInput,
                }),
            });
        },
        'Не показываем.': makeCase({
            async test() {
                const {
                    isExist,
                } = this.params;

                if (isExist) {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('игнорируем тест если попап должен быть показан');
                }

                this.liftingToFloorPopup.isExisting()
                    .should.eventually.to.be.equal(
                        false,
                        'Попап подъема на этаж не должен быть виден'
                    );
            },
        }),
        'Показываем с ожидаемыми данными.': makeCase({
            async test() {
                const {
                    isExist,
                    liftingType,
                    comment = '',
                } = this.params;

                if (!isExist) {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('игнорируем тест если попап не должен быть показан');
                }

                await this.liftingToFloorPopup.waitForVisible();

                await this.liftingToFloorPopup.isLiftingTypeChecked(liftingType);

                await this.liftingToFloorPopup.isFloorExist()
                    .should.eventually.to.be.equal(
                        true,
                        'Поле ввода этажа должно быть всегда видно'
                    );

                await this.commentInput.getText()
                    .should.eventually.to.be.equal(
                        comment,
                        `Поле ввода комментария должно содержать: "${comment}"`
                    );
            },
        }),
    },
});

export default popupStateSuite;
