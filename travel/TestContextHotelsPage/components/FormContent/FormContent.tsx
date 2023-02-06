import React from 'react';

import EHotelPartnerId from 'server/api/GenericOrderApi/types/common/service/IHotelServiceInfo/IHotelOfferInfo/EHotelPartnerId';
import {
    ECancellation,
    EConfirmationOutcome,
    EHotelDataLookupOutcome,
    EOutcome,
    EPansionType,
    ERefundOutcome,
    EReservationOutcome,
} from 'server/api/HotelsBookAPI/types/ITestBookOfferToken';

import {renderSelect} from 'projects/testControlPanel/utilities/renderSelect';
import {renderDatePicker} from 'projects/testControlPanel/utilities/renderDatePicker';
import convertEnumToSelectOptions from 'projects/testControlPanel/utilities/convertEnumToSelectOptions';
import {renderInput} from 'projects/testControlPanel/utilities/renderInput';
import {renderCheckbox} from 'projects/testControlPanel/utilities/renderCheckbox';

import Field from 'components/Form/components/Field/Field';
import Flex from 'components/Flex/Flex';

import getOptionDescription from 'projects/testControlPanel/pages/TestContextHotelsPage/getOptionDescription';

const FormContent: React.FC = () => {
    return (
        <Flex below={8} flexDirection="column" between={5}>
            <Field name="originalId" label="ID отеля" inputType="number">
                {renderInput}
            </Field>
            <Field
                name="partnerId"
                options={convertEnumToSelectOptions(EHotelPartnerId)}
                getOptionDescription={getOptionDescription}
                label="Партнер"
            >
                {renderSelect}
            </Field>
            <Field name="checkinDate" label="Дата заезда">
                {renderDatePicker}
            </Field>
            <Field name="checkoutDate" label="Дата выезда">
                {renderDatePicker}
            </Field>
            <Field
                name="occupancy"
                label="Количество гостей"
                message="В формате N-C1,C2,…’, где N количество взрослых, C1,C2,… - возраста детей"
            >
                {renderInput}
            </Field>
            <Field
                name="cancellation"
                options={convertEnumToSelectOptions(ECancellation)}
                getOptionDescription={getOptionDescription}
                label="Политика отмены"
            >
                {renderSelect}
            </Field>
            <Field
                name="pansionType"
                options={convertEnumToSelectOptions(EPansionType)}
                getOptionDescription={getOptionDescription}
                label="Тип питания"
            >
                {renderSelect}
            </Field>
            <Field name="offerName" label="Имя сгенерированного оффера">
                {renderInput}
            </Field>
            <Field
                name="priceAmount"
                label="Цена сгенерированного оффера"
                inputType="number"
            >
                {renderInput}
            </Field>
            <Field
                name="discountAmount"
                label="Скидка на оффер"
                inputType="number"
            >
                {renderInput}
            </Field>
            <Field
                name="mealPrice"
                label="Стоимость питания (включенная в общую цену оффера)"
                inputType="number"
            >
                {renderInput}
            </Field>
            <Field
                name="partiallyRefundRate"
                label="Процент штрафа в сгенерированном оффере"
                inputType="number"
            >
                {renderInput}
            </Field>
            <Field
                name="partiallyRefundableInMinutes"
                label="Период бесплатной отмены сгенерированного оффера"
                inputType="number"
                message="В минутах"
            >
                {renderInput}
            </Field>
            <Field
                name="nonRefundableInMinutes"
                label="Период отмены сгенерированного оффера со штрафом"
                inputType="number"
                message="В минутах"
            >
                {renderInput}
            </Field>
            <Field
                name="getOfferOutcome"
                options={convertEnumToSelectOptions(EOutcome)}
                getOptionDescription={getOptionDescription}
                label="Результат проверки оффера на API при получении оффера по токену"
            >
                {renderSelect}
            </Field>
            <Field
                name="createOrderOutcome"
                options={convertEnumToSelectOptions(EOutcome)}
                getOptionDescription={getOptionDescription}
                label="Результат проверки оффера на API при получении создании заказа"
            >
                {renderSelect}
            </Field>
            <Field
                name="hotelDataLookupOutcome"
                options={convertEnumToSelectOptions(EHotelDataLookupOutcome)}
                getOptionDescription={getOptionDescription}
                label="Источник партнерских данных про отель при генерации оффера"
            >
                {renderSelect}
            </Field>
            <Field
                name="reservationOutcome"
                options={convertEnumToSelectOptions(EReservationOutcome)}
                getOptionDescription={getOptionDescription}
                label="Результат предварительного бронирования заказа"
            >
                {renderSelect}
            </Field>
            <Field
                name="confirmationOutcome"
                options={convertEnumToSelectOptions(EConfirmationOutcome)}
                getOptionDescription={getOptionDescription}
                label="Результат подтверждения бронирования заказа"
            >
                {renderSelect}
            </Field>
            <Field
                name="refundOutcome"
                options={convertEnumToSelectOptions(ERefundOutcome)}
                getOptionDescription={getOptionDescription}
                label="Результат возврата заказа"
            >
                {renderSelect}
            </Field>
            <Field
                name="existingDolphinOrder"
                label="ID существующего заказа в Дельфине"
                message="Для симуляции дублей"
            >
                {renderInput}
            </Field>
            <Field
                name="existingDolphinOrder"
                label="ID существующего заказа в Дельфине"
                message="Для симуляции дублей"
            >
                {renderInput}
            </Field>
            <Field
                name="forceAvailability"
                label="Заменить партнерскую выдачу офферов на сгенерированный оффер"
            >
                {renderCheckbox}
            </Field>
            <Field
                name="ignorePaymentScheduleRestrictions"
                label="Игнорировать минимальную длину интервала рассрочки и уменьшить интервал безопасности"
            >
                {renderCheckbox}
            </Field>
            <Field
                name="isPostPay"
                label="Сделать офферы с возможностью постоплаты"
            >
                {renderCheckbox}
            </Field>
        </Flex>
    );
};

export default FormContent;
