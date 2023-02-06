const PO = require('../../../../UniSearchMedicine.test/UniSearchMedicine.page-object/index@common');
const {
    Clinics,
    ClinicsItem,
    ClinicsAppointmentsButton,
} = require('../../../../UniSearchMedicine.components/PreviewContent/Clinics/Clinics.page-object/index@touch-phone');
const {
    TimeTable,
    TimeTableDates,
    TimeTableSlots,
    TimeTableButton,
} = require('../../../../UniSearchMedicine.components/TimeTable/TimeTable.page-object/index@touch-phone');
const {
    UniSearchAppointmentPopup,
} = require('../../../../UniSearchMedicine.components/PreviewContent/AppointmentPopup/AppointmentPopup.test/AppointmentPopup.page-object/index@touch-phone');

PO.UniSearchClinics = Clinics.copy();
PO.UniSearchClinics.Item = ClinicsItem.copy();
PO.UniSearchClinics.ClinicsItemFirst = ClinicsItem.copy().withIndex(0);
PO.UniSearchMedicinePreview.Clinics = Clinics.copy();
PO.UniSearchMedicinePreview.Clinics.Item = ClinicsItem.copy();
PO.UniSearchMedicinePreview.ClinicsItemFirst = ClinicsItem.copy().withIndex(0);
PO.UniSearchMedicinePreview.Clinics.Item.AppointmentsButton = ClinicsAppointmentsButton.copy();

PO.UniSearchMedicinePreview.Clinics.Item.TimeTable = TimeTable.copy();
PO.UniSearchMedicinePreview.Clinics.Item.TimeTable.Dates = TimeTableDates.copy();
PO.UniSearchMedicinePreview.Clinics.Item.TimeTable.Dates.Date = TimeTableButton.copy();
PO.UniSearchMedicinePreview.Clinics.Item.TimeTable.Dates.FirstDate =
    PO.UniSearchMedicinePreview.Clinics.Item.TimeTable.Dates.Date.copy().withIndex(0);

PO.UniSearchMedicinePreview.Clinics.Item.TimeTable.Slots = TimeTableSlots.copy();
PO.UniSearchMedicinePreview.Clinics.Item.TimeTable.Slots.Slot = TimeTableButton.copy();
PO.UniSearchMedicinePreview.Clinics.Item.TimeTable.Slots.FirstSlot =
    PO.UniSearchMedicinePreview.Clinics.Item.TimeTable.Slots.Slot.copy().withIndex(0);

PO.UniSearchMedicinePreview.Clinics.FirstItem = PO.UniSearchMedicinePreview.Clinics.Item.copy().withIndex(0);

PO.UniSearchAppointmentPopup = UniSearchAppointmentPopup.copy();
PO.UniSearchAppointmentPopup.Content.FirstSourceButton =
    PO.UniSearchAppointmentPopup.Content.SourceButton.copy().withIndex(0);
PO.UniSearchAppointmentPopup.Content.SecondSourceButton =
    PO.UniSearchAppointmentPopup.Content.SourceButton.copy().withIndex(1);

module.exports = PO;
