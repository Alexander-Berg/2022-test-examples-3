package ui_tests.src.test.java.Classes;


import java.util.Objects;

public class SmsTemplate {
    private String title;
    private String code;
    private String sender;
    private String text;

    public String getTitle() {
        return title;
    }

    public SmsTemplate setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getCode() {
        return code;
    }

    public SmsTemplate setCode(String code) {
        this.code = code;
        return this;
    }

    public String getSender() {
        return sender;
    }

    public SmsTemplate setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public String getText() {
        return text;
    }

    public SmsTemplate setText(String text) {
        this.text = text;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SmsTemplate that = (SmsTemplate) o;
        if (this.code!=null){
            if (!this.code.equals(that.code)){
                return false;
            }
        }
        if (this.text!=null){
            if (!this.text.equals(that.text)){
                return false;
            }
        }
        if (this.sender!=null){
            if (!this.sender.equals(that.sender)){
                return false;
            }
        }
        if (this.title!=null){
            if (!this.title.equals(that.title)){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, code, sender, text);
    }

    @Override
    public String toString() {
        return "SmsTemplate{" +
                "title='" + title + '\'' +
                ", code='" + code + '\'' +
                ", sender='" + sender + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
