specs({
    feature: 'Закрепление чатов',
}, function () {
    const { sendTextMessage } = require('../shared-steps/send-message.hermione');
    // При записи теста, чаты "Def-Имя-autotests D.", "Тест публичного чата", "SearchInChatTest"
    // должны находиться в 1, 2, 3 позиции соответственно

    const getSelectorSeparator = (nth) => `${PO.chatListItemBlock()}:nth-child(${nth}) + ${PO.separator()}`;
    const getSelectorChatName = (nth) => `${PO.chatListItemBlock()}:nth-child(${nth}) ${PO.chatListItemName()}`;

    // Здесь специально перепутаны 0 и 1, так как в таком порядке должны добавляться в запины
    const chatsName = ['Тест публичного чата', 'Def-Имя-autotests D.', 'SearchInChatTest'];

    async function changePinnedChat(name, unpin) {
        const { browser } = this;
        const chatSelector = await browser.yaGetContainsSelector(PO.chatListItemName(), name);
        const elementToClick = unpin ? PO.popup.menu.unpin() : PO.popup.menu.pin();

        await browser.yaScrollIntoView(chatSelector);
        await browser.yaWaitForVisible(chatSelector, 'Не появился чат в списке чатов');
        await browser.rightClick(chatSelector);

        await browser.yaWaitForVisible(
            elementToClick,
            'контекстное меню чата не открылось',
        );
        await browser.click(elementToClick);
    }

    it('Закрепленный чат закрепляется в верхней части списков', async function () {
        const { browser } = this;
        await browser.yaOpenMessenger();

        await browser.waitForVisible('.yamb-sidebar', 'Не показался список чатов');
        // @ts-ignore
        await changePinnedChat.call(this, chatsName[0]);
        await browser.yaWaitForVisible(getSelectorSeparator(1), 'Не появился разделитель после запина первого чата');

        // @ts-ignore
        await changePinnedChat.call(this, chatsName[1]);
        await browser.yaWaitForHidden(getSelectorSeparator(1), 'Не убрался разделитель после первого запина');

        await browser.yaScrollIntoView(PO.chatListItemBlock1());

        await browser.yaWaitForVisible(getSelectorSeparator(2), 'Не появился разделитель после запина второго чата');

        assert.equal(await browser.getText(getSelectorChatName(1)), chatsName[0]);
        assert.equal(await browser.getText(getSelectorChatName(2)), chatsName[1]);
    });

    it('При получении новых сообщений закрепленные чаты отображаются в порядке закрепления в верхней части списка чатов', async function () {
        const { browser } = this;
        await browser.yaOpenMessenger();

        await browser.waitForVisible('.yamb-sidebar', 'Не показался список чатов');
        // @ts-ignore
        await changePinnedChat.call(this, chatsName[0]);
        await browser.yaWaitForVisible(getSelectorSeparator(1), 'Не появился разделитель после запина первого чата');

        // @ts-ignore
        await changePinnedChat.call(this, chatsName[1]);
        await browser.yaWaitForHidden(getSelectorSeparator(1), 'Не убрался разделитель после первого запина');

        await browser.yaScrollIntoView(PO.chatListItemBlock1());

        await browser.yaWaitForVisible(getSelectorSeparator(2), 'Не появился разделитель после запина второго чата');

        // @ts-ignore
        await changePinnedChat.call(this, chatsName[2]);
        await browser.yaWaitForHidden(getSelectorSeparator(2), 'Не убрался разделитель после второго запина');

        await browser.yaScrollIntoView(PO.chatListItemBlock1());

        await browser.yaWaitForVisible(getSelectorSeparator(3), 'Не появился разделитель после запина третьего чата');

        const selectorChat3 = getSelectorChatName(3);
        await browser.click(selectorChat3);

        await browser.yaWaitForVisible(PO.chat(), 'Чат не открылся');

        await sendTextMessage.call(this, 'hello world');

        const isVisibleBackBtn = await browser.isVisible(PO.chat.header.backButton());

        if (isVisibleBackBtn) {
            await browser.click(PO.chat.header.backButton());
        }

        await browser.waitForVisible('.yamb-sidebar', 'Не показался список чатов');
        await browser.yaScrollIntoView(PO.chatListItemBlock1());

        assert.equal(await browser.getText(getSelectorChatName(1)), chatsName[0]);
        assert.equal(await browser.getText(getSelectorChatName(2)), chatsName[1]);
        assert.equal(await browser.getText(getSelectorChatName(3)), chatsName[2]);
    });

    it('При откреплении чат ранжируется в общем порядке, если в него ничего не отправляли', async function () {
        const { browser } = this;
        await browser.yaOpenMessenger();

        await browser.waitForVisible('.yamb-sidebar', 'Не показался список чатов');
        // @ts-ignore
        await changePinnedChat.call(this, chatsName[0]);

        await browser.yaWaitForVisible(getSelectorSeparator(1), 'Не появился разделитель после запина второго чата');

        assert.equal(await browser.getText(getSelectorChatName(1)), chatsName[0]);

        await changePinnedChat.call(this, chatsName[0], true);

        await browser.yaWaitForHidden(getSelectorSeparator(1), 'Не убрался разделитель после запина');

        assert.equal(await browser.getText(getSelectorChatName(1)), chatsName[1]);
        assert.equal(await browser.getText(getSelectorChatName(2)), chatsName[0]);
        assert.equal(await browser.getText(getSelectorChatName(3)), chatsName[2]);
    });
});
