package ru.yandex.direct.internaltools.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

public class TestParamForSerialisation extends InternalToolParameter {
    static final Map<String, Object> BASE_MAP = ImmutableMap.<String, Object>builder()
            .put("cb", true)
            .put("cbt", false)
            .put("number", 15)
            .put("shard", 3)
            .put("text", "some text")
            .put("testEnum", "NOT_ACTION")
            .put("date", "2017-08-12")
            .put("dateTime", "2017-08-20T15:55:01")
            .put("file", new byte[]{12, 13, -123, 10})
            .build();

    static final TestParamForSerialisation RESULT_OBJ = new TestParamForSerialisation();

    static {
        RESULT_OBJ.cb = true;
        RESULT_OBJ.cbt = false;
        RESULT_OBJ.number = 15L;
        RESULT_OBJ.shard = 3;
        RESULT_OBJ.text = "some_text";
        RESULT_OBJ.testEnum = TestEnum.NOT_ACTION;
        RESULT_OBJ.date = LocalDate.parse("2017-08-12");
        RESULT_OBJ.dateTime = LocalDateTime.parse("2017-08-20T15:55:01");
        RESULT_OBJ.file = new byte[]{12, 13, -123, 10};
    }

    public enum TestEnum {
        ACTION,
        NOT_ACTION
    }

    private Boolean cb;
    private boolean cbt;
    private Long number;
    private int shard;
    private String text;
    private TestEnum testEnum;
    private LocalDate date;
    private LocalDateTime dateTime;
    private byte[] file;

    public Boolean getCb() {
        return cb;
    }

    public void setCb(Boolean cb) {
        this.cb = cb;
    }

    public boolean isCbt() {
        return cbt;
    }

    public void setCbt(boolean cbt) {
        this.cbt = cbt;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public int getShard() {
        return shard;
    }

    public void setShard(int shard) {
        this.shard = shard;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public TestEnum getTestEnum() {
        return testEnum;
    }

    public void setTestEnum(TestEnum testEnum) {
        this.testEnum = testEnum;
    }
}
