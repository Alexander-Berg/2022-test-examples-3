package ru.yandex.calendar.frontend.web.cmd.generic;

import Yandex.RequestPackage.RequestData;

import ru.yandex.calendar.frontend.web.cmd.ctx.XmlCmdContext;

public class CmdTestCmd extends XmlCommand {
    public CmdTestCmd(String tagName, RequestData requestData) {
        super(tagName, requestData);
    }

    @Override
    protected void buildXmlResponse(XmlCmdContext ctx) {
    }
}
