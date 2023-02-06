const randomWords = require('random-words');
const { createNewVacancy } = require('../../../../backend/vacancy');
const { createCandidate } = require('../../../../backend/candidate');
const { createApplication, createOffer } = require('../../../../backend/applications');
const { getExternalLink, sendForApproval } = require('../../../../backend/offers');

// Создает внешнюю анкету для нового сотрудника
const getFormerEmployeeExternalProfile = async() => {
    try {
        // [manual] Ручками установить статусы https://femida.test.yandex-team.ru/admin/waffle/switch/ в такое положение:
        // ✅enable_fake_oebs https://femida.test.yandex-team.ru/admin/waffle/switch/44/change/
        // ✅enable_bp_always_valid https://femida.test.yandex-team.ru/admin/waffle/switch/366/change/ Отключить в админке проверку статусов JOB-тикетов
        // ✅ignore_job_issue_workflow https://femida.test.yandex-team.ru/admin/waffle/switch/110/change/
        // ❌enable_verification_check_on_offer_sending https://femida.test.yandex-team.ru/admin/waffle/switch/365/change/

        // Создаем вакансию
        const vacancy = await createNewVacancy();

        // Берем в работу
        // const vacancy2 = await takeVacancyInProgress(vacancy.id);

        // Создаем кандидата
        const firstName = randomWords();
        const lastName = randomWords();
        const skype = randomWords();
        const candidate = await createCandidate(firstName, lastName, skype);

        // Закрываем его дубликаты
        // const result = await handleDuplicatesAsNotDuplicates(candidate.id);

        // Добавляем кандидата на вакансию
        const applications = await createApplication(candidate.id, vacancy.id);

        // Сделать оффер
        const offer = await createOffer(applications.items[0].id);

        // Отредактировать оффер
        // const offer1 = await makeFormerEmployeeRussianOffer(offer.id, firstName + ' ' + lastName);

        // Отправить на согласование
        const offer2 = await sendForApproval(offer.id);

        // Получаем ссылку на оффер
        const link = await getExternalLink(offer2.id);

        // Делаем ссылку доступной для работы
        // const offer3 = await sendToCandidate(offer1.id);

        return link.replace('https://femida-ext.test.yandex-team.ru', '');
    } catch (e) {
        // console.error('ERROR', e);
    }
};

module.exports = {
    getFormerEmployeeExternalProfile,
};
