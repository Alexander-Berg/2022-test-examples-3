const { createTestTagSelector } = require('./utils');

module.exports = (El) => {
    const PO = {};

    function taggedEl(testTag, dataTag) {
        return new El(createTestTagSelector(testTag, dataTag));
    }

    PO.chat = new El({ block: 'yamb-chat' });
    PO.chat.loader = new El({ block: 'ui-loader' });
    PO.chat.joinButton = new El({ block: 'yamb-chat-action', elem: 'button' }).mods({ filled: true });
    PO.chat.header = taggedEl('chat-header');
    PO.chat.header.title = new El({ block: 'yamb-text' }).mods({ leading: true });

    PO.chat.header.menuButton = taggedEl('chat-header-menu');
    PO.chat.header.backButton = taggedEl('chat-header-back');
    PO.chat.header.searchButton = taggedEl('chat-header-search');

    PO.chat.search = taggedEl('chat-search');
    PO.chat.search.input = taggedEl('chat-search-input');
    PO.chat.search.prev = taggedEl('chat-search-prev');
    PO.chat.search.next = taggedEl('chat-search-next');

    PO.chat.toolbar = new El({ block: 'ui-toolbar' });
    PO.chat.toolbar.title = new El({ block: 'ui-toolbar', elem: 'title' });
    PO.scrollToBottom = taggedEl('scroll-to-bottom');

    PO.chatInfo = taggedEl('chat-info');
    PO.chatInfo.title = taggedEl('chat-info-title');
    PO.chatInfo.description = taggedEl('chat-info-description');
    PO.chatInfo.members = taggedEl('chat-info-followers');
    PO.chatInfo.blockUser = taggedEl('chat-info-block-user');
    PO.chatInfo.unblockUser = taggedEl('chat-info-unblock-user');
    PO.chatInfo.notifications = taggedEl('chat-info-notifications');
    PO.chatInfo.search = taggedEl('chat-info-search');
    PO.chatInfo.inviteLink = taggedEl('chat-info-invite-link');
    PO.chatInfo.inviteLink.link = taggedEl('info-list-item-text-secondary');
    PO.chatInfo.settings = taggedEl('chat-info-channel-settings');
    PO.chatInfo.assetsBrowser = taggedEl('chat-info-assets-browser');
    PO.chatInfo.report = taggedEl('chat-info-assets-report');
    PO.chatInfo.leave = taggedEl('chat-info-assets-leave');
    PO.chatSettings = taggedEl('chat-settings');
    PO.chatSettings.publicSwitchOn = taggedEl('chat-settings-public-switch-on');
    PO.chatSettings.publicSwitchOff = taggedEl('chat-settings-public-switch-off');
    PO.chatSettings.save = taggedEl('chat-settings-save');
    PO.chatMember = taggedEl('list-item-user');
    PO.chatMembersList = taggedEl('chat-members-list');
    PO.chatMembersList.memberAdmin = taggedEl('chat-members-list-admin');
    PO.chatMembersList.memberAdmin.menu = taggedEl('chat-members-list-admin-menu');
    PO.chatMembersList.member = taggedEl('chat-members-list-subscriber');
    PO.chatMembersList.member.menu = taggedEl('chat-members-list-subscriber-menu');
    PO.chatMemberMenuRemove = taggedEl('chat-member-menu-remove');

    PO.removeMemberConfirm = taggedEl('remove-member-confirm-ok');

    // ChatHeader standalone
    PO.chatHeader = new El({ block: 'yamb-chat-header' });

    PO.chatListItem = taggedEl('yamb-chat-list-item');
    PO.chatListItemChannel = taggedEl('yamb-chat-list-item-channel');
    PO.chatListItemPrivate = taggedEl('yamb-chat-list-item-private');
    // Используется как общее для PO.chatListItem PO.chatListItemChannel PO.chatListItemPrivate
    PO.chatListItemBlock = new El({ block: 'yamb-chat-list-item-block' });
    PO.chatListItemBlock1 = PO.chatListItemBlock.nthChild(1);
    PO.chatListItemName = taggedEl('yamb-chat-list-item-title');
    PO.chatListWriteBtn = taggedEl('chatlist-write-btn');

    PO.createChatWizard = taggedEl('create-chat-wizard');
    PO.createGroupChatBtn = taggedEl('create-group-chat-btn');
    PO.createGroupChat = taggedEl('create-group-chat');
    PO.createGroupChatName = taggedEl('create-group-chat-name');
    PO.createGroupChatDescription = taggedEl('create-group-chat-description');
    PO.createGroupChatAddMembers = taggedEl('create-group-chat-add-members-btn');
    PO.createChatSelectMembers = taggedEl('create-chat-select-members');
    PO.createChatCreateBtn = taggedEl('create-chat-create-btn');

    PO.pinnedMessage = new El({ block: 'yamb-pinned-message' });
    PO.pinnedMessage.remove = new El({ block: 'yamb-pinned-message', elem: 'remove' });

    PO.sidebar = new El({ block: 'yamb-sidebar' });
    PO.sidebarChatList = new El({ block: 'yamb-sidebar', elem: 'chat-list' });

    PO.chatListItem1 = PO.chatListItem.nthChild(1);
    PO.chatListItem2 = PO.chatListItem.nthChild(2);
    PO.separator = new El({ block: 'ui-list-separator' });
    PO.chatListItemNotSticky = PO.separator.adjacentSibling(PO.chatListItem);

    PO.mediaItem = new El({ block: 'yamb-media-gallery-item' });
    PO.mediaItem.image = new El({ block: 'yamb-media-gallery-item', elem: 'image' });
    PO.mediaItem.imageContainer = new El({ block: 'yamb-media-gallery-item', elem: 'image-container' });

    PO.lightbox = new El({ block: 'yamb-lightbox' });
    PO.lightbox.loader = new El({ block: 'yamb-lightbox', elem: 'loader' });
    PO.lightbox.loader.spinner = new El({ block: 'ui-spinner' });
    PO.lightbox.toolbar = new El({ block: 'yamb-lightbox', elem: 'toolbar' });
    PO.lightbox.toolbar.summary = new El({ block: 'yamb-lightbox', elem: 'summary' });
    PO.lightbox.toolbar.download = new El({ block: 'yamb-lightbox-action-panel', elem: 'link' });
    PO.lightbox.close = new El({ block: 'yamb-lightbox', elem: 'close' });
    PO.lightbox.left = new El({ block: 'yamb-lightbox', elem: 'left' });
    PO.lightbox.right = new El({ block: 'yamb-lightbox', elem: 'right' });

    PO.compose = new El({ block: 'yamb-compose' });
    PO.compose.container = new El({ block: 'ui-textarea', elem: 'container' });
    PO.compose.container.input = new El({ block: 'ui-textarea', elem: 'control' });
    PO.compose.quote = new El({ block: 'yamb-quote' });
    PO.compose.quote.close = new El({ block: 'yamb-quote', elem: 'cancel' });
    PO.compose.sendMessageButtonDisabled = new El({ block: 'yamb-compose-submit-button' }).mods({ type: 'button', disabled: true });
    PO.compose.sendMessageButtonEnabled = new El({ block: 'yamb-compose-submit-button' }).mods({ type: 'button' });
    PO.compose.stickerButton = new El({ block: 'sticker-button' });
    PO.compose.importantButton = taggedEl('input-imortant-button');
    PO.compose.suggestsList = new El({ block: 'yamb-suggests-list' });
    PO.compose.suggestsList.suggest = new El({ block: 'yamb-suggest-list-item' });
    PO.compose.suggestsList.suggest1 = PO.compose.suggestsList.suggest.nthChild(1);
    PO.compose.suggestsList.suggest2 = PO.compose.suggestsList.suggest.nthChild(2);
    PO.compose.suggestsList.suggest3 = PO.compose.suggestsList.suggest.nthChild(3);
    PO.compose.suggestsList.lastSuggest = new El({ block: 'yamb-suggest-list-item' }).lastChild();
    PO.compose.file = new El({ block: 'yamb-compose-attach-button', elem: 'file' });
    PO.compose.input = taggedEl('message-editor');

    PO.conversation = new El({ block: 'yamb-conversation' });
    PO.lastMessage = new El({ block: 'message' }).lastChild();
    PO.lastMessage.message = new El({ block: 'yamb-message-row' });
    PO.lastMessage.message.messagesUser = new El({ block: 'yamb-message-user' });
    PO.lastMessage.message.messagesUser.role = new El({ block: 'yamb-message-user__role' });
    PO.lastMessage.system = new El({ block: 'yamb-message-system' }).lastChild();
    PO.lastMessage.system.text = new El({ block: 'text' });
    PO.lastMessage.system.text.lastLink = new El({ block: 'link' }).lastChild();
    PO.lastMessage.user = new El({ block: 'yamb-message-user' });
    PO.lastMessage.user.avatar = new El({ block: 'yamb-message-user', elem: 'avatar' });
    PO.lastMessage.userName = new El({ block: 'yamb-message-user', elem: 'name' });
    PO.lastMessage.messageText = new El({ block: 'text' });
    PO.lastMessage.balloonContent = new El({ block: 'yamb-message-content' });
    PO.lastMessage.balloonInfo = new El({ block: 'yamb-message-info' });
    PO.lastMessage.balloonInfo.time = new El({ block: 'yamb-message-balloon-info', elem: 'time' });
    PO.lastMessage.text = new El({ block: 'yamb-message-text' });
    PO.lastMessage.text.lastLink = new El({ block: 'link' }).lastChild();
    PO.lastMessage.urlPreview = new El({ block: 'yamb-url-preview' });
    PO.lastMessage.sticker = new El({ block: 'yamb-message-sticker' });
    PO.lastMessage.sticker.image = new El({ block: 'yamb-sticker', elem: 'image' });
    PO.lastMessage.file = new El({ block: 'yamb-message-file' });
    PO.lastMessage.file.control = new El({ block: 'yamb-message-file', elem: 'control' });
    PO.lastMessage.file.description = new El({ block: 'yamb-message-file', elem: 'description' });
    PO.lastMessage.file.actions = new El({ block: 'yamb-message-file', elem: 'actions' });
    PO.lastMessage.file.actions.link = new El({ block: 'link' });
    PO.lastMessage.image = new El({ block: 'yamb-message-image' });
    PO.lastMessage.image.picture = new El({ block: 'ui-picture' });
    PO.lastMessage.reply = new El({ block: 'yamb-message-reply' });

    PO.floatingDateVisible = new El({ block: 'yamb-floating-date__container_visible' });

    PO.chatListItemName = new El({ block: 'yamb-chat-list-item', elem: 'title' });
    PO.chatListItemStatus = new El({ block: 'yamb-chat-list-item', elem: 'status' });
    PO.chatListItemDate = new El({ block: 'yamb-entity-block-date' });

    PO.popupWrapper = new El({ block: 'ui-popup' });
    PO.popup = new El({ block: 'ui-popup', elem: 'content' });
    PO.popup.menu = new El({ block: 'ui-menu' });
    PO.popup.notMenu = PO.popup.not(new El({ block: 'ui-menu' }));
    PO.popup.menu.item = new El({ block: 'ui-menu-item' });
    PO.popup.menu.pin = taggedEl('chat-context-menu-pin');
    PO.popup.menu.unpin = taggedEl('chat-context-menu-unpin');
    PO.popup.menu.item1 = PO.popup.menu.item.nthChild(1);
    PO.popup.menu.item2 = PO.popup.menu.item.nthChild(2);
    PO.popup.menu.item3 = PO.popup.menu.item.nthChild(3);
    PO.popup.menu.item4 = PO.popup.menu.item.nthChild(4);
    PO.popup.menu.item5 = PO.popup.menu.item.nthChild(5);
    PO.popup.stickers = new El({ block: 'yamb-stickers' });
    PO.popup.stickers.loader = new El({ block: 'ui-loader' });
    PO.popup.stickers.firstSticker = new El({ block: 'yamb-sticker' }).firstChild();
    PO.popup.stickers.firstRow = new El({ block: 'yamb-stickers', elem: 'row' });
    PO.popup.stickers.firstRow.firstSticker = new El({ block: 'yamb-sticker' }).firstChild();
    PO.popup.stickers.tabs = new El({ block: 'ui-tabs' });
    PO.popup.stickers.tabs.tab = new El({ block: 'ui-tab' });
    PO.popup.stickers.tabs.firstTab = PO.popup.stickers.tabs.tab.nthChild(1);
    PO.popup.stickers.tabs.secondTab = PO.popup.stickers.tabs.tab.nthChild(2);
    PO.popup.stickers.tabs.sticker = new El({ block: 'yamb-sticker' });
    PO.popup.stickers.virtualList = new El({ block: 'ui-virtual-list' });
    PO.popup.stickers.virtualList.scroller = new El({ block: 'ui-scroller' });
    PO.popup.confirm = new El({ block: 'yamb-confirm' });
    PO.popup.confirm.button = new El({ block: 'ui-button' });
    PO.popup.confirm.cancelButton = new El({ block: 'ui-button' }).firstChild();
    PO.popup.confirm.submitButton = new El({ block: 'ui-button' }).lastChild();
    PO.confirmDialogOk = taggedEl('confirm-dialog-ok');
    PO.sticker = new El({ block: 'yamb-sticker' });

    PO.settingsButton = new El(createTestTagSelector('settings-btn'));
    PO.searchInput = new El({ block: 'yamb-search', elem: 'input' });

    PO.settingsModal = taggedEl('settings-modal');
    PO.settingsModal.blacklist = taggedEl('settings-blacklist');

    PO.blacklistModal = taggedEl('blacklist-modal');

    PO.modal = new El({ block: 'yamb-modal' });
    PO.modal.tumbler = new El({ block: 'ui-switch', elem: 'viewbox' });
    PO.modal.toolbar = new El({ block: 'yamb-modal-panel', elem: 'toolbar' });
    PO.modal.toolbar.closeButton = new El(createTestTagSelector('modal-close'));
    PO.modal.toolbar.menuButton = new El(createTestTagSelector('modal-menu'));
    PO.modal.toolbar.backButton = new El(createTestTagSelector('modal-back'));
    PO.modal.stickerPack = new El({ block: 'yamb-sticker-pack', elem: 'stickers' });
    PO.modal.buttonsRow = new El({ block: 'ui-buttons-row' });
    PO.modal.buttonsRow.cancelButton = new El({ block: 'ui-button' }).firstOfType();
    PO.modal.buttonsRow.lastButton = new El({ block: 'ui-button' }).lastOfType();
    PO.modal.addMembers = taggedEl('add-member');

    PO.modal.addMembers.usersList = new El({ block: 'yamb-users-pills-list' });
    PO.modal.addMembers.notMembersFirst = new El({ block: 'yamb-list-item-user' }).nthChild(2);
    PO.modal.addMembers.notMembersFirst.nameMember = taggedEl('list-item-user__name');
    PO.modal.addMembers.notMembersFirst.control = new El({ block: 'yamb-members-list-item', elem: 'control' });
    PO.modal.addMembers.joinButton = taggedEl('add-members-add-button');
    PO.modal.panel = new El({ block: 'yamb-modal-panel' });
    PO.modal.panel.lastNewMember = taggedEl('chat-members-list-subscriber').lastChild();
    PO.modal.panel.lastNewMember.title = taggedEl('chat-members-list-subscriber__name');

    PO.overlayShadow = taggedEl('overlay-shadow');

    PO.tumbler = new El({ block: 'ui-switch', elem: 'viewbox' });
    PO.input = new El({ block: 'ui-input' });

    // TODO: src/client/components/ForwardDialog/index.tsx - для класса ui-panel нет микса yamb-forward-dialog,
    // поэтому конструкция new El({ block: 'yamb-forward-dialog' }); работать не будет - такого класса нет
    PO.forwardDialog = new El({ block: 'yamb-modal-panel' });
    PO.forwardDialog.search = new El({ block: 'yamb-forward-dialog', elem: 'search' });
    PO.forwardDialog.listItem = taggedEl('forward-list-item');
    PO.forwardDialog.listItemName = taggedEl('forward-list-item__name');
    PO.forwardDialog.firstListItem = taggedEl('forward-list-item').firstOfType();

    PO.quote = new El({ block: 'yamb-quote' });
    PO.quoteDescription = new El({ block: 'yamb-quote', elem: 'description' });

    PO.infoMessage = new El({ block: 'ui-toast' });
    PO.infoMessage.content = new El({ block: 'ui-toast', elem: 'text' });

    PO.modalUploadFiles = taggedEl('modal-upload-files');
    PO.modalUploadFiles.upload = taggedEl('compose-submit');
    PO.modalUploadFiles.file = new El({ block: 'yamb-compose-attach-button', elem: 'file' });
    PO.modalUploadFiles.input = new El({ block: 'ui-textarea', elem: 'control' });
    PO.modalUploadFiles.item = taggedEl('file-upload-item');
    PO.modalUploadFiles.item.cancel = new El({ block: 'yamb-upload-files-item', elem: 'cancel-container' });
    PO.modalUploadFiles.item.cancelHidden = new El({
        block: 'yamb-upload-files-item',
        elem: 'cancel-container',
    }).mods({
        hidden: true,
    });

    // SelectMembers related selectors
    PO.selectMembers = new El({ block: 'yamb-select-members' });
    PO.selectMembers.pillsList = new El({ block: 'yamb-users-pills-list' });
    PO.selectMembers.pillsList.user1 = new El({ block: 'yamb-user-pill' }).firstChild();
    PO.selectMembers.input = new El({ block: 'uik-input' });
    PO.selectMembers.membersList = new El({ block: 'yamb-members-list' });
    PO.selectMembers.membersList.firstSelectableUser = new El('.yamb-list-item-user:not(.yamb-list-item-user_disabled)');

    PO.messagesActionPanel = new El();
    PO.messagesActionPanel.cancel = new El(createTestTagSelector('messages-action-panel-cancel'));
    PO.messagesActionPanel.forward = new El(createTestTagSelector('messages-action-panel-forward'));
    PO.messagesActionPanel.copy = new El(createTestTagSelector('messages-action-panel-copy'));
    PO.messagesActionPanel.count = new El(createTestTagSelector('messages-action-panel-count'));
    PO.messagesActionPanel.reply = new El(createTestTagSelector('messages-action-panel-reply'));
    PO.messagesActionPanel.pin = new El(createTestTagSelector('messages-action-panel-pin'));

    PO.chatActionActionPanel = new El();
    PO.chatActionActionPanel.join = taggedEl('chat-action-join');

    PO.chatActionSubscriber = new El({ block: 'yamb-chat-action-subscriber' });
    PO.chatActionSubscriber.shareChannel = new El({ block: 'ui-icon-button' }).firstChild();
    PO.chatActionSubscriber.disableNotification = new El({ block: 'ui-icon-button' }).lastChild();

    PO.chatWindow = taggedEl('chat-window');
    PO.sidebar = taggedEl('sidebar');
    PO.globalSearch = taggedEl('global-search');
    PO.globalSearchClear = taggedEl('search-clear');

    PO.searchItemUser = taggedEl('search-item-user');
    PO.searchItemMessage = taggedEl('search-item-message');
    PO.searchItemChat = taggedEl('search-item-chat');
    PO.usersPillsSelectedUser = taggedEl('users-pills-selected-user');
    PO.usersPillsSelectedUser.removeMemberButton = new El({ block: '.yamb-user-pill__delete' });

    PO.listItemUser = taggedEl('list-item-user');

    PO.firstListItemUser = taggedEl('list-item-user').firstChild();
    PO.chatWindow = taggedEl('chat-window');
    PO.messageEditor = taggedEl('message-editor');
    PO.editableAvatar = taggedEl('editable-avatar');
    PO.editableAvatarInput = taggedEl('editable-avatar-input');
    PO.imageCrop = taggedEl('image-crop');
    PO.imageCropSave = taggedEl('image-crop-save');
    PO.selectMembersInput = taggedEl('select-members-input');

    // Messages
    PO.message = taggedEl('message');
    PO.lastMessage2 = taggedEl('message-text').lastChild();
    PO.messageText = taggedEl('message-text');
    PO.messageText.text = new El({ block: 'text' });
    PO.messageFile = taggedEl('message-file');
    PO.messageFile.statusProgress = taggedEl('message-file-status-progress');
    PO.messageFile.statusProgress.button = taggedEl('message-file-button');
    PO.messageImage = taggedEl('message-image');
    PO.messageImage.statusProgress = taggedEl('message-image-status-progress');
    PO.messageSticker = taggedEl('message-sticker');
    PO.messageCard = taggedEl('message-card');
    PO.messageGallery = taggedEl('message-gallery');
    PO.messageVoice = taggedEl('message-voice');
    PO.messageUrl = taggedEl('message-url');
    PO.messageReply = taggedEl('message-reply');
    PO.messageForward = taggedEl('message-forward');
    PO.messageUrlPreviewVideo = taggedEl('message-url-preview-video');

    PO.messageMenuDelete = taggedEl('message-menu-delete');
    PO.messageMenuReply = taggedEl('message-menu-reply');
    PO.messageMenuPin = taggedEl('message-menu-pin');
    PO.messageMenuSelect = taggedEl('message-menu-select');
    PO.messageMenuDeselect = taggedEl('message-menu-deselect');
    PO.messageMenuForward = taggedEl('message-menu-forward');
    PO.messageMenuEdit = taggedEl('message-menu-edit');

    PO.pollViewResultsTitle = new El({ block: 'yamb-poll-view-results__title' });

    return PO;
};
