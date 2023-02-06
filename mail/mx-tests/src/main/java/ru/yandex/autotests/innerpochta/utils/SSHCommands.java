package ru.yandex.autotests.innerpochta.utils;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import org.apache.log4j.Logger;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;

public class SSHCommands {
    public static String executeCommAndResturnResultAsString(Connection conn, String comm, Logger log)
        throws IOException {
        Session sess = conn.openSession();
        log.info(comm);
        sess.execCommand(comm);
        StreamGobbler stdout = new StreamGobbler(sess.getStdout());
        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
        String result = "";

        while (true) {
            String line = br.readLine();
            if (line == null) {
                sess.close();
                return result;
            }

            result = result.concat(line + "\n");
        }
    }
}
