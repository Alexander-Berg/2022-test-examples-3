package client;

import com.google.common.collect.ImmutableMap;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.passport.tvmauth.NativeTvmClient;
import ru.yandex.passport.tvmauth.TvmApiSettings;
import ru.yandex.passport.tvmauth.TvmClient;

@Resource.Classpath("delivery/tvm.properties")
public enum TVM {
    INSTANCE;

    public static final String LMS_ADMIN = "lms-admin";
    public static final String TRACKER = "tracker";
    public static final String TRISTERO = "tristero";
    public static final String LAVKA_ADMIN = "lavkaAdmin";
    public static final String CHECKOUTER = "checkouter";
    public static final String LGW = "lgw";
    public static final String NESU = "nesu";
    public static final String TAXI = "taxi";
    public static final String IRIS = "iris";
    public static final String PLATFORM = "platform";
    public static final String LRM = "lrm";
    public static final String CS = "capacityStorage";
    public static final String CARRIER_PLANNER = "carrierPlanner";
    public static final String L4S = "l4s";
    public static final String L4G = "l4g";
    public static final String MBIOS = "mbios";

    public static final String HEADER = "X-Ya-Service-Ticket";
    private final TvmClient tvmClient;

    @Property("tvm.self_id")
    private int selfId;

    @Property("tvm.tracker_id")
    private int trackerId;
    @Property("tvm.lgw_id")
    private int lgwId;
    @Property("tvm.lms_admin_id")
    private int lmsAdminId;
    @Property("tvm.tristero_id")
    private int tristeroId;
    @Property("tvm.lavka_admin_id")
    private int lavkaAdminId;
    @Property("tvm.checkouter_id")
    private int checkouterid;
    @Property("tvm.nesu_id")
    private int nesuid;
    @Property("tvm.taxi_id")
    private int taxi;
    @Property("tvm.iris_id")
    private int irisid;
    @Property("tvm.platform_id")
    private int platformId;
    @Property("tvm.lrm_id")
    private int lrmId;
    @Property("tvm.cs_id")
    private int csId;
    @Property("tvm.carrier_planner_id")
    private int carrierPlannerId;
    @Property("tvm.l4s_id")
    private int l4sId;
    @Property("tvm.l4g_id")
    private int l4gId;
    @Property("tvm.mbios_id")
    private int mbiosId;

    TVM() {
        PropertyLoader.newInstance().populate(this);

        String secret = System.getenv("TVM_CLIENT_SECRET");

        TvmApiSettings tvmApiSettings = new TvmApiSettings();
        tvmApiSettings.setSelfTvmId(selfId);

        tvmApiSettings.enableServiceTicketsFetchOptions(secret,
            new ImmutableMap.Builder()
                .put(LMS_ADMIN, lmsAdminId)
                .put(TRACKER, trackerId)
                .put(TRISTERO, tristeroId)
                .put(LAVKA_ADMIN, lavkaAdminId)
                .put(CHECKOUTER, checkouterid)
                .put(LGW, lgwId)
                .put(NESU, nesuid)
                .put(TAXI, taxi)
                .put(IRIS, irisid)
                .put(CS, csId)
                .put(PLATFORM, platformId)
                .put(LRM, lrmId)
                .put(CARRIER_PLANNER, carrierPlannerId)
                .put(L4S, l4sId)
                .put(L4G, l4gId)
                .put(MBIOS, mbiosId)
                .build()
        );

        tvmClient = new NativeTvmClient(tvmApiSettings);
    }

    public String getServiceTicket(String alias) {
        return tvmClient.getServiceTicketFor(alias);
    }
}
