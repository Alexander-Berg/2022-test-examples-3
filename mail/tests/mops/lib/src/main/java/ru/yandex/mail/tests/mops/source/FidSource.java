package ru.yandex.mail.tests.mops.source;

public class FidSource implements Source {
    private final String fid;
    private final String subject;
    private final String from;
    private final Integer age;

    public FidSource(String fid) {
        this(fid, null, null, null);
    }

    public FidSource(String fid, String subject, String from, Integer age) {
        this.fid = fid;
        this.subject = subject;
        this.from = from;
        this.age = age;
    }

    public FidSource withSubject(String subject) {
        return new FidSource(fid, subject, from, age);
    }

    public FidSource withFrom(String from) {
        return new FidSource(fid, subject, from, age);
    }

    public FidSource withAge(int age) {
        return new FidSource(fid, subject, from, age);
    }

    @Override
    public <T> void fill(T obj) throws Exception {
        fill(obj, "withFid", fid);

        if (subject != null) {
            fill(obj, "withSubject", subject);
        }

        if (from != null) {
            fill(obj, "withFrom", from);
        }

        if (age != null) {
            fill(obj, "withAge", String.valueOf(age));
        }
    }

    private <T, U> void fill(T obj, String setterName, U value) throws Exception {
        obj.getClass().getMethod(setterName, value.getClass()).invoke(obj, value);
    }
}