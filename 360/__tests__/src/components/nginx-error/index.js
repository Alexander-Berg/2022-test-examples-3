import NginxError from 'components/nginx-error';
import React from 'react';
import { render } from 'enzyme';

const runTest = (errorCode, lang = 'ru') => {
    const component = render(
        <NginxError lang={lang} errorCode={errorCode}/>
    );
    expect(component).toMatchSnapshot();
};

describe('nginx-error', () => {
    it('403, ru', () => {
        runTest(403);
    });

    it('404, ru', () => {
        runTest(404);
    });

    it('500, ru', () => {
        runTest(500);
    });

    it('403, uk', () => {
        runTest(403, 'uk');
    });

    it('404, en', () => {
        runTest(404, 'en');
    });

    it('500, tr', () => {
        runTest(500, 'tr');
    });
});
