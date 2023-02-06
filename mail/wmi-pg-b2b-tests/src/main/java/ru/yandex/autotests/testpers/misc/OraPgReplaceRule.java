package ru.yandex.autotests.testpers.misc;

import com.jcabi.aspects.Cacheable;
import com.jcabi.jdbc.JdbcSession;
import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.rules.ExternalResource;
import org.junit.runners.model.Statement;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ComposeCheck;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.selectFirst;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static org.apache.commons.lang3.Validate.notNull;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.testpers.misc.OraPg.oraToPg;
import static ru.yandex.autotests.testpers.misc.OraPg.replaceOraWithPg;
import static ru.yandex.autotests.testpers.misc.OraPg.replaceOraWithPgWithoutPrefix;
import static ru.yandex.autotests.testpers.misc.OraPgReplaceRule.Kind.FID;
import static ru.yandex.autotests.testpers.misc.OraPgReplaceRule.Kind.LID;
import static ru.yandex.autotests.testpers.misc.OraPgReplaceRule.Kind.MID;
import static ru.yandex.autotests.testpers.misc.OraPgReplaceRule.Kind.TID;
import static ru.yandex.autotests.testpers.misc.PgProperties.CONN_STRING;
import static ru.yandex.autotests.testpers.misc.PgProperties.pgProps;

/**
 * User: lanwen
 * Date: 06.05.15
 * Time: 20:47
 */
public class OraPgReplaceRule extends ExternalResource {
    public static final String SELECT_ORA_MIDS = "SELECT ora_mid, mid FROM ora.mids WHERE uid = ?";
    public static final String SELECT_ORA_FIDS = "SELECT ora_fid, fid FROM ora.fids WHERE uid = ?";
    public static final String SELECT_ORA_LIDS = "SELECT ora_lid, lid FROM ora.lids WHERE uid = ?";
    public static final String SELECT_ORA_TIDS = "SELECT ora_tid, tid FROM ora.tids WHERE uid = ?";

    private static final Logger LOGGER = LogManager.getLogger(OraPgReplaceRule.class);

    private SshLocalPortForwardingRule fwd;
    private DataSource source;
    private DefaultHttpClient hc;
    private HttpClientManagerRule authCli;

    private List<OraPg> mids = new ArrayList<>();
    private List<OraPg> tids = new ArrayList<>();
    private List<OraPg> fids = new ArrayList<>();
    private List<OraPg> lids = new ArrayList<>();

    public OraPgReplaceRule(SshLocalPortForwardingRule fwd) {
        this.fwd = fwd;
    }

    public OraPgReplaceRule withAuthClient(HttpClientManagerRule authCli) {
        this.authCli = authCli;
        return this;
    }
    
    public OraPgReplaceRule withHc(DefaultHttpClient client) {
        this.hc = client;
        return this;
    }

    @Override
    public void before() throws Exception {
        String uid = api(ComposeCheck.class).get().via(hc).getUid();
        mids = new JdbcSession(source()).sql(SELECT_ORA_MIDS).set(parseInt(uid)).select(oraToPg());
        fids = new JdbcSession(source()).sql(SELECT_ORA_FIDS).set(parseInt(uid)).select(oraToPg());
        lids = new JdbcSession(source()).sql(SELECT_ORA_LIDS).set(parseInt(uid)).select(oraToPg());
        tids = new JdbcSession(source()).sql(SELECT_ORA_TIDS).set(parseInt(uid)).select(oraToPg());
    }

    @Override
    public Statement apply(Statement base, org.junit.runner.Description description) {
        Credentials annotation = description.getTestClass().getAnnotation(Credentials.class);
        if(annotation != null) {
            String group = annotation.loginGroup();
            hc = auth().with(group).login().authHC();
        } else {
            hc = authCli.authHC();
        }
        
        return super.apply(base, description);
    }

    public List<OraPg> replacement(Kind kind) {
        switch (kind) {
            default:
            case MID:
                return mids;
            case TID:
                return tids;
            case LID:
                return lids;
            case FID:
                return fids;
        }
    }

    public String replace(String resp) {
        LOGGER.info(format("Заменено: mid: %s", replacement(MID).size()));

        return replaceOraWithPgWithoutPrefix(newArrayList(concat(tids, mids, lids, fids)), replaceOraWithPg(lids,
                replaceOraWithPg(fids,
                        replaceOraWithPg(mids,
                                replaceOraWithPg(tids, resp, TID.prefixes()
                                ), MID.prefixes()
                        ), FID.prefixes()
                ), LID.prefixes()
        ));
    }

    public String resolveOraFromPg(String pgid, Kind kind) {
        return ((OraPg) notNull(
                selectFirst(replacement(kind), having(on(OraPg.class).getPg(), is(pgid))),
                "PG ID %s не найден среди %s", pgid, kind)
        ).getOra();
    }

    @Cacheable(forever = true)
    private DataSource source() {
        if (source == null) {
            BoneCPDataSource src = new BoneCPDataSource();
            src.setDriverClass("org.postgresql.Driver");
            src.setJdbcUrl(format(CONN_STRING, fwd.local().getHost(), fwd.local().getPort(), pgProps().getDbdb()));
            src.setUser(pgProps().getDbuser());
            src.setPassword(pgProps().getDbpwd());
            source = src;
        }
        return source;
    }

    public enum Kind {
        MID("mid="),
        TID("thread_id="),
        FID("fid="),
        LID("lid=");

        private List<String> prefixes;

        Kind(String... prefixes) {
            this.prefixes = newArrayList(prefixes);
        }

        public List<String> prefixes() {
            return prefixes;
        }
    }
}