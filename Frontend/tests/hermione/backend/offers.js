/* eslint-disable camelcase */
const moment = require('moment');
const { put, post, get } = require('./_request');

// Создание оффера для бывшего сотрудника (русский)
const makeFormerEmployeeRussianOffer = async(offer_id, name) => {
    const now = moment();
    const join_at = now.add(7, 'days').format('DD.MM.YYYY');
    return await put(
        `https://femida.test.yandex-team.ru/api/offers/${offer_id}/`,
        `{\"full_name\":\"${name}\",\"employee_type\":\"former\",\"username\":\"dezmound\",\"form_type\":\"russian\",\"org\":\"1\",\"department\":\"3577\",\"position\":\"1\",\"staff_position_name\":\"разработчик\",\"work_place\":\"office\",\"office\":\"1\",\"join_at\":\"${join_at}\",\"grade\":\"17\",\"payment_type\":\"monthly\",\"salary\":\"80000\",\"payment_currency\":\"1\",\"employment_type\":\"full\",\"is_main_work_place\":\"true\",\"contract_type\":\"indefinite\",\"probation_period_type\":\"3m\",\"vmi\":\"true\",\"sick_leave_supplement\":\"false\",\"housing_program\":\"false\",\"cellular_compensation\":\"false\",\"internet_compensation_amount\":\"0.00\",\"rsu_cost\":\"0.00\",\"signup_bonus\":\"0.00\",\"allowance\":\"0.00\",\"need_relocation\":\"false\"}`
    );
};

const makeNewInternationalOffer = async(offer_id, name) => {
    const now = moment();
    const join_at = now.add(7, 'days').format('DD.MM.YYYY');
    return await put(
        `https://femida.test.yandex-team.ru/api/offers/${offer_id}/`,
        `{\"full_name\":\"${name}\",\"employee_type\":\"new\",\"form_type\":\"international\",\"org\":\"1\",\"department\":\"3577\",\"position\":\"1\",\"staff_position_name\":\"разработчик\",\"work_place\":\"office\",\"office\":\"1\",\"join_at\":\"${join_at}\",\"grade\":\"17\",\"payment_type\":\"monthly\",\"salary\":\"80000.00\",\"payment_currency\":\"1\",\"employment_type\":\"full\",\"is_main_work_place\":\"true\",\"contract_type\":\"indefinite\",\"probation_period_type\":\"3m\",\"vmi\":\"true\",\"sick_leave_supplement\":\"false\",\"housing_program\":\"false\",\"cellular_compensation\":\"false\",\"internet_compensation_amount\":\"0.00\",\"rsu_cost\":\"0.00\",\"signup_bonus\":\"0.00\",\"allowance\":\"0.00\",\"need_relocation\":\"false\"}`
    );
};

// Создание оффера для нового сотрудника (русский)
const makeNewRussianOffer = async(offer_id, name) => {
    const now = moment();
    const join_at = now.add(7, 'days').format('DD.MM.YYYY');
    return await put(
        `https://femida.test.yandex-team.ru/api/offers/${offer_id}/`,
        `{\"full_name\":\"${name}\",\"employee_type\":\"new\",\"is_resident\":\"true\",\"form_type\":\"russian\",\"org\":\"1\",\"department\":\"3577\",\"position\":\"1\",\"staff_position_name\":\"разработчик\",\"work_place\":\"office\",\"office\":\"1\",\"join_at\":\"${join_at}\",\"grade\":\"17\",\"payment_type\":\"monthly\",\"salary\":\"80000\",\"payment_currency\":\"1\",\"employment_type\":\"full\",\"is_main_work_place\":\"true\",\"contract_type\":\"indefinite\",\"probation_period_type\":\"3m\",\"vmi\":\"true\",\"sick_leave_supplement\":\"false\",\"housing_program\":\"false\",\"cellular_compensation\":\"false\",\"internet_compensation_amount\":\"0.00\",\"rsu_cost\":\"0.00\",\"signup_bonus\":\"0.00\",\"allowance\":\"0.00\",\"need_relocation\":\"false\"}`
    );
};

const sendForApproval = async offer_id => {
    return await post(
        `https://femida.test.yandex-team.ru/api/offers/${offer_id}/approve/`,
        '{\"abc_services\":[\"825\"],\"other_payments\":\"\",\"professional_sphere\":\"1\",\"profession\":\"2\",\"professional_level\":\"junior\",\"programming_language\":\"\",\"salary_expectations\":\"\",\"salary_expectations_currency\":\"1\",\"current_company\":\"\",\"source\":\"external_website\",\"source_description\":\"\"}'
    );
};

const getExternalLink = async offer_id => {
    const data = await get(
        `https://femida.test.yandex-team.ru/api/offers/${offer_id}/send/_form/`
    );
    const offerText = data.data.offer_text.value;
    const regex = /(https:\/\/femida[^ ]*)/;
    regex.test(offerText);
    return RegExp.$1;
};

const sendToCandidate = async offer_id => {
    return await post(
        `https://femida.test.yandex-team.ru/api/offers/${offer_id}/send/`,
        '{\"receiver\":\"aaa@bbb.cc\",\"bcc\":[\"annvas\"],\"subject\":\"Яндекс. Предложение о работе\",\"message\":\"a\",\"offer_text\":\"a\"}'
    );
};

module.exports = {
    makeNewRussianOffer,
    makeFormerEmployeeRussianOffer,
    makeNewInternationalOffer,
    sendForApproval,
    getExternalLink,
    sendToCandidate,
};
