package ru.yandex.autotests.market.stat.attribute.ip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Segment<T extends Comparable<T>> implements Comparable<Segment<T>> {

    private final T startPoint;
    private final T endPoint;

    public Segment(T startPoint, T endPoint) {
        if (startPoint == null || endPoint == null) {
            throw new IllegalArgumentException("Start point and end point of the segment must be not null. [start point:" +
                    String.valueOf(startPoint) + "; end point :" + String.valueOf(endPoint) + "]");
        }
        if (startPoint.compareTo(endPoint) == 1) {
            throw new IllegalArgumentException("Start point less than end point of the segment. [start point:" +
                    String.valueOf(startPoint) + "; end point :" + String.valueOf(endPoint) + "]");
        }
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    private static <T extends Comparable<T>> T min(T object1, T object2) {
        return object1.compareTo(object2) < 0 ? object1 : object2;
    }

    private static <T extends Comparable<T>> T max(T object1, T object2) {
        return object1.compareTo(object2) > 0 ? object1 : object2;
    }

    public static <T extends Comparable<T>> List<Segment<T>> unionAllIntersectedSegment(Segment<T>... segments) {
        return unionAllIntersectedSegment(Arrays.asList(segments));
    }

    public static <T extends Comparable<T>> List<Segment<T>> unionAllIntersectedSegment(final List<Segment<T>> segments) {
        Segment<T>[] array = segments.toArray(new Segment[segments.size()]);
        Arrays.sort(array);
        List<Segment<T>> result = new ArrayList();
        Segment<T> current = array[0];
        for (Segment segment : array) {
            if (current.isIntersect(segment)) {
                current = current.union(segment);
            } else {
                result.add(current);
                current = segment;
            }
        }
        result.add(current);
        return result;
    }

    public T getStartPoint() {
        return startPoint;
    }

    public T getEndPoint() {
        return endPoint;
    }

    public int compareTo(Segment<T> other) {
        int compare = this.getStartPoint().compareTo(other.getStartPoint());
        if (compare == 0) {
            return this.getEndPoint().compareTo(other.getEndPoint());
        }
        return compare;
    }

    public boolean isContainsPoint(T point) {
        return this.startPoint.compareTo(point) <= 0 && this.endPoint.compareTo(point) >= 0;
    }

    public boolean isIntersect(Segment<T> other) {
        return this.isContainsPoint(other.getStartPoint()) || other.isContainsPoint(this.getStartPoint());
    }

    public Segment<T> union(Segment<T> other) {
        if (this.isIntersect(other)) {
            return new Segment<T>(min(this.getStartPoint(), other.getStartPoint()),
                    max(this.getEndPoint(), other.getEndPoint()));
        }
        throw new IllegalArgumentException("The segments do not intersect [" + this + ";" + other + "]");
    }

    public List<Segment<T>> separation(List<Segment<T>> others) {
        return separation(others.toArray(new Segment[others.size()]));
    }

    public List<Segment<T>> separation(Segment<T>... others) {
        List<Segment<T>> result = new ArrayList();
        Segment<T> segment = this;
        for (Segment<T> other : unionAllIntersectedSegment(others)) {
            List<Segment<T>> tmp = segment.separation(other);
            if (tmp.isEmpty()) {
                return result;
            }
            if (tmp.size() == 1) {
                segment = tmp.get(0);
            } else {
                result.add(tmp.get(0));
                segment = tmp.get(1);
            }
        }
        result.add(segment);
        return result;
    }

    public List<Segment<T>> separation(Segment<T> other) {
        List<Segment<T>> result = new ArrayList();
        if (this.isIntersect(other)) {
            if (this.isContainsPoint(other.getStartPoint())) {
                result.add(new Segment<T>(this.getStartPoint(), other.getStartPoint()));
            }
            if (this.isContainsPoint(other.getEndPoint())) {
                result.add(new Segment<T>(other.getEndPoint(), this.getEndPoint()));
            }
        } else {
            result.add(this);
        }
        return unionAllIntersectedSegment(result);
    }

    @Override
    public String toString() {
        return "Segment[" + this.getStartPoint().toString() + ";" + this.getEndPoint().toString() + "]";
    }
}