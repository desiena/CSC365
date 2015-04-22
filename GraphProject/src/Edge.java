/**
 * Created by home on 4/8/15.
 */
public class Edge {
    Vertex destination;
    Edge next;
    double weight;

    public Edge(Vertex v, double weight){
        destination = v;
        this.weight = weight;
    }
}
