import assert from 'assert';
const {
    chatOpenComponent,
    clickTo,
    yaAssertModal,
    yaCheckClientErrors,
    yaCheckCSPViolations,
    yaCheckCustomErrors,
    yaCheckMetrics,
    yaClick,
    yaCloseModal,
    yaDisableAnimation,
    yaExpectOnPage,
    yaGetBaobabTree,
    yaGetContainsSelector,
    yaGetCounter,
    yaGetCounters,
    yaHideDeviceKeyboard,
    yaOpenContactCardFromTitle,
    yaOpenLastMessageContextMenu,
    yaOpenMessageContextMenu,
    yaOpenMessenger,
    yaOpenMessengerAndGroupChat,
    yaScrollInfiniteList,
    yaScrollIntoView,
    yaWaitForNewFileMessage,
    yaWaitForNewForwardMessage,
    yaWaitForNewGalleryMessage,
    yaWaitForNewImageMessage,
    yaWaitForNewMessage,
    yaWaitForNewReplyMessage,
    yaWaitForNewStickerMessage,
    yaWaitForNewSystemMessage,
    yaWaitForNewTextMessage,
    yaWaitForVisibleWithContent,
} = require('../hermione/commands');

declare global {
    namespace WebdriverIO {
        interface Browser {
            authOnRecord,
            chatOpenComponent: typeof chatOpenComponent,
            click,
            clickTo: typeof clickTo,
            yaAssertModal: typeof yaAssertModal,
            yaCheckClientErrors: typeof yaCheckClientErrors,
            yaCheckCSPViolations: typeof yaCheckCSPViolations,
            yaCheckCustomErrors: typeof yaCheckCustomErrors,
            yaCheckMetrics: typeof yaCheckMetrics,
            yaClick: typeof yaClick,
            yaCloseModal: typeof yaCloseModal,
            yaDisableAnimation: typeof yaDisableAnimation,
            yaExpectOnPage: typeof yaExpectOnPage,
            yaGetBaobabTree: typeof yaGetBaobabTree,
            yaGetContainsSelector: typeof yaGetContainsSelector,
            yaGetCounter: typeof yaGetCounter,
            yaGetCounters: typeof yaGetCounters,
            yaHideDeviceKeyboard: typeof yaHideDeviceKeyboard,
            yaOpenContactCardFromTitle: typeof yaOpenContactCardFromTitle,
            yaOpenLastMessageContextMenu: typeof yaOpenLastMessageContextMenu,
            yaOpenMessageContextMenu: typeof yaOpenMessageContextMenu,
            yaOpenMessenger: typeof yaOpenMessenger,
            yaOpenMessengerAndGroupChat: typeof yaOpenMessengerAndGroupChat,
            yaScrollInfiniteList: typeof yaScrollInfiniteList,
            yaScrollIntoView: typeof yaScrollIntoView,
            yaWaitForNewFileMessage: typeof yaWaitForNewFileMessage,
            yaWaitForNewForwardMessage: typeof yaWaitForNewForwardMessage,
            yaWaitForNewGalleryMessage: typeof yaWaitForNewGalleryMessage,
            yaWaitForNewImageMessage: typeof yaWaitForNewImageMessage,
            yaWaitForNewMessage: typeof yaWaitForNewMessage,
            yaWaitForNewReplyMessage: typeof yaWaitForNewReplyMessage,
            yaWaitForNewStickerMessage: typeof yaWaitForNewStickerMessage,
            yaWaitForNewSystemMessage: typeof yaWaitForNewSystemMessage,
            yaWaitForNewTextMessage: typeof yaWaitForNewTextMessage,
            yaWaitForVisibleWithContent: typeof yaWaitForVisibleWithContent,
        }

        interface ItDefinitionCallbackCtx extends Hermione.TestDefinitionCallbackCtx {
            currentTest,
        }
    }

    declare interface IPO {
        chat: {
            loader,
            joinButton,
            header: {
                title,
                menuButton,
                backButton,
                searchButton,
            } & Function,
            search: {
                input,
                prev,
                next,
            } & Function,
            toolbar: {
                title,
            } & Function,
        } & Function,
        chatHeader,
        chatInfo: {
            assetsBrowser,
            title,
            description,
            members,
            blockUser,
            unblockUser,
            notifications,
            search,
            report,
            leave,
            inviteLink: {
                link,
            } & Function,
            settings,
        } & Function,
        chatListItem,
        chatListItemBlock,
        chatListItemBlock1,
        chatListItemChannel,
        chatListItemName,
        chatListItemPrivate,
        chatListWriteBtn,
        chatSettings: {
            publicSwitchOn,
            publicSwitchOff,
            save,
        } & Function,
        chatMember,
        chatMembersList: {
            memberAdmin: {
                menu,
            } & Function,
            member: {
                menu,
            } & Function,
        } & Function,
        chatMemberMenuRemove,
        createChatWizard,
        createGroupChat,
        createGroupChatBtn,
        createGroupChatName,
        createGroupChatDescription,
        createGroupChatAddMembers,
        createChatSelectMembers,
        createChatCreateBtn,
        pinnedMessage: {
            remove,
        } & Function,
        sidebar,
        sidebarChatList,
        chatListItem1,
        chatListItem2,
        separator,
        chatListItemNotSticky,
        mediaItem: {
            image,
            imageContainer,
        } & Function,
        lightbox: {
            left,
            right,
            loader: {
                spinner,
            } & Function,
            toolbar: {
                summary,
                download,
            } & Function,
            close,
        } & Function,
        compose: {
            container: {
                input,
            } & Function,
            quote: {
                close,
            } & Function,
            sendMessageButtonDisabled,
            sendMessageButtonEnabled,
            stickerButton,
            importantButton,
            suggestsList: {
                suggest,
                suggest1,
                suggest2,
                suggest3,
                lastSuggest,
            } & Function,
            file,
            input,
        } & Function,
        conversation,
        lastMessage: {
            message,
            system: {
                text: {
                    lastLink,
                } & Function,
            } & Function,
            user: {
                avatar,
            } & Function,
            userName,
            messageText,
            balloonContent,
            balloonInfo: {
                time,
            } & Function,
            text: {
                lastLink,
            } & Function,
            urlPreview,
            sticker: {
                image,
            } & Function,
            file: {
                control,
                description,
                actions: {
                    link,
                } & Function,
            } & Function,
            image: {
                picture,
            } & Function,
            reply,
        } & Function,
        floatingDateVisible,
        chatListItemStatus,
        chatListItemDate,
        chatActionSubscriber,
        popupWrapper,
        popup: {
            menu: {
                item,
                pin,
                unpin,
                item1,
                item2,
                item3,
                item4,
                item5,
            } & Function,
            notMenu,
            stickers: {
                loader,
                firstSticker,
                firstRow: {
                    firstSticker,
                } & Function,
                tabs: {
                    tab,
                    firstTab,
                    secondTab,
                    sticker,
                } & Function,
                virtualList: {
                    scroller,
                } & Function,
            } & Function,
            confirm: {
                button,
                cancelButton,
                submitButton,
            } & Function,
        } & Function,
        confirmDialogOk,
        sticker,
        settingsButton,
        searchInput,
        settingsModal,
        blacklistModal,
        modal: {
            tumbler,
            toolbar: {
                closeButton,
                menuButton,
                backButton,
            } & Function,
            stickerPack,
            buttonsRow: {
                cancelButton,
                lastButton,
            } & Function,
            addMembers: {
                usersList,
                notMembersFirst: {
                    nameMember,
                    control,
                } & Function,
                joinButton,
            } & Function,
            panel: {
                lastNewMember: {
                    title,
                } & Function,
            } & Function,
        } & Function,
        overlayShadow,
        tumbler,
        input,
        forwardDialog: {
            search,
            listItem,
            listItemName,
            firstListItem,
        } & Function,
        quote,
        quoteDescription,
        infoMessage: {
            content,
        } & Function,
        modalUploadFiles: {
            upload,
            file,
            input,
            item: {
                cancel,
                cancelHidden,
            } & Function,
        } & Function,
        selectMembers: {
            pillsList: {
                user1,
            } & Function,
            input,
            membersList: {
                firstSelectableUser,
            } & Function,
        } & Function,
        messagesActionPanel: {
            cancel,
            forward,
            copy,
            count,
            reply,
            pin,
        } & Function,
        chatActionActionPanel: {
            join,
        } & Function,
        globalSearch,
        searchItemUser,
        listItemUser,
        firstListItemUser,
        chatWindow,
        messagesUser,
        messageEditor,
        editableAvatar,
        editableAvatarInput,
        imageCrop,
        imageCropSave,
        selectMembersInput,
        message,
        lastMessage2,
        messageText: {
            text,
        } & Function,
        messageFile: {
            statusProgress: {
                button,
            } & Function,
        } & Function,
        messageImage: {
            statusProgress,
        } & Function,
        messageSticker,
        messageCard,
        messageGallery,
        messageVoice,
        messageUrl,
        messageReply,
        messageForward,
        messageUrlPreviewVideo,
        messageMenuDelete,
        messageMenuReply,
        removeMemberConfirm,
        scrollToBottom,
        globalSearchClear,
        searchItemMessage,
        searchItemChat,
        usersPillsSelectedUser: {
            removeMemberButton,
        } & Function,
        messageMenuForward,
        messageMenuEdit,
        messageMenuSelect,
        messageMenuPin,
        pollViewResultsTitle,
    }

    interface IPOUtils {
        createTestTagSelector(testTag: string, dataTag?: string): string;
    }

    declare const PO: IPO;
    declare const POUtils: IPOUtils;
    declare const assert: typeof assert;
}
