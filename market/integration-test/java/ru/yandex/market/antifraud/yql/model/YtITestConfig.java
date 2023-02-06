package ru.yandex.market.antifraud.yql.model;

import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.util.StrParams;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;

import static ru.yandex.market.antifraud.util.IntDateUtil.hyphenated;

public class YtITestConfig implements YtConfig {

    private static final String ROOT_DIR_TPL = "//home/market/${env}/mstat/${cname}";
    private static final String TMP_ROLLBACKS_DIR_TPL = ROOT_DIR_TPL + "/tmp_session/${log_type}";
    private static final String FINAL_ROLLBACKS_DIR_TPL = ROOT_DIR_TPL + "/filters_executed/${log_type}";
    private static final String DATA_READY_DIR_TPL = ROOT_DIR_TPL + "/data_ready/${log_type}";
    private static final String UNSENT_DIR_TPL = ROOT_DIR_TPL + "/rollbacks_unsent/${log_type}";
    private static final String ROLLBACKS_DIR_TPL = ROOT_DIR_TPL + "/rollbacks/${log_type}/1d";
    private static final String YT_LOG_PATH_SUFFIX_TPL = "/${log_type}/${scale}/${day}";
    private static final String TEST_DATA_PATH = "//home/market/testing/mstat/yqlaf/test_data";

    private String rootDir;
    private String tmpRollbacksDir;
    private String finalRollbacksDir;
    private String dataReadyDir;
    private String unsentDir;
    private String rollbacksDir;
    private String logPath;

    @PostConstruct
    public void init() {
        String env = "testing";
        String cname = "yqlaf_itests/" + new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date()) + "_" +
            RndUtil.randomAlphabetic(3);
        rootDir = r(ROOT_DIR_TPL, env, cname);
        tmpRollbacksDir = r(TMP_ROLLBACKS_DIR_TPL, env, cname);
        finalRollbacksDir = r(FINAL_ROLLBACKS_DIR_TPL, env, cname);
        dataReadyDir = r(DATA_READY_DIR_TPL, env, cname);
        unsentDir = r(UNSENT_DIR_TPL, env, cname);
        rollbacksDir = r(ROLLBACKS_DIR_TPL, env, cname);
        logPath = TEST_DATA_PATH + YT_LOG_PATH_SUFFIX_TPL;
    }

    private String r(String dirTpl, String env, String cname) {
        return dirTpl
                .replace("${env}", env)
                .replace("${cname}", cname);
    }

    @Override
    public String getCluster() {
        return "hahn"; // FIXME hardcoded test cluster
    }

    @Override
    public String getPool() {
        return "";
    }

    @Override
    public String getPoolPragma() {
        return "";
    }

    @Override
    public String getDayclosingPoolPragma() {
        return "";
    }


    @Override
    public String getToken() {
        return System.getenv("MARKET_CI_YT_TOKEN");
    }

    @Override
    public String getTmpRollbacksDir(YtLogConfig ytLogConfig) {
        return StrParams.replace(tmpRollbacksDir,
                "log_type", ytLogConfig.getLogName().getLogName());
    }

    @Override
    public String getFinalRollbacksDir(YtLogConfig ytLogConfig) {
        return StrParams.replace(finalRollbacksDir,
                "log_type", ytLogConfig.getLogName().getLogName());
    }

    @Override
    public String getDataReadyDir(YtLogConfig ytLogConfig) {
        return StrParams.replace(dataReadyDir,
                "log_type", ytLogConfig.getLogName().getLogName());
    }

    @Override
    public String getUnsentDir(YtLogConfig ytLogConfig) {
        return StrParams.replace(unsentDir,
                "log_type", ytLogConfig.getLogName().getLogName());
    }

    @Override
    public String getRollbacksDir(YtLogConfig ytLogConfig) {
        return StrParams.replace(rollbacksDir,
                "log_type", ytLogConfig.getLogName().getLogName());
    }

    @Override
    public String getRollbacksDayTable(YtLogConfig ytLogConfig, int day) {
        return StrParams.replace(rollbacksDir,
                "log_type", ytLogConfig.getLogName().getLogName()) + "/" + hyphenated(day);
    }

    @Override
    public String getAfRootDir() {
        return rootDir;
    }

    @Override
    public String getLogPath(YtLogConfig ytLogConfig, UnvalidatedDay.Scale scale, String partition) {
        return StrParams.replace(logPath,
                "log_type", ytLogConfig.getLogName().getLogName(),
                "scale", ytLogConfig.getScales().get(scale),
                "day", partition);
    }

    @Override
    public String getLogDir(YtLogConfig ytLogConfig, UnvalidatedDay.Scale scale) {
        return logPath
                .replace("${log_type}", ytLogConfig.getLogName().getLogName())
                .replace("${scale}", ytLogConfig.getScales().get(scale))
                .replace("/${day}", "");
    }
}
