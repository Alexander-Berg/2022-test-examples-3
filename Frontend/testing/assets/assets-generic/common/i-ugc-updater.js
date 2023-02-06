//Если ugcUpdateUrl использует srсrwr из квери-параметров, флаг test_tool=generic не нужен
(function() {
    var ugcUpdateUrlFromTemplates = BEM.blocks['i-global'].param('ugcUpdateUrl');

    if (! /srcrwr/.test(ugcUpdateUrlFromTemplates)) {
        BEM.blocks['i-global'].setParams({
            ugcUpdateUrl: '/search/ugcupdate?exp_flags=test_tool%3Dgeneric'
        });
    }
})();
