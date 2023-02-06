package ru.yandex.market.mbo.audit.clickhouse;

import java.util.Objects;

import javax.sql.DataSource;

import org.mockito.Mockito;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class NamedJdbcTemplateMock extends NamedParameterJdbcTemplate {

    private String name;
    private boolean templateEnabled;
    private JdbcOperations operations;

    public NamedJdbcTemplateMock(String name, DataSource dataSource) {
        super(dataSource);
        this.name = name;
        templateEnabled = true;
        operations = Mockito.mock(JdbcOperations.class);
    }

    @Override
    public JdbcOperations getJdbcOperations() {
        return operations;
    }

    @Override
    public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
        if (templateEnabled) {
            return;
        }
        throw new RuntimeException("Host not responding!");
    }

    public void setTemplateEnabled(boolean templateEnabled) {
        this.templateEnabled = templateEnabled;
    }

    @Override
    public String toString() {
        return "NamedJdbcTemplateMock{" +
            "name='" + name + '\'' +
            ", templateEnabled=" + templateEnabled +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NamedJdbcTemplateMock that = (NamedJdbcTemplateMock) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
