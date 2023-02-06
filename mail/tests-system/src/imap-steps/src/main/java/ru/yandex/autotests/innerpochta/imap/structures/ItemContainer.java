package ru.yandex.autotests.innerpochta.imap.structures;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matcher;

import static com.google.common.base.Joiner.on;
import static ru.yandex.autotests.innerpochta.imap.matchers.ListItemMatcher.listItem;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 21.04.14
 * Time: 1:41
 * <p>
 * Вспомогательный класс для работы с флагами
 */
public class ItemContainer {

    private Set<String> flags = new HashSet<>();
    private String reference = "|";
    private String name;

    private ItemContainer() {
    }

    public static ItemContainer newItem() {
        return new ItemContainer();
    }

    public Set<String> getFlags() {
        return flags;
    }

    public ItemContainer setFlags(String... flags) {
        Collections.addAll(this.flags, flags);
        return this;
    }

    public String getReference() {
        return reference;
    }

    public ItemContainer setReference(String reference) {
        this.reference = reference;
        return this;
    }

    public String getName() {
        return name;
    }

    public ItemContainer setName(String name) {
        this.name = name;
        return this;
    }

    public ItemContainer replaceFlag(String flag, String anotherFlag) {
        this.flags.remove(flag);
        this.flags.add(anotherFlag);
        return this;
    }

    public ItemContainer removeFlag(String flag) {
        this.flags.remove(flag);
        return this;
    }

    public ItemContainer addFlag(String flag) {
        this.flags.add(flag);
        return this;
    }

    public Matcher<ListItem> getListItem() {
        return listItem(new ListItem(reference, name, flags));
    }

    public ItemContainer setListItem(ListItem listItem) {
        flags = listItem.getFlags();
        name = listItem.getName();
        reference = listItem.getReference();
        return this;
    }

    public String toString() {
        return String.format("(flags=(%s) | reference=%s | name=%s)", on(' ').join(flags), reference, name);
    }
}
