import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
// mocks
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import {
    skuMock as largeCargoTypeSkuMock,
    offerMock as largeCargoTypeOfferMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';


export function getLiftingToFloorCarts({
    liftPrice, // Цена подъема на этаж, приходящая с бека
    manualLiftPerFloorCost,
    elevatorLiftCost,
    type,
} = {}) {
    let deliveryOption = deliveryDeliveryMock;

    if (liftPrice) {
        deliveryOption = {
            ...deliveryOption,
            liftPrice,
        };
    }

    if (manualLiftPerFloorCost && elevatorLiftCost) {
        deliveryOption = {
            ...deliveryOption,
            presentationFields: {
                liftingOptions: {
                    ...(deliveryOption.presentationFields || {}).liftingOptions,
                    manualLiftPerFloorCost,
                    elevatorLiftCost,
                },
            },
        };
    }

    if (type) {
        deliveryOption = {
            ...deliveryOption,
            presentationFields: {
                liftingOptions: {
                    ...(deliveryOption.presentationFields || {}).liftingOptions,
                    type,
                },
            },
        };
    }

    return [
        buildCheckouterBucket({
            items: [{
                skuMock: largeCargoTypeSkuMock,
                offerMock: largeCargoTypeOfferMock,
                cargoTypes: largeCargoTypeOfferMock.cargoTypes,
                count: 1,
            }],
            deliveryOptions: [deliveryOption],
        }),
    ];
}

export function getCheckboxText(liftingType, floor) {
    if (liftingType === 'NOT_NEEDED' && floor) {
        return `Добавить подъём на ${floor} этаж`;
    }

    return CHECKBOX_TEXTS[liftingType];
}

export const CHECKBOX_TEXTS = {
    NOT_NEEDED: 'Добавить подъём на этаж',
    ELEVATOR: 'Подъём на пассажирском лифте',
    CARGO_ELEVATOR: 'Подъём на грузовом лифте',
    MANUAL: 'Подъём без лифта',
};

export function getPrice({
    liftingType,
    manualLiftPerFloorCost,
    elevatorLiftCost,
    floor,
}) {
    switch (liftingType) {
        case 'ELEVATOR':
        case 'CARGO_ELEVATOR':
            return elevatorLiftCost;
        case 'MANUAL':
            return floor * manualLiftPerFloorCost;
        case 'NOT_NEEDED':
        default:
            return 0;
    }
}
