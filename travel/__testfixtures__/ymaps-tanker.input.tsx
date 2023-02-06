/* eslint-disable */
import React from 'react';
import i18n from 'i18n';

const SomeComponent: React.FC = () => {
    const keySetName = 'account';
    const keyName = 'agency';
    const params = {agencyTitle: 'blabla'};

    console.log(i18n(keySetName, keyName, params));
    console.log(i18n('account-Order', 'actions.cancelAllCheckin'));

    const type = 'cancelAllCheckin';
    console.log(i18n('account-Order', `actions.${type}`));

    return <div>{i18n('account', 'agency', {agencyTitle: 'blabla'})}</div>;
};

export default SomeComponent;
