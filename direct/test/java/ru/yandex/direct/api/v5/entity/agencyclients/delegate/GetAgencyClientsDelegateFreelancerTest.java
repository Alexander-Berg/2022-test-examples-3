package ru.yandex.direct.api.v5.entity.agencyclients.delegate;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.agencyclients.AgencyClientsSelectionCriteria;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.generalclients.ClientGetItem;
import com.yandex.direct.api.v5.generalclients.GrantGetItem;
import com.yandex.direct.api.v5.generalclients.PrivilegeEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.entity.agencyclients.service.RequestedField;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.security.utils.ApiAuthenticationSourceMockBuilder;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.ApiUserRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.rbac.RbacClientsRelations;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@Api5Test
@RunWith(SpringRunner.class)
public class GetAgencyClientsDelegateFreelancerTest {
    @Autowired
    Steps steps;

    @Autowired
    ApiUserRepository apiUserRepository;
    @Autowired
    RbacClientsRelations rbacClientsRelations;
    @Autowired
    ApiAuthenticationSource apiAuthenticationSourceMock;

    @Autowired
    GetAgencyClientsDelegate testedGetDelegate;

    private ClientInfo customer;
    private FreelancerInfo freelancer;


    @Before
    public void setUp() {
        shiftUserIdSequence();
        customer = steps.clientSteps().createDefaultClient();
        freelancer = steps.freelancerSteps().addDefaultFreelancer();

        rbacClientsRelations.addFreelancerRelation(customer.getClientId(), freelancer.getClientId());

        ApiUser freelancerOperator =
                apiUserRepository.fetchByUid(freelancer.getShard(), freelancer.getClientInfo().getUid());

        new ApiAuthenticationSourceMockBuilder()
                .withOperator(freelancerOperator)
                .tuneAuthSourceMock(apiAuthenticationSourceMock);
    }

    /**
     * Получает Uid из генератора, что приводит к расхождению ClientId и UserId при создании их парой
     */
    private void shiftUserIdSequence() {
        steps.userSteps().generateNewUserUid();
    }

    @Test
    public void get_responseNotEmpty() {
        Set<RequestedField> requestedField = EnumSet.allOf(RequestedField.class).stream()
                .filter(r -> r != RequestedField.NOTIFICATION && r != RequestedField.PHONE)
                .collect(Collectors.toSet());
        GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> request =
                getRequest(requestedField);

        List<ClientGetItem> itemsReturned = testedGetDelegate.get(request);

        ClientGetItem expected = new ClientGetItem()
                .withType("CLIENT")
                .withArchived(YesNoEnum.NO);
        BeanDifferMatcher<ClientGetItem> matcher = beanDiffer(expected)
                .useCompareStrategy(onlyFields(newPath("type"), newPath("archived")));

        assertThat(itemsReturned).isNotEmpty();
        assertThat(itemsReturned).first()
                .is(matchedBy(matcher));
    }

    @Test
    public void get_responseContainsCorrectType() {
        GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> request =
                getRequest(RequestedField.TYPE);

        List<ClientGetItem> itemsReturned = testedGetDelegate.get(request);

        assertThat(itemsReturned).first()
                .extracting(ClientGetItem::getType)
                .isEqualTo("CLIENT");
    }

    @Test
    public void get_responseContainsCorrectArchived() {
        GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> request =
                getRequest(RequestedField.ARCHIVED);

        List<ClientGetItem> itemsReturned = testedGetDelegate.get(request);

        assertThat(itemsReturned).first()
                .extracting(ClientGetItem::getArchived)
                .isEqualTo(YesNoEnum.NO);
    }

    @Test
    public void get_responseContainsCorrectGrants() {
        GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> request =
                getRequest(RequestedField.GRANTS);

        List<ClientGetItem> itemsReturned = testedGetDelegate.get(request);

        assertThat(itemsReturned).isNotEmpty();
        assertThat(itemsReturned.get(0).getGrants())
                .usingFieldByFieldElementComparator()
                .withFailMessage("Free Client should have all grants: EDIT_CAMPAIGNS, IMPORT_XLS, TRANSFER_MONEY")
                .containsOnly(
                        new GrantGetItem().withPrivilege(PrivilegeEnum.EDIT_CAMPAIGNS).withValue(YesNoEnum.YES),
                        new GrantGetItem().withPrivilege(PrivilegeEnum.IMPORT_XLS).withValue(YesNoEnum.YES),
                        new GrantGetItem().withPrivilege(PrivilegeEnum.TRANSFER_MONEY).withValue(YesNoEnum.YES)
                );
    }

    @Test
    public void get_withArchivedNoInSelectionCriteria() {
        AgencyClientsSelectionCriteria agencyClientsSelectionCriteria = new AgencyClientsSelectionCriteria();
        agencyClientsSelectionCriteria.setArchived(YesNoEnum.NO);
        GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> request =
                getRequest(EnumSet.of(RequestedField.CLIENT_ID, RequestedField.TYPE, RequestedField.ARCHIVED),
                        agencyClientsSelectionCriteria);

        List<ClientGetItem> itemsReturned = testedGetDelegate.get(request);

        ClientGetItem expected = new ClientGetItem()
                .withClientId(customer.getClientId().asLong())
                .withType("CLIENT")
                .withArchived(YesNoEnum.NO);
        BeanDifferMatcher<ClientGetItem> matcher = beanDiffer(expected)
                .useCompareStrategy(onlyFields(newPath("clientId"), newPath("type"), newPath("archived")));

        assertThat(itemsReturned).isNotEmpty();
        assertThat(itemsReturned).first()
                .is(matchedBy(matcher));
    }

    @Test
    public void get_withArchivedYesInSelectionCriteria() {
        // Создадим архивного клиента
        User customerUser = generateNewUser()
                .withStatusArch(true);
        UserInfo userInfo = steps.userSteps().createUser(customerUser);
        customer = userInfo.getClientInfo();
        rbacClientsRelations.addFreelancerRelation(customer.getClientId(), freelancer.getClientId());

        AgencyClientsSelectionCriteria agencyClientsSelectionCriteria = new AgencyClientsSelectionCriteria();
        agencyClientsSelectionCriteria.setArchived(YesNoEnum.YES);
        GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> request =
                getRequest(EnumSet.of(RequestedField.CLIENT_ID, RequestedField.TYPE, RequestedField.ARCHIVED),
                        agencyClientsSelectionCriteria);

        List<ClientGetItem> itemsReturned = testedGetDelegate.get(request);

        ClientGetItem expected = new ClientGetItem()
                .withClientId(customer.getClientId().asLong())
                .withType("CLIENT")
                .withArchived(YesNoEnum.YES);
        BeanDifferMatcher<ClientGetItem> matcher = beanDiffer(expected)
                .useCompareStrategy(onlyFields(newPath("clientId"), newPath("type"), newPath("archived")));

        assertThat(itemsReturned).isNotEmpty();
        assertThat(itemsReturned).first()
                .is(matchedBy(matcher));
    }

    @Test
    public void get_withLoginsInSelectionCriteria() {
        AgencyClientsSelectionCriteria agencyClientsSelectionCriteria = new AgencyClientsSelectionCriteria();
        agencyClientsSelectionCriteria.setLogins(singletonList(customer.getLogin()));
        GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> request =
                getRequest(EnumSet.of(RequestedField.CLIENT_ID, RequestedField.TYPE, RequestedField.ARCHIVED),
                        agencyClientsSelectionCriteria);

        List<ClientGetItem> itemsReturned = testedGetDelegate.get(request);

        ClientGetItem expected = new ClientGetItem()
                .withClientId(customer.getClientId().asLong())
                .withType("CLIENT")
                .withArchived(YesNoEnum.NO);
        BeanDifferMatcher<ClientGetItem> matcher = beanDiffer(expected)
                .useCompareStrategy(onlyFields(newPath("clientId"), newPath("type"), newPath("archived")));

        assertThat(itemsReturned).isNotEmpty();
        assertThat(itemsReturned).first()
                .is(matchedBy(matcher));
    }

    @Test
    public void get_emptyResponse_whenNoArchivedCustomers() {
        AgencyClientsSelectionCriteria agencyClientsSelectionCriteria = new AgencyClientsSelectionCriteria();
        agencyClientsSelectionCriteria.setArchived(YesNoEnum.YES);
        GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> request =
                getRequest(EnumSet.of(RequestedField.CLIENT_ID, RequestedField.TYPE, RequestedField.ARCHIVED),
                        agencyClientsSelectionCriteria);

        List<ClientGetItem> itemsReturned = testedGetDelegate.get(request);

        assertThat(itemsReturned).isEmpty();
    }

    @Test
    public void get_emptyResponse_withAbsentLoginsInSelectionCriteria() {
        AgencyClientsSelectionCriteria agencyClientsSelectionCriteria = new AgencyClientsSelectionCriteria();
        agencyClientsSelectionCriteria.setLogins(singletonList("absent-login"));
        GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> request =
                getRequest(EnumSet.of(RequestedField.CLIENT_ID, RequestedField.TYPE, RequestedField.ARCHIVED),
                        agencyClientsSelectionCriteria);

        List<ClientGetItem> itemsReturned = testedGetDelegate.get(request);

        assertThat(itemsReturned).isEmpty();
    }

    @Test
    public void get_allFreelancerClients() {
        // подготавливаем данные
        ClientInfo secondCustomer = steps.clientSteps().createDefaultClient();
        rbacClientsRelations.addFreelancerRelation(secondCustomer.getClientId(), freelancer.getClientId());

        Long customer1Id = customer.getClientId().asLong();
        Long customer2Id = secondCustomer.getClientId().asLong();

        //выполняем запрос
        GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> request =
                getRequest(RequestedField.CLIENT_ID);
        List<ClientGetItem> itemsReturned = testedGetDelegate.get(request);

        //сверяем ожидания и реальность
        assertThat(itemsReturned).extracting(ClientGetItem::getClientId)
                .containsOnly(customer1Id, customer2Id);
    }

    // Утилитные методы

    private GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> getRequest(
            RequestedField requestedField) {
        return getRequest(EnumSet.of(requestedField));
    }

    private GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> getRequest(
            Set<RequestedField> requestedFields) {
        return getRequest(requestedFields, new AgencyClientsSelectionCriteria());
    }

    private GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> getRequest(
            Set<RequestedField> requestedFields, AgencyClientsSelectionCriteria agencyClientsSelectionCriteria) {
        return new GenericGetRequest<>(
                requestedFields,
                agencyClientsSelectionCriteria,
                LimitOffset.maxLimited());
    }
}
