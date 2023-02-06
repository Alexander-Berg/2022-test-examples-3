import Error from 'components/error';
import React from 'react';
import { shallow } from 'enzyme';

const runTest = (errorCode, limitLength) => {
    const component = shallow(
        <Error lang="ru" errorCode={errorCode} limitLength={limitLength}/>
    );
    expect(component).toMatchSnapshot();
};

it('content-error-400', () => {
    runTest(400);
});

it('content-error-500', () => {
    runTest('500');
});

it('content-error-file-too-big', () => {
    runTest('FILE_TOO_BIG', 128 * 1024 * 1024);
});

it('content-error-file-is-empty', () => {
    runTest('FILE_IS_EMPTY');
});

it('content-error-unknown', () => {
    runTest('');
});
