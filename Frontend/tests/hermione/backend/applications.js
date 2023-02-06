/* eslint-disable camelcase */
const { post } = require('./_request');

// Добавление кандидата на вакансию
const createApplication = async(candidate_id, vacancy_id) => {
    return await post(
        'https://femida.test.yandex-team.ru/api/applications/bulk_create/',
        `{\"candidate\":\"${candidate_id}\",\"vacancies\":[\"${vacancy_id}\"],\"create_activated\":\"true\",\"interviews\":\"\",\"comment\":\"\"}`
    );
};

const createOffer = async id => {
    return await post(
        `https://femida.test.yandex-team.ru/api/applications/${id}/create_offer/`,
        '{}'
    );
};

module.exports = {
    createApplication,
    createOffer,
};
