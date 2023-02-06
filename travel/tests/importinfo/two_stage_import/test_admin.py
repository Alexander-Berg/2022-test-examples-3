# -*- coding: utf-8 -*-

import pytest
from django.core.urlresolvers import reverse

from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage
from tester.factories import create_supplier


@pytest.mark.dbuser
def test_tsi_package_fill_data_url(superuser_client):
    supplier = create_supplier(code='test_supplier')
    tsi_package = TwoStageImportPackage.objects.create(title=u'test', supplier=supplier)
    response = superuser_client.get(
        reverse('admin:%s_%s_%s' % (tsi_package._meta.app_label, tsi_package._meta.model_name, 'change'),
                args=(tsi_package.id,)))

    assert response.status_code == 200
    assert response.context_data['data_url']
