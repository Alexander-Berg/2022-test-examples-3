package ru.yandex.market.mbo.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AbstractAmazonS3;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author ayratgdl
 * @date 07.05.17
 */
public class AmazonS3Mock extends AbstractAmazonS3 {
    private Map<String, byte[]> storage = new ConcurrentHashMap<>();

    // The real S3 batch size is 1000 by default
    private int batchSize = 10;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Set<String> getAllKeys() {
        return storage.keySet();
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, File file)
        throws AmazonClientException, AmazonServiceException {

        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            storage.put(key, bytes);
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata)
        throws AmazonClientException, AmazonServiceException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteStreams.copy(input, output);
            byte[] bytes = output.toByteArray();
            storage.put(key, bytes);
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest getObjectRequest, File destinationFile)
            throws AmazonClientException, AmazonServiceException {

        String key = getObjectRequest.getKey();
        try {
            FileUtils.writeByteArrayToFile(destinationFile, storage.get(key));
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public S3Object getObject(String bucketName, String key) throws AmazonClientException, AmazonServiceException {
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(storage.get(key)));
        return s3Object;
    }

    @Override
    public void deleteObject(String bucketName, String key)
            throws AmazonClientException, AmazonServiceException {
        storage.remove(key);
    }

    @Override
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest)
            throws AmazonClientException, AmazonServiceException {
        for (DeleteObjectsRequest.KeyVersion keyVersion : deleteObjectsRequest.getKeys()) {
            storage.remove(keyVersion.getKey());
        }
        return null;
    }

    @Override
    public boolean doesObjectExist(String bucketName, String objectName)
            throws AmazonServiceException, AmazonClientException {
        return storage.containsKey(objectName);
    }

    @Override
    public ObjectListing listObjects(String bucketName, String prefix)
        throws AmazonClientException, AmazonServiceException {
        return listObj(prefix, null);
    }

    @Override
    public ObjectListing listNextBatchOfObjects(
        ObjectListing previousObjectListing) throws AmazonClientException,
        AmazonServiceException {
        return listObj(null, previousObjectListing);
    }

    private ObjectListing listObj(String prefix, ObjectListing previousObjectListing)
        throws AmazonClientException, AmazonServiceException {
            ObjectListing result = new ObjectListing();
            int counter = 0;
            List<String> keys;

            if (previousObjectListing == null) {
                // listObjects call
                keys = getKeys(prefix);
            } else {
                // listNextBatchOfObjects call
                prefix = previousObjectListing.getPrefix();
                keys = getKeys(prefix);

                String previousLastKey = previousObjectListing.getObjectSummaries()
                    .get(previousObjectListing.getObjectSummaries().size() - 1).getKey();
                keys = keys.subList(keys.indexOf(previousLastKey) + 1, keys.size());
            }

            for (String key : keys) {
                S3ObjectSummary objectSummary = new S3ObjectSummary();
                objectSummary.setKey(key);
                result.getObjectSummaries().add(objectSummary);
                result.setPrefix(prefix);
                counter++;
                if (counter == batchSize) {
                    break;
                }
            }
            result.setTruncated(counter < keys.size());
            return result;
    }

    private List<String> getKeys(String prefix) {
        // We need to sort it otherwise listNextBatchOfObjects won't work correctly if you add
        // new objects during walk through batches
        return storage.keySet().stream().filter(key -> key.startsWith(prefix)).sorted().collect(Collectors.toList());
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(
        InitiateMultipartUploadRequest initiateMultipartUploadRequest)
        throws AmazonClientException, AmazonServiceException {
        InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
        result.setUploadId(Instant.now().toString());
        return result;
    }

    public UploadPartResult uploadPart(UploadPartRequest request)
        throws AmazonClientException, AmazonServiceException {
        putObject(null, request.getKey(), request.getInputStream(), null);
        UploadPartResult result = new UploadPartResult();
        result.setETag("hack Amazon");
        return result;
    }

    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request)
        throws AmazonClientException, AmazonServiceException {
        return null;
    }

    @Override
    public CopyObjectResult copyObject(CopyObjectRequest copyObjRequest) {
        storage.put(copyObjRequest.getDestinationKey(), storage.get(copyObjRequest.getSourceKey()));
        return null;
    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String key)
        throws AmazonClientException, AmazonServiceException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(1);
        return metadata;
    }


}
