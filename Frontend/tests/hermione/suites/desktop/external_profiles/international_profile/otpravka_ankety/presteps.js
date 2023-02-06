const path = require('path');
const fs = require('fs-extra');

const { getInternationalProfile } = require('../newEmployee');

// Создает новую внешнюю анкету и записывает ссылку в data.json

// Сначала надо ручками установить статусы в такое положение:
// ✅enable_fake_oebs https://femida.test.yandex-team.ru/admin/waffle/switch/44/change/
// ✅enable_bp_always_valid https://femida.test.yandex-team.ru/admin/waffle/switch/366/change/ Отключить в админке проверку статусов JOB-тикетов
// ✅ignore_job_issue_workflow https://femida.test.yandex-team.ru/admin/waffle/switch/110/change/
// ❌enable_verification_check_on_offer_sending https://femida.test.yandex-team.ru/admin/waffle/switch/365/change/

// Потом сделать так:
// NODE_TLS_REJECT_UNAUTHORIZED='0' TOKEN=<your token> node
// ./tests/hermione/suites/desktop/external_profiles/international_profile/otpravka_ankety/presteps.js
const prestep = async() => {
    try {
        const link = await getInternationalProfile();

        fs.writeFileSync(path.resolve(__dirname, './data.json'), JSON.stringify({
            offer_link: link,
        }, null, 4));
    } catch (e) {
        console.error('ERROR', e);
    }
};
prestep();
