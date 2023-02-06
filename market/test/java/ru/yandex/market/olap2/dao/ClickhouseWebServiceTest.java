package ru.yandex.market.olap2.dao;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import ru.yandex.market.olap2.model.DistributedTable;
import ru.yandex.market.olap2.model.UnionTable;
import ru.yandex.market.olap2.util.exceptions.TableNotFoundException;
import ru.yandex.market.olap2.util.exceptions.ValidateException;
import ru.yandex.market.olap2.yt.YtTableService;

import java.util.List;
import java.util.Map;

public class ClickhouseWebServiceTest {
    private ClickhouseDao mockedChDao;

    private ClickhouseWebService mockedImplementation;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final String eventNameRebuild = "event_name_rebuild";

    private final boolean disableLowCardinality = true;

    @Before
    public void setup() {
        mockedChDao = mockChDao();
        YtTableService mockedYtTableService = mockYtService();
        MetadataDao mockedMetadataDao = mockMetadataDao();
        mockedImplementation = new ClickhouseWebService(
                mockedYtTableService, mockedChDao, mockedMetadataDao, eventNameRebuild, disableLowCardinality
        );
    }

    @Test
    public void testGetNotExistingDistributedTableEngine() throws TableNotFoundException {
        thrown.expect(TableNotFoundException.class);
        String notFoundTable = "not_existing_distributed";
        when(
            mockedChDao.getTableEngine(eq(notFoundTable))
        ).thenReturn(null);

        mockedImplementation.getEngineForTable(notFoundTable);
    }

    @Test
    public void testGetExistingTableEngine() throws TableNotFoundException {
        String existingTable = "existing_distributed";
        String engine = "Distributed('cubes', 'my_table_union', rand())";
        when(
                mockedChDao.getTableEngine(eq(existingTable))
        ).thenReturn(engine);
        assertEquals(mockedImplementation.getEngineForTable(existingTable), engine);
    }

    @Test
    public void testGetNotExistingTableEngine() throws TableNotFoundException {
        thrown.expect(TableNotFoundException.class);
        String notFoundTable = "not_existing";
        when(
                mockedChDao.getTableEngine(eq(notFoundTable))
        ).thenReturn(null);

        mockedImplementation.getEngineForTable(notFoundTable);
    }

    @Test
    public void testGetExistingDistributedTableEngine() throws TableNotFoundException {
        String existingTable = "existing_distributed";
        String engine = "Distributed('cubes', 'my_table_union', rand())";
        when(
                mockedChDao.getTableEngine(eq(existingTable))
        ).thenReturn(engine);
        assertEquals(mockedImplementation.getEngineForTable(existingTable), engine);
    }

    @Test
    public void testGetUnionTable() throws TableNotFoundException, RuntimeException {
        String existingTable = "existing_union";
        String mergeRegexp = "^(my_table_union|my_another_union)$";
        String engine = String.format("Merge('cubes', '%s')", mergeRegexp);
        List<String> expectedTables = List.of("my_table_union", "my_another_union");
        when(
                mockedChDao.getTableEngine(eq(existingTable))
        ).thenReturn(engine);
        when(
                mockedChDao.getTableNamesByRegex(eq(mergeRegexp))
        ).thenReturn(expectedTables);
        UnionTable unionTable = mockedImplementation.getUnionTable(existingTable);

        assertEquals(unionTable.getTableName(), existingTable);
        assertEquals(unionTable.getMergeStatement(), engine);
        assertEquals(unionTable.getUnionTables(), expectedTables);
    }

    @Test
    public void testGetUnionTableIfNotMergeEngine() throws TableNotFoundException, RuntimeException {
        thrown.expect(RuntimeException.class);
        String existingTable = "existing_union";
        String mergeRegexp = "^(my_table_union|my_another_union)$";
        String engine = String.format("Dist('cubes', '%s')", mergeRegexp);
        List<String> expectedTables = List.of("my_table_union", "my_another_union");
        when(
                mockedChDao.getTableEngine(eq(existingTable))
        ).thenReturn(engine);
        when(
                mockedChDao.getTableNamesByRegex(eq(mergeRegexp))
        ).thenReturn(expectedTables);
        mockedImplementation.getUnionTable(existingTable);
    }

    @Test
    public void testGetUnionTableIfNotMergeTables() throws TableNotFoundException, RuntimeException {
        thrown.expect(RuntimeException.class);
        String existingTable = "existing_union";
        String mergeRegexp = "^(my_table_union|my_another_union)$";
        String engine = String.format("Merge('cubes', '%s')", mergeRegexp);
        when(
                mockedChDao.getTableEngine(eq(existingTable))
        ).thenReturn(engine);
        when(
                mockedChDao.getTableNamesByRegex(eq(mergeRegexp))
        ).thenReturn(null);
        mockedImplementation.getUnionTable(existingTable);
    }

    @Test
    public void testGetUnionTableIfTableNotExists() throws TableNotFoundException, RuntimeException {
        thrown.expect(TableNotFoundException.class);
        String existingTable = "not_existing_union";
        when(
                mockedChDao.getTableEngine(eq(existingTable))
        ).thenReturn(null);
        mockedImplementation.getUnionTable(existingTable);
    }

    @Test
    public void testGetTablesWithDifferentSchemaWhenSameSchema() {
        String firstTable = "table1";
        Map<String, String> firstTableColumns = ImmutableMap.of(
                "column_1", "string",
                "column_2", "integer"
        );
        when(
                mockedChDao.getColumns(eq(firstTable))
        ).thenReturn(firstTableColumns);
        String secondTable = "table2";
        Map<String, String> secondTableColumns = ImmutableMap.of(
                "column_2", "integer",
                "column_1", "string"
        );
        when(
                mockedChDao.getColumns(eq(secondTable))
        ).thenReturn(secondTableColumns);
        List<String> tableList = List.of(firstTable, secondTable);
        assertTrue(mockedImplementation.getTablesWithDifferentSchema(tableList).isEmpty());
    }

    @Test
    public void testGetTablesWithDifferentSchemaWhenDiffSchema() {
        String firstTable = "table1";
        Map<String, String> firstTableColumns = ImmutableMap.of(
                "column_1", "string",
                "column_2", "integer"
        );
        when(
                mockedChDao.getColumns(eq(firstTable))
        ).thenReturn(firstTableColumns);
        String secondTable = "table2";
        Map<String, String> secondTableColumns = ImmutableMap.of(
                "column_2", "long",  // here differenct
                "column_1", "string"
        );
        when(
                mockedChDao.getColumns(eq(secondTable))
        ).thenReturn(secondTableColumns);
        List<String> tableList = List.of(firstTable, secondTable);
        List<String> expectedDifference = List.of(secondTable);
        assertEquals(mockedImplementation.getTablesWithDifferentSchema(tableList), expectedDifference);
    }

    @Test
    public void testGetTablesWithDifferentSchemaWhenOneTable() {
        String firstTable = "table1";
        Map<String, String> firstTableColumns = ImmutableMap.of(
                "column_1", "string",
                "column_2", "integer"
        );
        when(
                mockedChDao.getColumns(eq(firstTable))
        ).thenReturn(firstTableColumns);

        List<String> tableList = List.of(firstTable);
        assertTrue(mockedImplementation.getTablesWithDifferentSchema(tableList).isEmpty());
    }

    @Test
    public void testCheckUnionTablesStructureForSameSchema() throws ValidateException {
        List<String> inputTables = List.of("table1", "table2");
        ClickhouseWebService spy = spy(mockedImplementation);
        doReturn(List.of()).when(spy).getTablesWithDifferentSchema(eq(inputTables));
        spy.checkUnionTablesStructure(inputTables);
    }

    @Test
    public void testCheckUnionTablesStructureForDiffSchema() throws ValidateException {
        thrown.expect(ValidateException.class);
        List<String> inputTables = List.of("table1", "table2");
        ClickhouseWebService spy = spy(mockedImplementation);
        doReturn(List.of("table2")).when(spy).getTablesWithDifferentSchema(eq(inputTables));
        spy.checkUnionTablesStructure(inputTables);
    }

    @Test
    public void testGetDistributedTableIfNotExists() throws TableNotFoundException, ValidateException {
        thrown.expect(TableNotFoundException.class);
        String notFoundTable = "not_existing_distributed";
        String baseTable = "base_table";
        when(
                mockedChDao.getTableEngine(eq(notFoundTable))
        ).thenReturn(null);

        mockedImplementation.getDistributedTable(notFoundTable, baseTable);
    }

    @Test
    public void testGetDistributedTableIfNotDistributed() throws TableNotFoundException, ValidateException {
        thrown.expect(ValidateException.class);
        String distTable = "existing_distributed";
        String baseTable = "base_table";
        String tableEngine = "Merge('cubes', '^(table1|table2)$')";
        when(
                mockedChDao.getTableEngine(eq(distTable))
        ).thenReturn(tableEngine);

        mockedImplementation.getDistributedTable(distTable, baseTable);
    }

    @Test
    public void testGetDistributedTable() throws TableNotFoundException, ValidateException {
        String distTable = "existing_distributed";
        String baseTable = "base_table";
        String tableEngine = "Distributed('cubes', 'base_table', rand())";
        when(
                mockedChDao.getTableEngine(eq(distTable))
        ).thenReturn(tableEngine);

        DistributedTable distributedTable = mockedImplementation.getDistributedTable(
                distTable,
                baseTable
        );

        assertEquals(distributedTable.getTableName(), distTable);
        assertEquals(distributedTable.getSourceTableName(), baseTable);
        assertEquals(distributedTable.getDistributeStatement(), tableEngine);

    }

    @Test
    public void testCheckAvailableAllShardsIfAllAlive() {
        List<String> deadShards = List.of();
        when(
                mockedChDao.getDeadShards()
        ).thenReturn(deadShards);
        mockedImplementation.checkAvailableAllShards();
    }

    @Test
    public void testCheckAvailableAllShardsIfNotAllAlive() {
        thrown.expect(RuntimeException.class);
        List<String> deadShards = List.of("shard1");
        when(
                mockedChDao.getDeadShards()
        ).thenReturn(deadShards);
        mockedImplementation.checkAvailableAllShards();
    }

    @Test
    public void testUpdateUnionTableFromExisting() throws ValidateException {
        List<String> tablesList = List.of("table1");
        String unionTableName = "table_union";
        UnionTable unionTable = new UnionTable(
                unionTableName,
            "Merge('cubes', '^(table1)$')",
                tablesList
        );
        String tmpTableName = "table_union__tmp";
        UnionTable tmpUnionTable = unionTable.getUnionTableWithAnotherName(tmpTableName);
        Map<String, String> columns = ImmutableMap.of("column1", "type1");
        ClickhouseWebService spy = spy(mockedImplementation);
        doNothing().when(spy).dropTable(anyString());
        when(
                mockedChDao.getColumns(tablesList.get(0))
        ).thenReturn(columns);
        doNothing().when(spy).checkAvailableAllShards();
        doNothing().when(spy).checkUnionTablesStructure(tablesList);
        doNothing().when(spy).createUnionTable(tmpUnionTable);
        InOrder updateOrder = inOrder(spy, mockedChDao);

        spy.recreateUnionTableFromExisting(unionTable);

//        check order of update
        updateOrder.verify(spy).checkAvailableAllShards();
        updateOrder.verify(spy).checkUnionTablesStructure(tablesList);
//        first to delete if exists
        updateOrder.verify(spy).dropTable(tmpTableName);
        updateOrder.verify(spy).createUnionTable(tmpUnionTable);
        updateOrder.verify(mockedChDao).swapNonPartitionedTables(unionTableName, tmpTableName);
//        cleanup old version
        updateOrder.verify(spy).dropTable(tmpTableName);
//        Check what we NEVER delete no tmp table
        updateOrder.verify(spy, never()).dropTable(unionTableName);
    }

    @Test
    public void testUpdateDistributedTableFromExisting() {
        String distributedTableName = "table_union_distributed";
        String sourceTableName = "table_union";
        DistributedTable distributedTable = new DistributedTable(
                distributedTableName,
                sourceTableName,
                "Distributed('cubes', 'table_union', rand())"
        );
        String tmpTableName = "table_union_distributed__tmp";
        DistributedTable tmpDistributedTable = distributedTable.getDistributedTableWithAnotherName(
                tmpTableName
        );
        ClickhouseWebService spy = spy(mockedImplementation);
        doNothing().when(spy).dropTable(anyString());

        doNothing().when(spy).checkAvailableAllShards();

        InOrder updateOrder = inOrder(spy, mockedChDao);
        System.out.println(tmpDistributedTable.toString());
        spy.recreateDistributedTableFromExisting(distributedTable);

//        check order of update
        updateOrder.verify(spy).checkAvailableAllShards();
//        first to delete if exists
        updateOrder.verify(spy).dropTable(tmpTableName);
//        Not mocking spy method because unexplained error throws
        updateOrder.verify(mockedChDao).createDistributedTableWithEngine(
                tmpDistributedTable.getSourceTableName(),
                tmpDistributedTable.getTableName(),
                tmpDistributedTable.getDistributeStatement()
        );
        updateOrder.verify(mockedChDao).swapNonPartitionedTables(distributedTableName, tmpTableName);
//        cleanup old version
        updateOrder.verify(spy).dropTable(tmpTableName);
//        Check what we NEVER delete no tmp table
        updateOrder.verify(spy, never()).dropTable(distributedTableName);
    }

    public ClickhouseDao mockChDao() {
        ClickhouseDao mockedDao = mock(ClickhouseDao.class);
        when(mockedDao.getTableNamesByRegex(eq("^(test1|test2)$"))).thenReturn(List.of("test1", "test2"));
        return mockedDao;
    }

    public YtTableService mockYtService() {
        return mock(YtTableService.class);
    }

    public MetadataDao mockMetadataDao() {
        return mock(MetadataDao.class);
    }

}
