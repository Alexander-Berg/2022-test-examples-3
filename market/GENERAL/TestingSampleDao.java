package ru.yandex.market.ir.classifier.trainer.dao;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowCountCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import ru.yandex.common.util.db.RowMappers;
import ru.yandex.market.ir.classifier.trainer.model.Markup;
import ru.yandex.market.ir.classifier.trainer.tasks.logic.QualityCheckerTask;
import ru.yandex.market.ir.http.Classifier;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;

/**
 * @author Evgeny Anisimoff <a href="mailto:anisimoff@yandex-team.ru"/>
 * @since {14:28}
 */
public class TestingSampleDao {
    private Logger log = LogManager.getLogger();
    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyyMMdd_HHmm");
    private JdbcTemplate scatJdbcTemplate;
    private JdbcTemplate classifierJdbcTemplate;
    private String findTableTemplate;
    private boolean isRobot;
    private boolean allowedToCreateTable = true;
    private String classificationResultTable;
    private String qualityChecksSessionsTable;
    private static final String OK_STATUS = "OK";
    private static final String FAILED_STATUS = "FAILED";

    private static String emptyForNull(String s) {
        return s == null ? "" : s;
    }


    public List<Markup> loadValidMarkup() {
        final List<Markup> result = new ArrayList<>();
        log.debug("Testing sample loading started");
        scatJdbcTemplate.query("" +
            "SELECT " +
            "  operator_category_id, o.offer_id  offer_id, offer,  descr, shop_category_name, params, price, " +
            "       locale, offer_params, mapped_id, market_category, l.group_id " +
            "FROM site_catalog.AS_OFFERS o " +
            "  JOIN site_catalog.v_as_clsif_sutable_markup s ON o.OFFER_ID = s.offer_id and o.LIST_ID = s.LIST_ID " +
            "  LEFT JOIN site_catalog.as_offer_list_info l ON o.list_id = l.id " +
            "WHERE l.is_robot_list = ? " +
            " AND snapshot_id = 0",
            new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    String[] shopCategoryNameSplit = emptyForNull(rs.getString("shop_category_name")).split(":", 2);
                    String shopName = shopCategoryNameSplit.length == 1 ? "" : shopCategoryNameSplit[0];
                    String categoryName = shopCategoryNameSplit.length == 1 ?
                            shopCategoryNameSplit[0] : shopCategoryNameSplit[1];
                    Classifier.Offer.Builder builder = Classifier.Offer.newBuilder()
                            .setOfferId(rs.getString("offer_id"))
                            .setTitle(rs.getString("offer"))
                            .setDescription(emptyForNull(rs.getString("descr")))
                            .setCategoryName(categoryName)
                            .setShopName(shopName)
                            .setParams(emptyForNull(rs.getString("params")))
                            .setPrice(rs.getFloat("price"))
                            .setLocale(emptyForNull(rs.getString("locale")))
                            .setYmlParams(emptyForNull(rs.getString("offer_params")))
                            .setMarketCategory(emptyForNull(rs.getString("market_category")))
                            .setUsePriceRange(false);
                    result.add(
                        new Markup(
                            builder.build(), rs.getInt("operator_category_id"), rs.getInt("group_id")
                        )
                    );
                }
            },
            isRobot ? 1 : 0);
        log.debug("Testing sample loading finished");
        return result;
    }

    public void addSession(
        boolean success,
        QualityCheckerTask.CheckResult currentWhite,
        QualityCheckerTask.CheckResult previousWhite,
        QualityCheckerTask.CheckResult currentBlue,
        QualityCheckerTask.CheckResult previousBlue,
        QualityCheckerTask.CheckResult currentHonestMark,
        QualityCheckerTask.CheckResult previousHonestMark
    ) {
        if (!tablesExists()) {
            createTables();
        }
        String sessionId = generateSessionId();
        SqlParameterSource[] batch = new SqlParameterSource[currentWhite.size()];
        int i = 0;
        for (String offerId : currentWhite.getOffers()) {
            batch[i++] = new MapSqlParameterSource()
                    .addValue("session_id", sessionId)
                    .addValue("offer_id", offerId)
                    .addValue("operator_category", currentWhite.getCorrectAnswer(offerId))
                    .addValue("classified_category", currentWhite.getClassifierAnswer(offerId));
        }
        new SimpleJdbcInsert(classifierJdbcTemplate).withTableName(classificationResultTable).executeBatch(batch);
        new SimpleJdbcInsert(classifierJdbcTemplate).withTableName(qualityChecksSessionsTable).execute(
            new MapSqlParameterSource()
                .addValue("session_id", sessionId)
                .addValue("status", success ? OK_STATUS : FAILED_STATUS)
                .addValue("quality", currentWhite.getQuality())
                .addValue("previous_quality", previousWhite.getQuality())
                .addValue("quality_grouped", currentWhite.getGroupedQuality())
                .addValue("previous_quality_grouped", previousWhite.getGroupedQuality())
                .addValue("red_quality", null)
                .addValue("previous_red_quality", null)
                .addValue("blue_quality", currentBlue.getQuality())
                .addValue("previous_blue_quality", previousBlue.getQuality())
                .addValue("honest_mark_quality", currentHonestMark.getQuality())
                .addValue("previous_honest_mark_quality", previousHonestMark.getQuality())
                .addValue("blue_confidence_logloss", currentBlue.getConfidenceLogLoss())
                .addValue("previous_blue_confidence_logloss", previousBlue.getConfidenceLogLoss())
        );
    }

    private String generateSessionId() {
        return DATE_FORMAT.format(new Date());
    }

    private boolean tablesExists() {
        return tableExists(classificationResultTable) && tableExists(qualityChecksSessionsTable);
    }

    private boolean tableExists(String tableName) {
        RowCountCallbackHandler counter = new RowCountCallbackHandler();
        classifierJdbcTemplate.query(format(findTableTemplate, tableName), counter);
        return counter.getRowCount() > 0;
    }

    private void createTables() {
        if (!tableExists(classificationResultTable)) {
            classifierJdbcTemplate.execute("CREATE TABLE " + classificationResultTable + " (" +
                "    session_id VARCHAR(20)," +
                "    offer_id VARCHAR(32)," +
                "    operator_category INTEGER," +
                "    classified_category INTEGER," +
                "    PRIMARY KEY (session_id, offer_id)" +
                ")");
        }
        if (!tableExists(qualityChecksSessionsTable)) {
            classifierJdbcTemplate.execute("CREATE TABLE " + qualityChecksSessionsTable + " (" +
                "   session_id VARCHAR(20) PRIMARY KEY ," +
                "   status VARCHAR(10)," +
                "   quality DOUBLE" +
                "   previous_quality DOUBLE" +
                "   quality_grouped DOUBLE" +
                "   previous_quality_grouped DOUBLE" +
                "   red_quality DOUBLE" +
                "   previous_red_quality DOUBLE" +
                "   blue_quality DOUBLE" +
                "   previous_blue_quality DOUBLE" +
                "   honest_mark_quality DOUBLE" +
                "   previous_honest_mark_quality DOUBLE" +
                ")");
        }
    }

    private String getLastSessionId() {
        return classifierJdbcTemplate.query(
                "SELECT max(session_Id) FROM " + classificationResultTable +
                        " WHERE session_id IN (SELECT session_id FROM " +
                        qualityChecksSessionsTable + " WHERE status = 'OK')",
                RowMappers.stringAt(1)).get(0);
    }

    private int correctlyClassifiedOffers(String lastSessionId) {
        return classifierJdbcTemplate.queryForInt("SELECT count(*) FROM " + classificationResultTable +
                " WHERE session_id = ? AND operator_category = classified_category", lastSessionId);
    }

    private int allClassifiedOffers(String lastSessionId) {
        return classifierJdbcTemplate.queryForInt("SELECT count(*) FROM " + classificationResultTable +
                " WHERE session_id = ?", lastSessionId);
    }

    @Required
    public void setScatJdbcTemplate(JdbcTemplate scatJdbcTemplate) {
        this.scatJdbcTemplate = scatJdbcTemplate;
    }

    @Required
    public void setClassifierJdbcTemplate(JdbcTemplate classifierJdbcTemplate) {
        this.classifierJdbcTemplate = classifierJdbcTemplate;
    }

    @Required
    public void setFindTableTemplate(String findTableTemplate) {
        this.findTableTemplate = findTableTemplate;
    }

    @Required
    public void setRobot(boolean robot) {
        isRobot = robot;
    }

    public void setAllowedToCreateTable(boolean allowedToCreateTable) {
        this.allowedToCreateTable = allowedToCreateTable;
    }

    @Required
    public void setClassificationResultTable(String classificationResultTable) {
        this.classificationResultTable = classificationResultTable;
    }

    @Required
    public void setQualityChecksSessionsTable(String qualityChecksSessionsTable) {
        this.qualityChecksSessionsTable = qualityChecksSessionsTable;
    }
}
