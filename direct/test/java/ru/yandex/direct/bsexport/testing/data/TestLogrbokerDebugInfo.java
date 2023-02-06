package ru.yandex.direct.bsexport.testing.data;

import ru.yandex.direct.bsexport.model.LogbrokerDebugInfo;

public class TestLogrbokerDebugInfo {
    public static final LogbrokerDebugInfo ppcdev3Std1 = LogbrokerDebugInfo.newBuilder()
            .setHost("ppcdev3.yandex.ru")
            .setParNormNick("std_1")
            .setReqid(7584740398309904693L)
            .setSendTime("2019-12-05 20:46:21")
            .setShard(2)
            .build();
}
