import React from 'react';

import {Text} from '@yandex-levitan/b2b';

import I18n from 'shared/containers/I18n';

import check from './quality_check.svg';
import css from '../style.css';

type TestingStatusProps = {
    startDate: string;
    endDate: string;
};

const TestingStatus = ({startDate, endDate}: TestingStatusProps) => (
    <>
        <Text as="div" className={css.icon}>
            <img src={check} />
        </Text>
        <Text className={css.testingDescription}>
            <I18n id="pages.dashboard:quality.testing.check" params={{startDate, endDate}} />
        </Text>
    </>
);

export default TestingStatus;
