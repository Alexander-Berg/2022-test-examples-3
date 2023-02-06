package ru.yandex.autotests.innerpochta.tests.matchers;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import ru.yandex.autotests.innerpochta.tests.pq.PqData;
import ru.yandex.autotests.innerpochta.tests.pq.PqData.MailBoxPqTable;
import ru.yandex.autotests.innerpochta.tests.pq.PqData.MailMessagesPqTable;

/**
 * User: alex89
 * Date: 06.05.2015
 */

public class PqMatchers {
    public static Matcher<MailMessagesPqTable> hasStid(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailMessagesPqTable, String>(tableItemMatcher,
                "st_id should be", "actual") {
            @Override
            protected String featureValueOf(MailMessagesPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getStid();
            }
        };
    }

    public static Matcher<MailMessagesPqTable> hasSize(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailMessagesPqTable, String>(tableItemMatcher,
                "size should be", "actual") {
            @Override
            protected String featureValueOf(MailMessagesPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getSize();
            }
        };
    }

    public static Matcher<MailMessagesPqTable> hasAttributes(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailMessagesPqTable, String>(tableItemMatcher,
                "attributes should be", "actual") {
            @Override
            protected String featureValueOf(MailMessagesPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getAttributes();
            }
        };
    }

    public static Matcher<MailMessagesPqTable> hasAttaches(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailMessagesPqTable, String>(tableItemMatcher,
                "attaches should be", "actual") {
            @Override
            protected String featureValueOf(MailMessagesPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getAttaches();
            }
        };
    }

    public static Matcher<MailMessagesPqTable> hasFirstLine(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailMessagesPqTable, String>(tableItemMatcher,
                "firstline should be", "actual") {
            @Override
            protected String featureValueOf(MailMessagesPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getFirstline();
            }
        };
    }

    public static Matcher<MailMessagesPqTable> hasSubject(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailMessagesPqTable, String>(tableItemMatcher,
                "subject should be", "actual") {
            @Override
            protected String featureValueOf(MailMessagesPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getSubject();
            }
        };
    }

    public static Matcher<MailMessagesPqTable> hasHdrDate(Matcher tableItemMatcher) {
        return new FeatureMatcher<MailMessagesPqTable, String>(tableItemMatcher,
                "hdr_date should be", "actual") {
            @Override
            protected String featureValueOf(MailMessagesPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getHdrDate();
            }
        };
    }

    public static Matcher<MailMessagesPqTable> hasHdrMsgId(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailMessagesPqTable, String>(tableItemMatcher,
                "hdr_msg_id should be", "actual") {
            @Override
            protected String featureValueOf(MailMessagesPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getHdrMsgId();
            }
        };
    }

    public static Matcher<MailMessagesPqTable> hasRecipients(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailMessagesPqTable, String>(tableItemMatcher,
                "recipients should be", "actual") {
            @Override
            protected String featureValueOf(MailMessagesPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getRecipients();
            }
        };
    }

    public static Matcher<MailMessagesPqTable> hasMime(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailMessagesPqTable, String>(tableItemMatcher,
                "mime should be", "actual") {
            @Override
            protected String featureValueOf(MailMessagesPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getMime();
            }
        };
    }

    public static Matcher<MailBoxPqTable> hasFid(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailBoxPqTable, String>(tableItemMatcher,
                "fid should be", "actual") {
            @Override
            protected String featureValueOf(MailBoxPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getFid();
            }
        };
    }

    public static Matcher<MailBoxPqTable> hasTid(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailBoxPqTable, String>(tableItemMatcher,
                "tid should be", "actual") {
            @Override
            protected String featureValueOf(MailBoxPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getTid();
            }
        };
    }

    public static Matcher<MailBoxPqTable> hasSeen(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailBoxPqTable, String>(tableItemMatcher,
                "seen should be", "actual") {
            @Override
            protected String featureValueOf(MailBoxPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getSeen();
            }
        };
    }

    public static Matcher<MailBoxPqTable> hasRecent(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailBoxPqTable, String>(tableItemMatcher,
                "recent should be", "actual") {
            @Override
            protected String featureValueOf(MailBoxPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getRecent();
            }
        };
    }

    public static Matcher<MailBoxPqTable> hasDeleted(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailBoxPqTable, String>(tableItemMatcher,
                "deleted should be", "actual") {
            @Override
            protected String featureValueOf(MailBoxPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getDeleted();
            }
        };
    }

    public static Matcher<MailBoxPqTable> hasReceivedDate(Matcher tableItemMatcher) {
        return new FeatureMatcher<MailBoxPqTable, String>(tableItemMatcher,
                "received_date should be", "actual") {
            @Override
            protected String featureValueOf(MailBoxPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getReceivedDate();
            }
        };
    }

    public static Matcher<MailBoxPqTable> hasLids(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<MailBoxPqTable, String>(tableItemMatcher,
                "lids should be", "actual") {
            @Override
            protected String featureValueOf(MailBoxPqTable mailMessagesPqTableInfo) {
                return mailMessagesPqTableInfo.getLids();
            }
        };
    }

    public static Matcher<PqData.MailishMessagesPqTable> hasMailishFid(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<PqData.MailishMessagesPqTable, String>(tableItemMatcher,
                "fid should be", "actual") {
            @Override
            protected String featureValueOf(PqData.MailishMessagesPqTable mailishMessagesPqTableInfo) {
                return mailishMessagesPqTableInfo.getFid();
            }
        };
    }

    public static Matcher<PqData.MailishMessagesPqTable> hasMailishErrors(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<PqData.MailishMessagesPqTable, String>(tableItemMatcher,
                "errors should be", "actual") {
            @Override
            protected String featureValueOf(PqData.MailishMessagesPqTable mailishMessagesPqTableInfo) {
                return mailishMessagesPqTableInfo.getErrors();
            }
        };
    }

    public static Matcher<PqData.MailishMessagesPqTable> hasMailishImapId(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<PqData.MailishMessagesPqTable, String>(tableItemMatcher,
                "imap_id should be", "actual") {
            @Override
            protected String featureValueOf(PqData.MailishMessagesPqTable mailishMessagesPqTableInfo) {
                return mailishMessagesPqTableInfo.getImapId();
            }
        };
    }

    public static Matcher<PqData.MailishMessagesPqTable> hasMailishImapTime(Matcher<String> tableItemMatcher) {
        return new FeatureMatcher<PqData.MailishMessagesPqTable, String>(tableItemMatcher,
                "imap_time should be", "actual") {
            @Override
            protected String featureValueOf(PqData.MailishMessagesPqTable mailishMessagesPqTableInfo) {
                return mailishMessagesPqTableInfo.getImapTime();
            }
        };
    }
}
