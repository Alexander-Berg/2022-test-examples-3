import luigi


def run_luigi_task(task):
    return luigi.build([task], detailed_summary=True, local_scheduler=True, no_lock=True, workers=1)
