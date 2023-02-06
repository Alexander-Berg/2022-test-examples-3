package ru.yandex.search.messenger.proxy;

import java.io.IOException;

import ru.yandex.test.util.TestBase;

public class MoxyTestBase extends TestBase {
    protected static final String URI
        = "/?kps=0&key=/disk/*&visible=1&only=id&";

    public static String userDoc(
        final int id,
        final String displayName)
    {
        return userDoc(id, displayName, null);
    }

    public static String userDoc(
        final int id,
        final String displayName,
        final String nickName)
    {
        return userDoc(id, displayName, nickName, null);
    }

    // CSOFF: ParameterNumber
    public static String userDoc(
        final int id,
        final String displayName,
        final String nickName,
        final String position)
    {
        return userDoc(id, displayName, nickName, position, null);
    }

    public static String userDoc(
        final int id,
        final String displayName,
        final String nickName,
        final String position,
        final String department)
    {
        return userDoc(id, displayName, nickName, position, department, null);
    }

    public static String userDoc(
        final int id,
        final String displayName,
        final String nickName,
        final String position,
        final String department,
        final String website)
    {
        return userDoc(id, displayName,nickName, position, department, website, null);
    }

    public static String userDoc(
        final int id,
        final String displayName,
        final String nickName,
        final String position,
        final String department,
        final String website,
        final String userChats)
    {
        StringBuilder sb =
            new StringBuilder();
        sb.append("\"i");
        sb.append("d\":\"");
        sb.append(id);
        sb.append("@10");
        sb.append("\",\"user_id\":\"");
        sb.append(id);
        if (displayName != null) {
            sb.append("\",\"user_display_name\":\"");
            sb.append(displayName);
        }
        if (nickName != null) {
            sb.append("\",\"user_nickname\":\"");
            sb.append(nickName);
        }
        if (position != null) {
            sb.append("\",\"user_position\":\"");
            sb.append(position);
        }
        if (department != null) {
            sb.append("\",\"user_department_name\":\"");
            sb.append(department);
        }
        if (website != null) {
            sb.append("\",\"user_website\":\"");
            sb.append(website);
        }
        if (userChats != null) {
            sb.append("\",\"user_chats\":\"");
            sb.append(userChats);
        }
        sb.append('"');
        return new String(sb);
    }

    public static String messageDoc(
        final String id,
        final String chatId,
        final String text)
    {
        return "\"id\": \"" + id + "\",\"message_id\": \"" + id
            + "\",\"message_chat_id_hash\": \"1275\",\n"
            + "\"type\": \"text_message\",\n"
            + "\"message_data\": \"\","
            + "\"message_chat_id\": \"" + chatId
            + "\",\"message_timestamp\": \"1564656474387013\",\n"
            + "\"message_last_edit_timestamp\": \"0\",\n"
            + "\"message_seq_no\": \"13\",\n"
            + "\"message_text\":\"" + text
            + "\",\"message_from_display_name\": \"Владислав Таболин\",\n"
            + "\"message_from_guid\": "
            + "\"7a87a9cb-17ce-4138-8928-de46c68ff919\",\n"
            + "\"message_from_phone_id\": \"\"";
    }

    // CSON: ParameterNumber

    public static String chatDoc(
        final int id,
        final String name,
        final String description)
    {
        StringBuilder sb =
            new StringBuilder();
        sb.append('"');
        sb.append("id\":\"");
        sb.append(id);
        sb.append("\",\"chat_id\":\"");
        sb.append(id);
        if (name != null) {
            sb.append("\",\"chat_name\":\"");
            sb.append(name);
        }
        if (description != null) {
            sb.append("\",\"chat_description\":\"");
            sb.append(description);
        }
        sb.append('"');
        return new String(sb);
    }
    // CSON: ParameterNumber

    public static ChatBuilder chatBuilder() {
        return new ChatBuilder();
    }

    // CSOFF: MagicNumber
    protected static void prepareIndex(final ProxyCluster cluster)
        throws IOException
    {
        cluster.backend().add(
            userDoc(1, "Иван Петров", "ivpetr12", "Директор по борьбе", "Стафф")
        );
    }

    static class ChatBuilder {
        private String chatId;
        private String chatName;
        private String chatDescription;
        private String chatEntityId;
        private int chatServiceId;
        private int chatMessageCount = -1;
        private int chatTotalMessageCount = -1;
        private int chatHiddenMessageCount = -1;
        private int chatTotalIndexedMessageCount = -1;
        private String chatParentUrl = null;

        ChatBuilder() {
        }

        public ChatBuilder chatId(final String chatId) {
            this.chatId = chatId;
            return this;
        }

        public ChatBuilder chatName(final String chatName) {
            this.chatName = chatName;
            return this;
        }

        public ChatBuilder chatDescription(final String chatDescription) {
            this.chatDescription = chatDescription;
            return this;
        }

        public ChatBuilder chatParentUrl(final String url) {
            this.chatParentUrl = url;
            return this;
        }

        public ChatBuilder chatEntityId(final String chatEntityId) {
            this.chatEntityId = chatEntityId;
            return this;
        }

        public ChatBuilder chatServiceId(final int chatServiceId) {
            this.chatServiceId = chatServiceId;
            return this;
        }

        public ChatBuilder chatMessageCount(final int chatMessageCount) {
            this.chatMessageCount = chatMessageCount;
            return this;
        }

        public ChatBuilder chatTotalMessageCount(
            final int chatTotalMessageCount)
        {
            this.chatTotalMessageCount = chatTotalMessageCount;
            return this;
        }

        public ChatBuilder chatHiddenMessageCount(
            final int chatHiddenMessageCount)
        {
            this.chatHiddenMessageCount = chatHiddenMessageCount;
            return this;
        }

        public ChatBuilder chatTotalIndexedMessageCount(
            final int chatTotalIndexedMessageCount)
        {
            this.chatTotalIndexedMessageCount = chatTotalIndexedMessageCount;
            return this;
        }

        public String build() {
            StringBuilder sb = new StringBuilder();
            sb.append("\"id\":\"chat_");
            sb.append(chatId);
            sb.append("\",\"chat_id\":\"");
            sb.append(chatId);
            sb.append("\",\"chat_name\":\"");
            sb.append(chatName);
            sb.append("\",\"chat_description\":\"");
            sb.append(chatDescription);
            if (chatEntityId != null) {
                sb.append("\",\"chat_entity_id\":\"");
                sb.append(chatEntityId);
            }
            sb.append("\",\"chat_subservice\":\"");
            sb.append(chatServiceId);
            if (chatParentUrl != null) {
                sb.append("\",\"chat_parent_url\":\"");
                sb.append(chatParentUrl);
            }

            if (chatMessageCount != -1) {
                sb.append("\",\"chat_message_count\":\"");
                sb.append(chatMessageCount);
            }
            if (chatTotalMessageCount != -1) {
                sb.append("\",\"chat_total_message_count\":\"");
                sb.append(chatTotalMessageCount);
            }
            if (chatHiddenMessageCount != -1) {
                sb.append("\",\"chat_hidden_message_count\":\"");
                sb.append(chatHiddenMessageCount);
            }
            if (chatTotalIndexedMessageCount != -1) {
                sb.append("\",\"chat_total_indexed_message_count\":\"");
                sb.append(chatTotalIndexedMessageCount);
            }

            sb.append("\"");
            return new String(sb);
        }
    }
}

