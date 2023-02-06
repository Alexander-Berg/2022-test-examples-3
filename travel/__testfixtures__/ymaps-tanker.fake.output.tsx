/* eslint-disable */
import React from 'react';

const i18n = (a: string, b: string) => 'haha i am not i18n' + a + b;

const SomeComponent: React.FC = () => {
    return <div>{i18n('account', 'agency')}</div>;
};

export default SomeComponent;
