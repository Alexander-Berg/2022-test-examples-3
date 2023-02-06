#!/usr/bin/env python3

import sys
import optparse
import json
import subprocess

ACCEPT = "junk/accept/" # Path to the compare pools.

FACTORS = ".factors"
OUTPUT = ".output"
OFFSET = 4
HEAD = 2
CLASSIFIER_TYPES = 5

# Selected features for new classifier.
CUT_F = ' | cut -f1,2,3,4,67,111,116,128,156,157,159,160,191,227,228,229,231,233,245,246,248,255,256,269,353,370,374,395,396,401,402,403,404,425,427,429,430,431,432,435,437,438,439,452,453,473,534,536,541,542,544,550,559,560,573,580,591,592,597,603,610,626,627,630,632,633,634,635,636,637,642,643,657,659,671,673,674,675,677,678,679,680,681,683,686,687,688,698,699,700,701,704,705,706,708,710,712,713,723,724,725,728,730,738,747,748,749,757,758,760,761,762,763,770,772,787,789,791,792,793,794,795,798,800,801,802,803,804,805,806,807,808,809,810,811,812,813,814,815,816,817,819,820,850,851,858,859,861,865,868,871,896,902,904,910,911,925,927,958,963,968,981,982,983,984,985,986,987,988,989,990,991,992,993,994,995,996,1001,1003,1006,1007,1011,1012,1013,1014,1020,1022,1026,1037,1040,1042,1043,1045,1054,1066,1085,1087,1091,1092,1093,1094,1101,1113,1123,1124,1149,1157,1174,1176,1177,1180,1184,1224,1226,1244,1267,1277,1279,1284,1297,1321,1346,1352,1355,1357,1358,1361,1363,1367,1368,1369,1370,1371,1372,1375,1383,1384,1400,1401,1405,1407,1413,1414,1431,1432,1478,1496,1497,1501,1502,1539,1555,1563,1564,1568,1575,1576,1582,1587,1607,1609,1610,1620,1621,1629,1630,1632,1674,1685,1687,1688,1701,1732'

FILES = [
    ("org1_relev_desktop", "DESKTOP", "SINGLE_ORG"),
    ("org1_relev_touch", "TOUCH", "SINGLE_ORG"),

    ("chain_relev_desktop", "DESKTOP", "CHAIN"),
    ("chain_relev_touch", "TOUCH", "CHAIN"),

    ("rubric_relev_desktop", "RUBRIC", "DESKTOP"),
    ("rubric_relev_touch", "RUBRIC", "TOUCH")
]

awkt = 'awk -v OFS="\\t" -F"\\t" '
sus = ' sort | uniq -c | sort -g '

def print_comparison(pool, results, previous_results):
    print(" ".join(["====", pool[0], pool[1], pool[2], "===="]))
    print(results)
    print("---- Previous classifier quality ----")
    print(previous_results)

def main():
    usage = "Usage: %prog [OPTIONS]"
    description = "Test multiclassifier on accept pools."
    op = optparse.OptionParser(usage, description = description)
    (options, arguments) = op.parse_args()

    for pool in FILES:
        path = ACCEPT + pool[0]
        factors_path = path + FACTORS
        output_path = path + OUTPUT

        # Get current factors (grab.py + parse_apply_blender_output.py in get_factors.sh).
        prepare_pool = awkt + ' \'{$3 = "0"; $4 = "0"; print }\' ' + path
        get_factors = ' ./get_factors.sh '
        if pool[1] == "TOUCH":
            get_factors += ' --touch '
        subprocess.getoutput(prepare_pool + ' | ' + get_factors + CUT_F + ' > ' + factors_path)

        # Calc mxmodel result on factors.
        model = '4_269_1600i.mnmc'
        mx_call = 'mx_ops calcmc ' + model + ' -s 4 -i ' + factors_path + ' -o ' + output_path
        subprocess.getoutput(mx_call)

        # Get current classifier results.
        previous_grab = "grab.py --threads=64 --method='upper_property' --arg='OrgWizard.QueryType'"
        if (pool[1]) == "TOUCH":
            previous_grab += ' --touch '
        previous_results_call = 'cat ' + path + ' |' + previous_grab + ' | cut -f3 | ' + sus

        process_call = ('cat ' + output_path + ' | ./process_mx_ops_result.py | cut -f2 | ' + sus)

        print_comparison(pool,
                         subprocess.getoutput(process_call),
                         subprocess.getoutput(previous_results_call))


if __name__ == "__main__":
    main()
