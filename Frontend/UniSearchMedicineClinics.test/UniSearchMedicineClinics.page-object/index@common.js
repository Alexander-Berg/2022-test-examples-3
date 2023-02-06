const { ReactEntity } = require('../../../../../../../../vendors/hermione');
const { Clinic } = require('../../../../UniSearchMedicine.components/PreviewContent/Clinic/Clinic.test/Clinic.page-object/index@common');

const UniSearchMedicineClinics = new ReactEntity({ block: 'UniSearchMedicineClinics' });

UniSearchMedicineClinics.Clinic = Clinic.copy();
UniSearchMedicineClinics.ClinicFirst = Clinic.copy().withIndex(0);

module.exports = {
    UniSearchMedicineClinics,
};
