package ru.yandex.mail.tests.hound;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.restassured.response.Response;
import ru.yandex.mail.tests.hound.generated.Fid;
import ru.yandex.mail.tests.hound.generated.Folder;
import ru.yandex.mail.tests.hound.generated.FolderSymbol;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Folders {
    private String respAsString;

    private Folders(String responseAsString) {
        this.respAsString = responseAsString;
    }

    public static Folders folders(Response response) {
        return new Folders(response.asString());
    }

    public boolean isFolderThreaded(String fid) {
        return folders().get(fid).getIsThreadable();
    }

    public String name(FolderSymbol symbol) {
        JsonParser parser = new JsonParser();
        JsonElement fidsFragment = parser.parse(respAsString).getAsJsonObject().get("folders");
        Map<String, Fid> lids = new Gson().fromJson(fidsFragment, new TypeToken<Map<String, Fid>>(){}.getType());
        Optional<Map.Entry<String, Fid>> fidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry -> entry
                                .getValue()
                                .getSymbolicName()
                                .getTitle()
                                .equals(symbol.toString()))
                .findFirst();
        return fidEntry.isPresent() ? fidEntry.get().getValue().getName() : null;
    }

    public String name(String fid) {
        JsonParser parser = new JsonParser();
        JsonElement fidsFragment = parser.parse(respAsString).getAsJsonObject().get("folders");
        Map<String, Fid> lids = new Gson().fromJson(fidsFragment, new TypeToken<Map<String, Fid>>(){}.getType());
        Optional<Map.Entry<String, Fid>> fidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry -> entry
                                .getKey()
                                .equals(fid))
                .findFirst();
        return fidEntry.isPresent() ? fidEntry.get().getValue().getName() : null;
    }

    public String fid(FolderSymbol symbol) {
        JsonParser parser = new JsonParser();
        JsonElement fidsFragment = parser.parse(respAsString).getAsJsonObject().get("folders");
        Map<String, Fid> lids = new Gson().fromJson(fidsFragment, new TypeToken<Map<String, Fid>>(){}.getType());
        Optional<Map.Entry<String, Fid>> fidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry -> entry
                                .getValue()
                                .getSymbolicName()
                                .getTitle()
                                .equals(symbol.toString()))
                .findFirst();
        return fidEntry.isPresent() ? fidEntry.get().getKey() : null;
    }

    public String symbolName(String name) {
        JsonParser parser = new JsonParser();
        JsonElement fidsFragment = parser.parse(respAsString).getAsJsonObject().get("folders");
        Map<String, Fid> lids = new Gson().fromJson(fidsFragment, new TypeToken<Map<String, Fid>>(){}.getType());
        Optional<Map.Entry<String, Fid>> fidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry -> entry
                                .getKey()
                                .equals(name))
                .findFirst();
        return fidEntry.isPresent() ? fidEntry.get().getValue().getSymbolicName().getTitle() : null;
    }

    public String fid(String name) {
        JsonParser parser = new JsonParser();
        JsonElement fidsFragment = parser.parse(respAsString).getAsJsonObject().get("folders");
        Map<String, Fid> lids = new Gson().fromJson(fidsFragment, new TypeToken<Map<String, Fid>>(){}.getType());
        Optional<Map.Entry<String, Fid>> fidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry -> entry
                                .getValue()
                                .getName()
                                .equals(name))
                .findFirst();
        return fidEntry.isPresent() ? fidEntry.get().getKey() : null;
    }

    public Integer count(String fid) {
        JsonParser parser = new JsonParser();
        JsonElement fidsFragment = parser.parse(respAsString).getAsJsonObject().get("folders");
        Map<String, Fid> fids = new Gson().fromJson(fidsFragment, new TypeToken<Map<String, Fid>>(){}.getType());

        Optional<Integer> count = fids
                .entrySet()
                .stream()
                .filter(
                        (entry -> entry
                                .getKey()
                                .equals(fid))
                )
                .map(
                        (entry -> entry
                                .getValue()
                                .getMessagesCount()
                                .intValue())
                )
                .findFirst();

        return count.orElse(null);
    }

    public Integer newCount(String fid) {
        JsonParser parser = new JsonParser();
        JsonElement fidsFragment = parser.parse(respAsString).getAsJsonObject().get("folders");
        Map<String, Fid> fids = new Gson().fromJson(fidsFragment, new TypeToken<Map<String, Fid>>(){}.getType());

        Optional<Integer> count = fids
                .entrySet()
                .stream()
                .filter(
                        (entry -> entry
                                .getKey()
                                .equals(fid))
                )
                .map(
                        (entry -> entry
                                .getValue()
                                .getNewMessagesCount()
                                .intValue())
                )
                .findFirst();

        return count.orElse(null);
    }

    public List<String> nonsystemFids() {
        JsonParser parser = new JsonParser();
        JsonElement fidsFragment = parser.parse(respAsString).getAsJsonObject().get("folders");
        Map<String, Fid> fids = new Gson().fromJson(fidsFragment, new TypeToken<Map<String, Fid>>(){}.getType());
        return fids
                .entrySet()
                .stream()
                .filter((entry -> entry.getValue().getSymbolicName().getTitle().isEmpty()))
                .map((entry -> entry.getKey()))
                .collect(Collectors.toList());
    }

    public List<String> fids() {
        JsonParser parser = new JsonParser();
        JsonElement fidsFragment = parser.parse(respAsString).getAsJsonObject().get("folders");
        Map<String, Fid> fids = new Gson().fromJson(fidsFragment, new TypeToken<Map<String, Fid>>(){}.getType());
        return fids
                .entrySet()
                .stream()
                .map((entry -> entry.getKey()))
                .collect(Collectors.toList());
    }

    public Map<String, Folder> folders() {
        JsonParser parser = new JsonParser();
        JsonElement foldersFragment = parser.parse(respAsString).getAsJsonObject().get("folders");
        return new Gson().fromJson(foldersFragment, new TypeToken<Map<String, Folder>>(){}.getType());
    }

    public String folderPop3(FolderSymbol symbol) {
        JsonParser parser = new JsonParser();
        JsonElement fidsFragment = parser.parse(respAsString).getAsJsonObject().get("folders");
        Map<String, Fid> lids = new Gson().fromJson(fidsFragment, new TypeToken<Map<String, Fid>>(){}.getType());
        Optional<Map.Entry<String, Fid>> fidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry -> entry
                                .getValue()
                                .getSymbolicName()
                                .getTitle()
                                .equals(symbol.toString()))
                .findFirst();
        return fidEntry.isPresent() ? fidEntry.get().getValue().getPop3On() : null;
    }
}
