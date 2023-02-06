package ru.yandex.autotests.innerpochta.util;

/**
 * User: lanwen
 * Date: 13.08.13
 * Time: 17:50
 */
public class SSHCmdBuilder {

    public static SSHCmdBuilder grep(String what, String where) {
        return new SSHCmdBuilder().grep(what).params(where);
    }


    private StringBuilder buffer = new StringBuilder();

    public SSHCmdBuilder params(String params) {
        buffer.append(" ").append(params);
        return this;
    }

    public SSHCmdBuilder cat(String which) {
        buffer.append("cat ").append(which);
        return this;
    }

    public SSHCmdBuilder pipe() {
        buffer.append(" | ");
        return this;
    }

    public SSHCmdBuilder grep(String grep) {
        buffer.append("grep ").append(grep);
        return this;
    }

    public SSHCmdBuilder awk(String script) {
        buffer.append(String.format("awk {'%s'}", script));
        return this;
    }

    public SSHCmdBuilder tail(int n) {
        buffer.append("tail -n ").append(n);
        return this;
    }

    public String build() {
        return buffer.toString();
    }


}
