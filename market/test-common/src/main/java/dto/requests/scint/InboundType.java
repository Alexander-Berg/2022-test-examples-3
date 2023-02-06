package dto.requests.scint;

public enum InboundType {
    XDOC_TRANSIT("XDOC_TRANSIT");
    private final String type;

    InboundType(String type) {
        this.type = type;
    }

    public String getValue() {
        return type;
    }
}
