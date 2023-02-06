import React from 'react';
import { render } from 'react-dom';

import { BPage } from '../../internal/components/BPage/BPage';
import { Hermione } from '../../internal/components/Hermione/Hermione';
import '../../internal/components/BPage/BPage@test.css';
import './Hermione.components/Hermione.css';

import { configureRootTheme } from '../../Theme';
import { theme } from '../../Theme/presets/default';

import { Dropdown } from '../desktop';
import { Button } from '../../Button/desktop/bundle';
import { SampleMenu } from '../__examples__/SampleMenu';

configureRootTheme({
    theme,
});

const DropdownHermioneSample = () => {
    return (
        <BPage>
            <Hermione element="CursorReset" />
            <Hermione>
                {['click', 'hover', 'focus'].map((action, index) => (
                    <Hermione key={index} className={action} element="Item">
                        <Dropdown trigger={[action as any]} view="default" content={<SampleMenu />}>
                            <Button size="s" view="default">
                                Dropdown
                            </Button>
                        </Dropdown>
                    </Hermione>
                ))}
            </Hermione>
        </BPage>
    );
};

render(<DropdownHermioneSample />, document.getElementById('root'));
