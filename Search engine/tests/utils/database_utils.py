import os
import yt.wrapper

from search.martylib.yt_utils.manager import YtManager

from search.zephyr.enigma.zephyr import models

manager = YtManager()
manager.set_project_path('//tmp')
yt_proxy = os.environ.get('YT_PROXY')


def clear_db():
    for model in models.YStage, models.YStaticBundle:
        if yt.wrapper.exists(f'//tmp/{model.TABLE_NAME}'):
            model.remove_table()
        model.create_table()
