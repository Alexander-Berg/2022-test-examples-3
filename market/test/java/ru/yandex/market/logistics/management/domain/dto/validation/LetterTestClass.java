package ru.yandex.market.logistics.management.domain.dto.validation;

public class LetterTestClass {
    private String sender;
    private Contents contents;

    public LetterTestClass(String sender,
                           Contents contents) {
        this.sender = sender;
        this.contents = contents;
    }

    public String getSender() {
        return sender;
    }

    public Contents getContents() {
        return contents;
    }

    public static class Contents {
        private String title;

        public Contents(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }
}
