package ru.yandex.market.pers.author.mock.mvc.socialecom;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.socialecom.dto.RoleInfo;
import ru.yandex.market.pers.author.socialecom.dto.RoleResult;
import ru.yandex.market.pers.author.socialecom.model.UserType;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service
public class CabinetMvcMocks extends AbstractMvcMocks {

    public List<RoleInfo> getUserRoleInfoByUID(String uid) {
        DtoList<RoleInfo> result = parseValue(
            invokeAndRetrieveResponse(
                get("/socialecom/cabinet/UID/" + uid + "/roles")
                    .accept(MediaType.APPLICATION_JSON_UTF8_VALUE),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });
        return result.getData();
    }

    public boolean checkUserRoleByBrand(String uid, long brandId) {
        return checkUserRoleByAuthor(uid, UserType.BRAND, brandId);
    }

    public boolean checkUserRoleByBusiness(String uid, long businessId) {
        return checkUserRoleByAuthor(uid, UserType.BUSINESS, businessId);
    }

    public boolean checkUserRoleByAuthor(String uid, UserType authorType, long authorId) {
        RoleResult result = parseValue(
            invokeAndRetrieveResponse(
                get("/socialecom/cabinet/UID/" + uid + "/roles/" + authorType.getName().toUpperCase() + "/" + authorId)
                    .accept(MediaType.APPLICATION_JSON_UTF8_VALUE),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });
        return result.getResult();
    }
}
