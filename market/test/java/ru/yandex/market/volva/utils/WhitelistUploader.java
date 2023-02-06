package ru.yandex.market.volva.utils;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import lombok.SneakyThrows;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.serializer.VolvaJsonUtils;
import ru.yandex.market.volva.web.controller.UserDto;
import ru.yandex.passport.tvmauth.NativeTvmClient;
import ru.yandex.passport.tvmauth.TvmApiSettings;
import ru.yandex.passport.tvmauth.TvmClient;

/**
 * @author dzvyagin
 */
public class WhitelistUploader {

    private static final int TVM_ID = 2017321;
    private static final int VOLVA_TVM_ID = 2024765;
    private static final String TVM_SECRET = "secret";

    public static void main(String[] args) {
        WhitelistUploader uploader = new WhitelistUploader();
        String volvaUrl = "http://volva-reader.vs.market.yandex.net";
        Path filePath = Path.of("/Users/dzvyagin/projects/other/glue_market_beru_exclude_vertices");
        uploader.upload(volvaUrl, filePath);
    }


    @SneakyThrows
    public void upload(String url, Path filePath) {
        TvmClient tvmClient = tvmClient();
        RestTemplate template = new RestTemplate();
        List<String> lines = Files.readAllLines(filePath);
        String urlPath = url + "/unglue/node?author=dzvyagin&reason=exclude_verticies";
        int count = 0;
        int errorCount = 0;
        System.out.println("Lines to process: " + lines.size());
        for (var line : lines) {
            char startChar = line.charAt(0);
            IdType type = null;
            String id = null;
            if (Character.isDigit(startChar)) {
                type = IdType.PUID;
                id = line;
            } else if (startChar == 'c') {
                type = IdType.CRYPTA_ID;
                id = line.substring(1);
            } else if (startChar == 'p') {
                type = IdType.CARD;
                id = line.substring(1);
            } else if (startChar == 'u') {
                type = IdType.UUID;
                id = line.substring(1);
            } else if (startChar == 'y') {
                type = IdType.YANDEXUID;
                id = line.substring(1);
            } else {
                System.out.println("Unrecognizable id: " + line);
                continue;
            }
            UserDto dto = new UserDto(id, type);
            String json = VolvaJsonUtils.toJson(dto);
            String finalPath = urlPath + "&id=" + id + "&idType=" + type;
            RequestEntity<String> request = RequestEntity
                    .put(URI.create(finalPath))
                    .header("X-Ya-Service-Ticket", tvmClient.getServiceTicketFor(VOLVA_TVM_ID))
                    .body(json);
            try {
                template.exchange(request, String.class);
            } catch (HttpServerErrorException e) {
                errorCount++;
            }
            count++;
            if (count % 1000 == 0) {
                System.out.println("Processed: " + count);
            }
            if (errorCount > 0 && errorCount % 50 == 0) {
                System.out.println("Failed: " + errorCount);
            }
//            System.out.println(json);
        }
        System.out.println("Finished: " + count);
    }

    private TvmClient tvmClient() {
        TvmApiSettings settings = TvmApiSettings.create();
        settings.setSelfTvmId(TVM_ID);
        settings.enableServiceTicketChecking();
        settings.enableServiceTicketsFetchOptions(
                TVM_SECRET,
                new int[]{VOLVA_TVM_ID}
        );
        return new NativeTvmClient(settings);
    }

}
