import {createAutomationRuleWithHttpRequestAction} from './helpers/createAutomationRuleWithHttpRequestAction';
import {checkSavedConfig} from './helpers/checkSavedConfig';
import {archiveAutomationRule} from './helpers/archiveAutomationRule';

const EXPECTED_CONFIG_WITH_REDIRECT =
    '1\n{\n2\n  "rules": [\n3\n    {\n4\n      "actions": [\n5\n        {\n6\n          "url": "https://test",\n7\n          "type": "httpRequest",\n8\n          "method": "GET",\n9\n          "authorization": {\n10\n            "type": "noAuth"\n11\n          },\n12\n          "processRedirect": true\n13\n        }\n14\n      ]\n15\n    }\n16\n  ]\n17\n}';

const EXPECTED_CONFIG_WITHOUT_REDIRECT =
    '1\n{\n2\n  "rules": [\n3\n    {\n4\n      "actions": [\n5\n        {\n6\n          "url": "https://test",\n7\n          "type": "httpRequest",\n8\n          "method": "GET",\n9\n          "authorization": {\n10\n            "type": "noAuth"\n11\n          },\n12\n          "processRedirect": false\n13\n        }\n14\n      ]\n15\n    }\n16\n  ]\n17\n}';

describe('ocrm-1625: Включение редиректа в действиях типа "HTTP запрос"', () => {
    it('правило автоматизации создается с флагом processRedirect = true', async function() {
        await createAutomationRuleWithHttpRequestAction(this, 'https://test', 'noAuth', true);

        await checkSavedConfig(this.browser, EXPECTED_CONFIG_WITH_REDIRECT);

        await archiveAutomationRule(this.browser);
    });
});

describe('ocrm-1626: Запрет редиректа в действиях типа "HTTP запрос"', () => {
    it('правило автоматизации создается с флагом processRedirect = false', async function() {
        await createAutomationRuleWithHttpRequestAction(this, 'https://test');

        await checkSavedConfig(this.browser, EXPECTED_CONFIG_WITHOUT_REDIRECT);

        await archiveAutomationRule(this.browser);
    });
});
