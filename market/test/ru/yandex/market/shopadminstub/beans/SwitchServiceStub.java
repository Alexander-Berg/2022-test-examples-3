package ru.yandex.market.shopadminstub.beans;

public class SwitchServiceStub extends SwitchService {
    private boolean enabled = false;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void ensureDisabled() throws Exception {
        if (enabled) {
            throw new IllegalStateException("enabled");
        }
    }
}
