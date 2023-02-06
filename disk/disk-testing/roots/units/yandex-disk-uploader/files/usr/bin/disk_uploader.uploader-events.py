#!/usr/bin/env python

# -*- coding: UTF-8 -*-

import os
import re
import sys

results_errors = { 'parse' : 0 }

meters_success = [ 'true',
'false',
'unknown',
'all']


results_count_success = dict()
for sk_key in meters_success:
    results_count_success[sk_key] = 0
results_count_success['others'] = 0

meters_types = {'uploadtodefault' : ['UploadToDefault'],
                'uploadfromservice' : ['UploadFromService'],
                'patchatdefault' : ['PatchAtDefault'],
                'generatepreview' : ['GeneratePreview'],
                'regenaratepreview' : ['RegeneratePreview'],
                'zipfolder' : ['ZipFolder'] }


types_meters = dict()
for meter,types in meters_types.items():
    for type in types:
        types_meters[type] = meter

meters_types['others'] = None
results_count_types = dict() 
for mt_key in meters_types:
    results_count_types[mt_key] = dict()
    for msu_key in meters_success:
        results_count_types[mt_key][msu_key] = 0


meters_stages = {
    'originalfile' : ['originalFile'],
    'generatepreview' : ['generatePreview'],
    'userfile' : ['userFile'],
    'commitfinal' : ['commitFinal'],
    'pp_commitfileinfo' : ['pp.commitFileInfo'],
    'payloadinfo' : ['payloadInfo'],
    'pp_antivirusresult2' : ['pp.antivirusResult2'],
    'pp_commitfileupload' : ['pp.commitFileUpload'],
    'pp_filemulcauploadinfo' : ['pp.fileMulcaUploadInfo'],
    'pp_digestmulcauploadinfo' : ['pp.digestMulcaUploadInfo'],
    'pp_exifinfo' : ['pp.exifInfo'],
    'pp_pi_generateonepreview' : ['pp.pi.generateOnePreview'],
    'pp_pi_previewmulcauploadinfo' : ['pp.pi.previewMulcaUploadInfo'],
    'md_d' : ['md.d'],
    'pp_pd_generatepreview' : ['pp.pd.generatePreview'],
    'pp_pd_previewmulcauploadinfo' : ['pp.pd.previewMulcaUploadInfo'],
    'incomingpatch' : ['incomingPatch'],
    'expectedpatchedmd5' : ['expectedPatchedMd5'],
    'originalfile2' : ['originalFile2'],
    'patchedfile' : ['patchedFile'],
    'patchedpayloadinfo' : ['patchedPayloadInfo'],
    'addlogo' : ['addLogo'],
    'upload' : ['upload'],
    'pp_videoinfo' : ['pp.videoInfo'],
    'pp_mediainfo' : ['pp.mediaInfo'],
    'pp_pv_generatepreview' : ['pp.pv.generatePreview'],
    'pp_pv_previewmulcauploadinfo' : ['pp.pv.previewMulcaUploadInfo'],
    'mpfsfulltree' : ['mpfsFullTree'],
    'parsedfulltree' : ['parsedFullTree'],
    'internalerror' : ['internalError'],
    'downloadedfilefromservice2' : ['downloadedFileFromService2'],
    'downloadedfileinfo' : ['downloadedFileInfo'],
    'pp_filemulcaremoveinfo2' : ['pp.fileMulcaRemoveInfo2'],
    'pp_digestmulcaremoveinfo2' : ['pp.digestMulcaRemoveInfo2'],
}

stages_meters = dict()
for meter,stages in meters_stages.items():
    for stage in stages:
        stages_meters[stage] = meter

meters_stages['others'] = None
results_count_stages = dict() 
results_timings_stages = dict()
for ms_key in meters_stages:
    results_timings_stages[ms_key] = []
    results_count_stages[ms_key] = dict()
    for msu_key in meters_success:
        results_count_stages[ms_key][msu_key] = 0 

#####


index_re = re.compile(' type=([^,]+), stage=([^,\[]+)[,\[](?:[^,]+,)? duration=([^,]+),.*, success=(\w+)')

for line in sys.stdin:
    
    line = line.strip()
    matches = index_re.findall(line)

    if len(matches):
        b_type = matches[0][0]
        b_stage = matches[0][1]
        u_duration = matches[0][2]
        b_success = matches[0][3]
#        print "%s, %s, %s, %s" % (b_type,b_stage,b_duration,b_success)

        if  b_type in types_meters:
            u_type = types_meters[b_type]
        else:
	    u_type = 'others'

        if b_stage in stages_meters:
            u_stage = stages_meters[b_stage]
            results_timings_stages[u_stage].append(u_duration)
        else:
            u_stage = 'others'

        if b_success in results_count_success:
            u_success = b_success
        else:
            u_success = 'others'

#        print "%s => %s, %s => %s, %s => %s, %s => %s" % (b_type,u_type,b_stage,u_stage,b_duration,u_duration,b_success,u_success)

        results_count_types[u_type][u_success] += 1
        results_count_stages[u_stage][u_success] += 1

        results_count_types[u_type]['all'] += 1
 	results_count_stages[u_stage]['all'] += 1

    else:
	results_errors['parse'] += 1
#        print line


for u_type, result in sorted(results_count_types.items()):
    for u_success, value in sorted (result.items()):
	print("uploader_count_type_%s_%s %d" % (u_type, u_success, value))


for u_stage, result in sorted(results_count_stages.items()):
    for u_success, value in sorted (result.items()):
        results_count_success[u_success] += value
        print("uploader_count_stage_%s_%s %d" % (u_stage, u_success, value))


for u_success, value in sorted(results_count_success.items()):
    print("uploader_count_stage_total_%s %d" % (u_success, value))


for u_stage, result in sorted(results_timings_stages.items()):
    print("uploader_timings_stage_%s %s" % (u_stage, ' '.join(result) ))


for error, value in sorted(results_errors.items()):
    print("uploader_count_events_error_%s %d" % (error, value))



sys.exit(0)

