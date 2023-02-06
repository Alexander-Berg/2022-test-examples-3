package ru.yandex.market.pers.tms.yt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.imp.RngImportState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class PrepareRNGImportExecutorTest extends MockedPersTmsTest {

    @Autowired
    private PrepareRNGImportExecutor prepareRngImportExecutor;

    @Autowired
    @Qualifier("ytJdbcTemplate")
    private JdbcTemplate ytJdbcTemplate;;

    @Test
    public void testPrepareImport() throws Exception {
        long modelId = new Random().nextInt(1000000);
        Pair<Long, Long> modelIdDeletedUploaded = Pair.of(modelId++, modelId++);
        Pair<Long, Long> modelIdDeletedUploadedOld = Pair.of(modelId++, modelId++);
        Pair<Long, Long> modelIdDeletedNeedUpload = Pair.of(modelId++, modelId++);
        Pair<Long, Long> modelIdRepeatedUploaded = Pair.of(modelId++, modelId++);
        Pair<Long, Long> modelIdRepeatedNeedUpload = Pair.of(modelId++, modelId++);
        Pair<Long, Long> modelIdNew = Pair.of(modelId++, modelId++);

        prepareRngImportExecutor.saveModelWithBarcodes(Arrays.asList(
            // uploaded, but not set in next models dataset - preserve for N days
            modelIdDeletedUploaded,
            // uploaded N days ago, but not set in next models dataset - delete
            modelIdDeletedUploadedOld,
            // not uploaded, not set in next models dataset - delete
            modelIdDeletedNeedUpload,
            // uploaded, found in next models dataset - preserve and reset state
            modelIdRepeatedUploaded,
            // not uploaded, found in next models dataset - preserve, keep same
            modelIdRepeatedNeedUpload));

        // set states
        pgJdbcTemplate.update("update model_rng_import set state = ?, UPDATE_TIME = now() where model_id = ?",
            RngImportState.ALREADY_UPLOAD.getValue(), modelIdDeletedUploaded.getFirst());
        pgJdbcTemplate.update(
            "update model_rng_import set state = ?, UPDATE_TIME = now() - make_interval(days := ? + 1) " +
                "where model_id = ?",
            RngImportState.ALREADY_UPLOAD.getValue(),
            PrepareRNGImportExecutor.DELETE_DELAY_DAYS,
            modelIdDeletedUploadedOld.getFirst());
        pgJdbcTemplate.update("update model_rng_import set state = ? where model_id = ?",
            RngImportState.ALREADY_UPLOAD.getValue(), modelIdRepeatedUploaded.getFirst());

        checkModelData(modelIdDeletedUploaded, 0, RngImportState.ALREADY_UPLOAD);
        checkModelData(modelIdDeletedUploadedOld, 0, RngImportState.ALREADY_UPLOAD);
        checkModelData(modelIdDeletedNeedUpload, 0, RngImportState.NEED_UPLOAD);
        checkModelData(modelIdRepeatedUploaded, 0, RngImportState.ALREADY_UPLOAD);
        checkModelData(modelIdRepeatedNeedUpload, 0, RngImportState.NEED_UPLOAD);

        doAnswer((i) -> {
            ResultSetExtractor rs = i.getArgument(1);
            rs.extractData(prepareResultSet(modelIdRepeatedUploaded, modelIdRepeatedNeedUpload, modelIdNew));
            return null;
        }).when(ytJdbcTemplate).query(anyString(), any(ResultSetExtractor.class));

        prepareRngImportExecutor.runTmsJob();

        checkModelData(modelIdDeletedUploaded, 1, RngImportState.ALREADY_UPLOAD);
        checkDeletedData(modelIdDeletedUploadedOld);
        checkDeletedData(modelIdDeletedNeedUpload);
        checkModelData(modelIdRepeatedUploaded, 0, RngImportState.NEED_UPLOAD);
        checkModelData(modelIdRepeatedNeedUpload, 0, RngImportState.NEED_UPLOAD);
        checkModelData(modelIdNew, 0, RngImportState.NEED_UPLOAD);
    }

    private void checkDeletedData(Pair<Long, Long> model) {
        final Long count = pgJdbcTemplate.queryForObject(
            "select count(*) from model_rng_import where model_id = ?", Long.class, model.getFirst());
        assertNotNull(count);
        assertEquals(0, count.longValue());
    }

    private void checkModelData(Pair<Long, Long> model, long deleteExp, RngImportState stateExp) {
        final Long modelId = model.getFirst();
        final Long barcode = pgJdbcTemplate.queryForObject(
            "select barcode from model_rng_import where model_id = ?", Long.class, modelId);
        assertNotNull(barcode);
        assertEquals(model.getSecond().longValue(), barcode.longValue());

        final Long delete = pgJdbcTemplate.queryForObject(
            "select deleted from model_rng_import where model_id = ?", Long.class, modelId);
        assertNotNull(delete);
        assertEquals(deleteExp, delete.longValue());

        final Long state = pgJdbcTemplate.queryForObject(
            "select state from model_rng_import where model_id = ?", Long.class, modelId);
        assertNotNull(state);
        assertEquals(stateExp.getValue(), state.longValue());
    }

    private ResultSet prepareResultSet(Pair<Long, Long> model1, Pair<Long, Long> model2, Pair<Long, Long> model3) throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        reset(resultSet);
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(resultSet.getString(eq("model_id")))
            .thenReturn(String.valueOf(model1.getFirst()))
            .thenReturn(String.valueOf(model2.getFirst()))
            .thenReturn(String.valueOf(model3.getFirst()));
        when(resultSet.getString(eq("barcode")))
            .thenReturn(String.valueOf(model1.getSecond()))
            .thenReturn(String.valueOf(model2.getSecond()))
            .thenReturn(String.valueOf(model3.getSecond()));
        return resultSet;
    }

}
