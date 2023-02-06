const { ReactEntity } = require('../../../../../../../../../vendors/hermione');

const Main = new ReactEntity({ block: 'UniSearchMedicineMain' });

Main.Features = new ReactEntity({ block: 'UniSearchMedicineMain', elem: 'Features' });
Main.Description = new ReactEntity({ block: 'UniSearchMedicineMain', elem: 'Description' });
Main.Source = new ReactEntity({ block: 'UniSearchMedicineMain', elem: 'Source' });
Main.Source.Link = new ReactEntity({ block: 'Link' });
Main.DescriptionMore = new ReactEntity({ block: 'ExtendedText', elem: 'Toggle' });

module.exports = { Main };
