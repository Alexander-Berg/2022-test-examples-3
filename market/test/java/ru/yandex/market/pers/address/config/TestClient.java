package ru.yandex.market.pers.address.config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.pers.address.controllers.model.AddressDtoResponse;
import ru.yandex.market.pers.address.controllers.model.AdultDto;
import ru.yandex.market.pers.address.controllers.model.ContactDto;
import ru.yandex.market.pers.address.controllers.model.ContactDtoResponse;
import ru.yandex.market.pers.address.controllers.model.FavouritePickpointRequest;
import ru.yandex.market.pers.address.controllers.model.FavouritePickpointResponse;
import ru.yandex.market.pers.address.controllers.model.IdResponse;
import ru.yandex.market.pers.address.controllers.model.LastStateDto;
import ru.yandex.market.pers.address.controllers.model.NewAddressDtoRequest;
import ru.yandex.market.pers.address.controllers.model.NewPresetDtoRequest;
import ru.yandex.market.pers.address.controllers.model.PresetsResponse;
import ru.yandex.market.pers.address.dao.ObjectKey;
import ru.yandex.market.pers.address.factories.TestPlatform;
import ru.yandex.market.pers.address.model.Address;
import ru.yandex.market.pers.address.model.Contact;
import ru.yandex.market.pers.address.model.Preset;
import ru.yandex.market.pers.address.model.identity.Identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestClient {
    private static final TypeReference<List<FavouritePickpointResponse>> FAVOURITE_PICKPOINT_RESPONSES_TYPE =
            new TypeReference<List<FavouritePickpointResponse>>() {
            };
    private static final TypeReference<List<AddressDtoResponse>> ADDRESSES_TYPE =
            new TypeReference<List<AddressDtoResponse>>() {
            };
    private static final TypeReference<List<ContactDtoResponse>> CONTACTS_TYPE =
            new TypeReference<List<ContactDtoResponse>>() {
            };
    private static final TypeReference<LastStateDto> LAST_STATE_TYPE =
            new TypeReference<LastStateDto>() {
            };
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public TestClient(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public static List<Address> toAddresses(List<AddressDtoResponse> addresses) {
        return addresses.stream()
                .map(b -> b.toAddressBuilder()
                        .setId(new ObjectKey(b.getAddressId()))
                        .setLastTouchedTime(b.getLastTouchedTime()))
                .map(Address.Builder::build)
                .collect(Collectors.toList());
    }

    public static List<Contact> toContacts(List<ContactDtoResponse> contacts) {
        return contacts.stream()
                .map(b -> b.toContactBuilder().setId(new ObjectKey(b.getContactId())))
                .map(Contact.Builder::build)
                .collect(Collectors.toList());
    }

    public static List<Preset> toPresets(PresetsResponse presets) {
        final Map<String, Address> addressIndex = presets.getAddresses().stream()
                .collect(
                        Collectors.toMap(
                                AddressDtoResponse::getId,
                                a -> a.toAddressBuilder().setId(new ObjectKey(a.getAddressId()))
                                        .setLastTouchedTime(a.getLastTouchedTime()).build()
                        )
                );
        final Map<String, Contact> contactIndex = presets.getContacts().stream()
                .collect(
                        Collectors.toMap(
                                ContactDtoResponse::getId,
                                a -> a.toContactBuilder().setId(new ObjectKey(a.getContactId())).build()
                        )
                );

        return presets.getPresets().stream()
                .map(p -> Preset.builder()
                        .setAddress(addressIndex.get(p.getAddressId()))
                        .setContact(contactIndex.get(p.getContactId()))
                        .setId(new ObjectKey(p.getId())).build()
                )
                .collect(Collectors.toList());
    }

    public ObjectKey addPreset(Identity<?> identity, NewPresetDtoRequest toSave, TestPlatform platform) throws Exception {
        String response = addPreset(identity, toSave, platform, status().isOk());
        return new ObjectKey(objectMapper.readValue(response, IdResponse.class).getId());
    }

    public ObjectKey addAddress(Identity<?> identity, NewAddressDtoRequest toSave, TestPlatform platform) throws Exception {
        String response = addAddress(identity, toSave, platform, status().isOk());
        return new ObjectKey(objectMapper.readValue(response, IdResponse.class).getId());
    }


    public AddressDtoResponse addAddressV2(Identity<?> identity, NewAddressDtoRequest toSave, TestPlatform platform,
                                           Boolean onlyPrepare, Boolean manualEntry) throws Exception {
        String response = addAddressV2(identity, toSave, platform, onlyPrepare, manualEntry, status().isOk());
        return objectMapper.readValue(response, AddressDtoResponse.class);
    }

    public void addAddressV2Expected400(Identity<?> identity, NewAddressDtoRequest toSave, TestPlatform platform,
                                        Boolean onlyPrepare, Boolean manualEntry) throws Exception {
        addAddressV2(identity, toSave, platform, onlyPrepare, manualEntry, status().isBadRequest());
    }

    public AddressDtoResponse updateAddressV2(Identity<?> identity, ObjectKey generatedId, TestPlatform platform,
                                              Boolean manualEntry, NewAddressDtoRequest address) throws Exception {
        String response = updateAddressV2(identity, generatedId, platform, manualEntry, address, status().isOk());
        return objectMapper.readValue(response, AddressDtoResponse.class);
    }

    public void updateAddressV2Expected400(Identity<?> identity, ObjectKey generatedId,
                                           TestPlatform platform, Boolean manualEntry,
                                           NewAddressDtoRequest address) throws Exception {
        updateAddressV2(identity, generatedId, platform, manualEntry, address, status().isBadRequest());
    }

    public void addAddressBadRequest(Identity<?> identity, NewAddressDtoRequest toSave, TestPlatform platform) throws Exception {
        addAddress(identity, toSave, platform, status().isBadRequest());
    }

    public PresetsResponse getPresets(Identity<?> identity, TestPlatform platform) throws Exception {
        String response = mockMvc
                .perform(
                        get("/presets/{type}/{uid}/{platform}?regionId=213", identity.getType().getCode(),
                                identity.getStringValue(), platform.path())
                                .header("X-Ya-Service-Ticket", "test")
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, PresetsResponse.class);
    }

    public List<AddressDtoResponse> getAddresses(Identity<?> identity, TestPlatform platform) throws Exception {
        return getAddresses(identity, platform, new LinkedMultiValueMap<>());
    }

    public List<AddressDtoResponse> getAddresses(Identity<?> identity, TestPlatform platform,
                                                 MultiValueMap<String, String> params) throws Exception {
        String response = mockMvc
                .perform(
                        get("/address/{type}/{uid}/{platform}?regionId=213", identity.getType().getCode(),
                                identity.getStringValue(), platform.path())
                                .header("X-Ya-Service-Ticket", "test")
                                .params(params)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, ADDRESSES_TYPE);
    }

    public List<ContactDtoResponse> getContacts(Identity<?> identity, TestPlatform platform) throws Exception {
        String response = mockMvc
                .perform(
                        get("/contact/{type}/{uid}/{platform}?regionId=213", identity.getType().getCode(),
                                identity.getStringValue(), platform.path())
                                .header("X-Ya-Service-Ticket", "test")
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, CONTACTS_TYPE);
    }

    public void deletePreset(Identity<?> identity, ObjectKey objectKey) throws Exception {
        mockMvc
                .perform(delete("/preset/{type}/{userId}/{presetId}", identity.getType().getCode(),
                        identity.getStringValue(), ObjectKey.getObjectKey(objectKey))
                        .header("X-Ya-Service-Ticket", "test")
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

    public void deleteContact(Identity<?> identity, ObjectKey contactId) throws Exception {
        mockMvc
                .perform(delete("/contact/{type}/{userId}/{contactId}", identity.getType().getCode(),
                        identity.getStringValue(), ObjectKey.getObjectKey(contactId))
                        .header("X-Ya-Service-Ticket", "test")
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

    public void deleteAddress(Identity<?> identity, ObjectKey addressId) throws Exception {
        mockMvc
                .perform(delete("/address/{type}/{userId}/{addressId}", identity.getType().getCode(),
                        identity.getStringValue(), ObjectKey.getObjectKey(addressId))
                        .header("X-Ya-Service-Ticket", "test")
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

    public ObjectKey addContact(Identity<?> identity, ContactDto toSave, TestPlatform platform) throws Exception {
        String result = addContact(identity, toSave, platform, status().isOk());
        return new ObjectKey(objectMapper.readValue(result, IdResponse.class).getId());
    }

    public void savePickpoint(Identity<?> identity, FavouritePickpointRequest request) throws Exception {
        savePickpoint(request, identity.getType().getCode(), identity.getStringValue(), status().isOk());
    }

    public void savePickpoint(FavouritePickpointRequest request, String userType, String userId) throws Exception {
        savePickpoint(request, userType, userId, status().isOk());
    }

    public void savePickpoint(FavouritePickpointRequest request, String userType, String userId,
                              ResultMatcher resultMatcher) throws Exception {
        mockMvc
                .perform(post("/pickpoint/{userType}/{userId}", userType, userId)
                        .header("X-Ya-Service-Ticket", "test")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(log())
                .andExpect(resultMatcher);
    }


    public void deletePickpoint(String userType, String userId, String pickId) throws Exception {
        mockMvc
                .perform(delete("/pickpoint/{userType}/{userId}/{pickId}", userType, userId, pickId)
                        .header("X-Ya-Service-Ticket", "test")
                )
                .andDo(log())
                .andExpect(status().isOk());
    }


    public void setAdult(String userType, String userId, boolean adult) throws Exception {
        mockMvc
                .perform(post("/settings/{user_type}/{user_id}/adult", userType, userId)
                        .param("source", "test")
                        .header("X-Ya-Service-Ticket", "test")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(new AdultDto(adult))))
                .andDo(log())
                .andExpect(status().isOk());
    }


    @NotNull
    public String getAdult(String userType, String userId) throws Exception {
        return mockMvc
                .perform(get("/settings/{user_type}/{user_id}/adult", userType, userId)
                        .header("X-Ya-Service-Ticket", "test")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    public List<FavouritePickpointResponse> getPickpoints(String userType, String userId, int limit) throws Exception {
        return getPickpoints(userType, userId, limit, status().isOk(), "default");
    }

    public List<FavouritePickpointResponse> getPickpoints(String userType, String userId, int limit, String exp) throws Exception {
        return getPickpoints(userType, userId, limit, status().isOk(), exp);
    }

    private List<FavouritePickpointResponse> getPickpoints(String userType, String userId, int limit,
                                                           ResultMatcher resultMatcher, String exp) throws Exception {

        String response = mockMvc
                .perform(get("/pickpoints/{userType}/{userId}?limit={limit}", userType, userId, limit)
                        .header("X-Ya-Service-Ticket", "test")
                        .header("X-Market-Rearrfactors", exp)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andDo(log())
                .andExpect(resultMatcher)
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, FAVOURITE_PICKPOINT_RESPONSES_TYPE);
    }

    private String addPreset(Identity<?> identity, NewPresetDtoRequest toSave, TestPlatform platform,
                             ResultMatcher expect) throws Exception {
        return mockMvc
                .perform(
                        post("/preset/{type}/{uid}/{platform}", identity.getType().getCode(),
                                identity.getStringValue(), platform.path())
                                .header("X-Ya-Service-Ticket", "test")
                                .param("source", "blue-market")
                                .content(objectMapper.writeValueAsString(toSave))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(expect)
                .andReturn().getResponse().getContentAsString();
    }

    private String addAddress(Identity<?> identity, NewAddressDtoRequest toSave, TestPlatform platform,
                              ResultMatcher expect) throws Exception {
        return mockMvc
                .perform(
                        post("/address/{type}/{uid}/{platform}", identity.getType().getCode(),
                                identity.getStringValue(), platform.path())
                                .header("X-Ya-Service-Ticket", "test")
                                .param("source", "blue-market")
                                .content(objectMapper.writeValueAsString(toSave))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(expect)
                .andReturn().getResponse().getContentAsString();
    }

    private String addAddressV2(Identity<?> identity, NewAddressDtoRequest toSave, TestPlatform platform,
                                Boolean onlyPrepare, Boolean manualEntry, ResultMatcher expect) throws Exception {
        return mockMvc
                .perform(
                        post("/v2/address/{type}/{uid}/{platform}", identity.getType().getCode(),
                                identity.getStringValue(), platform.path())
                                .header("X-Ya-Service-Ticket", "test")
                                .param("source", "blue-market")
                                .param("onlyPrepare", Boolean.toString(BooleanUtils.isTrue(onlyPrepare)))
                                .param("manualEntry", Boolean.toString(BooleanUtils.isTrue(manualEntry)))
                                .content(objectMapper.writeValueAsString(toSave))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(expect)
                .andReturn().getResponse().getContentAsString();
    }

    private String addContact(Identity<?> identity, ContactDto toSave, TestPlatform platform, ResultMatcher expect) throws Exception {
        return mockMvc
                .perform(
                        post("/contact/{type}/{uid}/{platform}", identity.getType().getCode(),
                                identity.getStringValue(), platform.path())
                                .header("X-Ya-Service-Ticket", "test")
                                .param("source", "blue-market")
                                .content(objectMapper.writeValueAsString(toSave))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(expect)
                .andReturn().getResponse().getContentAsString();
    }

    public void updatePreset(Identity<?> identity, ObjectKey generatedId, TestPlatform platform,
                             NewPresetDtoRequest preset) throws Exception {
        mockMvc
                .perform(
                        put("/preset/{type}/{userId}/{presetId}/{platform}", identity.getType().getCode(),
                                identity.getStringValue(), generatedId.getObjectKey(), platform.path())
                                .content(objectMapper.writeValueAsString(preset))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

    public void updateAddress(Identity<?> identity, ObjectKey generatedId, TestPlatform platform,
                              NewAddressDtoRequest address) throws Exception {
        mockMvc
                .perform(
                        put("/address/{type}/{userId}/{addressId}/{platform}", identity.getType().getCode(),
                                identity.getStringValue(), generatedId, platform.path())
                                .content(objectMapper.writeValueAsString(address))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

    private String updateAddressV2(Identity<?> identity, ObjectKey generatedId, TestPlatform platform,
                                   Boolean manualEntry, NewAddressDtoRequest address,
                                   ResultMatcher expect) throws Exception {
        return mockMvc
                .perform(
                        put("/v2/address/{type}/{userId}/{addressId}/{platform}", identity.getType().getCode(),
                                identity.getStringValue(), generatedId, platform.path())
                                .param("manualEntry", Boolean.toString(BooleanUtils.isTrue(manualEntry)))
                                .content(objectMapper.writeValueAsString(address))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(expect)
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    public void updateContact(Identity<?> identity, ObjectKey generatedId, TestPlatform platform, ContactDto contact) throws Exception {
        mockMvc
                .perform(
                        put("/contact/{type}/{userId}/{contactId}/{platform}", identity.getType().getCode(),
                                identity.getStringValue(), generatedId, platform.path())
                                .content(objectMapper.writeValueAsString(contact))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }


    public void touchAddress(Identity<?> identity, ObjectKey generatedId) throws Exception {
        mockMvc
                .perform(
                        put("/address/{type}/{userId}/{addressId}/touch", identity.getType().getCode(),
                                identity.getStringValue(), generatedId)
                                .header("X-Ya-Service-Ticket", "test")
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

    public LastStateDto getLastState(Identity<?> identity) throws Exception {
        return getLastState(identity, status().isOk());
    }

    private LastStateDto getLastState(Identity<?> identity, ResultMatcher resultMatcher) throws Exception {
        String response = mockMvc
                .perform(
                        get("/last-state/{type}/{userId}", identity.getType().getCode(),
                                identity.getStringValue())
                                .header("X-Ya-Service-Ticket", "test")
                                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andDo(log())
                .andExpect(resultMatcher)
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, LAST_STATE_TYPE);
    }

    public void addLastState(LastStateDto rq, Identity<?> identity) throws Exception {
        mockMvc
                .perform(
                        post("/last-state/{type}/{userId}", identity.getType().getCode(),
                                identity.getStringValue())
                                .header("X-Ya-Service-Ticket", "test")
                                .content(objectMapper.writeValueAsString(rq))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

    public void updateLastState(LastStateDto rq, Identity<?> identity) throws Exception {
        mockMvc
                .perform(
                        put("/last-state/{type}/{userId}", identity.getType().getCode(),
                                identity.getStringValue())
                                .header("X-Ya-Service-Ticket", "test")
                                .content(objectMapper.writeValueAsString(rq))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

    public void deleteLastState(Identity<?> identity) throws Exception {
        mockMvc
                .perform(
                        delete("/last-state/{type}/{userId}", identity.getType().getCode(),
                                identity.getStringValue())
                                .header("X-Ya-Service-Ticket", "test"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

}
