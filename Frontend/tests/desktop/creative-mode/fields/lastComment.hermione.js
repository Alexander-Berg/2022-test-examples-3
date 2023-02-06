const PO = require('../../../../page-objects');
const START_URL = '/creative-mode/57452';

describe('Поля режима креатива', function() {
    describe('lastComment', function() {
        beforeEach(function() {
            return this.browser
                .setViewportSize({ width: 2500, height: 2000 })
                .loginToGoals();
        });

        it('внешний вид', function() {
            return this.browser
                .preparePage('creative-mode-lastComment', START_URL)
                .waitForVisible(PO.creativeMode.table.contentRow.comments.paranja())
                .assertView(
                    'umb-lastComment-plain',
                    PO.creativeMode.table.headerRow.comments(),
                )
                .assertView(
                    'outline-lastComment-with-paranja',
                    PO.creativeMode.table.contentRow.comments(),
                );
        });

        it('паранжа отображается для длинных комментов и не отображается для коротких', async function() {
            return this.browser
                .preparePage('creative-mode-lastComment', START_URL)
                .waitForVisible(PO.creativeMode.table.contentRow.comments.paranja())
                .yaShouldBeVisible(
                    PO.creativeMode.table.headerRow.comments.paranja(),
                    'Паранжи не должно быть на коротких комментах',
                    false,
                );
        });

        it('длинный коммент открывается и закрывается', async function() {
            return this.browser
                .preparePage('creative-mode-lastComment', START_URL)
                .waitForVisible(PO.creativeMode.table.contentRow.comments.paranja())
                .click(PO.creativeMode.table.contentRow.comments.paranja())
                .yaWaitForHidden(PO.creativeMode.table.contentRow.comments.paranja())
                .assertView(
                    'unfolded-comment',
                    PO.creativeMode.table.contentRow.comments(),
                )
                .click(PO.creativeMode.table.contentRow.comments.collapseButton())
                .waitForVisible(PO.creativeMode.table.contentRow.comments.paranja())
                .assertView(
                    'folded-comment',
                    PO.creativeMode.table.contentRow.comments(),
                );
        });
    });
});
