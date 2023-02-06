package ru.yandex.autotests.innerpochta.tests.pq;

import ch.ethz.ssh2.Connection;
import com.jcabi.aspects.Cacheable;
import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.ListOutcome;
import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.yandex.autotests.innerpochta.utils.SSHCommands;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * User: alex89
 * Date: 23.04.15
 * http://www.yegor256.com/2014/08/18/fluent-jdbc-decorator.html
 * https://github.com/jcabi/jcabi-email
 * http://jdbc.jcabi.com/example-select.html
 * http://stackoverflow.com/questions/25641047/org-postgresql-util-psqlexception-fatal-no-pg-hba-conf-entry-for-host
 */

public class PqData {
    private static final Logger LOG = LogManager.getLogger(PqData.class);
    private static final String MAIL_MESSAGES_TABLE_SELECT_CMD = "SELECT st_id,size,attributes,ARRAY(SELECT UNNEST(attaches) ORDER BY 1),subject," +
            "firstline,hdr_date,hdr_message_id,recipients,ARRAY(SELECT UNNEST(mime) ORDER BY 1)  FROM mail.messages WHERE uid =%s and mid=%s";
    private static final String MAIL_BOX_TABLE_SELECT_CMD = "SELECT fid,tid,seen,recent,deleted,received_date,lids " +
            " FROM mail.box WHERE uid =%s and mid=%s";
    private static final String MAIL_LABELS_TABLE_SELECT_CMD = "SELECT lid,name,type,color,created,message_count " +
            "FROM mail.labels WHERE uid =%s and name like '%s'";
    private static final String MAIL_MESSAGE_REFERENCES_TABLE_SELECT_CMD = "SELECT value,type " +
            "FROM  mail.message_references WHERE uid =%s and mid=%s";
    private static final String MAILISH_MESSAGES_TABLE_SELECT_CMD = "SELECT fid,imap_id,imap_time,errors " +
            "FROM  mailish.messages WHERE uid =%s and mid=%s";
    private static final String XDB_USER = "mxback";
    private static final String SHARPEI_URL = "sharpei.mail.yandex.net";
    public static final Pattern MID_LOG_STRING_PATTERN = compile(": mid=([0-9]+),");
    public static final Pattern TID_LOG_STRING_PATTERN = compile(" tid=([0-9]+)");
    public static final Pattern STID_LOG_STRING_PATTERN = compile(" mulcaid=([0-9.:a-zE]+),?");
    public static final Pattern HDR_DATE_LOG_STRING_PATTERN = compile("hdr_date=([\\+0-9. :-]+);");
    public static final Pattern SIZE_LOG_STRING_PATTERN = compile("size=([0-9]+)");
    private static final Pattern XDB_HOST_PATTERN = compile("\"host\":\"([A-Za-z-_.0-9]+)\"");
    private static final String JDBC_URL =
            "jdbc:postgresql://%s:%s/maildb?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory&lazyInit=true";
    public static final String NO_ATTACH = "{}";
    public static final String REMIND_SYS_LABEL = "remindme_threadabout:mark";
    public static final org.hamcrest.Matcher<String> SYSTEM_TYPE = equalTo("system");
    public static final String SENT_FOLDER_FID = "4";
    public static final String DRAFT_FOLDER_FID = "6";
    public static final String DEFAULT_FOLDER_FID = "1";
    public static final String SPAM_FOLDER_FID = "2";
    public static final String FALSE = "f";
    public static final String TRUE = "t";
    public static final Pattern KEY_PATTERN = compile(XDB_USER + ":([A-Za-z0-9]+)");


    public static String extractedParamFromLogByPattern(Pattern pattern, String sessionLog) {
        Matcher logParamMatcher = pattern.matcher(sessionLog);
        if (logParamMatcher.find()) {
            return logParamMatcher.group(1);
        }
        return "";
    }

    public static String getXdbPwd(Connection conn) {
        String key;
        try {
            key = SSHCommands.executeCommAndResturnResultAsString(conn, "cat /app/secrets/pgpass", LOG);
        } catch (IOException e) {
            throw new RuntimeException("Не нашли ключ");
        }
        String pwd = extractedParamFromLogByPattern(KEY_PATTERN, key).trim();
        if (pwd.length() != 20) {
            throw new RuntimeException("Bad password length");
        }
        return pwd;
    }

    @Cacheable(forever = true)
    public static String getXdbHostBySharpei(Connection conn, String uid) {
        String sharpeiResponse;
        try {
            sharpeiResponse = SSHCommands.executeCommAndResturnResultAsString(conn,
                    format("curl -s \"%s/conninfo?uid=%s&mode=master\"", SHARPEI_URL, uid), LOG);
        } catch (IOException e) {
            throw new RuntimeException("Не смогли узнать XDB-сервер при запросе у шарпея через сервер");
        }
        Matcher logParamMatcher = XDB_HOST_PATTERN.matcher(sharpeiResponse);
        if (!logParamMatcher.find()) {
            throw new RuntimeException("Unable to get Xdb host from Sharpei");
        }
        return logParamMatcher.group(1);
    }

    @Cacheable(forever = true)
    public static DataSource source(Connection conn, SshLocalPortForwardingRule fwd) {
        return source(conn, fwd.local().getHost(), fwd.local().getPort());
    }

    // @Cacheable(forever = true)
    public static DataSource source(Connection conn, String localHost, int localPort) {
        BoneCPDataSource src = new BoneCPDataSource();
        src.setDriverClass("org.postgresql.Driver");
        src.setJdbcUrl(format(JDBC_URL, localHost, localPort));
        src.setUser(XDB_USER);
        src.setPassword(getXdbPwd(conn));
        return src;
    }

    // st_id                          | size | attributes | attaches |           subject           |
    //        firstline            |        hdr _date        |
    //         hdr_message_id                       | recipients  | extra_data | pop_uidl
    public static class MailMessagesPqTable {
        private String stid;
        private String size;
        private String attributes;
        private String attaches;
        private String subject;
        private String firstline;
        private String hdrDate;
        private String hdrMsgId;
        private String recipients;
        private String mime;


        public MailMessagesPqTable(String stid, String size, String attributes, String attaches, String subject,
                                   String firstline, String hdrDate, String hdrMsgId, String recipients, String mime) {
            this.stid = stid;
            this.size = size;
            this.attributes = attributes;
            this.attaches = attaches;
            this.subject = subject;
            this.firstline = firstline;
            this.hdrDate = hdrDate;
            this.hdrMsgId = hdrMsgId;
            this.recipients = recipients;
            this.mime = mime;
        }

        public static MailMessagesPqTable mailMessagesTableInfoFromPq(Connection conn, SshLocalPortForwardingRule fwd,
                                                                      String uid, String mid) {
            System.out.println("mid=" + mid);
            DataSource dataSource = source(conn, fwd.local().getHost(), fwd.local().getPort());
            return mailMessagesTableInfoFromPq(dataSource, uid, mid);
        }

        public static MailMessagesPqTable mailMessagesTableInfoFromPq(Connection conn, String uid, String mid) {
            DataSource dataSource = source(conn, getXdbHostBySharpei(conn, uid), 6432);
            return mailMessagesTableInfoFromPq(dataSource, uid, mid);
        }

        public static MailMessagesPqTable mailMessagesTableInfoFromPq(Connection conn, String host, int port, String uid, String mid) {
            DataSource dataSource = source(conn, host, port);
            return mailMessagesTableInfoFromPq(dataSource, uid, mid);
        }

        public static MailMessagesPqTable mailMessagesTableInfoFromPq(DataSource dataSource, String uid, String mid) {
            try {
                Thread.sleep(1000);//попытка компенсировать "тормоза" тестинга
                List<MailMessagesPqTable> select = new JdbcSession(dataSource)
                        .sql(format(MAIL_MESSAGES_TABLE_SELECT_CMD, uid, mid))
                        .select(new ListOutcome<>(new ListOutcome.Mapping<MailMessagesPqTable>() {
                            @Override
                            public MailMessagesPqTable map(ResultSet rset) throws SQLException {
                                return new MailMessagesPqTable(rset.getString(1), rset.getString(2),
                                        rset.getString(3), rset.getString(4), rset.getString(5), rset.getString(6),
                                        rset.getString(7), rset.getString(8), rset.getString(9), rset.getString(10));
                            }
                        }));
                return select.get(0);
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String getStid() {
            return stid;
        }

        public String getAttaches() {
            return attaches;
        }

        public String getSubject() {
            return subject;
        }

        public String getFirstline() {
            return firstline;
        }

        public String getSize() {
            return size;
        }

        public String getAttributes() {
            return attributes;
        }

        public String getHdrDate() {
            return hdrDate;
        }

        public String getHdrMsgId() {
            return hdrMsgId;
        }

        public String getRecipients() {
            return recipients;
        }

        public String getMime() {
            return mime;
        }

    }


    //
    //  uid |mid | fid | tid | imap_id | revision | chain | seen | recent | deleted | receivedDate | lids
    public static class MailBoxPqTable {
        private String uid;
        private String fid;
        private String tid;
        private String seen;
        private String recent;
        private String deleted;
        private String receivedDate;
        private String lids;
        private Connection conn;

        public MailBoxPqTable(String uid, String fid, String tid, String seen, String recent,
                              String deleted, String receivedDate, String lids) {
            this.uid = uid;
            this.fid = fid;
            this.tid = tid;
            this.seen = seen;
            this.recent = recent;
            this.deleted = deleted;
            this.receivedDate = receivedDate;
            this.lids = lids;
        }

        public static MailBoxPqTable mailBoxTableInfoFromPq(Connection conn, final SshLocalPortForwardingRule fwd,
                                                            final String uid, String mid) {
            DataSource dataSource = source(conn, fwd.local().getHost(), fwd.local().getPort());
            return mailBoxTableInfoFromPq(dataSource, uid, mid).setConnection(conn);
        }

        public static MailBoxPqTable mailBoxTableInfoFromPq(Connection conn, String uid, String mid) {
            DataSource dataSource = source(conn, getXdbHostBySharpei(conn, uid), 6432);
            return mailBoxTableInfoFromPq(dataSource, uid, mid).setConnection(conn);
        }

        public static MailBoxPqTable mailBoxTableInfoFromPq(DataSource dataSource, String uid, String mid) {
            try {
                Thread.sleep(4000);//попытка компенсировать "тормоза" тестинга
                List<MailBoxPqTable> select = new JdbcSession(dataSource)
                        .sql(format(MAIL_BOX_TABLE_SELECT_CMD, uid, mid))
                        .select(new ListOutcome<>(new ListOutcome.Mapping<MailBoxPqTable>() {
                            @Override
                            public MailBoxPqTable map(ResultSet rset) throws SQLException {
                                return new MailBoxPqTable(uid, rset.getString(1), rset.getString(2),
                                        rset.getString(3), rset.getString(4), rset.getString(5), rset.getString(6),
                                        rset.getString(7));
                            }
                        }));
                return select.get(0);
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String getUid() {
            return uid;
        }

        public String getFid() {
            return fid;
        }

        public String getTid() {
            return tid;
        }

        public String getSeen() {
            return seen;
        }

        public String getRecent() {
            return recent;
        }

        public String getDeleted() {
            return deleted;
        }

        public String getReceivedDate() {
            return receivedDate;
        }

        public String getLids() {
            return lids;
        }

        public MailBoxPqTable setConnection(Connection conn) {
            this.conn = conn;
            return this;
        }

        public Connection getConnection() { return this.conn; }

        public List<String> getLidsList() {
            String lidsInformationWithoutBracket = lids.substring(1, lids.length() - 1);
            return asList(lidsInformationWithoutBracket.split(","));
        }
    }

    //    uid    | lid | revision |   name   |  type  |  color   |        created            | message_count
    public static class MailLabelsPqTable {
        private String lid;
        private String name;
        private String type;
        private String color;
        private String created;
        private String msgCount;


        public MailLabelsPqTable(String lid, String name, String type, String color, String created, String msgCount) {
            this.lid = lid;
            this.name = name;
            this.type = type;
            this.color = color;
            this.created = created;
            this.msgCount = msgCount;
        }

        public static List<MailLabelsPqTable> mailLabelsTableInfoFromPq(Connection conn, SshLocalPortForwardingRule fwd,
                                                                        String uid, String labelName) {
            DataSource dataSource = source(conn, fwd.local().getHost(), fwd.local().getPort());
            return mailLabelsTableInfoFromPq(dataSource, uid, labelName);
        }

        public static List<MailLabelsPqTable> mailLabelsTableInfoFromPq(Connection conn, String uid, String labelName) {
            DataSource dataSource = source(conn, getXdbHostBySharpei(conn, uid), 6432);
            return mailLabelsTableInfoFromPq(dataSource, uid, labelName);
        }

        public static List<MailLabelsPqTable> mailLabelsTableInfoFromPq(DataSource dataSource,
                                                                        String uid, String labelName) {
            try {
                Thread.sleep(1000);//попытка компенсировать "тормоза" тестинга
                return new JdbcSession(dataSource)
                        .sql(format(MAIL_LABELS_TABLE_SELECT_CMD, uid, labelName))
                        .select(new ListOutcome<>(new ListOutcome.Mapping<MailLabelsPqTable>() {
                            @Override
                            public MailLabelsPqTable map(ResultSet rset) throws SQLException {
                                return new MailLabelsPqTable(rset.getString(1), rset.getString(2),
                                        rset.getString(3), rset.getString(4), rset.getString(5), rset.getString(6));
                            }
                        }));

            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String getLid() {
            return lid;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getColor() {
            return color;
        }

        public String getCreated() {
            return created;
        }

        public String getMsgCount() {
            return msgCount;
        }

        @Override
        public String toString() {
            return format("MailLabelsPqTable{lid='%s', name='%s', type='%s', color='%s', created='%s', msgCount='%s'}",
                    lid, name, type, color, created, msgCount);
        }
    }

    //       uid    |        mid         |        value         |    type
    public static class MailMessageReferencesPqTable {
        private String value;
        private String type;

        public MailMessageReferencesPqTable(String value, String type) {
            this.value = value;
            this.type = type;
        }

        public static List<MailMessageReferencesPqTable> mailMessageReferencesTableInfoFromPq(
                Connection conn, SshLocalPortForwardingRule fwd, String uid, String mid) {
            DataSource dataSource = source(conn, fwd.local().getHost(), fwd.local().getPort());
            return mailMessageReferencesTableInfoFromPq(dataSource, uid, mid);
        }

        public static List<MailMessageReferencesPqTable> mailMessageReferencesTableInfoFromPq(Connection conn, String uid, String mid) {
            DataSource dataSource = source(conn, getXdbHostBySharpei(conn, uid), 6432);
            return mailMessageReferencesTableInfoFromPq(dataSource, uid, mid);
        }

        public static List<MailMessageReferencesPqTable> mailMessageReferencesTableInfoFromPq(
                DataSource dataSource, String uid, String mid) {
            try {
                Thread.sleep(1000);//попытка компенсировать "тормоза" тестинга
                return new JdbcSession(dataSource)
                        .sql(format(MAIL_MESSAGE_REFERENCES_TABLE_SELECT_CMD, uid, mid))
                        .select(new ListOutcome<>(new ListOutcome.Mapping<MailMessageReferencesPqTable>() {
                            @Override
                            public MailMessageReferencesPqTable map(ResultSet rset) throws SQLException {
                                return new MailMessageReferencesPqTable(rset.getString(1), rset.getString(2));
                            }
                        }));

            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String getValue() {
            return value;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return format("{value='%s', type='%s'}", value, type);
        }
    }

    //uid | fid | imap_id | imap_time | mid | errors
    public static class MailishMessagesPqTable {
        private String fid;
        private String imapId;
        private String imapTime;
        private String errors;

        public MailishMessagesPqTable(String fid, String imapId, String imapTime, String errors) {
            this.fid = fid;
            this.imapId = imapId;
            this.imapTime = imapTime;
            this.errors = errors;
        }

        public static MailishMessagesPqTable mailishMessagesTableInfoFromPq(Connection conn, SshLocalPortForwardingRule fwd,
                                                                            String uid, String mid) {
            DataSource dataSource = source(conn, fwd.local().getHost(), fwd.local().getPort());
            return mailishMessagesTableInfoFromPq(dataSource, uid, mid);
        }

        public static MailishMessagesPqTable mailishMessagesTableInfoFromPq(Connection conn, String uid, String mid) {
            DataSource dataSource = source(conn, getXdbHostBySharpei(conn, uid), 6432);
            return mailishMessagesTableInfoFromPq(dataSource, uid, mid);
        }

        public static MailishMessagesPqTable mailishMessagesTableInfoFromPq(DataSource dataSource,
                                                                            String uid, String mid) {
            try {
                Thread.sleep(1000);//попытка компенсировать "тормоза" тестинга
                List<MailishMessagesPqTable> select = new JdbcSession(dataSource)
                        .sql(format(MAILISH_MESSAGES_TABLE_SELECT_CMD, uid, mid))
                        .select(new ListOutcome<>(new ListOutcome.Mapping<MailishMessagesPqTable>() {
                            @Override
                            public MailishMessagesPqTable map(ResultSet rset) throws SQLException {
                                return new MailishMessagesPqTable(rset.getString(1), rset.getString(2),
                                        rset.getString(3), rset.getString(4));
                            }
                        }));
                return select.get(0);
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String getFid() {
            return fid;
        }

        public String getImapId() {
            return imapId;
        }

        public String getImapTime() {
            return imapTime;
        }

        public String getErrors() {
            return errors;
        }

        @Override
        public String toString() {
            return String.format("MailishMessagesPqTable{fid='%s', imapId='%s', imapTime='%s', errors='%s'}",
                    fid, imapId, imapTime, errors);
        }
    }

}
