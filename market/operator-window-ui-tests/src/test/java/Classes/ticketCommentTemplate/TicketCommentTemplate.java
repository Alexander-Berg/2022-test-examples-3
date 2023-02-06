package ui_tests.src.test.java.Classes.ticketCommentTemplate;

import java.util.List;

// Написан по аналогии с Classes.ticket.Ticket
public class TicketCommentTemplate {

    // Атрибуты
    private String title;
    private String code;
    private String text;
    private List<String> categories;

    @Override
    public String toString() {
        return "TicketCommentTemplate{" +
                "title=" + title +
                ", code=" + code +
                ", text=" + text +
                ", categories=" + categories +
                '}';
    }

    // Сеттеры и геттеры для атрибутов
    public String getTitle() { return title; }
    public TicketCommentTemplate setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getCode() { return code; }
    public TicketCommentTemplate setCode(String code) {
        this.code = code;
        return this;
    }

    public String getText() { return text; }
    public TicketCommentTemplate setText(String text) {
        this.text = text;
        return this;
    }

    public List<String> getCategories() { return categories; }
    public TicketCommentTemplate setCategories(List<String> categories) {
        this.categories = categories;
        return this;
    }

    @Override
    public boolean equals(Object expectedObject) {
        if (this == expectedObject) return true;
        if (expectedObject == null || getClass() != expectedObject.getClass()) return false;
        TicketCommentTemplate template = (TicketCommentTemplate) expectedObject;
        if (this.title!=null){
            if (!template.title.equals(this.title)){
                return false;
            }
        }
        if (this.code!=null){
            if (!template.code.equals(this.code)){
                return false;
            }
        }

        if (this.text!=null){
            if (!template.text.equals(this.text)){
                return false;
            }
        }
        if (this.categories!=null){
            if (template.categories.equals(this.categories)){
                return false;
            }
        }
        return true;
    }
}
