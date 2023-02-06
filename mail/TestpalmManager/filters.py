projects = {
    'mail-touch': {
        'automated_filter': {
            'type': 'AND',
            'left': {'type': 'EQ', 'key': 'attributes.58eba70b88955049314bf7d4', 'value': 'Кандидат в автоматизацию'},
            'right': {'type': 'EQ', 'key': 'isAutotest', 'value': 'true'}
        },
        'automation_empty_filter': {
            'type': 'AND',
            'left': {
                'type': 'AND',
                'left': {
                    'type': 'AND',
                    'left': {'type': 'EQ', 'key': 'attributes.58eba70b88955049314bf7d4', 'value': 'null'},
                    'right': {
                        'type': 'AND',
                        'left': {'type': 'NEQ', 'key': 'status', 'value': 'duplicate'},
                        'right': {'type': 'NEQ', 'key': 'status', 'value': 'archived'}
                    },
                },
                'right': {'type': 'EQ', 'key': 'isAutotest', 'value': 'false'}
            },
            'right': {
                'type': 'OR',
                'left': {
                    'type': 'AND',
                    'left': {'type': 'EQ', 'key': 'attributes.5d66cac646af84e7088d5781', 'value': 'High'},
                    'right': {
                        'type': 'OR',
                        'left': {'type': 'EQ', 'key': 'attributes.5d691c9df97f241126194041', 'value': 'Medium'},
                        'right': {'type': 'EQ', 'key': 'attributes.5d691c9df97f241126194041', 'value': 'High'}
                    }
                },
                'right': {
                    'type': 'AND',
                    'left': {'type': 'EQ', 'key': 'attributes.5d66cac646af84e7088d5781', 'value': 'Medium'},
                    'right': {'type': 'EQ', 'key': 'attributes.5d691c9df97f241126194041', 'value': 'High'}
                }
            }
        },
        'missed_obligatory_keys_filter': {
            'type': 'AND',
            'left': {
                'type': 'AND',
                'left': {
                    'type': 'AND',
                    'left': {
                        'type': 'AND',
                        'left': {
                            'type': 'AND',
                            'left': {
                                'type': 'OR',
                                'left': {'type': 'EQ', 'key': "attributes.582c40f3c6123313214de3d6", 'value': 'null'},
                                'right': {'type': 'EQ', 'key': 'attributes.5d691c9df97f241126194041', 'value': 'null'}
                            },
                            'right': {'type': 'NEQ', 'key': 'attributes.582c40f3c6123313214de3d6',
                                      'value': '[Административное]'}
                        },
                        'right': {'type': 'NEQ', 'key': 'status', 'value': 'duplicate'}
                    },
                    'right': {'type': 'NEQ', 'key': 'status', 'value': 'archived'}
                },
                'right': {'type': 'NEQ', 'key': 'status', 'value': 'draft'}
            },
            "right": {"type": "EQ", "key": "isAutotest", "value": "false"}
            # 'right': {'type': 'GT', 'key': 'createdTime', 'value': 1577887531000}
        },
        'cases_with_feature_priority': {
            "type": "AND",
            "left":
                {"type": "AND",
                 "left":
                     {"type": "AND",
                      "left":
                          {"type": "NEQ", "key": "status", "value": "archived"},
                      "right":
                          {"type": "NEQ", "key": "status", "value": "duplicate"}
                      },
                 "right":
                     {"type": "NEQ", "key": "attributes.582c40f3c6123313214de3d6", "value": "[Административное]"}
                 },
            "right":
                {"type": "NEQ", "key": "attributes.582c40f3c6123313214de3d6", "value": 'null'}
        },
        'automation_problem_filter': {
            "type": "OR", "left":
                {"type": "EQ", "key": "attributes.58eba70b88955049314bf7d4",
                 "value": "Возможно нет вилки"},
            "right": {"type": "AND",
                      "left": {
                          "type": "AND",
                          "left": {
                              "type": "AND",
                              "left": {
                                  "type": "AND",
                                  "left": {
                                      "type": "EQ",
                                      "key": "attributes.58eba70b88955049314bf7d4",
                                      "value": "Не запускался долгое время"},
                                  "right": {
                                      "type": "NEQ",
                                      "key": "attributes.58eba70b88955049314bf7d4",
                                      "value": "Проверяется в другом кейсе"}
                              },
                              "right": {
                                  "type": "NEQ",
                                  "key": "status",
                                  "value": "duplicate"}
                          },
                          "right": {
                              "type": "NEQ",
                              "key": "status",
                              "value": "archived"}
                      },
                      "right": {
                          "type": "NEQ",
                          "key": "attributes.58eba70b88955049314bf7d4",
                          "value": "Заигнорен"}
                      }
        },
        'wrong_status_filter': {
            "left": {"type": "AND",
                     "left": {"type": "NEQ", "key": "status", "value": "draft"},
                     "right": {"type": "NEQ", "key": "status", "value": "actual"}
                     },
            "type": "AND",
            "right": {"type": "NEQ", "key": "status", "value": "archived"}
        },
        'AC':
            {"type": "AND", "left": {"type": "EQ", "key": "attributes.5d66cac646af84e7088d5781", "value": "High"},
             "right": {"type": "EQ", "key": "attributes.5d691c9df97f241126194041", "value": "High"}
             },
        'BL':
            {"type": "OR",
             "left":
                 {"type": "AND",
                  "left": {"type": "EQ", "key": "attributes.5d66cac646af84e7088d5781",
                           "value": "High"},
                  "right": {"type": "EQ", "key": "attributes.5d691c9df97f241126194041",
                            "value": "Medium"}
                  },
             "right": {"type": "AND",
                       "left": {"type": "EQ",
                                "key": "attributes.5d66cac646af84e7088d5781",
                                "value": "Medium"},
                       "right": {"type": "EQ",
                                 "key": "attributes.5d691c9df97f241126194041",
                                 "value": "High"}
                       }
             },
        'Regress':
            {"type": "OR",
             "left": {"type": "OR", "left":
                 {"type": "AND", "left": {"type": "EQ",
                                          "key": "attributes.5d66cac646af84e7088d5781",
                                          "value": "High"},
                  "right": {"type": "EQ",
                            "key": "attributes.5d691c9df97f241126194041",
                            "value": "Low"}
                  }, "right": {"type": "AND",
                               "left": {"type": "EQ",
                                        "key": "attributes.5d66cac646af84e7088d5781",
                                        "value": "Medium"},
                               "right": {"type": "EQ",
                                         "key": "attributes.5d691c9df97f241126194041",
                                         "value": "Medium"}
                               }
                      },
             "right": {"type": "AND",
                       "left": {"type": "EQ", "key": "attributes.5d66cac646af84e7088d5781", "value": "Low"},
                       "right": {"type": "EQ", "key": "attributes.5d691c9df97f241126194041", "value": "High"}
                       }
             },
        'Full regress':
            {"type": "OR", "left":
                {"type": "AND", "left":
                    {"type": "EQ", "key": "attributes.5d66cac646af84e7088d5781", "value": "Medium"},
                 "right": {"type": "EQ", "key": "attributes.5d691c9df97f241126194041", "value": "Low"}
                 },
             "right": {"type": "AND", "left": {"type": "EQ",
                                               "key": "attributes.5d66cac646af84e7088d5781",
                                               "value": "Low"},
                       "right": {"type": "EQ",
                                 "key": "attributes.5d691c9df97f241126194041",
                                 "value": "Medium"}
                       }
             },
        'Once':
            {"type": "OR", "left":
                {"type": "OR", "left":
                    {"type": "AND", "left":
                        {"type": "EQ",
                         "key": "attributes.5d66cac646af84e7088d5781",
                         "value": "High"},
                     "right": {"type": "EQ",
                               "key": "attributes.5d691c9df97f241126194041",
                               "value": "Zero"}
                     }, "right": {"type": "AND",
                                  "left": {"type": "EQ",
                                           "key": "attributes.5d66cac646af84e7088d5781",
                                           "value": "Medium"},
                                  "right": {
                                      "type": "EQ",
                                      "key": "attributes.5d691c9df97f241126194041",
                                      "value": "Zero"}
                                  }
                 },
             "right": {"type": "AND",
                       "left": {"type": "EQ", "key": "attributes.5d66cac646af84e7088d5781", "value": "Low"},
                       "right": {"type": "EQ", "key": "attributes.5d691c9df97f241126194041", "value": "Low"}
                       }
             },
        'asessors_need_actions':
            {"type": "AND",
             "left": {"type": "AND",
                      "left": {"type": "AND",
                               "left": {"type": "OR",
                                        "left": {"type": "EQ",
                                                 "key": "attributes.60e70a5b22220a0022296bd5",
                                                 "value": "AC"},
                                        "right": {"type": "EQ",
                                                  "key": "attributes.60e70a5b22220a0022296bd5",
                                                  "value": "BL"}
                                        },
                               "right": {"type": "EQ", "key": "isAutotest",
                                         "value": "false"}
                               },
                      "right": {"type": "NEQ", "key": "attributes.59bd6554833454a818904054",
                                "value": "[Инструкции для асессоров]"}
                      },
             "right": {"type": "EQ", "key": "status", "value": "actual"}
             }
    },
    'sender_main': {
        'wrong_status_filter': {
            "type": "AND",
            "left": {"type": "AND",
                     "left": {"type": "NEQ", "key": "status", "value": "draft"},
                     "right": {"type": "NEQ", "key": "status", "value": "actual"}
                     },
            "right": {"type": "NEQ", "key": "status", "value": "archived"}
        },
        'missed_obligatory_keys_filter':
            {"type": "AND",
             "left": {"type": "OR",
                      "left": {"type": "OR",
                               "left": {"type": "EQ",
                                        "key": "attributes.620bbc01e57fde0022b5fed4",
                                        "value": 'null'},
                               "right": {"type": "EQ",
                                         "key": "attributes.61c4983089b1250022d59879",
                                         "value": 'null'}},
                      "right": {"type": "EQ", "key": "attributes.61c49f0dc4653000222f54e5",
                                "value": 'null'}},
             "right": {"type": "NEQ", "key": "attributes.61c4983089b1250022d59879", "value": "Административное"}
             },
        'cases_with_feature_priority':
            {"type": "AND", "left": {"type": "AND",
                                     "left": {"type": "NEQ", "key": "attributes.61c4983089b1250022d59879",
                                              "value": "Административное"},
                                     "right": {"type": "NEQ", "key": "attributes.61c49f0dc4653000222f54e5",
                                               "value": 'null'}},
             "right": {"type": "EQ", "key": "status", "value": "actual"}
             },
        'AC':
            {"type": "AND", "left": {"type": "EQ", "key": "attributes.61c499af0ee78400220cf451", "value": "High"},
             "right": {"type": "EQ", "key": "attributes.61c499ca0ee78400220cf452", "value": "High"}
             },
        'BL':
            {"type": "OR",
             "left":
                 {"type": "AND",
                  "left": {"type": "EQ", "key": "attributes.61c499af0ee78400220cf451",
                           "value": "High"},
                  "right": {"type": "EQ", "key": "attributes.61c499ca0ee78400220cf452",
                            "value": "Medium"}
                  },
             "right": {"type": "AND",
                       "left": {"type": "EQ",
                                "key": "attributes.61c499af0ee78400220cf451",
                                "value": "Medium"},
                       "right": {"type": "EQ",
                                 "key": "attributes.61c499ca0ee78400220cf452",
                                 "value": "High"}
                       }
             },
        'Regress':
            {"type": "OR",
             "left": {"type": "OR", "left":
                 {"type": "AND", "left": {"type": "EQ",
                                          "key": "attributes.61c499ca0ee78400220cf452",
                                          "value": "High"},
                  "right": {"type": "EQ",
                            "key": "attributes.61c499af0ee78400220cf451",
                            "value": "Low"}
                  }, "right": {"type": "AND",
                               "left": {"type": "EQ",
                                        "key": "attributes.61c499ca0ee78400220cf452",
                                        "value": "Medium"},
                               "right": {"type": "EQ",
                                         "key": "attributes.61c499af0ee78400220cf451",
                                         "value": "Medium"}
                               }
                      },
             "right": {"type": "AND",
                       "left": {"type": "EQ", "key": "attributes.61c499ca0ee78400220cf452", "value": "Low"},
                       "right": {"type": "EQ", "key": "attributes.61c499af0ee78400220cf451", "value": "High"}
                       }
             },
        'Full regress':
            {"type": "OR", "left":
                {"type": "AND", "left":
                    {"type": "EQ", "key": "attributes.61c499ca0ee78400220cf452", "value": "Medium"},
                 "right": {"type": "EQ", "key": "attributes.61c499af0ee78400220cf451", "value": "Low"}
                 },
             "right": {"type": "AND", "left": {"type": "EQ",
                                               "key": "attributes.61c499ca0ee78400220cf452",
                                               "value": "Low"},
                       "right": {"type": "EQ",
                                 "key": "attributes.61c499af0ee78400220cf451",
                                 "value": "Medium"}
                       }
             },
        'Once':
            {"type": "OR", "left":
                {"type": "OR", "left":
                    {"type": "AND", "left":
                        {"type": "EQ",
                         "key": "attributes.61c499ca0ee78400220cf452",
                         "value": "High"},
                     "right": {"type": "EQ",
                               "key": "attributes.61c499af0ee78400220cf451",
                               "value": "Zero"}
                     }, "right": {"type": "AND",
                                  "left": {"type": "EQ",
                                           "key": "attributes.61c499ca0ee78400220cf452",
                                           "value": "Medium"},
                                  "right": {
                                      "type": "EQ",
                                      "key": "attributes.61c499af0ee78400220cf451",
                                      "value": "Zero"}
                                  }
                 },
             "right": {"type": "AND",
                       "left": {"type": "EQ", "key": "attributes.61c499ca0ee78400220cf452", "value": "Low"},
                       "right": {"type": "EQ", "key": "attributes.61c499af0ee78400220cf451", "value": "Low"}
                       }
             }
    },
    'mail-liza': {
        'automated_filter': {
            'type': 'AND',
            'left': {'type': 'EQ', 'key': 'attributes.5c5453a66acd3d903a10fba9',
                     'value': 'Кандидат на автоматизацию'},
            'right': {'type': 'EQ', 'key': 'isAutotest', 'value': 'true'}
        },
        'automation_empty_filter': {
            'type': 'AND',
            'left': {
                'type': 'AND',
                'left': {
                    'type': 'AND',
                    'left': {'type': 'EQ', 'key': 'attributes.5c5453a66acd3d903a10fba9', 'value': 'null'},
                    'right': {
                        'type': 'AND',
                        'left': {'type': 'NEQ', 'key': 'status', 'value': 'duplicate'},
                        'right': {'type': 'NEQ', 'key': 'status', 'value': 'archived'}
                    },
                },
                'right': {'type': 'EQ', 'key': 'isAutotest', 'value': 'false'}
            },
            'right': {
                'type': 'OR',
                'left': {
                    'type': 'AND',
                    'left': {'type': 'EQ', 'key': 'attributes.5d5b2320f97f2449b408c4bf', 'value': 'High'},
                    'right': {
                        'type': 'OR',
                        'left': {'type': 'EQ', 'key': 'attributes.5d5b233ca6a6dd7b6bcb3a46', 'value': 'Medium'},
                        'right': {'type': 'EQ', 'key': 'attributes.5d5b233ca6a6dd7b6bcb3a46', 'value': 'High'}
                    }
                },
                'right': {
                    'type': 'AND',
                    'left': {'type': 'EQ', 'key': 'attributes.5d5b2320f97f2449b408c4bf', 'value': 'Medium'},
                    'right': {'type': 'EQ', 'key': 'attributes.5d5b233ca6a6dd7b6bcb3a46', 'value': 'High'}
                }
            }
        },
        'automation_problem_filter': {
            "type": "AND", "left":
                {"type": "AND", "left":
                    {"type": "AND",
                     "left": {"type": "AND",
                              "left": {"type": "OR",
                                       "left": {
                                           "type": "EQ",
                                           "key": "attributes.5c5453a66acd3d903a10fba9",
                                           "value": "Возможно нет вилки"},
                                       "right": {
                                           "type": "EQ",
                                           "key": "attributes.5c5453a66acd3d903a10fba9",
                                           "value": "Не запускался долгое время"
                                       }
                                       },
                              "right": {"type": "NEQ",
                                        "key": "attributes.5c5453a66acd3d903a10fba9",
                                        "value": "Автоматизирован в другом кейсе"}
                              },
                     "right": {"type": "NEQ",
                               "key": "status",
                               "value": "duplicate"}
                     },
                 "right": {"type": "NEQ", "key": "status",
                           "value": "archived"}
                 },
            "right": {"type": "NEQ", "key": "attributes.5c5453a66acd3d903a10fba9",
                      "value": "Заигнорен"}
        },
        'wrong_status_filter': {
            "type": "AND",
            "left": {"type": "AND",
                     "left": {"type": "NEQ", "key": "status", "value": "draft"},
                     "right": {"type": "NEQ", "key": "status", "value": "actual"}
                     },
            "right": {"type": "NEQ", "key": "status", "value": "archived"}
        },
        'missed_obligatory_keys_filter': {
            'type': 'AND',
            'left': {
                'type': 'AND',
                'left': {
                    'type': 'AND',
                    'left': {
                        'type': 'AND',
                        'left': {
                            'type': 'AND',
                            'left': {
                                'type': 'AND',
                                'left': {
                                    'type': 'OR',
                                    'left': {
                                        'type': 'OR',
                                        'left': {'type': 'EQ', 'key': 'attributes.55a7b654e4b0de1599b0c517',
                                                 'value': 'null'},
                                        'right': {'type': 'EQ', 'key': 'attributes.5d5b233ca6a6dd7b6bcb3a46',
                                                  'value': 'null'}
                                    },
                                    'right': {'type': 'EQ', 'key': 'attributes.57066efa8895507aa3071f8e',
                                              'value': 'null'}
                                },
                                'right': {'type': 'NEQ', 'key': 'attributes.57066efa8895507aa3071f8e',
                                          'value': '[Административное]'}
                            },
                            'right': {'type': 'NEQ', 'key': 'attributes.55a7b654e4b0de1599b0c517',
                                      'value': 'Административное'}
                        },
                        'right': {'type': 'NEQ', 'key': 'status', 'value': 'duplicate'}
                    },
                    'right': {'type': 'NEQ', 'key': 'status', 'value': 'archived'}
                },
                'right': {'type': 'NEQ', 'key': 'status', 'value': 'draft'}
            },
            "right": {"type": "EQ", "key": "isAutotest", "value": "false"}
        },
        'cases_with_feature_priority': {
            "type": "AND",
            "left": {
                "type": "AND",
                "left": {
                    "type": "AND",
                    "left": {
                        "type": "NEQ",
                        "key": "status",
                        "value": "archived"},
                    "right": {
                        "type": "NEQ",
                        "key": "status",
                        "value": "duplicate"}
                },
                "right": {
                    "type": "NEQ",
                    "key": "attributes.55a7b654e4b0de1599b0c517",
                    "value": 'null'}
            },
            "right": {
                "type": "NEQ",
                "key": "attributes.55a7b654e4b0de1599b0c517",
                "value": "Административное"
            }
        },
        'AC':
            {"type": "AND", "left": {"type": "EQ", "key": "attributes.5d5b2320f97f2449b408c4bf", "value": "High"},
             "right": {"type": "EQ", "key": "attributes.5d5b233ca6a6dd7b6bcb3a46", "value": "High"}
             },
        'BL':
            {"type": "OR",
             "left":
                 {"type": "AND",
                  "left": {"type": "EQ", "key": "attributes.5d5b2320f97f2449b408c4bf",
                           "value": "High"},
                  "right": {"type": "EQ", "key": "attributes.5d5b233ca6a6dd7b6bcb3a46",
                            "value": "Medium"}
                  },
             "right": {"type": "AND",
                       "left": {"type": "EQ",
                                "key": "attributes.5d5b2320f97f2449b408c4bf",
                                "value": "Medium"},
                       "right": {"type": "EQ",
                                 "key": "attributes.5d5b233ca6a6dd7b6bcb3a46",
                                 "value": "High"}
                       }
             },
        'Regress':
            {"type": "OR",
             "left": {"type": "OR", "left":
                 {"type": "AND", "left": {"type": "EQ",
                                          "key": "attributes.5d5b2320f97f2449b408c4bf",
                                          "value": "High"},
                  "right": {"type": "EQ",
                            "key": "attributes.5d5b233ca6a6dd7b6bcb3a46",
                            "value": "Low"}
                  }, "right": {"type": "AND",
                               "left": {"type": "EQ",
                                        "key": "attributes.5d5b2320f97f2449b408c4bf",
                                        "value": "Medium"},
                               "right": {"type": "EQ",
                                         "key": "attributes.5d5b233ca6a6dd7b6bcb3a46",
                                         "value": "Medium"}
                               }
                      },
             "right": {"type": "AND",
                       "left": {"type": "EQ", "key": "attributes.5d5b2320f97f2449b408c4bf", "value": "Low"},
                       "right": {"type": "EQ", "key": "attributes.5d5b233ca6a6dd7b6bcb3a46", "value": "High"}
                       }
             },
        'Full regress':
            {"type": "OR", "left":
                {"type": "AND", "left":
                    {"type": "EQ", "key": "attributes.5d5b2320f97f2449b408c4bf", "value": "Medium"},
                 "right": {"type": "EQ", "key": "attributes.5d5b233ca6a6dd7b6bcb3a46", "value": "Low"}
                 },
             "right": {"type": "AND", "left": {"type": "EQ",
                                               "key": "attributes.5d5b2320f97f2449b408c4bf",
                                               "value": "Low"},
                       "right": {"type": "EQ",
                                 "key": "attributes.5d5b233ca6a6dd7b6bcb3a46",
                                 "value": "Medium"}
                       }
             },
        'Once':
            {"type": "OR", "left":
                {"type": "OR", "left":
                    {"type": "AND", "left":
                        {"type": "EQ",
                         "key": "attributes.5d5b2320f97f2449b408c4bf",
                         "value": "High"},
                     "right": {"type": "EQ",
                               "key": "attributes.5d5b233ca6a6dd7b6bcb3a46",
                               "value": "Zero"}
                     }, "right": {"type": "AND",
                                  "left": {"type": "EQ",
                                           "key": "attributes.5d5b2320f97f2449b408c4bf",
                                           "value": "Medium"},
                                  "right": {
                                      "type": "EQ",
                                      "key": "attributes.5d5b233ca6a6dd7b6bcb3a46",
                                      "value": "Zero"}
                                  }
                 },
             "right": {"type": "AND",
                       "left": {"type": "EQ", "key": "attributes.5d5b2320f97f2449b408c4bf", "value": "Low"},
                       "right": {"type": "EQ", "key": "attributes.5d5b233ca6a6dd7b6bcb3a46", "value": "Low"}
                       }
             },
        'asessors_need_actions':
            {"type": "AND", "left":
                {"type": "AND", "left":
                    {"type": "AND", "left":
                        {"type": "AND",
                         "left": {"type": "EQ",
                                  "key": "attributes.54002c9de4b0fc51d80071f9",
                                  "value": "Тестплан"},
                         "right": {"type": "EQ",
                                   "key": "status",
                                   "value": "actual"}
                         },
                     "right": {"type": "EQ", "key": "isAutotest",
                               "value": "false"}
                     },
                 "right": {"type": "NEQ", "key": "attributes.55a7b654e4b0de1599b0c517",
                           "value": "Административное"}
                 },
             "right": {"type": "EQ", "key": "attributes.5e00815027a990afebeed31c", "value": 'null'}
             }
    },
    'cal': {
        'automated_filter': {
            'type': 'AND',
            'left': {'type': 'EQ', 'key': 'attributes.5c9cc8d07c48e33b4a624932',
                     'value': 'Кандидат в автоматизацию'},
            'right': {'type': 'EQ', 'key': 'isAutotest', 'value': 'true'}
        },
        'automation_empty_filter': {
            'type': 'AND',
            'left': {
                'type': 'AND',
                'left': {
                    'type': 'AND',
                    'left': {'type': 'EQ', 'key': 'attributes.5c9cc8d07c48e33b4a624932', 'value': 'null'},
                    'right': {
                        'type': 'AND',
                        'left': {'type': 'NEQ', 'key': 'status', 'value': 'duplicate'},
                        'right': {'type': 'NEQ', 'key': 'status', 'value': 'archived'}
                    },
                },
                'right': {'type': 'EQ', 'key': 'isAutotest', 'value': 'false'}
            },
            'right': {
                'type': 'OR',
                'left': {
                    'type': 'AND',
                    'left': {'type': 'EQ', 'key': 'attributes.5d6e2d543d42cbf92a6bef45', 'value': 'High'},
                    'right': {
                        'type': 'OR',
                        'left': {'type': 'EQ', 'key': 'attributes.5d6e2d6ff97f24466b0678e6', 'value': 'Medium'},
                        'right': {'type': 'EQ', 'key': 'attributes.5d6e2d6ff97f24466b0678e6', 'value': 'High'}}},
                'right': {
                    'type': 'AND',
                    'left': {'type': 'EQ', 'key': 'attributes.5d6e2d543d42cbf92a6bef45', 'value': 'Medium'},
                    'right': {'type': 'EQ', 'key': 'attributes.5d6e2d6ff97f24466b0678e6', 'value': 'High'}
                }
            }
        },
        'missed_obligatory_keys_filter': {
            'type': 'AND',
            'left': {
                "type": "AND",
                "left": {
                    "type": "AND",
                    "left": {
                        "type": "AND",
                        "left": {
                            "type": "AND",
                            "left": {
                                "type": "AND",
                                "left": {
                                    "type": "OR",
                                    "left": {
                                        "type": "OR",
                                        "left": {'type': 'EQ', 'key': 'attributes.5822dfb1889550027e060679',
                                                 'value': 'null'},
                                        'right': {'type': 'EQ', 'key': 'attributes.57cfc17ac61233278bb45ef2',
                                                  'value': 'null'}
                                    },
                                    "right": {'type': 'EQ', 'key': 'attributes.5d6e2d6ff97f24466b0678e6',
                                              'value': 'null'}
                                },
                                "right": {"type": "NEQ", "key": "attributes.5822dfb1889550027e060679",
                                          "value": "Административное"}},
                            "right": {"type": "NEQ", "key": "attributes.57cfc17ac61233278bb45ef2",
                                      "value": "[Административное]"}
                        },
                        "right": {"type": "NEQ", "key": "status", "value": "duplicate"}
                    },
                    "right": {"type": "NEQ", "key": "status", "value": "archived"}
                },
                "right": {"type": "NEQ", "key": "status", "value": "draft"}
            },
            "right": {"type": "EQ", "key": "isAutotest", "value": "false"}},
        'cases_with_feature_priority': {
            "type": "AND",
            "left":
                {"type": "AND",
                 "left":
                     {"type": "AND",
                      "left":
                          {"type": "NEQ", "key": "status", "value": "archived"},
                      "right":
                          {"type": "NEQ", "key": "status", "value": "duplicate"}},
                 "right":
                     {"type": "NEQ", "key": "attributes.5822dfb1889550027e060679", "value": "Административное"}},
            "right":
                {"type": "NEQ", "key": "attributes.5822dfb1889550027e060679", "value": 'null'}
        },
        'wrong_status_filter': {
            "left": {"type": "AND",
                     "left": {"type": "NEQ", "key": "status", "value": "draft"},
                     "right": {"type": "NEQ", "key": "status", "value": "actual"}
                     },
            "type": "AND",
            "right": {"type": "NEQ", "key": "status", "value": "archived"}
        },
        'automation_problem_filter': {
            "type": "OR", "left":
                {"type": "EQ", "key": "attributes.5c9cc8d07c48e33b4a624932",
                 "value": "Возможно нет вилки"},
            "right": {"type": "AND",
                      "left": {
                          "type": "AND",
                          "left": {
                              "type": "AND",
                              "left": {
                                  "type": "EQ",
                                  "key": "attributes.5c9cc8d07c48e33b4a624932",
                                  "value": "Не запускался долгое время"},
                              "right": {
                                  "type": "NEQ",
                                  "key": "status",
                                  "value": "duplicate"}
                          },
                          "right": {
                              "type": "NEQ",
                              "key": "status",
                              "value": "archived"}
                      },
                      "right": {
                          "type": "NEQ",
                          "key": "attributes.5c9cc8d07c48e33b4a624932",
                          "value": "Заигнорен"}
                      }
        },
        'AC':
            {"type": "AND", "left": {"type": "EQ", "key": "attributes.5d6e2d543d42cbf92a6bef45", "value": "High"},
             "right": {"type": "EQ", "key": "attributes.5d6e2d6ff97f24466b0678e6", "value": "High"}
             },
        'BL':
            {"type": "OR",
             "left":
                 {"type": "AND",
                  "left": {"type": "EQ", "key": "attributes.5d6e2d543d42cbf92a6bef45",
                           "value": "High"},
                  "right": {"type": "EQ", "key": "attributes.5d6e2d6ff97f24466b0678e6",
                            "value": "Medium"}
                  },
             "right": {"type": "AND",
                       "left": {"type": "EQ",
                                "key": "attributes.5d6e2d543d42cbf92a6bef45",
                                "value": "Medium"},
                       "right": {"type": "EQ",
                                 "key": "attributes.5d6e2d6ff97f24466b0678e6",
                                 "value": "High"}
                       }
             },
        'Regress':
            {"type": "OR",
             "left": {"type": "OR", "left":
                 {"type": "AND", "left": {"type": "EQ",
                                          "key": "attributes.5d6e2d543d42cbf92a6bef45",
                                          "value": "High"},
                  "right": {"type": "EQ",
                            "key": "attributes.5d6e2d6ff97f24466b0678e6",
                            "value": "Low"}
                  }, "right": {"type": "AND",
                               "left": {"type": "EQ",
                                        "key": "attributes.5d6e2d543d42cbf92a6bef45",
                                        "value": "Medium"},
                               "right": {"type": "EQ",
                                         "key": "attributes.5d6e2d6ff97f24466b0678e6",
                                         "value": "Medium"}
                               }
                      },
             "right": {"type": "AND",
                       "left": {"type": "EQ", "key": "attributes.5d6e2d543d42cbf92a6bef45", "value": "Low"},
                       "right": {"type": "EQ", "key": "attributes.5d6e2d6ff97f24466b0678e6", "value": "High"}
                       }
             },
        'Full regress':
            {"type": "OR", "left":
                {"type": "AND", "left":
                    {"type": "EQ", "key": "attributes.5d6e2d543d42cbf92a6bef45", "value": "Medium"},
                 "right": {"type": "EQ", "key": "attributes.5d6e2d6ff97f24466b0678e6", "value": "Low"}
                 },
             "right": {"type": "AND", "left": {"type": "EQ",
                                               "key": "attributes.5d6e2d543d42cbf92a6bef45",
                                               "value": "Low"},
                       "right": {"type": "EQ",
                                 "key": "attributes.5d6e2d6ff97f24466b0678e6",
                                 "value": "Medium"}
                       }
             },
        'Once':
            {"type": "OR", "left":
                {"type": "OR", "left":
                    {"type": "AND", "left":
                        {"type": "EQ",
                         "key": "attributes.5d6e2d543d42cbf92a6bef45",
                         "value": "High"},
                     "right": {"type": "EQ",
                               "key": "attributes.5d6e2d6ff97f24466b0678e6",
                               "value": "Zero"}
                     }, "right": {"type": "AND",
                                  "left": {"type": "EQ",
                                           "key": "attributes.5d6e2d543d42cbf92a6bef45",
                                           "value": "Medium"},
                                  "right": {
                                      "type": "EQ",
                                      "key": "attributes.5d6e2d6ff97f24466b0678e6",
                                      "value": "Zero"}
                                  }
                 },
             "right": {"type": "AND",
                       "left": {"type": "EQ", "key": "attributes.5d6e2d543d42cbf92a6bef45", "value": "Low"},
                       "right": {"type": "EQ", "key": "attributes.5d6e2d6ff97f24466b0678e6", "value": "Low"}
                       }
             },
        'asessors_need_actions':
            {"type": "AND",
             "left": {"type": "AND",
                      "left": {"type": "AND",
                               "left": {"type": "AND",
                                        "left": {"type": "AND",
                                                 "left": {"type": "OR",
                                                          "left": {
                                                              "type": "OR",
                                                              "left": {
                                                                  "type": "EQ",
                                                                  "key": "attributes.57f379898895502c9b12bd4b",
                                                                  "value": "[touch] Тестплан - паблик"},
                                                              "right": {
                                                                  "type": "EQ",
                                                                  "key": "attributes.57f379898895502c9b12bd4b",
                                                                  "value": "[ВЕБ] Тестплан - паблик"}
                                                          },
                                                          "right": {
                                                              "type": "EQ",
                                                              "key": "attributes.57f379898895502c9b12bd4b",
                                                              "value": "[ВЕБ] Тестплан - корп"}
                                                          },
                                                 "right": {"type": "OR",
                                                           "left": {
                                                               "type": "OR",
                                                               "left": {
                                                                   "type": "OR",
                                                                   "left": {
                                                                       "type": "EQ",
                                                                       "key": "attributes.60e70a2169d473002270ecd6",
                                                                       "value": "AC"},
                                                                   "right": {
                                                                       "type": "EQ",
                                                                       "key": "attributes.60e70a2169d473002270ecd6",
                                                                       "value": "BL"}
                                                               },
                                                               "right": {
                                                                   "type": "EQ",
                                                                   "key": "attributes.60e70a2169d473002270ecd6",
                                                                   "value": "Regress"}
                                                           },
                                                           "right": {
                                                               "type": "EQ",
                                                               "key": "attributes.60e70a2169d473002270ecd6",
                                                               "value": "Full Regress"}
                                                           }
                                                 },
                                        "right": {"type": "NEQ",
                                                  "key": "attributes.57cfc17ac61233278bb45ef2",
                                                  "value": 'null'}
                                        },
                               "right": {"type": "EQ",
                                         "key": "attributes.5dfa3ea18d2030e9c880d4df",
                                         "value": 'null'}
                               },
                      "right": {"type": "EQ", "key": "status", "value": "actual"}
                      },
             "right": {"type": "EQ", "key": "isAutotest", "value": "false"}
             }

    }
}
