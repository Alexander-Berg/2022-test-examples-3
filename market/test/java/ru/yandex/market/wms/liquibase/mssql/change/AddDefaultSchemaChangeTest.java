package ru.yandex.market.wms.liquibase.mssql.change;

import java.sql.SQLException;
import java.sql.Statement;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddDefaultSchemaChangeTest {

    @Mock
    Database mockDatabase;

    @Mock
    JdbcConnection mockConnection;

    @Mock
    Statement mockStatement;

//    @Test
//    public void shouldAddDefaultSchema() throws CustomChangeException, DatabaseException, SQLException {
//        String expected = "ALTER USER [test-username] WITH DEFAULT_SCHEMA = test-schema;";
//
//        when(mockDatabase.getConnection()).thenReturn(mockConnection);
//        when(mockConnection.createStatement()).thenReturn(mockStatement);
//
//        AddDefaultSchemaChange change = new AddDefaultSchemaChange();
//        change.setSchema("test-schema");
//        change.setUsername("test-username");
//        change.generateStatements(mockDatabase);
//
//        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
//        verify(mockStatement, times(1)).execute(queryCaptor.capture());
//
//        String actual = queryCaptor.getValue();
//
//        Assertions.assertEquals(expected, actual);
//    }
//
//    @Test
//    public void shouldAddDefaultSchema1() {
//        AddDefaultSchemaChange change = new AddDefaultSchemaChange();
//        change.setSchema("${test-schema}");
//        assertThrows(CustomChangeException.class, () -> change.generateRollbackStatements(mockDatabase));
//    }


}