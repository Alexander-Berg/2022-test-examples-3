class SeqNoCounter(object):
    count = 0

    @staticmethod
    def next_count():
        SeqNoCounter.count += 1
        return SeqNoCounter.count
