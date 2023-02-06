import os

SERVICE_NAME = 'TEST_SERVICE_NAME'

NEIGHBOURS_NUMBER = 20

CRYPTA_YT_WORKING_DIR = '//home/crypta/production'
SIBERIA_DIR = os.path.join(CRYPTA_YT_WORKING_DIR, 'siberia')
CUSTOM_AUDIENCE_DIR = os.path.join(SIBERIA_DIR, 'custom_audience')

CENTROIDS_TABLE = os.path.join(CUSTOM_AUDIENCE_DIR, 'clusters')
MONTHLY_CLUSTERING_DIR = os.path.join(CUSTOM_AUDIENCE_DIR, 'monthly_clustering')
USERID_CLUSTERID_TABLE = os.path.join(CUSTOM_AUDIENCE_DIR, 'userid_clusterid')
SEGMENTID_USERID_TABLE = os.path.join(CUSTOM_AUDIENCE_DIR, 'segmentid_userid_table')

DATALENS_METRICS_TABLE = os.path.join(CUSTOM_AUDIENCE_DIR, 'metrics_table')
