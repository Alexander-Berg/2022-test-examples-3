import random, os, itertools
from subprocess import Popen, PIPE

def rblocks(f, blocksize = 4096):
    if 'b' not in f.mode.lower():
        raise Exception("file must be opened using binary mode.")

    size = os.stat(f.name).st_size
    fullblocks, lastblock = divmod(size, blocksize)
    f.seek(-lastblock, 2)

    yield f.read(lastblock)

    for i in range(fullblocks - 1, -1, -1):
        f.seek(i * blocksize)
        yield f.read(blocksize)

def rreadlines(f):
    buf = ''

    for block in rblocks(f):
        buf = block + buf
        lines = buf.splitlines()

        if lines:
            buf = lines[0]

            for l in reversed(lines[1:]):
                yield l

    if buf:
        yield buf

class Stat(object):
    def __init__(self):
        self.samples = []
        self.counts = 0

    def appendSample(self, sample):
        self.samples.append(sample)

    def appendCount(self):
        self.counts += 1

    def mergeWith(self, stat):
        self.samples += stat.samples
        self.counts += stat.counts

class Stats(object):
    def __init__(self, percents, fr, to):
        self.percents = percents
        self.fr = fr
        self.to = to
        self.evlogdump_suff = '/upper_bundle/bin/evlogdump'
        self.apachecfg_suff = '/conf/apache.ywsearch.cfg'
        self.blockstat = '/hol/www/logs/blockstat_log'
        self.bsconfig = '/Berkanavt/bin/scripts/bsconfig'

    def run(self):
        import random, os
        os.nice(10)

        ret = {}

        for (source, time) in self.collectSamples():
            if source not in ret:
                ret[source] = Stat()

            stat = ret[source]

            if source in self.percents:
                if random.random() < self.percents[source]:
                    stat.appendSample(time)

            stat.appendCount()

        return ret

    def collectSamples(self):
        idx2name = self.parseSourceNames()
        shows = self.parseShows()
        inprogr = {}
        knownsrc = {
              'WEB' : ['source=web', 'source=foreign', 'source=diversity']
            , 'WEB_EXPERIMENTAL' : ['source=web_experimental']
            , 'QUICK' : ['source=quick']
            , 'QUICK_EXPERIMENTAL' : ['source=quick']
            , "ADRESA" : ["/wiz/adresa/"]
            , "AFISHA" : ["/wiz/afisha/", "/wiz/rubrics/afisha/"]
            , "ASSOCQ": ["/group/related_query/"]
            , "AUTO" : ["/wiz/auto/"]
            , "BOOKS" : ["source=books"]
            , "DOSSIERP" : ["/wiz/pressp/"]
            , "DOSSIERPSLOW" : ["/wiz/pressp/"]
            , "ENCYC" : ["/wiz/encyc/"]
            , "IMAGESP" : ["/wiz/images/", "/miniimg/"]
            , "IMAGESQUICKP" : ["/wiz/images/", "/miniimg/"]
            , "IMAGES" : ["/image/single/", "/image/slideshow/"]
            , "IMAGESQUICK" : ["/image/single/", "/image/slideshow/"]
            , "KINO" : ["/wiz/kino/"]
            , "LINGVO" : ["/wiz/lingvo/"]
            , "MAP2" : ["/wiz/maps/"]
            , "MISSPELL" : ["/wiz/misspell/", "/reask/"]
            , "MUSIC" : ["source=music"]
            , "NEWSP" : ["/wiz/news/", "/wiz/rubrics/news/"]
            , "NEWSP_ENG" : ["/wiz/news/", "/wiz/rubrics/news/"]
            , "MININEWSP" : ["/wiz/news/", "/wiz/rubrics/news/"]
            , "NEWSP_LINKS" : ["source=newsp_links"]
            , "POETRY" : ["/wiz/poetry_lover/"]
            , "PROG" : ["/wiz/developer/"]
            , "RABOTA" : ["/wiz/rabota/"]
            , "RASPISANIE" : ["/wiz/rasp/"]
            #, "REALTIME" : ["realtime"]
            , "FASTTIER" : ["fasttier"]
            , "REALTY" : ["/wiz/realty/"]
            , "REGIONUS" : ["source=regionus"]
            , "RUSLANG" : ["/wiz/rus_lang/"]
            , "TRAINPP" : ["/wiz/rasp/"]
            , "TV" : ["/wiz/tv/"]
            , "UNITCONVERTER" : ["/wiz/converter/"]
            , "USLUGI" : ["/wiz/uslugi/"]
            , "VIDEOP" : ["/video/", "source=videop"]
            , "VIDEOQUICK" : ["source=videoquick"]
            , "VIDEOQUICKP" : ["source=videoquick"]
            , "VIDEOULTRA" : ["source=videoultra"]
            , "VIDEOULTRAP" : ["source=videoultra"]
            , "WIKIFACTS" : ["/wiz/wikifacts/", "main=wikifacts"]
            , "YMUSIC" : ["/wiz/musicplayer/"]
            , "LYRICS" : ["/wiz/lyrics/"]
            , "YARES" : ["/wiz/topic/catalog"]
            , "YARES_UA" : ["/wiz/topic/catalog"]
            , "TOPSYP" : ["/snippet/tweet/more"]
            , "TOPSY" : ["filter=freshtweets"]
            , "PEOPLEP" : ["source=peoplep"]
        }

        def parseReqId(line):
            return line.partition('&reqid=')[2].partition('&')[0]

        cur_req_type = ''

        layers = ['REQUESTED', 'INITED', 'CONNECTED', 'ANSWERED', 'SHOWED']

        for event in self.parseEventLog():
            name = event[2]

            if len(cur_req_type) == 0 and name != 'ReportMessage':
                continue

            if name == 'ReportMessage':
                if event[4] == 'done':
                    cur_req_type = ''
                    continue
                cur_req_type = self.typeFromEvent(event)
                for tid in idx2name:
                    for i in [ 1, 2, 3, 4 ]:
                        yield ((idx2name[tid], 'all', 'REQUESTED', layers[i]), 10000)
                        yield ((idx2name[tid], cur_req_type, 'REQUESTED', layers[i]), 10000)
                continue
            key = self.idFromEvent(event)
            src = idx2name[key[1]]

            if name == 'SubSourceInit':
                for i in [ 0 ]:
                    yield ((src, 'all', layers[i], 'INITED'), 0)
                    yield ((src, cur_req_type, layers[i], 'INITED'), 0)
                for i in [ 2, 3, 4 ]:
                    yield ((src, 'all', 'INITED', layers[i]), 10000)
                    yield ((src, cur_req_type, 'INITED', layers[i]), 10000)
                yield ((src, 'all', 'INITED', 'ERROR'), 10000)
                yield ((src, cur_req_type, 'INITED', 'ERROR'), 10000)
            elif name == 'SubSourceRequest':
                inprogr[key] = (long(event[0]), parseReqId(event[7]))
                for i in [ 0, 1 ]:
                    yield ((src, 'all', layers[i], 'CONNECTED'), 0)
                    yield ((src, cur_req_type, layers[i], 'CONNECTED'), 0)
                for i in [ 3, 4 ]:
                    yield ((src, 'all', 'CONNECTED', layers[i]), 10000)
                    yield ((src, cur_req_type, 'CONNECTED', layers[i]), 10000)
                yield ((src, 'all', 'CONNECTED', 'ERROR'), 10000)
                yield ((src, cur_req_type, 'CONNECTED', 'ERROR'), 10000)
            elif name == 'SubSourceError':
                if key in inprogr:
                    yield ((src, 'all', 'CONNECTED', 'ERROR'), 0)
                    yield ((src, cur_req_type, 'CONNECTED', 'ERROR'), 0)
                else:
                    yield ((src, 'all', 'INITED', 'ERROR'), 0)
                    yield ((src, cur_req_type, 'INITED', 'ERROR'), 0)
            elif name == 'SubSourceOk':
                if key in inprogr:
                    for i in [ 0, 1, 2 ]:
                        yield ((src, 'all', layers[i], 'ANSWERED'), 0)
                        yield ((src, cur_req_type, layers[i], 'ANSWERED'), 0)
                    for i in [ 4 ]:
                        yield ((src, 'all', 'ANSWERED', layers[i]), 10000)
                        yield ((src, cur_req_type, 'ANSWERED', layers[i]), 10000)

                    (start, reqid) = inprogr[key]
                    duration = long(event[0]) - start
                    del inprogr[key]

                    yield ((src, 'all', 'ANSWERED', 'DURATION'), duration)
                    yield ((src, cur_req_type, 'ANSWERED', 'DURATION'), duration)

                    if src in knownsrc:
                        if reqid in shows:
                            blockstat = shows[reqid]

                            flag = 0
                            for substr in knownsrc[src]:
                                if blockstat.find(substr) != -1:
                                    flag = 1

                            if flag == 1:
                                for i in [ 0, 1, 2, 3 ]:
                                    yield ((src, 'all', layers[i], 'SHOWED'), 0)
                                    yield ((src, cur_req_type, layers[i], 'SHOWED'), 0)
                                yield ((src, 'all', 'SHOWED', 'DURATION'), duration)
                                yield ((src, cur_req_type, 'SHOWED', 'DURATION'), duration)

    def idFromEvent(self, event):
        return (event[1], long(event[3]))

    def typeFromEvent(self, event):
        return event[5].strip()

    def parseShows(self):
        ret = {}

        with open(self.blockstat, 'rb') as f:
            for line in rreadlines(f):
                (time, x, data) = line.partition('\t')
                time = long(time)

                if time < self.fr - 1:
                    break

                if time < self.to + 1:
                    spl = data.split('\t')
                    ret[spl[9]] = data

        return ret

    def getConfBase(self):
        cmd = '%s list | grep upper | grep Name | head -n 1' % (self.bsconfig)
        proc = Popen(cmd, shell = True, stdout = PIPE)

        try:
            name = '/hol/arkanavt-' + proc.stdout.readline().strip().rpartition('-')[2]
        except:
            name = '/hol/arkanavt'

        return name

    def parseEventLog(self):
        fr = long(self.fr * 1000000)
        to = long(self.to * 1000000)

        cmd = '%s -o -i298,300,302,303,353 -s %s -e %s %s' % (self.getConfBase() + self.evlogdump_suff, fr, to, self.eventlog)
        proc = Popen(cmd, shell = True, stdout = PIPE)

        for line in proc.stdout:
            yield line.split('\t')

        if proc.returncode:
            raise Exception('shit happens')

    def parseSourceNames(self):
        cnt = 0
        ret = {}

        def parseAfter(line, after):
            p = line.find(after)

            if p == -1:
                return None

            ret = line[p + len(after):]
            ret = ret.strip()

            return ret

        with open(self.getConfBase() + self.apachecfg_suff, 'r') as f:
            for line in f.readlines():
                line = line[:-1]
                res = parseAfter(line, 'ServerDescr')

                if res:
                    ret[cnt] = res
                    cnt += 1
                else:
                    cfg = parseAfter(line, 'EventLog')

                    if cfg:
                        self.eventlog = cfg

        return ret


def mergeStatsTo(fr, to):
    for (source, stat) in fr.items():
        if source in to:
            to[source].mergeWith(stat)
        else:
            to[source] = stat

def main():
    import time
    percents = {}
    to = time.time()
    fr = to - 10
    print Stats(percents, fr, to).run()

if __name__ == '__main__':
    main()
