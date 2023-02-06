# -*- coding: utf-8 -*-

import pytest
import zlib
import json

from report.const import *
from report.proto import meta_pb2
from report.functional.web.base import BaseFuncTest

class TestWizardsIntegration(BaseFuncTest):
    def change_show_badge(self, data, in_show_badge):
        proto_data = zlib.decompress(data)
        d = meta_pb2.TReport()
        d.ParseFromString(proto_data)

        found, added = (False, False)
        debug_docs, debug_sd = ([], [])

        debug_docs.append('GROUPING=' + str(len(d.Grouping)))
        for s in d.Grouping:
            debug_docs.append('\tGROUPS=' + str(len(s.Group)))
            for g in s.Group:
                debug_docs.append('\t\tDOCS=' + str(len(g.Document)))
                for doc in g.Document:
                    debug_docs.append("\t\t\t" + doc.ServerDescr)
                    if doc.ServerDescr == 'AFISHA_PROXY':
                        found = True
                        for b in doc.ArchiveInfo.GtaRelatedAttribute:
                            if b.Key == '_SerpInfo':
                                j = json.loads(b.Value)
                                if j[u'type'] != u'afisha':
                                    debug_sd.append(str(b))
                                    continue

                                j[u'mods'][0][u'actions'][u'change'][1] = in_show_badge
                                b.Value = json.dumps(j)
                                added = True

        assert found, "\n".join(debug_docs) + "\n\nAFISHA_PROXY document was not found in UPPER response"
        assert added, "\n".join(debug_sd) + "in_show_badge was not added in document"

        new_resp = d.SerializeToString()
        return zlib.compress(new_resp)

    @pytest.mark.unstable
    @pytest.mark.parametrize(('s_show_badge'), [
        1,
        0
    ])
    def test_wizard_afisha_ban(self, query, s_show_badge):
        """
        https://st.yandex-team.ru/WIZARD-8535
        Надо банить часть объективного ответа, если из источника AFISHA_PROXY к нам пришел параметр ['mods'][0][u'actions'][u'change'] 1
        s_show_badge = 0 : В таком случае показываем только актеров (отправляем на верстку display_options->show_badge = 0)
        s_show_badge = 1 : Показываем полный объективный ответ (отправляем на верстку display_options->show_badge = 1)
        """
        query.set_params({'text': 'афиша кино'})
        #разворачиваем данные, правим их и заворачиваем обратно
        upper = self.change_show_badge(self.noapache_resp(query), s_show_badge)
        #нам надо проверить, что на верстку ушло то же, что мы ожидаем
        resp = self.json_request(query, source=('UPPER', self.get_config(data)))
        #print json.dumps(resp['searchdata']['docs_right'][0]['snippets']['full']['data']['display_options'], indent=4)
        show_badge = resp.data['searchdata']['docs_right'][0]['snippets']['full']['data']['display_options']['show_badge']
        #проверяем, что на верстку ушло то, что мы ожидаем
        assert show_badge == s_show_badge


