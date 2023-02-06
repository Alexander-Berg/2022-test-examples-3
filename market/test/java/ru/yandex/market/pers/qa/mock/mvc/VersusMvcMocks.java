package ru.yandex.market.pers.qa.mock.mvc;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.utils.ControllerConstants;
import ru.yandex.market.pers.qa.controller.dto.VersusDto;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 30.08.2019
 */
@Service
public class VersusMvcMocks extends AbstractMvcMocks {

    public VersusDto getVersus(long versusId) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/versus/" + versusId)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return objectMapper.readValue(response, new TypeReference<VersusDto>() {
        });
    }

    public List<VersusDto> getVersusByModel(long modelId, long pageSize) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/versus/model/" + modelId)
                .param(ControllerConstants.PAGE_SIZE_KEY, String.valueOf(pageSize))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        DtoList<VersusDto> versusDtos = objectMapper.readValue(response,
            new TypeReference<DtoList<VersusDto>>() {
            });
        return versusDtos.getData();
    }

    public List<VersusDto> getVersusByCategory(long hid, long pageSize) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/versus/category/" + hid)
                .param(ControllerConstants.PAGE_SIZE_KEY, String.valueOf(pageSize))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        DtoList<VersusDto> versusDtos = objectMapper.readValue(response,
            new TypeReference<DtoList<VersusDto>>() {
            });
        return versusDtos.getData();
    }
}
