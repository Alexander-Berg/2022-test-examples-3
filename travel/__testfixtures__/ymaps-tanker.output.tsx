/* eslint-disable */
import React from 'react';
import { unsafeDynamicKeyset } from 'utilities/tanker/unsafeDynamicKeyset';
import * as i18nBlock from 'i18nNew/account';
import * as i18nAccountOrderBlock from 'i18nNew/account-Order';

const SomeComponent: React.FC = () => {
    const params = {agencyTitle: 'blabla'};

    console.log(i18nBlock.agency(params));
    console.log(i18nAccountOrderBlock.actionsDotCancelAllCheckin());

    const type = 'cancelAllCheckin';
    console.log(unsafeDynamicKeyset(i18nAccountOrderBlock, `actions.${type}`)());

    return <div>{i18nBlock.agency({agencyTitle: 'blabla'})}</div>;
};

export default SomeComponent;
