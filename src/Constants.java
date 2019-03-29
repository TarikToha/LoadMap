
final class Constants {

    static final boolean WHOLEMAP = true;
    static final boolean ALLPOINTSFILE = false;
    static final boolean RAWCORNERPOINTSFILE = true;
    static final boolean PRINT = true;

    //input file
    static final String FILENAMEIN = "Miami_beach.csv";

    static final int RESOLUTION = 1;
    static final int ANGRESOLUTION = 3;
    static final double DIVIDERWIDTH = 2;
    static final double LEASTDIST = 4;
    static final double MOSTDIST = 100;
    static final double MULTI = 1.4;
    static final int QUANTILE = 75;
    static final double CORNERAREAFACTOR = 12;
    static final double EPS = 1e-10;
    static final int INF = 999999999;
    static final double TOLERANCE = 1.1;
    static final int TRAFFIC = 200;

    //internal output files
    static final String ALLPOINTS = "AllPoints.csv";
    static final String CORNERPOINTSRAW = "CornerPointsRaw.csv";
    static final String CORNERPOINTS = "CornerPoints.csv";
    static final String ROADS = "Roads.csv";

    //final output files
    static final String NODE = "node.txt";
    static final String LINK = "link.txt";
    static final String PATH = "path.txt";
    static final String DEMAND = "demand.txt";
}
