/* global langs */
import Error from 'components/error';
import React from 'react';
import { shallow } from 'enzyme';

const errCodes = ['', '!@#$', 400, 403, 404, 500, 'UNABLE_TO_RETRIEVE_FILE', 'UNABLE_TO_RETRIEVE_FILE_NO_LENGTH_SPECIFIED',
    'UNKNOWN_CONVERT_ERROR', 'UNKNOWN_COPY_ERROR', 'UNSUPPORTED_SOURCE_TYPE', 'UNSUPPORTED_CONVERTION',
    'CONVERT_TIMEOUT', 'FILE_TOO_BIG', 'ARCHIVE_TOO_BIG', 'FILE_IS_EMPTY', 'FILE_IS_FORBIDDEN',
    'FILE_IS_PASSWORD_PROTECTED', 'FILE_NOT_FOUND', 'UNKNOWN_ERROR', 'BROWSER_FILE_EXPIRED'];

const runTest = (lang, errorCode, limitLength) => {
    const component = shallow(
        <Error lang={lang} errorCode={errorCode} limitLength={limitLength}/>
    );
    expect(component).toMatchSnapshot();
};

for (let i = 0; i < langs.length; i++) {
    it(langs[i] + '-error-pages', () => {
        for (let j = 0; j < errCodes.length; j++) {
            const limitLength = errCodes[j] === 'FILE_TOO_BIG' ? 128 * 1024 * 1024 :
                errCodes[j] === 'ARCHIVE_TOO_BIG' ? 1024 * 1024 * 1024 : 0;
            runTest(langs[i], errCodes[j], limitLength);
        }
    });
}
