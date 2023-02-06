package ru.yandex.market.crm.core.test.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.zip.GZIPOutputStream;

import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.yt.paths.CrmYtTables;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.platform.models.Email;
import ru.yandex.market.crm.platform.models.EmailOwnership;
import ru.yandex.market.crm.platform.models.ExecutedAction;
import ru.yandex.market.crm.platform.models.GenericSubscription;
import ru.yandex.market.crm.platform.models.MetrikaMobileApp;
import ru.yandex.market.crm.platform.models.MobileAppInfo;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.PushTokenStatuses;
import ru.yandex.market.crm.platform.models.Subscription;
import ru.yandex.market.crm.util.logging.LogBuilder;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

/**
 * @author apershukov
 */
@Component
public class YtSchemaTestHelper implements StatefulHelper {

    private static final Logger LOG = LoggerFactory.getLogger(YtSchemaTestHelper.class);

    private final YtClient ytClient;
    private final CrmYtTables ytTables;
    private final YtTestTables ytTestTables;
    private final YtFolders ytFolders;
    private final CommunicationTables communicationTables;

    private final Stack<YPath> createdTables;

    YtSchemaTestHelper(YtClient ytClient,
                       CrmYtTables ytTables,
                       YtTestTables ytTestTables,
                       YtFolders ytFolders,
                       CommunicationTables communicationTables) {
        this.ytClient = ytClient;
        this.ytTables = ytTables;
        this.ytTestTables = ytTestTables;
        this.ytFolders = ytFolders;
        this.communicationTables = communicationTables;
        this.createdTables = new Stack<>();
    }

    private static DescriptorProtos.FileDescriptorSet buildDescriptorSet(Descriptors.Descriptor descriptor) {
        List<Descriptors.FileDescriptor> flat = new ArrayList<>();

        ArrayDeque<Descriptors.FileDescriptor> queue = new ArrayDeque<>();
        queue.add(descriptor.getFile());

        while (!queue.isEmpty()) {
            Descriptors.FileDescriptor file = queue.pop();
            queue.addAll(file.getDependencies());
            flat.add(file);
        }

        DescriptorProtos.FileDescriptorSet.Builder builder = DescriptorProtos.FileDescriptorSet.newBuilder();
        Set<Descriptors.FileDescriptor> visited = new HashSet<>();
        for (Descriptors.FileDescriptor file : Lists.reverse(flat)) {
            if (visited.add(file)) {
                builder.addFile(file.toProto());
            }
        }

        return builder.build();
    }

    private static String getDescriptor(Descriptors.Descriptor descriptor) {
        DescriptorProtos.FileDescriptorSet set = buildDescriptorSet(descriptor);
        byte[] originalBytes = set.toByteArray();

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (OutputStream out = new GZIPOutputStream(byteOut)) {
            IOUtils.copy(new ByteArrayInputStream(originalBytes), out);
        } catch (IOException e) {
            throw new RuntimeException("Exception on generating proto descriptor", e);
        }
        String protoDescriptor = Base64.getEncoder().encodeToString(byteOut.toByteArray());
        return '#' + descriptor.getFullName() + '+' + protoDescriptor;
    }

    public void prepareSubscriptionFactsTable() {
        prepareFactsTable(ytTables.getSubscriptions(), "string_key_fact.yson", Subscription.getDescriptor());
    }

    public void prepareEmailFactsTable() {
        prepareFactsTable(ytTables.getEmailTable(), "email_facts.yson", Email.getDescriptor());
    }

    public void prepareExecutedActionFactsTable() {
        prepareFactsTable(
                ytTestTables.getExecutedActions(),
                "timestamp_key_fact.yson",
                ExecutedAction.getDescriptor()
        );
    }

    public void prepareEmailOwnershipFactsTable() {
        prepareFactsTable(
                ytTestTables.getEmailOwnership(),
                "string_key_fact.yson",
                EmailOwnership.getDescriptor()
        );
    }

    public void prepareUserTables() {
        createTable(ytTables.getUsers(), "users.yson");
        ytClient.setYqlScheme(ytTables.getUsers(), "/yt/schemas/users.spec.yson");

        createTable(ytTables.getUserIds(), "user_ids.yson");
        ytClient.setYqlScheme(ytTables.getUserIds(), "/yt/schemas/user_ids.spec.yson");

        var supportedIdTypes = Set.of(UidType.EMAIL, UidType.YUID, UidType.PUID, UidType.UUID);

        for (var sourceType : supportedIdTypes) {
            for (var targetType : supportedIdTypes) {
                if (sourceType == targetType) {
                    continue;
                }

                createChytLinksTable(LinkingMode.ALL, sourceType, targetType);
                createChytLinksTable(LinkingMode.DIRECT_ONLY, sourceType, targetType);
            }
        }
    }

    public void preparePassportProfilesTable() {
        createTable(ytTestTables.getPassportProfiles(), "passport_userdata.yson");
    }

    public void prepareModelInfoTable() {
        createTable(ytTestTables.getModelInfo(), "model_info.yson");
    }

    public void prepareModelStatTable() {
        createTable(ytTestTables.getModelStat(), "models_stat.yson");
    }

    public void prepareCampaignDir() {
        ytClient.createDirectory(ytFolders.getCampaignsPath(), true, true);
    }

    public void prepareMetrikaAppFactsTable() {
        prepareFactsTable(
                ytTestTables.getMertikaAppFacts(),
                "minimal_key_fact.yson",
                MetrikaMobileApp.getDescriptor()
        );
    }

    public void prepareMobileAppInfoFactsTable() {
        prepareFactsTable(
                ytTestTables.getMobileAppInfoFacts(),
                "mobile_app_info_fact.yson",
                MobileAppInfo.getDescriptor()
        );
    }

    public void prepareGenericSubscriptionFactsTable() {
        prepareFactsTable(
                ytTestTables.getGenericSubscriptionFacts(),
                "generic_subscription_fact.yson",
                GenericSubscription.getDescriptor()
        );
    }

    public void preparePlusDataTable() {
        createTable(ytTestTables.getPlusDataTablePath(), "plus_data.yson");
    }

    public void prepareAccessDataTables() {
        createTable(ytTestTables.getCapiAccessDataTablePath(), "access_data.yson");
        createTable(ytTestTables.getFapiAccessDataTablePath(), "access_data.yson");
    }

    public void prepareCryptaMatchingTable(String sourceType, String targetType) {
        YPath tablePath = ytTestTables.getCryptaMatchingDir().child(sourceType).child(targetType);
        createTable(tablePath, "crypta_matching.yson");
    }

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
        createdTables.forEach(path -> {
            LOG.info(
                    LogBuilder.builder("#yt_schema_helper")
                            .append("Removing table...")
                            .append("PATH", path)
                            .build()
            );
            ytClient.delete(path);
        });

        createdTables.clear();
    }

    public void prepareGlobalControlSplitsTable() {
        prepareDynamicTable(ytTables.getCurrentGlobalSplitsTable(), "global_control/uniform_global_splits_table.yson");
    }

    public void prepareLoyaltyActiveAuthCoinsTable() {
        createTable(ytTables.getLoyaltyActiveAuthCoinsTable(), "active_auth_coins.yson");
    }

    public void createTable(YPath path, String schema) {
        ytClient.createTable(path, schema);
        createdTables.push(path);
    }

    public void preparePushTokenStatusesTable() {
        prepareFactsTable(
                ytTables.getPushTokenStatusesTable(),
                "fact_with_id.yson",
                PushTokenStatuses.getDescriptor()
        );
    }

    public void prepareChytPassportEmailsTable() {
        createTable(ytTestTables.getChytPassportEmails(), "chyt_passport_emails.yson");
    }

    public void prepareChytPassportUuidsTable() {
        createTable(ytTestTables.getChytPassportUuids(), "chyt_passport_uuids.yson");
    }

    public void prepareChytUuidsWithTokensTable(YPath tablePath) {
        createTable(tablePath, "chyt_uuids_with_tokens.yson");
    }

    public void prepareChytUuidsWithTokensTable() {
        prepareChytUuidsWithTokensTable(ytTestTables.getChytUuidsWithTokens());
    }

    public void prepareEmailsGeoInfo() {
        createTable(ytTestTables.getEmailsGeoInfo(), "emails_geo_info.yson");
    }

    public void prepareChytUuidsWithSubscriptionsTable() {
        createTable(ytTestTables.getChytUuidsWithSubscriptions(), "chyt_uuids_with_subscriptions.yson");
    }

    public void prepareCommunicationsTable() {
        prepareCommunicationTable(communicationTables.pushTable());
        prepareCommunicationTable(communicationTables.emailTable());
    }

    public void prepareFactsTable(YPath path, String schema, Descriptors.Descriptor descriptor) {
        createTable(path, schema);
        ytClient.setAttribute(
                path,
                "_yql_proto_field_fact",
                YTree.stringNode(getDescriptor(descriptor))
        );
    }

    void prepareOrderFactsTable(String path) {
        prepareFactsTable(YPath.simple(path), "order_facts.yson", Order.getDescriptor());
    }

    private void prepareCommunicationTable(YPath tablePath) {
        createTable(tablePath, "communications.yson");
        ytClient.makeDynamicWithDefaultBundle(tablePath);
    }

    private void prepareDynamicTable(YPath path, String schema) {
        createTable(path, schema);
        ytClient.makeDynamic(path);
    }

    private void createChytLinksTable(LinkingMode mode, UidType sourceType, UidType targetType) {
        var path = ytTables.getLinksTable(mode, sourceType, targetType);
        createTable(path, "chyt_links_table.yson");
    }
}
