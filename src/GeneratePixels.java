
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class GeneratePixels implements Runnable {

    private final double miniY, maxiY, x;
    private final int id;
    private final ArrayList<Polygon> polygons;

    final HashSet<Point> pixelPoints = new HashSet<>();
    final Thread thread;

    public GeneratePixels(double miniY, double maxiY, double x, int id, ArrayList<Polygon> polygons) {
        this.miniY = miniY;
        this.maxiY = maxiY;
        this.x = x;
        this.id = id;
        this.polygons = polygons;

        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {

        int j;

        int idx[] = new int[5];
        Point p;

        for (double y = miniY; y <= maxiY; y += Constants.RESOLUTION) {

            for (j = 0; j < 5; j++) {
                idx[j] = 0;
            }

            p = new Point(x, y);

            for (j = 0; j < polygons.size(); j++) {
                if (p.isInside(polygons.get(j))) {
                    idx[0]++;
                }
            }

            if (idx[0] == 0 && Constants.RESOLUTION > 1) {
                for (j = 0; j < polygons.size(); j++) {
                    if ((p.add(new Point(-1, 0))).isInside(polygons.get(j))) {
                        idx[1]++;
                    }
                }
                for (j = 0; j < polygons.size(); j++) {
                    if ((p.add(new Point(1, 0))).isInside(polygons.get(j))) {
                        idx[2]++;
                    }
                }
                for (j = 0; j < polygons.size(); j++) {
                    if ((p.add(new Point(0, -1))).isInside(polygons.get(j))) {
                        idx[3]++;
                    }
                }
                for (j = 0; j < polygons.size(); j++) {
                    if ((p.add(new Point(0, 1))).isInside(polygons.get(j))) {
                        idx[4]++;
                    }
                }
            }

            for (j = 0; j < 5; j++) {
                if (idx[j] % 2 == 1) {
                    pixelPoints.add(p);
                    break;
                }
            }
        }

        if (Constants.PRINT || Constants.WHOLEMAP) {
            System.out.println("Thread " + id + " has finished pixel generation at " + new Date(System.currentTimeMillis()));
        }
    }

}
