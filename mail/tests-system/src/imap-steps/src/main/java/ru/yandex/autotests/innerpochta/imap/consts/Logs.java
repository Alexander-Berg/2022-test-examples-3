package ru.yandex.autotests.innerpochta.imap.consts;

/**
 * User: lanwen
 * Date: 21.04.14
 * Time: 5:04
 */
public enum Logs {

    CLIENTS("/var/log/imap/clients/", "-r -A 20"),
    TIMING("/var/log/imap/timing.log", ""),
    API("/var/log/imap/api.log", ""),
    USER_JOURNAL("/var/log/imap/user_journal.tskv", ""),
    IMAP_SO_REPORT("/var/log/imap/so_report.tskv", ""),
    YIMAP("/var/log/imap/imap.log", "");
//    MDB("/var/log/sql_pool/mypp.log.1", "");

    private String path;
    private String grepArgs;

    Logs(String path, String grepArgs) {
        this.path = path;
        this.grepArgs = grepArgs;
    }

    public String path() {
        return path;
    }

    public String grepArgs() {
        return grepArgs;
    }
}
