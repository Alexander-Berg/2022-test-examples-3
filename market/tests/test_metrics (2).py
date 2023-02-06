import os
from collections import defaultdict

import numpy as np
import pandas as pd
import math
import io

Array = np.array


def calc_rmse(y: Array, p: Array, g: Array):
    """
    mse / mean_target
    adapted to multi-dimensional case
    """
    return np.sqrt(np.power(y - p, 2).mean()) / np.mean(y)


def calc_corr_rmse(y: Array, p: Array, g: Array):
    """
    corr_rmse = rmse(y, p * calc_best_mul_rmse)
    adapted to multi-dimensional case
    """
    return (
        np.sqrt(
            ((y * y).sum() - np.power((p * y).sum(), 2) / ((p * p).sum())) / len(p)
        ) / y.mean()
    )


def calc_g_corr_rmse(y: Array, p: Array, g: Array):
    """
    corr_rmse = rmse(y, p * calc_best_mul_rmse)
    adapted to multi-dimensional case
    """
    df = pd.DataFrame({
        "y": y,
        "p": p,
        "y2": y * y,
        "p2": p * p,
        "yp": y * p,
        "g": g,
        "n": np.ones(len(y), dtype=float)
    })
    df_acc = df.groupby("g").sum().reset_index()
    df_acc["sq_corr_error"] = df_acc.y2 - np.float_power(df_acc.yp, 2) / df_acc.p2
    df_result = df_acc.sum(axis=0)
    return math.sqrt(max(0.0, df_result["sq_corr_error"] / df_result["n"])) / (df_result["y"] / df_result["n"])


def calc_g_mse_log_diff(target, predict, group):
    weight = target + 1
    eps = 0.01
    log_target = np.log(target + eps)
    log_predict = np.log(predict + eps)
    x = log_target - log_predict
    x_df = pd.DataFrame({
        'x': x,
        'group': group,
        'weight': weight,
        'x_weight': x * weight,
        'x2_weight': x * x * weight,
    })
    x_agg_df = x_df.groupby('group').agg(
        sum_weight=('weight', 'sum'),
        sum_x_weight=('x_weight', 'sum'),
        sum_x2_weight=('x2_weight', 'sum'),
    )
    x_agg_df['d_x'] = (
        (x_agg_df['sum_x2_weight'] / x_agg_df['sum_weight'])
        - np.power(x_agg_df['sum_x_weight'] / x_agg_df['sum_weight'], 2.0)
    )
    return np.sqrt((x_agg_df['d_x'] * x_agg_df['sum_weight']).sum() / x_agg_df['sum_weight'].sum())


def true_model_items(rel_price: Array, base_items: Array, slope: Array):
    return base_items * np.exp(- slope * rel_price)


# random vectors defining distortions in models
n1 = None
n2 = None
n3 = None


def distorted_model_items(
    rel_price: Array, base_items: Array, slope: Array,
    distort_abs: float = 0.0,
    distort_mul_slope: float = 0.0,
    distort_add_slope: float = 0.0,
    distort_add_slope_bias: float = 0.0,
):
    global n1, n2, n3
    true_items = true_model_items(rel_price, base_items, slope)
    base_items = true_model_items(0 * rel_price + 1.0, base_items, slope)
    if n1 is None:
        n1 = np.random.randn(len(true_items))
        n2 = np.random.randn(len(true_items))
        n3 = np.random.randn(len(true_items))

    slope_mul = np.exp(distort_mul_slope * n2)
    new_slope = slope * slope_mul
    return (
        base_items
        * np.exp(n1 * distort_abs)
        * np.float_power(true_items / base_items, slope_mul)
        * np.exp(((n3 * distort_add_slope + distort_add_slope_bias + new_slope).clip(0.0) - new_slope) * rel_price)
    )


def gen_distorted_model(
    distort_abs: float = 0.0,
    distort_mul_slope: float = 0.0,
    distort_add_slope: float = 0.0,
    distort_add_slope_bias: float = 0.0,
):
    return (
        lambda rel_price, base_items, slope: distorted_model_items(
            rel_price, base_items, slope,
            distort_abs, distort_mul_slope, distort_add_slope, distort_add_slope_bias
        )
    )


metrics0 = {
    "rmse": calc_rmse,
    "corr_rmse": calc_corr_rmse,
    "msku_corr_rmse": lambda y, p, g: calc_g_corr_rmse(y, p, g.transpose()[0]),
    "hid_corr_rmse": lambda y, p, g: calc_g_corr_rmse(y, p, g.transpose()[1]),
    # "g0_corr_rmse": lambda y, p, g: calc_g_corr_rmse(y, p, np.zeros(len(y))), # == corr_rmse, testing calc_g_corr_rmse
    "msku_mse_log_diff": lambda y, p, g: calc_g_mse_log_diff(y, p, g.transpose()[0]),
    "hid_mse_log_diff": lambda y, p, g: calc_g_mse_log_diff(y, p, g.transpose()[1]),
}

models0 = {
    "true": gen_distorted_model(0.0, 0.0, 0.0),
    "e0.1": gen_distorted_model(0.1, 0.1, 0.1),
    "e0.2": gen_distorted_model(0.2, 0.2, 0.2),
    "e0.4": gen_distorted_model(0.4, 0.4, 0.4),
    "a0.6_m0_s0": gen_distorted_model(0.6, 0.0, 0.0),
    "a0.7_m0.05_s0": gen_distorted_model(0.7, 0.05, 0.0),
    "a0.7_m0.05_s0.05": gen_distorted_model(0.7, 0.05, 0.05),
    "a0.5_m0.2_s0.1": gen_distorted_model(0.5, 0.2, 0.1),
    "a0.5_m0.2_s0.4": gen_distorted_model(0.5, 0.2, 0.4),
    "a0.5_m0.4_s0.2": gen_distorted_model(0.5, 0.4, 0.2),
    "a0.5_m0.3_s0.3": gen_distorted_model(0.5, 0.3, 0.3),
    "a0.5_m0.3_s0.3_b0.2": gen_distorted_model(0.5, 0.3, 0.3, 0.2),
    "a0.5_m0.3_s0.3_b-0.2": gen_distorted_model(0.5, 0.3, 0.3, -0.2),
    "a0.5_m0.3_s0.3_b-0.7": gen_distorted_model(0.5, 0.3, 0.3, -0.7),
    "a0.5_m0.2_s0.2_b-0.7": gen_distorted_model(0.5, 0.2, 0.2, -0.7),
    "a0.2_m0.2_s0.2_b-0.7": gen_distorted_model(0.2, 0.2, 0.2, -0.7),
    "a0_m0.2_s0.2_b-0.7": gen_distorted_model(0.0, 0.2, 0.2, -0.7),
    "a0_m0.2_s0.2_b-1.5": gen_distorted_model(0.0, 0.2, 0.2, -1.5),
    "a0.7_m0.15_s0.15_b-0.1": gen_distorted_model(0.7, 0.15, 0.15, -0.1),
}


def prepare_data(ground):
    return [
        np.array(ground["base_price"]),
        np.array(ground["slope"]),
        np.array(ground["base_items"]),
        np.array(ground["cost"]) / np.array(ground["base_price"]),
        np.array(ground[["hid", "msku", "d42_items"]])
    ]


def measure_l(model, rel_price, base_items, slope, base_price, rel_cost, margin_l):
    items = model(rel_price, base_items, slope)
    return items, items * base_price * (rel_price + margin_l * (rel_price - rel_cost))


def evaluate_metrics(models, ground, metrics, margin_l):
    global n1, n2, n3
    rel_prices_variants = np.linspace(0.7, 1.3, 31)
    base_price, slope, base_items, rel_cost, group = prepare_data(ground)
    n = len(ground)
    true_model = models["true"]
    result = defaultdict(dict)
    for name, model in models.items():
        model_lagrange = []
        for x in rel_prices_variants:
            rel_price = np.ones(n) * x
            m_items, m_lagrange = measure_l(model, rel_price, base_items, slope, base_price, rel_cost, margin_l)
            model_lagrange.append(m_lagrange)

        model_lagrange = np.array(model_lagrange)
        best_idxs = model_lagrange.argmax(axis=0)
        best_rel_price = rel_prices_variants[best_idxs]
        items, lagrange = measure_l(true_model, best_rel_price, base_items, slope, base_price, rel_cost, margin_l)
        m_items, m_lagrange = measure_l(model, best_rel_price, base_items, slope, base_price, rel_cost, margin_l)
        result[name]['lagrange'] = lagrange.sum()
        result[name]['items'] = items.sum()
        result[name]['gmv'] = (items * best_rel_price * base_price).sum()
        result[name]['margin'] = (items * (best_rel_price - rel_cost) * base_price).sum()

        items_arr = [items]
        m_items_arr = [m_items]
        group_arr = [group]
        for day_i in range(10):
            # n1 = (n1 * 10 + np.random.randn(len(items))) / math.sqrt(101)
            # n2 = (n2 * 10 + np.random.randn(len(items))) / math.sqrt(101)
            # n3 = (n3 * 10 + np.random.randn(len(items))) / math.sqrt(101)

            rel_price = best_rel_price * np.exp(0.15 * np.random.randn(len(best_rel_price)))
            items, lagrange = measure_l(true_model, rel_price, base_items, slope, base_price, rel_cost, margin_l)
            m_items, m_lagrange = measure_l(model, rel_price, base_items, slope, base_price, rel_cost, margin_l)
            items_arr.append(items)
            m_items_arr.append(m_items)
            group_arr.append(group)

        items_full = np.concatenate(items_arr)
        m_items_full = np.concatenate(m_items_arr)
        group_full = np.concatenate(group_arr)

        for metrics_name, metrics_fn in metrics.items():
            result[name][metrics_name] = metrics_fn(items_full, m_items_full, group_full)
    return result


data = """base_price	slope	base_items	cost	hid	msku	d42_items
77.8147739802	7.9945373535	2799	65.2161058747374	982439	203727583	2799
111.1179941003	4.230465889	646	72.35752319392286	14994695	364975059	646
177.2049723757	4.7362794876	1740	152.18871258344666	15720054	241616044	1740
206.7837837838	2.923728466	37	121.89776000001001	15685457	511939032	37
214.0537190083	5.2475681305	478	141.1762234971478	91329	152408341	478
200.7169811321	1.5	48	124.21707733336399	15727886	715728813	48
151.9793103448	4.7396736145	117	155.2174672297594	91343	153252821	117
30.6666666667	1.5	21	25.97271553239428	91423	1699730598	21
492.315855181	3.2728517056	777	371.8031601262269	15368134	1974680806	777
237.1031746032	1.5	129	140.69454190475886	15720046	635219254	129
236.0157480315	4.3153905869	487	203.82382614700848	14621180	171117018	487
356.7083333333	1.5	24	216.55946934999318	14621180	605247026	24
78.9193362526	8.9891052246	4276	69.26876642926231	91329	661056234	4276
27.0	1.5	54	27.699759999998	15719828	873703123	54
1108.5714285714	2.8256931305	36	669.0191550000069	982439	759043084	36
138.7371428571	1.6450172663	175	76.27673666666666	15934091	354180132	175
62.3888888889	1.5	87	35.36270666666667	15714682	860513336	87
156.3409090909	2.131816864	78	133.50795134657196	91329	162661636	78
269.9135802469	6.4011993408	236	354.7355271837749	13360738	675381688	236
40.0	2.0352540016	31	21.672715532394278	91423	569252288	31
197.9122807018	1.5	74	133.59133522666548	15727878	921612764	74
39.34375	2.9217514992	32	28.8660174557225	91330	904947180	32
252.3291139241	2.9159660339	227	176.52108571829322	13314877	1936433688	227
72.4	2.6835446358	261	44.53411030777294	91423	414307888	261
103.2857142857	3.6792123318	138	96.66584775269831	15720056	486126180	138
283.623655914	4.1584892273	93	299.01894666666846	13360738	675379816	93
480.0833333333	1.5	10	384.14714412500024	91329	235757204	10
85.4583333333	1.5	24	72.79477999999999	91327	251066004	24
79.1799027553	7.6629314423	418	78.53826599116526	91329	754308796	418
83.72	1.5	48	93.56306666667334	14706137	651709156	48
47.0	1.8208770752	50	29.52271553239428	91423	414308148	50
27.28	1.5	50	21.672715532394278	91423	923620172	50
63.5777777778	1.5576286316	40	41.611093333332	91419	218214224	40
83.880952381	1.5	42	53.024760000006005	91344	482908004	42
142.3157894737	1.5	19	92.6543273846178	16593082	874589060	19
126.3804347826	1.5	92	94.23975999999999	90689	428592028	92
324.1904761905	1.5	12	274.7075559599902	15714122	674804644	12
75.253968254	1.5273747444	59	44.428501333332086	91419	218540004	59
57.8333333333	1.5	37	50.292736000004	91423	414307316	37
81.638623327	6.2085528374	514	57.59752889244871	91331	494682117	514
214.0238095238	1.5	31	191.54744861538705	14621180	546433089	31
145.1762452107	3.4875321388	249	118.40672509258354	15727896	471593025	249
66.0088691796	4.7019238472	435	42.746986538457435	15714680	586038421	435
155.0126582278	4.7345395088	141	129.15379062456535	91329	162662637	141
86.5145631068	1.5	103	70.00978000002	15720051	212822253	103
274.2272727273	1.8238792419	21	149.091799999984	91382	225699065	21
37.4831460674	2.2418591976	84	43.856402666669354	15714675	224175413	84
328.4013605442	2.5267891884	141	232.81463054544798	15714122	545999201	141
135.3286978508	3.8062703609	768	170.78153886461575	16099944	470087053	768
64.6935483871	2.2896852493	62	56.32612235346659	91329	950596017	62
96.4736842105	1.6753131151	176	84.77976000000001	14621180	157463001	176
228.1048387097	1.5	120	145.80909081579802	91346	186697245	120
1078.4339622642	2.7653005123	53	557.8658884821144	13041460	673016377	53
116.1230769231	2.4544267654	63	109.38104823999215	15720039	265356865	63
197.0	2.8394064903	26	149.8694693499932	14621180	281156181	26
67.4253731343	3.2857942581	298	54.6188386666684	13041431	1456969417	298
149.2615384615	3.6887626648	61	160.41400184623748	13360738	545542017	61
330.9836065574	4.0340662003	62	319.78936762769246	15714102	1456705413	62
171.9818181818	1.9839737415	43	150.74934574263344	91329	163583921	43
127.6833333333	1.5	50	108.9778143999944	90689	486113245	50
210.7824773414	5.8166270256	337	209.78072367433498	13360738	1478077413	337
17.4238310709	3.9336919785	3331	13.943292549019606	15557928	417283094	3331
52.2842105263	2.7600791454	95	41.396539033291496	91423	221885502	95
44.8333333333	1.5	25	39.314112000007995	15714135	549546082	25
163.4064171123	4.7261486053	169	145.106413783866	15727896	424091766	169
26.5217391304	1.5	82	37.413100500000596	15720388	204462226	82
74.7050691244	5.409183979	217	54.25368642394454	91329	143458478	217
188.2666666667	1.6939687729	15	91.43968000000001	15714122	933509402	15
83.3	1.5	15	83.3436479431468	15714122	150947102	15
70.4956521739	3.9655530453	353	50.91351209828591	91329	151671098	353
138.8156682028	3.7798500061	215	123.1994693499932	14621180	158325070	215
113.5918367347	1.5	49	84.41976000002	15714135	575774154	49
221.75	1.5	19	153.91612235346656	91329	157468162	19
81.2782608696	1.7679476738	117	79.33978000004001	15727878	545993286	117
99.0857142857	4.9617843628	137	103.32258172728774	91343	154657354	137
177.6052631579	1.5	36	164.42975999998	16331664	843306614	36
161.5238095238	1.5	20	122.344820000001	13334231	1729141382	20
143.7368421053	1.5	38	167.20976000004399	14621180	153253562	38
129.3144654088	1.5	99	137.64362116238019	91329	157247162	99
51.4533333333	1.5	67	46.291199999999	90689	427673318	67
244.421875	2.9971065521	58	194.490684581166	91382	224176882	58
118.8571428571	1.7679053545	14	84.001093333334	91342	622438278	14
88.7096774194	1.5	31	108.68950553893302	14706137	549338030	31
46.5675675676	1.5	74	31.3171691838584	15714135	549552062	74
144.0	1.5032866001	12	140.48340171117022	982439	430248007	12
64.1111111111	3.158788681	41	31.377170666666665	15714122	1401352271	41
57.5714285714	1.5	21	62.31305833333175	15720388	865614003	21
20.7962962963	1.5	486	20.50304446251824	14706137	298116319	486
360.4666666667	1.5667319298	15	257.2411133333065	91329	162661623	15
584.5123966942	4.8033485413	121	638.1332496021399	91346	661329195	121
1525.1666666667	4.3840618134	270	1118.103325562539	982439	663643507	270
57.7608695652	9.4714279175	141	56.71133522666548	15727878	545999279	141
100.6296296296	1.5	27	115.904646946765	15720051	212361663	27
159.75	1.5	19	70.335147999999	14621180	169422299	19
59.7056277056	1.8451946974	231	35.825856666667164	16147683	834968063	231
61.4263093788	3.3304464817	805	50.48447614241853	91331	159404551	805
177.5333333333	1.6079068184	15	152.516748	15714135	650311227	15
111.1294117647	3.7839827538	85	73.17338500000001	15727465	1699610231	85
206.5053763441	4.0399413109	86	196.387036617354	14621180	150334087	86
337.0434782609	1.5	23	235.44974725	12943705	693037703	23
"""

ground0 = pd.read_csv(io.StringIO(data), sep="\t")

np.random.seed(1234)

result = evaluate_metrics(
    models=models0,
    ground=ground0,
    metrics=metrics0,
    margin_l=10.0
)

pd.options.display.max_columns = None
pd.options.display.max_rows = None
pd.options.display.width = 1000

result_df = pd.DataFrame(result).transpose().sort_values(by=["lagrange"], ascending=False).reset_index()

print(result_df)

ranks = []
for metrics_name in metrics0:
    # print(f"ranks({metrics_name}): {np.array(result_df[metrics_name]).argsort()}")
    lagrange_diff = np.abs(
        np.array(result_df["lagrange"][np.array(result_df[metrics_name]).argsort()]) - np.array(result_df["lagrange"])
    ).sum() / 1000.0
    ranks_diff = np.abs(
        np.array(result_df[metrics_name]).argsort() - np.array(range(len(result_df)))
    ).sum()
    ranks.append({"metrics": metrics_name, "lagrange_diff":  lagrange_diff, "rank_diff": ranks_diff})

rank_df = pd.DataFrame(ranks).sort_values(by="lagrange_diff")
print(rank_df)

with open("/Users/avorozhtsov/tmp/lastic_metrics.csv", "w+") as f:
    f.write(result_df.to_csv())
