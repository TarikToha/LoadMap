
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Corner is special type of Point which has an area associated with it. The
 * area indicated the size of the road junction. Also it has an adjacent list to
 * indicate other corners adjacent to this corner
 */
public class Corner extends Point {

    final int id;
    final double radius;
    final HashSet<Integer> adjacent = new HashSet<>();
    final ArrayList<Integer> linkId = new ArrayList<>();

    public Corner(int id, double x, double y, double area) {
        super(x, y);
        this.id = id;
        this.radius = Math.sqrt(area / Math.acos(-1));
    }

}
