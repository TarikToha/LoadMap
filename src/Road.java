
public class Road {

    int id, cornerId1, cornerId2;
    Point line00, line01, line10, line11, final1, final2;
    final double width;

    public Road(int id, int id1, int id2, Point c1, Point c2, double width) {
        this.id = id;
        cornerId1 = id1;
        cornerId2 = id2;
        this.width = width;

        double theta = Math.atan((c2.y - c1.y) / (c2.x - c1.x));
        double y = width / 2 * Math.cos(theta);
        double x = width / 2 * Math.sin(theta);

        line00 = c1.add(new Point(-x, y));
        line01 = c2.add(new Point(-x, y));
        line10 = c1.add(new Point(x, -y));
        line11 = c2.add(new Point(x, -y));

        if (line01.minus(line00).cross(c2.minus(line01)) > 0) {
            Point temp = line00;
            line00 = line01;
            line01 = temp;

            temp = line10;
            line10 = line11;
            line11 = temp;

            id = cornerId1;
            cornerId1 = cornerId2;
            cornerId2 = id;
        }

    }

    @Override
    public String toString() {
        return "id = " + id + ", id1 = " + cornerId1 + ", id2 = " + cornerId2 + ", " + line00 + line01 + line10 + line11 + width;
    }

}
