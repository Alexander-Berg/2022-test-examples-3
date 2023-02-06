import mongomock
import unittest

from pymongo.errors import DuplicateKeyError
from bson import InvalidDocument

class MongomockMonkeyPatchTestCase(unittest.TestCase):

    def test_mongomock_insert_doc_without_reserved_kwds_on_upsert_with_set(self):
        collection = mongomock.MongoClient().db.collection

        query = {
            '_id': 1,
            'key': {'$exists': True},
            '$or': [
                {'other_key': {'$lt': 1}},
                {'other_key': {'$exists': False}}
            ]
        }
        doc = {'other_key': 2}
        collection.update(query, {'$set': doc}, upsert=True)
        self.assertEquals(collection.find_one({'_id': 1}), {'_id': 1, 'other_key': 2})
        self.assertRaises(DuplicateKeyError, collection.update, query, {'$set': doc}, upsert=True)

    def test_mongomock_raw_insert_still_raises_error_on_invalid_doc(self):
        collection = mongomock.MongoClient().db.collection
        self.assertRaises(InvalidDocument, collection.insert, {'_id': 1, '$or': []})
