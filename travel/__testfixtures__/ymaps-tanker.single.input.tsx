/* eslint-disable */
import React from 'react';
import i18n from 'i18n';

const SomeComponent: React.FC = () => {
    const keySetName = 'account';
    const keyName = 'agency';
    const params = {agencyTitle: 'blabla'};

    console.log(i18n(keySetName, keyName, params));

    return <div>{i18n('account', 'agency', {agencyTitle: 'blabla'})}</div>;
};

export default SomeComponent;
