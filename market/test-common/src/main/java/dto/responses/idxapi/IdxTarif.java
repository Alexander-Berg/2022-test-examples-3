package dto.responses.idxapi;

public enum IdxTarif {
    COURIER, POST, PICKUP;

    public String returnName() {
        return name() + ".MMAP";
    }
}
