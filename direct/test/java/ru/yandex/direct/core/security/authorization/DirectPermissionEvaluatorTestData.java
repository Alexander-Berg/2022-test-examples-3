package ru.yandex.direct.core.security.authorization;

import java.util.Arrays;
import java.util.EnumSet;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.ClientPerm;
import ru.yandex.direct.rbac.RbacRole;

class DirectPermissionEvaluatorTestData {

    static final Long SUPER_UID = 10L;
    static final Long SUPPORT_UID = 11L;
    static final Long PLACER_UID = 12L;
    static final Long MEDIA_UID = 13L;
    static final Long MANAGER_UID = 14L;
    static final Long AGENCY_UID = 15L;
    static final Long SIMPLE_CLIENT_UID = 16L;
    static final Long SERVICED_CLIENT_UID = 25L;
    static final Long SUBCLIENT_UID = 17L;
    static final Long SUPER_SUBCLIENT_UID = 18L;
    static final Long SUPER_READER_UID = 23L;
    static final Long SUPER_READER_CLIENT_ID = 24L;

    private static final Long INTERNAL_AD_PRODUCT_UID = 25L;
    static final Long INTERNAL_AD_PRODUCT_CLIENT_ID = 26L;
    private static final Long NOT_INTERNAL_AD_PRODUCT_UID = 123L;
    private static final Long NOT_INTERNAL_AD_PRODUCT_CLIENT_ID = 124L;
    private static final Long INTERNAL_AD_ADMIN_UID = 27L;
    private static final Long INTERNAL_AD_ADMIN_CLIENT_ID = 28L;
    private static final Long INTERNAL_AD_SUPERREADER_UID = 29L;
    private static final Long INTERNAL_AD_SUPERREADER_CLIENT_ID = 30L;
    private static final Long INTERNAL_AD_MANAGER_UID = 31L;
    static final Long INTERNAL_AD_MANAGER_CLIENT_ID = 32L;

    static final Long MCC_CONTROL_UID = 200L;
    static final Long MCC_CONTROL_CLIENT_ID = 201L;
    static final Long MCC_MANAGED_UID = 202L;
    static final Long MCC_MANAGED_CLIENT_ID = 203L;

    static final Long SIMPLE_CLIENT_ID = 19L;
    static final ClientId SERVICED_CLIENT_ID = ClientId.fromLong(20L);
    static final ClientId SUBCLIENT_CLIENT_ID = ClientId.fromLong(21L);
    static final ClientId SUPER_SUBCLIENT_CLIENT_ID = ClientId.fromLong(22L);

    private static final User SUPER_USER = createUser(RbacRole.SUPER, SUPER_UID, 34589L);
    private static final User SUPPORT = createUser(RbacRole.SUPPORT, SUPPORT_UID, 23904289L);
    private static final User PLACER = createUser(RbacRole.PLACER, PLACER_UID, 23904289L);
    private static final User MEDIA = createUser(RbacRole.MEDIA, MEDIA_UID, 34589L);
    private static final User MANAGER = createUser(RbacRole.MANAGER, MANAGER_UID, 123890L);
    private static final User AGENCY = createUser(RbacRole.AGENCY, AGENCY_UID, 123890L);
    private static final User SIMPLE_CLIENT = createUser(RbacRole.CLIENT, SIMPLE_CLIENT_UID, SIMPLE_CLIENT_ID);
    private static final User SERVICED_CLIENT = createUser(RbacRole.CLIENT, SERVICED_CLIENT_UID, SERVICED_CLIENT_ID);
    private static final User SUBCLIENT = createUser(RbacRole.CLIENT, SUBCLIENT_UID, SUBCLIENT_CLIENT_ID);
    private static final User SUPER_READER = createUser(RbacRole.SUPERREADER, SUPER_READER_UID, SUPER_READER_CLIENT_ID);
    private static final User MCC_CONTROL_CLIENT = createUser(RbacRole.CLIENT, MCC_CONTROL_UID, MCC_CONTROL_CLIENT_ID);
    private static final User MCC_MANAGED_CLIENT = createUser(RbacRole.CLIENT, MCC_MANAGED_UID, MCC_MANAGED_CLIENT_ID);

    private static final User INTERNAL_AD_PRODUCT =
            createUser(RbacRole.CLIENT, INTERNAL_AD_PRODUCT_UID, INTERNAL_AD_PRODUCT_CLIENT_ID)
                    .withPerms(EnumSet.of(ClientPerm.INTERNAL_AD_PRODUCT));
    private static final User NOT_INTERNAL_AD_PRODUCT =
            createUser(RbacRole.CLIENT, NOT_INTERNAL_AD_PRODUCT_UID, NOT_INTERNAL_AD_PRODUCT_CLIENT_ID)
                    .withPerms(EnumSet.noneOf(ClientPerm.class));
    private static final User INTERNAL_AD_ADMIN =
            createUser(RbacRole.INTERNAL_AD_ADMIN, INTERNAL_AD_ADMIN_UID, INTERNAL_AD_ADMIN_CLIENT_ID);
    private static final User INTERNAL_AD_SUPERREADER =
            createUser(RbacRole.INTERNAL_AD_SUPERREADER, INTERNAL_AD_SUPERREADER_UID,
                    INTERNAL_AD_SUPERREADER_CLIENT_ID);
    private static final User INTERNAL_AD_MANAGER =
            createUser(RbacRole.INTERNAL_AD_MANAGER, INTERNAL_AD_MANAGER_UID, INTERNAL_AD_MANAGER_CLIENT_ID);

    static final User SUPER_SUBCLIENT = createUser(RbacRole.CLIENT, SUPER_SUBCLIENT_UID, SUPER_SUBCLIENT_CLIENT_ID);

    static Iterable<Object[]> provideData() {

        return Arrays.asList(new Object[][]{
                // role super
                {SUPER_USER, SIMPLE_CLIENT, null, Permission.WRITE, true},
                {SUPER_USER, SIMPLE_CLIENT, null, Permission.READ, true},
                {SUPER_USER, SIMPLE_CLIENT, null, Permission.WRITE_SETTINGS, true},

                // role support
                {SUPPORT, SIMPLE_CLIENT, null, Permission.WRITE, true},
                {SUPPORT, SIMPLE_CLIENT, null, Permission.READ, true},
                {SUPPORT, SIMPLE_CLIENT, null, Permission.WRITE_SETTINGS, true},


                // role placer
                {PLACER, SIMPLE_CLIENT, null, Permission.WRITE, false},
                {PLACER, SIMPLE_CLIENT, null, Permission.READ, true},
                {PLACER, SIMPLE_CLIENT, null, Permission.WRITE_SETTINGS, false},


                // role mediaplanner and client
                {MEDIA, SIMPLE_CLIENT, null, Permission.WRITE, false},
                {MEDIA, SIMPLE_CLIENT, null, Permission.READ, true},
                {MEDIA, SIMPLE_CLIENT, null, Permission.WRITE_SETTINGS, false},


                // role manager and simple client
                {MANAGER, SIMPLE_CLIENT, null, Permission.WRITE, false},
                {MANAGER, SIMPLE_CLIENT, null, Permission.READ, true},
                {MANAGER, SIMPLE_CLIENT, null, Permission.WRITE_SETTINGS, false},


                // role manager and serviced client
                {MANAGER, SERVICED_CLIENT, null, Permission.WRITE, true},
                {MANAGER, SERVICED_CLIENT, null, Permission.READ, true},
                {MANAGER, SERVICED_CLIENT, null, Permission.WRITE_SETTINGS, true},

                // internal roles

                // role agency and subclient
                {AGENCY, SUBCLIENT, null, Permission.WRITE, true},
                {AGENCY, SUBCLIENT, null, Permission.READ, true},
                {AGENCY, SUBCLIENT, null, Permission.WRITE_SETTINGS, true},

                // role agency and simple client
                {AGENCY, SIMPLE_CLIENT, null, Permission.WRITE, false},
                {AGENCY, SIMPLE_CLIENT, null, Permission.READ, false},
                {AGENCY, SIMPLE_CLIENT, null, Permission.WRITE_SETTINGS, false},

                // role client with itself
                {SIMPLE_CLIENT, SIMPLE_CLIENT, null, Permission.WRITE, true},
                {SIMPLE_CLIENT, SIMPLE_CLIENT, null, Permission.READ, true},
                {SIMPLE_CLIENT, SIMPLE_CLIENT, null, Permission.WRITE_SETTINGS, true},

                // role client with another client
                {SIMPLE_CLIENT, SUBCLIENT, null, Permission.WRITE, false},
                {SIMPLE_CLIENT, SUBCLIENT, null, Permission.READ, false},
                {SIMPLE_CLIENT, SUBCLIENT, null, Permission.WRITE_SETTINGS, false},

                // role subclient with itself
                {SUBCLIENT, SUBCLIENT, null, Permission.WRITE, false},
                {SUBCLIENT, SUBCLIENT, null, Permission.READ, true},
                {SUBCLIENT, SUBCLIENT, null, Permission.WRITE_SETTINGS, true},

                // role super-subclient with itself
                {SUPER_SUBCLIENT, SUPER_SUBCLIENT, null, Permission.WRITE, true},
                {SUPER_SUBCLIENT, SUPER_SUBCLIENT, null, Permission.READ, true},
                {SUPER_SUBCLIENT, SUPER_SUBCLIENT, null, Permission.WRITE_SETTINGS, true},

                // role super-subclient with itself
                {SUPER_READER, SIMPLE_CLIENT, null, Permission.WRITE, false},
                {SUPER_READER, SIMPLE_CLIENT, null, Permission.READ, true},
                {SUPER_READER, SIMPLE_CLIENT, null, Permission.WRITE_SETTINGS, false},

                //internal-ad
                {INTERNAL_AD_ADMIN, INTERNAL_AD_ADMIN, null, Permission.WRITE, true},
                {INTERNAL_AD_ADMIN, INTERNAL_AD_PRODUCT, null, Permission.WRITE, true},
                {INTERNAL_AD_ADMIN, NOT_INTERNAL_AD_PRODUCT, null, Permission.WRITE, false},
                {INTERNAL_AD_ADMIN, NOT_INTERNAL_AD_PRODUCT, null, Permission.READ, false},

                {INTERNAL_AD_SUPERREADER, INTERNAL_AD_SUPERREADER, null, Permission.WRITE, true},
                {INTERNAL_AD_SUPERREADER, INTERNAL_AD_PRODUCT, null, Permission.READ, true},
                {INTERNAL_AD_SUPERREADER, INTERNAL_AD_PRODUCT, null, Permission.WRITE, false},
                {INTERNAL_AD_SUPERREADER, NOT_INTERNAL_AD_PRODUCT, null, Permission.READ, false},

                {INTERNAL_AD_MANAGER, INTERNAL_AD_MANAGER, null, Permission.WRITE, true},
                {INTERNAL_AD_MANAGER, INTERNAL_AD_PRODUCT, null, Permission.READ, true},
                {INTERNAL_AD_MANAGER, INTERNAL_AD_PRODUCT, null, Permission.WRITE, false},
                {INTERNAL_AD_MANAGER, NOT_INTERNAL_AD_PRODUCT, null, Permission.READ, false},

                {MCC_CONTROL_CLIENT, SIMPLE_CLIENT, null, Permission.READ, false},
                {MCC_CONTROL_CLIENT, SIMPLE_CLIENT, null, Permission.WRITE, false},
                {MCC_CONTROL_CLIENT, SIMPLE_CLIENT, null, Permission.WRITE_SETTINGS, false},

                {MCC_CONTROL_CLIENT, MCC_MANAGED_CLIENT, null, Permission.READ, true},
                {MCC_CONTROL_CLIENT, MCC_MANAGED_CLIENT, null, Permission.WRITE, true},
                {MCC_CONTROL_CLIENT, MCC_MANAGED_CLIENT, null, Permission.WRITE_SETTINGS, true},

                {MCC_CONTROL_CLIENT, SUBCLIENT, null, Permission.READ, true},
                {MCC_CONTROL_CLIENT, SUBCLIENT, null, Permission.WRITE, false},
                {MCC_CONTROL_CLIENT, SUBCLIENT, null, Permission.WRITE_SETTINGS, true},

                {MCC_CONTROL_CLIENT, SUPER_SUBCLIENT, null, Permission.READ, true},
                {MCC_CONTROL_CLIENT, SUPER_SUBCLIENT, null, Permission.WRITE, true},
                {MCC_CONTROL_CLIENT, SUPER_SUBCLIENT, null, Permission.WRITE_SETTINGS, true},
        });
    }

    private static User createUser(RbacRole role, long uid, ClientId clientId) {
        return new UserPrintingDescription()
                .withUid(uid)
                .withLogin("login")
                .withClientId(clientId)
                .withRole(role)
                .withIsReadonlyRep(false)
                .withAutobanned(false);
    }

    private static User createUser(RbacRole role, long uid, long clientId) {
        return createUser(role, uid, ClientId.fromLong(clientId));
    }

    private static class UserPrintingDescription extends User {
        @Override
        public String toString() {
            return String.format("%s(uid=%d)", getRole().name(), getUid());
        }
    }

}
