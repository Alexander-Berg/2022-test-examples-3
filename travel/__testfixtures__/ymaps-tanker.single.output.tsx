/* eslint-disable */
import React from 'react';
import * as i18nBlock from 'i18nNew/account';

const SomeComponent: React.FC = () => {
    const params = {agencyTitle: 'blabla'};

    console.log(i18nBlock.agency(params));

    return <div>{i18nBlock.agency({agencyTitle: 'blabla'})}</div>;
};

export default SomeComponent;
