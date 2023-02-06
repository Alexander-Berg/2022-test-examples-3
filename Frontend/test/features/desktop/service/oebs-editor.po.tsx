import React from 'react';
import { render as libRender } from '@testing-library/react';

import OebsEditorConnected from '~/src/features/Service/components/OebsEditor/OebsEditor.container';

import { configureStore } from '~/src/abc/react/redux/store';
import { withRedux } from '~/src/common/hoc';

import { Button, Checkbox, PageFragment, RootPageFragment } from '~/test/jest/utils';

jest.mock('~/src/features/Perfection/components/PerfectionTrafficLights/PerfectionTrafficLights.container.js');

export class OebsEditor extends RootPageFragment {
    get oebsUseForHrCheckbox() {
        return this.query(Checkbox, '.OebsEditor-Checkbox_type_useForHr');
    }

    get oebsUseForProcurementCheckbox() {
        return this.query(Checkbox, '.OebsEditor-Checkbox_type_useForProcurement');
    }

    get oebsUseForRevenueCheckbox() {
        return this.query(Checkbox, '.OebsEditor-Checkbox_type_useForRevenue');
    }

    get oebsUseForHardwareCheckbox() {
        return this.query(Checkbox, '.OebsEditor-Checkbox_type_useForHardware');
    }

    get oebsUseForGroupCheckbox() {
        return this.query(Checkbox, '.OebsEditor-Checkbox_type_useForGroup');
    }

    get checkboxes() {
        return this.getAll(Checkbox, '.OebsEditor-Checkbox');
    }

    get submitButton() {
        return this.query(Button, '.OebsEditor-Footer .Button2');
    }

    get infoMessage() {
        return this.query(PageFragment, '.Message_type_info');
    }

    get errorMessage() {
        return this.query(PageFragment, '.Message_type_error');
    }

    areFormValuesEqualTo(hr: boolean, procurement: boolean, revenue: boolean) {
        return this.oebsUseForHrCheckbox?.isChecked === hr &&
            this.oebsUseForProcurementCheckbox?.isChecked === procurement &&
            this.oebsUseForRevenueCheckbox?.isChecked === revenue;
    }
}

export function render(storeData: Record<string, unknown>): OebsEditor {
    const store = configureStore({
        initialState: storeData,
        fetcherOptions: {
            fetch: () => Promise.resolve({}),
        },
        sagaContextExtension: {
            api: {
                getServiceFields: () => Promise.resolve({}),
                getOebsFields: () => Promise.resolve({}),
                setOebsFlags: () => Promise.resolve({}),
            },
        },
    });

    const OebsEditorForm = withRedux(OebsEditorConnected, store);

    return new OebsEditor(libRender(
        <OebsEditorForm />,
    ));
}
