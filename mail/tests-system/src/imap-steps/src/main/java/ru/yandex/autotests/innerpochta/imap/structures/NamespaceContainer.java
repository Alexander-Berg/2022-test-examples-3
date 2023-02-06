package ru.yandex.autotests.innerpochta.imap.structures;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 17.04.14
 * Time: 18:52
 */
public class NamespaceContainer {

    private String name = "";
    private String reference = "|";

    private NamespaceContainer() {
    }

    public static NamespaceContainer emptyNamespace() {
        return new NamespaceContainer();
    }

    public String getName() {
        return name;
    }

    public NamespaceContainer setName(String name) {
        this.name = name;
        return this;
    }

    public String getReference() {
        return reference;
    }

    public NamespaceContainer setReference(String reference) {
        this.reference = reference;
        return this;
    }

    public String getNamespace() {
        return String.format("((\"%s\" \"%s\"))", name, reference);
    }


}
