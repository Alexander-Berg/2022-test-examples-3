const config = require('config');

const normalizeVal = (val) => val < 10 ? '0' + val : val;
const getTime = (date) => `${normalizeVal(date.getHours())}:${normalizeVal(date.getMinutes())}:${normalizeVal(date.getSeconds())}`;
const getData = (date) => `${normalizeVal(date.getDate())}.${normalizeVal(date.getMonth())}`;

const getTextWithDate = (customText) => {
    const date = new Date();
    return `[${getTime(date)}/${getData(date)}] hermione-e2e: ${customText}`;
};

const getRandomString = () => Math.random().toString(36).substring(2);

const getMDSHostName = () => {
    const { s3Storage } = config;
    return `${s3Storage.publicEndpoint.replace('${bucket}', s3Storage.defaultBucket)}`;
};

module.exports = { getTextWithDate, getRandomString, getMDSHostName };
