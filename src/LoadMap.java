
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoadMap {

    private static void handleException(Exception ex) {
        Logger.getLogger(LoadMap.class.getName()).log(Level.SEVERE, null, ex);
        System.exit(0);
    }

    public static void main(String[] args) {
        //for (int road = 0; road <= 3746; road++) {
        int road = 1314;
        long startTime = System.currentTimeMillis();

        try {

            if (!Constants.WHOLEMAP) {
                System.out.println("working road id = " + Constants.WORKINGROAD);
            } else {
                System.out.println("working road id = ALL");
            }

            System.out.println("program has started at " + new Date(startTime));
            System.out.println("-----------------------++++------------------------");
            new LoadMap(startTime, road);

            System.out.println("-----------------------++++------------------------");
            long endTime = System.currentTimeMillis();
            System.out.println("program has ended at " + new Date(endTime));

            long totalTime = endTime - startTime;
            System.out.println("totalTime = " + totalTime / 1000 + " seconds");
        } catch (Exception ex) {
            if (!"continue".equals(ex.getMessage())) {
                handleException(ex);
            }
        }
        //}
    }
    private double miniX = Float.MAX_VALUE;
    private double maxiX = 0;
    private double miniY = Float.MAX_VALUE;
    private double maxiY = 0;
    private int polySizeSum = 0;
    private ArrayList<Polygon> prevPolygons;
    private Thread pixelThread;
    private Thread rawCornerPointsThread;
    private Thread cornerPointsThread;
    private Thread roadThread;
    private Thread nodeThread;
    private Thread linkThread;
    private Thread pathThread;
    private final int[][] dirArray = {{-Constants.RESOLUTION, Constants.RESOLUTION, 0, 0}, {0, 0, -Constants.RESOLUTION, Constants.RESOLUTION}};
    private final Map pointWidth = new HashMap();
    private final Map cornersMap = new HashMap();
    private double biasX = Float.MAX_VALUE;
    private double biasY = Float.MAX_VALUE;
    private final HashSet<Point> rawTerminalPoints = new HashSet<>();

    public LoadMap(long startTime, int road) throws Exception {
        /**
         * *Get the polygons**
         */
        ArrayList<Polygon> polygons = getPolygons(road);

        long endTime = System.currentTimeMillis();

        System.out.println("Checkpoint 1 " + (maxiX - miniX) + " " + (maxiY - miniY) + " " + polySizeSum + " " + polygons.size());
        System.out.println("Time passed: " + (endTime - startTime));
        System.out.println("polygons have been extracted at " + new Date(endTime));
        System.out.println("------------------------------------------");
        long timeTracker = System.currentTimeMillis();
        /**
         * *Get pixel points. The Logic is: if any point around the current
         * point is inside a polygon then it's inside that polygon**
         */
        HashSet<Point> pixelPoints = getPixelPoints(polygons);
        endTime = System.currentTimeMillis();

        System.out.println("Checkpoint 2 " + pixelPoints.size());
        System.out.println("Time passed: " + (endTime - startTime));
        System.out.println("pixelPoints have been extracted at " + new Date(endTime));
        System.out.println("------------------------------------------");
        System.out.print(road + "\t" + (maxiX - miniX) + "\t" + (maxiY - miniY) + "\t" + polySizeSum + "\t" + prevPolygons.size()
                + "\t" + (System.currentTimeMillis() - timeTracker) + "\t");
        timeTracker = System.currentTimeMillis();
        /**
         * *Detect raw corner points**
         */
        HashSet<Point> rawCornerPoints = detectRawCornerPoints(pixelPoints);
        endTime = System.currentTimeMillis();

        System.out.println("Checkpoint 3 " + rawCornerPoints.size());
        System.out.println("Time passed: " + (endTime - startTime));
        System.out.println("rawCornerPoints have been extracted at " + new Date(endTime));
        System.out.println("------------------------------------------");
        System.out.print(pixelPoints.size() + "\t" + (System.currentTimeMillis() - timeTracker) + "\t");

        startTime = System.currentTimeMillis();
        /**
         * *Detect final corner points using BFS. The average ones**
         */
        ArrayList<Corner> nodes = detectCornerPoints(rawCornerPoints, pixelPoints);
        endTime = System.currentTimeMillis();

        System.out.println("Checkpoint 4 " + rawCornerPoints.size() + " " + nodes.size());
        System.out.println("Time passed: " + (endTime - startTime));
        System.out.println("nodes have been extracted at " + new Date(endTime));
        System.out.println("------------------------------------------");

        startTime = System.currentTimeMillis();
        /**
         * *Detect Connections. Corner id calculated in previous stage. Run
         * BFS**
         */
        ArrayList<Road> links = detectConnections(rawCornerPoints, pixelPoints, nodes);
        endTime = System.currentTimeMillis();

        System.out.println("Checkpoint 5 " + nodes.size() + " " + links.size());
        System.out.println("Time passed: " + (endTime - startTime));
        System.out.println("links have been extracted at " + new Date(endTime));
        System.out.println("------------------------------------------");
        startTime = System.currentTimeMillis();
        /**
         * *Formatted Output For Simulator**
         */
        writeData(nodes, links);
        endTime = System.currentTimeMillis();

        System.out.println("done simulator input generation");
        System.out.println("Time passed: " + (endTime - startTime));
        System.out.println("nodes, links, paths, and demands have been written at " + new Date(endTime));
        System.out.println(System.currentTimeMillis() - startTime);
    }

    private ArrayList<Point> getBorderPoints(int road) throws Exception {

        ArrayList<Point> borderPoints = new ArrayList<>();

        try (CSVReader cr = new CSVReader(new BufferedReader(new FileReader(Constants.FILENAMEIN)))) {
            List<String[]> readAll = cr.readAll();
            List<String> headers = Arrays.asList(readAll.get(0));
            int x = headers.indexOf("xcoord");
            int y = headers.indexOf("ycoord");
            if (!Constants.WHOLEMAP) {
                for (int i = 1; i < readAll.size(); i++) {
                    if (Double.parseDouble(readAll.get(i)[0]) == road) {
                        borderPoints.add(new Point(Double.parseDouble(readAll.get(i)[x]), Double.parseDouble(readAll.get(i)[y])));
                    }
                }
            } else {
                for (int i = 1; i < readAll.size(); i++) {
                    borderPoints.add(new Point(Double.parseDouble(readAll.get(i)[x]), Double.parseDouble(readAll.get(i)[y])));
                }
            }
        }

        return borderPoints;
    }

    /*
     * Checks if this polygon is a divider. If the average width of the polygon
     * is less than DIVIDERWIDTH, it will be identified as a divider
     */
    private boolean isTooNarrow(Polygon polygon) {
        double mostDist = 0, area = 0;
        int j;
        for (int i = 0; i < polygon.polygon.size(); i++) {
            for (j = i + 1; j < polygon.polygon.size(); j++) {
                mostDist = Math.max(mostDist, polygon.polygon.get(i).distFrom(polygon.polygon.get(j)));
            }
            //Shoelace formula
            area += polygon.polygon.get(i).x * polygon.polygon.get((i + 1) % polygon.polygon.size()).y - polygon.polygon.get(i).y * polygon.polygon.get((i + 1) % polygon.polygon.size()).x;
        }

        return 0.5 * Math.abs(area) / mostDist <= Constants.DIVIDERWIDTH;
    }

    private ArrayList<Polygon> getPolygons(int road) throws Exception {
        /**
         * *Get border points from the file**
         */
        ArrayList<Point> borderPoints = getBorderPoints(road);
        if (borderPoints.isEmpty()) {
            throw new Exception("continue");
        }
        int i, n;

        Polygon polygon;

        ArrayList<Polygon> polygons = new ArrayList<>();

        while (borderPoints.size() > 0) {
            /**
             * *Removing too close points**
             */
            for (i = 0; borderPoints.get(i + 1).x != borderPoints.get(0).x || borderPoints.get(i + 1).y != borderPoints.get(0).y; i++) {
                if (borderPoints.get(i).distFrom(borderPoints.get(i + 1)) < Constants.RESOLUTION) {
                    borderPoints.remove(i + 1);
                    i--;
                }
            }

            /**
             * *Creating the polygon**
             */
            polygon = new Polygon();

            polygon.xMins = polygon.xMaxs = borderPoints.get(i).x;
            polygon.yMins = polygon.yMaxs = borderPoints.get(i).y;

            n = i + 2;

            for (i = 0; i < n; i++) {
                polygon.xMins = Math.min(polygon.xMins, borderPoints.get(i).x);
                polygon.xMaxs = Math.max(polygon.xMaxs, borderPoints.get(i).x);
                polygon.yMins = Math.min(polygon.yMins, borderPoints.get(i).y);
                polygon.yMaxs = Math.max(polygon.yMaxs, borderPoints.get(i).y);
                polygon.polygon.add(borderPoints.get(i));
            }

            miniX = Math.min(miniX, polygon.xMins);
            maxiX = Math.max(maxiX, polygon.xMaxs);
            miniY = Math.min(miniY, polygon.yMins);
            maxiY = Math.max(maxiY, polygon.yMaxs);

            polygons.add(polygon);
            polySizeSum += polygon.polygon.size();
            borderPoints.subList(0, n).clear();

        }
        prevPolygons = new ArrayList<>(polygons);
        /**
         * *Eliminate dividers**
         */
        int j;
        if (polygons.size() > 1) {
            for (i = 0; i < polygons.size(); i++) {
                if (isTooNarrow(polygons.get(i))) {
                    //divider must be in another polygon
                    for (j = 0; j < polygons.size(); j++) {
                        if (j == i) {
                            continue;
                        }
                        if (polygons.get(i).polygon.get(0).isInside(polygons.get(j))) {
                            polygons.remove(i);
                            i--;
                            break;
                        }
                    }
                }
            }
        } else {
            throw new Exception("continue");
        }

        return polygons;
    }

    //computaionally very expensive
    private HashSet<Point> getPixelPoints(ArrayList<Polygon> polygons) throws Exception {

        miniX = Math.floor(miniX);
        maxiX = Math.ceil(maxiX);
        miniY = Math.floor(miniY);
        maxiY = Math.ceil(maxiY);

        ArrayList<GeneratePixels> generatePixels = new ArrayList<>();
        int id = 0;

//        System.out.println((maxiX - miniX) / Constants.RESOLUTION);
        for (double x = miniX; x <= maxiX; x += Constants.RESOLUTION) {
            generatePixels.add(new GeneratePixels(miniY, maxiY, x, id++, polygons));
        }

        for (id = 0; id < generatePixels.size(); id++) {
            generatePixels.get(id).thread.join();
        }

        HashSet<Point> pixelPoints = new HashSet<>();
        for (id = 0; id < generatePixels.size(); id++) {
            pixelPoints.addAll(generatePixels.get(id).pixelPoints);
        }

        writePixels(pixelPoints);

        return pixelPoints;

    }

    private void writePixels(HashSet<Point> pixelPoints) {
        pixelThread = new Thread() {
            @Override
            public void run() {
                List<String[]> writeAll = new ArrayList<>();
                writeAll.add(new String[]{"ID", "xcoord", "ycoord"});

                for (Point ip : pixelPoints) {
                    writeAll.add(new String[]{"0", Double.toString(ip.x), Double.toString(ip.y)});
                }

                try (CSVWriter cw = new CSVWriter(new BufferedWriter(new FileWriter(Constants.ALLPOINTS)))) {
                    cw.writeAll(writeAll);
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        };
        pixelThread.start();
    }

    //computaionally expensive
    private HashSet<Point> detectRawCornerPoints(HashSet<Point> pixelPoints) throws Exception {

        ArrayList<DetectRawCornerPoints> cornerRawDetection = new ArrayList<>();

        int i = 0;

//        System.out.println(pixelPoints.size());
        for (Point ip : pixelPoints) {
            cornerRawDetection.add(new DetectRawCornerPoints(ip, pixelPoints, i++));
        }

        for (i = 0; i < cornerRawDetection.size(); i++) {
            cornerRawDetection.get(i).thread.join();
        }

        HashSet<Point> rawNonTerminalPoints = new HashSet<>();

        List<String[]> writeAll = new ArrayList<>();
        writeAll.add(new String[]{"ID", "xcoord", "ycoord"});

        for (i = 0; i < cornerRawDetection.size(); i++) {
            rawTerminalPoints.addAll(cornerRawDetection.get(i).rawTerminalPoints);
            rawNonTerminalPoints.addAll(cornerRawDetection.get(i).rawNonTerminalPoints);
            writeAll.addAll(cornerRawDetection.get(i).writeAll);
            pointWidth.putAll(cornerRawDetection.get(i).pointWidth);
        }

        /**
         * *Expanding corner points**
         */
        int count;

        for (Point ip : pixelPoints) {
            if (rawTerminalPoints.contains(ip) || rawNonTerminalPoints.contains(ip)) {
                continue;
            }

            count = 0;
            for (i = 0; i < dirArray[0].length; i++) {
                if (rawTerminalPoints.contains(ip.add(new Point(dirArray[0][i], dirArray[1][i])))) {
                    count++;
                }
            }
            //avoid false points
            if (count > 1) {
                rawTerminalPoints.add(new Point(ip.x, ip.y));
                writeAll.add(new String[]{"0", Double.toString(ip.x), Double.toString(ip.y)});
            } else if (count == 0) {
                for (i = 0; i < dirArray[0].length; i++) {
                    if (rawNonTerminalPoints.contains(ip.add(new Point(dirArray[0][i], dirArray[1][i])))) {
                        count++;
                    }
                }
                if (count > 1) {
                    rawNonTerminalPoints.add(new Point(ip.x, ip.y));
                    writeAll.add(new String[]{"1", Double.toString(ip.x), Double.toString(ip.y)});
                }
            }
        }

        writeRawCornerPoints(writeAll);

        HashSet<Point> rawCornerPoints = new HashSet<>();
        rawCornerPoints.addAll(rawTerminalPoints);
        rawCornerPoints.addAll(rawNonTerminalPoints);

        return rawCornerPoints;
    }

    private void writeRawCornerPoints(List<String[]> writeAll) {
        rawCornerPointsThread = new Thread() {
            @Override
            public void run() {
                try (CSVWriter cw = new CSVWriter(new BufferedWriter(new FileWriter(Constants.CORNERPOINTSRAW)))) {
                    cw.writeAll(writeAll);
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        };
        rawCornerPointsThread.start();
    }

    private ArrayList<Corner> detectCornerPoints(HashSet<Point> rawCornerPoints, HashSet<Point> pixelPoints) {

        HashSet<Point> processedCorner = new HashSet<>(), cornerToDel = new HashSet<>(), cornerAdjacentPoint;

        double x, y;
        int rawCorners, cornerIdx = 0, i;

        ArrayList<Point> temp, potentialCornerToDel;
        Point point, neighbour;

        ArrayList<Corner> nodes = new ArrayList<>();
        List<String[]> writeAll = new ArrayList<>();
        writeAll.add(new String[]{"ID", "xcoord", "ycoord"});

        for (Point ip : rawCornerPoints) {

            if (processedCorner.contains(ip)) {
                continue;
            }

            x = y = 0;
            rawCorners = 0;

            temp = new ArrayList<>();
            temp.add(new Point(ip.x, ip.y));
            processedCorner.add(new Point(ip.x, ip.y));

            potentialCornerToDel = new ArrayList<>();
            cornerAdjacentPoint = new HashSet<>();

            while (temp.size() > 0) {
                point = temp.get(0);
                temp.remove(0);

                x += point.x;
                y += point.y;
                rawCorners++;

                cornersMap.put(new Point(point.x, point.y), cornerIdx);
                potentialCornerToDel.add(new Point(point.x, point.y));

                for (i = 0; i < dirArray[0].length; i++) {
                    neighbour = point.add(new Point(dirArray[0][i], dirArray[1][i]));
                    if (rawCornerPoints.contains(neighbour) && !processedCorner.contains(neighbour)) {
                        temp.add(neighbour);
                        processedCorner.add(neighbour);
                    } else if (!rawCornerPoints.contains(neighbour) && pixelPoints.contains(neighbour) && !cornerAdjacentPoint.contains(neighbour)) {
                        cornerAdjacentPoint.add(neighbour);
                    }
                }
            }

            int directions[][] = {{-Constants.RESOLUTION, -Constants.RESOLUTION, -Constants.RESOLUTION, Constants.RESOLUTION, Constants.RESOLUTION, Constants.RESOLUTION, 0, 0},
            {Constants.RESOLUTION, 0, -Constants.RESOLUTION, Constants.RESOLUTION, 0, -Constants.RESOLUTION, -Constants.RESOLUTION, Constants.RESOLUTION}};

            int count = 0;
            while (cornerAdjacentPoint.size() > 0) {
                temp = new ArrayList<>();
                temp.add(cornerAdjacentPoint.iterator().next());
                cornerAdjacentPoint.remove(temp.get(0));
                while (temp.size() > 0) {
                    point = temp.get(0);
                    temp.remove(0);
                    for (i = 0; i < directions[0].length; i++) {
                        neighbour = point.add(new Point(directions[0][i], directions[1][i]));
                        if (cornerAdjacentPoint.contains(neighbour)) {
                            temp.add(neighbour);
                            cornerAdjacentPoint.remove(neighbour);
                        }
                    }
                }
                count++;
            }

            if (rawCorners > 1 && (count > 2 || rawTerminalPoints.contains(ip))) {
                nodes.add(new Corner(cornerIdx, x / rawCorners, y / rawCorners, rawCorners * Constants.CORNERAREAFACTOR));
                writeAll.add(new String[]{Integer.toString(cornerIdx), Double.toString(x / rawCorners), Double.toString(y / rawCorners)});
                cornerIdx++;

            } else {
                for (Point tp : potentialCornerToDel) {
                    cornersMap.remove(tp);
                    cornerToDel.add(tp);
                }
            }
        }

        //cornerToDel.stream().forEach(rawCornerPoints::remove);
        rawCornerPoints.removeAll(cornerToDel);

        writeCornerPoints(writeAll);

        return nodes;
    }

    private void writeCornerPoints(List<String[]> writeAll) {
        cornerPointsThread = new Thread() {
            @Override
            public void run() {
                try (CSVWriter cw = new CSVWriter(new BufferedWriter(new FileWriter(Constants.CORNERPOINTS)))) {
                    cw.writeAll(writeAll);
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        };
        cornerPointsThread.start();
    }

    /*
     * Finds intersection of two lines
     */
    //https://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect
    private Point computeLineIntersection(Point a, Point b, Point c, Point d) {
        b = b.minus(a);
        d = c.minus(d);
        c = c.minus(a);
        //assert (b.magnitude() > Constants.EPS && d.magnitude() > Constants.EPS);
        return a.add(b.mult(c.cross(d) / b.cross(d)));
    }

    private ArrayList<Road> detectConnections(HashSet<Point> rawCornerPoints, HashSet<Point> pixelPoints, ArrayList<Corner> nodes) {

        HashSet<Point> processedPoint = new HashSet<>();
        int cornerIdx = nodes.size(), i, tempIdx;

        ArrayList<Point> temp;
        Point point, neighbour;

        Map roadCornerMap = new HashMap();

        double tempWidth, width;

        double roadWidth[][] = new double[cornerIdx][cornerIdx];
        for (i = 0; i < cornerIdx; i++) {
            Arrays.fill(roadWidth[i], Constants.INF);
        }

        for (Point ip : rawCornerPoints) {

            if (processedPoint.contains(ip)) {
                continue;
            }

            cornerIdx = (int) cornersMap.get(ip);

            temp = new ArrayList<>();
            temp.add(new Point(ip.x, ip.y));
            processedPoint.add(new Point(ip.x, ip.y));

            while (temp.size() > 0) {
                point = temp.get(0);
                temp.remove(0);

                for (i = 0; i < 4; i++) {
                    neighbour = point.add(new Point(dirArray[0][i], dirArray[1][i]));

                    if (pixelPoints.contains(neighbour) && !processedPoint.contains(neighbour)) {

                        if (cornersMap.containsKey(neighbour)) {
                            tempIdx = (int) cornersMap.get(neighbour);

                            if (cornerIdx == tempIdx) {
                                temp.add(neighbour);
                                processedPoint.add(neighbour);
                            } else {
                                nodes.get(cornerIdx).adjacent.add(tempIdx);
                                nodes.get(tempIdx).adjacent.add(cornerIdx);
                            }

                        } else {

                            if (roadCornerMap.containsKey(neighbour)) {
                                tempIdx = (int) roadCornerMap.get(neighbour);
                                if (cornerIdx == tempIdx) {
                                    continue;
                                }

                                width = (double) pointWidth.get(neighbour);
                                tempWidth = roadWidth[cornerIdx][tempIdx] / width;
                                if (tempWidth < Constants.TOLERANCE
                                        || roadWidth[cornerIdx][tempIdx] == Constants.INF) {
                                    tempWidth = Math.min(roadWidth[cornerIdx][tempIdx], width);
                                    roadWidth[cornerIdx][tempIdx] = roadWidth[tempIdx][cornerIdx] = tempWidth;
                                }
                                processedPoint.add(neighbour);

                            } else {
                                roadCornerMap.put(neighbour, cornerIdx);
                            }

                            temp.add(neighbour);
                        }
                    }
                }
            }
        }

        Road link;
        ArrayList< Road> links = new ArrayList<>();

        int m = 0;
        for (i = 0; i < nodes.size(); i++) {
            biasX = Math.min(biasX, nodes.get(i).x);
            biasY = Math.min(biasY, nodes.get(i).y);

            for (Integer it : nodes.get(i).adjacent) {
                if (it > i) {
                    if (roadWidth[i][it] == Constants.INF) {
                        continue;
                    }
                    link = new Road(m, i, it, nodes.get(i), nodes.get(it), roadWidth[i][it] - 1);
                    nodes.get(i).linkId.add(m);
                    nodes.get(it).linkId.add(m);
                    links.add(link);
                    m++;
                }
            }
        }

        biasX -= 10;
        biasY -= 10;

        //get road length
        int j, k;
        Point pp1, pp2;
        double scaling;

        for (i = 0; i < links.size(); i++) {

            cornerIdx = links.get(i).cornerId1;
            for (j = 0; j < nodes.get(cornerIdx).linkId.size(); j++) {
                k = nodes.get(cornerIdx).linkId.get(j);
                if (i == k) {
                    continue;
                }

                pp1 = computeLineIntersection(links.get(i).line00, links.get(i).line01, links.get(k).line00, links.get(k).line01);
                pp2 = computeLineIntersection(links.get(i).line10, links.get(i).line11, links.get(k).line10, links.get(k).line11);

                if (links.get(i).line01.distFrom(pp1) < links.get(i).line01.distFrom(links.get(i).line00)
                        && nodes.get(cornerIdx).distFrom(pp1) <= nodes.get(cornerIdx).radius) {
                    links.get(i).line00 = pp1;
                }
                if (links.get(i).line11.distFrom(pp2) < links.get(i).line11.distFrom(links.get(i).line10)
                        && nodes.get(cornerIdx).distFrom(pp2) <= nodes.get(cornerIdx).radius) {
                    links.get(i).line10 = pp2;
                }

                pp1 = computeLineIntersection(links.get(i).line00, links.get(i).line01, links.get(k).line10, links.get(k).line11);
                pp2 = computeLineIntersection(links.get(i).line10, links.get(i).line11, links.get(k).line00, links.get(k).line01);

                if (links.get(i).line01.distFrom(pp1) < links.get(i).line01.distFrom(links.get(i).line00)
                        && nodes.get(cornerIdx).distFrom(pp1) <= nodes.get(cornerIdx).radius) {
                    links.get(i).line00 = pp1;
                }
                if (links.get(i).line11.distFrom(pp2) < links.get(i).line11.distFrom(links.get(i).line10)
                        && nodes.get(cornerIdx).distFrom(pp2) <= nodes.get(cornerIdx).radius) {
                    links.get(i).line10 = pp2;
                }

            }

            if (links.get(i).line01.distFrom(links.get(i).line00) > links.get(i).line11.distFrom(links.get(i).line10)) {
                scaling = links.get(i).line11.distFrom(links.get(i).line10);
                neighbour = (links.get(i).line00.minus(links.get(i).line01)).scale(scaling);
                links.get(i).line00 = links.get(i).line01.add(neighbour);
            } else {
                scaling = links.get(i).line01.distFrom(links.get(i).line00);
                neighbour = (links.get(i).line10.minus(links.get(i).line11)).scale(scaling);
                links.get(i).line10 = links.get(i).line11.add(neighbour);
            }

            cornerIdx = links.get(i).cornerId2;
            for (j = 0; j < nodes.get(cornerIdx).linkId.size(); j++) {
                k = nodes.get(cornerIdx).linkId.get(j);
                if (i == k) {
                    continue;
                }

                pp1 = computeLineIntersection(links.get(i).line00, links.get(i).line01, links.get(k).line00, links.get(k).line01);
                pp2 = computeLineIntersection(links.get(i).line10, links.get(i).line11, links.get(k).line10, links.get(k).line11);

                if (links.get(i).line00.distFrom(pp1) < links.get(i).line00.distFrom(links.get(i).line01)
                        && nodes.get(cornerIdx).distFrom(pp1) <= nodes.get(cornerIdx).radius) {
                    links.get(i).line01 = pp1;
                }
                if (links.get(i).line10.distFrom(pp2) < links.get(i).line10.distFrom(links.get(i).line11)
                        && nodes.get(cornerIdx).distFrom(pp2) <= nodes.get(cornerIdx).radius) {
                    links.get(i).line11 = pp2;
                }

                pp1 = computeLineIntersection(links.get(i).line00, links.get(i).line01, links.get(k).line10, links.get(k).line11);
                pp2 = computeLineIntersection(links.get(i).line10, links.get(i).line11, links.get(k).line00, links.get(k).line01);

                if (links.get(i).line00.distFrom(pp1) < links.get(i).line00.distFrom(links.get(i).line01)
                        && nodes.get(cornerIdx).distFrom(pp1) <= nodes.get(cornerIdx).radius) {
                    links.get(i).line01 = pp1;
                }
                if (links.get(i).line10.distFrom(pp2) < links.get(i).line10.distFrom(links.get(i).line11)
                        && nodes.get(cornerIdx).distFrom(pp2) <= nodes.get(cornerIdx).radius) {
                    links.get(i).line11 = pp2;
                }

            }

            if (links.get(i).line00.distFrom(links.get(i).line01) > links.get(i).line10.distFrom(links.get(i).line11)) {
                scaling = links.get(i).line10.distFrom(links.get(i).line11);
                neighbour = (links.get(i).line01.minus(links.get(i).line00)).scale(scaling);
                links.get(i).line01 = links.get(i).line00.add(neighbour);
            } else {
                scaling = links.get(i).line00.distFrom(links.get(i).line01);
                neighbour = (links.get(i).line11.minus(links.get(i).line10)).scale(scaling);
                links.get(i).line11 = links.get(i).line10.add(neighbour);
            }
        }

        writeRoads(links);

        return links;

    }

    private void writeRoads(ArrayList<Road> links) {
        roadThread = new Thread() {
            @Override
            public void run() {
                List<String[]> writeAll = new ArrayList<>();
                writeAll.add(new String[]{"ID", "xcoord", "ycoord"});

                for (Road link : links) {
                    writeAll.add(new String[]{Integer.toString(link.id), Double.toString(link.line00.x), Double.toString(link.line00.y)});
                    writeAll.add(new String[]{Integer.toString(link.id), Double.toString(link.line01.x), Double.toString(link.line01.y)});
                    writeAll.add(new String[]{Integer.toString(link.id), Double.toString(link.line10.x), Double.toString(link.line10.y)});
                    writeAll.add(new String[]{Integer.toString(link.id), Double.toString(link.line11.x), Double.toString(link.line11.y)});
                }

                try (CSVWriter cw = new CSVWriter(new BufferedWriter(new FileWriter(Constants.ROADS)))) {
                    cw.writeAll(writeAll);
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        };
        roadThread.start();
    }

    private int[][] floydWarshall(int[][] adjMatrix) {

        int[][] next = new int[adjMatrix.length][adjMatrix.length];
        int i, j, k;

        for (i = 0; i < next.length; i++) {
            for (j = 0; j < next.length; j++) {
                if (i != j) {
                    next[i][j] = j;
                }
            }
        }

        for (k = 0; k < adjMatrix.length; k++) {
            for (i = 0; i < adjMatrix.length; i++) {
                for (j = 0; j < adjMatrix.length; j++) {
                    if (adjMatrix[i][k] + adjMatrix[k][j] < adjMatrix[i][j]) {
                        adjMatrix[i][j] = adjMatrix[i][k] + adjMatrix[k][j];
                        next[i][j] = next[i][k];
                    }
                }
            }
        }

        return next;
    }

    private String getPath(int src, int dest, int[][] next, int[][] pathMatrix) {

        String outPath = src + " " + dest + " ";

        ArrayList<Integer> path = new ArrayList<>();
        path.add(src);
        do {
            src = next[src][dest];
            path.add(src);
        } while (src != dest);

        for (int i = 0; i < path.size() - 1; i++) {
            outPath += pathMatrix[path.get(i)][path.get(i + 1)] + " ";
        }

        return outPath;
    }

    private void writeData(ArrayList<Corner> nodes, ArrayList<Road> links) throws Exception {

        nodeThread = new Thread() {
            @Override
            public void run() {
                try (BufferedWriter nodeFile = new BufferedWriter(new FileWriter(Constants.NODE))) {
                    String line = nodes.size() + "\n";
                    nodeFile.write(line);

                    int j;
                    for (int i = 0; i < nodes.size(); i++) {
                        line = "" + i + " " + (nodes.get(i).x - biasX) + " " + (-1) * (nodes.get(i).y - biasY);
                        for (j = 0; j < nodes.get(i).linkId.size(); j++) {
                            line += " " + nodes.get(i).linkId.get(j);
                        }
                        line += "\n";
                        nodeFile.write(line);
                    }
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        };
        nodeThread.start();

        linkThread = new Thread() {
            @Override
            public void run() {
                try (BufferedWriter linkFile = new BufferedWriter(new FileWriter(Constants.LINK))) {
                    String line = links.size() + "\n";
                    linkFile.write(line);

                    for (int i = 0; i < links.size(); i++) {
                        line = i + " " + links.get(i).cornerId1 + " " + links.get(i).cornerId2 + " 1\n";
                        linkFile.write(line);
                        line = "0 " + (links.get(i).line00.x - biasX) + " " + (-1) * (links.get(i).line00.y - biasY)
                                + " " + (links.get(i).line01.x - biasX) + " " + (-1) * (links.get(i).line01.y - biasY)
                                + " " + links.get(i).width + "\n";
                        linkFile.write(line);
                    }
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        };
        linkThread.start();

        pathThread = new Thread() {
            @Override
            public void run() {
                try {
                    int[][] tempMatrix = new int[links.size()][nodes.size()], adjMatrix = new int[nodes.size()][nodes.size()],
                            pathMatrix = new int[nodes.size()][nodes.size()];
                    int i, j, row, col;

                    ArrayList<Integer> outNode = new ArrayList<>(), arrayList = new ArrayList<>();

                    for (i = 0; i < nodes.size(); i++) {

                        for (Integer link : nodes.get(i).linkId) {
                            tempMatrix[link][i] = 1;
                        }

                        if (nodes.get(i).linkId.size() == 1) {
                            outNode.add(i);
                        }

                        Arrays.fill(adjMatrix[i], Constants.INF);
                        adjMatrix[i][i] = 0;
                    }

                    for (i = 0; i < links.size(); i++) {
                        for (j = 0; j < nodes.size(); j++) {
                            arrayList.add(tempMatrix[i][j]);
                        }

                        row = arrayList.indexOf(1);
                        col = arrayList.lastIndexOf(1);

                        adjMatrix[row][col] = adjMatrix[col][row] = 1;
                        pathMatrix[row][col] = pathMatrix[col][row] = i;

                        arrayList.clear();
                    }

                    int[][] next = floydWarshall(adjMatrix);

                    Random rand = new Random();

                    int s, d;
                    Integer src, dest;

                    ArrayList<String> outPath = new ArrayList<>(), outDemand = new ArrayList<>();

                    i = 0;
                    while (outNode.size() > 1) {

                        s = rand.nextInt(outNode.size());
                        d = rand.nextInt(outNode.size());

                        if (s == d) {
                            continue;
                        }

                        src = outNode.get(s);
                        dest = outNode.get(d);

                        outPath.add(getPath(src, dest, next, pathMatrix));
                        outDemand.add(src + " " + dest + " " + Constants.TRAFFIC);

                        outPath.add(getPath(dest, src, next, pathMatrix));
                        outDemand.add(dest + " " + src + " " + Constants.TRAFFIC);

                        if (rand.nextInt(2) == 0) {
                            outNode.remove(src);
                        } else {
                            outNode.remove(dest);
                        }

                        i++;
                    }

                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(Constants.PATH))) {
                        bw.write(i * 2 + "\n");
                        for (String path : outPath) {
                            bw.write(path + "\n");
                        }
                    }

                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(Constants.DEMAND))) {
                        bw.write(i * 2 + "\n");
                        for (String demand : outDemand) {
                            bw.write(demand + "\n");
                        }
                    }

                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        };
        pathThread.start();
        pixelThread.join();
        rawCornerPointsThread.join();
        cornerPointsThread.join();
        roadThread.join();
        nodeThread.join();
        linkThread.join();
        pathThread.join();
    }
}
