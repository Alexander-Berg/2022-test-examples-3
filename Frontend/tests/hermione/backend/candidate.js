/* eslint-disable no-console */
/* eslint-disable camelcase */
const { post, get } = require('./_request');

// Создать нового кандидата вакансию
const createCandidate = async(firstName, lastName, skype) => {
    return await post(
        'https://femida.test.yandex-team.ru/api/candidates/',
        `{"ignore_duplicates":"true","last_name":"${lastName}","first_name":"${firstName}","middle_name":"","birthday":"","country":"","city":"","source":"","source_description":"","gender":"","contacts":[{"id":"","is_main":"true","type":"email","account_id":""},{"id":"","is_main":"true","type":"phone","account_id":""},{"id":"","is_main":"true","type":"skype","account_id":"${skype}"}]}`
    );
};

const mergeDuplicate = async(candidate_id, duplicate_id) => {
    return await post(
        'https://femida.test.yandex-team.ru/api/candidates/merge/',
        `{\"id\":[\"${candidate_id}\",\"${duplicate_id}\"],\"contacts\":[],\"educations\":[],\"jobs\":[],\"candidate_professions\":[],\"skills\":[],\"attachments\":[],\"target_cities\":[],\"tags\":[]}`
    );
};

const notADuplicate = async id => {
    return await post(
        `https://femida.test.yandex-team.ru/api/duplication_cases/${id}/cancel/`,
        '{}'
    );
};

const handleDuplicatesAsNotDuplicates = async candidate_id => {
    const candidate = await get(
        `https://femida.test.yandex-team.ru/api/candidates/${candidate_id}`
    );

    console.log('candidate with duplicates');
    console.log(candidate);
    console.log('');

    if (!candidate.duplication_cases.length) {
        return candidate;
    }

    const notDuplicates = await Promise.all(candidate.duplication_cases.map(dupCase => {
        return notADuplicate(dupCase.id);
    }));

    console.log('notDuplicates');
    console.log(notDuplicates);
    console.log('');

    return candidate;
};

module.exports = {
    createCandidate,
    handleDuplicatesAsNotDuplicates,
    mergeDuplicate,
};
