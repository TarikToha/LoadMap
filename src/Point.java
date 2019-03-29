
import java.io.Serializable;

public class Point implements Serializable {

    final double x, y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point add(Point p) {
        return new Point(x + p.x, y + p.y);
    }

    public Point minus(Point p) {
        return new Point(x - p.x, y - p.y);
    }

    public Point mult(double c) {
        return new Point(x * c, y * c);
    }

    public double dot(Point p) {
        return x * p.x + y * p.y;
    }

    public double cross(Point p) {
        return x * p.y - y * p.x;
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y);
    }

    public Point scale(double s) {
        return new Point((s * x) / this.magnitude(), (s * y) / this.magnitude());
    }

    public double distFrom(Point p) {
        return this.minus(p).magnitude();
    }

    /*
    * poly is a polygon represented by an array of points. The function returns
    * true if the point is inside the polygon
     */
    //https://www.codeproject.com/Tips/84226/Is-a-Point-inside-a-Polygon
    public boolean isInside(Polygon polygon) {

        if (this.x < polygon.xMins || this.x > polygon.xMaxs || this.y < polygon.yMins || this.y > polygon.yMaxs) {
            return false;
        }

        boolean c = false;
        for (int i = 0; i < polygon.polygon.size(); i++) {
            int j = (i + 1) % polygon.polygon.size();
            if ((polygon.polygon.get(i).y <= this.y && this.y < polygon.polygon.get(j).y || polygon.polygon.get(j).y <= this.y && this.y < polygon.polygon.get(i).y)
                    && this.x < polygon.polygon.get(i).x + (polygon.polygon.get(j).x - polygon.polygon.get(i).x) * (this.y - polygon.polygon.get(i).y) / (polygon.polygon.get(j).y - polygon.polygon.get(i).y)) {
                c = !c;
            }
        }

        return c;
    }

    /**
     * *For hashing*
     */
    @Override
    public boolean equals(Object o) {
        Point p = (Point) o;
        return this.x == p.x && this.y == p.y;
    }

    @Override
    public int hashCode() {
        return (int) (this.x * 1000 + this.y);
    }

    @Override
    public String toString() {
        return "x = " + x + ", y = " + y + "; ";
    }

}
