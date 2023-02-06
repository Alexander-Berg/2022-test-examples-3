'use strict';
const { checkNodeExists } = require('../../../../../UniSearch.test/helpers');
const PO = require('./UniSearchMedicineClinics.page-object/index@touch-phone');
const PREVIEW_OPEN_TIMEOUT = 3000;

specs({
    feature: 'Универсальный колдунщик поиска врачей',
}, function() {
    describe('Блоки', function() {
        describe('Список клиник', function() {
            const serpParams = {
                text: 'foreverdata',
                foreverdata: 2132929412,
                data_filter: 'unisearch/medicine',
            };

            it('Ссылки на перезапрос с 1Орг', async function() {
                await this.browser.yaOpenSerp(serpParams, PO.UniSearchClinics());
                await this.browser.yaIndexify(PO.UniSearchClinics.Item());
                await this.browser.yaCheckBaobabCounter(
                    PO.UniSearchClinics.ClinicsItemFirst.Name(),
                    {
                        path: '/$page/$main/$result/unisearch-clinics/clinic/title-link',
                        attrs: {
                            url: '?text=%D0%9A%D0%BB%D0%B8%D0%BD%D0%B8%D0%BA%D0%B0+%D1%84%D0%B0%D0%BA%D1%83%D0%BB%D1%8C%D1%82%D0%B5%D1%82%D1%81%D0%BA%D0%BE%D0%B9+%D1%85%D0%B8%D1%80%D1%83%D1%80%D0%B3%D0%B8%D0%B8+%D0%B8%D0%BC%D0%B5%D0%BD%D0%B8+%D0%9D.%D0%9D.+%D0%91%D1%83%D1%80%D0%B4%D0%B5%D0%BD%D0%BA%D0%BE+%D0%A3%D0%BD%D0%B8%D0%B2%D0%B5%D1%80%D1%81%D0%B8%D1%82%D0%B5%D1%82%D1%81%D0%BA%D0%BE%D0%B9+%D0%BA%D0%BB%D0%B8%D0%BD%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%BE%D0%B9+%D0%B1%D0%BE%D0%BB%D1%8C%D0%BD%D0%B8%D1%86%D1%8B+%E2%84%961&oid=b:161203215287&serp-reload-from=unisearch/medicine',
                        },
                    },
                );
            });
        });

        describe('Клиника с расписанием', function() {
            it('Внешний вид', async function() {
                await this.browser.yaOpenSerp({
                    text: 'foreverdata',
                    foreverdata: 676973423,
                    exp_flags: ['unisearch_medicine_appointments_slots=true'],
                    data_filter: 'unisearch/medicine',
                }, PO.UniSearchMedicine());

                await this.browser.yaIndexify(PO.UniSearchMedicine.Content.List.Item());
                await this.browser.click(PO.UniSearchMedicine.Content.List.ItemFirst());
                await this.browser.yaWaitForVisible(
                    PO.UniSearchMedicinePreview.Clinics(),
                    PREVIEW_OPEN_TIMEOUT,
                    'Не удалось открыть полную карточку врача',
                );

                await this.browser.yaIndexify(PO.UniSearchMedicinePreview.Clinics.Item());
                await this.browser.yaScroll(PO.UniSearchMedicinePreview.Clinics.FirstItem());

                await this.browser.assertView(
                    'plain',
                    PO.UniSearchMedicinePreview.Clinics.FirstItem(),
                );
            });

            it('Клик по дате', async function() {
                await this.browser.yaOpenSerp({
                    text: 'foreverdata',
                    foreverdata: 676973423,
                    exp_flags: ['unisearch_medicine_appointments_slots=true'],
                    data_filter: 'unisearch/medicine',
                }, PO.UniSearchMedicine());

                await this.browser.yaIndexify(PO.UniSearchMedicine.Content.List.Item());
                await this.browser.click(PO.UniSearchMedicine.Content.List.ItemFirst());
                await this.browser.yaWaitForVisible(
                    PO.UniSearchMedicinePreview.Clinics(),
                    PREVIEW_OPEN_TIMEOUT,
                    'Не удалось открыть полную карточку врача',
                );

                await this.browser.yaIndexify(PO.UniSearchMedicinePreview.Clinics.Item());
                await this.browser.yaIndexify(PO.UniSearchMedicinePreview.Clinics.FirstItem.TimeTable.Dates.Date());
                await this.browser.click(PO.UniSearchMedicinePreview.Clinics.FirstItem.TimeTable.Dates.FirstDate());
                await this.browser.yaScroll(PO.UniSearchMedicinePreview.Clinics.FirstItem());

                await this.browser.assertView(
                    'date_selected',
                    PO.UniSearchMedicinePreview.Clinics.FirstItem(),
                );
            });

            it('Разметка baobab', async function() {
                await this.browser.yaOpenSerp({
                    text: 'foreverdata',
                    foreverdata: 676973423,
                    exp_flags: ['unisearch_medicine_appointments_slots=true'],
                    data_filter: 'unisearch/medicine',
                }, PO.UniSearchMedicine());

                await this.browser.yaIndexify(PO.UniSearchMedicine.Content.List.Item());
                await this.browser.click(PO.UniSearchMedicine.Content.List.ItemFirst());
                await this.browser.yaWaitForVisible(
                    PO.UniSearchMedicinePreview.Clinics(),
                    PREVIEW_OPEN_TIMEOUT,
                    'Не удалось открыть полную карточку врача',
                );

                await this.browser.yaIndexify(PO.UniSearchMedicinePreview.Clinics.Item());
                await this.browser.yaCheckLink2({
                    selector: PO.UniSearchMedicinePreview.Clinics.FirstItem.TimeTable.Dates.PhoneButton(),
                    url: 'tel:84951850101',
                    baobab: {
                        path: '$page/$main/$result/unisearch_medicine/preview/unisearch-clinics/clinic/phone-button',
                        attrs: {
                            action: 'phone',
                            phoneLink: 'tel:84951850101',
                        },
                    },
                });
            });

            describe('Запись к врачу', function() {
                describe('Один источник', function() {
                    hermione.only.in(['chrome-phone'], 'Не браузерозависимо');
                    it('По нажатию в слот происходит переход по ссылке', async function() {
                        await this.browser.yaOpenSerp({
                            text: 'foreverdata',
                            foreverdata: 676973423,
                            exp_flags: ['unisearch_medicine_appointments_slots=true'],
                            data_filter: 'unisearch/medicine',
                        }, PO.UniSearchMedicine());

                        await this.browser.yaIndexify(PO.UniSearchMedicine.Content.List.Item());
                        await this.browser.click(PO.UniSearchMedicine.Content.List.ItemFirst());
                        await this.browser.yaWaitForVisible(
                            PO.UniSearchMedicinePreview.Clinics(),
                            PREVIEW_OPEN_TIMEOUT,
                            'Не удалось открыть полную карточку врача',
                        );

                        await this.browser.yaIndexify(PO.UniSearchMedicinePreview.Clinics.Item());
                        await this.browser.yaIndexify(
                            PO.UniSearchMedicinePreview.Clinics.FirstItem.TimeTable.Slots.Slot(),
                        );
                    });
                });

                describe('Несколько источников', function() {
                    it('По нажатию в слот происходит открытие шторки', async function() {
                        await this.browser.yaOpenSerp({
                            text: 'foreverdata',
                            foreverdata: 3265722964,
                            exp_flags: ['unisearch_medicine_appointments_slots=true'],
                            data_filter: 'unisearch/medicine',
                        }, PO.UniSearchMedicine());

                        await this.browser.yaIndexify(PO.UniSearchMedicine.Content.List.Item());
                        await this.browser.click(PO.UniSearchMedicine.Content.List.ItemFirst());
                        await this.browser.yaWaitForVisible(
                            PO.UniSearchMedicinePreview.Clinics(),
                            PREVIEW_OPEN_TIMEOUT,
                            'Не удалось открыть полную карточку врача',
                        );

                        await this.browser.yaIndexify(PO.UniSearchMedicinePreview.Clinics.Item());
                        await this.browser.yaIndexify(
                            PO.UniSearchMedicinePreview.Clinics.FirstItem.TimeTable.Slots.Slot(),
                        );
                        await this.browser.click(
                            PO.UniSearchMedicinePreview.Clinics.FirstItem.TimeTable.Slots.FirstSlot(),
                        );

                        await this.browser.yaWaitForVisible(
                            PO.UniSearchAppointmentPopup(),
                            PREVIEW_OPEN_TIMEOUT,
                            'Не удалось открыть шторку с выбором источника записи',
                        );

                        await this.browser.assertView('plain', PO.UniSearchAppointmentPopup.Content());
                    });

                    hermione.only.in(['chrome-phone'], 'Не браузерозависимо');
                    it('Baobab-разметка', async function() {
                        const assertNode = checkNodeExists.bind(this);

                        await this.browser.yaOpenSerp({
                            text: 'foreverdata',
                            foreverdata: 3265722964,
                            exp_flags: ['unisearch_medicine_appointments_slots=true'],
                            data_filter: 'unisearch/medicine',
                        }, PO.UniSearchMedicine());

                        await this.browser.yaIndexify(PO.UniSearchMedicine.Content.List.Item());
                        await this.browser.click(PO.UniSearchMedicine.Content.List.ItemFirst());
                        await this.browser.yaWaitForVisible(
                            PO.UniSearchMedicinePreview.Clinics(),
                            PREVIEW_OPEN_TIMEOUT,
                            'Не удалось открыть полную карточку врача',
                        );

                        await assertNode({
                            path: '/$page/$main/$result/unisearch_medicine/preview/unisearch-clinics/clinic/slot-button',
                            attrs: {
                                id: 'slot1',
                                title: '15:00',
                            },
                        });

                        await this.browser.yaIndexify(PO.UniSearchMedicinePreview.Clinics.Item());
                        await this.browser.yaIndexify(
                            PO.UniSearchMedicinePreview.Clinics.FirstItem.TimeTable.Slots.Slot());
                        await this.browser.click(
                            PO.UniSearchMedicinePreview.Clinics.FirstItem.TimeTable.Slots.FirstSlot());

                        await this.browser.yaWaitForVisible(
                            PO.UniSearchAppointmentPopup(),
                            PREVIEW_OPEN_TIMEOUT,
                            'Не удалось открыть шторку с выбором источника записи',
                        );

                        await assertNode({
                            path: '/$page/$main/$result/unisearch_medicine/preview/unisearch-clinics/appointment-popup/close-button',
                        });
                    });
                });
            });
        });

        describe('Клиника без расписания', function() {
            it('Внешний вид', async function() {
                await this.browser.yaOpenSerp({
                    text: 'foreverdata',
                    foreverdata: 3253888529,
                    exp_flags: ['unisearch_medicine_appointments_slots=true'],
                    data_filter: 'unisearch/medicine',
                }, PO.UniSearchMedicine());

                await this.browser.yaIndexify(PO.UniSearchMedicine.Content.List.Item());
                await this.browser.click(PO.UniSearchMedicine.Content.List.ItemFirst());
                await this.browser.yaWaitForVisible(
                    PO.UniSearchMedicinePreview.Clinics(),
                    PREVIEW_OPEN_TIMEOUT,
                    'Не удалось открыть полную карточку врача',
                );

                await this.browser.yaIndexify(PO.UniSearchMedicinePreview.Clinics.Item());
                await this.browser.yaScroll(PO.UniSearchMedicinePreview.Clinics.FirstItem());

                await this.browser.assertView(
                    'no_timetable',
                    PO.UniSearchMedicinePreview.Clinics.FirstItem(),
                );
            });

            describe('Запись к врачу', function() {
                describe('Один источник', function() {
                    hermione.only.in(['chrome-phone'], 'Не браузерозависимо');
                    it('По нажатию в кнопку записи происходит переход по ссылке', async function() {
                        await this.browser.yaOpenSerp({
                            text: 'foreverdata',
                            foreverdata: 3253888529,
                            exp_flags: ['unisearch_medicine_appointments_slots=true'],
                            data_filter: 'unisearch/medicine',
                        }, PO.UniSearchMedicine());

                        await this.browser.yaIndexify(PO.UniSearchMedicine.Content.List.Item());
                        await this.browser.click(PO.UniSearchMedicine.Content.List.ItemFirst());
                        await this.browser.yaWaitForVisible(
                            PO.UniSearchMedicinePreview.Clinics(),
                            PREVIEW_OPEN_TIMEOUT,
                            'Не удалось открыть полную карточку врача',
                        );

                        await this.browser.yaIndexify(PO.UniSearchMedicinePreview.Clinics.Item());

                        await this.browser.yaCheckLink2({
                            selector: PO.UniSearchMedicinePreview.Clinics.FirstItem.AppointmentsButton(),
                            url: 'https://zoon.ru/',
                            baobab: {
                                path: '$page/$main/$result/unisearch_medicine/preview/unisearch-clinics/clinic/make-appointment-button',
                                attrs: {
                                    url: 'https://zoon.ru/',
                                },
                            },
                        });
                    });
                });

                describe('Несколько источников', function() {
                    it('По нажатию в кнопку записи происходит открытие шторки', async function() {
                        await this.browser.yaOpenSerp({
                            text: 'foreverdata',
                            foreverdata: 2140326007,
                            exp_flags: ['unisearch_medicine_appointments_slots=true'],
                            data_filter: 'unisearch/medicine',
                        }, PO.UniSearchMedicine());

                        await this.browser.yaIndexify(PO.UniSearchMedicine.Content.List.Item());
                        await this.browser.click(PO.UniSearchMedicine.Content.List.ItemFirst());
                        await this.browser.yaWaitForVisible(
                            PO.UniSearchMedicinePreview.Clinics(),
                            PREVIEW_OPEN_TIMEOUT,
                            'Не удалось открыть полную карточку врача',
                        );

                        await this.browser.yaIndexify(PO.UniSearchMedicinePreview.Clinics.Item());
                        await this.browser.click(PO.UniSearchMedicinePreview.Clinics.FirstItem.AppointmentsButton());

                        await this.browser.yaWaitForVisible(
                            PO.UniSearchAppointmentPopup(),
                            PREVIEW_OPEN_TIMEOUT,
                            'Не удалось открыть шторку с выбором источника записи',
                        );

                        await this.browser.assertView('plain', PO.UniSearchAppointmentPopup.Content());
                    });

                    hermione.only.in(['chrome-phone'], 'Не браузерозависимо');
                    it('Baobab-разметка', async function() {
                        const assertNode = checkNodeExists.bind(this);

                        await this.browser.yaOpenSerp({
                            text: 'foreverdata',
                            foreverdata: 2140326007,
                            exp_flags: ['unisearch_medicine_appointments_slots=true'],
                            data_filter: 'unisearch/medicine',
                        }, PO.UniSearchMedicine());

                        await this.browser.yaIndexify(PO.UniSearchMedicine.Content.List.Item());
                        await this.browser.click(PO.UniSearchMedicine.Content.List.ItemFirst());
                        await this.browser.yaWaitForVisible(
                            PO.UniSearchMedicinePreview.Clinics(),
                            PREVIEW_OPEN_TIMEOUT,
                            'Не удалось открыть полную карточку врача',
                        );

                        await assertNode({
                            path: '/$page/$main/$result/unisearch_medicine/preview/unisearch-clinics/clinic/make-appointment-button',
                        });

                        await this.browser.yaIndexify(PO.UniSearchMedicinePreview.Clinics.Item());
                        await this.browser.click(PO.UniSearchMedicinePreview.Clinics.FirstItem.AppointmentsButton());

                        await this.browser.yaWaitForVisible(
                            PO.UniSearchAppointmentPopup(),
                            PREVIEW_OPEN_TIMEOUT,
                            'Не удалось открыть шторку с выбором источника записи',
                        );

                        await this.browser.yaIndexify(PO.UniSearchAppointmentPopup.Content.SourceButton());

                        await this.browser.yaCheckLink2({
                            selector: PO.UniSearchAppointmentPopup.Content.FirstSourceButton(),
                            url: 'https://zoon.ru/',
                            baobab: {
                                path: '$page/$main/$result/unisearch_medicine/preview/unisearch-clinics/appointment-popup/source-button',
                                attrs: {
                                    url: 'https://zoon.ru/',
                                },
                            },
                        });
                        await this.browser.yaCheckLink2({
                            selector: PO.UniSearchAppointmentPopup.Content.SecondSourceButton(),
                            url: 'https://docdoc.ru/',
                            baobab: {
                                path: '$page/$main/$result/unisearch_medicine/preview/unisearch-clinics/appointment-popup/source-button',
                                attrs: {
                                    url: 'https://docdoc.ru/',
                                },
                            },
                        });
                        await assertNode({
                            path: '/$page/$main/$result/unisearch_medicine/preview/unisearch-clinics/appointment-popup/close-button',
                        });
                    });
                });
            });
        });
    });
});
