package ru.yandex.autotests.market.stat.mappers;

import org.apache.commons.lang.StringUtils;
import static ru.yandex.autotests.market.stat.util.DateUtils.fromMillis;
import org.springframework.jdbc.core.RowMapper;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by entarrion on 02.04.15.
 */
public class TmsStateRowMapper implements RowMapper<TmsRunState> {
    public static final String ID = "ID";
    public static final String JOB_NAME = "JOB_NAME";
    public static final String TRIGGER_FIRE_TIME = "TRIGGER_FIRE_TIME";
    public static final String JOB_FINISHED_TIME = "JOB_FINISHED_TIME";
    public static final String JOB_STATUS = "JOB_STATUS";
    public static final String HOST_NAME = "HOST_NAME";

    @Override
    public TmsRunState mapRow(ResultSet rs, int rowNum) throws SQLException {
        TmsRunState result = new TmsRunState();
        result.setId(rs.getString(ID));
        result.setJobName(rs.getString(JOB_NAME));
        result.setFireTime(fromMillis(rs.getLong(TRIGGER_FIRE_TIME)));
        String endTime = rs.getString(JOB_FINISHED_TIME);
        result.setFinishTime(StringUtils.defaultIfEmpty(endTime, "null").equals("null") ?
                null : fromMillis(rs.getLong(JOB_FINISHED_TIME)));
        result.setJobStatus(rs.getString(JOB_STATUS));
        result.setHostName(rs.getString(HOST_NAME));
        return result;
    }
}
