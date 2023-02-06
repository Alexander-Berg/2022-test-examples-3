"""
    Test data for grafana
"""

EXAMPLE_DASHBOARD_RESPONSE = {
  "meta": {
    "type": "db",
    "canSave": True,
    "canEdit": True,
    "canAdmin": False,
    "canStar": True,
    "slug": "strm-rtmp-stream-quality",
    "url": "/d/000016640/strm-rtmp-stream-quality",
    "expires": "0001-01-01T00:00:00Z",
    "created": "2018-02-16T18:11:22+03:00",
    "updated": "2019-07-10T04:14:40+03:00",
    "updatedBy": "xoiss",
    "createdBy": "xoiss",
    "version": 18,
    "hasAcl": False,
    "isFolder": False,
    "folderId": 0,
    "folderTitle": "General",
    "folderUrl": "",
    "provisioned": False
  },
  "dashboard": {
    "annotations": {
      "list": [
        {
          "builtIn": 1,
          "datasource": "-- Grafana --",
          "enable": True,
          "hide": True,
          "iconColor": "rgba(0, 211, 255, 1)",
          "name": "Annotations & Alerts",
          "type": "dashboard"
        }
      ]
    },
    "editable": True,
    "gnetId": None,
    "graphTooltip": 0,
    "id": 16640,
    "iteration": 1562720332051,
    "links": [],
    "panels": [
      {
        "aliasColors": {},
        "bars": False,
        "dashLength": 10,
        "dashes": False,
        "datasource": "gr-mg",
        "fill": 1,
        "gridPos": {
          "h": 7,
          "w": 12,
          "x": 0,
          "y": 0
        },
        "id": 2,
        "legend": {
          "avg": False,
          "current": False,
          "max": False,
          "min": False,
          "show": True,
          "total": False,
          "values": False
        },
        "lines": True,
        "linewidth": 1,
        "links": [],
        "nullPointMode": "null",
        "percentage": False,
        "pointradius": 5,
        "points": False,
        "renderer": "flot",
        "seriesOverrides": [],
        "spaceLength": 10,
        "stack": False,
        "steppedLine": False,
        "targets": [
          {
            "refId": "A",
            "target": "aliasByNode(media.strm.$environment.channel.$channel_a.source.$publisher_a.stream_quality.video.frms_num, -2)"
          },
          {
            "refId": "B",
            "target": "aliasByNode(media.strm.$environment.channel.$channel_a.source.$publisher_a.stream_quality.audio.frms_num, -2)"
          }
        ],
        "thresholds": [],
        "timeFrom": None,
        "timeRegions": [],
        "timeShift": None,
        "title": "Frames received -- $channel_a -- $publisher_a",
        "tooltip": {
          "shared": True,
          "sort": 0,
          "value_type": "individual"
        },
        "type": "graph",
        "xaxis": {
          "buckets": None,
          "mode": "time",
          "name": None,
          "show": True,
          "values": []
        },
        "yaxes": [
          {
            "decimals": 0,
            "format": "none",
            "label": None,
            "logBase": 1,
            "max": "5000",
            "min": "0",
            "show": True
          },
          {
            "decimals": 0,
            "format": "none",
            "label": None,
            "logBase": 1,
            "max": "5000",
            "min": "0",
            "show": True
          }
        ],
        "yaxis": {
          "align": False,
          "alignLevel": None
        }
      },
      {
        "aliasColors": {},
        "bars": False,
        "dashLength": 10,
        "dashes": False,
        "datasource": "gr-mg",
        "fill": 1,
        "gridPos": {
          "h": 7,
          "w": 12,
          "x": 12,
          "y": 119
        },
        "id": 43,
        "legend": {
          "avg": False,
          "current": False,
          "max": False,
          "min": False,
          "show": True,
          "total": False,
          "values": False
        },
        "lines": True,
        "linewidth": 1,
        "links": [],
        "nullPointMode": "null",
        "percentage": False,
        "pointradius": 5,
        "points": False,
        "renderer": "flot",
        "seriesOverrides": [],
        "spaceLength": 10,
        "stack": False,
        "steppedLine": False,
        "targets": [
          {
            "refId": "A",
            "target": "aliasByNode(media.strm.$environment.channel.$channel_b.source.$publisher_b.stream_quality.common.status, -1)",
            "textEditor": False
          }
        ],
        "thresholds": [],
        "timeFrom": None,
        "timeRegions": [],
        "timeShift": None,
        "title": "Stream status -- $channel_b -- $publisher_b",
        "tooltip": {
          "shared": True,
          "sort": 0,
          "value_type": "individual"
        },
        "type": "graph",
        "xaxis": {
          "buckets": None,
          "mode": "time",
          "name": None,
          "show": True,
          "values": []
        },
        "yaxes": [
          {
            "decimals": 0,
            "format": "short",
            "label": None,
            "logBase": 1,
            "max": "2",
            "min": "0",
            "show": True
          },
          {
            "decimals": 0,
            "format": "short",
            "label": None,
            "logBase": 1,
            "max": "2",
            "min": "0",
            "show": True
          }
        ],
        "yaxis": {
          "align": False,
          "alignLevel": None
        }
      }
    ],
    "refresh": "1m",
    "schemaVersion": 18,
    "style": "dark",
    "tags": [],
    "templating": {
      "list": [
        {
          "allValue": None,
          "current": {
            "text": "stable",
            "value": "stable"
          },
          "datasource": "gr-mg",
          "definition": "",
          "hide": 0,
          "includeAll": False,
          "label": "Environment",
          "multi": False,
          "name": "environment",
          "options": [],
          "query": "media.strm.*",
          "refresh": 2,
          "regex": "(stable.*|testing.*|development.*|debug.*)",
          "skipUrlSync": False,
          "sort": 1,
          "tagValuesQuery": "",
          "tags": [],
          "tagsQuery": "",
          "type": "query",
          "useTags": False
        },
        {
          "allValue": None,
          "current": {
            "text": "9may_2line_source",
            "value": "9may_2line_source"
          },
          "datasource": "gr-mg",
          "definition": "",
          "hide": 0,
          "includeAll": False,
          "label": "Channel A",
          "multi": False,
          "name": "channel_a",
          "options": [],
          "query": "media.strm.$environment.channel.*",
          "refresh": 2,
          "regex": "(?!.*-src|.*strm_yandex_net).*$",
          "skipUrlSync": False,
          "sort": 1,
          "tagValuesQuery": "",
          "tags": [],
          "tagsQuery": "",
          "type": "query",
          "useTags": False
        },
        {
          "allValue": None,
          "current": {
            "text": "src-rtmp-mskm903_strm_yandex_net",
            "value": "src-rtmp-mskm903_strm_yandex_net"
          },
          "datasource": "gr-mg",
          "definition": "",
          "hide": 0,
          "includeAll": False,
          "label": "Publisher A",
          "multi": False,
          "name": "publisher_a",
          "options": [],
          "query": "media.strm.$environment.channel.$channel_a.source.*_yandex_net",
          "refresh": 2,
          "regex": "",
          "skipUrlSync": False,
          "sort": 1,
          "tagValuesQuery": "",
          "tags": [],
          "tagsQuery": "",
          "type": "query",
          "useTags": False
        },
        {
          "allValue": None,
          "current": {
            "text": "9may_2line",
            "value": "9may_2line"
          },
          "datasource": "gr-mg",
          "definition": "",
          "hide": 0,
          "includeAll": False,
          "label": "Channel B",
          "multi": False,
          "name": "channel_b",
          "options": [],
          "query": "media.strm.$environment.channel.*",
          "refresh": 2,
          "regex": "(?!.*-src|.*strm_yandex_net).*$",
          "skipUrlSync": False,
          "sort": 1,
          "tagValuesQuery": "",
          "tags": [],
          "tagsQuery": "",
          "type": "query",
          "useTags": False
        },
        {
          "allValue": None,
          "current": {
            "text": "nnmetr-src-rtmp-mskm903_strm_yandex_net",
            "value": "nnmetr-src-rtmp-mskm903_strm_yandex_net"
          },
          "datasource": "gr-mg",
          "definition": "",
          "hide": 0,
          "includeAll": False,
          "label": "Publisher B",
          "multi": False,
          "name": "publisher_b",
          "options": [],
          "query": "media.strm.$environment.channel.$channel_b.source.*_yandex_net",
          "refresh": 2,
          "regex": "",
          "skipUrlSync": False,
          "sort": 1,
          "tagValuesQuery": "",
          "tags": [],
          "tagsQuery": "",
          "type": "query",
          "useTags": False
        }
      ]
    },
    "time": {
      "from": "now-15m",
      "to": "now"
    },
    "timepicker": {
      "refresh_intervals": [
        "1m",
        "5m",
        "15m",
        "30m",
        "1h",
        "2h",
        "1d"
      ],
      "time_options": [
        "5m",
        "15m",
        "1h",
        "6h",
        "12h",
        "24h",
        "2d",
        "7d",
        "30d"
      ]
    },
    "timezone": "",
    "title": "STRM: RTMP Stream Quality",
    "uid": "000016640",
    "version": 18
  }
}

EXAMPLE_SNAPSHOT_RESPONSE = {
  "meta": {
    "isSnapshot": True,
    "type": "snapshot",
    "canSave": False,
    "canEdit": False,
    "canAdmin": False,
    "canStar": False,
    "slug": "",
    "url": "",
    "expires": "2069-07-12T02:00:30+03:00",
    "created": "2019-07-25T02:00:30+03:00",
    "updated": "0001-01-01T00:00:00Z",
    "updatedBy": "",
    "createdBy": "",
    "version": 0,
    "hasAcl": False,
    "isFolder": False,
    "folderId": 0,
    "folderTitle": "",
    "folderUrl": "",
    "provisioned": False
  },
  "dashboard": {
    "annotations": {
      "list": [
        {
          "builtIn": 1,
          "datasource": "-- Grafana --",
          "enable": True,
          "hide": True,
          "iconColor": "rgba(0, 211, 255, 1)",
          "name": "Annotations & Alerts",
          "type": "dashboard"
        }
      ]
    },
    "editable": True,
    "gnetId": None,
    "graphTooltip": 0,
    "id": 16640,
    "iteration": 1564005558584,
    "links": [],
    "panels": [
      {
        "aliasColors": {},
        "bars": False,
        "dashLength": 10,
        "dashes": False,
        "datasource": "gr-mg",
        "fill": 1,
        "gridPos": {
          "h": 7,
          "w": 12,
          "x": 0,
          "y": 0
        },
        "id": 2,
        "legend": {
          "avg": False,
          "current": False,
          "max": False,
          "min": False,
          "show": True,
          "total": False,
          "values": False
        },
        "lines": True,
        "linewidth": 1,
        "links": [],
        "nullPointMode": "null",
        "percentage": False,
        "pointradius": 5,
        "points": False,
        "renderer": "flot",
        "seriesOverrides": [],
        "spaceLength": 10,
        "stack": False,
        "steppedLine": False,
        "targets": [
          {
            "refId": "A",
            "target": "aliasByNode(media.strm.$environment.channel.$channel_a.source.$publisher_a.stream_quality.video.frms_num, -2)"
          },
          {
            "refId": "B",
            "target": "aliasByNode(media.strm.$environment.channel.$channel_a.source.$publisher_a.stream_quality.audio.frms_num, -2)"
          }
        ],
        "thresholds": [],
        "timeFrom": None,
        "timeRegions": [],
        "timeShift": None,
        "title": "Frames received -- $channel_a -- $publisher_a",
        "tooltip": {
          "shared": True,
          "sort": 0,
          "value_type": "individual"
        },
        "type": "graph",
        "xaxis": {
          "buckets": None,
          "mode": "time",
          "name": None,
          "show": True,
          "values": []
        },
        "yaxes": [
          {
            "decimals": 0,
            "format": "none",
            "label": None,
            "logBase": 1,
            "max": "5000",
            "min": "0",
            "show": True
          },
          {
            "decimals": 0,
            "format": "none",
            "label": None,
            "logBase": 1,
            "max": "5000",
            "min": "0",
            "show": True
          }
        ],
        "yaxis": {
          "align": False,
          "alignLevel": None
        }
      },
      {
        "aliasColors": {},
        "bars": False,
        "dashLength": 10,
        "dashes": False,
        "datasource": "gr-mg",
        "fill": 1,
        "gridPos": {
          "h": 7,
          "w": 12,
          "x": 12,
          "y": 119
        },
        "id": 43,
        "legend": {
          "avg": False,
          "current": False,
          "max": False,
          "min": False,
          "show": True,
          "total": False,
          "values": False
        },
        "lines": True,
        "linewidth": 1,
        "links": [],
        "nullPointMode": "null",
        "percentage": False,
        "pointradius": 5,
        "points": False,
        "renderer": "flot",
        "seriesOverrides": [],
        "spaceLength": 10,
        "stack": False,
        "steppedLine": False,
        "targets": [
          {
            "refId": "A",
            "target": "aliasByNode(media.strm.$environment.channel.$channel_b.source.$publisher_b.stream_quality.common.status, -1)",
            "textEditor": False
          }
        ],
        "thresholds": [],
        "timeFrom": None,
        "timeRegions": [],
        "timeShift": None,
        "title": "Stream status -- $channel_b -- $publisher_b",
        "tooltip": {
          "shared": True,
          "sort": 0,
          "value_type": "individual"
        },
        "type": "graph",
        "xaxis": {
          "buckets": None,
          "mode": "time",
          "name": None,
          "show": True,
          "values": []
        },
        "yaxes": [
          {
            "decimals": 0,
            "format": "short",
            "label": None,
            "logBase": 1,
            "max": "2",
            "min": "0",
            "show": True
          },
          {
            "decimals": 0,
            "format": "short",
            "label": None,
            "logBase": 1,
            "max": "2",
            "min": "0",
            "show": True
          }
        ],
        "yaxis": {
          "align": False,
          "alignLevel": None
        }
      }
    ],
    "refresh": "1m",
    "schemaVersion": 18,
    "style": "dark",
    "tags": [],
    "templating": {
      "list": [
        {
          "allValue": None,
          "current": {
            "text": "stable",
            "value": "stable"
          },
          "datasource": "gr-mg",
          "definition": "",
          "hide": 0,
          "includeAll": False,
          "label": "Environment",
          "multi": False,
          "name": "environment",
          "options": [],
          "query": "media.strm.*",
          "refresh": 2,
          "regex": "(stable.*|testing.*|development.*|debug.*)",
          "skipUrlSync": False,
          "sort": 1,
          "tagValuesQuery": "",
          "tags": [],
          "tagsQuery": "",
          "type": "query",
          "useTags": False
        },
        {
          "allValue": None,
          "current": {
            "selected": True,
            "text": "raztv_supres_source",
            "value": "raztv_supres_source"
          },
          "datasource": "gr-mg",
          "definition": "",
          "hide": 0,
          "includeAll": False,
          "label": "Channel A",
          "multi": False,
          "name": "channel_a",
          "options": [],
          "query": "media.strm.$environment.channel.*",
          "refresh": 2,
          "regex": "(?!.*-src|.*strm_yandex_net).*$",
          "skipUrlSync": False,
          "sort": 1,
          "tagValuesQuery": "",
          "tags": [],
          "tagsQuery": "",
          "type": "query",
          "useTags": False
        },
        {
          "allValue": None,
          "current": {
            "selected": True,
            "text": "src-rtmp-mskm903_strm_yandex_net",
            "value": "src-rtmp-mskm903_strm_yandex_net"
          },
          "datasource": "gr-mg",
          "definition": "",
          "hide": 0,
          "includeAll": False,
          "label": "Publisher A",
          "multi": False,
          "name": "publisher_a",
          "options": [],
          "query": "media.strm.$environment.channel.$channel_a.source.*_yandex_net",
          "refresh": 2,
          "regex": "",
          "skipUrlSync": False,
          "sort": 1,
          "tagValuesQuery": "",
          "tags": [],
          "tagsQuery": "",
          "type": "query",
          "useTags": False
        },
        {
          "allValue": None,
          "current": {
            "selected": True,
            "text": "raztv_supres",
            "value": "raztv_supres"
          },
          "datasource": "gr-mg",
          "definition": "",
          "hide": 0,
          "includeAll": False,
          "label": "Channel B",
          "multi": False,
          "name": "channel_b",
          "options": [],
          "query": "media.strm.$environment.channel.*",
          "refresh": 2,
          "regex": "(?!.*-src|.*strm_yandex_net).*$",
          "skipUrlSync": False,
          "sort": 1,
          "tagValuesQuery": "",
          "tags": [],
          "tagsQuery": "",
          "type": "query",
          "useTags": False
        },
        {
          "allValue": None,
          "current": {
            "selected": True,
            "text": "nnmetr-src-rtmp-mskm903_strm_yandex_net",
            "value": "nnmetr-src-rtmp-mskm903_strm_yandex_net"
          },
          "datasource": "gr-mg",
          "definition": "",
          "hide": 0,
          "includeAll": False,
          "label": "Publisher B",
          "multi": False,
          "name": "publisher_b",
          "options": [],
          "query": "media.strm.$environment.channel.$channel_b.source.*_yandex_net",
          "refresh": 2,
          "regex": "",
          "skipUrlSync": False,
          "sort": 1,
          "tagValuesQuery": "",
          "tags": [],
          "tagsQuery": "",
          "type": "query",
          "useTags": False
        }
      ]
    },
    "time": {
      "from": "now-24h",
      "to": "now"
    },
    "timepicker": {
      "refresh_intervals": [
        "1m",
        "5m",
        "15m",
        "30m",
        "1h",
        "2h",
        "1d"
      ],
      "time_options": [
        "5m",
        "15m",
        "1h",
        "6h",
        "12h",
        "24h",
        "2d",
        "7d",
        "30d"
      ]
    },
    "timezone": "",
    "title": "STRM: RTMP Stream Quality",
    "uid": "000016640",
    "version": 18
  }
}

