import os
import glob
import tqdm
import time
import cv2
from lib.detector.plt_detector import PLTDetector
from lib.types.image import Image


def test_tf_detection(tf_config, image):
    detector = PLTDetector(tf_config)
    ret, detector_outputs = detector.detect(image)
    assert ret
    img = None
    for out in detector_outputs:
        if img is None:
            img = out.image.data.copy()
        x, y, w, h = out.detection.box.get_coordinates()
        img = cv2.rectangle(img, (x, y), (x + w, y + h), (36, 255, 12), 1)
        cv2.putText(img,
                    f'score={out.detection.score:.3f}',
                    (x, y - 10),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.9, (36, 255, 12), 2)
    cv2.imwrite("/home/ymbot-jetson/dev/py_inventory_mvp/data/results/test_image_vis_tf.jpeg", img)


def test_tf_detector_with_perf(tf_config, images_base_folder):
    detector = PLTDetector(tf_config)
    mask = os.path.join(images_base_folder, "*", "*.jpeg")
    print(f"Searching files for perf test by {mask}")
    images_path = sorted(glob.glob(mask))
    print(f"{len(images_path)} files found")
    images = []
    for path in images_path[:500]:
        images.append(Image(cv2.imread(path)))
    start = time.time()
    for image in tqdm.tqdm(images):
        detector.detect(image)
    print(f"Processing of {len(images)} frames took {time.time() - start} seconds")
