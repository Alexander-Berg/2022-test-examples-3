# -*- coding: utf-8 -*-

import imp
import os


def create_report_lock(cluster_index):
    try:
        import deploy
    except ImportError:
        deploy = imp.load_source('deploy', os.path.join(os.path.dirname(__file__), '..', 'report_deploy', 'deploy.py'))
    return deploy.HPReportLock(cluster_index)
