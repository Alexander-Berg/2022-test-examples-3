const fetch = require('node-fetch');
const token = process.env.TOKEN;

const post = async(url, data) => {
    const response = await fetch(
        url,
        {
            headers: {
                Authorization: `OAuth ${token}`,
                'content-type': 'application/json',
            },
            body: data,
            method: 'POST',
        }
    );

    const json = await response.json();

    if (response.status >= 400) {
        throw json;
    }

    return json;
};

const put = async(url, data) => {
    const response = await fetch(
        url,
        {
            headers: {
                Authorization: `OAuth ${token}`,
                'content-type': 'application/json',
            },
            body: data,
            method: 'PUT',
        }
    );
    const json = await response.json();

    if (response.status >= 400) {
        throw json;
    }

    return json;
};

const get = async url => {
    const response = await fetch(
        url,
        {
            headers: {
                Authorization: `OAuth ${token}`,
                'content-type': 'application/json',
            },
            method: 'GET',
        }
    );
    const json = await response.json();

    if (response.status >= 400) {
        throw json;
    }

    return json;
};

module.exports = {
    post,
    get,
    put,
};
