import React, { ReactNode } from 'react';

import Fieldset from 'client/components/fieldset';
import { SectionNavHashItem } from 'client/components/section-nav';
import block from 'client/utils/cn';
import i18n from 'client/utils/i18n';

interface Props {
    isDisabled?: boolean;
    children?: ReactNode;
}

const b = block('testing-and-report');
const fieldset = block('fieldset');

const TestingAndReport = (props: Props) => (
    <Fieldset className={b()} isDisabled={props.isDisabled} name="testing-and-report" raw group>
        <legend className={fieldset('legend', { size: 'm' })}>
            <SectionNavHashItem hash="testing-and-report">
                {i18n.text({ keyset: 'contest-settings', key: 'testing-and-report' })}
            </SectionNavHashItem>
        </legend>
        {props.children}
    </Fieldset>
);

export default TestingAndReport;
