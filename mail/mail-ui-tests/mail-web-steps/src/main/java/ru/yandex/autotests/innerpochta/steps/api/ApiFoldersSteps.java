package ru.yandex.autotests.innerpochta.steps.api;

import edu.emory.mathcs.backport.java.util.Arrays;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.ArrayList;
import java.util.List;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.innerpochta.api.folders.DoFolderClearHandler.doFolderClearHandler;
import static ru.yandex.autotests.innerpochta.api.folders.DoFolderMoveHandler.doFolderMoveHandler;
import static ru.yandex.autotests.innerpochta.api.folders.DoFolderRemoveHandler.doFolderRemoveHandler;
import static ru.yandex.autotests.innerpochta.api.folders.DoFolderSetSymbolHandler.doFolderSetSymbolHandler;
import static ru.yandex.autotests.innerpochta.api.folders.DoFoldersAddHandler.doFoldersAddHandler;
import static ru.yandex.autotests.innerpochta.api.folders.FoldersHandler.foldersHandler;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_CLEAR_PARAM_PURGE;
import static ru.yandex.autotests.innerpochta.util.handlers.FoldersConstants.FOLDERS_PARAM_PARENT_ID;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ARCHIVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TEMPLATE;


/**
 * Created by mabelpines on 05.05.15.
 */
@SuppressWarnings({"UnusedReturnValue", "unchecked"})
public class ApiFoldersSteps {

    public RestAssuredAuthRule auth;

    public ApiFoldersSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    @Step("Вызов api-метода: folders. Получаем список всех папок пользователя.")
    public List<Folder> getAllFolders() {
        return Arrays.asList(foldersHandler().withAuth(auth).callFoldersHandler().then().extract()
            .jsonPath(getJsonPathConfig()).getObject("models[0].data.folder", Folder[].class));
    }

    @Step("Вызов api-метода: folders. Получаем все пользовательские папки.")
    public List<Folder> getAllUserFolders() {
        return select(getAllFolders(), having(on(Folder.class).getUser(), equalTo(true)));
    }

    @Step("Вызов api-метода: folders. Выбираем папку с названием: {0}")
    public Folder getFolderBySymbol(String symbol) {
        Folder folder;
        folder = selectFirst(getAllFolders(), having(on(Folder.class).getSymbol(), equalTo(symbol)));
        if (folder == null) {
            folder = getFolderByName(symbol);
        }
        return folder;
    }

    public Folder getFolderByName(String name) {
        return selectFirst(getAllFolders(), having(on(Folder.class).getName(), equalTo(name)));
    }

    @Step("Вызов api-метода: do-folders-add. Создаем новую папку: “{0}“")
    public Folder createNewFolder(String foldersName) {
        doFoldersAddHandler().withAuth(auth).withFolderName(foldersName).callDoFoldersAddHandler();
        return getCreatedFolderWithWait(foldersName);
    }

    @Step("Вызов api-метода: do-folders-add. Создаем новую вложенную  папку: “{0}“")
    public Folder createNewSubFolder(String foldersName, Folder parentFolder) {
        doFoldersAddHandler().withAuth(auth).withFolderName(foldersName)
            .withParam(FOLDERS_PARAM_PARENT_ID, parentFolder.getFid()).callDoFoldersAddHandler();
        return getCreatedFolderWithWait(foldersName);
    }

    @Step("Вызов api-метода: do-folders-add. Создаем папку template")
    public Folder createTemplateFolder() {
        Folder folder = getFolderBySymbol(TEMPLATE);
        if (folder == null) {
            doFoldersAddHandler().withAuth(auth).withFolderName(TEMPLATE)
                .withParam(FOLDERS_PARAM_PARENT_ID, getFolderBySymbol(DRAFT).getFid()).callDoFoldersAddHandler();
            doFolderSetSymbolHandler().withAuth(auth).withFid(getFolderByName(TEMPLATE).getFid())
                .withSymbol(TEMPLATE).callFolderSetSymbolHandler();
            folder = getFolderBySymbol(TEMPLATE);
        }
        return folder;
    }

    @Step("Вызов api-метода: do-folders-add. Создаем папку archive")
    public Folder createArchiveFolder() {
        Folder folder = getFolderBySymbol(ARCHIVE);
        if (folder == null) {
            doFoldersAddHandler().withAuth(auth).withFolderName(ARCHIVE).callDoFoldersAddHandler();
            doFolderSetSymbolHandler().withAuth(auth).withFid(getFolderByName(ARCHIVE).getFid())
                .withSymbol(ARCHIVE).callFolderSetSymbolHandler();
            folder = getFolderBySymbol(ARCHIVE);
        }
        return folder;
    }

    @Step("Ждём создания папки {0}")
    private Folder getCreatedFolderWithWait(String foldersName) {
        List<Folder> folders = new ArrayList<>();
        int retries = 2;
        while (folders.size() < 1 && retries > 0) {
            folders = select(getAllFolders(), having(on(Folder.class).getName(), equalTo(foldersName)));
            retries--;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return folders.get(0);
    }

    @Step("Вызов api-метода: do-folder-remove. Удаляем все пользовательские папки")
    public ApiFoldersSteps deleteAllCustomFolders() {
        List<Folder> customFolders = select(getAllFolders(), having(on(Folder.class).getUser(), equalTo(true)));
        for (Folder folder : customFolders) {
            deleteFolder(folder.getName(), folder);
        }
        return this;
    }

    @Step("Вызов api-метода: do-folder-remove. Удаляем папку: “{0}“")
    public ApiFoldersSteps deleteFolder(String folderName, Folder folder) {
        if (folder.getFid() != null) {
            Response resp = doFolderRemoveHandler().withAuth(auth).withFid(folder.getFid()).callDoFolderRemoveHandler();
        }
        return this;
    }

    @Step("Вызов api-метода: do-folder-remove. Удаляем папку “{0}“, если она существует")
    public ApiFoldersSteps deleteFolderIfExist(String folderName) {
        Folder folder = getFolderBySymbol(folderName);
        if (folder != null) {
            doFolderSetSymbolHandler().withAuth(auth).withFid(folder.getFid()).callFolderSetSymbolHandler();
            doFolderRemoveHandler().withAuth(auth).withFid(folder.getFid()).callDoFolderRemoveHandler();
        }
        return this;
    }

    @Step("Вызов api-метода: do-folder-remove. Удаляем все системные папки, кроме первоначальных")
    public ApiFoldersSteps deleteAllDefaultFoldersExceptInitial() {
        List<Folder> customFolders = select(getAllFolders(), having(on(Folder.class).getDefault(), equalTo("true")));
        String[] defaultFolders = {"Inbox", "Sent", "Drafts", "Spam", "Trash", "Outbox", "Reply Later"};
        for (Folder folder : customFolders) {
            if (!Arrays.asList(defaultFolders).contains(folder.getName())) {
                doFolderSetSymbolHandler().withAuth(auth).withFid(folder.getFid()).callFolderSetSymbolHandler();
                doFolderRemoveHandler().withAuth(auth).withFid(folder.getFid()).callDoFolderRemoveHandler();
            }
        }
        return this;
    }

    @Step("Вызов api-метода: do-folders-clear. Очищаем папку: “{0}“")
    public ApiFoldersSteps purgeFolder(Folder folder) {
        doFolderClearHandler().withAuth(auth).withCfid(folder.getFid()).withMethod(FOLDERS_CLEAR_PARAM_PURGE)
            .callDoFolderClearHandler();
        return this;
    }

    @Step("Вызов метода: do-folders-clear. Устанавливаем правило очистки папки “Письмо старше {1}“.")
    public ApiFoldersSteps purgeInboxByDateAction(String apiDaysParam, String daysInInterface, String fid) {
        doFolderClearHandler().withAuth(auth).withCfid(fid).withMethod(FOLDERS_CLEAR_PARAM_PURGE)
            .withOldF(apiDaysParam).callDoFolderClearHandler();
        return this;
    }

    @Step("Вызов api-метода: do-folder-move. Перемещаем папку {0} в папку {1}")
    public ApiFoldersSteps moveFolderToFolder(String folderId, String parentId) {
        if (parentId.equals("1")) {
            parentId = "0";
        }
        doFolderMoveHandler().withAuth(auth).withFid(folderId).withParentId(parentId).callDoFolderMoveHandler();
        return this;
    }

    @Step("Получаем все fid пользовательских папок")
    public String getAllFids() {
        ArrayList<String> arrayList = new ArrayList<>();
        List<Folder> customFolders = getAllFolders();
        for (Folder folder : customFolders) {
            arrayList.add(folder.getFid());
        }
        return String.join(",", arrayList);
    }

}
