import datetime
import pytz

from cars.users.factories.user_documents import UserDocumentFactory
from cars.users.models.user_documents import UserDocument, UserDocumentPhoto
from ...factories.assignment import YangAssignmentFactory
from ...models.assignment import YangAssignment


class YangAssignmentTestHelper:

    def __init__(self, user, datasync_client):
        self._user = user
        self._datasync = datasync_client

    def create_processed_yang_assignment(self,
                                         user=None,
                                         fraud_status=YangAssignment.Status.NOT_FRAUD,
                                         license_data=None,
                                         passport_data=None,
                                         license_data_override=None,
                                         passport_data_override=None,
                                         processed_at=None,
                                         verified_at=None,
                                         **kwargs):
        if user is None:
            user = self._user

        if processed_at is None:
            processed_at = pytz.utc.localize(datetime.datetime(2017, 1, 1))

        if verified_at is None:
            verified_at = pytz.utc.localize(datetime.datetime(2017, 1, 1))

        passport = UserDocumentFactory.create(
            user=user,
            type=UserDocument.Type.PASSPORT.value,
        )
        driver_license = UserDocumentFactory.create(
            user=user,
            type=UserDocument.Type.DRIVER_LICENSE.value,
        )

        ok_status = UserDocumentPhoto.VerificationStatus.OK.value
        kwargs.setdefault('passport_biographical__verification_status', ok_status)
        kwargs.setdefault('passport_registration__verification_status', ok_status)
        kwargs.setdefault('passport_selfie__verification_status', ok_status)
        kwargs.setdefault('license_front__verification_status', ok_status)
        kwargs.setdefault('license_back__verification_status', ok_status)
        assignment = YangAssignmentFactory.create(
            processed_at=processed_at,
            ingested_at=None,
            is_fraud=fraud_status.value,
            passport_biographical__type=UserDocumentPhoto.Type.PASSPORT_BIOGRAPHICAL.value,
            passport_biographical__verified_at=verified_at,
            passport_registration__type=UserDocumentPhoto.Type.PASSPORT_REGISTRATION.value,
            passport_registration__verified_at=verified_at,
            passport_selfie__type=UserDocumentPhoto.Type.PASSPORT_SELFIE.value,
            passport_selfie__verified_at=verified_at,
            license_front__type=UserDocumentPhoto.Type.DRIVER_LICENSE_FRONT.value,
            license_front__verified_at=verified_at,
            license_back__type=UserDocumentPhoto.Type.DRIVER_LICENSE_BACK.value,
            license_back__verified_at=verified_at,
            passport_biographical__document=passport,
            passport_registration__document=passport,
            passport_selfie__document=passport,
            license_front__document=driver_license,
            license_back__document=driver_license,
            **kwargs
        )

        if license_data is None:
            license_data = {
                'prev_licence_number': '1234567890',
                'last_name': 'ФАМИЛИЯ',
                'categories_b_valid_from_date': '1999-12-31T21:00:00.000Z',
                'number': '1234567890',
                'issue_date': '2000-01-01T00:00:00.000Z',
                'first_name': 'ИМЯ',
                'birth_date': '1992-11-27T00:00:00.000Z',
                'middle_name': 'ОТЧЕСТВО',
                'prev_licence_issue_date': '2000-01-01T00:00:00.000Z',
                'categories': 'B',
                'categories_b_valid_to_date': '2020-01-01T00:00:00.000Z',
                'id': 'carsharing'
            }
        if license_data_override:
            license_data.update(license_data_override)

        if passport_data is None:
            passport_data = {
                'registration_housing': '5',
                'last_name': 'ФАМИЛИЯ',
                'subdivision_code': '123-123',
                'registration_apartment': '19',
                'doc_type': 'id',
                'registration_locality': 'МОСКВА',
                'registration_region': 'МОСКОВСКАЯ',
                'registration_house': '1А',
                'gender': 'МУЖ',
                'birth_date': '1992-11-27T00:00:00.000Z',
                'middle_name': 'ОТЧЕСТВО',
                'registration_area': 'ЦЕНТРАЛЬНЫЙ',
                'citizenship': 'РОССИЙСКАЯ ФЕДЕРАЦИЯ',
                'doc_value': '1585941000',
                'registration_street': 'ПУШКИНА',
                'id': 'carsharing',
                'first_name': 'ИМЯ ',
                'issue_date': '2000-01-01T00:00:00.000Z',
                'birth_place': 'МОСКВА'
            }
        if passport_data_override:
            passport_data.update(passport_data_override)

        self._datasync.update_license(
            uid=assignment.passport_biographical.user.uid,
            new_data=license_data,
        )
        self._datasync.update_passport(
            uid=assignment.passport_biographical.user.uid,
            new_data=passport_data,
        )
        self._datasync.update_passport_unverified(
            uid=assignment.passport_biographical.user.uid,
            key=assignment.passport_biographical.user.passport_ds_revision,
            new_data=passport_data,
        )
        self._datasync.update_license_unverified(
            uid=assignment.passport_biographical.user.uid,
            key=assignment.passport_biographical.user.driving_license_ds_revision,
            new_data=license_data,
        )

        return assignment
