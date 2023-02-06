const { ReactEntity } = require('../../../../../../../../../vendors/hermione');

const block = 'UniSearchMedicineClinic';
const Clinic = new ReactEntity({ block });

Clinic.Name = new ReactEntity({ block, elem: 'Name' });
Clinic.Name.Link = new ReactEntity({ block, elem: 'Link' });

module.exports = {
    Clinic,
};
