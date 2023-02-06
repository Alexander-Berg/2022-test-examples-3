package ru.yandex.autotests.innerpochta.imap.structures;

import java.util.List;

import com.google.common.collect.Lists;

import ru.yandex.autotests.innerpochta.imap.utils.Utils;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Lists.newArrayList;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.generateRandomList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 02.04.14
 * Time: 15:09
 */
public class FolderContainer {

    private List<String> folders;

    private FolderContainer(List<String> folders) {
        this.folders = folders;
    }

    public static FolderContainer newFolder() {
        return new FolderContainer(Lists.newArrayList(Utils.generateName()));
    }

    public static FolderContainer newFolder(int levelOfHierarchy) {
        return new FolderContainer(generateRandomList(levelOfHierarchy));
    }

    public static FolderContainer newFolder(String... folder) {
        return new FolderContainer(newArrayList(folder));
    }

    public static FolderContainer newFolder(List<String> folders) {
        return new FolderContainer(folders);
    }

    public FolderContainer add(String folder) {
        folders.add(folder);
        return this;
    }

    public FolderContainer add(List<String> list) {
        folders.addAll(list);
        return this;
    }

    public List<String> withChild() {
        return foldersTreeAsList().subList(0, folders.size() - 1);
    }

    public String parent() {
        return folders.get(0);
    }

    public String fullName() {
        return on("|").join(folders);
    }

    public List<String> foldersTreeAsList() {
        List<String> result = newArrayList(parent());
        String varFolder = parent();
        for (String folder : folders.subList(1, folders.size())) {
            varFolder = on("|").join(varFolder, folder);
            result.add(varFolder);
        }
        return result;
    }
}
