/* eslint-disable */
'use strict';

const str2base64 = (str) => Buffer.from(str).toString('base64');

module.exports = {
    wrongSchema: [{
        enabled: '28235,0,96;36569,0,42;36596,0,26;38970,0,2;25465,0,50;34680,0,78;38060,0,14;123123,0,14',
        expboxes: '28235,0,96;36569,0,42;36596,0,26;38970,0,2;25465,0,50;34680,0,78;38060,0,14;123123,0,14',
        checkers: [
            `[{"HANDLER": "GATEWAY", "CONTEXT": {"GATEWAY": {"pre": ["true"], "atom.params.relev": ["atom_stored-candidate-list=promoliba_EXP_prod"]}}, "CONDITION": "SESSION_atom_client == 'promolib'"}]`,
            `[{"HANDLER": "MORDA", "CONDITION": "desktop && ( (device.BrowserName eq 'MSIE' && device.BrowserVersion ge '11') || device.BrowserEngine eq 'Edge' || (device.BrowserBase eq 'Chromium' && device.BrowserBaseVersion ge '31') || (device.BrowserName eq 'Opera' && device.BrowserVersion ge '15.6') || device.BrowserName eq 'YandexBrowser') && !(device.OSFamily eq 'Ubuntu' && device.BrowserName eq 'YandexBrowser')", "CONTEXT": {"MORDA": {"testid": ["36569"]}}}, {"HANDLER": "REPORT", "CONDITION": "desktop && ( (device.BrowserName eq 'MSIE' && device.BrowserVersion ge '11') || device.BrowserEngine eq 'Edge' || (device.BrowserBase eq 'Chromium' && device.BrowserBaseVersion ge '31') || (device.BrowserName eq 'Opera' && device.BrowserVersion ge '15.6') || device.BrowserName eq 'YandexBrowser') && !(device.OSFamily eq 'Ubuntu' && device.BrowserName eq 'YandexBrowser')", "CONTEXT": {"REPORT": {"testid": ["36569"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "MORDA", "CONTEXT": {"MORDA": {"flags": ["yxnews_all_rubrics_rank_extra=exp2"], "testid": ["36596"]}}, "CONDITION": "desktop"}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"flags": ["yxnews_all_rubrics_rank_extra=exp2", "yxnews_exp_banner=exp2"], "testid": ["36596"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "MORDA", "CONTEXT": {"MORDA": {"testid": ["38970"]}}, "CONDITION": "device.isTV && device.BrowserEngine eq 'WebKit'"}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"testid": ["38970"]}, "MAIN": {}}, "CONDITION": "device.isTV && device.BrowserEngine eq 'WebKit'"}]`,
            `[{"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"testid": ["25465"]}, "MAIN": {}}}]`,
            // applied in settings, wrong format
            `[{"HANDLER": "MAIL", "CONTEXT": {"MAIL": {"flags": ["{\\"conditions\\":{\\"type\\":\\"object\\",\\"required\\":[\\"currentInterface\\",\\"isCorp\\"],\\"properties\\":{\\"currentInterface\\":{\\"enum\\":[\\"liza\\"]},\\"isCorp\\":{\\"enum\\":[false]}}}}"]}}}]`,
            // applied in settings, wrong format
            `[{"HANDLER": "MAIL", "CONTEXT": {"MAIL": {"flags": ["{\\"conditions\\":{\\"type\\":\\"object\\",\\"required\\":[\\"currentInterface\\",\\"isCorp\\"],\\"properties\\":{\\"currentInterface\\":{\\"enum\\":[\\"liza\\"]},\\"isCorp\\":{\\"enum\\":[false]}}}}"]}}}]`,
            // wrong format
            `[{"HANDLER": "MAIL", "CONTEXT": {"MAIL": {"flags": ["{\\"conditions\\":{\\"type\\":\\"object\\",\\"required\\":[\\"currentInterface\\",\\"isCorp\\"],\\"properties\\":{\\"currentInterface\\":{\\"enum\\":[\\"liza\\"]},\\"isCorp\\":{\\"enum\\":[false]}}}}"]}}}]`
        ].map(str2base64).join(',')
    }],

    schemaError: [{
        enabled: '28235,0,96;36569,0,42;36596,0,26;38970,0,2;25465,0,50',
        expboxes: '28235,0,96;36569,0,42;36596,0,26;38970,0,2;25465,0,50;123123,0,14',
        checkers: [
            `[{"HANDLER": "GATEWAY", "CONTEXT": {"GATEWAY": {"pre": ["true"], "atom.params.relev": ["atom_stored-candidate-list=promoliba_EXP_prod"]}}, "CONDITION": "SESSION_atom_client == 'promolib'"}]`,
            `[{"HANDLER": "MORDA", "CONDITION": "desktop && ( (device.BrowserName eq 'MSIE' && device.BrowserVersion ge '11') || device.BrowserEngine eq 'Edge' || (device.BrowserBase eq 'Chromium' && device.BrowserBaseVersion ge '31') || (device.BrowserName eq 'Opera' && device.BrowserVersion ge '15.6') || device.BrowserName eq 'YandexBrowser') && !(device.OSFamily eq 'Ubuntu' && device.BrowserName eq 'YandexBrowser')", "CONTEXT": {"MORDA": {"testid": ["36569"]}}}, {"HANDLER": "REPORT", "CONDITION": "desktop && ( (device.BrowserName eq 'MSIE' && device.BrowserVersion ge '11') || device.BrowserEngine eq 'Edge' || (device.BrowserBase eq 'Chromium' && device.BrowserBaseVersion ge '31') || (device.BrowserName eq 'Opera' && device.BrowserVersion ge '15.6') || device.BrowserName eq 'YandexBrowser') && !(device.OSFamily eq 'Ubuntu' && device.BrowserName eq 'YandexBrowser')", "CONTEXT": {"REPORT": {"testid": ["36569"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "MORDA", "CONTEXT": {"MORDA": {"flags": ["yxnews_all_rubrics_rank_extra=exp2"], "testid": ["36596"]}}, "CONDITION": "desktop"}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"flags": ["yxnews_all_rubrics_rank_extra=exp2", "yxnews_exp_banner=exp2"], "testid": ["36596"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "MORDA", "CONTEXT": {"MORDA": {"testid": ["38970"]}}, "CONDITION": "device.isTV && device.BrowserEngine eq 'WebKit'"}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"testid": ["38970"]}, "MAIN": {}}, "CONDITION": "device.isTV && device.BrowserEngine eq 'WebKit'"}]`,
            `[{"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"testid": ["25465"]}, "MAIN": {}}}]`,
            // schema with error
            `[{"HANDLER":"MAIL","CONTEXT":{"MAIL":{"flags":["{\\"conditions\\":\\"trololo\\"}"]}}}]`
        ].map(str2base64).join(',')
    }],

    user: [{
        enabled: '28235,0,96;36569,0,42;36596,0,26;38970,0,2;25465,0,50;34680,0,78;38060,0,14;123123,0,0',
        expboxes: '28235,0,96;36569,0,42;36596,0,26;38970,0,2;25465,0,50;34680,0,78;38060,0,14;123123,0,0',
        checkers: [
            `[{"HANDLER": "GATEWAY", "CONTEXT": {"GATEWAY": {"pre": ["true"], "atom.params.relev": ["atom_stored-candidate-list=promoliba_EXP_prod"]}}, "CONDITION": "SESSION_atom_client == 'promolib'"}]`,
            `[{"HANDLER": "MORDA", "CONDITION": "desktop && ( (device.BrowserName eq 'MSIE' && device.BrowserVersion ge '11') || device.BrowserEngine eq 'Edge' || (device.BrowserBase eq 'Chromium' && device.BrowserBaseVersion ge '31') || (device.BrowserName eq 'Opera' && device.BrowserVersion ge '15.6') || device.BrowserName eq 'YandexBrowser') && !(device.OSFamily eq 'Ubuntu' && device.BrowserName eq 'YandexBrowser')", "CONTEXT": {"MORDA": {"testid": ["36569"]}}}, {"HANDLER": "REPORT", "CONDITION": "desktop && ( (device.BrowserName eq 'MSIE' && device.BrowserVersion ge '11') || device.BrowserEngine eq 'Edge' || (device.BrowserBase eq 'Chromium' && device.BrowserBaseVersion ge '31') || (device.BrowserName eq 'Opera' && device.BrowserVersion ge '15.6') || device.BrowserName eq 'YandexBrowser') && !(device.OSFamily eq 'Ubuntu' && device.BrowserName eq 'YandexBrowser')", "CONTEXT": {"REPORT": {"testid": ["36569"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "MORDA", "CONTEXT": {"MORDA": {"flags": ["yxnews_all_rubrics_rank_extra=exp2"], "testid": ["36596"]}}, "CONDITION": "desktop"}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"flags": ["yxnews_all_rubrics_rank_extra=exp2", "yxnews_exp_banner=exp2"], "testid": ["36596"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "MORDA", "CONTEXT": {"MORDA": {"testid": ["38970"]}}, "CONDITION": "device.isTV && device.BrowserEngine eq 'WebKit'"}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"testid": ["38970"]}, "MAIN": {}}, "CONDITION": "device.isTV && device.BrowserEngine eq 'WebKit'"}]`,
            `[{"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"testid": ["25465"]}, "MAIN": {}}}]`,
            // applied in settings
            `[{"HANDLER":"MAIL","CONTEXT":{"MAIL":{"flags":["{\\"conditions\\":\\"currentInterface == 'liza' && !isCorp\\"}"]}}}]`,
            // applied in settings
            `[{"HANDLER":"MAIL","CONTEXT":{"MAIL":{"flags":["{\\"conditions\\":\\"currentInterface == 'liza' && !isCorp\\"}"]}}}]`,
            // suitable with values
            `[{"HANDLER":"MAIL","CONTEXT":{"MAIL":{"flags":["{\\"conditions\\":\\"'lite' in supportedInterfaces && !isCorp\\"}"]}}}]`
        ].map(str2base64).join(',')
    }],

    pddUser: [{
        enabled: '28236,0,73;34317,0,99;36569,0,72;36596,0,7;38970,0,86;34680,0,94;38060,0,75',
        expboxes: '28236,0,73;34317,0,99;36569,0,72;36596,0,7;38970,0,86;34680,0,94;38060,0,75',
        checkers: [
            `[{"HANDLER": "GATEWAY", "CONTEXT": {"GATEWAY": {"pre": ["true"], "atom.params.relev": ["atom_stored-candidate-list=promoliba_EXP_exp;atom_fml=EXP_promolib_fml"]}}, "CONDITION": "SESSION_atom_client == 'promolib'"}]`,
            `[{"HANDLER": "MORDA", "CONDITION": "desktop", "CONTEXT": {"MORDA": {"testid": ["34317"]}}}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"testid": ["34317"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "MORDA", "CONDITION": "desktop && ( (device.BrowserName eq 'MSIE' && device.BrowserVersion ge '11') || device.BrowserEngine eq 'Edge' || (device.BrowserBase eq 'Chromium' && device.BrowserBaseVersion ge '31') || (device.BrowserName eq 'Opera' && device.BrowserVersion ge '15.6') || device.BrowserName eq 'YandexBrowser') && !(device.OSFamily eq 'Ubuntu' && device.BrowserName eq 'YandexBrowser')", "CONTEXT": {"MORDA": {"testid": ["36569"]}}}, {"HANDLER": "REPORT", "CONDITION": "desktop && ( (device.BrowserName eq 'MSIE' && device.BrowserVersion ge '11') || device.BrowserEngine eq 'Edge' || (device.BrowserBase eq 'Chromium' && device.BrowserBaseVersion ge '31') || (device.BrowserName eq 'Opera' && device.BrowserVersion ge '15.6') || device.BrowserName eq 'YandexBrowser') && !(device.OSFamily eq 'Ubuntu' && device.BrowserName eq 'YandexBrowser')", "CONTEXT": {"REPORT": {"testid": ["36569"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "MORDA", "CONTEXT": {"MORDA": {"flags": ["yxnews_all_rubrics_rank_extra=exp2"], "testid": ["36596"]}}, "CONDITION": "desktop"}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"flags": ["yxnews_all_rubrics_rank_extra=exp2", "yxnews_exp_banner=exp2"], "testid": ["36596"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "MORDA", "CONTEXT": {"MORDA": {"testid": ["38970"]}}, "CONDITION": "device.isTV && device.BrowserEngine eq 'WebKit'"}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"testid": ["38970"]}, "MAIN": {}}, "CONDITION": "device.isTV && device.BrowserEngine eq 'WebKit'"}]`,
            `[{"HANDLER": "MAIL", "CONTEXT": {"MAIL": {"flags": ["{\\"conditions\\":\\"currentInterface == 'liza' && isPDD\\"}"]}}}]`,
            `[{"HANDLER": "MAIL", "CONTEXT": {"MAIL": {"flags": ["{\\"conditions\\":\\"currentInterface == 'liza' && isPDD\\"}"]}}}]`
        ].map(str2base64).join(',')
    }],

    corpUser: [{
        enabled: '36596,0,93;38969,0,79;37580,0,41;25469,0,76',
        expboxes: '36596,0,93;38969,0,79;37580,0,41;25469,0,76;34680,0,94;38060,0,3',
        checkers: [
            `[{"HANDLER": "MORDA", "CONTEXT": {"MORDA": {"flags": ["yxnews_all_rubrics_rank_extra=exp2"], "testid": ["36596"]}}, "CONDITION": "desktop"}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"flags": ["yxnews_all_rubrics_rank_extra=exp2", "yxnews_exp_banner=exp2"], "testid": ["36596"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "MORDA", "CONTEXT": {"MORDA": {"testid": ["38969"]}}, "CONDITION": "device.isTV && device.BrowserEngine eq 'WebKit'"}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"testid": ["38969"]}, "MAIN": {}}, "CONDITION": "device.isTV && device.BrowserEngine eq 'WebKit'"}]`,
            `[{"HANDLER": "MORDA", "CONDITION": "", "CONTEXT": {"MORDA": {"testid": ["37580"]}}}, {"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"testid": ["37580"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "REPORT", "CONTEXT": {"REPORT": {"testid": ["25469"]}, "MAIN": {}}}]`,
            `[{"HANDLER": "MAIL", "CONTEXT": {"MAIL": {"flags": ["{\\"conditions\\":\\"currentInterface == 'liza' && !isCorp\\"}"]}}}]`,
            `[{"HANDLER": "MAIL", "CONTEXT": {"MAIL": {"flags": ["{\\"conditions\\":\\"currentInterface == 'liza' && !isCorp\\"}"]}}}]`
        ].map(str2base64).join(',')
    }],

    test1: [{
        enabled: '80003,0,0',
        expboxes: '80003,0,0',
        checkers: [
            `[{"HANDLER": "MAIL","CONTEXT": {"MAIL": {"flags": ["{\\"conditions\\":\\"currentInterface == 'liza' && !isCorp\\",\\"actions\\":{\\"settings\\":{\\"color_scheme\\":\\"tanks\\"}}}"]}}}]`
        ].map(str2base64).join(',')
    }]
}
