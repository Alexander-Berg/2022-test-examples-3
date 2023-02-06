const adminPage = require('../../../../hermione/pages/admin');

async function navigateRestore(bro) {
    await bro.url('/restore/new');
}

async function fillDomain(bro) {
    await bro.setValue(adminPage.restoreDomainInput, 'restore.adm-testliza.ru');
}

module.exports = {
    navigateRestore,
    fillDomain
};
