import cv2
from lib.detector.plt_detector import PLTDetector


def test_trt_detection(trt_config, image):
    detector = PLTDetector(trt_config)
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
    cv2.imwrite("/home/ymbot-jetson/dev/py_inventory_mvp/data/results/test_image_vis_trt.jpeg", img)
