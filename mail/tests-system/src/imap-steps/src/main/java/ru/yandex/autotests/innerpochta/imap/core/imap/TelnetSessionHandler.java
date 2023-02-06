package ru.yandex.autotests.innerpochta.imap.core.imap;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 02.06.14
 * Time: 18:43
 */

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class TelnetSessionHandler extends IoHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private String tag = "";

    public TelnetSessionHandler(String tag) {
        this.tag = tag;
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        logger.error(cause.getMessage());
        session.close(true);
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        logMessage(tag + ".S <-", message.toString());
    }

    @Override
    public void messageSent(IoSession session, Object message) {
        logMessage(tag + ".C ->", message.toString());
    }

    private void logMessage(String sender, String line) {
        String[] lines = line.split("\r\n");
        for (String subLine : lines) {
            logger.info(format("%s %s", sender, subLine));
        }
    }

}
