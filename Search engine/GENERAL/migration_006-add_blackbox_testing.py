# -*- coding: utf-8 -*-
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from extensions.configurations.sfront.model import Base, RequestsConfiguration, SimpleSources, HostGroup


def main():
    engine = create_engine('sqlite:///data/configurations/request.db')
    Base.metadata.bind = engine
    DBSession = sessionmaker(bind=engine)
    session = DBSession()
    for location in ["MSK", "SAS", "MAN"]:
        for project in ["WEB", "IMGS", "NEWS"]:
            for region in ["RKUB", "COM", "TUR"]:
                for env in ["HAMSTER", "PRIEMKA-IN-PRODUCTION", "PRODUCTION"]:
                    cfg = session.query(RequestsConfiguration).filter(RequestsConfiguration.location == location,
                                                                      RequestsConfiguration.region == region,
                                                                      RequestsConfiguration.project == project,
                                                                      RequestsConfiguration.environment == env).first()

                    host_group = HostGroup(expression="pass-test.yandex.ru",
                                           port=80,
                                           type='user',
                                           path="",
                                           groupping="none")
                    blackbox_testing = SimpleSources(name='BLACKBOX_TESTING', timeout=150, host_groups=host_group)
                    cfg.sources.append(blackbox_testing)
                    print "Done"

    # session.rollback()
    session.commit()


if __name__ == '__main__':
    main()

