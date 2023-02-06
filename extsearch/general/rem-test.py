#!/usr/bin/env python
import sys
import time
import subprocess
from string import zfill
from optparse import OptionParser
from config import PrepareTags, Config, rem_master, mailrecipients, bindir, scrdir, remdir, monitoringdir

sys.path.append(remdir)
import remclient

parser = OptionParser()
parser.add_option('-m', '--mode', dest = 'mode', default = 'main')
parser.add_option('-i', '--newindex', dest = 'newindex', action = "store_true")
o, args = parser.parse_args()

c = Config(o.mode)

conn = remclient.Connector(rem_master)
TS, OLDTAGS, NEWTAG = PrepareTags(conn, [c.matching_tag_prefix], c.matching_tag_prefix, new_suffix = "_test{}".format(c.modesuffix))

if o.newindex:
    indexTS = "`ls -td {}{}000-* | grep -E \"[0-9]+$\" | head -1 | sed 's/.*-//'`".format(c.shardsdir, c.shardPrefix)
    ansName = "testindex_{}".format(indexTS)
else:
    ansName = TS

test_pack = conn.Packet(
    "test{}_pack_{}".format(c.modesuffix, TS),
    wait_tags = OLDTAGS,
    set_tag = NEWTAG,
    notify_emails = mailrecipients
)
if o.newindex:
    mv_indexinfo = test_pack.AddJob(
        shell = "cd {}; mv new_indexinfo old_indexinfo".format(c.testdir)
    )
    make_indexinfo = test_pack.AddJob(
        shell = "cat {}/music*{}/indexinfo.txt > {}new_indexinfo".format(c.shardsdir, indexTS, c.testdir),
        parents = [mv_indexinfo]
    )
copy_reqs = test_pack.AddJob(
    shell = "cd {}; cp requests {}".format(c.testdir, ansName)
)
make_ans = test_pack.AddJob(
    shell = "{}make_ans.py -i {}{} -s {}".format(scrdir, c.testdir, ansName, c.musicdubshost),
    parents = [copy_reqs]
)

if o.newindex:
    diff = test_pack.AddJob(
        shell = "cd {}; {}testdiff.py `ls -t *.ans|head -2|tail -1` {}.ans --i1 old_indexinfo --i2 new_indexinfo".format(c.testdir, scrdir, ansName),
        parents = [make_ans, make_indexinfo]
    )
else:
    diff = test_pack.AddJob(
        shell = "cd {}; {}testdiff.py `ls -t *.ans|head -2|tail -1` {}.ans".format(c.testdir, scrdir, ansName),
        parents = [make_ans]
    )
lastDiff = "`ls -t {}*.diff|head -1`".format(c.testdir)
mail = test_pack.AddJob(
    shell = "if [ -s {} ]; then echo {}; cat {} | mail -s \"music{}_match test: \" {}; fi".format(lastDiff, lastDiff, lastDiff, c.modesuffix, " ".join(mailrecipients)),
    parents = [diff]
)
lastLog = "`ls -t {}*.log|head -1`".format(c.testdir)
mail_error = test_pack.AddJob(
    shell = "if [ -s {} ]; then echo {}; cat {} | mail -s \"music{}_match test errors\" {}; fi".format(lastLog, lastLog, lastLog, c.modesuffix, " ".join(mailrecipients)),
    parents = [diff]
)
clean = test_pack.AddJob(
    shell = "touch {}requests; find {} -mtime +{} -delete".format(c.testdir, c.testdir, c.backuptime),
    parents = [mail, mail_error]
)
conn.Queue(c.matching_queue).AddPacket(test_pack)

if (o.newindex):
    finish_pack = conn.Packet(
        "finish_test{}_pack_{}".format(c.modesuffix, TS),
        wait_tags = [NEWTAG],
        notify_emails = mailrecipients
    )
    finish_pack.AddJob(
        shell = "echo $(date +%s) > {}matching{}_rem_test.last_finish".format(monitoringdir, c.modesuffix)
    )
    conn.Queue(c.finish_queue).AddPacket(finish_pack)
