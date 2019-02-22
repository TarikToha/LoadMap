
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DetectRawCornerPoints implements Runnable {

    private final Point ip;
    private final HashSet<Point> pixelPoints;
    private final int id;

    final HashSet<Point> rawTerminalPoints = new HashSet<>();
    final HashSet<Point> rawNonTerminalPoints = new HashSet<>();
    final List<String[]> writeAll = new ArrayList<>();
    final Map pointWidth = new HashMap();
    final Thread thread;

    public DetectRawCornerPoints(Point ip, HashSet<Point> pixelPoints, int id) {
        this.ip = ip;
        this.pixelPoints = pixelPoints;
        this.id = id;

        thread = new Thread(this);
        thread.start();
    }

    /*
     * Checks if any point in the range of x+-RESOLUTION/2, y+-RESOLUTION/2 is
     * present in the HashSet
     */
    private boolean doesContain(double x, double y, HashSet<Point> pts) {
        double lowX = Math.floor(x), lowY = Math.floor(y);
        int j;
        for (int i = -(Constants.RESOLUTION / 2); i <= (Constants.RESOLUTION + 1) / 2; i++) {
            for (j = -(Constants.RESOLUTION / 2); j <= (Constants.RESOLUTION + 1) / 2; j++) {
                if (pts.contains(new Point(lowX + i, lowY + j))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void run() {

        /**
         * *Check distance around 360 angle**
         */
        int i;
        double ang, r, dists[] = new double[360 / Constants.ANGRESOLUTION];
        for (i = 0; i < 360; i += Constants.ANGRESOLUTION) {
            ang = (i * Math.PI) / 180;
            for (r = Constants.LEASTDIST;; r++) {
                if (!doesContain(ip.x + r * Math.cos(ang), ip.y + r * Math.sin(ang), pixelPoints) || r > Constants.MOSTDIST) {
                    dists[i / Constants.ANGRESOLUTION] = r - 1;
                    break;
                }
            }
        }

        /**
         * *TempWidth will be the minimum distance around the point**
         */
        double tempWidth = Constants.INF;
        for (i = 0; i < 360; i += Constants.ANGRESOLUTION) {
            tempWidth = Math.min(tempWidth, dists[i / Constants.ANGRESOLUTION] + dists[((i + 180) % 360) / Constants.ANGRESOLUTION]);
        }

        pointWidth.put(new Point(ip.x, ip.y), tempWidth);

        Arrays.sort(dists);

        /**
         * *Calculating the threshold distance**
         */
        double threshDist = dists[(360 * Constants.QUANTILE) / (100 * Constants.ANGRESOLUTION)] * Constants.MULTI;

        /**
         * *Check which angles exists**
         */
        boolean angExist[] = new boolean[360];
        for (i = 0; i < 360; i += Constants.ANGRESOLUTION) {
            ang = (i * Math.PI) / 180;
            for (r = Constants.LEASTDIST; r <= threshDist; r++) {
                if (!doesContain(ip.x + r * Math.cos(ang), ip.y + r * Math.sin(ang), pixelPoints)) {
                    break;
                }
            }
            angExist[i] = r > threshDist;
        }

        /**
         * *Adding missed angles**
         */
        for (i = Constants.ANGRESOLUTION; i < 360 - Constants.ANGRESOLUTION; i += Constants.ANGRESOLUTION) {
            if (angExist[i - Constants.ANGRESOLUTION] && angExist[i + Constants.ANGRESOLUTION]) {
                angExist[i] = true;
            }
        }

        if (angExist[360 - Constants.ANGRESOLUTION] && angExist[Constants.ANGRESOLUTION]) {
            angExist[0] = true;
        }
        if (angExist[360 - 2 * Constants.ANGRESOLUTION] && angExist[0]) {
            angExist[360 - Constants.ANGRESOLUTION] = true;
        }

        /**
         * *Count continuous angle portions**
         */
        boolean prev = false;
        int count = 0;
        for (i = 0; i < 360; i += Constants.ANGRESOLUTION) {
            if (angExist[i] && !prev) {
                count++;
            }
            prev = angExist[i];
        }

        if (angExist[360 - Constants.ANGRESOLUTION] && angExist[0]) {
            count--;
        }

        if (count != 2) {
            if (count == 1) {
                writeAll.add(new String[]{"0", Double.toString(ip.x), Double.toString(ip.y)});
                rawTerminalPoints.add(new Point(ip.x, ip.y));
            } else {
                writeAll.add(new String[]{"1", Double.toString(ip.x), Double.toString(ip.y)});
                rawNonTerminalPoints.add(new Point(ip.x, ip.y));
            }
        }

        if (Constants.PRINT || Constants.WHOLEMAP) {
            System.out.println("Thread " + id + " has finished raw corner points detection at " + new Date(System.currentTimeMillis()));
        }
    }

}
