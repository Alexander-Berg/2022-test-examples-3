package ru.yandex.market.clab.test;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.clab.common.service.nas.NasService;
import ru.yandex.market.clab.common.service.nas.PhotoServiceImpl;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author anmalysh
 * @since 1/24/2019
 */
@Configuration
public class NasTestHelper {

    private static final long SEED = 930841029489L;
    private static final String TEST_RAW_PHOTO = "/nas/test.CR2";
    private static final String TEST_EDITED_PHOTO = "/nas/test.jpg";

    @Autowired
    private NasService nasService;

    @Value("${contentlab.nas.photo.root.dir:}")
    private String photoRootDir;

    private ThreadLocal<List<String>> testUsedInProcessDirs = ThreadLocal.withInitial(ArrayList::new);
    private ThreadLocal<List<String>> testUsedProcessedDirs = ThreadLocal.withInitial(ArrayList::new);

    private EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .stringLengthRange(20, 20)
        .seed(SEED)
        .build();

    public List<String> createRawPhotos(Good good, int count, boolean processed) {
        List<String> fileNames = new ArrayList<>();
        String barcodeRootDir = checkAndCreateGoodDir(good, processed);

        String rawPhotoDir = nasService.getRelativePath(barcodeRootDir, PhotoServiceImpl.RAW_FOLDER);
        if (!nasService.directoryExists(rawPhotoDir)) {
            nasService.createDir(rawPhotoDir);
        }

        for (int i = 0; i < count; i++) {
            InputStream fileStream = getClass().getResourceAsStream(TEST_RAW_PHOTO);
            String fileName = random.nextObject(String.class) + ".CR2";
            String filePath = nasService.getRelativePath(rawPhotoDir, fileName);
            nasService.createFile(filePath, fileStream);
            fileNames.add(fileName);
        }
        return fileNames;
    }

    public String createCustomFile(Good good, Collection<String> path, boolean processed) {
        String barcodeRootDir = checkAndCreateGoodDir(good, processed);

        Iterator<String> paths = path.iterator();
        String createdPath = barcodeRootDir;
        while (paths.hasNext()) {
            String pathPart = paths.next();
            if (paths.hasNext()) {
                createdPath = nasService.getRelativePath(createdPath, pathPart);
                if (!nasService.directoryExists(createdPath)) {
                    nasService.createDir(createdPath);
                }
            } else {
                String fullFilePath = nasService.getRelativePath(createdPath, pathPart);
                nasService.createFile(fullFilePath, new ByteArrayInputStream(new byte[0]));
                return fullFilePath;
            }
        }
        throw new IllegalArgumentException("paths is empty");
    }

    public boolean fileExists(Good good, Collection<String> path, boolean processed) {
        String barcodeRootDir = checkAndCreateGoodDir(good, processed);
        String[] pathParts = Stream.concat(Stream.of(barcodeRootDir), path.stream()).toArray(String[]::new);
        String fullFilePath = nasService.getRelativePath(pathParts);
        return nasService.fileExists(fullFilePath);
    }

    public List<String> createEditedPhotos(Good good, int count, boolean processed) {
        List<String> fileNames = new ArrayList<>();
        String barcodeRootDir = checkAndCreateGoodDir(good, processed);

        String editedPhotoDir = nasService.getRelativePath(barcodeRootDir, PhotoServiceImpl.EDITED_FOLDER);
        if (!nasService.directoryExists(editedPhotoDir)) {
            nasService.createDir(editedPhotoDir);
        }

        for (int i = 0; i < count; i++) {
            InputStream fileStream = getClass().getResourceAsStream(TEST_EDITED_PHOTO);
            String fileName = random.nextObject(String.class) + ".jpeg";
            String filePath = nasService.getRelativePath(editedPhotoDir, fileName);
            nasService.createFile(filePath, fileStream);
            fileNames.add(fileName);
        }
        return fileNames;
    }

    public List<String> getInProcessRawPhotos(Good good) {
        String dir = nasService.getRelativePath(photoRootDir, PhotoServiceImpl.IN_PROCESS_FOLDER,
            good.getWhBarcode(), PhotoServiceImpl.RAW_FOLDER);
        return nasService.list(dir);
    }

    public List<String> getProcessedRawPhotos(Good good) {
        String dir = nasService.getRelativePath(photoRootDir, PhotoServiceImpl.PROCESSED_FOLDER,
            getProcessedGoodDirName(good), PhotoServiceImpl.RAW_FOLDER);
        return nasService.list(dir);
    }

    public List<String> getInProcessPhotoEditingPhotos(String login, Good good) {
        String dir = nasService.getRelativePath(photoRootDir, PhotoServiceImpl.IN_PROCESS_FOLDER,
            login, good.getWhBarcode(), PhotoServiceImpl.EDITED_FOLDER);
        return nasService.list(dir);
    }

    public List<String> getInProcessEditedPhotos(Good good) {
        String dir = nasService.getRelativePath(photoRootDir, PhotoServiceImpl.IN_PROCESS_FOLDER,
            good.getWhBarcode(), PhotoServiceImpl.EDITED_FOLDER);
        return nasService.list(dir);
    }

    public List<String> getProcessedEditedPhotos(Good good) {
        String dir = nasService.getRelativePath(photoRootDir, PhotoServiceImpl.PROCESSED_FOLDER,
            getProcessedGoodDirName(good), PhotoServiceImpl.EDITED_FOLDER);
        return nasService.list(dir);
    }

    public boolean checkInProcessDirectoryExists(Good good) {
        String dir = nasService.getRelativePath(photoRootDir, PhotoServiceImpl.IN_PROCESS_FOLDER, good.getWhBarcode());
        return nasService.directoryExists(dir);
    }

    public boolean checkProcessedDirectoryExists(Good good) {
        String dir = nasService.getRelativePath(photoRootDir, PhotoServiceImpl.PROCESSED_FOLDER,
            getProcessedGoodDirName(good));
        return nasService.directoryExists(dir);
    }

    public boolean checkProcessedRawDirectoryExists(Good good) {
        String dir = nasService.getRelativePath(photoRootDir, PhotoServiceImpl.PROCESSED_FOLDER,
            getProcessedGoodDirName(good), PhotoServiceImpl.RAW_FOLDER);
        return nasService.directoryExists(dir);
    }

    public boolean checkProcessedEditedDirectoryExists(Good good) {
        String dir = nasService.getRelativePath(photoRootDir, PhotoServiceImpl.PROCESSED_FOLDER,
            getProcessedGoodDirName(good), PhotoServiceImpl.EDITED_FOLDER);
        return nasService.directoryExists(dir);
    }

    public void cleanTestDirs(Collection<String> lookupLogins) {
        List<String> usedInProcess = testUsedInProcessDirs.get();
        List<String> usedProcessed = testUsedProcessedDirs.get();
        for (String login : lookupLogins) {
            for (String barcode : usedInProcess) {
                String userBarcodePath = nasService.getRelativePath(photoRootDir,
                    PhotoServiceImpl.IN_PROCESS_FOLDER, login, barcode);
                deleteIfExists(userBarcodePath);
            }
        }
        usedInProcess.forEach(inProcessDir -> {
            String barcodeInProcessDir = nasService.getRelativePath(
                photoRootDir, PhotoServiceImpl.IN_PROCESS_FOLDER, inProcessDir);
            deleteIfExists(barcodeInProcessDir);
        });
        usedProcessed.forEach(processedDir -> {
            String barcodeProcessed = nasService.getRelativePath(
                photoRootDir, PhotoServiceImpl.PROCESSED_FOLDER, processedDir);
            deleteIfExists(barcodeProcessed);
        });
        testUsedInProcessDirs.set(new ArrayList<>());
        testUsedProcessedDirs.set(new ArrayList<>());
    }

    private void deleteIfExists(String barcodeInProcessDir) {
        if (nasService.directoryExists(barcodeInProcessDir)) {
            nasService.delete(barcodeInProcessDir);
        }
    }

    private String checkAndCreateGoodDir(Good good, boolean processed) {
        String inProcessDir = nasService.getRelativePath(photoRootDir, PhotoServiceImpl.IN_PROCESS_FOLDER);
        if (!nasService.directoryExists(inProcessDir)) {
            nasService.createDir(inProcessDir);
        }
        String processedDir = nasService.getRelativePath(photoRootDir, PhotoServiceImpl.PROCESSED_FOLDER);
        if (!nasService.directoryExists(processedDir)) {
            nasService.createDir(processedDir);
        }
        String goodProcessedDir = getProcessedGoodDirName(good);
        String barcodeRootDir = processed ?
            nasService.getRelativePath(processedDir, goodProcessedDir) :
            nasService.getRelativePath(inProcessDir, good.getWhBarcode());
        if (!nasService.directoryExists(barcodeRootDir)) {
            nasService.createDir(barcodeRootDir);
        }
        testUsedInProcessDirs.get().add(good.getWhBarcode());
        testUsedProcessedDirs.get().add(goodProcessedDir);
        return barcodeRootDir;
    }

    private String getProcessedGoodDirName(Good good) {
        return good.getWhBarcode() + "_" + good.getId();
    }

    @Bean
    NasTestHelper nasHelper() {
        return new NasTestHelper();
    }
}
