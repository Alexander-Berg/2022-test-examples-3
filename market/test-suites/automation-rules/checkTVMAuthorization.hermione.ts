import 'hermione';

import {checkSavedConfig} from './helpers/checkSavedConfig';
import {createAutomationRuleWithHttpRequestAction} from './helpers/createAutomationRuleWithHttpRequestAction';
import {archiveAutomationRule} from './helpers/archiveAutomationRule';

const EXPECTED_CONFIG =
    '1\n{\n2\n  "rules": [\n3\n    {\n4\n      "actions": [\n5\n        {\n6\n          "url": "https://test",\n7\n          "type": "httpRequest",\n8\n          "method": "GET",\n9\n          "authorization": {\n10\n            "url": "https://test",\n11\n            "type": "safetyTvm",\n12\n            "clientId": 240921,\n13\n            "clientName": "test"\n14\n          },\n15\n          "processRedirect": false\n16\n        }\n17\n      ]\n18\n    }\n19\n  ]\n20\n}';

describe('ocrm-1650: Добавление в правило действия в виде HTTP запроса с TVM авторизацией: ', () => {
    it('правило автоматизации создается с правильными параметрами', async function() {
        await createAutomationRuleWithHttpRequestAction(this, 'https://test', 'safetyTvm');

        await checkSavedConfig(this.browser, EXPECTED_CONFIG);

        await archiveAutomationRule(this.browser);
    });
});
