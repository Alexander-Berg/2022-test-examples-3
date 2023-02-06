package ru.yandex.autotests.innerpochta.touch.data;

/**
 * @author oleshko
 */
public class Scrips {

    public static final String PTR_SCRIPT = "document.querySelector('.ptr').style.transform = 'translateY(96px)';" +
        "document.querySelector('.ptr-scrollable').style.transform = 'translateY(96px)'";

    public static final String SWIPE_SCRIPT = "document.querySelector('.messagesMessage-inner')" +
        ".style.transform = 'translate3d(-160px, 0px, 0px)'";

    public static final String SCRIPT_FOR_SCROLLDOWN_THREAD =
        "document.querySelector('.messagesView').scrollTop = 1500";

    public static final String SCRIPT_FOR_SCROLL_ATTACHMENTS =
        "document.querySelector('.messages-message__attachments').scrollLeft = 80";

    public static final String SCRIPT_FOR_SCROLL_TWO_ATTACHMENTS_LEFT =
        "document.querySelector('.messages-message__attachments').scrollLeft = 210";

    public static final String SCRIPT_FOR_SCROLLDOWN_CHAT =
        "document.querySelector('.chat-scroll').scrollTop = 1500";
}
