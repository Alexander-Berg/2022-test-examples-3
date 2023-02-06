package ru.yandex.market.mbo.integration.test.orchestrator;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class DatabaseGroup {
    private final int groupId;
    private final UUID clientGuid;
    private final Map<String, Schema> schemaMap = new HashMap<>();

    private boolean reuseTestSchemas;

    public DatabaseGroup(int groupId, @Nonnull UUID clientGuid) {
        Preconditions.checkNotNull(clientGuid);
        this.groupId = groupId;
        this.clientGuid = clientGuid;
    }

    public int getGroupId() {
        return groupId;
    }

    public UUID getClientGuid() {
        return clientGuid;
    }

    public List<String> getSchemaNames() {
        return schemaMap.values().stream()
            .map(schema -> schema.schemaName)
            .collect(Collectors.toList());
    }

    public void addSchema(String schema, String schemaName, String username, String password) {
        Schema testSchema = new Schema(schemaName, username, password);
        schemaMap.put(schema, testSchema);
    }

    public String getSchemeName(String schema) {
        return schemaMap.get(schema).schemaName;
    }

    public String getSchemeUserName(String schema) {
        return schemaMap.get(schema).username;
    }

    public String getSchemePassword(String schema) {
        return schemaMap.get(schema).password;
    }

    public boolean reuseTestSchemas() {
        return reuseTestSchemas;
    }

    public void setReuseTestSchemas(boolean reuseTestSchemas) {
        this.reuseTestSchemas = reuseTestSchemas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DatabaseGroup that = (DatabaseGroup) o;
        return groupId == that.groupId &&
            clientGuid.equals(that.clientGuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, clientGuid);
    }

    @Override
    public String toString() {
        return "DatabaseGroup{" +
            "groupId=" + groupId +
            ", clientGuid=" + clientGuid +
            ", schemas=" + schemaMap.values().stream()
            .map(schema -> schema.schemaName).collect(Collectors.joining(", ")) +
            ", reuseTestSchemas=" + reuseTestSchemas +
            '}';
    }

    private class Schema {
        private final String schemaName;
        private final String username;
        private final String password;

        Schema(String schemaName, String username, String password) {
            this.schemaName = schemaName;
            this.username = username;
            this.password = password;
        }
    }
}
