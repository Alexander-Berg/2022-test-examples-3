/* eslint-disable camelcase */
const { post } = require('./_request');

// Создать новую вакансию
const createNewVacancy = async() => {
    return await post(
        'https://femida.test.yandex-team.ru/api/vacancies/',
        '{"type":"new","name":"new employee","department":"3577","abc_services":["825"],"reason":"Тестирование","hiring_manager":"olgakozlova","professional_sphere":"1","profession":"1","pro_level_min":"1","pro_level_max":"4","cities":["1"],"wage_system":"fixed","max_salary":"","currency":"1","max_salary_comment":"","comment":""}'
    );
};

// Взять вакансию в работу
const tryToTakeVacancyInProgress = async(vacancy_id, bp) => {
    return post(
        `https://femida.test.yandex-team.ru/api/vacancies/${vacancy_id}/approve/`,
        `{"budget_position_id":"${bp}","main_recruiter":"olgakozlova"}`
    );
};

// Взять вакансию в работу
const takeVacancyInProgress = async vacancy_id => {
    let err;
    for (let i = 0; i < 10; i++) {
        try {
            let bp = Math.floor(Math.random() * (9999 - 2) + 2);
            const result = await tryToTakeVacancyInProgress(vacancy_id, bp);
            return result;
        } catch (e) {
            err = e;
        }
    }
    throw err;
};

// Закрыть вакансию
const closeVacancy = async vacancy_id => {
    return await post(
        `https://femida.test.yandex-team.ru/api/vacancies/${vacancy_id}/close/`,
        '{\"comment\":\"1\",\"application_resolution\":\"vacancy_closed\"}'
    );
};

module.exports = {
    createNewVacancy,
    takeVacancyInProgress,
    closeVacancy,
};
