package dto.responses.lom.admin.order;

@SuppressWarnings("checkstyle:NoWhitespaceBefore")
public enum LomAdminOrderTag {
    B2B_CUSTOMER("B2B"),
    ;
    private final String text;

    LomAdminOrderTag(String experiment) {
        this.text = experiment;
    }

    public String getValue() {
        return text;
    }
}
