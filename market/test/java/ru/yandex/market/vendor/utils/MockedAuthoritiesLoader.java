package ru.yandex.market.vendor.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.market.security.AuthoritiesLoader;
import ru.yandex.market.security.model.Authority;
import ru.yandex.market.security.model.OperationAuthorities;
import ru.yandex.market.security.model.OperationPermission;
import ru.yandex.market.vendor.sec.OperationRolesService;

import static ru.yandex.cs.billing.CsBillingCoreConstants.VENDOR_IDM_DOMAIN;

public class MockedAuthoritiesLoader implements AuthoritiesLoader {

    private final OperationRolesService operationRolesService;
    private final Map<String, OperationAuthorities> operationAuthorities = new HashMap<>();
    private long id = 0;

    public MockedAuthoritiesLoader(OperationRolesService operationRolesService) {
        this.operationRolesService = operationRolesService;
    }


    @Override
    public OperationAuthorities load(String domain, String operationName) {
        if (!operationAuthorities.containsKey(operationName)) {
            updateMappings();
        }
        return operationAuthorities.get(operationName);
    }

    private void updateMappings() {
        operationRolesService.getJavaSecOperationRoles()
                .forEach((k, v) -> operationAuthorities.put(k, createOperationAuthorities(k, v)));
    }

    private OperationAuthorities createOperationAuthorities(String operationName, List<String> authorities) {
        OperationAuthorities operationAuthorities = new OperationAuthorities(operationName);
        authorities.forEach(auth -> operationAuthorities.addPermission(createOperationPermission(operationName, auth)));
        return operationAuthorities;
    }

    private OperationPermission createOperationPermission(String operationName, String authority) {
        OperationPermission operationPermission = new OperationPermission();
        operationPermission.setOperationName(operationName);
        operationPermission.setDomain(VENDOR_IDM_DOMAIN);
        Authority.Builder builder = new Authority.Builder();
        builder.setName(authority);
        builder.setId(id++);
        builder.setChecker("staticDomainAuthorityChecker");
        builder.setDomain(VENDOR_IDM_DOMAIN);
        builder.setParams("");
        operationPermission.setAuthorities(List.of(builder.build()));
        return operationPermission;
    }
}
