const { ReactEntity } = require('../../../../../../../../vendors/hermione');
const { Main } = require('../../Main/Main.test/Main.page-object');
const { UniSearchMedicineClinics } = require('../../../../UniSearchMedicine.features/Clinics/UniSearchMedicineClinics.test/UniSearchMedicineClinics.page-object/index@common');
const { UniSearchReviews } = require('../../../../../../UniSearch.components/Reviews/Reviews.page-objects');

const UniSearchMedicinePreview = new ReactEntity({ block: 'UniSearchMedicine', elem: 'Preview' });
UniSearchMedicinePreview.Main = Main.copy();
UniSearchMedicinePreview.Reviews = UniSearchReviews.copy();
UniSearchMedicinePreview.Clinics = UniSearchMedicineClinics.copy();

module.exports = { UniSearchMedicinePreview };
